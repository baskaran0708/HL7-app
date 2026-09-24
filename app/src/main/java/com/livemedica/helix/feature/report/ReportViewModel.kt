package com.livemedica.helix.feature.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.common.dataOrNull
import com.livemedica.helix.core.navigation.HelixRoute
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.repository.DoctorRepository
import com.livemedica.helix.domain.repository.OrderRepository
import com.livemedica.helix.domain.repository.PatientRepository
import com.livemedica.helix.domain.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The report, plus the context a clinician needs before signing it: who the patient is, what was
 * ordered and by whom, and which attending's credentials the signature will carry.
 */
data class ReportUiModel(
    val report: RadiologyReport,
    val patient: Patient?,
    val order: Order?,
    val doctor: Doctor,
)

/**
 * The sign-off transaction, tracked separately from the screen's [UiState].
 *
 * A failed signature must not blank out the report the clinician is reading — they still need to
 * see the findings — so signing has its own small state machine that the confirmation sheet
 * renders, while the report itself stays on screen throughout.
 */
sealed interface SignatureState {
    data object Idle : SignatureState
    data object Submitting : SignatureState
    data object Signed : SignatureState
    data class Failed(val message: String) : SignatureState
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val resultRepository: ResultRepository,
    private val patientRepository: PatientRepository,
    private val orderRepository: OrderRepository,
    private val doctorRepository: DoctorRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val reportId: String = savedStateHandle.toRoute<HelixRoute.Report>().reportId

    private val _state = MutableStateFlow<UiState<ReportUiModel>>(UiState.Loading)
    val state: StateFlow<UiState<ReportUiModel>> = _state.asStateFlow()

    private val _signature = MutableStateFlow<SignatureState>(SignatureState.Idle)
    val signature: StateFlow<SignatureState> = _signature.asStateFlow()

    /**
     * Patient, order and attending are fetched once and cached here: they cannot change while the
     * screen is open, whereas the report itself can, so only the report is re-read from the
     * observed flow.
     */
    private val _context = MutableStateFlow<ReportContext?>(null)

    init {
        load()
        observeLiveUpdates()
    }

    fun refresh() = load()

    /**
     * Applies the attending's signature.
     *
     * The repository is idempotent, but the guard is still here because the confirmation sheet
     * stays on screen during submission — a second tap must not queue a second write.
     */
    fun signReport() {
        if (_signature.value == SignatureState.Submitting) return
        viewModelScope.launch {
            _signature.value = SignatureState.Submitting
            _signature.value = when (val result = resultRepository.signReport(reportId)) {
                is AppResult.Success -> {
                    // Applied immediately as well as through the observed flow, so the signed
                    // state is on screen the instant the sheet dismisses.
                    applyReport(result.data)
                    SignatureState.Signed
                }

                is AppResult.Offline -> SignatureState.Failed(
                    "You're offline. A report can only be signed while connected.",
                )

                is AppResult.Failure -> SignatureState.Failed(result.message)
            }
        }
    }

    /** Returns the sheet to its resting state after a failure has been read and dismissed. */
    fun acknowledgeSignature() {
        if (_signature.value !is SignatureState.Submitting) _signature.value = SignatureState.Idle
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = resultRepository.getReport(reportId)) {
                is AppResult.Success -> {
                    val context = loadContext(result.data)
                    _context.value = context
                    _state.value = UiState.Success(result.data.toModel(context))
                }

                is AppResult.Offline -> _state.value = UiState.Offline(null)
                is AppResult.Failure -> _state.value = UiState.Error(result.message)
            }
        }
    }

    /**
     * Keeps the screen in step with the shared store — the same mechanism that makes signing here
     * update the Results list and the Today counters also updates this screen.
     */
    private fun observeLiveUpdates() {
        viewModelScope.launch {
            combine(resultRepository.observeReports(), _context) { reports, context ->
                val report = reports.firstOrNull { it.id == reportId }
                if (report != null && context != null) report.toModel(context) else null
            }.collect { model ->
                if (model != null && _state.value is UiState.Success) _state.value = UiState.Success(model)
            }
        }
    }

    private fun applyReport(report: RadiologyReport) {
        val context = _context.value ?: return
        _state.value = UiState.Success(report.toModel(context))
    }

    private suspend fun loadContext(report: RadiologyReport) = ReportContext(
        patient = patientRepository.getPatient(report.patientId).dataOrNull(),
        order = orderRepository.getOrder(report.orderId).dataOrNull(),
        // Falls back to the name already stamped on the report if the directory call fails, so the
        // signature block is never left without an author.
        doctor = doctorRepository.getCurrentDoctor().dataOrNull() ?: fallbackDoctor(report),
    )

    private fun RadiologyReport.toModel(context: ReportContext) = ReportUiModel(
        report = this,
        patient = context.patient,
        order = context.order,
        doctor = context.doctor,
    )

    private fun fallbackDoctor(report: RadiologyReport) = Doctor(
        id = report.radiologist,
        displayName = report.radiologist,
        specialty = "Diagnostic Radiology",
        facility = "",
        licenceIdentifier = "",
        isOnCall = false,
        initials = report.radiologist.take(INITIALS_LENGTH).uppercase(),
    )

    private data class ReportContext(
        val patient: Patient?,
        val order: Order?,
        val doctor: Doctor,
    )

    private companion object {
        const val INITIALS_LENGTH = 2
    }
}
