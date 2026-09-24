package com.livemedica.helix.feature.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.feature.schedule.ScheduleDayLoad
import java.time.LocalDate

/**
 * Week view: relative load per day rather than exact times.
 *
 * The bar is scaled against the busiest day of the week, which is what makes an over-booked
 * Wednesday obvious without reading a single number. Tapping a day drops back into Day view for it.
 */
@Composable
fun ScheduleWeekGrid(
    week: List<ScheduleDayLoad>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val busiest = week.maxOfOrNull { it.appointmentCount }?.coerceAtLeast(1) ?: 1

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Spacing.xl,
            end = Spacing.xl,
            top = Spacing.xs,
            bottom = Spacing.xxl,
        ),
    ) {
        item(key = "week-overline") {
            HelixOverline(
                text = week.firstOrNull()?.let { "Week of ${it.dateLabel}" } ?: "Week",
                modifier = Modifier.padding(bottom = Spacing.md),
            )
        }
        items(week, key = { it.date.toString() }) { day ->
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onSelectDate(day.date) })
                        .heightIn(min = Dimens.touchTargetMin)
                        .padding(vertical = 13.dp, horizontal = Spacing.xs)
                        .semantics {
                            contentDescription = "${day.weekdayLabel} ${day.dateLabel}, " +
                                "${day.appointmentCount} appointments" +
                                if (day.statCount > 0) ", ${day.statCount} STAT" else ""
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Column(Modifier.width(WEEK_LABEL_WIDTH)) {
                        Text(
                            text = day.weekdayLabel,
                            style = HelixTheme.typography.label,
                            color = if (day.date == selectedDate) colors.text else colors.textDim,
                        )
                        Text(
                            text = day.dateLabel,
                            style = HelixTheme.typography.monoSmall,
                            color = colors.textMute,
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(LOAD_BAR_HEIGHT)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(colors.elevated),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(day.appointmentCount / busiest.toFloat())
                                .height(LOAD_BAR_HEIGHT)
                                .clip(RoundedCornerShape(Radius.sm))
                                .background(colors.primary),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        // STAT is spelled out rather than shown as a red pip: on a week overview the
                        // count is what matters, and colour alone would not survive greyscale.
                        if (day.statCount > 0) {
                            Text(
                                text = "${day.statCount} STAT",
                                style = HelixTheme.typography.monoSmall,
                                color = colors.critical,
                            )
                        }
                        Text(
                            text = day.appointmentCount.toString(),
                            style = HelixTheme.typography.monoSmall,
                            color = colors.text,
                        )
                    }
                }
                HelixHairline()
            }
        }
    }
}

private val WEEK_LABEL_WIDTH = Spacing.xxxl + Spacing.sm
private val LOAD_BAR_HEIGHT = Spacing.sm - 2.dp
