package com.andrin.examcountdown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.data.IcalSyncEngine
import com.andrin.examcountdown.data.SyncStatus
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.reminder.ExamReminderScheduler
import com.andrin.examcountdown.worker.IcalSyncScheduler
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
    val savedIcalUrl = repository.iCalUrlFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
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
    val syncStatus = repository.syncStatusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncStatus()
    )

    fun addExam(
        title: String,
        location: String?,
        startsAtMillis: Long,
        reminderAtMillis: Long?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val exam = Exam(
                title = title.trim(),
                location = location?.trim()?.takeIf { it.isNotBlank() },
                startsAtEpochMillis = startsAtMillis,
                reminderAtEpochMillis = reminderAtMillis
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

    fun importFromIcal(url: String, onDone: (String) -> Unit) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            onDone("Bitte iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            repository.saveIcalUrl(trimmedUrl)
            val (success, message) = syncFromIcalUrl(
                url = trimmedUrl,
                emitChangeNotification = false
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

            val (_, message) = syncFromIcalUrl(
                url = savedUrl,
                emitChangeNotification = false
            )
            onDone(message)
        }
    }

    fun testIcalConnection(url: String, onDone: (Boolean, String) -> Unit) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            onDone(false, "Bitte iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            runCatching {
                val result = syncEngine.testConnection(trimmedUrl)
                "Verbindung erfolgreich. ${result.examsImported} Prüfungen und ${result.lessonsImported} Lektionen gefunden."
            }.onSuccess { message ->
                onDone(true, message)
            }.onFailure { throwable ->
                val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
                onDone(false, "Verbindung fehlgeschlagen: $error")
            }
        }
    }

    fun completeOnboarding(url: String, onDone: (Boolean, String) -> Unit) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            onDone(false, "Bitte iCal-URL eingeben.")
            return
        }

        viewModelScope.launch {
            repository.saveIcalUrl(trimmedUrl)
            val (success, message) = syncFromIcalUrl(
                url = trimmedUrl,
                emitChangeNotification = false
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

    private suspend fun syncFromIcalUrl(
        url: String,
        emitChangeNotification: Boolean
    ): Pair<Boolean, String> {
        return runCatching {
            val result = syncEngine.syncFromUrl(
                url = url,
                emitChangeNotification = emitChangeNotification
            )
            IcalSyncScheduler.schedule(getApplication())
            WidgetUpdater.updateAll(getApplication())
            true to result.summaryText()
        }.getOrElse { throwable ->
            val error = throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
            repository.markSyncError("Sync fehlgeschlagen: $error")
            false to "iCal-Sync fehlgeschlagen: $error"
        }
    }
}
