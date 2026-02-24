package com.andrin.examcountdown.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.TimetableLesson
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "exam_store")

data class SyncStatus(
    val lastSyncAtMillis: Long? = null,
    val lastSyncSummary: String? = null,
    val lastSyncError: String? = null
)

class ExamRepository(private val appContext: Context) {
    private val examsKey = stringPreferencesKey("exams_json")
    private val lessonsKey = stringPreferencesKey("lessons_json")
    private val iCalUrlKey = stringPreferencesKey("ical_url")
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val lastSyncAtMillisKey = longPreferencesKey("last_sync_at_ms")
    private val lastSyncSummaryKey = stringPreferencesKey("last_sync_summary")
    private val lastSyncErrorKey = stringPreferencesKey("last_sync_error")
    private val json = Json { ignoreUnknownKeys = true }

    private val preferencesFlow: Flow<Preferences> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val examsFlow: Flow<List<Exam>> = preferencesFlow
        .map { preferences ->
            decodeExams(preferences[examsKey])
                .sortedBy { it.startsAtEpochMillis }
        }

    val lessonsFlow: Flow<List<TimetableLesson>> = preferencesFlow
        .map { preferences ->
            decodeLessons(preferences[lessonsKey])
                .sortedBy { it.startsAtEpochMillis }
        }

    val iCalUrlFlow: Flow<String> = preferencesFlow
        .map { preferences -> preferences[iCalUrlKey].orEmpty() }

    val onboardingDoneFlow: Flow<Boolean> = preferencesFlow
        .map { preferences -> preferences[onboardingDoneKey] ?: false }

    val syncStatusFlow: Flow<SyncStatus> = preferencesFlow
        .map { preferences ->
            SyncStatus(
                lastSyncAtMillis = preferences[lastSyncAtMillisKey],
                lastSyncSummary = preferences[lastSyncSummaryKey],
                lastSyncError = preferences[lastSyncErrorKey]
            )
        }

    suspend fun addExam(exam: Exam) {
        updateExams { current ->
            (current + exam)
                .distinctBy { it.id }
                .sortedBy { it.startsAtEpochMillis }
        }
    }

    suspend fun replaceIcalImportedExams(imported: List<Exam>) {
        updateExams { current ->
            val manualExams = current.filterNot { it.id.startsWith("ical:") }
            (manualExams + imported)
                .sortedBy { it.startsAtEpochMillis }
        }
    }

    suspend fun replaceSyncedLessons(imported: List<TimetableLesson>) {
        appContext.dataStore.edit { preferences ->
            val updated = imported.sortedBy { it.startsAtEpochMillis }
            preferences[lessonsKey] = json.encodeToString(updated)
        }
    }

    suspend fun deleteExam(examId: String) {
        updateExams { current ->
            current.filterNot { it.id == examId }
        }
    }

    suspend fun saveIcalUrl(url: String) {
        appContext.dataStore.edit { preferences ->
            preferences[iCalUrlKey] = url.trim()
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[onboardingDoneKey] = done
        }
    }

    suspend fun markSyncSuccess(summary: String) {
        appContext.dataStore.edit { preferences ->
            preferences[lastSyncAtMillisKey] = System.currentTimeMillis()
            preferences[lastSyncSummaryKey] = summary.trim()
            preferences.remove(lastSyncErrorKey)
        }
    }

    suspend fun markSyncError(error: String) {
        appContext.dataStore.edit { preferences ->
            preferences[lastSyncErrorKey] = error.trim()
        }
    }

    suspend fun readIcalUrl(): String? = iCalUrlFlow.first().takeIf { it.isNotBlank() }

    suspend fun readSnapshot(): List<Exam> = examsFlow.first()
    suspend fun readLessonsSnapshot(): List<TimetableLesson> = lessonsFlow.first()

    private suspend fun updateExams(transform: (List<Exam>) -> List<Exam>) {
        appContext.dataStore.edit { preferences ->
            val updated = transform(decodeExams(preferences[examsKey]))
            preferences[examsKey] = json.encodeToString(updated)
        }
    }

    private fun decodeExams(raw: String?): List<Exam> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Exam>>(raw) }
            .getOrDefault(emptyList())
    }

    private fun decodeLessons(raw: String?): List<TimetableLesson> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<TimetableLesson>>(raw) }
            .getOrDefault(emptyList())
    }
}
