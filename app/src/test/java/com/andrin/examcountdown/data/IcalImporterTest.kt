package com.andrin.examcountdown.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IcalImporterTest {
    private val importer = IcalImporter()

    @Test
    fun parseExamSummary_extractsSubjectFromSchulnetzPattern() {
        val parsed = importer.parseExamSummaryForDisplay("egsp_l24B_SutPe Staatskunde")

        assertEquals("EGSP", parsed.subject)
        assertEquals("Staatskunde", parsed.title)
    }

    @Test
    fun parseExamSummary_extractsSubjectFromColonPattern() {
        val parsed = importer.parseExamSummaryForDisplay("Mathematik: Prüfung 2")

        assertEquals("Mathematik", parsed.subject)
        assertEquals("Prüfung 2", parsed.title)
    }

    @Test
    fun parseExamSummary_handlesBlankInput() {
        val parsed = importer.parseExamSummaryForDisplay("   ")

        assertNull(parsed.subject)
        assertEquals("Prüfung", parsed.title)
    }

    @Test
    fun importFromRaw_usesSchoolTimezoneWhenTzidMissing() = runBlocking {
        val zone = ZoneId.of("Europe/Zurich")
        val date = LocalDate.now(zone).plusDays(2)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        val raw = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:test-1
            SUMMARY:Mathematik Prüfung
            DTSTART:${date.atTime(8, 0).format(formatter)}
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = importer.importFromRaw(raw)

        assertEquals(1, result.exams.size)
        val expectedMillis = date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedMillis, result.exams.first().startsAtEpochMillis)
    }
}
