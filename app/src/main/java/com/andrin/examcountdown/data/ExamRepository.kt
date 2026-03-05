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
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "exam_store")

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

data class AppLockVerificationResult(
    val success: Boolean,
    val message: String? = null,
    val lockoutRemainingMillis: Long = 0L
)

class ExamRepository(
    private val appContext: Context,
    preferencesDataStoreOverride: DataStore<Preferences>? = null
) {
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
    private val screenshotProtectionEnabledKey = booleanPreferencesKey("screenshot_protection_enabled")
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val appLockPinHashKey = stringPreferencesKey("app_lock_pin_hash")
    private val appLockPinSaltKey = stringPreferencesKey("app_lock_pin_salt")
    private val appLockBiometricEnabledKey = booleanPreferencesKey("app_lock_biometric_enabled")
    private val appLockFailedAttemptsKey = longPreferencesKey("app_lock_failed_attempts")
    private val appLockLockUntilMillisKey = longPreferencesKey("app_lock_lock_until_ms")
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
    private val preferencesDataStore: DataStore<Preferences> =
        preferencesDataStoreOverride ?: appContext.dataStore
    private val secureIcalUrlStore: SecureIcalUrlStore by lazy {
        SecureIcalUrlStore(appContext)
    }
    private val snapshotStore = ExamSnapshotStore(preferencesDataStore, json)
    private val syncMetadataStore = ExamSyncMetadataStore()

    private val preferencesFlow: Flow<Preferences> = preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val examsFlow: Flow<List<Exam>> = snapshotStore.examsFlow(preferencesFlow)

    val lessonsFlow: Flow<List<TimetableLesson>> = snapshotStore.lessonsFlow(preferencesFlow)

    val eventsFlow: Flow<List<SchoolEvent>> = snapshotStore.eventsFlow(preferencesFlow)

    val timetableChangesFlow: Flow<List<TimetableChangeEntry>> =
        snapshotStore.timetableChangesFlow(preferencesFlow)

    val iCalUrlsFlow: Flow<List<String>> = preferencesFlow
        .map { preferences ->
            // Touch revision key so changes in encrypted storage trigger flow refresh.
            preferences[iCalUrlRevisionKey]
            val secureUrls = secureIcalUrlStore.readAll()
            if (secureUrls.isNotEmpty()) {
                secureUrls
            } else {
                preferences[iCalUrlKey]
                    .orEmpty()
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { listOf(it) }
                    ?: emptyList()
            }
        }

    val iCalUrlFlow: Flow<String> = iCalUrlsFlow
        .map { urls -> urls.firstOrNull().orEmpty() }

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

    val syncStatusFlow: Flow<SyncStatus> = syncMetadataStore.syncStatusFlow(preferencesFlow)
    val syncDiagnosticsFlow: Flow<SyncDiagnostics> = syncMetadataStore.syncDiagnosticsFlow(preferencesFlow)

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

    val screenshotProtectionEnabledFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[screenshotProtectionEnabledKey] ?: false
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
        snapshotStore.addExam(exam)
    }

    suspend fun replaceIcalImportedExams(imported: List<Exam>) {
        snapshotStore.replaceIcalImportedExams(imported)
    }

    suspend fun replaceIcalSyncSnapshot(
        importedExams: List<Exam>,
        importedLessons: List<TimetableLesson>,
        importedEvents: List<SchoolEvent>
    ) {
        snapshotStore.replaceIcalSyncSnapshot(
            importedExams = importedExams,
            importedLessons = importedLessons,
            importedEvents = importedEvents
        )
    }

    suspend fun replaceSyncedLessons(imported: List<TimetableLesson>) {
        snapshotStore.replaceSyncedLessons(imported)
    }

    suspend fun replaceSyncedEvents(imported: List<SchoolEvent>) {
        snapshotStore.replaceSyncedEvents(imported)
    }

    suspend fun addCustomEvents(events: List<SchoolEvent>) {
        snapshotStore.addCustomEvents(events)
    }

    suspend fun deleteEvent(eventId: String) {
        snapshotStore.deleteEvent(eventId)
    }

    suspend fun updateEvent(event: SchoolEvent) {
        snapshotStore.updateEvent(event)
    }

    suspend fun appendTimetableChanges(
        changes: List<TimetableChangeEntry>,
        maxEntries: Int = 120
    ) {
        snapshotStore.appendTimetableChanges(changes, maxEntries)
    }

    suspend fun clearTimetableChanges() {
        snapshotStore.clearTimetableChanges()
    }

    suspend fun deleteExam(examId: String) {
        snapshotStore.deleteExam(examId)
    }

    suspend fun saveIcalUrl(url: String) {
        saveIcalUrls(listOf(url))
    }

    suspend fun saveIcalUrls(urls: List<String>) {
        val normalized = urls
            .map { normalizeAndValidateIcalUrl(it) }
            .distinct()
            .take(MAX_ICAL_URLS)
        require(normalized.isNotEmpty()) { "Bitte mindestens eine iCal-URL eingeben." }

        val previous = secureIcalUrlStore.readAll()
        secureIcalUrlStore.writeAll(normalized)
        preferencesDataStore.edit { preferences ->
            // Remove legacy plain-text value after migration/update.
            preferences.remove(iCalUrlKey)
            preferences[iCalUrlRevisionKey] = System.currentTimeMillis()
            if (previous != normalized) {
                syncMetadataStore.clearSyncMetadata(preferences)
            }
        }
    }

    suspend fun migrateLegacyIcalUrlIfNeeded() {
        if (secureIcalUrlStore.readAll().isNotEmpty()) return
        val preferences = preferencesFlow.first()
        val legacyUrl = normalizeImportedIcalUrlOrNull(preferences[iCalUrlKey]) ?: return
        secureIcalUrlStore.writeAll(listOf(legacyUrl))
        preferencesDataStore.edit { editable ->
            editable.remove(iCalUrlKey)
            editable[iCalUrlRevisionKey] = System.currentTimeMillis()
        }
    }

    suspend fun setImportEventsEnabled(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[importEventsEnabledKey] = enabled
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[onboardingDoneKey] = done
        }
    }

    suspend fun setOnboardingPromptSeen(seen: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[onboardingPromptSeenKey] = seen
        }
    }

    suspend fun saveQuietHours(config: QuietHoursConfig) {
        preferencesDataStore.edit { preferences ->
            preferences[quietHoursEnabledKey] = config.enabled
            preferences[quietHoursStartMinutesKey] = config.startMinutesOfDay.toLong()
            preferences[quietHoursEndMinutesKey] = config.endMinutesOfDay.toLong()
        }
    }

    suspend fun markSyncSuccess(summary: String) {
        syncMetadataStore.markSyncSuccess(preferencesDataStore, summary)
    }

    suspend fun markSyncError(error: String) {
        syncMetadataStore.markSyncError(preferencesDataStore, error)
    }

    suspend fun saveIcalSyncCacheHeaders(headers: IcalSyncCacheHeaders) {
        syncMetadataStore.saveIcalSyncCacheHeaders(preferencesDataStore, headers)
    }

    suspend fun saveSyncDiagnostics(diagnostics: SyncDiagnostics) {
        syncMetadataStore.saveSyncDiagnostics(preferencesDataStore, diagnostics)
    }

    suspend fun saveSyncIntervalMinutes(minutes: Long) {
        preferencesDataStore.edit { preferences ->
            preferences[syncIntervalMinutesKey] = normalizeSyncIntervalMinutes(minutes)
        }
    }

    suspend fun setShowSyncStatusStrip(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[showSyncStatusStripKey] = enabled
        }
    }

    suspend fun setShowTimetableTab(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[showTimetableTabKey] = enabled
        }
    }

    suspend fun setShowAgendaTab(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[showAgendaTabKey] = enabled
        }
    }

    suspend fun setShowExamCollisionBadges(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[showExamCollisionBadgesKey] = enabled
        }
    }

    suspend fun saveCollisionRuleSettings(settings: CollisionRuleSettings) {
        preferencesDataStore.edit { preferences ->
            preferences[collisionIncludeLessonsKey] = settings.includeLessonCollisions
            preferences[collisionIncludeEventsKey] = settings.includeEventCollisions
            preferences[collisionOnlyDifferentSubjectKey] = settings.onlyDifferentSubject
            preferences[collisionRequireExactOverlapKey] = settings.requireExactTimeOverlap
        }
    }

    suspend fun setAccessibilityModeEnabled(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[accessibilityModeEnabledKey] = enabled
        }
    }

    suspend fun setSimpleModeEnabled(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[simpleModeEnabledKey] = enabled
        }
    }

    suspend fun setLastSeenVersion(versionName: String) {
        val normalized = versionName.trim()
        preferencesDataStore.edit { preferences ->
            if (normalized.isBlank()) {
                preferences.remove(lastSeenVersionKey)
            } else {
                preferences[lastSeenVersionKey] = normalized
            }
        }
    }

    suspend fun setShowSetupGuideCard(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[showSetupGuideCardKey] = enabled
        }
    }

    suspend fun setScreenshotProtectionEnabled(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[screenshotProtectionEnabledKey] = enabled
        }
    }

    suspend fun enableAppLockWithPin(pin: String, biometricEnabled: Boolean = false) {
        val normalizedPin = requireValidPin(pin)
        val saltBytes = ByteArray(APP_LOCK_SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val encodedSalt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val encodedHash = encodePinHashV2(normalizedPin, saltBytes)

        preferencesDataStore.edit { preferences ->
            preferences[appLockEnabledKey] = true
            preferences[appLockPinSaltKey] = encodedSalt
            preferences[appLockPinHashKey] = encodedHash
            preferences[appLockBiometricEnabledKey] = biometricEnabled
            preferences[appLockFailedAttemptsKey] = 0L
            preferences.remove(appLockLockUntilMillisKey)
        }
    }

    suspend fun disableAppLock() {
        preferencesDataStore.edit { preferences ->
            preferences[appLockEnabledKey] = false
            preferences.remove(appLockPinSaltKey)
            preferences.remove(appLockPinHashKey)
            preferences.remove(appLockBiometricEnabledKey)
            preferences.remove(appLockFailedAttemptsKey)
            preferences.remove(appLockLockUntilMillisKey)
        }
    }

    suspend fun setAppLockBiometricEnabled(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            if (preferences[appLockEnabledKey] ?: false) {
                preferences[appLockBiometricEnabledKey] = enabled
            } else {
                preferences.remove(appLockBiometricEnabledKey)
            }
        }
    }

    suspend fun verifyAppLockPin(pin: String): Boolean {
        return verifyAppLockPinDetailed(pin).success
    }

    suspend fun verifyAppLockPinDetailed(pin: String): AppLockVerificationResult {
        val normalizedPin = pin.trim()
        if (normalizedPin.isEmpty()) {
            return AppLockVerificationResult(success = false, message = "PIN fehlt.")
        }

        val preferences = preferencesFlow.first()
        if (!(preferences[appLockEnabledKey] ?: false)) {
            return AppLockVerificationResult(success = true)
        }

        val now = System.currentTimeMillis()
        val lockUntil = preferences[appLockLockUntilMillisKey] ?: 0L
        if (lockUntil > now) {
            val remaining = (lockUntil - now).coerceAtLeast(0L)
            return AppLockVerificationResult(
                success = false,
                message = "Zu viele Fehlversuche. Warte ${formatLockoutDuration(remaining)}.",
                lockoutRemainingMillis = remaining
            )
        }

        val encodedSalt = preferences[appLockPinSaltKey].orEmpty()
        val encodedHash = preferences[appLockPinHashKey].orEmpty()
        if (encodedSalt.isBlank() || encodedHash.isBlank()) {
            return AppLockVerificationResult(success = false, message = "App-Schutz ist fehlerhaft konfiguriert.")
        }

        val saltBytes = runCatching {
            Base64.decode(encodedSalt, Base64.NO_WRAP)
        }.getOrNull() ?: return AppLockVerificationResult(success = false, message = "PIN-Prüfung fehlgeschlagen.")

        val isValid = verifyStoredPinHash(
            pin = normalizedPin,
            saltBytes = saltBytes,
            storedHash = encodedHash
        )

        if (isValid) {
            preferencesDataStore.edit { editable ->
                editable[appLockFailedAttemptsKey] = 0L
                editable.remove(appLockLockUntilMillisKey)
                if (!encodedHash.startsWith(PIN_HASH_PREFIX_V2)) {
                    editable[appLockPinHashKey] = encodePinHashV2(normalizedPin, saltBytes)
                }
            }
            return AppLockVerificationResult(success = true)
        }

        val failedAttempts = (preferences[appLockFailedAttemptsKey] ?: 0L) + 1L
        val lockoutDuration = computeLockoutDurationMillis(failedAttempts)
        preferencesDataStore.edit { editable ->
            editable[appLockFailedAttemptsKey] = failedAttempts
            if (lockoutDuration > 0L) {
                editable[appLockLockUntilMillisKey] = now + lockoutDuration
            } else {
                editable.remove(appLockLockUntilMillisKey)
            }
        }

        return if (lockoutDuration > 0L) {
            AppLockVerificationResult(
                success = false,
                message = "Zu viele Fehlversuche. Warte ${formatLockoutDuration(lockoutDuration)}.",
                lockoutRemainingMillis = lockoutDuration
            )
        } else {
            AppLockVerificationResult(success = false, message = "PIN falsch.")
        }
    }

    suspend fun readIcalUrls(): List<String> = iCalUrlsFlow.first()
    suspend fun readIcalUrl(): String? = readIcalUrls().firstOrNull()
    suspend fun readImportEventsEnabled(): Boolean = importEventsEnabledFlow.first()
    suspend fun readSyncIntervalMinutes(): Long = syncIntervalMinutesFlow.first()
    suspend fun readCollisionRuleSettings(): CollisionRuleSettings = collisionRuleSettingsFlow.first()
    suspend fun readAccessibilityModeEnabled(): Boolean = accessibilityModeEnabledFlow.first()
    suspend fun readScreenshotProtectionEnabled(): Boolean = screenshotProtectionEnabledFlow.first()
    suspend fun readIcalSyncCacheHeaders(): IcalSyncCacheHeaders =
        syncMetadataStore.readIcalSyncCacheHeaders(preferencesFlow)

    suspend fun readSnapshot(): List<Exam> = snapshotStore.readSnapshot(preferencesFlow)
    suspend fun readLessonsSnapshot(): List<TimetableLesson> = snapshotStore.readLessonsSnapshot(preferencesFlow)
    suspend fun readEventsSnapshot(): List<SchoolEvent> = snapshotStore.readEventsSnapshot(preferencesFlow)
    suspend fun readTimetableChangesSnapshot(): List<TimetableChangeEntry> =
        snapshotStore.readTimetableChangesSnapshot(preferencesFlow)
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
            screenshotProtectionEnabled = readScreenshotProtectionEnabled(),
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

        preferencesDataStore.edit { preferences ->
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
                secureIcalUrlStore.writeAll(listOf(sanitizedUrl))
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
            preferences[screenshotProtectionEnabledKey] = backup.screenshotProtectionEnabled
            preferences[showSetupGuideCardKey] = backup.showSetupGuideCard
            preferences[onboardingDoneKey] = backup.onboardingDone
            preferences[onboardingPromptSeenKey] = backup.onboardingPromptSeen
            preferences[quietHoursEnabledKey] = sanitizedQuietHours.enabled
            preferences[quietHoursStartMinutesKey] = sanitizedQuietHours.startMinutesOfDay.toLong()
            preferences[quietHoursEndMinutesKey] = sanitizedQuietHours.endMinutesOfDay.toLong()
            preferences[syncIntervalMinutesKey] = normalizeSyncIntervalMinutes(backup.syncIntervalMinutes)
            preferences[showSyncStatusStripKey] = backup.showSyncStatusStrip
            syncMetadataStore.clearSyncMetadata(preferences)
            preferences.remove(lastSeenVersionKey)
        }
        return backup
    }

    suspend fun clearAllLocalData() {
        secureIcalUrlStore.writeAll(emptyList())
        preferencesDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun normalizeSyncIntervalMinutes(value: Long): Long {
        return value.coerceIn(15L, 12L * 60L)
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

    private fun encodePinHashV2(pin: String, saltBytes: ByteArray): String {
        val hash = hashPinV2(pin, saltBytes)
        val encoded = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$PIN_HASH_PREFIX_V2$encoded"
    }

    private fun verifyStoredPinHash(pin: String, saltBytes: ByteArray, storedHash: String): Boolean {
        return if (storedHash.startsWith(PIN_HASH_PREFIX_V2)) {
            val encoded = storedHash.removePrefix(PIN_HASH_PREFIX_V2)
            val storedBytes = runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull() ?: return false
            val candidate = hashPinV2(pin, saltBytes)
            MessageDigest.isEqual(candidate, storedBytes)
        } else {
            val storedBytes = runCatching { Base64.decode(storedHash, Base64.NO_WRAP) }.getOrNull() ?: return false
            val candidate = hashPinLegacy(pin, saltBytes)
            MessageDigest.isEqual(candidate, storedBytes)
        }
    }

    private fun hashPinV2(pin: String, saltBytes: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            saltBytes,
            PIN_HASH_ITERATIONS,
            PIN_HASH_KEY_LENGTH_BITS
        )
        return runCatching {
            SecretKeyFactory.getInstance(PIN_HASH_ALGORITHM).generateSecret(spec).encoded
        }.getOrElse {
            // Fallback for older devices: still deterministic and stronger than plain SHA.
            hashPinLegacy(pin, saltBytes)
        }.also {
            spec.clearPassword()
        }
    }

    private fun hashPinLegacy(pin: String, saltBytes: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltBytes)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }

    private fun computeLockoutDurationMillis(failedAttempts: Long): Long {
        return when {
            failedAttempts < APP_LOCK_LOCKOUT_THRESHOLD -> 0L
            failedAttempts == APP_LOCK_LOCKOUT_THRESHOLD -> 30_000L
            failedAttempts == APP_LOCK_LOCKOUT_THRESHOLD + 1L -> 60_000L
            failedAttempts == APP_LOCK_LOCKOUT_THRESHOLD + 2L -> 120_000L
            else -> 300_000L
        }
    }

    private fun formatLockoutDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1000L).coerceAtLeast(1L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0L) {
            "$minutes Min ${seconds.toString().padStart(2, '0')} Sek"
        } else {
            "$seconds Sek"
        }
    }

    companion object {
        const val DEFAULT_SYNC_INTERVAL_MINUTES: Long = 6L * 60L
        const val MAX_ICAL_URLS: Int = 2
        const val APP_LOCK_PIN_MIN_DIGITS: Int = 4
        const val APP_LOCK_PIN_MAX_DIGITS: Int = 10
        private const val APP_LOCK_LOCKOUT_THRESHOLD: Long = 5L
        private const val APP_LOCK_SALT_BYTES: Int = 16
        private const val PIN_HASH_PREFIX_V2: String = "v2:"
        private const val PIN_HASH_ALGORITHM: String = "PBKDF2WithHmacSHA256"
        private const val PIN_HASH_ITERATIONS: Int = 120_000
        private const val PIN_HASH_KEY_LENGTH_BITS: Int = 256
        private const val MAX_BACKUP_CHARS: Int = 1_000_000
        private const val MAX_BACKUP_EXAMS: Int = 5_000
        private const val MAX_BACKUP_LESSONS: Int = 15_000
        private const val MAX_BACKUP_EVENTS: Int = 8_000
        private const val MAX_BACKUP_TIMETABLE_CHANGES: Int = 500
    }
}

