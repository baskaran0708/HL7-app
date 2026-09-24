package com.livemedica.helix.data

import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.domain.model.ThemePreference
import com.livemedica.helix.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory settings for tests. Lets a test drive the offline/error simulation that the mock
 * repositories consult, without DataStore or a Context.
 */
class FakeSettingsRepository(
    initial: HelixSettings = HelixSettings(),
) : SettingsRepository {

    private val state = MutableStateFlow(initial)

    override fun observeSettings(): Flow<HelixSettings> = state

    override suspend fun setThemePreference(preference: ThemePreference) =
        state.update { it.copy(themePreference = preference) }

    override suspend fun setCriticalAlertsEnabled(enabled: Boolean) =
        state.update { it.copy(criticalAlertsEnabled = enabled) }

    override suspend fun setResultAlertsEnabled(enabled: Boolean) =
        state.update { it.copy(resultAlertsEnabled = enabled) }

    override suspend fun setScheduleAlertsEnabled(enabled: Boolean) =
        state.update { it.copy(scheduleAlertsEnabled = enabled) }

    override suspend fun setBiometricUnlockEnabled(enabled: Boolean) =
        state.update { it.copy(biometricUnlockEnabled = enabled) }

    override suspend fun setSimulateOffline(enabled: Boolean) =
        state.update { it.copy(simulateOffline = enabled) }

    override suspend fun setSimulateError(enabled: Boolean) =
        state.update { it.copy(simulateError = enabled) }
}
