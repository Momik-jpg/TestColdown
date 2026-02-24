package com.andrin.examcountdown.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.andrin.examcountdown.worker.ExamReminderWorker

class ExamReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val delayMinutes = when (action) {
            ACTION_SNOOZE_10_MIN -> 10L
            ACTION_SNOOZE_30_MIN -> 30L
            else -> return
        }

        val examId = intent.getStringExtra(ExamReminderWorker.KEY_EXAM_ID) ?: return
        val title = intent.getStringExtra(ExamReminderWorker.KEY_TITLE) ?: return
        val location = intent.getStringExtra(ExamReminderWorker.KEY_LOCATION)
        val startsAt = intent.getLongExtra(ExamReminderWorker.KEY_STARTS_AT, -1L)
        if (startsAt <= 0L) return

        ExamReminderScheduler.scheduleSnoozeReminder(
            context = context.applicationContext,
            examId = examId,
            title = title,
            location = location,
            startsAtMillis = startsAt,
            delayMinutes = delayMinutes
        )
    }

    companion object {
        const val ACTION_SNOOZE_10_MIN = "com.andrin.examcountdown.action.SNOOZE_10_MIN"
        const val ACTION_SNOOZE_30_MIN = "com.andrin.examcountdown.action.SNOOZE_30_MIN"
    }
}
