package com.livemedica.helix.feature.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.feature.schedule.components.ScheduleDayList
import com.livemedica.helix.feature.schedule.components.ScheduleDayTimeline
import com.livemedica.helix.feature.schedule.components.ScheduleFacilitySheet
import com.livemedica.helix.feature.schedule.components.ScheduleHeader
import com.livemedica.helix.feature.schedule.components.ScheduleModalitySheet
import com.livemedica.helix.feature.schedule.components.ScheduleWeekGrid
import java.time.LocalDate

/**
 * The day's schedule, in three readings of the same data.
 *
 * Day is the default because it answers "what is happening now"; Week answers "how bad is this
 * week"; List answers "what still needs doing", grouped by lifecycle. Nothing is fetched per view —
 * the ViewModel hands all three the same day, so switching is instant.
 */
@Composable
fun ScheduleScreen(
    actions: HelixNavActions,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val nowMinutes by viewModel.nowMinutes.collectAsStateWithLifecycle()

    var isFacilitySheetOpen by rememberSaveable { mutableStateOf(false) }
    var isModalitySheetOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ScheduleHeader(
            filters = filters,
            onSelectDate = viewModel::selectDate,
            onShiftWeek = viewModel::shiftWeek,
            onSelectViewMode = viewModel::selectViewMode,
            onToggleModality = viewModel::toggleModality,
            onOpenFacilitySheet = { isFacilitySheetOpen = true },
            onOpenModalitySheet = { isModalitySheetOpen = true },
        )

        HelixStateHost(
            state = state,
            modifier = Modifier.weight(1f),
            onRetry = viewModel::refresh,
            emptyTitle = "No appointments",
            emptyBody = "Nothing is booked for this day.",
        ) { model ->
            ScheduleContent(
                viewMode = filters.viewMode,
                model = model,
                nowMinutes = nowMinutes,
                onOpenAppointment = { appointment -> actions.openAppointment(appointment.id) },
                onSelectDate = viewModel::selectDate,
            )
        }
    }

    if (isFacilitySheetOpen) {
        ScheduleFacilitySheet(
            facilities = filters.facilities,
            selectedFacilityId = filters.facilityId,
            onSelectFacility = viewModel::selectFacility,
            onDismiss = { isFacilitySheetOpen = false },
        )
    }

    if (isModalitySheetOpen) {
        ScheduleModalitySheet(
            selectedModalities = filters.modalities,
            onToggleModality = viewModel::toggleModality,
            onReset = viewModel::clearModalities,
            onDismiss = { isModalitySheetOpen = false },
        )
    }
}

@Composable
private fun ScheduleContent(
    viewMode: ScheduleViewMode,
    model: ScheduleUiModel,
    nowMinutes: Int?,
    onOpenAppointment: (Appointment) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    when (viewMode) {
        ScheduleViewMode.DAY -> ScheduleDayTimeline(
            timeline = model.timeline,
            nowMinutes = nowMinutes,
            onOpen = onOpenAppointment,
        )

        ScheduleViewMode.WEEK -> ScheduleWeekGrid(
            week = model.week,
            selectedDate = model.date,
            onSelectDate = onSelectDate,
        )

        ScheduleViewMode.LIST -> ScheduleDayList(
            groups = model.groups,
            onOpen = onOpenAppointment,
        )
    }
}
