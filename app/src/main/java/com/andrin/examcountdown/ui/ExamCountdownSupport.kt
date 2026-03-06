package com.andrin.examcountdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatExamDate
import com.andrin.examcountdown.util.formatExamDateShort
import com.andrin.examcountdown.util.formatReminderLeadTime
import com.andrin.examcountdown.util.formatTimeRange
import java.io.OutputStream
import java.net.URI
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

internal data class ChangelogVersion(
    val versionName: String,
    val highlights: List<String>
)

internal fun changelogEntriesFor(versionName: String): List<String> {
    val normalized = normalizeVersionName(versionName)
    return changelogTimeline()
        .firstOrNull { it.versionName == normalized }
        ?.highlights
        ?: listOf(
            "Neue Version mit Verbesserungen für Sync, UI und Stabilität.",
            "Mehr Details findest du im vollständigen Update-Log."
        )
}

private fun normalizeVersionName(versionName: String): String {
    return versionName.trim()
        .removePrefix("v")
        .substringBefore("-")
}

internal fun changelogTimeline(): List<ChangelogVersion> {
    return listOf(
        ChangelogVersion(
            versionName = "1.6.6",
            highlights = listOf(
                "Was ist neu: Button 'Mehr anzeigen' öffnet jetzt den vollständigen Update-Verlauf.",
                "Neues Update-Log zeigt Änderungen von Version 1.0.0 bis heute in der App."
            )
        ),
        ChangelogVersion(
            versionName = "1.6.5",
            highlights = listOf(
                "Kalender-Badges verbessert (lesbarer bei vielen Einträgen).",
                "Hilfe-Texte klarer geschrieben und Widget-Ansicht responsiver gemacht."
            )
        ),
        ChangelogVersion(
            versionName = "1.6.4",
            highlights = listOf(
                "UI-Polish in mehreren Screens.",
                "Datenschutz-Hinweise vereinfacht und iCal-Anleitung ergänzt."
            )
        ),
        ChangelogVersion(
            versionName = "1.6.3",
            highlights = listOf(
                "Agenda-Monatsansicht und Tagesansicht ausgebaut.",
                "Eigene wiederkehrende Events und Lern-Sessions hinzugefügt."
            )
        ),
        ChangelogVersion(
            versionName = "1.6.2",
            highlights = listOf(
                "Sicherheits-Setup erweitert (Security Policy, Dependabot, CodeQL).",
                "Biometrie-Entsperrung mit CryptoObject-Prüfung gehärtet."
            )
        ),
        ChangelogVersion(
            versionName = "1.6.1",
            highlights = listOf(
                "School-ready UX verbessert und optionalen App-Schutz ergänzt.",
                "Setup-Hilfe ein-/ausschaltbar gemacht und Stundenplan-Layout poliert."
            )
        ),
        ChangelogVersion(
            versionName = "1.6.0",
            highlights = listOf(
                "Onboarding klarer gemacht und Erste-Schritte-Führung verbessert."
            )
        ),
        ChangelogVersion(
            versionName = "1.5.0",
            highlights = listOf(
                "Sync-Diagnose, Delta-Sync (ETag/Last-Modified) und bessere Fehlersichtbarkeit.",
                "Widget-Konfiguration, Barrierefreiheit sowie CSV/PDF-Export ergänzt."
            )
        ),
        ChangelogVersion(
            versionName = "1.4.0",
            highlights = listOf(
                "Agenda-Events importiert und Personalisierung erweitert."
            )
        ),
        ChangelogVersion(
            versionName = "1.3.3",
            highlights = listOf(
                "Sync-Härtung, Backup-Sicherheit und Reminder-Verhalten verbessert."
            )
        ),
        ChangelogVersion(
            versionName = "1.3.2",
            highlights = listOf(
                "Prüfungs-Suche optimiert und Sync-Status-Leiste schaltbar gemacht."
            )
        ),
        ChangelogVersion(
            versionName = "1.3.1",
            highlights = listOf(
                "Werkzeuge in das Top-Menü verschoben und Prüfungsansicht vereinfacht."
            )
        ),
        ChangelogVersion(
            versionName = "1.3.0",
            highlights = listOf(
                "Prüfungs-UX mit Suche/Überblick verbessert.",
                "Jetzt/Nächste-Lektion-Ansicht für den Stundenplan ergänzt."
            )
        ),
        ChangelogVersion(
            versionName = "1.2.0",
            highlights = listOf(
                "Sync, Facherkennung und Benachrichtigungen verbessert."
            )
        ),
        ChangelogVersion(
            versionName = "1.1.2",
            highlights = listOf(
                "Release-Workflow: KEY_PASSWORD als optional unterstützt."
            )
        ),
        ChangelogVersion(
            versionName = "1.1.1",
            highlights = listOf(
                "Release-Signing robuster gemacht (sicherer Fallback)."
            )
        ),
        ChangelogVersion(
            versionName = "1.1.0",
            highlights = listOf(
                "Change-Feed, Backup, Quiet Hours, Widgets und Noten-Kategorien ergänzt.",
                "Onboarding-Popup nur noch einmal pro Gerät angezeigt."
            )
        ),
        ChangelogVersion(
            versionName = "1.0.5",
            highlights = listOf(
                "CI/CD-Workflow für APK-Ausgabe robuster gemacht."
            )
        ),
        ChangelogVersion(
            versionName = "1.0.4",
            highlights = listOf(
                "GitHub-Actions-Pfade korrigiert und Stundenplan-UX verbessert."
            )
        ),
        ChangelogVersion(
            versionName = "1.0.3",
            highlights = listOf(
                "Kleines Stabilitäts-Update ohne größere UI-Änderung."
            )
        ),
        ChangelogVersion(
            versionName = "1.0.2",
            highlights = listOf(
                "schulNetz-iCal-Import eingeführt (inkl. Prüfungsfilter).",
                "Stundenplan verbessert: Verschiebungen, Raumwechsel und Doppel-Lektionen."
            )
        ),
        ChangelogVersion(
            versionName = "1.0.1",
            highlights = listOf(
                "GitHub-Workflow-Fix: gradlew unter Linux korrekt ausführbar."
            )
        ),
        ChangelogVersion(
            versionName = "1.0.0",
            highlights = listOf(
                "Erste Version mit automatischem GitHub-Release-APK-Workflow."
            )
        )
    )
}

