package com.livemedica.helix.core.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.livemedica.helix.domain.model.NotificationTarget

/**
 * The navigation surface screens are given.
 *
 * Screens receive this rather than a [NavHostController] so they cannot manipulate the back stack
 * directly — every transition the app supports is named here, which keeps navigation reviewable and
 * makes screens trivially previewable with no-op actions.
 */
@Immutable
class HelixNavActions(
    val openPatient: (String) -> Unit,
    val openOrder: (String) -> Unit,
    val openStudy: (String) -> Unit,
    val openReport: (String) -> Unit,
    val openAppointment: (String) -> Unit,
    val openNotifications: () -> Unit,
    val openSearch: () -> Unit,
    val openIntegration: () -> Unit,
    val openProfile: () -> Unit,
    val openSettings: () -> Unit,
    val openSchedule: () -> Unit,
    val openWorklist: () -> Unit,
    val openResults: () -> Unit,
    val navigateBack: () -> Unit,
) {
    fun open(target: NotificationTarget) = when (target) {
        is NotificationTarget.Report -> openReport(target.reportId)
        is NotificationTarget.OrderDetail -> openOrder(target.orderId)
        is NotificationTarget.PatientDetail -> openPatient(target.patientId)
        is NotificationTarget.AppointmentDetail -> openAppointment(target.appointmentId)
        NotificationTarget.Integration -> openIntegration()
    }

    companion object {
        /** For previews and tests: every action is a no-op. */
        val Noop = HelixNavActions(
            openPatient = {}, openOrder = {}, openStudy = {}, openReport = {},
            openAppointment = {}, openNotifications = {}, openSearch = {},
            openIntegration = {}, openProfile = {}, openSettings = {},
            openSchedule = {}, openWorklist = {}, openResults = {}, navigateBack = {},
        )
    }
}

fun rememberHelixNavActions(
    navController: NavHostController,
    selectTab: (TopLevelDestination) -> Unit,
): HelixNavActions = HelixNavActions(
    openPatient = { navController.navigate(HelixRoute.Patient(it)) },
    openOrder = { navController.navigate(HelixRoute.Order(it)) },
    openStudy = { navController.navigate(HelixRoute.Study(it)) },
    openReport = { navController.navigate(HelixRoute.Report(it)) },
    openAppointment = { navController.navigate(HelixRoute.Appointment(it)) },
    openNotifications = { navController.navigate(HelixRoute.Notifications) },
    openSearch = { navController.navigate(HelixRoute.Search) },
    openIntegration = { navController.navigate(HelixRoute.Integration) },
    openProfile = { navController.navigate(HelixRoute.Profile) },
    openSettings = { navController.navigate(HelixRoute.Settings) },
    openSchedule = { selectTab(TopLevelDestination.SCHEDULE) },
    openWorklist = { selectTab(TopLevelDestination.WORKLIST) },
    openResults = { selectTab(TopLevelDestination.RESULTS) },
    navigateBack = { navController.popBackStack() },
)

/**
 * Tab switching semantics: pop back to the tab's root, keep each tab's own stack, and never stack
 * duplicate copies of a tab.
 */
fun NavOptionsBuilder.topLevelTabOptions(navController: NavHostController) {
    popUpTo(navController.graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
