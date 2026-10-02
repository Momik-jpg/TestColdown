package com.andrin.examcountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GradeNumberPolicyTest {
    @Test fun acceptsGermanAndDecimalPointInput() {
        assertEquals(5.25, parseGradeNumber(" 5,25 ")!!, 0.0)
        assertEquals(5.25, parseGradeNumber("5.25")!!, 0.0)
    }

    @Test fun rejectsInvalidAndNonFiniteNumbers() {
        listOf("", "fünf", "NaN", "Infinity", "-Infinity", "1e999").forEach {
            assertNull(parseGradeNumber(it))
        }
    }
}
