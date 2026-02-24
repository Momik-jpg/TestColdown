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
import com.andrin.examcountdown.reminder.ExamReminderActionReceiver
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

        val location = inputData.getString(KEY_LOCATION)
        showImmediateNotification(
            context = applicationContext,
            examId = examId,
            title = title,
            location = location,
            startsAtMillis = startsAt,
            reminderLabel = reminderLabel
        )
        return Result.success()
    }

    companion object {
        const val KEY_EXAM_ID = "exam_id"
        const val KEY_TITLE = "title"
        const val KEY_LOCATION = "location"
        const val KEY_STARTS_AT = "starts_at"
        const val KEY_REMINDER_LABEL = "reminder_label"

        fun showImmediateNotification(
            context: Context,
            examId: String,
            title: String,
            location: String?,
            startsAtMillis: Long,
            reminderLabel: String?
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!permissionGranted) return
            }

            ExamNotificationManager.ensureChannel(context)

            val baseText = if (location.isNullOrBlank()) {
                "Start: ${formatExamDate(startsAtMillis)}"
            } else {
                "Start: ${formatExamDate(startsAtMillis)} · $location"
            }
            val contentText = if (reminderLabel.isNullOrBlank()) {
                baseText
            } else {
                "$baseText · $reminderLabel"
            }

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                examId.hashCode(),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, ExamReminderActionReceiver::class.java).apply {
                action = ExamReminderActionReceiver.ACTION_SNOOZE_10_MIN
                putExtra(KEY_EXAM_ID, examId)
                putExtra(KEY_TITLE, title)
                putExtra(KEY_LOCATION, location)
                putExtra(KEY_STARTS_AT, startsAtMillis)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                "${examId}-snooze10".hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, ExamNotificationManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_exam)
                .setContentTitle("Erinnerung: $title")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_notification_exam, "In 10 Min", snoozePendingIntent)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(examId.hashCode(), notification)
        }
    }
}
