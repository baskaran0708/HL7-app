package com.livemedica.helix.feature.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixFilterChip
import com.livemedica.helix.core.designsystem.component.HelixFilterChipRow
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.component.HelixSegmentedControl
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.modalityColor
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.feature.schedule.ScheduleDayCell
import com.livemedica.helix.feature.schedule.ScheduleFilterState
import com.livemedica.helix.feature.schedule.ScheduleViewMode
import java.time.LocalDate

/**
 * The sticky context header: what day, whose site, how much work, and how to look at it.
 *
 * It is rendered outside the screen's state host on purpose — a clinician who lands on an empty or
 * failed day must still be able to change the date or drop a filter without a retry round trip.
 */
@Composable
fun ScheduleHeader(
    filters: ScheduleFilterState,
    onSelectDate: (LocalDate) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onSelectViewMode: (ScheduleViewMode) -> Unit,
    onToggleModality: (Modality) -> Unit,
    onOpenFacilitySheet: () -> Unit,
    onOpenModalitySheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = ClinicalFormat.fullDate(filters.selectedDate),
                    style = HelixTheme.typography.title,
                    color = colors.text,
                )
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = "${filters.visibleCount} appointments",
                        style = HelixTheme.typography.monoSmall,
                        color = colors.textDim,
                    )
                    HelixDot(color = colors.textMute, size = 3.dp)
                    Text(
                        text = filters.facilityLabel,
                        style = HelixTheme.typography.monoSmall,
                        color = colors.textDim,
                    )
                }
            }

            IconButton(onClick = onOpenFacilitySheet) {
                Icon(
                    imageVector = Icons.Outlined.Apartment,
                    contentDescription = "Choose facility, currently ${filters.facilityLabel}",
                    tint = if (filters.facilityId != null) colors.primary else colors.text,
                    modifier = Modifier.size(Dimens.iconMd),
                )
            }
            IconButton(onClick = onOpenModalitySheet) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = if (filters.modalities.isEmpty()) {
                        "Filter by modality"
                    } else {
                        "Filter by modality, ${filters.modalities.size} active"
                    },
                    tint = if (filters.modalities.isNotEmpty()) colors.primary else colors.text,
                    modifier = Modifier.size(Dimens.iconMd),
                )
            }
        }

        WeekNavigation(
            weekStart = filters.week.firstOrNull()?.date,
            onShiftWeek = onShiftWeek,
            modifier = Modifier.padding(top = Spacing.sm),
        )

        ScheduleDateStrip(
            days = filters.week,
            selectedDate = filters.selectedDate,
            onSelectDate = onSelectDate,
        )

        if (filters.modalityCounts.isNotEmpty()) {
            HelixFilterChipRow(modifier = Modifier.padding(top = Spacing.md)) {
                filters.modalityCounts.forEach { entry ->
                    HelixFilterChip(
                        label = entry.modality.code,
                        count = entry.count,
                        selected = entry.modality in filters.modalities,
                        tone = modalityColor(entry.modality),
                        onClick = { onToggleModality(entry.modality) },
                    )
                }
            }
        }

        HelixSegmentedControl(
            options = ScheduleViewMode.entries,
            selected = filters.viewMode,
            label = ScheduleViewMode::label,
            onSelect = onSelectViewMode,
            modifier = Modifier.padding(top = Spacing.md),
        )
    }
}

/** Week stepper. The strip only ever shows one ISO week, so this is how the user leaves it. */
@Composable
private fun WeekNavigation(
    weekStart: LocalDate?,
    onShiftWeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onShiftWeek(-1) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous week",
                tint = HelixTheme.colors.textDim,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
        HelixOverline(text = weekStart?.let { "Week of ${ClinicalFormat.shortDate(it)}" } ?: "Week")
        IconButton(onClick = { onShiftWeek(1) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next week",
                tint = HelixTheme.colors.textDim,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
    }
}

/**
 * The horizontal date selector.
 *
 * Seven equal columns so the week reads as a single unit. The load dot under each number is the
 * cheapest possible preview of "is that day busy" — the design uses it instead of a number badge,
 * which would compete with the date itself.
 */
@Composable
fun ScheduleDateStrip(
    days: List<ScheduleDayCell>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        days.forEach { day ->
            val isSelected = day.date == selectedDate
            val shape = RoundedCornerShape(Radius.md)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .then(
                        when {
                            isSelected -> Modifier.background(colors.text)
                            day.isToday -> Modifier.border(Dimens.hairline, colors.primary, shape)
                            else -> Modifier
                        },
                    )
                    .clickable(onClick = { onSelectDate(day.date) })
                    .heightIn(min = Dimens.touchTargetMin)
                    .padding(vertical = Spacing.sm)
                    .semantics {
                        selected = isSelected
                        contentDescription = "${day.weekdayInitial} ${day.dayOfMonth}, " +
                            "${day.appointmentCount} appointments"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = day.weekdayInitial,
                    style = HelixTheme.typography.overline,
                    color = if (isSelected) colors.background else colors.textMute,
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = HelixTheme.typography.monoLarge.copy(fontSize = HelixTheme.typography.headline.fontSize),
                    color = if (isSelected) colors.background else colors.text,
                )
                HelixDot(
                    color = when {
                        day.appointmentCount == 0 -> colors.background.copy(alpha = 0f)
                        isSelected -> colors.background
                        else -> colors.textMute
                    },
                    size = Spacing.xs,
                )
            }
        }
    }
}
