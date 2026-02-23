package com.andrin.examcountdown.util

data class ReminderOption(
    val label: String,
    val minutesBefore: Long?
)

val reminderOptions = listOf(
    ReminderOption("Keine Erinnerung", null),
    ReminderOption("10 Minuten vorher", 10L),
    ReminderOption("30 Minuten vorher", 30L),
    ReminderOption("1 Stunde vorher", 60L),
    ReminderOption("2 Stunden vorher", 120L),
    ReminderOption("1 Tag vorher", 24L * 60L)
)

fun formatReminderLeadTime(minutesBefore: Long?): String {
    if (minutesBefore == null) return "Keine Erinnerung"

    val preset = reminderOptions.firstOrNull { it.minutesBefore == minutesBefore }
    if (preset != null) return preset.label

    return if (minutesBefore < 60L) {
        "$minutesBefore Minuten vorher"
    } else if (minutesBefore % 60L == 0L) {
        val hours = minutesBefore / 60L
        if (hours == 1L) "1 Stunde vorher" else "$hours Stunden vorher"
    } else {
        "$minutesBefore Minuten vorher"
    }
}
