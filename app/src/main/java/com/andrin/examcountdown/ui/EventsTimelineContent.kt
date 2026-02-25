package com.andrin.examcountdown.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.SchoolEventType
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatExamDateShort
import com.andrin.examcountdown.util.formatTimeRange
import java.time.Instant
import java.time.ZoneId

private enum class CalendarSourceFilter(val title: String) {
    ALL("Alles"),
    EXAMS_ONLY("Nur Prüfungen"),
    LESSONS_ONLY("Nur Lektionen"),
    EVENTS_ONLY("Nur Events")
}

private enum class CalendarWindowFilter(val title: String, val maxDaysAhead: Int?) {
    TODAY("Heute", 1),
    NEXT_7("7 Tage", 7),
    NEXT_30("30 Tage", 30),
    ALL("Alle", null)
}

private enum class CalendarItemKind {
    EXAM,
    LESSON,
    EVENT
}

private data class CalendarTimelineItem(
    val id: String,
    val kind: CalendarItemKind,
    val title: String,
    val subtitle: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long?,
    val location: String?,
    val isAllDay: Boolean,
    val eventType: SchoolEventType? = null
)

@Composable
fun EventsTimelineContent(
    exams: List<Exam>,
    lessons: List<TimetableLesson>,
    events: List<SchoolEvent>,
    hasIcalUrl: Boolean,
    importEventsEnabled: Boolean,
    onOpenIcalImport: () -> Unit,
    onEnableEventsImportAndSync: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sourceFilter by rememberSaveable { mutableStateOf(CalendarSourceFilter.ALL) }
    var windowFilter by rememberSaveable { mutableStateOf(CalendarWindowFilter.NEXT_30) }
    val schoolZone = remember { ZoneId.of("Europe/Zurich") }

    val items = remember(exams, lessons, events) {
        buildList {
            exams.forEach { exam ->
                add(
                    CalendarTimelineItem(
                        id = "exam-${exam.id}",
                        kind = CalendarItemKind.EXAM,
                        title = exam.title,
                        subtitle = exam.subject?.takeIf { it.isNotBlank() }?.let { "Fach: $it" } ?: "Prüfung",
                        startsAtEpochMillis = exam.startsAtEpochMillis,
                        endsAtEpochMillis = null,
                        location = exam.location,
                        isAllDay = false
                    )
                )
            }
            lessons.forEach { lesson ->
                add(
                    CalendarTimelineItem(
                        id = "lesson-${lesson.id}",
                        kind = CalendarItemKind.LESSON,
                        title = lesson.title,
                        subtitle = if (lesson.isMoved) "Verschoben" else "Lektion",
                        startsAtEpochMillis = lesson.startsAtEpochMillis,
                        endsAtEpochMillis = lesson.endsAtEpochMillis,
                        location = lesson.location,
                        isAllDay = false
                    )
                )
            }
            events.forEach { event ->
                add(
                    CalendarTimelineItem(
                        id = "event-${event.id}",
                        kind = CalendarItemKind.EVENT,
                        title = event.title,
                        subtitle = eventTypeLabel(event.type),
                        startsAtEpochMillis = event.startsAtEpochMillis,
                        endsAtEpochMillis = event.endsAtEpochMillis,
                        location = event.location,
                        isAllDay = event.isAllDay,
                        eventType = event.type
                    )
                )
            }
        }.sortedBy { it.startsAtEpochMillis }
    }

    val filteredItems = remember(items, sourceFilter, windowFilter, searchQuery) {
        val now = System.currentTimeMillis()
        val windowEnd = windowFilter.maxDaysAhead?.let { days ->
            now + days * 24L * 60L * 60L * 1000L
        }
        val query = searchQuery.trim().lowercase()

        items.filter { item ->
            val matchesSource = when (sourceFilter) {
                CalendarSourceFilter.ALL -> true
                CalendarSourceFilter.EXAMS_ONLY -> item.kind == CalendarItemKind.EXAM
                CalendarSourceFilter.LESSONS_ONLY -> item.kind == CalendarItemKind.LESSON
                CalendarSourceFilter.EVENTS_ONLY -> item.kind == CalendarItemKind.EVENT
            }
            val itemEnd = item.endsAtEpochMillis ?: item.startsAtEpochMillis
            val matchesWindow = if (windowEnd == null) {
                itemEnd >= now
            } else {
                itemEnd >= now && item.startsAtEpochMillis <= windowEnd
            }
            val matchesQuery = query.isBlank() || listOf(
                item.title,
                item.subtitle,
                item.location.orEmpty()
            ).joinToString(" ").lowercase().contains(query)

            matchesSource && matchesWindow && matchesQuery
        }
    }

    if (items.isEmpty()) {
        EventEmptyState(
            hasIcalUrl = hasIcalUrl,
            importEventsEnabled = importEventsEnabled,
            onOpenIcalImport = onOpenIcalImport,
            onEnableEventsImportAndSync = onEnableEventsImportAndSync
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item("calendar-search") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Suche im Kalender") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Suche zurücksetzen"
                                    )
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalendarSourceFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = sourceFilter == filter,
                                onClick = { sourceFilter = filter },
                                label = { Text(filter.title) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalendarWindowFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = windowFilter == filter,
                                onClick = { windowFilter = filter },
                                label = { Text(filter.title) }
                            )
                        }
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item("calendar-empty-filtered") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (sourceFilter == CalendarSourceFilter.EVENTS_ONLY && !importEventsEnabled) {
                                "Event-Import ist deaktiviert."
                            } else {
                                "Keine Einträge für den aktuellen Filter."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (sourceFilter == CalendarSourceFilter.EVENTS_ONLY && !importEventsEnabled) {
                            OutlinedButton(onClick = onEnableEventsImportAndSync) {
                                Text("Events aktivieren + synchronisieren")
                            }
                        }
                        OutlinedButton(onClick = {
                            searchQuery = ""
                            sourceFilter = CalendarSourceFilter.ALL
                            windowFilter = CalendarWindowFilter.NEXT_30
                        }) {
                            Text("Filter zurücksetzen")
                        }
                    }
                }
            }
        } else {
            items(items = filteredItems, key = { it.id }) { item ->
                CalendarTimelineCard(item = item, schoolZone = schoolZone)
            }
        }
    }
}

