package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Elevation
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * The small shared vocabulary every Helix screen is assembled from.
 *
 * These exist so the design's rules are expressed once: hairline strokes carry structure in dark
 * mode, soft shadows carry it in light mode, signal colours appear as dots, rules and text but
 * never as filled surfaces, and every status is spelled out in words next to its colour.
 */

/** A 1px rule at the design's `hairline` opacity. */
@Composable
fun HelixHairline(modifier: Modifier = Modifier, color: Color = HelixTheme.colors.hairline) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.hairline)
            .background(color),
    )
}

/**
 * The standard card.
 *
 * Elevation is applied in light mode only — matching the design, where the dark theme builds depth
 * from the surface ramp and hairlines instead of shadows.
 */
@Composable
fun HelixCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(Radius.lg),
    elevation: Dp = Elevation.sm,
    contentPadding: Dp = Spacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = HelixTheme.colors
    val base = modifier
        .fillMaxWidth()
        .then(if (colors.isDark) Modifier else Modifier.shadow(elevation, shape, clip = false))
        .clip(shape)
        .background(colors.surface)
        .then(if (colors.isDark) Modifier.border(Dimens.hairline, colors.hairline, shape) else Modifier)

    Column(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        content = {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        },
    )
}

/** Wide-tracked, all-caps section marker — the design's signature header. */
@Composable
fun HelixOverline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HelixTheme.colors.textMute,
) {
    Text(
        text = text.uppercase(),
        style = HelixTheme.typography.overline,
        color = color,
        modifier = modifier,
    )
}

/**
 * Section header with an optional trailing action, e.g. `UP NEXT   Schedule ›`.
 */
@Composable
fun HelixSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HelixOverline(title)
        if (actionLabel != null && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onActionClick),
            ) {
                Text(
                    text = actionLabel,
                    style = HelixTheme.typography.label,
                    color = HelixTheme.colors.textDim,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = HelixTheme.colors.textMute,
                    modifier = Modifier.size(Dimens.iconSm),
                )
            }
        }
    }
}

/** A 6dp status dot. Always paired with its label by callers — never used alone. */
@Composable
fun HelixDot(color: Color, modifier: Modifier = Modifier, size: Dp = 6.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * A mono tag such as `MR`, `CT`, `STAT`. Renders text, so the meaning survives without colour.
 *
 * The label never wraps: a tag broken across two lines reads as two tags, and `STAT` split into
 * `ST`/`AT` reads as neither. Callers that cannot spare the width must hide the tag outright and
 * carry its meaning in the surrounding row's description instead.
 */
@Composable
fun HelixTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (filled) Modifier.background(color.copy(alpha = TAG_FILL_ALPHA))
                else Modifier.border(Dimens.hairline, color.copy(alpha = TAG_BORDER_ALPHA), shape)
            )
            .padding(horizontal = Spacing.sm - 2.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = HelixTheme.typography.monoSmall,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Dot + label pair used for statuses: colour and words always travel together. */
@Composable
fun HelixStatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 1.dp),
    ) {
        HelixDot(color)
        Text(text = label, style = HelixTheme.typography.caption, color = color)
    }
}

/** Icon + text row used for allergy and contrast warnings. */
@Composable
fun HelixInlineAlert(
    icon: ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(Dimens.iconSm),
        )
        Text(text = text, style = HelixTheme.typography.caption, color = color)
    }
}

/** Separates metadata fragments with the design's `·` bullet. */
@Composable
fun HelixMetaRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp),
        content = content,
    )
}

@Composable
fun HelixMetaText(text: String, modifier: Modifier = Modifier, color: Color = HelixTheme.colors.textMute) {
    Text(text = text, style = HelixTheme.typography.mono, color = color, modifier = modifier)
}

@Composable
fun HelixMetaSeparator() {
    Text(text = "·", style = HelixTheme.typography.mono, color = HelixTheme.colors.textMute)
}

private const val TAG_FILL_ALPHA = 0.13f
private const val TAG_BORDER_ALPHA = 0.45f
