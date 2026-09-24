package com.livemedica.helix.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixFactCard
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixMiniFieldPair
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.component.ModalityBlock
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.reportStatusMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport

/**
 * The report's non-prose sections.
 *
 * Order on screen is clinical, not decorative: who this is about, what was done, why it was asked
 * for — before a single word of findings. A radiologist signing under their own licence reads the
 * context first every time.
 */

/** Patient identity plus the report's signal row, with a route into the full patient record. */
@Composable
fun ReportPatientHeader(
    report: RadiologyReport,
    onOpenPatient: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val statusMeta = reportStatusMeta(report.status)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.sm + 2.dp),
        ) {
            if (report.isCritical) {
                HelixStatusChip(label = "Critical finding", color = colors.critical)
            } else if (report.priority != Priority.ROUTINE) {
                HelixPriorityBadge(report.priority)
            }
            HelixStatusChip(label = statusMeta.label, color = statusMeta.color)
        }

        Text(text = report.patientName, style = HelixTheme.typography.title, color = colors.text)

        Row(
            modifier = Modifier.padding(top = Spacing.xs + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = ClinicalFormat.ageSex(report.patientAge, report.patientSex),
                style = HelixTheme.typography.mono,
                color = colors.textDim,
            )
            HelixDot(color = colors.textMute, size = 3.dp)
            Text(text = report.mrn, style = HelixTheme.typography.mono, color = colors.textDim)
        }

        Row(
            modifier = Modifier
                .padding(top = Spacing.sm)
                .heightIn(min = Dimens.touchTargetMin)
                .clickable(onClick = onOpenPatient),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Patient record", style = HelixTheme.typography.label, color = colors.primary)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(Dimens.iconSm),
            )
        }
    }
}

/** Study information: what was performed, under which accession and CPT, when, and for whom. */
@Composable
fun ReportStudyCard(
    report: RadiologyReport,
    order: Order?,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    HelixFactCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ModalityBlock(report.modality, size = 42.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = report.procedure,
                    style = HelixTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = colors.text,
                )
                Row(
                    modifier = Modifier.padding(top = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 1.dp),
                ) {
                    Text(
                        text = order?.cptCode?.let { "CPT $it" } ?: report.modality.label,
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
            }
        }
        HelixHairline()
        HelixMiniFieldPair(
            startLabel = "Reported",
            startValue = ClinicalFormat.time(report.finalizedAt ?: report.createdAt),
            endLabel = "Referring",
            endValue = order?.orderingPhysician ?: "—",
            endMono = false,
        )
    }
}

/**
 * Critical-result protocol notice.
 *
 * A tinted panel rather than a solid red block: the design's rule is that red carries meaning as a
 * stroke and as text, and a full red surface here would drown the findings it sits above.
 */
@Composable
fun CriticalProtocolCallout(modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(Radius.lg - 2.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.critical.copy(alpha = CALLOUT_FILL_ALPHA))
            .border(Dimens.hairline, colors.critical.copy(alpha = CALLOUT_STROKE_ALPHA), shape)
            .padding(horizontal = Spacing.lg, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = colors.critical,
                modifier = Modifier.size(Dimens.iconSm),
            )
            Text(
                text = "Critical result protocol",
                style = HelixTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                color = colors.critical,
            )
        }
        Text(
            text = "The ordering clinician is notified on sign-off and read-back is recorded " +
                "against this accession.",
            style = HelixTheme.typography.caption,
            color = colors.textDim,
            modifier = Modifier.padding(top = Spacing.sm + 1.dp),
        )
    }
}

/**
 * One block of report prose.
 *
 * Findings and impressions run to several hundred words, so line height is generous and the body
 * face is the medium-weight sans rather than mono. [emphasis] lifts the impression — the single
 * paragraph a referring clinician will actually act on — above the surrounding narrative.
 */
@Composable
fun ReportProseSection(
    label: String,
    body: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
) {
    val colors = HelixTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        HelixOverline(label)
        Text(
            text = body,
            style = if (emphasis) HelixTheme.typography.bodyLarge else HelixTheme.typography.body,
            color = if (emphasis) colors.text else colors.textDim,
            modifier = Modifier.padding(top = Spacing.sm - 1.dp),
        )
    }
}

private const val CALLOUT_FILL_ALPHA = 0.06f
private const val CALLOUT_STROKE_ALPHA = 0.22f
