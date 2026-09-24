package com.livemedica.helix.feature.study

import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.common.dataOrNull
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.Study
import com.livemedica.helix.domain.repository.AppointmentRepository
import com.livemedica.helix.domain.repository.OrderRepository
import com.livemedica.helix.domain.repository.PatientRepository
import com.livemedica.helix.domain.repository.ResultRepository
import javax.inject.Inject

/**
 * One exam, seen from whichever end the clinician arrived at.
 *
 * An order, a study and an appointment are three views of the same episode of care, and all three
 * screens need the same surrounding facts. Modelling that once — rather than three times in three
 * ViewModels — is what keeps "Order detail" and "Study detail" from quietly drifting apart.
 */
data class ExamContext(
    val patient: Patient?,
    val order: Order?,
    val study: Study?,
    val report: RadiologyReport?,
    val appointment: Appointment?,
    val facilityName: String?,
) {
    /** "NKDA" is a recorded negative, not an allergy. */
    val activeAllergies: List<String>
        get() = patient?.allergies.orEmpty().filterNot { it.equals("NKDA", ignoreCase = true) }
}

/**
 * Assembles an [ExamContext] from whichever identifier is in hand.
 *
 * A plain injectable collaborator rather than a base ViewModel: each screen keeps its own
 * `SavedStateHandle` and its own lifecycle, and this stays trivially testable on its own.
 */
class ExamContextLoader @Inject constructor(
    private val patientRepository: PatientRepository,
    private val orderRepository: OrderRepository,
    private val resultRepository: ResultRepository,
    private val appointmentRepository: AppointmentRepository,
) {

    suspend fun forOrder(orderId: String): AppResult<ExamContext> =
        when (val result = orderRepository.getOrder(orderId)) {
            is AppResult.Success -> AppResult.Success(contextFor(result.data))
            is AppResult.Offline -> AppResult.Offline(null)
            is AppResult.Failure -> result
        }

    suspend fun forStudy(studyId: String): AppResult<ExamContext> =
        when (val result = orderRepository.getStudy(studyId)) {
            is AppResult.Success -> {
                val order = orderRepository.getOrder(result.data.orderId).dataOrNull()
                AppResult.Success(
                    if (order != null) contextFor(order, result.data)
                    else ExamContext(null, null, result.data, null, null, null),
                )
            }

            is AppResult.Offline -> AppResult.Offline(null)
            is AppResult.Failure -> result
        }

    suspend fun forAppointment(appointmentId: String): AppResult<ExamContext> =
        when (val result = appointmentRepository.getAppointment(appointmentId)) {
            is AppResult.Success -> {
                val appointment = result.data
                AppResult.Success(
                    ExamContext(
                        patient = patientRepository.getPatient(appointment.patientId).dataOrNull(),
                        // An appointment is booked before an order is filled, so both may legitimately
                        // be absent here; the screen says so rather than inventing a placeholder.
                        order = null,
                        study = null,
                        report = null,
                        appointment = appointment,
                        facilityName = facilityName(appointment.facilityId),
                    ),
                )
            }

            is AppResult.Offline -> AppResult.Offline(null)
            is AppResult.Failure -> result
        }

    private suspend fun contextFor(order: Order, study: Study? = null) = ExamContext(
        patient = patientRepository.getPatient(order.patientId).dataOrNull(),
        order = order,
        study = study ?: orderRepository.getStudyForOrder(order.id).dataOrNull(),
        report = reportFor(order),
        appointment = null,
        facilityName = facilityName(order.facilityId),
    )

    /**
     * Prefers the id the order carries, falling back to a match on accession number — the order is
     * the system of record for the link, but an ORU can arrive before the order is updated.
     */
    private suspend fun reportFor(order: Order): RadiologyReport? {
        val reports = resultRepository.getReports().dataOrNull().orEmpty()
        return reports.firstOrNull { it.id == order.reportId }
            ?: reports.firstOrNull { it.accessionNumber == order.accessionNumber }
    }

    private suspend fun facilityName(facilityId: String): String? =
        appointmentRepository.getFacilities().dataOrNull()?.firstOrNull { it.id == facilityId }?.name
}
