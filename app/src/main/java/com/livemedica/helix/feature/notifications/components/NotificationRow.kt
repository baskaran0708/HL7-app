package com.livemedica.helix.feature.notifications.components

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
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.NotificationCategory

/**
 * One notification.
 *
 * The category icon and its tint use the same mapping as the Today timeline, so "red triangle"
 * means critical everywhere in the app. Unread state is carried by a dot *and* by the row's
 * full-strength text — a read row dims — so it survives greyscale.
 */
@Composable
fun NotificationRow(
    notification: HelixNotification,
    onOpen: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val (icon, tint) = notification.category.presentation()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .alpha(if (notification.isRead) READ_ALPHA else 1f)
            .padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.md, bottom = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(Radius.md - 1.dp))
                .background(tint.copy(alpha = if (colors.isDark) 0.12f else 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        }

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
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
                if (!notification.isRead) {
                    HelixDot(
                        color = colors.primary,
                        size = 7.dp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Text(
                text = notification.body,
                style = HelixTheme.typography.caption,
                color = colors.textDim,
                modifier = Modifier.padding(top = 3.dp),
            )
            // Spelled out so the category survives colour-blindness and a greyscale screenshot.
            Text(
                text = notification.category.label,
                style = HelixTheme.typography.overline,
                color = tint,
                modifier = Modifier.padding(top = Spacing.sm - 2.dp),
            )
        }

        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Clear notification: ${notification.title}",
                tint = colors.textMute,
                modifier = Modifier.size(Dimens.iconSm),
            )
        }
    }
}

/** The app-wide category → icon + colour mapping, shared with the Today activity timeline. */
@Composable
fun NotificationCategory.presentation(): Pair<ImageVector, Color> {
    val colors = HelixTheme.colors
    return when (this) {
        NotificationCategory.CRITICAL -> Icons.Outlined.WarningAmber to colors.critical
        NotificationCategory.RESULTS -> Icons.Outlined.Description to colors.success
        NotificationCategory.ORDERS -> Icons.AutoMirrored.Outlined.Assignment to colors.warning
        NotificationCategory.APPOINTMENTS -> Icons.Outlined.CalendarToday to colors.info
        NotificationCategory.SYSTEM -> Icons.Outlined.Dns to colors.textMute
    }
}

private const val READ_ALPHA = 0.68f
