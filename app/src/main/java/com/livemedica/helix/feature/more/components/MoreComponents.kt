package com.livemedica.helix.feature.more.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixAvatar
import com.livemedica.helix.core.designsystem.component.HelixCard
import com.livemedica.helix.core.designsystem.component.HelixMenuRow
import com.livemedica.helix.core.designsystem.component.HelixRowDivider
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.Doctor

/** The signed-in clinician, as the menu's first card and the entry point to the profile. */
@Composable
fun MoreIdentityCard(
    doctor: Doctor,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    HelixCard(modifier = modifier, onClick = onOpenProfile, contentPadding = Spacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            HelixAvatar(initials = doctor.initials, size = 50.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = doctor.displayName,
                    style = HelixTheme.typography.headline,
                    color = colors.text,
                )
                Text(
                    text = "${doctor.specialty} · ${doctor.facility}",
                    style = HelixTheme.typography.caption,
                    color = colors.textDim,
                    modifier = Modifier.padding(top = 3.dp),
                )
                HelixStatusChip(
                    label = if (doctor.isOnCall) "On call" else "Off call",
                    color = if (doctor.isOnCall) colors.success else colors.textMute,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMute,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
    }
}

/** One menu group rendered as a single card of hairline-separated rows. */
@Composable
fun MoreMenuGroupCard(group: MoreMenuGroup, modifier: Modifier = Modifier) {
    HelixSection(title = group.title, modifier = modifier) {
        HelixCard(contentPadding = 0.dp) {
            group.items.forEachIndexed { index, item ->
                if (index > 0) HelixRowDivider(inset = 63.dp)
                HelixMenuRow(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    statusColor = item.tone.toColor(),
                    count = item.count,
                    onClick = item.onClick,
                    unavailableNote = item.unavailableNote,
                )
            }
        }
    }
}

@Composable
private fun MoreStatusTone?.toColor(): Color? {
    val colors = HelixTheme.colors
    return when (this) {
        MoreStatusTone.HEALTHY -> colors.success
        MoreStatusTone.ATTENTION -> colors.warning
        MoreStatusTone.FAULT -> colors.critical
        null -> null
    }
}
