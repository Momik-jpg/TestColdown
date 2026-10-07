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

    @Test fun schoolGradesRespectBothScaleBoundaries() {
        assertEquals(1.0, parseSchoolGrade("1")!!, 0.0)
        assertEquals(6.0, parseSchoolGrade("6,0")!!, 0.0)
        listOf("0", "6.01", "-2", "fünf", "1e308").forEach { assertNull(parseSchoolGrade(it)) }
    }

    @Test fun weightsArePositiveAndFinite() {
        assertEquals(0.5, parsePositiveGradeNumber("0,5")!!, 0.0)
        listOf("0", "-1", "", "Infinity").forEach { assertNull(parsePositiveGradeNumber(it)) }
    }

    @Test fun largeWeightsCannotTurnAValidAverageIntoNaN() {
        assertEquals(4.0, weightedGradeAverage(listOf(2.0 to 1e308, 6.0 to 1e308))!!, 0.0)
        assertEquals(5.0, weightedGradeAverage(listOf(2.0 to 1.0, 6.0 to 3.0))!!, 0.0)
        assertNull(weightedGradeAverage(emptyList()))
    }
}
