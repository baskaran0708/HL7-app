package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Bottom navigation, built to the design rather than to the M3 default: a hairline top rule, a
 * compact 60dp bar, 20dp icons over a 10dp overline label, and the active tab carried by the brand
 * colour on both icon and label.
 *
 * Each item is a single `selectable` target sized to the 48dp minimum, and the icon + badge are
 * folded into one semantics node so a screen reader announces "Results, 4 unread" rather than
 * reading the badge as a stray number.
 */
@Composable
fun HelixBottomNavigation(
    destinations: List<TopLevelDestination>,
    selected: TopLevelDestination,
    badgeCounts: Map<TopLevelDestination, Int>,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        HelixHairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(Dimens.bottomBarHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                HelixBottomNavigationItem(
                    destination = destination,
                    selected = destination == selected,
                    badgeCount = badgeCounts[destination] ?: 0,
                    onSelect = { onSelect(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HelixBottomNavigationItem(
    destination: TopLevelDestination,
    selected: Boolean,
    badgeCount: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val tint = if (selected) colors.primary else colors.textMute
    val description = buildString {
        append(destination.label)
        if (badgeCount > 0) append(", $badgeCount unread")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.touchTargetMin)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
            )
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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

/** Small red count badge. Red here is a signal on a neutral bar, never a filled surface. */
@Composable
fun HelixCountBadge(count: Int, modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    Box(
        modifier = modifier
            .size(15.dp)
            .clip(CircleShape)
            .background(colors.critical),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > MAX_BADGE) "$MAX_BADGE+" else count.toString(),
            style = HelixTheme.typography.monoSmall,
            color = if (colors.isDark) colors.sunken else androidx.compose.ui.graphics.Color.White,
        )
    }
}

private const val MAX_BADGE = 9
