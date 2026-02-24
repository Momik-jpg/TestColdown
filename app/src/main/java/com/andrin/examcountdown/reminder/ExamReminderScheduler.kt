package com.andrin.examcountdown.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.data.QuietHoursConfig
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.worker.ExamReminderWorker
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object ExamReminderScheduler {
    private const val WORK_PREFIX = "exam-reminder-"
    private const val SNOOZE_WORK_PREFIX = "exam-reminder-snooze-"

    fun scheduleExamReminder(context: Context, exam: Exam) {
        val appContext = context.applicationContext
        val quietHours = runBlocking { ExamRepository(appContext).readQuietHoursConfig() }
        val triggers = resolveTriggerTimes(exam, quietHours)
        if (triggers.isEmpty()) {
            cancelExamReminder(context, exam.id)
            return
        }

        cancelExamReminder(context, exam.id)

        val now = System.currentTimeMillis()
        triggers.forEachIndexed { index, trigger ->
            if (trigger.triggerAtMillis <= now) return@forEachIndexed

            val inputData = Data.Builder()
                .putString(ExamReminderWorker.KEY_EXAM_ID, exam.id)
                .putString(ExamReminderWorker.KEY_TITLE, exam.title)
                .putString(ExamReminderWorker.KEY_LOCATION, exam.location)
                .putLong(ExamReminderWorker.KEY_STARTS_AT, exam.startsAtEpochMillis)
                .putString(ExamReminderWorker.KEY_REMINDER_LABEL, trigger.label)
                .build()

            val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
                .setInitialDelay(trigger.triggerAtMillis - now, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(workTag(exam.id))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueName(exam.id, index, trigger.triggerAtMillis),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancelExamReminder(context: Context, examId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(workTag(examId))
    }

    fun syncFromStoredExams(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val exams = ExamRepository(context.applicationContext).readSnapshot()
            exams.forEach { exam -> scheduleExamReminder(context, exam) }
        }
    }

    fun scheduleSnoozeReminder(
        context: Context,
        examId: String,
        title: String,
        location: String?,
        startsAtMillis: Long,
        delayMinutes: Long = 10L
    ) {
        val delay = delayMinutes.coerceIn(1L, 180L)
        val triggerAtMillis = System.currentTimeMillis() + delay * 60_000L
        if (triggerAtMillis >= startsAtMillis) return

        val inputData = Data.Builder()
            .putString(ExamReminderWorker.KEY_EXAM_ID, examId)
            .putString(ExamReminderWorker.KEY_TITLE, title)
            .putString(ExamReminderWorker.KEY_LOCATION, location)
            .putLong(ExamReminderWorker.KEY_STARTS_AT, startsAtMillis)
            .putString(ExamReminderWorker.KEY_REMINDER_LABEL, "Snooze (${delay} Min)")
            .build()

        val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "$SNOOZE_WORK_PREFIX$examId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private data class ReminderTrigger(
        val triggerAtMillis: Long,
        val label: String
    )

    private fun uniqueName(examId: String, index: Int, triggerAtMillis: Long): String {
        return "$WORK_PREFIX$examId-$index-$triggerAtMillis"
    }

    private fun workTag(examId: String): String = "exam-reminder-tag-$examId"

    private fun resolveTriggerTimes(
        exam: Exam,
        quietHours: QuietHoursConfig
    ): List<ReminderTrigger> {
        val raw = mutableListOf<ReminderTrigger>()

        exam.reminderAtEpochMillis?.let { exactAt ->
            raw += ReminderTrigger(
                triggerAtMillis = exactAt,
                label = "Geplante Erinnerung"
            )
        }

        val leadTimes = buildList {
            addAll(exam.reminderLeadTimesMinutes)
            exam.reminderMinutesBefore?.let { add(it) }
        }
            .map { it.coerceAtLeast(0L) }
            .filter { it > 0L }
            .distinct()
            .sorted()

        leadTimes.forEach { minutes ->
            val triggerAt = exam.startsAtEpochMillis - minutes * 60_000L
            raw += ReminderTrigger(
                triggerAtMillis = triggerAt,
                label = when {
                    minutes < 60L -> "$minutes Min vorher"
                    minutes % 60L == 0L -> "${minutes / 60L} Std vorher"
                    else -> "$minutes Min vorher"
                }
            )
        }

        return raw
            .mapNotNull { trigger ->
                val adjusted = adjustTriggerForQuietHours(
                    triggerAtMillis = trigger.triggerAtMillis,
                    examStartMillis = exam.startsAtEpochMillis,
                    quietHours = quietHours
                ) ?: return@mapNotNull null
                trigger.copy(triggerAtMillis = adjusted)
            }
            .filter { it.triggerAtMillis > System.currentTimeMillis() }
            .distinctBy { it.triggerAtMillis }
            .sortedBy { it.triggerAtMillis }
    }

    private fun adjustTriggerForQuietHours(
        triggerAtMillis: Long,
        examStartMillis: Long,
        quietHours: QuietHoursConfig
    ): Long? {
        if (!quietHours.enabled) return triggerAtMillis

        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(triggerAtMillis).atZone(zone).toLocalDateTime()
        val minuteOfDay = dateTime.hour * 60 + dateTime.minute

        val start = quietHours.startMinutesOfDay.coerceIn(0, 24 * 60 - 1)
        val end = quietHours.endMinutesOfDay.coerceIn(0, 24 * 60 - 1)
        if (start == end) return triggerAtMillis

        val isOvernight = start > end
        val inQuiet = if (isOvernight) {
            minuteOfDay >= start || minuteOfDay < end
        } else {
            minuteOfDay in start until end
        }
        if (!inQuiet) return triggerAtMillis

        val quietEnd = if (isOvernight && minuteOfDay >= start) {
            dateTime.toLocalDate().plusDays(1)
                .atTime(end / 60, end % 60)
        } else {
            dateTime.toLocalDate()
                .atTime(end / 60, end % 60)
        }

        val adjusted = quietEnd.atZone(zone).toInstant().toEpochMilli()
        if (adjusted >= examStartMillis) return null
        return adjusted
    }
}
