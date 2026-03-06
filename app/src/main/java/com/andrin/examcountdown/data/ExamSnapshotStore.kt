package com.andrin.examcountdown.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableLesson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class ExamSnapshotStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    private val examsKey = stringPreferencesKey("exams_json")
    private val lessonsKey = stringPreferencesKey("lessons_json")
    private val eventsKey = stringPreferencesKey("events_json")
    private val timetableChangesKey = stringPreferencesKey("timetable_changes_json")

    fun examsFlow(preferencesFlow: Flow<Preferences>): Flow<List<Exam>> = preferencesFlow
        .map { preferences ->
            decodeExams(preferences[examsKey])
                .sortedBy { it.startsAtEpochMillis }
        }

    fun lessonsFlow(preferencesFlow: Flow<Preferences>): Flow<List<TimetableLesson>> = preferencesFlow
        .map { preferences ->
            decodeLessons(preferences[lessonsKey])
                .sortedBy { it.startsAtEpochMillis }
        }

    fun eventsFlow(preferencesFlow: Flow<Preferences>): Flow<List<SchoolEvent>> = preferencesFlow
        .map { preferences ->
            decodeEvents(preferences[eventsKey])
                .sortedBy { it.startsAtEpochMillis }
        }

    fun timetableChangesFlow(preferencesFlow: Flow<Preferences>): Flow<List<TimetableChangeEntry>> =
        preferencesFlow.map { preferences ->
            decodeTimetableChanges(preferences[timetableChangesKey])
                .sortedByDescending { it.changedAtEpochMillis }
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
        dataStore.edit { preferences ->
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
            val mergedExamsJson = json.encodeToString(mergedExams)
            val mergedLessonsJson = json.encodeToString(mergedLessons)
            val mergedEventsJson = json.encodeToString(mergedEvents)

            if (preferences[examsKey] != mergedExamsJson) {
                preferences[examsKey] = mergedExamsJson
            }
            if (preferences[lessonsKey] != mergedLessonsJson) {
                preferences[lessonsKey] = mergedLessonsJson
            }
            if (preferences[eventsKey] != mergedEventsJson) {
                preferences[eventsKey] = mergedEventsJson
            }
        }
    }

    suspend fun replaceSyncedLessons(imported: List<TimetableLesson>) {
        dataStore.edit { preferences ->
            val updated = imported.sortedBy { it.startsAtEpochMillis }
            val updatedJson = json.encodeToString(updated)
            if (preferences[lessonsKey] != updatedJson) {
                preferences[lessonsKey] = updatedJson
            }
        }
    }

    suspend fun replaceSyncedEvents(imported: List<SchoolEvent>) {
        dataStore.edit { preferences ->
            val current = decodeEvents(preferences[eventsKey])
            val manualEvents = current.filterNot { isSyncedCalendarEventId(it.id) }
            val updated = imported
                .plus(manualEvents)
                .distinctBy { it.id }
                .sortedBy { it.startsAtEpochMillis }
            val updatedJson = json.encodeToString(updated)
            if (preferences[eventsKey] != updatedJson) {
                preferences[eventsKey] = updatedJson
            }
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

    suspend fun updateEvent(event: SchoolEvent) {
        updateEvents { current ->
            var found = false
            val updated = current.map { existing ->
                if (existing.id == event.id) {
                    found = true
                    event
                } else {
                    existing
                }
            }
            val merged = if (found) updated else (updated + event)
            merged.sortedBy { it.startsAtEpochMillis }
        }
    }

    suspend fun appendTimetableChanges(
        changes: List<TimetableChangeEntry>,
        maxEntries: Int = 120
    ) {
        if (changes.isEmpty()) return
        dataStore.edit { preferences ->
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
        dataStore.edit { preferences ->
            preferences.remove(timetableChangesKey)
        }
    }

    suspend fun deleteExam(examId: String) {
        updateExams { current ->
            current.filterNot { it.id == examId }
        }
    }

    suspend fun readSnapshot(preferencesFlow: Flow<Preferences>): List<Exam> = examsFlow(preferencesFlow).first()
    suspend fun readLessonsSnapshot(preferencesFlow: Flow<Preferences>): List<TimetableLesson> = lessonsFlow(preferencesFlow).first()
    suspend fun readEventsSnapshot(preferencesFlow: Flow<Preferences>): List<SchoolEvent> = eventsFlow(preferencesFlow).first()
    suspend fun readTimetableChangesSnapshot(preferencesFlow: Flow<Preferences>): List<TimetableChangeEntry> =
        timetableChangesFlow(preferencesFlow).first()

    private suspend fun updateExams(transform: (List<Exam>) -> List<Exam>) {
        dataStore.edit { preferences ->
            val updated = transform(decodeExams(preferences[examsKey]))
            val updatedJson = json.encodeToString(updated)
            if (preferences[examsKey] != updatedJson) {
                preferences[examsKey] = updatedJson
            }
        }
    }

    private suspend fun updateEvents(transform: (List<SchoolEvent>) -> List<SchoolEvent>) {
        dataStore.edit { preferences ->
            val updated = transform(decodeEvents(preferences[eventsKey]))
            val updatedJson = json.encodeToString(updated)
            if (preferences[eventsKey] != updatedJson) {
                preferences[eventsKey] = updatedJson
            }
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

    private fun isSyncedCalendarEventId(id: String): Boolean {
        return id.startsWith("ical-event:") || id.startsWith("ical:")
    }
}
