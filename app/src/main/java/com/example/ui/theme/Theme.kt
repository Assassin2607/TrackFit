package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class FitTrackSection {
    HOME,
    WORKOUT,
    NUTRITION,
    PROGRESS,
    PROFILE
}

@Immutable
data class FitTrackExtendedColors(
    val success: Color = Emerald500,
    val onSuccess: Color = Slate950,
    val successContainer: Color = Emerald900,
    val onSuccessContainer: Color = Emerald100,
    val warning: Color = Amber500,
    val onWarning: Color = Slate950,
    val warningContainer: Color = Amber900,
    val onWarningContainer: Color = Amber100,
    val surfaceElevated: Color = HomeSurfaceVariant,
    val mutedText: Color = HomeTextMuted,
    val cardioAccent: Color = Cyan400,
    val volumeAccent: Color = Violet400,
    val hydrationAccent: Color = Cyan400,
    val cardSurfaceTop: Color = HomeIndigoTop,
    val cardSurfaceBottom: Color = HomeIndigoBottom,
    val cardTopHighlight: Color = HomeBevelHighlight,
    val cardBottomShadow: Color = HomeShadowLip,
    val navBarContainer: Color = HomeIndigo,
    val navIndicator: Color = HomeTerracotta,
    val navOnIndicator: Color = Color.White,
    val isLightTheme: Boolean = false
)

/**
 * Lightweight animated shell colors for the outer Scaffold background and
 * NavigationBar / NavigationRail so switching tabs smoothly blends chrome
 * colors at 120Hz without forcing 45+ theme token recompositions across screen content.
 */
@Immutable
data class FitTrackShellPalette(
    val background: Color,
    val onSurface: Color,
    val mutedText: Color,
    val navBarContainer: Color,
    val navIndicator: Color,
    val navOnIndicator: Color,
    val navTopHighlight: Color,
    val isLightTheme: Boolean
)

val LocalFitTrackColors = compositionLocalOf { HomeExtendedColors }

private val HomeColorScheme = darkColorScheme(
    primary = HomeTerracotta,
    onPrimary = Color.White,
    primaryContainer = HomeTerracottaContainer,
    onPrimaryContainer = HomeOnTerracottaContainer,
    secondary = HomeTerracottaLight,
    onSecondary = HomeBgDeep,
    secondaryContainer = HomeSurfaceVariant,
    onSecondaryContainer = HomeTextPrimary,
    tertiary = Cyan400,
    onTertiary = HomeBgDeep,
    background = HomeBgDeep,
    onBackground = HomeTextPrimary,
    surface = HomeIndigo,
    onSurface = HomeTextPrimary,
    surfaceVariant = HomeSurfaceVariant,
    onSurfaceVariant = HomeTextSecondary,
    surfaceContainerLowest = HomeShadowLip,
    surfaceContainerLow = HomeBgDeep,
    surfaceContainer = HomeIndigo,
    surfaceContainerHigh = HomeSurfaceVariant,
    surfaceContainerHighest = HomeOutline,
    outline = HomeOutline,
    outlineVariant = HomeIndigoTop,
    error = Crimson500,
    onError = Color.White,
    errorContainer = Crimson900,
    onErrorContainer = Crimson100
)

private val WorkoutColorScheme = darkColorScheme(
    primary = WorkoutBeigeAccent,
    onPrimary = WorkoutBgDeep,
    primaryContainer = WorkoutCrimson,
    onPrimaryContainer = WorkoutCreamText,
    secondary = WorkoutRoyalBlueGlow,
    onSecondary = WorkoutBgDeep,
    secondaryContainer = WorkoutRoyalBlue,
    onSecondaryContainer = WorkoutCreamText,
    tertiary = WorkoutCrimsonGlow,
    onTertiary = WorkoutCreamText,
    background = WorkoutBgDeep,
    onBackground = WorkoutCreamText,
    surface = WorkoutSurfaceTop,
    onSurface = WorkoutCreamText,
    surfaceVariant = WorkoutSurfaceVariant,
    onSurfaceVariant = WorkoutTextSecondary,
    surfaceContainerLowest = WorkoutShadowLip,
    surfaceContainerLow = WorkoutBgDeep,
    surfaceContainer = WorkoutSurfaceTop,
    surfaceContainerHigh = Color(0xFF1D1224),
    surfaceContainerHighest = WorkoutRoyalBlue,
    outline = WorkoutOutline,
    outlineVariant = WorkoutRoyalBlueBright,
    error = WorkoutCrimsonGlow,
    onError = Color.White,
    errorContainer = WorkoutCrimson,
    onErrorContainer = Crimson100
)

