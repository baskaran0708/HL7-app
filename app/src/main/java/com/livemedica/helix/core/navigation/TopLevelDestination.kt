package com.livemedica.helix.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The five primary destinations, in the order the design lays them out.
 *
 * [badgeSource] declares where a tab's badge count comes from, so the badge logic lives with the
 * destination rather than being re-derived inside the bottom bar.
 */
enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val graph: HelixRoute,
    val root: HelixRoute,
    val badgeSource: BadgeSource = BadgeSource.NONE,
) {
    TODAY("Today", Icons.Outlined.Home, HelixRoute.TodayGraph, HelixRoute.Today),
    SCHEDULE("Schedule", Icons.Outlined.CalendarToday, HelixRoute.ScheduleGraph, HelixRoute.Schedule),
    WORKLIST("Worklist", Icons.AutoMirrored.Outlined.Assignment, HelixRoute.WorklistGraph, HelixRoute.Worklist),
    RESULTS("Results", Icons.Outlined.Description, HelixRoute.ResultsGraph, HelixRoute.Results, BadgeSource.UNSIGNED_REPORTS),
    MORE("More", Icons.Outlined.Menu, HelixRoute.MoreGraph, HelixRoute.More),
    ;

    enum class BadgeSource { NONE, UNSIGNED_REPORTS }
}
