package com.livemedica.helix.feature.worklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.common.dataOrNull
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.OrderStatus
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Worklist state.
 *
 * Priority, status and the free-text query are pushed down to [OrderRepository.getOrders] because
 * they are query parameters the backend will own; the tab and the modality set are applied here
 * because they are ways of looking at the same fetched page. Nothing filters in a composable.
 */
@HiltViewModel
class WorklistViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Order>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Order>>> = _state.asStateFlow()

    private val _filters = MutableStateFlow(WorklistFilterState())
    val filters: StateFlow<WorklistFilterState> = _filters.asStateFlow()

    /**
     * Set when a contextual action needs a study the list does not carry an id for. The screen
     * consumes it, navigates, and clears it — so a configuration change cannot replay the jump.
     */
    private val _studyToOpen = MutableStateFlow<String?>(null)
    val studyToOpen: StateFlow<String?> = _studyToOpen.asStateFlow()

    /** The last page as fetched, before the tab and modality filters are applied locally. */
    private var fetched: List<Order> = emptyList()

    /** Every order, kept live, purely so the priority chips can show honest counts. */
    private var allOrders: List<Order> = emptyList()

    private var searchJob: Job? = null

    /** Cancelled on every new load so a slow fetch can't overwrite a newer one's results. */
    private var loadJob: Job? = null

    init {
        load()
        observeCounts()
    }

    fun refresh() = load(showLoading = false)

    fun selectTab(tab: WorklistTab) {
        if (tab == _filters.value.tab) return
        _filters.update { it.copy(tab = tab) }
        recomputeCounts()
        publish()
    }

    /**
     * Debounced: a clinician types an MRN a character at a time, and re-querying on every keystroke
     * would thrash the list under their thumb.
     */
    fun setQuery(query: String) {
        _filters.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(showLoading = false)
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _filters.update { it.copy(query = "") }
        load(showLoading = false)
    }

    fun selectPriority(priority: Priority?) {
        if (priority == _filters.value.priority) return
        _filters.update { it.copy(priority = priority) }
        load(showLoading = false)
    }

    fun selectStatus(status: OrderStatus?) {
        if (status == _filters.value.status) return
        _filters.update { it.copy(status = status) }
        load(showLoading = false)
    }

    fun toggleModality(modality: Modality) {
        _filters.update { filters ->
            val next = filters.modalities.toMutableSet()
            if (!next.remove(modality)) next.add(modality)
            filters.copy(modalities = next)
        }
        publish()
    }

    fun resetFilters() {
        _filters.update { it.copy(priority = null, status = null, modalities = emptySet()) }
        load(showLoading = false)
    }

    fun requestStudy(orderId: String) {
        viewModelScope.launch {
            _studyToOpen.value = orderRepository.getStudyForOrder(orderId).dataOrNull()?.id
        }
    }

    fun consumeStudyRequest() {
        _studyToOpen.value = null
    }

    private fun load(showLoading: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showLoading) _state.value = UiState.Loading
            val filters = _filters.value

            when (
                val result = orderRepository.getOrders(
                    priority = filters.priority,
                    status = filters.status,
                    query = filters.query.takeIf { it.isNotBlank() },
                )
            ) {
                is AppResult.Success -> {
                    fetched = result.data
                    publish()
                }

                is AppResult.Offline -> {
                    fetched = result.cached.orEmpty()
                    _state.value = UiState.Offline(visibleOrders().takeIf { it.isNotEmpty() })
                }

                is AppResult.Failure -> {
                    fetched = emptyList()
                    _state.value = UiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Keeps the worklist honest after the initial fetch: an order arriving or completing elsewhere
     * in the app must move the chip counts here without a pull-to-refresh.
     */
    private fun observeCounts() {
        viewModelScope.launch {
            orderRepository.observeOrders().collect { orders ->
                allOrders = orders
                recomputeCounts()
            }
        }
    }

    private fun recomputeCounts() {
        val scoped = allOrders.inCurrentTab()
        _filters.update { filters ->
            filters.copy(
                priorityFilters = buildList {
                    add(PriorityFilter(null, "All", scoped.size))
                    Priority.entries.forEach { priority ->
                        add(
                            PriorityFilter(
                                priority = priority,
                                label = priority.displayLabel(),
                                count = scoped.count { it.priority == priority },
                            ),
                        )
                    }
                },
            )
        }
    }

    /** Re-derives the visible list from the last fetch. Tab and modality changes route through here. */
    private fun publish() {
        val visible = visibleOrders()
        _filters.update { it.copy(visibleCount = visible.size) }
        _state.value = if (visible.isEmpty()) {
            UiState.Empty(emptyMessageFor(_filters.value))
        } else {
            UiState.Success(visible)
        }
    }

    private fun visibleOrders(): List<Order> {
        val modalities = _filters.value.modalities
        return fetched.inCurrentTab().filter { modalities.isEmpty() || it.modality in modalities }
    }

    private fun List<Order>.inCurrentTab(): List<Order> = when (_filters.value.tab) {
        WorklistTab.ORDERS -> this
        // A result only exists once a report is attached to the order.
        WorklistTab.RESULTS -> filter { it.reportId != null }
    }

    private fun emptyMessageFor(filters: WorklistFilterState): String = when {
        filters.query.isNotBlank() ->
            "Nothing matches \"${filters.query.trim()}\". Try a different term or clear your filters."

        filters.activeFilterCount > 0 -> "No orders match the current filters."
        filters.tab == WorklistTab.RESULTS -> "Results appear here once a study has been read."
        else -> "Every order on this worklist has been actioned."
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L

        /** Sentence case for the chips; the enum's own label is the all-caps badge form. */
        fun Priority.displayLabel(): String = when (this) {
            Priority.STAT -> "STAT"
            Priority.URGENT -> "Urgent"
            Priority.ROUTINE -> "Routine"
        }
    }
}
