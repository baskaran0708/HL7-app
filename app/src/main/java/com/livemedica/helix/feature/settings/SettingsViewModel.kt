package com.livemedica.helix.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.domain.model.ThemePreference
import com.livemedica.helix.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings.
 *
 * Every write goes straight to the repository and the screen re-renders from what comes back out of
 * [SettingsRepository.observeSettings] — there is no local mirror of a switch's position. That is
 * what makes the theme selector and the developer switches genuinely authoritative: the same flow
 * drives this screen, the app's theme and the mock layer's offline/error behaviour.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<UiState<HelixSettings>> = settingsRepository.observeSettings()
        .map { UiState.Success(it) as UiState<HelixSettings> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    fun setThemePreference(preference: ThemePreference) = update {
        settingsRepository.setThemePreference(preference)
    }

    fun setCriticalAlerts(enabled: Boolean) = update {
        settingsRepository.setCriticalAlertsEnabled(enabled)
    }

    fun setResultAlerts(enabled: Boolean) = update {
        settingsRepository.setResultAlertsEnabled(enabled)
    }

    fun setScheduleAlerts(enabled: Boolean) = update {
        settingsRepository.setScheduleAlertsEnabled(enabled)
    }

    fun setBiometricUnlock(enabled: Boolean) = update {
        settingsRepository.setBiometricUnlockEnabled(enabled)
    }

    fun setSimulateOffline(enabled: Boolean) = update {
        settingsRepository.setSimulateOffline(enabled)
    }

    fun setSimulateError(enabled: Boolean) = update {
        settingsRepository.setSimulateError(enabled)
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
