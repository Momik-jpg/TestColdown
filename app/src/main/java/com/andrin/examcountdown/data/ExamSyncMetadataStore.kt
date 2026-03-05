package com.andrin.examcountdown.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class ExamSyncMetadataStore {
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

    fun syncStatusFlow(preferencesFlow: Flow<Preferences>): Flow<SyncStatus> = preferencesFlow
        .map { preferences ->
            SyncStatus(
                lastSyncAtMillis = preferences[lastSyncAtMillisKey],
                lastSyncSummary = preferences[lastSyncSummaryKey],
                lastSyncError = preferences[lastSyncErrorKey]
            )
        }

    fun syncDiagnosticsFlow(preferencesFlow: Flow<Preferences>): Flow<SyncDiagnostics> = preferencesFlow
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

    suspend fun markSyncSuccess(appContext: Context, summary: String) {
        appContext.dataStore.edit { preferences ->
            preferences[lastSyncAtMillisKey] = System.currentTimeMillis()
            preferences[lastSyncSummaryKey] = summary.trim()
            preferences.remove(lastSyncErrorKey)
        }
    }

    suspend fun markSyncError(appContext: Context, error: String) {
        appContext.dataStore.edit { preferences ->
            preferences[lastSyncErrorKey] = error.trim()
        }
    }

    suspend fun saveIcalSyncCacheHeaders(appContext: Context, headers: IcalSyncCacheHeaders) {
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

    suspend fun saveSyncDiagnostics(appContext: Context, diagnostics: SyncDiagnostics) {
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

    suspend fun readIcalSyncCacheHeaders(preferencesFlow: Flow<Preferences>): IcalSyncCacheHeaders {
        val preferences = preferencesFlow.first()
        return IcalSyncCacheHeaders(
            etag = preferences[iCalEtagKey],
            lastModified = preferences[iCalLastModifiedKey]
        )
    }

    fun clearSyncMetadata(preferences: MutablePreferences) {
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
    }
}
