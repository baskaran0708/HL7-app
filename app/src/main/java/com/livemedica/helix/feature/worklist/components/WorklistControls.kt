package com.livemedica.helix.feature.worklist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.feature.worklist.WorklistTab

/**
 * ORDERS / RESULTS.
 *
 * Built from the design's overline type rather than Material's tab styling so the two tabs read as
 * part of the same typographic system as every other section marker in the app. The search field and
 * the filter chips this screen also uses now come from `core/designsystem/component`.
 */
@Composable
fun WorklistTabRow(
    selected: WorklistTab,
    onSelect: (WorklistTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Row(modifier = modifier.fillMaxWidth()) {
        WorklistTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = { onSelect(tab) })
                    .heightIn(min = Dimens.touchTargetMin)
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HelixOverline(
                    text = tab.label,
                    color = if (isSelected) colors.text else colors.textMute,
                    modifier = Modifier.padding(bottom = Spacing.md),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(TAB_INDICATOR_HEIGHT)
                        .background(if (isSelected) colors.text else colors.hairline),
                )
            }
        }
    }
}

private val TAB_INDICATOR_HEIGHT = 2.dp
