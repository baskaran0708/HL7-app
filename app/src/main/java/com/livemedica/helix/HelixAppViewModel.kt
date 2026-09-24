package com.livemedica.helix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.domain.repository.ResultRepository
import com.livemedica.helix.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Shell-level state: the theme the whole app renders with, and the Results tab badge.
 *
 * Both are app-wide rather than screen-local, which is why they live here instead of being
 * recomputed inside each screen's ViewModel.
 */
@HiltViewModel
class HelixAppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    resultRepository: ResultRepository,
) : ViewModel() {

    val settings: StateFlow<HelixSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HelixSettings())

    val unsignedReportCount: StateFlow<Int> = resultRepository.observeReports()
        .map { reports -> reports.count { !it.isSigned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
