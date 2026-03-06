package com.andrin.examcountdown.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        when (WidgetRefreshScheduler.WORKER_EXECUTION_MODE) {
            WidgetRefreshExecutionMode.TRIGGER_SYNC_ONLY -> {
                // Legacy path: trigger a normal sync instead of forcing direct widget render.
                // Widget rendering is handled after sync in IcalSyncEngine and by provider updates.
                IcalSyncScheduler.syncNow(applicationContext)
            }
        }
        return Result.success()
    }
}

internal enum class WidgetRefreshExecutionMode {
    TRIGGER_SYNC_ONLY
}

object WidgetRefreshScheduler {
    private const val WORK_NAME = "widget-refresh-work"
    internal val WORKER_EXECUTION_MODE: WidgetRefreshExecutionMode =
        WidgetRefreshExecutionMode.TRIGGER_SYNC_ONLY
    internal const val LEGACY_PERIODIC_REFRESH_ENABLED: Boolean = false
    internal fun workName(): String = WORK_NAME

    fun schedule(context: Context) {
        // Deprecated periodic widget refresh removed to avoid redundant background render work.
        // Keep this call for backward compatibility and cleanup any already scheduled legacy work.
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
