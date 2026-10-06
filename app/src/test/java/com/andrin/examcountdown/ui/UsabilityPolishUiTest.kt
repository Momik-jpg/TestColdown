package com.andrin.examcountdown.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.data.SyncStatus
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import com.andrin.examcountdown.widget.WidgetConfig
import com.andrin.examcountdown.widget.WidgetConfigScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Real narrow Compose layouts and user actions; no external-calendar or phone claims. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h800dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UsabilityPolishUiTest {
    @get:Rule val compose = createComposeRule()
    private var rendered: View? = null

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            rendered = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.6f)) {
                ExamCountdownTheme { Surface(Modifier.fillMaxSize(), content = content) }
            }
        }
    }

    private fun screenshot(name: String) {
        compose.runOnIdle {
            val view = requireNotNull(rendered)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val folder = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui").apply { mkdirs() }
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun layout(node: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }

    private fun assertWholeLabel(text: String) {
        val textLayout = layout(compose.onAllNodesWithText(text, useUnmergedTree = true).onFirst())
        assertEquals("$text breaks mid-word", 1, textLayout.lineCount)
        assertEquals("$text is shortened", text.length, textLayout.getLineEnd(0))
        assertFalse("$text is ellipsized", textLayout.isLineEllipsized(0))
    }

    @Test fun narrowLargeTextNavigationKeepsSelectedNameWhole() {
        show {
            var selected by remember { mutableStateOf(HomeTab.EXAMS) }
            Column {
                Spacer(Modifier.weight(1f))
                HomeNavigationBar(HomeTab.entries, selected) { selected = it }
            }
        }
        screenshot("polish-navigation-large-text")
        HomeTab.entries.forEach { assertWholeLabel(it.shortTitle) }
        HomeTab.entries.forEach { tab ->
            val target = compose.onNodeWithContentDescription(tab.title)
            assertTrue(target.fetchSemanticsNode().boundsInRoot.width >= 48f)
            target.performClick().assertIsSelected()
            assertWholeLabel(tab.shortTitle)
        }
    }

    @Test fun narrowLargeTextFiltersKeepCountAndBothActionsReachable() {
        var reset = false
        var toggle = false
        show {
            Column(Modifier.padding(16.dp)) {
                FilterControls("125 Prüfungen", 2, false, { toggle = true }, { reset = true })
            }
        }
        screenshot("polish-filters-large-text")
        compose.onNodeWithText("125 Prüfungen").assertIsDisplayed()
        assertWholeLabel("Zurücksetzen")
        assertWholeLabel("Filter (2)")
        compose.onNodeWithText("Filter (2)").performClick()
        compose.onNodeWithText("Zurücksetzen").performClick()
        assertTrue(reset && toggle)
    }

    @Test fun narrowGradeFieldsKeepWeightLabelReadable() {
        show { GradeCalculatorScreen(Modifier.padding(16.dp)) }
        screenshot("polish-grades-large-text")
        assertWholeLabel("Gewicht")
    }

    @Test fun fullToggleRowHasActualMinimumTouchHeightAndChangesExactlyOnce() {
        var calls = 0
        show {
            var checked by remember { mutableStateOf(false) }
            Column(Modifier.padding(16.dp)) {
                SettingToggleRow("Status anzeigen", checked) { checked = it; calls++ }
            }
        }
        val row = compose.onNodeWithText("Status anzeigen")
        assertTrue("Switch row relies on overlapping invisible touch padding", row.fetchSemanticsNode().boundsInRoot.height >= 48f)
        row.performClick().assertIsOn()
        assertEquals(1, calls)
    }

    @Test fun narrowWidgetFooterKeepsSaveAndCancelWholeAndFunctional() {
        var saved: WidgetConfig? = null
        var cancelled = false
        val config = WidgetConfig()
        show { WidgetConfigScreen(config, false, { saved = it }, { cancelled = true }) }
        screenshot("polish-widget-footer-large-text")
        assertWholeLabel("Ausblenden")
        assertWholeLabel("Speichern")
        assertWholeLabel("Abbrechen")
        compose.onNodeWithText("Speichern").performClick()
        compose.onNodeWithText("Abbrechen").performClick()
        assertEquals(config, saved)
        assertTrue(cancelled)
        compose.onNodeWithText("Ausblenden").performClick()
        compose.onNodeWithTag("widget-config-list").performScrollToNode(hasText("Nur Prüfungen"))
        assertWholeLabel("Nur Prüfungen")
        compose.onNodeWithText("Nur Prüfungen").assertIsSelected()
        screenshot("polish-widget-choice-large-text")
    }

    @Test fun syncDetailsKeepFullErrorReadable() {
        val error = "Der Kalender konnte nicht aktualisiert werden. Prüfe die Verbindung und deinen Kalender-Link. Deine gespeicherten Termine bleiben verfügbar."
        show { Column { SyncStatusStrip(SyncStatus(lastSyncError = error)) } }
        compose.onNodeWithText("Kalenderaktualisierung fehlgeschlagen").performClick()
        screenshot("polish-sync-error-large-text")
        val textLayout = layout(compose.onNodeWithText(error))
        assertEquals("Sync error is shortened", error.length, textLayout.getLineEnd(textLayout.lineCount - 1))
        assertFalse(textLayout.isLineEllipsized(textLayout.lineCount - 1))
    }

    @Test fun filterChipsExposeSelectionAndHaveDistinctTouchBounds() {
        var calls = 0
        show {
            var selected by remember { mutableStateOf(false) }
            Column {
                AppFilterChip(selected, { selected = !selected; calls++ }, { Text("Raumwechsel") })
                AppFilterChip(false, {}, { Text("Alle") })
            }
        }
        val chip = compose.onNodeWithText("Raumwechsel")
        chip.assertIsNotSelected().performClick().assertIsSelected()
        val first = chip.fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Alle").fetchSemanticsNode().boundsInRoot
        assertTrue(first.height >= 48f && second.top >= first.bottom)
        assertEquals(1, calls)
    }

    @Test fun gradeWeightExplainsErrorsToAccessibilityAndDoneDismissesFocus() {
        show { GradeCalculatorScreen(Modifier.padding(16.dp)) }
        val weight = compose.onAllNodesWithText("Gewicht").onFirst()
        weight.performScrollTo().performTextReplacement("0")
        weight.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Das Gewicht muss eine Zahl größer als 0 sein."))
        weight.performTextReplacement("1,5")
        weight.performImeAction()
        weight.assertIsNotFocused()
        weight.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test fun narrowMainTitleRemainsAReadableAccessibilityHeading() {
        show { Column(Modifier.padding(16.dp)) { AppScreenHeading("Einstellungen") } }
        assertWholeLabel("Einstellungen")
        compose.onNodeWithText("Einstellungen").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        screenshot("polish-heading-large-text")
    }

    @Test fun syncExpansionSurvivesRestorationAndRemainsDismissible() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ExamCountdownTheme {
                SyncStatusStrip(SyncStatus(lastSyncSummary = "16 Prüfungen und 135 Lektionen synchronisiert."))
            }
        }
        val heading = compose.onNodeWithText("Noch keine Synchronisierung")
        heading.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Geschlossen"))
            .performClick()
        compose.onNodeWithText("16 Prüfungen und 135 Lektionen synchronisiert.").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("16 Prüfungen und 135 Lektionen synchronisiert.").assertIsDisplayed()
        heading.performClick()
        compose.onNodeWithText("16 Prüfungen und 135 Lektionen synchronisiert.").assertDoesNotExist()
    }
}
