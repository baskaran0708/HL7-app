package com.livemedica.helix.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the future AWS backend.
 *
 * Kept separate from the domain models on purpose. The API will evolve on the backend's schedule
 * and will carry HL7-derived quirks (string enums, ISO-8601 timestamps, nullable fields that are
 * only nullable because a hospital feed omitted them). Absorbing that here means the domain layer
 * and every screen stay clean, and a breaking API change is a mapper edit rather than an app-wide
 * refactor.
 *
 * Unused at runtime in Phase 1.
 */

@Serializable
data class DoctorDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val specialty: String,
    val facility: String,
    @SerialName("licence_identifier") val licenceIdentifier: String? = null,
    @SerialName("on_call") val onCall: Boolean = false,
)

@Serializable
data class PatientDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val mrn: String,
    val age: Int,
    /** `F` / `M` / `X`, as HL7 PID-8 delivers it. */
    val sex: String,
    @SerialName("date_of_birth") val dateOfBirth: String,
    val allergies: List<String> = emptyList(),
)

@Serializable
data class AppointmentDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("patient_name") val patientName: String,
    @SerialName("patient_age") val patientAge: Int,
    @SerialName("patient_sex") val patientSex: String,
    val mrn: String,
    /** ISO-8601 local date-time. */
    val start: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val modality: String,
    val procedure: String,
    val priority: String,
    val status: String,
    @SerialName("facility_id") val facilityId: String,
    val room: String? = null,
)

@Serializable
data class OrderDto(
    val id: String,
    @SerialName("accession_number") val accessionNumber: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("patient_name") val patientName: String,
    @SerialName("patient_age") val patientAge: Int,
    @SerialName("patient_sex") val patientSex: String,
    val mrn: String,
    val modality: String,
    @SerialName("cpt_code") val cptCode: String,
    val procedure: String,
    @SerialName("clinical_indication") val clinicalIndication: String,
    @SerialName("ordering_physician") val orderingPhysician: String,
    @SerialName("ordering_location") val orderingLocation: String,
    val priority: String,
    val status: String,
    @SerialName("ordered_at") val orderedAt: String,
    @SerialName("facility_id") val facilityId: String,
    @SerialName("report_id") val reportId: String? = null,
)

@Serializable
data class ReportDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("study_id") val studyId: String,
    @SerialName("accession_number") val accessionNumber: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("patient_name") val patientName: String,
    @SerialName("patient_age") val patientAge: Int,
    @SerialName("patient_sex") val patientSex: String,
    val mrn: String,
    val modality: String,
    val procedure: String,
    @SerialName("clinical_indication") val clinicalIndication: String,
    val comparison: String? = null,
    val technique: String? = null,
    val findings: String,
    val impression: String,
    val priority: String,
    val status: String,
    val radiologist: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("finalized_at") val finalizedAt: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
    @SerialName("is_critical") val isCritical: Boolean = false,
)

@Serializable
data class StudyDto(
    val id: String,
    @SerialName("accession_number") val accessionNumber: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("patient_id") val patientId: String,
    val modality: String,
    val description: String,
    @SerialName("performed_at") val performedAt: String? = null,
    @SerialName("series_count") val seriesCount: Int = 0,
    @SerialName("image_count") val imageCount: Int = 0,
    @SerialName("study_instance_uid") val studyInstanceUid: String,
)

@Serializable
data class NotificationDto(
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("target_type") val targetType: String? = null,
    @SerialName("target_id") val targetId: String? = null,
)

@Serializable
data class SearchResultsDto(
    val query: String,
    val patients: List<PatientDto> = emptyList(),
    val studies: List<StudyDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
    val reports: List<ReportDto> = emptyList(),
    val appointments: List<AppointmentDto> = emptyList(),
)
