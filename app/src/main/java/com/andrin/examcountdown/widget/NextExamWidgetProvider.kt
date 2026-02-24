package com.andrin.examcountdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.andrin.examcountdown.MainActivity
import com.andrin.examcountdown.R
import com.andrin.examcountdown.ui.HomeTab
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatExamDateShort
import com.andrin.examcountdown.worker.IcalSyncScheduler

class NextExamWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                IcalSyncScheduler.syncNow(context.applicationContext)
                WidgetUpdater.updateAll(context.applicationContext)
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val nextExam = WidgetContentLoader.loadUpcoming(context, limit = 1).firstOrNull()

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_next_exam)

                if (nextExam == null) {
                    views.setTextViewText(R.id.nextExamTitle, "Keine kommenden Prüfungen")
                    views.setTextViewText(R.id.nextExamTime, "Füge eine Prüfung in der App hinzu")
                    views.setTextViewText(R.id.nextExamCountdown, "")
                } else {
                    val title = nextExam.subject
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "$it · ${nextExam.title}" }
                        ?: nextExam.title
                    views.setTextViewText(R.id.nextExamTitle, title)
                    views.setTextViewText(R.id.nextExamTime, formatExamDateShort(nextExam.startsAtEpochMillis))
                    views.setTextViewText(R.id.nextExamCountdown, formatCountdown(nextExam.startsAtEpochMillis))
                }

                views.setOnClickPendingIntent(
                    R.id.widgetRoot,
                    createOpenAppIntent(context, widgetId, HomeTab.EXAMS)
                )
                views.setOnClickPendingIntent(
                    R.id.nextWidgetOpenTimetable,
                    createOpenAppIntent(context, widgetId + 10_000, HomeTab.TIMETABLE)
                )
                views.setOnClickPendingIntent(
                    R.id.nextWidgetRefresh,
                    createRefreshIntent(context, widgetId)
                )

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun createOpenAppIntent(context: Context, requestCode: Int, tab: HomeTab): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_TAB, tab.route)
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createRefreshIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, NextExamWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                70_000 + widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private const val ACTION_WIDGET_REFRESH = "com.andrin.examcountdown.widget.NEXT_REFRESH"
    }
}
