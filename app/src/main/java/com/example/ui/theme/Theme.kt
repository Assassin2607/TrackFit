package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    val surfaceElevated: Color = Slate750,
    val mutedText: Color = Slate400,
    val cardioAccent: Color = Cyan400,
    val volumeAccent: Color = Violet400
)

val LocalFitTrackColors = staticCompositionLocalOf { FitTrackExtendedColors() }

private val FitTrackDarkColorScheme = darkColorScheme(
    primary = Cobalt500,
    onPrimary = Slate50,
    primaryContainer = Cobalt900,
    onPrimaryContainer = Cobalt100,
    secondary = Emerald500,
    onSecondary = Slate950,
    secondaryContainer = Emerald900,
    onSecondaryContainer = Emerald100,
    tertiary = Cyan400,
    onTertiary = Slate950,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate750,
    onSurfaceVariant = Slate200,
    surfaceContainerLowest = Slate950,
    surfaceContainerLow = Slate900,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate750,
    surfaceContainerHighest = Slate700,
    outline = Slate700,
    outlineVariant = Slate600,
    error = Crimson500,
    onError = Slate50,
    errorContainer = Crimson900,
    onErrorContainer = Crimson100
)

private val FitTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FitTrackTheme(
    content: @Composable () -> Unit
) {
    val extendedColors = FitTrackExtendedColors()
    CompositionLocalProvider(LocalFitTrackColors provides extendedColors) {
        MaterialTheme(
            colorScheme = FitTrackDarkColorScheme,
            typography = FitTrackTypography,
            shapes = FitTrackShapes,
            content = content
        )
    }
}

object FitTrackThemeExt {
    val colors: FitTrackExtendedColors
        @Composable get() = LocalFitTrackColors.current
}