internal fun buildExamsCsv(exams: List<Exam>): String {
    val header = "Fach,Titel,Datum,Ort,Countdown"
    val rows = exams
        .sortedBy { it.startsAtEpochMillis }
        .map { exam ->
            listOf(
                exam.subject.orEmpty(),
                exam.title,
                formatExamDate(exam.startsAtEpochMillis),
                exam.location.orEmpty(),
                formatCountdown(exam.startsAtEpochMillis)
            ).joinToString(",") { csvEscape(it) }
        }
    return buildString {
        appendLine(header)
        rows.forEach { appendLine(it) }
    }
}

internal fun buildTimetableCsv(lessons: List<TimetableLesson>): String {
    val header = "Titel,Start,Ende,Uhrzeit,Ort,Verschoben,Raum geändert"
    val rows = lessons
        .sortedBy { it.startsAtEpochMillis }
        .map { lesson ->
            listOf(
                lesson.title,
                formatExamDateShort(lesson.startsAtEpochMillis),
                formatExamDateShort(lesson.endsAtEpochMillis),
                formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis),
                lesson.location.orEmpty(),
                if (lesson.isMoved) "Ja" else "Nein",
                if (lesson.isLocationChanged) "Ja" else "Nein"
            ).joinToString(",") { csvEscape(it) }
        }
    return buildString {
        appendLine(header)
        rows.forEach { appendLine(it) }
    }
}

internal fun buildExamPdfLines(exams: List<Exam>): List<String> {
    if (exams.isEmpty()) return listOf("Keine Prüfungen vorhanden.")
    return exams.sortedBy { it.startsAtEpochMillis }.map { exam ->
        val subjectPrefix = exam.subject?.takeIf { it.isNotBlank() }?.let { "[$it] " }.orEmpty()
        "$subjectPrefix${exam.title} | ${formatExamDate(exam.startsAtEpochMillis)}${exam.location?.let { " | $it" }.orEmpty()}"
    }
}

