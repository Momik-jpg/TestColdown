package com.andrin.examcountdown.widget

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
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
        compose.onNodeWithText("Agenda").performClick()
        compose.onNodeWithText("90 Tage").performScrollTo().performClick()
        compose.onNodeWithText("Nach Typ").performScrollTo().performClick()
        compose.onNodeWithText("Kompakte Ansicht").performScrollTo().performClick()
        compose.onNodeWithText("Raum anzeigen").performScrollTo().performClick()
        compose.onNodeWithText("Countdown anzeigen").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Kompakte Ansicht").performScrollTo().assertIsOn()
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
        compose.onNodeWithText("14 Tage").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Nach Typ").assertDoesNotExist()
        screenshot("widget-config-dark")
        compose.onNodeWithText("Abbrechen").assertIsDisplayed().performClick()
        assertTrue(cancelled)
        assertFalse(saved)
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
        compose.onNodeWithText("Nächster Eintrag hinzufügen").performClick()
        compose.onNodeWithText("Terminliste hinzufügen").performScrollTo().performClick()
        assertEquals(WidgetKind.entries, added)
        screenshot("widget-management-light")
        compose.onNodeWithText("Widget #42 einstellen").performScrollTo().performClick()
        assertEquals(42, configured)
        compose.onNodeWithText("Zurück").performScrollTo().performClick()
        assertTrue(closed)
    }

    @Test fun unsupportedLauncherShowsAddingInstructionsInsteadOfInactiveButton() {
        compose.setContent { ExamCountdownTheme {
            WidgetSettingsScreen(emptyList(), false, { fail("Pin unsupported") }, { fail("No widgets") }, {})
        } }
        compose.onNodeWithText("Nächster Eintrag hinzufügen").assertDoesNotExist()
        compose.onNodeWithText("Zum Hinzufügen auf dem Startbildschirm lange drücken", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Noch kein Widget hinzugefügt").performScrollTo().assertIsDisplayed()
    }
}
