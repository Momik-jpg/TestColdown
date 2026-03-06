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
import com.andrin.examcountdown.util.SchoolTime
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatExamDate
import com.andrin.examcountdown.util.formatReminderDateTime
import com.andrin.examcountdown.util.formatReminderLeadTime

internal const val SUBJECT_FILTER_ALL = "Alle Fächer"

internal enum class ExamWindowFilter(val title: String, val maxDaysAhead: Int?) {
    ALL("Alle", null),
    NEXT_7("7 Tage", 7),
    NEXT_30("30 Tage", 30),
    NEXT_90("90 Tage", 90)
}

internal enum class ExamSortMode(val title: String) {
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
        val now = SchoolTime.nowMillis()
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
        val now = SchoolTime.nowMillis()
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

