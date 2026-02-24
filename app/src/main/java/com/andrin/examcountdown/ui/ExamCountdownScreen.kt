package com.andrin.examcountdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrin.examcountdown.data.SyncStatus
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatCompactDay
import com.andrin.examcountdown.util.formatDayHeader
import com.andrin.examcountdown.util.formatExamDate
import com.andrin.examcountdown.util.formatReminderDateTime
import com.andrin.examcountdown.util.formatReminderLeadTime
import com.andrin.examcountdown.util.formatSyncDateTime
import com.andrin.examcountdown.util.formatTimeRange
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import kotlinx.coroutines.launch

private enum class HomeTab(val title: String) {
    EXAMS("Prüfungen"),
    TIMETABLE("Stundenplan"),
    GRADES("Notenrechner")
}

private enum class TimetableViewMode(val title: String) {
    LIST("Liste"),
    WEEK("Woche")
}

private enum class TimetableFilter(val title: String) {
    ALL("Alle"),
    ONLY_TODAY("Nur heute"),
    ONLY_MOVED("Nur verschoben"),
    ONLY_ROOM_CHANGED("Nur Raum geändert")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCountdownScreen(viewModel: ExamViewModel = viewModel()) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val savedIcalUrl by viewModel.savedIcalUrl.collectAsStateWithLifecycle()
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val onboardingPromptSeen by viewModel.onboardingPromptSeen.collectAsStateWithLifecycle()
    val preferencesLoaded by viewModel.preferencesLoaded.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isDarkMode = isSystemInDarkTheme()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showIcalDialog by rememberSaveable { mutableStateOf(false) }
    var showOnboardingDialog by rememberSaveable { mutableStateOf(false) }
    var iCalUrl by rememberSaveable { mutableStateOf("") }
    var onboardingUrl by rememberSaveable { mutableStateOf("") }
    var onboardingTestedOk by rememberSaveable { mutableStateOf(false) }
    var onboardingInfoMessage by rememberSaveable { mutableStateOf("") }
    var isSyncingIcal by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.EXAMS) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(preferencesLoaded, onboardingDone, onboardingPromptSeen, savedIcalUrl) {
        if (!preferencesLoaded) return@LaunchedEffect

        val shouldShowOnboarding = !onboardingDone &&
            !onboardingPromptSeen &&
            savedIcalUrl.isBlank()

        if (shouldShowOnboarding) {
            showOnboardingDialog = true
            onboardingUrl = savedIcalUrl
            onboardingTestedOk = false
            onboardingInfoMessage = ""
            viewModel.markOnboardingPromptSeen()
        } else {
            showOnboardingDialog = false
        }
    }

    if (showAddDialog) {
        AddExamDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, location, examMillis, reminderAtMillis ->
                viewModel.addExam(title, location, examMillis, reminderAtMillis)
            }
        )
    }

    if (showIcalDialog) {
        IcalImportDialog(
            url = iCalUrl,
            isImporting = isSyncingIcal,
            onUrlChange = { iCalUrl = it },
            onDismiss = {
                if (!isSyncingIcal) showIcalDialog = false
            },
            onImport = {
                isSyncingIcal = true
                viewModel.importFromIcal(iCalUrl) { message ->
                    isSyncingIcal = false
                    showIcalDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        )
    }

    if (showOnboardingDialog) {
        OnboardingDialog(
            url = onboardingUrl,
            statusMessage = onboardingInfoMessage,
            isBusy = isSyncingIcal,
            canFinish = onboardingTestedOk && onboardingUrl.isNotBlank(),
            onUrlChange = { newUrl ->
                onboardingUrl = newUrl
                onboardingTestedOk = false
            },
            onTest = {
                isSyncingIcal = true
                viewModel.testIcalConnection(onboardingUrl) { ok, message ->
                    isSyncingIcal = false
                    onboardingTestedOk = ok
                    onboardingInfoMessage = message
                }
            },
            onFinish = {
                isSyncingIcal = true
                viewModel.completeOnboarding(onboardingUrl) { success, message ->
                    isSyncingIcal = false
                    onboardingInfoMessage = message
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                    if (success) {
                        showOnboardingDialog = false
                    }
                }
            },
            onDismiss = {
                showOnboardingDialog = false
                viewModel.dismissOnboardingPrompt()
            }
        )
    }

    val backgroundBrush = remember(isDarkMode) {
        val colors = if (isDarkMode) {
            listOf(
                Color(0xFF0A1422),
                Color(0xFF132338),
                Color(0xFF0A1422)
            )
        } else {
            listOf(
                Color(0xFFF4F8FF),
                Color(0xFFEAF2FF),
                Color(0xFFE0ECFF)
            )
        }
        Brush.verticalGradient(colors)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Prüfungs-Planer",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        if (selectedTab != HomeTab.GRADES) {
                            IconButton(
                                enabled = !isSyncingIcal,
                                onClick = {
                                    isSyncingIcal = true
                                    viewModel.refreshFromSavedIcal { message ->
                                        isSyncingIcal = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Jetzt aktualisieren"
                                )
                            }
                            IconButton(
                                enabled = !isSyncingIcal,
                                onClick = {
                                    iCalUrl = savedIcalUrl
                                    showIcalDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = "iCal importieren"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Transparent
                ) {
                    HomeTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title) }
                        )
                    }
                }
                SyncStatusStrip(syncStatus = syncStatus)
            }
        },
        floatingActionButton = {
            if (selectedTab == HomeTab.EXAMS) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = "Prüfung hinzufügen")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                HomeTab.EXAMS -> ExamListContent(
                    exams = exams,
                    onAddClick = { showAddDialog = true },
                    onDelete = { examId -> viewModel.deleteExam(examId) }
                )

                HomeTab.TIMETABLE -> TimetableContent(
                    lessons = lessons,
                    hasIcalUrl = savedIcalUrl.isNotBlank(),
                    onOpenIcalImport = {
                        iCalUrl = savedIcalUrl
                        showIcalDialog = true
                    }
                )

                HomeTab.GRADES -> GradeCalculatorScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun IcalImportDialog(
    url: String,
    isImporting: Boolean,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("iCal aus schulNetz importieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text("iCal-URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "iCal-Link einmal eingeben und speichern. Danach reicht oben der Pfeil zum Aktualisieren. Termine/Events werden im Stundenplan ausgeblendet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isImporting && url.isNotBlank(),
                onClick = onImport
            ) {
                Text(if (isImporting) "Import läuft..." else "Importieren")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isImporting,
                onClick = onDismiss
            ) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun OnboardingDialog(
    url: String,
    statusMessage: String,
    isBusy: Boolean,
    canFinish: Boolean,
    onUrlChange: (String) -> Unit,
    onTest: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Erststart: iCal verbinden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1) iCal-Link einfügen  2) Verbindung testen  3) Fertig.\nDer Link bleibt gespeichert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text("schulNetz iCal-URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (canFinish) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = !isBusy && url.isNotBlank(),
                    onClick = onTest
                ) {
                    Text(if (isBusy) "Prüfe..." else "Verbindung testen")
                }
                TextButton(
                    enabled = !isBusy && canFinish,
                    onClick = onFinish
                ) {
                    Text(if (isBusy) "Sync..." else "Fertig")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onDismiss) {
                Text("Später")
            }
        }
    )
}

