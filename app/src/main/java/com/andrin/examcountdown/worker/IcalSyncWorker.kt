package com.andrin.examcountdown.worker

import android.content.Context
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
import com.andrin.examcountdown.data.IcalImporter
import com.andrin.examcountdown.widget.WidgetUpdater
import java.io.IOException
import java.util.concurrent.TimeUnit

class IcalSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = ExamRepository(applicationContext)
        val iCalUrl = repository.readIcalUrl() ?: return Result.success()

        return try {
            val importResult = IcalImporter().importFromUrl(iCalUrl)
            repository.replaceIcalImportedExams(importResult.exams)
            WidgetUpdater.updateAll(applicationContext)
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.success()
        }
    }
}

object IcalSyncScheduler {
    private const val PERIODIC_WORK_NAME = "ical-sync-periodic"
    private const val IMMEDIATE_WORK_NAME = "ical-sync-immediate"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<IcalSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(networkConstraint())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<IcalSyncWorker>()
            .setConstraints(networkConstraint())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun networkConstraint(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
