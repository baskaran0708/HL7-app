package com.livemedica.helix.feature.more.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.domain.model.InterfaceState
import com.livemedica.helix.feature.more.MoreUiModel

/**
 * The More menu, expressed as data.
 *
 * Building the menu as a plain list keeps the composable a renderer and makes the product's
 * grouping — Clinical, Workflow, Integration, System — reviewable in one place instead of being
 * buried in layout code.
 */

/**
 * Status tone carried by a row's subtitle. An enum rather than a `Color` because colours can only
 * be resolved inside composition, and this list is built outside it.
 */
enum class MoreStatusTone { HEALTHY, ATTENTION, FAULT }

@Immutable
data class MoreMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val tone: MoreStatusTone? = null,
    val count: Int = 0,
    val onClick: (() -> Unit)? = null,
    /** Set on rows whose destination does not exist yet; the row renders dimmed and inert. */
    val unavailableNote: String? = null,
)

@Immutable
data class MoreMenuGroup(val title: String, val items: List<MoreMenuItem>)

fun moreMenuGroups(model: MoreUiModel, actions: HelixNavActions): List<MoreMenuGroup> = listOf(
    MoreMenuGroup(
        title = "Clinical",
        items = listOf(
            MoreMenuItem(
                icon = Icons.Outlined.Groups,
                title = "Patients",
                subtitle = "${model.patientsToday} scheduled today · search by name or MRN",
                onClick = actions.openSearch,
            ),
            MoreMenuItem(
                icon = Icons.Outlined.Description,
                title = "Reports",
                subtitle = if (model.unsignedReports > 0) {
                    "${model.unsignedReports} awaiting sign-off"
                } else {
                    "All reports signed"
                },
                count = model.unsignedReports,
                onClick = actions.openResults,
            ),
            MoreMenuItem(
                icon = Icons.Outlined.NotificationsNone,
                title = "Notifications",
                subtitle = if (model.unreadNotifications > 0) {
                    "${model.unreadNotifications} unread"
                } else {
                    "You're all caught up"
                },
                count = model.unreadNotifications,
                onClick = actions.openNotifications,
            ),
        ),
    ),
    MoreMenuGroup(
        title = "Workflow",
        items = listOf(
            MoreMenuItem(
                icon = Icons.Outlined.Layers,
                title = "Audit trail",
                subtitle = "HL7 event traceability per patient",
                unavailableNote = "Soon",
            ),
            MoreMenuItem(
                icon = Icons.Outlined.StarBorder,
                title = "Saved items",
                subtitle = "Studies and reports you've flagged",
                unavailableNote = "Soon",
            ),
        ),
    ),
    MoreMenuGroup(
        title = "Integration",
        items = listOf(
            MoreMenuItem(
                icon = Icons.Outlined.Timeline,
                title = "HL7 feed",
                subtitle = "${model.hl7MessagesToday} messages today",
                onClick = actions.openIntegration,
            ),
            MoreMenuItem(
                icon = Icons.Outlined.Dns,
                title = "Interface status",
                subtitle = if (model.unhealthyChannels > 0) {
                    "${model.unhealthyChannels} channel(s) need attention"
                } else {
                    "All channels online"
                },
                tone = if (model.unhealthyChannels > 0) MoreStatusTone.ATTENTION else MoreStatusTone.HEALTHY,
                onClick = actions.openIntegration,
            ),
            MoreMenuItem(
                icon = Icons.Outlined.Storage,
                title = "MWL sync",
                subtitle = model.worklistSyncState?.let { "Modality worklist · ${it.label.lowercase()}" }
                    ?: "Modality worklist · no channel reporting",
                tone = model.worklistSyncState.toTone(),
                onClick = actions.openIntegration,
            ),
        ),
    ),
    MoreMenuGroup(
        title = "System",
        items = listOf(
            MoreMenuItem(
                icon = Icons.Outlined.Person,
                title = "Profile",
                subtitle = "${model.doctor.displayName} · ${model.doctor.specialty}",
                onClick = actions.openProfile,
            ),
            MoreMenuItem(
                icon = Icons.Outlined.DarkMode,
                title = "Appearance",
                subtitle = "Theme and display",
                onClick = actions.openSettings,
            ),
            MoreMenuItem(
                icon = Icons.Outlined.Settings,
                title = "Settings",
                subtitle = "Notifications, security, developer tools",
                onClick = actions.openSettings,
            ),
        ),
    ),
)

private fun InterfaceState?.toTone(): MoreStatusTone? = when (this) {
    InterfaceState.ONLINE -> MoreStatusTone.HEALTHY
    InterfaceState.DEGRADED -> MoreStatusTone.ATTENTION
    InterfaceState.OFFLINE -> MoreStatusTone.FAULT
    null -> null
}
