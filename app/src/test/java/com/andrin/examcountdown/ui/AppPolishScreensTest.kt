package com.andrin.examcountdown.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.ui.tabs.ExamsTabContent
import com.andrin.examcountdown.ui.tabs.TimetableTabContent
import com.andrin.examcountdown.ui.tabs.state.ExamsTabUiState
import com.andrin.examcountdown.ui.tabs.state.TimetableTabUiState
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import com.andrin.examcountdown.util.SchoolTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Native app components assembled in a scaffold with synthetic data; not an on-phone or sync test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppPolishScreensTest {
    @get:Rule val compose = createComposeRule()
    private var rendered: View? = null

    @OptIn(ExperimentalMaterial3Api::class)
    private fun checkTabs(dark: Boolean) {
        val now = SchoolTime.nowMillis()
        val exams = listOf(Exam("sample", "Lineare Funktionen", subject = "Mathematik", startsAtEpochMillis = now + 86_400_000L))
        val lessons = listOf(
            TimetableLesson("now", "Mathematik", "101", now - 600_000, now + 2_100_000),
            TimetableLesson("next", "Englisch", "204", now + 3_600_000, now + 6_300_000,
                isLocationChanged = true, originalLocation = "102")
        )
        compose.setContent {
            rendered = LocalView.current
            var selected by remember { mutableStateOf(HomeTab.EXAMS) }
            ExamCountdownTheme(darkTheme = dark) {
                Scaffold(topBar = { TopAppBar(title = { AppScreenHeading(selected.title) }) },
                    bottomBar = { HomeNavigationBar(HomeTab.entries, selected) { selected = it } }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (selected) {
                            HomeTab.EXAMS -> ExamsTabContent(ExamsTabUiState(exams = exams, showSetupGuideCard = false)) {}
                            HomeTab.TIMETABLE -> TimetableTabContent(TimetableTabUiState(lessons = lessons)) {}
                            HomeTab.EVENTS -> EventsTimelineContent(exams, lessons,
                                listOf(SchoolEvent("project", "Projektabgabe", startsAtEpochMillis = now + 7_200_000,
                                    endsAtEpochMillis = now + 10_800_000)),
                                false, true, {}, {}, {}, {}, {})
                            HomeTab.GRADES -> GradeCalculatorScreen(Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                            HomeTab.SETTINGS -> SettingsContent(false, false, {}) {}
                        }
                    }
                }
            }
        }
        HomeTab.entries.forEach { tab ->
            compose.onNodeWithContentDescription(tab.title).performClick().assertIsSelected()
            val expected = when (tab) {
                HomeTab.EXAMS -> "Lernen planen"
                HomeTab.TIMETABLE -> "Raum geändert"
                HomeTab.EVENTS -> "Suche"
                HomeTab.GRADES -> "Durchschnitt"
                HomeTab.SETTINGS -> "Dein Setup"
            }
            compose.onNodeWithText(expected).assertIsDisplayed()
            compose.runOnIdle {
                val view = requireNotNull(rendered)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val root = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui").apply { mkdirs() }
                File(root, "app-${tab.route}-${if (dark) "dark" else "light"}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
        }
    }
    @Test fun lightScaffoldNavigatesThroughAllFiveActualTabComponents() = checkTabs(false)
    @Test fun darkScaffoldNavigatesThroughAllFiveActualTabComponents() = checkTabs(true)
}
