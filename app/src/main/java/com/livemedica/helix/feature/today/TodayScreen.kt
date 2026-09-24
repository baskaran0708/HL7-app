package com.livemedica.helix.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixAvatar
import com.livemedica.helix.core.designsystem.component.HelixCard
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixMetric
import com.livemedica.helix.core.designsystem.component.HelixMetricPair
import com.livemedica.helix.core.designsystem.component.HelixSectionHeader
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.feature.today.components.ActivityLine
import com.livemedica.helix.feature.today.components.CriticalResultCard
import com.livemedica.helix.feature.today.components.PendingReportRow
import com.livemedica.helix.feature.today.components.UpNextAppointment

/**
 * Doctor home.
 *
 * Section order is the design's clinical hierarchy and is deliberate: greeting → the one thing that
 * cannot wait → today's workload → what's next → what needs signing → background activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    actions: HelixNavActions,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        HelixStateHost(
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Nothing scheduled",
            emptyBody = "You have no exams or reports today.",
        ) { model ->
            TodayContent(model = model, actions = actions)
        }
    }
}

@Composable
private fun TodayContent(model: TodayUiModel, actions: HelixNavActions) {
    val colors = HelixTheme.colors
    val dashboard = model.dashboard

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        TodayHeaderActions(
            initials = dashboard.doctor.initials,
            unreadCount = dashboard.unreadNotifications,
            onSearch = actions.openSearch,
            onNotifications = actions.openNotifications,
            onProfile = actions.openProfile,
        )

        Greeting(
            doctorName = dashboard.doctor.displayName,
            date = ClinicalFormat.dayMonth(dashboard.date),
            isOnCall = dashboard.doctor.isOnCall,
            facility = dashboard.doctor.facility.substringAfterLast("· ").trim(),
        )

        dashboard.criticalReport?.let { report ->
            CriticalResultCard(report = report, onOpen = { actions.openReport(report.id) })
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            HelixSectionHeader(title = "Today")
            HelixMetricPair(
                start = {
                    HelixMetric(
                        value = dashboard.examsToday.toString(),
                        label = "Exams remaining",
                        note = "${dashboard.pendingReads} pending reads",
                        onClick = actions.openSchedule,
                    )
                },
                end = {
                    HelixMetric(
                        value = dashboard.unsignedReports.toString(),
                        label = "Reports awaiting sign-off",
                        note = if (dashboard.statCount > 0) "${dashboard.statCount} STAT" else "None urgent",
                        noteColor = if (dashboard.statCount > 0) colors.critical else null,
                        valueColor = if (dashboard.unsignedReports > 0) colors.text else colors.textDim,
                        onClick = actions.openResults,
                    )
                },
            )
        }

        dashboard.upNext.firstOrNull()?.let { appointment ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                HelixSectionHeader(
                    title = "Up next",
                    actionLabel = "Schedule",
                    onActionClick = actions.openSchedule,
                )
                UpNextAppointment(
                    appointment = appointment,
                    allergy = null,
                    onOpen = { actions.openAppointment(appointment.id) },
                )
            }
        }

        if (model.awaitingSignOff.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                HelixSectionHeader(
                    title = "Awaiting sign-off",
                    actionLabel = "All ${model.awaitingSignOff.size}",
                    onActionClick = actions.openResults,
                )
                HelixCard(contentPadding = 0.dp) {
                    model.awaitingSignOff.take(AWAITING_PREVIEW_COUNT).forEachIndexed { index, report ->
                        if (index > 0) HelixHairline(Modifier.padding(start = Spacing.lg))
                        PendingReportRow(report = report, onOpen = { actions.openReport(report.id) })
                    }
                }
            }
        }

        if (model.recentActivity.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                HelixSectionHeader(
                    title = "Recent activity",
                    actionLabel = if (dashboard.unreadNotifications > 0) {
                        "${dashboard.unreadNotifications} new"
                    } else {
                        "All"
                    },
                    onActionClick = actions.openNotifications,
                )
                Column {
                    model.recentActivity.forEachIndexed { index, notification ->
                        ActivityLine(
                            notification = notification,
                            isLast = index == model.recentActivity.lastIndex,
                            onOpen = notification.target?.let { target -> { actions.open(target) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHeaderActions(
    initials: String,
    unreadCount: Int,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search patients, orders and reports",
                tint = colors.textDim,
                modifier = Modifier.size(Dimens.iconLg),
            )
        }
        IconButton(onClick = onNotifications) {
            Box {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = if (unreadCount > 0) {
                        "Notifications, $unreadCount unread"
                    } else {
                        "Notifications"
                    },
                    tint = colors.textDim,
                    modifier = Modifier.size(Dimens.iconLg),
                )
                if (unreadCount > 0) {
                    HelixDot(
                        color = colors.critical,
                        size = 7.dp,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
        HelixAvatar(
            initials = initials,
            onClick = onProfile,
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}

@Composable
private fun Greeting(
    doctorName: String,
    date: String,
    isOnCall: Boolean,
    facility: String,
) {
    val colors = HelixTheme.colors
    Column {
        Text(
            // Surname only, as the design does: "Morning, Dr. Chen" reads as a colleague speaking.
            text = "${ClinicalFormat.greeting()},\nDr. ${doctorName.substringAfterLast(' ')}",
            style = HelixTheme.typography.display,
            color = colors.text,
        )
        Row(
            modifier = Modifier.padding(top = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(text = date, style = HelixTheme.typography.monoSmall, color = colors.textDim)
            HelixDot(color = colors.textMute, size = 3.dp)
            HelixStatusChip(
                label = if (isOnCall) "On call" else "Off call",
                color = if (isOnCall) colors.success else colors.textMute,
            )
            HelixDot(color = colors.textMute, size = 3.dp)
            Text(text = facility, style = HelixTheme.typography.monoSmall, color = colors.textDim)
        }
    }
}

private const val AWAITING_PREVIEW_COUNT = 3

/** Kept for the patient-allergy lookup the Up Next row will use once patient context is wired in. */
private fun Patient?.primaryAllergy(): String? =
    this?.allergies?.firstOrNull { !it.equals("NKDA", ignoreCase = true) }
