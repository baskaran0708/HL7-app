package com.livemedica.helix.feature.worklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixFilterChip
import com.livemedica.helix.core.designsystem.component.HelixFilterChipRow
import com.livemedica.helix.core.designsystem.component.HelixSearchBar
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.priorityMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.core.ui.dataOrNull
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.feature.worklist.components.OrderActionSheet
import com.livemedica.helix.feature.worklist.components.OrderCard
import com.livemedica.helix.feature.worklist.components.WorklistFilterSheet
import com.livemedica.helix.feature.worklist.components.WorklistTabRow

/**
 * The ORM worklist: every requested exam, triaged.
 *
 * Search and the priority chips sit above the list and outside the state host, because the fastest
 * route out of an empty result is the control that emptied it. Long-pressing a card opens the
 * contextual actions rather than burying them behind a per-card overflow button, which keeps the
 * card itself free of chrome.
 */
@Composable
fun WorklistScreen(
    actions: HelixNavActions,
    viewModel: WorklistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val studyToOpen by viewModel.studyToOpen.collectAsStateWithLifecycle()

    var isFilterSheetOpen by rememberSaveable { mutableStateOf(false) }
    var actionOrderId by rememberSaveable { mutableStateOf<String?>(null) }

    // The study id has to be resolved from the order before we can navigate, so the jump happens
    // here once the ViewModel reports it rather than inside the sheet's click handler.
    LaunchedEffect(studyToOpen) {
        studyToOpen?.let { studyId ->
            actions.openStudy(studyId)
            viewModel.consumeStudyRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        WorklistTabRow(selected = filters.tab, onSelect = viewModel::selectTab)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xl, end = Spacing.sm, top = Spacing.md, bottom = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            HelixSearchBar(
                query = filters.query,
                onQueryChange = viewModel::setQuery,
                onClear = viewModel::clearQuery,
                modifier = Modifier.weight(1f),
            )
            FilterButton(
                activeCount = filters.activeFilterCount,
                onClick = { isFilterSheetOpen = true },
            )
        }

        HelixFilterChipRow(
            modifier = Modifier.padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.md),
        ) {
            filters.priorityFilters.forEach { option ->
                HelixFilterChip(
                    label = option.label,
                    count = option.count,
                    selected = filters.priority == option.priority,
                    tone = option.priority?.takeIf { it != Priority.ROUTINE }?.let { priorityMeta(it).color },
                    onClick = { viewModel.selectPriority(option.priority) },
                )
            }
        }

        HelixStateHost(
            state = state,
            modifier = Modifier.weight(1f),
            onRetry = viewModel::refresh,
            emptyTitle = when (filters.tab) {
                WorklistTab.ORDERS -> "No pending orders"
                WorklistTab.RESULTS -> "No results ready"
            },
        ) { orders ->
            OrderList(
                orders = orders,
                onOpen = { order -> actions.openOrder(order.id) },
                onShowActions = { order -> actionOrderId = order.id },
            )
        }
    }

    if (isFilterSheetOpen) {
        WorklistFilterSheet(
            filters = filters,
            onSelectPriority = viewModel::selectPriority,
            onSelectStatus = viewModel::selectStatus,
            onToggleModality = viewModel::toggleModality,
            onReset = viewModel::resetFilters,
            onDismiss = { isFilterSheetOpen = false },
        )
    }

    val actionOrder = state.dataOrNull()?.firstOrNull { it.id == actionOrderId }
    if (actionOrder != null) {
        OrderActionSheet(
            order = actionOrder,
            onOpenPatient = {
                actionOrderId = null
                actions.openPatient(actionOrder.patientId)
            },
            onOpenOrder = {
                actionOrderId = null
                actions.openOrder(actionOrder.id)
            },
            onOpenStudy = {
                actionOrderId = null
                viewModel.requestStudy(actionOrder.id)
            },
            onOpenReport = {
                actionOrderId = null
                actionOrder.reportId?.let(actions.openReport)
            },
            onDismiss = { actionOrderId = null },
        )
    }
}

@Composable
private fun OrderList(
    orders: List<Order>,
    onOpen: (Order) -> Unit,
    onShowActions: (Order) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.xl,
            end = Spacing.xl,
            top = Spacing.xs,
            bottom = Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = orders, key = { it.id }) { order ->
            OrderCard(
                order = order,
                onOpen = { onOpen(order) },
                onShowActions = { onShowActions(order) },
            )
        }
    }
}

/** Filter entry point. The count is both a badge and part of the button's spoken description. */
@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    val colors = HelixTheme.colors
    Box {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = if (activeCount == 0) {
                    "Filter orders"
                } else {
                    "Filter orders, $activeCount active"
                },
                tint = if (activeCount > 0) colors.primary else colors.text,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
        if (activeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.xs)
                    .size(BADGE_SIZE)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = activeCount.toString(),
                    style = HelixTheme.typography.overline,
                    color = if (colors.isDark) colors.background else colors.surface,
                )
            }
        }
    }
}

private val BADGE_SIZE = 15.dp
