package app.unpeep.privacy.screens

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import app.unpeep.privacy.ui.components.ToastHost
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import app.unpeep.privacy.domain.ChecklistItem
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.unpeep.privacy.R
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.domain.DeviceEvidence
import app.unpeep.privacy.domain.KnownAs
import app.unpeep.privacy.domain.NearbyBlock
import app.unpeep.privacy.domain.NearbyDevice
import app.unpeep.privacy.domain.NearbyKind
import app.unpeep.privacy.domain.NearbySnapshot
import app.unpeep.privacy.domain.NearbyStatus
import app.unpeep.privacy.domain.Nearness
import app.unpeep.privacy.domain.Place
import app.unpeep.privacy.domain.Risk
import app.unpeep.privacy.domain.Sign
import app.unpeep.privacy.domain.Trend
import app.unpeep.privacy.domain.Verdict
import app.unpeep.privacy.domain.WifiDevice
import app.unpeep.privacy.domain.WifiSnapshot
import app.unpeep.privacy.domain.WifiStatus
import app.unpeep.privacy.plural
import app.unpeep.privacy.setUnpeepContent
import app.unpeep.privacy.text
import app.unpeep.privacy.ui.lens.CameraStatus
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import app.unpeep.privacy.ui.checklist.AnswerSheet
import app.unpeep.privacy.ui.checklist.ChecklistContent
import app.unpeep.privacy.ui.checklist.ChecklistState
import app.unpeep.privacy.ui.found.FoundCameraContent
import app.unpeep.privacy.ui.found.HelpNumbersSheet
import app.unpeep.privacy.ui.found.HelpState
import app.unpeep.privacy.ui.report.ReportContent
import app.unpeep.privacy.ui.reports.ReportsContent
import app.unpeep.privacy.ui.reports.ReportsState
import app.unpeep.privacy.ui.lens.LensContent
import app.unpeep.privacy.ui.lens.LensState
import app.unpeep.privacy.ui.magnet.MagnetContent
import app.unpeep.privacy.ui.magnet.MagnetState
import app.unpeep.privacy.ui.nearby.FindItContent
import app.unpeep.privacy.ui.nearby.FindItState
import app.unpeep.privacy.ui.nearby.NearbyOthersContent
import app.unpeep.privacy.ui.nearby.NearbyResultsContent
import app.unpeep.privacy.ui.nearby.NearbyResultsState
import app.unpeep.privacy.ui.sweep.SweepContent
import app.unpeep.privacy.ui.sweep.SweepState
import app.unpeep.privacy.ui.wifi.DeviceContent
import app.unpeep.privacy.ui.wifi.NotACameraSheet
import app.unpeep.privacy.ui.wifi.WifiOthersContent
import app.unpeep.privacy.ui.wifi.WifiResultsContent
import app.unpeep.privacy.ui.wifi.WifiResultsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.unpeep.privacy.ui.theme.UnpeepTheme
import app.unpeep.privacy.ui.theme.ThemeSettings
import app.unpeep.privacy.ui.theme.ThemeMode
import androidx.compose.runtime.mutableStateOf
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Before
import java.io.File

/**
 * Not a test: draws each designed state from made-up data and saves a screenshot named like its PNG in
 * design/screens/, so the build can be put beside the design. Runs only when asked:
 * `./gradlew :app:connectedPlayDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.shots=1
 *  -Pandroid.testInstrumentationRunnerArguments.class=app.unpeep.privacy.screens.DesignShots`, then
 * look in app/build/outputs/connected_android_test_additional_output.
 */
@RunWith(AndroidJUnit4::class)
class DesignShots {
    @get:Rule val rule = createComposeRule()
    private val screen = mutableStateOf<@Composable () -> Unit>({})
    private var dark = mutableStateOf(false)
    private var lightBars = mutableStateOf(true)

