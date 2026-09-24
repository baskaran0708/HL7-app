package com.livemedica.helix.feature.schedule.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixFilterChip
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.modalityColor
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.Facility
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.feature.schedule.ScheduleFilterState

/**
 * The two filter sheets.
 *
 * Both are Material 3 [ModalBottomSheet]s rather than bespoke overlays so they inherit the
 * platform's scrim, drag and predictive-back behaviour — a filter panel is exactly the kind of
 * component where reimplementing gestures buys nothing and loses accessibility.
 */

/** The five modalities a radiology worklist filters by day to day. */
val ScheduleFilterModalities: List<Modality> = listOf(
    Modality.CT,
    Modality.MR,
    Modality.XR,
    Modality.US,
    Modality.MG,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFacilitySheet(
    facilities: List<Facility>,
    selectedFacilityId: String?,
    onSelectFacility: (String?) -> Unit,
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
            HelixOverline(
                text = "Facility",
                modifier = Modifier.padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.md),
            )
            FacilityRow(
                label = ScheduleFilterState.ALL_SITES,
                detail = "Every site you cover",
                isSelected = selectedFacilityId == null,
                onClick = { onSelectFacility(null); onDismiss() },
            )
            facilities.forEach { facility ->
                HelixHairline(Modifier.padding(start = Spacing.xl))
                FacilityRow(
                    label = facility.shortName,
                    detail = facility.name,
                    isSelected = facility.id == selectedFacilityId,
                    onClick = { onSelectFacility(facility.id); onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun FacilityRow(
    label: String,
    detail: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = Dimens.touchTargetMin)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = HelixTheme.typography.bodyLarge,
                color = if (isSelected) colors.text else colors.textDim,
            )
            Text(text = detail, style = HelixTheme.typography.caption, color = colors.textMute)
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleModalitySheet(
    selectedModalities: Set<Modality>,
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
                .padding(horizontal = Spacing.xl),
        ) {
            HelixOverline(text = "Modality", modifier = Modifier.padding(bottom = Spacing.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ScheduleFilterModalities.forEach { modality ->
                    HelixFilterChip(
                        label = modality.code,
                        selected = modality in selectedModalities,
                        tone = modalityColor(modality),
                        onClick = { onToggleModality(modality) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xl, bottom = Spacing.md),
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
                    Text(text = "Apply", style = HelixTheme.typography.label)
                }
            }
        }
    }
}

private const val WIDE_BUTTON_WEIGHT = 2f
