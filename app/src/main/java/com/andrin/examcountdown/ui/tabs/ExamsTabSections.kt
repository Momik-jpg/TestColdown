package com.andrin.examcountdown.ui.tabs

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.andrin.examcountdown.ui.AppFilterChip as FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.ui.AppTextField
import com.andrin.examcountdown.ui.ActiveFilterChip
import com.andrin.examcountdown.ui.AdaptiveFieldPair
import com.andrin.examcountdown.ui.FilterControls
import com.andrin.examcountdown.R
import com.andrin.examcountdown.ui.ExamPresentation
import com.andrin.examcountdown.util.CollisionSource
import com.andrin.examcountdown.util.ExamCollision
import com.andrin.examcountdown.util.SchoolTime
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatExamDate
import com.andrin.examcountdown.util.formatReminderDateTime
import com.andrin.examcountdown.util.formatReminderLeadTime

@Composable
internal fun ExamInsightsCard(
    exams: List<Exam>,
    visibleCount: Int,
    now: Long
) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Überblick",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val metrics = listOf("Sichtbar" to visibleCount, "7 Tage" to examsNext7, "30 Tage" to examsNext30)
                if (maxWidth / LocalDensity.current.fontScale < 240.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        metrics.forEach { (label, value) ->
                            InsightPill(label, value.toString(), Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        metrics.forEach { (label, value) ->
                            InsightPill(label, value.toString(), Modifier.weight(1f))
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
    val hasError = !lastSyncError.isNullOrBlank() || shouldSuggestLinkRepair
    val actionText = when {
        !hasIcalUrl -> "Kalender verbinden"
        shouldSuggestLinkRepair -> "Link reparieren"
        hasError -> "Erneut versuchen"
        else -> "Jetzt synchronisieren"
    }
    val statusText = when {
        !hasIcalUrl -> "Verbinde deinen iCal-Kalender oder lege Prüfungen manuell an."
        shouldSuggestLinkRepair -> "Der gespeicherte Link ist ungültig oder abgelaufen."
        hasError -> "Die letzte Aktualisierung ist fehlgeschlagen. Gespeicherte Termine bleiben verfügbar."
        !hasSyncedOnce -> "Dein Kalender ist verbunden. Starte jetzt die erste Aktualisierung."
        examCount == 0 -> "Dein Kalender wurde aktualisiert. Es sind noch keine Prüfungen gespeichert."
        else -> "Dein Kalender ist verbunden. Du kannst ihn jederzeit erneut aktualisieren."
    }
    val container = if (hasError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val foreground = if (hasError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = container
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dein Kalenderstart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = foreground,
                modifier = Modifier.semantics { heading() }
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SetupStatusPill(
                    label = if (hasIcalUrl) "Kalender verbunden" else "Kalender fehlt",
                    ok = hasIcalUrl
                )
                SetupStatusPill(
                    label = when {
                        !hasIcalUrl -> "Noch kein Sync"
                        hasError -> "Sync prüfen"
                        hasSyncedOnce -> "Sync erledigt"
                        else -> "Noch kein Sync"
                    },
                    ok = hasIcalUrl && hasSyncedOnce && !hasError
                )
                SetupStatusPill(
                    label = "$examCount Prüfungen",
                    ok = examCount > 0
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = foreground,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            if (!lastSyncError.isNullOrBlank()) {
                Text(
                    text = lastSyncError,
                    style = MaterialTheme.typography.bodySmall,
                    color = foreground
                )
            }

            AdaptiveFieldPair(first = { buttonModifier ->
                Button(
                    onClick = if (!hasIcalUrl || shouldSuggestLinkRepair) onOpenIcalImport else onRefreshNow,
                    modifier = buttonModifier.heightIn(min = 48.dp)
                ) {
                    Text(actionText)
                }
            }, second = { buttonModifier ->
                OutlinedButton(
                    onClick = onOpenHelp,
                    modifier = buttonModifier.heightIn(min = 48.dp)
                ) {
                    Text("Hilfe")
                }
            }, minimumFieldWidth = 168.dp)
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
        modifier = modifier.semantics(mergeDescendants = true) {},
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
    resultCount: Int,
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
    onSortModeSelected: (ExamSortMode) -> Unit,
    onReset: () -> Unit
) {
    var showExtendedFilters by rememberSaveable(simpleModeEnabled) { mutableStateOf(false) }
    val activeCount = listOf(query.isNotBlank(), selectedSubject != SUBJECT_FILTER_ALL,
        selectedWindow != ExamWindowFilter.ALL, selectedSortMode != ExamSortMode.NEXT_FIRST).count { it }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Surface(color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Suchen") },
                placeholder = { Text("Fach, Titel oder Raum") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Suche löschen"
                            )
                        }
                    }
                }
            )
            FilterControls(
                resultLabel = "$resultCount ${if (resultCount == 1) "Prüfung" else "Prüfungen"}",
                activeCount = activeCount,
                expanded = showExtendedFilters,
                onToggle = { showExtendedFilters = !showExtendedFilters },
                onReset = onReset
            )
            if (activeCount > 0) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (query.isNotBlank()) ActiveFilterChip("Suche: ${query.trim()}") { onQueryChange("") }
                    if (selectedSubject != SUBJECT_FILTER_ALL) ActiveFilterChip(selectedSubject) {
                        onSubjectSelected(SUBJECT_FILTER_ALL)
                    }
                    if (selectedWindow != ExamWindowFilter.ALL) ActiveFilterChip(selectedWindow.title) {
                        onWindowSelected(ExamWindowFilter.ALL)
                    }
                    if (selectedSortMode != ExamSortMode.NEXT_FIRST) ActiveFilterChip(selectedSortMode.title) {
                        onSortModeSelected(ExamSortMode.NEXT_FIRST)
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
            val showSubjectFilters = subjects.size > 1 && showExtendedFilters
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
            if (showSortOptions && showExtendedFilters) {
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Keine Prüfungen für diesen Filter.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(onClick = onClearFilters) {
                Text("Filter zurücksetzen")
            }
        }
    }
}

@Composable
internal fun NextExamHero(
    exam: Exam,
    presentation: ExamPresentation,
    now: Long,
    onPlanStudy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nächste Prüfung",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Prüfung löschen"
                        )
                    }
                }
                presentation.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Fach: $subject",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatExamDate(exam.startsAtEpochMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                exam.location?.takeIf { it.isNotBlank() }?.let { room ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.LocationOn, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(room, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(formatCountdown(exam.startsAtEpochMillis, now),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(onClick = onPlanStudy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Lernen planen", modifier = Modifier.padding(start = 8.dp))
                }
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Synchronisierung braucht Aufmerksamkeit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRetryNow) {
                    Text("Erneut versuchen")
                }
                if (showRepairAction) {
                    OutlinedButton(onClick = onRepairLink) {
                        Text("Link reparieren")
                    }
                }
                OutlinedButton(onClick = onOpenDiagnostics) {
                    Text("Diagnose öffnen")
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
        modifier = modifier.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Noch keine Prüfungen geplant",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Füge jetzt deine erste Prüfung hinzu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
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
    now: Long,
    collisions: List<ExamCollision>,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = onPlanStudy,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Lern-Sessions planen"
                    )
                }
                FilledTonalIconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Löschen")
                }
            }

            Text(
                text = formatExamDate(exam.startsAtEpochMillis),
                style = MaterialTheme.typography.bodyMedium
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = formatCountdown(exam.startsAtEpochMillis, now),
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

            val reminderText = when {
                exam.reminderAtEpochMillis != null && exam.reminderLeadTimesMinutes.isNotEmpty() -> {
                    val leads = exam.reminderLeadTimesMinutes
                        .take(3)
                        .joinToString(", ") { formatReminderLeadTime(it) }
                    "Erinnerung: fix ${formatReminderDateTime(exam.reminderAtEpochMillis)} + $leads"
                }
                exam.reminderAtEpochMillis != null -> "Erinnerung: ${formatReminderDateTime(exam.reminderAtEpochMillis)}"
                exam.reminderLeadTimesMinutes.isNotEmpty() -> {
                    val leads = exam.reminderLeadTimesMinutes
                        .take(3)
                        .joinToString(", ") { formatReminderLeadTime(it) }
                    val suffix = if (exam.reminderLeadTimesMinutes.size > 3) ", ..." else ""
                    "Erinnerung: $leads$suffix"
                }
                exam.reminderMinutesBefore != null -> "Erinnerung: ${formatReminderLeadTime(exam.reminderMinutesBefore)}"
                else -> null
            }

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
        }
    }
}


private fun collisionSourceLabel(source: CollisionSource): String {
    return when (source) {
        CollisionSource.LESSON -> "Lektion"
        CollisionSource.EVENT -> "Event"
    }
}
