package com.andrin.examcountdown.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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
@Config(sdk = [33], qualifiers = "w320dp-h800dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactNavigationUiTest {
    @get:Rule val compose = createComposeRule()
    private var rendered: View? = null

    private fun save(name: String) {
        compose.runOnIdle {
            val view = requireNotNull(rendered)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            val folder = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui").apply { mkdirs() }
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun directDestinationsStayVisibleAndRouteOnceAtLargeText() {
        val invoked = mutableListOf<HomeTab>()
        compose.setContent {
            rendered = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.6f)) {
                var selected by remember { mutableStateOf(HomeTab.EXAMS) }
                ExamCountdownTheme(darkTheme = true) {
                    Surface(Modifier.fillMaxSize()) {
                        Column {
                            Spacer(Modifier.weight(1f))
                            HomeNavigationBar(HomeTab.entries, selected) { selected = it; invoked += it }
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Bereiche öffnen").assertDoesNotExist()
        var rightEdge = 0f
        HomeTab.entries.forEach { tab ->
            val bounds = compose.onNodeWithTag("home-tab-${tab.route}").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            assertTrue("Distinct 48 dp targets", bounds.width >= 48f && bounds.height >= 48f)
            assertTrue("Destination leaves screen or overlaps", bounds.left >= rightEdge && bounds.right <= 320f)
            rightEdge = bounds.right
        }
        save("navigation-dark-large-text")
        assertTrue(invoked.isEmpty())
        HomeTab.entries.forEach { tab ->
            val destination = compose.onNodeWithTag("home-tab-${tab.route}")
            assertTrue(destination.fetchSemanticsNode().boundsInRoot.height >= 48f)
            destination.performClick()
            compose.onNodeWithContentDescription(tab.title).assertIsSelected()
            compose.onNodeWithText(tab.title).assertIsDisplayed()
        }
        assertEquals(HomeTab.entries.toList(), invoked)
        save("navigation-options-dark-large-text")
    }

    @Test fun simplifiedNavigationOnlyOffersEnabledDestinationsDirectly() {
        var calls = 0
        compose.setContent {
            rendered = LocalView.current
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column {
                        Spacer(Modifier.weight(1f))
                        HomeNavigationBar(listOf(HomeTab.EXAMS, HomeTab.SETTINGS), HomeTab.EXAMS) { calls++ }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Notenrechner").assertDoesNotExist()
        compose.onNodeWithText("Stundenplan").assertDoesNotExist()
        compose.onNodeWithText("Agenda").assertDoesNotExist()
        compose.onNodeWithText("Notenrechner").assertDoesNotExist()
        compose.onNodeWithContentDescription("Einstellungen").assertIsDisplayed()
        save("navigation-simple")
        compose.onNodeWithText("Prüfungen").assertIsSelected()
        assertEquals(0, calls)
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        assertEquals(1, calls)
    }
}
