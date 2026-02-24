package com.andrin.examcountdown.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.andrin.examcountdown.MainActivity
import com.andrin.examcountdown.R

object TimetableSyncNotificationManager {
    const val CHANNEL_ID = "timetable_sync_updates"
    private const val NOTIFICATION_ID = 22001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stundenplan-Änderungen",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Benachrichtigungen bei Stundenplan-Änderungen"
        }
        manager.createNotificationChannel(channel)
    }

    fun showChangedLessons(
        context: Context,
        changedCount: Int,
        movedCount: Int,
        roomChangedCount: Int
    ) {
        if (changedCount <= 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)

        val details = buildString {
            append("$changedCount Änderung")
            if (changedCount != 1) append("en")
            if (movedCount > 0 || roomChangedCount > 0) {
                append(" · ")
                append("Verschoben: $movedCount")
                append(" · ")
                append("Raum: $roomChangedCount")
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_exam)
            .setContentTitle("Stundenplan aktualisiert")
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
