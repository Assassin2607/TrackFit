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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
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

val LocalFitTrackColors = staticCompositionLocalOf { FitTrackExtendedColors() }

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
    primary = WorkoutCrimson,
    onPrimary = Color.White,
    primaryContainer = WorkoutCrimsonBright,
    onPrimaryContainer = Color.White,
    secondary = WorkoutRoyalBlue,
    onSecondary = Color.White,
    secondaryContainer = WorkoutRoyalBlueBright,
    onSecondaryContainer = Color.White,
    tertiary = WorkoutRoyalBlueGlow,
    onTertiary = WorkoutBgDeep,
    background = WorkoutBgDeep,
    onBackground = WorkoutTextPrimary,
    surface = WorkoutSurfaceTop,
    onSurface = WorkoutTextPrimary,
    surfaceVariant = WorkoutSurfaceVariant,
    onSurfaceVariant = WorkoutTextSecondary,
    surfaceContainerLowest = WorkoutShadowLip,
    surfaceContainerLow = WorkoutBgDeep,
    surfaceContainer = WorkoutSurfaceTop,
    surfaceContainerHigh = WorkoutSurfaceVariant,
    surfaceContainerHighest = WorkoutRoyalBlue,
    outline = WorkoutOutline,
    outlineVariant = WorkoutRoyalBlue,
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

private fun targetColorScheme(section: FitTrackSection): ColorScheme = when (section) {
    FitTrackSection.HOME -> HomeColorScheme
    FitTrackSection.WORKOUT -> WorkoutColorScheme
    FitTrackSection.NUTRITION -> NutritionColorScheme
    FitTrackSection.PROGRESS -> ProgressColorScheme
    FitTrackSection.PROFILE -> ProfileColorScheme
}

private fun targetExtendedColors(section: FitTrackSection): FitTrackExtendedColors = when (section) {
    FitTrackSection.HOME -> FitTrackExtendedColors(
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

    FitTrackSection.WORKOUT -> FitTrackExtendedColors(
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

    FitTrackSection.NUTRITION -> FitTrackExtendedColors(
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

    FitTrackSection.PROGRESS -> FitTrackExtendedColors(
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

    FitTrackSection.PROFILE -> FitTrackExtendedColors(
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
}

@Composable
private fun animColor(target: Color, label: String): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = label
    )
    return color
}

private val FitTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FitTrackTheme(
    section: FitTrackSection = FitTrackSection.HOME,
    content: @Composable () -> Unit
) {
    val targetScheme = targetColorScheme(section)
    val targetExt = targetExtendedColors(section)

    val animatedScheme = targetScheme.copy(
        primary = animColor(targetScheme.primary, "primary"),
        onPrimary = animColor(targetScheme.onPrimary, "onPrimary"),
        primaryContainer = animColor(targetScheme.primaryContainer, "primaryContainer"),
        onPrimaryContainer = animColor(targetScheme.onPrimaryContainer, "onPrimaryContainer"),
        secondary = animColor(targetScheme.secondary, "secondary"),
        onSecondary = animColor(targetScheme.onSecondary, "onSecondary"),
        secondaryContainer = animColor(targetScheme.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = animColor(targetScheme.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = animColor(targetScheme.tertiary, "tertiary"),
        onTertiary = animColor(targetScheme.onTertiary, "onTertiary"),
        background = animColor(targetScheme.background, "background"),
        onBackground = animColor(targetScheme.onBackground, "onBackground"),
        surface = animColor(targetScheme.surface, "surface"),
        onSurface = animColor(targetScheme.onSurface, "onSurface"),
        surfaceVariant = animColor(targetScheme.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = animColor(targetScheme.onSurfaceVariant, "onSurfaceVariant"),
        surfaceContainerLowest = animColor(targetScheme.surfaceContainerLowest, "surfaceContainerLowest"),
        surfaceContainerLow = animColor(targetScheme.surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainer = animColor(targetScheme.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = animColor(targetScheme.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = animColor(targetScheme.surfaceContainerHighest, "surfaceContainerHighest"),
        outline = animColor(targetScheme.outline, "outline"),
        outlineVariant = animColor(targetScheme.outlineVariant, "outlineVariant"),
        error = animColor(targetScheme.error, "error"),
        onError = animColor(targetScheme.onError, "onError"),
        errorContainer = animColor(targetScheme.errorContainer, "errorContainer"),
        onErrorContainer = animColor(targetScheme.onErrorContainer, "onErrorContainer")
    )

    val animatedExt = FitTrackExtendedColors(
        success = animColor(targetExt.success, "success"),
        onSuccess = animColor(targetExt.onSuccess, "onSuccess"),
        successContainer = animColor(targetExt.successContainer, "successContainer"),
        onSuccessContainer = animColor(targetExt.onSuccessContainer, "onSuccessContainer"),
        warning = animColor(targetExt.warning, "warning"),
        onWarning = animColor(targetExt.onWarning, "onWarning"),
        warningContainer = animColor(targetExt.warningContainer, "warningContainer"),
        onWarningContainer = animColor(targetExt.onWarningContainer, "onWarningContainer"),
        surfaceElevated = animColor(targetExt.surfaceElevated, "surfaceElevated"),
        mutedText = animColor(targetExt.mutedText, "mutedText"),
        cardioAccent = animColor(targetExt.cardioAccent, "cardioAccent"),
        volumeAccent = animColor(targetExt.volumeAccent, "volumeAccent"),
        hydrationAccent = animColor(targetExt.hydrationAccent, "hydrationAccent"),
        cardSurfaceTop = animColor(targetExt.cardSurfaceTop, "cardSurfaceTop"),
        cardSurfaceBottom = animColor(targetExt.cardSurfaceBottom, "cardSurfaceBottom"),
        cardTopHighlight = animColor(targetExt.cardTopHighlight, "cardTopHighlight"),
        cardBottomShadow = animColor(targetExt.cardBottomShadow, "cardBottomShadow"),
        navBarContainer = animColor(targetExt.navBarContainer, "navBarContainer"),
        navIndicator = animColor(targetExt.navIndicator, "navIndicator"),
        navOnIndicator = animColor(targetExt.navOnIndicator, "navOnIndicator"),
        isLightTheme = targetExt.isLightTheme
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        val isLight = targetExt.isLightTheme
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = isLight
            insetsController.isAppearanceLightNavigationBars = isLight
        }
    }

    CompositionLocalProvider(LocalFitTrackColors provides animatedExt) {
        MaterialTheme(
            colorScheme = animatedScheme,
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
