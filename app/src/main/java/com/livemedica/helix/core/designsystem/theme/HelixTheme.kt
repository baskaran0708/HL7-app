package com.livemedica.helix.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.domain.model.ThemePreference

/**
 * Root theme.
 *
 * Helix layers its own token set *on top of* Material 3 rather than replacing it: M3 components
 * (sheets, ripples, text fields) keep working and stay accessible, while every Helix surface reads
 * from [HelixTheme.colors] / [HelixTheme.typography].
 *
 * Dynamic colour is deliberately not supported. Helix is a branded clinical product — the palette
 * must be identical on every device, and modality/priority hues carry meaning that a wallpaper-
 * derived scheme would destroy.
 */
object HelixTheme {
    val colors: HelixColors
        @Composable @ReadOnlyComposable get() = LocalHelixColors.current

    val typography: HelixTypography
        @Composable @ReadOnlyComposable get() = LocalHelixTypography.current
}

val LocalHelixColors = staticCompositionLocalOf { darkHelixColors() }
val LocalHelixTypography = staticCompositionLocalOf { helixTypography }

@Composable
fun HelixTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    palette: HelixPalette = HelixPalette.VIOLET,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val colors = if (darkTheme) darkHelixColors(palette) else lightHelixColors(palette)

    // Keep the system bar icon polarity in step with the theme. Without this the status bar
    // glyphs vanish against the light canvas, which is very visible in an edge-to-edge layout.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalHelixColors provides colors,
        LocalHelixTypography provides helixTypography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography = colors.toMaterialTypography(),
            shapes = helixShapes,
            content = content,
        )
    }
}

private val helixShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)

/**
 * Bridges the Helix tokens onto M3's scheme so built-in components inherit the clinical palette
 * instead of falling back to baseline purple.
 */
private fun HelixColors.toMaterialScheme() = if (isDark) {
    darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryDim,
        onPrimaryContainer = Color.White,
        secondary = primaryLift,
        onSecondary = Color.Black,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = elevated,
        onSurfaceVariant = textDim,
        surfaceContainer = elevated,
        surfaceContainerHigh = elevated,
        surfaceContainerLow = surface,
        surfaceContainerLowest = sunken,
        outline = line,
        outlineVariant = hairline,
        error = critical,
        onError = Color.Black,
        scrim = scrim,
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryLift,
        onPrimaryContainer = Color.White,
        secondary = primaryDim,
        onSecondary = Color.White,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = elevated,
        onSurfaceVariant = textDim,
        surfaceContainer = elevated,
        surfaceContainerHigh = elevated,
        surfaceContainerLow = surface,
        surfaceContainerLowest = sunken,
        outline = line,
        outlineVariant = hairline,
        error = critical,
        onError = Color.White,
        scrim = scrim,
    )
}

private fun HelixColors.toMaterialTypography() = androidx.compose.material3.Typography(
    displaySmall = helixTypography.display,
    headlineMedium = helixTypography.title,
    headlineSmall = helixTypography.headline,
    titleLarge = helixTypography.headline,
    titleMedium = helixTypography.label,
    bodyLarge = helixTypography.bodyLarge,
    bodyMedium = helixTypography.body,
    bodySmall = helixTypography.caption,
    labelLarge = helixTypography.label,
    labelMedium = helixTypography.caption,
    labelSmall = helixTypography.overline,
)

/** Convenience for the many places that need a mono style tinted to a signal colour. */
@Composable
@ReadOnlyComposable
fun monoTag(color: Color): TextStyle = HelixTheme.typography.monoSmall.copy(color = color)
