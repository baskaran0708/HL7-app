package com.livemedica.helix.feature.integration.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixCard
import com.livemedica.helix.core.designsystem.component.HelixMetric
import com.livemedica.helix.core.designsystem.component.HelixMetricPair
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.Hl7AckStatus
import com.livemedica.helix.domain.model.Hl7MessageType
import com.livemedica.helix.domain.model.IntegrationSnapshot
import java.util.Locale

/** Volume and acknowledgement rate — the two numbers an interface engineer checks first. */
@Composable
fun IntegrationHeadline(snapshot: IntegrationSnapshot, modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    val ackRate = String.format(Locale.US, "%.1f%%", snapshot.ackRatePercent)
    val unacknowledged = snapshot.recentMessages.count { it.ackStatus.isUnacknowledged() }

    HelixMetricPair(
        modifier = modifier,
        start = {
            HelixMetric(
                value = snapshot.messagesToday.toString(),
                label = "HL7 messages today",
                note = "${snapshot.channels.size} channels",
            )
        },
        end = {
            HelixMetric(
                value = ackRate,
                label = "Acknowledgement rate",
                note = if (unacknowledged > 0) "$unacknowledged not acknowledged" else "All acknowledged",
                noteColor = if (unacknowledged > 0) colors.warning else colors.success,
                // Below target the number itself has to read as a problem, not just its note.
                valueColor = if (snapshot.ackRatePercent < ACK_TARGET_PERCENT) colors.warning else colors.text,
            )
        },
    )
}

/**
 * Per-type volume: ADT, SIU, ORM, ORU.
 *
 * Always all four, even at zero — a missing ADT count is itself the diagnosis, and hiding the row
 * would hide the outage.
 */
@Composable
fun MessageTypeCounts(
    counts: Map<Hl7MessageType, Int>,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    HelixCard(modifier = modifier, contentPadding = Spacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Hl7MessageType.entries.forEach { type ->
                val count = counts[type] ?: 0
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics {
                            contentDescription = "${type.label}, $count messages"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = type.label,
                        style = HelixTheme.typography.monoSmall,
                        color = hl7TypeColor(type),
                    )
                    Text(
                        text = count.toString(),
                        style = HelixTheme.typography.monoLarge,
                        color = if (count > 0) colors.text else colors.textMute,
                        modifier = Modifier.padding(top = Spacing.sm - 2.dp),
                    )
                }
            }
        }
    }
}

private fun Hl7AckStatus.isUnacknowledged(): Boolean =
    this == Hl7AckStatus.PENDING || this == Hl7AckStatus.NAK

private const val ACK_TARGET_PERCENT = 99.0
