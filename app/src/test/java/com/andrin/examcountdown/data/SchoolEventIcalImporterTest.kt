package com.andrin.examcountdown.data

import com.andrin.examcountdown.model.SchoolEventType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
