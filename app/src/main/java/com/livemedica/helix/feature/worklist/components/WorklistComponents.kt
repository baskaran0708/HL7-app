package com.livemedica.helix.feature.worklist.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livemedica.helix.core.designsystem.component.HelixClinicalIndication
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.component.ModalityBlock
import com.livemedica.helix.core.designsystem.component.ModalityTag
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.orderStatusMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Elevation
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Priority

/**
 * The order card — the densest surface in the app, and the reason the Worklist exists.
 *
 * Its information order is the order a radiologist triages in: how urgent, how far along, who, what
 * study, why (the clinical indication), and only then the administrative identifiers. The left rule
 * is the one place urgency is expressed as colour, and it is never a fill — red stays a signal.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrderCard(
    order: Order,
    onOpen: () -> Unit,
    onShowActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    val statusMeta = orderStatusMeta(order.status)
    val isRoutine = order.priority == Priority.ROUTINE
    val accent = when (order.priority) {
        Priority.STAT -> colors.critical
        Priority.URGENT -> colors.warning
        Priority.ROUTINE -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (colors.isDark) Modifier else Modifier.shadow(Elevation.md, shape, clip = false))
            .clip(shape)
            .background(colors.surface)
            .then(if (colors.isDark) Modifier.border(Dimens.hairline, colors.hairline, shape) else Modifier)
            .combinedClickable(
                onClick = onOpen,
                onClickLabel = "Open order for ${order.patientName}",
                onLongClick = onShowActions,
                onLongClickLabel = "Show actions",
            ),
    ) {
        if (accent != null) {
            Box(
                Modifier
                    .width(Dimens.criticalAccentWidth)
                    .fillMaxHeight()
                    .background(accent),
            )
        }

        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Routine orders lead with the modality; anything urgent leads with the word for it.
                if (isRoutine) ModalityTag(order.modality) else HelixPriorityBadge(order.priority)
                HelixStatusChip(label = statusMeta.label, color = statusMeta.color)
                Box(Modifier.weight(1f))
                Text(
                    text = ClinicalFormat.time(order.orderedAt),
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (!isRoutine) ModalityBlock(order.modality, size = 42.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = order.patientName,
                        style = HelixTheme.typography.headline.copy(fontSize = 16.sp),
                        color = colors.text,
                    )
                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            text = ClinicalFormat.ageSex(order.patientAge, order.patientSex),
                            style = HelixTheme.typography.monoSmall,
                            color = colors.textMute,
                        )
                        HelixDot(color = colors.textMute.copy(alpha = DOT_ALPHA), size = 3.dp)
                        Text(
                            text = order.mrn,
                            style = HelixTheme.typography.monoSmall,
                            color = colors.textMute,
                        )
                    }
                    Text(
                        text = order.procedure,
                        style = HelixTheme.typography.label,
                        color = colors.textDim,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }
            }

            HelixClinicalIndication(
                text = order.clinicalIndication,
                // The card has no section overline of its own, so the panel carries the label.
                label = "Clinical indication",
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp),
            )

            HelixHairline(Modifier.padding(top = Spacing.md))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = order.accessionNumber,
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                    maxLines = 1,
                )
                HelixDot(color = colors.textMute.copy(alpha = DOT_ALPHA), size = 3.dp)
                Text(
                    text = "CPT ${order.cptCode}",
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                    maxLines = 1,
                )
                Box(Modifier.weight(1f))
                Text(
                    text = "${order.orderingPhysician} · ${order.orderingLocation}",
                    style = HelixTheme.typography.caption,
                    color = colors.textMute,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = REFERRER_MAX_WIDTH),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textMute,
                    modifier = Modifier.size(Dimens.iconSm),
                )
            }
        }
    }
}

private val REFERRER_MAX_WIDTH = 108.dp
private const val DOT_ALPHA = 0.5f
