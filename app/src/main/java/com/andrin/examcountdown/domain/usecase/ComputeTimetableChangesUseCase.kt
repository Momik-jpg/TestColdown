package com.andrin.examcountdown.domain.usecase

import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableChangeType
import com.andrin.examcountdown.model.TimetableLesson

/**
 * Compares old/new timetable snapshots and classifies changes (moved, room changed, added, removed, time changed).
 * The result is deterministic for equal inputs and can be tested independently from sync/network code.
 */
class ComputeTimetableChangesUseCase {
    data class Result(
        val total: Int,
        val movedCount: Int,
        val roomChangedCount: Int,
        val isFirstSync: Boolean,
        val entries: List<TimetableChangeEntry>
    )

    operator fun invoke(
        oldLessons: List<TimetableLesson>,
        newLessons: List<TimetableLesson>
    ): Result {
        if (oldLessons.isEmpty()) {
            return Result(
                total = 0,
                movedCount = 0,
                roomChangedCount = 0,
                isFirstSync = true,
                entries = emptyList()
            )
        }

        val oldWindowStart = oldLessons.minOfOrNull { it.startsAtEpochMillis } ?: Long.MIN_VALUE
        val oldWindowEnd = oldLessons.maxOfOrNull { it.endsAtEpochMillis } ?: Long.MAX_VALUE
        val newWindowStart = newLessons.minOfOrNull { it.startsAtEpochMillis } ?: Long.MIN_VALUE
        val newWindowEnd = newLessons.maxOfOrNull { it.endsAtEpochMillis } ?: Long.MAX_VALUE
        val overlapStart = maxOf(oldWindowStart, newWindowStart)
        val overlapEnd = minOf(oldWindowEnd, newWindowEnd)
        val hasOverlap = overlapStart <= overlapEnd

        fun isInsideOverlap(startMillis: Long, endMillis: Long): Boolean {
            if (!hasOverlap) return false
            return endMillis >= overlapStart && startMillis <= overlapEnd
        }

        val oldById = oldLessons.associateBy { it.id }
        val newById = newLessons.associateBy { it.id }

        val changedLessonIds = mutableSetOf<String>()
        var moved = 0
        var roomChanged = 0
        val entries = mutableListOf<TimetableChangeEntry>()

        oldById.forEach { (id, oldLesson) ->
            if (id in newById) return@forEach
            if (!isInsideOverlap(oldLesson.startsAtEpochMillis, oldLesson.endsAtEpochMillis)) {
                return@forEach
            }
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
                if (!isInsideOverlap(newLesson.startsAtEpochMillis, newLesson.endsAtEpochMillis)) {
                    return@forEach
                }
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

        return Result(
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
}
