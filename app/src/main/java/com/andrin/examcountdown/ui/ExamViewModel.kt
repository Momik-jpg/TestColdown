package com.andrin.examcountdown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.andrin.examcountdown.data.AppBackup
import com.andrin.examcountdown.data.AppLockVerificationResult
import com.andrin.examcountdown.data.CollisionRuleSettings
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.data.IcalSyncEngine
import com.andrin.examcountdown.data.SyncDiagnostics
import com.andrin.examcountdown.data.QuietHoursConfig
import com.andrin.examcountdown.data.SyncCoordinator
import com.andrin.examcountdown.data.SyncExecutionResult
import com.andrin.examcountdown.data.SyncStatus
import com.andrin.examcountdown.data.toSyncErrorMessage
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.reminder.ExamNotificationManager
import com.andrin.examcountdown.reminder.ExamReminderScheduler
import com.andrin.examcountdown.ui.tabs.events.AgendaTabEvent
import com.andrin.examcountdown.ui.tabs.events.ExamsTabEvent
import com.andrin.examcountdown.ui.tabs.events.TimetableTabEvent
import com.andrin.examcountdown.ui.tabs.state.AgendaTabUiState
import com.andrin.examcountdown.ui.tabs.state.ExamsTabUiState
import com.andrin.examcountdown.ui.tabs.state.GradesTabUiState
import com.andrin.examcountdown.ui.tabs.state.TimetableTabUiState
import com.andrin.examcountdown.worker.IcalSyncScheduler
import com.andrin.examcountdown.worker.ExamReminderWorker
import com.andrin.examcountdown.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExamRepository(application.applicationContext)
    private val syncEngine = IcalSyncEngine(application.applicationContext)

    val exams = repository.examsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val lessons = repository.lessonsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val events = repository.eventsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val timetableChanges = repository.timetableChangesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val savedIcalUrls = repository.iCalUrlsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val savedIcalUrl = repository.iCalUrlFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )
    val importEventsEnabled = repository.importEventsEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val onboardingDone = repository.onboardingDoneFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val onboardingPromptSeen = repository.onboardingPromptSeenFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val preferencesLoaded = repository.preferencesLoadedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val quietHours = repository.quietHoursFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = QuietHoursConfig()
    )
    val syncStatus = repository.syncStatusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncStatus()
    )
    val syncDiagnostics = repository.syncDiagnosticsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncDiagnostics()
    )
    val syncIntervalMinutes = repository.syncIntervalMinutesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExamRepository.DEFAULT_SYNC_INTERVAL_MINUTES
    )
    val showSyncStatusStrip = repository.showSyncStatusStripFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )
    val showTimetableTab = repository.showTimetableTabFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )
    val showAgendaTab = repository.showAgendaTabFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )
    val showExamCollisionBadges = repository.showExamCollisionBadgesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val collisionRuleSettings = repository.collisionRuleSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollisionRuleSettings()
    )
    val accessibilityModeEnabled = repository.accessibilityModeEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val simpleModeEnabled = repository.simpleModeEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )
    val lastSeenVersion = repository.lastSeenVersionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )
    val showSetupGuideCard = repository.showSetupGuideCardFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )
    val appLockEnabled = repository.appLockEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val appLockBiometricEnabled = repository.appLockBiometricEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val screenshotProtectionEnabled = repository.screenshotProtectionEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    val examsTabUiState = combine(
        combine(exams, lessons, events) { exams, lessons, events ->
            Triple(exams, lessons, events)
        },
        combine(showExamCollisionBadges, collisionRuleSettings) { showCollisionBadges, collisionRules ->
            showCollisionBadges to collisionRules
        },
        combine(savedIcalUrls, syncStatus) { urls, syncStatus ->
            urls to syncStatus
        },
        combine(simpleModeEnabled, showSetupGuideCard) { simpleModeEnabled, showSetupGuideCard ->
            simpleModeEnabled to showSetupGuideCard
        }
    ) { examData, collisionData, syncData, preferenceData ->
        val (exams, lessons, events) = examData
        val (showCollisionBadges, collisionRules) = collisionData
        val (urls, syncStatus) = syncData
        val (simpleModeEnabled, showSetupGuideCard) = preferenceData

        ExamsTabUiState(
            exams = exams,
            lessons = lessons,
            events = events,
            showCollisionBadges = showCollisionBadges,
            collisionRules = collisionRules,
            hasIcalUrl = urls.isNotEmpty(),
            hasSyncedOnce = syncStatus.lastSyncAtMillis != null,
            lastSyncError = syncStatus.lastSyncError,
            simpleModeEnabled = simpleModeEnabled,
            showSetupGuideCard = showSetupGuideCard
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExamsTabUiState()
    )
    val timetableTabUiState = combine(
        lessons,
        timetableChanges,
        savedIcalUrls
    ) { lessons, changes, urls ->
        TimetableTabUiState(
            lessons = lessons,
            changes = changes,
            hasIcalUrl = urls.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimetableTabUiState()
    )
    val agendaTabUiState = combine(
        exams,
        lessons,
        events,
        savedIcalUrls,
        importEventsEnabled
    ) { exams, lessons, events, urls, importEventsEnabled ->
        AgendaTabUiState(
            exams = exams,
            lessons = lessons,
            events = events,
            hasIcalUrl = urls.isNotEmpty(),
            importEventsEnabled = importEventsEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AgendaTabUiState()
    )
    val gradesTabUiState = preferencesLoaded
        .combine(savedIcalUrls) { preferencesLoaded, _ ->
            GradesTabUiState(preferencesLoaded = preferencesLoaded)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GradesTabUiState()
        )

    init {
        viewModelScope.launch {
            repository.migrateLegacyIcalUrlIfNeeded()
        }
    }

    fun addExam(
        subject: String?,
        title: String,
        location: String?,
        startsAtMillis: Long,
        reminderAtMillis: Long?,
        reminderLeadTimesMinutes: List<Long> = emptyList(),
        studySessions: List<SchoolEvent> = emptyList()
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val normalizedLeadTimes = reminderLeadTimesMinutes
                .map { it.coerceAtLeast(1L) }
                .distinct()
                .sorted()

            val exam = Exam(
                title = title.trim(),
                subject = subject?.trim()?.takeIf { it.isNotBlank() },
                location = location?.trim()?.takeIf { it.isNotBlank() },
                startsAtEpochMillis = startsAtMillis,
                reminderAtEpochMillis = reminderAtMillis,
                reminderLeadTimesMinutes = normalizedLeadTimes
            )
            repository.addExam(exam)
            if (studySessions.isNotEmpty()) {
                repository.addCustomEvents(studySessions)
            }
            ExamReminderScheduler.scheduleExamReminder(getApplication(), exam)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            repository.deleteExam(examId)
            ExamReminderScheduler.cancelExamReminder(getApplication(), examId)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun restoreExam(exam: Exam) {
        viewModelScope.launch {
            repository.addExam(exam)
            ExamReminderScheduler.scheduleExamReminder(getApplication(), exam)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun importFromIcal(url: String, includeEvents: Boolean, onDone: (String) -> Unit) {
        importFromIcal(urls = listOf(url), includeEvents = includeEvents, onDone = onDone)
    }

    fun importFromIcal(urls: List<String>, includeEvents: Boolean, onDone: (String) -> Unit) {
        val normalizedUrls = normalizeIcalInputUrls(urls)
        if (normalizedUrls.isEmpty()) {
            onDone("Bitte mindestens eine iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            repository.saveIcalUrls(normalizedUrls)
            repository.setImportEventsEnabled(includeEvents)
            val (success, message) = syncFromIcalUrls(
                urls = normalizedUrls,
                emitChangeNotification = false,
                includeEvents = includeEvents
            )
            if (success) {
                repository.setOnboardingDone(true)
            }
            onDone(message)
        }
    }

    fun refreshFromSavedIcal(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val savedUrls = repository.readIcalUrls()
            if (savedUrls.isEmpty()) {
                onDone("Bitte zuerst mindestens eine iCal-URL eingeben.")
                return@launch
            }
            val includeEvents = repository.readImportEventsEnabled()

            val (_, message) = syncFromIcalUrls(
                urls = savedUrls,
                emitChangeNotification = false,
                includeEvents = includeEvents
            )
            onDone(message)
        }
    }

    fun testIcalConnection(url: String, includeEvents: Boolean, onDone: (Boolean, String) -> Unit) {
        testIcalConnection(urls = listOf(url), includeEvents = includeEvents, onDone = onDone)
    }

    fun testIcalConnection(urls: List<String>, includeEvents: Boolean, onDone: (Boolean, String) -> Unit) {
        val normalizedUrls = normalizeIcalInputUrls(urls)
        if (normalizedUrls.isEmpty()) {
            onDone(false, "Bitte mindestens eine iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            runCatching {
                var examsImported = 0
                var lessonsImported = 0
                var eventsImported = 0

                normalizedUrls.forEach { link ->
                    val result = syncEngine.testConnection(
                        url = link,
                        importEvents = includeEvents
                    )
                    examsImported += result.examsImported
                    lessonsImported += result.lessonsImported
                    eventsImported += result.eventsImported
                }
                val eventsInfo = if (includeEvents) " und $eventsImported Events" else ""
                val linkInfo = if (normalizedUrls.size > 1) " aus ${normalizedUrls.size} Links" else ""
                "Verbindung erfolgreich. $examsImported Prüfungen, $lessonsImported Lektionen$eventsInfo$linkInfo gefunden."
            }.onSuccess { message ->
                onDone(true, message)
            }.onFailure { throwable ->
                val error = toSyncErrorMessage(throwable)
                onDone(false, "Verbindung fehlgeschlagen: $error")
            }
        }
    }

    fun completeOnboarding(url: String, includeEvents: Boolean, onDone: (Boolean, String) -> Unit) {
        completeOnboarding(urls = listOf(url), includeEvents = includeEvents, onDone = onDone)
    }

    fun completeOnboarding(urls: List<String>, includeEvents: Boolean, onDone: (Boolean, String) -> Unit) {
        val normalizedUrls = normalizeIcalInputUrls(urls)
        if (normalizedUrls.isEmpty()) {
            onDone(false, "Bitte mindestens eine iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            repository.saveIcalUrls(normalizedUrls)
            repository.setImportEventsEnabled(includeEvents)
            val (success, message) = syncFromIcalUrls(
                urls = normalizedUrls,
                emitChangeNotification = false,
                includeEvents = includeEvents
            )
            if (success) {
                repository.setOnboardingDone(true)
            }
            onDone(success, message)
        }
    }

    fun dismissOnboardingPrompt() {
        viewModelScope.launch {
            repository.setOnboardingPromptSeen(true)
        }
    }

    suspend fun markOnboardingPromptSeen() {
        repository.setOnboardingPromptSeen(true)
    }

    fun saveQuietHours(config: QuietHoursConfig, onDone: (String) -> Unit) {
        viewModelScope.launch {
            repository.saveQuietHours(config)
            ExamReminderScheduler.syncFromStoredExams(getApplication())
            onDone("Benachrichtigungszeiten gespeichert.")
        }
    }

    fun clearTimetableChanges() {
        viewModelScope.launch {
            repository.clearTimetableChanges()
        }
    }

    fun exportBackupJson(onDone: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { repository.exportBackupJson() }
            onDone(result)
        }
    }

    fun importBackupJson(raw: String, onDone: (Result<AppBackup>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val backup = repository.importBackupJson(raw)
                ExamReminderScheduler.syncFromStoredExams(getApplication())
                IcalSyncScheduler.scheduleFromRepository(getApplication())
                WidgetUpdater.updateAll(getApplication())
                backup
            }
            onDone(result)
        }
    }

    private suspend fun syncFromIcalUrls(
        urls: List<String>,
        emitChangeNotification: Boolean,
        includeEvents: Boolean
    ): Pair<Boolean, String> {
        return when (
            val result = SyncCoordinator.syncExplicit(
                context = getApplication(),
                urls = urls,
                includeEvents = includeEvents,
                emitChangeNotification = emitChangeNotification
            )
        ) {
            SyncExecutionResult.NoUrls -> {
                false to "Bitte zuerst mindestens eine iCal-URL eingeben."
            }
            is SyncExecutionResult.Success -> {
                IcalSyncScheduler.scheduleFromRepository(getApplication())
                true to result.result.summaryText()
            }
            is SyncExecutionResult.Failed -> {
                false to "iCal-Sync fehlgeschlagen: ${result.message}"
            }
        }
    }

    fun saveSyncIntervalMinutes(minutes: Long, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val normalized = minutes.coerceIn(15L, 12L * 60L)
            repository.saveSyncIntervalMinutes(normalized)
            IcalSyncScheduler.schedule(getApplication(), normalized)
            onDone("Auto-Sync alle $normalized Minuten gespeichert.")
        }
    }

    fun sendTestNotification(onDone: (String) -> Unit) {
        viewModelScope.launch {
            ExamNotificationManager.ensureChannel(getApplication())
            ExamReminderWorker.showImmediateNotification(
                context = getApplication(),
                examId = "test-notification",
                title = "Test-Erinnerung",
                location = "App-Test",
                startsAtMillis = System.currentTimeMillis() + 60L * 60L * 1000L,
                reminderLabel = "Manueller Test"
            )
            onDone("Test-Benachrichtigung gesendet.")
        }
    }

    fun setShowSyncStatusStrip(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowSyncStatusStrip(enabled)
        }
    }

    fun setImportEventsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setImportEventsEnabled(enabled)
        }
    }

    fun enableEventsImportAndRefresh(onDone: (String) -> Unit) {
        viewModelScope.launch {
            repository.setImportEventsEnabled(true)
            val savedUrls = repository.readIcalUrls()
            if (savedUrls.isEmpty()) {
                onDone("Events-Import aktiviert. Bitte zuerst iCal-Link eingeben.")
                return@launch
            }
            val (_, message) = syncFromIcalUrls(
                urls = savedUrls,
                emitChangeNotification = false,
                includeEvents = true
            )
            onDone(message)
        }
    }

    fun setShowTimetableTab(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowTimetableTab(enabled)
        }
    }

    fun setShowAgendaTab(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowAgendaTab(enabled)
        }
    }

    fun setShowExamCollisionBadges(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowExamCollisionBadges(enabled)
        }
    }

    fun saveCollisionRuleSettings(settings: CollisionRuleSettings) {
        viewModelScope.launch {
            repository.saveCollisionRuleSettings(settings)
        }
    }

    fun setAccessibilityModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAccessibilityModeEnabled(enabled)
        }
    }

    fun setSimpleModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSimpleModeEnabled(enabled)
        }
    }

    fun setLastSeenVersion(versionName: String) {
        viewModelScope.launch {
            repository.setLastSeenVersion(versionName)
        }
    }

    fun setShowSetupGuideCard(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowSetupGuideCard(enabled)
        }
    }

    fun onExamsEvent(event: ExamsTabEvent) {
        when (event) {
            ExamsTabEvent.HideSetupGuide -> setShowSetupGuideCard(false)
            is ExamsTabEvent.DeleteExam -> deleteExam(event.exam.id)
            ExamsTabEvent.OpenIcalImport,
            ExamsTabEvent.RefreshNow,
            ExamsTabEvent.OpenHelp,
            ExamsTabEvent.OpenSyncDiagnostics,
            ExamsTabEvent.AddExam,
            is ExamsTabEvent.PlanStudy -> Unit
        }
    }

    fun onTimetableEvent(event: TimetableTabEvent) {
        when (event) {
            TimetableTabEvent.ClearChanges -> clearTimetableChanges()
            TimetableTabEvent.OpenIcalImport -> Unit
        }
    }

    fun onAgendaEvent(event: AgendaTabEvent) {
        when (event) {
            is AgendaTabEvent.AddCustomEvents -> addCustomEvents(event.events)
            is AgendaTabEvent.DeleteCustomEvent -> deleteCalendarEvent(event.eventId)
            is AgendaTabEvent.UpdateCustomEvent -> updateCalendarEvent(event.event)
            AgendaTabEvent.OpenIcalImport,
            AgendaTabEvent.EnableEventsImportAndSync -> Unit
        }
    }

    fun enableAppLock(pin: String, biometricEnabled: Boolean, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.enableAppLockWithPin(pin, biometricEnabled = biometricEnabled)
            }.onSuccess {
                onDone(true, "App-Schutz aktiviert.")
            }.onFailure { throwable ->
                val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Ungültige PIN."
                onDone(false, error)
            }
        }
    }

    fun disableAppLock(pin: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val verifyResult = repository.verifyAppLockPinDetailed(pin)
            if (!verifyResult.success) {
                onDone(false, verifyResult.message ?: "PIN falsch.")
                return@launch
            }
            repository.disableAppLock()
            onDone(true, "App-Schutz deaktiviert.")
        }
    }

    fun verifyAppLockPin(pin: String, onDone: (AppLockVerificationResult) -> Unit) {
        viewModelScope.launch {
            onDone(repository.verifyAppLockPinDetailed(pin))
        }
    }

    fun setAppLockBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppLockBiometricEnabled(enabled)
        }
    }

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setScreenshotProtectionEnabled(enabled)
        }
    }

    fun clearAllLocalData(onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val appContext = getApplication<Application>().applicationContext
                WorkManager.getInstance(appContext).cancelAllWork()
                repository.clearAllLocalData()
                IcalSyncScheduler.scheduleFromRepository(appContext)
                WidgetUpdater.updateAll(appContext)
            }.onSuccess {
                onDone("Alle lokalen Daten wurden gelöscht.")
            }.onFailure { throwable ->
                val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
                onDone("Löschen fehlgeschlagen: $error")
            }
        }
    }

    fun addCustomEvents(events: List<SchoolEvent>) {
        if (events.isEmpty()) return
        viewModelScope.launch {
            repository.addCustomEvents(events)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun deleteCalendarEvent(eventId: String) {
        if (eventId.isBlank()) return
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun updateCalendarEvent(event: SchoolEvent) {
        if (event.id.isBlank()) return
        viewModelScope.launch {
            repository.updateEvent(event)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    private fun normalizeIcalInputUrls(urls: List<String>): List<String> {
        return urls
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(ExamRepository.MAX_ICAL_URLS)
    }
}