    @Before fun onlyWhenAsked() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("shots") != null)
        rule.setContent {
            UnpeepTheme(ThemeSettings(mode = if (dark.value) ThemeMode.DARK else ThemeMode.LIGHT)) {
                // Dark status-bar icons on a light page, light ones on a dark page or the viewfinder, as the app sets them.
                val view = androidx.compose.ui.platform.LocalView.current
                val light = lightBars.value
                androidx.compose.runtime.SideEffect {
                    var context = view.context
                    while (context is android.content.ContextWrapper && context !is android.app.Activity) context = context.baseContext
                    (context as? android.app.Activity)?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = light }
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { screen.value() }
            }
        }
    }

    /** Draws [content], lets it settle, and saves the whole screen (sheets included) as shots/[name].png. */
    private fun shot(name: String, isDark: Boolean = false, fontScale: Float = 1f, lightBars: Boolean = !isDark, content: @Composable () -> Unit) {
        dark.value = isDark
        this.lightBars.value = lightBars
        screen.value = if (fontScale == 1f) content else { ->
            val d = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, fontScale), content = content)
        }
        rule.waitForIdle()
        Thread.sleep(SETTLE_MS)
        rule.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        // AGP pulls this folder into app/build/outputs/connected_android_test_additional_output after the run.
        val out = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val dir = (if (out != null) File(out) else File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "shots")).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screen.value = {}
        rule.waitForIdle()
    }

    private val camera = WifiDevice(DeviceEvidence("192.168.1.23", name = "IPC-3F2A", openPorts = setOf(80, 554, 8899)), Verdict(Risk.LIKELY_CAMERA, listOf(Sign.Onvif, Sign.Rtsp(554), Sign.CameraMaker("Xiongmai")), "Xiongmai"), 0)
    private val web = WifiDevice(DeviceEvidence("192.168.1.41", openPorts = setOf(8080)), Verdict(Risk.CHECK_THIS, listOf(Sign.WebPage)), 0)
    private val web2 = WifiDevice(DeviceEvidence("192.168.1.52", name = "esp-1F3C", openPorts = setOf(80)), Verdict(Risk.CHECK_THIS, listOf(Sign.WebPage)), 0)
    private val quiet = WifiDevice(DeviceEvidence("192.168.1.9"), Verdict(Risk.UNKNOWN, emptyList()), 0)
    private val tv = WifiDevice(DeviceEvidence("192.168.1.19", name = "Living-room TV"), Verdict(Risk.KNOWN, listOf(Sign.KnownKind("tv")), kind = "tv"), 0)
    private val printer = WifiDevice(DeviceEvidence("192.168.1.30", name = "HP LaserJet"), Verdict(Risk.KNOWN, listOf(Sign.KnownKind("printer")), kind = "printer"), 0)
    private val router = WifiDevice(DeviceEvidence("192.168.1.1", isGateway = true), Verdict(Risk.KNOWN, listOf(Sign.Router)), 0)
    private val phone = WifiDevice(DeviceEvidence("192.168.1.14", isSelf = true), Verdict(Risk.KNOWN, listOf(Sign.ThisPhone)), 0)
    private fun wifi(vararg d: WifiDevice, isolated: Boolean = false, ssid: String? = "SeaView_Guest") = WifiSnapshot(1, WifiStatus.DONE, ssid, 1f, d.size, d.size, 0, d.toList(), isolated)

    private val v380 = NearbyDevice(Check.HOTSPOTS, "a", "V380-8A12F0", -58, NearbyKind.CAMERA_HOTSPOT, Risk.LIKELY_CAMERA, "V380")
    private val smartLife = NearbyDevice(Check.HOTSPOTS, "b", "SmartLife-1A2B", -70, NearbyKind.CAMERA_HOTSPOT, Risk.CHECK_THIS, "Tuya")
    private val cafe = NearbyDevice(Check.HOTSPOTS, "c", "SeaView_Guest", -51, NearbyKind.NETWORK, Risk.UNKNOWN)
    private val cafe2 = NearbyDevice(Check.HOTSPOTS, "d", "Cafe Lumen", -78, NearbyKind.NETWORK, Risk.UNKNOWN)
    private val tile = NearbyDevice(Check.BLUETOOTH, "AA", null, -60, NearbyKind.TRACKER, Risk.CHECK_THIS, "Tile")
    private val buds = NearbyDevice(Check.BLUETOOTH, "BB", "Buds", -50, NearbyKind.BLE_OTHER, Risk.UNKNOWN)
    private fun nearby(check: Check, vararg d: NearbyDevice, status: NearbyStatus = NearbyStatus.DONE, block: NearbyBlock? = null, total: Int = d.size) =
        NearbySnapshot(check, 1, status, block, 0.5f, total, d.toList(), 1)

    @Test fun sweepAndResults() {
        val running = WifiSnapshot(1, WifiStatus.RUNNING, "SeaView_Guest", 0.6f, 11, 18, 10, listOf(camera, tv))
        shot("03-sweep-wifi") { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = running), {}, {}, {}) }
        shot("03a-sweep-wifi-empty") { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.RUNNING, "SeaView_Guest", 0.1f, 2, 254, 18)), {}, {}, {}) }
        shot("03b-sweep-not-on-wifi") { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.NOT_ON_WIFI)), {}, {}, {}) }
        shot("03c-sweep-permission") { SweepContent(SweepState(Place.STAY, Check.entries, Check.HOTSPOTS, nearby = nearby(Check.HOTSPOTS, status = NearbyStatus.NEEDS_PERMISSION)), {}, {}, {}) }
        shot("03d-sweep-permission-settings") { SweepContent(SweepState(Place.STAY, Check.entries, Check.HOTSPOTS, nearby = nearby(Check.HOTSPOTS, status = NearbyStatus.NEEDS_PERMISSION)), {}, {}, {}, settingsOnly = true) }
        shot("03e-sweep-radio-off") { SweepContent(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH, nearby = nearby(Check.BLUETOOTH, status = NearbyStatus.UNAVAILABLE, block = NearbyBlock.BLUETOOTH_OFF)), {}, {}, {}) }
        shot("03f-sweep-no-hardware") { SweepContent(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH, nearby = nearby(Check.BLUETOOTH, status = NearbyStatus.UNAVAILABLE, block = NearbyBlock.NO_BLUETOOTH)), {}, {}, {}) }
        shot("03g-sweep-hotspots") { SweepContent(SweepState(Place.STAY, Check.entries, Check.HOTSPOTS, nearby = nearby(Check.HOTSPOTS, v380, cafe, status = NearbyStatus.RUNNING, total = 0)), {}, {}, {}) }
        shot("03h-sweep-location-off") { SweepContent(SweepState(Place.STAY, Check.entries, Check.HOTSPOTS, nearby = nearby(Check.HOTSPOTS, status = NearbyStatus.UNAVAILABLE, block = NearbyBlock.LOCATION_OFF)), {}, {}, {}) }

        val many = (1..15).map { tv.copy(evidence = tv.evidence.copy(ip = "192.168.1.${100 + it}")) }
        shot("04-wifi-results") { WifiResultsContent(WifiResultsState(wifi(camera, web, web2, *many.toTypedArray()), Check.HOTSPOTS, true), {}, {}, {}, {}, {}) }
        shot("04a-wifi-nothing-found") { WifiResultsContent(WifiResultsState(wifi(tv, printer, router, phone), Check.HOTSPOTS, true), {}, {}, {}, {}, {}) }
        shot("04b-wifi-hidden-name") { WifiResultsContent(WifiResultsState(wifi(web, quiet, tv, router, phone, ssid = null), Check.HOTSPOTS, true), {}, {}, {}, {}, {}) }
        shot("04d-other-devices") { WifiOthersContent(wifi(camera, quiet, tv, printer, router, phone), {}, {}) }
        shot("04e-other-devices-empty") { WifiOthersContent(wifi(camera), {}, {}) }
        shot("04f-hotspot-results") { NearbyResultsContent(NearbyResultsState(nearby(Check.HOTSPOTS, v380, smartLife, cafe, cafe2), Check.BLUETOOTH, 4, true), {}, {}, {}, {}, {}) }
        shot("04g-bluetooth-nothing-found") { NearbyResultsContent(NearbyResultsState(nearby(Check.BLUETOOTH, buds), Check.LENS, 3, true), {}, {}, {}, {}, {}) }
        shot("04h-hotspot-zero") { NearbyResultsContent(NearbyResultsState(nearby(Check.HOTSPOTS), Check.BLUETOOTH, 4, true), {}, {}, {}, {}, {}) }
        shot("04i-other-networks") { NearbyOthersContent(nearby(Check.HOTSPOTS, v380, cafe, cafe2), {}, {}) }

        shot("05-device") { DeviceContent(camera, {}, {}, {}, {}, {}) }
        shot("05a-device-not-recognised") { DeviceContent(quiet, {}, {}, {}, {}, {}) }
        shot("05b-device-this-phone") { DeviceContent(phone, {}, {}, {}, {}, {}) }
        shot("05c-device-known") { DeviceContent(tv.copy(knownAs = KnownAs.ROOM), {}, {}, {}, {}, {}) }
    }

    private val backCamera = app.unpeep.privacy.optics.CameraSpec("0", app.unpeep.privacy.optics.Facing.BACK, 90, true, android.util.Size(1440, 1080), android.util.Size(320, 240), 8f, null, 2)

    @Composable private fun Lens(s: LensState, settingsOnly: Boolean = false, photoFailed: Boolean = false) = app.unpeep.privacy.ui.theme.ViewfinderTheme {
        LensContent(s, onBack = {}, onNext = {}, onMode = {}, onFilter = {}, onZoom = {}, onTorch = {}, onTips = {}, onShutter = {}, onAllow = {}, settingsOnly = settingsOnly, onRetry = {}, photoFailed = photoFailed)
    }

    /** [content] as a 892 × 412 dp phone on its side, scaled down to fit the shot's width (T05). */
    @Composable private fun Sideways(content: @Composable () -> Unit) {
        val cfg = android.content.res.Configuration(androidx.compose.ui.platform.LocalConfiguration.current).apply { screenWidthDp = 892; screenHeightDp = 412 }
        val scale = 412f / 892f
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopStart) {
            androidx.compose.foundation.layout.Box(
                Modifier.wrapContentSize(androidx.compose.ui.Alignment.TopStart, unbounded = true).requiredSize(892.dp, 412.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f) },
            ) {
                // The app reads the window's size from LocalWindowInfo (Window.kt): give it the same 892 × 412 dp.
                val d = androidx.compose.ui.platform.LocalDensity.current
                val info = androidx.compose.ui.platform.LocalWindowInfo.current
                val window = object : androidx.compose.ui.platform.WindowInfo by info {
                    override val containerSize get() = with(d) { androidx.compose.ui.unit.IntSize(892.dp.roundToPx(), 412.dp.roundToPx()) }
                }
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalConfiguration provides cfg, androidx.compose.ui.platform.LocalWindowInfo provides window) { content() }
            }
        }
    }

    @Composable private fun Magnet(s: MagnetState) = MagnetContent(s, onBack = {}, onNext = {}, onBeep = {}, onSetNormal = {}, onAdd = {})

    @Test fun findersAndSheets() {
        shot("06-not-a-camera-sheet") { DeviceContent(camera, {}, {}, {}, {}, {}); NotACameraSheet(null, {}, {}, {}) }
        shot("06b-not-a-camera-dark", isDark = true) { DeviceContent(camera, {}, {}, {}, {}, {}); NotACameraSheet(null, {}, {}, {}) }
        shot("06c-not-a-camera-200", fontScale = 2f) { DeviceContent(camera, {}, {}, {}, {}, {}); NotACameraSheet(null, {}, {}, {}) }

        shot("07-find-it") { FindItContent(FindItState(v380, -58, Nearness.CLOSE, 4, Trend.STRONGER, 12, loaded = true), {}, {}, {}) }
        shot("07a-find-it-weaker") { FindItContent(FindItState(v380, -70, Nearness.NEAR, 2, Trend.WEAKER, 4, loaded = true), {}, {}, {}) }
        shot("07b-find-it-steady-added") { FindItContent(FindItState(v380.copy(added = true), -46, Nearness.VERY_CLOSE, 5, Trend.STEADY, 2, loaded = true), {}, {}, {}) }
        shot("07c-find-it-not-heard") { FindItContent(FindItState(v380, -80, Nearness.FAR, 1, Trend.STEADY, 48, lost = true, loaded = true), {}, {}, {}) }
        shot("07d-find-it-200", fontScale = 2f) { FindItContent(FindItState(v380, -58, Nearness.CLOSE, 4, Trend.STRONGER, 12, loaded = true), {}, {}, {}) }

        val spot = app.unpeep.privacy.optics.Spot(0.55f, 0.42f, 0.01f, 250)
        shot("08-lens-finder") { Lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, spots = listOf(spot), photos = 0)) }
        shot("T05-lens-landscape") { Sideways { Lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, spots = listOf(spot), photos = 1)) } }
        shot("T05a-infrared-landscape-sweep") { Sideways { Lens(LensState(status = CameraStatus.RUNNING, mode = Check.INFRARED, sweep = true, spec = backCamera, spots = listOf(spot), photos = 0)) } }
        shot("08a-lens-permission") { Lens(LensState(status = CameraStatus.NEEDS_PERMISSION)) }
        shot("08b-lens-camera-in-use") { Lens(LensState(status = CameraStatus.IN_USE, spec = backCamera)) }
        shot("08c-lens-camera-failed") { Lens(LensState(status = CameraStatus.FAILED, spec = backCamera)) }
        shot("08d-lens-no-camera") { Lens(LensState(status = CameraStatus.NO_CAMERA)) }
        shot("08e-lens-starting") { Lens(LensState(status = CameraStatus.STARTING, spec = backCamera)) }
        shot("08f-lens-infrared") { Lens(LensState(mode = Check.INFRARED, status = CameraStatus.RUNNING, spec = backCamera, spots = listOf(spot), seesInfrared = true)) }
        shot("08g-infrared-may-not-see") { Lens(LensState(mode = Check.INFRARED, status = CameraStatus.RUNNING, spec = backCamera, seesInfrared = false)) }
        shot("08h-lens-photo-failed") { Lens(LensState(status = CameraStatus.RUNNING, spec = backCamera), photoFailed = true) }
        shot("08i-lens-sweep") { Lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, torch = false, photos = 2, sweep = true, next = Check.INFRARED)) }
        shot("08j-lens-tips-sheet") { Lens(LensState(status = CameraStatus.RUNNING, spec = backCamera)); app.unpeep.privacy.ui.theme.ViewfinderTheme { app.unpeep.privacy.ui.lens.LensTipsSheet(Check.LENS, {}, {}) } }
        shot("08k-lens-tv-remote-sheet") { Lens(LensState(mode = Check.INFRARED, status = CameraStatus.RUNNING, spec = backCamera)); app.unpeep.privacy.ui.theme.ViewfinderTheme { app.unpeep.privacy.ui.lens.InfraredTestSheet({}, {}) } }

        shot("08m-magnet-learning") { Magnet(MagnetState(learned = 0.4f)) }
        shot("08n-magnet-reading") { Magnet(MagnetState(learning = false, learned = 1f, above = 2, normal = 48, now = 50, bars = listOf(1, 2, 1, 0, 2, 1, 2, 1, 1, 2))) }
        shot("08o-magnet-jump") { Magnet(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(0, 0, 1, 0, 1, 2, 9, 11, 14, 13))) }
        shot("08p-magnet-added-sweep") { Magnet(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(0, 1, 2, 9, 11, 14), added = true, sweep = true, next = Check.CHECKLIST)) }
        shot("08q-magnet-no-sensor") { Magnet(MagnetState(available = false)) }
        shot("08r-magnet-200", fontScale = 2f) { Magnet(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(0, 1, 2, 9, 11, 14))) }
    }

    /** The report with a toast over its dock, as the app shows an export's result. */
    @Composable private fun ReportWithToast(r: app.unpeep.privacy.data.SavedReport, text: Int, action: Int, icon: Int) {
        val toasts = androidx.compose.runtime.remember { app.unpeep.privacy.ui.components.ToastState() }
        val ctx = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) { toasts.show(ctx.getString(text), action = ctx.getString(action), onAction = {}, icon = icon) }
        androidx.compose.runtime.CompositionLocalProvider(app.unpeep.privacy.ui.components.LocalToasts provides toasts) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                ReportScreen(r)
                ToastHost(toasts, overNav = false)
            }
        }
    }

    @Composable private fun ReportScreen(r: app.unpeep.privacy.data.SavedReport = app.unpeep.privacy.ui.report.previewReport()) =
        ReportContent(r, app.unpeep.privacy.ui.report.rememberReportWords(), {}, {}, {}, {}, {}, {}, {})

    @Test fun checklistReportsAndHelp() {
        val e = app.unpeep.privacy.domain.ChecklistEntry(app.unpeep.privacy.domain.ChecklistAnswer.LOOKS_FINE)
        val unsure = app.unpeep.privacy.domain.ChecklistEntry(app.unpeep.privacy.domain.ChecklistAnswer.NOT_SURE, note = "", photoPath = "x")
        val skip = app.unpeep.privacy.domain.ChecklistEntry(app.unpeep.privacy.domain.ChecklistAnswer.SKIPPED)
        val answers = mapOf(ChecklistItem.MIRROR_LIGHT to e, ChecklistItem.MIRROR_EDGES to e, ChecklistItem.LAMPS to e, ChecklistItem.HOOKS to unsure)
        shot("09-checklist") { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers, sweep = true), {}, {}, {}) }
        shot("09a-checklist-answered") { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers + (ChecklistItem.CORNERS to skip), sweep = true), {}, {}, {}) }
        shot("09b-checklist-answer-sheet") { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers, sweep = true), {}, {}, {}); AnswerSheet(ChecklistItem.CORNERS, null, {}, {}) }
        shot("09c-checklist-not-sure-sheet") { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers, sweep = true), {}, {}, {}); AnswerSheet(ChecklistItem.HOOKS, unsure, {}, {}) }
        shot("09d-checklist-200", fontScale = 2f) { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers, sweep = true), {}, {}, {}) }

        val r = app.unpeep.privacy.ui.report.previewReport()
        shot("10-report") { ReportScreen(r) }
        shot("10a-report-nothing-found") { ReportScreen(r.copy(findings = emptyList(), toLookAt = 0)) }
        shot("10c-report-rename-sheet") { ReportScreen(r); app.unpeep.privacy.ui.report.RenameSheet(r.name, {}, {}) }
        shot("10d-report-undo-sheet") { ReportScreen(r); app.unpeep.privacy.ui.report.UndoSheet("Living-room TV", {}, {}) }
        shot("10e-finding-detail") { app.unpeep.privacy.ui.report.FindingContent(r.findings.first().finding, {}) }
        shot("10h-report-export-saved") { ReportWithToast(r, R.string.export_done, R.string.open, R.drawable.ic_check_circle) }
        shot("10i-report-export-failed") { ReportWithToast(r, R.string.export_failed, R.string.try_again, R.drawable.ic_info) }
        shot("10k-report-200", fontScale = 2f) { ReportScreen(r) }
        shot("E12-no-checks-ran") { ReportScreen(r.copy(findings = emptyList(), toLookAt = 0, ran = emptyList())) }

        val de = app.unpeep.privacy.data.HelpEntry("DE", "112", "110")
        val inEntry = app.unpeep.privacy.data.HelpEntry("IN", "112", "112", "https://cybercrime.gov.in/", "1930")
        val all = listOf(de, inEntry, app.unpeep.privacy.data.HelpEntry("US", "911"), app.unpeep.privacy.data.HelpEntry("GB", "999", "101"))
        shot("11-if-you-find-a-camera") { FoundCameraContent(HelpState(inEntry, true, null, all, "IN"), {}, {}, {}, {}, {}) }
        shot("11a-found-camera-no-report") { FoundCameraContent(HelpState(inEntry, true, null, all, "IN"), {}, {}, {}, {}, null) }
        shot("15-help-numbers-sheet") { FoundCameraContent(HelpState(inEntry, true, null, all, "IN"), {}, {}, {}, {}, {}); HelpNumbersSheet(HelpState(inEntry, true, null, all, "IN"), {}, {}) }
        shot("15b-help-numbers-region-unknown") { FoundCameraContent(HelpState(null, true, null, all, null), {}, {}, {}, {}, {}) }

        val now = System.currentTimeMillis()
        val day = 24L * 3600 * 1000
        val list = listOf(
            r.copy(id = 1, savedAt = now - 1000),
            r.copy(id = 2, name = "", place = Place.TRIAL_ROOM, savedAt = now - day, toLookAt = 0),
            r.copy(id = 3, name = "Office", place = Place.OFFICE, savedAt = now - 40 * day, toLookAt = 0),
            r.copy(id = 4, name = "Airport washroom", place = Place.WASHROOM, savedAt = now - 45 * day, toLookAt = 1),
        )
        shot("12-reports") { ReportsContent(ReportsState(list, true), {}, {}, now) }
        shot("12a-reports-empty") { ReportsContent(ReportsState(emptyList(), true), {}, {}, now) }
        shot("12d-reports-delete-sheet") { ReportsContent(ReportsState(list, true), {}, {}, now); app.unpeep.privacy.ui.reports.DeleteSheet({}, {}) }
        shot("12e-reports-dark", isDark = true) { ReportsContent(ReportsState(list, true), {}, {}, now) }
        shot("12f-reports-200", fontScale = 2f) { ReportsContent(ReportsState(list, true), {}, {}, now) }
    }

    @Test fun moreScreens() {
        val appearance: @Composable () -> Unit = {
            app.unpeep.privacy.ui.more.AppearanceContent(app.unpeep.privacy.data.Settings(), {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> })
        }
        shot("14-appearance") { appearance() }
        val large = app.unpeep.privacy.data.Settings().let { it.copy(theme = it.theme.copy(textSize = app.unpeep.privacy.ui.theme.TextSize.LARGE, font = app.unpeep.privacy.ui.theme.BodyFont.OUTFIT)) }
        shot("14a-text-size-large") { app.unpeep.privacy.ui.more.AppearanceContent(large, {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }) }
        shot("13c-language-sheet") { app.unpeep.privacy.ui.more.LanguageSheet {} }
        val marks = listOf(
            app.unpeep.privacy.data.Mark("a", KnownAs.ROOM, "Living-room TV", Check.WIFI, 0),
            app.unpeep.privacy.data.Mark("b", KnownAs.MINE, "Pixel 8", Check.WIFI, 0),
            app.unpeep.privacy.data.Mark("c", KnownAs.MINE, "Buds", Check.BLUETOOTH, 0),
        )
        shot("13d-marked-as-known") { app.unpeep.privacy.ui.more.MarkedKnownContent(marks, {}, {}) }
        shot("13e-marked-empty") { app.unpeep.privacy.ui.more.MarkedKnownContent(emptyList(), {}, {}) }
        shot("13f-how-it-works") { app.unpeep.privacy.ui.more.HowItWorksScreen {} }
        shot("13g-cant-find") { app.unpeep.privacy.ui.more.LimitsScreen({}) }
        shot("17-about") { app.unpeep.privacy.ui.more.AboutScreen({}, {}, {}) }
        shot("18-licences") { app.unpeep.privacy.ui.more.LicencesScreen({}, {}) }
        shot("18a-licence-text") { app.unpeep.privacy.ui.more.LicenceTextScreen("GNU GPL v3", "gpl-3.0.txt", {}) }
        shot("10j-report-exporting") { ReportContent(app.unpeep.privacy.ui.report.previewReport(), app.unpeep.privacy.ui.report.rememberReportWords(), {}, {}, {}, {}, {}, {}, {}, exporting = true) }
    }

    /** A narrow phone (360 dp) with larger text: every screen whose words could overflow. */
    @Composable private fun Narrow(content: @Composable () -> Unit) =
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(Modifier.align(androidx.compose.ui.Alignment.TopStart).requiredWidth(360.dp).fillMaxHeight()) { content() }
        }

    /**
     * The eight raw Play Store shots (release/README.md): real screens with made-up state, saved as home.png …
     * home-dark.png for `tools/store_shots.py`. Run on a 1080 × 2400 phone with the status bar in demo mode:
     * `-Pandroid.testInstrumentationRunnerArguments.class=app.unpeep.privacy.screens.DesignShots#storeShots`.
     */
    @Test fun storeShots() {
        val tabs = @Composable { content: @Composable () -> Unit ->
            Column(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { content() }
                app.unpeep.privacy.ui.components.UnpeepNavBar(
                    listOf(
                        app.unpeep.privacy.ui.components.NavItem(text(R.string.tab_check), R.drawable.ic_radar),
                        app.unpeep.privacy.ui.components.NavItem(text(R.string.tab_reports), R.drawable.ic_reports),
                        app.unpeep.privacy.ui.components.NavItem(text(R.string.tab_more), R.drawable.ic_more),
                    ),
                    0, {},
                )
            }
        }
        val home = @Composable { tabs { app.unpeep.privacy.ui.home.HomeContent(app.unpeep.privacy.ui.home.HomeState(loaded = true), {}, {}, { _, _ -> }, {}) } }
        shot("home") { home() }
        val pixel = WifiDevice(DeviceEvidence("192.168.1.14", name = "Pixel 8"), Verdict(Risk.KNOWN, listOf(Sign.KnownKind("phone")), kind = "phone"), 0)
        shot("sweep") { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.RUNNING, "SeaView_Guest", 0.61f, 11, 18, 10, listOf(camera, tv, pixel))), {}, {}, {}) }
        val many = (1..15).map { tv.copy(evidence = tv.evidence.copy(ip = "192.168.1.${100 + it}")) }
        shot("results") { WifiResultsContent(WifiResultsState(wifi(camera, web, web2, *many.toTypedArray()), Check.HOTSPOTS, true), {}, {}, {}, {}, {}) }
        shot("device") { DeviceContent(camera, {}, {}, {}, {}, {}) }
        val spots = listOf(app.unpeep.privacy.optics.Spot(0.4f, 0.3f, 0.012f, 250), app.unpeep.privacy.optics.Spot(0.73f, 0.6f, 0.006f, 230))
        shot("lens", lightBars = false) {
            app.unpeep.privacy.ui.theme.ViewfinderTheme {
                LensContent(
                    LensState(status = CameraStatus.RUNNING, spec = backCamera, spots = spots, photos = 0), onBack = {}, onNext = {}, onMode = {}, onFilter = {}, onZoom = {},
                    onTorch = {}, onTips = {}, onShutter = {}, onAllow = {}, settingsOnly = false, onRetry = {}, preview = { m -> DimRoom(m) },
                )
            }
        }
        shot("find-it") { FindItContent(FindItState(v380, -58, Nearness.CLOSE, 4, Trend.STRONGER, 12, loaded = true), {}, {}, {}) }
        shot("report") { ReportScreen() }
        shot("home-dark", isDark = true) { home() }
    }

    /** A dim room for the lens finder's store shot (the emulator has no camera picture): a wall, a shelf and two things on it. */
    @Composable private fun DimRoom(modifier: Modifier) = androidx.compose.foundation.Canvas(modifier) {
        val wall = androidx.compose.ui.graphics.Color(0xFF1A1D26)
        val floor = androidx.compose.ui.graphics.Color(0xFF0E1016)
        val thing = androidx.compose.ui.graphics.Color(0xFF0A0B10)
        val edge = androidx.compose.ui.graphics.Color(0xFF242833)
        drawRect(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFF3A3F4E), wall), center = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.2f), radius = size.width * 0.9f))
        drawRect(floor, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.74f))
        fun box(x: Float, y: Float, w: Float, h: Float) {
            val at = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y)
            val sz = androidx.compose.ui.geometry.Size(size.width * w, size.height * h)
            val r = androidx.compose.ui.geometry.CornerRadius(size.width * 0.012f)
            drawRoundRect(thing, at, sz, r)
            drawRoundRect(edge, at, sz, r, style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * 0.003f))
        }
        box(0.06f, 0.61f, 0.3f, 0.13f)
        box(0.11f, 0.53f, 0.1f, 0.08f)
        box(0.56f, 0.48f, 0.34f, 0.26f)
        box(0.6f, 0.52f, 0.26f, 0.15f)
    }

    @Test fun overflowAudit() {
        val longName = camera.copy(evidence = camera.evidence.copy(name = "Hikvision-DS-2CD2143G2-IU-Office-Corridor-East"))
        val longSsid = "SeaView_Guest_5G_Floor3_Wing_B_Extended"
        val many = (1..15).map { tv.copy(evidence = tv.evidence.copy(ip = "192.168.1.${100 + it}")) }
        for ((tag, scale) in listOf("130" to 1.3f, "200" to 2f)) {
            shot("A-$tag-wifi-results", fontScale = scale) { Narrow { WifiResultsContent(WifiResultsState(wifi(longName, web, *many.toTypedArray(), ssid = longSsid), Check.HOTSPOTS, true), {}, {}, {}, {}, {}) } }
            shot("A-$tag-bt-results", fontScale = scale) { Narrow { NearbyResultsContent(NearbyResultsState(nearby(Check.BLUETOOTH, tile, buds), Check.LENS, 28, true), {}, {}, {}, {}, {}) } }
            shot("A-$tag-hotspot-results", fontScale = scale) { Narrow { NearbyResultsContent(NearbyResultsState(nearby(Check.HOTSPOTS, v380, smartLife, cafe), Check.BLUETOOTH, 28, true, scanWait = 48), {}, {}, {}, {}, {}) } }
            shot("A-$tag-sweep", fontScale = scale) { Narrow { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.RUNNING, longSsid, 0.6f, 11, 18, 10, listOf(longName, tv))), {}, {}, {}) } }
            shot("A-$tag-device", fontScale = scale) { Narrow { DeviceContent(longName, {}, {}, {}, {}, {}) } }
            shot("A-$tag-find-it", fontScale = scale) { Narrow { FindItContent(FindItState(v380.copy(name = "DIRECT-HP-LaserJet-Pro-M404dn-Office"), -58, Nearness.CLOSE, 4, Trend.STRONGER, 28, loaded = true), {}, {}, {}) } }
            shot("A-$tag-magnet", fontScale = scale) { Narrow { Magnet(MagnetState(learning = false, learned = 1f, above = 140, normal = 48, now = 188, jumped = true, bars = listOf(0, 1, 2, 9, 11, 14))) } }
            shot("A-$tag-report", fontScale = scale) { Narrow { ReportScreen(app.unpeep.privacy.ui.report.previewReport().copy(name = "Sea View Hotel and Residences, room 304, east wing")) } }
            shot("A-$tag-checklist", fontScale = scale) { Narrow { ChecklistContent(ChecklistState(Place.STAY), {}, {}, {}) } }
            shot("A-$tag-lens", fontScale = scale) { Narrow { Lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, photos = 12, sweep = true, next = Check.INFRARED)) } }
            shot("A-$tag-appearance", fontScale = scale) { Narrow { app.unpeep.privacy.ui.more.AppearanceContent(app.unpeep.privacy.data.Settings(), {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }) } }
            shot("A-$tag-about", fontScale = scale) { Narrow { app.unpeep.privacy.ui.more.AboutScreen({}, {}, {}) } }
            shot("A-$tag-stop-sheet", fontScale = scale) { Narrow { SweepContent(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH, nearby = nearby(Check.BLUETOOTH, status = NearbyStatus.RUNNING)), {}, {}, {}); app.unpeep.privacy.ui.sweep.StopSweepSheet(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH), {}, {}, {}) } }
        }
    }

    /** The PDF of the sample report (10g), written beside the shots. */
    @Test fun reportPdf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val out = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val dir = (if (out != null) File(out) else File(ctx.getExternalFilesDir(null), "shots")).apply { mkdirs() }
        val font = androidx.core.content.res.ResourcesCompat.getFont(ctx, R.font.lexend)
        File(dir, "10g-report-pdf.pdf").outputStream().use {
            app.unpeep.privacy.report.PdfReport(app.unpeep.privacy.ui.report.ReportWords(ctx.resources), ctx.resources, font).write(app.unpeep.privacy.ui.report.previewReport(), it)
        }
    }

    private companion object {
        const val SETTLE_MS = 700L
    }
}
