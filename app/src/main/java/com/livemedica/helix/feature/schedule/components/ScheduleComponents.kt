package com.livemedica.helix.feature.schedule.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixCard
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.ModalityBlock
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.feature.schedule.ScheduleGroup
import com.livemedica.helix.feature.schedule.ScheduleGroupSection

/**
 * The List and Week views — the two reading modes that do not depend on pixel-accurate time.
 *
 * List is also the accessibility fallback for the timeline: every appointment appears as a
 * full-height row with a real tap target and a spoken summary.
 */

@Composable
fun ScheduleDayList(
    groups: List<ScheduleGroupSection>,
    onOpen: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Spacing.xl,
            end = Spacing.xl,
            top = Spacing.xs,
            bottom = Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl - Spacing.xs),
    ) {
        groups.forEach { section ->
            item(key = "header-${section.group.name}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(bottom = Spacing.md),
                ) {
                    HelixDot(color = section.group.accent(), size = 6.dp)
                    HelixOverline(text = "${section.group.label} · ${section.appointments.size}")
                }
            }
            item(key = "group-${section.group.name}") {
                HelixCard(contentPadding = 0.dp) {
                    section.appointments.forEachIndexed { index, appointment ->
                        if (index > 0) HelixHairline(Modifier.padding(start = LIST_RULE_INSET))
                        ScheduleAppointmentRow(
                            appointment = appointment,
                            onOpen = { onOpen(appointment) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A scheduled exam as a list row: when, what, who, where.
 *
 * The mono time column is fixed-width so times align down the list — a schedule that jitters
 * horizontally is measurably slower to scan.
 */
@Composable
fun ScheduleAppointmentRow(
    appointment: Appointment,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .heightIn(min = Dimens.touchTargetMin)
            .padding(horizontal = Spacing.lg, vertical = 14.dp)
            .semantics {
                contentDescription = "${ClinicalFormat.time(appointment.start)}, ${appointment.patientName}, " +
                    "${appointment.modality.label}, ${appointment.procedure}, " +
                    "${appointment.priority.label}, ${appointment.status.label}, room ${appointment.room}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.width(TIME_COLUMN_WIDTH)) {
            Text(
                text = ClinicalFormat.time(appointment.start),
                style = HelixTheme.typography.monoLarge.copy(fontSize = HelixTheme.typography.body.fontSize),
                color = colors.text,
            )
            Text(
                text = "${appointment.durationMinutes}m",
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }

        ModalityBlock(appointment.modality)

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = appointment.patientName,
                    style = HelixTheme.typography.label.copy(fontSize = HelixTheme.typography.body.fontSize),
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                HelixPriorityBadge(appointment.priority)
            }
            Text(
                text = appointment.procedure.substringBefore(" w/"),
                style = HelixTheme.typography.caption,
                color = colors.textDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = appointment.room,
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
                HelixDot(color = colors.textMute.copy(alpha = DOT_ALPHA), size = 3.dp)
                Text(
                    text = ClinicalFormat.ageSex(appointment.patientAge, appointment.patientSex),
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
                HelixDot(color = colors.textMute.copy(alpha = DOT_ALPHA), size = 3.dp)
                Text(
                    text = appointment.mrn,
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconMd),
        )
    }
}

@Composable
private fun ScheduleGroup.accent(): Color {
    val colors = HelixTheme.colors
    return when (this) {
        ScheduleGroup.IN_PROGRESS -> colors.info
        ScheduleGroup.UPCOMING -> colors.primary
        ScheduleGroup.COMPLETED -> colors.success
        ScheduleGroup.MISSED -> colors.textMute
    }
}

private val TIME_COLUMN_WIDTH = Spacing.xxl + Spacing.lg
private val LIST_RULE_INSET = Spacing.xxxl + Spacing.xxl + Spacing.xs

private const val DOT_ALPHA = 0.5f