internal fun buildTimetablePdfLines(lessons: List<TimetableLesson>): List<String> {
    if (lessons.isEmpty()) return listOf("Keine Lektionen vorhanden.")
    return lessons.sortedBy { it.startsAtEpochMillis }.map { lesson ->
        val flags = buildList {
            if (lesson.isMoved) add("verschoben")
            if (lesson.isLocationChanged) add("Raum geändert")
        }.joinToString(", ")
        "${lesson.title} | ${formatExamDateShort(lesson.startsAtEpochMillis)} ${formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis)}${lesson.location?.let { " | $it" }.orEmpty()}${if (flags.isNotBlank()) " | $flags" else ""}"
    }
}

private fun csvEscape(raw: String): String {
    val normalized = raw.replace("\r", " ").replace("\n", " ")
    return "\"${normalized.replace("\"", "\"\"")}\""
}

internal fun writeSimplePdf(
    outputStream: OutputStream,
    title: String,
    lines: List<String>
) {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val left = 40f
    val top = 56f
    val bottom = pageHeight - 48f

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f
        isFakeBoldText = true
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
    }

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = top

    fun newPage() {
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = top
        canvas.drawText(title, left, y, titlePaint)
        y += 24f
    }

    canvas.drawText(title, left, y, titlePaint)
    y += 24f

    val maxCharsPerLine = 92
    lines.forEach { line ->
        val wrapped = line.chunked(maxCharsPerLine)
        wrapped.forEach { part ->
            if (y > bottom) {
                newPage()
            }
            canvas.drawText(part, left, y, bodyPaint)
            y += 16f
        }
    }

    document.finishPage(page)
    document.writeTo(outputStream)
    document.close()
}

internal fun formatDurationMillis(durationMillis: Long): String {
    val safe = durationMillis.coerceAtLeast(0L)
    val seconds = safe / 1_000L
    val millis = safe % 1_000L
    return "${seconds}s ${millis}ms"
}

internal data class ExamPresentation(
    val subject: String?,
    val title: String
)

internal fun buildExamPresentation(exam: Exam): ExamPresentation {
    val normalizedTitle = normalizeExamDisplayText(exam.title)
    val parsedFromTitle = parseEmbeddedSubjectFromTitle(normalizedTitle)
    val subject = normalizeSubjectForDisplay(exam.subject) ?: parsedFromTitle?.subject
    val title = (parsedFromTitle?.title ?: normalizedTitle)
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { "Prüfung" }
    return ExamPresentation(
        subject = subject,
        title = title
    )
}

private data class ParsedSubjectTitle(
    val subject: String?,
    val title: String
)

private fun parseEmbeddedSubjectFromTitle(title: String): ParsedSubjectTitle? {
    val cleaned = title.trim().replace(Regex("\\s+"), " ")
    if (cleaned.isBlank()) return null

    val firstToken = cleaned.substringBefore(' ')
    val remaining = cleaned.substringAfter(' ', "").trim()
    if (firstToken.contains("_")) {
        val subject = normalizeSubjectForDisplay(firstToken.substringBefore('_'))
        val parsedTitle = remaining.ifBlank {
            firstToken.substringAfter('_', "")
                .replace('_', ' ')
                .trim()
        }.ifBlank { "Prüfung" }
        return ParsedSubjectTitle(subject = subject, title = parsedTitle)
    }

    val parts = cleaned.split(':', '-', limit = 2)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (parts.size == 2 && parts.first().length in 2..10) {
        return ParsedSubjectTitle(
            subject = normalizeSubjectForDisplay(parts.first()),
            title = parts.last().ifBlank { "Prüfung" }
        )
    }

    return null
}

private fun normalizeSubjectForDisplay(raw: String?): String? {
    val normalized = raw.orEmpty()
        .trim()
        .replace(Regex("[^A-Za-zÄÖÜäöü0-9]"), "")
    if (normalized.isBlank()) return null
    return if (normalized.length <= 6) {
        normalized.uppercase()
    } else {
        normalized.lowercase().replaceFirstChar { it.titlecase() }
    }
}

