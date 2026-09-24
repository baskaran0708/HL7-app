package com.livemedica.helix.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixCard
import com.livemedica.helix.core.designsystem.component.HelixMenuRow
import com.livemedica.helix.core.designsystem.component.HelixRowDivider
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixSegmentedControl
import com.livemedica.helix.core.designsystem.component.HelixToggleRow
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.domain.model.ThemePreference

/**
 * Settings, one composable per section.
 *
 * Split out of the screen so each section stays readable and the screen itself is just the order
 * they appear in.
 */

/** Theme. Writing the preference re-themes the whole app, because `MainActivity` renders from it. */
@Composable
fun AppearanceSection(
    preference: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    HelixSection(title = "Appearance", modifier = modifier) {
        HelixCard(contentPadding = Spacing.lg) {
            HelixSegmentedControl(
                options = ThemePreference.entries,
                selected = preference,
                label = { it.label },
                onSelect = onSelect,
            )
            Text(
                text = "System follows your device's light or dark setting.",
                style = HelixTheme.typography.caption,
                color = HelixTheme.colors.textMute,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
    }
}

@Composable
fun NotificationSection(
    settings: HelixSettings,
    onCriticalChange: (Boolean) -> Unit,
    onResultsChange: (Boolean) -> Unit,
    onScheduleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    HelixSection(title = "Notifications", modifier = modifier) {
        HelixCard(contentPadding = 0.dp) {
            HelixToggleRow(
                title = "Critical findings",
                subtitle = "Page me the moment a critical result is ready to communicate.",
                checked = settings.criticalAlertsEnabled,
                onCheckedChange = onCriticalChange,
                // A patient-safety alert being off is worth saying out loud, every time.
                lockedNote = if (settings.criticalAlertsEnabled) {
                    null
                } else {
                    "Critical findings will not alert you while this is off."
                },
            )
            HelixRowDivider()
            HelixToggleRow(
                title = "Results ready",
                subtitle = "When a study finishes and is ready to read.",
                checked = settings.resultAlertsEnabled,
                onCheckedChange = onResultsChange,
            )
            HelixRowDivider()
            HelixToggleRow(
                title = "Schedule changes",
                subtitle = "Appointment moves, arrivals and cancellations.",
                checked = settings.scheduleAlertsEnabled,
                onCheckedChange = onScheduleChange,
            )
        }
    }
}

/**
 * Security.
 *
 * Biometric unlock stores a preference and nothing else — no `BiometricPrompt` is wired up. The row
 * says so, because a clinician must never believe a shared device is locked when it is not.
 */
@Composable
fun SecuritySection(
    settings: HelixSettings,
    onBiometricChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    HelixSection(title = "Security", modifier = modifier) {
        HelixCard(contentPadding = 0.dp) {
            HelixToggleRow(
                title = "Biometric unlock",
                subtitle = "Fingerprint or face unlock for the app.",
                checked = settings.biometricUnlockEnabled,
                onCheckedChange = onBiometricChange,
                lockedNote = "Not active yet — the preference is stored, but nothing is locked.",
            )
            HelixRowDivider()
            HelixMenuRow(
                icon = Icons.Outlined.Lock,
                title = "Active sessions",
                subtitle = "Devices signed in to your account",
                unavailableNote = "Soon",
            )
        }
    }
}

/**
 * Developer tools.
 *
 * These two switches drive [com.livemedica.helix.data.mock.MockCallSimulator], so every repository
 * call in the app returns offline or failure while they are on. That is the only way to review the
 * offline and error states of a screen before there is a backend to unplug.
 */
@Composable
fun DeveloperSection(
    settings: HelixSettings,
    onSimulateOfflineChange: (Boolean) -> Unit,
    onSimulateErrorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    HelixSection(title = "Developer", modifier = modifier) {
        Text(
            text = "Not for clinical use. These switches change how every screen in the app loads " +
                "its data.",
            style = HelixTheme.typography.caption,
            color = colors.warning,
        )
        HelixCard(contentPadding = 0.dp) {
            HelixToggleRow(
                title = "Simulate offline",
                subtitle = "Every screen falls back to its last synchronised data.",
                checked = settings.simulateOffline,
                onCheckedChange = onSimulateOfflineChange,
            )
            HelixRowDivider()
            HelixToggleRow(
                title = "Simulate error",
                subtitle = "Every data load fails, so error states and retry can be reviewed.",
                checked = settings.simulateError,
                onCheckedChange = onSimulateErrorChange,
            )
        }
    }
}

@Composable
fun AboutSection(versionName: String, modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    HelixSection(title = "About", modifier = modifier) {
        HelixCard(contentPadding = Spacing.lg) {
            Column(Modifier.fillMaxWidth()) {
                Text(text = "Helix", style = HelixTheme.typography.label, color = colors.text)
                Text(
                    text = "Version $versionName",
                    style = HelixTheme.typography.mono,
                    color = colors.textDim,
                    modifier = Modifier.padding(top = Spacing.xs + 2.dp),
                )
                Text(
                    text = "HL7 v2.5.1 · FHIR R4 · DICOM",
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
        }
    }
}
