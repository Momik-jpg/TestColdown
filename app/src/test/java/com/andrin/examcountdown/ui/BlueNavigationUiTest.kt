package com.andrin.examcountdown.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.ui.tabs.ExamsTabContent
import com.andrin.examcountdown.ui.tabs.TimetableTabContent
import com.andrin.examcountdown.ui.tabs.state.ExamsTabUiState
import com.andrin.examcountdown.ui.tabs.state.TimetableTabUiState
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import com.andrin.examcountdown.util.SchoolTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Native component checks with synthetic data; no external service or dialog-save claims. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BlueNavigationUiTest {
    @get:Rule val compose = createComposeRule()
    private var renderedView: View? = null

    private fun screenshot(name: String) {
        val root = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui").apply { mkdirs() }
        compose.runOnIdle {
            val view = requireNotNull(renderedView)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(root, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun examFiltersRemainVisibleAfterClosingAndRestoringAndRemoveIndividually() {
        val now = SchoolTime.nowMillis()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    ExamsTabContent(ExamsTabUiState(exams = listOf(
                        Exam("math", "Lineare Funktionen", startsAtEpochMillis = now + 86_400_000L, subject = "Mathematik"),
                        Exam("english", "Literatur", startsAtEpochMillis = now + 172_800_000L, subject = "Englisch")
                    ), simpleModeEnabled = true, showSetupGuideCard = false)) {}
                }
            }
        }
        compose.onNodeWithText("Filter").performClick()
        compose.onNodeWithText("Englisch").performClick()
        compose.onNodeWithText("Späteste").performClick()
        compose.onNodeWithText("Schließen").performClick()
        compose.onNodeWithText("Filter (2)").assertIsDisplayed()
        compose.onNodeWithText("1 Prüfung").assertIsDisplayed()
        screenshot("exams-filtered")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Filter (2)").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filter Englisch entfernen").performClick()
        compose.onNodeWithText("2 Prüfungen").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filter Späteste entfernen").assertIsDisplayed()
        compose.onNodeWithText("Zurücksetzen").performClick()
        compose.onNodeWithText("Filter").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filter Späteste entfernen").assertDoesNotExist()
    }

    @Test fun timetableSearchCombinesWithStatusAndResetPreservesWeekView() {
        val now = SchoolTime.nowMillis()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    TimetableTabContent(TimetableTabUiState(lessons = listOf(
                        TimetableLesson("math", "Mathematik", "101", now + 3_600_000L, now + 6_300_000L),
                        TimetableLesson("english", "Englisch", "204", now + 7_200_000L, now + 9_900_000L,
                            isLocationChanged = true, originalLocation = "102")
                    ))) {}
                }
            }
        }
        compose.onNodeWithText("Woche").performClick()
        compose.onNodeWithText("Filter").performClick()
        compose.onNodeWithText("Raumwechsel").performClick()
        compose.onNodeWithText("Schließen").performClick()
        compose.onNodeWithText("Stundenplan durchsuchen").performTextInput("102")
        compose.onNodeWithText("Stundenplan durchsuchen").performImeAction()
        compose.onNodeWithText("1 Eintrag").assertIsDisplayed()
        compose.onNodeWithText("Filter (2)").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Woche").assertIsSelected()
        compose.onNodeWithText("Filter (2)").assertIsDisplayed()
        screenshot("timetable-filtered")
        compose.onNodeWithContentDescription("Filter Suche: 102 entfernen").performClick()
        compose.onNodeWithContentDescription("Filter Raumwechsel entfernen").assertIsDisplayed()
        compose.onNodeWithText("Zurücksetzen").performClick()
        compose.onNodeWithText("2 Einträge").assertIsDisplayed()
        compose.onNodeWithText("Woche").assertIsSelected()
    }

    @Test fun agendaKeepsActiveSourceAndImportActionVisibleAndResetPreservesList() {
        val now = SchoolTime.nowMillis()
        var enabled = false
        compose.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    EventsTimelineContent(
                        exams = listOf(Exam("math", "Mathematik", startsAtEpochMillis = now + 86_400_000L)), lessons = emptyList(),
                        events = listOf(SchoolEvent("project", "Projektabgabe", startsAtEpochMillis = now + 3_600_000L,
                            endsAtEpochMillis = now + 7_200_000L)),
                        hasIcalUrl = true, importEventsEnabled = false,
                        onOpenIcalImport = {}, onEnableEventsImportAndSync = { enabled = true },
                        onAddCustomEvents = {}, onDeleteCustomEvent = {}, onUpdateCustomEvent = {}
                    )
                }
            }
        }
        compose.onNodeWithText("Liste").performClick()
        compose.onNodeWithText("Filter").performClick()
        compose.onNodeWithText("Termine").performClick()
        compose.onNodeWithText("Schließen").performClick()
        compose.onNodeWithText("Filter (1)").assertIsDisplayed()
        compose.onNodeWithText("1 Eintrag").assertIsDisplayed()
        compose.onNodeWithText("Event-Import aktivieren").assertIsDisplayed().performClick()
        assertTrue(enabled)
        screenshot("agenda-filtered")
        compose.onNodeWithText("Zurücksetzen").performClick()
        compose.onNodeWithText("2 Einträge").assertIsDisplayed()
        compose.onNodeWithText("Liste").assertIsSelected()
        compose.onNodeWithText("Event-Import aktivieren").assertDoesNotExist()
    }

    @Test fun settingsOverviewAndWholeToggleRowAreAccessible() {
        var changed = false
        var calls = 0
        compose.setContent {
            renderedView = LocalView.current
            var status by remember { mutableStateOf(false) }
            ExamCountdownTheme {
                Scaffold(bottomBar = { HomeNavigationBar(HomeTab.entries, HomeTab.SETTINGS) {} }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        SettingsContent(status, false, { status = it; changed = it; calls++ }) {}
                    }
                }
            }
        }
        compose.onNodeWithText("Optionen").assertIsSelected()
        screenshot("settings-light")
        compose.onNodeWithText("Darstellung").performClick()
        compose.onNodeWithText("Sync-Status anzeigen").performClick().assertIsOn()
        assertTrue(changed)
        assertEquals(1, calls)
        compose.onNodeWithText("Ansicht & Bedienung").assertIsDisplayed()
    }

    @Test fun settingsSearchFindsBackupsAcrossCategoriesAndSurvivesRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme(darkTheme = true) {
                Scaffold(bottomBar = { HomeNavigationBar(HomeTab.entries, HomeTab.SETTINGS) {} }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        SettingsContent(false, false, {}) {}
                    }
                }
            }
        }
        screenshot("settings-dark-overview")
        compose.onNodeWithText("Daten").performClick()
        screenshot("settings-dark")
        compose.onNodeWithText("Einstellungen suchen").performTextInput(" bAcKuP ")
        compose.onNodeWithText("Einstellungen suchen").performImeAction()
        compose.onNodeWithText("Sicherung exportieren").assertIsDisplayed()
        compose.onNodeWithText("Sicherung importieren").assertIsDisplayed()
        compose.onNodeWithText("Kalender").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Sicherung importieren").assertIsDisplayed()
        compose.onNodeWithContentDescription("Einstellungssuche löschen").performClick()
        compose.onNodeWithText("CSV / PDF exportieren").assertIsDisplayed()
        compose.onNodeWithText("Einstellungen suchen").performTextInput("unbekannte Einstellung")
        compose.onNodeWithText("Keine passende Einstellung.", substring = true).assertIsDisplayed()
    }

    @Test fun allExistingSettingsActionsAndShortcutsRouteToTheirOwnCallback() {
        val invoked = mutableListOf<SettingsAction>()
        compose.setContent { ExamCountdownTheme { SettingsContent(false, true, {}) { invoked += it } } }
        compose.onNodeWithText("Verbinden").performClick()
        compose.onNodeWithText("Aktualisieren").performClick()
        val actions = SettingsAction.entries.filter { it != SettingsAction.SYNC_STATUS }
        actions.forEach { action ->
            compose.onNodeWithText("Einstellungen suchen").performScrollTo().performTextReplacement(action.title)
            compose.onNode(hasText(action.title) and hasClickAction() and !hasSetTextAction())
                .performScrollTo().performClick()
        }
        assertEquals(listOf(SettingsAction.CALENDAR, SettingsAction.SYNC_NOW) + actions, invoked)
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-mdpi")
    fun narrowSettingsWithLargeTextKeepShortcutLabelsWholeAndActionsReachable() {
        val invoked = mutableListOf<SettingsAction>()
        compose.setContent {
            renderedView = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.6f)) {
                ExamCountdownTheme {
                    Surface(Modifier.fillMaxSize()) {
                        SettingsContent(false, false, {}) { invoked += it }
                    }
                }
            }
        }
        val connect = compose.onNodeWithText("Verbinden").assertIsDisplayed()
        val update = compose.onNodeWithText("Aktualisieren").assertIsDisplayed()
        assertTrue("Large-text buttons must stack", update.fetchSemanticsNode().boundsInRoot.top >
            connect.fetchSemanticsNode().boundsInRoot.bottom)
        screenshot("settings-narrow-large-text")
        for (node in listOf(connect, update)) {
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.single().lineCount)
            val layout = layouts.single()
            assertEquals(layout.layoutInput.text.length, layout.getLineEnd(0))
            assertTrue("Shortcut label is ellipsized", !layout.isLineEllipsized(0))
            // Compose rounds intrinsic text width to whole pixels at non-integral font scales.
            val width = layout.getLineRight(0) - layout.getLineLeft(0)
            assertTrue("Shortcut label exceeds its actual bounds: $width/${layout.size.width}", width <= layout.size.width + 1f)
            assertTrue("Shortcut label is cut vertically", layout.multiParagraph.height <= layout.size.height + 1f)
        }
        connect.performClick()
        update.performClick()
        assertEquals(listOf(SettingsAction.CALENDAR, SettingsAction.SYNC_NOW), invoked)
    }
}
