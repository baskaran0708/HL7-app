package com.livemedica.helix.feature.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixFilterChip
import com.livemedica.helix.core.designsystem.component.HelixFilterChipRow
import com.livemedica.helix.core.designsystem.component.HelixMetric
import com.livemedica.helix.core.designsystem.component.HelixMetricPair
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixEmptyState
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.feature.results.components.ReportCard

/**
 * Results (ORU) — the radiologist's inbox.
 *
 * The screen answers "what do I owe?" before it lists anything: the two headline figures, then the
 * category chips, then the cards. Reports are the app's only mutable clinical object, so this list
 * is driven by an observed flow and re-slices itself the moment a report is signed anywhere else.
 */
@Composable
fun ResultsScreen(
    actions: HelixNavActions,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HelixStateHost(
        state = state,
        onRetry = viewModel::refresh,
        emptyTitle = "No results yet",
        emptyBody = "Reports appear here as studies are read and dictated.",
    ) { model ->
        ResultsContent(
            model = model,
            onSelectCategory = viewModel::selectCategory,
            onOpenReport = actions.openReport,
        )
    }
}

@Composable
private fun ResultsContent(
    model: ResultsUiModel,
    onSelectCategory: (ResultsCategory) -> Unit,
    onOpenReport: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ResultsHeader(model = model)
        CategoryRow(
            selected = model.category,
            counts = model.counts,
            onSelect = onSelectCategory,
        )

        if (model.reports.isEmpty()) {
            // Boxed with a weight so the empty state centres in the space left below the chips
            // rather than claiming the whole viewport and pushing them off screen.
            Box(Modifier.weight(1f)) {
                HelixEmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = model.category.emptyTitle,
                    body = model.category.emptyBody,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = Spacing.xl,
                    end = Spacing.xl,
                    top = Spacing.xs,
                    bottom = Spacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md - 2.dp),
            ) {
                // Keyed by report id so signing one card does not recompose or re-animate the rest.
                items(items = model.reports, key = { it.id }) { report ->
                    ReportCard(report = report, onOpen = { onOpenReport(report.id) })
                }
            }
        }
    }
}

@Composable
private fun ResultsHeader(model: ResultsUiModel) {
    val colors = HelixTheme.colors
    HelixMetricPair(
        modifier = Modifier.padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.md, bottom = Spacing.lg),
        start = {
            HelixMetric(
                value = model.awaitingSignOff.toString(),
                label = "Awaiting sign-off",
                note = if (model.statAwaiting > 0) "${model.statAwaiting} STAT" else "None urgent",
                noteColor = if (model.statAwaiting > 0) colors.critical else null,
                valueColor = if (model.awaitingSignOff > 0) colors.text else colors.textDim,
            )
        },
        end = {
            HelixMetric(
                value = (model.counts[ResultsCategory.NEW] ?: 0).toString(),
                label = "Results today",
                note = "${model.counts[ResultsCategory.SIGNED] ?: 0} signed",
            )
        },
    )
}

@Composable
private fun CategoryRow(
    selected: ResultsCategory,
    counts: Map<ResultsCategory, Int>,
    onSelect: (ResultsCategory) -> Unit,
) {
    val colors = HelixTheme.colors
    HelixFilterChipRow(modifier = Modifier.padding(horizontal = Spacing.xl)) {
        ResultsCategory.entries.forEach { category ->
            HelixFilterChip(
                label = category.label,
                count = counts[category] ?: 0,
                selected = category == selected,
                onClick = { onSelect(category) },
                // STAT is the one chip that carries a signal colour, and only when it is the
                // active filter — an always-red chip would desensitise the clinician to red.
                tone = if (category == ResultsCategory.STAT) colors.critical else null,
            )
        }
    }
}

/** Empty copy is per category: "nothing to sign" and "nothing signed yet" mean very different things. */
private val ResultsCategory.emptyTitle: String
    get() = when (this) {
        ResultsCategory.NEW -> "No new results today"
        ResultsCategory.STAT -> "No STAT results"
        ResultsCategory.PENDING_SIGN_OFF -> "No reports awaiting review"
        ResultsCategory.SIGNED -> "No signed reports"
    }

private val ResultsCategory.emptyBody: String
    get() = when (this) {
        ResultsCategory.NEW -> "Results dictated today will appear here."
        ResultsCategory.STAT -> "Nothing urgent is waiting on you."
        ResultsCategory.PENDING_SIGN_OFF -> "Every report assigned to you has been signed."
        ResultsCategory.SIGNED -> "Reports you sign will be listed here."
    }
