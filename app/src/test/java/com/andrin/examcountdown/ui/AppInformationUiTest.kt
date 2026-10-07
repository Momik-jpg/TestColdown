package com.andrin.examcountdown.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import com.andrin.examcountdown.ui.theme.LocalDecorativeArtEnabled
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h800dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppInformationUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun asset(page: AppInformationPage) = context.assets.open("legal/${page.asset}").bufferedReader().use { it.readText() }

    @Test fun actualApkAssetsContainPolicyProjectLicenseAndResolvedDependencyNotices() {
        assertTrue(asset(AppInformationPage.PRIVACY).contains("IP-Adresse"))
        val licenses = asset(AppInformationPage.LICENSES)
        assertTrue(licenses.contains("MIT License"))
        assertTrue(licenses.contains("LicenseRef-TestColdown-NC-Privacy-1.0"))
        assertTrue(licenses.contains("Kein Verkauf"))
        assertTrue(licenses.contains("Bisherige TestColdown-Bestandteile"))
        assertTrue(licenses.contains("Benutzerdaten"))
        assertTrue(licenses.contains("## androidx.core:core-ktx:1.13.0"))
        assertTrue(licenses.contains("Apache License"))
        assertTrue(licenses.contains("Originale Hinweise"))
        assertTrue(licenses.contains("KI-generierte"))
    }

    @Test fun largeTextPolicyIsScrollableOfflineAndCanBeClosed() {
        var view: View? = null
        var closed = false
        compose.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                ExamCountdownTheme(accessibilityMode = true) {
                    AppInformationContent(AppInformationPage.PRIVACY, asset(AppInformationPage.PRIVACY), { closed = true }, {})
                }
            }
        }
        val heading = compose.onNodeWithTag("app-information-title").assertIsDisplayed().fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        assertTrue(heading.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts))
        assertFalse(layouts.single().hasVisualOverflow)
        assertTrue("The large-font heading must fit its visible bounds", layouts.single().size.height <= heading.boundsInRoot.height)
        compose.onNodeWithText("Projekt und Kontakt").performScrollTo().assertIsDisplayed()
        screenshot(requireNotNull(view), "privacy-large-text")
        compose.onNodeWithTag("app-information-list").performScrollToNode(hasText("Export, Teilen und Sicherungen"))
        compose.onNodeWithText("Export, Teilen und Sicherungen").assertIsDisplayed()
        compose.onNodeWithTag("app-information-list").performScrollToNode(hasText("Deine Kontrolle und Löschung"))
        compose.onNodeWithText("Deine Kontrolle und Löschung").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hinweise schließen").performClick()
        assertTrue(closed)
    }

    @Test fun accessibilityHelpOffersWorkingShortcutAndSuppressesDecorativeArt() {
        var adjusted = false
        var decorativeArt = true
        var view: View? = null
        compose.setContent {
            view = LocalView.current
            ExamCountdownTheme(darkTheme = true, accessibilityMode = true) {
                decorativeArt = LocalDecorativeArtEnabled.current
                AppInformationContent(AppInformationPage.ACCESSIBILITY, asset(AppInformationPage.ACCESSIBILITY), {}, { adjusted = true })
            }
        }
        compose.onNodeWithText("Ansicht & Bedienung anpassen").performClick()
        assertTrue(adjusted)
        assertFalse(decorativeArt)
        screenshot(requireNotNull(view), "accessibility-help-dark")
    }

    private fun screenshot(view: View, name: String) = compose.runOnIdle {
        val dir = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui").apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
