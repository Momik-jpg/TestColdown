package com.andrin.examcountdown.data

import android.content.Context
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableChangeType
import com.andrin.examcountdown.reminder.TimetableSyncNotificationManager
import com.andrin.examcountdown.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IcalSyncResult(
    val examsImported: Int,
    val lessonsImported: Int,
    val eventsImported: Int,
    val changedLessons: Int,
    val movedLessons: Int,
    val roomChangedLessons: Int
) {
    fun summaryText(): String {
        return buildString {
            append("$examsImported Prüfungen und $lessonsImported Lektionen synchronisiert.")
            if (eventsImported > 0) {
                append(" $eventsImported Events synchronisiert.")
            }
            if (changedLessons > 0) {
                append(" $changedLessons Änderung")
                if (changedLessons != 1) append("en")
                append(" erkannt.")
            }
        }
    }
}

class IcalSyncEngine(
    private val context: Context
) {
    private val appContext = context.applicationContext
    private val repository = ExamRepository(appContext)
    private val examImporter = IcalImporter()
    private val timetableImporter = TimetableIcalImporter()
    private val eventImporter = SchoolEventIcalImporter()

    suspend fun syncFromUrl(
        url: String,
        emitChangeNotification: Boolean,
        importEvents: Boolean? = null
    ): IcalSyncResult {
        val normalizedUrl = normalizeAndValidateUrl(url)
        val raw = withContext(Dispatchers.IO) { IcalHttpClient.download(normalizedUrl) }
        val previousLessons = repository.readLessonsSnapshot()
        val shouldImportEvents = importEvents ?: repository.readImportEventsEnabled()

        val examResult = examImporter.importFromRaw(raw)
        val timetableResult = timetableImporter.importFromRaw(raw)
        val eventsResult = if (shouldImportEvents) {
            eventImporter.importFromRaw(raw)
        } else {
            SchoolEventImportResult(events = emptyList(), message = "Event-Import deaktiviert.")
        }

        repository.replaceIcalSyncSnapshot(
            importedExams = examResult.exams,
            importedLessons = timetableResult.lessons,
            importedEvents = eventsResult.events
        )
        WidgetUpdater.updateAll(appContext)

        val changes = detectLessonChanges(previousLessons, timetableResult.lessons)
        val result = IcalSyncResult(
            examsImported = examResult.exams.size,
            lessonsImported = timetableResult.lessons.size,
            eventsImported = eventsResult.events.size,
            changedLessons = changes.total,
            movedLessons = changes.movedCount,
            roomChangedLessons = changes.roomChangedCount
        )

        repository.markSyncSuccess(result.summaryText())
        if (!changes.isFirstSync && changes.entries.isNotEmpty()) {
            repository.appendTimetableChanges(changes.entries)
        }

        if (emitChangeNotification && !changes.isFirstSync && changes.total > 0) {
            TimetableSyncNotificationManager.showChangedLessons(
                context = appContext,
                changedCount = changes.total,
                movedCount = changes.movedCount,
                roomChangedCount = changes.roomChangedCount
            )
        }

        return result
    }

    suspend fun testConnection(url: String, importEvents: Boolean? = null): IcalSyncResult {
        val normalizedUrl = normalizeAndValidateUrl(url)
        val raw = withContext(Dispatchers.IO) { IcalHttpClient.download(normalizedUrl) }
        val shouldImportEvents = importEvents ?: repository.readImportEventsEnabled()
        val examResult = examImporter.importFromRaw(raw)
        val timetableResult = timetableImporter.importFromRaw(raw)
        val eventsResult = if (shouldImportEvents) {
            eventImporter.importFromRaw(raw)
        } else {
            SchoolEventImportResult(events = emptyList(), message = "Event-Import deaktiviert.")
        }

        return IcalSyncResult(
            examsImported = examResult.exams.size,
            lessonsImported = timetableResult.lessons.size,
            eventsImported = eventsResult.events.size,
            changedLessons = 0,
            movedLessons = 0,
            roomChangedLessons = 0
        )
    }

    private data class LessonChangeCounters(
        val total: Int,
        val movedCount: Int,
        val roomChangedCount: Int,
        val isFirstSync: Boolean,
        val entries: List<TimetableChangeEntry>
    )

    private fun detectLessonChanges(
        oldLessons: List<TimetableLesson>,
        newLessons: List<TimetableLesson>
    ): LessonChangeCounters {
        if (oldLessons.isEmpty()) {
            return LessonChangeCounters(
                total = 0,
                movedCount = 0,
                roomChangedCount = 0,
                isFirstSync = true,
                entries = emptyList()
            )
        }

        val oldById = oldLessons.associateBy { it.id }
        val newById = newLessons.associateBy { it.id }

        val changedLessonIds = mutableSetOf<String>()
        var moved = 0
        var roomChanged = 0
        val entries = mutableListOf<TimetableChangeEntry>()

        oldById.forEach { (id, oldLesson) ->
            if (id in newById) return@forEach
            changedLessonIds += id
            entries += TimetableChangeEntry(
                lessonId = id,
                title = oldLesson.title,
                startsAtEpochMillis = oldLesson.startsAtEpochMillis,
                changeType = TimetableChangeType.REMOVED
            )
        }

        newById.forEach { (id, newLesson) ->
            val oldLesson = oldById[id]
            if (oldLesson == null) {
                changedLessonIds += id
                if (newLesson.isMoved) moved += 1
                if (newLesson.isLocationChanged) roomChanged += 1
                entries += TimetableChangeEntry(
                    lessonId = id,
                    title = newLesson.title,
                    startsAtEpochMillis = newLesson.startsAtEpochMillis,
                    changeType = TimetableChangeType.ADDED
                )
                return@forEach
            }

            val timeChanged = oldLesson.startsAtEpochMillis != newLesson.startsAtEpochMillis ||
                oldLesson.endsAtEpochMillis != newLesson.endsAtEpochMillis
            val locationChanged = normalizeLocation(oldLesson.location) != normalizeLocation(newLesson.location)
            val movedChanged = oldLesson.isMoved != newLesson.isMoved
            val roomChangedFlagChanged = oldLesson.isLocationChanged != newLesson.isLocationChanged

            if (timeChanged || locationChanged || movedChanged || roomChangedFlagChanged) {
                changedLessonIds += id
            }
            if (newLesson.isMoved && (timeChanged || movedChanged)) {
                moved += 1
                entries += TimetableChangeEntry(
                    lessonId = id,
                    title = newLesson.title,
                    startsAtEpochMillis = newLesson.startsAtEpochMillis,
                    changeType = TimetableChangeType.MOVED,
                    oldValue = "${oldLesson.startsAtEpochMillis}",
                    newValue = "${newLesson.startsAtEpochMillis}"
                )
            } else if (timeChanged) {
                entries += TimetableChangeEntry(
                    lessonId = id,
                    title = newLesson.title,
                    startsAtEpochMillis = newLesson.startsAtEpochMillis,
                    changeType = TimetableChangeType.TIME_CHANGED,
                    oldValue = "${oldLesson.startsAtEpochMillis}",
                    newValue = "${newLesson.startsAtEpochMillis}"
                )
            }
            if (newLesson.isLocationChanged && (locationChanged || roomChangedFlagChanged)) {
                roomChanged += 1
                entries += TimetableChangeEntry(
                    lessonId = id,
                    title = newLesson.title,
                    startsAtEpochMillis = newLesson.startsAtEpochMillis,
                    changeType = TimetableChangeType.ROOM_CHANGED,
                    oldValue = oldLesson.location,
                    newValue = newLesson.location
                )
            }
        }

        return LessonChangeCounters(
            total = changedLessonIds.size,
            movedCount = moved,
            roomChangedCount = roomChanged,
            isFirstSync = false,
            entries = entries
                .sortedByDescending { it.changedAtEpochMillis }
                .take(40)
        )
    }

    private fun normalizeLocation(value: String?): String {
        return value.orEmpty()
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), "")
    }

    private fun normalizeAndValidateUrl(url: String): String {
        val normalizedUrl = url.trim()
        require(normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
            "Ungültige URL"
        }
        return normalizedUrl
    }
}
