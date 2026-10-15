package com.andrin.examcountdown.ui

/** Accept decimal commas as well as dots; non-finite values cannot be a grade or weight. */
internal fun parseGradeNumber(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

internal fun parseSchoolGrade(raw: String): Double? =
    parseGradeNumber(raw)?.takeIf { it in 1.0..6.0 }

internal fun parsePositiveGradeNumber(raw: String): Double? =
    parseGradeNumber(raw)?.takeIf { it > 0.0 }

/** Scale weights first so even large, finite weights cannot overflow the average. */
internal fun weightedGradeAverage(values: List<Pair<Double, Double>>): Double? {
    if (values.isEmpty()) return null
    val scale = values.maxOf { it.second }
    val weight = values.sumOf { it.second / scale }
    return values.sumOf { it.first * (it.second / scale) } / weight
}
