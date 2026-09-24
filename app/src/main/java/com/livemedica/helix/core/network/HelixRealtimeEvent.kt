package com.livemedica.helix.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Events the backend will push once WebSocket / FCM delivery is in place.
 *
 * Modelled as a sealed type now so that adding a transport later cannot quietly introduce an event
 * the app fails to handle — the `when` over this type will stop compiling instead.
 *
 * Nothing implements [RealtimeEventSource] in Phase 1; the mock store mutates in-process instead.
 */
sealed interface HelixRealtimeEvent {
    data class NewAppointment(val appointmentId: String) : HelixRealtimeEvent
    data class AppointmentChanged(val appointmentId: String) : HelixRealtimeEvent
    data class NewOrder(val orderId: String) : HelixRealtimeEvent
    data class NewOruResult(val reportId: String) : HelixRealtimeEvent
    data class CriticalResult(val reportId: String) : HelixRealtimeEvent
    data class ReportSignatureRequired(val reportId: String) : HelixRealtimeEvent
}

interface RealtimeEventSource {
    val events: Flow<HelixRealtimeEvent>
    suspend fun connect()
    suspend fun disconnect()
}
