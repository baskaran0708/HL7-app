package com.livemedica.helix.core.designsystem.token

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout tokens, ported 1:1 from the approved `theme.jsx`.
 *
 * Screens must never hard-code a dp value that exists here — that is what keeps a restyle a
 * token edit rather than a sweep through every composable.
 */

/** 4pt base spacing scale. */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 28.dp
    val xxxl: Dp = 40.dp
}

object Radius {
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    /** Effectively a stadium shape; matches the design's `999` pill radius. */
    val pill: Dp = 999.dp
}

/**
 * Motion. The design's principle is that glow and movement signal *interaction*, never decoration,
 * so these are short and mostly used for press and expand transitions.
 */
object Motion {
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val spring: Easing = CubicBezierEasing(0.22f, 1.2f, 0.36f, 1f)

    const val FAST_MS = 140
    const val BASE_MS = 220
    const val SLOW_MS = 340
    const val SPRING_MS = 420
}

/**
 * Elevation is a light-mode device only: the dark theme carries structure with surface ramp and
 * hairlines instead, exactly as the design specifies (`shadowSm/Md/Lg` are `none` in dark).
 */
object Elevation {
    val none: Dp = 0.dp
    val sm: Dp = 1.dp
    val md: Dp = 3.dp
    val lg: Dp = 8.dp
}

/** Fixed component dimensions that recur across screens. */
object Dimens {
    val touchTargetMin: Dp = 48.dp
    val bottomBarHeight: Dp = 60.dp
    val avatarSm: Dp = 28.dp
    val avatarMd: Dp = 36.dp
    val avatarLg: Dp = 56.dp
    val criticalAccentWidth: Dp = 3.dp
    val timelineAccentWidth: Dp = 2.dp
    val hairline: Dp = 1.dp
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 20.dp
    val iconLg: Dp = 24.dp
}
