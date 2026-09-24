package com.livemedica.helix.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.TodayDashboard
import com.livemedica.helix.domain.repository.NotificationRepository
import com.livemedica.helix.domain.repository.ResultRepository
import com.livemedica.helix.domain.repository.TodayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything the Today screen renders, in one state object.
 *
 * Assembled from three repositories here rather than in the composable, so the screen stays a pure
 * function of state and the "awaiting sign-off" list stays in step with the workload counters.
 */
data class TodayUiModel(
    val dashboard: TodayDashboard,
    val awaitingSignOff: List<RadiologyReport>,
    val recentActivity: List<HelixNotification>,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val todayRepository: TodayRepository,
    private val resultRepository: ResultRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TodayUiModel>>(UiState.Loading)
    val state: StateFlow<UiState<TodayUiModel>> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
        observeLiveUpdates()
    }

    fun refresh() {
        _isRefreshing.value = true
        load(showLoading = false)
    }

    private fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _state.value = UiState.Loading
            _state.value = when (val result = todayRepository.getDashboard()) {
                is AppResult.Success -> UiState.Success(buildModel(result.data))
                is AppResult.Offline -> UiState.Offline(result.cached?.let { buildModel(it) })
                is AppResult.Failure -> UiState.Error(result.message)
            }
            _isRefreshing.value = false
        }
    }

    /**
     * Keeps Today live after the initial load: signing a report elsewhere in the app must update
     * the counters and the sign-off list here without the user pulling to refresh.
     */
    private fun observeLiveUpdates() {
        viewModelScope.launch {
            combine(
                todayRepository.observeDashboard(),
                resultRepository.observeReports(),
                notificationRepository.observeNotifications(),
            ) { dashboard, reports, notifications ->
                TodayUiModel(
                    dashboard = dashboard,
                    awaitingSignOff = reports.filterNot { it.isSigned },
                    recentActivity = notifications.take(RECENT_ACTIVITY_LIMIT),
                )
            }.collect { model ->
                // Only replace a state that is already showing data, so a live update can't
                // silently cancel a visible loading, error or offline state.
                if (_state.value is UiState.Success) _state.value = UiState.Success(model)
            }
        }
    }

    private suspend fun buildModel(dashboard: TodayDashboard): TodayUiModel = TodayUiModel(
        dashboard = dashboard,
        awaitingSignOff = resultRepository.observeReports().first().filterNot { it.isSigned },
        recentActivity = notificationRepository.observeNotifications().first().take(RECENT_ACTIVITY_LIMIT),
    )

    private companion object {
        const val RECENT_ACTIVITY_LIMIT = 4
    }
}
