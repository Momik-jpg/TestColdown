package com.andrin.examcountdown.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AppPrimaryLight,
    onPrimary = AppOnPrimaryLight,
    primaryContainer = AppPrimaryContainerLight,
    onPrimaryContainer = AppOnPrimaryContainerLight,
    secondary = AppSecondaryLight,
    onSecondary = AppOnSecondaryLight,
    secondaryContainer = AppSecondaryContainerLight,
    onSecondaryContainer = AppOnSecondaryContainerLight,
    tertiary = AppAccentLight,
    onTertiary = AppOnAccentLight,
    tertiaryContainer = AppAccentContainerLight,
    onTertiaryContainer = AppOnAccentContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight
)

private val DarkColors = darkColorScheme(
    primary = AppPrimaryDark,
    onPrimary = AppOnPrimaryDark,
    primaryContainer = AppPrimaryContainerDark,
    onPrimaryContainer = AppOnPrimaryContainerDark,
    secondary = AppSecondaryDark,
    onSecondary = AppOnSecondaryDark,
    secondaryContainer = AppSecondaryContainerDark,
    onSecondaryContainer = AppOnSecondaryContainerDark,
    tertiary = AppAccentDark,
    onTertiary = AppOnAccentDark,
    tertiaryContainer = AppAccentContainerDark,
    onTertiaryContainer = AppOnAccentContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark
)

private val LightAccessibleColors = LightColors.copy(
    primary = Color(0xFF233E5A), onPrimary = Color.White,
    background = Color.White, surface = Color.White,
    onBackground = Color(0xFF111111), onSurface = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF37424F), outline = Color(0xFF465463)
)

private val DarkAccessibleColors = DarkColors.copy(
    background = Color.Black, surface = Color(0xFF101010),
    onBackground = Color(0xFFF5F7FA), onSurface = Color(0xFFF5F7FA),
    onSurfaceVariant = Color(0xFFDCE3EB), outline = Color(0xFFB7C4D3)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

/** Decorative artwork is suppressed by the stronger-contrast, simpler reading mode. */
internal val LocalDecorativeArtEnabled = staticCompositionLocalOf { true }

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

    CompositionLocalProvider(LocalDecorativeArtEnabled provides !accessibilityMode) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = AppShapes, content = content)
    }
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
