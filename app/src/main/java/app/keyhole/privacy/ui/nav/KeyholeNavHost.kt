package app.keyhole.privacy.ui.nav

import androidx.compose.foundation.layout.Row
import app.keyhole.privacy.ui.isWide
import app.keyhole.privacy.ui.components.KeyholeNavRail
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import app.keyhole.privacy.ui.checklist.ChecklistScreen
import app.keyhole.privacy.ui.checklist.ChecklistViewModel
import app.keyhole.privacy.ui.found.FoundCameraScreen
import app.keyhole.privacy.ui.found.HelpViewModel
import app.keyhole.privacy.ui.found.HelpNumbersSheet
import app.keyhole.privacy.ui.more.AboutScreen
import app.keyhole.privacy.ui.more.AppearanceScreen
import app.keyhole.privacy.ui.more.AppearanceViewModel
import app.keyhole.privacy.ui.more.HowItWorksScreen
import app.keyhole.privacy.ui.more.LanguageSheet
import app.keyhole.privacy.ui.more.LicenceTextScreen
import app.keyhole.privacy.ui.more.LicencesScreen
import app.keyhole.privacy.ui.more.LimitsScreen
import app.keyhole.privacy.ui.more.MarkedKnownScreen
import app.keyhole.privacy.ui.more.MarkedKnownViewModel
import app.keyhole.privacy.ui.more.PrivacyScreen
import app.keyhole.privacy.ui.more.PrivacyViewModel
import app.keyhole.privacy.ui.more.SweepStepsScreen
import app.keyhole.privacy.ui.welcome.WelcomeScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.keyhole.privacy.ui.report.FindingScreen
import app.keyhole.privacy.ui.report.PhotoScreen
import app.keyhole.privacy.ui.report.ReportScreen
import app.keyhole.privacy.ui.report.ReportViewModel
import app.keyhole.privacy.ui.lens.LensScreen
import app.keyhole.privacy.ui.lens.LensViewModel
import app.keyhole.privacy.ui.magnet.MagnetScreen
import app.keyhole.privacy.ui.magnet.MagnetViewModel
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
import app.keyhole.privacy.ui.components.LocalToasts
import app.keyhole.privacy.ui.components.ToastHost
import app.keyhole.privacy.ui.components.NavItem
import app.keyhole.privacy.ui.home.HomeScreen
import app.keyhole.privacy.ui.home.HomeViewModel
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.more.MoreScreen
import app.keyhole.privacy.ui.more.MoreViewModel
import app.keyhole.privacy.ui.reports.ReportsScreen
import app.keyhole.privacy.ui.reports.ReportsViewModel
import app.keyhole.privacy.ui.theme.NavMotion

/** Where step [check] of a sweep opens: the sweep screen for scans, the check's own screen for hands-on ones. */
private fun stepRoute(place: Place, check: Check): Any = when (check) {
    Check.LENS, Check.INFRARED -> LensRoute(place, sweep = true, start = check)
    Check.MAGNET -> MagnetRoute(place, sweep = true)
    Check.CHECKLIST -> ChecklistRoute(place, sweep = true)
    else -> SweepRoute(place, check)
}

/** Where a scan's results open. */
private fun resultsRoute(check: Check, place: Place, sweep: Boolean): Any =
    if (check == Check.WIFI) WifiResultsRoute(place, sweep) else NearbyResultsRoute(check, place, sweep)

/** The three tabs, in nav order. */
private val TABS = listOf<Any>(HomeRoute, ReportsRoute, MoreRoute)