@Composable
private fun CalendarTimelineCard(
    item: CalendarTimelineItem,
    schoolZone: ZoneId
) {
    val badgeColor = when (item.kind) {
        CalendarItemKind.EXAM -> MaterialTheme.colorScheme.primary
        CalendarItemKind.LESSON -> MaterialTheme.colorScheme.tertiary
        CalendarItemKind.EVENT -> eventTypeColor(item.eventType)
    }
    val badgeLabel = when (item.kind) {
        CalendarItemKind.EXAM -> "Prüfung"
        CalendarItemKind.LESSON -> "Lektion"
        CalendarItemKind.EVENT -> eventTypeLabel(item.eventType)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = badgeColor.copy(alpha = 0.14f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = badgeLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeColor
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val timeLabel = when {
                item.isAllDay -> formatAllDayLabel(
                    startsAtMillis = item.startsAtEpochMillis,
                    endsAtMillis = item.endsAtEpochMillis
                        ?: (item.startsAtEpochMillis + 24L * 60L * 60L * 1000L),
                    zoneId = schoolZone
                )
                item.endsAtEpochMillis != null -> "${formatExamDateShort(item.startsAtEpochMillis)} · ${formatTimeRange(item.startsAtEpochMillis, item.endsAtEpochMillis)}"
                else -> formatExamDateShort(item.startsAtEpochMillis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.isAllDay) Icons.Outlined.CalendarToday else Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item.location?.takeIf { it.isNotBlank() }?.let { location ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (item.kind == CalendarItemKind.EXAM) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = formatCountdown(item.startsAtEpochMillis),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EventEmptyState(
    hasIcalUrl: Boolean,
    importEventsEnabled: Boolean,
    onOpenIcalImport: () -> Unit,
    onEnableEventsImportAndSync: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item("events-empty") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Keine Kalender-Einträge verfügbar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (hasIcalUrl) {
                            "Tippe oben rechts auf Aktualisieren. Optional kannst du Event-Import aktivieren."
                        } else {
                            "Füge zuerst deinen iCal-Link hinzu."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onOpenIcalImport) {
                        Text(if (hasIcalUrl) "iCal aktualisieren" else "iCal hinzufügen")
                    }
                    if (hasIcalUrl && !importEventsEnabled) {
                        OutlinedButton(onClick = onEnableEventsImportAndSync) {
                            Text("Events aktivieren + synchronisieren")
                        }
                    }
                }
            }
        }
    }
}

private fun eventTypeLabel(type: SchoolEventType?): String {
    return when (type) {
        SchoolEventType.HOLIDAY -> "Ferien"
        SchoolEventType.DEADLINE -> "Abgabe"
        SchoolEventType.INFO -> "Info"
        SchoolEventType.SCHOOL -> "Anlass"
        else -> "Event"
    }
}

@Composable
private fun eventTypeColor(type: SchoolEventType?): Color {
    return when (type) {
        SchoolEventType.HOLIDAY -> MaterialTheme.colorScheme.secondary
        SchoolEventType.DEADLINE -> MaterialTheme.colorScheme.error
        SchoolEventType.INFO -> MaterialTheme.colorScheme.tertiary
        SchoolEventType.SCHOOL -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
}

private fun formatAllDayLabel(startsAtMillis: Long, endsAtMillis: Long, zoneId: ZoneId): String {
    val startDate = Instant.ofEpochMilli(startsAtMillis).atZone(zoneId).toLocalDate()
    val endDateExclusive = Instant.ofEpochMilli(endsAtMillis).atZone(zoneId).toLocalDate()
    val endDateInclusive = if (endDateExclusive > startDate) endDateExclusive.minusDays(1) else startDate
    return if (startDate == endDateInclusive) {
        "${formatExamDateShort(startsAtMillis)} · Ganztägig"
    } else {
        "${startDate.dayOfMonth}.${startDate.monthValue} - ${endDateInclusive.dayOfMonth}.${endDateInclusive.monthValue} · Ganztägig"
    }
}
