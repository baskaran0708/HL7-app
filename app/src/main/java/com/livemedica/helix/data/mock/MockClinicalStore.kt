package com.livemedica.helix.data.mock

import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.model.Facility
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.Hl7Message
import com.livemedica.helix.domain.model.InterfaceChannel
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.ReportStatus
import com.livemedica.helix.domain.model.Study
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single in-memory source of truth for Phase 1.
 *
 * Deliberately one shared store rather than per-repository copies: signing a report has to make the
 * Results list, the Today workload counters and the notification badge all change at once, which is
 * what makes the mocked app behave like a real one. Every repository reads and writes here, so a
 * mutation fans out through the [StateFlow]s to whichever screens are observing.
 *
 * When the AWS backend lands this class is replaced by Room + the API; nothing above the repository
 * layer has to change.
 */
@Singleton
class MockClinicalStore @Inject constructor() {

    private val seed: ClinicalSeed = SeedClinicalData.build(LocalDate.now())

    private val _doctor = MutableStateFlow(seed.doctor)
    private val _patients = MutableStateFlow(seed.patients)
    private val _appointments = MutableStateFlow(seed.appointments)
    private val _orders = MutableStateFlow(seed.orders)
    private val _studies = MutableStateFlow(seed.studies)
    private val _reports = MutableStateFlow(seed.reports)
    private val _notifications = MutableStateFlow(seed.notifications)
    private val _recentQueries = MutableStateFlow<List<String>>(emptyList())

    val doctor: StateFlow<Doctor> = _doctor.asStateFlow()
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    val studies: StateFlow<List<Study>> = _studies.asStateFlow()
    val reports: StateFlow<List<RadiologyReport>> = _reports.asStateFlow()
    val notifications: StateFlow<List<HelixNotification>> = _notifications.asStateFlow()
    val recentQueries: StateFlow<List<String>> = _recentQueries.asStateFlow()

    val facilities: List<Facility> = seed.facilities
    val hl7Messages: List<Hl7Message> = seed.hl7Messages
    val channels: List<InterfaceChannel> = seed.channels

    fun setOnCall(onCall: Boolean): Doctor {
        _doctor.update { it.copy(isOnCall = onCall) }
        return _doctor.value
    }

    /**
     * Idempotent by design: signing an already-signed report returns it unchanged rather than
     * stamping a second signature, mirroring what the backend will enforce.
     */
    fun signReport(id: String, signedAt: LocalDateTime = LocalDateTime.now()): RadiologyReport? {
        var signed: RadiologyReport? = null
        _reports.update { reports ->
            reports.map { report ->
                if (report.id != id) {
                    report
                } else if (report.isSigned) {
                    report.also { signed = it }
                } else {
                    report.copy(
                        status = ReportStatus.SIGNED,
                        signedAt = signedAt,
                        finalizedAt = report.finalizedAt ?: signedAt,
                    ).also { signed = it }
                }
            }
        }
        // A signed report no longer needs the clinician's attention, so its prompts clear too.
        signed?.let { report ->
            _notifications.update { list ->
                list.map { if (it.body.contains(report.accessionNumber)) it.copy(isRead = true) else it }
            }
        }
        return signed
    }

    fun markNotificationRead(id: String) {
        _notifications.update { list -> list.map { if (it.id == id) it.copy(isRead = true) else it } }
    }

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }

    fun clearNotification(id: String) {
        _notifications.update { list -> list.filterNot { it.id == id } }
    }

    fun recordQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _recentQueries.update { existing ->
            (listOf(trimmed) + existing.filterNot { it.equals(trimmed, ignoreCase = true) }).take(RECENT_QUERY_LIMIT)
        }
    }

    fun clearRecentQueries() {
        _recentQueries.value = emptyList()
    }

    private companion object {
        const val RECENT_QUERY_LIMIT = 8
    }
}
