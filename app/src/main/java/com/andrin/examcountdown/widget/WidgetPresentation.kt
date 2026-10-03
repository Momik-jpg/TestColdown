package com.andrin.examcountdown.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.andrin.examcountdown.R
import com.andrin.examcountdown.ui.HomeTab
import com.andrin.examcountdown.util.formatCompactDay
import com.andrin.examcountdown.util.formatExamDateShort
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val widgetZone = ZoneId.of("Europe/Zurich")
private val widgetClock = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
private val widgetMonth = DateTimeFormatter.ofPattern("MMM", Locale.GERMANY)

internal data class WidgetCountdown(val value: String, val unit: String)

internal fun widgetCountdown(item: WidgetTimelineItem, now: Long): WidgetCountdown {
    if (item.isCancelled) return WidgetCountdown("–", "ENTFÄLLT")
    if (item.startsAtEpochMillis <= now) return WidgetCountdown(if (item.isAllDay) "Heute" else "Jetzt", "")
    val minutes = ((item.startsAtEpochMillis - now) / 60_000).coerceAtLeast(1)
    return when {
        minutes >= 1440 -> WidgetCountdown((minutes / 1440).toString(), if (minutes / 1440 == 1L) "TAG" else "TAGE")
        minutes >= 60 -> WidgetCountdown((minutes / 60).toString(), "STD")
        else -> WidgetCountdown(minutes.toString(), "MIN")
    }
}

internal fun widgetTimeDetails(item: WidgetTimelineItem, config: WidgetConfig): String {
    val start = Instant.ofEpochMilli(item.startsAtEpochMillis).atZone(widgetZone)
    val end = Instant.ofEpochMilli(item.endsAtEpochMillis).atZone(widgetZone)
    val time = when {
        item.isAllDay -> "Ganztägig"
        start.toLocalDate() != end.toLocalDate() -> "${widgetClock.format(start)} – ${formatCompactDay(end.toLocalDate())} ${widgetClock.format(end)}"
        item.endsAtEpochMillis > item.startsAtEpochMillis -> "${widgetClock.format(start)}–${widgetClock.format(end)}"
        else -> widgetClock.format(start)
    }
    return listOfNotNull(time, item.location?.trim()?.takeIf { config.showLocation && it.isNotEmpty() }).joinToString(" · ")
}

internal fun widgetStatus(item: WidgetTimelineItem, now: Long): String = when {
    item.isCancelled -> "Entfällt"
    item.startsAtEpochMillis <= now && item.isAllDay -> "Ganztägig"
    item.startsAtEpochMillis <= now -> "Jetzt"
    else -> {
        val minutes = ((item.startsAtEpochMillis - now) / 60_000L).coerceAtLeast(1)
        when {
            minutes >= 1440 -> "in ${minutes / 1440} ${if (minutes / 1440 == 1L) "Tag" else "Tagen"}"
            minutes >= 60 -> "in ${minutes / 60} Std ${minutes % 60} Min"
            else -> "in $minutes Min"
        }
    }
}

internal fun widgetDetails(item: WidgetTimelineItem, config: WidgetConfig): String {
    val date = if (item.isAllDay) {
        formatCompactDay(Instant.ofEpochMilli(item.startsAtEpochMillis).atZone(ZoneId.of("Europe/Zurich")).toLocalDate()) + " · Ganztägig"
    } else formatExamDateShort(item.startsAtEpochMillis)
    val location = item.location?.trim()?.takeIf { it.isNotEmpty() && config.showLocation }
    return listOfNotNull(date, location).joinToString(" · ")
}

internal fun widgetHeight(options: Bundle, fallback: Int): Int =
    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: fallback

/** Reserve header/footer and scale text space for the launcher's smallest orientation. */
internal fun widgetRowLimit(height: Int, compact: Boolean, fontScale: Float = 1f): Int {
    val footer = if (height >= 320) 48 else 0
    val textReserve = (20 * fontScale.coerceAtLeast(1f)).toInt()
    val rowHeight = widgetRowHeight(compact, fontScale)
    return ((height - 84 - footer - textReserve) / rowHeight).coerceIn(0, 10)
}

internal fun widgetRowHeight(compact: Boolean, fontScale: Float): Int =
    (if (compact) 64 else 80) + (36 * (fontScale.coerceAtLeast(1f) - 1f)).toInt()

