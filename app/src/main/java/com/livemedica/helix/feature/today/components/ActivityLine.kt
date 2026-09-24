package com.livemedica.helix.feature.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.NotificationCategory

/**
 * A single line of recent activity, drawn as a timeline: a category-tinted icon on a rail, with a
 * hairline connector running to the next entry.
 */
@Composable
fun ActivityLine(
    notification: HelixNotification,
    isLast: Boolean,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val (icon, tint) = notification.category.presentation()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(end = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(
            modifier = Modifier.width(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(tint.copy(alpha = if (colors.isDark) 0.09f else 0.07f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            }
            if (!isLast) {
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .width(1.dp)
                        .height(CONNECTOR_HEIGHT)
                        .background(colors.hairline),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp, bottom = if (isLast) Spacing.xs else Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = notification.title,
                    style = HelixTheme.typography.label,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = ClinicalFormat.time(notification.createdAt),
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                )
            }
            Text(
                text = notification.body,
                style = HelixTheme.typography.caption,
                color = colors.textDim,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun NotificationCategory.presentation(): Pair<ImageVector, Color> {
    val colors = HelixTheme.colors
    return when (this) {
        NotificationCategory.CRITICAL -> Icons.Outlined.WarningAmber to colors.critical
        NotificationCategory.RESULTS -> Icons.Outlined.Description to colors.success
        NotificationCategory.ORDERS -> Icons.AutoMirrored.Outlined.Assignment to colors.warning
        NotificationCategory.APPOINTMENTS -> Icons.Outlined.CalendarToday to colors.info
        NotificationCategory.SYSTEM -> Icons.Outlined.Dns to colors.textMute
    }
}

private val CONNECTOR_HEIGHT = 28.dp
