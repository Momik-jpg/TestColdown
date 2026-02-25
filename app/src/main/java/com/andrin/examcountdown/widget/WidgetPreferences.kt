package com.andrin.examcountdown.widget

import android.content.Context

enum class WidgetMode {
    EXAMS,
    AGENDA
}

enum class WidgetSortMode {
    TIME_ASC,
    TYPE_THEN_TIME
}

data class WidgetConfig(
    val mode: WidgetMode = WidgetMode.EXAMS,
    val windowDays: Int = 30,
    val sortMode: WidgetSortMode = WidgetSortMode.TIME_ASC
)

object WidgetPreferences {
    private const val PREFS_NAME = "widget_config_store"
    private const val KEY_MODE_PREFIX = "mode_"
    private const val KEY_WINDOW_DAYS_PREFIX = "window_days_"
    private const val KEY_SORT_PREFIX = "sort_"

    fun readConfig(context: Context, appWidgetId: Int): WidgetConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString("$KEY_MODE_PREFIX$appWidgetId", WidgetMode.EXAMS.name)
            ?.let { raw -> WidgetMode.entries.firstOrNull { it.name == raw } }
            ?: WidgetMode.EXAMS
        val windowDays = prefs.getInt("$KEY_WINDOW_DAYS_PREFIX$appWidgetId", 30)
            .coerceIn(1, 180)
        val sortMode = prefs.getString("$KEY_SORT_PREFIX$appWidgetId", WidgetSortMode.TIME_ASC.name)
            ?.let { raw -> WidgetSortMode.entries.firstOrNull { it.name == raw } }
            ?: WidgetSortMode.TIME_ASC
        return WidgetConfig(
            mode = mode,
            windowDays = windowDays,
            sortMode = sortMode
        )
    }

    fun saveConfig(context: Context, appWidgetId: Int, config: WidgetConfig) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("$KEY_MODE_PREFIX$appWidgetId", config.mode.name)
            .putInt("$KEY_WINDOW_DAYS_PREFIX$appWidgetId", config.windowDays.coerceIn(1, 180))
            .putString("$KEY_SORT_PREFIX$appWidgetId", config.sortMode.name)
            .apply()
    }

    fun clearConfig(context: Context, appWidgetId: Int) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("$KEY_MODE_PREFIX$appWidgetId")
            .remove("$KEY_WINDOW_DAYS_PREFIX$appWidgetId")
            .remove("$KEY_SORT_PREFIX$appWidgetId")
            .apply()
    }
}
