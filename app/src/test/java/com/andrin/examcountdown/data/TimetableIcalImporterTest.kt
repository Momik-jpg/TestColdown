package com.andrin.examcountdown.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TimetableIcalImporterTest {
    private val importer = TimetableIcalImporter()
    private val zone = ZoneId.of("Europe/Zurich")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    @Test
    fun importFromRaw_usesSchoolTimezoneWhenTzidMissing() = runBlocking {
        val lessonDate = LocalDate.now(zone).plusDays(2)
        val raw = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:lesson-abc@centerboard.ch
            SUMMARY:mat_l24B_HeiCa
            DTSTART:${lessonDate.atTime(7, 45).format(dateTimeFormatter)}
            DTEND:${lessonDate.atTime(8, 30).format(dateTimeFormatter)}
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = importer.importFromRaw(raw)

        assertEquals(1, result.lessons.size)
        val expectedStart = lessonDate.atTime(7, 45).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, result.lessons.first().startsAtEpochMillis)
    }
}
