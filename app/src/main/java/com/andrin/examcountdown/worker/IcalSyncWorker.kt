package com.andrin.examcountdown.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.data.SyncCoordinator
import com.andrin.examcountdown.data.SyncExecutionResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IcalSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return when (
            val result = SyncCoordinator.syncFromRepository(
                context = applicationContext,
                emitChangeNotification = true
            )
        ) {
            SyncExecutionResult.NoUrls -> Result.success()
            is SyncExecutionResult.Success -> Result.success()
            is SyncExecutionResult.Failed -> {
                if (result.retryable) Result.retry() else Result.success()
            }
        }
    }
}

object IcalSyncScheduler {
    private const val PERIODIC_WORK_NAME = "ical-sync-periodic"
    private const val IMMEDIATE_WORK_NAME = "ical-sync-immediate"
    private val scheduleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun schedule(context: Context, repeatIntervalMinutes: Long = ExamRepository.DEFAULT_SYNC_INTERVAL_MINUTES) {
        val interval = repeatIntervalMinutes.coerceIn(15L, 12L * 60L)
        val request = PeriodicWorkRequestBuilder<IcalSyncWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(networkConstraint())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleFromRepository(context: Context) {
        val appContext = context.applicationContext
        scheduleScope.launch {
            val interval = runCatching {
                ExamRepository(appContext).readSyncIntervalMinutes()
            }.getOrDefault(ExamRepository.DEFAULT_SYNC_INTERVAL_MINUTES)
            schedule(appContext, interval)
        }
    }

    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<IcalSyncWorker>()
            .setConstraints(networkConstraint())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun networkConstraint(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
