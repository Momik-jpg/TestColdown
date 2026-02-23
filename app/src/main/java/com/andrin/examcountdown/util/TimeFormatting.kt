package com.andrin.examcountdown.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val appLocale: Locale = Locale.GERMANY
private val fullFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy · HH:mm", appLocale)
private val shortFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm", appLocale)
private val reminderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", appLocale)

fun formatExamDate(epochMillis: Long): String {
    val localDateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return fullFormatter.format(localDateTime)
}

fun formatExamDateShort(epochMillis: Long): String {
    val localDateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return shortFormatter.format(localDateTime)
}

fun formatReminderDateTime(epochMillis: Long): String {
    val localDateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return reminderFormatter.format(localDateTime)
}

fun formatCountdown(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diffMillis = epochMillis - nowMillis
    if (diffMillis <= 0L) return "Prüfung läuft"

    val totalMinutes = max(1L, diffMillis / 60_000L)
    val days = totalMinutes / (60L * 24L)
    val hours = (totalMinutes % (60L * 24L)) / 60L
    val minutes = totalMinutes % 60L

    return when {
        days > 0 -> "in ${days} Tagen ${hours} Std ${minutes} Min"
        hours > 0 -> "in ${hours} Std ${minutes} Min"
        else -> "in ${minutes} Min"
    }
}
