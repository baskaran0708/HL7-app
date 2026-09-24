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
 * Study detail: the acquired images and the order that asked for them.
 *
 * Entered from a report, a patient record or a worklist row, so it reconstructs the whole exam
 * context from the study id alone rather than expecting the caller to pass it through.
 */
@HiltViewModel
class StudyDetailViewModel @Inject constructor(
    private val loader: ExamContextLoader,
    private val resultRepository: ResultRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val studyId: String = savedStateHandle.toRoute<HelixRoute.Study>().studyId

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
            _state.value = when (val result = loader.forStudy(studyId)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Offline -> UiState.Offline(null)
                is AppResult.Failure -> UiState.Error(result.message)
            }
        }
    }

    private fun observeReportChanges() {
        viewModelScope.launch {
            resultRepository.observeReports().drop(1).collect {
                if (_state.value is UiState.Success) load(showLoading = false)
            }
        }
    }
}
