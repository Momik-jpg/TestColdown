package com.andrin.examcountdown.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.TextUnit
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BluePrimaryLight,
    secondary = BlueSecondaryLight,
    tertiary = BlueAccentLight,
    background = BackgroundLight,
    surface = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark,
    secondary = BlueSecondaryDark,
    tertiary = BlueAccentDark,
    background = BackgroundDark,
    surface = SurfaceDark
)

private val LightAccessibleColors = lightColorScheme(
    primary = Color(0xFF003A75),
    secondary = Color(0xFF0059B8),
    tertiary = Color(0xFF006B87),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF)
)

private val DarkAccessibleColors = darkColorScheme(
    primary = Color(0xFF9DCCFF),
    secondary = Color(0xFF8EC2FF),
    tertiary = Color(0xFF8DE7FF),
    background = Color(0xFF000000),
    surface = Color(0xFF0D0D0D)
)

@Composable
fun ExamCountdownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accessibilityMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        accessibilityMode && darkTheme -> DarkAccessibleColors
        accessibilityMode && !darkTheme -> LightAccessibleColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    val typography = if (accessibilityMode) {
        scaledTypography(AppTypography, 1.12f)
    } else {
        AppTypography
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

private fun scaledTypography(base: Typography, factor: Float): Typography {
    return Typography(
        displayLarge = base.displayLarge.scaledBy(factor),
        displayMedium = base.displayMedium.scaledBy(factor),
        displaySmall = base.displaySmall.scaledBy(factor),
        headlineLarge = base.headlineLarge.scaledBy(factor),
        headlineMedium = base.headlineMedium.scaledBy(factor),
        headlineSmall = base.headlineSmall.scaledBy(factor),
        titleLarge = base.titleLarge.scaledBy(factor),
        titleMedium = base.titleMedium.scaledBy(factor),
        titleSmall = base.titleSmall.scaledBy(factor),
        bodyLarge = base.bodyLarge.scaledBy(factor),
        bodyMedium = base.bodyMedium.scaledBy(factor),
        bodySmall = base.bodySmall.scaledBy(factor),
        labelLarge = base.labelLarge.scaledBy(factor),
        labelMedium = base.labelMedium.scaledBy(factor),
        labelSmall = base.labelSmall.scaledBy(factor)
    )
}

private fun TextStyle.scaledBy(factor: Float): TextStyle {
    return copy(
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize * factor else fontSize,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight * factor else lineHeight,
        letterSpacing = if (letterSpacing != TextUnit.Unspecified) letterSpacing * factor else letterSpacing
    )
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
