package com.andrin.examcountdown.ui.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.domain.usecase.DetectScheduleCollisionsUseCase
import com.andrin.examcountdown.ui.ExamPresentation
import com.andrin.examcountdown.ui.buildExamPresentation
import com.andrin.examcountdown.ui.isIcalLinkRepairRecommended
import com.andrin.examcountdown.ui.tabs.events.ExamsTabEvent
import com.andrin.examcountdown.ui.tabs.state.ExamsTabUiState
import com.andrin.examcountdown.util.CollisionSource
import com.andrin.examcountdown.util.ExamCollision
import com.andrin.examcountdown.util.CollisionRules
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatExamDate
import com.andrin.examcountdown.util.formatReminderDateTime
import com.andrin.examcountdown.util.formatReminderLeadTime

private const val SUBJECT_FILTER_ALL = "Alle Fächer"

private enum class ExamWindowFilter(val title: String, val maxDaysAhead: Int?) {
    ALL("Alle", null),
    NEXT_7("7 Tage", 7),
    NEXT_30("30 Tage", 30),
    NEXT_90("90 Tage", 90)
}

private enum class ExamSortMode(val title: String) {
    NEXT_FIRST("Nächste"),
    LATEST_FIRST("Späteste"),
    SUBJECT_AZ("Fach A-Z"),
    TITLE_AZ("Titel A-Z")
}
@Composable
fun ExamsTabContent(
    state: ExamsTabUiState,
    onEvent: (ExamsTabEvent) -> Unit
) {
    val exams = state.exams
    val lessons = state.lessons
    val events = state.events
    val showCollisionBadges = state.showCollisionBadges
    val collisionRules = state.collisionRules
    val hasIcalUrl = state.hasIcalUrl
    val hasSyncedOnce = state.hasSyncedOnce
    val lastSyncError = state.lastSyncError
    val simpleModeEnabled = state.simpleModeEnabled
    val showSetupGuideCard = state.showSetupGuideCard
    val detectScheduleCollisions = remember { DetectScheduleCollisionsUseCase() }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSubject by rememberSaveable { mutableStateOf(SUBJECT_FILTER_ALL) }
    var selectedWindow by rememberSaveable { mutableStateOf(ExamWindowFilter.ALL) }
    var selectedSortMode by rememberSaveable { mutableStateOf(ExamSortMode.NEXT_FIRST) }
    val examPresentations = remember(exams) {
        exams.associate { exam -> exam.id to buildExamPresentation(exam) }
    }
    val subjectOptions = remember(examPresentations) {
        val subjects = examPresentations.values.mapNotNull { info ->
            info.subject?.trim()?.takeIf { it.isNotBlank() }
        }
            .distinct()
            .sortedBy { it.lowercase() }
        listOf(SUBJECT_FILTER_ALL) + subjects
    }
    LaunchedEffect(subjectOptions) {
        if (selectedSubject !in subjectOptions) {
            selectedSubject = SUBJECT_FILTER_ALL
        }
    }

    val filteredExams = remember(
        exams,
        examPresentations,
        searchQuery,
        selectedSubject,
        selectedWindow,
        selectedSortMode
    ) {
        val query = searchQuery.trim().lowercase()
        val now = System.currentTimeMillis()
        val windowEnd = selectedWindow.maxDaysAhead?.let { days ->
            now + days * 24L * 60L * 60L * 1000L
        }

        val filtered = exams.filter { exam ->
            val info = examPresentations[exam.id] ?: buildExamPresentation(exam)
            val matchesSubject = selectedSubject == SUBJECT_FILTER_ALL ||
                info.subject?.equals(selectedSubject, ignoreCase = true) == true
            val matchesQuery = query.isBlank() || listOf(
                info.subject.orEmpty(),
                info.title,
                exam.location.orEmpty()
            )
                .joinToString(" ")
                .lowercase()
                .contains(query)
            val matchesWindow = windowEnd == null || exam.startsAtEpochMillis in now..windowEnd
            matchesSubject && matchesQuery && matchesWindow
        }

        when (selectedSortMode) {
            ExamSortMode.NEXT_FIRST -> filtered.sortedBy { it.startsAtEpochMillis }
            ExamSortMode.LATEST_FIRST -> filtered.sortedByDescending { it.startsAtEpochMillis }
            ExamSortMode.SUBJECT_AZ -> filtered.sortedWith(
                compareBy<Exam>(
                    { examPresentations[it.id]?.subject.orEmpty().lowercase() },
                    { examPresentations[it.id]?.title.orEmpty().lowercase() },
                    { it.startsAtEpochMillis }
                )
            )
            ExamSortMode.TITLE_AZ -> filtered.sortedWith(
                compareBy<Exam>(
                    { examPresentations[it.id]?.title.orEmpty().lowercase() },
                    { examPresentations[it.id]?.subject.orEmpty().lowercase() },
                    { it.startsAtEpochMillis }
                )
            )
        }
    }
    val nextExam = remember(filteredExams) {
        val now = System.currentTimeMillis()
        filteredExams.firstOrNull { it.startsAtEpochMillis >= now } ?: filteredExams.firstOrNull()
    }
    val listExams = remember(filteredExams, nextExam) {
        val heroId = nextExam?.id ?: return@remember filteredExams
        filteredExams.filterNot { it.id == heroId }
    }
    val showCollisionBadgesEffective = showCollisionBadges && !simpleModeEnabled
    val collisionMap = remember(
        exams,
        lessons,
        events,
        showCollisionBadgesEffective,
        collisionRules
    ) {
        if (!showCollisionBadgesEffective) {
            emptyMap()
        } else {
            detectScheduleCollisions(
                DetectScheduleCollisionsUseCase.Params(
                    exams = exams,
                    lessons = lessons,
                    events = events,
                    rules = CollisionRules(
                        includeLessonCollisions = collisionRules.includeLessonCollisions,
                        includeEventCollisions = collisionRules.includeEventCollisions,
                        onlyDifferentSubject = collisionRules.onlyDifferentSubject,
                        requireExactTimeOverlap = collisionRules.requireExactTimeOverlap
                    )
                )
            )
                .byExam
        }
    }
    val collisionCount = remember(collisionMap) {
        collisionMap.values.flatten().size
    }
    val showSetupGuide = remember(showSetupGuideCard) {
        showSetupGuideCard
    }
    val suggestLinkRepair = remember(lastSyncError) {
        isIcalLinkRepairRecommended(lastSyncError)
    }
    val onOpenIcalImport = { onEvent(ExamsTabEvent.OpenIcalImport) }
    val onRefreshNow = { onEvent(ExamsTabEvent.RefreshNow) }
    val onOpenHelp = { onEvent(ExamsTabEvent.OpenHelp) }
    val onOpenSyncDiagnostics = { onEvent(ExamsTabEvent.OpenSyncDiagnostics) }
    val onHideSetupGuide = { onEvent(ExamsTabEvent.HideSetupGuide) }
    val onAddClick = { onEvent(ExamsTabEvent.AddExam) }

    if (exams.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showSetupGuide) {
                item("setup-guide-empty") {
                    SetupGuideCard(
                        examCount = exams.size,
                        hasIcalUrl = hasIcalUrl,
                        hasSyncedOnce = hasSyncedOnce,
                        lastSyncError = lastSyncError,
                        shouldSuggestLinkRepair = suggestLinkRepair,
                        onOpenIcalImport = onOpenIcalImport,
                        onRefreshNow = onRefreshNow,
                        onOpenHelp = onOpenHelp,
                        onHide = onHideSetupGuide
                    )
                }
            }
            if (!lastSyncError.isNullOrBlank()) {
                item("sync-issue-empty") {
                    SyncIssueCard(
                        errorText = lastSyncError,
                        showRepairAction = suggestLinkRepair,
                        onRetryNow = onRefreshNow,
                        onRepairLink = onOpenIcalImport,
                        onOpenDiagnostics = onOpenSyncDiagnostics
                    )
                }
            }
            item {
                EmptyState(
                    onAddClick = onAddClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showSetupGuide) {
            item("setup-guide") {
                SetupGuideCard(
                    examCount = exams.size,
                    hasIcalUrl = hasIcalUrl,
                    hasSyncedOnce = hasSyncedOnce,
                    lastSyncError = lastSyncError,
                    shouldSuggestLinkRepair = suggestLinkRepair,
                    onOpenIcalImport = onOpenIcalImport,
                    onRefreshNow = onRefreshNow,
                    onOpenHelp = onOpenHelp,
                    onHide = onHideSetupGuide
                )
            }
        }
        if (!lastSyncError.isNullOrBlank()) {
            item("sync-issue") {
                SyncIssueCard(
                    errorText = lastSyncError,
                    showRepairAction = suggestLinkRepair,
                    onRetryNow = onRefreshNow,
                    onRepairLink = onOpenIcalImport,
                    onOpenDiagnostics = onOpenSyncDiagnostics
                )
            }
        }
        item {
            ExamSearchAndFilterCard(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                selectedSubject = selectedSubject,
                subjects = subjectOptions,
                onSubjectSelected = { selectedSubject = it },
                selectedWindow = selectedWindow,
                onWindowSelected = { selectedWindow = it },
                simpleModeEnabled = simpleModeEnabled,
                showSortOptions = !simpleModeEnabled,
                selectedSortMode = selectedSortMode,
                onSortModeSelected = { selectedSortMode = it }
            )
        }
        if (!simpleModeEnabled) {
            item {
                ExamInsightsCard(
                    exams = exams,
                    visibleCount = filteredExams.size
                )
            }
            if (collisionCount > 0) {
                item {
                    ExamCollisionOverviewCard(
                        collisionMap = collisionMap
                    )
                }
            }
        }
        item {
            nextExam?.let { exam ->
                val info = examPresentations[exam.id] ?: buildExamPresentation(exam)
                NextExamHero(
                    exam = exam,
                    presentation = info,
                    onPlanStudy = { onEvent(ExamsTabEvent.PlanStudy(exam)) },
                    onDelete = { onEvent(ExamsTabEvent.DeleteExam(exam)) }
                )
            }
        }

        if (filteredExams.isEmpty()) {
            item {
                NoExamResultsCard(
                    onClearFilters = {
                        searchQuery = ""
                        selectedSubject = SUBJECT_FILTER_ALL
                    }
                )
            }
        } else {
            if (listExams.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "Keine weiteren Prüfungen im aktuellen Filter.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(items = listExams, key = { it.id }) { exam ->
                    val info = examPresentations[exam.id] ?: buildExamPresentation(exam)
                    ExamCard(
                        exam = exam,
                        presentation = info,
                        collisions = collisionMap[exam.id].orEmpty(),
                        onPlanStudy = { onEvent(ExamsTabEvent.PlanStudy(exam)) },
                        onDelete = { onEvent(ExamsTabEvent.DeleteExam(exam)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamInsightsCard(
    exams: List<Exam>,
    visibleCount: Int
) {
    val now = System.currentTimeMillis()
    val in7Days = now + 7L * 24L * 60L * 60L * 1000L
    val in30Days = now + 30L * 24L * 60L * 60L * 1000L
    val examsNext7 = exams.count { it.startsAtEpochMillis in now..in7Days }
    val examsNext30 = exams.count { it.startsAtEpochMillis in now..in30Days }
    val subjectCount = exams.mapNotNull { it.subject?.trim()?.takeIf(String::isNotBlank) }.distinct().size

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
private fun SetupGuideCard(
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
        !hasIcalUrl -> "Link einfügen"
        shouldSuggestLinkRepair -> "Link reparieren"
        else -> "Aktualisieren"
    }
    val statusText = when {
        !hasIcalUrl -> "Schritt 1: Tippe auf \"Link einfügen\"."
        shouldSuggestLinkRepair -> "Link scheint ungültig/abgelaufen. Bitte reparieren."
        !hasSyncedOnce -> "Schritt 2: Tippe auf \"Aktualisieren\"."
        examCount == 0 -> "Noch keine Prüfungen gefunden. Bitte aktualisieren."
        !lastSyncError.isNullOrBlank() -> "Es gab ein Problem. Tippe auf \"Hilfe\"."
        else -> "Alles bereit. Du kannst die App normal nutzen."
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Erste Schritte",
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
                    label = if (hasIcalUrl) "Link verbunden" else "Link fehlt",
                    ok = hasIcalUrl
                )
                SetupStatusPill(
                    label = if (hasSyncedOnce) "Daten geladen" else "Noch nicht geladen",
                    ok = hasSyncedOnce
                )
                SetupStatusPill(
                    label = "$examCount Prüfungen sichtbar",
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
                    Text("So geht's")
                }
            }
            TextButton(
                onClick = onHide,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Karte ausblenden")
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
        shape = MaterialTheme.shapes.small
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
        shape = MaterialTheme.shapes.medium
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
private fun ExamCollisionOverviewCard(
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
private fun ExamSearchAndFilterCard(
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

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            Text(
                text = "Zeitraum",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        label = { Text(window.title) }
                    )
                }
            }
            if (simpleModeEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (showExtendedFilters) "Weniger Filter" else "Weitere Filter",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showExtendedFilters = !showExtendedFilters }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
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
                            label = { Text(subject) }
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
                            label = { Text(mode.title) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoExamResultsCard(
    onClearFilters: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
private fun NextExamHero(
    exam: Exam,
    presentation: ExamPresentation,
    onPlanStudy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                IconButton(onClick = onPlanStudy) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Lern-Sessions planen",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Prüfung löschen",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
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

@Composable
private fun SyncIssueCard(
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
private fun EmptyState(
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
private fun ExamCard(
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
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onPlanStudy) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Lern-Sessions planen"
                    )
                }
                IconButton(onClick = onDelete) {
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
