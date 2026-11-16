package app.keyhole.privacy.screens

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import app.keyhole.privacy.domain.ChecklistItem
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
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.DeviceEvidence
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.domain.NearbyBlock
import app.keyhole.privacy.domain.NearbyDevice
import app.keyhole.privacy.domain.NearbyKind
import app.keyhole.privacy.domain.NearbySnapshot
import app.keyhole.privacy.domain.NearbyStatus
import app.keyhole.privacy.domain.Nearness
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.domain.Sign
import app.keyhole.privacy.domain.Trend
import app.keyhole.privacy.domain.Verdict
import app.keyhole.privacy.domain.WifiDevice
import app.keyhole.privacy.domain.WifiSnapshot
import app.keyhole.privacy.domain.WifiStatus
import app.keyhole.privacy.plural
import app.keyhole.privacy.setKeyholeContent
import app.keyhole.privacy.text
import app.keyhole.privacy.ui.lens.CameraStatus
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import app.keyhole.privacy.ui.checklist.AnswerSheet
import app.keyhole.privacy.ui.checklist.ChecklistContent
import app.keyhole.privacy.ui.checklist.ChecklistState
import app.keyhole.privacy.ui.found.FoundCameraContent
import app.keyhole.privacy.ui.found.HelpNumbersSheet
import app.keyhole.privacy.ui.found.HelpState
import app.keyhole.privacy.ui.report.ReportContent
import app.keyhole.privacy.ui.reports.ReportsContent
import app.keyhole.privacy.ui.reports.ReportsState
import app.keyhole.privacy.ui.lens.LensContent
import app.keyhole.privacy.ui.lens.LensState
import app.keyhole.privacy.ui.magnet.MagnetContent
import app.keyhole.privacy.ui.magnet.MagnetState
import app.keyhole.privacy.ui.nearby.FindItContent
import app.keyhole.privacy.ui.nearby.FindItState
import app.keyhole.privacy.ui.nearby.NearbyOthersContent
import app.keyhole.privacy.ui.nearby.NearbyResultsContent
import app.keyhole.privacy.ui.nearby.NearbyResultsState
import app.keyhole.privacy.ui.sweep.SweepContent
import app.keyhole.privacy.ui.sweep.SweepState
import app.keyhole.privacy.ui.wifi.DeviceContent
import app.keyhole.privacy.ui.wifi.NotACameraSheet
import app.keyhole.privacy.ui.wifi.WifiOthersContent
import app.keyhole.privacy.ui.wifi.WifiResultsContent
import app.keyhole.privacy.ui.wifi.WifiResultsState
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
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.ThemeSettings
import app.keyhole.privacy.ui.theme.ThemeMode
import androidx.compose.runtime.mutableStateOf
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Before
import java.io.File

/**
 * Not a test: draws each designed state from made-up data and saves a screenshot named like its PNG in
 * design/screens/, so the build can be put beside the design. Runs only when asked:
 * `./gradlew :app:connectedPlayDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.shots=1
 *  -Pandroid.testInstrumentationRunnerArguments.class=app.keyhole.privacy.screens.DesignShots`, then
 * look in app/build/outputs/connected_android_test_additional_output.
 */
@RunWith(AndroidJUnit4::class)
class DesignShots {
    @get:Rule val rule = createComposeRule()
    private val screen = mutableStateOf<@Composable () -> Unit>({})
    private var dark = mutableStateOf(false)

