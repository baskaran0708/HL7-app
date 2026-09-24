package com.livemedica.helix.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixRowDivider
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * What the screen shows before anything has been typed.
 *
 * Deliberately not an empty state: "nothing here yet" is a different message from "nothing
 * matched", and conflating the two is the classic way a search screen feels broken on first open.
 */
@Composable
fun SearchIntroPanel(
    recentQueries: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        if (recentQueries.isNotEmpty()) {
            SearchGroup(label = "Recent searches", count = recentQueries.size) {
                recentQueries.forEachIndexed { index, recent ->
                    if (index > 0) HelixRowDivider()
                    RecentQueryRow(query = recent, onSelect = { onSelect(recent) })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Search across",
                style = HelixTheme.typography.overline,
                color = colors.textMute,
            )
            Text(
                text = "Patients · Studies · Orders · Reports · Appointments",
                style = HelixTheme.typography.caption,
                color = colors.textDim,
            )
            Text(
                text = "Names, MRNs, accession numbers, CPT codes and report impressions are all " +
                    "matched.",
                style = HelixTheme.typography.caption,
                color = colors.textMute,
            )
        }
    }
}

@Composable
private fun RecentQueryRow(query: String, onSelect: () -> Unit) {
    val colors = HelixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconSm),
        )
        Text(
            text = query,
            style = HelixTheme.typography.label,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.NorthEast,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(15.dp),
        )
    }
}
