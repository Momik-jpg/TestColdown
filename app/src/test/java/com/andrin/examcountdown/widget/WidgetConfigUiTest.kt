package com.andrin.examcountdown.widget

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.TextView
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import com.andrin.examcountdown.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h700dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetConfigUiTest {
    @get:Rule val compose = createComposeRule()
    private var renderedView: View? = null
    private fun screenshot(name: String) {
        val dir = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui").apply { mkdirs() }
        compose.runOnIdle {
            val view = requireNotNull(renderedView)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun configNode(text: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("widget-config-list").performScrollToNode(hasText(text))
        return compose.onNodeWithText(text).performScrollTo()
    }

    private fun managementNode(text: String, substring: Boolean = false): SemanticsNodeInteraction {
        compose.onNodeWithTag("widget-settings-list").performScrollToNode(hasText(text, substring = substring))
        return compose.onNodeWithText(text, substring = substring).performScrollTo()
    }

    @Test fun configurationRestoresFiltersAndAppearanceAndSaveStaysVisibleOnNarrowPhone() {
        var saved: WidgetConfig? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                WidgetConfigScreen(WidgetConfig(), true, { saved = it }, {})
            }
        }
        screenshot("widget-config-light")
        configNode("Agenda").performClick()
        configNode("90 Tage").performClick()
        configNode("Nach Typ").performClick()
        configNode("Kompakte Ansicht").performClick()
        configNode("Raum anzeigen").performClick()
        configNode("Countdown anzeigen").performClick()
        restoration.emulateSavedInstanceStateRestore()
        configNode("Kompakte Ansicht").assertIsOn()
        compose.onNodeWithText("Raum anzeigen").assertIsOff()
        compose.onNodeWithText("Speichern").assertIsDisplayed().performClick()
        assertEquals(WidgetConfig(WidgetMode.AGENDA, 90, WidgetSortMode.TYPE_THEN_TIME, true, false, false), saved)
        screenshot("widget-config-appearance")
    }

    @Test fun nextConfigurationPreservesLegacyListSortAndCancelDoesNotSave() {
        var saved = false
        var cancelled = false
        compose.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme(darkTheme = true) {
                WidgetConfigScreen(WidgetConfig(sortMode = WidgetSortMode.TYPE_THEN_TIME, windowDays = 14), false,
                    { saved = true }, { cancelled = true })
            }
        }
        configNode("14 Tage").assertIsSelected()
        compose.onNodeWithText("Nach Typ").assertDoesNotExist()
        screenshot("widget-config-dark")
        compose.onNodeWithText("Abbrechen").assertIsDisplayed().performClick()
        assertTrue(cancelled)
        assertFalse(saved)
    }

    @Test fun privacySwitchRestoresAndSavingKeepsTheChosenMode() {
        var saved: WidgetConfig? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme { WidgetConfigScreen(WidgetConfig(), true, { saved = it }, {}) }
        }
        configNode("Persönliche Details verbergen").assertIsOff().performClick().assertIsOn()
        restoration.emulateSavedInstanceStateRestore()
        configNode("Persönliche Details verbergen").assertIsOn()
        compose.onNodeWithText("Speichern").performClick()
        assertTrue(requireNotNull(saved).privacyMode)
        screenshot("widget-config-private")
    }

    @Test fun widgetManagementRoutesAddAndExistingConfigurationIndependently() {
        val added = mutableListOf<WidgetKind>()
        var configured = 0
        var closed = false
        compose.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    WidgetSettingsScreen(listOf(InstalledWidget(42, WidgetKind.LIST, WidgetConfig(WidgetMode.AGENDA))), true,
                        { added += it }, { configured = it }, { closed = true })
                }
            }
        }
        screenshot("widget-management-light")
        compose.onNodeWithText("Nächster Eintrag hinzufügen").performClick()
        managementNode("Terminliste hinzufügen").performClick()
        assertEquals(WidgetKind.entries, added)
        screenshot("widget-management-list")
        compose.onNodeWithTag("widget-settings-list").performScrollToNode(hasTestTag("edit-widget-42"))
        compose.onNodeWithTag("edit-widget-42").performScrollTo().performClick()
        assertEquals(42, configured)
        managementNode("Zurück").performClick()
        assertTrue(closed)
    }

    @Test fun unsupportedLauncherShowsAddingInstructionsInsteadOfInactiveButton() {
        compose.setContent { ExamCountdownTheme {
            WidgetSettingsScreen(emptyList(), false, { fail("Pin unsupported") }, { fail("No widgets") }, {})
        } }
        compose.onNodeWithText("Nächster Eintrag hinzufügen").assertDoesNotExist()
        managementNode("Zum Hinzufügen auf dem Startbildschirm lange drücken", substring = true).assertIsDisplayed()
        managementNode("Noch kein Widget hinzugefügt").assertIsDisplayed()
    }

    @Test fun nativePreviewReflectsSourceAndAppearanceAndCannotLaunchExampleActions() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            renderedView = LocalView.current
            ExamCountdownTheme { WidgetConfigScreen(WidgetConfig(), false, {}, {}) }
        }
        compose.runOnIdle {
            val root = requireNotNull(renderedView)
            assertEquals("Mathematik · Funktionen", root.findViewById<TextView>(R.id.nextExamTitle).text.toString())
            assertFalse(root.findViewById<View>(R.id.nextWidgetConfigure).performClick())
            assertFalse(root.findViewById<View>(R.id.widgetRoot).performClick())
        }
        screenshot("widget-config-next")
        configNode("Agenda").performClick()
        configNode("Vorschau · Beispiel")
        compose.runOnIdle {
            assertEquals("Englisch", requireNotNull(renderedView).findViewById<TextView>(R.id.nextExamTitle).text.toString())
        }
        configNode("Raum anzeigen").performClick()
        configNode("Countdown anzeigen").performClick()
        configNode("Vorschau · Beispiel")
        compose.runOnIdle {
            val root = requireNotNull(renderedView)
            assertFalse(root.findViewById<TextView>(R.id.nextExamTime).text.contains("102"))
            assertEquals(View.GONE, root.findViewById<View>(R.id.nextExamCountdown).visibility)
        }
        compose.onNodeWithText("Ausblenden").performClick()
        compose.onNodeWithTag("widget-live-preview").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("widget-live-preview").assertDoesNotExist()
        compose.onNodeWithText("Anzeigen").performClick()
        compose.onNodeWithTag("widget-live-preview").assertIsDisplayed()
    }
}
