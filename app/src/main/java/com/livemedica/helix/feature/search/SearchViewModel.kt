package com.livemedica.helix.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.core.ui.toUiState
import com.livemedica.helix.domain.model.SearchResults
import com.livemedica.helix.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global search.
 *
 * Typing is debounced rather than searched per keystroke: the repository call is a real round trip
 * (and will be a network one), and firing it on every character would both waste calls and make the
 * result list flicker through partial matches while the user is still typing.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<UiState<SearchResults>>(IDLE_STATE)
    val state: StateFlow<UiState<SearchResults>> = _state.asStateFlow()

    val recentQueries: StateFlow<List<String>> = searchRepository.observeRecentQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _query.value = value
        searchJob?.cancel()

        if (value.isBlank()) {
            // Back to the "not searched yet" surface, which shows recents rather than an empty list.
            _state.value = IDLE_STATE
            return
        }

        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(value)
        }
    }

    /** Called when the user commits a query from the keyboard, bypassing the debounce. */
    fun onSearchSubmitted() {
        val value = _query.value
        if (value.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runSearch(value)
            searchRepository.recordQuery(value)
        }
    }

    fun onRecentQuerySelected(value: String) {
        _query.value = value
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(value) }
    }

    /**
     * Opening a result is the strongest signal that a query was useful, so that is what promotes it
     * into the recent list — not merely having typed it.
     */
    fun onResultOpened() {
        val value = _query.value
        if (value.isNotBlank()) viewModelScope.launch { searchRepository.recordQuery(value) }
    }

    fun clearQuery() = onQueryChange("")

    private suspend fun runSearch(value: String) {
        _state.value = UiState.Loading
        _state.value = searchRepository.search(value).toUiState(
            isEmpty = { it.isEmpty },
            emptyMessage = "Nothing matched \"$value\". Try an MRN, accession number or patient name.",
        )
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L

        /**
         * The blank-query state. The screen never renders it — it swaps in the recent-searches
         * surface instead — but keeping it Empty means an accidental render still says something
         * sensible rather than showing a stale result list.
         */
        val IDLE_STATE = UiState.Empty("Search patients, orders, studies and reports.")
    }
}
