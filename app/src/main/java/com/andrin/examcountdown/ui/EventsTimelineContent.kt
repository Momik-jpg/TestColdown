package com.andrin.examcountdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private enum class CalendarSourceFilter(val title: String) {
    ALL("Alles"),
    EXAMS_ONLY("Nur Prüfungen"),
    LESSONS_ONLY("Nur Lektionen"),
    EVENTS_ONLY("Nur Events")
}

private enum class AgendaLayoutMode(val title: String) {
    LIST("Liste"),
    MONTH("Kalender")
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
    val eventType: SchoolEventType? = null,
    val canDelete: Boolean = false
)

@Composable
fun EventsTimelineContent(
    exams: List<Exam>,
    lessons: List<TimetableLesson>,
    events: List<SchoolEvent>,
    hasIcalUrl: Boolean,
    importEventsEnabled: Boolean,
    onOpenIcalImport: () -> Unit,
    onEnableEventsImportAndSync: () -> Unit,
    onAddCustomEvents: (List<SchoolEvent>) -> Unit,
    onDeleteCustomEvent: (String) -> Unit,
    onUpdateCustomEvent: (SchoolEvent) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sourceFilter by rememberSaveable { mutableStateOf(CalendarSourceFilter.ALL) }
    var layoutMode by rememberSaveable { mutableStateOf(AgendaLayoutMode.MONTH) }
    var showAddCustomEventDialog by rememberSaveable { mutableStateOf(false) }
    var eventDialogInitialStartsAtMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var dayDialogEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val schoolZone = remember { ZoneId.of("Europe/Zurich") }
    val editingEvent = remember(editingEventId, events) {
        editingEventId?.let { id -> events.firstOrNull { it.id == id } }
    }
    val selectedDayForDialog = remember(dayDialogEpochDay) {
        dayDialogEpochDay?.let { LocalDate.ofEpochDay(it) }
    }

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
                val isManualEvent = event.source.equals("manual", ignoreCase = true) ||
                    event.id.startsWith("manual-event:")
                add(
                    CalendarTimelineItem(
                        id = "event-${event.id}",
                        kind = CalendarItemKind.EVENT,
                        title = event.title,
                        subtitle = if (isManualEvent) "Eigener Termin" else eventTypeLabel(event.type),
                        startsAtEpochMillis = event.startsAtEpochMillis,
                        endsAtEpochMillis = event.endsAtEpochMillis,
                        location = event.location,
                        isAllDay = event.isAllDay,
                        eventType = event.type,
                        canDelete = isManualEvent
                    )
                )
            }
        }.sortedBy { it.startsAtEpochMillis }
    }

    val filteredItems = remember(items, sourceFilter, searchQuery) {
        val now = System.currentTimeMillis()
        val query = searchQuery.trim().lowercase()

        items.filter { item ->
            val matchesSource = when (sourceFilter) {
                CalendarSourceFilter.ALL -> true
                CalendarSourceFilter.EXAMS_ONLY -> item.kind == CalendarItemKind.EXAM
                CalendarSourceFilter.LESSONS_ONLY -> item.kind == CalendarItemKind.LESSON
                CalendarSourceFilter.EVENTS_ONLY -> item.kind == CalendarItemKind.EVENT
            }
            val itemEnd = item.endsAtEpochMillis ?: item.startsAtEpochMillis
            val matchesWindow = itemEnd >= now
            val matchesQuery = query.isBlank() || listOf(
                item.title,
                item.subtitle,
                item.location.orEmpty()
            ).joinToString(" ").lowercase().contains(query)

            matchesSource && matchesWindow && matchesQuery
        }
    }

    if (showAddCustomEventDialog) {
        AddCustomEventDialog(
            initialStartsAtMillis = eventDialogInitialStartsAtMillis,
            editingEvent = editingEvent,
            onDismiss = {
                showAddCustomEventDialog = false
                eventDialogInitialStartsAtMillis = null
                editingEventId = null
            },
            onSave = { createdEvent ->
                showAddCustomEventDialog = false
                eventDialogInitialStartsAtMillis = null
                editingEventId = null
                if (editingEvent != null) {
                    onUpdateCustomEvent(createdEvent)
                } else {
                    onAddCustomEvents(listOf(createdEvent))
                }
            }
        )
    }

    if (selectedDayForDialog != null) {
        val dayItems = filteredItems.filter { item ->
            Instant.ofEpochMilli(item.startsAtEpochMillis)
                .atZone(schoolZone)
                .toLocalDate() == selectedDayForDialog
        }
        DayAgendaDialog(
            date = selectedDayForDialog,
            items = dayItems.sortedBy { it.startsAtEpochMillis },
            schoolZone = schoolZone,
            onDismiss = { dayDialogEpochDay = null },
            onAddEvent = {
                val startAt = selectedDayForDialog
                    .atTime(17, 0)
                    .atZone(schoolZone)
                    .toInstant()
                    .toEpochMilli()
                eventDialogInitialStartsAtMillis = startAt
                editingEventId = null
                showAddCustomEventDialog = true
            },
            onEditEvent = { eventId ->
                editingEventId = eventId
                eventDialogInitialStartsAtMillis = null
                showAddCustomEventDialog = true
            },
            onDeleteEvent = onDeleteCustomEvent
        )
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
        item("calendar-controls") {
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
                        AgendaLayoutMode.entries.forEach { mode ->
                            FilterChip(
                                selected = layoutMode == mode,
                                onClick = { layoutMode = mode },
                                label = { Text(mode.title) }
                            )
                        }
                    }

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

                    OutlinedButton(
                        onClick = {
                            showAddCustomEventDialog = true
                            editingEventId = null
                            eventDialogInitialStartsAtMillis = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text("Eigenes Event")
                    }

                    if (sourceFilter == CalendarSourceFilter.EVENTS_ONLY && !importEventsEnabled) {
                        OutlinedButton(
                            onClick = onEnableEventsImportAndSync,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Event-Import aktivieren")
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
                        OutlinedButton(onClick = {
                            searchQuery = ""
                            sourceFilter = CalendarSourceFilter.ALL
                        }) {
                            Text("Filter zurücksetzen")
                        }
                    }
                }
            }
        } else if (layoutMode == AgendaLayoutMode.MONTH) {
            item("month-grid") {
                AgendaMonthContent(
                    items = filteredItems,
                    schoolZone = schoolZone,
                    onDeleteCustomEvent = onDeleteCustomEvent,
                    onOpenDay = { day ->
                        dayDialogEpochDay = day.toEpochDay()
                    }
                )
            }
        } else {
            items(items = filteredItems, key = { it.id }) { item ->
                CalendarTimelineCard(
                    item = item,
                    schoolZone = schoolZone,
                    onDeleteCustomEvent = onDeleteCustomEvent
                )
            }
        }
    }
}

