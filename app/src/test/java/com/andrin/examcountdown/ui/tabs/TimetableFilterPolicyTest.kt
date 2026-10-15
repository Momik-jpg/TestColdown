package com.andrin.examcountdown.ui.tabs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimetableFilterPolicyTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private fun lesson(id: String, title: String, movedRoom: Boolean = false) = TimetableLessonBlock(
        id, title, if (movedRoom) "204" else "101", if (movedRoom) "102" else null,
        1_800_000_000_000L, 1_800_003_600_000L, false, movedRoom, false, 1
    )

    @Test fun searchMatchesDisplayedTitlesAndBothRoomsAndCombinesWithStatus() {
        val exam = lesson("exam", "Pruefung Mathematik", movedRoom = true)
        val normal = lesson("normal", "Mathematik")
        val lessons = listOf(exam, normal)
        listOf(" PRÜFUNG ", "204", "102").forEach { query ->
            assertEquals(listOf(exam), filterTimetableBlocks(lessons, TimetableFilter.ALL, zone, query))
        }
        assertEquals(listOf(exam), filterTimetableBlocks(lessons, TimetableFilter.ONLY_ROOM_CHANGED, zone, "mathematik"))
        assertEquals(emptyList<TimetableLessonBlock>(), filterTimetableBlocks(lessons, TimetableFilter.ONLY_MOVED, zone, "204"))
    }

    @Test fun weekendLessonsAreIncludedInTheWeekGrid() {
        val monday = LocalDate.of(2026, 9, 28)
        val weekend = monday.plusDays(5)
        assertEquals(monday.plusDays(6), timetableWeekDates(monday, mapOf(weekend to listOf(lesson("sat", "Kurs")))).last())
        assertEquals(monday.plusDays(4), timetableWeekDates(monday, mapOf(monday to listOf(lesson("mon", "Kurs")))).last())
    }
}
