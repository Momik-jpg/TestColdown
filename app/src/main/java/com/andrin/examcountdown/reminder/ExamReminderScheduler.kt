package com.andrin.examcountdown.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.worker.ExamReminderWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ExamReminderScheduler {
    private const val WORK_PREFIX = "exam-reminder-"

    fun scheduleExamReminder(context: Context, exam: Exam) {
        val triggerAt = resolveTriggerTime(exam)
        if (triggerAt == null) {
            cancelExamReminder(context, exam.id)
            return
        }

        val now = System.currentTimeMillis()
        if (triggerAt <= now) {
            cancelExamReminder(context, exam.id)
            return
        }

        val inputData = Data.Builder()
            .putString(ExamReminderWorker.KEY_EXAM_ID, exam.id)
            .putString(ExamReminderWorker.KEY_TITLE, exam.title)
            .putString(ExamReminderWorker.KEY_LOCATION, exam.location)
            .putLong(ExamReminderWorker.KEY_STARTS_AT, exam.startsAtEpochMillis)
            .build()

        val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
            .setInitialDelay(triggerAt - now, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(exam.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelExamReminder(context: Context, examId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(examId))
    }

    fun syncFromStoredExams(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val exams = ExamRepository(context.applicationContext).readSnapshot()
            exams.forEach { exam -> scheduleExamReminder(context, exam) }
        }
    }

    private fun uniqueName(examId: String): String = "$WORK_PREFIX$examId"

    private fun resolveTriggerTime(exam: Exam): Long? {
        exam.reminderAtEpochMillis?.let { return it }
        exam.reminderMinutesBefore?.let { minutes ->
            return exam.startsAtEpochMillis - minutes * 60_000L
        }
        return null
    }
}
