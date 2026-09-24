package com.livemedica.helix.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import com.livemedica.helix.feature.integration.IntegrationScreen
import com.livemedica.helix.feature.more.MoreScreen
import com.livemedica.helix.feature.notifications.NotificationsScreen
import com.livemedica.helix.feature.patient.PatientDetailScreen
import com.livemedica.helix.feature.profile.ProfileScreen
import com.livemedica.helix.feature.report.ReportDetailScreen
import com.livemedica.helix.feature.results.ResultsScreen
import com.livemedica.helix.feature.schedule.ScheduleScreen
import com.livemedica.helix.feature.search.SearchScreen
import com.livemedica.helix.feature.settings.SettingsScreen
import com.livemedica.helix.feature.study.AppointmentDetailScreen
import com.livemedica.helix.feature.study.OrderDetailScreen
import com.livemedica.helix.feature.study.StudyDetailScreen
import com.livemedica.helix.feature.today.TodayScreen
import com.livemedica.helix.feature.worklist.WorklistScreen

/**
 * The app's navigation graph.
 *
 * Each tab is a nested graph so its back stack survives switching tabs. Detail destinations are
 * declared once at the host level rather than duplicated per tab — a report opened from Today and
 * the same report opened from Results are the same destination, which keeps the back stack honest.
 */
@Composable
fun HelixNavHost(
    navController: NavHostController,
    actions: HelixNavActions,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HelixRoute.TodayGraph,
        modifier = modifier,
    ) {
        navigation<HelixRoute.TodayGraph>(startDestination = HelixRoute.Today) {
            composable<HelixRoute.Today> { TodayScreen(actions) }
        }
        navigation<HelixRoute.ScheduleGraph>(startDestination = HelixRoute.Schedule) {
            composable<HelixRoute.Schedule> { ScheduleScreen(actions) }
        }
        navigation<HelixRoute.WorklistGraph>(startDestination = HelixRoute.Worklist) {
            composable<HelixRoute.Worklist> { WorklistScreen(actions) }
        }
        navigation<HelixRoute.ResultsGraph>(startDestination = HelixRoute.Results) {
            composable<HelixRoute.Results> { ResultsScreen(actions) }
        }
        navigation<HelixRoute.MoreGraph>(startDestination = HelixRoute.More) {
            composable<HelixRoute.More> { MoreScreen(actions) }
        }

        detailDestinations(actions)
        globalDestinations(actions)
    }
}

private fun NavGraphBuilder.detailDestinations(actions: HelixNavActions) {
    composable<HelixRoute.Patient>(
        deepLinks = listOf(navDeepLink { uriPattern = HelixDeepLinks.PATIENT }),
    ) { PatientDetailScreen(actions) }

    composable<HelixRoute.Order>(
        deepLinks = listOf(navDeepLink { uriPattern = HelixDeepLinks.ORDER }),
    ) { OrderDetailScreen(actions) }

    composable<HelixRoute.Study>(
        deepLinks = listOf(navDeepLink { uriPattern = HelixDeepLinks.STUDY }),
    ) { StudyDetailScreen(actions) }

    composable<HelixRoute.Report>(
        deepLinks = listOf(navDeepLink { uriPattern = HelixDeepLinks.REPORT }),
    ) { ReportDetailScreen(actions) }

    composable<HelixRoute.Appointment>(
        deepLinks = listOf(navDeepLink { uriPattern = HelixDeepLinks.APPOINTMENT }),
    ) { AppointmentDetailScreen(actions) }
}

private fun NavGraphBuilder.globalDestinations(actions: HelixNavActions) {
    composable<HelixRoute.Notifications> { NotificationsScreen(actions) }
    composable<HelixRoute.Search> { SearchScreen(actions) }
    composable<HelixRoute.Integration> { IntegrationScreen(actions) }
    composable<HelixRoute.Profile> { ProfileScreen(actions) }
    composable<HelixRoute.Settings> { SettingsScreen(actions) }
}
