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
            val now = System.currentTimeMillis()
            val remainingMillis = startsAtMillis - now

            val notification = NotificationCompat.Builder(context, ExamNotificationManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_exam)
                .setContentTitle("Erinnerung: $title")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)

            if (remainingMillis > 10L * 60L * 1000L) {
                notification.addAction(
                    R.drawable.ic_notification_exam,
                    "In 10 Min",
                    createSnoozePendingIntent(
                        context = context,
                        examId = examId,
                        title = title,
                        location = location,
                        startsAtMillis = startsAtMillis,
                        action = ExamReminderActionReceiver.ACTION_SNOOZE_10_MIN,
                        requestCodeSeed = "${examId}-snooze10".hashCode()
                    )
                )
            }
            if (remainingMillis > 30L * 60L * 1000L) {
                notification.addAction(
                    R.drawable.ic_notification_exam,
                    "In 30 Min",
                    createSnoozePendingIntent(
                        context = context,
                        examId = examId,
                        title = title,
                        location = location,
                        startsAtMillis = startsAtMillis,
                        action = ExamReminderActionReceiver.ACTION_SNOOZE_30_MIN,
                        requestCodeSeed = "${examId}-snooze30".hashCode()
                    )
                )
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(examId.hashCode(), notification.build())
        }

        private fun createSnoozePendingIntent(
            context: Context,
            examId: String,
            title: String,
            location: String?,
            startsAtMillis: Long,
            action: String,
            requestCodeSeed: Int
        ): PendingIntent {
            val snoozeIntent = Intent(context, ExamReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(KEY_EXAM_ID, examId)
                putExtra(KEY_TITLE, title)
                putExtra(KEY_LOCATION, location)
                putExtra(KEY_STARTS_AT, startsAtMillis)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCodeSeed,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