    @Before fun onlyWhenAsked() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("shots") != null)
        rule.setContent {
            KeyholeTheme(ThemeSettings(mode = if (dark.value) ThemeMode.DARK else ThemeMode.LIGHT)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { screen.value() }
            }
        }
    }

    /** Draws [content], lets it settle, and saves the whole screen (sheets included) as shots/[name].png. */
    private fun shot(name: String, isDark: Boolean = false, fontScale: Float = 1f, content: @Composable () -> Unit) {
        dark.value = isDark
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

    private val backCamera = app.keyhole.privacy.optics.CameraSpec("0", app.keyhole.privacy.optics.Facing.BACK, 90, true, android.util.Size(1440, 1080), android.util.Size(320, 240), 8f, null, 2)

    @Composable private fun lens(s: LensState, settingsOnly: Boolean = false, photoFailed: Boolean = false) = app.keyhole.privacy.ui.theme.ViewfinderTheme {
        LensContent(s, onBack = {}, onNext = {}, onMode = {}, onFilter = {}, onZoom = {}, onTorch = {}, onTips = {}, onShutter = {}, onAllow = {}, settingsOnly = settingsOnly, onRetry = {}, photoFailed = photoFailed)
    }

    @Composable private fun magnet(s: MagnetState) = MagnetContent(s, onBack = {}, onNext = {}, onBeep = {}, onSetNormal = {}, onAdd = {})

    @Test fun findersAndSheets() {
        shot("06-not-a-camera-sheet") { DeviceContent(camera, {}, {}, {}, {}, {}); NotACameraSheet(null, {}, {}, {}) }
        shot("06b-not-a-camera-dark", isDark = true) { DeviceContent(camera, {}, {}, {}, {}, {}); NotACameraSheet(null, {}, {}, {}) }
        shot("06c-not-a-camera-200", fontScale = 2f) { DeviceContent(camera, {}, {}, {}, {}, {}); NotACameraSheet(null, {}, {}, {}) }

        shot("07-find-it") { FindItContent(FindItState(v380, -58, Nearness.CLOSE, 4, Trend.STRONGER, 12, loaded = true), {}, {}, {}) }
        shot("07a-find-it-weaker") { FindItContent(FindItState(v380, -70, Nearness.NEAR, 2, Trend.WEAKER, 4, loaded = true), {}, {}, {}) }
        shot("07b-find-it-steady-added") { FindItContent(FindItState(v380.copy(added = true), -46, Nearness.VERY_CLOSE, 5, Trend.STEADY, 2, loaded = true), {}, {}, {}) }
        shot("07c-find-it-not-heard") { FindItContent(FindItState(v380, -80, Nearness.FAR, 1, Trend.STEADY, 48, lost = true, loaded = true), {}, {}, {}) }
        shot("07d-find-it-200", fontScale = 2f) { FindItContent(FindItState(v380, -58, Nearness.CLOSE, 4, Trend.STRONGER, 12, loaded = true), {}, {}, {}) }

        val spot = app.keyhole.privacy.optics.Spot(0.55f, 0.42f, 0.01f, 250)
        shot("08-lens-finder") { lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, spots = listOf(spot), photos = 0)) }
        shot("08a-lens-permission") { lens(LensState(status = CameraStatus.NEEDS_PERMISSION)) }
        shot("08b-lens-camera-in-use") { lens(LensState(status = CameraStatus.IN_USE, spec = backCamera)) }
        shot("08c-lens-camera-failed") { lens(LensState(status = CameraStatus.FAILED, spec = backCamera)) }
        shot("08d-lens-no-camera") { lens(LensState(status = CameraStatus.NO_CAMERA)) }
        shot("08e-lens-starting") { lens(LensState(status = CameraStatus.STARTING, spec = backCamera)) }
        shot("08f-lens-infrared") { lens(LensState(mode = Check.INFRARED, status = CameraStatus.RUNNING, spec = backCamera, spots = listOf(spot), seesInfrared = true)) }
        shot("08g-infrared-may-not-see") { lens(LensState(mode = Check.INFRARED, status = CameraStatus.RUNNING, spec = backCamera, seesInfrared = false)) }
        shot("08h-lens-photo-failed") { lens(LensState(status = CameraStatus.RUNNING, spec = backCamera), photoFailed = true) }
        shot("08i-lens-sweep") { lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, torch = false, photos = 2, sweep = true, next = Check.INFRARED)) }
        shot("08j-lens-tips-sheet") { lens(LensState(status = CameraStatus.RUNNING, spec = backCamera)); app.keyhole.privacy.ui.lens.LensTipsSheet(Check.LENS, {}, {}) }
        shot("08k-lens-tv-remote-sheet") { lens(LensState(mode = Check.INFRARED, status = CameraStatus.RUNNING, spec = backCamera)); app.keyhole.privacy.ui.lens.InfraredTestSheet({}, {}) }

        shot("08m-magnet-learning") { magnet(MagnetState(learned = 0.4f)) }
        shot("08n-magnet-reading") { magnet(MagnetState(learning = false, learned = 1f, above = 2, normal = 48, now = 50, bars = listOf(1, 2, 1, 0, 2, 1, 2, 1, 1, 2))) }
        shot("08o-magnet-jump") { magnet(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(0, 0, 1, 0, 1, 2, 9, 11, 14, 13))) }
        shot("08p-magnet-added-sweep") { magnet(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(0, 1, 2, 9, 11, 14), added = true, sweep = true, next = Check.CHECKLIST)) }
        shot("08q-magnet-no-sensor") { magnet(MagnetState(available = false)) }
        shot("08r-magnet-200", fontScale = 2f) { magnet(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(0, 1, 2, 9, 11, 14))) }
    }

    @Composable private fun reportScreen(r: app.keyhole.privacy.data.SavedReport = app.keyhole.privacy.ui.report.previewReport(), export: Boolean? = null) =
        ReportContent(r, app.keyhole.privacy.ui.report.rememberReportWords(), {}, {}, {}, {}, {}, {}, {}, export)

    @Test fun checklistReportsAndHelp() {
        val e = app.keyhole.privacy.domain.ChecklistEntry(app.keyhole.privacy.domain.ChecklistAnswer.LOOKS_FINE)
        val unsure = app.keyhole.privacy.domain.ChecklistEntry(app.keyhole.privacy.domain.ChecklistAnswer.NOT_SURE, note = "", photoPath = "x")
        val skip = app.keyhole.privacy.domain.ChecklistEntry(app.keyhole.privacy.domain.ChecklistAnswer.SKIPPED)
        val answers = mapOf(ChecklistItem.MIRROR_LIGHT to e, ChecklistItem.MIRROR_EDGES to e, ChecklistItem.LAMPS to e, ChecklistItem.HOOKS to unsure)
        shot("09-checklist") { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers, sweep = true), {}, {}, {}) }
        shot("09a-checklist-answered") { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers + (ChecklistItem.CORNERS to skip), sweep = true), {}, {}, {}) }
        shot("09b-checklist-answer-sheet") { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers, sweep = true), {}, {}, {}); AnswerSheet(ChecklistItem.CORNERS, null, {}, {}) }
        shot("09c-checklist-not-sure-sheet") { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers, sweep = true), {}, {}, {}); AnswerSheet(ChecklistItem.HOOKS, unsure, {}, {}) }
        shot("09d-checklist-200", fontScale = 2f) { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers, sweep = true), {}, {}, {}) }

        val r = app.keyhole.privacy.ui.report.previewReport()
        shot("10-report") { reportScreen(r) }
        shot("10a-report-nothing-found") { reportScreen(r.copy(findings = emptyList(), toLookAt = 0)) }
        shot("10c-report-rename-sheet") { reportScreen(r); app.keyhole.privacy.ui.report.RenameSheet(r.name, {}, {}) }
        shot("10d-report-undo-sheet") { reportScreen(r); app.keyhole.privacy.ui.report.UndoSheet("Living-room TV", {}, {}) }
        shot("10e-finding-detail") { app.keyhole.privacy.ui.report.FindingContent(r.findings.first().finding, {}) }
        shot("10h-report-export-saved") { reportScreen(r, export = true) }
        shot("10i-report-export-failed") { reportScreen(r, export = false) }
        shot("10k-report-200", fontScale = 2f) { reportScreen(r) }
        shot("E12-no-checks-ran") { reportScreen(r.copy(findings = emptyList(), toLookAt = 0, ran = emptyList())) }

        val de = app.keyhole.privacy.data.HelpEntry("DE", "112", "110")
        val inEntry = app.keyhole.privacy.data.HelpEntry("IN", "112", "112", "https://cybercrime.gov.in/", "1930")
        val all = listOf(de, inEntry, app.keyhole.privacy.data.HelpEntry("US", "911"), app.keyhole.privacy.data.HelpEntry("GB", "999", "101"))
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
        shot("12d-reports-delete-sheet") { ReportsContent(ReportsState(list, true), {}, {}, now); app.keyhole.privacy.ui.reports.DeleteSheet({}, {}) }
        shot("12e-reports-dark", isDark = true) { ReportsContent(ReportsState(list, true), {}, {}, now) }
        shot("12f-reports-200", fontScale = 2f) { ReportsContent(ReportsState(list, true), {}, {}, now) }
    }

    @Test fun moreScreens() {
        val appearance: @Composable () -> Unit = {
            app.keyhole.privacy.ui.more.AppearanceContent(app.keyhole.privacy.data.Settings(), {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> })
        }
        shot("14-appearance") { appearance() }
        shot("13c-language-sheet") { app.keyhole.privacy.ui.more.LanguageSheet {} }
        val marks = listOf(
            app.keyhole.privacy.data.Mark("a", KnownAs.ROOM, "Living-room TV", Check.WIFI, 0),
            app.keyhole.privacy.data.Mark("b", KnownAs.MINE, "Pixel 8", Check.WIFI, 0),
            app.keyhole.privacy.data.Mark("c", KnownAs.MINE, "Buds", Check.BLUETOOTH, 0),
        )
        shot("13d-marked-as-known") { app.keyhole.privacy.ui.more.MarkedKnownContent(marks, {}, {}) }
        shot("13e-marked-empty") { app.keyhole.privacy.ui.more.MarkedKnownContent(emptyList(), {}, {}) }
        shot("13f-how-it-works") { app.keyhole.privacy.ui.more.HowItWorksScreen {} }
        shot("13g-cant-find") { app.keyhole.privacy.ui.more.LimitsScreen({}, {}) }
        shot("17-about") { app.keyhole.privacy.ui.more.AboutScreen({}, {}, {}) }
        shot("18-licences") { app.keyhole.privacy.ui.more.LicencesScreen({}, {}) }
        shot("18a-licence-text") { app.keyhole.privacy.ui.more.LicenceTextScreen("GNU GPL v3", "gpl-3.0.txt", {}) }
        shot("10j-report-exporting") { ReportContent(app.keyhole.privacy.ui.report.previewReport(), app.keyhole.privacy.ui.report.rememberReportWords(), {}, {}, {}, {}, {}, {}, {}, null, exporting = true) }
    }

    /** A narrow phone (360 dp) with larger text: every screen whose words could overflow. */
    @Composable private fun narrow(content: @Composable () -> Unit) =
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(Modifier.align(androidx.compose.ui.Alignment.TopStart).requiredWidth(360.dp).fillMaxHeight()) { content() }
        }

    @Test fun overflowAudit() {
        val longName = camera.copy(evidence = camera.evidence.copy(name = "Hikvision-DS-2CD2143G2-IU-Office-Corridor-East"))
        val longSsid = "SeaView_Guest_5G_Floor3_Wing_B_Extended"
        val many = (1..15).map { tv.copy(evidence = tv.evidence.copy(ip = "192.168.1.${100 + it}")) }
        for ((tag, scale) in listOf("130" to 1.3f, "200" to 2f)) {
            shot("A-$tag-wifi-results", fontScale = scale) { narrow { WifiResultsContent(WifiResultsState(wifi(longName, web, *many.toTypedArray(), ssid = longSsid), Check.HOTSPOTS, true), {}, {}, {}, {}, {}) } }
            shot("A-$tag-bt-results", fontScale = scale) { narrow { NearbyResultsContent(NearbyResultsState(nearby(Check.BLUETOOTH, tile, buds), Check.LENS, 28, true), {}, {}, {}, {}, {}) } }
            shot("A-$tag-hotspot-results", fontScale = scale) { narrow { NearbyResultsContent(NearbyResultsState(nearby(Check.HOTSPOTS, v380, smartLife, cafe), Check.BLUETOOTH, 28, true, scanWait = 48), {}, {}, {}, {}, {}) } }
            shot("A-$tag-sweep", fontScale = scale) { narrow { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.RUNNING, longSsid, 0.6f, 11, 18, 10, listOf(longName, tv))), {}, {}, {}) } }
            shot("A-$tag-device", fontScale = scale) { narrow { DeviceContent(longName, {}, {}, {}, {}, {}) } }
            shot("A-$tag-find-it", fontScale = scale) { narrow { FindItContent(FindItState(v380.copy(name = "DIRECT-HP-LaserJet-Pro-M404dn-Office"), -58, Nearness.CLOSE, 4, Trend.STRONGER, 28, loaded = true), {}, {}, {}) } }
            shot("A-$tag-magnet", fontScale = scale) { narrow { magnet(MagnetState(learning = false, learned = 1f, above = 140, normal = 48, now = 188, jumped = true, bars = listOf(0, 1, 2, 9, 11, 14))) } }
            shot("A-$tag-report", fontScale = scale) { narrow { reportScreen(app.keyhole.privacy.ui.report.previewReport().copy(name = "Sea View Hotel and Residences, room 304, east wing")) } }
            shot("A-$tag-checklist", fontScale = scale) { narrow { ChecklistContent(ChecklistState(Place.STAY), {}, {}, {}) } }
            shot("A-$tag-lens", fontScale = scale) { narrow { lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, photos = 12, sweep = true, next = Check.INFRARED)) } }
            shot("A-$tag-appearance", fontScale = scale) { narrow { app.keyhole.privacy.ui.more.AppearanceContent(app.keyhole.privacy.data.Settings(), {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }) } }
            shot("A-$tag-about", fontScale = scale) { narrow { app.keyhole.privacy.ui.more.AboutScreen({}, {}, {}) } }
            shot("A-$tag-stop-sheet", fontScale = scale) { narrow { SweepContent(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH, nearby = nearby(Check.BLUETOOTH, status = NearbyStatus.RUNNING)), {}, {}, {}); app.keyhole.privacy.ui.sweep.StopSweepSheet(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH), {}, {}, {}) } }
        }
    }

    private companion object {
        const val SETTLE_MS = 700L
    }
}
