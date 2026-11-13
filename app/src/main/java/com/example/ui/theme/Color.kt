package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Nothing OS & Vernier Scrubber Palette Tokens
val NothingOledBlack = Color(0xFF050506)
val NothingDeepCharcoal = Color(0xFF121215)
val NothingSlateLoupeDark = Color(0xFF1A1B1E)
val NothingWatermarkDark = Color(0xFF111113)
val NothingElevatedSheetDark = Color(0xFF222226)
val NothingDotDark = Color(0xFF232429)
val NothingHairlineDark = Color(0xFF28292E)

val NothingAlabasterLight = Color(0xFFF7F7F8)
val NothingSurfaceLight = Color(0xFFFFFFFF)
val NothingSlateLoupeLight = Color(0xFFFFFFFF)
val NothingWatermarkLight = Color(0xFFEDEDEF)
val NothingElevatedSheetLight = Color(0xFFEAEAEF)
val NothingDotLight = Color(0xFFD5D6DC)
val NothingHairlineLight = Color(0xFFDCDCE2)

// Signature Accents
val NothingRed = Color(0xFFD71921)
val NothingRedBright = Color(0xFFFF2832)
val NothingYellow = Color(0xFFF5C518)
val NothingYellowLightMode = Color(0xFFD9A700)
val VernierElectricBlue = Color(0xFF0080FF)
val VernierDeepBlue = Color(0xFF0A66C2)

// Semantic Timeline Colors (Inspired by Structured + Nothing Industrial Spin)
val TimelineCoral = Color(0xFFF27D72)
val TimelineCerulean = Color(0xFF5A9BD5)
val TimelineLeafGreen = Color(0xFF78B856)
val TimelineWarmAmber = Color(0xFFF3C13A)
val TimelineBurntOrange = Color(0xFFD9783B)
val TimelineMutedPassiveDark = Color(0xFF343436)
val TimelineMutedPassiveLight = Color(0xFFD8D8DE)

@Immutable
data class NothingPalette(
    val isDark: Boolean,
    val backgroundCanvas: Color,
    val dotMatrixGrid: Color,
    val surfaceElevated: Color,
    val loupeContainer: Color,
    val watermarkText: Color,
    val primaryInk: Color,
    val antiUiInk: Color,
    val mutedInk: Color,
    val activeLocalHourInk: Color,
    val hairlineBorder: Color,
    val nothingRed: Color,
    val nothingYellow: Color,
    val vernierElectric: Color,
    val passiveBlockSurface: Color
)

val DarkNothingPalette = NothingPalette(
    isDark = true,
    backgroundCanvas = NothingOledBlack,
    dotMatrixGrid = NothingDotDark,
    surfaceElevated = NothingElevatedSheetDark,
    loupeContainer = NothingSlateLoupeDark,
    watermarkText = NothingWatermarkDark,
    primaryInk = Color(0xFFFFFFFF),
    antiUiInk = Color(0xFFFFFFFF),
    mutedInk = Color(0xFF7A7D85),
    activeLocalHourInk = Color(0xFF7A7D85),
    hairlineBorder = NothingHairlineDark,
    nothingRed = NothingRed,
    nothingYellow = NothingYellow,
    vernierElectric = VernierElectricBlue,
    passiveBlockSurface = TimelineMutedPassiveDark
)

val LightNothingPalette = NothingPalette(
    isDark = false,
    backgroundCanvas = NothingAlabasterLight,
    dotMatrixGrid = NothingDotLight,
    surfaceElevated = NothingElevatedSheetLight,
    loupeContainer = NothingSlateLoupeLight,
    watermarkText = NothingWatermarkLight,
    primaryInk = Color(0xFF111113),
    antiUiInk = Color(0xFF111113),
    mutedInk = Color(0xFF6E7178),
    activeLocalHourInk = Color(0xFF6E7178),
    hairlineBorder = NothingHairlineLight,
    nothingRed = NothingRed,
    nothingYellow = NothingYellowLightMode,
    vernierElectric = VernierDeepBlue,
    passiveBlockSurface = TimelineMutedPassiveLight
)

val LocalNothingPalette = staticCompositionLocalOf { DarkNothingPalette }
