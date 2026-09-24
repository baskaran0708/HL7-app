package com.livemedica.helix.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.domain.model.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("helix_settings")

/**
 * Persists user preferences only.
 *
 * Nothing clinical is written here: no patient data, no tokens, no identifiers. When Room lands it
 * will hold the cached clinical records; this file stays limited to display and alert preferences.
 */
@Singleton
class HelixPreferencesDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    val settings: Flow<HelixSettings> = context.settingsDataStore.data.map { prefs ->
        HelixSettings(
            themePreference = prefs[Keys.THEME]
                ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            criticalAlertsEnabled = prefs[Keys.CRITICAL_ALERTS] ?: true,
            resultAlertsEnabled = prefs[Keys.RESULT_ALERTS] ?: true,
            scheduleAlertsEnabled = prefs[Keys.SCHEDULE_ALERTS] ?: true,
            biometricUnlockEnabled = prefs[Keys.BIOMETRIC] ?: false,
            simulateOffline = prefs[Keys.SIMULATE_OFFLINE] ?: false,
            simulateError = prefs[Keys.SIMULATE_ERROR] ?: false,
        )
    }

    suspend fun setTheme(preference: ThemePreference) = edit { it[Keys.THEME] = preference.name }
    suspend fun setCriticalAlerts(enabled: Boolean) = edit { it[Keys.CRITICAL_ALERTS] = enabled }
    suspend fun setResultAlerts(enabled: Boolean) = edit { it[Keys.RESULT_ALERTS] = enabled }
    suspend fun setScheduleAlerts(enabled: Boolean) = edit { it[Keys.SCHEDULE_ALERTS] = enabled }
    suspend fun setBiometric(enabled: Boolean) = edit { it[Keys.BIOMETRIC] = enabled }
    suspend fun setSimulateOffline(enabled: Boolean) = edit { it[Keys.SIMULATE_OFFLINE] = enabled }
    suspend fun setSimulateError(enabled: Boolean) = edit { it[Keys.SIMULATE_ERROR] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_preference")
        val CRITICAL_ALERTS = booleanPreferencesKey("critical_alerts")
        val RESULT_ALERTS = booleanPreferencesKey("result_alerts")
        val SCHEDULE_ALERTS = booleanPreferencesKey("schedule_alerts")
        val BIOMETRIC = booleanPreferencesKey("biometric_unlock")
        val SIMULATE_OFFLINE = booleanPreferencesKey("dev_simulate_offline")
        val SIMULATE_ERROR = booleanPreferencesKey("dev_simulate_error")
    }
}
