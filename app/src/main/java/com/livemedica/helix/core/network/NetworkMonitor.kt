package com.livemedica.helix.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Reports connectivity to the repository layer.
 *
 * Phase 1 combines the real device state with the Settings → Developer "simulate offline" switch,
 * so the offline UI can be demonstrated without turning off the radio.
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
