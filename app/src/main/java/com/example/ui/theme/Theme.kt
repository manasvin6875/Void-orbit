package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

private val NothingDarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    secondary = NothingYellow,
    onSecondary = NothingOledBlack,
    tertiary = VernierElectricBlue,
    background = NothingOledBlack,
    onBackground = Color.White,
    surface = NothingDeepCharcoal,
    onSurface = Color.White,
    surfaceVariant = NothingSlateLoupeDark,
    onSurfaceVariant = Color(0xFF9EA1AA),
    outline = NothingHairlineDark
)

private val NothingLightColorScheme = lightColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    secondary = NothingYellowLightMode,
    onSecondary = NothingOledBlack,
    tertiary = VernierDeepBlue,
    background = NothingAlabasterLight,
    onBackground = Color(0xFF111113),
    surface = NothingSurfaceLight,
    onSurface = Color(0xFF111113),
    surfaceVariant = NothingElevatedSheetLight,
    onSurfaceVariant = Color(0xFF5A5D65),
    outline = NothingHairlineLight
)

@Composable
fun NOrbitTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (useDark) DarkNothingPalette else LightNothingPalette
    val colorScheme = if (useDark) NothingDarkColorScheme else NothingLightColorScheme

    CompositionLocalProvider(LocalNothingPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
