package com.andrin.examcountdown.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.andrin.examcountdown.worker.ExamReminderWorker

class ExamReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != ACTION_SNOOZE_10_MIN) return

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
            delayMinutes = 10L
        )
    }

    companion object {
        const val ACTION_SNOOZE_10_MIN = "com.andrin.examcountdown.action.SNOOZE_10_MIN"
    }
}
