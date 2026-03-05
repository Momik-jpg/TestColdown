package com.andrin.examcountdown.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableChangeType
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.ui.tabs.events.TimetableTabEvent
import com.andrin.examcountdown.ui.tabs.state.TimetableTabUiState
import com.andrin.examcountdown.util.formatCompactDay
import com.andrin.examcountdown.util.formatDayHeader
import com.andrin.examcountdown.util.formatExamDateShort
import com.andrin.examcountdown.util.formatTimeRange
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private enum class TimetableViewMode(val title: String) {
    LIST("Liste"),
    WEEK("Woche")
}

private enum class TimetableFilter(val title: String) {
    ALL("Alle"),
    ONLY_TODAY("Nur heute"),
    ONLY_MOVED("Verschoben"),
    ONLY_ROOM_CHANGED("Nur Raum")
}

private data class TimetableLessonBlock(
    val id: String,
    val title: String,
    val location: String?,
    val originalLocation: String?,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val isMoved: Boolean,
    val isLocationChanged: Boolean,
    val isCancelledSlot: Boolean,
    val lessonCount: Int
)
@Composable
private fun TimetableChangesCard(
    changes: List<TimetableChangeEntry>,
    onClear: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Heute geändert",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onClear) {
                    Text("Leeren")
                }
            }

            changes.take(6).forEach { change ->
                TimetableChangeRow(change)
            }
        }
    }
}

