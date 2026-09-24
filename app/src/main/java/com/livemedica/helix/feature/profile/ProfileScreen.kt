package com.livemedica.helix.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixMenuRow
import com.livemedica.helix.core.designsystem.component.HelixRowDivider
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixToggleRow
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.feature.profile.components.CredentialField
import com.livemedica.helix.feature.profile.components.CredentialRow
import com.livemedica.helix.feature.profile.components.ProfileCard
import com.livemedica.helix.feature.profile.components.ProfileHeader
import com.livemedica.helix.feature.profile.components.SignOutDialog

/**
 * The clinician's own record.
 *
 * On-call sits near the top because it is the only control here that changes clinical behaviour —
 * everything below it is reference information or a route to another screen.
 */
@Composable
fun ProfileScreen(
    actions: HelixNavActions,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        HelixTopBar(title = "Profile", onBack = actions.navigateBack)

        actionMessage?.let { message ->
            Text(
                text = message,
                style = HelixTheme.typography.caption,
                color = HelixTheme.colors.warning,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HelixTheme.colors.warning.copy(alpha = 0.12f))
                    .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
            )
        }

        HelixStateHost(
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Profile unavailable",
            emptyBody = "We couldn't load your clinician record.",
        ) { doctor ->
            ProfileContent(
                doctor = doctor,
                actions = actions,
                onOnCallChange = viewModel::setOnCall,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    doctor: Doctor,
    actions: HelixNavActions,
    onOnCallChange: (Boolean) -> Unit,
) {
    var showSignOut by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        ProfileHeader(doctor)

        HelixSection(title = "Availability") {
            ProfileCard {
                HelixToggleRow(
                    title = "On call",
                    subtitle = "Critical findings and STAT orders page you directly while this is on.",
                    checked = doctor.isOnCall,
                    onCheckedChange = onOnCallChange,
                )
            }
        }

        HelixSection(title = "Credentials") {
            ProfileCard {
                CredentialRow(
                    start = { CredentialField("Licence", doctor.licenceIdentifier, isIdentifier = true) },
                    end = { CredentialField("Staff ID", doctor.id, isIdentifier = true) },
                )
                HelixHairline()
                CredentialRow(
                    start = { CredentialField("Specialty", doctor.specialty) },
                    end = { CredentialField("Facility", doctor.facility) },
                )
            }
        }

        HelixSection(title = "Preferences") {
            ProfileCard {
                HelixMenuRow(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Notification preferences",
                    subtitle = "Which alerts reach you, and how",
                    onClick = actions.openSettings,
                )
                HelixRowDivider(inset = 63.dp)
                HelixMenuRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Appearance",
                    subtitle = "Theme and display",
                    onClick = actions.openSettings,
                )
                HelixRowDivider(inset = 63.dp)
                HelixMenuRow(
                    icon = Icons.Outlined.Shield,
                    title = "Security",
                    subtitle = "Device unlock and session policy",
                    onClick = actions.openSettings,
                )
            }
        }

        HelixSection(title = "Account") {
            ProfileCard {
                HelixMenuRow(
                    icon = Icons.Outlined.Edit,
                    title = "Edit profile",
                    subtitle = "Name, specialty and contact details are managed by your facility",
                    unavailableNote = "Read-only",
                )
                HelixRowDivider(inset = 63.dp)
                HelixMenuRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "Sign out",
                    subtitle = "Not connected yet — see details",
                    onClick = { showSignOut = true },
                )
            }
        }
    }

    if (showSignOut) SignOutDialog(onDismiss = { showSignOut = false })
}
