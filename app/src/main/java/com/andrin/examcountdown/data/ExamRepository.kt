package com.andrin.examcountdown.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableLesson
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
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

data class IcalSyncCacheHeaders(
    val etag: String? = null,
    val lastModified: String? = null
)

data class SyncDiagnostics(
    val lastAttemptAtMillis: Long? = null,
    val lastDurationMillis: Long? = null,
    val lastHttpStatusCode: Int? = null,
    val lastDeltaNotModified: Boolean = false,
    val importedExams: Int = 0,
    val importedLessons: Int = 0,
    val importedEvents: Int = 0,
    val changedLessons: Int = 0,
    val movedLessons: Int = 0,
    val roomChangedLessons: Int = 0,
    val lastErrorReason: String? = null
)

data class CollisionRuleSettings(
    val includeLessonCollisions: Boolean = true,
    val includeEventCollisions: Boolean = false,
    val onlyDifferentSubject: Boolean = true,
    val requireExactTimeOverlap: Boolean = true
)

class ExamRepository(private val appContext: Context) {
    private val examsKey = stringPreferencesKey("exams_json")
    private val lessonsKey = stringPreferencesKey("lessons_json")
    private val eventsKey = stringPreferencesKey("events_json")
    private val timetableChangesKey = stringPreferencesKey("timetable_changes_json")
    // Legacy key: kept only for one-time migration to encrypted storage.
    private val iCalUrlKey = stringPreferencesKey("ical_url")
    private val iCalUrlRevisionKey = longPreferencesKey("ical_url_revision")
    private val importEventsEnabledKey = booleanPreferencesKey("import_events_enabled")
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val onboardingPromptSeenKey = booleanPreferencesKey("onboarding_prompt_seen")
    private val quietHoursEnabledKey = booleanPreferencesKey("quiet_hours_enabled")
    private val quietHoursStartMinutesKey = longPreferencesKey("quiet_hours_start_minutes")
    private val quietHoursEndMinutesKey = longPreferencesKey("quiet_hours_end_minutes")
    private val syncIntervalMinutesKey = longPreferencesKey("sync_interval_minutes")
    private val showSyncStatusStripKey = booleanPreferencesKey("show_sync_status_strip")
    private val showTimetableTabKey = booleanPreferencesKey("show_timetable_tab")
    private val showAgendaTabKey = booleanPreferencesKey("show_agenda_tab")
    private val showExamCollisionBadgesKey = booleanPreferencesKey("show_exam_collision_badges")
    private val collisionIncludeLessonsKey = booleanPreferencesKey("collision_include_lessons")
    private val collisionIncludeEventsKey = booleanPreferencesKey("collision_include_events")
    private val collisionOnlyDifferentSubjectKey = booleanPreferencesKey("collision_only_different_subject")
    private val collisionRequireExactOverlapKey = booleanPreferencesKey("collision_require_exact_overlap")
    private val accessibilityModeEnabledKey = booleanPreferencesKey("accessibility_mode_enabled")
    private val simpleModeEnabledKey = booleanPreferencesKey("simple_mode_enabled")
    private val lastSeenVersionKey = stringPreferencesKey("last_seen_version")
    private val showSetupGuideCardKey = booleanPreferencesKey("show_setup_guide_card")
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val appLockPinHashKey = stringPreferencesKey("app_lock_pin_hash")
    private val appLockPinSaltKey = stringPreferencesKey("app_lock_pin_salt")
    private val appLockBiometricEnabledKey = booleanPreferencesKey("app_lock_biometric_enabled")
    private val iCalEtagKey = stringPreferencesKey("ical_etag")
    private val iCalLastModifiedKey = stringPreferencesKey("ical_last_modified")
    private val lastSyncAtMillisKey = longPreferencesKey("last_sync_at_ms")
    private val lastSyncSummaryKey = stringPreferencesKey("last_sync_summary")
    private val lastSyncErrorKey = stringPreferencesKey("last_sync_error")
    private val diagAttemptAtMillisKey = longPreferencesKey("sync_diag_attempt_at_ms")
    private val diagDurationMillisKey = longPreferencesKey("sync_diag_duration_ms")
    private val diagHttpStatusKey = longPreferencesKey("sync_diag_http_status")
    private val diagDeltaNotModifiedKey = booleanPreferencesKey("sync_diag_delta_not_modified")
    private val diagImportedExamsKey = longPreferencesKey("sync_diag_imported_exams")
    private val diagImportedLessonsKey = longPreferencesKey("sync_diag_imported_lessons")
    private val diagImportedEventsKey = longPreferencesKey("sync_diag_imported_events")
    private val diagChangedLessonsKey = longPreferencesKey("sync_diag_changed_lessons")
    private val diagMovedLessonsKey = longPreferencesKey("sync_diag_moved_lessons")
    private val diagRoomChangedLessonsKey = longPreferencesKey("sync_diag_room_changed_lessons")
    private val diagLastErrorReasonKey = stringPreferencesKey("sync_diag_last_error_reason")
    private val json = Json { ignoreUnknownKeys = true }
    private val secureIcalUrlStore = SecureIcalUrlStore(appContext)

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