@Composable
private fun SyncStatusStrip(syncStatus: SyncStatus) {
    val error = syncStatus.lastSyncError
    val text = when {
        !error.isNullOrBlank() -> error
        syncStatus.lastSyncAtMillis != null -> {
            val time = formatSyncDateTime(syncStatus.lastSyncAtMillis)
            "Zuletzt synchronisiert: $time"
        }
        else -> "Noch keine Synchronisierung"
    }

    val containerColor = when {
        !error.isNullOrBlank() -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    }

    val textColor = when {
        !error.isNullOrBlank() -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimetableContent(
    lessons: List<TimetableLesson>,
    hasIcalUrl: Boolean,
    onOpenIcalImport: () -> Unit
) {
    if (lessons.isEmpty()) {
        TimetableEmptyState(
            hasIcalUrl = hasIcalUrl,
            onOpenIcalImport = onOpenIcalImport,
            modifier = Modifier.fillMaxSize()
        )
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "timezone-note") {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Zeiten sind in Schweizer Zeit. Filter und Wochenansicht verfügbar.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item(key = "timetable-controls") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimetableViewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
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
                    TimetableFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.title) }
                        )
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
                    imageVector = Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Vorherige Woche"
                )
            }
            Text(
                text = "Woche ab ${formatCompactDay(weekStart)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { onWeekOffsetChange(weekOffset + 1) }) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowRight,
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
private fun WeekGridLessonRow(lesson: TimetableLessonBlock) {
    val titleColor = when {
        lesson.isCancelledSlot -> MaterialTheme.colorScheme.onSurfaceVariant
        lesson.isMoved -> MaterialTheme.colorScheme.tertiary
        lesson.isLocationChanged -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = lesson.title,
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
private fun TimetableLessonCard(lesson: TimetableLessonBlock) {
    val isCancelled = lesson.isCancelledSlot
    val cardColor = if (isCancelled) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = lesson.title,
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

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        "Gib deinen schulNetz-iCal-Link einmal ein, er bleibt gespeichert."
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
private fun ExamListContent(
    exams: List<Exam>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    val nextExam = exams.firstOrNull()

    if (exams.isEmpty()) {
        EmptyState(
            onAddClick = onAddClick,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            nextExam?.let { exam ->
                NextExamHero(exam = exam)
            }
        }

        items(items = exams, key = { it.id }) { exam ->
            ExamCard(
                exam = exam,
                onDelete = { onDelete(exam.id) }
            )
        }
    }
}

@Composable
private fun NextExamHero(exam: Exam) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10406F)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nächste Prüfung",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFD0E4FF)
            )
            Text(
                text = exam.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatExamDate(exam.startsAtEpochMillis),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE6F0FF)
            )
            Surface(
                color = Color(0xFF2F6EBA),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = formatCountdown(exam.startsAtEpochMillis),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
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
private fun ExamCard(exam: Exam, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = exam.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
                exam.reminderAtEpochMillis != null -> "Erinnerung: ${formatReminderDateTime(exam.reminderAtEpochMillis)}"
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
        }
    }
}

