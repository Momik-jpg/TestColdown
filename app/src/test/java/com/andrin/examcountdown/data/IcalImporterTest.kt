package com.andrin.examcountdown.data

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
}
