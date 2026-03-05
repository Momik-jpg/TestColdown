package com.andrin.examcountdown.integration

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.data.IcalHttpClient
import com.andrin.examcountdown.data.IcalSyncEngine
import com.andrin.examcountdown.data.SyncCoordinator
import com.andrin.examcountdown.data.SyncExecutionResult
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = android.app.Application::class
)
class SyncPipelineIntegrationTest {
    private lateinit var context: Context
    private lateinit var repository: ExamRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = ExamRepository(
            appContext = context,
            preferencesDataStoreOverride = InMemoryPreferencesDataStore()
        )
        SyncCoordinator.repositoryFactory = { repository }
        IcalSyncEngine.widgetUpdateHook = { _ -> }
    }

    @After
    fun tearDown() {
        IcalHttpClient.resetConnectionFactoryForTest()
        SyncCoordinator.resetRepositoryFactoryForTest()
        IcalSyncEngine.resetWidgetUpdateHookForTest()
    }

    @Test
    fun sync200_persistsSnapshot_and_updatesMetadata() = runBlocking {
        val fixture = buildIcsPayload("sync200")
        IcalHttpClient.connectionFactory = queueConnectionFactory(
            mutableListOf(
                FakeHttpResponse(
                    statusCode = HttpURLConnection.HTTP_OK,
                    body = fixture.ics,
                    etag = "\"v1\"",
                    lastModified = "Wed, 01 Jan 2026 10:00:00 GMT"
                )
            )
        )

        val result = SyncCoordinator.syncExplicit(
            context = context,
            urls = listOf(TEST_URL),
            includeEvents = true,
            emitChangeNotification = false
        )

        assertTrue(result is SyncExecutionResult.Success)
        val success = result as SyncExecutionResult.Success
        assertEquals(HttpURLConnection.HTTP_OK, success.result.httpStatusCode)
        assertTrue(success.result.examsImported > 0)
        assertTrue(success.result.lessonsImported > 0)
        assertTrue(success.result.eventsImported > 0)

        assertEquals(1, repository.readSnapshot().count { it.id == fixture.expectedExamId })
        assertEquals(1, repository.readLessonsSnapshot().count { it.id == fixture.expectedLessonId })
        assertEquals(1, repository.readEventsSnapshot().count { it.id == fixture.expectedEventId })

        val syncStatus = repository.syncStatusFlow.first()
        assertNotNull(syncStatus.lastSyncAtMillis)
        assertTrue(syncStatus.lastSyncError.isNullOrBlank())

        val diagnostics = repository.readSyncDiagnostics()
        assertEquals(HttpURLConnection.HTTP_OK, diagnostics.lastHttpStatusCode)
        assertEquals(false, diagnostics.lastDeltaNotModified)
        assertTrue(diagnostics.importedExams > 0)
        assertTrue(diagnostics.importedLessons > 0)
        assertTrue(diagnostics.importedEvents > 0)
    }

    @Test
    fun sync304_doesNotReimport_but_updatesMetadataOnly() = runBlocking {
        val fixture = buildIcsPayload("sync304")
        IcalHttpClient.connectionFactory = queueConnectionFactory(
            mutableListOf(
                FakeHttpResponse(
                    statusCode = HttpURLConnection.HTTP_OK,
                    body = fixture.ics,
                    etag = "\"v1\"",
                    lastModified = "Wed, 01 Jan 2026 10:00:00 GMT"
                ),
                FakeHttpResponse(
                    statusCode = HttpURLConnection.HTTP_NOT_MODIFIED,
                    etag = "\"v1\"",
                    lastModified = "Wed, 01 Jan 2026 10:00:00 GMT"
                )
            )
        )

        val firstSync = SyncCoordinator.syncExplicit(
            context = context,
            urls = listOf(TEST_URL),
            includeEvents = true,
            emitChangeNotification = false
        )
        assertTrue(firstSync is SyncExecutionResult.Success)

        val examCountBefore = repository.readSnapshot().count { it.id == fixture.expectedExamId }
        val lessonCountBefore = repository.readLessonsSnapshot().count { it.id == fixture.expectedLessonId }
        val eventCountBefore = repository.readEventsSnapshot().count { it.id == fixture.expectedEventId }

        val secondSync = SyncCoordinator.syncExplicit(
            context = context,
            urls = listOf(TEST_URL),
            includeEvents = true,
            emitChangeNotification = false
        )
        assertTrue(secondSync is SyncExecutionResult.Success)
        val secondSuccess = secondSync as SyncExecutionResult.Success
        assertTrue(secondSuccess.result.deltaNotModified)
        assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, secondSuccess.result.httpStatusCode)

        assertEquals(
            examCountBefore,
            repository.readSnapshot().count { it.id == fixture.expectedExamId }
        )
        assertEquals(
            lessonCountBefore,
            repository.readLessonsSnapshot().count { it.id == fixture.expectedLessonId }
        )
        assertEquals(
            eventCountBefore,
            repository.readEventsSnapshot().count { it.id == fixture.expectedEventId }
        )

        val diagnostics = repository.readSyncDiagnostics()
        assertEquals(true, diagnostics.lastDeltaNotModified)
        assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, diagnostics.lastHttpStatusCode)
        assertEquals(0, diagnostics.importedExams)
        assertEquals(0, diagnostics.importedLessons)
        assertEquals(0, diagnostics.importedEvents)
    }

    @Test
    fun syncFailure_setsError_and_doesNotCorruptSnapshot() = runBlocking {
        val fixture = buildIcsPayload("syncFail")
        IcalHttpClient.connectionFactory = queueConnectionFactory(
            mutableListOf(
                FakeHttpResponse(
                    statusCode = HttpURLConnection.HTTP_OK,
                    body = fixture.ics,
                    etag = "\"v1\"",
                    lastModified = "Wed, 01 Jan 2026 10:00:00 GMT"
                )
            )
        )

        val initialSync = SyncCoordinator.syncExplicit(
            context = context,
            urls = listOf(TEST_URL),
            includeEvents = true,
            emitChangeNotification = false
        )
        assertTrue(initialSync is SyncExecutionResult.Success)

        val examCountBefore = repository.readSnapshot().count { it.id == fixture.expectedExamId }
        val lessonCountBefore = repository.readLessonsSnapshot().count { it.id == fixture.expectedLessonId }
        val eventCountBefore = repository.readEventsSnapshot().count { it.id == fixture.expectedEventId }

        IcalHttpClient.connectionFactory = queueConnectionFactory(
            mutableListOf(
                FakeHttpResponse(statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR),
                FakeHttpResponse(statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR)
            )
        )

        val failedSync = SyncCoordinator.syncExplicit(
            context = context,
            urls = listOf(TEST_URL),
            includeEvents = true,
            emitChangeNotification = false
        )
        assertTrue(failedSync is SyncExecutionResult.Failed)

        assertEquals(
            examCountBefore,
            repository.readSnapshot().count { it.id == fixture.expectedExamId }
        )
        assertEquals(
            lessonCountBefore,
            repository.readLessonsSnapshot().count { it.id == fixture.expectedLessonId }
        )
        assertEquals(
            eventCountBefore,
            repository.readEventsSnapshot().count { it.id == fixture.expectedEventId }
        )

        val syncStatus = repository.syncStatusFlow.first()
        assertTrue(!syncStatus.lastSyncError.isNullOrBlank())

        val diagnostics = repository.readSyncDiagnostics()
        assertTrue(!diagnostics.lastErrorReason.isNullOrBlank())
    }

    private fun queueConnectionFactory(
        responses: MutableList<FakeHttpResponse>
    ): (String) -> HttpURLConnection {
        val index = AtomicInteger(0)
        return { url ->
            val position = index.getAndIncrement()
            val response = responses.getOrElse(position) {
                responses.lastOrNull()
                    ?: FakeHttpResponse(statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR)
            }
            FakeHttpConnection(URL(url), response)
        }
    }

    private fun buildIcsPayload(token: String): TestIcsFixture {
        val today = LocalDate.now(SCHOOL_ZONE)
        val examDay = today.plusDays(3)
        val lessonDay = today.plusDays(4)
        val eventDay = today.plusDays(5)
        val examUid = "etP_exam-$token@centerboard.ch"
        val lessonUid = "lesson-$token@centerboard.ch"
        val eventUid = "ett_event-$token@centerboard.ch"

        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:$examUid
            SUMMARY:mat_l24B_HeiCa Mathematik Prüfung 1
            DTSTART:${examDay.atTime(8, 0).format(DATE_TIME_FORMATTER)}
            DTEND:${examDay.atTime(9, 0).format(DATE_TIME_FORMATTER)}
            LOCATION:B401
            END:VEVENT
            BEGIN:VEVENT
            UID:$lessonUid
            SUMMARY:frw_l24B_MeiLu
            DTSTART:${lessonDay.atTime(7, 45).format(DATE_TIME_FORMATTER)}
            DTEND:${lessonDay.atTime(8, 30).format(DATE_TIME_FORMATTER)}
            LOCATION:B512
            END:VEVENT
            BEGIN:VEVENT
            UID:$eventUid
            SUMMARY:Infoveranstaltung Elternabend
            DTSTART:${eventDay.atTime(10, 0).format(DATE_TIME_FORMATTER)}
            DTEND:${eventDay.atTime(11, 0).format(DATE_TIME_FORMATTER)}
            LOCATION:Aula
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        return TestIcsFixture(
            ics = ics,
            expectedExamId = "ical:$examUid",
            expectedLessonId = "lesson:$lessonUid",
            expectedEventId = "ical-event:$eventUid"
        )
    }

    private data class TestIcsFixture(
        val ics: String,
        val expectedExamId: String,
        val expectedLessonId: String,
        val expectedEventId: String
    )

    private data class FakeHttpResponse(
        val statusCode: Int,
        val body: String? = null,
        val etag: String? = null,
        val lastModified: String? = null
    )

    private class FakeHttpConnection(
        url: URL,
        private val response: FakeHttpResponse
    ) : HttpURLConnection(url) {
        private val requestHeaders = mutableMapOf<String, String>()

        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun setRequestProperty(key: String?, value: String?) {
            if (key != null && value != null) {
                requestHeaders[key] = value
            }
        }

        override fun getRequestProperty(key: String?): String? {
            return key?.let { requestHeaders[it] }
        }

        override fun getResponseCode(): Int = response.statusCode

        override fun getHeaderField(name: String?): String? {
            return when (name) {
                "ETag" -> response.etag
                "Last-Modified" -> response.lastModified
                else -> null
            }
        }

        override fun getInputStream(): ByteArrayInputStream {
            val payload = response.body ?: throw IOException("No body configured for fake HTTP response.")
            return ByteArrayInputStream(payload.toByteArray(Charsets.UTF_8))
        }
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val state = MutableStateFlow<Preferences>(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            return mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                updated
            }
        }
    }

    companion object {
        private const val TEST_URL = "https://example.com/school.ics"
        private val SCHOOL_ZONE: ZoneId = ZoneId.of("Europe/Zurich")
        private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    }
}
