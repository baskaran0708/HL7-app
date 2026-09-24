package com.livemedica.helix.data.repository

import com.livemedica.helix.data.local.HelixPreferencesDataSource
import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.domain.model.ThemePreference
import com.livemedica.helix.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferences: HelixPreferencesDataSource,
) : SettingsRepository {

    override fun observeSettings(): Flow<HelixSettings> = preferences.settings

    override suspend fun setThemePreference(preference: ThemePreference) = preferences.setTheme(preference)
    override suspend fun setCriticalAlertsEnabled(enabled: Boolean) = preferences.setCriticalAlerts(enabled)
    override suspend fun setResultAlertsEnabled(enabled: Boolean) = preferences.setResultAlerts(enabled)
    override suspend fun setScheduleAlertsEnabled(enabled: Boolean) = preferences.setScheduleAlerts(enabled)
    override suspend fun setBiometricUnlockEnabled(enabled: Boolean) = preferences.setBiometric(enabled)
    override suspend fun setSimulateOffline(enabled: Boolean) = preferences.setSimulateOffline(enabled)
    override suspend fun setSimulateError(enabled: Boolean) = preferences.setSimulateError(enabled)
}
