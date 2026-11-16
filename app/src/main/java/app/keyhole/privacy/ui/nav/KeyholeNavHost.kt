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
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.ui.nearby.FindItScreen
import app.keyhole.privacy.ui.nearby.FindItViewModel
import app.keyhole.privacy.ui.nearby.NearbyOthersScreen
import app.keyhole.privacy.ui.nearby.NearbyOthersViewModel
import app.keyhole.privacy.ui.nearby.NearbyResultsScreen
import app.keyhole.privacy.ui.nearby.NearbyResultsViewModel
import app.keyhole.privacy.ui.sweep.SweepScreen
import app.keyhole.privacy.ui.sweep.SweepViewModel
import app.keyhole.privacy.ui.wifi.DeviceScreen
import app.keyhole.privacy.ui.wifi.DeviceViewModel
import app.keyhole.privacy.ui.wifi.WifiOthersScreen
import app.keyhole.privacy.ui.wifi.WifiOthersViewModel
import app.keyhole.privacy.ui.wifi.WifiResultsScreen
import app.keyhole.privacy.ui.wifi.WifiResultsViewModel
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

/** The checks that scan and then show a list of results. */
private val SCANS = setOf(Check.WIFI, Check.HOTSPOTS, Check.BLUETOOTH)

/** Where a scan's results open. */
private fun resultsRoute(check: Check, place: Place, sweep: Boolean): Any =
    if (check == Check.WIFI) WifiResultsRoute(place, sweep) else NearbyResultsRoute(check, place, sweep)

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
                    val vm = viewModel { SweepViewModel(route.place, route.at, null, container.settings, container.wifi, container.hotspots, container.bluetooth) }
                    SweepScreen(
                        vm, onClose = nav::popBackStack,
                        onStepDone = { check -> nav.navigate(resultsRoute(check, route.place, sweep = true)) { popUpTo<SweepRoute> { inclusive = true } } },
                        onFinished = nav::popBackStack,
                    )
                }
                composable<CheckRoute> { e ->
                    val route = e.toRoute<CheckRoute>()
                    if (route.check in SCANS) {
                        val vm = viewModel { SweepViewModel(route.place, null, route.check, container.settings, container.wifi, container.hotspots, container.bluetooth) }
                        SweepScreen(
                            vm, onClose = nav::popBackStack,
                            onStepDone = { check -> nav.navigate(resultsRoute(check, route.place, sweep = false)) { popUpTo<CheckRoute> { inclusive = true } } },
                            onFinished = nav::popBackStack,
                        )
                    } else {
                        StandInScreen(stringResource(route.check.label), stringResource(route.place.label), onBack = nav::popBackStack)
                    }
                }
                composable<NearbyResultsRoute> { e ->
                    val route = e.toRoute<NearbyResultsRoute>()
                    val vm = viewModel { NearbyResultsViewModel(route.place, route.sweep, container.settings, container.nearby(route.check)) }
                    NearbyResultsScreen(
                        vm, onBack = nav::popBackStack,
                        onScanAgain = {
                            val again: Any = if (route.sweep) SweepRoute(route.place, route.check) else CheckRoute(route.check, route.place)
                            nav.navigate(again) { popUpTo<NearbyResultsRoute> { inclusive = true } }
                        },
                        onFind = { nav.navigate(FindItRoute(route.check, it)) },
                        onOthers = { nav.navigate(NearbyOthersRoute(route.check)) },
                        onNext = { next ->
                            if (next == null) nav.popBackStack()
                            else nav.navigate(SweepRoute(route.place, next)) { popUpTo<NearbyResultsRoute> { inclusive = true } }
                        },
                    )
                }
                composable<NearbyOthersRoute> { e ->
                    val route = e.toRoute<NearbyOthersRoute>()
                    val vm = viewModel { NearbyOthersViewModel(container.nearby(route.check)) }
                    NearbyOthersScreen(vm, onBack = nav::popBackStack, onFind = { nav.navigate(FindItRoute(route.check, it)) })
                }
                composable<FindItRoute> { e ->
                    val route = e.toRoute<FindItRoute>()
                    val vm = viewModel { FindItViewModel(route.id, container.nearby(route.check)) }
                    FindItScreen(vm, onBack = nav::popBackStack)
                }
                composable<WifiResultsRoute> { e ->
                    val route = e.toRoute<WifiResultsRoute>()
                    val vm = viewModel { WifiResultsViewModel(route.place, route.sweep, container.settings, container.wifi) }
                    WifiResultsScreen(
                        vm, onBack = nav::popBackStack,
                        onScanAgain = {
                            val again: Any = if (route.sweep) SweepRoute(route.place, Check.WIFI) else CheckRoute(Check.WIFI, route.place)
                            nav.navigate(again) { popUpTo<WifiResultsRoute> { inclusive = true } }
                        },
                        onDevice = { nav.navigate(DeviceRoute(it, route.place)) },
                        onOthers = { nav.navigate(WifiOthersRoute(route.place)) },
                        onNext = { next ->
                            if (next == null) nav.popBackStack()
                            else nav.navigate(SweepRoute(route.place, next)) { popUpTo<WifiResultsRoute> { inclusive = true } }
                        },
                    )
                }
                composable<WifiOthersRoute> { e ->
                    val route = e.toRoute<WifiOthersRoute>()
                    val vm = viewModel { WifiOthersViewModel(container.wifi) }
                    WifiOthersScreen(vm, onBack = nav::popBackStack, onDevice = { nav.navigate(DeviceRoute(it, route.place)) })
                }
                composable<DeviceRoute> { e ->
                    val route = e.toRoute<DeviceRoute>()
                    val vm = viewModel { DeviceViewModel(route.ip, container.wifi) }
                    DeviceScreen(
                        vm, onBack = nav::popBackStack,
                        onFoundIt = { nav.navigate(MoreItemRoute(MoreItem.FOUND_CAMERA)) },
                        onFindLens = { nav.navigate(CheckRoute(Check.LENS, route.place)) },
                    )
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
