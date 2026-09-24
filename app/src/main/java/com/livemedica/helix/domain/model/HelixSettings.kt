package com.livemedica.helix.domain.model

import androidx.compose.runtime.Immutable

enum class ThemePreference(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

@Immutable
data class HelixSettings(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val criticalAlertsEnabled: Boolean = true,
    val resultAlertsEnabled: Boolean = true,
    val scheduleAlertsEnabled: Boolean = true,
    /**
     * Architecture-ready only. Phase 1 stores the preference; no BiometricPrompt is wired up, so
     * enabling this does not gate access to anything yet.
     */
    val biometricUnlockEnabled: Boolean = false,
    val simulateOffline: Boolean = false,
    val simulateError: Boolean = false,
)