    val eventsFlow: Flow<List<SchoolEvent>> = preferencesFlow
        .map { preferences ->
            decodeEvents(preferences[eventsKey])
                .sortedBy { it.startsAtEpochMillis }
        }

    val timetableChangesFlow: Flow<List<TimetableChangeEntry>> = preferencesFlow
        .map { preferences ->
            decodeTimetableChanges(preferences[timetableChangesKey])
                .sortedByDescending { it.changedAtEpochMillis }
        }

    val iCalUrlFlow: Flow<String> = preferencesFlow
        .map { preferences ->
            // Touch revision key so changes in encrypted storage trigger flow refresh.
            preferences[iCalUrlRevisionKey]
            secureIcalUrlStore.read()
                ?: preferences[iCalUrlKey].orEmpty().trim()
        }

    val importEventsEnabledFlow: Flow<Boolean> = preferencesFlow
        .map { preferences -> preferences[importEventsEnabledKey] ?: false }

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

    val syncDiagnosticsFlow: Flow<SyncDiagnostics> = preferencesFlow
        .map { preferences ->
            SyncDiagnostics(
                lastAttemptAtMillis = preferences[diagAttemptAtMillisKey],
                lastDurationMillis = preferences[diagDurationMillisKey],
                lastHttpStatusCode = preferences[diagHttpStatusKey]?.toInt(),
                lastDeltaNotModified = preferences[diagDeltaNotModifiedKey] ?: false,
                importedExams = preferences[diagImportedExamsKey]?.toInt() ?: 0,
                importedLessons = preferences[diagImportedLessonsKey]?.toInt() ?: 0,
                importedEvents = preferences[diagImportedEventsKey]?.toInt() ?: 0,
                changedLessons = preferences[diagChangedLessonsKey]?.toInt() ?: 0,
                movedLessons = preferences[diagMovedLessonsKey]?.toInt() ?: 0,
                roomChangedLessons = preferences[diagRoomChangedLessonsKey]?.toInt() ?: 0,
                lastErrorReason = preferences[diagLastErrorReasonKey]
            )
        }

    val syncIntervalMinutesFlow: Flow<Long> = preferencesFlow
        .map { preferences ->
            normalizeSyncIntervalMinutes(preferences[syncIntervalMinutesKey] ?: DEFAULT_SYNC_INTERVAL_MINUTES)
        }

