package com.andrin.examcountdown.ui.tabs

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.ui.ExamPresentation
import com.andrin.examcountdown.util.CollisionSource
import com.andrin.examcountdown.util.ExamCollision
import com.andrin.examcountdown.util.SchoolTime
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatExamDate
import com.andrin.examcountdown.util.formatExamDateShort
import com.andrin.examcountdown.util.formatTimeRange
import com.andrin.examcountdown.util.formatReminderDateTime
import com.andrin.examcountdown.util.formatReminderLeadTime
import com.andrin.examcountdown.ui.theme.AppDimens

private data class AgendaPreviewEntry(
    val id: String,
    val kind: String,
    val title: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val location: String?,
    val isAllDay: Boolean
)

private fun buildAgendaPreviewEntries(
    lessons: List<TimetableLesson>,
    events: List<SchoolEvent>,
    nowMillis: Long
): List<AgendaPreviewEntry> {
    return (lessons.map { lesson ->
        AgendaPreviewEntry(
            id = "lesson:${lesson.id}",
            kind = "Stundenplan",
            title = lesson.title,
            startsAtEpochMillis = lesson.startsAtEpochMillis,
            endsAtEpochMillis = lesson.endsAtEpochMillis,
            location = lesson.location,
            isAllDay = false
        )
    } + events.map { event ->
        AgendaPreviewEntry(
            id = "event:${event.id}",
            kind = "Agenda",
            title = event.title,
            startsAtEpochMillis = event.startsAtEpochMillis,
            endsAtEpochMillis = event.endsAtEpochMillis,
            location = event.location,
            isAllDay = event.isAllDay
        )
    })
        .filter { entry -> entry.endsAtEpochMillis >= nowMillis }
        .sortedBy(AgendaPreviewEntry::startsAtEpochMillis)
        .take(4)
}

