package com.andrin.examcountdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.andrin.examcountdown.BuildConfig
import com.andrin.examcountdown.data.CollisionRuleSettings
import com.andrin.examcountdown.data.QuietHoursConfig
import com.andrin.examcountdown.data.SyncStatus
import com.andrin.examcountdown.data.SyncDiagnostics
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableChangeType
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.util.CollisionSource
import com.andrin.examcountdown.util.CollisionRules
import com.andrin.examcountdown.util.ExamCollision
import com.andrin.examcountdown.util.collisionsByExam
import com.andrin.examcountdown.util.detectExamCollisions
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatCompactDay
import com.andrin.examcountdown.util.formatDayHeader
import com.andrin.examcountdown.util.formatExamDate
import com.andrin.examcountdown.util.formatExamDateShort
import com.andrin.examcountdown.util.formatReminderDateTime
import com.andrin.examcountdown.util.formatReminderLeadTime
import com.andrin.examcountdown.util.formatSyncDateTime
import com.andrin.examcountdown.util.formatTimeRange
import java.io.BufferedReader
import java.io.OutputStream
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.coroutines.launch

enum class HomeTab(val title: String, val route: String) {
    EXAMS("Prüfungen", "exams"),
    TIMETABLE("Stundenplan", "timetable"),
    EVENTS("Agenda", "events"),
    GRADES("Notenrechner", "grades");

