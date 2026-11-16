package app.keyhole.privacy.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.keyhole.privacy.AppContainer
import app.keyhole.privacy.R
import app.keyhole.privacy.data.Settings
import app.keyhole.privacy.ui.components.KeyholeNavBar
import app.keyhole.privacy.ui.components.NavItem
import app.keyhole.privacy.ui.home.HomeScreen
import app.keyhole.privacy.ui.home.HomeViewModel
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.more.MoreScreen
import app.keyhole.privacy.ui.more.MoreViewModel
import app.keyhole.privacy.ui.reports.ReportsScreen
import app.keyhole.privacy.ui.reports.ReportsViewModel
import app.keyhole.privacy.ui.theme.NavMotion

/** The three tabs, in nav order. */
private val TABS = listOf<Any>(HomeRoute, ReportsRoute, MoreRoute)

/**
 * The whole app: the three tabs with the bottom nav, and every pushed screen without it. Tabs fade through,
 * pushes use shared axis X, predictive back is on (manifest).
 */
@Composable
fun KeyholeNavHost(container: AppContainer, settings: Settings, nav: NavHostController = rememberNavController()) {
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val tab = TABS.indexOfFirst { t -> destination?.hierarchy?.any { it.hasRoute(t::class) } == true }
    val items = listOf(
        NavItem(stringResource(R.string.tab_check), R.drawable.ic_radar),
        NavItem(stringResource(R.string.tab_reports), R.drawable.ic_reports),
        NavItem(stringResource(R.string.tab_more), R.drawable.ic_more),
    )
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(
                nav, startDestination = HomeRoute,
                enterTransition = NavMotion.push, exitTransition = NavMotion.pushExit,
                popEnterTransition = NavMotion.pop, popExitTransition = NavMotion.popExit,
            ) {
                composable<HomeRoute>(enterTransition = NavMotion.tabEnter, exitTransition = NavMotion.tabExit, popEnterTransition = NavMotion.tabEnter, popExitTransition = NavMotion.tabExit) {
                    val vm = viewModel { HomeViewModel(container.settings) }
                    HomeScreen(
                        vm,
                        onStart = { nav.navigate(SweepRoute(it)) },
                        onCheck = { check, place -> nav.navigate(CheckRoute(check, place)) },
                        onHelp = { nav.navigate(MoreItemRoute(MoreItem.HOW_IT_WORKS)) },
                    )
                }
                composable<ReportsRoute>(enterTransition = NavMotion.tabEnter, exitTransition = NavMotion.tabExit, popEnterTransition = NavMotion.tabEnter, popExitTransition = NavMotion.tabExit) {
                    val vm = viewModel { ReportsViewModel() }
                    ReportsScreen(vm)
                }
                composable<MoreRoute>(enterTransition = NavMotion.tabEnter, exitTransition = NavMotion.tabExit, popEnterTransition = NavMotion.tabEnter, popExitTransition = NavMotion.tabExit) {
                    val vm = viewModel { MoreViewModel(container.settings) }
                    MoreScreen(vm, onOpen = { nav.navigate(MoreItemRoute(it)) })
                }
                composable<SweepRoute> { e ->
                    val route = e.toRoute<SweepRoute>()
                    StandInScreen(stringResource(R.string.check_this_room), stringResource(route.place.label), onBack = nav::popBackStack)
                }
                composable<CheckRoute> { e ->
                    val route = e.toRoute<CheckRoute>()
                    StandInScreen(stringResource(route.check.label), stringResource(route.place.label), onBack = nav::popBackStack)
                }
                composable<MoreItemRoute> { e ->
                    val route = e.toRoute<MoreItemRoute>()
                    StandInScreen(stringResource(route.item.title), null, onBack = nav::popBackStack)
                }
            }
        }
        if (tab >= 0) {
            KeyholeNavBar(items, tab, onSelect = { i ->
                nav.navigate(TABS[i]) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
        }
    }
}

val MoreItem.title: Int
    get() = when (this) {
        MoreItem.APPEARANCE -> R.string.appearance
        MoreItem.LANGUAGE -> R.string.language
        MoreItem.SWEEP_STEPS -> R.string.sweep_steps
        MoreItem.MARKED_KNOWN -> R.string.marked_as_known
        MoreItem.HOW_IT_WORKS -> R.string.how_keyhole_works
        MoreItem.LIMITS -> R.string.what_keyhole_cant_find
        MoreItem.PRIVACY -> R.string.privacy
        MoreItem.ABOUT -> R.string.about
        MoreItem.LICENCES -> R.string.licences
        MoreItem.FOUND_CAMERA -> R.string.if_you_find_a_camera
    }