private fun normalizeExamDisplayText(raw: String): String {
    var text = raw.trim()
        .replace(Regex("\\s+"), " ")
    text = replaceWordCaseInsensitive(text, "pruefungen", "Prüfungen")
    text = replaceWordCaseInsensitive(text, "pruefung", "Prüfung")
    text = replaceWordCaseInsensitive(text, "nachpruefungen", "Nachprüfungen")
    text = replaceWordCaseInsensitive(text, "nachpruefung", "Nachprüfung")
    return text
}

private fun replaceWordCaseInsensitive(input: String, from: String, replacement: String): String {
    val regex = Regex("\\b$from\\b", RegexOption.IGNORE_CASE)
    return regex.replace(input, replacement)
}

internal fun isIcalLinkRepairRecommended(lastSyncError: String?): Boolean {
    val error = lastSyncError.orEmpty().lowercase()
    if (error.isBlank()) return false
    return listOf(
        "http-401",
        "http-403",
        "http-404",
        "http-410",
        "zugriff verweigert",
        "ungültig",
        "ungueltig",
        "abgelaufen",
        "nicht gefunden",
        "nicht mehr verfügbar",
        "nicht mehr verfuegbar"
    ).any { token -> error.contains(token) }
}

internal data class ReminderSeriesLeadTimes(
    val leadTimes: List<Long>,
    val error: String? = null,
    val preview: String? = null
)

internal fun parseLeadTimesMinutes(raw: String): List<Long> {
    return raw.split(',', ';', ' ')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull { it.toLongOrNull() }
        .filter { it in 1L..(60L * 24L * 60L) }
        .distinct()
        .sorted()
}

internal fun toggleLeadTimePreset(currentRaw: String, minutes: Long): String {
    val values = parseLeadTimesMinutes(currentRaw).toMutableSet()
    if (minutes in values) {
        values.remove(minutes)
    } else {
        values.add(minutes)
    }
    return values.sorted().joinToString(",")
}

internal fun buildReminderSeriesLeadTimes(
    startDaysRaw: String,
    startHoursRaw: String,
    startMinutesRaw: String,
    repeatCountRaw: String,
    intervalDaysRaw: String,
    intervalHoursRaw: String,
    intervalMinutesRaw: String
): ReminderSeriesLeadTimes {
    val startDays = startDaysRaw.toIntOrNull() ?: return ReminderSeriesLeadTimes(
        leadTimes = emptyList(),
        error = "Bitte bei 'Ab wann' nur Zahlen eintragen."
    )
    val startHours = startHoursRaw.toIntOrNull() ?: return ReminderSeriesLeadTimes(
        leadTimes = emptyList(),
        error = "Bitte bei 'Ab wann' nur Zahlen eintragen."
    )
    val startMinutes = startMinutesRaw.toIntOrNull() ?: return ReminderSeriesLeadTimes(
        leadTimes = emptyList(),
        error = "Bitte bei 'Ab wann' nur Zahlen eintragen."
    )
    val repeatCount = repeatCountRaw.toIntOrNull() ?: return ReminderSeriesLeadTimes(
        leadTimes = emptyList(),
        error = "Bitte eine Anzahl zwischen 1 und 8 wählen."
    )

    if (startDays !in 0..60 || startHours !in 0..23 || startMinutes !in 0..59) {
        return ReminderSeriesLeadTimes(
            leadTimes = emptyList(),
            error = "Ab wann: Tage 0-60, Stunden 0-23, Minuten 0-59."
        )
    }
    if (repeatCount !in 1..8) {
        return ReminderSeriesLeadTimes(
            leadTimes = emptyList(),
            error = "Wie oft: bitte 1 bis 8."
        )
    }

    val startTotalMinutes = startDays * 24L * 60L + startHours * 60L + startMinutes
    if (startTotalMinutes <= 0L) {
        return ReminderSeriesLeadTimes(
            leadTimes = emptyList(),
            error = "Ab wann muss größer als 0 sein."
        )
    }

    val intervalDays = intervalDaysRaw.toIntOrNull() ?: 0
    val intervalHours = intervalHoursRaw.toIntOrNull() ?: 0
    val intervalMinutes = intervalMinutesRaw.toIntOrNull() ?: 0
    if (intervalDays !in 0..60 || intervalHours !in 0..23 || intervalMinutes !in 0..59) {
        return ReminderSeriesLeadTimes(
            leadTimes = emptyList(),
            error = "Abstand: Tage 0-60, Stunden 0-23, Minuten 0-59."
        )
    }

    val intervalTotalMinutes = intervalDays * 24L * 60L + intervalHours * 60L + intervalMinutes
    if (repeatCount > 1 && intervalTotalMinutes <= 0L) {
        return ReminderSeriesLeadTimes(
            leadTimes = emptyList(),
            error = "Für mehrere Erinnerungen muss der Abstand größer als 0 sein."
        )
    }

    val leads = (0 until repeatCount)
        .map { index -> startTotalMinutes + index * intervalTotalMinutes }
        .distinct()
        .sorted()

    if (leads.any { it > 60L * 24L * 60L }) {
        return ReminderSeriesLeadTimes(
            leadTimes = emptyList(),
            error = "Erinnerungen dürfen maximal 60 Tage vorher liegen."
        )
    }

    val preview = if (repeatCount == 1) {
        "Erinnert ${formatReminderLeadTime(startTotalMinutes)}."
    } else {
        "Erinnert $repeatCount-mal: zuerst ${formatReminderLeadTime(startTotalMinutes)}, dann alle ${formatDurationCompact(intervalTotalMinutes)}."
    }

    return ReminderSeriesLeadTimes(
        leadTimes = leads,
        preview = preview
    )
}

