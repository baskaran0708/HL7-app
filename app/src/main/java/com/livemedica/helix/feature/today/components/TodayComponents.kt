package com.livemedica.helix.feature.today.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.component.ModalityBlock
import com.livemedica.helix.core.designsystem.component.ModalityTag
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.appointmentStatusMeta
import com.livemedica.helix.core.designsystem.theme.modalityColor
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Motion
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport

/**
 * Press feedback used across Today's tappable surfaces: a 1.5% scale-down over 140ms.
 *
 * Small on purpose — the design treats movement as interaction feedback, not decoration, and an
 * exaggerated press animation is tiring on a screen a radiologist works all day.
 */
@Composable
private fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(Motion.FAST_MS, easing = Motion.standard),
        label = "pressScale",
    )
    return this.scale(scale)
}

/**
 * The critical-result card.
 *
 * Red appears only as the 3dp left rule, the status dot, and the CTA text — never as a fill. That
 * is the design's central rule: a critical finding must be unmissable without the screen shouting.
 */
@Composable
fun CriticalResultCard(
    report: RadiologyReport,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val headlineImpression = remember(report.impression) {
        report.impression.lineSequence().first().removePrefix("1.").trim()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .pressScale(interactionSource)
            .then(if (colors.isDark) Modifier else Modifier.shadow(3.dp, shape, clip = false))
            .clip(shape)
            .background(colors.surface)
            .then(if (colors.isDark) Modifier.border(Dimens.hairline, colors.hairline, shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onOpen)
            .semantics { contentDescription = "Critical result. ${report.patientName}. ${report.procedure}." },
    ) {
        // The signal rule.
        Box(
            Modifier
                .width(Dimens.criticalAccentWidth)
                .fillMaxHeight()
                .background(colors.critical),
        )
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.lg, top = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HelixStatusChip(label = "Critical", color = colors.critical)
                Box(Modifier.weight(1f))
                Text(
                    text = ClinicalFormat.time(report.createdAt),
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }

            Column(Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = 10.dp)) {
                Text(
                    text = report.patientName,
                    style = HelixTheme.typography.title.copy(fontSize = 20.sp),
                    color = colors.text,
                )
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    ModalityTag(report.modality)
                    Text(
                        text = report.procedure.substringBefore(" w/"),
                        style = HelixTheme.typography.label,
                        color = colors.textDim,
                    )
                }
            }

            Text(
                text = headlineImpression,
                style = HelixTheme.typography.body,
                color = colors.textDim,
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md),
            )

            HelixHairline(Modifier.padding(top = 14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Review report",
                    style = HelixTheme.typography.label.copy(fontSize = 13.5.sp),
                    color = colors.critical,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.critical,
                    modifier = Modifier.size(Dimens.iconSm),
                )
            }
        }
    }
}

/**
 * The "up next" appointment: a typographic row with a mono time column and a modality-coloured
 * accent rule, which turns red when the exam is STAT.
 */
@Composable
fun UpNextAppointment(
    appointment: Appointment,
    allergy: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val accent = if (appointment.priority == Priority.STAT) colors.critical else modalityColor(appointment.modality)
    val statusMeta = appointmentStatusMeta(appointment.status)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.widthIn(min = 52.dp).padding(top = 2.dp)) {
            Text(
                text = ClinicalFormat.time(appointment.start),
                style = HelixTheme.typography.monoLarge.copy(fontSize = 21.sp),
                color = colors.text,
            )
            Text(
                text = "${appointment.durationMinutes} min",
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
                modifier = Modifier.padding(top = 5.dp),
            )
        }

        Box(
            Modifier
                .width(Dimens.timelineAccentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(accent.copy(alpha = 0.85f)),
        )

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(bottom = 5.dp),
            ) {
                ModalityTag(appointment.modality)
                HelixPriorityBadge(appointment.priority)
                HelixStatusChip(label = statusMeta.label, color = statusMeta.color)
            }
            Text(
                text = appointment.patientName,
                style = HelixTheme.typography.headline.copy(fontSize = 18.sp),
                color = colors.text,
            )
            Text(
                text = appointment.procedure,
                style = HelixTheme.typography.label,
                color = colors.textDim,
                modifier = Modifier.padding(top = 3.dp),
            )
            Row(
                modifier = Modifier.padding(top = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                MutedMono(ClinicalFormat.ageSex(appointment.patientAge, appointment.patientSex))
                MetaDot()
                MutedMono(appointment.room)
                MetaDot()
                MutedMono(appointment.mrn)
            }
            if (allergy != null) {
                Row(
                    modifier = Modifier.padding(top = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(text = allergy, style = HelixTheme.typography.caption, color = colors.warning)
                }
            }
        }
    }
}

/** A report awaiting signature, as a compact list row. */
@Composable
fun PendingReportRow(
    report: RadiologyReport,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ModalityBlock(report.modality)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = report.patientName,
                    style = HelixTheme.typography.label.copy(fontSize = 14.sp),
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                HelixPriorityBadge(report.priority)
            }
            Text(
                text = report.procedure.substringBefore(" w/"),
                style = HelixTheme.typography.caption,
                color = colors.textDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = ClinicalFormat.time(report.finalizedAt ?: report.createdAt),
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
            )
        }
    }
}

@Composable
private fun MutedMono(text: String) {
    Text(text = text, style = HelixTheme.typography.monoSmall, color = HelixTheme.colors.textMute)
}

@Composable
private fun MetaDot() {
    HelixDot(color = HelixTheme.colors.textMute.copy(alpha = 0.6f), size = 3.dp)
}

