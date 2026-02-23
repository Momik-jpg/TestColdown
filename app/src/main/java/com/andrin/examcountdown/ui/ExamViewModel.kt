package com.andrin.examcountdown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.reminder.ExamReminderScheduler
import com.andrin.examcountdown.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExamRepository(application.applicationContext)

    val exams = repository.examsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
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
}
