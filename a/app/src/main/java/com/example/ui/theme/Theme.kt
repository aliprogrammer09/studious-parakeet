package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.AppearanceConfig

data class DominaPalette(
    val accent: Color,
    val accentBright: Color,
    val accentSoft: Color,
    val accentGlow: Color,
    val bg: Color,
    val cardBg: Color,
    val cardInner: Color,
    val border: Color,
    val starFill: Color,
    val starStroke: Color,
    val intensity: Float
)

val LocalDominaPalette = staticCompositionLocalOf {
    DominaPalette(
        accent = RoyalVioletAccent,
        accentBright = RoyalVioletBright,
        accentSoft = RoyalVioletAccent.copy(alpha = 0.28f),
        accentGlow = RoyalVioletBright.copy(alpha = 0.52f),
        bg = ObsidianBg,
        cardBg = DominaCard,
        cardInner = DominaCardInner,
        border = Purple900.copy(alpha = 0.6f),
        starFill = Purple500,
        starStroke = Purple400,
        intensity = 0.95f
    )
}

fun buildDominaPalette(appearance: AppearanceConfig): DominaPalette {
    val intensity = (appearance.accentIntensity.coerceIn(20, 100)) / 100f
    val (accent, bright, defaultBg) = when (appearance.accentColor) {
        "amethyst" -> Triple(AmethystAccent, AmethystBright, AmethystBg)
        "indigo" -> Triple(IndigoAccent, IndigoBright, IndigoBg)
        "crimson" -> Triple(CrimsonAccent, CrimsonBright, CrimsonBg)
        "emerald" -> Triple(EmeraldAccent, EmeraldBright, EmeraldBg)
        "amber" -> Triple(AmberAccent, AmberBright, AmberBg)
        "fuchsia" -> Triple(FuchsiaAccent, FuchsiaBright, FuchsiaBg)
        else -> Triple(RoyalVioletAccent, RoyalVioletBright, ObsidianBg)
    }

    val bg = when (appearance.bgDarkness) {
        "pitch-black" -> PitchBlackBg
        "charcoal" -> CharcoalVioletBg
        else -> defaultBg
    }

    return DominaPalette(
        accent = accent,
        accentBright = bright,
        accentSoft = accent.copy(alpha = 0.30f * intensity),
        accentGlow = bright.copy(alpha = 0.55f * intensity),
        bg = bg,
        cardBg = DominaCard,
        cardInner = DominaCardInner,
        border = accent.copy(alpha = 0.35f * intensity),
        starFill = accent,
        starStroke = bright,
        intensity = intensity
    )
}

@Composable
fun DominaTheme(
    appearance: AppearanceConfig = AppearanceConfig(),
    content: @Composable () -> Unit
) {
    val palette = buildDominaPalette(appearance)
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        secondary = palette.accentBright,
        tertiary = Purple400,
        background = palette.bg,
        surface = DominaSurface,
        surfaceVariant = DominaCard,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Purple50,
        onSurface = Purple50,
        onSurfaceVariant = Purple200,
        outline = palette.border
    )

    CompositionLocalProvider(LocalDominaPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    DominaTheme(content = content)
}
