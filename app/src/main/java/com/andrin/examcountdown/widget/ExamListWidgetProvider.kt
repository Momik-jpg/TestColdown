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
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.ui.HomeTab
import com.andrin.examcountdown.util.formatExamDateShort
import com.andrin.examcountdown.worker.IcalSyncScheduler

class ExamListWidgetProvider : AppWidgetProvider() {
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
            val upcoming = WidgetContentLoader.loadUpcoming(context, limit = 5)

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_exam_list)

                if (upcoming.isEmpty()) {
                    views.setViewVisibility(R.id.listEmptyState, View.VISIBLE)
                    clearRows(views)
                } else {
                    views.setViewVisibility(R.id.listEmptyState, View.GONE)
                    bindRows(views, upcoming)
                }

                views.setOnClickPendingIntent(
                    R.id.listWidgetRoot,
                    createOpenAppIntent(context, widgetId, HomeTab.EXAMS)
                )
                views.setOnClickPendingIntent(
                    R.id.listWidgetOpenTimetable,
                    createOpenAppIntent(context, widgetId + 10_000, HomeTab.TIMETABLE)
                )
                views.setOnClickPendingIntent(
                    R.id.listWidgetRefresh,
                    createRefreshIntent(context, widgetId)
                )

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun bindRows(views: RemoteViews, exams: List<Exam>) {
            val rowIds = intArrayOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4, R.id.row5)
            rowIds.forEachIndexed { index, rowId ->
                if (index < exams.size) {
                    val exam = exams[index]
                    views.setViewVisibility(rowId, View.VISIBLE)
                    views.setTextViewText(rowId, "${formatExamDateShort(exam.startsAtEpochMillis)}  •  ${exam.title}")
                } else {
                    views.setViewVisibility(rowId, View.GONE)
                }
            }
        }

        private fun clearRows(views: RemoteViews) {
            val rowIds = intArrayOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4, R.id.row5)
            rowIds.forEach { rowId ->
                views.setTextViewText(rowId, "")
                views.setViewVisibility(rowId, View.GONE)
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