private fun formatDurationCompact(totalMinutes: Long): String {
    val safe = totalMinutes.coerceAtLeast(0L)
    val days = safe / (24L * 60L)
    val restAfterDays = safe % (24L * 60L)
    val hours = restAfterDays / 60L
    val minutes = restAfterDays % 60L
    val parts = buildList {
        if (days > 0) add("$days Tage")
        if (hours > 0) add("$hours Std")
        if (minutes > 0) add("$minutes Min")
    }
    return parts.joinToString(" ").ifBlank { "0 Min" }
}

internal fun maskUrlForDisplay(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    return runCatching {
        val uri = URI(trimmed)
        val host = uri.host.orEmpty()
        val path = uri.path.orEmpty()
        val safePath = if (path.length > 18) {
            "${path.take(9)}...${path.takeLast(6)}"
        } else {
            path
        }
        val hasQuery = !uri.rawQuery.isNullOrBlank()
        buildString {
            append(uri.scheme ?: "https")
            append("://")
            append(if (host.isBlank()) "***" else host)
            if (safePath.isNotBlank()) append(safePath)
            if (hasQuery) append("?***")
        }
    }.getOrElse {
        if (trimmed.length > 24) {
            "${trimmed.take(12)}...${trimmed.takeLast(8)}"
        } else {
            "***"
        }
    }
}

internal fun formatMinutesOfDay(minutesOfDay: Int): String {
    val normalized = minutesOfDay.coerceIn(0, 24 * 60 - 1)
    val time = LocalTime.of(normalized / 60, normalized % 60)
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

internal fun openTimePicker(
    context: Context,
    initialMinutesOfDay: Int,
    onPicked: (Int) -> Unit
) {
    val hour = (initialMinutesOfDay / 60).coerceIn(0, 23)
    val minute = (initialMinutesOfDay % 60).coerceIn(0, 59)
    TimePickerDialog(
        context,
        { _, pickedHour, pickedMinute ->
            onPicked(pickedHour * 60 + pickedMinute)
        },
        hour,
        minute,
        true
    ).show()
}

internal fun openDateTimePicker(
    context: Context,
    initialMillis: Long,
    onPicked: (Long) -> Unit
) {
    val initial = Calendar.getInstance().apply { timeInMillis = initialMillis }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, initial.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, initial.get(Calendar.MINUTE))
            }

            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selected.set(Calendar.MINUTE, minute)
                    selected.set(Calendar.SECOND, 0)
                    selected.set(Calendar.MILLISECOND, 0)
                    onPicked(selected.timeInMillis)
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).show()
}