@Composable
internal fun AgendaPreview(
    lessons: List<TimetableLesson>,
    events: List<SchoolEvent>,
    modifier: Modifier = Modifier
) {
    val entries = remember(lessons, events) {
        buildAgendaPreviewEntries(
            lessons = lessons,
            events = events,
            nowMillis = SchoolTime.nowMillis()
        )
    }
    if (entries.isEmpty()) return

    val sourceLabel = when {
        lessons.isNotEmpty() && events.isNotEmpty() -> "Stundenplan und Agenda"
        lessons.isNotEmpty() -> "Stundenplan"
        else -> "Agenda"
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("exam-agenda-preview"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.cardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Als Nächstes",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatExamDateShort(entry.startsAtEpochMillis)
                                    .substringBefore(' '),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (entry.isAllDay) "ganztags" else formatTimeRange(
                                    entry.startsAtEpochMillis,
                                    entry.endsAtEpochMillis
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.kind,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        entry.location?.takeIf { it.isNotBlank() }?.let { location ->
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorkloadSummary(
    exams: List<Exam>,
    visibleCount: Int
) {
    val now = SchoolTime.nowMillis()
    val in7Days = now + 7L * 24L * 60L * 60L * 1000L
    val in30Days = now + 30L * 24L * 60L * 60L * 1000L
    val examsNext7 = exams.count { it.startsAtEpochMillis in now..in7Days }
    val examsNext30 = exams.count { it.startsAtEpochMillis in now..in30Days }
    val subjectCount = exams.mapNotNull { it.subject?.trim()?.takeIf(String::isNotBlank) }.distinct().size

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Überblick",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            val counts = listOf(
                "Sichtbar" to visibleCount,
                "7 Tage" to examsNext7,
                "30 Tage" to examsNext30
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.4f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        counts.forEach { (label, count) ->
                            InsightPill(label, count.toString(), Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        counts.forEach { (label, count) ->
                            InsightPill(label, count.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
            Text(
                text = "Fächer mit Prüfungen: $subjectCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SetupGuideCard(
    examCount: Int,
    hasIcalUrl: Boolean,
    hasSyncedOnce: Boolean,
    lastSyncError: String?,
    shouldSuggestLinkRepair: Boolean,
    onOpenIcalImport: () -> Unit,
    onRefreshNow: () -> Unit,
    onOpenHelp: () -> Unit,
    onHide: () -> Unit
) {
    val actionText = when {
        !hasIcalUrl -> "Kalender verbinden"
        shouldSuggestLinkRepair -> "Link reparieren"
        else -> "Jetzt synchronisieren"
    }
    val statusText = when {
        !hasIcalUrl -> "Verbinde zuerst deinen iCal-Kalender."
        shouldSuggestLinkRepair -> "Der gespeicherte Link ist ungültig oder abgelaufen."
        !hasSyncedOnce -> "Starte den ersten Sync mit \"Jetzt synchronisieren\"."
        examCount == 0 -> "Sync war erfolgreich, aber es wurden noch keine Prüfungen gefunden."
        !lastSyncError.isNullOrBlank() -> "Beim letzten Sync gab es ein Problem."
        else -> "Alles bereit. Deine Prüfungen sind aktuell."
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Start-Hilfe",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SetupStatusPill(
                    label = if (hasIcalUrl) "Kalender verbunden" else "Kalender fehlt",
                    ok = hasIcalUrl
                )
                SetupStatusPill(
                    label = if (hasSyncedOnce) "Sync erledigt" else "Noch kein Sync",
                    ok = hasSyncedOnce
                )
                SetupStatusPill(
                    label = "$examCount Prüfungen",
                    ok = examCount > 0
                )
            }

            if (!lastSyncError.isNullOrBlank()) {
                Text(
                    text = lastSyncError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val primaryAction = if (!hasIcalUrl || shouldSuggestLinkRepair) {
                    onOpenIcalImport
                } else {
                    onRefreshNow
                }
                if (maxWidth < 320.dp || LocalDensity.current.fontScale >= 1.3f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = primaryAction, modifier = Modifier.fillMaxWidth()) {
                            Text(actionText)
                        }
                        OutlinedButton(onClick = onOpenHelp, modifier = Modifier.fillMaxWidth()) {
                            Text("Hilfe")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = primaryAction, modifier = Modifier.weight(1f)) {
                            Text(actionText)
                        }
                        OutlinedButton(onClick = onOpenHelp, modifier = Modifier.weight(1f)) {
                            Text("Hilfe")
                        }
                    }
                }
            }
            TextButton(
                onClick = onHide,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Nicht mehr anzeigen")
            }
        }
    }
}

@Composable
private fun SetupStatusPill(label: String, ok: Boolean) {
    val bg = if (ok) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (ok) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.small,
        tonalElevation = if (ok) 1.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

@Composable
private fun InsightPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
internal fun ExamCollisionOverviewCard(
    collisionMap: Map<String, List<ExamCollision>>
) {
    val totalCount = remember(collisionMap) { collisionMap.values.sumOf { it.size } }
    val entries = remember(collisionMap) {
        collisionMap.values.flatten()
            .sortedBy { it.examStartsAtEpochMillis }
            .take(5)
    }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Kollisionen erkannt",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "$totalCount Kollisionen erkannt. Prüfe betroffene Prüfungen unten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            entries.forEach { collision ->
                val sourceText = when (collision.source) {
                    CollisionSource.LESSON -> "Lektion"
                    CollisionSource.EVENT -> "Event"
                }
                Text(
                    text = "• ${collision.examTitle} ↔ $sourceText: ${collision.sourceTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
internal fun ExamSearchAndFilterCard(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedSubject: String,
    subjects: List<String>,
    onSubjectSelected: (String) -> Unit,
    selectedWindow: ExamWindowFilter,
    onWindowSelected: (ExamWindowFilter) -> Unit,
    simpleModeEnabled: Boolean,
    showSortOptions: Boolean,
    selectedSortMode: ExamSortMode,
    onSortModeSelected: (ExamSortMode) -> Unit
) {
    var showExtendedFilters by rememberSaveable(simpleModeEnabled) { mutableStateOf(!simpleModeEnabled) }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Prüfungen suchen") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Suche löschen"
                            )
                        }
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Zeitraum",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (simpleModeEnabled) {
                    TextButton(onClick = { showExtendedFilters = !showExtendedFilters }) {
                        Text(if (showExtendedFilters) "Weniger Filter" else "Weitere Filter")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExamWindowFilter.entries.forEach { window ->
                    FilterChip(
                        selected = selectedWindow == window,
                        onClick = { onWindowSelected(window) },
                        label = { Text(window.title) },
                        colors = chipColors
                    )
                }
            }
            val showSubjectFilters = subjects.size > 1 && (!simpleModeEnabled || showExtendedFilters)
            if (showSubjectFilters) {
                Text(
                    text = "Fächer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subject ->
                        FilterChip(
                            selected = selectedSubject == subject,
                            onClick = { onSubjectSelected(subject) },
                            label = { Text(subject) },
                            colors = chipColors
                        )
                    }
                }
            }
            if (showSortOptions && (!simpleModeEnabled || showExtendedFilters)) {
                Text(
                    text = "Sortierung",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExamSortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedSortMode == mode,
                            onClick = { onSortModeSelected(mode) },
                            label = { Text(mode.title) },
                            colors = chipColors
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NoExamResultsCard(
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam-no-results"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Keine Prüfungen für diesen Filter.",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Passe Suche oder Zeitraum an oder setze die Filter zurück.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onClearFilters) {
                Text("Filter zurücksetzen")
            }
        }
    }
}


@Composable
internal fun ExamDetailsDialog(
    exam: Exam,
    presentation: ExamPresentation,
    collisions: List<ExamCollision>,
    onDismiss: () -> Unit,
    onPlanStudy: () -> Unit
) {
    val isPast = exam.startsAtEpochMillis < SchoolTime.nowMillis()
    val statusContainer = if (isPast) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val statusContent = if (isPast) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Prüfungsdetails",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = AppDimens.dialogMaxHeightLarge)
                    .verticalScroll(rememberScrollState())
                    .testTag("exam-details-dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = statusContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (isPast) "Abgeschlossen" else "Bevorstehend",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = statusContent
                    )
                }
                presentation.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                    ExamDetailRow(
                        icon = Icons.Outlined.School,
                        label = "Fach",
                        value = subject
                    )
                }
                ExamDetailRow(
                    icon = Icons.Outlined.CalendarToday,
                    label = "Termin",
                    value = formatExamDate(exam.startsAtEpochMillis)
                )
                ExamDetailRow(
                    icon = Icons.Outlined.Schedule,
                    label = "Countdown",
                    value = formatCountdown(exam.startsAtEpochMillis)
                )
                exam.location?.trim()?.takeIf { it.isNotBlank() }?.let { location ->
                    ExamDetailRow(
                        icon = Icons.Outlined.LocationOn,
                        label = "Ort",
                        value = location
                    )
                }
                buildExamReminderText(exam)?.let { reminder ->
                    ExamDetailRow(
                        icon = Icons.Outlined.NotificationsActive,
                        label = "Erinnerung",
                        value = reminder.removePrefix("Erinnerung: ")
                    )
                }
                if (collisions.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (collisions.size == 1) {
                                    "Terminüberschneidung"
                                } else {
                                    "${collisions.size} Terminüberschneidungen"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            collisions.take(3).forEach { collision ->
                                Text(
                                    text = "${collisionSourceLabel(collision.source)}: ${collision.sourceTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onPlanStudy) {
                Text("Lern-Sessions planen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
private fun ExamDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun CountdownHero(
    exam: Exam,
    presentation: ExamPresentation,
    onOpenDetails: () -> Unit,
    onPlanStudy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam-countdown-hero"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Nächste Prüfung",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
                presentation.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Fach: $subject",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = formatExamDate(exam.startsAtEpochMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = formatCountdown(exam.startsAtEpochMillis),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                ExamActionButtons(presentation.title, onOpenDetails, onPlanStudy, onDelete)
            }
        }
    }
}

@Composable
internal fun SyncIssueCard(
    errorText: String,
    showRepairAction: Boolean,
    onRetryNow: () -> Unit,
    onRepairLink: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam-sync-error"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Synchronisierung braucht Aufmerksamkeit",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 420.dp || LocalDensity.current.fontScale >= 1.3f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRetryNow, modifier = Modifier.fillMaxWidth()) {
                            Text("Erneut versuchen")
                        }
                        if (showRepairAction) {
                            OutlinedButton(onClick = onRepairLink, modifier = Modifier.fillMaxWidth()) {
                                Text("Link reparieren")
                            }
                        }
                        OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) {
                            Text("Diagnose öffnen")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRetryNow) { Text("Erneut versuchen") }
                        if (showRepairAction) {
                            OutlinedButton(onClick = onRepairLink) { Text("Link reparieren") }
                        }
                        OutlinedButton(onClick = onOpenDiagnostics) { Text("Diagnose öffnen") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyState(
    onAddClick: () -> Unit,
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
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Noch keine Prüfungen geplant",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Füge jetzt deine erste Prüfung hinzu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onAddClick) {
                    Text("Prüfung hinzufügen")
                }
            }
        }
    }
}

@Composable
internal fun ExamCard(
    exam: Exam,
    presentation: ExamPresentation,
    collisions: List<ExamCollision>,
    onOpenDetails: () -> Unit,
    onPlanStudy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presentation.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Fach: $subject",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            if (collisions.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (collisions.size == 1) {
                            "Kollision mit ${collisionSourceLabel(collisions.first().source)}"
                        } else {
                            "${collisions.size} Kollisionen"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text(
                text = presentation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = formatExamDate(exam.startsAtEpochMillis),
                style = MaterialTheme.typography.bodyMedium
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = formatCountdown(exam.startsAtEpochMillis),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            exam.location?.let { location ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(text = location, style = MaterialTheme.typography.bodyMedium)
                }
            }

            val reminderText = buildExamReminderText(exam)

            reminderText?.let { text ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (collisions.isNotEmpty()) {
                collisions.take(2).forEach { collision ->
                    val source = collisionSourceLabel(collision.source)
                    Text(
                        text = "Kollision $source: ${collision.sourceTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            ExamActionButtons(presentation.title, onOpenDetails, onPlanStudy, onDelete)
        }
    }
}

@Composable
private fun ExamActionButtons(
    title: String,
    onOpenDetails: () -> Unit,
    onPlanStudy: () -> Unit,
    onDelete: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenDetails, modifier = Modifier.fillMaxWidth()) {
                    Text("Details")
                }
                Button(onClick = onPlanStudy, modifier = Modifier.fillMaxWidth()) {
                    Text("Lernen planen")
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Prüfung löschen")
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onOpenDetails) { Text("Details") }
                Button(onClick = onPlanStudy) { Text("Lernen planen") }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Prüfung löschen: $title"
                    )
                }
            }
        }
    }
}

