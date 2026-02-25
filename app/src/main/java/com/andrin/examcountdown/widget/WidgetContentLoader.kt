package com.andrin.examcountdown.widget

import android.content.Context
import com.andrin.examcountdown.data.ExamRepository
import kotlinx.coroutines.runBlocking

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
    val kind: WidgetItemKind
)

object WidgetContentLoader {
    fun loadUpcomingItems(context: Context, appWidgetId: Int, limit: Int): List<WidgetTimelineItem> {
        val config = WidgetPreferences.readConfig(context, appWidgetId)
        val now = System.currentTimeMillis()
        val windowEnd = now + config.windowDays.coerceIn(1, 180) * 24L * 60L * 60L * 1000L

        val items = runBlocking {
            val repository = ExamRepository(context.applicationContext)
            val exams = repository.readSnapshot()
                .map { exam ->
                    WidgetTimelineItem(
                        id = "exam:${exam.id}",
                        title = exam.subject
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "$it · ${exam.title}" }
                            ?: exam.title,
                        startsAtEpochMillis = exam.startsAtEpochMillis,
                        endsAtEpochMillis = exam.startsAtEpochMillis,
                        kind = WidgetItemKind.EXAM
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
                            kind = WidgetItemKind.LESSON
                        )
                    }
                val events = repository.readEventsSnapshot()
                    .map { event ->
                        WidgetTimelineItem(
                            id = "event:${event.id}",
                            title = event.title,
                            startsAtEpochMillis = event.startsAtEpochMillis,
                            endsAtEpochMillis = event.endsAtEpochMillis,
                            kind = WidgetItemKind.EVENT
                        )
                    }
                exams + lessons + events
            }
        }

        val filtered = items.filter { item ->
            item.endsAtEpochMillis >= now && item.startsAtEpochMillis <= windowEnd
        }

        val sorted = when (config.sortMode) {
            WidgetSortMode.TIME_ASC -> filtered.sortedBy { it.startsAtEpochMillis }
            WidgetSortMode.TYPE_THEN_TIME -> filtered.sortedWith(
                compareBy<WidgetTimelineItem>(
                    { kindSortWeight(it.kind) },
                    { it.startsAtEpochMillis },
                    { it.title.lowercase() }
                )
            )
        }

        return sorted.take(limit.coerceAtLeast(1))
    }

    fun headerLabel(context: Context, appWidgetId: Int): String {
        val config = WidgetPreferences.readConfig(context, appWidgetId)
        return if (config.mode == WidgetMode.EXAMS) {
            "Prüfungen (${config.windowDays}T)"
        } else {
            "Agenda (${config.windowDays}T)"
        }
    }

    fun openTabForConfig(context: Context, appWidgetId: Int): String {
        return if (WidgetPreferences.readConfig(context, appWidgetId).mode == WidgetMode.EXAMS) {
            "exams"
        } else {
            "events"
        }
    }

    private fun kindSortWeight(kind: WidgetItemKind): Int {
        return when (kind) {
            WidgetItemKind.EXAM -> 0
            WidgetItemKind.LESSON -> 1
            WidgetItemKind.EVENT -> 2
        }
    }
}
