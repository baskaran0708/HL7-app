package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.TopLevelDestination

/**
 * Wide-screen counterpart to the bottom bar.
 *
 * Same destinations, same badge semantics, same brand-coloured selection — only the axis changes,
 * so a radiologist moving between phone and tablet does not have to relearn the app.
 */
@Composable
fun HelixNavigationRail(
    destinations: List<TopLevelDestination>,
    selected: TopLevelDestination,
    badgeCounts: Map<TopLevelDestination, Int>,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors

    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(RAIL_WIDTH)
                .fillMaxHeight()
                .background(colors.surface)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
        ) {
            destinations.forEach { destination ->
                HelixRailItem(
                    destination = destination,
                    selected = destination == selected,
                    badgeCount = badgeCounts[destination] ?: 0,
                    onSelect = { onSelect(destination) },
                )
            }
        }
        // Vertical hairline, mirroring the bottom bar's top rule.
        Box(
            Modifier
                .width(Dimens.hairline)
                .fillMaxHeight()
                .background(colors.hairline),
        )
    }
}

@Composable
private fun HelixRailItem(
    destination: TopLevelDestination,
    selected: Boolean,
    badgeCount: Int,
    onSelect: () -> Unit,
) {
    val colors = HelixTheme.colors
    val tint = if (selected) colors.primary else colors.textMute
    val description = buildString {
        append(destination.label)
        if (badgeCount > 0) append(", $badgeCount unread")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
            )
            .padding(vertical = Spacing.md)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(Dimens.iconMd),
            )
            if (badgeCount > 0) {
                HelixCountBadge(
                    count = badgeCount,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-5).dp),
                )
            }
        }
        Text(
            text = destination.label,
            style = HelixTheme.typography.caption,
            color = tint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

private val RAIL_WIDTH = 84.dp