@Composable
private fun AgendaMonthContent(
    items: List<CalendarTimelineItem>,
    schoolZone: ZoneId,
    onDeleteCustomEvent: (String) -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now(schoolZone) }
    var monthAnchorEpochDay by rememberSaveable { mutableStateOf(today.withDayOfMonth(1).toEpochDay()) }
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val month = remember(monthAnchorEpochDay) { YearMonth.from(LocalDate.ofEpochDay(monthAnchorEpochDay)) }
    val selectedDate = remember(selectedEpochDay) { LocalDate.ofEpochDay(selectedEpochDay) }

    LaunchedEffect(monthAnchorEpochDay) {
        if (YearMonth.from(selectedDate) != month) {
            selectedEpochDay = month.atDay(1).toEpochDay()
        }
    }

    val itemsByDate = remember(items, schoolZone) {
        items.groupBy { item ->
            Instant.ofEpochMilli(item.startsAtEpochMillis)
                .atZone(schoolZone)
                .toLocalDate()
        }
    }
    val selectedItems = remember(itemsByDate, selectedDate) {
        itemsByDate[selectedDate].orEmpty().sortedBy { it.startsAtEpochMillis }
    }
    val dayTimeBounds = remember(itemsByDate) {
        itemsByDate.mapValues { (_, dayItems) ->
            val timed = dayItems.filter { !it.isAllDay }
            if (timed.isEmpty()) {
                null
            } else {
                val startsAt = timed.minOf { it.startsAtEpochMillis }
                val endsAt = timed.maxOf { it.endsAtEpochMillis ?: it.startsAtEpochMillis }
                startsAt to endsAt
            }
        }
    }

    val firstDayOfMonth = month.atDay(1)
    val leadingEmpty = firstDayOfMonth.dayOfWeek.value - 1
    val totalCells = leadingEmpty + month.lengthOfMonth()
    val weekRows = (totalCells + 6) / 7
    val weekdayLabels = remember { listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        val previous = month.minusMonths(1).atDay(1)
                        monthAnchorEpochDay = previous.toEpochDay()
                        selectedEpochDay = previous.toEpochDay()
                    }) {
                        Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Vorheriger Monat")
                    }
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = {
                        val next = month.plusMonths(1).atDay(1)
                        monthAnchorEpochDay = next.toEpochDay()
                        selectedEpochDay = next.toEpochDay()
                    }) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "Nächster Monat")
                    }
                }

                TextButton(
                    onClick = {
                        monthAnchorEpochDay = today.withDayOfMonth(1).toEpochDay()
                        selectedEpochDay = today.toEpochDay()
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Heute")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weekdayLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                repeat(weekRows) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(7) { weekdayIndex ->
                            val cellIndex = rowIndex * 7 + weekdayIndex
                            val dayNumber = cellIndex - leadingEmpty + 1
                            if (dayNumber in 1..month.lengthOfMonth()) {
                                val day = month.atDay(dayNumber)
                                CalendarDayCell(
                                    modifier = Modifier.weight(1f),
                                    day = day,
                                    isSelected = day == selectedDate,
                                    isToday = day == today,
                                    itemCount = itemsByDate[day].orEmpty().size,
                                    dayTimeBounds = dayTimeBounds[day],
                                    schoolZone = schoolZone,
                                    onClick = {
                                        selectedEpochDay = day.toEpochDay()
                                        onOpenDay(day)
                                    }
                                )
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(74.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = "Einträge: ${formatExamDateShort(selectedDate.atStartOfDay(schoolZone).toInstant().toEpochMilli())}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
                val dayBounds = dayTimeBounds[selectedDate]
                if (dayBounds != null) {
                    val startText = Instant.ofEpochMilli(dayBounds.first)
                        .atZone(schoolZone)
                        .toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                    val endText = Instant.ofEpochMilli(dayBounds.second)
                        .atZone(schoolZone)
                        .toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                    Text(
                        text = "Tag: $startText - $endText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (selectedItems.any { it.isAllDay }) {
                    Text(
                        text = "Tag: ganztägige Einträge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (selectedItems.isEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "Keine Einträge für den gewählten Tag.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedItems.forEach { item ->
                    CalendarTimelineCard(
                        item = item,
                        schoolZone = schoolZone,
                        onDeleteCustomEvent = onDeleteCustomEvent
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    day: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    itemCount: Int,
    dayTimeBounds: Pair<Long, Long>?,
    schoolZone: ZoneId,
    onClick: () -> Unit
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .height(86.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (itemCount > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "$itemCount",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                val bounds = dayTimeBounds
                val timeText = if (bounds == null) {
                    "ganztägig"
                } else {
                    val start = Instant.ofEpochMilli(bounds.first)
                        .atZone(schoolZone)
                        .toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                    val end = Instant.ofEpochMilli(bounds.second)
                        .atZone(schoolZone)
                        .toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                    "$start-$end"
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DayAgendaDialog(
    date: LocalDate,
    items: List<CalendarTimelineItem>,
    schoolZone: ZoneId,
    onDismiss: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    val timedItems = remember(items) { items.filter { !it.isAllDay } }
    val dayStartEndText = remember(timedItems, items, schoolZone) {
        if (timedItems.isNotEmpty()) {
            val startsAt = timedItems.minOf { it.startsAtEpochMillis }
            val endsAt = timedItems.maxOf { it.endsAtEpochMillis ?: it.startsAtEpochMillis }
            val startText = Instant.ofEpochMilli(startsAt)
                .atZone(schoolZone)
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            val endText = Instant.ofEpochMilli(endsAt)
                .atZone(schoolZone)
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            "Tag: $startText - $endText"
        } else if (items.any { it.isAllDay }) {
            "Tag: ganztägige Einträge"
        } else {
            "Keine Einträge"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN))
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = dayStartEndText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                OutlinedButton(
                    onClick = onAddEvent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Termin hinzufügen")
                }

                if (items.isEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Für diesen Tag gibt es keine Einträge.",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    items.forEach { item ->
                        val manualEventId = if (item.kind == CalendarItemKind.EVENT && item.canDelete) {
                            item.id.removePrefix("event-")
                        } else {
                            null
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCalendarItemTimeLabel(item, schoolZone),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                item.location?.takeIf { it.isNotBlank() }?.let { location ->
                                    Text(
                                        text = "Ort: $location",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (manualEventId != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onEditEvent(manualEventId) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text("Bearbeiten")
                                        }
                                        OutlinedButton(
                                            onClick = { onDeleteEvent(manualEventId) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text("Löschen")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
private fun AddCustomEventDialog(
    initialStartsAtMillis: Long?,
    editingEvent: SchoolEvent?,
    onDismiss: () -> Unit,
    onSave: (SchoolEvent) -> Unit
) {
    val context = LocalContext.current
    val schoolZone = remember { ZoneId.of("Europe/Zurich") }
    val now = remember { System.currentTimeMillis() }
    val initialStart = remember(editingEvent, initialStartsAtMillis, now) {
        editingEvent?.startsAtEpochMillis ?: initialStartsAtMillis ?: now
    }
    val initialDuration = remember(editingEvent) {
        editingEvent?.let { event ->
            ((event.endsAtEpochMillis - event.startsAtEpochMillis) / 60_000L)
                .coerceIn(5L, 720L)
                .toInt()
        } ?: 60
    }
    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var startsAtMillis by rememberSaveable { mutableStateOf(initialStart) }
    var durationMinutesRaw by rememberSaveable { mutableStateOf("60") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(editingEvent?.id, initialStartsAtMillis) {
        title = editingEvent?.title.orEmpty()
        location = editingEvent?.location.orEmpty()
        startsAtMillis = initialStart
        durationMinutesRaw = initialDuration.toString()
        errorMessage = null
    }

    val startDateText = formatExamDateShort(startsAtMillis)
    val startTimeText = Instant.ofEpochMilli(startsAtMillis)
        .atZone(schoolZone)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingEvent != null) "Event bearbeiten" else "Eigenes Event hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Titel") }
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Ort (optional)") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = startsAtMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val updated = Calendar.getInstance().apply {
                                        timeInMillis = startsAtMillis
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    startsAtMillis = updated.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startDateText)
                    }

                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = startsAtMillis }
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val updated = Calendar.getInstance().apply {
                                        timeInMillis = startsAtMillis
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    startsAtMillis = updated.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startTimeText)
                    }
                }

                OutlinedTextField(
                    value = durationMinutesRaw,
                    onValueChange = { durationMinutesRaw = it.filter { ch -> ch.isDigit() }.take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = { Text("Dauer in Minuten") }
                )

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedTitle = title.trim()
                    val durationMinutes = durationMinutesRaw.toIntOrNull()

                    if (normalizedTitle.isBlank()) {
                        errorMessage = "Bitte einen Titel eingeben."
                        return@TextButton
                    }
                    if (durationMinutes == null || durationMinutes !in 5..720) {
                        errorMessage = "Dauer muss zwischen 5 und 720 Minuten liegen."
                        return@TextButton
                    }
                    val createdEvent = buildSingleCustomEvent(
                        eventId = editingEvent?.id,
                        title = normalizedTitle,
                        location = location.trim().takeIf { it.isNotBlank() },
                        startsAtEpochMillis = startsAtMillis,
                        durationMinutes = durationMinutes,
                        schoolZone = schoolZone
                    )
                    onSave(createdEvent)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

private fun buildSingleCustomEvent(
    eventId: String?,
    title: String,
    location: String?,
    startsAtEpochMillis: Long,
    durationMinutes: Int,
    schoolZone: ZoneId
): SchoolEvent {
    val baseStart = Instant.ofEpochMilli(startsAtEpochMillis).atZone(schoolZone)
    val seed = System.currentTimeMillis()
    val end = baseStart.plusMinutes(durationMinutes.toLong())
    return SchoolEvent(
        id = eventId ?: "manual-event:$seed",
        title = title,
        type = SchoolEventType.OTHER,
        location = location,
        startsAtEpochMillis = baseStart.toInstant().toEpochMilli(),
        endsAtEpochMillis = end.toInstant().toEpochMilli(),
        isAllDay = false,
        source = "manual"
    )
}

@Composable
private fun CalendarTimelineCard(
    item: CalendarTimelineItem,
    schoolZone: ZoneId,
    onDeleteCustomEvent: (String) -> Unit
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.canDelete) {
                    IconButton(
                        onClick = { onDeleteCustomEvent(item.id.removePrefix("event-")) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Event löschen"
                        )
                    }
                }
            }

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val timeLabel = formatCalendarItemTimeLabel(item, schoolZone)
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

private fun formatCalendarItemTimeLabel(item: CalendarTimelineItem, schoolZone: ZoneId): String {
    return when {
        item.isAllDay -> formatAllDayLabel(
            startsAtMillis = item.startsAtEpochMillis,
            endsAtMillis = item.endsAtEpochMillis
                ?: (item.startsAtEpochMillis + 24L * 60L * 60L * 1000L),
            zoneId = schoolZone
        )
        item.endsAtEpochMillis != null -> "${formatExamDateShort(item.startsAtEpochMillis)} · ${formatTimeRange(item.startsAtEpochMillis, item.endsAtEpochMillis)}"
        else -> formatExamDateShort(item.startsAtEpochMillis)
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
