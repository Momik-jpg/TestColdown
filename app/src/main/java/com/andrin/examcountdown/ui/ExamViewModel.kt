package com.andrin.examcountdown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andrin.examcountdown.data.IcalImporter
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.data.TimetableIcalImporter
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
    private val timetableImporter = TimetableIcalImporter()

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
            onDone(syncFromIcalUrl(trimmedUrl))
        }
    }

    fun refreshFromSavedIcal(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val savedUrl = repository.readIcalUrl()
            if (savedUrl.isNullOrBlank()) {
                onDone("Bitte zuerst einmal eine iCal-URL eingeben.")
                return@launch
            }

            onDone(syncFromIcalUrl(savedUrl))
        }
    }

    private suspend fun syncFromIcalUrl(url: String): String {
        return runCatching {
            val importResult = iCalImporter.importFromUrl(url)
            val timetableResult = timetableImporter.importFromUrl(url)
            repository.replaceIcalImportedExams(importResult.exams)
            repository.replaceSyncedLessons(timetableResult.lessons)
            IcalSyncScheduler.schedule(getApplication())
            WidgetUpdater.updateAll(getApplication())
            "${importResult.exams.size} Prüfungen und ${timetableResult.lessons.size} Lektionen synchronisiert."
        }.getOrElse { throwable ->
            "iCal-Sync fehlgeschlagen: ${throwable.message ?: "Unbekannter Fehler"}"
        }
    }
}
