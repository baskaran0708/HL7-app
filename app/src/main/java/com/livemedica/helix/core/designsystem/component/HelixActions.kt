package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * Buttons in the design's own idiom.
 *
 * Material's `Button` is not used here because the design's primary action is a *neutral* fill —
 * the text colour, not the brand colour — which M3 has no variant for. Keeping the brand violet out
 * of buttons is what leaves it free to mean "link" and "selected" everywhere else.
 */

/**
 * The primary action: ink on the page, inverted.
 *
 * [container] and [content] exist for the handful of actions that must carry a signal colour of
 * their own — a destructive confirm, for instance — without every other button in the app growing a
 * colour parameter it never uses.
 */
@Composable
fun HelixPrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color? = null,
    content: Color? = null,
) {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val background = (container ?: colors.text).copy(alpha = if (enabled) 1f else DISABLED_ALPHA)
    val foreground = content ?: colors.background

    Row(
        modifier = modifier
            .heightIn(min = Dimens.touchTargetMin)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
        Text(text = label, style = HelixTheme.typography.label, color = foreground)
    }
}

/** The secondary action: outlined, never competing with the primary CTA. */
@Composable
fun HelixSecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    Row(
        modifier = modifier
            .heightIn(min = Dimens.touchTargetMin)
            .clip(shape)
            .border(Dimens.hairline, colors.line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textDim,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
        Text(text = label, style = HelixTheme.typography.label, color = colors.text)
    }
}

/** Equal-width action pair — the design's `1fr 1fr` button grid. */
@Composable
fun HelixActionPair(
    modifier: Modifier = Modifier,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 1.dp),
    ) {
        Box(Modifier.weight(1f)) { start() }
        Box(Modifier.weight(1f)) { end() }
    }
}

private const val DISABLED_ALPHA = 0.4f
