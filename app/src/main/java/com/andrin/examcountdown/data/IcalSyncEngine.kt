package com.andrin.examcountdown.data

import android.content.Context
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.reminder.TimetableSyncNotificationManager
import com.andrin.examcountdown.widget.WidgetUpdater

data class IcalSyncResult(
    val examsImported: Int,
    val lessonsImported: Int,
    val changedLessons: Int,
    val movedLessons: Int,
    val roomChangedLessons: Int
) {
    fun summaryText(): String {
        return buildString {
            append("$examsImported Prüfungen und $lessonsImported Lektionen synchronisiert.")
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

    suspend fun syncFromUrl(
        url: String,
        emitChangeNotification: Boolean
    ): IcalSyncResult {
        val previousLessons = repository.readLessonsSnapshot()

        val examResult = examImporter.importFromUrl(url)
        val timetableResult = timetableImporter.importFromUrl(url)

        repository.replaceIcalImportedExams(examResult.exams)
        repository.replaceSyncedLessons(timetableResult.lessons)
        WidgetUpdater.updateAll(appContext)

        val changes = detectLessonChanges(previousLessons, timetableResult.lessons)
        val result = IcalSyncResult(
            examsImported = examResult.exams.size,
            lessonsImported = timetableResult.lessons.size,
            changedLessons = changes.total,
            movedLessons = changes.movedCount,
            roomChangedLessons = changes.roomChangedCount
        )

        repository.markSyncSuccess(result.summaryText())

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

    suspend fun testConnection(url: String): IcalSyncResult {
        val examResult = examImporter.importFromUrl(url)
        val timetableResult = timetableImporter.importFromUrl(url)

        return IcalSyncResult(
            examsImported = examResult.exams.size,
            lessonsImported = timetableResult.lessons.size,
            changedLessons = 0,
            movedLessons = 0,
            roomChangedLessons = 0
        )
    }

    private data class LessonChangeCounters(
        val total: Int,
        val movedCount: Int,
        val roomChangedCount: Int,
        val isFirstSync: Boolean
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
                isFirstSync = true
            )
        }

        val oldById = oldLessons.associateBy { it.id }
        val newById = newLessons.associateBy { it.id }

        var changed = 0
        var moved = 0
        var roomChanged = 0

        val removedCount = oldById.keys.count { it !in newById }
        changed += removedCount

        newById.forEach { (id, newLesson) ->
            val oldLesson = oldById[id]
            if (oldLesson == null) {
                changed += 1
                if (newLesson.isMoved) moved += 1
                if (newLesson.isLocationChanged) roomChanged += 1
                return@forEach
            }

            val timeChanged = oldLesson.startsAtEpochMillis != newLesson.startsAtEpochMillis ||
                oldLesson.endsAtEpochMillis != newLesson.endsAtEpochMillis
            val locationChanged = normalizeLocation(oldLesson.location) != normalizeLocation(newLesson.location)
            val movedChanged = oldLesson.isMoved != newLesson.isMoved
            val roomChangedFlagChanged = oldLesson.isLocationChanged != newLesson.isLocationChanged

            if (timeChanged || locationChanged || movedChanged || roomChangedFlagChanged) {
                changed += 1
            }
            if (newLesson.isMoved && (timeChanged || movedChanged)) {
                moved += 1
            }
            if (newLesson.isLocationChanged && (locationChanged || roomChangedFlagChanged)) {
                roomChanged += 1
            }
        }

        return LessonChangeCounters(
            total = changed,
            movedCount = moved,
            roomChangedCount = roomChanged,
            isFirstSync = false
        )
    }

    private fun normalizeLocation(value: String?): String {
        return value.orEmpty()
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), "")
    }
}
