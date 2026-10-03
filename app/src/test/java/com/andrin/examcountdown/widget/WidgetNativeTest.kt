package com.andrin.examcountdown.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.andrin.examcountdown.MainActivity
import com.andrin.examcountdown.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetNativeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val now = Instant.parse("2026-10-05T06:00:00Z").toEpochMilli()
    private val items get() = listOf(
        WidgetTimelineItem("math", "Mathematik · Funktionen", now + 86_400_000, now + 86_400_000, WidgetItemKind.EXAM, "Raum 204"),
        WidgetTimelineItem("english", "Englisch", now + 90_000_000, now + 93_600_000, WidgetItemKind.LESSON, "Raum 102"),
        WidgetTimelineItem("project", "Projektabgabe", now + 172_800_000, now + 172_800_000, WidgetItemKind.EVENT, "Online"),
        WidgetTimelineItem("history", "Geschichte · Europa", now + 259_200_000, now + 259_200_000, WidgetItemKind.EXAM, "Aula")
    )

    private fun options(height: Int, width: Int = 320) = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height + 300)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
    }

    private fun render(list: Boolean, height: Int, width: Int = 320, config: WidgetConfig = WidgetConfig(WidgetMode.AGENDA), empty: Boolean = false): View {
        val views = if (list) WidgetPresentation.list(context, 42, config, if (empty) emptyList() else items, options(height, width), now)
        else WidgetPresentation.next(context, 42, config, if (empty) null else items.first(), options(height, width), now)
        val view = views.apply(context, FrameLayout(context))
        val density = context.resources.displayMetrics.density
        view.measure(View.MeasureSpec.makeMeasureSpec((width * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((height * density).toInt(), View.MeasureSpec.EXACTLY))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        return view
    }

    private fun screenshot(view: View, name: String) {
        val dir = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui").apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun nativeWidgetsRenderLightAndDarkAndGearOpensTheirOwnConfiguration() {
        for (dark in listOf(false, true)) {
            RuntimeEnvironment.setQualifiers("w411dp-h891dp" + (if (dark) "-night" else "-notnight") + "-mdpi")
            for (list in listOf(false, true)) {
                val view = render(list, if (list) 440 else 240)
                screenshot(view, "widget-${if (list) "list" else "next"}-${if (dark) "dark" else "light"}")
                view.findViewById<View>(if (list) R.id.listWidgetConfigure else R.id.nextWidgetConfigure).performClick()
                val intent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
                assertEquals(WidgetConfigActivity::class.java.name, intent.component?.className)
                assertEquals(42, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
            }
        }
    }

    @Test fun resizeUsesSmallestOrientationAndRowsFitActualNativeContainer() {
        val small = render(true, 220, 220)
        val tall = render(true, 440)
        val smallRows = small.findViewById<ViewGroup>(R.id.listRows)
        val tallRows = tall.findViewById<ViewGroup>(R.id.listRows)
        assertEquals(1, smallRows.childCount)
        assertTrue(tallRows.childCount > smallRows.childCount)
        for (rows in listOf(smallRows, tallRows)) {
            assertTrue("Last row clipped", rows.getChildAt(rows.childCount - 1).bottom <= rows.height)
        }
        screenshot(small, "widget-list-small")
        screenshot(render(true, 320, 260, WidgetConfig(WidgetMode.AGENDA, compact = true)), "widget-list-compact")
    }

    @Test fun largeTextRowsFitAndEmptyStateRemainsActionable() {
        val resources = context.resources
        val config = android.content.res.Configuration(resources.configuration).apply { fontScale = 1.6f }
        @Suppress("DEPRECATION") resources.updateConfiguration(config, resources.displayMetrics)
        val list = render(true, 320)
        val rows = list.findViewById<ViewGroup>(R.id.listRows)
        assertTrue(rows.childCount > 0)
        assertTrue(rows.getChildAt(rows.childCount - 1).bottom <= rows.height)
        screenshot(list, "widget-list-large-text")
        val empty = render(true, 220, empty = true)
        assertEquals(0, empty.findViewById<ViewGroup>(R.id.listRows).childCount)
        assertEquals(View.VISIBLE, empty.findViewById<View>(R.id.listEmptyState).visibility)
        empty.performClick()
        assertEquals("events", shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity.getStringExtra(MainActivity.EXTRA_OPEN_TAB))
        screenshot(empty, "widget-list-empty")
    }

    @Test fun rowClickOpensMatchingTabAndLocationCanBeHidden() {
        val view = render(true, 440, config = WidgetConfig(WidgetMode.AGENDA, showLocation = false))
        val rows = view.findViewById<ViewGroup>(R.id.listRows)
        rows.getChildAt(1).performClick()
        assertEquals("timetable", shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity.getStringExtra(MainActivity.EXTRA_OPEN_TAB))
        assertFalse(rows.getChildAt(0).findViewById<TextView>(R.id.widgetRowDetails).text.contains("204"))
        val next = render(false, 180, 220, WidgetConfig(showCountdown = false, compact = true))
        assertEquals(View.GONE, next.findViewById<View>(R.id.nextExamCountdown).visibility)
        screenshot(next, "widget-next-compact")
    }

    @Test fun legacyPreferencesGainDefaultsAndEditingOneWidgetPreservesOthers() {
        val prefs = context.getSharedPreferences("widget_config_store", Context.MODE_PRIVATE)
        prefs.edit().putString("mode_42", "AGENDA").putInt("window_days_42", 7).putString("sort_42", "TYPE_THEN_TIME").commit()
        val legacy = WidgetPreferences.readConfig(context, 42)
        assertEquals(WidgetConfig(WidgetMode.AGENDA, 7, WidgetSortMode.TYPE_THEN_TIME), legacy)
        val changed = legacy.copy(compact = true, showLocation = false, showCountdown = false)
        WidgetPreferences.saveConfig(context, 43, changed)
        assertEquals(changed, WidgetPreferences.readConfig(context, 43))
        WidgetPreferences.clearConfig(context, 42)
        assertEquals(WidgetConfig(), WidgetPreferences.readConfig(context, 42))
        assertEquals(changed, WidgetPreferences.readConfig(context, 43))
    }

    @Test fun corruptEnumsAndWindowBoundsRecoverSafely() {
        context.getSharedPreferences("widget_config_store", Context.MODE_PRIVATE).edit()
            .putString("mode_1", "OLD").putString("sort_1", "OLD").putInt("window_days_1", -10).commit()
        assertEquals(WidgetConfig(windowDays = 1), WidgetPreferences.readConfig(context, 1))
        WidgetPreferences.saveConfig(context, 2, WidgetConfig(windowDays = Int.MAX_VALUE))
        assertEquals(WIDGET_WINDOW_DAYS_ALL, WidgetPreferences.readConfig(context, 2).windowDays)
    }

    @Test fun pendingIntentsCannotOverwriteOtherWidgetsOrActions() {
        val main = WidgetIntents.open(context, 10_001, "open", "exams")
        val other = WidgetIntents.open(context, 1, "secondary", "timetable")
        assertNotEquals(main, other)
        assertEquals("exams", shadowOf(main).savedIntent.getStringExtra(MainActivity.EXTRA_OPEN_TAB))
        assertEquals("timetable", shadowOf(other).savedIntent.getStringExtra(MainActivity.EXTRA_OPEN_TAB))
        assertTrue(shadowOf(main).isImmutable)
        assertNotEquals(WidgetIntents.configure(context, 1), WidgetIntents.configure(context, 2))
        assertTrue(shadowOf(WidgetIntents.refresh(context, 1, NextExamWidgetProvider::class.java)).isBroadcast)
    }

    @Test fun exportedConfigurationAcceptsOnlyThisAppsInstalledWidgets() {
        val manager = shadowOf(AppWidgetManager.getInstance(context))
        manager.putWidgetInfo(42, AppWidgetProviderInfo().apply { provider = ComponentName(context, NextExamWidgetProvider::class.java) })
        manager.putWidgetInfo(43, AppWidgetProviderInfo().apply { provider = ComponentName("other.app", "other.Widget") })
        assertTrue(isOwnWidget(context, 42))
        assertFalse(isOwnWidget(context, 43))
        assertFalse(isOwnWidget(context, 999))
        val component = ComponentName(context, WidgetConfigActivity::class.java)
        assertTrue(context.packageManager.getActivityInfo(component, 0).exported)
    }

    @Test fun dateColumnAndProminentCountdownKeepRealContentAndAdaptToNarrowSpace() {
        val list = render(true, 440)
        val row = list.findViewById<ViewGroup>(R.id.listRows).getChildAt(0)
        assertEquals("06", row.findViewById<TextView>(R.id.widgetRowDay).text.toString())
        assertEquals("OKT", row.findViewById<TextView>(R.id.widgetRowMonth).text.toString())
        assertTrue(row.findViewById<TextView>(R.id.widgetRowDetails).text.startsWith("08:00"))
        val next = render(false, 240)
        assertEquals("1", next.findViewById<TextView>(R.id.nextExamCountdown).text.toString())
        assertEquals("TAG", next.findViewById<TextView>(R.id.nextCountdownUnit).text.toString())
        val narrow = render(false, 240, 220)
        assertNull(narrow.findViewById<View>(R.id.nextCountdownBox))
        assertEquals("in 1 Tag", narrow.findViewById<TextView>(R.id.nextExamCountdown).text.toString())
        screenshot(narrow, "widget-next-narrow")
        val compact = render(true, 320, config = WidgetConfig(WidgetMode.AGENDA, compact = true))
        val rows = compact.findViewById<ViewGroup>(R.id.listRows)
        assertTrue(rows.getChildAt(rows.childCount - 1).bottom <= rows.height)
    }
}
