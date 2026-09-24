package com.livemedica.helix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.modalityColor
import com.livemedica.helix.core.designsystem.theme.priorityMeta
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.Priority

/**
 * Clinical atoms shared by every list and detail screen.
 *
 * The modality "colour thread" lives here: a modality keeps the same hue wherever it appears, so a
 * clinician learns to spot MR or CT before reading the label.
 */

/** Small mono modality tag, e.g. `MR`. */
@Composable
fun ModalityTag(modality: Modality, modifier: Modifier = Modifier) {
    HelixTag(
        text = modality.code,
        color = modalityColor(modality),
        modifier = modifier.clearAndSetSemantics { contentDescription = modality.label },
    )
}

/** Larger filled modality block used as a list-row leading element. */
@Composable
fun ModalityBlock(
    modality: Modality,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val color = modalityColor(modality)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(Radius.md))
            .background(color.copy(alpha = 0.13f))
            .clearAndSetSemantics { contentDescription = modality.label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = modality.code, style = HelixTheme.typography.monoSmall, color = color)
    }
}

/**
 * Priority badge.
 *
 * STAT and Urgent are outlined tags carrying their own words; Routine is plain muted text because
 * the design treats "normal" as the absence of a signal rather than another badge competing for
 * attention.
 */
@Composable
fun HelixPriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier,
) {
    val meta = priorityMeta(priority)
    if (priority == Priority.ROUTINE) {
        Text(
            text = meta.label,
            style = HelixTheme.typography.caption,
            color = meta.color,
            modifier = modifier,
        )
    } else {
        HelixTag(
            text = meta.label.uppercase(),
            color = meta.color,
            filled = false,
            modifier = modifier,
        )
    }
}

/** Circular initials avatar. */
@Composable
fun HelixAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    onClick: (() -> Unit)? = null,
) {
    val colors = HelixTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.primary.copy(alpha = 0.16f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, style = HelixTheme.typography.monoSmall, color = colors.primary)
    }
}

/**
 * The paired workload metric from the Today screen: a large figure, its label, and a secondary note
 * that can be tinted when it carries urgency (e.g. "2 STAT" in red).
 */
@Composable
fun HelixMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    noteColor: Color? = null,
    valueColor: Color = HelixTheme.colors.text,
    onClick: (() -> Unit)? = null,
) {
    val colors = HelixTheme.colors
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Spacing.xs),
    ) {
        Text(text = value, style = HelixTheme.typography.display.copy(fontSize = 30.sp), color = valueColor)
        Text(
            text = label,
            style = HelixTheme.typography.caption,
            color = colors.textDim,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        if (note != null) {
            Text(
                text = note,
                style = HelixTheme.typography.caption,
                color = noteColor ?: colors.textMute,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * The reason for the exam, as it arrived on the order.
 *
 * It is the sentence that decides how a study is read, so it is never just another line of grey
 * body text. [label] is what decides how hard it is pushed: a detail screen already sits it under
 * its own section overline and wants the plain reading line, while a worklist card has no such
 * frame and gets the recessed panel with the label inside it.
 */
@Composable
fun HelixClinicalIndication(
    text: String,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val colors = HelixTheme.colors
    if (label == null) {
        Text(
            text = text,
            style = HelixTheme.typography.bodyLarge,
            color = colors.textDim,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(if (colors.isDark) colors.elevated else colors.sunken)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
    ) {
        HelixOverline(text = label, modifier = Modifier.padding(bottom = Spacing.xs))
        Text(text = text, style = HelixTheme.typography.caption, color = colors.textDim)
    }
}

/** Two metrics side by side — the design's `StatPair`. */
@Composable
fun HelixMetricPair(
    modifier: Modifier = Modifier,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Box(Modifier.weight(1f)) { start() }
        Box(Modifier.weight(1f)) { end() }
    }
}
