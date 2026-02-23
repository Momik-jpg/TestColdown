package com.andrin.examcountdown.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val widgetManager = AppWidgetManager.getInstance(appContext)

        val nextWidgetIds = widgetManager.getAppWidgetIds(
            ComponentName(appContext, NextExamWidgetProvider::class.java)
        )
        if (nextWidgetIds.isNotEmpty()) {
            NextExamWidgetProvider.updateWidgets(appContext, widgetManager, nextWidgetIds)
        }

        val listWidgetIds = widgetManager.getAppWidgetIds(
            ComponentName(appContext, ExamListWidgetProvider::class.java)
        )
        if (listWidgetIds.isNotEmpty()) {
            ExamListWidgetProvider.updateWidgets(appContext, widgetManager, listWidgetIds)
        }
    }
}