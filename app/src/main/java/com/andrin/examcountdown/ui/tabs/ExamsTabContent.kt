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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.domain.usecase.DetectScheduleCollisionsUseCase
import com.andrin.examcountdown.ui.StudyWorldHeader
import com.andrin.examcountdown.ui.ExamPresentation
import kotlinx.coroutines.delay
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

private val ExamFiltersSaver = listSaver<ExamFilters, String>(
    save = { listOf(it.query, it.subject, it.window.name, it.sort.name) },
    restore = { ExamFilters(it[0], it[1], ExamWindowFilter.valueOf(it[2]), ExamSortMode.valueOf(it[3])) }
)

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

    var filters by rememberSaveable(stateSaver = ExamFiltersSaver) { mutableStateOf(ExamFilters()) }
    var now by remember { mutableLongStateOf(SchoolTime.nowMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = SchoolTime.nowMillis()
            delay(30_000)
        }
    }
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
        if (filters.subject !in subjectOptions) {
            filters = filters.copy(subject = SUBJECT_FILTER_ALL)
        }
    }

    val searchDetails = remember(examPresentations) {
        examPresentations.mapValues { (_, info) -> ExamSearchDetails(info.title, info.subject) }
    }
    val filteredExams = remember(exams, searchDetails, filters, now) {
        filterExams(exams, searchDetails, filters, now)
    }
    val nextExam = remember(filteredExams, now) { nextUpcomingExam(filteredExams, now) }
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item("study-world-empty") {
                StudyWorldHeader("Bereit für deine erste Prüfung?", "Kalender importieren oder Prüfung anlegen.", illustrated = true)
            }
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item("study-world") {
            StudyWorldHeader("Deine Prüfungen", "Nächste Prüfung & Lernplan", illustrated = true)
        }
        item {
            ExamSearchAndFilterCard(
                resultCount = filteredExams.size,
                query = filters.query,
                onQueryChange = { filters = filters.copy(query = it) },
                selectedSubject = filters.subject,
                subjects = subjectOptions,
                onSubjectSelected = { filters = filters.copy(subject = it) },
                selectedWindow = filters.window,
                onWindowSelected = { filters = filters.copy(window = it) },
                simpleModeEnabled = simpleModeEnabled,
                showSortOptions = true,
                selectedSortMode = filters.sort,
                onSortModeSelected = { filters = filters.copy(sort = it) },
                onReset = { filters = ExamFilters() }
            )
        }
        nextExam?.let { exam ->
            item("next-exam-${exam.id}") {
                NextExamHero(
                    exam = exam,
                    presentation = examPresentations[exam.id] ?: buildExamPresentation(exam),
                    now = now,
                    onPlanStudy = { onEvent(ExamsTabEvent.PlanStudy(exam)) },
                    onDelete = { onEvent(ExamsTabEvent.DeleteExam(exam)) }
                )
            }
        }
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

        if (!simpleModeEnabled) {
            item {
                ExamInsightsCard(
                    exams = exams,
                    visibleCount = filteredExams.size,
                    now = now
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
        if (filteredExams.isEmpty()) {
            item {
                NoExamResultsCard(
                    onClearFilters = { filters = ExamFilters() }
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
                        now = now,
                        collisions = collisionMap[exam.id].orEmpty(),
                        onPlanStudy = { onEvent(ExamsTabEvent.PlanStudy(exam)) },
                        onDelete = { onEvent(ExamsTabEvent.DeleteExam(exam)) }
                    )
                }
            }
        }
    }
}
