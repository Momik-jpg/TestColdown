package com.andrin.examcountdown.worker

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrin.examcountdown.MainActivity
import com.andrin.examcountdown.R
import com.andrin.examcountdown.reminder.ExamNotificationManager
import com.andrin.examcountdown.util.formatExamDate

class ExamReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val examId = inputData.getString(KEY_EXAM_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val reminderLabel = inputData.getString(KEY_REMINDER_LABEL)
        val startsAt = inputData.getLong(KEY_STARTS_AT, -1L)
        if (startsAt <= 0L) return Result.failure()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return Result.success()
        }

        ExamNotificationManager.ensureChannel(applicationContext)

        val location = inputData.getString(KEY_LOCATION)
        val baseText = if (location.isNullOrBlank()) {
            "Start: ${formatExamDate(startsAt)}"
        } else {
            "Start: ${formatExamDate(startsAt)} · $location"
        }
        val contentText = if (reminderLabel.isNullOrBlank()) {
            baseText
        } else {
            "$baseText · $reminderLabel"
        }

        val openAppIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            examId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ExamNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_exam)
            .setContentTitle("Erinnerung: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(examId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_EXAM_ID = "exam_id"
        const val KEY_TITLE = "title"
        const val KEY_LOCATION = "location"
        const val KEY_STARTS_AT = "starts_at"
        const val KEY_REMINDER_LABEL = "reminder_label"
    }
}
