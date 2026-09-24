package com.livemedica.helix.feature.integration.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.ackStatusMeta
import com.livemedica.helix.core.designsystem.theme.interfaceStateMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Hl7Message
import com.livemedica.helix.domain.model.Hl7MessageType
import com.livemedica.helix.domain.model.InterfaceChannel

/**
 * The operational atoms of the HL7 screen.
 *
 * Everything identifier-shaped — message type, channel name, timestamps, counts — is mono with
 * tabular figures, so a column of them lines up and an interface engineer can scan for the odd one
 * out instead of reading every row.
 */

/** The colour thread for HL7 message types, mirroring the modality thread on clinical screens. */
@Composable
fun hl7TypeColor(type: Hl7MessageType): Color {
    val colors = HelixTheme.colors
    return when (type) {
        Hl7MessageType.ADT -> colors.info
        Hl7MessageType.SIU -> colors.primary
        Hl7MessageType.ORM -> colors.warning
        Hl7MessageType.ORU -> colors.success
    }
}

/** Square mono tag used as the leading element of a message row. */
@Composable
fun Hl7TypeBlock(type: Hl7MessageType, modifier: Modifier = Modifier) {
    val color = hl7TypeColor(type)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(Radius.md - 1.dp))
            .background(color.copy(alpha = if (HelixTheme.colors.isDark) 0.14f else 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = type.label, style = HelixTheme.typography.monoSmall, color = color)
    }
}

/**
 * A Mirth channel.
 *
 * Direction is inferred from the channel's naming convention — `-IN-` / `-OUT-` — because the
 * domain model does not carry it yet. Shown as an icon only, with the state always spelled out
 * next to its dot.
 */
@Composable
fun ChannelRow(channel: InterfaceChannel, modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    val meta = interfaceStateMeta(channel.state)
    val isInbound = channel.name.contains("-IN", ignoreCase = true)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = if (isInbound) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
            contentDescription = if (isInbound) "Inbound channel" else "Outbound channel",
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconSm),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = HelixTheme.typography.mono,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append("${channel.messagesToday} today")
                    channel.lastMessageAt?.let { append(" · last ${ClinicalFormat.time(it)}") }
                },
                style = HelixTheme.typography.caption,
                color = colors.textMute,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        HelixStatusChip(label = meta.label, color = meta.color)
    }
}

/** One HL7 message: type, what it was, when it arrived, and whether it was acknowledged. */
@Composable
fun Hl7MessageRow(
    message: Hl7Message,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = HelixTheme.colors
    val ack = ackStatusMeta(message.ackStatus)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Hl7TypeBlock(message.type)
        Column(Modifier.weight(1f)) {
            Text(
                text = message.reference,
                style = HelixTheme.typography.label,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.channel,
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(text = "·", style = HelixTheme.typography.monoSmall, color = colors.textMute)
                Text(
                    text = message.id,
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs + 1.dp),
        ) {
            Text(
                text = ClinicalFormat.time(message.receivedAt),
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
            )
            // The ACK state is the whole point of the row, so it carries its word, not just a dot.
            Text(text = ack.label, style = HelixTheme.typography.monoSmall, color = ack.color)
        }
    }
}
