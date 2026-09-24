package com.livemedica.helix.feature.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.navigation.HelixRoute
import com.livemedica.helix.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Appointment detail: the booking, before it becomes an order and a study.
 *
 * Nothing here is observed live — an appointment is not mutated by anything the app can currently
 * do — so it is a straightforward one-shot read with a manual retry.
 */
@HiltViewModel
class AppointmentDetailViewModel @Inject constructor(
    private val loader: ExamContextLoader,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val appointmentId: String = savedStateHandle.toRoute<HelixRoute.Appointment>().appointmentId

    private val _state = MutableStateFlow<UiState<ExamContext>>(UiState.Loading)
    val state: StateFlow<UiState<ExamContext>> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = loader.forAppointment(appointmentId)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Offline -> UiState.Offline(null)
                is AppResult.Failure -> UiState.Error(result.message)
            }
        }
    }
}
