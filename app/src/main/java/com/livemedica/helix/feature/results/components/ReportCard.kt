package com.livemedica.helix.feature.results.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.component.ModalityBlock
import com.livemedica.helix.core.designsystem.component.ModalityTag
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.reportStatusMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Elevation
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport

/**
 * The result card — the unit the Results list, the patient record and the study detail all share.
 *
 * It is built to be triaged, not read: signal row first (critical / priority / status / time), then
 * who and what, then the first line of the impression, then a footer that states the one thing the
 * clinician must do next. A critical report gains a 3dp left rule and nothing else — red stays a
 * signal, never a surface.
 */
@Composable
fun ReportCard(
    report: RadiologyReport,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    val statusMeta = reportStatusMeta(report.status)
    // Radiologists scan impressions by their numbered leading line; the rest is detail for the
    // detail screen, so the card shows only that first statement.
    val headline = remember(report.impression) {
        report.impression.lineSequence().first().removePrefix("1.").trim()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (colors.isDark) Modifier else Modifier.shadow(Elevation.md, shape, clip = false))
            .clip(shape)
            .background(colors.surface)
            .then(if (colors.isDark) Modifier.border(Dimens.hairline, colors.hairline, shape) else Modifier)
            .clickable(onClick = onOpen)
            .semantics {
                contentDescription = buildString {
                    if (report.isCritical) append("Critical result. ")
                    append("${report.patientName}. ${report.procedure}. ")
                    append("${report.priority.label} priority. ${statusMeta.label}.")
                }
            },
    ) {
        if (report.isCritical) {
            Box(
                Modifier
                    .width(Dimens.criticalAccentWidth)
                    .fillMaxHeight()
                    .background(colors.critical),
            )
        }
        Column(Modifier.weight(1f)) {
            SignalRow(report = report, statusLabel = statusMeta.label, statusColor = statusMeta.color)
            IdentityRow(report = report)
            ImpressionBlock(headline = headline)
            HelixHairline(Modifier.padding(top = Spacing.md))
            FooterRow(report = report)
        }
    }
}

@Composable
private fun SignalRow(
    report: RadiologyReport,
    statusLabel: String,
    statusColor: androidx.compose.ui.graphics.Color,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        when {
            report.isCritical -> HelixStatusChip(label = "Critical", color = colors.critical)
            report.priority != Priority.ROUTINE -> HelixPriorityBadge(report.priority)
            else -> ModalityTag(report.modality)
        }
        HelixStatusChip(label = statusLabel, color = statusColor)
        Box(Modifier.weight(1f))
        Text(
            text = ClinicalFormat.time(report.finalizedAt ?: report.createdAt),
            style = HelixTheme.typography.monoSmall,
            color = colors.textMute,
        )
    }
}

@Composable
private fun IdentityRow(report: RadiologyReport) {
    val colors = HelixTheme.colors
    Row(
        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ModalityBlock(report.modality, size = 42.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = report.patientName,
                style = HelixTheme.typography.headline,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 1.dp),
            ) {
                Text(
                    text = ClinicalFormat.ageSex(report.patientAge, report.patientSex),
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
                HelixDot(color = colors.textMute.copy(alpha = 0.6f), size = 3.dp)
                Text(
                    text = report.accessionNumber,
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }
            Text(
                text = report.procedure,
                style = HelixTheme.typography.label,
                color = colors.textDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.sm - 1.dp),
            )
        }
    }
}

@Composable
private fun ImpressionBlock(headline: String) {
    Column(Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md)) {
        HelixOverline("Impression")
        Text(
            text = headline,
            style = HelixTheme.typography.body,
            color = HelixTheme.colors.text,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs + 1.dp),
        )
    }
}

@Composable
private fun FooterRow(report: RadiologyReport) {
    val colors = HelixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 1.dp),
    ) {
        if (report.isSigned) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(Dimens.iconSm),
            )
            Text(
                text = report.signedAt?.let { "Signed ${ClinicalFormat.time(it)} · ${report.radiologist}" }
                    ?: "Signed · ${report.radiologist}",
                style = HelixTheme.typography.caption,
                color = colors.textDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = report.radiologist,
                style = HelixTheme.typography.caption,
                color = colors.textMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // The card states the next action rather than offering a button: signing is a
            // deliberate act that belongs on the report itself, where the prose can be read first.
            Text(
                text = "Sign report",
                style = HelixTheme.typography.label,
                color = if (report.isCritical) colors.critical else colors.primary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (report.isCritical) colors.critical else colors.primary,
                modifier = Modifier.size(Dimens.iconSm),
            )
        }
    }
}