    val showSyncStatusStripFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[showSyncStatusStripKey] ?: true
        }

    val showTimetableTabFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[showTimetableTabKey] ?: true
        }

    val showAgendaTabFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[showAgendaTabKey] ?: true
        }

    val showExamCollisionBadgesFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[showExamCollisionBadgesKey] ?: false
        }

    val collisionRuleSettingsFlow: Flow<CollisionRuleSettings> = preferencesFlow
        .map { preferences ->
            CollisionRuleSettings(
                includeLessonCollisions = preferences[collisionIncludeLessonsKey] ?: true,
                includeEventCollisions = preferences[collisionIncludeEventsKey] ?: false,
                onlyDifferentSubject = preferences[collisionOnlyDifferentSubjectKey] ?: true,
                requireExactTimeOverlap = preferences[collisionRequireExactOverlapKey] ?: true
            )
        }

    val accessibilityModeEnabledFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[accessibilityModeEnabledKey] ?: false
        }

    val simpleModeEnabledFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[simpleModeEnabledKey] ?: true
        }

    val lastSeenVersionFlow: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[lastSeenVersionKey].orEmpty()
        }

    val showSetupGuideCardFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[showSetupGuideCardKey] ?: true
        }

    val appLockEnabledFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[appLockEnabledKey] ?: false
        }

    val appLockBiometricEnabledFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            if (!(preferences[appLockEnabledKey] ?: false)) {
                false
            } else {
                preferences[appLockBiometricEnabledKey] ?: false
            }
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

    suspend fun replaceIcalSyncSnapshot(
        importedExams: List<Exam>,
        importedLessons: List<TimetableLesson>,
        importedEvents: List<SchoolEvent>
    ) {
        appContext.dataStore.edit { preferences ->
            val currentExams = decodeExams(preferences[examsKey])
            val manualExams = currentExams.filterNot { it.id.startsWith("ical:") }
            val mergedExams = (manualExams + importedExams)
                .distinctBy { it.id }
                .sortedBy { it.startsAtEpochMillis }
            val mergedLessons = importedLessons
                .distinctBy { it.id }
                .sortedBy { it.startsAtEpochMillis }
            val currentEvents = decodeEvents(preferences[eventsKey])
            val manualEvents = currentEvents.filterNot { isSyncedCalendarEventId(it.id) }
            val mergedEvents = importedEvents
                .plus(manualEvents)
                .distinctBy { it.id }
                .sortedBy { it.startsAtEpochMillis }

            preferences[examsKey] = json.encodeToString(mergedExams)
            preferences[lessonsKey] = json.encodeToString(mergedLessons)
            preferences[eventsKey] = json.encodeToString(mergedEvents)
        }
    }

    suspend fun replaceSyncedLessons(imported: List<TimetableLesson>) {
        appContext.dataStore.edit { preferences ->
            val updated = imported.sortedBy { it.startsAtEpochMillis }
            preferences[lessonsKey] = json.encodeToString(updated)
        }
    }

    suspend fun replaceSyncedEvents(imported: List<SchoolEvent>) {
        appContext.dataStore.edit { preferences ->
            val current = decodeEvents(preferences[eventsKey])
            val manualEvents = current.filterNot { isSyncedCalendarEventId(it.id) }
            val updated = imported
                .plus(manualEvents)
                .distinctBy { it.id }
                .sortedBy { it.startsAtEpochMillis }
            preferences[eventsKey] = json.encodeToString(updated)
        }
    }

    suspend fun addCustomEvents(events: List<SchoolEvent>) {
        if (events.isEmpty()) return
        updateEvents { current ->
            (current + events)
                .distinctBy { it.id }
                .sortedBy { it.startsAtEpochMillis }
        }
    }

    suspend fun deleteEvent(eventId: String) {
        updateEvents { current ->
            current.filterNot { it.id == eventId }
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
        val normalized = normalizeAndValidateIcalUrl(url)
        val previous = secureIcalUrlStore.read().orEmpty()
        secureIcalUrlStore.write(normalized)
        appContext.dataStore.edit { preferences ->
            // Remove legacy plain-text value after migration/update.
            preferences.remove(iCalUrlKey)
            preferences[iCalUrlRevisionKey] = System.currentTimeMillis()
            if (previous != normalized) {
                preferences.remove(iCalEtagKey)
                preferences.remove(iCalLastModifiedKey)
                preferences.remove(lastSyncAtMillisKey)
                preferences.remove(lastSyncSummaryKey)
                preferences.remove(lastSyncErrorKey)
                preferences.remove(diagAttemptAtMillisKey)
                preferences.remove(diagDurationMillisKey)
                preferences.remove(diagHttpStatusKey)
                preferences.remove(diagDeltaNotModifiedKey)
                preferences.remove(diagImportedExamsKey)
                preferences.remove(diagImportedLessonsKey)
                preferences.remove(diagImportedEventsKey)
                preferences.remove(diagChangedLessonsKey)
                preferences.remove(diagMovedLessonsKey)
                preferences.remove(diagRoomChangedLessonsKey)
                preferences.remove(diagLastErrorReasonKey)
            }
        }
    }

    suspend fun migrateLegacyIcalUrlIfNeeded() {
        val secureUrl = secureIcalUrlStore.read()
        if (!secureUrl.isNullOrBlank()) return
        val preferences = preferencesFlow.first()
        val legacyUrl = normalizeImportedIcalUrlOrNull(preferences[iCalUrlKey]) ?: return
        secureIcalUrlStore.write(legacyUrl)
        appContext.dataStore.edit { editable ->
            editable.remove(iCalUrlKey)
            editable[iCalUrlRevisionKey] = System.currentTimeMillis()
        }
    }

    suspend fun setImportEventsEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[importEventsEnabledKey] = enabled
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

    suspend fun saveIcalSyncCacheHeaders(headers: IcalSyncCacheHeaders) {
        appContext.dataStore.edit { preferences ->
            val etag = headers.etag?.trim().orEmpty()
            val lastModified = headers.lastModified?.trim().orEmpty()
            if (etag.isBlank()) {
                preferences.remove(iCalEtagKey)
            } else {
                preferences[iCalEtagKey] = etag
            }
            if (lastModified.isBlank()) {
                preferences.remove(iCalLastModifiedKey)
            } else {
                preferences[iCalLastModifiedKey] = lastModified
            }
        }
    }

    suspend fun saveSyncDiagnostics(diagnostics: SyncDiagnostics) {
        appContext.dataStore.edit { preferences ->
            diagnostics.lastAttemptAtMillis?.let {
                preferences[diagAttemptAtMillisKey] = it
            } ?: preferences.remove(diagAttemptAtMillisKey)
            diagnostics.lastDurationMillis?.let {
                preferences[diagDurationMillisKey] = it
            } ?: preferences.remove(diagDurationMillisKey)
            diagnostics.lastHttpStatusCode?.let {
                preferences[diagHttpStatusKey] = it.toLong()
            } ?: preferences.remove(diagHttpStatusKey)

            preferences[diagDeltaNotModifiedKey] = diagnostics.lastDeltaNotModified
            preferences[diagImportedExamsKey] = diagnostics.importedExams.toLong()
            preferences[diagImportedLessonsKey] = diagnostics.importedLessons.toLong()
            preferences[diagImportedEventsKey] = diagnostics.importedEvents.toLong()
            preferences[diagChangedLessonsKey] = diagnostics.changedLessons.toLong()
            preferences[diagMovedLessonsKey] = diagnostics.movedLessons.toLong()
            preferences[diagRoomChangedLessonsKey] = diagnostics.roomChangedLessons.toLong()

            val error = diagnostics.lastErrorReason?.trim().orEmpty()
            if (error.isBlank()) {
                preferences.remove(diagLastErrorReasonKey)
            } else {
                preferences[diagLastErrorReasonKey] = error
            }
        }
    }

    suspend fun saveSyncIntervalMinutes(minutes: Long) {
        appContext.dataStore.edit { preferences ->
            preferences[syncIntervalMinutesKey] = normalizeSyncIntervalMinutes(minutes)
        }
    }

    suspend fun setShowSyncStatusStrip(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[showSyncStatusStripKey] = enabled
        }
    }

    suspend fun setShowTimetableTab(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[showTimetableTabKey] = enabled
        }
    }

    suspend fun setShowAgendaTab(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[showAgendaTabKey] = enabled
        }
    }

    suspend fun setShowExamCollisionBadges(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[showExamCollisionBadgesKey] = enabled
        }
    }

    suspend fun saveCollisionRuleSettings(settings: CollisionRuleSettings) {
        appContext.dataStore.edit { preferences ->
            preferences[collisionIncludeLessonsKey] = settings.includeLessonCollisions
            preferences[collisionIncludeEventsKey] = settings.includeEventCollisions
            preferences[collisionOnlyDifferentSubjectKey] = settings.onlyDifferentSubject
            preferences[collisionRequireExactOverlapKey] = settings.requireExactTimeOverlap
        }
    }

    suspend fun setAccessibilityModeEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[accessibilityModeEnabledKey] = enabled
        }
    }

    suspend fun setSimpleModeEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[simpleModeEnabledKey] = enabled
        }
    }

    suspend fun setLastSeenVersion(versionName: String) {
        val normalized = versionName.trim()
        appContext.dataStore.edit { preferences ->
            if (normalized.isBlank()) {
                preferences.remove(lastSeenVersionKey)
            } else {
                preferences[lastSeenVersionKey] = normalized
            }
        }
    }

    suspend fun setShowSetupGuideCard(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[showSetupGuideCardKey] = enabled
        }
    }

    suspend fun enableAppLockWithPin(pin: String, biometricEnabled: Boolean = false) {
        val normalizedPin = requireValidPin(pin)
        val saltBytes = ByteArray(APP_LOCK_SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val encodedSalt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val encodedHash = hashPin(normalizedPin, saltBytes)

        appContext.dataStore.edit { preferences ->
            preferences[appLockEnabledKey] = true
            preferences[appLockPinSaltKey] = encodedSalt
            preferences[appLockPinHashKey] = encodedHash
            preferences[appLockBiometricEnabledKey] = biometricEnabled
        }
    }

    suspend fun disableAppLock() {
        appContext.dataStore.edit { preferences ->
            preferences[appLockEnabledKey] = false
            preferences.remove(appLockPinSaltKey)
            preferences.remove(appLockPinHashKey)
            preferences.remove(appLockBiometricEnabledKey)
        }
    }

    suspend fun setAppLockBiometricEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            if (preferences[appLockEnabledKey] ?: false) {
                preferences[appLockBiometricEnabledKey] = enabled
            } else {
                preferences.remove(appLockBiometricEnabledKey)
            }
        }
    }

    suspend fun verifyAppLockPin(pin: String): Boolean {
        val normalizedPin = pin.trim()
        if (normalizedPin.isEmpty()) return false

        val preferences = preferencesFlow.first()
        if (!(preferences[appLockEnabledKey] ?: false)) {
            return true
        }

        val encodedSalt = preferences[appLockPinSaltKey].orEmpty()
        val encodedHash = preferences[appLockPinHashKey].orEmpty()
        if (encodedSalt.isBlank() || encodedHash.isBlank()) {
            return false
        }

        val saltBytes = runCatching {
            Base64.decode(encodedSalt, Base64.NO_WRAP)
        }.getOrNull() ?: return false

        val candidateHash = hashPin(normalizedPin, saltBytes)
        return candidateHash == encodedHash
    }

    suspend fun readIcalUrl(): String? = iCalUrlFlow.first().takeIf { it.isNotBlank() }
    suspend fun readImportEventsEnabled(): Boolean = importEventsEnabledFlow.first()
    suspend fun readSyncIntervalMinutes(): Long = syncIntervalMinutesFlow.first()
    suspend fun readCollisionRuleSettings(): CollisionRuleSettings = collisionRuleSettingsFlow.first()
    suspend fun readAccessibilityModeEnabled(): Boolean = accessibilityModeEnabledFlow.first()
    suspend fun readIcalSyncCacheHeaders(): IcalSyncCacheHeaders {
        val preferences = preferencesFlow.first()
        return IcalSyncCacheHeaders(
            etag = preferences[iCalEtagKey],
            lastModified = preferences[iCalLastModifiedKey]
        )
    }

    suspend fun readSnapshot(): List<Exam> = examsFlow.first()
    suspend fun readLessonsSnapshot(): List<TimetableLesson> = lessonsFlow.first()
    suspend fun readEventsSnapshot(): List<SchoolEvent> = eventsFlow.first()
    suspend fun readTimetableChangesSnapshot(): List<TimetableChangeEntry> = timetableChangesFlow.first()
    suspend fun readQuietHoursConfig(): QuietHoursConfig = quietHoursFlow.first()
    suspend fun readSyncDiagnostics(): SyncDiagnostics = syncDiagnosticsFlow.first()

    suspend fun exportBackupJson(): String {
        val collisionRules = readCollisionRuleSettings()
        val backup = AppBackup(
            exams = readSnapshot(),
            lessons = readLessonsSnapshot(),
            events = readEventsSnapshot(),
            timetableChanges = readTimetableChangesSnapshot(),
            // Sensitive tokenized iCal links are intentionally excluded from backups.
            iCalUrl = null,
            importEventsEnabled = readImportEventsEnabled(),
            showTimetableTab = showTimetableTabFlow.first(),
            showAgendaTab = showAgendaTabFlow.first(),
            showExamCollisionBadges = showExamCollisionBadgesFlow.first(),
            collisionIncludeLessons = collisionRules.includeLessonCollisions,
            collisionIncludeEvents = collisionRules.includeEventCollisions,
            collisionOnlyDifferentSubject = collisionRules.onlyDifferentSubject,
            collisionRequireExactTimeOverlap = collisionRules.requireExactTimeOverlap,
            accessibilityModeEnabled = readAccessibilityModeEnabled(),
            simpleModeEnabled = simpleModeEnabledFlow.first(),
            appLockBiometricEnabled = appLockBiometricEnabledFlow.first(),
            showSetupGuideCard = showSetupGuideCardFlow.first(),
            onboardingDone = onboardingDoneFlow.first(),
            onboardingPromptSeen = onboardingPromptSeenFlow.first(),
            quietHours = readQuietHoursConfig(),
            syncIntervalMinutes = readSyncIntervalMinutes(),
            showSyncStatusStrip = showSyncStatusStripFlow.first()
        )
        return json.encodeToString(backup)
    }

    suspend fun importBackupJson(raw: String): AppBackup {
        val normalizedRaw = raw.trim()
        require(normalizedRaw.isNotBlank()) { "Backup ist leer." }
        require(normalizedRaw.length <= MAX_BACKUP_CHARS) {
            "Backup ist zu groß (max. ${MAX_BACKUP_CHARS / 1_000} KB)."
        }

        val backup = runCatching { json.decodeFromString<AppBackup>(normalizedRaw) }
            .getOrElse { throwable ->
                throw IllegalArgumentException("Backup-Datei ist ungültig.", throwable)
            }

        require(backup.schemaVersion in 1..AppBackup.CURRENT_SCHEMA_VERSION) {
            "Nicht unterstützte Backup-Version (${backup.schemaVersion})."
        }

        val sanitizedExams = backup.exams
            .distinctBy { it.id }
            .take(MAX_BACKUP_EXAMS)
            .sortedBy { it.startsAtEpochMillis }

        val sanitizedLessons = backup.lessons
            .distinctBy { it.id }
            .take(MAX_BACKUP_LESSONS)
            .sortedBy { it.startsAtEpochMillis }

        val sanitizedEvents = backup.events
            .distinctBy { it.id }
            .take(MAX_BACKUP_EVENTS)
            .sortedBy { it.startsAtEpochMillis }

        val sanitizedChanges = backup.timetableChanges
            .sortedByDescending { it.changedAtEpochMillis }
            .distinctBy { entry ->
                "${entry.lessonId}|${entry.changeType}|${entry.startsAtEpochMillis}|${entry.oldValue.orEmpty()}|${entry.newValue.orEmpty()}|${entry.changedAtEpochMillis}"
            }
            .take(MAX_BACKUP_TIMETABLE_CHANGES)

        val sanitizedUrl = normalizeImportedIcalUrl(backup.iCalUrl)
        val sanitizedQuietHours = QuietHoursConfig(
            enabled = backup.quietHours.enabled,
            startMinutesOfDay = backup.quietHours.startMinutesOfDay.coerceIn(0, 24 * 60 - 1),
            endMinutesOfDay = backup.quietHours.endMinutesOfDay.coerceIn(0, 24 * 60 - 1)
        )

        appContext.dataStore.edit { preferences ->
            preferences[examsKey] = json.encodeToString(
                sanitizedExams
            )
            preferences[lessonsKey] = json.encodeToString(
                sanitizedLessons
            )
            preferences[eventsKey] = json.encodeToString(
                sanitizedEvents
            )
            preferences[timetableChangesKey] = json.encodeToString(
                sanitizedChanges
            )

            if (sanitizedUrl != null) {
                secureIcalUrlStore.write(sanitizedUrl)
                preferences[iCalUrlRevisionKey] = System.currentTimeMillis()
            }
            preferences.remove(iCalUrlKey)

            preferences[importEventsEnabledKey] = backup.importEventsEnabled
            preferences[showTimetableTabKey] = backup.showTimetableTab
            preferences[showAgendaTabKey] = backup.showAgendaTab
            preferences[showExamCollisionBadgesKey] = backup.showExamCollisionBadges
            preferences[collisionIncludeLessonsKey] = backup.collisionIncludeLessons
            preferences[collisionIncludeEventsKey] = backup.collisionIncludeEvents
            preferences[collisionOnlyDifferentSubjectKey] = backup.collisionOnlyDifferentSubject
            preferences[collisionRequireExactOverlapKey] = backup.collisionRequireExactTimeOverlap
            preferences[accessibilityModeEnabledKey] = backup.accessibilityModeEnabled
            preferences[simpleModeEnabledKey] = backup.simpleModeEnabled
            if (preferences[appLockEnabledKey] ?: false) {
                preferences[appLockBiometricEnabledKey] = backup.appLockBiometricEnabled
            } else {
                preferences.remove(appLockBiometricEnabledKey)
            }
            preferences[showSetupGuideCardKey] = backup.showSetupGuideCard
            preferences[onboardingDoneKey] = backup.onboardingDone
            preferences[onboardingPromptSeenKey] = backup.onboardingPromptSeen
            preferences[quietHoursEnabledKey] = sanitizedQuietHours.enabled
            preferences[quietHoursStartMinutesKey] = sanitizedQuietHours.startMinutesOfDay.toLong()
            preferences[quietHoursEndMinutesKey] = sanitizedQuietHours.endMinutesOfDay.toLong()
            preferences[syncIntervalMinutesKey] = normalizeSyncIntervalMinutes(backup.syncIntervalMinutes)
            preferences[showSyncStatusStripKey] = backup.showSyncStatusStrip
            preferences.remove(lastSyncAtMillisKey)
            preferences.remove(lastSyncSummaryKey)
            preferences.remove(lastSyncErrorKey)
            preferences.remove(iCalEtagKey)
            preferences.remove(iCalLastModifiedKey)
            preferences.remove(diagAttemptAtMillisKey)
            preferences.remove(diagDurationMillisKey)
            preferences.remove(diagHttpStatusKey)
            preferences.remove(diagDeltaNotModifiedKey)
            preferences.remove(diagImportedExamsKey)
            preferences.remove(diagImportedLessonsKey)
            preferences.remove(diagImportedEventsKey)
            preferences.remove(diagChangedLessonsKey)
            preferences.remove(diagMovedLessonsKey)
            preferences.remove(diagRoomChangedLessonsKey)
            preferences.remove(diagLastErrorReasonKey)
            preferences.remove(lastSeenVersionKey)
        }
        return backup
    }

    private suspend fun updateExams(transform: (List<Exam>) -> List<Exam>) {
        appContext.dataStore.edit { preferences ->
            val updated = transform(decodeExams(preferences[examsKey]))
            preferences[examsKey] = json.encodeToString(updated)
        }
    }

    private suspend fun updateEvents(transform: (List<SchoolEvent>) -> List<SchoolEvent>) {
        appContext.dataStore.edit { preferences ->
            val updated = transform(decodeEvents(preferences[eventsKey]))
            preferences[eventsKey] = json.encodeToString(updated)
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

    private fun decodeEvents(raw: String?): List<SchoolEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<SchoolEvent>>(raw) }
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

    private fun isSyncedCalendarEventId(id: String): Boolean {
        return id.startsWith("ical-event:") || id.startsWith("ical:")
    }

    private fun normalizeImportedIcalUrl(raw: String?): String? {
        return normalizeImportedIcalUrlOrNull(raw)
    }

    private fun requireValidPin(pin: String): String {
        val normalized = pin.trim()
        require(normalized.matches(Regex("^\\d{$APP_LOCK_PIN_MIN_DIGITS,$APP_LOCK_PIN_MAX_DIGITS}$"))) {
            "PIN muss aus $APP_LOCK_PIN_MIN_DIGITS bis $APP_LOCK_PIN_MAX_DIGITS Ziffern bestehen."
        }
        return normalized
    }

    private fun hashPin(pin: String, saltBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltBytes)
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    companion object {
        const val DEFAULT_SYNC_INTERVAL_MINUTES: Long = 6L * 60L
        const val APP_LOCK_PIN_MIN_DIGITS: Int = 4
        const val APP_LOCK_PIN_MAX_DIGITS: Int = 10
        private const val APP_LOCK_SALT_BYTES: Int = 16
        private const val MAX_BACKUP_CHARS: Int = 1_000_000
        private const val MAX_BACKUP_EXAMS: Int = 5_000
        private const val MAX_BACKUP_LESSONS: Int = 15_000
        private const val MAX_BACKUP_EVENTS: Int = 8_000
        private const val MAX_BACKUP_TIMETABLE_CHANGES: Int = 500
    }
}
