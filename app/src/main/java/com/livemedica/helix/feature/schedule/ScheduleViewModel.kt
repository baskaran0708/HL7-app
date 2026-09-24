package com.livemedica.helix.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.common.dataOrNull
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Schedule state.
 *
 * Two rules shape this class. First, the facility filter is pushed down to the repository (it is a
 * query parameter the backend will honour) while the modality filter is applied here over the day's
 * already-fetched list — toggling a modality chip must never cost a round trip. Second, every
 * derived view (timeline lanes, list groups, chip counts) is computed here, so the composables stay
 * pure renderers of state.
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ScheduleUiModel>>(UiState.Loading)
    val state: StateFlow<UiState<ScheduleUiModel>> = _state.asStateFlow()

    private val _filters = MutableStateFlow(ScheduleFilterState(selectedDate = LocalDate.now()))
    val filters: StateFlow<ScheduleFilterState> = _filters.asStateFlow()

    /**
     * Minutes since midnight for the timeline's "now" line, or null when the selected day is not
     * today. Driven by a ticker rather than read at composition time so the line actually moves
     * during a shift instead of freezing wherever the screen happened to open.
     */
    val nowMinutes: StateFlow<Int?> = combine(_filters, minuteTicker()) { filters, minuteOfDay ->
        minuteOfDay.takeIf { filters.selectedDate == LocalDate.now() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

    /** The day as fetched, before the modality filter — kept so chip toggles re-derive locally. */
    private var dayAppointments: List<Appointment> = emptyList()
    private var weekLoads: List<ScheduleDayLoad> = emptyList()

    /** Cancelled on every new load so a slow week fetch can't overwrite a day the user has moved on from. */
    private var loadJob: Job? = null

    init {
        loadFacilities()
        load()
    }

    fun refresh() = load(showLoading = false)

    fun selectDate(date: LocalDate) {
        if (date == _filters.value.selectedDate) return
        _filters.update { it.copy(selectedDate = date) }
        load()
    }

    /** Moves the date strip a whole week, keeping the same weekday selected. */
    fun shiftWeek(weeks: Long) {
        _filters.update { it.copy(selectedDate = it.selectedDate.plusWeeks(weeks)) }
        load()
    }

    fun selectViewMode(mode: ScheduleViewMode) {
        _filters.update { it.copy(viewMode = mode) }
        // Week view survives an empty day — it is about the week, not the date — so what counts as
        // "empty" changes with the mode and the state has to be re-derived.
        publish()
    }

    fun selectFacility(facilityId: String?) {
        if (facilityId == _filters.value.facilityId) return
        _filters.update { it.copy(facilityId = facilityId) }
        load()
    }

    fun toggleModality(modality: Modality) {
        _filters.update { filters ->
            val next = filters.modalities.toMutableSet()
            if (!next.remove(modality)) next.add(modality)
            filters.copy(modalities = next)
        }
        publish()
    }

    fun clearModalities() {
        _filters.update { it.copy(modalities = emptySet()) }
        publish()
    }

    fun resetFilters() {
        val hadFacility = _filters.value.facilityId != null
        _filters.update { it.copy(facilityId = null, modalities = emptySet()) }
        if (hadFacility) load() else publish()
    }

    /**
     * Facilities are static reference data, so a failure here is not worth blocking the screen for:
     * the selector simply falls back to "All sites" and the day still loads.
     */
    private fun loadFacilities() {
        viewModelScope.launch {
            val facilities = appointmentRepository.getFacilities().dataOrNull().orEmpty()
            _filters.update { it.copy(facilities = facilities) }
        }
    }

    private fun load(showLoading: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showLoading) _state.value = UiState.Loading
            val filters = _filters.value

            weekLoads = loadWeek(filters.selectedDate, filters.facilityId)
            _filters.update { current -> current.copy(week = weekLoads.map { it.toCell() }) }

            when (val result = appointmentRepository.getAppointments(filters.selectedDate, filters.facilityId)) {
                is AppResult.Success -> {
                    dayAppointments = result.data
                    publish()
                }

                is AppResult.Offline -> {
                    dayAppointments = result.cached.orEmpty()
                    // With nothing cached there is no day to render, so the offline state owns the
                    // whole screen rather than showing a banner above an empty timeline.
                    _state.value = UiState.Offline(
                        dayAppointments
                            .takeIf { it.isNotEmpty() }
                            ?.let { buildModel(filters.selectedDate, visibleAppointments()) },
                    )
                }

                is AppResult.Failure -> {
                    dayAppointments = emptyList()
                    _state.value = UiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Loads the surrounding week in parallel. The counts feed both the date strip's load dots and
     * the Week view, so one fetch serves both and they can never disagree.
     */
    private suspend fun loadWeek(date: LocalDate, facilityId: String?): List<ScheduleDayLoad> {
        val monday = date.with(DayOfWeek.MONDAY)
        return coroutineScope {
            (0 until DAYS_IN_WEEK).map { offset ->
                val day = monday.plusDays(offset.toLong())
                async {
                    val appointments = appointmentRepository.getAppointments(day, facilityId).dataOrNull().orEmpty()
                    ScheduleDayLoad(
                        date = day,
                        weekdayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                        dateLabel = day.format(dayMonthFormatter),
                        appointmentCount = appointments.size,
                        statCount = appointments.count { it.priority == Priority.STAT },
                    )
                }
            }.awaitAll()
        }
    }

    /** Re-derives the whole screen from the last fetch. Every local filter change routes through here. */
    private fun publish() {
        val filters = _filters.value
        val visible = visibleAppointments()

        _filters.update { current ->
            current.copy(
                modalityCounts = dayAppointments
                    .groupingBy(Appointment::modality)
                    .eachCount()
                    .map { (modality, count) -> ModalityCount(modality, count) }
                    .sortedWith(compareByDescending<ModalityCount> { it.count }.thenBy { it.modality.code }),
                visibleCount = visible.size,
            )
        }

        val hasContent = if (filters.viewMode == ScheduleViewMode.WEEK) {
            weekLoads.any { it.appointmentCount > 0 }
        } else {
            visible.isNotEmpty()
        }

        _state.value = if (hasContent) {
            UiState.Success(buildModel(filters.selectedDate, visible))
        } else {
            UiState.Empty(emptyMessageFor(filters))
        }
    }

    private fun visibleAppointments(): List<Appointment> {
        val modalities = _filters.value.modalities
        return dayAppointments
            .filter { modalities.isEmpty() || it.modality in modalities }
            .sortedBy { it.start }
    }

    private fun emptyMessageFor(filters: ScheduleFilterState): String = when {
        filters.isFiltered -> "No studies match the current filters for this day."
        filters.viewMode == ScheduleViewMode.WEEK -> "Nothing is booked this week."
        else -> "Nothing is booked for this day."
    }

    private fun buildModel(date: LocalDate, appointments: List<Appointment>) = ScheduleUiModel(
        date = date,
        appointments = appointments,
        timeline = ScheduleTimelineBuilder.build(appointments),
        groups = ScheduleGroup.entries
            .map { group -> ScheduleGroupSection(group, appointments.filter { it.status.scheduleGroup() == group }) }
            .filter { it.appointments.isNotEmpty() },
        week = weekLoads,
    )

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val DAYS_IN_WEEK = 7
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val MINUTE_MS = 60_000L

        val dayMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.US)

        fun LocalTime.minuteOfDay(): Int = hour * MINUTES_PER_HOUR + minute

        /** Emits the current minute of the day, then once a minute for as long as anyone is looking. */
        fun minuteTicker() = flow {
            while (true) {
                emit(LocalTime.now().minuteOfDay())
                delay(MINUTE_MS)
            }
        }

        fun ScheduleDayLoad.toCell() = ScheduleDayCell(
            date = date,
            weekdayInitial = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.US),
            dayOfMonth = date.dayOfMonth,
            appointmentCount = appointmentCount,
            isToday = date == LocalDate.now(),
        )
    }
}
