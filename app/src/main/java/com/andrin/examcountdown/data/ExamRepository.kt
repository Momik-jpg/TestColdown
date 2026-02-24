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
import com.andrin.examcountdown.model.TimetableChangeEntry
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
    private val timetableChangesKey = stringPreferencesKey("timetable_changes_json")
    private val iCalUrlKey = stringPreferencesKey("ical_url")
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val onboardingPromptSeenKey = booleanPreferencesKey("onboarding_prompt_seen")
    private val quietHoursEnabledKey = booleanPreferencesKey("quiet_hours_enabled")
    private val quietHoursStartMinutesKey = longPreferencesKey("quiet_hours_start_minutes")
    private val quietHoursEndMinutesKey = longPreferencesKey("quiet_hours_end_minutes")
    private val syncIntervalMinutesKey = longPreferencesKey("sync_interval_minutes")
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

    val timetableChangesFlow: Flow<List<TimetableChangeEntry>> = preferencesFlow
        .map { preferences ->
            decodeTimetableChanges(preferences[timetableChangesKey])
                .sortedByDescending { it.changedAtEpochMillis }
        }

    val iCalUrlFlow: Flow<String> = preferencesFlow
        .map { preferences -> preferences[iCalUrlKey].orEmpty() }

    val onboardingDoneFlow: Flow<Boolean> = preferencesFlow
        .map { preferences -> preferences[onboardingDoneKey] ?: false }

    val onboardingPromptSeenFlow: Flow<Boolean> = preferencesFlow
        .map { preferences -> preferences[onboardingPromptSeenKey] ?: false }

    val preferencesLoadedFlow: Flow<Boolean> = preferencesFlow
        .map { true }

    val quietHoursFlow: Flow<QuietHoursConfig> = preferencesFlow
        .map { preferences ->
            val start = preferences[quietHoursStartMinutesKey]?.toInt() ?: (22 * 60)
            val end = preferences[quietHoursEndMinutesKey]?.toInt() ?: (7 * 60)
            QuietHoursConfig(
                enabled = preferences[quietHoursEnabledKey] ?: false,
                startMinutesOfDay = start.coerceIn(0, 24 * 60 - 1),
                endMinutesOfDay = end.coerceIn(0, 24 * 60 - 1)
            )
        }

    val syncStatusFlow: Flow<SyncStatus> = preferencesFlow
        .map { preferences ->
            SyncStatus(
                lastSyncAtMillis = preferences[lastSyncAtMillisKey],
                lastSyncSummary = preferences[lastSyncSummaryKey],
                lastSyncError = preferences[lastSyncErrorKey]
            )
        }

    val syncIntervalMinutesFlow: Flow<Long> = preferencesFlow
        .map { preferences ->
            normalizeSyncIntervalMinutes(preferences[syncIntervalMinutesKey] ?: DEFAULT_SYNC_INTERVAL_MINUTES)
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

    suspend fun appendTimetableChanges(
        changes: List<TimetableChangeEntry>,
        maxEntries: Int = 120
    ) {
        if (changes.isEmpty()) return
        appContext.dataStore.edit { preferences ->
            val current = decodeTimetableChanges(preferences[timetableChangesKey])
            val merged = (changes + current)
                .sortedByDescending { it.changedAtEpochMillis }
                .distinctBy { entry ->
                    "${entry.lessonId}|${entry.changeType}|${entry.startsAtEpochMillis}|${entry.oldValue.orEmpty()}|${entry.newValue.orEmpty()}|${entry.changedAtEpochMillis}"
                }
                .take(maxEntries)
            preferences[timetableChangesKey] = json.encodeToString(merged)
        }
    }

    suspend fun clearTimetableChanges() {
        appContext.dataStore.edit { preferences ->
            preferences.remove(timetableChangesKey)
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

    suspend fun setOnboardingPromptSeen(seen: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[onboardingPromptSeenKey] = seen
        }
    }

    suspend fun saveQuietHours(config: QuietHoursConfig) {
        appContext.dataStore.edit { preferences ->
            preferences[quietHoursEnabledKey] = config.enabled
            preferences[quietHoursStartMinutesKey] = config.startMinutesOfDay.toLong()
            preferences[quietHoursEndMinutesKey] = config.endMinutesOfDay.toLong()
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

    suspend fun saveSyncIntervalMinutes(minutes: Long) {
        appContext.dataStore.edit { preferences ->
            preferences[syncIntervalMinutesKey] = normalizeSyncIntervalMinutes(minutes)
        }
    }

    suspend fun readIcalUrl(): String? = iCalUrlFlow.first().takeIf { it.isNotBlank() }
    suspend fun readSyncIntervalMinutes(): Long = syncIntervalMinutesFlow.first()

    suspend fun readSnapshot(): List<Exam> = examsFlow.first()
    suspend fun readLessonsSnapshot(): List<TimetableLesson> = lessonsFlow.first()
    suspend fun readTimetableChangesSnapshot(): List<TimetableChangeEntry> = timetableChangesFlow.first()
    suspend fun readQuietHoursConfig(): QuietHoursConfig = quietHoursFlow.first()

    suspend fun exportBackupJson(): String {
        val backup = AppBackup(
            exams = readSnapshot(),
            lessons = readLessonsSnapshot(),
            timetableChanges = readTimetableChangesSnapshot(),
            iCalUrl = readIcalUrl(),
            onboardingDone = onboardingDoneFlow.first(),
            onboardingPromptSeen = onboardingPromptSeenFlow.first(),
            quietHours = readQuietHoursConfig(),
            syncIntervalMinutes = readSyncIntervalMinutes()
        )
        return json.encodeToString(backup)
    }

    suspend fun importBackupJson(raw: String): AppBackup {
        val backup = json.decodeFromString<AppBackup>(raw)
        appContext.dataStore.edit { preferences ->
            preferences[examsKey] = json.encodeToString(
                backup.exams.sortedBy { it.startsAtEpochMillis }
            )
            preferences[lessonsKey] = json.encodeToString(
                backup.lessons.sortedBy { it.startsAtEpochMillis }
            )
            preferences[timetableChangesKey] = json.encodeToString(
                backup.timetableChanges
                    .sortedByDescending { it.changedAtEpochMillis }
                    .take(120)
            )

            val url = backup.iCalUrl?.trim().orEmpty()
            if (url.isBlank()) {
                preferences.remove(iCalUrlKey)
            } else {
                preferences[iCalUrlKey] = url
            }

            preferences[onboardingDoneKey] = backup.onboardingDone
            preferences[onboardingPromptSeenKey] = backup.onboardingPromptSeen
            preferences[quietHoursEnabledKey] = backup.quietHours.enabled
            preferences[quietHoursStartMinutesKey] = backup.quietHours.startMinutesOfDay.toLong()
            preferences[quietHoursEndMinutesKey] = backup.quietHours.endMinutesOfDay.toLong()
            preferences[syncIntervalMinutesKey] = normalizeSyncIntervalMinutes(backup.syncIntervalMinutes)
        }
        return backup
    }

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

    private fun decodeTimetableChanges(raw: String?): List<TimetableChangeEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<TimetableChangeEntry>>(raw) }
            .getOrDefault(emptyList())
    }

    private fun normalizeSyncIntervalMinutes(value: Long): Long {
        return value.coerceIn(15L, 12L * 60L)
    }

    companion object {
        const val DEFAULT_SYNC_INTERVAL_MINUTES: Long = 6L * 60L
    }
}
