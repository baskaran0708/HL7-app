package com.livemedica.helix.feature.worklist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixFilterChip
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.component.ModalityTag
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.modalityColor
import com.livemedica.helix.core.designsystem.theme.orderStatusMeta
import com.livemedica.helix.core.designsystem.theme.priorityMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.OrderStatus
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.feature.worklist.WorklistFilterState
import com.livemedica.helix.feature.worklist.WorklistModalityFilters
import com.livemedica.helix.feature.worklist.WorklistStatusFilters

/**
 * The Worklist's two sheets: the filter panel, and the long-press action menu.
 *
 * Both are Material 3 [ModalBottomSheet]s so they inherit the platform's scrim, drag handle and
 * predictive-back handling rather than re-implementing them.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorklistFilterSheet(
    filters: WorklistFilterState,
    onSelectPriority: (Priority?) -> Unit,
    onSelectStatus: (OrderStatus?) -> Unit,
    onToggleModality: (Modality) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = HelixTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        scrimColor = colors.scrim,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl),
        ) {
            FilterGroup(label = "Priority") {
                HelixFilterChip(
                    label = "All",
                    selected = filters.priority == null,
                    onClick = { onSelectPriority(null) },
                )
                Priority.entries.forEach { priority ->
                    val meta = priorityMeta(priority)
                    HelixFilterChip(
                        label = meta.label,
                        selected = filters.priority == priority,
                        tone = if (priority == Priority.ROUTINE) null else meta.color,
                        onClick = { onSelectPriority(priority) },
                    )
                }
            }

            FilterGroup(label = "Status") {
                WorklistStatusFilters.forEach { status ->
                    HelixFilterChip(
                        label = status?.let { orderStatusMeta(it).label } ?: "All",
                        selected = filters.status == status,
                        onClick = { onSelectStatus(status) },
                    )
                }
            }

            FilterGroup(label = "Modality") {
                WorklistModalityFilters.forEach { modality ->
                    HelixFilterChip(
                        label = modality.code,
                        selected = modality in filters.modalities,
                        tone = modalityColor(modality),
                        onClick = { onToggleModality(modality) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg, bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).heightIn(min = Dimens.touchTargetMin),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text),
                ) {
                    Text(text = "Reset", style = HelixTheme.typography.label)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(WIDE_BUTTON_WEIGHT).heightIn(min = Dimens.touchTargetMin),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.text,
                        contentColor = colors.background,
                    ),
                ) {
                    Text(text = "Show ${filters.visibleCount} results", style = HelixTheme.typography.label)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = Spacing.xl)) {
        HelixOverline(text = label, modifier = Modifier.padding(bottom = Spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            content()
        }
    }
}

/**
 * Long-press menu.
 *
 * Only actions that actually navigate somewhere are offered — a menu of disabled or no-op entries
 * costs more trust than it buys. "Open report" is therefore present only when the order has one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderActionSheet(
    order: Order,
    onOpenPatient: () -> Unit,
    onOpenOrder: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenReport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = HelixTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        scrimColor = colors.scrim,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = Spacing.xl)) {
                Text(
                    text = order.patientName,
                    style = HelixTheme.typography.headline,
                    color = colors.text,
                )
                Row(
                    modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    ModalityTag(order.modality)
                    Text(
                        text = order.accessionNumber,
                        style = HelixTheme.typography.monoSmall,
                        color = colors.textDim,
                    )
                }
            }

            ActionRow(Icons.Outlined.Person, "Open patient record", onOpenPatient)
            HelixHairline(Modifier.padding(start = Spacing.xl))
            ActionRow(Icons.Outlined.Assignment, "Open order detail", onOpenOrder)
            HelixHairline(Modifier.padding(start = Spacing.xl))
            ActionRow(Icons.Outlined.Image, "Open study images", onOpenStudy)
            if (order.reportId != null) {
                HelixHairline(Modifier.padding(start = Spacing.xl))
                ActionRow(Icons.Outlined.Description, "Open report", onOpenReport)
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = HelixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = Dimens.touchTargetMin)
            .padding(horizontal = Spacing.xl, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textDim,
            modifier = Modifier.size(Dimens.iconMd),
        )
        Text(
            text = label,
            style = HelixTheme.typography.bodyLarge,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconSm),
        )
    }
}

private const val WIDE_BUTTON_WEIGHT = 2f
