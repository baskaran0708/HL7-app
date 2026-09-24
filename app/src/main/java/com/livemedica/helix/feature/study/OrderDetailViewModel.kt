package com.livemedica.helix.feature.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.navigation.HelixRoute
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Order detail: the requisition, and everything that has happened to it since.
 *
 * Reports are re-read whenever the shared store changes so that signing a report — from Results,
 * from Today, or from the report itself — is reflected here without a manual refresh.
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val loader: ExamContextLoader,
    private val resultRepository: ResultRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: String = savedStateHandle.toRoute<HelixRoute.Order>().orderId

    private val _state = MutableStateFlow<UiState<ExamContext>>(UiState.Loading)
    val state: StateFlow<UiState<ExamContext>> = _state.asStateFlow()

    init {
        load()
        observeReportChanges()
    }

    fun refresh() = load()

    private fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _state.value = UiState.Loading
            _state.value = when (val result = loader.forOrder(orderId)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Offline -> UiState.Offline(null)
                is AppResult.Failure -> UiState.Error(result.message)
            }
        }
    }

    private fun observeReportChanges() {
        viewModelScope.launch {
            // `drop(1)` skips the store's current value: the initial load has already read it, and
            // re-entering `load` here would restart the request for no gain.
            resultRepository.observeReports().drop(1).collect {
                if (_state.value is UiState.Success) load(showLoading = false)
            }
        }
    }
}
