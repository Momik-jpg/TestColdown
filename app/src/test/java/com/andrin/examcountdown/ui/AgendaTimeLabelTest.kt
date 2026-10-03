package com.andrin.examcountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class AgendaTimeLabelTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private fun millis(time: String) = LocalDateTime.parse(time).atZone(zone).toInstant().toEpochMilli()

    @Test fun sameDayRangeShowsTheStartTimeOnlyOnce() {
        assertEquals("03.10. · 08:00 – 09:00", formatAgendaTimeLabel(
            millis("2026-10-03T08:00"), millis("2026-10-03T09:00"), false, zone
        ))
    }

    @Test fun overnightRangeRetainsBothDatesInTheSchoolTimezone() {
        assertEquals("03.10. · 23:30 – 04.10. · 01:00", formatAgendaTimeLabel(
            millis("2026-10-03T23:30"), millis("2026-10-04T01:00"), false, zone
        ))
    }

    @Test fun singleTimeAndAllDayLabelsAvoidAnInventedEndOrMidnightTime() {
        assertEquals("03.10. · 08:00", formatAgendaTimeLabel(millis("2026-10-03T08:00"), null, false, zone))
        assertEquals("03.10. · Ganztägig", formatAgendaTimeLabel(
            millis("2026-10-03T00:00"), millis("2026-10-04T00:00"), true, zone
        ))
        assertEquals("03.10. – 05.10. · Ganztägig", formatAgendaTimeLabel(
            millis("2026-10-03T00:00"), millis("2026-10-06T00:00"), true, zone
        ))
    }
}
