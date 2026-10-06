package com.andrin.examcountdown.widget

import android.content.Context
import com.andrin.examcountdown.data.ExamRepository
import kotlinx.coroutines.runBlocking
import java.util.Locale

enum class WidgetItemKind {
    EXAM,
    LESSON,
    EVENT
}

data class WidgetTimelineItem(
    val id: String,
    val title: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val kind: WidgetItemKind,
    val location: String? = null,
    val isAllDay: Boolean = false,
    val isCancelled: Boolean = false
)

object WidgetContentLoader {
    fun loadUpcomingItems(context: Context, appWidgetId: Int, limit: Int, nextOnly: Boolean = false): List<WidgetTimelineItem> {
        val config = WidgetPreferences.readConfig(context, appWidgetId)
        val now = System.currentTimeMillis()
        val items = runBlocking {
            val repository = ExamRepository(context.applicationContext)
            val exams = repository.readSnapshot()
                .map { exam ->
                    WidgetTimelineItem(
                        id = "exam:${exam.id}",
                        title = exam.subject
                            ?.takeIf { it.isNotBlank() && !it.equals(exam.title, ignoreCase = true) }
                            ?.let { "$it · ${exam.title}" }
                            ?: exam.title,
                        startsAtEpochMillis = exam.startsAtEpochMillis,
                        endsAtEpochMillis = exam.startsAtEpochMillis,
                        kind = WidgetItemKind.EXAM,
                        location = exam.location
                    )
                }

            if (config.mode == WidgetMode.EXAMS) {
                exams
            } else {
                val lessons = repository.readLessonsSnapshot()
                    .map { lesson ->
                        WidgetTimelineItem(
                            id = "lesson:${lesson.id}",
                            title = lesson.title,
                            startsAtEpochMillis = lesson.startsAtEpochMillis,
                            endsAtEpochMillis = lesson.endsAtEpochMillis,
                            kind = WidgetItemKind.LESSON,
                            location = lesson.location,
                            isCancelled = lesson.isCancelledSlot
                        )
                    }
                val events = repository.readEventsSnapshot()
                    .map { event ->
                        WidgetTimelineItem(
                            id = "event:${event.id}",
                            title = event.title,
                            startsAtEpochMillis = event.startsAtEpochMillis,
                            endsAtEpochMillis = event.endsAtEpochMillis,
                            kind = WidgetItemKind.EVENT,
                            location = event.location,
                            isAllDay = event.isAllDay
                        )
                    }
                exams + lessons + events
            }
        }

        return selectWidgetItems(items, config, now, limit, nextOnly)
    }

    fun headerLabel(context: Context, appWidgetId: Int): String = widgetHeaderLabel(WidgetPreferences.readConfig(context, appWidgetId))

    fun openTabForConfig(context: Context, appWidgetId: Int): String = widgetRoute(WidgetPreferences.readConfig(context, appWidgetId))
}

internal fun selectWidgetItems(
items: List<WidgetTimelineItem>, config: WidgetConfig, now: Long, limit: Int, nextOnly: Boolean = false
): List<WidgetTimelineItem> {
    if (limit <= 0) return emptyList()
    val windowEnd = if (config.windowDays >= WIDGET_WINDOW_DAYS_ALL) Long.MAX_VALUE
    else now + config.windowDays.coerceAtLeast(1) * 86_400_000L
    val filtered = items.filter { item ->
        (config.mode == WidgetMode.AGENDA || item.kind == WidgetItemKind.EXAM) &&
            item.endsAtEpochMillis >= now && item.startsAtEpochMillis <= windowEnd &&
            (!nextOnly || !item.isCancelled)
    }

    val sorted = when (if (nextOnly) WidgetSortMode.TIME_ASC else config.sortMode) {
        WidgetSortMode.TIME_ASC -> filtered.sortedBy { it.startsAtEpochMillis }
        WidgetSortMode.TYPE_THEN_TIME -> filtered.sortedWith(
            compareBy<WidgetTimelineItem>(
                { it.kind.ordinal },
                { it.startsAtEpochMillis },
                { it.title.lowercase(Locale.ROOT) }
            )
        )
    }

    return sorted.take(limit)
}

internal fun widgetHeaderLabel(config: WidgetConfig): String {
    val windowLabel = if (config.windowDays >= WIDGET_WINDOW_DAYS_ALL) {
        "Alle"
    } else {
        "${config.windowDays} Tage"
    }
    return if (config.mode == WidgetMode.EXAMS) {
        "Prüfungen · $windowLabel"
    } else {
        "Agenda · $windowLabel"
    }
}

internal fun widgetRoute(config: WidgetConfig): String {
    return if (config.mode == WidgetMode.EXAMS) {
        "exams"
    } else {
        "events"
    }
}
