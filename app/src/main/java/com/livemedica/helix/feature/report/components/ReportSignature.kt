package com.livemedica.helix.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixAvatar
import com.livemedica.helix.core.designsystem.component.HelixFactCard
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixMiniFieldPair
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.reportStatusMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.model.RadiologyReport

/** Attribution: who read the study, and under which credentials the report will be signed. */
@Composable
fun ReportRadiologistCard(
    report: RadiologyReport,
    doctor: Doctor,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    HelixFactCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            HelixAvatar(initials = doctor.initials, size = Dimens.avatarMd)
            Column(Modifier.weight(1f)) {
                Text(
                    text = report.radiologist,
                    style = HelixTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = colors.text,
                )
                Text(
                    text = doctor.specialty,
                    style = HelixTheme.typography.caption,
                    color = colors.textMute,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (doctor.licenceIdentifier.isNotBlank()) {
                Text(
                    text = doctor.licenceIdentifier,
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }
        }
    }
}

/**
 * Report status, spelled out as words plus the lifecycle timestamps behind them.
 *
 * Dictated / finalised / signed are shown as three separate facts because "unsigned" can mean two
 * very different things — not yet written, or written and waiting on the attending.
 */
@Composable
fun ReportStatusCard(
    report: RadiologyReport,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val meta = reportStatusMeta(report.status)
    HelixFactCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HelixStatusChip(label = meta.label, color = meta.color)
            Text(
                text = report.accessionNumber,
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
            )
        }
        HelixHairline()
        HelixMiniFieldPair(
            startLabel = "Dictated",
            startValue = ClinicalFormat.time(report.createdAt),
            endLabel = "Finalised",
            endValue = report.finalizedAt?.let { ClinicalFormat.time(it) } ?: "—",
        )
    }
}

/**
 * The signature block — the legal artefact of this screen.
 *
 * Signed and unsigned are two genuinely different statements, so they are two different blocks
 * rather than one block with a changing colour: signed states who signed and when, unsigned states
 * plainly that nothing has been signed yet.
 */
@Composable
fun ReportSignatureBlock(
    report: RadiologyReport,
    doctor: Doctor,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val signedAt = report.signedAt
    val shape = RoundedCornerShape(Radius.lg)

    if (report.isSigned) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.success.copy(alpha = SIGNED_FILL_ALPHA))
                .border(Dimens.hairline, colors.success.copy(alpha = SIGNED_STROKE_ALPHA), shape)
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md - 1.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(Dimens.iconMd),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Signed by ${report.radiologist}",
                    style = HelixTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = colors.text,
                )
                Text(
                    text = listOfNotNull(
                        signedAt?.let { ClinicalFormat.dateTime(it) },
                        doctor.licenceIdentifier.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .border(Dimens.hairline, colors.line, shape)
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md - 1.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = colors.textMute,
                modifier = Modifier.size(Dimens.iconMd),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Not signed",
                    style = HelixTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = colors.text,
                )
                Text(
                    text = "This report is not final until ${report.radiologist} signs it.",
                    style = HelixTheme.typography.caption,
                    color = colors.textMute,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

private const val SIGNED_FILL_ALPHA = 0.08f
private const val SIGNED_STROKE_ALPHA = 0.25f
