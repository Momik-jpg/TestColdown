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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            WidgetPreferences.clearConfig(context, widgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_next_exam)
                val config = WidgetPreferences.readConfig(context, widgetId)
                val nextItem = WidgetContentLoader.loadUpcomingItems(
                    context = context,
                    appWidgetId = widgetId,
                    limit = 1
                ).firstOrNull()

                views.setTextViewText(
                    R.id.nextWidgetHeader,
                    if (config.mode == WidgetMode.EXAMS) "Nächste Prüfung" else "Nächster Termin"
                )
                views.setTextViewText(
                    R.id.nextWidgetOpenTimetable,
                    if (config.mode == WidgetMode.EXAMS) "Stundenplan" else "Prüfungen"
                )

                if (nextItem == null) {
                    views.setTextViewText(R.id.nextExamTitle, "Keine kommenden Einträge")
                    views.setTextViewText(R.id.nextExamTime, "Widget konfigurieren oder in der App synchronisieren")
                    views.setTextViewText(R.id.nextExamCountdown, "")
                } else {
                    val kindPrefix = when (nextItem.kind) {
                        WidgetItemKind.EXAM -> ""
                        WidgetItemKind.LESSON -> "Lektion · "
                        WidgetItemKind.EVENT -> "Event · "
                    }
                    views.setTextViewText(R.id.nextExamTitle, "$kindPrefix${nextItem.title}")
                    views.setTextViewText(R.id.nextExamTime, formatExamDateShort(nextItem.startsAtEpochMillis))
                    views.setTextViewText(R.id.nextExamCountdown, formatCountdown(nextItem.startsAtEpochMillis))
                }

                val mainRoute = WidgetContentLoader.openTabForConfig(context, widgetId)
                val secondaryRoute = if (config.mode == WidgetMode.EXAMS) {
                    HomeTab.TIMETABLE.route
                } else {
                    HomeTab.EXAMS.route
                }

                views.setOnClickPendingIntent(
                    R.id.widgetRoot,
                    createOpenAppIntent(context, widgetId, mainRoute)
                )
                views.setOnClickPendingIntent(
                    R.id.nextWidgetOpenTimetable,
                    createOpenAppIntent(context, widgetId + 10_000, secondaryRoute)
                )
                views.setOnClickPendingIntent(
                    R.id.nextWidgetRefresh,
                    createRefreshIntent(context, widgetId)
                )

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun createOpenAppIntent(context: Context, requestCode: Int, route: String): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_TAB, route)
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
