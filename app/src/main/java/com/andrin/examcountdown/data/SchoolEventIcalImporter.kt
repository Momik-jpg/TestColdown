package com.andrin.examcountdown.data

import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.SchoolEventType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SchoolEventImportResult(
    val events: List<SchoolEvent>,
    val message: String
)

class SchoolEventIcalImporter {
    private val schoolZone: ZoneId = ZoneId.of("Europe/Zurich")

    suspend fun importFromUrl(url: String): SchoolEventImportResult = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeAndValidateIcalUrl(url)
        val raw = IcalHttpClient.download(normalizedUrl)
        importFromRawInternal(raw)
    }

    internal suspend fun importFromRaw(raw: String): SchoolEventImportResult = withContext(Dispatchers.Default) {
        importFromRawInternal(raw)
    }

    private fun importFromRawInternal(raw: String): SchoolEventImportResult {
        val now = System.currentTimeMillis()
        val windowStart = LocalDate.now(schoolZone)
            .atStartOfDay(schoolZone)
            .toInstant()
            .toEpochMilli()
        val windowEnd = LocalDate.now(schoolZone)
            .plusDays(180)
            .atTime(23, 59, 59)
            .atZone(schoolZone)
            .toInstant()
            .toEpochMilli()

        val parsed = parseIcalEvents(raw)
            .filter { event ->
                event.endsAtEpochMillis > now &&
                    event.startsAtEpochMillis <= windowEnd &&
                    event.endsAtEpochMillis >= windowStart &&
                    isEventLike(event)
            }
            .sortedBy { it.startsAtEpochMillis }

        if (parsed.isEmpty()) {
            return SchoolEventImportResult(
                events = emptyList(),
                message = "Keine kommenden Events im iCal gefunden."
            )
        }

        val imported = parsed.map { event ->
            val seed = event.uid?.takeIf { it.isNotBlank() }
                ?: "${event.summary}|${event.startsAtEpochMillis}|${event.endsAtEpochMillis}|${event.location.orEmpty()}"
            SchoolEvent(
                id = "ical-event:$seed".replace("\\s+".toRegex(), "_"),
                title = normalizeGermanWords(event.summary).take(160),
                type = classifyEventType(event),
                location = event.location
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::normalizeGermanWords)
                    ?.take(180),
                description = event.description
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::normalizeGermanWords)
                    ?.take(600),
                startsAtEpochMillis = event.startsAtEpochMillis,
                endsAtEpochMillis = event.endsAtEpochMillis,
                isAllDay = event.startsAtIsDateOnly || event.endsAtIsDateOnly,
                source = if (event.uid.orEmpty().contains("@centerboard.ch", ignoreCase = true)) {
                    "schulNetz"
                } else {
                    "iCal"
                }
            )
        }
            .distinctBy { it.id }

        return SchoolEventImportResult(
            events = imported,
            message = "${imported.size} Events synchronisiert."
        )
    }

    private data class ParsedDateTime(
        val epochMillis: Long,
        val isDateOnly: Boolean
    )

    private data class ParsedIcalEvent(
        val uid: String?,
        val summary: String,
        val description: String?,
        val location: String?,
        val startsAtEpochMillis: Long,
        val endsAtEpochMillis: Long,
        val startsAtIsDateOnly: Boolean,
        val endsAtIsDateOnly: Boolean
    )

    private fun parseIcalEvents(raw: String): List<ParsedIcalEvent> {
        val lines = unfoldIcalLines(raw)
        val events = mutableListOf<ParsedIcalEvent>()

        var inEvent = false
        var uid: String? = null
        var summary: String? = null
        var description: String? = null
        var location: String? = null
        var startsAt: ParsedDateTime? = null
        var endsAt: ParsedDateTime? = null
        var durationMillis: Long? = null

        lines.forEach { line ->
            when (line) {
                "BEGIN:VEVENT" -> {
                    inEvent = true
                    uid = null
                    summary = null
                    description = null
                    location = null
                    startsAt = null
                    endsAt = null
                    durationMillis = null
                }

                "END:VEVENT" -> {
                    val start = startsAt
                    val end = endsAt
                        ?: start?.let { parsedStart ->
                            durationMillis?.let { parsedDuration ->
                                ParsedDateTime(
                                    epochMillis = parsedStart.epochMillis + parsedDuration,
                                    isDateOnly = false
                                )
                            }
                        }
                        ?: start?.takeIf { it.isDateOnly }?.let {
                            ParsedDateTime(
                                epochMillis = it.epochMillis + 24L * 60L * 60L * 1000L,
                                isDateOnly = true
                            )
                        }

                    if (inEvent && start != null && end != null && !summary.isNullOrBlank()) {
                        events += ParsedIcalEvent(
                            uid = uid,
                            summary = summary.orEmpty(),
                            description = description,
                            location = location,
                            startsAtEpochMillis = start.epochMillis,
                            endsAtEpochMillis = end.epochMillis,
                            startsAtIsDateOnly = start.isDateOnly,
                            endsAtIsDateOnly = end.isDateOnly
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
                        "DTSTART" -> startsAt = parseDateTime(
                            value = value,
                            tzid = params["TZID"],
                            valueType = params["VALUE"]
                        )

                        "DTEND" -> endsAt = parseDateTime(
                            value = value,
                            tzid = params["TZID"],
                            valueType = params["VALUE"]
                        )

                        "DURATION" -> durationMillis = parseDuration(value)
                    }
                }
            }
        }

        return events
    }

    private fun isEventLike(event: ParsedIcalEvent): Boolean {
        if (event.summary.isBlank()) return false

        val uid = event.uid.orEmpty().lowercase(Locale.ROOT)
        val text = listOf(
            uid,
            event.summary,
            event.description.orEmpty(),
            event.location.orEmpty()
        )
            .joinToString(" ")
            .lowercase(Locale.ROOT)

        val examKeywords = listOf(
            "prüfung", "pruefung", "test", "klausur", "exam", "quiz",
            "lernkontrolle", "nachprüfung", "nachpruefung"
        )
        if (examKeywords.any { keyword -> text.contains(keyword) }) return false

        val isCenterboard = uid.contains("@centerboard.ch")
        val isCenterboardExam = uid.contains("etp_") || uid.contains("pruefung@centerboard.ch")
        if (isCenterboardExam) return false

        val lessonKeywords = listOf(
            "lektion", "unterricht", "stundenplan", "doppellektion", "fachstunde"
        )
        val looksLikeLesson = isCenterboard &&
            !uid.contains("ett_") &&
            !uid.contains("termin@centerboard.ch") &&
            !event.startsAtIsDateOnly &&
            !event.endsAtIsDateOnly &&
            !lessonKeywords.any { keyword -> text.contains(keyword) } &&
            (event.endsAtEpochMillis - event.startsAtEpochMillis) in (20L * 60L * 1000L)..(8L * 60L * 60L * 1000L)
        if (looksLikeLesson) return false

        val duration = event.endsAtEpochMillis - event.startsAtEpochMillis
        if (!event.startsAtIsDateOnly && !event.endsAtIsDateOnly && duration < 10L * 60L * 1000L) {
            return false
        }

        return true
    }

    private fun classifyEventType(event: ParsedIcalEvent): SchoolEventType {
        val text = listOf(
            event.summary,
            event.description.orEmpty(),
            event.location.orEmpty(),
            event.uid.orEmpty()
        )
            .joinToString(" ")
            .lowercase(Locale.ROOT)

        return when {
            listOf("ferien", "urlaub", "holiday", "schulfrei", "unterrichtsfrei").any { text.contains(it) } -> {
                SchoolEventType.HOLIDAY
            }

            listOf("abgabe", "deadline", "einsendeschluss", "anmeldeschluss").any { text.contains(it) } -> {
                SchoolEventType.DEADLINE
            }

            listOf("info", "elternabend", "sprechstunde", "mitteilung").any { text.contains(it) } -> {
                SchoolEventType.INFO
            }

            listOf("anlass", "event", "ausflug", "projektwoche", "sporttag", "kultur").any { text.contains(it) } -> {
                SchoolEventType.SCHOOL
            }

            else -> SchoolEventType.OTHER
        }
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

    private fun parseDateTime(value: String, tzid: String?, valueType: String?): ParsedDateTime? {
        if (valueType.equals("DATE", ignoreCase = true) || value.length == 8) {
            return runCatching {
                val date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
                ParsedDateTime(
                    epochMillis = date.atStartOfDay(resolveZone(tzid)).toInstant().toEpochMilli(),
                    isDateOnly = true
                )
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
                ParsedDateTime(
                    epochMillis = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                    isDateOnly = false
                )
            }.getOrNull()
        }

        val localFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyyMMdd'T'HHmm")
            .optionalStart().appendPattern("ss").optionalEnd()
            .toFormatter(Locale.ROOT)

        return runCatching {
            val localDateTime = LocalDateTime.parse(value, localFormatter)
            ParsedDateTime(
                epochMillis = localDateTime.atZone(resolveZone(tzid)).toInstant().toEpochMilli(),
                isDateOnly = false
            )
        }.getOrNull()
    }

    private fun parseDuration(value: String): Long? {
        return runCatching { Duration.parse(value).toMillis() }.getOrNull()
    }

    private fun resolveZone(tzid: String?): ZoneId {
        if (tzid.isNullOrBlank()) return ZoneId.systemDefault()
        return runCatching { ZoneId.of(tzid) }.getOrDefault(ZoneId.systemDefault())
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

    private fun normalizeGermanWords(input: String): String {
        var text = input
        text = replaceWordCaseInsensitive(text, "pruefungen", "Prüfungen")
        text = replaceWordCaseInsensitive(text, "pruefung", "Prüfung")
        text = replaceWordCaseInsensitive(text, "stundenplaene", "Stundenpläne")
        text = replaceWordCaseInsensitive(text, "stundenplan", "Stundenplan")
        text = replaceWordCaseInsensitive(text, "ueber", "über")
        return text
    }

    private fun replaceWordCaseInsensitive(input: String, from: String, replacement: String): String {
        val regex = Regex("\\b$from\\b", RegexOption.IGNORE_CASE)
        return regex.replace(input) { match ->
            val startsUpper = match.value.firstOrNull()?.isUpperCase() == true
            if (startsUpper) replacement else replacement.lowercase(Locale.ROOT)
        }
    }
}
