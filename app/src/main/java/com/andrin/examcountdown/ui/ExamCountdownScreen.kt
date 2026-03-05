package com.andrin.examcountdown.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.andrin.examcountdown.BuildConfig
import com.andrin.examcountdown.data.CollisionRuleSettings
import com.andrin.examcountdown.data.QuietHoursConfig
import com.andrin.examcountdown.data.SyncStatus
import com.andrin.examcountdown.data.SyncDiagnostics
import com.andrin.examcountdown.data.BackupCrypto
import com.andrin.examcountdown.domain.usecase.PlanStudySessionsUseCase
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableChangeType
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.ui.tabs.AgendaTabContent
import com.andrin.examcountdown.ui.tabs.ExamsTabContent
import com.andrin.examcountdown.ui.tabs.GradesTabContent
import com.andrin.examcountdown.ui.tabs.TimetableTabContent
import com.andrin.examcountdown.ui.tabs.events.AgendaTabEvent
import com.andrin.examcountdown.ui.tabs.events.ExamsTabEvent
import com.andrin.examcountdown.ui.tabs.events.TimetableTabEvent
import com.andrin.examcountdown.util.CollisionRules
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
import java.net.URI
import java.security.KeyStore
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.launch

enum class HomeTab(
    val title: String,
    val shortTitle: String,
    val route: String,
    val icon: ImageVector
) {
    EXAMS("Prüfungen", "Prüf.", "exams", Icons.Outlined.School),
    TIMETABLE("Stundenplan", "Plan", "timetable", Icons.Outlined.Schedule),
    EVENTS("Agenda", "Agenda", "events", Icons.Outlined.CalendarToday),
    GRADES("Notenrechner", "Noten", "grades", Icons.Outlined.Calculate);

    companion object {
        fun fromRoute(route: String?): HomeTab {
            return entries.firstOrNull { it.route == route } ?: EXAMS
        }
    }
}

private data class StudyWeekdayOption(
    val dayOfWeek: DayOfWeek,
    val shortLabel: String
)

private fun studyWeekdayOptions(): List<StudyWeekdayOption> = listOf(
    StudyWeekdayOption(DayOfWeek.MONDAY, "Mo"),
    StudyWeekdayOption(DayOfWeek.TUESDAY, "Di"),
    StudyWeekdayOption(DayOfWeek.WEDNESDAY, "Mi"),
    StudyWeekdayOption(DayOfWeek.THURSDAY, "Do"),
    StudyWeekdayOption(DayOfWeek.FRIDAY, "Fr"),
    StudyWeekdayOption(DayOfWeek.SATURDAY, "Sa"),
    StudyWeekdayOption(DayOfWeek.SUNDAY, "So")
)

