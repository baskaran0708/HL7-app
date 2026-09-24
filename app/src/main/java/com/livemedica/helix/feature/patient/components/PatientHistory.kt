package com.livemedica.helix.feature.patient.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.modalityColor
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.HistoryEntry
import com.livemedica.helix.domain.model.Modality

/**
 * Relevant history: the problem list and the prior imaging that frame the current read.
 */

/**
 * A prior study from the problem list.
 *
 * Carries its own modality-coloured rule instead of a block, because a prior is context for the
 * current read rather than something to navigate into — the seed holds it as history text, not as
 * an addressable study.
 */
@Composable
fun PriorStudyEntry(
    entry: HistoryEntry,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val modalityCode = entry.summary.substringBefore(" · ")
    val accent = Modality.entries
        .firstOrNull { it.code == modalityCode }
        ?.let { modalityColor(it) }
        ?: colors.textMute

    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            Modifier
                .width(Dimens.timelineAccentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Radius.sm))
                .background(accent.copy(alpha = 0.6f)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = ClinicalFormat.shortDate(entry.date),
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
            )
            Text(
                text = entry.summary.substringAfter(" · ", entry.summary),
                style = HelixTheme.typography.label,
                color = colors.text,
                modifier = Modifier.padding(top = Spacing.xs + 1.dp),
            )
            Text(
                text = entry.detail,
                style = HelixTheme.typography.caption,
                color = colors.textDim,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** A problem-list entry: a bullet, the condition, and when it was recorded. */
@Composable
fun HistoryBullet(
    entry: HistoryEntry,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 1.dp),
        verticalAlignment = Alignment.Top,
    ) {
        HelixDot(
            color = colors.textMute,
            size = 4.dp,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            text = entry.summary,
            style = HelixTheme.typography.body,
            color = colors.textDim,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ClinicalFormat.shortDate(entry.date),
            style = HelixTheme.typography.monoSmall,
            color = colors.textMute,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
