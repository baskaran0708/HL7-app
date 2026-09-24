package com.livemedica.helix.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.model.InterfaceState
import com.livemedica.helix.domain.model.IntegrationSnapshot
import com.livemedica.helix.domain.repository.AppointmentRepository
import com.livemedica.helix.domain.repository.DoctorRepository
import com.livemedica.helix.domain.repository.IntegrationRepository
import com.livemedica.helix.domain.repository.NotificationRepository
import com.livemedica.helix.domain.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The counts the More menu puts on its rows.
 *
 * Every one is resolved here rather than in the composable so the menu stays a pure render of
 * state — and so a row's subtitle ("3 awaiting sign-off") can never drift from the screen it opens.
 */
data class MoreUiModel(
    val doctor: Doctor,
    val patientsToday: Int,
    val unsignedReports: Int,
    val unreadNotifications: Int,
    val hl7MessagesToday: Int,
    val unhealthyChannels: Int,
    /** Null when the mock/backend exposes no modality-worklist channel at all. */
    val worklistSyncState: InterfaceState?,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository,
    private val appointmentRepository: AppointmentRepository,
    private val resultRepository: ResultRepository,
    private val notificationRepository: NotificationRepository,
    private val integrationRepository: IntegrationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MoreUiModel>>(UiState.Loading)
    val state: StateFlow<UiState<MoreUiModel>> = _state.asStateFlow()

    init {
        load()
        observeLiveUpdates()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = doctorRepository.getCurrentDoctor()) {
                is AppResult.Success -> UiState.Success(buildModel(result.data))
                is AppResult.Offline -> UiState.Offline(result.cached?.let { buildModel(it) })
                is AppResult.Failure -> UiState.Error(result.message)
            }
        }
    }

    /**
     * Keeps the badges live. Reading a notification or signing a report on another screen has to be
     * reflected here the moment the user returns to the menu, without a refresh.
     */
    private fun observeLiveUpdates() {
        viewModelScope.launch {
            combine(
                doctorRepository.observeCurrentDoctor(),
                appointmentRepository.observeTodayAppointments(),
                resultRepository.observeReports(),
                notificationRepository.observeUnreadCount(),
                integrationRepository.observeSnapshot(),
            ) { doctor, appointments, reports, unread, snapshot ->
                MoreUiModel(
                    doctor = doctor,
                    patientsToday = appointments.distinctBy { it.patientId }.size,
                    unsignedReports = reports.count { !it.isSigned },
                    unreadNotifications = unread,
                    hl7MessagesToday = snapshot.messagesToday,
                    unhealthyChannels = snapshot.unhealthyChannelCount(),
                    worklistSyncState = snapshot.worklistChannelState(),
                )
            }.collect { model ->
                // Never let a background update overwrite a visible loading, error or offline state.
                if (_state.value is UiState.Success) _state.value = UiState.Success(model)
            }
        }
    }

    private suspend fun buildModel(doctor: Doctor): MoreUiModel {
        val snapshot = integrationRepository.observeSnapshot().first()
        return MoreUiModel(
            doctor = doctor,
            patientsToday = appointmentRepository.observeTodayAppointments().first()
                .distinctBy { it.patientId }.size,
            unsignedReports = resultRepository.observeReports().first().count { !it.isSigned },
            unreadNotifications = notificationRepository.observeUnreadCount().first(),
            hl7MessagesToday = snapshot.messagesToday,
            unhealthyChannels = snapshot.unhealthyChannelCount(),
            worklistSyncState = snapshot.worklistChannelState(),
        )
    }
}

private fun IntegrationSnapshot.unhealthyChannelCount(): Int =
    channels.count { it.state != InterfaceState.ONLINE }

/**
 * The modality worklist is a named Mirth channel rather than a first-class domain concept, so it is
 * matched by name here. Kept in one place so the convention is easy to replace when the backend
 * starts typing channels properly.
 */
private fun IntegrationSnapshot.worklistChannelState(): InterfaceState? =
    channels.firstOrNull { it.name.contains(WORKLIST_CHANNEL_MARKER, ignoreCase = true) }?.state

private const val WORKLIST_CHANNEL_MARKER = "MWL"
