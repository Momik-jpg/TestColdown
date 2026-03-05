package com.andrin.examcountdown.domain.usecase

import com.andrin.examcountdown.model.TimetableChangeType
import com.andrin.examcountdown.model.TimetableLesson
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeTimetableChangesUseCaseTest {
    private val zone = ZoneId.of("Europe/Zurich")

    @Test
    fun returnsFirstSyncWhenNoPreviousLessons() {
        val result = ComputeTimetableChangesUseCase().invoke(
            oldLessons = emptyList(),
            newLessons = listOf(lesson("L1", 2026, 3, 2, 7, 30, 8, 15))
        )

        assertTrue(result.isFirstSync)
        assertEquals(0, result.total)
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun classifiesMovedRoomChangedAndAddedLessons() {
        val oldLessons = listOf(
            lesson("A", 2026, 3, 2, 7, 30, 8, 15, location = "B411"),
            lesson("B", 2026, 3, 2, 9, 0, 9, 45, location = "B311")
        )
        val newLessons = listOf(
            lesson("A", 2026, 3, 2, 7, 35, 8, 20, location = "B411", isMoved = true),
            lesson("B", 2026, 3, 2, 9, 0, 9, 45, location = "B312", isLocationChanged = true),
            lesson("C", 2026, 3, 2, 9, 30, 9, 40, location = "A102")
        )

        val result = ComputeTimetableChangesUseCase().invoke(oldLessons, newLessons)

        assertFalse(result.isFirstSync)
        assertEquals(3, result.total)
        assertEquals(1, result.movedCount)
        assertEquals(1, result.roomChangedCount)
        assertTrue(result.entries.any { it.changeType == TimetableChangeType.MOVED && it.lessonId == "A" })
        assertTrue(result.entries.any { it.changeType == TimetableChangeType.ROOM_CHANGED && it.lessonId == "B" })
        assertTrue(result.entries.any { it.changeType == TimetableChangeType.ADDED && it.lessonId == "C" })
    }

    @Test
    fun ignoresRemovedLessonsOutsideOverlapWindow() {
        val oldLessons = listOf(
            lesson("D1", 2026, 3, 2, 7, 30, 8, 15),
            lesson("D2", 2026, 3, 10, 7, 30, 8, 15)
        )
        val newLessons = listOf(
            lesson("D1", 2026, 3, 2, 7, 30, 8, 15)
        )

        val result = ComputeTimetableChangesUseCase().invoke(oldLessons, newLessons)

        assertEquals(0, result.total)
        assertTrue(result.entries.isEmpty())
    }

    private fun lesson(
        id: String,
        year: Int,
        month: Int,
        day: Int,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        location: String? = null,
        isMoved: Boolean = false,
        isLocationChanged: Boolean = false
    ): TimetableLesson {
        return TimetableLesson(
            id = id,
            title = "Lesson $id",
            location = location,
            startsAtEpochMillis = LocalDate.of(year, month, day)
                .atTime(startHour, startMinute)
                .atZone(zone)
                .toInstant()
                .toEpochMilli(),
            endsAtEpochMillis = LocalDate.of(year, month, day)
                .atTime(endHour, endMinute)
                .atZone(zone)
                .toInstant()
                .toEpochMilli(),
            isMoved = isMoved,
            isLocationChanged = isLocationChanged
        )
    }
}
