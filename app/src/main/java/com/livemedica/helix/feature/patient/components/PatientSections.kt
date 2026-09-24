package com.livemedica.helix.feature.patient.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.component.ModalityBlock
import com.livemedica.helix.core.designsystem.component.ModalityTag
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.appointmentStatusMeta
import com.livemedica.helix.core.designsystem.theme.orderStatusMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Study

/** The open order: what this patient is in the department for right now. */
@Composable
fun CurrentOrderRow(
    order: Order,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val statusMeta = orderStatusMeta(order.status)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.touchTargetMin)
            .clickable(onClick = onOpen)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ModalityBlock(order.modality, size = 38.dp)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 1.dp),
            ) {
                Text(
                    text = order.procedure,
                    style = HelixTheme.typography.label,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                HelixPriorityBadge(order.priority)
            }
            Row(
                modifier = Modifier.padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 1.dp),
            ) {
                HelixStatusChip(label = statusMeta.label, color = statusMeta.color)
                HelixDot(color = colors.textMute.copy(alpha = 0.6f), size = 3.dp)
                Text(
                    text = order.accessionNumber,
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconSm),
        )
    }
}

/** A study — current or historical — as a compact row. */
@Composable
fun StudyRow(
    study: Study,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.touchTargetMin)
            .clickable(onClick = onOpen)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ModalityBlock(study.modality, size = 38.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = study.description,
                style = HelixTheme.typography.label,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    study.performedAt?.let { ClinicalFormat.dateTime(it) } ?: "Not yet acquired",
                    study.accessionNumber,
                ).joinToString(" · "),
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconSm),
        )
    }
}

/** A scheduled or past appointment. */
@Composable
fun PatientAppointmentRow(
    appointment: Appointment,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val statusMeta = appointmentStatusMeta(appointment.status)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.touchTargetMin)
            .clickable(onClick = onOpen)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 1.dp),
            ) {
                ModalityTag(appointment.modality)
                HelixStatusChip(label = statusMeta.label, color = statusMeta.color)
            }
            Text(
                text = appointment.procedure,
                style = HelixTheme.typography.label,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.xs + 1.dp),
            )
            Text(
                text = "${ClinicalFormat.dateTime(appointment.start)} · ${appointment.room}",
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconSm),
        )
    }
}