internal fun useTallNextWidget(options: Bundle, config: WidgetConfig, fontScale: Float): Boolean =
    !config.compact && options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320) >= 260 &&
        widgetHeight(options, 240) >= 360 * fontScale.coerceAtLeast(1f)

/** Leave room for two title lines, date/time/room, countdown, controls and footer. */
internal fun nextWidgetRowLimit(options: Bundle, config: WidgetConfig, fontScale: Float): Int =
    if (!useTallNextWidget(options, config, fontScale)) 0 else
        ((widgetHeight(options, 240) - (220 * fontScale.coerceAtLeast(1f)).toInt() - 110) /
            widgetRowHeight(false, fontScale)).coerceIn(0, 5)

internal object WidgetPresentation {
    fun next(context: Context, id: Int, config: WidgetConfig, item: WidgetTimelineItem?, options: Bundle, now: Long,
             following: List<WidgetTimelineItem> = emptyList()): RemoteViews {
        val height = widgetHeight(options, 240)
        val fontScale = context.resources.configuration.fontScale.coerceAtLeast(1f)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
        val tall = useTallNextWidget(options, config, fontScale)
        val compact = !tall && (config.compact || height < 220 * fontScale || width < 260 || fontScale > 1.2f)
        val views = RemoteViews(context.packageName, when {
            tall -> R.layout.widget_next_exam_tall
            compact -> R.layout.widget_next_exam_compact
            else -> R.layout.widget_next_exam
        })
        views.setTextViewText(R.id.nextWidgetHeader, if (config.mode == WidgetMode.EXAMS) "Nächste Prüfung" else "Nächster Termin")
        views.setTextViewText(R.id.nextExamTitle, item?.title ?: "Keine Einträge")
        views.setInt(R.id.nextExamTitle, "setMaxLines", if (compact && height < 220 * fontScale) 1 else 2)
        views.setTextViewText(R.id.nextExamTime, item?.let {
            if (tall) {
                val date = DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMANY)
                    .format(Instant.ofEpochMilli(it.startsAtEpochMillis).atZone(widgetZone))
                listOfNotNull(date, widgetTimeDetails(it, config.copy(showLocation = false)),
                    it.location?.trim()?.takeIf { location -> config.showLocation && location.isNotEmpty() }).joinToString("\n")
            } else widgetDetails(it, config)
        } ?: "App öffnen · Zeitraum oder Kalender prüfen")
        views.setInt(R.id.nextExamTime, "setMaxLines", if (tall) { if (fontScale > 1.2f) 4 else 3 } else if (compact) 1 else 2)
        val showCountdown = item != null && config.showCountdown && height >= 180 * fontScale
        views.setTextViewText(R.id.nextExamCountdown, item?.let { if (compact) widgetStatus(it, now) else widgetCountdown(it, now).value } ?: "")
        views.setViewVisibility(R.id.nextExamCountdown, if (showCountdown) View.VISIBLE else View.GONE)
        if (!compact) {
            views.setTextViewText(R.id.nextCountdownUnit, item?.let { widgetCountdown(it, now).unit } ?: "")
            views.setViewVisibility(R.id.nextCountdownBox, if (showCountdown) View.VISIBLE else View.GONE)
            views.setContentDescription(R.id.nextCountdownBox, item?.let { widgetStatus(it, now) })
            views.setTextViewTextSize(R.id.nextExamCountdown, android.util.TypedValue.COMPLEX_UNIT_SP,
                if (item != null && widgetCountdown(item, now).value.length > 2) 25f else if (tall && fontScale <= 1.2f) 48f else 40f)
        }
        if (tall) {
            val limit = nextWidgetRowLimit(options, config, fontScale)
            views.removeAllViews(R.id.nextUpcomingRows)
            following.filter { it.id != item?.id }.take(limit).forEach {
                views.addView(R.id.nextUpcomingRows, timelineRow(context, id, config, it, now))
            }
            views.setTextViewText(R.id.nextUpcomingHeader, "Danach · " + widgetHeaderLabel(config).substringAfter(" · "))
            views.setViewVisibility(R.id.nextUpcomingHeader, if (limit > 0) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.nextUpcomingEmpty,
                if (limit > 0 && following.none { it.id != item?.id }) View.VISIBLE else View.GONE)
        }
        views.setViewVisibility(R.id.nextWidgetOpenTimetable, if (height >= 240 * fontScale && !config.compact) View.VISIBLE else View.GONE)
        bindActions(context, views, id, config, false)
        return views
    }

    fun list(context: Context, id: Int, config: WidgetConfig, items: List<WidgetTimelineItem>, options: Bundle, now: Long): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_exam_list)
        val height = widgetHeight(options, 320)
        val scale = context.resources.configuration.fontScale
        val limit = widgetRowLimit(height, config.compact, scale)
        views.setTextViewText(R.id.listWidgetHeader, if (config.mode == WidgetMode.EXAMS) "Prüfungen" else "Agenda")
        views.setTextViewText(R.id.listWidgetSubtitle, widgetHeaderLabel(config).substringAfter(" · ") +
            if (config.sortMode == WidgetSortMode.TYPE_THEN_TIME) " · Nach Typ" else " · Nach Zeit")
        views.removeAllViews(R.id.listRows)
        items.take(limit).forEach { item ->
            views.addView(R.id.listRows, timelineRow(context, id, config, item, now))
        }
        views.setViewVisibility(R.id.listEmptyState, if (items.isEmpty() || limit == 0) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.listEmptyState, if (limit == 0) "Widget vergrößern" else "Keine Einträge im Zeitraum\nApp öffnen · Kalender prüfen")
        views.setViewVisibility(R.id.listWidgetOpenTimetable, if (height >= 320) View.VISIBLE else View.GONE)
        bindActions(context, views, id, config, true)
        return views
    }

    private fun timelineRow(context: Context, id: Int, config: WidgetConfig, item: WidgetTimelineItem, now: Long): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_exam_list_row)
        val start = Instant.ofEpochMilli(item.startsAtEpochMillis).atZone(widgetZone)
        row.setTextViewText(R.id.widgetRowDay, start.dayOfMonth.toString().padStart(2, '0'))
        row.setTextViewText(R.id.widgetRowMonth, widgetMonth.format(start).replace(".", "").uppercase(Locale.GERMANY))
        row.setTextViewText(R.id.widgetRowTitle, item.title)
        val status = if (item.isCancelled || config.showCountdown) widgetStatus(item, now) else null
        val kind = when (item.kind) { WidgetItemKind.EXAM -> "Prüfung"; WidgetItemKind.LESSON -> "Unterricht"; WidgetItemKind.EVENT -> "Termin" }
        row.setTextViewText(R.id.widgetRowKind, listOfNotNull(kind, status).joinToString(" · "))
        row.setViewVisibility(R.id.widgetRowKind, if (config.compact) View.GONE else View.VISIBLE)
        val details = widgetTimeDetails(item, config)
        row.setTextViewText(R.id.widgetRowDetails, if (config.compact && status != null) "$status · $details" else details)
        row.setContentDescription(R.id.widgetRowRoot, listOfNotNull(item.title, widgetDetails(item, config), status).joinToString(" · "))
        row.setInt(R.id.widgetRowRoot, "setMinimumHeight", ((widgetRowHeight(config.compact, context.resources.configuration.fontScale) - 6) * context.resources.displayMetrics.density).toInt())
        val route = when (item.kind) { WidgetItemKind.EXAM -> "exams"; WidgetItemKind.LESSON -> "timetable"; WidgetItemKind.EVENT -> "events" }
        row.setOnClickPendingIntent(R.id.widgetRowRoot, WidgetIntents.open(context, id, "row-${item.id}", route))
        return row
    }

    private fun bindActions(context: Context, views: RemoteViews, id: Int, config: WidgetConfig, list: Boolean) {
        views.setOnClickPendingIntent(if (list) R.id.listWidgetRoot else R.id.widgetRoot, WidgetIntents.open(context, id, "open", widgetRoute(config)))
        val link = if (list) R.id.listWidgetOpenTimetable else R.id.nextWidgetOpenTimetable
        views.setTextViewText(link, if (config.mode == WidgetMode.EXAMS) "Stundenplan öffnen  ›" else "Prüfungen öffnen  ›")
        views.setOnClickPendingIntent(link, WidgetIntents.open(context, id, "secondary", if (config.mode == WidgetMode.EXAMS) HomeTab.TIMETABLE.route else HomeTab.EXAMS.route))
        views.setOnClickPendingIntent(if (list) R.id.listWidgetConfigure else R.id.nextWidgetConfigure, WidgetIntents.configure(context, id))
        views.setOnClickPendingIntent(if (list) R.id.listWidgetRefresh else R.id.nextWidgetRefresh, WidgetIntents.refresh(context, id, if (list) ExamListWidgetProvider::class.java else NextExamWidgetProvider::class.java))
    }
}
