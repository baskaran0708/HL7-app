package com.livemedica.helix.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour tokens, ported 1:1 from the approved `theme.jsx`.
 *
 * The design's governing rule is encoded here: *red is a signal, not a surface*. [critical] is only
 * ever used for text, a dot, or a 3dp accent rule — never as a card background — so a STAT item
 * reads instantly without the screen turning into an alarm wash.
 *
 * Composables consume these through [LocalHelixColors]; nothing reads a raw hex value.
 */
@Immutable
data class HelixColors(
    val isDark: Boolean,

    // Brand
    val primary: Color,
    val primaryDim: Color,
    val primaryLift: Color,

    // Semantic signals
    val success: Color,
    val warning: Color,
    val critical: Color,
    val info: Color,

    // Surface ramp. In light mode the canvas is deliberately a tinted cool grey so that
    // pure-white cards genuinely lift off it.
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val sunken: Color,

    // Strokes — hairline by design. Light mode leans on shadow for structure instead.
    val hairline: Color,
    val line: Color,
    val lineStrong: Color,

    // Text
    val text: Color,
    val textDim: Color,
    val textMute: Color,

    // Interaction
    val press: Color,
    val fill: Color,
    val scrim: Color,
)

/** Selectable brand palettes offered by the design. */
enum class HelixPalette(
    val label: String,
    val primary: Color,
    val primaryDim: Color,
    val primaryLift: Color,
) {
    VIOLET("Violet", Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFFA78BFA)),
    INDIGO("Indigo", Color(0xFF6366F1), Color(0xFF4338CA), Color(0xFF93A1FC)),
    TEAL("Teal", Color(0xFF14B8A6), Color(0xFF0F766E), Color(0xFF5EEAD4)),
    STEEL("Steel", Color(0xFF64748B), Color(0xFF475569), Color(0xFF94A3B8)),
}

fun darkHelixColors(palette: HelixPalette = HelixPalette.VIOLET) = HelixColors(
    isDark = true,
    primary = palette.primary,
    primaryDim = palette.primaryDim,
    primaryLift = palette.primaryLift,
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    critical = Color(0xFFF87171),
    info = Color(0xFF38BDF8),
    background = Color(0xFF0B0A12),
    surface = Color(0xFF13121D),
    elevated = Color(0xFF191725),
    sunken = Color(0xFF08070E),
    hairline = Color.White.copy(alpha = 0.055f),
    line = Color.White.copy(alpha = 0.09f),
    lineStrong = Color.White.copy(alpha = 0.16f),
    text = Color(0xFFF5F4F9),
    textDim = Color(0xFFA29EB8),
    textMute = Color(0xFF6B6783),
    press = Color.White.copy(alpha = 0.05f),
    fill = Color.White.copy(alpha = 0.045f),
    scrim = Color(0xFF04030A).copy(alpha = 0.72f),
)

fun lightHelixColors(palette: HelixPalette = HelixPalette.VIOLET) = HelixColors(
    isDark = false,
    primary = palette.primary,
    primaryDim = palette.primaryDim,
    primaryLift = palette.primaryLift,
    // Light-mode signals are darkened so small bold text clears 4.5:1 on white cards.
    success = Color(0xFF047857),
    warning = Color(0xFF8A5406),
    critical = Color(0xFFC11C1C),
    info = Color(0xFF0369A1),
    background = Color(0xFFEFEEF4),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFF7F6FA),
    sunken = Color(0xFFE6E4EE),
    hairline = Color(0xFF171430).copy(alpha = 0.055f),
    line = Color(0xFF171430).copy(alpha = 0.09f),
    lineStrong = Color(0xFF171430).copy(alpha = 0.16f),
    text = Color(0xFF16142B),
    textDim = Color(0xFF4E4B68),
    textMute = Color(0xFF63607A),
    press = Color(0xFF171430).copy(alpha = 0.045f),
    fill = Color(0xFF171430).copy(alpha = 0.035f),
    scrim = Color(0xFF171430).copy(alpha = 0.38f),
)
