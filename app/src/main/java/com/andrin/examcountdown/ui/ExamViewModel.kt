package com.andrin.examcountdown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andrin.examcountdown.data.IcalImporter
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.reminder.ExamReminderScheduler
import com.andrin.examcountdown.worker.IcalSyncScheduler
import com.andrin.examcountdown.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExamRepository(application.applicationContext)
    private val iCalImporter = IcalImporter()

    val exams = repository.examsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val savedIcalUrl = repository.iCalUrlFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
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
            val message = runCatching {
                val importResult = iCalImporter.importFromUrl(trimmedUrl)
                repository.replaceIcalImportedExams(importResult.exams)
                repository.saveIcalUrl(trimmedUrl)
                IcalSyncScheduler.schedule(getApplication())
                WidgetUpdater.updateAll(getApplication())
                importResult.message
            }.getOrElse { throwable ->
                "iCal-Import fehlgeschlagen: ${throwable.message ?: "Unbekannter Fehler"}"
            }

            onDone(message)
        }
    }
}
