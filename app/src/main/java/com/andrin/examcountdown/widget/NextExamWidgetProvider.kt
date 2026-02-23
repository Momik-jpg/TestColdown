package com.andrin.examcountdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.andrin.examcountdown.MainActivity
import com.andrin.examcountdown.R
import com.andrin.examcountdown.util.formatCountdown
import com.andrin.examcountdown.util.formatExamDateShort

class NextExamWidgetProvider : AppWidgetProvider() {
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
                    views.setTextViewText(R.id.nextExamTitle, nextExam.title)
                    views.setTextViewText(R.id.nextExamTime, formatExamDateShort(nextExam.startsAtEpochMillis))
                    views.setTextViewText(R.id.nextExamCountdown, formatCountdown(nextExam.startsAtEpochMillis))
                }

                views.setOnClickPendingIntent(
                    R.id.widgetRoot,
                    createOpenAppIntent(context, widgetId)
                )

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun createOpenAppIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
            return PendingIntent.getActivity(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
