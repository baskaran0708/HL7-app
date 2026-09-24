package com.livemedica.helix.domain.repository

import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<HelixSettings>
    suspend fun setThemePreference(preference: ThemePreference)
    suspend fun setCriticalAlertsEnabled(enabled: Boolean)
    suspend fun setResultAlertsEnabled(enabled: Boolean)
    suspend fun setScheduleAlertsEnabled(enabled: Boolean)
    suspend fun setBiometricUnlockEnabled(enabled: Boolean)

    /** Developer-only switches used to demonstrate the offline and error states on demand. */
    suspend fun setSimulateOffline(enabled: Boolean)
    suspend fun setSimulateError(enabled: Boolean)
}
