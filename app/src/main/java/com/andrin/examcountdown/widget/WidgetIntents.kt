package com.andrin.examcountdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.andrin.examcountdown.MainActivity

/** Intent data isolates every widget/action; extras alone do not identify a PendingIntent. */
internal object WidgetIntents {
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun open(context: Context, id: Int, action: String, route: String): PendingIntent =
        PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java)
                .setData(key(context, id, action))
                .putExtra(MainActivity.EXTRA_OPEN_TAB, route), FLAGS)

    fun configure(context: Context, id: Int): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, WidgetConfigActivity::class.java)
            .setData(key(context, id, "configure"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id), FLAGS)

    fun refresh(context: Context, id: Int, provider: Class<*>): PendingIntent =
        PendingIntent.getBroadcast(context, 0,
            Intent(context, provider).setAction(ACTION_REFRESH)
                .setData(key(context, id, "refresh"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id), FLAGS)

    private fun key(context: Context, id: Int, action: String): Uri =
        Uri.Builder().scheme("examcountdown").authority(context.packageName)
            .appendPath("widget").appendPath(id.toString()).appendPath(action).build()

    const val ACTION_REFRESH = "com.andrin.examcountdown.widget.REFRESH"
}
