package com.andrin.examcountdown.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ExamNotificationManager {
    const val CHANNEL_ID = "exam_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Prüfungserinnerungen",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Benachrichtigungen vor deinen Prüfungen"
        }
        manager.createNotificationChannel(channel)
    }
}