private const val APP_LOCK_MIN_PIN_DIGITS = 4
private const val APP_LOCK_MAX_PIN_DIGITS = 10
private const val BIOMETRIC_KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val BIOMETRIC_KEY_ALIAS = "examcountdown.app.lock.biometric"
private const val BIOMETRIC_CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
private val BIOMETRIC_UNLOCK_CHALLENGE = "examcountdown-unlock".toByteArray(Charsets.UTF_8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCountdownScreen(
    initialTab: HomeTab = HomeTab.EXAMS,
    viewModel: ExamViewModel = viewModel()
) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val savedIcalUrls by viewModel.savedIcalUrls.collectAsStateWithLifecycle()
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
    val simpleModeEnabled by viewModel.simpleModeEnabled.collectAsStateWithLifecycle()
    val lastSeenVersion by viewModel.lastSeenVersion.collectAsStateWithLifecycle()
    val showSetupGuideCard by viewModel.showSetupGuideCard.collectAsStateWithLifecycle()
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val appLockBiometricEnabled by viewModel.appLockBiometricEnabled.collectAsStateWithLifecycle()
    val screenshotProtectionEnabled by viewModel.screenshotProtectionEnabled.collectAsStateWithLifecycle()
    val examsTabUiState by viewModel.examsTabUiState.collectAsStateWithLifecycle()
    val timetableTabUiState by viewModel.timetableTabUiState.collectAsStateWithLifecycle()
    val agendaTabUiState by viewModel.agendaTabUiState.collectAsStateWithLifecycle()
    val gradesTabUiState by viewModel.gradesTabUiState.collectAsStateWithLifecycle()
    val isDarkMode = isSystemInDarkTheme()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showIcalDialog by rememberSaveable { mutableStateOf(false) }
    var showOnboardingDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showSyncSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickActionsDialog by rememberSaveable { mutableStateOf(false) }
    var showPersonalizationDialog by rememberSaveable { mutableStateOf(false) }
    var showAppLockDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showBackupExportDialog by rememberSaveable { mutableStateOf(false) }
    var showBackupImportDialog by rememberSaveable { mutableStateOf(false) }
    var showSyncDiagnosticsDialog by rememberSaveable { mutableStateOf(false) }
    var showChangelogDialog by rememberSaveable { mutableStateOf(false) }
    var showFullChangelogDialog by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var studyPlanExam by remember { mutableStateOf<Exam?>(null) }
    var studyPlanExamPresentation by remember { mutableStateOf<ExamPresentation?>(null) }
    var iCalUrlPrimary by rememberSaveable { mutableStateOf("") }
    var iCalUrlSecondary by rememberSaveable { mutableStateOf("") }
    var importEventsToggle by rememberSaveable { mutableStateOf(false) }
    var onboardingUrlPrimary by rememberSaveable { mutableStateOf("") }
    var onboardingUrlSecondary by rememberSaveable { mutableStateOf("") }
    var onboardingImportEvents by rememberSaveable { mutableStateOf(false) }
    var onboardingTestedOk by rememberSaveable { mutableStateOf(false) }
    var onboardingInfoMessage by rememberSaveable { mutableStateOf("") }
    var isSyncingIcal by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var appLockInitialized by rememberSaveable { mutableStateOf(false) }
    var isAppUnlocked by rememberSaveable { mutableStateOf(false) }
    var biometricAutoPromptConsumed by rememberSaveable { mutableStateOf(false) }
    var biometricUnlockError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hostActivity = remember(context) { context as? FragmentActivity }
    val lifecycleOwner = LocalLifecycleOwner.current
    val biometricAvailable = remember(context) { isBiometricUnlockAvailable(context) }
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var pendingBackupImportRaw by remember { mutableStateOf<String?>(null) }
    var pendingCsvExport by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingPdfExport by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var backupExportPassword by rememberSaveable { mutableStateOf("") }
    var backupImportPassword by rememberSaveable { mutableStateOf("") }
    val hasUnseenChangelog = lastSeenVersion != BuildConfig.VERSION_NAME
    val collectIcalUrls: (String, String) -> List<String> = { primary, secondary ->
        listOf(primary, secondary)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(2)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingNotificationAction
        pendingNotificationAction = null
        if (granted) {
            action?.invoke()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Benachrichtigungen sind deaktiviert. Du kannst sie später in den App-Einstellungen erlauben."
                )
            }
        }
    }
    val requestNotificationPermissionIfNeeded: (() -> Unit) -> Unit = { onGranted ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onGranted()
            } else {
                pendingNotificationAction = onGranted
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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

    LaunchedEffect(preferencesLoaded, appLockEnabled) {
        if (!preferencesLoaded) return@LaunchedEffect
        if (!appLockInitialized) {
            appLockInitialized = true
            isAppUnlocked = !appLockEnabled
            biometricAutoPromptConsumed = false
            biometricUnlockError = null
            return@LaunchedEffect
        }
        if (!appLockEnabled) {
            isAppUnlocked = true
            biometricAutoPromptConsumed = false
            biometricUnlockError = null
        }
    }

    DisposableEffect(lifecycleOwner, appLockEnabled, appLockInitialized) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_STOP &&
                appLockEnabled &&
                appLockInitialized
            ) {
                isAppUnlocked = false
                biometricAutoPromptConsumed = false
                biometricUnlockError = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onBiometricUnlockSuccess = rememberUpdatedState(newValue = {
        isAppUnlocked = true
        biometricUnlockError = null
    })
    val onBiometricUnlockFailure = rememberUpdatedState(newValue = { error: String? ->
        biometricUnlockError = error?.takeIf { it.isNotBlank() } ?: "Biometrie fehlgeschlagen."
    })

    LaunchedEffect(
        appLockInitialized,
        appLockEnabled,
        isAppUnlocked,
        appLockBiometricEnabled,
        biometricAvailable,
        biometricAutoPromptConsumed,
        hostActivity
    ) {
        if (!appLockInitialized || !appLockEnabled || isAppUnlocked) return@LaunchedEffect
        if (!appLockBiometricEnabled || !biometricAvailable) return@LaunchedEffect
        if (biometricAutoPromptConsumed) return@LaunchedEffect
        val activity = hostActivity ?: return@LaunchedEffect

        biometricAutoPromptConsumed = true
        runBiometricUnlock(
            activity = activity,
            title = "App entsperren",
            subtitle = "Mit Fingerabdruck oder Face entsperren",
            onSuccess = onBiometricUnlockSuccess.value,
            onFailure = onBiometricUnlockFailure.value
        )
    }

    LaunchedEffect(importEventsEnabled) {
        importEventsToggle = importEventsEnabled
        onboardingImportEvents = importEventsEnabled
    }

    LaunchedEffect(hostActivity, screenshotProtectionEnabled) {
        val window = hostActivity?.window ?: return@LaunchedEffect
        if (screenshotProtectionEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
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
            pendingBackupImportRaw = raw
            backupImportPassword = ""
            showBackupImportDialog = true
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

    LaunchedEffect(preferencesLoaded, onboardingDone, onboardingPromptSeen, savedIcalUrls) {
        if (!preferencesLoaded) return@LaunchedEffect

        val shouldShowOnboarding = !onboardingDone &&
            !onboardingPromptSeen &&
            savedIcalUrls.isEmpty()

        if (shouldShowOnboarding) {
            showOnboardingDialog = true
            onboardingUrlPrimary = savedIcalUrls.getOrNull(0).orEmpty()
            onboardingUrlSecondary = savedIcalUrls.getOrNull(1).orEmpty()
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
            onSave = { subject, title, location, examMillis, reminderAtMillis, reminderLeadTimes, studySessions ->
                viewModel.addExam(
                    subject = subject,
                    title = title,
                    location = location,
                    startsAtMillis = examMillis,
                    reminderAtMillis = reminderAtMillis,
                    reminderLeadTimesMinutes = reminderLeadTimes,
                    studySessions = studySessions
                )
            }
        )
    }

    val activeStudyPlanExam = studyPlanExam
    val activeStudyPlanPresentation = studyPlanExamPresentation
    if (activeStudyPlanExam != null && activeStudyPlanPresentation != null) {
        PlanExamStudySessionsDialog(
            exam = activeStudyPlanExam,
            presentation = activeStudyPlanPresentation,
            onDismiss = {
                studyPlanExam = null
                studyPlanExamPresentation = null
            },
            onSaveSessions = { sessions ->
                viewModel.addCustomEvents(sessions)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (sessions.size == 1) "1 Lern-Session erstellt." else "${sessions.size} Lern-Sessions erstellt."
                    )
                }
                studyPlanExam = null
                studyPlanExamPresentation = null
            }
        )
    }

    if (showIcalDialog) {
        IcalImportDialog(
            primaryUrl = iCalUrlPrimary,
            secondaryUrl = iCalUrlSecondary,
            includeEvents = importEventsToggle,
            isImporting = isSyncingIcal,
            onPrimaryUrlChange = { iCalUrlPrimary = it },
            onSecondaryUrlChange = { iCalUrlSecondary = it },
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
                    urls = collectIcalUrls(iCalUrlPrimary, iCalUrlSecondary),
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
            primaryUrl = onboardingUrlPrimary,
            secondaryUrl = onboardingUrlSecondary,
            includeEvents = onboardingImportEvents,
            statusMessage = onboardingInfoMessage,
            isBusy = isSyncingIcal,
            canFinish = onboardingTestedOk && collectIcalUrls(onboardingUrlPrimary, onboardingUrlSecondary).isNotEmpty(),
            onPrimaryUrlChange = { newUrl ->
                onboardingUrlPrimary = newUrl
                onboardingTestedOk = false
            },
            onSecondaryUrlChange = { newUrl ->
                onboardingUrlSecondary = newUrl
                onboardingTestedOk = false
            },
            onIncludeEventsChange = { onboardingImportEvents = it },
            onTest = {
                isSyncingIcal = true
                viewModel.testIcalConnection(
                    urls = collectIcalUrls(onboardingUrlPrimary, onboardingUrlSecondary),
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
                    urls = collectIcalUrls(onboardingUrlPrimary, onboardingUrlSecondary),
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
                requestNotificationPermissionIfNeeded {
                    viewModel.sendTestNotification { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
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
            onSyncNow = {
                showQuickActionsDialog = false
                triggerManualRefresh()
            },
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
                iCalUrlPrimary = savedIcalUrls.getOrNull(0).orEmpty()
                iCalUrlSecondary = savedIcalUrls.getOrNull(1).orEmpty()
                importEventsToggle = importEventsEnabled
                showQuickActionsDialog = false
                showIcalDialog = true
            },
            onOpenHelp = {
                showQuickActionsDialog = false
                showHelpDialog = true
            },
            onOpenPrivacy = {
                showQuickActionsDialog = false
                showPrivacyDialog = true
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
                viewModel.setLastSeenVersion(BuildConfig.VERSION_NAME)
                showChangelogDialog = true
            },
            onOpenPersonalization = {
                showQuickActionsDialog = false
                showPersonalizationDialog = true
            },
            onOpenAppLock = {
                showQuickActionsDialog = false
                showAppLockDialog = true
            },
            onExportBackup = {
                showQuickActionsDialog = false
                backupExportPassword = ""
                showBackupExportDialog = true
            },
            onImportBackup = {
                showQuickActionsDialog = false
                importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            hasUnseenChangelog = hasUnseenChangelog
        )
    }

    if (showAppLockDialog) {
        AppLockSettingsDialog(
            isEnabled = appLockEnabled,
            biometricEnabled = appLockBiometricEnabled,
            canUseBiometric = biometricAvailable && hostActivity != null,
            onDismiss = { showAppLockDialog = false },
            onBiometricEnabledChange = { enabled ->
                viewModel.setAppLockBiometricEnabled(enabled)
            },
            onEnable = { pin, biometricEnabled ->
                viewModel.enableAppLock(pin, biometricEnabled = biometricEnabled) { success, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                    if (success) {
                        isAppUnlocked = true
                        biometricAutoPromptConsumed = false
                        biometricUnlockError = null
                        showAppLockDialog = false
                    }
                }
            },
            onDisable = { pin ->
                viewModel.disableAppLock(pin) { success, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                    if (success) {
                        isAppUnlocked = true
                        biometricAutoPromptConsumed = false
                        biometricUnlockError = null
                        showAppLockDialog = false
                    }
                }
            }
        )
    }

    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyDialog(
            screenshotProtectionEnabled = screenshotProtectionEnabled,
            onScreenshotProtectionChange = { enabled ->
                viewModel.setScreenshotProtectionEnabled(enabled)
            },
            onDeleteAllData = {
                viewModel.clearAllLocalData { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showBackupExportDialog) {
        BackupPasswordDialog(
            title = "Backup Export",
            message = "Optional: Passwort setzen, um das Backup zu verschlüsseln.",
            password = backupExportPassword,
            confirmLabel = "Exportieren",
            onPasswordChange = { backupExportPassword = it },
            onDismiss = { showBackupExportDialog = false },
            onConfirm = {
                showBackupExportDialog = false
                viewModel.exportBackupJson { result ->
                    result.onSuccess { json ->
                        runCatching {
                            if (backupExportPassword.trim().isBlank()) {
                                json
                            } else {
                                BackupCrypto.encrypt(json, backupExportPassword.trim())
                            }
                        }.onSuccess { payload ->
                            pendingBackupJson = payload
                            val suffix = if (backupExportPassword.trim().isBlank()) "json" else "ecbkp"
                            exportBackupLauncher.launch(
                                "examcountdown-backup-${System.currentTimeMillis()}.$suffix"
                            )
                        }.onFailure { throwable ->
                            val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
                            scope.launch { snackbarHostState.showSnackbar("Backup fehlgeschlagen: $error") }
                        }
                    }.onFailure { throwable ->
                        val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
                        scope.launch { snackbarHostState.showSnackbar("Backup fehlgeschlagen: $error") }
                    }
                }
            }
        )
    }

    if (showBackupImportDialog) {
        BackupPasswordDialog(
            title = "Backup Import",
            message = "Falls die Datei verschlüsselt ist, Passwort eingeben.",
            password = backupImportPassword,
            confirmLabel = "Importieren",
            onPasswordChange = { backupImportPassword = it },
            onDismiss = {
                pendingBackupImportRaw = null
                showBackupImportDialog = false
            },
            onConfirm = {
                val raw = pendingBackupImportRaw
                if (raw.isNullOrBlank()) {
                    showBackupImportDialog = false
                } else {
                    val prepared = runCatching {
                        if (BackupCrypto.isEncryptedPayload(raw)) {
                            BackupCrypto.decrypt(raw, backupImportPassword.trim())
                        } else {
                            raw
                        }
                    }
                    showBackupImportDialog = false
                    pendingBackupImportRaw = null

                    prepared.onSuccess { decoded ->
                        viewModel.importBackupJson(decoded) { result ->
                            result.onSuccess {
                                scope.launch { snackbarHostState.showSnackbar("Backup importiert.") }
                            }.onFailure { throwable ->
                                val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
                                scope.launch { snackbarHostState.showSnackbar("Import fehlgeschlagen: $error") }
                            }
                        }
                    }.onFailure { throwable ->
                        val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
                        scope.launch { snackbarHostState.showSnackbar("Backup konnte nicht entschlüsselt werden: $error") }
                    }
                }
            }
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
            onShowFullLog = {
                showChangelogDialog = false
                showFullChangelogDialog = true
            },
            onDismiss = {
                showChangelogDialog = false
                viewModel.setLastSeenVersion(BuildConfig.VERSION_NAME)
            }
        )
    }

    if (showFullChangelogDialog) {
        FullChangelogDialog(
            versions = changelogTimeline(),
            onDismiss = {
                showFullChangelogDialog = false
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
            simpleModeEnabled = simpleModeEnabled,
            showSetupGuideCard = showSetupGuideCard,
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
            },
            onSimpleModeChange = { enabled ->
                viewModel.setSimpleModeEnabled(enabled)
            },
            onShowSetupGuideCardChange = { enabled ->
                viewModel.setShowSetupGuideCard(enabled)
            }
        )
    }

    if (appLockInitialized && appLockEnabled && !isAppUnlocked) {
        AppUnlockDialog(
            showBiometricButton = appLockBiometricEnabled && biometricAvailable && hostActivity != null,
            biometricError = biometricUnlockError,
            onUseBiometric = {
                val activity = hostActivity ?: return@AppUnlockDialog
                runBiometricUnlock(
                    activity = activity,
                    title = "App entsperren",
                    subtitle = "Mit Fingerabdruck oder Face entsperren",
                    onSuccess = {
                        isAppUnlocked = true
                        biometricUnlockError = null
                    },
                    onFailure = { error ->
                        biometricUnlockError = error
                    }
                )
            },
            onUnlock = { pin, onResult ->
                viewModel.verifyAppLockPin(pin) { result ->
                    if (result.success) {
                        isAppUnlocked = true
                        biometricUnlockError = null
                    }
                    onResult(result.success, result.message)
                }
            }
        )
    }

    val scheme = MaterialTheme.colorScheme
    val backgroundBrush = remember(
        isDarkMode,
        scheme.background,
        scheme.surface,
        scheme.surfaceVariant
    ) {
        val colors = if (isDarkMode) {
            listOf(
                scheme.background,
                scheme.surface,
                scheme.surfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            listOf(
                scheme.background,
                scheme.surface,
                scheme.surfaceVariant.copy(alpha = 0.55f)
            )
        }
        Brush.verticalGradient(colors)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val topBarColor = MaterialTheme.colorScheme.background.copy(
                alpha = if (isDarkMode) 0.94f else 0.98f
            )
            Column {
                Surface(color = topBarColor) {
                    Column {
                        TopAppBar(
                            title = {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "Prüfungs-Planer",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Klar organisiert für den Schulalltag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                if (selectedTab != HomeTab.GRADES) {
                                    FilledTonalIconButton(
                                        enabled = !isSyncingIcal,
                                        onClick = triggerManualRefresh
                                    ) {
                                        if (isSyncingIcal) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
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
                            containerColor = Color.Transparent,
                            divider = {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                                )
                            }
                        ) {
                            visibleTabs.forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = tab.shortTitle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                if (showSyncStatusStrip) {
                    SyncStatusStrip(
                        syncStatus = syncStatus,
                        onRepairIcalLink = {
                            iCalUrlPrimary = savedIcalUrls.getOrNull(0).orEmpty()
                            iCalUrlSecondary = savedIcalUrls.getOrNull(1).orEmpty()
                            importEventsToggle = importEventsEnabled
                            showIcalDialog = true
                        }
                    )
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
                HomeTab.EXAMS -> ExamsTabContent(
                    state = examsTabUiState,
                    onEvent = { event ->
                        when (event) {
                            ExamsTabEvent.OpenIcalImport -> {
                                iCalUrlPrimary = savedIcalUrls.getOrNull(0).orEmpty()
                                iCalUrlSecondary = savedIcalUrls.getOrNull(1).orEmpty()
                                importEventsToggle = importEventsEnabled
                                showIcalDialog = true
                            }
                            ExamsTabEvent.RefreshNow -> triggerManualRefresh()
                            ExamsTabEvent.OpenHelp -> {
                                showHelpDialog = true
                            }
                            ExamsTabEvent.OpenSyncDiagnostics -> {
                                showSyncDiagnosticsDialog = true
                            }
                            ExamsTabEvent.AddExam -> {
                                showAddDialog = true
                            }
                            ExamsTabEvent.HideSetupGuide -> {
                                viewModel.onExamsEvent(event)
                            }
                            is ExamsTabEvent.PlanStudy -> {
                                studyPlanExam = event.exam
                                studyPlanExamPresentation = buildExamPresentation(event.exam)
                            }
                            is ExamsTabEvent.DeleteExam -> {
                                val exam = event.exam
                                viewModel.onExamsEvent(event)
                                val deletedTitle = buildExamPresentation(exam).title
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "\"$deletedTitle\" gelöscht",
                                        actionLabel = "Rückgängig",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreExam(exam)
                                        snackbarHostState.showSnackbar("Prüfung wiederhergestellt.")
                                    }
                                }
                            }
                        }
                    }
                )

                HomeTab.TIMETABLE -> TimetableTabContent(
                    state = timetableTabUiState,
                    onEvent = { event ->
                        when (event) {
                            TimetableTabEvent.OpenIcalImport -> {
                                iCalUrlPrimary = savedIcalUrls.getOrNull(0).orEmpty()
                                iCalUrlSecondary = savedIcalUrls.getOrNull(1).orEmpty()
                                importEventsToggle = importEventsEnabled
                                showIcalDialog = true
                            }
                            TimetableTabEvent.ClearChanges -> viewModel.onTimetableEvent(event)
                        }
                    }
                )

                HomeTab.EVENTS -> AgendaTabContent(
                    state = agendaTabUiState,
                    onEvent = { event ->
                        when (event) {
                            AgendaTabEvent.OpenIcalImport -> {
                                iCalUrlPrimary = savedIcalUrls.getOrNull(0).orEmpty()
                                iCalUrlSecondary = savedIcalUrls.getOrNull(1).orEmpty()
                                importEventsToggle = importEventsEnabled
                                showIcalDialog = true
                            }
                            AgendaTabEvent.EnableEventsImportAndSync -> {
                                isSyncingIcal = true
                                viewModel.enableEventsImportAndRefresh { message ->
                                    isSyncingIcal = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                }
                            }
                            is AgendaTabEvent.AddCustomEvents -> {
                                viewModel.onAgendaEvent(event)
                                scope.launch {
                                    val label = if (event.events.size == 1) "Event gespeichert." else "${event.events.size} Events gespeichert."
                                    snackbarHostState.showSnackbar(label)
                                }
                            }
                            is AgendaTabEvent.DeleteCustomEvent -> {
                                viewModel.onAgendaEvent(event)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Event gelöscht.")
                                }
                            }
                            is AgendaTabEvent.UpdateCustomEvent -> {
                                viewModel.onAgendaEvent(event)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Event aktualisiert.")
                                }
                            }
                        }
                    }
                )

                HomeTab.GRADES -> GradesTabContent(
                    state = gradesTabUiState,
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
    primaryUrl: String,
    secondaryUrl: String,
    includeEvents: Boolean,
    isImporting: Boolean,
    onPrimaryUrlChange: (String) -> Unit,
    onSecondaryUrlChange: (String) -> Unit,
    onIncludeEventsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    var showUrl by rememberSaveable { mutableStateOf(primaryUrl.isBlank() && secondaryUrl.isBlank()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("iCal-Kalender verbinden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = primaryUrl,
                    onValueChange = onPrimaryUrlChange,
                    label = { Text("iCal-URL 1") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showUrl) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showUrl = !showUrl }) {
                            Icon(
                                imageVector = if (showUrl) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (showUrl) {
                                    "Link ausblenden"
                                } else {
                                    "Link anzeigen"
                                }
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = secondaryUrl,
                    onValueChange = onSecondaryUrlChange,
                    label = { Text("iCal-URL 2 (optional)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showUrl) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    }
                )

                Text(
                    text = if (showUrl) {
                        "Die Links werden lokal verschlüsselt gespeichert."
                    } else {
                        "Links sind aus Sicherheitsgründen ausgeblendet. Tippe auf das Auge zum Anzeigen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "schulNetz: Agenda > Schüler/-innenpläne > Exports > \"Diesen Plan im iCal Format abonnieren\" > Link kopieren (nicht öffnen). Beispiel: https://www.examplelink.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                enabled = !isImporting && primaryUrl.isNotBlank(),
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
    onSyncNow: () -> Unit,
    onShowSyncStatusStripChange: (Boolean) -> Unit,
    onOpenReminderSettings: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenIcalImport: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSyncDiagnostics: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onOpenAppLock: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    hasUnseenChangelog: Boolean
) {
    val scrollState = rememberScrollState()
    val dialogContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Einstellungen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Alles Wichtige an einem Ort",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsSectionCard(
                    title = "Anzeige",
                    containerColor = dialogContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Sync-Leiste anzeigen",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Zeigt zuletzt synchronisiert + Status direkt oben.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = showSyncStatusStrip,
                            onCheckedChange = onShowSyncStatusStripChange
                        )
                    }
                }

                SettingsSectionCard(
                    title = "Kalender & Sync",
                    containerColor = dialogContainer
                ) {
                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Jetzt synchronisieren")
                    }
                    QuickActionTile(
                        text = "Kalender verbinden",
                        subtitle = "iCal-Links prüfen oder ändern",
                        icon = Icons.Outlined.CloudDownload,
                        onClick = onOpenIcalImport
                    )
                    QuickActionTile(
                        text = "Benachrichtigungen",
                        subtitle = "Vorzeiten, Quiet Hours, Test",
                        icon = Icons.Outlined.NotificationsActive,
                        onClick = onOpenReminderSettings
                    )
                    QuickActionTile(
                        text = "Automatisch aktualisieren",
                        subtitle = "Zeitplan und Hintergrund-Sync",
                        icon = Icons.Outlined.Sync,
                        onClick = onOpenSyncSettings
                    )
                    QuickActionTile(
                        text = "Sync-Diagnose",
                        subtitle = "Status, Dauer und Fehlersuche",
                        icon = Icons.Outlined.Schedule,
                        onClick = onOpenSyncDiagnostics
                    )
                }

                SettingsSectionCard(
                    title = "Datenschutz & Sicherheit",
                    containerColor = dialogContainer
                ) {
                    QuickActionTile(
                        text = "App-Schutz (PIN)",
                        subtitle = "Optional mit Biometrie",
                        icon = Icons.Outlined.Lock,
                        onClick = onOpenAppLock
                    )
                    QuickActionTile(
                        text = "Datenschutz",
                        subtitle = "Sicherheit, Screenshots, lokale Daten",
                        icon = Icons.Outlined.Lock,
                        onClick = onOpenPrivacy
                    )
                }

                SettingsSectionCard(
                    title = "Daten",
                    containerColor = dialogContainer
                ) {
                    QuickActionTile(
                        text = "CSV/PDF Export",
                        subtitle = "Prüfungen und Agenda exportieren",
                        icon = Icons.Outlined.CloudDownload,
                        onClick = onOpenExport
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onExportBackup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Backup Export")
                        }
                        FilledTonalButton(
                            onClick = onImportBackup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Backup Import")
                        }
                    }
                }

                SettingsSectionCard(
                    title = "App",
                    containerColor = dialogContainer
                ) {
                    QuickActionTile(
                        text = "App anpassen",
                        subtitle = "Ansicht und Tabs verwalten",
                        icon = Icons.Outlined.MoreVert,
                        onClick = onOpenPersonalization
                    )
                    QuickActionTile(
                        text = "Hilfe",
                        subtitle = "Kurzanleitung und Troubleshooting",
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = onOpenHelp
                    )
                    QuickActionTile(
                        text = "Was ist neu",
                        subtitle = "Neue Funktionen der Version",
                        icon = Icons.Outlined.CalendarToday,
                        showAlertBadge = hasUnseenChangelog,
                        onClick = onOpenChangelog
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
private fun AppLockSettingsDialog(
    isEnabled: Boolean,
    biometricEnabled: Boolean,
    canUseBiometric: Boolean,
    onDismiss: () -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onEnable: (String, Boolean) -> Unit,
    onDisable: (String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var enableBiometricOnSetup by rememberSaveable { mutableStateOf(canUseBiometric) }
    val normalizedPin = pin.trim()
    val pinPattern = remember {
        Regex("^\\d{$APP_LOCK_MIN_PIN_DIGITS,$APP_LOCK_MAX_PIN_DIGITS}$")
    }
    val pinValid = normalizedPin.matches(pinPattern)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEnabled) "App-Schutz deaktivieren" else "App-Schutz aktivieren")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isEnabled) {
                        "Schutz ist aktiv. Du kannst ihn hier anpassen oder deaktivieren."
                    } else {
                        "Optionaler Schutz: PIN (und optional Biometrie)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isEnabled) {
                    if (canUseBiometric) {
                        SettingToggleRow(
                            label = "Biometrie zum Entsperren",
                            checked = biometricEnabled,
                            onCheckedChange = onBiometricEnabledChange
                        )
                    } else {
                        Text(
                            text = "Biometrie ist auf diesem Gerät gerade nicht verfügbar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (canUseBiometric) {
                    SettingToggleRow(
                        label = "Biometrie direkt aktivieren",
                        checked = enableBiometricOnSetup,
                        onCheckedChange = { enableBiometricOnSetup = it }
                    )
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter { it.isDigit() }.take(APP_LOCK_MAX_PIN_DIGITS)
                    },
                    label = {
                        Text(
                            if (isEnabled) "Aktuelle PIN" else "Neue PIN ($APP_LOCK_MIN_PIN_DIGITS-$APP_LOCK_MAX_PIN_DIGITS Ziffern)"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pinValid,
                onClick = {
                    if (isEnabled) {
                        onDisable(normalizedPin)
                    } else {
                        onEnable(normalizedPin, enableBiometricOnSetup && canUseBiometric)
                    }
                }
            ) {
                Text(if (isEnabled) "Deaktivieren" else "Aktivieren")
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
private fun AppUnlockDialog(
    showBiometricButton: Boolean,
    biometricError: String?,
    onUseBiometric: (() -> Unit)?,
    onUnlock: (String, (Boolean, String?) -> Unit) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var isChecking by rememberSaveable { mutableStateOf(false) }
    var localErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("App entsperren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (showBiometricButton) {
                        "Nutze Biometrie oder gib deine PIN ein."
                    } else {
                        "Bitte PIN eingeben, um fortzufahren."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter { it.isDigit() }.take(APP_LOCK_MAX_PIN_DIGITS)
                        localErrorMessage = null
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                if (!localErrorMessage.isNullOrBlank()) {
                    Text(
                        text = localErrorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (!biometricError.isNullOrBlank()) {
                    Text(
                        text = biometricError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isChecking && pin.trim().isNotBlank(),
                onClick = {
                    isChecking = true
                    onUnlock(pin.trim()) { success, message ->
                        isChecking = false
                        localErrorMessage = if (success) null else (message ?: "PIN falsch.")
                        if (success) {
                            pin = ""
                        }
                    }
                }
            ) {
                Text(if (isChecking) "Prüfe..." else "Entsperren")
            }
        },
        dismissButton = {
            if (showBiometricButton && onUseBiometric != null) {
                TextButton(onClick = onUseBiometric) {
                    Text("Biometrie")
                }
            }
        }
    )
}

@Composable
private fun OnboardingDialog(
    primaryUrl: String,
    secondaryUrl: String,
    includeEvents: Boolean,
    statusMessage: String,
    isBusy: Boolean,
    canFinish: Boolean,
    onPrimaryUrlChange: (String) -> Unit,
    onSecondaryUrlChange: (String) -> Unit,
    onIncludeEventsChange: (Boolean) -> Unit,
    onTest: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var showUrl by rememberSaveable { mutableStateOf(primaryUrl.isBlank() && secondaryUrl.isBlank()) }
    val hasAnyUrl = primaryUrl.isNotBlank() || secondaryUrl.isNotBlank()
    val statusColor = when {
        statusMessage.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
        canFinish -> MaterialTheme.colorScheme.primary
        statusMessage.contains("fehlgeschlagen", ignoreCase = true) -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start in 3 Schritten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1 Link", "2 Test", "3 Fertig").forEachIndexed { index, title ->
                        FilterChip(
                            selected = step == index,
                            onClick = { step = index },
                            label = { Text(title) }
                        )
                    }
                }

                if (step == 0) {
                    Text(
                        text = "Schritt 1: Füge 1-2 iCal-Links ein. schulNetz: Agenda > Schüler/-innenpläne > Exports > \"Diesen Plan im iCal Format abonnieren\" > Link kopieren (nicht öffnen). Beispiel: https://www.examplelink.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = primaryUrl,
                        onValueChange = onPrimaryUrlChange,
                        label = { Text("iCal-URL 1") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showUrl) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showUrl = !showUrl }) {
                                Icon(
                                    imageVector = if (showUrl) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (showUrl) {
                                        "Link ausblenden"
                                    } else {
                                        "Link anzeigen"
                                    }
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = secondaryUrl,
                        onValueChange = onSecondaryUrlChange,
                        label = { Text("iCal-URL 2 (optional)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showUrl) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        }
                    )

                    Text(
                        text = if (showUrl) {
                            "Die Links werden lokal verschlüsselt gespeichert."
                        } else {
                            "Links sind aus Sicherheitsgründen ausgeblendet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                if (step == 1) {
                    Text(
                        text = "Schritt 2: Teste die Verbindung.",
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
                                    "Import: Prüfungen, Lektionen und Events"
                                } else {
                                    "Import: Prüfungen und Lektionen"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = when {
                                    !hasAnyUrl -> "Noch kein Link eingegeben"
                                    secondaryUrl.isBlank() -> "Link 1: ${maskUrlForDisplay(primaryUrl)}"
                                    else -> "Link 1: ${maskUrlForDisplay(primaryUrl)}\nLink 2: ${maskUrlForDisplay(secondaryUrl)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (step == 2) {
                    Text(
                        text = "Schritt 3: Fertigstellen und loslegen.",
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
                                text = if (canFinish) "Alles bereit. Du kannst die App jetzt normal nutzen." else "Bitte zuerst Verbindung testen.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Später kannst du jederzeit oben mit ↻ aktualisieren.",
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
                        color = statusColor
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = !isBusy && step > 0,
                    onClick = { step -= 1 }
                ) {
                    Text("Zurück")
                }
                when (step) {
                    0 -> {
                        TextButton(
                            enabled = !isBusy && hasAnyUrl,
                            onClick = { step = 1 }
                        ) {
                            Text("Weiter")
                        }
                    }

                    1 -> {
                        TextButton(
                            enabled = !isBusy && hasAnyUrl,
                            onClick = onTest
                        ) {
                            Text(if (isBusy) "Prüfe..." else "Testen")
                        }
                        TextButton(
                            enabled = !isBusy && canFinish,
                            onClick = { step = 2 }
                        ) {
                            Text("Weiter")
                        }
                    }

                    else -> {
                        TextButton(
                            enabled = !isBusy && canFinish,
                            onClick = onFinish
                        ) {
                            Text(if (isBusy) "Sync..." else "Fertig")
                        }
                    }
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
                HelpSectionTitle("Schnellstart (2 Minuten)")
                HelpStepLine("1.", "SchulNetz öffnen -> Agenda -> Schüler/-innenpläne.")
                HelpStepLine("2.", "Exports öffnen -> \"Diesen Plan im iCal-Format abonnieren\".")
                HelpStepLine("3.", "iCal-Link kopieren (nicht öffnen).")
                HelpStepLine("4.", "In der App Link einfügen -> Testen -> Fertig.")
                HelpStepLine("5.", "Oben auf Aktualisieren tippen.")

                HelpSectionTitle("Was die Tabs machen")
                HelpBulletLine("Prüfungen: Countdown, Suche, Filter.")
                HelpBulletLine("Stundenplan: Lektionen mit Verschiebungen und Raumänderungen.")
                HelpBulletLine("Events: Gesamtagenda nach Zeit.")
                HelpBulletLine("Notenrechner: Durchschnitt, Zielnote, Punkte-Rechner.")

                HelpSectionTitle("Täglich")
                HelpStepLine("1.", "App öffnen.")
                HelpStepLine("2.", "Aktualisieren.")
                HelpStepLine("3.", "Nächste Prüfungen und Lektionen prüfen.")

                HelpSectionTitle("Wenn etwas nicht klappt")
                HelpBulletLine("Sync-Fehler: Link + Internet prüfen; bei HTTP 410 neuen iCal-Link erstellen.")
                HelpBulletLine("Keine Events: In iCal-Einstellungen den Event-Import aktivieren.")
                HelpBulletLine("Backup: Einstellungen -> Backup Export/Import.")
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
private fun HelpSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun HelpStepLine(step: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HelpBulletLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    message: String,
    password: String,
    confirmLabel: String,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Passwort (optional)") },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword }
                        ) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (showPassword) {
                                    "Passwort ausblenden"
                                } else {
                                    "Passwort anzeigen"
                                }
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
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
private fun PrivacyDialog(
    screenshotProtectionEnabled: Boolean,
    onScreenshotProtectionChange: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datenschutz") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Kurz erklärt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Daten bleiben lokal auf deinem Gerät.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "iCal-Links sind verschlüsselt gespeichert.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Für Sync wird nur dein iCal-Link abgerufen.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Berechtigungen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Benachrichtigungen werden erst dann angefragt, wenn du Erinnerungen wirklich nutzen willst.",
                    style = MaterialTheme.typography.bodySmall
                )
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Screenshots blockieren",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Aktiviert FLAG_SECURE gegen Mitschnitt in Apps/Recent-Screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = screenshotProtectionEnabled,
                            onCheckedChange = onScreenshotProtectionChange
                        )
                    }
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Momik-jpg/TestColdown")
                                )
                            )
                        }
                    }
                ) {
                    Text("Datenschutz-Infos öffnen")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDeleteConfirmDialog = true }
                ) {
                    Text("Alle lokalen Daten löschen")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Lokale Daten löschen?") },
            text = {
                Text(
                    text = "Das entfernt lokale Termine, Einstellungen und iCal-Links auf diesem Gerät. Dieser Schritt kann nicht rückgängig gemacht werden.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAllData()
                        onDismiss()
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun PersonalizationDialog(
    showTimetableTab: Boolean,
    showAgendaTab: Boolean,
    showExamCollisionBadges: Boolean,
    collisionRules: CollisionRuleSettings,
    accessibilityModeEnabled: Boolean,
    simpleModeEnabled: Boolean,
    showSetupGuideCard: Boolean,
    onDismiss: () -> Unit,
    onShowTimetableTabChange: (Boolean) -> Unit,
    onShowAgendaTabChange: (Boolean) -> Unit,
    onShowExamCollisionBadgesChange: (Boolean) -> Unit,
    onCollisionRulesChange: (CollisionRuleSettings) -> Unit,
    onAccessibilityModeChange: (Boolean) -> Unit,
    onSimpleModeChange: (Boolean) -> Unit,
    onShowSetupGuideCardChange: (Boolean) -> Unit
) {
    var showAdvancedCollisionRules by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalisieren") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                        SettingToggleRow(
                            label = "Stundenplan-Tab anzeigen",
                            checked = showTimetableTab,
                            onCheckedChange = onShowTimetableTabChange
                        )
                        SettingToggleRow(
                            label = "Agenda-Tab anzeigen",
                            checked = showAgendaTab,
                            onCheckedChange = onShowAgendaTabChange
                        )
                        SettingToggleRow(
                            label = "Setup-Hilfe anzeigen",
                            checked = showSetupGuideCard,
                            onCheckedChange = onShowSetupGuideCardChange
                        )
                        SettingToggleRow(
                            label = "Barrierefreiheit-Modus",
                            checked = accessibilityModeEnabled,
                            onCheckedChange = onAccessibilityModeChange
                        )
                        SettingToggleRow(
                            label = "Einfach-Modus (weniger Optionen)",
                            checked = simpleModeEnabled,
                            onCheckedChange = onSimpleModeChange
                        )
                        SettingToggleRow(
                            label = "Kollisions-Badges anzeigen",
                            checked = showExamCollisionBadges,
                            onCheckedChange = onShowExamCollisionBadgesChange
                        )
                    }
                }

                if (showExamCollisionBadges) {
                    TextButton(
                        onClick = { showAdvancedCollisionRules = !showAdvancedCollisionRules },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            if (showAdvancedCollisionRules) {
                                "Kollisionsregeln ausblenden"
                            } else {
                                "Kollisionsregeln anzeigen"
                            }
                        )
                    }
                }

                if (showExamCollisionBadges && showAdvancedCollisionRules) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Kollisionen - Erweitert",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            SettingToggleRow(
                                label = "Kollisionen mit Lektionen",
                                checked = collisionRules.includeLessonCollisions,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(includeLessonCollisions = it)
                                    )
                                }
                            )
                            SettingToggleRow(
                                label = "Kollisionen mit Events",
                                checked = collisionRules.includeEventCollisions,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(includeEventCollisions = it)
                                    )
                                }
                            )
                            SettingToggleRow(
                                label = "Nur anderes Fach",
                                checked = collisionRules.onlyDifferentSubject,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(onlyDifferentSubject = it)
                                    )
                                }
                            )
                            SettingToggleRow(
                                label = "Nur echte Zeitüberschneidung",
                                checked = collisionRules.requireExactTimeOverlap,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(requireExactTimeOverlap = it)
                                    )
                                }
                            )
                        }
                    }
                }
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
private fun PlanExamStudySessionsDialog(
    exam: Exam,
    presentation: ExamPresentation,
    onDismiss: () -> Unit,
    onSaveSessions: (List<SchoolEvent>) -> Unit
) {
    val context = LocalContext.current
    val schoolZone = remember { ZoneId.of("Europe/Zurich") }
    val storageKey = remember(exam.id) { exam.id }

    var studyStartWeeksBeforeRaw by rememberSaveable(storageKey) { mutableStateOf("3") }
    var studyDurationMinutesRaw by rememberSaveable(storageKey) { mutableStateOf("60") }
    var studySessionCountRaw by rememberSaveable(storageKey) { mutableStateOf("10") }
    var studyWeekdayValuesRaw by rememberSaveable(storageKey) { mutableStateOf("1,2,3,7") }
    var studyStartMinutesOfDay by rememberSaveable(storageKey) { mutableIntStateOf(17 * 60) }
    var studyValidationError by rememberSaveable(storageKey) { mutableStateOf<String?>(null) }

    val studyDurationPreview = studyDurationMinutesRaw.toIntOrNull()
    val studyStartWeeksPreview = studyStartWeeksBeforeRaw.toIntOrNull()
    val studySessionCountPreview = studySessionCountRaw.toIntOrNull()
    val selectedStudyWeekdays = remember(studyWeekdayValuesRaw) {
        studyWeekdayValuesRaw
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .distinct()
            .sorted()
            .map { DayOfWeek.of(it) }
    }

    val studyPreviewCount = remember(
        presentation.subject,
        presentation.title,
        exam.location,
        exam.startsAtEpochMillis,
        studyStartWeeksPreview,
        studyDurationPreview,
        studySessionCountPreview,
        selectedStudyWeekdays,
        studyStartMinutesOfDay
    ) {
        if (
            studyStartWeeksPreview == null ||
            studyDurationPreview == null ||
            studySessionCountPreview == null ||
            selectedStudyWeekdays.isEmpty()
        ) {
            null
        } else {
            buildExamStudySessions(
                subject = presentation.subject,
                examTitle = presentation.title,
                examLocation = exam.location,
                examStartsAtMillis = exam.startsAtEpochMillis,
                startWeeksBefore = studyStartWeeksPreview,
                durationMinutes = studyDurationPreview,
                targetSessions = studySessionCountPreview,
                weekdays = selectedStudyWeekdays.toSet(),
                startMinutesOfDay = studyStartMinutesOfDay,
                schoolZone = schoolZone
            ).size
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lern-Sessions planen") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Prüfung: ${formatExamDate(exam.startsAtEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = studyStartWeeksBeforeRaw,
                    onValueChange = {
                        studyStartWeeksBeforeRaw = it.filter(Char::isDigit).take(2)
                    },
                    label = { Text("Start vor Prüfung (Wochen)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = studyDurationMinutesRaw,
                    onValueChange = {
                        studyDurationMinutesRaw = it.filter(Char::isDigit).take(3)
                    },
                    label = { Text("Dauer pro Session (Min)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = studySessionCountRaw,
                    onValueChange = {
                        studySessionCountRaw = it.filter(Char::isDigit).take(3)
                    },
                    label = { Text("Anzahl Sessions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = "Wochentage",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    studyWeekdayOptions().forEach { option ->
                        val isSelected = option.dayOfWeek in selectedStudyWeekdays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val nextValues = selectedStudyWeekdays
                                    .map { it.value }
                                    .toMutableSet()
                                if (isSelected) {
                                    nextValues.remove(option.dayOfWeek.value)
                                } else {
                                    nextValues.add(option.dayOfWeek.value)
                                }
                                studyWeekdayValuesRaw = nextValues
                                    .toList()
                                    .sorted()
                                    .joinToString(",")
                            },
                            label = { Text(option.shortLabel) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        openTimePicker(
                            context = context,
                            initialMinutesOfDay = studyStartMinutesOfDay,
                            onPicked = { picked -> studyStartMinutesOfDay = picked }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Session-Uhrzeit: ${formatMinutesOfDay(studyStartMinutesOfDay)}")
                }

                val previewText = when {
                    studyPreviewCount == null -> null
                    studyPreviewCount == 0 -> "Aktuell würden keine Lern-Sessions vor der Prüfung entstehen."
                    studyPreviewCount == 1 -> "Es wird 1 Lern-Session erstellt."
                    studySessionCountPreview != null && studyPreviewCount < studySessionCountPreview ->
                        "Es passen nur $studyPreviewCount von ${studySessionCountPreview} Sessions in den Zeitraum."
                    else -> "Es werden $studyPreviewCount Lern-Sessions erstellt."
                }
                previewText?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                studyValidationError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startWeeksBefore = studyStartWeeksBeforeRaw.toIntOrNull()
                    val durationMinutes = studyDurationMinutesRaw.toIntOrNull()
                    val targetSessions = studySessionCountRaw.toIntOrNull()
                    val selectedWeekdays = studyWeekdayValuesRaw
                        .split(',')
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in 1..7 }
                        .distinct()
                        .sorted()
                        .map { DayOfWeek.of(it) }
                        .toSet()

                    when {
                        startWeeksBefore == null || startWeeksBefore !in 1..26 -> {
                            studyValidationError = "Bitte 1 bis 26 Wochen wählen."
                            return@TextButton
                        }
                        durationMinutes == null || durationMinutes !in 15..240 -> {
                            studyValidationError = "Bitte 15 bis 240 Minuten wählen."
                            return@TextButton
                        }
                        targetSessions == null || targetSessions !in 1..400 -> {
                            studyValidationError = "Anzahl Sessions: bitte 1 bis 400."
                            return@TextButton
                        }
                        selectedWeekdays.isEmpty() -> {
                            studyValidationError = "Wähle mindestens einen Wochentag."
                            return@TextButton
                        }
                        else -> {
                            val sessions = buildExamStudySessions(
                                subject = presentation.subject,
                                examTitle = presentation.title,
                                examLocation = exam.location,
                                examStartsAtMillis = exam.startsAtEpochMillis,
                                startWeeksBefore = startWeeksBefore,
                                durationMinutes = durationMinutes,
                                targetSessions = targetSessions,
                                weekdays = selectedWeekdays,
                                startMinutesOfDay = studyStartMinutesOfDay,
                                schoolZone = schoolZone
                            )
                            if (sessions.isEmpty()) {
                                studyValidationError = "Keine Lern-Sessions vor der Prüfung möglich. Prüfe Tage/Uhrzeit."
                                return@TextButton
                            }
                            if (sessions.size < targetSessions) {
                                studyValidationError = "Es passen nur ${sessions.size} von $targetSessions Sessions bis zur Prüfung."
                                return@TextButton
                            }
                            studyValidationError = null
                            onSaveSessions(sessions)
                            onDismiss()
                        }
                    }
                }
            ) {
                Text("Erstellen")
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
private fun AddExamDialog(
    onDismiss: () -> Unit,
    onSave: (String?, String, String?, Long, Long?, List<Long>, List<SchoolEvent>) -> Unit
) {
    val context = LocalContext.current
    val schoolZone = remember { ZoneId.of("Europe/Zurich") }
    var subject by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var selectedExamMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis() + 24L * 60L * 60L * 1000L)
    }
    var reminderEnabled by rememberSaveable { mutableStateOf(true) }
    var quickLeadTimesRaw by rememberSaveable { mutableStateOf("30, 1440") }
    var exactReminderEnabled by rememberSaveable { mutableStateOf(false) }
    var reminderManuallySet by rememberSaveable { mutableStateOf(false) }
    var selectedReminderMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis() + 24L * 60L * 60L * 1000L - 30L * 60L * 1000L)
    }
    var studyPlanEnabled by rememberSaveable { mutableStateOf(false) }
    var studyStartWeeksBeforeRaw by rememberSaveable { mutableStateOf("3") }
    var studyDurationMinutesRaw by rememberSaveable { mutableStateOf("60") }
    var studySessionCountRaw by rememberSaveable { mutableStateOf("10") }
    var studyWeekdayValuesRaw by rememberSaveable { mutableStateOf("1,2,3,7") }
    var studyStartMinutesOfDay by rememberSaveable { mutableIntStateOf(17 * 60) }
    var studyValidationError by rememberSaveable { mutableStateOf<String?>(null) }

    val quickLeadTimes = remember(quickLeadTimesRaw) {
        parseLeadTimesMinutes(quickLeadTimesRaw)
    }
    val selectedLeadTimes = remember(reminderEnabled, quickLeadTimes) {
        if (!reminderEnabled) {
            emptyList()
        } else {
            quickLeadTimes
        }
    }

    val reminderValidationError = when {
        !reminderEnabled -> null
        selectedLeadTimes.isEmpty() -> "Wähle mindestens eine Erinnerungszeit."
        exactReminderEnabled && selectedReminderMillis <= System.currentTimeMillis() -> "Erinnerungszeit liegt in der Vergangenheit"
        exactReminderEnabled && selectedReminderMillis >= selectedExamMillis -> "Erinnerung muss vor Prüfungsbeginn liegen"
        else -> null
    }
    val studyDurationPreview = studyDurationMinutesRaw.toIntOrNull()
    val studyStartWeeksPreview = studyStartWeeksBeforeRaw.toIntOrNull()
    val studySessionCountPreview = studySessionCountRaw.toIntOrNull()
    val selectedStudyWeekdays = remember(studyWeekdayValuesRaw) {
        studyWeekdayValuesRaw
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .distinct()
            .sorted()
            .map { DayOfWeek.of(it) }
    }
    val studyPreviewCount = remember(
        studyPlanEnabled,
        subject,
        title,
        location,
        selectedExamMillis,
        studyStartWeeksPreview,
        studyDurationPreview,
        studySessionCountPreview,
        selectedStudyWeekdays,
        studyStartMinutesOfDay
    ) {
        if (
            !studyPlanEnabled ||
            studyStartWeeksPreview == null ||
            studyDurationPreview == null ||
            studySessionCountPreview == null ||
            selectedStudyWeekdays.isEmpty()
        ) {
            null
        } else {
            buildExamStudySessions(
                subject = subject,
                examTitle = title.ifBlank { "Prüfung" },
                examLocation = location,
                examStartsAtMillis = selectedExamMillis,
                startWeeksBefore = studyStartWeeksPreview,
                durationMinutes = studyDurationPreview,
                targetSessions = studySessionCountPreview,
                weekdays = selectedStudyWeekdays.toSet(),
                startMinutesOfDay = studyStartMinutesOfDay,
                schoolZone = schoolZone
            ).size
        }
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Benachrichtigung aktiv",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { checked -> reminderEnabled = checked }
                    )
                }

                if (reminderEnabled) {
                    val quickOptions = listOf(
                        10L to "10 Min",
                        30L to "30 Min",
                        60L to "1 Std",
                        120L to "2 Std",
                        24L * 60L to "1 Tag",
                        2L * 24L * 60L to "2 Tage",
                        7L * 24L * 60L to "7 Tage"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickOptions.forEach { option ->
                            val isSelected = option.first in quickLeadTimes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    quickLeadTimesRaw = toggleLeadTimePreset(
                                        currentRaw = quickLeadTimesRaw,
                                        minutes = option.first
                                    )
                                },
                                label = { Text(option.second) }
                            )
                        }
                    }
                    Text(
                        text = "Mehrere Erinnerungen gleichzeitig möglich.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Exakte Erinnerungszeit",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lern-Sessions planen",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Switch(
                                checked = studyPlanEnabled,
                                onCheckedChange = { checked ->
                                    studyPlanEnabled = checked
                                    if (!checked) {
                                        studyValidationError = null
                                    }
                                }
                            )
                        }

                        if (studyPlanEnabled) {
                            OutlinedTextField(
                                value = studyStartWeeksBeforeRaw,
                                onValueChange = {
                                    studyStartWeeksBeforeRaw = it.filter(Char::isDigit).take(2)
                                },
                                label = { Text("Start vor Prüfung (Wochen)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = studyDurationMinutesRaw,
                                onValueChange = {
                                    studyDurationMinutesRaw = it.filter(Char::isDigit).take(3)
                                },
                                label = { Text("Dauer pro Session (Min)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = studySessionCountRaw,
                                onValueChange = {
                                    studySessionCountRaw = it.filter(Char::isDigit).take(3)
                                },
                                label = { Text("Anzahl Sessions") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Text(
                                text = "Wochentage",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                studyWeekdayOptions().forEach { option ->
                                    val isSelected = option.dayOfWeek in selectedStudyWeekdays
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val nextValues = selectedStudyWeekdays
                                                .map { it.value }
                                                .toMutableSet()
                                            if (isSelected) {
                                                nextValues.remove(option.dayOfWeek.value)
                                            } else {
                                                nextValues.add(option.dayOfWeek.value)
                                            }
                                            studyWeekdayValuesRaw = nextValues
                                                .toList()
                                                .sorted()
                                                .joinToString(",")
                                        },
                                        label = { Text(option.shortLabel) }
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    openTimePicker(
                                        context = context,
                                        initialMinutesOfDay = studyStartMinutesOfDay,
                                        onPicked = { picked -> studyStartMinutesOfDay = picked }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Session-Uhrzeit: ${formatMinutesOfDay(studyStartMinutesOfDay)}")
                            }

                            val previewText = when {
                                studyPreviewCount == null -> null
                                studyPreviewCount == 0 -> "Aktuell würden keine Lern-Sessions vor der Prüfung entstehen."
                                studyPreviewCount == 1 -> "Es wird 1 Lern-Session erstellt."
                                studySessionCountPreview != null && studyPreviewCount < studySessionCountPreview ->
                                    "Es passen nur $studyPreviewCount von ${studySessionCountPreview} Sessions in den Zeitraum."
                                else -> "Es werden $studyPreviewCount Lern-Sessions erstellt."
                            }
                            previewText?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            studyValidationError?.let { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && reminderValidationError == null,
                onClick = {
                    val generatedStudySessions = if (studyPlanEnabled) {
                        val startWeeksBefore = studyStartWeeksBeforeRaw.toIntOrNull()
                        val durationMinutes = studyDurationMinutesRaw.toIntOrNull()
                        val targetSessions = studySessionCountRaw.toIntOrNull()
                        val selectedWeekdays = studyWeekdayValuesRaw
                            .split(',')
                            .mapNotNull { it.trim().toIntOrNull() }
                            .filter { it in 1..7 }
                            .distinct()
                            .sorted()
                            .map { DayOfWeek.of(it) }
                            .toSet()
                        when {
                            startWeeksBefore == null || startWeeksBefore !in 1..26 -> {
                                studyValidationError = "Bitte 1 bis 26 Wochen wählen."
                                return@TextButton
                            }
                            durationMinutes == null || durationMinutes !in 15..240 -> {
                                studyValidationError = "Bitte 15 bis 240 Minuten wählen."
                                return@TextButton
                            }
                            targetSessions == null || targetSessions !in 1..400 -> {
                                studyValidationError = "Anzahl Sessions: bitte 1 bis 400."
                                return@TextButton
                            }
                            selectedWeekdays.isEmpty() -> {
                                studyValidationError = "Wähle mindestens einen Wochentag."
                                return@TextButton
                            }
                            else -> {
                                val sessions = buildExamStudySessions(
                                    subject = subject,
                                    examTitle = title,
                                    examLocation = location,
                                    examStartsAtMillis = selectedExamMillis,
                                    startWeeksBefore = startWeeksBefore,
                                    durationMinutes = durationMinutes,
                                    targetSessions = targetSessions,
                                    weekdays = selectedWeekdays,
                                    startMinutesOfDay = studyStartMinutesOfDay,
                                    schoolZone = schoolZone
                                )
                                if (sessions.isEmpty()) {
                                    studyValidationError = "Keine Lern-Sessions vor der Prüfung möglich. Prüfe Tage/Uhrzeit."
                                    return@TextButton
                                }
                                if (sessions.size < targetSessions) {
                                    studyValidationError = "Es passen nur ${sessions.size} von $targetSessions Sessions bis zur Prüfung."
                                    return@TextButton
                                }
                                studyValidationError = null
                                sessions
                            }
                        }
                    } else {
                        emptyList()
                    }

                    onSave(
                        subject.ifBlank { null },
                        title,
                        location.ifBlank { null },
                        selectedExamMillis,
                        if (reminderEnabled && exactReminderEnabled) selectedReminderMillis else null,
                        if (reminderEnabled) selectedLeadTimes else emptyList(),
                        generatedStudySessions
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

private fun buildExamStudySessions(
    subject: String?,
    examTitle: String,
    examLocation: String?,
    examStartsAtMillis: Long,
    startWeeksBefore: Int,
    durationMinutes: Int,
    targetSessions: Int,
    weekdays: Set<DayOfWeek>,
    startMinutesOfDay: Int,
    schoolZone: ZoneId
): List<SchoolEvent> {
    return PlanStudySessionsUseCase().invoke(
        PlanStudySessionsUseCase.Params(
            subject = subject,
            examTitle = examTitle,
            examLocation = examLocation,
            examStartsAtMillis = examStartsAtMillis,
            startWeeksBefore = startWeeksBefore,
            durationMinutes = durationMinutes,
            targetSessions = targetSessions,
            weekdays = weekdays,
            startMinutesOfDay = startMinutesOfDay,
            schoolZone = schoolZone
        )
    )
}

@Composable
private fun DurationPartsInputRow(
    daysRaw: String,
    hoursRaw: String,
    minutesRaw: String,
    onDaysChange: (String) -> Unit,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = daysRaw,
            onValueChange = { onDaysChange(it.filter(Char::isDigit).take(2)) },
            label = { Text("Tage") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = hoursRaw,
            onValueChange = { onHoursChange(it.filter(Char::isDigit).take(2)) },
            label = { Text("Std") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = minutesRaw,
            onValueChange = { onMinutesChange(it.filter(Char::isDigit).take(2)) },
            label = { Text("Min") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stille Zeiten aktiv",
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                startMinutes = 22 * 60
                                endMinutes = 7 * 60
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("22:00-07:00")
                        }
                        OutlinedButton(
                            onClick = {
                                startMinutes = 23 * 60
                                endMinutes = 6 * 60 + 30
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("23:00-06:30")
                        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30L, 60L, 180L, 360L).forEach { quick ->
                        OutlinedButton(
                            onClick = { intervalRaw = quick.toString() },
                            modifier = Modifier.width(84.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "$quick",
                                maxLines = 1,
                                softWrap = false
                            )
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

private fun isBiometricUnlockAvailable(context: Context): Boolean {
    val result = BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    )
    return result == BiometricManager.BIOMETRIC_SUCCESS
}

private fun runBiometricUnlock(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFailure: (String?) -> Unit
) {
    val cryptoCipher = runCatching { createBiometricCipher() }
        .getOrElse {
            onFailure("Biometrie konnte nicht sicher initialisiert werden. Bitte PIN verwenden.")
            return
        }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText("PIN verwenden")
        .setConfirmationRequired(false)
        .build()

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val isVerified = runCatching {
                    val cipher = result.cryptoObject?.cipher ?: return@runCatching false
                    cipher.doFinal(BIOMETRIC_UNLOCK_CHALLENGE).isNotEmpty()
                }.getOrDefault(false)

                if (!isVerified) {
                    onFailure("Biometrie-Validierung fehlgeschlagen. Bitte PIN verwenden.")
                    return
                }
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onFailure(null)
                    return
                }
                onFailure(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailure("Biometrie nicht erkannt. Bitte erneut versuchen.")
            }
        }
    )

    prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cryptoCipher))
}

private fun createBiometricCipher(): Cipher {
    val secretKey = getOrCreateBiometricSecretKey()
    return Cipher.getInstance(BIOMETRIC_CIPHER_TRANSFORMATION).apply {
        init(Cipher.ENCRYPT_MODE, secretKey)
    }
}

private fun getOrCreateBiometricSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(BIOMETRIC_KEYSTORE_PROVIDER).apply { load(null) }
    val existing = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing

    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        BIOMETRIC_KEYSTORE_PROVIDER
    )

    val builder = KeyGenParameterSpec.Builder(
        BIOMETRIC_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setUserAuthenticationRequired(true)
        .setInvalidatedByBiometricEnrollment(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setUserAuthenticationParameters(
            0,
            KeyProperties.AUTH_BIOMETRIC_STRONG
        )
    } else {
        @Suppress("DEPRECATION")
        builder.setUserAuthenticationValidityDurationSeconds(-1)
    }

    keyGenerator.init(builder.build())
    return keyGenerator.generateKey()
}


