package com.andrin.examcountdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.andrin.examcountdown.MainActivity
import com.andrin.examcountdown.R
import com.andrin.examcountdown.ui.HomeTab
import com.andrin.examcountdown.util.formatExamDateShort
import com.andrin.examcountdown.worker.IcalSyncScheduler

class ExamListWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                IcalSyncScheduler.syncNow(context.applicationContext)
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
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
                val views = RemoteViews(context.packageName, R.layout.widget_exam_list)
                val config = WidgetPreferences.readConfig(context, widgetId)
                val rowLimit = resolveRowLimit(appWidgetManager, widgetId)
                val upcoming = WidgetContentLoader.loadUpcomingItems(
                    context = context,
                    appWidgetId = widgetId,
                    limit = rowLimit
                )

                views.setTextViewText(R.id.listWidgetHeader, WidgetContentLoader.headerLabel(context, widgetId))
                views.setTextViewText(
                    R.id.listWidgetOpenTimetable,
                    if (config.mode == WidgetMode.EXAMS) "Zum Plan" else "Zu Prüfungen"
                )

                if (upcoming.isEmpty()) {
                    views.setViewVisibility(R.id.listEmptyState, View.VISIBLE)
                    clearRows(views)
                } else {
                    views.setViewVisibility(R.id.listEmptyState, View.GONE)
                    bindRows(views, upcoming, rowLimit)
                }

                val mainRoute = WidgetContentLoader.openTabForConfig(context, widgetId)
                val secondaryRoute = if (config.mode == WidgetMode.EXAMS) {
                    HomeTab.TIMETABLE.route
                } else {
                    HomeTab.EXAMS.route
                }

                views.setOnClickPendingIntent(
                    R.id.listWidgetRoot,
                    createOpenAppIntent(context, widgetId, mainRoute)
                )
                views.setOnClickPendingIntent(
                    R.id.listWidgetOpenTimetable,
                    createOpenAppIntent(context, widgetId + 10_000, secondaryRoute)
                )
                views.setOnClickPendingIntent(
                    R.id.listWidgetRefresh,
                    createRefreshIntent(context, widgetId)
                )

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun bindRows(views: RemoteViews, items: List<WidgetTimelineItem>, rowLimit: Int) {
            val rowIds = allRowIds()
            rowIds.forEachIndexed { index, rowId ->
                if (index < rowLimit && index < items.size) {
                    val item = items[index]
                    val typePrefix = kindLabel(item.kind)
                    views.setViewVisibility(rowId, View.VISIBLE)
                    views.setTextViewText(
                        rowId,
                        "${formatExamDateShort(item.startsAtEpochMillis)}  •  $typePrefix${item.title}"
                    )
                } else {
                    views.setViewVisibility(rowId, View.GONE)
                }
            }
        }

        private fun clearRows(views: RemoteViews) {
            val rowIds = allRowIds()
            rowIds.forEach { rowId ->
                views.setTextViewText(rowId, "")
                views.setViewVisibility(rowId, View.GONE)
            }
        }

        private fun allRowIds(): IntArray = intArrayOf(
            R.id.row1,
            R.id.row2,
            R.id.row3,
            R.id.row4,
            R.id.row5,
            R.id.row6,
            R.id.row7,
            R.id.row8,
            R.id.row9,
            R.id.row10
        )

        private fun resolveRowLimit(appWidgetManager: AppWidgetManager, widgetId: Int): Int {
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            if (maxHeightDp <= 0) return 5
            return ((maxHeightDp - 46) / 26).coerceIn(5, 10)
        }

        private fun kindLabel(kind: WidgetItemKind): String {
            return when (kind) {
                WidgetItemKind.EXAM -> ""
                WidgetItemKind.LESSON -> "Lektion · "
                WidgetItemKind.EVENT -> "Event · "
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
            val intent = Intent(context, ExamListWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                90_000 + widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private const val ACTION_WIDGET_REFRESH = "com.andrin.examcountdown.widget.LIST_REFRESH"
    }
}
