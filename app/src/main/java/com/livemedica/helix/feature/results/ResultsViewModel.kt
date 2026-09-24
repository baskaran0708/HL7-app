package com.livemedica.helix.feature.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.ReportStatus
import com.livemedica.helix.domain.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * How the result inbox is sliced.
 *
 * The categories deliberately overlap — a STAT report awaiting signature appears under [NEW],
 * [STAT] and [PENDING_SIGN_OFF]. That mirrors how a radiologist actually works a list: each chip
 * answers a different question ("what landed today?", "what can't wait?", "what do I owe?"), and
 * forcing them into disjoint buckets would hide the urgent item behind the wrong filter.
 */
enum class ResultsCategory(val label: String) {
    NEW("New"),
    STAT("STAT"),
    PENDING_SIGN_OFF("Pending sign-off"),
    SIGNED("Signed"),
}

/** Everything the Results screen renders, assembled once so the chips and list can never disagree. */
data class ResultsUiModel(
    val category: ResultsCategory,
    val reports: List<RadiologyReport>,
    val counts: Map<ResultsCategory, Int>,
    val awaitingSignOff: Int,
    val statAwaiting: Int,
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val resultRepository: ResultRepository,
) : ViewModel() {

    private val _category = MutableStateFlow(ResultsCategory.PENDING_SIGN_OFF)
    val category: StateFlow<ResultsCategory> = _category.asStateFlow()

    private val _state = MutableStateFlow<UiState<ResultsUiModel>>(UiState.Loading)
    val state: StateFlow<UiState<ResultsUiModel>> = _state.asStateFlow()

    init {
        load()
        observeLiveUpdates()
    }

    fun refresh() = load()

    /**
     * Switching filter is a pure re-slice of data already held, so it never returns the screen to
     * a loading state — the list simply changes under a stable chip row.
     */
    fun selectCategory(category: ResultsCategory) {
        _category.value = category
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val selected = _category.value
            _state.value = when (val result = resultRepository.getReports()) {
                is AppResult.Success ->
                    // Empty here means the whole inbox is empty. A filter that matches nothing is
                    // *not* an empty screen — the chip row has to stay reachable — so that case is
                    // a Success carrying an empty list and is handled inside the content.
                    if (result.data.isEmpty()) UiState.Empty("Results appear here as studies are read.")
                    else UiState.Success(buildModel(result.data, selected))

                is AppResult.Offline -> UiState.Offline(result.cached?.let { buildModel(it, selected) })
                is AppResult.Failure -> UiState.Error(result.message)
            }
        }
    }

    /**
     * Keeps the list live: signing a report on the Report screen must remove it from
     * "Pending sign-off" and add it to "Signed" here without the user going back and refreshing.
     */
    private fun observeLiveUpdates() {
        viewModelScope.launch {
            combine(resultRepository.observeReports(), _category) { reports, category ->
                buildModel(reports, category)
            }.collect { model ->
                // Only refresh a screen that is already showing data, so a live update cannot
                // silently cancel a visible loading, error or offline state.
                if (_state.value is UiState.Success) _state.value = UiState.Success(model)
            }
        }
    }

    private fun buildModel(reports: List<RadiologyReport>, category: ResultsCategory) = ResultsUiModel(
        category = category,
        reports = reports.filter { it.matches(category) },
        counts = ResultsCategory.entries.associateWith { slice -> reports.count { it.matches(slice) } },
        awaitingSignOff = reports.count { it.awaitsSignature },
        statAwaiting = reports.count { it.awaitsSignature && it.priority == Priority.STAT },
    )

    private fun RadiologyReport.matches(category: ResultsCategory): Boolean = when (category) {
        ResultsCategory.NEW -> createdAt.toLocalDate() == LocalDate.now()
        ResultsCategory.STAT -> priority == Priority.STAT
        ResultsCategory.PENDING_SIGN_OFF -> awaitsSignature
        ResultsCategory.SIGNED -> isSigned
    }
}

/**
 * A draft is still being dictated, so it is not yet the attending's to sign — everything else
 * unsigned is work the clinician owes.
 */
private val RadiologyReport.awaitsSignature: Boolean
    get() = !isSigned && status != ReportStatus.DRAFT