private val NutritionColorScheme = lightColorScheme(
    primary = NutritionForestPrimary,
    onPrimary = Color.White,
    primaryContainer = NutritionLime,
    onPrimaryContainer = NutritionForestDark,
    secondary = NutritionAqua,
    onSecondary = Color.White,
    secondaryContainer = NutritionAquaContainer,
    onSecondaryContainer = NutritionOnAquaContainer,
    tertiary = NutritionLimeDeep,
    onTertiary = NutritionForestDark,
    background = NutritionSageBg,
    onBackground = NutritionForestDark,
    surface = NutritionSurface,
    onSurface = NutritionForestDark,
    surfaceVariant = NutritionLime,
    onSurfaceVariant = NutritionForestSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = NutritionSurface,
    surfaceContainer = NutritionSageBg,
    surfaceContainerHigh = NutritionLime,
    surfaceContainerHighest = NutritionLimeDeep,
    outline = NutritionOutline,
    outlineVariant = NutritionShadowLip,
    error = Crimson700,
    onError = Color.White,
    errorContainer = Crimson100,
    onErrorContainer = Crimson900
)

private val ProgressColorScheme = darkColorScheme(
    primary = ProgressStoneLight,
    onPrimary = ProgressBgDeep,
    primaryContainer = ProgressSurfaceVariant,
    onPrimaryContainer = ProgressTextPrimary,
    secondary = ProgressStone,
    onSecondary = ProgressBgDeep,
    secondaryContainer = ProgressObsidian,
    onSecondaryContainer = ProgressTextSecondary,
    tertiary = Cyan400,
    onTertiary = ProgressBgDeep,
    background = ProgressBgDeep,
    onBackground = ProgressTextPrimary,
    surface = ProgressObsidian,
    onSurface = ProgressTextPrimary,
    surfaceVariant = ProgressSurfaceVariant,
    onSurfaceVariant = ProgressTextSecondary,
    surfaceContainerLowest = ProgressShadowLip,
    surfaceContainerLow = ProgressBgDeep,
    surfaceContainer = ProgressObsidian,
    surfaceContainerHigh = ProgressSurfaceVariant,
    surfaceContainerHighest = ProgressOutline,
    outline = ProgressOutline,
    outlineVariant = ProgressStone,
    error = Crimson500,
    onError = Color.White,
    errorContainer = Crimson900,
    onErrorContainer = Crimson100
)

private val ProfileColorScheme = darkColorScheme(
    primary = ProfileGold,
    onPrimary = ProgressBgDeep,
    primaryContainer = ProfileGoldContainer,
    onPrimaryContainer = ProfileOnGoldContainer,
    secondary = ProfileSeafoam,
    onSecondary = ProfileBgDeep,
    secondaryContainer = ProfileSurfaceVariant,
    onSecondaryContainer = ProfileTextPrimary,
    tertiary = ProfileGold,
    onTertiary = ProfileBgDeep,
    background = ProfileBgDeep,
    onBackground = ProfileTextPrimary,
    surface = ProfileTeal,
    onSurface = ProfileTextPrimary,
    surfaceVariant = ProfileSurfaceVariant,
    onSurfaceVariant = ProfileTextSecondary,
    surfaceContainerLowest = ProfileShadowLip,
    surfaceContainerLow = ProfileBgDeep,
    surfaceContainer = ProfileTeal,
    surfaceContainerHigh = ProfileSurfaceVariant,
    surfaceContainerHighest = ProfileOutline,
    outline = ProfileOutline,
    outlineVariant = ProfileGold,
    error = Crimson500,
    onError = Color.White,
    errorContainer = Crimson900,
    onErrorContainer = Crimson100
)

