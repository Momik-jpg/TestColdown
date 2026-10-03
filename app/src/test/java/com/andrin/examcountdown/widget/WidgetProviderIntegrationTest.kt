package com.andrin.examcountdown.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.andrin.examcountdown.R
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableLesson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Real local DataStore -> loader -> Android provider -> RemoteViews, with synthetic entries. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class WidgetProviderIntegrationTest {
    @Test fun storedCalendarFlowsIntoBothProvidersAndResizeAndDeletionPreserveOtherWidgets() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ExamRepository(context)
        val now = System.currentTimeMillis()
        repository.addExam(Exam("widget-exam", "Mathematik", "Mathematik", "204", now + 7_200_000))
        repository.replaceSyncedLessons(listOf(TimetableLesson("widget-lesson", "Englisch", "102", now + 60_000, now + 2_760_000)))
        repository.replaceSyncedEvents(listOf(SchoolEvent("widget-event", "Projekttag", location = "Aula", startsAtEpochMillis = now + 86_400_000,
            endsAtEpochMillis = now + 172_800_000, isAllDay = true)))
        try {
            val manager = AppWidgetManager.getInstance(context)
            val shadowManager = shadowOf(manager)
            val nextId = shadowManager.createWidget(NextExamWidgetProvider::class.java, R.layout.widget_next_exam)
            val listId = shadowManager.createWidget(ExamListWidgetProvider::class.java, R.layout.widget_exam_list)
            val config = WidgetConfig(WidgetMode.AGENDA, 7, WidgetSortMode.TYPE_THEN_TIME)
            WidgetPreferences.saveConfig(context, nextId, config)
            WidgetPreferences.saveConfig(context, listId, config)
            NextExamWidgetProvider.updateWidgets(context, manager, intArrayOf(nextId))
            assertEquals("Englisch", shadowManager.getViewFor(nextId).findViewById<TextView>(R.id.nextExamTitle).text.toString())
            val loaded = WidgetContentLoader.loadUpcomingItems(context, listId, 5)
            assertEquals("Mathematik", loaded.first().title)
            assertEquals("204", loaded.first().location)
            assertTrue(loaded.last().isAllDay)
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 220)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 800)
            }
            manager.updateAppWidgetOptions(listId, options)
            ExamListWidgetProvider().onAppWidgetOptionsChanged(context, manager, listId, options)
            val rows = shadowManager.getViewFor(listId).findViewById<ViewGroup>(R.id.listRows)
            assertEquals(1, rows.childCount)
            assertEquals("Mathematik", rows.getChildAt(0).findViewById<TextView>(R.id.widgetRowTitle).text.toString())
            ExamListWidgetProvider().onDeleted(context, intArrayOf(listId))
            assertEquals(WidgetConfig(), WidgetPreferences.readConfig(context, listId))
            assertEquals(config, WidgetPreferences.readConfig(context, nextId))
        } finally {
            repository.deleteExam("widget-exam")
            repository.replaceSyncedLessons(emptyList())
            repository.replaceSyncedEvents(emptyList())
        }
    }
}
