package com.livemedica.helix.feature.patient.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.feature.patient.PatientEvent
import com.livemedica.helix.feature.patient.PatientEventKind

/**
 * One event on the patient's care timeline.
 *
 * The connector line is drawn per row rather than behind the whole column so the list can be a
 * plain `Column` of arbitrary length without the rail and the entries ever drifting apart.
 */
@Composable
fun PatientTimelineEvent(
    event: PatientEvent,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val accent = event.kind.accent()

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(RAIL_MARKER)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(accent.copy(alpha = MARKER_FILL_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = event.kind.icon(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(13.dp),
                )
            }
            if (!isLast) {
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .width(Dimens.hairline)
                        .height(RAIL_CONNECTOR)
                        .background(colors.hairline),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.label,
                    style = HelixTheme.typography.label,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = ClinicalFormat.dateTime(event.at),
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }
            Text(
                text = event.detail,
                style = HelixTheme.typography.caption,
                color = colors.textDim,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun PatientEventKind.accent(): Color {
    val colors = HelixTheme.colors
    return when (this) {
        PatientEventKind.ORDER -> colors.warning
        PatientEventKind.STUDY -> colors.info
        PatientEventKind.REPORT -> colors.success
        PatientEventKind.APPOINTMENT -> colors.primary
    }
}

private fun PatientEventKind.icon(): ImageVector = when (this) {
    PatientEventKind.ORDER -> Icons.AutoMirrored.Outlined.Assignment
    PatientEventKind.STUDY -> Icons.Outlined.Image
    PatientEventKind.REPORT -> Icons.Outlined.Description
    PatientEventKind.APPOINTMENT -> Icons.Outlined.Event
}

private val RAIL_MARKER = 26.dp
private val RAIL_CONNECTOR = 28.dp
private const val MARKER_FILL_ALPHA = 0.14f
