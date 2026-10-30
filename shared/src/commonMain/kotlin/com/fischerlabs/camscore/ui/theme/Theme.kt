package com.fischerlabs.camscore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Custom diagnostic color palette ───────────────────────────────────────────

@Immutable
data class CamScoreColors(
    val canvas: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentBlue: Color,
    val statusPass: Color,
    val statusWarn: Color,
    val statusFail: Color,
    val gaugeTrack: Color,
    val gaugeFillGreen: Color,
    val gaugeFillAmber: Color,
    val gaugeFillRed: Color,
    val waveformGyro: Color,
    val waveformOpticalFlow: Color,
    val certificateHeader: Color,
    val infoBadge: Color,
    val goldRank: Color,
    val silverRank: Color,
    val bronzeRank: Color,
    val overlay: Color
)

val LightCamScoreColors = CamScoreColors(
    canvas = LightCanvas,
    cardBackground = LightCardBackground,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    accentBlue = AccentBlue,
    statusPass = StatusPass,
    statusWarn = StatusWarn,
    statusFail = StatusFail,
    gaugeTrack = GaugeTrack,
    gaugeFillGreen = GaugeFillGreen,
    gaugeFillAmber = GaugeFillAmber,
    gaugeFillRed = GaugeFillRed,
    waveformGyro = WaveformGyro,
    waveformOpticalFlow = WaveformOpticalFlow,
    certificateHeader = CertificateHeaderBlue,
    infoBadge = InfoBadgeBlue,
    goldRank = GoldRank,
    silverRank = SilverRank,
    bronzeRank = BronzeRank,
    overlay = SemiTransparentBlack
)

val DarkCamScoreColors = CamScoreColors(
    canvas = DarkCanvas,
    cardBackground = DarkCardBackground,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    accentBlue = AccentBlueDark,
    statusPass = StatusPass,
    statusWarn = StatusWarn,
    statusFail = StatusFail,
    gaugeTrack = GaugeTrackDark,
    gaugeFillGreen = GaugeFillGreen,
    gaugeFillAmber = GaugeFillAmber,
    gaugeFillRed = GaugeFillRed,
    waveformGyro = WaveformGyroDark,
    waveformOpticalFlow = WaveformOpticalFlowDark,
    certificateHeader = CertificateHeaderBlueDark,
    infoBadge = InfoBadgeBlueDark,
    goldRank = GoldRank,
    silverRank = SilverRank,
    bronzeRank = BronzeRank,
    overlay = TransparentOverlay
)

val LocalCamScoreColors = staticCompositionLocalOf { LightCamScoreColors }

// ── Material 3 Color Schemes ──────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    primaryContainer = LightPrimaryVariant,
    onPrimaryContainer = LightOnPrimary,
    surfaceContainerLow = LightCanvas,
    surfaceContainer = LightCardBackground,
    surfaceContainerHigh = LightSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    primaryContainer = DarkPrimaryVariant,
    onPrimaryContainer = DarkOnPrimary,
    surfaceContainerLow = DarkCanvas,
    surfaceContainer = DarkCardBackground,
    surfaceContainerHigh = DarkSurfaceVariant
)

// ── Theme Composable ──────────────────────────────────────────────────────────

@Composable
fun CamScoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val camScoreColors = if (darkTheme) DarkCamScoreColors else LightCamScoreColors

    CompositionLocalProvider(LocalCamScoreColors provides camScoreColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CamScoreTypography,
            shapes = CamScoreShapes,
            content = content
        )
    }
}

// ── Convenience Accessor ──────────────────────────────────────────────────────

object CamScoreTheme {
    val colors: CamScoreColors
        @Composable
        get() = LocalCamScoreColors.current
}
