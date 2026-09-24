package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Elevation
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * The design's segmented control: a sunken track with the active segment lifted onto a surface.
 *
 * Generic over the option type so callers pass their own enum rather than stringly-typed indices —
 * an unselectable state is then impossible to express, which is the point.
 *
 * The lift is a light-mode shadow only. In dark mode the surface ramp already separates the active
 * segment from its track, and a shadow there would only muddy it.
 */
@Composable
fun <T> HelixSegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val trackShape = RoundedCornerShape(Radius.md)
    val segmentShape = RoundedCornerShape(Radius.sm)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(if (colors.isDark) colors.elevated else colors.sunken)
            .border(Dimens.hairline, colors.hairline, trackShape)
            .padding(Spacing.xs - 1.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs - 1.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected && !colors.isDark) {
                            Modifier.shadow(Elevation.sm, segmentShape, clip = false)
                        } else {
                            Modifier
                        },
                    )
                    .clip(segmentShape)
                    .then(if (isSelected) Modifier.background(colors.surface) else Modifier)
                    // One selectable node per segment, so the whole control reads as a single
                    // choice rather than as a row of unrelated buttons.
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .heightIn(min = Dimens.touchTargetMin - Spacing.sm)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = HelixTheme.typography.label,
                    color = if (isSelected) colors.text else colors.textDim,
                )
            }
        }
    }
}
