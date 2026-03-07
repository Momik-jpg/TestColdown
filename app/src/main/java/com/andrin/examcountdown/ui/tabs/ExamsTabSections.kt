package com.andrin.examcountdown.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.Exam
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightPill(
                    label = "Sichtbar",
                    value = visibleCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                InsightPill(
                    label = "7 Tage",
                    value = examsNext7.toString(),
                    modifier = Modifier.weight(1f)
                )
                InsightPill(
                    label = "30 Tage",
                    value = examsNext30.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Fächer mit Prüfungen: $subjectCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = if (!hasIcalUrl || shouldSuggestLinkRepair) onOpenIcalImport else onRefreshNow,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(actionText)
                }
                OutlinedButton(
                    onClick = onOpenHelp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hilfe")
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Suche") },
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
                        Text(if (showExtendedFilters) "Weniger" else "Mehr")
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
    onPlanStudy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nächste Prüfung",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = onPlanStudy,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
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
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            contentColor = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
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
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
