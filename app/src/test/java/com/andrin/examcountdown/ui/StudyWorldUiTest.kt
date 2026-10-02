package com.andrin.examcountdown.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.ui.tabs.ExamsTabContent
import com.andrin.examcountdown.ui.tabs.events.ExamsTabEvent
import com.andrin.examcountdown.ui.tabs.state.ExamsTabUiState
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Render real Compose controls with synthetic school data, never a private calendar. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudyWorldUiTest {
    @get:Rule val compose = createComposeRule()
    private var renderedView: View? = null
    private val exam = Exam(id = "sample", title = "Lineare Funktionen", subject = "Mathematik",
        startsAtEpochMillis = System.currentTimeMillis() + 86_400_000L)

    private fun show(dark: Boolean, empty: Boolean = false, accessible: Boolean = false,
                     onEvent: (ExamsTabEvent) -> Unit = {}) {
        compose.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme(darkTheme = dark, accessibilityMode = accessible) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ExamsTabContent(
                        ExamsTabUiState(exams = if (empty) emptyList() else listOf(exam),
                            showSetupGuideCard = false), onEvent
                    )
                }
            }
        }
    }

    private fun screenshot(name: String) {
        val root = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui")
        root.mkdirs()
        // Drawing the native view avoids PixelCopy's asynchronous callback under Robolectric.
        compose.runOnIdle {
            val view = requireNotNull(renderedView)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(root, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }

    @Test fun lightThemeShowsTheNextExamAndRoutesStudyAction() {
        var planned = false
        show(dark = false, onEvent = { planned = it == ExamsTabEvent.PlanStudy(exam) })
        compose.onNodeWithText("Lernen planen").assertIsDisplayed().performClick()
        assertTrue(planned)
        screenshot("exams-light")
    }

    @Test fun darkThemeKeepsTheStudyActionReadable() {
        show(dark = true)
        compose.onNodeWithText("Lernen planen").assertIsDisplayed()
        screenshot("exams-dark")
    }

    @Test fun highContrastThemeStillOffersThePrimaryAction() {
        show(dark = false, accessible = true)
        compose.onNodeWithText("Lernen planen").assertIsDisplayed()
        screenshot("exams-high-contrast")
    }

    @Test fun emptyStateOffersAnActualAddAction() {
        var added = false
        show(dark = false, empty = true, onEvent = { added = it == ExamsTabEvent.AddExam })
        compose.onNodeWithText("Prüfung hinzufügen").assertIsDisplayed().performClick()
        assertTrue(added)
        screenshot("exams-empty")
    }

    @Test fun gradeInputsSurviveStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GradeCalculatorScreen()
                }
            }
        }
        compose.onAllNodesWithText("Note").onFirst().performTextInput("5,5")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("5,5").assertIsDisplayed()
        screenshot("grades-light")
    }

    @Test fun navigationAnnouncesSelectionAndChangesTheActiveTab() {
        compose.setContent {
            var selected by remember { mutableStateOf(HomeTab.EXAMS) }
            ExamCountdownTheme {
                HomeNavigationBar(HomeTab.entries, selected) { selected = it }
            }
        }
        compose.onNodeWithText("Prüfungen").assertIsSelected()
        compose.onNodeWithText("Noten").performClick().assertIsSelected()
    }
}
