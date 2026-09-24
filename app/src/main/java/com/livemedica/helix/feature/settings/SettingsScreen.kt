package com.livemedica.helix.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.feature.settings.components.AboutSection
import com.livemedica.helix.feature.settings.components.AppearanceSection
import com.livemedica.helix.feature.settings.components.DeveloperSection
import com.livemedica.helix.feature.settings.components.NotificationSection
import com.livemedica.helix.feature.settings.components.SecuritySection
import com.livemedica.helix.feature.settings.components.rememberAppVersionName

/**
 * Settings.
 *
 * Ordered by how often a clinician touches it: appearance and alerts first, security next, and the
 * developer switches last — visibly separated, because they change how the whole app loads data.
 */
@Composable
fun SettingsScreen(
    actions: HelixNavActions,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        HelixTopBar(title = "Settings", onBack = actions.navigateBack)

        HelixStateHost(
            state = state,
            emptyTitle = "Settings unavailable",
            emptyBody = "Your preferences could not be read from this device.",
        ) { settings ->
            SettingsContent(settings = settings, viewModel = viewModel)
        }
    }
}

@Composable
private fun SettingsContent(settings: HelixSettings, viewModel: SettingsViewModel) {
    val versionName = rememberAppVersionName()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        AppearanceSection(
            preference = settings.themePreference,
            onSelect = viewModel::setThemePreference,
        )

        NotificationSection(
            settings = settings,
            onCriticalChange = viewModel::setCriticalAlerts,
            onResultsChange = viewModel::setResultAlerts,
            onScheduleChange = viewModel::setScheduleAlerts,
        )

        SecuritySection(
            settings = settings,
            onBiometricChange = viewModel::setBiometricUnlock,
        )

        DeveloperSection(
            settings = settings,
            onSimulateOfflineChange = viewModel::setSimulateOffline,
            onSimulateErrorChange = viewModel::setSimulateError,
        )

        AboutSection(versionName = versionName, modifier = Modifier.padding(bottom = 8.dp))
    }
}