/**
 * The whole app: the three tabs with the bottom nav, and every pushed screen without it. Tabs fade through,
 * pushes use shared axis X, predictive back is on (manifest).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyholeNavHost(container: AppContainer, settings: Settings, open: OpenFromOutside? = null, onOpened: () -> Unit = {}, nav: NavHostController = rememberNavController()) {
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val tab = TABS.indexOfFirst { t -> destination?.hierarchy?.any { it.hasRoute(t::class) } == true }
    val items = listOf(
        NavItem(stringResource(R.string.tab_check), R.drawable.ic_radar),
        NavItem(stringResource(R.string.tab_reports), R.drawable.ic_reports),
        NavItem(stringResource(R.string.tab_more), R.drawable.ic_more),
    )
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    // The end of a sweep (or "See report" on Home): the visit becomes a saved report, which opens over Home.
    // Saved off the main thread, opened on it; a second tap while saving does nothing (one visit, one report).
    val finish: () -> Unit = {
        if (!saving) {
            saving = true
            scope.launch(Dispatchers.Main.immediate) {
                try {
                    val id = withContext(Dispatchers.IO) { container.visit.save() }
                    nav.navigate(ReportRoute(id)) { popUpTo<HomeRoute>() }
                } finally {
                    saving = false
                }
            }
        }
    }
    val toasts = LocalToasts.current
    // One More item's screen: pushed on a phone, in the right pane of More on a wide window (T04).
    val MoreItemScreen: @Composable (MoreItem, () -> Unit) -> Unit = { item, back ->
        when (item) {
            MoreItem.APPEARANCE -> AppearanceScreen(viewModel { AppearanceViewModel(container.settings) }, onBack = back)
            MoreItem.SWEEP_STEPS -> SweepStepsScreen(viewModel { AppearanceViewModel(container.settings) }, onBack = back)
            // Opened as a sheet from More; a route only for completeness (deep links, tests).
            MoreItem.LANGUAGE -> LanguageSheet(onDismiss = back)
            MoreItem.MARKED_KNOWN -> MarkedKnownScreen(viewModel { MarkedKnownViewModel(container.reports) }, onBack = back)
            MoreItem.HOW_IT_WORKS -> HowItWorksScreen(onBack = back)
            MoreItem.LIMITS -> LimitsScreen(onBack = back, onChecklist = { nav.navigate(CheckRoute(Check.CHECKLIST, settings.place)) })
            MoreItem.PRIVACY -> PrivacyScreen(viewModel { PrivacyViewModel(container.reports, container.notes) }, onBack = back)
            MoreItem.ABOUT -> AboutScreen(onBack = back, onPrivacy = { nav.navigate(MoreItemRoute(MoreItem.PRIVACY)) }, onLicences = { nav.navigate(MoreItemRoute(MoreItem.LICENCES)) })
            MoreItem.LICENCES -> LicencesScreen(onBack = back, onOpen = { nav.navigate(LicenceRoute(it.title, it.file)) })
            MoreItem.FOUND_CAMERA -> FoundCameraScreen(viewModel { HelpViewModel(container.helpNumbers, container.settings) }, onBack = back)
        }
    }
    // A launcher shortcut. Held until Welcome is done (a first run starts there), then opened over Home.
    LaunchedEffect(open, settings.onboarded) {
        if (open == null || !settings.onboarded) return@LaunchedEffect
        when (open) {
            OpenFromOutside.Sweep -> nav.navigate(SweepRoute(settings.place)) { popUpTo<HomeRoute>(); launchSingleTop = true }
            is OpenFromOutside.OneCheck -> nav.navigate(CheckRoute(open.check, settings.place)) { popUpTo<HomeRoute>(); launchSingleTop = true }
            OpenFromOutside.Reports -> nav.navigate(ReportsRoute) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        onOpened()
    }
    // Landscape: the system's buttons and the camera cutout sit at the sides; keep clear of them (as Burrow).
    // The keyboard: everything above it; the nav bar steps aside while it is up.
    val keyboard = WindowInsets.isImeVisible
    // Wide (from 840 dp): the rail instead of the bottom nav, beside every screen but Welcome and the viewfinder.
    val wide = isWide()
    val fullScreen = destination?.hierarchy?.any { it.hasRoute(WelcomeRoute::class) || it.hasRoute(LensRoute::class) } == true
    val rail = wide && !fullScreen && destination != null
    val goTab: (Int) -> Unit = { i ->
        nav.navigate(TABS[i]) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal))
            .imePadding(),
    ) {
    if (rail) KeyholeNavRail(items, tab, onSelect = goTab)
    Column(Modifier.weight(1f).fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(
                nav, startDestination = if (settings.onboarded) HomeRoute else WelcomeRoute,
                enterTransition = NavMotion.push, exitTransition = NavMotion.pushExit,
                popEnterTransition = NavMotion.pop, popExitTransition = NavMotion.popExit,
            ) {
                composable<WelcomeRoute> {
                    WelcomeScreen(onStart = {
                        container.appScope.launch { container.settings.setOnboarded() }
                        nav.navigate(HomeRoute) { popUpTo<WelcomeRoute> { inclusive = true } }
                    })
                }
                composable<HomeRoute>(enterTransition = NavMotion.tabEnter, exitTransition = NavMotion.tabExit, popEnterTransition = NavMotion.tabEnter, popExitTransition = NavMotion.tabExit) {
                    val vm = viewModel { HomeViewModel(container.settings, container.reports, container.notes) }
                    HomeScreen(
                        vm,
                        onStart = { nav.navigate(SweepRoute(it)) },
                        onCheck = { check, place -> nav.navigate(CheckRoute(check, place)) },
                        onHelp = { nav.navigate(MoreItemRoute(MoreItem.HOW_IT_WORKS)) },
                        onReport = { nav.navigate(ReportRoute(it)) },
                        onSeeVisit = finish,
                    )
                }
                composable<ReportsRoute>(enterTransition = NavMotion.tabEnter, exitTransition = NavMotion.tabExit, popEnterTransition = NavMotion.tabEnter, popExitTransition = NavMotion.tabExit) {
                    val vm = viewModel { ReportsViewModel(container.reports, container.appScope) }
                    if (wide) {
                        // T02: the list on the left, the picked report (the newest at first) on the right.
                        val reports by vm.state.collectAsStateWithLifecycle()
                        var picked by rememberSaveable { mutableStateOf<Long?>(null) }
                        val shown = picked?.takeIf { id -> reports.reports.any { it.id == id } } ?: reports.reports.firstOrNull()?.id
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) { ReportsScreen(vm, onOpen = { picked = it }) }
                            Box(Modifier.weight(1f)) {
                                if (shown != null) {
                                    val report = viewModel(key = "report-$shown") { ReportViewModel(shown, container.reports) }
                                    ReportScreen(
                                        report, onBack = { picked = null },
                                        onFinding = { nav.navigate(FindingRoute(shown, it)) },
                                        onPhoto = { nav.navigate(PhotoRoute(it)) },
                                        onFoundSomething = { nav.navigate(FoundCameraRoute(shown)) },
                                    )
                                }
                            }
                        }
                    } else {
                        ReportsScreen(vm, onOpen = { nav.navigate(ReportRoute(it)) })
                    }
                }
                composable<MoreRoute>(enterTransition = NavMotion.tabEnter, exitTransition = NavMotion.tabExit, popEnterTransition = NavMotion.tabEnter, popExitTransition = NavMotion.tabExit) {
                    val vm = viewModel { MoreViewModel(container.settings, container.reports, container.helpNumbers) }
                    val help = viewModel { HelpViewModel(container.helpNumbers, container.settings) }
                    val helpState by help.state.collectAsStateWithLifecycle()
                    var helpOpen by rememberSaveable { mutableStateOf(false) }
                    var languageOpen by rememberSaveable { mutableStateOf(false) }
                    if (wide) {
                        // T04: More's list on the left, the picked item (Appearance at first) on the right.
                        var picked by rememberSaveable { mutableStateOf(MoreItem.APPEARANCE) }
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) { MoreScreen(vm, onOpen = { if (it == MoreItem.FOUND_CAMERA) nav.navigate(MoreItemRoute(it)) else picked = it }, onHelpNumbers = { helpOpen = true }, onLanguage = { languageOpen = true }) }
                            Box(Modifier.weight(1f)) { MoreItemScreen(picked) { picked = MoreItem.APPEARANCE } }
                        }
                    } else {
                        MoreScreen(vm, onOpen = { nav.navigate(MoreItemRoute(it)) }, onHelpNumbers = { helpOpen = true }, onLanguage = { languageOpen = true })
                    }
                    if (helpOpen) HelpNumbersSheet(helpState, onDismiss = { helpOpen = false }, onChoose = { help.choose(it); helpOpen = false })
                    if (languageOpen) LanguageSheet(onDismiss = { languageOpen = false })
                }
                composable<SweepRoute> { e ->
                    val route = e.toRoute<SweepRoute>()
                    val vm = viewModel { SweepViewModel(route.place, route.at, null, container.settings, container.wifi, container.hotspots, container.bluetooth, container.notes) }
                    SweepScreen(
                        vm, onClose = nav::popBackStack,
                        onStepDone = { check ->
                            val to = if (check in SweepViewModel.HANDS_ON) stepRoute(route.place, check) else resultsRoute(check, route.place, sweep = true)
                            nav.navigate(to) { popUpTo<SweepRoute> { inclusive = true } }
                        },
                        onFinished = finish,
                        onDrop = {
                            container.visit.drop()
                            nav.popBackStack()
                        },
                    )
                }
                composable<CheckRoute> { e ->
                    val route = e.toRoute<CheckRoute>()
                    when (route.check) {
                        Check.WIFI, Check.HOTSPOTS, Check.BLUETOOTH -> {
                            val vm = viewModel { SweepViewModel(route.place, null, route.check, container.settings, container.wifi, container.hotspots, container.bluetooth, container.notes) }
                            SweepScreen(
                                vm, onClose = nav::popBackStack,
                                onStepDone = { check -> nav.navigate(resultsRoute(check, route.place, sweep = false)) { popUpTo<CheckRoute> { inclusive = true } } },
                                onFinished = nav::popBackStack,
                            )
                        }
                        Check.LENS, Check.INFRARED -> {
                            val vm = viewModel { LensViewModel(route.place, false, route.check, container.settings, container.notes, container.photos) }
                            LensScreen(vm, onBack = nav::popBackStack, onNext = { nav.popBackStack() })
                        }
                        Check.MAGNET -> {
                            val vm = viewModel { MagnetViewModel(route.place, false, container.magnet, container.settings, container.notes) }
                            MagnetScreen(vm, onBack = nav::popBackStack, onNext = { nav.popBackStack() })
                        }
                        Check.CHECKLIST -> {
                            val vm = viewModel { ChecklistViewModel(route.place, false, container.settings, container.notes) }
                            ChecklistScreen(vm, onBack = nav::popBackStack, onNext = { finish() })
                        }
                    }
                }
                composable<ChecklistRoute> { e ->
                    val route = e.toRoute<ChecklistRoute>()
                    val vm = viewModel { ChecklistViewModel(route.place, route.sweep, container.settings, container.notes) }
                    ChecklistScreen(vm, onBack = nav::popBackStack, onNext = { next ->
                        if (next == null) finish() else nav.navigate(stepRoute(route.place, next)) { popUpTo<ChecklistRoute> { inclusive = true } }
                    })
                }
                composable<ReportRoute> { e ->
                    val route = e.toRoute<ReportRoute>()
                    val vm = viewModel { ReportViewModel(route.id, container.reports) }
                    ReportScreen(
                        vm, onBack = nav::popBackStack,
                        onFinding = { nav.navigate(FindingRoute(route.id, it)) },
                        onPhoto = { nav.navigate(PhotoRoute(it)) },
                        onFoundSomething = { nav.navigate(FoundCameraRoute(route.id)) },
                    )
                }
                composable<FindingRoute> { e ->
                    val route = e.toRoute<FindingRoute>()
                    val vm = viewModel { ReportViewModel(route.reportId, container.reports) }
                    FindingScreen(vm, route.findingId, onBack = nav::popBackStack, onFoundIt = { nav.navigate(FoundCameraRoute(route.reportId)) })
                }
                composable<PhotoRoute> { e ->
                    PhotoScreen(e.toRoute<PhotoRoute>().path, onBack = nav::popBackStack)
                }
                composable<FoundCameraRoute> { e ->
                    val route = e.toRoute<FoundCameraRoute>()
                    val vm = viewModel { HelpViewModel(container.helpNumbers, container.settings, container.reports, route.reportId.takeIf { it >= 0 }) }
                    FoundCameraScreen(vm, onBack = nav::popBackStack)
                }
                composable<LensRoute> { e ->
                    val route = e.toRoute<LensRoute>()
                    val vm = viewModel { LensViewModel(route.place, route.sweep, route.start, container.settings, container.notes, container.photos) }
                    LensScreen(vm, onBack = nav::popBackStack, onNext = { next ->
                        if (next == null) { if (route.sweep) finish() else nav.popBackStack() } else nav.navigate(stepRoute(route.place, next)) { popUpTo<LensRoute> { inclusive = true } }
                    })
                }
                composable<MagnetRoute> { e ->
                    val route = e.toRoute<MagnetRoute>()
                    val vm = viewModel { MagnetViewModel(route.place, route.sweep, container.magnet, container.settings, container.notes) }
                    MagnetScreen(vm, onBack = nav::popBackStack, onNext = { next ->
                        if (next == null) { if (route.sweep) finish() else nav.popBackStack() } else nav.navigate(stepRoute(route.place, next)) { popUpTo<MagnetRoute> { inclusive = true } }
                    })
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
                            if (next == null) { if (route.sweep) finish() else nav.popBackStack() }
                            else nav.navigate(stepRoute(route.place, next)) { popUpTo<NearbyResultsRoute> { inclusive = true } }
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
                            if (next == null) { if (route.sweep) finish() else nav.popBackStack() }
                            else nav.navigate(stepRoute(route.place, next)) { popUpTo<WifiResultsRoute> { inclusive = true } }
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
                        onFoundIt = { nav.navigate(FoundCameraRoute()) },
                        onFindLens = { nav.navigate(CheckRoute(Check.LENS, route.place)) },
                    )
                }
                composable<MoreItemRoute> { e ->
                    MoreItemScreen(e.toRoute<MoreItemRoute>().item) { nav.popBackStack() }
                }
                composable<LicenceRoute> { e ->
                    val route = e.toRoute<LicenceRoute>()
                    LicenceTextScreen(route.title, route.file, onBack = nav::popBackStack)
                }
            }
            // Over every screen; above the nav bar on a tab (the bar sits under this box), else above the dock.
            ToastHost(toasts, overNav = tab >= 0 && !rail)
        }
        if (tab >= 0 && !keyboard && !rail) KeyholeNavBar(items, tab, onSelect = goTab)
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
