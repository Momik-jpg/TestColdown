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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
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

private enum class CalendarWindowFilter(val title: String, val maxDaysAhead: Int?) {
    TODAY("Heute", 1),
    NEXT_7("7 Tage", 7),
    NEXT_30("30 Tage", 30),
    ALL("Alle", null)
}

private enum class AgendaLayoutMode(val title: String) {
    LIST("Liste"),
    MONTH("Kalender")
}

private enum class CustomEventRepeat(val title: String) {
    NONE("Einmalig"),
    DAILY("Täglich"),
    WEEKLY("Wöchentlich"),
    MONTHLY("Monatlich")
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
    onDeleteCustomEvent: (String) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sourceFilter by rememberSaveable { mutableStateOf(CalendarSourceFilter.ALL) }
    var windowFilter by rememberSaveable { mutableStateOf(CalendarWindowFilter.NEXT_30) }
    var layoutMode by rememberSaveable { mutableStateOf(AgendaLayoutMode.MONTH) }
    var showAddCustomEventDialog by rememberSaveable { mutableStateOf(false) }
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

    if (showAddCustomEventDialog) {
        AddCustomEventDialog(
            onDismiss = { showAddCustomEventDialog = false },
            onSave = { createdEvents ->
                showAddCustomEventDialog = false
                onAddCustomEvents(createdEvents)
            }
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddCustomEventDialog = true },
                            modifier = Modifier.weight(1f)
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Event-Import aktivieren")
                            }
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
                            windowFilter = CalendarWindowFilter.NEXT_30
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
                    onDeleteCustomEvent = onDeleteCustomEvent
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
    onDeleteCustomEvent: (String) -> Unit
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
                                    onClick = { selectedEpochDay = day.toEpochDay() }
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
            Text(
                text = "Einträge: ${formatExamDateShort(selectedDate.atStartOfDay(schoolZone).toInstant().toEpochMilli())}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
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
            .height(74.dp)
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
            }
        }
    }
}

@Composable
private fun AddCustomEventDialog(
    onDismiss: () -> Unit,
    onSave: (List<SchoolEvent>) -> Unit
) {
    val context = LocalContext.current
    val schoolZone = remember { ZoneId.of("Europe/Zurich") }
    val now = remember { System.currentTimeMillis() }
    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var startsAtMillis by rememberSaveable { mutableStateOf(now) }
    var durationMinutesRaw by rememberSaveable { mutableStateOf("60") }
    var sessionsRaw by rememberSaveable { mutableStateOf("1") }
    var repeat by rememberSaveable { mutableStateOf(CustomEventRepeat.NONE) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val startDateText = formatExamDateShort(startsAtMillis)
    val startTimeText = Instant.ofEpochMilli(startsAtMillis)
        .atZone(schoolZone)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eigenes Event hinzufügen") },
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
                OutlinedTextField(
                    value = sessionsRaw,
                    onValueChange = { sessionsRaw = it.filter { ch -> ch.isDigit() }.take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = { Text("Anzahl Sessions") }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomEventRepeat.entries.forEach { option ->
                        FilterChip(
                            selected = repeat == option,
                            onClick = { repeat = option },
                            label = { Text(option.title) }
                        )
                    }
                }

                Text(
                    text = "Sessions + Wiederholung helfen beim Lernplan (z. B. 6 Sessions wöchentlich).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    val sessions = sessionsRaw.toIntOrNull()

                    if (normalizedTitle.isBlank()) {
                        errorMessage = "Bitte einen Titel eingeben."
                        return@TextButton
                    }
                    if (durationMinutes == null || durationMinutes !in 5..720) {
                        errorMessage = "Dauer muss zwischen 5 und 720 Minuten liegen."
                        return@TextButton
                    }
                    if (sessions == null || sessions !in 1..60) {
                        errorMessage = "Sessions müssen zwischen 1 und 60 liegen."
                        return@TextButton
                    }
                    if (sessions > 1 && repeat == CustomEventRepeat.NONE) {
                        errorMessage = "Für mehrere Sessions bitte eine Wiederholung wählen."
                        return@TextButton
                    }

                    val createdEvents = buildCustomEventSeries(
                        title = normalizedTitle,
                        location = location.trim().takeIf { it.isNotBlank() },
                        startsAtEpochMillis = startsAtMillis,
                        durationMinutes = durationMinutes,
                        sessions = sessions,
                        repeat = repeat,
                        schoolZone = schoolZone
                    )
                    onSave(createdEvents)
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

private fun buildCustomEventSeries(
    title: String,
    location: String?,
    startsAtEpochMillis: Long,
    durationMinutes: Int,
    sessions: Int,
    repeat: CustomEventRepeat,
    schoolZone: ZoneId
): List<SchoolEvent> {
    val baseStart = Instant.ofEpochMilli(startsAtEpochMillis).atZone(schoolZone)
    val seed = System.currentTimeMillis()
    return (0 until sessions).map { index ->
        val start = when (repeat) {
            CustomEventRepeat.NONE -> baseStart
            CustomEventRepeat.DAILY -> baseStart.plusDays(index.toLong())
            CustomEventRepeat.WEEKLY -> baseStart.plusWeeks(index.toLong())
            CustomEventRepeat.MONTHLY -> baseStart.plusMonths(index.toLong())
        }
        val end = start.plusMinutes(durationMinutes.toLong())
        val sessionSuffix = if (sessions > 1) " (${index + 1}/$sessions)" else ""
        SchoolEvent(
            id = "manual-event:$seed:$index",
            title = "$title$sessionSuffix",
            type = SchoolEventType.OTHER,
            location = location,
            startsAtEpochMillis = start.toInstant().toEpochMilli(),
            endsAtEpochMillis = end.toInstant().toEpochMilli(),
            isAllDay = false,
            source = "manual"
        )
    }
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
