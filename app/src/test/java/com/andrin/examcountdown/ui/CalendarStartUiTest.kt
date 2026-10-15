package com.andrin.examcountdown.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.ui.tabs.ExamInsightsCard
import com.andrin.examcountdown.ui.tabs.SetupGuideCard
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Native narrow layouts with synthetic data; action routing is not a live calendar test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h800dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarStartUiTest {
    @get:Rule val compose = createComposeRule()
    private var rendered: View? = null

    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            rendered = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.6f)) {
                ExamCountdownTheme(darkTheme = dark) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) { content() }
                    }
                }
            }
        }
    }

    private fun assertCompleteText(text: String) {
        val node = compose.onNodeWithText(text, useUnmergedTree = true)
        node.performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val result = layouts.single()
        // Intrinsic text widths are fractional; layout sizes are rounded to whole pixels.
        // Check the rendered line extents with one pixel of rounding tolerance.
        val lineExtents = (0 until result.lineCount).map { result.getLineLeft(it) to result.getLineRight(it) }
        assertTrue("$text exceeds its ${result.size.width}px layout: $lineExtents",
            lineExtents.all { (left, right) -> left >= -1f && right <= result.size.width + 1f })
        assertEquals(text.length, result.getLineEnd(result.lineCount - 1))
        assertFalse(result.isLineEllipsized(result.lineCount - 1))
    }

    private fun clickAction(text: String) {
        val action = compose.onNodeWithText(text).performScrollTo()
        assertTrue("$text touch height is too small", action.fetchSemanticsNode().boundsInRoot.height >= 48f)
        action.performClick()
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

    @Test fun firstCalendarSetupKeepsAllActionsReadableAndRoutesEachOnce() {
        var connected = 0; var refreshed = 0; var helped = 0; var hidden = 0
        show {
            SetupGuideCard(0, false, false, null, false,
                { connected++ }, { refreshed++ }, { helped++ }, { hidden++ })
        }
        compose.onNodeWithText("Dein Kalenderstart").assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        assertCompleteText("Kalender verbinden")
        assertCompleteText("Hilfe")
        clickAction("Kalender verbinden"); clickAction("Hilfe")
        clickAction("Nicht mehr anzeigen")
        assertEquals(1, connected); assertEquals(0, refreshed)
        assertEquals(1, helped); assertEquals(1, hidden)
        compose.onNodeWithText("Dein Kalenderstart").performScrollTo()
        screenshot("calendar-start-large-text")
    }

    @Test fun failedSyncDoesNotLookCompletedEvenAfterAnEarlierSuccess() {
        var retried = 0; var connected = 0
        val error = "Der Kalender konnte nicht geladen werden. Prüfe deine Internetverbindung und versuche es erneut."
        val message = "Die letzte Aktualisierung ist fehlgeschlagen. Gespeicherte Termine bleiben verfügbar."
        show {
            SetupGuideCard(0, true, true, error, false, { connected++ }, { retried++ }, {}, {})
        }
        compose.onNodeWithText("Sync prüfen").assertIsDisplayed()
        compose.onNodeWithText("Sync erledigt").assertDoesNotExist()
        compose.onNodeWithText(message).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertCompleteText(message); assertCompleteText(error); assertCompleteText("Erneut versuchen")
        clickAction("Erneut versuchen")
        assertEquals(1, retried); assertEquals(0, connected)
        compose.onNodeWithText("Dein Kalenderstart").performScrollTo()
        screenshot("calendar-retry-large-text")
    }

    @Test fun invalidLinkOffersRepairInsteadOfRetryInDarkMode() {
        var repaired = 0; var retried = 0
        show(dark = true) {
            SetupGuideCard(4, true, true, "Der Link ist abgelaufen.", true,
                { repaired++ }, { retried++ }, {}, {})
        }
        compose.onNodeWithText("Sync prüfen").assertIsDisplayed()
        compose.onNodeWithText("Sync erledigt").assertDoesNotExist()
        assertCompleteText("Der gespeicherte Link ist ungültig oder abgelaufen.")
        assertCompleteText("Link reparieren"); clickAction("Link reparieren")
        assertEquals(1, repaired); assertEquals(0, retried)
        compose.onNodeWithText("Dein Kalenderstart").performScrollTo()
        screenshot("calendar-repair-dark-large-text")
    }

    @Test fun connectedEmptyCalendarAndFirstSyncHaveDifferentGuidance() {
        show {
            SetupGuideCard(0, true, false, null, false, {}, {}, {}, {})
            SetupGuideCard(0, true, true, null, false, {}, {}, {}, {})
        }
        assertCompleteText("Dein Kalender ist verbunden. Starte jetzt die erste Aktualisierung.")
        assertCompleteText("Dein Kalender wurde aktualisiert. Es sind noch keine Prüfungen gespeichert.")
        screenshot("calendar-empty-after-sync-large-text")
    }

    @Test fun overviewCountsAndLabelsStayWholeWithoutOverlappingAtLargeText() {
        val now = 1_000_000L
        val exams = (1..125).map { day ->
            Exam("sample-$day", "Beispiel $day", subject = "Mathematik",
                startsAtEpochMillis = now + day * 86_400_000L)
        }
        show { ExamInsightsCard(exams, 125, now) }
        listOf("Sichtbar", "7 Tage", "30 Tage", "125", "7", "30").forEach { assertCompleteText(it) }
        val labels = listOf("Sichtbar", "7 Tage", "30 Tage").map {
            compose.onNodeWithText(it, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        }
        assertTrue(labels[1].top > labels[0].bottom)
        assertTrue(labels[2].top > labels[1].bottom)
        compose.onNodeWithText("Überblick").performScrollTo()
        screenshot("exam-overview-large-text")
    }
}