    companion object {
        fun fromRoute(route: String?): HomeTab {
            return entries.firstOrNull { it.route == route } ?: EXAMS
        }
    }
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

private enum class ExamWindowFilter(val title: String, val maxDaysAhead: Int?) {
    ALL("Alle", null),
    NEXT_7("7 Tage", 7),
    NEXT_30("30 Tage", 30),
    NEXT_90("90 Tage", 90)
}

private enum class ExamSortMode(val title: String) {
    NEXT_FIRST("Nächste zuerst"),
    LATEST_FIRST("Späteste zuerst"),
    SUBJECT_AZ("Fach A-Z"),
    TITLE_AZ("Titel A-Z")
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

private const val SUBJECT_FILTER_ALL = "Alle Fächer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCountdownScreen(
    initialTab: HomeTab = HomeTab.EXAMS,
    viewModel: ExamViewModel = viewModel()
) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val timetableChanges by viewModel.timetableChanges.collectAsStateWithLifecycle()
    val savedIcalUrl by viewModel.savedIcalUrl.collectAsStateWithLifecycle()
    val importEventsEnabled by viewModel.importEventsEnabled.collectAsStateWithLifecycle()
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val onboardingPromptSeen by viewModel.onboardingPromptSeen.collectAsStateWithLifecycle()
    val preferencesLoaded by viewModel.preferencesLoaded.collectAsStateWithLifecycle()
    val quietHours by viewModel.quietHours.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val syncDiagnostics by viewModel.syncDiagnostics.collectAsStateWithLifecycle()
    val syncIntervalMinutes by viewModel.syncIntervalMinutes.collectAsStateWithLifecycle()
    val showSyncStatusStrip by viewModel.showSyncStatusStrip.collectAsStateWithLifecycle()
    val showTimetableTab by viewModel.showTimetableTab.collectAsStateWithLifecycle()
    val showAgendaTab by viewModel.showAgendaTab.collectAsStateWithLifecycle()
    val showExamCollisionBadges by viewModel.showExamCollisionBadges.collectAsStateWithLifecycle()
    val collisionRuleSettings by viewModel.collisionRuleSettings.collectAsStateWithLifecycle()
    val accessibilityModeEnabled by viewModel.accessibilityModeEnabled.collectAsStateWithLifecycle()
    val lastSeenVersion by viewModel.lastSeenVersion.collectAsStateWithLifecycle()
    val isDarkMode = isSystemInDarkTheme()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showIcalDialog by rememberSaveable { mutableStateOf(false) }
    var showOnboardingDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showSyncSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickActionsDialog by rememberSaveable { mutableStateOf(false) }
    var showPersonalizationDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showSyncDiagnosticsDialog by rememberSaveable { mutableStateOf(false) }
    var showChangelogDialog by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var iCalUrl by rememberSaveable { mutableStateOf("") }
    var importEventsToggle by rememberSaveable { mutableStateOf(false) }
    var onboardingUrl by rememberSaveable { mutableStateOf("") }
    var onboardingImportEvents by rememberSaveable { mutableStateOf(false) }
    var onboardingTestedOk by rememberSaveable { mutableStateOf(false) }
    var onboardingInfoMessage by rememberSaveable { mutableStateOf("") }
    var isSyncingIcal by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var pendingCsvExport by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingPdfExport by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    val triggerManualRefresh: () -> Unit = {
        isSyncingIcal = true
        viewModel.refreshFromSavedIcal { message ->
            isSyncingIcal = false
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    LaunchedEffect(importEventsEnabled) {
        importEventsToggle = importEventsEnabled
        onboardingImportEvents = importEventsEnabled
    }

    val visibleTabs = remember(showTimetableTab, showAgendaTab) {
        buildList {
            add(HomeTab.EXAMS)
            if (showTimetableTab) add(HomeTab.TIMETABLE)
            if (showAgendaTab) add(HomeTab.EVENTS)
            add(HomeTab.GRADES)
        }
    }

    LaunchedEffect(visibleTabs, selectedTab) {
        if (selectedTab !in visibleTabs) {
            selectedTab = visibleTabs.firstOrNull() ?: HomeTab.EXAMS
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val backupJson = pendingBackupJson ?: return@rememberLauncherForActivityResult
        pendingBackupJson = null
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(backupJson)
            } ?: error("Datei konnte nicht geschrieben werden.")
        }.onSuccess {
            scope.launch { snackbarHostState.showSnackbar("Backup exportiert.") }
        }.onFailure { throwable ->
            val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
            scope.launch { snackbarHostState.showSnackbar("Backup fehlgeschlagen: $error") }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(stream.reader()).readText()
            } ?: error("Datei konnte nicht gelesen werden.")
        }.onSuccess { raw ->
            viewModel.importBackupJson(raw) { result ->
                result.onSuccess {
                    scope.launch {
                        snackbarHostState.showSnackbar("Backup importiert.")
                    }
                }.onFailure { throwable ->
                    val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
                    scope.launch {
                        snackbarHostState.showSnackbar("Import fehlgeschlagen: $error")
                    }
                }
            }
        }.onFailure { throwable ->
            val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
            scope.launch { snackbarHostState.showSnackbar("Datei konnte nicht gelesen werden: $error") }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val payload = pendingCsvExport ?: return@rememberLauncherForActivityResult
        pendingCsvExport = null
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(payload.second)
            } ?: error("Datei konnte nicht geschrieben werden.")
        }.onSuccess {
            scope.launch { snackbarHostState.showSnackbar("${payload.first} exportiert.") }
        }.onFailure { throwable ->
            val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
            scope.launch { snackbarHostState.showSnackbar("CSV-Export fehlgeschlagen: $error") }
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val payload = pendingPdfExport ?: return@rememberLauncherForActivityResult
        pendingPdfExport = null
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                writeSimplePdf(
                    outputStream = output,
                    title = payload.first,
                    lines = payload.second
                )
            } ?: error("Datei konnte nicht geschrieben werden.")
        }.onSuccess {
            scope.launch { snackbarHostState.showSnackbar("${payload.first} als PDF exportiert.") }
        }.onFailure { throwable ->
            val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
            scope.launch { snackbarHostState.showSnackbar("PDF-Export fehlgeschlagen: $error") }
        }
    }

    LaunchedEffect(preferencesLoaded, onboardingDone, onboardingPromptSeen, savedIcalUrl) {
        if (!preferencesLoaded) return@LaunchedEffect

        val shouldShowOnboarding = !onboardingDone &&
            !onboardingPromptSeen &&
            savedIcalUrl.isBlank()

        if (shouldShowOnboarding) {
            showOnboardingDialog = true
            onboardingUrl = savedIcalUrl
            onboardingImportEvents = importEventsEnabled
            onboardingTestedOk = false
            onboardingInfoMessage = ""
            viewModel.markOnboardingPromptSeen()
        } else {
            showOnboardingDialog = false
        }
    }

    LaunchedEffect(preferencesLoaded, lastSeenVersion) {
        if (!preferencesLoaded) return@LaunchedEffect
        if (lastSeenVersion != BuildConfig.VERSION_NAME) {
            showChangelogDialog = true
        }
    }

    if (showAddDialog) {
        AddExamDialog(
            onDismiss = { showAddDialog = false },
            onSave = { subject, title, location, examMillis, reminderAtMillis, reminderLeadTimes ->
                viewModel.addExam(
                    subject = subject,
                    title = title,
                    location = location,
                    startsAtMillis = examMillis,
                    reminderAtMillis = reminderAtMillis,
                    reminderLeadTimesMinutes = reminderLeadTimes
                )
            }
        )
    }

    if (showIcalDialog) {
        IcalImportDialog(
            url = iCalUrl,
            includeEvents = importEventsToggle,
            isImporting = isSyncingIcal,
            onUrlChange = { iCalUrl = it },
            onIncludeEventsChange = { enabled ->
                importEventsToggle = enabled
                viewModel.setImportEventsEnabled(enabled)
            },
            onDismiss = {
                if (!isSyncingIcal) showIcalDialog = false
            },
            onImport = {
                isSyncingIcal = true
                viewModel.importFromIcal(
                    url = iCalUrl,
                    includeEvents = importEventsToggle
                ) { message ->
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
            includeEvents = onboardingImportEvents,
            statusMessage = onboardingInfoMessage,
            isBusy = isSyncingIcal,
            canFinish = onboardingTestedOk && onboardingUrl.isNotBlank(),
            onUrlChange = { newUrl ->
                onboardingUrl = newUrl
                onboardingTestedOk = false
            },
            onIncludeEventsChange = { onboardingImportEvents = it },
            onTest = {
                isSyncingIcal = true
                viewModel.testIcalConnection(
                    url = onboardingUrl,
                    includeEvents = onboardingImportEvents
                ) { ok, message ->
                    isSyncingIcal = false
                    onboardingTestedOk = ok
                    onboardingInfoMessage = message
                }
            },
            onFinish = {
                isSyncingIcal = true
                viewModel.completeOnboarding(
                    url = onboardingUrl,
                    includeEvents = onboardingImportEvents
                ) { success, message ->
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

    if (showReminderSettingsDialog) {
        ReminderSettingsDialog(
            initialConfig = quietHours,
            syncIntervalMinutes = syncIntervalMinutes,
            onDismiss = { showReminderSettingsDialog = false },
            onSave = { config ->
                viewModel.saveQuietHours(config) { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
                showReminderSettingsDialog = false
            },
            onSendTestNotification = {
                viewModel.sendTestNotification { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onOpenSyncSettings = {
                showReminderSettingsDialog = false
                showSyncSettingsDialog = true
            }
        )
    }

    if (showSyncSettingsDialog) {
        SyncSettingsDialog(
            initialIntervalMinutes = syncIntervalMinutes,
            onDismiss = { showSyncSettingsDialog = false },
            onSave = { minutes ->
                viewModel.saveSyncIntervalMinutes(minutes) { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
                showSyncSettingsDialog = false
            }
        )
    }

    if (showQuickActionsDialog) {
        QuickActionsDialog(
            showSyncStatusStrip = showSyncStatusStrip,
            onDismiss = { showQuickActionsDialog = false },
            onShowSyncStatusStripChange = { enabled ->
                viewModel.setShowSyncStatusStrip(enabled)
            },
            onOpenReminderSettings = {
                showQuickActionsDialog = false
                showReminderSettingsDialog = true
            },
            onOpenSyncSettings = {
                showQuickActionsDialog = false
                showSyncSettingsDialog = true
            },
            onOpenIcalImport = {
                iCalUrl = savedIcalUrl
                importEventsToggle = importEventsEnabled
                showQuickActionsDialog = false
                showIcalDialog = true
            },
            onOpenHelp = {
                showQuickActionsDialog = false
                showHelpDialog = true
            },
            onOpenSyncDiagnostics = {
                showQuickActionsDialog = false
                showSyncDiagnosticsDialog = true
            },
            onOpenExport = {
                showQuickActionsDialog = false
                showExportDialog = true
            },
            onOpenChangelog = {
                showQuickActionsDialog = false
                showChangelogDialog = true
            },
            onOpenPersonalization = {
                showQuickActionsDialog = false
                showPersonalizationDialog = true
            },
            onExportBackup = {
                showQuickActionsDialog = false
                viewModel.exportBackupJson { result ->
                    result.onSuccess { json ->
                        pendingBackupJson = json
                        exportBackupLauncher.launch(
                            "examcountdown-backup-${System.currentTimeMillis()}.json"
                        )
                    }.onFailure { throwable ->
                        val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
                        scope.launch {
                            snackbarHostState.showSnackbar("Backup fehlgeschlagen: $error")
                        }
                    }
                }
            },
            onImportBackup = {
                showQuickActionsDialog = false
                importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
            }
        )
    }

    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showSyncDiagnosticsDialog) {
        SyncDiagnosticsDialog(
            diagnostics = syncDiagnostics,
            syncStatus = syncStatus,
            onDismiss = { showSyncDiagnosticsDialog = false }
        )
    }

    if (showChangelogDialog) {
        ChangelogDialog(
            versionName = BuildConfig.VERSION_NAME,
            entries = changelogEntriesFor(BuildConfig.VERSION_NAME),
            onDismiss = {
                showChangelogDialog = false
                viewModel.setLastSeenVersion(BuildConfig.VERSION_NAME)
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportExamsCsv = {
                pendingCsvExport = "pruefungen" to buildExamsCsv(exams)
                exportCsvLauncher.launch("pruefungen-${System.currentTimeMillis()}.csv")
                showExportDialog = false
            },
            onExportTimetableCsv = {
                pendingCsvExport = "stundenplan" to buildTimetableCsv(lessons)
                exportCsvLauncher.launch("stundenplan-${System.currentTimeMillis()}.csv")
                showExportDialog = false
            },
            onExportExamsPdf = {
                pendingPdfExport = "Prüfungen" to buildExamPdfLines(exams)
                exportPdfLauncher.launch("pruefungen-${System.currentTimeMillis()}.pdf")
                showExportDialog = false
            },
            onExportTimetablePdf = {
                pendingPdfExport = "Stundenplan" to buildTimetablePdfLines(lessons)
                exportPdfLauncher.launch("stundenplan-${System.currentTimeMillis()}.pdf")
                showExportDialog = false
            }
        )
    }

    if (showPersonalizationDialog) {
        PersonalizationDialog(
            showTimetableTab = showTimetableTab,
            showAgendaTab = showAgendaTab,
            showExamCollisionBadges = showExamCollisionBadges,
            collisionRules = collisionRuleSettings,
            accessibilityModeEnabled = accessibilityModeEnabled,
            onDismiss = { showPersonalizationDialog = false },
            onShowTimetableTabChange = { enabled ->
                if (!enabled && !showAgendaTab) {
                    viewModel.setShowAgendaTab(true)
                }
                viewModel.setShowTimetableTab(enabled)
            },
            onShowAgendaTabChange = { enabled ->
                if (!enabled && !showTimetableTab) {
                    viewModel.setShowTimetableTab(true)
                }
                viewModel.setShowAgendaTab(enabled)
            },
            onShowExamCollisionBadgesChange = { enabled ->
                viewModel.setShowExamCollisionBadges(enabled)
            },
            onCollisionRulesChange = { rules ->
                viewModel.saveCollisionRuleSettings(rules)
            },
            onAccessibilityModeChange = { enabled ->
                viewModel.setAccessibilityModeEnabled(enabled)
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
                                onClick = triggerManualRefresh
                            ) {
                                if (isSyncingIcal) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Jetzt aktualisieren"
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    showQuickActionsDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Mehr Aktionen"
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
                    selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0),
                    containerColor = Color.Transparent
                ) {
                    visibleTabs.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title) }
                        )
                    }
                }
                if (showSyncStatusStrip) {
                    SyncStatusStrip(syncStatus = syncStatus)
                }
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
                    lessons = lessons,
                    events = events,
                    showCollisionBadges = showExamCollisionBadges,
                    collisionRules = collisionRuleSettings,
                    hasIcalUrl = savedIcalUrl.isNotBlank(),
                    hasSyncedOnce = syncStatus.lastSyncAtMillis != null,
                    lastSyncError = syncStatus.lastSyncError,
                    onOpenIcalImport = {
                        iCalUrl = savedIcalUrl
                        importEventsToggle = importEventsEnabled
                        showIcalDialog = true
                    },
                    onRefreshNow = triggerManualRefresh,
                    onOpenHelp = {
                        showHelpDialog = true
                    },
                    onAddClick = { showAddDialog = true },
                    onDelete = { exam ->
                        viewModel.deleteExam(exam.id)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "\"${exam.title}\" gelöscht",
                                actionLabel = "Rückgängig",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreExam(exam)
                                snackbarHostState.showSnackbar("Prüfung wiederhergestellt.")
                            }
                        }
                    }
                )

                HomeTab.TIMETABLE -> TimetableContent(
                    lessons = lessons,
                    changes = timetableChanges,
                    hasIcalUrl = savedIcalUrl.isNotBlank(),
                    onOpenIcalImport = {
                        iCalUrl = savedIcalUrl
                        importEventsToggle = importEventsEnabled
                        showIcalDialog = true
                    },
                    onClearChanges = { viewModel.clearTimetableChanges() }
                )

                HomeTab.EVENTS -> EventsTimelineContent(
                    exams = exams,
                    lessons = lessons,
                    events = events,
                    hasIcalUrl = savedIcalUrl.isNotBlank(),
                    importEventsEnabled = importEventsEnabled,
                    onOpenIcalImport = {
                        iCalUrl = savedIcalUrl
                        importEventsToggle = importEventsEnabled
                        showIcalDialog = true
                    },
                    onEnableEventsImportAndSync = {
                        isSyncingIcal = true
                        viewModel.enableEventsImportAndRefresh { message ->
                            isSyncingIcal = false
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
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
    includeEvents: Boolean,
    isImporting: Boolean,
    onUrlChange: (String) -> Unit,
    onIncludeEventsChange: (Boolean) -> Unit,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Events zusätzlich importieren",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = includeEvents,
                        onCheckedChange = onIncludeEventsChange
                    )
                }

                Text(
                    text = if (includeEvents) {
                        "iCal-Link bleibt gespeichert. Es werden Prüfungen, Lektionen und Events importiert."
                    } else {
                        "iCal-Link bleibt gespeichert. Standard: nur Prüfungen und Lektionen (ohne Events)."
                    },
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
private fun QuickActionsDialog(
    showSyncStatusStrip: Boolean,
    onDismiss: () -> Unit,
    onShowSyncStatusStripChange: (Boolean) -> Unit,
    onOpenReminderSettings: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenIcalImport: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenSyncDiagnostics: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Werkzeuge") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync-Balken anzeigen",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = showSyncStatusStrip,
                            onCheckedChange = onShowSyncStatusStripChange
                        )
                    }
                }
                OutlinedButton(
                    onClick = onOpenIcalImport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("iCal-Link verwalten")
                }
                OutlinedButton(
                    onClick = onOpenReminderSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Benachrichtigungen")
                }
                OutlinedButton(
                    onClick = onOpenSyncSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Auto-Sync")
                }
                OutlinedButton(
                    onClick = onOpenSyncDiagnostics,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Sync-Diagnose")
                }
                OutlinedButton(
                    onClick = onOpenExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("CSV/PDF Export")
                }
                OutlinedButton(
                    onClick = onOpenHelp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Hilfe / Troubleshooting")
                }
                OutlinedButton(
                    onClick = onOpenChangelog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Was ist neu")
                }
                OutlinedButton(
                    onClick = onOpenPersonalization,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Personalisieren")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportBackup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Backup Export")
                    }
                    OutlinedButton(
                        onClick = onImportBackup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Backup Import")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
private fun OnboardingDialog(
    url: String,
    includeEvents: Boolean,
    statusMessage: String,
    isBusy: Boolean,
    canFinish: Boolean,
    onUrlChange: (String) -> Unit,
    onIncludeEventsChange: (Boolean) -> Unit,
    onTest: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Erststart: iCal verbinden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { number ->
                        FilterChip(
                            selected = step == number,
                            onClick = { step = number },
                            label = { Text("Schritt $number") }
                        )
                    }
                }

                if (step == 1) {
                    Text(
                        text = "Schritt 1/3: iCal-Link einfügen. Der Link bleibt gespeichert.",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Events zusätzlich importieren",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = includeEvents,
                            onCheckedChange = onIncludeEventsChange
                        )
                    }
                }

                if (step == 2) {
                    Text(
                        text = "Schritt 2/3: Verbindung testen. Erst bei Erfolg wird 'Fertig' aktiv.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (includeEvents) {
                                    "Import-Modus: Prüfungen + Lektionen + Events"
                                } else {
                                    "Import-Modus: Prüfungen + Lektionen"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (url.isBlank()) "Kein Link eingegeben" else url.take(64),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (step == 3) {
                    Text(
                        text = "Schritt 3/3: Fertigstellen. Danach kannst du oben mit dem Pfeil jederzeit aktualisieren.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (canFinish) "Verbindung geprüft. Bereit für ersten Sync." else "Bitte zuerst Verbindung testen.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (includeEvents) "Events-Import ist aktiviert." else "Events-Import ist deaktiviert.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

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
                    enabled = !isBusy && step > 1,
                    onClick = { step -= 1 }
                ) {
                    Text("Zurück")
                }
                TextButton(
                    enabled = !isBusy && step < 3,
                    onClick = { step += 1 }
                ) {
                    Text("Weiter")
                }
                TextButton(
                    enabled = !isBusy && url.isNotBlank(),
                    onClick = onTest
                ) {
                    Text(if (isBusy) "Prüfe..." else "Verbindung testen")
                }
                TextButton(
                    enabled = !isBusy && canFinish && step == 3,
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
private fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hilfe & Troubleshooting") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Schnellstart",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "1) iCal-Link einfügen 2) Verbindung testen 3) Fertig. Danach oben mit dem Pfeil aktualisieren.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Was danach passiert",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Prüfungen: Liste mit Countdown, Suche und Filter.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Stundenplan: Lektionen inkl. Verschiebungen und Raumänderungen.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Events: Gesamtagenda (Prüfungen/Lektionen/Events) nach Zeit.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Notenrechner: Durchschnitt, Zielnote und Punkte-Rechner.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Tägliche Nutzung",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "1) App öffnen 2) oben auf Aktualisieren tippen 3) offene Prüfungen prüfen.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Optional: Auto-Sync, Reminder und Export im Menü 'Werkzeuge'.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Typische Probleme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Schwarzer Emulator: AVD kalt neu starten (Cold Boot) und GPU auf Software stellen.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Sync-Fehler: Link prüfen, Internet prüfen, bei HTTP 410 einen neuen iCal-Link in schulNetz erzeugen.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Keine Events: In iCal-Einstellungen den Event-Import aktivieren und erneut synchronisieren.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Daten sichern: Unter Werkzeuge Backup Export/Import verwenden.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
private fun PersonalizationDialog(
    showTimetableTab: Boolean,
    showAgendaTab: Boolean,
    showExamCollisionBadges: Boolean,
    collisionRules: CollisionRuleSettings,
    accessibilityModeEnabled: Boolean,
    onDismiss: () -> Unit,
    onShowTimetableTabChange: (Boolean) -> Unit,
    onShowAgendaTabChange: (Boolean) -> Unit,
    onShowExamCollisionBadgesChange: (Boolean) -> Unit,
    onCollisionRulesChange: (CollisionRuleSettings) -> Unit,
    onAccessibilityModeChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalisieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stundenplan-Tab anzeigen")
                            Switch(
                                checked = showTimetableTab,
                                onCheckedChange = onShowTimetableTabChange
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Agenda-Tab anzeigen")
                            Switch(
                                checked = showAgendaTab,
                                onCheckedChange = onShowAgendaTabChange
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kollisions-Badges anzeigen")
                            Switch(
                                checked = showExamCollisionBadges,
                                onCheckedChange = onShowExamCollisionBadgesChange
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kollisionen mit Lektionen")
                            Switch(
                                checked = collisionRules.includeLessonCollisions,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(includeLessonCollisions = it)
                                    )
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kollisionen mit Events")
                            Switch(
                                checked = collisionRules.includeEventCollisions,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(includeEventCollisions = it)
                                    )
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nur anderes Fach")
                            Switch(
                                checked = collisionRules.onlyDifferentSubject,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(onlyDifferentSubject = it)
                                    )
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nur echte Zeitüberschneidung")
                            Switch(
                                checked = collisionRules.requireExactTimeOverlap,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(requireExactTimeOverlap = it)
                                    )
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Barrierefreiheit-Modus")
                            Switch(
                                checked = accessibilityModeEnabled,
                                onCheckedChange = onAccessibilityModeChange
                            )
                        }
                    }
                }

                Text(
                    text = "Agenda zeigt Prüfungen, Lektionen und Events kombiniert. Kollisionsregeln und Accessibility lassen sich hier zentral steuern.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fertig")
            }
        }
    )
}

@Composable
private fun SyncStatusStrip(syncStatus: SyncStatus) {
    val error = syncStatus.lastSyncError
    val now = System.currentTimeMillis()
    val staleThresholdMillis = 24L * 60L * 60L * 1000L
    val isStale = syncStatus.lastSyncAtMillis?.let { last ->
        now - last > staleThresholdMillis
    } == true
    val headline = when {
        !error.isNullOrBlank() -> error
        syncStatus.lastSyncAtMillis != null -> {
            val time = formatSyncDateTime(syncStatus.lastSyncAtMillis)
            if (isStale) {
                "Zuletzt synchronisiert: $time (veraltet)"
            } else {
                "Zuletzt synchronisiert: $time"
            }
        }
        else -> "Noch keine Synchronisierung"
    }
    val details = when {
        !error.isNullOrBlank() -> null
        isStale -> "Letzter erfolgreicher Sync ist älter als 24h. Bitte oben auf Aktualisieren tippen."
        else -> syncStatus.lastSyncSummary?.takeIf { it.isNotBlank() }
    }

    val containerColor = when {
        !error.isNullOrBlank() -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        isStale -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    }

    val textColor = when {
        !error.isNullOrBlank() -> MaterialTheme.colorScheme.onErrorContainer
        isStale -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            details?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SyncDiagnosticsDialog(
    diagnostics: SyncDiagnostics,
    syncStatus: SyncStatus,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync-Diagnose") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Letzter erfolgreicher Sync: ${
                        syncStatus.lastSyncAtMillis?.let(::formatSyncDateTime) ?: "noch nie"
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Letzter Versuch: ${
                        diagnostics.lastAttemptAtMillis?.let(::formatSyncDateTime) ?: "unbekannt"
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Dauer: ${diagnostics.lastDurationMillis?.let(::formatDurationMillis) ?: "unbekannt"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "HTTP-Status: ${diagnostics.lastHttpStatusCode?.toString() ?: "unbekannt"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Delta-Sync: ${if (diagnostics.lastDeltaNotModified) "Keine Änderungen (304)" else "Daten aktualisiert"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Importiert",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Prüfungen: ${diagnostics.importedExams}", style = MaterialTheme.typography.bodySmall)
                        Text("Lektionen: ${diagnostics.importedLessons}", style = MaterialTheme.typography.bodySmall)
                        Text("Events: ${diagnostics.importedEvents}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Erkannte Änderungen",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Gesamt: ${diagnostics.changedLessons}", style = MaterialTheme.typography.bodySmall)
                        Text("Verschoben: ${diagnostics.movedLessons}", style = MaterialTheme.typography.bodySmall)
                        Text("Raum geändert: ${diagnostics.roomChangedLessons}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                val error = diagnostics.lastErrorReason
                    ?.takeIf { it.isNotBlank() }
                    ?: syncStatus.lastSyncError
                if (!error.isNullOrBlank()) {
                    Text(
                        text = "Fehlerursache: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
private fun ChangelogDialog(
    versionName: String,
    entries: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neu in Version $versionName") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entries.forEach { entry ->
                    Text(
                        text = "• $entry",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Verstanden")
            }
        }
    )
}

@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExportExamsCsv: () -> Unit,
    onExportTimetableCsv: () -> Unit,
    onExportExamsPdf: () -> Unit,
    onExportTimetablePdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CSV/PDF Export") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportExamsCsv
                ) {
                    Text("Prüfungen als CSV")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportTimetableCsv
                ) {
                    Text("Stundenplan als CSV")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportExamsPdf
                ) {
                    Text("Prüfungen als PDF")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportTimetablePdf
                ) {
                    Text("Stundenplan als PDF")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

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
private fun TimetableContent(
    lessons: List<TimetableLesson>,
    changes: List<TimetableChangeEntry>,
    hasIcalUrl: Boolean,
    onOpenIcalImport: () -> Unit,
    onClearChanges: () -> Unit
) {
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
                .toLocalDate() == today
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
private fun TimetableNowNextCard(
    activeLesson: TimetableLessonBlock?,
    upcomingLesson: TimetableLessonBlock?
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Orientierung",
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
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Jetzt: ${lesson.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis)}${lesson.location?.let { " · $it" }.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                upcomingLesson?.let { lesson ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Nächste: ${lesson.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${formatExamDateShort(lesson.startsAtEpochMillis)} · ${formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
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
    val nowMillis = System.currentTimeMillis()
    val isCurrent = !isCancelled && nowMillis in lesson.startsAtEpochMillis until lesson.endsAtEpochMillis
    val cardColor = if (isCancelled) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
    } else if (isCurrent) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f)
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
                    if (isCurrent) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Jetzt",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
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
    lessons: List<TimetableLesson>,
    events: List<SchoolEvent>,
    showCollisionBadges: Boolean,
    collisionRules: CollisionRuleSettings,
    hasIcalUrl: Boolean,
    hasSyncedOnce: Boolean,
    lastSyncError: String?,
    onOpenIcalImport: () -> Unit,
    onRefreshNow: () -> Unit,
    onOpenHelp: () -> Unit,
    onAddClick: () -> Unit,
    onDelete: (Exam) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSubject by rememberSaveable { mutableStateOf(SUBJECT_FILTER_ALL) }
    var selectedWindow by rememberSaveable { mutableStateOf(ExamWindowFilter.ALL) }
    var selectedSortMode by rememberSaveable { mutableStateOf(ExamSortMode.NEXT_FIRST) }
    val subjectOptions = remember(exams) {
        val subjects = exams.mapNotNull { exam ->
            exam.subject?.trim()?.takeIf { it.isNotBlank() }
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

    val filteredExams = remember(exams, searchQuery, selectedSubject, selectedWindow, selectedSortMode) {
        val query = searchQuery.trim().lowercase()
        val now = System.currentTimeMillis()
        val windowEnd = selectedWindow.maxDaysAhead?.let { days ->
            now + days * 24L * 60L * 60L * 1000L
        }

        val filtered = exams.filter { exam ->
            val matchesSubject = selectedSubject == SUBJECT_FILTER_ALL ||
                exam.subject?.equals(selectedSubject, ignoreCase = true) == true
            val matchesQuery = query.isBlank() || listOf(
                exam.subject.orEmpty(),
                exam.title,
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
                    { it.subject.orEmpty().lowercase() },
                    { it.title.lowercase() },
                    { it.startsAtEpochMillis }
                )
            )
            ExamSortMode.TITLE_AZ -> filtered.sortedWith(
                compareBy<Exam>(
                    { it.title.lowercase() },
                    { it.subject.orEmpty().lowercase() },
                    { it.startsAtEpochMillis }
                )
            )
        }
    }
    val nextExam = remember(filteredExams) {
        val now = System.currentTimeMillis()
        filteredExams.firstOrNull { it.startsAtEpochMillis >= now } ?: filteredExams.firstOrNull()
    }
    val collisionMap = remember(exams, lessons, events, showCollisionBadges, collisionRules) {
        if (!showCollisionBadges) {
            emptyMap()
        } else {
            val collisions = detectExamCollisions(
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
            collisionsByExam(collisions)
        }
    }
    val collisionCount = remember(collisionMap) {
        collisionMap.values.flatten().size
    }

    if (exams.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item("setup-guide-empty") {
                SetupGuideCard(
                    examCount = exams.size,
                    hasIcalUrl = hasIcalUrl,
                    hasSyncedOnce = hasSyncedOnce,
                    lastSyncError = lastSyncError,
                    onOpenIcalImport = onOpenIcalImport,
                    onRefreshNow = onRefreshNow,
                    onOpenHelp = onOpenHelp
                )
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item("setup-guide") {
            SetupGuideCard(
                examCount = exams.size,
                hasIcalUrl = hasIcalUrl,
                hasSyncedOnce = hasSyncedOnce,
                lastSyncError = lastSyncError,
                onOpenIcalImport = onOpenIcalImport,
                onRefreshNow = onRefreshNow,
                onOpenHelp = onOpenHelp
            )
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
                selectedSortMode = selectedSortMode,
                onSortModeSelected = { selectedSortMode = it }
            )
        }
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
        item {
            nextExam?.let { exam ->
                NextExamHero(exam = exam)
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
            items(items = filteredExams, key = { it.id }) { exam ->
                ExamCard(
                    exam = exam,
                    collisions = collisionMap[exam.id].orEmpty(),
                    onDelete = { onDelete(exam) }
                )
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
    onOpenIcalImport: () -> Unit,
    onRefreshNow: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val nextStep = when {
        !hasIcalUrl -> "1) iCal-Link aus schulNetz einfügen."
        !hasSyncedOnce -> "2) Ersten Sync starten (Aktualisieren)."
        examCount == 0 -> "3) Prüfungen prüfen oder manuell hinzufügen."
        else -> "Alles bereit. Du kannst jetzt normal nutzen."
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = nextStep,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "iCal-Link: ${if (hasIcalUrl) "verbunden" else "fehlt"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Erster Sync: ${if (hasSyncedOnce) "gemacht" else "offen"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Prüfungen sichtbar: $examCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!lastSyncError.isNullOrBlank()) {
                Text(
                    text = "Letzter Fehler: $lastSyncError",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = if (hasIcalUrl) onRefreshNow else onOpenIcalImport,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (hasIcalUrl) "Jetzt synchronisieren" else "iCal einrichten")
                }
                OutlinedButton(
                    onClick = onOpenHelp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("So funktioniert's")
                }
            }
        }
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
    selectedSortMode: ExamSortMode,
    onSortModeSelected: (ExamSortMode) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Suche") },
                placeholder = { Text("Titel, Fach oder Ort") },
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
            if (subjects.size > 1) {
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
            exam.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                Surface(
                    color = Color(0x332F6EBA),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Fach: $subject",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE6F0FF)
                    )
                }
            }
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
private fun ExamCard(
    exam: Exam,
    collisions: List<ExamCollision>,
    onDelete: () -> Unit
) {
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
            exam.subject?.takeIf { it.isNotBlank() }?.let { subject ->
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

@Composable
private fun AddExamDialog(
    onDismiss: () -> Unit,
    onSave: (String?, String, String?, Long, Long?, List<Long>) -> Unit
) {
    val context = LocalContext.current
    var subject by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var selectedExamMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis() + 24L * 60L * 60L * 1000L)
    }
    var reminderEnabled by rememberSaveable { mutableStateOf(true) }
    var leadTimesRaw by rememberSaveable { mutableStateOf("30, 1440") }
    var exactReminderEnabled by rememberSaveable { mutableStateOf(false) }
    var reminderManuallySet by rememberSaveable { mutableStateOf(false) }
    var selectedReminderMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis() + 24L * 60L * 60L * 1000L - 30L * 60L * 1000L)
    }

    val parsedLeadTimes = remember(leadTimesRaw) { parseLeadTimesMinutes(leadTimesRaw) }
    val leadTimesInvalid = reminderEnabled && leadTimesRaw.isNotBlank() && parsedLeadTimes.isEmpty()

    val reminderValidationError = when {
        !reminderEnabled -> null
        leadTimesInvalid -> "Vorlaufzeiten ungültig (z. B. 30, 120, 1440)"
        exactReminderEnabled && selectedReminderMillis <= System.currentTimeMillis() -> "Erinnerungszeit liegt in der Vergangenheit"
        exactReminderEnabled && selectedReminderMillis >= selectedExamMillis -> "Erinnerung muss vor Prüfungsbeginn liegen"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Prüfung") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Fach (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel / Prüfung") },
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
                    OutlinedTextField(
                        value = leadTimesRaw,
                        onValueChange = { leadTimesRaw = it },
                        label = { Text("Vorlaufzeiten in Minuten") },
                        placeholder = { Text("z. B. 30, 120, 1440") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Mehrere Erinnerungen möglich. Beispiel: 30, 120, 1440.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Exakte Erinnerungszeit",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = exactReminderEnabled,
                            onCheckedChange = { checked -> exactReminderEnabled = checked }
                        )
                    }

                }

                if (reminderEnabled && exactReminderEnabled) {
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
                        subject.ifBlank { null },
                        title,
                        location.ifBlank { null },
                        selectedExamMillis,
                        if (reminderEnabled && exactReminderEnabled) selectedReminderMillis else null,
                        if (reminderEnabled) parsedLeadTimes else emptyList()
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

@Composable
private fun ReminderSettingsDialog(
    initialConfig: QuietHoursConfig,
    syncIntervalMinutes: Long,
    onDismiss: () -> Unit,
    onSave: (QuietHoursConfig) -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenSyncSettings: () -> Unit
) {
    var enabled by remember(initialConfig) { mutableStateOf(initialConfig.enabled) }
    var startMinutes by remember(initialConfig) { mutableIntStateOf(initialConfig.startMinutesOfDay) }
    var endMinutes by remember(initialConfig) { mutableIntStateOf(initialConfig.endMinutesOfDay) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Benachrichtigungen") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stille Zeiten aktiv")
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                if (enabled) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            openTimePicker(
                                context = context,
                                initialMinutesOfDay = startMinutes,
                                onPicked = { picked -> startMinutes = picked }
                            )
                        }
                    ) {
                        Text("Stille Zeit ab: ${formatMinutesOfDay(startMinutes)}")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            openTimePicker(
                                context = context,
                                initialMinutesOfDay = endMinutes,
                                onPicked = { picked -> endMinutes = picked }
                            )
                        }
                    ) {
                        Text("Stille Zeit bis: ${formatMinutesOfDay(endMinutes)}")
                    }
                    Text(
                        text = "Erinnerungen in stiller Zeit werden auf das Ende der stillen Zeit verschoben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Auto-Sync aktuell: alle $syncIntervalMinutes Minuten",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSendTestNotification,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test senden")
                    }
                    OutlinedButton(
                        onClick = onOpenSyncSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sync einstellen")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        QuietHoursConfig(
                            enabled = enabled,
                            startMinutesOfDay = startMinutes,
                            endMinutesOfDay = endMinutes
                        )
                    )
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

@Composable
private fun SyncSettingsDialog(
    initialIntervalMinutes: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var intervalRaw by rememberSaveable(initialIntervalMinutes) {
        mutableStateOf(initialIntervalMinutes.toString())
    }
    val parsed = intervalRaw.trim().toLongOrNull()
    val normalized = parsed?.coerceIn(15L, 12L * 60L)
    val isValid = normalized != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Synchronisierung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = intervalRaw,
                    onValueChange = { intervalRaw = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Intervall in Minuten") },
                    placeholder = { Text("z. B. 60") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Gültig: 15 bis 720 Minuten. Empfohlen: 60 oder 180 Minuten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30L, 60L, 180L, 360L).forEach { quick ->
                        OutlinedButton(
                            onClick = { intervalRaw = quick.toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$quick")
                        }
                    }
                }
                if (!isValid) {
                    Text(
                        text = "Bitte eine Zahl zwischen 15 und 720 eingeben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { onSave(normalized ?: 60L) }
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

private fun changelogEntriesFor(versionName: String): List<String> {
    return when (versionName) {
        "1.6.0" -> listOf(
            "Neue 'Erste Schritte'-Karte im Prüfungs-Tab mit klarer nächster Aktion.",
            "Bessere In-App-Hilfe: Was jeder Tab macht + täglicher Ablauf.",
            "Schneller Zugriff auf Einrichtung/Sync direkt aus der Start-Hilfe.",
            "Fehlermeldungen sichtbarer für schnellere Problemlösung.",
            "Allgemeine Bedienung und Orientierung verbessert."
        )
        "1.5.0" -> listOf(
            "Sync-Diagnose mit letzter Dauer, HTTP-Status und klarer Fehlerursache.",
            "Delta-Sync per ETag/Last-Modified für stabilere und sparsamere Synchronisierung.",
            "Kollisionsregeln: Lektion/Event getrennt, nur anderes Fach, echte Zeitüberschneidung.",
            "Widget-Konfiguration pro Widget: Modus, Zeitraum und Sortierung.",
            "Barrierefreiheit-Modus mit größerer Schrift und höherem Kontrast.",
            "CSV/PDF-Export für Prüfungen und Stundenplan.",
            "In-App Changelog nach App-Updates."
        )
        else -> listOf(
            "Neue Version mit Verbesserungen für Sync, UI und Stabilität.",
            "Details findest du in README und den Release Notes auf GitHub."
        )
    }
}

private fun buildExamsCsv(exams: List<Exam>): String {
    val header = "Fach,Titel,Datum,Ort,Countdown"
    val rows = exams
        .sortedBy { it.startsAtEpochMillis }
        .map { exam ->
            listOf(
                exam.subject.orEmpty(),
                exam.title,
                formatExamDate(exam.startsAtEpochMillis),
                exam.location.orEmpty(),
                formatCountdown(exam.startsAtEpochMillis)
            ).joinToString(",") { csvEscape(it) }
        }
    return buildString {
        appendLine(header)
        rows.forEach { appendLine(it) }
    }
}

private fun buildTimetableCsv(lessons: List<TimetableLesson>): String {
    val header = "Titel,Start,Ende,Uhrzeit,Ort,Verschoben,Raum geändert"
    val rows = lessons
        .sortedBy { it.startsAtEpochMillis }
        .map { lesson ->
            listOf(
                lesson.title,
                formatExamDateShort(lesson.startsAtEpochMillis),
                formatExamDateShort(lesson.endsAtEpochMillis),
                formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis),
                lesson.location.orEmpty(),
                if (lesson.isMoved) "Ja" else "Nein",
                if (lesson.isLocationChanged) "Ja" else "Nein"
            ).joinToString(",") { csvEscape(it) }
        }
    return buildString {
        appendLine(header)
        rows.forEach { appendLine(it) }
    }
}

private fun buildExamPdfLines(exams: List<Exam>): List<String> {
    if (exams.isEmpty()) return listOf("Keine Prüfungen vorhanden.")
    return exams.sortedBy { it.startsAtEpochMillis }.map { exam ->
        val subjectPrefix = exam.subject?.takeIf { it.isNotBlank() }?.let { "[$it] " }.orEmpty()
        "$subjectPrefix${exam.title} | ${formatExamDate(exam.startsAtEpochMillis)}${exam.location?.let { " | $it" }.orEmpty()}"
    }
}

private fun buildTimetablePdfLines(lessons: List<TimetableLesson>): List<String> {
    if (lessons.isEmpty()) return listOf("Keine Lektionen vorhanden.")
    return lessons.sortedBy { it.startsAtEpochMillis }.map { lesson ->
        val flags = buildList {
            if (lesson.isMoved) add("verschoben")
            if (lesson.isLocationChanged) add("Raum geändert")
        }.joinToString(", ")
        "${lesson.title} | ${formatExamDateShort(lesson.startsAtEpochMillis)} ${formatTimeRange(lesson.startsAtEpochMillis, lesson.endsAtEpochMillis)}${lesson.location?.let { " | $it" }.orEmpty()}${if (flags.isNotBlank()) " | $flags" else ""}"
    }
}

private fun csvEscape(raw: String): String {
    val normalized = raw.replace("\r", " ").replace("\n", " ")
    return "\"${normalized.replace("\"", "\"\"")}\""
}

private fun writeSimplePdf(
    outputStream: OutputStream,
    title: String,
    lines: List<String>
) {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val left = 40f
    val top = 56f
    val bottom = pageHeight - 48f

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f
        isFakeBoldText = true
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
    }

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = top

    fun newPage() {
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = top
        canvas.drawText(title, left, y, titlePaint)
        y += 24f
    }

    canvas.drawText(title, left, y, titlePaint)
    y += 24f

    val maxCharsPerLine = 92
    lines.forEach { line ->
        val wrapped = line.chunked(maxCharsPerLine)
        wrapped.forEach { part ->
            if (y > bottom) {
                newPage()
            }
            canvas.drawText(part, left, y, bodyPaint)
            y += 16f
        }
    }

    document.finishPage(page)
    document.writeTo(outputStream)
    document.close()
}

private fun formatDurationMillis(durationMillis: Long): String {
    val safe = durationMillis.coerceAtLeast(0L)
    val seconds = safe / 1_000L
    val millis = safe % 1_000L
    return "${seconds}s ${millis}ms"
}

private fun parseLeadTimesMinutes(raw: String): List<Long> {
    return raw.split(',', ';', ' ')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull { it.toLongOrNull() }
        .filter { it in 1L..(14L * 24L * 60L) }
        .distinct()
        .sorted()
}

private fun collisionSourceLabel(source: CollisionSource): String {
    return when (source) {
        CollisionSource.LESSON -> "Lektion"
        CollisionSource.EVENT -> "Event"
    }
}

private fun formatMinutesOfDay(minutesOfDay: Int): String {
    val normalized = minutesOfDay.coerceIn(0, 24 * 60 - 1)
    val time = LocalTime.of(normalized / 60, normalized % 60)
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun openTimePicker(
    context: Context,
    initialMinutesOfDay: Int,
    onPicked: (Int) -> Unit
) {
    val hour = (initialMinutesOfDay / 60).coerceIn(0, 23)
    val minute = (initialMinutesOfDay % 60).coerceIn(0, 59)
    TimePickerDialog(
        context,
        { _, pickedHour, pickedMinute ->
            onPicked(pickedHour * 60 + pickedMinute)
        },
        hour,
        minute,
        true
    ).show()
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