private val HomeExtendedColors = FitTrackExtendedColors(
    success = Emerald500,
    onSuccess = HomeBgDeep,
    successContainer = Color(0xFF153B38),
    onSuccessContainer = Emerald100,
    warning = HomeTerracottaLight,
    onWarning = HomeBgDeep,
    warningContainer = HomeTerracottaContainer,
    onWarningContainer = HomeOnTerracottaContainer,
    surfaceElevated = HomeSurfaceVariant,
    mutedText = HomeTextMuted,
    cardioAccent = Cyan400,
    volumeAccent = Color(0xFFA5B4FC),
    hydrationAccent = Color(0xFF38BDF8),
    cardSurfaceTop = HomeIndigoTop,
    cardSurfaceBottom = HomeIndigoBottom,
    cardTopHighlight = HomeBevelHighlight,
    cardBottomShadow = HomeShadowLip,
    navBarContainer = Color(0xFF1C2048),
    navIndicator = HomeTerracotta,
    navOnIndicator = Color.White,
    isLightTheme = false
)

private val WorkoutExtendedColors = FitTrackExtendedColors(
    success = Emerald500,
    onSuccess = WorkoutBgDeep,
    successContainer = Emerald900,
    onSuccessContainer = Emerald100,
    warning = Amber500,
    onWarning = WorkoutBgDeep,
    warningContainer = Amber900,
    onWarningContainer = Amber100,
    surfaceElevated = WorkoutSurfaceVariant,
    mutedText = WorkoutTextMuted,
    cardioAccent = WorkoutRoyalBlueGlow,
    volumeAccent = WorkoutCrimsonGlow,
    hydrationAccent = WorkoutRoyalBlueGlow,
    cardSurfaceTop = WorkoutSurfaceTop,
    cardSurfaceBottom = WorkoutSurfaceBottom,
    cardTopHighlight = WorkoutBevelHighlight,
    cardBottomShadow = WorkoutShadowLip,
    navBarContainer = Color(0xFF1D0E17),
    navIndicator = WorkoutCrimson,
    navOnIndicator = Color.White,
    isLightTheme = false
)

private val NutritionExtendedColors = FitTrackExtendedColors(
    success = Emerald700,
    onSuccess = Color.White,
    successContainer = NutritionLime,
    onSuccessContainer = NutritionForestDark,
    warning = Amber700,
    onWarning = Color.White,
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
    surfaceElevated = NutritionLime,
    mutedText = NutritionForestMuted,
    cardioAccent = NutritionAqua,
    volumeAccent = NutritionForestPrimary,
    hydrationAccent = NutritionAqua,
    cardSurfaceTop = NutritionCardTop,
    cardSurfaceBottom = NutritionCardBottom,
    cardTopHighlight = NutritionBevelHighlight,
    cardBottomShadow = NutritionShadowLip,
    navBarContainer = Color(0xFFDAE8CC),
    navIndicator = NutritionLime,
    navOnIndicator = NutritionForestDark,
    isLightTheme = true
)

private val ProgressExtendedColors = FitTrackExtendedColors(
    success = Color(0xFF4ADE80),
    onSuccess = ProgressBgDeep,
    successContainer = Color(0xFF1F3526),
    onSuccessContainer = Emerald100,
    warning = Color(0xFFFBBF24),
    onWarning = ProgressBgDeep,
    warningContainer = Color(0xFF3E3018),
    onWarningContainer = Amber100,
    surfaceElevated = ProgressSurfaceVariant,
    mutedText = ProgressTextMuted,
    cardioAccent = Color(0xFF38BDF8),
    volumeAccent = Color(0xFFD6B98C),
    hydrationAccent = Color(0xFF38BDF8),
    cardSurfaceTop = ProgressCardTop,
    cardSurfaceBottom = ProgressCardBottom,
    cardTopHighlight = ProgressBevelHighlight,
    cardBottomShadow = ProgressShadowLip,
    navBarContainer = Color(0xFF211F1D),
    navIndicator = ProgressStone,
    navOnIndicator = ProgressBgDeep,
    isLightTheme = false
)

