package com.livemedica.helix.feature.worklist

import androidx.compose.runtime.Immutable
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.OrderStatus
import com.livemedica.helix.domain.model.Priority

/**
 * Worklist filter state.
 *
 * Like Schedule, the controls live outside the screen's `UiState` so a clinician who has filtered
 * themselves into an empty list can always get back out without a reload.
 */

/**
 * The two halves of a radiology worklist.
 *
 * [ORDERS] is the ORM inbox — everything requested. [RESULTS] is the same list narrowed to orders
 * that already have a report attached, which is the reading queue rather than the request queue.
 */
enum class WorklistTab(val label: String) {
    ORDERS("Orders"),
    RESULTS("Results"),
}

/** A priority quick-filter with its live count. A null [priority] is the "All" chip. */
@Immutable
data class PriorityFilter(
    val priority: Priority?,
    val label: String,
    val count: Int,
)

@Immutable
data class WorklistFilterState(
    val tab: WorklistTab = WorklistTab.ORDERS,
    val query: String = "",
    val priority: Priority? = null,
    val status: OrderStatus? = null,
    val modalities: Set<Modality> = emptySet(),
    val priorityFilters: List<PriorityFilter> = emptyList(),
    val visibleCount: Int = 0,
) {
    /** Drives the badge on the filter button — the count of *narrowing* choices currently applied. */
    val activeFilterCount: Int
        get() = (if (priority != null) 1 else 0) +
            (if (status != null) 1 else 0) +
            (if (modalities.isEmpty()) 0 else 1)
}

/** Status options offered by the filter sheet; null is "All". */
val WorklistStatusFilters: List<OrderStatus?> = listOf(
    null,
    OrderStatus.PENDING,
    OrderStatus.IN_PROGRESS,
    OrderStatus.COMPLETED,
)

/** The five modalities a radiology worklist filters by day to day. */
val WorklistModalityFilters: List<Modality> = listOf(
    Modality.CT,
    Modality.MR,
    Modality.XR,
    Modality.US,
    Modality.MG,
)
