package com.andrin.examcountdown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andrin.examcountdown.data.AppBackup
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.data.IcalSyncEngine
import com.andrin.examcountdown.data.QuietHoursConfig
import com.andrin.examcountdown.data.SyncStatus
import com.andrin.examcountdown.data.toSyncErrorMessage
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.reminder.ExamNotificationManager
import com.andrin.examcountdown.reminder.ExamReminderScheduler
import com.andrin.examcountdown.worker.IcalSyncScheduler
import com.andrin.examcountdown.worker.ExamReminderWorker
import com.andrin.examcountdown.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
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

    fun addExam(
        subject: String?,
        title: String,
        location: String?,
        startsAtMillis: Long,
        reminderAtMillis: Long?,
        reminderLeadTimesMinutes: List<Long> = emptyList()
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
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            onDone("Bitte iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            repository.saveIcalUrl(trimmedUrl)
            repository.setImportEventsEnabled(includeEvents)
            val (success, message) = syncFromIcalUrl(
                url = trimmedUrl,
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
            val savedUrl = repository.readIcalUrl()
            if (savedUrl.isNullOrBlank()) {
                onDone("Bitte zuerst einmal eine iCal-URL eingeben.")
                return@launch
            }
            val includeEvents = repository.readImportEventsEnabled()

            val (_, message) = syncFromIcalUrl(
                url = savedUrl,
                emitChangeNotification = false,
                includeEvents = includeEvents
            )
            onDone(message)
        }
    }

    fun testIcalConnection(url: String, includeEvents: Boolean, onDone: (Boolean, String) -> Unit) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            onDone(false, "Bitte iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            runCatching {
                val result = syncEngine.testConnection(
                    url = trimmedUrl,
                    importEvents = includeEvents
                )
                val eventsInfo = if (includeEvents) " und ${result.eventsImported} Events" else ""
                "Verbindung erfolgreich. ${result.examsImported} Prüfungen, ${result.lessonsImported} Lektionen$eventsInfo gefunden."
            }.onSuccess { message ->
                onDone(true, message)
            }.onFailure { throwable ->
                val error = toSyncErrorMessage(throwable)
                onDone(false, "Verbindung fehlgeschlagen: $error")
            }
        }
    }

    fun completeOnboarding(url: String, includeEvents: Boolean, onDone: (Boolean, String) -> Unit) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            onDone(false, "Bitte iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            repository.saveIcalUrl(trimmedUrl)
            repository.setImportEventsEnabled(includeEvents)
            val (success, message) = syncFromIcalUrl(
                url = trimmedUrl,
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

    private suspend fun syncFromIcalUrl(
        url: String,
        emitChangeNotification: Boolean,
        includeEvents: Boolean
    ): Pair<Boolean, String> {
        return runCatching {
            val result = syncEngine.syncFromUrl(
                url = url,
                emitChangeNotification = emitChangeNotification,
                importEvents = includeEvents
            )
            IcalSyncScheduler.scheduleFromRepository(getApplication())
            WidgetUpdater.updateAll(getApplication())
            true to result.summaryText()
        }.getOrElse { throwable ->
            val error = toSyncErrorMessage(throwable)
            repository.markSyncError("Sync fehlgeschlagen: $error")
            false to "iCal-Sync fehlgeschlagen: $error"
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
            val savedUrl = repository.readIcalUrl()
            if (savedUrl.isNullOrBlank()) {
                onDone("Events-Import aktiviert. Bitte zuerst iCal-Link eingeben.")
                return@launch
            }
            val (_, message) = syncFromIcalUrl(
                url = savedUrl,
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
}
