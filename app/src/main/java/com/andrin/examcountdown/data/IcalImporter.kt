package com.andrin.examcountdown.data

import com.andrin.examcountdown.model.Exam
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IcalImportResult(
    val exams: List<Exam>,
    val message: String
)

class IcalImporter {
    suspend fun importFromUrl(url: String): IcalImportResult = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeAndValidateUrl(url)
        val raw = IcalHttpClient.download(normalizedUrl)
        importFromRawInternal(raw)
    }

    internal suspend fun importFromRaw(raw: String): IcalImportResult = withContext(Dispatchers.Default) {
        importFromRawInternal(raw)
    }

    private fun importFromRawInternal(raw: String): IcalImportResult {
        val parsedEvents = parseIcalEvents(raw)
            .filter { it.startsAtEpochMillis > System.currentTimeMillis() }
            .sortedBy { it.startsAtEpochMillis }

        val examCandidates = parsedEvents.filter { event ->
            isExamLike(event)
        }

        if (examCandidates.isEmpty()) {
            return IcalImportResult(
                exams = emptyList(),
                message = "Keine kommenden Prüfungen im iCal gefunden."
            )
        }

        val exams = examCandidates.map { event ->
            val stableIdSeed = event.uid?.takeIf { it.isNotBlank() }
                ?: "${event.summary}|${event.startsAtEpochMillis}|${event.location.orEmpty()}"
            val summaryInfo = parseExamSummaryForDisplay(event.summary)

            Exam(
                id = "ical:$stableIdSeed".replace("\\s+".toRegex(), "_"),
                title = summaryInfo.title.take(140),
                subject = summaryInfo.subject?.take(40),
                location = event.location
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { normalizeGermanWords(it) }
                    ?.take(160),
                startsAtEpochMillis = event.startsAtEpochMillis
            )
        }.distinctBy { it.id }

        return IcalImportResult(
            exams = exams,
            message = "${exams.size} Prüfungen aus iCal importiert."
        )
    }

    private fun normalizeAndValidateUrl(url: String): String {
        val normalizedUrl = url.trim()
        require(normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
            "Ungültige URL"
        }
        return normalizedUrl
    }

    private fun isExamLike(event: ParsedIcalEvent): Boolean {
        val uid = event.uid.orEmpty().lowercase(Locale.ROOT)
        val isCenterboardEntry = uid.contains("@centerboard.ch")
        val isCenterboardExam = uid.contains("etp_") || uid.contains("pruefung@centerboard.ch")
        val isCenterboardNonExam = uid.contains("ett_") || uid.contains("termin@centerboard.ch")

        // schulNetz/Centerboard liefert Prüfungen mit etP_.
        // Alle anderen Centerboard-Typen (z. B. etT_ Termine) werden ausgeschlossen.
        if (isCenterboardEntry) {
            if (isCenterboardNonExam) return false
            if (isCenterboardExam) return true
        }

        val text = listOf(
            uid,
            event.summary,
            event.description.orEmpty(),
            event.location.orEmpty()
        )
            .joinToString(" ")
            .lowercase(Locale.ROOT)

        val positiveKeywords = listOf(
            "prüfung", "pruefung", "test", "klausur", "exam", "quiz", "lernkontrolle",
            "matura", "probe", "prüfungstermin", "assessment", "nachprüfung", "nachpruefung",
            "aufnahmeprüfung", "aufnahmepruefung", "kurzprüfung", "kurzpruefung", "kontrolle"
        )
        val negativeKeywords = listOf(
            "lektion", "unterricht", "stunde", "stundenplan", "termin", "event", "anlass",
            "sprechstunde", "abgabe", "ferien", "urlaub", "ausflug", "meeting"
        )

        val containsExamKeyword = positiveKeywords.any { keyword -> text.contains(keyword) }
        val containsExcludedKeyword = negativeKeywords.any { keyword -> text.contains(keyword) }
        return containsExamKeyword && !containsExcludedKeyword
    }

    private data class ParsedIcalEvent(
        val uid: String?,
        val summary: String,
        val description: String?,
        val location: String?,
        val startsAtEpochMillis: Long
    )

    private fun parseIcalEvents(raw: String): List<ParsedIcalEvent> {
        val lines = unfoldIcalLines(raw)
        val events = mutableListOf<ParsedIcalEvent>()

        var inEvent = false
        var uid: String? = null
        var summary: String? = null
        var description: String? = null
        var location: String? = null
        var startsAt: Long? = null

        lines.forEach { line ->
            when (line) {
                "BEGIN:VEVENT" -> {
                    inEvent = true
                    uid = null
                    summary = null
                    description = null
                    location = null
                    startsAt = null
                }

                "END:VEVENT" -> {
                    if (inEvent && startsAt != null && !summary.isNullOrBlank()) {
                        events += ParsedIcalEvent(
                            uid = uid,
                            summary = summary.orEmpty(),
                            description = description,
                            location = location,
                            startsAtEpochMillis = startsAt!!
                        )
                    }
                    inEvent = false
                }

                else -> {
                    if (!inEvent) return@forEach

                    val separatorIndex = line.indexOf(':')
                    if (separatorIndex <= 0) return@forEach

                    val property = line.substring(0, separatorIndex)
                    val value = line.substring(separatorIndex + 1)
                    val key = property.substringBefore(';').uppercase(Locale.ROOT)
                    val params = parseParams(property)

                    when (key) {
                        "UID" -> uid = unescapeIcalText(value)
                        "SUMMARY" -> summary = unescapeIcalText(value)
                        "DESCRIPTION" -> description = unescapeIcalText(value)
                        "LOCATION" -> location = unescapeIcalText(value)
                        "DTSTART" -> startsAt = parseDateTime(value, params["TZID"])
                    }
                }
            }
        }

        return events
    }

    private fun unfoldIcalLines(raw: String): List<String> {
        val normalized = raw.replace("\r\n", "\n")
        val output = mutableListOf<String>()

        normalized.lines().forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && output.isNotEmpty()) {
                output[output.lastIndex] = output.last() + line.trimStart()
            } else {
                output += line
            }
        }

        return output
    }

    private fun parseParams(property: String): Map<String, String> {
        val parts = property.split(';')
        if (parts.size <= 1) return emptyMap()

        return parts.drop(1)
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = part.substring(0, idx).uppercase(Locale.ROOT)
                val value = part.substring(idx + 1)
                key to value
            }
            .toMap()
    }

    private fun parseDateTime(value: String, tzid: String?): Long? {
        if (value.length == 8) {
            return runCatching {
                val date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
                date.atStartOfDay(resolveZone(tzid)).toInstant().toEpochMilli()
            }.getOrNull()
        }

        if (value.endsWith("Z")) {
            val utcFormatter = DateTimeFormatterBuilder()
                .appendPattern("yyyyMMdd'T'HHmm")
                .optionalStart().appendPattern("ss").optionalEnd()
                .appendLiteral('Z')
                .toFormatter(Locale.ROOT)

            return runCatching {
                val localDateTime = LocalDateTime.parse(value, utcFormatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }.getOrNull()
        }

        val localFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyyMMdd'T'HHmm")
            .optionalStart().appendPattern("ss").optionalEnd()
            .toFormatter(Locale.ROOT)

        return runCatching {
            val localDateTime = LocalDateTime.parse(value, localFormatter)
            localDateTime.atZone(resolveZone(tzid)).toInstant().toEpochMilli()
        }.getOrNull()
    }

    private fun resolveZone(tzid: String?): ZoneId {
        if (tzid.isNullOrBlank()) return ZoneId.systemDefault()
        return runCatching { ZoneId.of(tzid) }.getOrDefault(ZoneId.systemDefault())
    }

    internal fun parseExamSummaryForDisplay(rawSummary: String): ExamSummaryInfo {
        val cleaned = normalizeGermanWords(rawSummary.trim())
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isBlank()) {
            return ExamSummaryInfo(
                subject = null,
                title = "Prüfung"
            )
        }

        val firstToken = cleaned.substringBefore(' ')
        val remaining = cleaned.substringAfter(' ', "").trim()

        if (firstToken.contains("_")) {
            val subject = firstToken.substringBefore('_')
                .trim()
                .takeIf { it.isNotBlank() }
                ?.let(::normalizeSubjectCode)
            val parsedTitle = if (remaining.isNotBlank()) {
                remaining
            } else {
                firstToken.replace('_', ' ')
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .ifBlank { "Prüfung" }
            }
            return ExamSummaryInfo(
                subject = subject,
                title = parsedTitle
            )
        }

        val splitByColon = cleaned.split(':', '-', limit = 2)
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (splitByColon.size == 2 && splitByColon.first().length in 2..24) {
            return ExamSummaryInfo(
                subject = splitByColon.first(),
                title = splitByColon.last()
            )
        }

        return ExamSummaryInfo(
            subject = extractSubjectFromKeywordPattern(cleaned),
            title = cleaned
        )
    }

    private fun normalizeGermanWords(input: String): String {
        var text = input
        text = replaceWordCaseInsensitive(text, "pruefungen", "Prüfungen")
        text = replaceWordCaseInsensitive(text, "pruefung", "Prüfung")
        text = replaceWordCaseInsensitive(text, "nachpruefungen", "Nachprüfungen")
        text = replaceWordCaseInsensitive(text, "nachpruefung", "Nachprüfung")
        return text
    }

    private fun replaceWordCaseInsensitive(input: String, from: String, replacement: String): String {
        val regex = Regex("\\b$from\\b", RegexOption.IGNORE_CASE)
        return regex.replace(input) { match ->
            val startsUpper = match.value.firstOrNull()?.isUpperCase() == true
            if (startsUpper) {
                replacement
            } else {
                replacement.lowercase(Locale.ROOT)
            }
        }
    }

    private fun unescapeIcalText(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .trim()
    }

    private fun extractSubjectFromKeywordPattern(text: String): String? {
        val lowered = text.lowercase(Locale.ROOT)
        val markers = listOf("prüfung", "pruefung", "test", "klausur", "exam", "quiz")
        val idx = markers
            .map { marker -> lowered.indexOf(marker) }
            .filter { it > 0 }
            .minOrNull()
            ?: return null

        return text.substring(0, idx)
            .trim(' ', '-', ':', ',', ';')
            .takeIf { it.length in 2..28 }
    }

    private fun normalizeSubjectCode(raw: String): String {
        val normalized = raw
            .trim()
            .replace(Regex("[^A-Za-zÄÖÜäöü0-9]"), "")
        if (normalized.isBlank()) return raw.trim()
        return when {
            normalized.length <= 4 -> normalized.uppercase(Locale.ROOT)
            else -> normalized.lowercase(Locale.ROOT)
                .replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    internal data class ExamSummaryInfo(
        val subject: String?,
        val title: String
    )
}
