package com.livemedica.helix.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.livemedica.helix.R

/**
 * Typography, ported 1:1 from the approved `theme.jsx`.
 *
 * Inter stands in for Geist (same grotesque proportions, and it ships as a static TTF we can bundle
 * so the app never waits on a font download). JetBrains Mono covers the design's mono role.
 *
 * Sizes stay in `sp` rather than `dp` so the whole scale still responds to the system font-size
 * setting — a hard requirement for clinicians reading dense worklists.
 */

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val MonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/**
 * Tabular figures. Applied to every time, count, MRN and accession number so digits sit in fixed
 * columns and a scrolling worklist does not visually jitter.
 */
const val TABULAR_NUMERALS = "tnum"

@Immutable
data class HelixTypography(
    val display: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    /** Small, wide-tracked, all-caps section headers — the design's signature section marker. */
    val overline: TextStyle,
    /** Mono variants for clinical identifiers. */
    val mono: TextStyle,
    val monoSmall: TextStyle,
    val monoLarge: TextStyle,
)

private fun sans(
    size: Double,
    weight: FontWeight,
    letterSpacingPx: Double,
    lineHeightMultiplier: Double,
) = TextStyle(
    fontFamily = InterFontFamily,
    fontSize = size.sp,
    fontWeight = weight,
    // theme.jsx expresses tracking in px at the given size; em keeps that ratio under font scaling.
    letterSpacing = (letterSpacingPx / size).em,
    lineHeight = (size * lineHeightMultiplier).sp,
    fontFeatureSettings = TABULAR_NUMERALS,
)

private fun mono(size: Double, weight: FontWeight, letterSpacingPx: Double) = TextStyle(
    fontFamily = MonoFontFamily,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = (letterSpacingPx / size).em,
    lineHeight = (size * 1.35).sp,
    fontFeatureSettings = TABULAR_NUMERALS,
)

val helixTypography = HelixTypography(
    display = sans(34.0, FontWeight.Bold, -1.2, 1.08),
    title = sans(22.0, FontWeight.Bold, -0.6, 1.15),
    headline = sans(17.0, FontWeight.Bold, -0.4, 1.25),
    bodyLarge = sans(15.0, FontWeight.Medium, -0.2, 1.45),
    body = sans(14.0, FontWeight.Medium, -0.1, 1.5),
    label = sans(12.5, FontWeight.SemiBold, -0.05, 1.35),
    caption = sans(11.5, FontWeight.Medium, 0.0, 1.4),
    overline = sans(10.0, FontWeight.Bold, 0.9, 1.2),
    mono = mono(12.5, FontWeight.Medium, 0.0),
    monoSmall = mono(10.5, FontWeight.Bold, 0.3),
    monoLarge = mono(22.0, FontWeight.Bold, -0.6),
)
