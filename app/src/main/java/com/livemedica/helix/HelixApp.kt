package com.livemedica.helix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowWidthSizeClass
import com.livemedica.helix.core.designsystem.component.HelixBottomNavigation
import com.livemedica.helix.core.designsystem.component.HelixNavigationRail
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.navigation.HelixNavHost
import com.livemedica.helix.core.navigation.TopLevelDestination
import com.livemedica.helix.core.navigation.rememberHelixNavActions
import com.livemedica.helix.core.navigation.topLevelTabOptions

/**
 * App shell: the tab affordance plus the navigation host.
 *
 * Two things are decided here and nowhere else:
 *  - **Where the tabs live.** A bottom bar on phones, a side rail from medium width up. One set of
 *    destinations drives both, so they can never drift apart.
 *  - **When tabs are shown at all.** Detail destinations hide the tab affordance, because the
 *    design treats a report or a patient record as a focused reading surface, not tab content.
 */
@Composable
fun HelixApp(viewModel: HelixAppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val unsignedReports by viewModel.unsignedReportCount.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedTab = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.isWithin(destination) == true
    }

    val actions = rememberHelixNavActions(
        navController = navController,
        selectTab = { navController.selectTab(it) },
    )
    val badgeCounts = mapOf(TopLevelDestination.RESULTS to unsignedReports)
    val useRail = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass !=
        WindowWidthSizeClass.COMPACT

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(HelixTheme.colors.background),
    ) {
        if (useRail && selectedTab != null) {
            HelixNavigationRail(
                destinations = TopLevelDestination.entries,
                selected = selectedTab,
                badgeCounts = badgeCounts,
                onSelect = navController::selectTab,
            )
        }
        Column(Modifier.weight(1f)) {
            HelixNavHost(
                navController = navController,
                actions = actions,
                modifier = Modifier.weight(1f),
            )
            if (!useRail && selectedTab != null) {
                HelixBottomNavigation(
                    destinations = TopLevelDestination.entries,
                    selected = selectedTab,
                    badgeCounts = badgeCounts,
                    onSelect = navController::selectTab,
                )
            }
        }
    }
}

private fun NavHostController.selectTab(destination: TopLevelDestination) {
    navigate(destination.graph) { topLevelTabOptions(this@selectTab) }
}

/**
 * A tab counts as current when its graph appears anywhere in the destination's parent chain, which
 * is what keeps the right tab highlighted while a nested screen inside it is showing.
 */
private fun NavDestination.isWithin(destination: TopLevelDestination): Boolean =
    generateSequence(this) { it.parent }.any { it.hasRoute(destination.graph::class) }
