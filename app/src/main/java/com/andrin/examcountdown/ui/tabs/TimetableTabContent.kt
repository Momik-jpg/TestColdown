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
import com.andrin.examcountdown.util.SchoolTime
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

internal enum class TimetableViewMode(val title: String) {
    LIST("Liste"),
    WEEK("Woche")
}

internal enum class TimetableFilter(val title: String) {
    ALL("Alle"),
    ONLY_TODAY("Nur heute"),
    ONLY_MOVED("Verschoben"),
    ONLY_ROOM_CHANGED("Nur Raum")
}

internal data class TimetableLessonBlock(
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
    val nowMillis = SchoolTime.nowMillis()
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

