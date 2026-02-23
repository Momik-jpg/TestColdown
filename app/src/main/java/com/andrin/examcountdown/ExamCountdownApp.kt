package com.andrin.examcountdown

import android.app.Application
import com.andrin.examcountdown.reminder.ExamNotificationManager
import com.andrin.examcountdown.reminder.ExamReminderScheduler
import com.andrin.examcountdown.worker.WidgetRefreshScheduler

class ExamCountdownApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ExamNotificationManager.ensureChannel(this)
        ExamReminderScheduler.syncFromStoredExams(this)
        WidgetRefreshScheduler.schedule(this)
    }
}