@Composable
private fun AddExamDialog(
    onDismiss: () -> Unit,
    onSave: (String, String?, Long, Long?) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var selectedExamMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis() + 24L * 60L * 60L * 1000L)
    }
    var reminderEnabled by rememberSaveable { mutableStateOf(true) }
    var reminderManuallySet by rememberSaveable { mutableStateOf(false) }
    var selectedReminderMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis() + 24L * 60L * 60L * 1000L - 30L * 60L * 1000L)
    }

    val reminderValidationError = when {
        !reminderEnabled -> null
        selectedReminderMillis <= System.currentTimeMillis() -> "Erinnerungszeit liegt in der Vergangenheit"
        selectedReminderMillis >= selectedExamMillis -> "Erinnerung muss vor Prüfungsbeginn liegen"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Prüfung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Fach / Titel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ort (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        openDateTimePicker(
                            context = context,
                            initialMillis = selectedExamMillis,
                            onPicked = { pickedMillis ->
                                selectedExamMillis = pickedMillis
                                if (!reminderManuallySet) {
                                    selectedReminderMillis = pickedMillis - 30L * 60L * 1000L
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = "Prüfung: ${formatExamDate(selectedExamMillis)}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Benachrichtigung aktiv",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { checked -> reminderEnabled = checked }
                    )
                }

                if (reminderEnabled) {
                    OutlinedButton(
                        onClick = {
                            openDateTimePicker(
                                context = context,
                                initialMillis = selectedReminderMillis,
                                onPicked = { pickedMillis ->
                                    reminderManuallySet = true
                                    selectedReminderMillis = pickedMillis
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = "Erinnerung: ${formatReminderDateTime(selectedReminderMillis)}")
                    }
                }

                reminderValidationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && reminderValidationError == null,
                onClick = {
                    onSave(
                        title,
                        location.ifBlank { null },
                        selectedExamMillis,
                        if (reminderEnabled) selectedReminderMillis else null
                    )
                    onDismiss()
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

private fun openDateTimePicker(
    context: Context,
    initialMillis: Long,
    onPicked: (Long) -> Unit
) {
    val initial = Calendar.getInstance().apply { timeInMillis = initialMillis }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, initial.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, initial.get(Calendar.MINUTE))
            }

            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selected.set(Calendar.MINUTE, minute)
                    selected.set(Calendar.SECOND, 0)
                    selected.set(Calendar.MILLISECOND, 0)
                    onPicked(selected.timeInMillis)
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).show()
}
