package com.andrin.examcountdown.data

import com.andrin.examcountdown.model.SchoolEventType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolEventIcalImporterTest {
    private val importer = SchoolEventIcalImporter()
    private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val zone = ZoneId.of("Europe/Zurich")

    @Test
    fun importFromRaw_importsOnlyNonExamAndNonLessonEvents() = runBlocking {
        val eventDate = LocalDate.now(zone).plusDays(3)
        val lessonDate = LocalDate.now(zone).plusDays(2)
        val examDate = LocalDate.now(zone).plusDays(1)

        val raw = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:etP_001@centerboard.ch
            SUMMARY:mat_l24B_HeiCa Mathematik Prüfung 1
            DTSTART;TZID=Europe/Zurich:${examDate.atTime(8, 0).format(dateTimeFormatter)}
            DTEND;TZID=Europe/Zurich:${examDate.atTime(9, 0).format(dateTimeFormatter)}
            END:VEVENT
            BEGIN:VEVENT
            UID:abc123@centerboard.ch
            SUMMARY:frw_l24B_MeiLu
            DTSTART;TZID=Europe/Zurich:${lessonDate.atTime(7, 45).format(dateTimeFormatter)}
            DTEND;TZID=Europe/Zurich:${lessonDate.atTime(8, 30).format(dateTimeFormatter)}
            END:VEVENT
            BEGIN:VEVENT
            UID:ett_900@centerboard.ch
            SUMMARY:Projektwoche
            DESCRIPTION:Schulanlass
            DTSTART;VALUE=DATE:${eventDate.format(dateFormatter)}
            DTEND;VALUE=DATE:${eventDate.plusDays(2).format(dateFormatter)}
            LOCATION:AKSA
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = importer.importFromRaw(raw)

        assertEquals(1, result.events.size)
        assertEquals("Projektwoche", result.events.first().title)
        assertTrue(result.events.first().isAllDay)
        assertEquals(SchoolEventType.SCHOOL, result.events.first().type)
    }

    @Test
    fun importFromRaw_classifiesHolidayEvent() = runBlocking {
        val start = LocalDate.now(zone).plusDays(10)
        val raw = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:holiday-1
            SUMMARY:Frühlingsferien
            DTSTART;VALUE=DATE:${start.format(dateFormatter)}
            DTEND;VALUE=DATE:${start.plusDays(7).format(dateFormatter)}
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = importer.importFromRaw(raw)

        assertEquals(1, result.events.size)
        assertEquals(SchoolEventType.HOLIDAY, result.events.first().type)
    }

    @Test
    fun importFromRaw_usesSchoolTimezoneWhenTzidMissing() = runBlocking {
        val eventDate = LocalDate.now(zone).plusDays(5)
        val raw = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:event-no-tz
            SUMMARY:Infoveranstaltung
            DTSTART:${eventDate.atTime(9, 0).format(dateTimeFormatter)}
            DTEND:${eventDate.atTime(10, 0).format(dateTimeFormatter)}
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = importer.importFromRaw(raw)

        assertEquals(1, result.events.size)
        val expectedStart = eventDate.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, result.events.first().startsAtEpochMillis)
    }

    @Test
    fun importFromRaw_dstTimedEventInZurich_hasCorrectStartAndEnd() = runBlocking {
        val importer = importerAt(LocalDate.of(2026, 3, 1))
        val raw = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:dst-timed
            SUMMARY:DST Schulanlass
            DTSTART;TZID=Europe/Zurich:20260329T003000
            DTEND;TZID=Europe/Zurich:20260329T043000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = importer.importFromRaw(raw)

        assertEquals(1, result.events.size)
        val event = result.events.first()
        val expectedStart = LocalDateTime.of(2026, 3, 29, 0, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val expectedEnd = LocalDateTime.of(2026, 3, 29, 4, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        assertEquals(expectedStart, event.startsAtEpochMillis)
        assertEquals(expectedEnd, event.endsAtEpochMillis)
        assertFalse(event.isAllDay)
    }

    @Test
    fun importFromRaw_dstAllDayEventInZurich_usesLocalDayBoundaryForEnd() = runBlocking {
        val importer = importerAt(LocalDate.of(2026, 3, 1))
        val raw = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:dst-allday
            SUMMARY:Sportferien
            DTSTART;VALUE=DATE:20260328
            DTEND;VALUE=DATE:20260330
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = importer.importFromRaw(raw)

        assertEquals(1, result.events.size)
        val event = result.events.first()
        val expectedStart = LocalDate.of(2026, 3, 28)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 3, 30)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        assertTrue(event.isAllDay)
        assertEquals(expectedStart, event.startsAtEpochMillis)
        assertEquals(expectedEnd, event.endsAtEpochMillis)
    }

    private fun importerAt(date: LocalDate): SchoolEventIcalImporter {
        val instant = date.atStartOfDay(zone).toInstant()
        return SchoolEventIcalImporter(clock = Clock.fixed(instant, zone))
    }
}
