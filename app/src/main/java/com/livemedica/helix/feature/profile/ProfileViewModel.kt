package com.livemedica.helix.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.core.ui.toUiState
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.repository.DoctorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Doctor>>(UiState.Loading)
    val state: StateFlow<UiState<Doctor>> = _state.asStateFlow()

    /** Set when an on-call change could not be written, so the switch can explain itself. */
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        load()
        observeDoctor()
    }

    fun refresh() = load()

    /**
     * On-call is a real write, not a local toggle: it changes who the paging system routes critical
     * findings to, so the switch must reflect what the server accepted rather than what was tapped.
     */
    fun setOnCall(onCall: Boolean) {
        viewModelScope.launch {
            _actionMessage.value = when (val result = doctorRepository.setOnCall(onCall)) {
                is AppResult.Success -> null
                is AppResult.Offline -> "You're offline — your on-call status was not changed."
                is AppResult.Failure -> result.message
            }
        }
    }

    fun dismissActionMessage() {
        _actionMessage.value = null
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = doctorRepository.getCurrentDoctor().toUiState()
        }
    }

    private fun observeDoctor() {
        viewModelScope.launch {
            doctorRepository.observeCurrentDoctor().collect { doctor ->
                if (_state.value is UiState.Success) _state.value = UiState.Success(doctor)
            }
        }
    }
}
