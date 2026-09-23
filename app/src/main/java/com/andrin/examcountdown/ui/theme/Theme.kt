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
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = BlueOnPrimaryContainerLight,
    secondary = BlueSecondaryLight,
    onSecondary = BlueOnSecondaryLight,
    secondaryContainer = BlueSecondaryContainerLight,
    onSecondaryContainer = BlueOnSecondaryContainerLight,
    tertiary = BlueAccentLight,
    onTertiary = BlueOnAccentLight,
    tertiaryContainer = BlueAccentContainerLight,
    onTertiaryContainer = BlueOnAccentContainerLight,
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
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    secondary = BlueSecondaryDark,
    onSecondary = BlueOnSecondaryDark,
    secondaryContainer = BlueSecondaryContainerDark,
    onSecondaryContainer = BlueOnSecondaryContainerDark,
    tertiary = BlueAccentDark,
    onTertiary = BlueOnAccentDark,
    tertiaryContainer = BlueAccentContainerDark,
    onTertiaryContainer = BlueOnAccentContainerDark,
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

private val LightAccessibleColors = lightColorScheme(
    primary = Color(0xFF003A75),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD2E4FF),
    onPrimaryContainer = Color(0xFF001C3D),
    secondary = Color(0xFF0059B8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E7FF),
    onSecondaryContainer = Color(0xFF002A58),
    tertiary = Color(0xFF3F66A8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9E5FF),
    onTertiaryContainer = Color(0xFF0F2B58),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE8EEF8),
    onSurfaceVariant = Color(0xFF213042),
    outline = Color(0xFF425D7B),
    outlineVariant = Color(0xFF9FB2CA)
)

private val DarkAccessibleColors = darkColorScheme(
    primary = Color(0xFF9DCCFF),
    onPrimary = Color(0xFF002655),
    primaryContainer = Color(0xFF00408C),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFF8EC2FF),
    onSecondary = Color(0xFF002855),
    secondaryContainer = Color(0xFF00428A),
    onSecondaryContainer = Color(0xFFD8E7FF),
    tertiary = Color(0xFFCAD8FF),
    onTertiary = Color(0xFF18345A),
    tertiaryContainer = Color(0xFF35507A),
    onTertiaryContainer = Color(0xFFE1E8FF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFEFEFEF),
    surface = Color(0xFF0D0D0D),
    onSurface = Color(0xFFEFEFEF),
    surfaceVariant = Color(0xFF1D242E),
    onSurfaceVariant = Color(0xFFC7D2DF),
    outline = Color(0xFFA8BACF),
    outlineVariant = Color(0xFF5D6F84)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
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
        shapes = AppShapes,
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
