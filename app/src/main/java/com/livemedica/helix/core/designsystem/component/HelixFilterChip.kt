package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing

/**
 * The filter chip.
 *
 * [tone] is what lets a chip carry the modality or priority colour thread — a CT chip is violet and
 * a STAT chip is red whether or not it is the active filter — while selection is always spoken as
 * well as painted, because colour alone cannot tell a screen reader which filter is on.
 */
@Composable
fun HelixFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    tone: Color? = null,
    style: HelixFilterChipStyle = HelixFilterChipStyle.Solid,
) {
    when (style) {
        HelixFilterChipStyle.Solid -> SolidChip(label, selected, onClick, modifier, count, tone)
        HelixFilterChipStyle.Tinted -> TintedChip(label, selected, onClick, modifier, count, tone)
    }
}

/**
 * The two chip treatments the design actually uses.
 *
 * They are a parameter rather than one flattened chip because they say different things: a clinical
 * worklist fills the chip so the active filter reads as a hard commitment on the data below it,
 * while the system screens tint it so a filter sitting over a feed never shouts louder than the
 * feed.
 */
enum class HelixFilterChipStyle {
    /** Solid accent fill when selected. Worklist, Schedule, Results. */
    Solid,

    /** Tinted wash behind an accent outline when selected. Notifications, HL7 Integration. */
    Tinted,
}

/** Horizontally scrolling chip strip. Filters never wrap — the design keeps them on one line. */
@Composable
fun HelixFilterChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SolidChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    count: Int?,
    tone: Color?,
) {
    val colors = HelixTheme.colors
    val accent = tone ?: colors.text
    // White on a light-mode accent, near-black on a dark-mode one — both read from the ramp, so no
    // literal colour is introduced here.
    val onAccent = if (colors.isDark) colors.background else colors.surface
    val shape = RoundedCornerShape(Radius.md)
    val contentColor = if (selected) onAccent else colors.textDim

    Row(
        modifier = modifier
            // Paints at the design's height but reserves a full 48dp slot, so the filter row stays
            // visually light without becoming a precision-tapping exercise one-handed.
            .minimumInteractiveComponentSize()
            .clip(shape)
            .then(
                if (selected) Modifier.background(accent)
                else Modifier.border(Dimens.hairline, colors.line, shape),
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = Dimens.touchTargetMin - Spacing.sm)
            .padding(horizontal = Spacing.md + 1.dp, vertical = Spacing.sm)
            .semantics { contentDescription = chipDescription(label, count) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 2.dp),
    ) {
        ChipContent(label = label, count = count, labelColor = contentColor, countColor = contentColor.copy(alpha = COUNT_ALPHA))
    }
}

@Composable
private fun TintedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    count: Int?,
    tone: Color?,
) {
    val colors = HelixTheme.colors
    val accent = tone ?: colors.primary
    val shape = RoundedCornerShape(Radius.md - 2.dp)

    // The touch target is the full 48dp row height while the painted pill stays at the design's
    // size — the 4dp vertical inset is what separates "how big it looks" from "how big it is to a
    // thumb".
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .clip(shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = Spacing.xs)
            .semantics { contentDescription = chipDescription(label, count) },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .then(
                    if (selected) Modifier.background(accent.copy(alpha = SELECTED_FILL_ALPHA))
                    else Modifier,
                )
                .border(Dimens.hairline, if (selected) accent else colors.line, shape)
                .padding(horizontal = Spacing.md + 1.dp, vertical = Spacing.sm + 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 2.dp),
        ) {
            ChipContent(
                label = label,
                count = count,
                labelColor = if (selected) accent else colors.textDim,
                countColor = if (selected) accent else colors.textMute,
            )
        }
    }
}

@Composable
private fun RowScope.ChipContent(label: String, count: Int?, labelColor: Color, countColor: Color) {
    Text(text = label, style = HelixTheme.typography.label, color = labelColor)
    if (count != null) {
        Text(text = count.toString(), style = HelixTheme.typography.monoSmall, color = countColor)
    }
}

private fun chipDescription(label: String, count: Int?): String =
    if (count != null) "$label, $count" else label

private const val COUNT_ALPHA = 0.7f
private const val SELECTED_FILL_ALPHA = 0.13f
