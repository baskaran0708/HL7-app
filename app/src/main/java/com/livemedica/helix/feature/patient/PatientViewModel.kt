package com.livemedica.helix.feature.patient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.common.dataOrNull
import com.livemedica.helix.core.navigation.HelixRoute
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.HistoryEntry
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.OrderStatus
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.Study
import com.livemedica.helix.domain.repository.PatientRepository
import com.livemedica.helix.domain.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** What a timeline entry represents, which is also how it is coloured and iconised. */
enum class PatientEventKind { ORDER, STUDY, REPORT, APPOINTMENT }

data class PatientEvent(
    val id: String,
    val at: LocalDateTime,
    val label: String,
    val detail: String,
    val kind: PatientEventKind,
)

/**
 * The patient record, arranged as a clinician reads it.
 *
 * "Current" is separated from "previous" in the model rather than in the composable because what
 * counts as current is a clinical judgement — an order that has not yet been reported — and that
 * rule belongs with the data, not the layout.
 */
data class PatientUiModel(
    val patient: Patient,
    val currentOrder: Order?,
    val currentStudy: Study?,
    val appointments: List<Appointment>,
    val previousStudies: List<Study>,
    val reports: List<RadiologyReport>,
    val problems: List<HistoryEntry>,
    val priors: List<HistoryEntry>,
    val timeline: List<PatientEvent>,
) {
    /** "NKDA" is a recorded negative, not an allergy — it must not raise the allergy banner. */
    val activeAllergies: List<String>
        get() = patient.allergies.filterNot { it.equals("NKDA", ignoreCase = true) }
}

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val resultRepository: ResultRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val patientId: String = savedStateHandle.toRoute<HelixRoute.Patient>().patientId

    private val _state = MutableStateFlow<UiState<PatientUiModel>>(UiState.Loading)
    val state: StateFlow<UiState<PatientUiModel>> = _state.asStateFlow()

    init {
        load()
        observeReportChanges()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = patientRepository.getPatient(patientId)) {
                is AppResult.Success -> UiState.Success(buildModel(result.data))
                is AppResult.Offline -> UiState.Offline(null)
                is AppResult.Failure -> UiState.Error(result.message)
            }
        }
    }

    /**
     * Signing a report elsewhere changes this record too — the report's status on the patient's
     * report list, and the event on their timeline — so the screen rebuilds when reports change.
     */
    private fun observeReportChanges() {
        viewModelScope.launch {
            resultRepository.observeReports().collect { reports ->
                val current = _state.value
                if (current is UiState.Success && reports.any { it.patientId == patientId }) {
                    _state.value = UiState.Success(buildModel(current.data.patient))
                }
            }
        }
    }

    private suspend fun buildModel(patient: Patient): PatientUiModel {
        val orders = patientRepository.getOrdersFor(patientId).dataOrNull().orEmpty()
        val studies = patientRepository.getStudiesFor(patientId).dataOrNull().orEmpty()
        val reports = patientRepository.getReportsFor(patientId).dataOrNull().orEmpty()
        val appointments = patientRepository.getAppointmentsFor(patientId).dataOrNull().orEmpty()

        val currentOrder = orders.firstOrNull { it.status.isOpen }
        val currentStudy = studies.firstOrNull { it.orderId == currentOrder?.id }

        return PatientUiModel(
            patient = patient,
            currentOrder = currentOrder,
            currentStudy = currentStudy,
            appointments = appointments,
            previousStudies = studies.filterNot { it.id == currentStudy?.id },
            reports = reports,
            // The seed carries problems and priors in one history list; priors are the entries
            // whose summary leads with a DICOM modality code (`CT · CT Head w/o Contrast`).
            problems = patient.history.filterNot { it.isPriorStudy },
            priors = patient.history.filter { it.isPriorStudy },
            timeline = buildTimeline(orders, studies, reports, appointments),
        )
    }

    private fun buildTimeline(
        orders: List<Order>,
        studies: List<Study>,
        reports: List<RadiologyReport>,
        appointments: List<Appointment>,
    ): List<PatientEvent> = buildList {
        orders.forEach {
            add(PatientEvent("ord-${it.id}", it.orderedAt, "Order placed", "${it.procedure} · ${it.orderingPhysician}", PatientEventKind.ORDER))
        }
        studies.forEach { study ->
            study.performedAt?.let {
                add(PatientEvent("std-${study.id}", it, "Study acquired", "${study.description} · ${study.imageCount} images", PatientEventKind.STUDY))
            }
        }
        reports.forEach { report ->
            add(PatientEvent("rpt-${report.id}", report.createdAt, "Report dictated", report.procedure, PatientEventKind.REPORT))
            report.signedAt?.let {
                add(PatientEvent("sig-${report.id}", it, "Report signed", "${report.procedure} · ${report.radiologist}", PatientEventKind.REPORT))
            }
        }
        appointments.forEach {
            add(PatientEvent("apt-${it.id}", it.start, "Appointment", "${it.procedure} · ${it.room}", PatientEventKind.APPOINTMENT))
        }
    }.sortedByDescending { it.at }.take(TIMELINE_LIMIT)

    private companion object {
        const val TIMELINE_LIMIT = 12
    }
}

/** An order still needs a radiologist's attention until it is reported or withdrawn. */
private val OrderStatus.isOpen: Boolean
    get() = this != OrderStatus.COMPLETED && this != OrderStatus.CANCELLED

private val HistoryEntry.isPriorStudy: Boolean
    get() = Regex("^[A-Z]{2} · ").containsMatchIn(summary)
