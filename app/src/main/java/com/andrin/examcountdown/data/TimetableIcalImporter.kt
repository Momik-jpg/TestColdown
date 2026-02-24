package com.andrin.examcountdown.data

import com.andrin.examcountdown.model.TimetableLesson
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TimetableImportResult(
    val lessons: List<TimetableLesson>,
    val message: String
)

class TimetableIcalImporter {
    private val schoolZone: ZoneId = ZoneId.of("Europe/Zurich")

    suspend fun importFromUrl(url: String): TimetableImportResult = withContext(Dispatchers.IO) {
        val normalizedUrl = url.trim()
        require(normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
            "Ungültige URL"
        }

        val raw = URL(normalizedUrl).readText(Charsets.UTF_8)
        val windowStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val windowEnd = LocalDate.now()
            .plusDays(35)
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val parsedLessons = parseIcalEvents(raw)
            .filter { event ->
                event.startsAtEpochMillis in windowStart..windowEnd &&
                    isLessonLike(event)
            }
            .sortedBy { it.startsAtEpochMillis }

        if (parsedLessons.isEmpty()) {
            return@withContext TimetableImportResult(
                lessons = emptyList(),
                message = "Keine Lektionen im iCal gefunden."
            )
        }

        val movementHints = detectMovedLessonHints(parsedLessons)
        val lessons = parsedLessons.map { event ->
            val uniqueKey = event.uniqueKey()
            val seed = event.uid?.takeIf { it.isNotBlank() }
                ?: "${event.summary}|${event.startsAtEpochMillis}|${event.endsAtEpochMillis}|${event.location.orEmpty()}"
            val isMoved = movementHints.movedIds.contains(uniqueKey) || hasShiftKeyword(event)
            val isLocationChanged = movementHints.locationChangedIds.contains(uniqueKey)
            val originalSlot = movementHints.originalSlotByEventId[uniqueKey]
            val originalLocation = movementHints.originalLocationByEventId[uniqueKey]

            TimetableLesson(
                id = "lesson:$seed".replace("\\s+".toRegex(), "_"),
                title = formatLessonTitle(event.summary).take(140),
                location = event.location?.trim()?.takeIf { it.isNotBlank() }?.take(160),
                startsAtEpochMillis = event.startsAtEpochMillis,
                endsAtEpochMillis = event.endsAtEpochMillis,
                isMoved = isMoved,
                isLocationChanged = isLocationChanged,
                originalLocation = originalLocation,
                originalStartsAtEpochMillis = if (isMoved) originalSlot?.first else null,
                originalEndsAtEpochMillis = if (isMoved) originalSlot?.second else null
            )
        }

        TimetableImportResult(
            lessons = lessons,
            message = "${lessons.size} Lektionen synchronisiert."
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
    ) {
        fun uniqueKey(): String {
            val uidPart = uid?.takeIf { it.isNotBlank() }
                ?: "${summary}|${startsAtEpochMillis}|${endsAtEpochMillis}"
            return uidPart
        }
    }

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
                    val end = endsAt ?: start?.let { parsedStart ->
                        durationMillis?.let { parsedDuration ->
                            ParsedDateTime(
                                epochMillis = parsedStart.epochMillis + parsedDuration,
                                isDateOnly = false
                            )
                        }
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

    private fun isLessonLike(event: ParsedIcalEvent): Boolean {
        val uid = event.uid.orEmpty().lowercase(Locale.ROOT)
        val isCenterboardEntry = uid.contains("@centerboard.ch")
        if (!isCenterboardEntry) return false

        if (uid.contains("etp_") || uid.contains("ett_")) return false
        if (uid.contains("pruefung") || uid.contains("termin")) return false
        if (event.startsAtIsDateOnly || event.endsAtIsDateOnly) return false

        val duration = event.endsAtEpochMillis - event.startsAtEpochMillis
        if (duration < 10L * 60L * 1000L) return false
        if (duration > 8L * 60L * 60L * 1000L) return false

        return event.summary.isNotBlank()
    }

    private fun hasShiftKeyword(event: ParsedIcalEvent): Boolean {
        val text = listOf(event.summary, event.description.orEmpty())
            .joinToString(" ")
            .lowercase(Locale.ROOT)

        val keywords = listOf(
            "verschoben",
            "verschieb",
            "verlegt",
            "nachhol",
            "fällt aus",
            "faellt aus",
            "entfällt",
            "entfaellt",
            "ausfall"
        )

        return keywords.any { keyword -> text.contains(keyword) }
    }

    private data class SlotSignature(
        val dayOfWeek: Int,
        val hour: Int,
        val minute: Int,
        val durationMillis: Long
    )

    private data class DominantLocation(
        val normalized: String,
        val display: String,
        val count: Int
    )

    private data class MovementHints(
        val movedIds: Set<String>,
        val originalSlotByEventId: Map<String, Pair<Long, Long>>,
        val locationChangedIds: Set<String>,
        val originalLocationByEventId: Map<String, String>
    )

    private fun detectMovedLessonHints(events: List<ParsedIcalEvent>): MovementHints {
        val moved = mutableSetOf<String>()
        val originalSlot = mutableMapOf<String, Pair<Long, Long>>()
        val locationChanged = mutableSetOf<String>()
        val originalLocation = mutableMapOf<String, String>()
        val bySubject = events.groupBy { subjectKey(it.summary) }

        bySubject.forEach { (_, lessons) ->
            if (lessons.size < 5) return@forEach

            val slotCounts = lessons
                .groupingBy { slotSignature(it) }
                .eachCount()

            val dominantSlot = slotCounts
                .entries
                .maxByOrNull { it.value }
                ?.takeIf { it.value >= 2 }
                ?.key
                ?: return@forEach

            val dominantLocationBySlot = lessons
                .groupBy { slotSignature(it) }
                .mapValues { (_, slotLessons) ->
                    val groupedLocations = slotLessons
                        .mapNotNull { it.location?.trim()?.takeIf(String::isNotBlank) }
                        .groupBy { normalizeLocation(it) }

                    groupedLocations
                        .maxByOrNull { it.value.size }
                        ?.let { (normalized, values) ->
                            DominantLocation(
                                normalized = normalized,
                                display = values.first(),
                                count = values.size
                            )
                        }
                }

            for (lesson in lessons) {
                val eventId = lesson.uniqueKey()
                val slot = slotSignature(lesson)
                val isMoved = slot != dominantSlot && (slotCounts[slot] ?: 0) == 1
                if (isMoved) {
                    moved += eventId
                    originalSlot[eventId] = expectedOriginalSlotForWeek(
                        lessonStartsAtEpochMillis = lesson.startsAtEpochMillis,
                        dominantSlot = dominantSlot
                    )
                    val dominantLocation = dominantLocationBySlot[dominantSlot]
                    if (dominantLocation != null && dominantLocation.display.isNotBlank()) {
                        originalLocation[eventId] = dominantLocation.display
                        val currentLocation = lesson.location?.trim().orEmpty()
                        if (
                            currentLocation.isNotBlank() &&
                            normalizeLocation(currentLocation) != dominantLocation.normalized
                        ) {
                            locationChanged += eventId
                        }
                    }
                    continue
                }

                val dominantLocation = dominantLocationBySlot[slot] ?: continue
                if (dominantLocation.count < 2) continue

                val currentLocation = lesson.location?.trim().orEmpty()
                if (currentLocation.isBlank()) continue

                val isLocationChanged = normalizeLocation(currentLocation) != dominantLocation.normalized
                if (isLocationChanged) {
                    locationChanged += eventId
                    originalLocation[eventId] = dominantLocation.display
                }
            }
        }

        return MovementHints(
            movedIds = moved,
            originalSlotByEventId = originalSlot,
            locationChangedIds = locationChanged,
            originalLocationByEventId = originalLocation
        )
    }

    private fun subjectKey(summary: String): String {
        return summary
            .trim()
            .lowercase(Locale.ROOT)
            .substringBefore(' ')
    }

    private fun formatLessonTitle(rawSummary: String): String {
        val raw = rawSummary.trim()
        if (raw.isBlank()) return "Lektion"

        val parts = raw.split('_')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (parts.size < 2) {
            return raw.replace('_', ' ')
                .replace(Regex("\\s+"), " ")
        }

        val subject = parts.first().uppercase(Locale.ROOT)
        val className = parts.getOrNull(1).orEmpty()
        val teacher = parts.getOrNull(2).orEmpty()

        return listOf(subject, className, teacher)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }

    private fun normalizeLocation(value: String): String {
        return value.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), "")
    }

    private fun slotSignature(event: ParsedIcalEvent): SlotSignature {
        val startsAt = Instant.ofEpochMilli(event.startsAtEpochMillis)
            .atZone(schoolZone)
        val durationMillis = (event.endsAtEpochMillis - event.startsAtEpochMillis)
            .coerceAtLeast(1L)

        return SlotSignature(
            dayOfWeek = startsAt.dayOfWeek.value,
            hour = startsAt.hour,
            minute = startsAt.minute,
            durationMillis = durationMillis
        )
    }

    private fun expectedOriginalSlotForWeek(
        lessonStartsAtEpochMillis: Long,
        dominantSlot: SlotSignature
    ): Pair<Long, Long> {
        val lessonDate = Instant.ofEpochMilli(lessonStartsAtEpochMillis)
            .atZone(schoolZone)
            .toLocalDate()
        val weekStart = lessonDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val targetDate = weekStart.plusDays((dominantSlot.dayOfWeek - 1).toLong())
        val targetStart = targetDate
            .atTime(dominantSlot.hour, dominantSlot.minute)
            .atZone(schoolZone)
            .toInstant()
            .toEpochMilli()
        val targetEnd = targetStart + dominantSlot.durationMillis
        return targetStart to targetEnd
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
}
