package com.livemedica.helix.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain models for the Helix clinical workflow.
 *
 * All of these are flat, immutable value types holding primitives and stable ids rather than object
 * graphs. That keeps them Compose-stable (no needless recomposition), trivially mappable from the
 * future API DTOs, and directly translatable to Room entities when offline support lands.
 */

@Immutable
data class Doctor(
    val id: String,
    val displayName: String,
    val specialty: String,
    val facility: String,
    /** Placeholder for the state medical licence / NPI identifier supplied by the backend. */
    val licenceIdentifier: String,
    val isOnCall: Boolean,
    val initials: String,
)

@Immutable
data class Facility(
    val id: String,
    val name: String,
    val shortName: String,
)

@Immutable
data class Patient(
    val id: String,
    val fullName: String,
    val mrn: String,
    val age: Int,
    val sex: Sex,
    val dateOfBirth: LocalDate,
    val allergies: List<String> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
)

@Immutable
data class HistoryEntry(
    val id: String,
    val date: LocalDate,
    val summary: String,
    val detail: String,
)

@Immutable
data class Appointment(
    val id: String,
    val patientId: String,
    val patientName: String,
    val patientAge: Int,
    val patientSex: Sex,
    val mrn: String,
    val start: LocalDateTime,
    val durationMinutes: Int,
    val modality: Modality,
    val procedure: String,
    val priority: Priority,
    val status: AppointmentStatus,
    val facilityId: String,
    val room: String,
) {
    val end: LocalDateTime get() = start.plusMinutes(durationMinutes.toLong())
}

@Immutable
data class Order(
    val id: String,
    val accessionNumber: String,
    val patientId: String,
    val patientName: String,
    val patientAge: Int,
    val patientSex: Sex,
    val mrn: String,
    val modality: Modality,
    val cptCode: String,
    val procedure: String,
    /** Free-text reason for exam, as it arrives in the HL7 ORM message. */
    val clinicalIndication: String,
    val orderingPhysician: String,
    val orderingLocation: String,
    val priority: Priority,
    val status: OrderStatus,
    val orderedAt: LocalDateTime,
    val facilityId: String,
    val reportId: String? = null,
)

@Immutable
data class Study(
    val id: String,
    val accessionNumber: String,
    val orderId: String,
    val patientId: String,
    val modality: Modality,
    val description: String,
    val performedAt: LocalDateTime?,
    val seriesCount: Int,
    val imageCount: Int,
    /** Opaque handle the PACS viewer will be deep-linked with once integration lands. */
    val studyInstanceUid: String,
)

@Immutable
data class RadiologyReport(
    val id: String,
    val orderId: String,
    val studyId: String,
    val accessionNumber: String,
    val patientId: String,
    val patientName: String,
    val patientAge: Int,
    val patientSex: Sex,
    val mrn: String,
    val modality: Modality,
    val procedure: String,
    val clinicalIndication: String,
    val comparison: String,
    val technique: String,
    val findings: String,
    val impression: String,
    val priority: Priority,
    val status: ReportStatus,
    val radiologist: String,
    val createdAt: LocalDateTime,
    val finalizedAt: LocalDateTime?,
    val signedAt: LocalDateTime?,
    /** True when the impression requires acknowledged communication to the ordering clinician. */
    val isCritical: Boolean,
) {
    val isSigned: Boolean get() = status == ReportStatus.SIGNED
}

@Immutable
data class HelixNotification(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val createdAt: LocalDateTime,
    val isRead: Boolean,
    /** Where tapping the notification should navigate. */
    val target: NotificationTarget?,
)

/**
 * Deliberately a sealed type rather than a raw id string: it forces every new notification kind to
 * declare where it navigates, so a notification can never be untappable by accident.
 */
@Immutable
sealed interface NotificationTarget {
    data class Report(val reportId: String) : NotificationTarget
    data class OrderDetail(val orderId: String) : NotificationTarget
    data class PatientDetail(val patientId: String) : NotificationTarget
    data class AppointmentDetail(val appointmentId: String) : NotificationTarget
    data object Integration : NotificationTarget
}

@Immutable
data class Hl7Message(
    val id: String,
    val type: Hl7MessageType,
    val receivedAt: LocalDateTime,
    val ackStatus: Hl7AckStatus,
    val reference: String,
    val channel: String,
)

@Immutable
data class InterfaceChannel(
    val id: String,
    val name: String,
    val state: InterfaceState,
    val messagesToday: Int,
    val lastMessageAt: LocalDateTime?,
)

@Immutable
data class IntegrationSnapshot(
    val messagesToday: Int,
    val ackRatePercent: Double,
    val countsByType: Map<Hl7MessageType, Int>,
    val channels: List<InterfaceChannel>,
    val recentMessages: List<Hl7Message>,
)

/** Aggregate powering the Today dashboard in one repository round-trip. */
@Immutable
data class TodayDashboard(
    val doctor: Doctor,
    val date: LocalDate,
    val criticalReport: RadiologyReport?,
    val examsToday: Int,
    val pendingReads: Int,
    val statCount: Int,
    val unsignedReports: Int,
    val upNext: List<Appointment>,
    val unreadNotifications: Int,
)

@Immutable
data class SearchResults(
    val query: String,
    val patients: List<Patient>,
    val studies: List<Study>,
    val orders: List<Order>,
    val reports: List<RadiologyReport>,
    val appointments: List<Appointment>,
) {
    val isEmpty: Boolean
        get() = patients.isEmpty() && studies.isEmpty() && orders.isEmpty() &&
            reports.isEmpty() && appointments.isEmpty()

    val totalCount: Int
        get() = patients.size + studies.size + orders.size + reports.size + appointments.size
}
