package com.andrin.examcountdown.widget

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WidgetPolicyTest {
    private val now = Instant.parse("2026-10-05T06:00:00Z").toEpochMilli()
    private fun item(id: String, minutes: Long, kind: WidgetItemKind = WidgetItemKind.EXAM, duration: Long = 0) =
        WidgetTimelineItem(id, id, now + minutes * 60_000, now + (minutes + duration) * 60_000, kind)

    @Test fun nextIsChronologicalEvenWhenListGroupsByType() {
        val items = listOf(item("exam", 120), item("lesson", 20, WidgetItemKind.LESSON, 45))
        val config = WidgetConfig(mode = WidgetMode.AGENDA, sortMode = WidgetSortMode.TYPE_THEN_TIME)
        assertEquals("exam", selectWidgetItems(items, config, now, 2).first().id)
        assertEquals("lesson", selectWidgetItems(items, config, now, 1, nextOnly = true).single().id)
    }

    @Test fun nextIncludesOngoingLessonsButSkipsCancelledSlots() {
        val items = listOf(item("cancelled", -20, WidgetItemKind.LESSON, 60).copy(isCancelled = true),
            item("current", -10, WidgetItemKind.LESSON, 45), item("later", 10))
        assertEquals("current", selectWidgetItems(items, WidgetConfig(mode = WidgetMode.AGENDA), now, 1, true).single().id)
        assertEquals(3, selectWidgetItems(items, WidgetConfig(mode = WidgetMode.AGENDA), now, 3).size)
    }

    @Test fun sourcesWindowAndEndedEntriesAreFilteredBeforeLimiting() {
        val items = listOf(item("ended", -60, WidgetItemKind.LESSON, 30), item("lesson", 1, WidgetItemKind.LESSON, 30),
            item("exam", 10), item("far", 8 * 1440))
        assertEquals(listOf("exam"), selectWidgetItems(items, WidgetConfig(windowDays = 7), now, 10).map { it.id })
        assertEquals(listOf("lesson", "exam"), selectWidgetItems(items, WidgetConfig(WidgetMode.AGENDA, 7), now, 10).map { it.id })
        assertEquals(2, selectWidgetItems(items, WidgetConfig(windowDays = WIDGET_WINDOW_DAYS_ALL), now, 10).size)
        assertTrue(selectWidgetItems(items, WidgetConfig(), now, 0).isEmpty())
    }

    @Test fun runningLessonsAndAllDayEventsHaveSuitableStatuses() {
        assertEquals("Jetzt", widgetStatus(item("lesson", -5, WidgetItemKind.LESSON, 30), now))
        assertEquals("Ganztägig", widgetStatus(item("holiday", -60, WidgetItemKind.EVENT, 1440).copy(isAllDay = true), now))
        assertEquals("Entfällt", widgetStatus(item("cancelled", 30).copy(isCancelled = true), now))
        assertEquals("in 1 Tag", widgetStatus(item("tomorrow", 1440), now))
    }

    @Test fun locationPreferenceAndAllDayDateDoNotLeakMidnight() {
        val event = item("event", 0, WidgetItemKind.EVENT, 1440).copy(isAllDay = true, location = "Aula")
        assertTrue(widgetDetails(event, WidgetConfig()).contains("Aula"))
        assertFalse(widgetDetails(event, WidgetConfig(showLocation = false)).contains("Aula"))
        assertFalse(widgetDetails(event, WidgetConfig()).contains("08:00"))
        assertTrue(widgetDetails(event, WidgetConfig()).contains("Ganztägig"))
    }

    @Test fun rowCapacityFitsAvailableHeightAndIncreasesWithResize() {
        assertEquals(1, widgetRowLimit(220, false))
        assertTrue(widgetRowLimit(440, false) > widgetRowLimit(220, false))
        assertTrue(widgetRowLimit(440, true) > widgetRowLimit(440, false))
        assertTrue(widgetRowLimit(220, false, 1.6f) <= widgetRowLimit(220, false))
        assertEquals(0, widgetRowLimit(100, false))
        assertEquals(10, widgetRowLimit(2000, false))
    }

    @Test fun prominentCountdownUsesDaysHoursMinutesAndHonestRunningState() {
        assertEquals(WidgetCountdown("1", "TAG"), widgetCountdown(item("tomorrow", 1440), now))
        assertEquals(WidgetCountdown("2", "TAGE"), widgetCountdown(item("later", 2880), now))
        assertEquals(WidgetCountdown("2", "STD"), widgetCountdown(item("soon", 120), now))
        assertEquals(WidgetCountdown("35", "MIN"), widgetCountdown(item("soon", 35), now))
        assertEquals(WidgetCountdown("Jetzt", ""), widgetCountdown(item("lesson", -5, WidgetItemKind.LESSON, 45), now))
        assertEquals(WidgetCountdown("Heute", ""), widgetCountdown(item("day", -5, WidgetItemKind.EVENT, 1440).copy(isAllDay = true), now))
    }

    @Test fun dateRailDetailsRetainLessonEndAndOvernightDateWithoutRedundantStartDate() {
        val lesson = item("lesson", 0, WidgetItemKind.LESSON, 45).copy(location = "204")
        assertEquals("08:00–08:45 · 204", widgetTimeDetails(lesson, WidgetConfig()))
        assertEquals("08:00–08:45", widgetTimeDetails(lesson, WidgetConfig(showLocation = false)))
        val overnight = lesson.copy(startsAtEpochMillis = Instant.parse("2026-10-05T21:00:00Z").toEpochMilli(),
            endsAtEpochMillis = Instant.parse("2026-10-06T01:00:00Z").toEpochMilli())
        assertTrue(widgetTimeDetails(overnight, WidgetConfig()).contains("06.10"))
        assertEquals("Ganztägig · 204", widgetTimeDetails(lesson.copy(isAllDay = true), WidgetConfig()))
    }
}