private val ProfileExtendedColors = FitTrackExtendedColors(
    success = ProfileSeafoam,
    onSuccess = ProfileBgDeep,
    successContainer = Color(0xFF0E3D3A),
    onSuccessContainer = ProfileTextPrimary,
    warning = ProfileGold,
    onWarning = ProfileBgDeep,
    warningContainer = ProfileGoldContainer,
    onWarningContainer = ProfileOnGoldContainer,
    surfaceElevated = ProfileSurfaceVariant,
    mutedText = ProfileTextMuted,
    cardioAccent = ProfileSeafoam,
    volumeAccent = ProfileGold,
    hydrationAccent = ProfileSeafoam,
    cardSurfaceTop = ProfileCardTop,
    cardSurfaceBottom = ProfileCardBottom,
    cardTopHighlight = ProfileBevelHighlight,
    cardBottomShadow = ProfileShadowLip,
    navBarContainer = Color(0xFF0E3B38),
    navIndicator = ProfileGold,
    navOnIndicator = ProgressBgDeep,
    isLightTheme = false
)

private fun targetColorScheme(section: FitTrackSection): ColorScheme = when (section) {
    FitTrackSection.HOME -> HomeColorScheme
    FitTrackSection.WORKOUT -> WorkoutColorScheme
    FitTrackSection.NUTRITION -> NutritionColorScheme
    FitTrackSection.PROGRESS -> ProgressColorScheme
    FitTrackSection.PROFILE -> ProfileColorScheme
}

private fun targetExtendedColors(section: FitTrackSection): FitTrackExtendedColors = when (section) {
    FitTrackSection.HOME -> HomeExtendedColors
    FitTrackSection.WORKOUT -> WorkoutExtendedColors
    FitTrackSection.NUTRITION -> NutritionExtendedColors
    FitTrackSection.PROGRESS -> ProgressExtendedColors
    FitTrackSection.PROFILE -> ProfileExtendedColors
}

private val ShellAnimSpec = tween<Color>(durationMillis = 220, easing = FastOutSlowInEasing)

@Composable
fun rememberAnimatedShellPalette(section: FitTrackSection): FitTrackShellPalette {
    val scheme = targetColorScheme(section)
    val ext = targetExtendedColors(section)

    val background by animateColorAsState(scheme.background, ShellAnimSpec, label = "shellBg")
    val onSurface by animateColorAsState(scheme.onSurface, ShellAnimSpec, label = "shellOnSurface")
    val mutedText by animateColorAsState(ext.mutedText, ShellAnimSpec, label = "shellMuted")
    val navBarContainer by animateColorAsState(ext.navBarContainer, ShellAnimSpec, label = "shellNavBg")
    val navIndicator by animateColorAsState(ext.navIndicator, ShellAnimSpec, label = "shellNavInd")
    val navOnIndicator by animateColorAsState(ext.navOnIndicator, ShellAnimSpec, label = "shellNavOnInd")
    val navTopHighlight by animateColorAsState(ext.cardTopHighlight, ShellAnimSpec, label = "shellNavHi")

    ApplySystemBarTheme(isLightTheme = ext.isLightTheme)

    return FitTrackShellPalette(
        background = background,
        onSurface = onSurface,
        mutedText = mutedText,
        navBarContainer = navBarContainer,
        navIndicator = navIndicator,
        navOnIndicator = navOnIndicator,
        navTopHighlight = navTopHighlight,
        isLightTheme = ext.isLightTheme
    )
}

@Composable
private fun ApplySystemBarTheme(isLightTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(view, isLightTheme) {
            val window = view.context.findActivity()?.window ?: return@LaunchedEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = isLightTheme
            insetsController.isAppearanceLightNavigationBars = isLightTheme
        }
    }
}

private val FitTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

/**
 * High-performance 120Hz theme provider. Uses cached immutable ColorScheme &
 * FitTrackExtendedColors instances so tab pages render with zero per-frame
 * recomposition storms during cross-tab transitions.
 */
@Composable
fun FitTrackTheme(
    section: FitTrackSection = FitTrackSection.HOME,
    applySystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = targetColorScheme(section)
    val ext = targetExtendedColors(section)

    if (applySystemBars) {
        ApplySystemBarTheme(isLightTheme = ext.isLightTheme)
    }

    CompositionLocalProvider(LocalFitTrackColors provides ext) {
        MaterialTheme(
            colorScheme = scheme,
            typography = FitTrackTypography,
            shapes = FitTrackShapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object FitTrackThemeExt {
    val colors: FitTrackExtendedColors
        @Composable get() = LocalFitTrackColors.current
}
