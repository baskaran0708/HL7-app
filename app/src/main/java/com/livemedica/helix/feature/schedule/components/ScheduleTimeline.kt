package com.livemedica.helix.feature.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.ModalityTag
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.modalityColor
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.AppointmentStatus
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.feature.schedule.ScheduleTimeline
import com.livemedica.helix.feature.schedule.TimelineSlot

/**
 * The day timeline: an hour ruler with appointments positioned by wall-clock time.
 *
 * Conflicting bookings arrive from the ViewModel already assigned to lanes, so this file only
 * translates minutes and lane indices into offsets. The "now" line is drawn last and above
 * everything, because on a live schedule it is the single most-read element on the screen.
 */
@Composable
fun ScheduleDayTimeline(
    timeline: ScheduleTimeline,
    nowMinutes: Int?,
    onOpen: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val hourCount = timeline.endHour - timeline.startHour
    val contentHeight = HOUR_HEIGHT * hourCount

    Row(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = Spacing.xxl),
    ) {
        Column(Modifier.width(HOUR_GUTTER_WIDTH)) {
            repeat(hourCount) { index ->
                Box(Modifier.height(HOUR_HEIGHT)) {
                    Text(
                        text = "%02d".format(timeline.startHour + index),
                        style = HelixTheme.typography.monoSmall,
                        color = colors.textMute,
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(contentHeight),
        ) {
            val laneArea = maxWidth

            repeat(hourCount + 1) { index ->
                Box(
                    Modifier
                        .offset(y = HOUR_HEIGHT * index)
                        .fillMaxWidth()
                        .height(Dimens.hairline)
                        .background(colors.hairline),
                )
            }

            timeline.slots.forEach { slot ->
                val laneWidth = (laneArea - LANE_GAP * (slot.laneCount - 1)) / slot.laneCount
                TimelineBlock(
                    slot = slot,
                    onOpen = onOpen,
                    modifier = Modifier
                        .offset(
                            x = (laneWidth + LANE_GAP) * slot.lane,
                            y = HOUR_HEIGHT * (slot.minuteOffset / MINUTES_PER_HOUR),
                        )
                        .width(laneWidth)
                        .height(slotHeight(slot.appointment.durationMinutes)),
                )
            }

            if (nowMinutes != null) {
                val offset = HOUR_HEIGHT * ((nowMinutes - timeline.startHour * 60) / MINUTES_PER_HOUR)
                if (offset >= 0.dp && offset <= contentHeight) {
                    NowIndicator(Modifier.offset(y = offset - NOW_DOT_SIZE / 2))
                }
            }
        }
    }
}

/**
 * One appointment block.
 *
 * Detail is shed in two directions. Sideways, as lanes multiply: at two lanes the procedure line
 * goes, at three the modality tag does. Vertically, as the block gets shorter — and a block gets
 * shorter because the exam is shorter, which is not something the layout is allowed to argue with.
 * So the content is measured against the height the clock gives it and drops to a single
 * `time · name` line rather than half-painting a second one; a name cut through the middle of a
 * glyph is worse than no second line at all.
 *
 * The patient name is the one thing that is never dropped, and nothing that is hidden here is lost:
 * modality, procedure, priority, status and room all stay in the block's content description, so a
 * screen reader still reads the full booking off the shortest block on the day.
 */
@Composable
private fun TimelineBlock(
    slot: TimelineSlot,
    onOpen: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val appointment = slot.appointment
    val isStat = appointment.priority == Priority.STAT
    val accent = if (isStat) colors.critical else modalityColor(appointment.modality)
    val shape = RoundedCornerShape(Radius.sm)
    val isDim = appointment.status in DIMMED_STATUSES
    val isTight = slot.laneCount >= 2
    val isCrowded = slot.laneCount >= 3
    // A STAT badge and a modality tag do not both fit beside the time in a shared lane. The badge
    // wins: the red accent rule already carries "urgent", but only the badge says it in words.
    val showModalityTag = !isCrowded && !(isStat && isTight)

    Row(
        modifier = modifier
            .alpha(if (isDim) DIMMED_ALPHA else 1f)
            .clip(shape)
            .background(colors.surface)
            .border(Dimens.hairline, colors.hairline, shape)
            .clickable(onClick = { onOpen(appointment) })
            .semantics {
                contentDescription = "${ClinicalFormat.time(appointment.start)}, ${appointment.patientName}, " +
                    "${appointment.modality.label}, ${appointment.procedure}, " +
                    "${appointment.priority.label}, ${appointment.status.label}, ${appointment.room}"
            },
    ) {
        Box(
            Modifier
                .width(Dimens.timelineAccentWidth + 1.dp)
                .fillMaxHeight()
                .background(accent),
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val verticalPadding = if (isTight) BLOCK_PADDING_TIGHT else Spacing.sm
            val available = maxHeight - verticalPadding * 2

            // Line heights come from the type scale rather than from guessed dp, so the decision
            // still holds when the clinician has scaled the system font up.
            val typography = HelixTheme.typography
            val density = LocalDensity.current
            val metaLine = with(density) { typography.monoSmall.lineHeight.toDp() }
            val nameLine = with(density) { typography.label.lineHeight.toDp() }
            val procedureLine = with(density) { typography.caption.lineHeight.toDp() }
            val headerLine = metaLine + TAG_VERTICAL_PADDING * 2

            val fitsName = available >= headerLine + ROW_GAP + nameLine
            val fitsProcedure = available >= headerLine + ROW_GAP + nameLine + ROW_GAP + procedureLine

            // Lane count alone does not predict width: two lanes on a narrow phone leave less room
            // than three on a tablet. Measure instead, and drop a tag rather than let it ellipsize —
            // a clipped "ST." is worse than no badge, because the red accent rule already says
            // "urgent" and the content description still says "STAT" in full.
            val fitsPriorityBadge = maxWidth >= PRIORITY_BADGE_MIN_WIDTH
            val fitsModalityTag = maxWidth >= MODALITY_TAG_MIN_WIDTH

            Column(
                modifier = Modifier.padding(
                    horizontal = if (isTight) BLOCK_PADDING_TIGHT + 1.dp else Spacing.sm + 2.dp,
                    vertical = verticalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(ROW_GAP),
            ) {
                if (fitsName) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        BlockTime(appointment)
                        if (showModalityTag && fitsModalityTag) ModalityTag(appointment.modality)
                        if (isStat && fitsPriorityBadge) HelixPriorityBadge(appointment.priority)
                    }
                    BlockName(appointment)
                    if (!isTight && fitsProcedure) {
                        Text(
                            text = appointment.procedure.substringBefore(" w/"),
                            style = typography.caption,
                            color = colors.textDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    // One clean line: when only a line fits, it is the time and who it is for.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        BlockTime(appointment)
                        BlockName(appointment, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockTime(appointment: Appointment, modifier: Modifier = Modifier) {
    Text(
        text = ClinicalFormat.time(appointment.start),
        style = HelixTheme.typography.monoSmall,
        color = HelixTheme.colors.text,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}

@Composable
private fun BlockName(appointment: Appointment, modifier: Modifier = Modifier) {
    Text(
        text = appointment.patientName,
        style = HelixTheme.typography.label,
        color = HelixTheme.colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Current wall-clock time. Red, hairline-thin, and the only thing allowed to cross the gutter. */
@Composable
private fun NowIndicator(modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NOW_DOT_SIZE)
            .semantics { contentDescription = "Current time" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HelixDot(color = colors.critical, size = NOW_DOT_SIZE)
        Box(
            Modifier
                .weight(1f)
                .height(Dimens.hairline)
                .background(colors.critical.copy(alpha = NOW_LINE_ALPHA)),
        )
    }
}

private fun slotHeight(durationMinutes: Int): Dp =
    (HOUR_HEIGHT * (durationMinutes / MINUTES_PER_HOUR) - LANE_GAP).coerceAtLeast(MIN_SLOT_HEIGHT)

/**
 * One hour of wall clock. A component dimension rather than a design token: it is the timeline's
 * scale, and changing it changes how much of a day fits on screen, not the app's spacing rhythm.
 */
private val HOUR_HEIGHT = 88.dp
private val HOUR_GUTTER_WIDTH = Spacing.xxxl
private val LANE_GAP = Spacing.xs
/**
 * A 15-minute block cannot be 48dp tall without lying about when it happens, so the timeline trades
 * the minimum tap target for temporal accuracy. The List view is the accessible equivalent: the same
 * appointments as full-height rows.
 */
private val MIN_SLOT_HEIGHT = Dimens.touchTargetMin - Spacing.xs
private val NOW_DOT_SIZE = Spacing.sm

/** Inset a block uses once it shares its hour with another booking. */
private val BLOCK_PADDING_TIGHT = Spacing.sm - 2.dp
/** Gap between the block's own lines; also what the fit calculation budgets for. */
private val ROW_GAP = 2.dp
/** [com.livemedica.helix.core.designsystem.component.HelixTag]'s own vertical padding. */
private val TAG_VERTICAL_PADDING = 3.dp

/** Below these widths the tag would ellipsize, so it is dropped instead. */
private val PRIORITY_BADGE_MIN_WIDTH = 116.dp
private val MODALITY_TAG_MIN_WIDTH = 132.dp

private const val MINUTES_PER_HOUR = 60f
private const val DIMMED_ALPHA = 0.5f
private const val NOW_LINE_ALPHA = 0.55f

private val DIMMED_STATUSES = setOf(
    AppointmentStatus.COMPLETED,
    AppointmentStatus.CANCELLED,
    AppointmentStatus.NO_SHOW,
)
