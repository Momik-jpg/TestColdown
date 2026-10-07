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
            // Modal sheets live in a separate Android window; capture its actual decor.
            val windowManager = Class.forName("android.view.WindowManagerGlobal")
            val manager = windowManager.getDeclaredMethod("getInstance").invoke(null)
            val viewsField = windowManager.getDeclaredField("mViews").apply { isAccessible = true }
            val windows = (viewsField.get(manager) as List<*>).filterIsInstance<View>()
            val view = windows.lastOrNull { it.visibility == View.VISIBLE && it.width > 0 && it.height > 0 }
                ?: requireNotNull(rendered)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            requireNotNull(rendered).draw(canvas)
            if (view !== rendered) view.draw(canvas)
            val folder = File(System.getenv("STUDY_UI_ARTIFACTS") ?: "build/study-ui").apply { mkdirs() }
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun verticalMenuRoutesEachVisibleDestinationExactlyOnceAtLargeText() {
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
        compose.onNodeWithContentDescription("Bereiche öffnen").performClick()
        compose.onNodeWithText("Bereiche").assertIsDisplayed()
        HomeTab.entries.forEach { compose.onNodeWithTag("home-menu-${it.route}").assertIsDisplayed() }
        save("compact-menu-dark-large-text")
        compose.onNodeWithContentDescription("Menü schliessen").performClick()
        assertTrue(invoked.isEmpty())
        HomeTab.entries.forEach { tab ->
            compose.onNodeWithContentDescription("Bereiche öffnen").performClick()
            val destination = compose.onNodeWithTag("home-menu-${tab.route}")
            assertTrue(destination.fetchSemanticsNode().boundsInRoot.height >= 48f)
            destination.performClick()
            compose.onNodeWithContentDescription(tab.title).assertIsSelected()
            compose.onNodeWithText("Bereiche").assertDoesNotExist()
        }
        assertEquals(HomeTab.entries.toList(), invoked)
        save("compact-dock-dark-large-text")
    }

    @Test fun simplifiedMenuOnlyOffersEnabledTabsAndDismissDoesNotNavigate() {
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
        compose.onNodeWithContentDescription("Bereiche öffnen").performClick()
        compose.onNodeWithText("Stundenplan").assertDoesNotExist()
        compose.onNodeWithText("Agenda").assertDoesNotExist()
        compose.onNodeWithText("Notenrechner").assertDoesNotExist()
        compose.onNodeWithText("Einstellungen").assertIsDisplayed()
        save("compact-menu-simple")
        compose.onNodeWithContentDescription("Menü schliessen").performClick()
        compose.onNodeWithText("Prüfungen").assertIsSelected()
        assertEquals(0, calls)
    }
}
