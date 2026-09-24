package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * Menu and settings rows.
 *
 * Each one binds its label to its control through a single `clickable` or `toggleable` node, so a
 * screen reader announces "Critical alerts, on" rather than reading the label and the control as two
 * unrelated elements.
 */

/**
 * A menu row: icon tile, title, optional status-coloured subtitle, optional count, chevron.
 *
 * Distinct from [HelixDetailRow] on purpose — a menu row is a destination in a list of destinations,
 * so its icon sits on a tile that gives the column a spine to scan down, while a detail row is one
 * fact among many inside a card and keeps its icon bare.
 *
 * [unavailableNote] is how a destination that does not exist yet stays honest: the row dims, loses
 * its chevron and its click target, and says so in words, rather than looking tappable and doing
 * nothing when pressed.
 */
@Composable
fun HelixMenuRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    statusColor: Color? = null,
    count: Int = 0,
    onClick: (() -> Unit)? = null,
    unavailableNote: String? = null,
) {
    val colors = HelixTheme.colors
    val isEnabled = onClick != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isEnabled) Modifier.clickable(onClick = onClick) else Modifier.semantics { disabled() })
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .alpha(if (isEnabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md + 1.dp),
    ) {
        Box(
            modifier = Modifier
                .size(ICON_TILE_SIZE)
                .clip(RoundedCornerShape(Radius.md - 2.dp))
                .background(colors.elevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textDim,
                modifier = Modifier.size(Dimens.iconSm),
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = HelixTheme.typography.label,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 2.dp),
                ) {
                    if (statusColor != null) HelixDot(color = statusColor, size = 5.dp)
                    Text(
                        text = subtitle,
                        style = HelixTheme.typography.caption,
                        color = statusColor ?: colors.textMute,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (count > 0) HelixCountPill(count)

        when {
            unavailableNote != null -> Text(
                text = unavailableNote,
                style = HelixTheme.typography.caption,
                color = colors.textMute,
            )

            isEnabled -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMute,
                modifier = Modifier.size(Dimens.iconSm),
            )
        }
    }
}

/**
 * Label + switch row.
 *
 * [lockedNote] covers settings that are shown but cannot be changed — patient-safety alerts that are
 * always on, for instance. The row says why instead of silently ignoring the tap.
 */
@Composable
fun HelixToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    lockedNote: String? = null,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .alpha(if (enabled) 1f else TOGGLE_DISABLED_ALPHA)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = HelixTheme.typography.label, color = colors.text)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = HelixTheme.typography.caption,
                    color = colors.textMute,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (lockedNote != null) {
                Text(
                    text = lockedNote,
                    style = HelixTheme.typography.caption,
                    color = colors.warning,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        // Null handler: the whole row owns the toggle semantics, so the switch must not add its own.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Neutral count pill. Unlike the tab-bar badge this is not a signal, so it stays uncoloured. */
@Composable
fun HelixCountPill(count: Int, modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = Spacing.xl)
            .clip(RoundedCornerShape(Radius.sm + 2.dp))
            .background(colors.elevated)
            .padding(horizontal = Spacing.sm - 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = count.toString(), style = HelixTheme.typography.monoSmall, color = colors.textDim)
    }
}

private val ICON_TILE_SIZE = 34.dp
private const val DISABLED_ALPHA = 0.5f
private const val TOGGLE_DISABLED_ALPHA = 0.55f
