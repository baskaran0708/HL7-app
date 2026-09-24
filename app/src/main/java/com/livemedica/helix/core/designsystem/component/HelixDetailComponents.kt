package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * The scaffolding every list and detail screen is framed with: sections, grouping surfaces, rows
 * and the rules between them.
 *
 * The design groups by whitespace and typography first and only reaches for a surface where the
 * grouping genuinely helps — which is why [HelixSection] draws no box at all and [HelixFactCard] is
 * the deliberate exception.
 */

/**
 * The design's organising unit: an overline, an optional trailing action, and content.
 *
 * [contentSpacing] is the gap the section puts between its own children. It defaults to the value
 * menu- and settings-style lists want; content that manages its own internal rhythm (a card, a
 * timeline) is unaffected because it is a single child.
 */
@Composable
fun HelixSection(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    contentSpacing: Dp = Spacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HelixSectionHeader(
            title = title,
            modifier = Modifier.padding(bottom = Spacing.md),
            actionLabel = actionLabel,
            onActionClick = onActionClick,
        )
        Column(verticalArrangement = Arrangement.spacedBy(contentSpacing), content = content)
    }
}

/** Grouping surface for fact grids and stacked navigation rows. Content supplies its own padding. */
@Composable
fun HelixFactCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    HelixCard(
        modifier = modifier,
        contentPadding = 0.dp,
        content = content,
    )
}

/** Divider between rows inside a card, inset past the leading element as the design specifies. */
@Composable
fun HelixRowDivider(modifier: Modifier = Modifier, inset: Dp = Spacing.lg) {
    HelixHairline(modifier.padding(start = inset))
}

/**
 * A navigational row: leading icon, title, subtitle, chevron.
 *
 * Minimum height is the 48dp touch target even when the row carries a single line of text, and the
 * chevron appears only when there is somewhere to go — a row that looks tappable and does nothing
 * is worse than a row that looks inert.
 */
@Composable
fun HelixDetailRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.touchTargetMin)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: colors.textDim,
            modifier = Modifier.size(Dimens.iconMd),
        )
        Column(Modifier.weight(1f)) {
            Text(text = title, style = HelixTheme.typography.label, color = tint ?: colors.text)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = HelixTheme.typography.caption,
                    color = colors.textMute,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMute,
                modifier = Modifier.size(Dimens.iconSm),
            )
        }
    }
}
