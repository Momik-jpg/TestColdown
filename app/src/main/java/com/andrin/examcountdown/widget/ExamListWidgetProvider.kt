package com.andrin.examcountdown.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.andrin.examcountdown.worker.IcalSyncScheduler

class ExamListWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetIntents.ACTION_REFRESH || intent.action == "com.andrin.examcountdown.widget.LIST_REFRESH") {
            IcalSyncScheduler.syncNow(context.applicationContext)
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPreferences.clearConfig(context, it) }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            appWidgetIds.forEach { id ->
                val config = WidgetPreferences.readConfig(context, id)
                val options = appWidgetManager.getAppWidgetOptions(id)
                val items = WidgetContentLoader.loadUpcomingItems(context, id,
                    widgetRowLimit(widgetHeight(options, 320), config.compact, context.resources.configuration.fontScale))
                val views = WidgetPresentation.list(context, id, config, items, options, System.currentTimeMillis())
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}
