package com.andrin.examcountdown

import android.app.Application
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.reminder.ExamNotificationManager
import com.andrin.examcountdown.reminder.ExamReminderScheduler
import com.andrin.examcountdown.reminder.TimetableSyncNotificationManager
import com.andrin.examcountdown.util.SchoolTime
import com.andrin.examcountdown.worker.IcalSyncScheduler
import com.andrin.examcountdown.worker.WidgetRefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExamCountdownApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ExamNotificationManager.ensureChannel(this)
        TimetableSyncNotificationManager.ensureChannel(this)
        ExamReminderScheduler.syncFromStoredExams(this)
        IcalSyncScheduler.scheduleFromRepository(this)
        appScope.launch {
            val shouldTriggerImmediateSync = runCatching {
                val status = ExamRepository(applicationContext).readSyncStatus()
                val lastSync = status.lastSyncAtMillis ?: return@runCatching true
                val ageMillis = (SchoolTime.nowMillis() - lastSync).coerceAtLeast(0L)
                ageMillis >= STARTUP_SYNC_STALE_THRESHOLD_MILLIS
            }.getOrDefault(true)

            if (shouldTriggerImmediateSync) {
                IcalSyncScheduler.syncNow(applicationContext)
            }
        }
        WidgetRefreshScheduler.schedule(this)
    }

    companion object {
        private const val STARTUP_SYNC_STALE_THRESHOLD_MILLIS = 6L * 60L * 60L * 1000L
    }
}
