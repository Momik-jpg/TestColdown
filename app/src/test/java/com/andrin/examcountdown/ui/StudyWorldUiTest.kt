package com.andrin.examcountdown.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.onNodeWithContentDescription
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.ui.tabs.ExamsTabContent
import com.andrin.examcountdown.ui.tabs.TimetableTabContent
import com.andrin.examcountdown.ui.tabs.events.ExamsTabEvent
import com.andrin.examcountdown.ui.tabs.state.ExamsTabUiState
import com.andrin.examcountdown.ui.tabs.state.TimetableTabUiState
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import com.andrin.examcountdown.util.SchoolTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
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

    private fun assertNativeEventDialogOpened() {
        // The headless renderer does not attach the dialog's Compose root (API 28 and 33).
        // Verify the real Android dialog was shown; this is not an editor/save assertion.
        compose.mainClock.advanceTimeBy(32)
        compose.runOnUiThread {
            val dialog = requireNotNull(ShadowDialog.getLatestDialog())
            assertTrue(dialog.isShowing)
            dialog.dismiss()
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
                    GradeCalculatorScreen(Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
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

    @Test fun compactTimetableKeepsRoomsChangesAndViewControls() {
        val now = SchoolTime.nowMillis()
        val lessons = listOf(
            TimetableLesson("now", "Mathematik", "101", now - 600_000, now + 2_100_000),
            TimetableLesson("next", "Englisch", "204", now + 3_600_000, now + 6_300_000,
                isLocationChanged = true, originalLocation = "102"),
            TimetableLesson("later", "Geschichte", "303", now + 7_200_000, now + 9_900_000)
        )
        compose.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    TimetableTabContent(TimetableTabUiState(lessons = lessons)) {}
                }
            }
        }
        compose.onNodeWithText("Raum 204", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Raum geändert").assertIsDisplayed()
        screenshot("timetable-light")
        compose.onNodeWithText("Filter").performClick()
        compose.onNodeWithText("Raumwechsel").performClick().assertIsSelected()
        compose.onNodeWithText("Zurücksetzen").performClick()
        compose.onNodeWithText("Alle").assertIsSelected()
        compose.onNodeWithText("Schließen").performClick()
        compose.onNodeWithText("Woche").performClick().assertIsSelected()
        screenshot("timetable-week")
    }

    @Test fun agendaSearchClearsAndOpensTheNativeEventDialog() {
        val now = SchoolTime.nowMillis()
        compose.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    EventsTimelineContent(
                        exams = listOf(exam), lessons = emptyList(),
                        events = listOf(SchoolEvent(id = "sample-event", title = "Projektabgabe",
                            startsAtEpochMillis = now, endsAtEpochMillis = now + 3_600_000)),
                        hasIcalUrl = false, importEventsEnabled = true,
                        onOpenIcalImport = {}, onEnableEventsImportAndSync = {},
                        onAddCustomEvents = {},
                        onDeleteCustomEvent = {}, onUpdateCustomEvent = {}
                    )
                }
            }
        }
        compose.onNodeWithText("Suche").performTextInput("Projekt")
        compose.onNodeWithText("Suche").performImeAction()
        compose.onNodeWithText("Suche").assertIsNotFocused()
        compose.onNodeWithContentDescription("Suche zurücksetzen").performClick()
        compose.onNodeWithText("Projekt").assertDoesNotExist()
        screenshot("agenda-light")
        compose.onNodeWithText("Neuer Termin").performClick()
        assertNativeEventDialogOpened()
    }

    @Test fun inputKeyboardMovesToNextFieldAndDoneClearsFocus() {
        compose.setContent {
            ExamCountdownTheme {
                Column {
                    var first by remember { mutableStateOf("") }
                    var second by remember { mutableStateOf("") }
                    AppTextField(first, { first = it }, singleLine = true, label = { Text("Erstes Feld") })
                    AppTextField(second, { second = it }, singleLine = true, label = { Text("Zweites Feld") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                }
            }
        }
        compose.onNodeWithText("Erstes Feld").performTextInput("Test")
        compose.onNodeWithText("Erstes Feld").performImeAction()
        compose.onNodeWithText("Zweites Feld").assertIsFocused().performImeAction()
        compose.onNodeWithText("Zweites Feld").assertIsNotFocused()
    }

    @Test fun emptyAgendaOpensTheFirstManualEventWithoutCalendarImport() {
        compose.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    EventsTimelineContent(
                        exams = emptyList(), lessons = emptyList(), events = emptyList(),
                        hasIcalUrl = false, importEventsEnabled = false,
                        onOpenIcalImport = {}, onEnableEventsImportAndSync = {},
                        onAddCustomEvents = {},
                        onDeleteCustomEvent = {}, onUpdateCustomEvent = {}
                    )
                }
            }
        }
        compose.onNodeWithText("Neuer Termin").assertIsDisplayed()
        screenshot("agenda-empty")
        compose.onNodeWithText("Neuer Termin").performClick()
        assertNativeEventDialogOpened()
    }

    @Test fun invalidWeightIsExplainedAndCanBeCorrected() {
        compose.setContent { ExamCountdownTheme { GradeCalculatorScreen() } }
        val weight = compose.onAllNodesWithText("Gewicht").onFirst()
        weight.performTextReplacement("0")
        compose.onNodeWithText("Größer als 0").assertIsDisplayed()
        weight.performTextReplacement("1,5")
        compose.onNodeWithText("Größer als 0").assertDoesNotExist()
    }

    @Test fun optionalCategoriesRemainEditableAndKeepTheirValues() {
        compose.setContent { ExamCountdownTheme { GradeCalculatorScreen() } }
        compose.onNodeWithText("Kategorie").assertDoesNotExist()
        compose.onNodeWithText("Kategorien").performClick()
        compose.onAllNodesWithText("Kategorie").onFirst().performTextReplacement("Mitarbeit")
        compose.onNodeWithText("Fertig").performClick()
        compose.onNodeWithText("Kategorien").performClick()
        compose.onNodeWithText("Mitarbeit").assertIsDisplayed()
    }
}
