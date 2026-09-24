package com.livemedica.helix.feature.schedule

import androidx.compose.runtime.Immutable
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.AppointmentStatus
import com.livemedica.helix.domain.model.Facility
import com.livemedica.helix.domain.model.Modality
import java.time.LocalDate

/**
 * Everything the Schedule screen renders, split into two halves on purpose.
 *
 * [ScheduleFilterState] is the sticky context header — date, facility, modality chips, view mode.
 * It is deliberately *outside* the `UiState` wrapper so the header keeps working while the day's
 * appointments are loading, failing or offline: a clinician must always be able to change the date
 * or drop a filter to get themselves out of an empty screen.
 *
 * [ScheduleUiModel] is the day's content, and is the thing that can legitimately be absent.
 */

/** Day / Week / List, the design's segmented control. */
enum class ScheduleViewMode(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    LIST("List"),
}

/**
 * The bands the List view groups a day into.
 *
 * Ordered by what a clinician needs first: what is happening now, then what is coming, then what is
 * already done, then what fell through. Colour is attached in the composable — this layer stays free
 * of Compose types so it remains testable.
 */
enum class ScheduleGroup(val label: String) {
    IN_PROGRESS("In progress"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    MISSED("No-show"),
}

internal fun AppointmentStatus.scheduleGroup(): ScheduleGroup = when (this) {
    AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_PROGRESS -> ScheduleGroup.IN_PROGRESS
    AppointmentStatus.SCHEDULED -> ScheduleGroup.UPCOMING
    AppointmentStatus.COMPLETED -> ScheduleGroup.COMPLETED
    AppointmentStatus.NO_SHOW, AppointmentStatus.CANCELLED -> ScheduleGroup.MISSED
}

/** One cell of the horizontal date selector. */
@Immutable
data class ScheduleDayCell(
    val date: LocalDate,
    /** Single-letter weekday, as the design's strip shows (`F S S M T W T`). */
    val weekdayInitial: String,
    val dayOfMonth: Int,
    val appointmentCount: Int,
    val isToday: Boolean,
)

/** One row of the Week view: the day's load, and whether any of it is STAT. */
@Immutable
data class ScheduleDayLoad(
    val date: LocalDate,
    val weekdayLabel: String,
    val dateLabel: String,
    val appointmentCount: Int,
    val statCount: Int,
)

/**
 * An appointment placed on the day timeline.
 *
 * [lane] / [laneCount] carry the conflict resolution: two exams booked over each other are given
 * side-by-side lanes rather than being drawn on top of one another, which is the only way a
 * double-booked scanner is visible at a glance.
 */
@Immutable
data class TimelineSlot(
    val appointment: Appointment,
    /** Minutes from the top of the timeline, i.e. from [ScheduleTimeline.startHour]. */
    val minuteOffset: Int,
    val lane: Int,
    val laneCount: Int,
)

@Immutable
data class ScheduleTimeline(
    val startHour: Int,
    val endHour: Int,
    val slots: List<TimelineSlot>,
)

@Immutable
data class ModalityCount(val modality: Modality, val count: Int)

@Immutable
data class ScheduleGroupSection(
    val group: ScheduleGroup,
    val appointments: List<Appointment>,
)

@Immutable
data class ScheduleFilterState(
    val selectedDate: LocalDate,
    val viewMode: ScheduleViewMode = ScheduleViewMode.DAY,
    val week: List<ScheduleDayCell> = emptyList(),
    val facilities: List<Facility> = emptyList(),
    val facilityId: String? = null,
    val modalities: Set<Modality> = emptySet(),
    /** Counts before the modality filter is applied, so the chips always show the full picture. */
    val modalityCounts: List<ModalityCount> = emptyList(),
    val visibleCount: Int = 0,
) {
    val facilityLabel: String
        get() = facilities.firstOrNull { it.id == facilityId }?.shortName ?: ALL_SITES

    val isFiltered: Boolean get() = facilityId != null || modalities.isNotEmpty()

    companion object {
        const val ALL_SITES = "All sites"
    }
}

@Immutable
data class ScheduleUiModel(
    val date: LocalDate,
    val appointments: List<Appointment>,
    val timeline: ScheduleTimeline,
    val groups: List<ScheduleGroupSection>,
    val week: List<ScheduleDayLoad>,
)