private fun buildExamReminderText(exam: Exam): String? {
    return when {
        exam.reminderAtEpochMillis != null && exam.reminderLeadTimesMinutes.isNotEmpty() -> {
            val leads = exam.reminderLeadTimesMinutes
                .take(3)
                .joinToString(", ") { formatReminderLeadTime(it) }
            "Erinnerung: fix ${formatReminderDateTime(exam.reminderAtEpochMillis)} + $leads"
        }
        exam.reminderAtEpochMillis != null -> {
            "Erinnerung: ${formatReminderDateTime(exam.reminderAtEpochMillis)}"
        }
        exam.reminderLeadTimesMinutes.isNotEmpty() -> {
            val leads = exam.reminderLeadTimesMinutes
                .take(3)
                .joinToString(", ") { formatReminderLeadTime(it) }
            val suffix = if (exam.reminderLeadTimesMinutes.size > 3) ", ..." else ""
            "Erinnerung: $leads$suffix"
        }
        exam.reminderMinutesBefore != null -> {
            "Erinnerung: ${formatReminderLeadTime(exam.reminderMinutesBefore)}"
        }
        else -> null
    }
}

private fun collisionSourceLabel(source: CollisionSource): String {
    return when (source) {
        CollisionSource.LESSON -> "Lektion"
        CollisionSource.EVENT -> "Event"
    }
}
