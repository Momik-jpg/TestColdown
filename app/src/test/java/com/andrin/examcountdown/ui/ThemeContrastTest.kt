package com.andrin.examcountdown.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** WCAG 2.2 text contrast for actual theme pairs; this is not a full accessibility certification. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ThemeContrastTest {
    @get:Rule val compose = createComposeRule()
    private fun check(dark: Boolean, accessible: Boolean) {
        var pairs: List<Pair<Color, Color>> = emptyList()
        var resolvedDark = !dark
        compose.setContent {
            ExamCountdownTheme(darkTheme = dark, accessibilityMode = accessible) {
                val c = MaterialTheme.colorScheme
                resolvedDark = isAppDarkTheme()
                pairs = listOf(
                    c.onSurface to c.surface, c.onSurfaceVariant to c.surface,
                    c.onSurfaceVariant to c.surfaceVariant, c.onBackground to c.background,
                    c.onPrimary to c.primary, c.onPrimaryContainer to c.primaryContainer,
                    c.onSecondaryContainer to c.secondaryContainer,
                    c.onErrorContainer to c.errorContainer, c.error to c.surface
                )
            }
        }
        compose.runOnIdle {
            assertEquals("Use the selected app theme, including explicitly forced dark mode", dark, resolvedDark)
            pairs.forEach { (foreground, background) ->
                val a = foreground.luminance() + 0.05f
                val b = background.luminance() + 0.05f
                val ratio = maxOf(a, b) / minOf(a, b)
                assertTrue("Text contrast $ratio is below 4.5:1 for $foreground / $background", ratio >= 4.5f)
            }
        }
    }
    @Test fun lightTextPairsMeetContrast() = check(false, false)
    @Test fun darkTextPairsMeetContrast() = check(true, false)
    @Test fun accessibleLightTextPairsMeetContrast() = check(false, true)
    @Test fun accessibleDarkTextPairsMeetContrast() = check(true, true)
}
