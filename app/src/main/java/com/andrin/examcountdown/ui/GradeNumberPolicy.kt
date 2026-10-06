package com.andrin.examcountdown.ui

/** Accept decimal commas as well as dots; non-finite values cannot be a grade or weight. */
internal fun parseGradeNumber(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