@Composable
private fun TimetableChangeRow(change: TimetableChangeEntry) {
    val color = when (change.changeType) {
        TimetableChangeType.MOVED -> MaterialTheme.colorScheme.tertiary
        TimetableChangeType.ROOM_CHANGED -> MaterialTheme.colorScheme.secondary
        TimetableChangeType.ADDED -> MaterialTheme.colorScheme.primary
        TimetableChangeType.REMOVED -> MaterialTheme.colorScheme.error
        TimetableChangeType.TIME_CHANGED -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = change.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatTimetableChangeDescription(change),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimetableChangeDescription(change: TimetableChangeEntry): String {
    val oldText = change.oldValue.orEmpty()
    val newText = change.newValue.orEmpty()

    val oldTime = oldText.toLongOrNull()?.let { formatExamDateShort(it) }
    val newTime = newText.toLongOrNull()?.let { formatExamDateShort(it) }

    return when (change.changeType) {
        TimetableChangeType.MOVED -> "Verschoben: ${oldTime.orEmpty()} -> ${newTime.orEmpty()}".trim()
        TimetableChangeType.TIME_CHANGED -> "Zeit geändert: ${oldTime.orEmpty()} -> ${newTime.orEmpty()}".trim()
        TimetableChangeType.ROOM_CHANGED -> {
            val from = oldText.ifBlank { "unbekannt" }
            val to = newText.ifBlank { "unbekannt" }
            "Raum: $from -> $to"
        }
        TimetableChangeType.ADDED -> "Neue Lektion im Stundenplan"
        TimetableChangeType.REMOVED -> "Lektion entfernt/entfallen"
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TimetableTabContent(
    state: TimetableTabUiState,
    onEvent: (TimetableTabEvent) -> Unit
) {
    val lessons = state.lessons
    val changes = state.changes
    val hasIcalUrl = state.hasIcalUrl
    val onOpenIcalImport = { onEvent(TimetableTabEvent.OpenIcalImport) }
    val onClearChanges = { onEvent(TimetableTabEvent.ClearChanges) }

    if (lessons.isEmpty()) {
        if (changes.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item("today-changes-feed-empty") {
                    TimetableChangesCard(
                        changes = changes.take(6),
                        onClear = onClearChanges
                    )
                }
                item("timetable-empty-state") {
                    TimetableEmptyState(
                        hasIcalUrl = hasIcalUrl,
                        onOpenIcalImport = onOpenIcalImport,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            TimetableEmptyState(
                hasIcalUrl = hasIcalUrl,
                onOpenIcalImport = onOpenIcalImport,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    var selectedFilter by rememberSaveable { mutableStateOf(TimetableFilter.ALL) }
    var viewMode by rememberSaveable { mutableStateOf(TimetableViewMode.LIST) }
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }

    val lessonsWithCancelledSlots = remember(lessons) { addCancelledSlotEntries(lessons) }
    val mergedLessons = remember(lessonsWithCancelledSlots) {
        mergeConsecutiveLessons(lessonsWithCancelledSlots)
    }
    val schoolZone = remember { ZoneId.of("Europe/Zurich") }
    val filteredLessons = remember(mergedLessons, selectedFilter) {
        filterTimetableBlocks(
            lessons = mergedLessons,
            filter = selectedFilter,
            schoolZone = schoolZone
        )
    }
    val grouped = remember(filteredLessons) {
        filteredLessons
            .groupBy { lesson ->
                Instant.ofEpochMilli(lesson.startsAtEpochMillis)
                    .atZone(schoolZone)
                    .toLocalDate()
            }
            .toSortedMap()
    }
    val todayChanges = remember(changes) {
        val today = LocalDate.now(schoolZone)
        changes.filter { entry ->
            Instant.ofEpochMilli(entry.changedAtEpochMillis)
                .atZone(schoolZone)
                .toLocalDate() == today &&
                entry.changeType != TimetableChangeType.ADDED
        }
    }
    val nowMillis = System.currentTimeMillis()
    val activeLesson = remember(mergedLessons, nowMillis) {
        mergedLessons.firstOrNull { lesson ->
            !lesson.isCancelledSlot &&
                nowMillis in lesson.startsAtEpochMillis until lesson.endsAtEpochMillis
        }
    }
    val upcomingLesson = remember(mergedLessons, nowMillis) {
        mergedLessons.firstOrNull { lesson ->
            !lesson.isCancelledSlot &&
                lesson.startsAtEpochMillis > nowMillis
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (todayChanges.isNotEmpty()) {
            item(key = "today-changes-feed") {
                TimetableChangesCard(
                    changes = todayChanges,
                    onClear = onClearChanges
                )
            }
        }

        item(key = "now-next-lesson") {
            TimetableNowNextCard(
                activeLesson = activeLesson,
                upcomingLesson = upcomingLesson
            )
        }

        item(key = "timetable-controls") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ansicht & Filter",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    TimetableSectionLabel("Ansicht")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimetableViewMode.entries.forEach { mode ->
                            TimetableChoiceChip(
                                text = mode.title,
                                selected = viewMode == mode,
                                onClick = { viewMode = mode }
                            )
                        }
                    }

                    TimetableSectionLabel("Filter")
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimetableFilter.entries.forEach { filter ->
                            TimetableChoiceChip(
                                text = filter.title,
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }

                    if (
                        viewMode != TimetableViewMode.LIST ||
                        selectedFilter != TimetableFilter.ALL ||
                        weekOffset != 0
                    ) {
                        TextButton(
                            onClick = {
                                viewMode = TimetableViewMode.LIST
                                selectedFilter = TimetableFilter.ALL
                                weekOffset = 0
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Zurücksetzen")
                        }
                    }
                }
            }
        }

        if (viewMode == TimetableViewMode.WEEK) {
            item(key = "week-grid") {
                TimetableWeekGrid(
                    groupedLessons = grouped,
                    weekOffset = weekOffset,
                    onWeekOffsetChange = { weekOffset = it }
                )
            }
        } else if (grouped.isEmpty()) {
            item(key = "no-filter-results") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Keine Lektionen für den gewählten Filter.",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            grouped.forEach { (date, dayLessons) ->
                item(key = "header-$date") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = formatDayHeader(dayLessons.first().startsAtEpochMillis),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                items(items = dayLessons, key = { it.id }) { lesson ->
                    TimetableLessonCard(lesson = lesson)
                }
            }
        }
    }
}

@Composable
private fun TimetableWeekGrid(
    groupedLessons: Map<LocalDate, List<TimetableLessonBlock>>,
    weekOffset: Int,
    onWeekOffsetChange: (Int) -> Unit
) {
    val schoolZone = remember { ZoneId.of("Europe/Zurich") }
    val weekStart = remember(weekOffset) {
        LocalDate.now(schoolZone)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(weekOffset.toLong())
    }
    val weekdays = remember(weekStart) {
        (0..4).map { index -> weekStart.plusDays(index.toLong()) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onWeekOffsetChange(weekOffset - 1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Vorherige Woche"
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Woche ab ${formatCompactDay(weekStart)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (weekOffset != 0) {
                    TextButton(onClick = { onWeekOffsetChange(0) }) {
                        Text("Heute")
                    }
                }
            }
            IconButton(onClick = { onWeekOffsetChange(weekOffset + 1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Nächste Woche"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            weekdays.forEach { day ->
                val dayLessons = groupedLessons[day].orEmpty()
                Card(
                    modifier = Modifier.width(240.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatCompactDay(day),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (dayLessons.isEmpty()) {
                            Text(
                                text = "Keine Lektionen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            dayLessons.forEach { lesson ->
                                WeekGridLessonRow(lesson)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
private fun TimetableSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun TimetableChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    )
}

@Composable
private fun TimetableNowNextCard(
    activeLesson: TimetableLessonBlock?,
    upcomingLesson: TimetableLessonBlock?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Jetzt & Nächste Lektion",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (activeLesson == null && upcomingLesson == null) {
                Text(
                    text = "Keine kommende Lektion gefunden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                activeLesson?.let { lesson ->
                    TimetableNowNextLessonTile(
                        label = "Jetzt",
                        lesson = lesson,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
                        dateHint = null
                    )
                }

                upcomingLesson?.let { lesson ->
                    val lessonDay = Instant.ofEpochMilli(lesson.startsAtEpochMillis)
                        .atZone(ZoneId.of("Europe/Zurich"))
                        .toLocalDate()
                    TimetableNowNextLessonTile(
                        label = "Nächste",
                        lesson = lesson,
                        accentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
                        dateHint = formatCompactDay(lessonDay)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableNowNextLessonTile(
    label: String,
    lesson: TimetableLessonBlock,
    accentColor: Color,
    containerColor: Color,
    dateHint: String?
) {
    val room = lesson.location?.trim().orEmpty()
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.16f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = formatLessonDisplayTitle(lesson.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            dateHint?.let { day ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (room.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Raum $room",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekGridLessonRow(lesson: TimetableLessonBlock) {
    val titleColor = when {
        lesson.isCancelledSlot -> MaterialTheme.colorScheme.onSurfaceVariant
        lesson.isMoved -> MaterialTheme.colorScheme.tertiary
        lesson.isLocationChanged -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = formatLessonDisplayTitle(lesson.title),
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            textDecoration = if (lesson.isCancelledSlot) TextDecoration.LineThrough else TextDecoration.None
        )
        Text(
            text = formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = if (lesson.isCancelledSlot) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TimetableLessonCard(lesson: TimetableLessonBlock) {
    val isCancelled = lesson.isCancelledSlot
    val nowMillis = System.currentTimeMillis()
    val isCurrent = !isCancelled && nowMillis in lesson.startsAtEpochMillis until lesson.endsAtEpochMillis
    val cardColor = if (isCancelled) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatLessonDisplayTitle(lesson.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCancelled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (isCurrent) {
                    Text(
                        text = "Jetzt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isCancelled && lesson.lessonCount > 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        val lessonLabel = if (lesson.lessonCount == 1) "Lektion" else "Lektionen"
                        Text(
                            text = "${lesson.lessonCount} $lessonLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isCancelled) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Ausfall (verschoben)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (lesson.isMoved) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Verschoben",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!isCancelled && lesson.isLocationChanged) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Raum geändert",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }

            val currentLocation = lesson.location?.trim().orEmpty()
            val previousLocation = lesson.originalLocation?.trim().orEmpty()
            if (currentLocation.isNotBlank() || previousLocation.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    if (!isCancelled && lesson.isLocationChanged && previousLocation.isNotBlank()) {
                        Text(
                            text = previousLocation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Text(
                            text = "  ->  ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentLocation.ifBlank { "unbekannt" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = currentLocation.ifBlank { previousLocation },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }
        }
    }
}

private fun addCancelledSlotEntries(lessons: List<TimetableLesson>): List<TimetableLesson> {
    if (lessons.isEmpty()) return emptyList()

    val placeholders = lessons.mapNotNull { lesson ->
        val originalStart = lesson.originalStartsAtEpochMillis
        val originalEnd = lesson.originalEndsAtEpochMillis
        if (!lesson.isMoved || originalStart == null || originalEnd == null) {
            return@mapNotNull null
        }

        TimetableLesson(
            id = "cancelled:${lesson.id}",
            title = lesson.title,
            location = lesson.originalLocation ?: lesson.location,
            startsAtEpochMillis = originalStart,
            endsAtEpochMillis = originalEnd,
            isCancelledSlot = true
        )
    }

    return (lessons + placeholders)
        .distinctBy { it.id }
        .sortedBy { it.startsAtEpochMillis }
}

private fun mergeConsecutiveLessons(
    lessons: List<TimetableLesson>,
    maxGapMinutes: Long = 20L
): List<TimetableLessonBlock> {
    if (lessons.isEmpty()) return emptyList()

    val maxGapMillis = maxGapMinutes * 60_000L
    val sorted = lessons.sortedBy { it.startsAtEpochMillis }
    val result = mutableListOf<TimetableLessonBlock>()

    var current = TimetableLessonBlock(
        id = sorted.first().id,
        title = sorted.first().title,
        location = sorted.first().location,
        originalLocation = sorted.first().originalLocation,
        startsAtEpochMillis = sorted.first().startsAtEpochMillis,
        endsAtEpochMillis = sorted.first().endsAtEpochMillis,
        isMoved = sorted.first().isMoved,
        isLocationChanged = sorted.first().isLocationChanged,
        isCancelledSlot = sorted.first().isCancelledSlot,
        lessonCount = 1
    )

    sorted.drop(1).forEach { next ->
        val gap = next.startsAtEpochMillis - current.endsAtEpochMillis
        val sameTitle = current.title.equals(next.title, ignoreCase = true)
        val sameLocation = current.location.orEmpty().trim().lowercase() ==
            next.location.orEmpty().trim().lowercase()
        val canMergeType = !current.isMoved &&
            !next.isMoved &&
            !current.isLocationChanged &&
            !next.isLocationChanged &&
            !current.isCancelledSlot &&
            !next.isCancelledSlot

        val shouldMerge = canMergeType && sameTitle && sameLocation && gap in 0..maxGapMillis

        if (shouldMerge) {
            current = current.copy(
                endsAtEpochMillis = maxOf(current.endsAtEpochMillis, next.endsAtEpochMillis),
                isMoved = current.isMoved || next.isMoved,
                isLocationChanged = current.isLocationChanged || next.isLocationChanged,
                isCancelledSlot = current.isCancelledSlot || next.isCancelledSlot,
                lessonCount = current.lessonCount + 1
            )
        } else {
            result += current
            current = TimetableLessonBlock(
                id = next.id,
                title = next.title,
                location = next.location,
                originalLocation = next.originalLocation,
                startsAtEpochMillis = next.startsAtEpochMillis,
                endsAtEpochMillis = next.endsAtEpochMillis,
                isMoved = next.isMoved,
                isLocationChanged = next.isLocationChanged,
                isCancelledSlot = next.isCancelledSlot,
                lessonCount = 1
            )
        }
    }

    result += current
    return result
}

private fun filterTimetableBlocks(
    lessons: List<TimetableLessonBlock>,
    filter: TimetableFilter,
    schoolZone: ZoneId
): List<TimetableLessonBlock> {
    if (lessons.isEmpty()) return emptyList()

    val today = LocalDate.now(schoolZone)
    return lessons.filter { lesson ->
        when (filter) {
            TimetableFilter.ALL -> true
            TimetableFilter.ONLY_TODAY -> {
                val lessonDay = Instant.ofEpochMilli(lesson.startsAtEpochMillis)
                    .atZone(schoolZone)
                    .toLocalDate()
                lessonDay == today
            }
            TimetableFilter.ONLY_MOVED -> lesson.isMoved || lesson.isCancelledSlot
            TimetableFilter.ONLY_ROOM_CHANGED -> lesson.isLocationChanged
        }
    }
}

@Composable
private fun TimetableEmptyState(
    hasIcalUrl: Boolean,
    onOpenIcalImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Noch kein Stundenplan verfügbar",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (hasIcalUrl) {
                        "Tippe oben rechts auf den Pfeil zum Aktualisieren."
                    } else {
                        "Gib deinen iCal-Link einmal ein, er bleibt gespeichert."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onOpenIcalImport) {
                    Text(if (hasIcalUrl) "iCal synchronisieren" else "iCal hinzufügen")
                }
            }
        }
    }
}

@Composable

private fun formatLessonDisplayTitle(raw: String): String {
    var text = raw
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
        if (startsUpper) replacement else replacement.lowercase()
    }
}
