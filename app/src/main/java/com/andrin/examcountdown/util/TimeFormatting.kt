package com.andrin.examcountdown.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val appLocale: Locale = Locale.GERMANY
private val schoolZone: ZoneId = ZoneId.of("Europe/Zurich")
private val fullFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy · HH:mm", appLocale)
private val shortFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm", appLocale)
private val reminderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", appLocale)
private val dayHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", appLocale)
private val dayCompactFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd.MM", appLocale)
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", appLocale)
private val syncFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm", appLocale)

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

fun formatDayHeader(epochMillis: Long): String {
    val localDateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(schoolZone)
        .toLocalDateTime()
    return dayHeaderFormatter.format(localDateTime)
}

fun formatTimeRange(startEpochMillis: Long, endEpochMillis: Long): String {
    val start = Instant.ofEpochMilli(startEpochMillis)
        .atZone(schoolZone)
        .toLocalDateTime()
    val end = Instant.ofEpochMilli(endEpochMillis)
        .atZone(schoolZone)
        .toLocalDateTime()
    return "${timeFormatter.format(start)} - ${timeFormatter.format(end)}"
}

fun formatSyncDateTime(epochMillis: Long): String {
    val localDateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(schoolZone)
        .toLocalDateTime()
    return syncFormatter.format(localDateTime)
}

fun formatCompactDay(date: LocalDate): String = dayCompactFormatter.format(date)

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
