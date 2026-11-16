package app.keyhole.privacy.screens

import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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

/**
 * Every built screen drawn from made-up state, one test per state it can be in: what it says, which action it
 * offers, and that the action reaches the caller. No scanning, no network, no permission questions.
 */
@RunWith(AndroidJUnit4::class)
class ScreensTest {
    @get:Rule val rule = createComposeRule()

    private val camera = WifiDevice(DeviceEvidence("192.168.1.23", name = "IPC-3F2A", openPorts = setOf(80, 554, 8899)), Verdict(Risk.LIKELY_CAMERA, listOf(Sign.Onvif, Sign.Rtsp(554), Sign.CameraMaker("Xiongmai")), "Xiongmai"), 0)
    private val web = WifiDevice(DeviceEvidence("192.168.1.41", openPorts = setOf(8080)), Verdict(Risk.CHECK_THIS, listOf(Sign.WebPage)), 0)
    private val quiet = WifiDevice(DeviceEvidence("192.168.1.9"), Verdict(Risk.UNKNOWN, emptyList()), 0)
    private val tv = WifiDevice(DeviceEvidence("192.168.1.19", name = "Living-room TV"), Verdict(Risk.KNOWN, listOf(Sign.KnownKind("tv")), kind = "tv"), 0)
    private val router = WifiDevice(DeviceEvidence("192.168.1.1", isGateway = true), Verdict(Risk.KNOWN, listOf(Sign.Router)), 0)
    private val phone = WifiDevice(DeviceEvidence("192.168.1.14", isSelf = true), Verdict(Risk.KNOWN, listOf(Sign.ThisPhone)), 0)
    private fun wifi(vararg d: WifiDevice, isolated: Boolean = false, ssid: String? = "SeaView_Guest") = WifiSnapshot(1, WifiStatus.DONE, ssid, 1f, d.size, d.size, 0, d.toList(), isolated)

    private val v380 = NearbyDevice(Check.HOTSPOTS, "a", "V380-8A12F0", -48, NearbyKind.CAMERA_HOTSPOT, Risk.LIKELY_CAMERA, "V380")
    private val smartLife = NearbyDevice(Check.HOTSPOTS, "b", "SmartLife-1A2B", -70, NearbyKind.CAMERA_HOTSPOT, Risk.CHECK_THIS, "Tuya")
    private val cafe = NearbyDevice(Check.HOTSPOTS, "c", "Cafe", -81, NearbyKind.NETWORK, Risk.UNKNOWN)
    private val tile = NearbyDevice(Check.BLUETOOTH, "AA", null, -60, NearbyKind.TRACKER, Risk.CHECK_THIS, "Tile")
    private val buds = NearbyDevice(Check.BLUETOOTH, "BB", "Buds", -50, NearbyKind.BLE_OTHER, Risk.UNKNOWN)
    private fun nearby(check: Check, vararg d: NearbyDevice, status: NearbyStatus = NearbyStatus.DONE, block: NearbyBlock? = null) =
        NearbySnapshot(check, 1, status, block, 0.5f, 4, d.toList(), 1)

    // --- 03 Sweep in progress -----------------------------------------------------------------------

    @Test fun sweepShowsTheWifiScanWithWhatWasFoundSoFar() {
        var skipped = 0
        rule.setKeyholeContent {
            SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.RUNNING, "SeaView_Guest", 0.6f, 11, 18, 10, listOf(camera, tv))), {}, { skipped++ }, {})
        }
        rule.onNodeWithText(text(R.string.place_stay)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.checking_wifi)).assertIsDisplayed()
        rule.onNodeWithContentDescription("11 ${text(R.string.of_total, 18)}").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.two_parts, "SeaView_Guest", plural(R.plurals.about_seconds_left, 10, 10))).assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.likely_cameras_count, 1, 1)).assertIsDisplayed()
        rule.onNodeWithText("IPC-3F2A").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.skip_step)).performClick()
        assertEquals(1, skipped)
    }

    @Test fun sweepCountsFoundDevicesBeforeItKnowsTheTotalAndHidesAnUnknownWifiName() {
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.RUNNING, null, 0.1f, 0, 0, 15, listOf(phone))), {}, {}, {}) }
        rule.onNodeWithContentDescription("1 ${text(R.string.found_line)}").assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.about_seconds_left, 15, 15)).assertIsDisplayed()
    }

    @Test fun sweepWithNothingFoundYetSaysSo() {
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.RUNNING)), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.nothing_answered_yet)).assertIsDisplayed()
    }

    @Test fun notOnWifiOffersScanAgain() {
        var retried = 0
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.NOT_ON_WIFI)), {}, {}, { retried++ }) }
        rule.onNodeWithText(text(R.string.not_on_wifi_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.scan_again)).performClick()
        assertEquals(1, retried)
    }

    @Test fun aSingleCheckHasNoSkipAndIsTitledByTheCheck() {
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, listOf(Check.WIFI), Check.WIFI, single = true, wifi = WifiSnapshot(1, WifiStatus.RUNNING)), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.check_wifi)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.skip_step)).assertCountEquals(0)
    }

    @Test fun aStepThatNeedsPermissionSaysWhyAndOffersAllow() {
        var allowed = 0
        rule.setKeyholeContent {
            SweepContent(SweepState(Place.STAY, Check.entries, Check.HOTSPOTS, nearby = nearby(Check.HOTSPOTS, status = NearbyStatus.NEEDS_PERMISSION)), {}, {}, {}, onAllow = { allowed++ })
        }
        rule.onNodeWithText(text(R.string.perm_hotspots_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.perm_hotspots_line)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.allow)).performClick()
        assertEquals(1, allowed)
    }

    @Test fun afterDontAllowTheButtonOpensSettingsAndTheLineSaysSo() {
        rule.setKeyholeContent {
            SweepContent(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH, nearby = nearby(Check.BLUETOOTH, status = NearbyStatus.NEEDS_PERMISSION)), {}, {}, {}, settingsOnly = true)
        }
        rule.onNodeWithText(text(R.string.perm_bluetooth_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.perm_denied_line), substring = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.open_settings)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.allow)).assertCountEquals(0)
    }

    private fun blockedOffersScanAgain(check: Check, block: NearbyBlock, title: Int) {
        var retried = 0
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, check, nearby = nearby(check, status = NearbyStatus.UNAVAILABLE, block = block)), {}, {}, { retried++ }) }
        rule.onNodeWithText(text(title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.scan_again)).performClick()
        assertEquals(1, retried)
    }

    @Test fun wifiSwitchedOffSaysSoAndOffersScanAgain() = blockedOffersScanAgain(Check.HOTSPOTS, NearbyBlock.WIFI_OFF, R.string.block_wifi_off_title)

    @Test fun bluetoothSwitchedOffSaysSoAndOffersScanAgain() = blockedOffersScanAgain(Check.BLUETOOTH, NearbyBlock.BLUETOOTH_OFF, R.string.block_bluetooth_off_title)

    @Test fun locationSwitchedOffSaysSoAndOffersScanAgain() = blockedOffersScanAgain(Check.HOTSPOTS, NearbyBlock.LOCATION_OFF, R.string.block_location_off_title)

    @Test fun aPhoneWithNoBluetoothHasNothingToRetryButCanSkip() {
        var skipped = 0
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH, nearby = nearby(Check.BLUETOOTH, status = NearbyStatus.UNAVAILABLE, block = NearbyBlock.NO_BLUETOOTH)), {}, { skipped++ }, {}) }
        rule.onNodeWithText(text(R.string.block_no_bluetooth_title)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.scan_again)).assertCountEquals(0)
        rule.onNodeWithText(text(R.string.skip_step)).performClick()
        assertEquals(1, skipped)
    }

    @Test fun aNearbyStepListsOnlyWhatIsWorthALook() {
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, Check.HOTSPOTS, nearby = nearby(Check.HOTSPOTS, v380, cafe, status = NearbyStatus.RUNNING)), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.checking_hotspots)).assertIsDisplayed()
        rule.onNodeWithContentDescription("2 ${text(R.string.found_line)}").assertIsDisplayed()
        rule.onNodeWithText("V380-8A12F0").assertIsDisplayed()
        rule.onAllNodesWithText("Cafe").assertCountEquals(0)
    }

    @Test fun aNearbyStepWithOnlyPlainNetworksSaysNothingIsWorthALookYet() {
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, Check.BLUETOOTH, nearby = nearby(Check.BLUETOOTH, buds, status = NearbyStatus.RUNNING)), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.checking_bluetooth)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nothing_flagged_yet)).assertIsDisplayed()
    }

    @Test fun aHandsOnStepDrawsOnlyItsCheckRowWhileItsScreenOpens() {
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, Check.MAGNET), {}, {}, {}) }
        rule.onNodeWithContentDescription("${text(R.string.check_magnet)}, ${text(R.string.check_state_current)}").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.skip_step)).assertIsDisplayed()
    }

    // --- 04 Wi-Fi results ---------------------------------------------------------------------------

    @Test fun wifiResultsListLikelyCamerasFirstAndGoOn() {
        val opened = mutableListOf<String>()
        var next: Check? = null
        rule.setKeyholeContent { WifiResultsContent(WifiResultsState(wifi(camera, web, quiet, tv, router), Check.HOTSPOTS, true), {}, {}, { opened += it }, {}, { next = it }) }
        rule.onNodeWithText(plural(R.plurals.devices_on_wifi, 5, 5, "SeaView_Guest"), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("IPC-3F2A").performClick()
        rule.onNodeWithText("192.168.1.41").performClick()
        assertEquals(listOf("192.168.1.23", "192.168.1.41"), opened)
        rule.onNodeWithText(plural(R.plurals.known_devices, 2, 2)).assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.other_devices, 1, 1)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.next_hotspots)).performClick()
        assertEquals(Check.HOTSPOTS, next)
    }

    @Test fun onlyTheRouterAnsweringShowsTheIsolationNoteFirst() {
        rule.setKeyholeContent { WifiResultsContent(WifiResultsState(wifi(router, phone, isolated = true), null, true), {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.isolation_title), useUnmergedTree = true).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.wifi_none_title), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun nothingLookingLikeACameraSaysWhatWasCheckedNeverThatTheRoomIsFine() {
        rule.setKeyholeContent { WifiResultsContent(WifiResultsState(wifi(tv, router, phone), null, true), {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.wifi_none_title), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.wifi_none_text), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun aCheckRunAloneEndsWithDoneAndAWifiWithNoNameSaysThisWifi() {
        var done = false
        var again = 0
        rule.setKeyholeContent { WifiResultsContent(WifiResultsState(wifi(camera, ssid = null), null, true), {}, { again++ }, {}, {}, { done = it == null }) }
        rule.onNodeWithText(plural(R.plurals.devices_on_this_wifi, 1, 1), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.scan_again)).performClick()
        rule.onNodeWithText(text(R.string.done)).performClick()
        assertTrue(done)
        assertEquals(1, again)
    }

    @Test fun theOtherDevicesListOpensEachDevice() {
        val opened = mutableListOf<String>()
        rule.setKeyholeContent { WifiOthersContent(wifi(camera, quiet, tv), {}, { opened += it }) }
        rule.onAllNodesWithText("IPC-3F2A").assertCountEquals(0)
        rule.onNodeWithText("192.168.1.9").performClick()
        rule.onNodeWithText("Living-room TV").performClick()
        assertEquals(listOf("192.168.1.9", "192.168.1.19"), opened)
    }

    // --- 05 Device ----------------------------------------------------------------------------------

    @Test fun aLikelyCameraSaysWhyAndOffersToFindIt() {
        val taps = mutableListOf<String>()
        rule.setKeyholeContent { DeviceContent(camera, {}, { taps += "share" }, { taps += "not" }, { taps += "found" }, { taps += "lens" }) }
        rule.onNodeWithText("IPC-3F2A").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.risk_likely_camera)).assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.strong_signs, 3, 3)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.sign_onvif)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.sign_rtsp, 554)).assertIsDisplayed()
        rule.onNodeWithText("80 · 554 · 8899", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.maker_likely, "Xiongmai"), useUnmergedTree = true).assertExists()
        rule.onNodeWithText(text(R.string.not_a_camera)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.found_it)).performClick()
        rule.onNodeWithText(text(R.string.find_its_lens)).performClick()
        rule.onNodeWithContentDescription(text(R.string.share)).performClick()
        assertEquals(listOf("not", "found", "lens", "share"), taps)
    }

    @Test fun aKnownDeviceHasNoFindButtonsAndThisPhoneCannotBeMarked() {
        rule.setKeyholeContent { DeviceContent(phone, {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.sign_this_phone)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.find_its_lens)).assertCountEquals(0)
        rule.onAllNodesWithText(text(R.string.not_a_camera)).assertCountEquals(0)
        rule.onNodeWithText(text(R.string.none_found), useUnmergedTree = true).assertExists()
    }

    @Test fun aDeviceThatShowsNothingSaysSoAndAMarkedOneSaysWhoMarkedIt() {
        rule.setKeyholeContent { DeviceContent(quiet, {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.sign_none)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.risk_not_recognised)).assertIsDisplayed()
        // Not recognised can still be looked for (05a), and it says why its maker isn't shown.
        rule.onNodeWithText(text(R.string.find_its_lens)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.maker_hidden), useUnmergedTree = true).assertExists()
    }

    @Test fun aMarkedDeviceKeepsItsSignsAndSaysItWasMarked() {
        val marked = camera.copy(verdict = Verdict(Risk.KNOWN, listOf(Sign.MarkedByUser, Sign.Onvif), "Xiongmai"), knownAs = KnownAs.ROOM)
        rule.setKeyholeContent { DeviceContent(marked, {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.sign_marked)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.sign_onvif)).assertIsDisplayed()
        // Marked (E15b): the row says so and offers to change or undo it.
        rule.onNodeWithText(text(R.string.marked_change_line)).assertExists()
        rule.onAllNodesWithText(text(R.string.find_its_lens)).assertCountEquals(0)
    }

    // --- 06 Not a camera? ---------------------------------------------------------------------------

    @Test fun theSheetMarksAsMineByDefault() {
        var marked: KnownAs? = null
        var called = false
        rule.setKeyholeContent { NotACameraSheet(null, {}, { marked = it; called = true }, {}) }
        rule.onNodeWithText(text(R.string.mark_as_known)).performClick()
        assertTrue(called)
        assertEquals(KnownAs.MINE, marked)
    }

    @Test fun theSheetMarksAsPartOfTheRoom() {
        var marked: KnownAs? = null
        rule.setKeyholeContent { NotACameraSheet(null, {}, { marked = it }, {}) }
        rule.onNodeWithText(text(R.string.part_of_the_room)).performClick()
        rule.onNodeWithText(text(R.string.mark_as_known)).performClick()
        assertEquals(KnownAs.ROOM, marked)
    }

    @Test fun notSureTakesTheMarkAwayAndTheButtonSaysDone() {
        var marked: KnownAs? = KnownAs.ROOM
        rule.setKeyholeContent { NotACameraSheet(KnownAs.ROOM, {}, { marked = it }, {}) }
        rule.onNodeWithText(text(R.string.not_sure)).performClick()
        rule.onAllNodesWithText(text(R.string.mark_as_known)).assertCountEquals(0)
        rule.onNodeWithText(text(R.string.done)).performClick()
        assertEquals(null, marked)
    }

    @Test fun cancelAndReportReachTheCaller() {
        val taps = mutableListOf<String>()
        rule.setKeyholeContent { NotACameraSheet(null, { taps += "dismiss" }, { taps += "mark" }, { taps += "report" }) }
        rule.onNodeWithText(text(R.string.report_wrong)).performClick()
        rule.onNodeWithText(text(R.string.cancel)).performClick()
        assertEquals(listOf("report", "dismiss"), taps)
    }

    // --- Hotspot and Bluetooth results --------------------------------------------------------------

    @Test fun hotspotResultsListCameraNamesAndKeepPlainNetworksBehindOneRow() {
        val found = mutableListOf<String>()
        var others = 0
        rule.setKeyholeContent { NearbyResultsContent(NearbyResultsState(nearby(Check.HOTSPOTS, v380, smartLife.copy(added = true), cafe), Check.BLUETOOTH, 12, true), {}, {}, { found += it }, { others++ }, {}) }
        // "Updated 12 s ago" never breaks inside itself (no-break spaces).
        rule.onNodeWithText(text(R.string.two_parts, plural(R.plurals.networks_nearby, 3, 3), plural(R.plurals.updated_ago, 12, 12).replace(' ', '\u00A0')), useUnmergedTree = true).assertIsDisplayed()
        // A row says what it is (04f); the reason is on Find it.
        rule.onAllNodesWithText(text(R.string.kind_camera_hotspot), substring = true).onFirst().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.two_parts, text(R.string.kind_camera_hotspot), text(R.string.added_to_report))).assertIsDisplayed()
        rule.onAllNodesWithText("Cafe").assertCountEquals(0)
        rule.onNodeWithText("V380-8A12F0").performClick()
        rule.onNodeWithText(plural(R.plurals.other_networks, 1, 1)).performClick()
        assertEquals(listOf("a"), found)
        assertEquals(1, others)
        rule.onNodeWithText(text(R.string.next_bluetooth)).assertIsDisplayed()
    }

    @Test fun bluetoothResultsNameANamelessTrackerByItsFamily() {
        rule.setKeyholeContent { NearbyResultsContent(NearbyResultsState(nearby(Check.BLUETOOTH, tile, buds), null, 0, true), {}, {}, {}, {}, {}) }
        rule.onNodeWithText("Tile").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.kind_tracker)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.two_parts, plural(R.plurals.bluetooth_nearby, 2, 2), text(R.string.updated_now).replace(' ', '\u00A0')), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.done)).assertIsDisplayed()
    }

    @Test fun nearbyResultsWithNothingFlaggedSayWhatThatMeans() {
        rule.setKeyholeContent { NearbyResultsContent(NearbyResultsState(nearby(Check.HOTSPOTS, cafe), null, 0, true), {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.hotspots_none_title), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun nearbyResultsWithNothingAtAllStillDraw() {
        rule.setKeyholeContent { NearbyResultsContent(NearbyResultsState(nearby(Check.BLUETOOTH), null, 0, true), {}, {}, {}, {}, {}) }
        // Nothing heard at all (04h): the count box at 0 with its quiet line, and the check's own empty state.
        rule.onNodeWithText(text(R.string.summary_nothing), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nothing_flagged_yet), useUnmergedTree = true).assertExists()
        rule.onNodeWithText(text(R.string.two_parts, plural(R.plurals.bluetooth_nearby, 0, 0), text(R.string.updated_now).replace(' ', '\u00A0')), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun aMarkedHotspotIsListedUnderMarkedAsKnown() {
        rule.setKeyholeContent { NearbyResultsContent(NearbyResultsState(nearby(Check.HOTSPOTS, v380.copy(risk = Risk.KNOWN, knownAs = KnownAs.MINE)), null, 0, true), {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.marked_as_known)).assertIsDisplayed()
        rule.onNodeWithText("V380-8A12F0").assertIsDisplayed()
    }

    @Test fun theOtherNetworksListOpensFindItAndAnEmptyOneSaysSo() {
        val found = mutableListOf<String>()
        rule.setKeyholeContent { NearbyOthersContent(nearby(Check.HOTSPOTS, v380, cafe), {}, { found += it }) }
        rule.onAllNodesWithText("V380-8A12F0").assertCountEquals(0)
        rule.onNodeWithText("Cafe").performClick()
        assertEquals(listOf("c"), found)
    }

    // --- 07 Find it ---------------------------------------------------------------------------------

    @Test fun findItSaysHowNearInWordsAndWhichWayTheSignalGoes() {
        val taps = mutableListOf<String>()
        rule.setKeyholeContent { FindItContent(FindItState(v380, -48, Nearness.CLOSE, 4, Trend.STRONGER, 12, loaded = true), {}, { taps += "not" }, { taps += "add" }) }
        rule.onNodeWithText(text(R.string.find_it)).assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.updated_ago, 12, 12)).assertIsDisplayed()
        rule.onNodeWithContentDescription("${text(R.string.nearness_close)} ${text(R.string.nearness_close_line)}").assertIsDisplayed()
        rule.onNodeWithText("V380-8A12F0").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.two_parts, text(R.string.kind_camera_hotspot), text(R.string.signal_dbm, -48).replace("-", "\u2212"))).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.getting_warmer)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.trend_stronger)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.find_note_hotspots), useUnmergedTree = true).assertExists()
        rule.onNodeWithText(text(R.string.not_a_camera)).performClick()
        rule.onNodeWithText(text(R.string.add_to_report)).performClick()
        assertEquals(listOf("not", "add"), taps)
    }

    @Test fun findItForSomethingAlreadyInTheReportOffersToTakeItOut() {
        rule.setKeyholeContent { FindItContent(FindItState(tile.copy(added = true), -60, Nearness.CLOSE, 3, Trend.STEADY, 1, loaded = true), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.remove_from_report)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.holding)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.updated_now)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.find_note_bluetooth), useUnmergedTree = true).assertExists()
    }

    @Test fun findItSaysWhenTheSignalIsLostAndWhenItFades() {
        rule.setKeyholeContent { FindItContent(FindItState(tile, -92, Nearness.FAR, 1, Trend.WEAKER, 40, lost = true, loaded = true), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.find_lost_title), useUnmergedTree = true).assertIsDisplayed()
        // Not heard for a while: the meter shows the last signal (07c).
        rule.onNodeWithText(text(R.string.last_signal)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.trend_weaker)).assertIsDisplayed()
        rule.onNodeWithContentDescription("${text(R.string.nearness_far)} ${text(R.string.nearness_far_line)}").assertIsDisplayed()
    }

    @Test fun findItWithNothingToFindDrawsNothing() {
        rule.setKeyholeContent { FindItContent(FindItState(loaded = true), {}, {}, {}) }
        rule.onAllNodesWithText(text(R.string.find_it)).assertCountEquals(0)
    }

    // --- 08 Lens finder and Infrared ------------------------------------------------------------------

    private val backCamera = app.keyhole.privacy.optics.CameraSpec("0", app.keyhole.privacy.optics.Facing.BACK, 90, true, android.util.Size(1440, 1080), android.util.Size(320, 240), 8f, null, 2)
    private val noTorchNoZoom = backCamera.copy(hasTorch = false, maxZoom = 1f)

    private fun lens(s: LensState, taps: MutableList<String> = mutableListOf(), settingsOnly: Boolean = false) = rule.setKeyholeContent {
        app.keyhole.privacy.ui.theme.ViewfinderTheme {
            LensContent(
                s, onBack = { taps += "back" }, onNext = { taps += "next" }, onMode = { taps += "mode:$it" }, onFilter = { taps += "filter:$it" },
                onZoom = { taps += "zoom" }, onTorch = { taps += "torch" }, onTips = { taps += "tips" }, onShutter = { taps += "shutter" },
                onAllow = { taps += "allow" }, settingsOnly = settingsOnly, onRetry = { taps += "retry" },
            )
        }
    }

    @Test fun theLensFinderOffersFiltersZoomTorchAndTheShutter() {
        val taps = mutableListOf<String>()
        lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, spots = listOf(app.keyhole.privacy.optics.Spot(0.5f, 0.5f, 0.01f, 250)), photos = 2), taps)
        rule.onNodeWithText(text(R.string.lens_finder)).assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.spots_count, 1, 1)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.lens_hint)).assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.photos_saved, 2, 2)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.filter_red)).performClick()
        rule.onNodeWithText(text(R.string.zoom_2x)).performClick()
        rule.onNodeWithContentDescription(text(R.string.torch)).performClick()
        rule.onNodeWithContentDescription(text(R.string.take_photo)).performClick()
        rule.onNodeWithContentDescription(text(R.string.tips)).performClick()
        rule.onNodeWithText(text(R.string.check_infrared)).performClick()
        assertEquals(listOf("filter:RED", "zoom", "torch", "shutter", "tips", "mode:INFRARED"), taps)
        // Not part of a sweep: no Next.
        rule.onAllNodesWithText(text(R.string.next)).assertCountEquals(0)
    }

    @Test fun aCameraWithoutTorchOrZoomHidesThem() {
        lens(LensState(status = CameraStatus.RUNNING, spec = noTorchNoZoom))
        rule.onAllNodesWithText(text(R.string.zoom_2x)).assertCountEquals(0)
        rule.onAllNodesWithContentDescription(text(R.string.torch)).assertCountEquals(0)
    }

    @Test fun infraredHasNoTorchAndWarnsWhenThePhoneCantSeeIt() {
        lens(LensState(mode = Check.INFRARED, status = CameraStatus.RUNNING, spec = backCamera, seesInfrared = false))
        rule.onNodeWithText(text(R.string.infrared_finder)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.infrared_hint)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.ir_blocked_title), useUnmergedTree = true).assertIsDisplayed()
        rule.onAllNodesWithContentDescription(text(R.string.torch)).assertCountEquals(0)
    }

    @Test fun whileTheCameraStartsTheShutterWaits() {
        val taps = mutableListOf<String>()
        lens(LensState(status = CameraStatus.STARTING, spec = backCamera), taps)
        rule.onNodeWithContentDescription(text(R.string.take_photo)).performClick()
        assertTrue(taps.isEmpty())
    }

    @Test fun withoutTheCameraPermissionItSaysWhyAndAsks() {
        val taps = mutableListOf<String>()
        lens(LensState(status = CameraStatus.NEEDS_PERMISSION), taps)
        rule.onNodeWithText(text(R.string.perm_camera_title)).assertIsDisplayed()
        rule.onAllNodesWithContentDescription(text(R.string.take_photo)).assertCountEquals(0)
        rule.onNodeWithText(text(R.string.allow)).performClick()
        assertEquals(listOf("allow"), taps)
    }

    @Test fun afterDontAllowItOpensSettings() {
        lens(LensState(status = CameraStatus.NEEDS_PERMISSION), settingsOnly = true)
        rule.onNodeWithText(text(R.string.open_settings)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.perm_denied_line)).assertIsDisplayed()
    }

    @Test fun aCameraInUseOffersTryAgainAndNoCameraOffersNothing() {
        val taps = mutableListOf<String>()
        lens(LensState(status = CameraStatus.IN_USE, spec = backCamera), taps)
        rule.onNodeWithText(text(R.string.camera_in_use_line)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.try_again)).performClick()
        assertEquals(listOf("retry"), taps)
    }

    @Test fun aPhoneWithNoCameraSaysSoAndCanStillSwitchMode() {
        lens(LensState(status = CameraStatus.NO_CAMERA))
        rule.onNodeWithText(text(R.string.no_camera_title)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.try_again)).assertCountEquals(0)
        rule.onNodeWithText(text(R.string.check_infrared)).assertIsDisplayed()
    }

    @Test fun inASweepTheLensFinderClosesAndGoesOn() {
        val taps = mutableListOf<String>()
        lens(LensState(status = CameraStatus.RUNNING, spec = backCamera, sweep = true, next = Check.INFRARED), taps)
        rule.onNodeWithText(text(R.string.next)).performClick()
        rule.onNodeWithContentDescription(text(R.string.close)).performClick()
        assertEquals(listOf("next", "back"), taps)
    }

    // --- 08 Magnet ------------------------------------------------------------------------------------

    private fun magnet(s: MagnetState, taps: MutableList<String> = mutableListOf()) = rule.setKeyholeContent {
        MagnetContent(s, onBack = { taps += "back" }, onNext = { taps += "next" }, onBeep = { taps += "beep" }, onSetNormal = { taps += "normal" }, onAdd = { taps += "add" })
    }

    @Test fun whileLearningTheMagnetSaysSoAndCantBeAdded() {
        val taps = mutableListOf<String>()
        magnet(MagnetState(learned = 0.4f), taps)
        rule.onNodeWithText(text(R.string.magnet_learning_title)).assertIsDisplayed()
        rule.onNodeWithContentDescription("${text(R.string.magnet_learning_value, 40)} ${text(R.string.magnet_learning_unit)}").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.add_to_report)).performClick()
        rule.onNodeWithText(text(R.string.set_normal_again)).performClick()
        assertTrue(taps.isEmpty())
    }

    @Test fun aJumpSaysElectronicsNeverCamera() {
        magnet(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(1, 2, 14)))
        rule.onNodeWithText(text(R.string.magnet_jumped_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.magnet_now_line, 48, 62)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.magnet_jumped)).assertIsDisplayed()
        rule.onNodeWithContentDescription("${text(R.string.magnet_above_value, 14)} ${text(R.string.magnet_above_line)}").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.magnet_note), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.risk_likely_camera)).assertCountEquals(0)
    }

    @Test fun aSteadyFieldIsAboutNormalAndCanBeAddedOnce() {
        val taps = mutableListOf<String>()
        magnet(MagnetState(learning = false, learned = 1f, above = 2, normal = 48, now = 50), taps)
        rule.onNodeWithText(text(R.string.magnet_normal_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.trend_steady)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.add_to_report)).performClick()
        rule.onNodeWithText(text(R.string.set_normal_again)).performClick()
        rule.onNodeWithContentDescription(text(R.string.beep_on_jump)).performClick()
        assertEquals(listOf("add", "normal", "beep"), taps)
    }

    @Test fun anAddedReadingSaysSoAndCantBeAddedAgain() {
        val taps = mutableListOf<String>()
        magnet(MagnetState(learning = false, learned = 1f, above = 20, normal = 48, now = 68, jumped = true, added = true), taps)
        rule.onNodeWithText(text(R.string.added_to_report)).performClick()
        assertTrue(taps.isEmpty())
    }

    @Test fun aPhoneWithNoMagnetSensorSaysSoWithNoButtons() {
        magnet(MagnetState(available = false))
        rule.onNodeWithText(text(R.string.no_magnet_title)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.add_to_report)).assertCountEquals(0)
        rule.onAllNodesWithContentDescription(text(R.string.beep_on_jump)).assertCountEquals(0)
    }

    @Test fun inASweepTheMagnetGoesOn() {
        val taps = mutableListOf<String>()
        magnet(MagnetState(sweep = true, next = Check.CHECKLIST), taps)
        rule.onNodeWithText(text(R.string.next)).performClick()
        assertEquals(listOf("next"), taps)
    }

    // --- 09 Checklist -------------------------------------------------------------------------------

    @Test fun theChecklistShowsHowFarAlongAndEveryItemOfThePlace() {
        val tapped = mutableListOf<app.keyhole.privacy.domain.ChecklistItem>()
        val answers = mapOf(app.keyhole.privacy.domain.ChecklistItem.MIRROR_LIGHT to app.keyhole.privacy.domain.ChecklistEntry(app.keyhole.privacy.domain.ChecklistAnswer.LOOKS_FINE))
        rule.setKeyholeContent { ChecklistContent(ChecklistState(Place.TRIAL_ROOM, answers = answers), {}, { tapped += it }, {}) }
        rule.onNodeWithText(text(R.string.checklist_of, 1, 7)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.section_mirror)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.item_hooks)).performScrollTo().performClick()
        assertEquals(listOf(app.keyhole.privacy.domain.ChecklistItem.HOOKS), tapped)
        rule.onNodeWithText(text(R.string.see_report)).assertIsDisplayed()
    }

    @Test fun inASweepWithMoreStepsTheChecklistGoesOn() {
        var next = 0
        rule.setKeyholeContent { ChecklistContent(ChecklistState(Place.STAY, sweep = true, next = Check.MAGNET), {}, {}, { next++ }) }
        rule.onNodeWithText(text(R.string.next_magnet)).performClick()
        assertEquals(1, next)
    }

    @Test fun anAnswerIsPickedThenSaved() {
        val got = mutableListOf<app.keyhole.privacy.domain.ChecklistEntry?>()
        rule.setKeyholeContent { AnswerSheet(app.keyhole.privacy.domain.ChecklistItem.HOOKS, null, {}, { got += it }) }
        // Nothing picked yet: Save waits, Cancel is offered (nothing to undo).
        rule.onNodeWithText(text(R.string.save)).assertIsNotEnabled()
        rule.onNodeWithText(text(R.string.cancel)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.looks_fine)).performClick()
        rule.onNodeWithText(text(R.string.skip)).performClick()
        assertEquals(emptyList<Any>(), got)
        rule.onNodeWithText(text(R.string.save)).performClick()
        assertEquals(listOf(app.keyhole.privacy.domain.ChecklistAnswer.SKIPPED), got.map { it?.answer })
    }

    @Test fun notSureOpensANoteAndSavesIt() {
        var got: app.keyhole.privacy.domain.ChecklistEntry? = null
        rule.setKeyholeContent { AnswerSheet(app.keyhole.privacy.domain.ChecklistItem.HOOKS, null, {}, { got = it }) }
        rule.onNodeWithText(text(R.string.not_sure)).performClick()
        rule.onNodeWithContentDescription(text(R.string.note_label)).performTextInput("loose hook")
        rule.onNodeWithText(text(R.string.add_photo)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.save)).performClick()
        assertEquals(app.keyhole.privacy.domain.ChecklistAnswer.NOT_SURE, got?.answer)
        assertEquals("loose hook", got?.note)
    }

    @Test fun anAnswerCanBeTakenBack() {
        var got: app.keyhole.privacy.domain.ChecklistEntry? = app.keyhole.privacy.domain.ChecklistEntry(app.keyhole.privacy.domain.ChecklistAnswer.LOOKS_FINE)
        rule.setKeyholeContent { AnswerSheet(app.keyhole.privacy.domain.ChecklistItem.HOOKS, got, {}, { got = it }) }
        rule.onNodeWithText(text(R.string.undo)).performClick()
        assertEquals(null, got)
    }

    // --- 10 Report ----------------------------------------------------------------------------------

    private fun report(words: (app.keyhole.privacy.ui.report.ReportWords) -> Unit = {}, r: app.keyhole.privacy.data.SavedReport = app.keyhole.privacy.ui.report.previewReport(), taps: MutableList<String> = mutableListOf()) =
        rule.setKeyholeContent {
            ReportContent(
                r, app.keyhole.privacy.ui.report.rememberReportWords().also(words), {}, { taps += "share" }, { taps += "rename" }, { taps += "finding:${it.id}" },
                { taps += "photo" }, { taps += "found" }, { taps += "export" },
            )
        }

    @Test fun aReportSaysWhatToLookAtWhatWasCheckedAndWhatCouldntBe() {
        val taps = mutableListOf<String>()
        report(taps = taps)
        rule.onNodeWithText("Sea View Hotel, room 304").assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.to_look_at, 2, 2)).assertIsDisplayed()
        rule.onNodeWithText(plural(R.plurals.checks_of, 7, 6, 7)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.step_was_skipped, text(R.string.check_infrared))).assertIsDisplayed()
        rule.onNodeWithText("IPC-3F2A").performClick()
        rule.onNodeWithText(text(R.string.couldnt_check)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.limit_memory_card_title)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.gap_memory_card_line)).assertExists()
        rule.onNodeWithText(text(R.string.report_not_safe_note), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.marked_room_undo)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.export)).performClick()
        rule.onNodeWithText(text(R.string.found_something)).performClick()
        rule.onNodeWithContentDescription(text(R.string.rename)).performClick()
        rule.onNodeWithContentDescription(text(R.string.share)).performClick()
        assertEquals(listOf("finding:1", "finding:3", "export", "found", "rename", "share"), taps)
    }

    @Test fun aReportWithNothingToLookAtSaysNothingFoundAndStillListsWhatCouldntBeChecked() {
        report(r = app.keyhole.privacy.ui.report.previewReport().copy(name = "", findings = emptyList(), toLookAt = 0, ran = Check.entries, planned = Check.entries))
        rule.onNodeWithText(text(R.string.place_stay)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nothing_found)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.all_checks_ran)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.couldnt_check)).performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.found)).assertCountEquals(0)
    }

    @Test fun exportResultsAreToastsNeverNotes() {
        // Saved (10h) and not saved (10i) are both toasts from the screen; the report itself shows no note for either.
        report()
        rule.onAllNodesWithText(text(R.string.export_done), useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText(text(R.string.export_failed), useUnmergedTree = true).assertCountEquals(0)
    }


    @Test fun theRenameSheetSavesWhatWasTyped() {
        var saved = ""
        rule.setKeyholeContent { app.keyhole.privacy.ui.report.RenameSheet("", {}, { saved = it }) }
        rule.onNodeWithContentDescription(text(R.string.rename)).performTextInput("Room 304")
        rule.onNodeWithText("Room 304", useUnmergedTree = true).assertExists()
        rule.onNodeWithText(text(R.string.save)).performClick()
        assertEquals("Room 304", saved)
    }

    // --- 11 If you find a camera, 15 Help numbers ---------------------------------------------------

    @Test fun withoutAReportThereIsOnlyTheCallButton() {
        val dialled = mutableListOf<String>()
        rule.setKeyholeContent { FoundCameraContent(HelpState(app.keyhole.privacy.data.HelpEntry("DE", "112", "110"), true), {}, {}, { dialled += it }, {}, null) }
        rule.onNodeWithText(text(R.string.found_step_1)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.share_report)).assertCountEquals(0)
        rule.onNodeWithText(text(R.string.police)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.call_number, "112")).performClick()
        assertEquals(listOf("110", "112"), dialled)
    }

    @Test fun anUnknownRegionStillOffers112AndASiteRowOnlyWhereKnown() {
        var shared = 0
        val sites = mutableListOf<String>()
        rule.setKeyholeContent { FoundCameraContent(HelpState(null, true), {}, {}, {}, { sites += it }, { shared++ }) }
        rule.onNodeWithText(text(R.string.emergency_unknown_line, "112")).performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.report_to_police)).assertCountEquals(0)
        rule.onNodeWithText(text(R.string.share_report)).performClick()
        assertEquals(1, shared)
    }

    @Test fun indiaShowsItsHelplineAndOfficialSite() {
        val sites = mutableListOf<String>()
        rule.setKeyholeContent { FoundCameraContent(HelpState(app.keyhole.privacy.data.HelpEntry("IN", "112", "112", "https://cybercrime.gov.in/", "1930"), false), {}, {}, {}, { sites += it }, null) }
        rule.onNodeWithText(text(R.string.helpline)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.report_to_police)).performScrollTo().performClick()
        assertEquals(listOf("https://cybercrime.gov.in/"), sites)
        rule.onAllNodesWithText(text(R.string.police)).assertCountEquals(0)
    }

    @Test fun theHelpSheetSearchesAndPicks() {
        var picked: String? = "x"
        val all = listOf(app.keyhole.privacy.data.HelpEntry("DE", "112", "110"), app.keyhole.privacy.data.HelpEntry("IN", "112"), app.keyhole.privacy.data.HelpEntry("US", "911"))
        rule.setKeyholeContent { HelpNumbersSheet(HelpState(all[0], true, null, all, "DE"), {}, { picked = it }) }
        rule.onNodeWithText(text(R.string.use_phone_region)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.search_countries)).performTextInput("zzz")
        rule.onNodeWithText(text(R.string.no_countries_found)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.search_countries)).performTextClearance()
        rule.onNodeWithContentDescription(text(R.string.search_countries)).performTextInput("US")
        rule.onNodeWithText("911").performClick()
        assertEquals("US", picked)
    }

    // --- 12 Reports ---------------------------------------------------------------------------------

    @Test fun theReportsListGroupsSearchesAndDeletesWithALongPress() {
        val opened = mutableListOf<Long>()
        val deleted = mutableListOf<Long>()
        val now = System.currentTimeMillis()
        val list = listOf(app.keyhole.privacy.ui.report.previewReport().copy(id = 7, savedAt = now - 1000), app.keyhole.privacy.ui.report.previewReport().copy(id = 8, name = "Office", savedAt = now - 90L * 24 * 3600 * 1000, toLookAt = 0))
        rule.setKeyholeContent { ReportsContent(ReportsState(list, true), { opened += it }, { deleted += it }, now) }
        rule.onNodeWithText(text(R.string.this_week)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nothing_found)).assertIsDisplayed()
        rule.onNodeWithText("Sea View Hotel, room 304").performClick()
        rule.onNodeWithText("Office").performTouchInput { longClick() }
        assertEquals(listOf(7L), opened)
        assertEquals(listOf(8L), deleted)
        rule.onNodeWithText(text(R.string.reports_footer)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.search)).performClick()
        rule.onNodeWithContentDescription(text(R.string.search_reports)).performTextInput("nowhere")
        rule.onNodeWithText(text(R.string.no_reports_match)).assertIsDisplayed()
    }

    @Test fun noReportsSaysSoWithNoSearch() {
        rule.setKeyholeContent { ReportsContent(ReportsState(emptyList(), true), {}, {}, 0L) }
        rule.onNodeWithText(text(R.string.reports_empty_title)).assertIsDisplayed()
        rule.onAllNodesWithContentDescription(text(R.string.search)).assertCountEquals(0)
    }

    // --- 01 Welcome, 14 Appearance, More's screens --------------------------------------------------

    @Test fun welcomeSaysWhatKeyholeIsAndStarts() {
        var started = 0
        rule.setKeyholeContent { app.keyhole.privacy.ui.welcome.WelcomeScreen { started++ } }
        rule.onNodeWithText(text(R.string.welcome_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.welcome_footer)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.get_started)).performClick()
        assertEquals(1, started)
    }

    @Test fun appearancePicksThemeAccentFontAndTextSize() {
        val taps = mutableListOf<String>()
        rule.setKeyholeContent {
            app.keyhole.privacy.ui.more.AppearanceContent(
                app.keyhole.privacy.data.Settings(), {}, { taps += "mode:$it" }, { taps += "dynamic:$it" }, { taps += "accent" }, { taps += "font:$it" },
                { taps += "size" }, { _, _ -> taps += "move" }, { c, on -> taps += "step:$c:$on" },
            )
        }
        rule.onNodeWithText(text(R.string.theme_dark)).performClick()
        rule.onNodeWithContentDescription(text(R.string.accent_green)).performClick()
        rule.onNodeWithText(text(R.string.font_outfit)).performClick()
        // Text size is a slider (as Weft): one step up.
        rule.onNode(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.SetProgress))
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(2f) }
        // Sweep steps are not on Appearance any more (14): only on their own screen.
        rule.onAllNodesWithText(text(R.string.check_hotspots)).assertCountEquals(0)
        assertTrue(taps.containsAll(listOf("mode:DARK", "accent", "font:OUTFIT", "size")))
    }

    @Test fun markedAsKnownListsMarksAndSaysWhenThereAreNone() {
        val tapped = mutableListOf<String>()
        rule.setKeyholeContent { app.keyhole.privacy.ui.more.MarkedKnownContent(listOf(app.keyhole.privacy.data.Mark("k", KnownAs.MINE, "IPC-3F2A", Check.WIFI, 0)), {}, { tapped += it.key }) }
        rule.onNodeWithText(text(R.string.marked_known_note), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("IPC-3F2A").performClick()
        assertEquals(listOf("k"), tapped)
    }

    @Test fun markedAsKnownWithNothingMarked() {
        rule.setKeyholeContent { app.keyhole.privacy.ui.more.MarkedKnownContent(emptyList(), {}, {}) }
        rule.onNodeWithText(text(R.string.marked_known_none)).assertIsDisplayed()
    }

    @Test fun howItWorksAndLimitsSayEveryCheckAndEveryLimit() {
        rule.setKeyholeContent { Column { app.keyhole.privacy.ui.more.HowItWorksScreen {} } }
        rule.onNodeWithText(text(R.string.how_wifi)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.how_note), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun limitsListTheFourThingsNoScanSees() {
        rule.setKeyholeContent { app.keyhole.privacy.ui.more.LimitsScreen({}) }
        listOf(R.string.limit_memory_card_title, R.string.limit_other_network_title, R.string.limit_isolation_title, R.string.limit_trackers_title)
            .forEach { rule.onNodeWithText(text(it)).performScrollTo().assertIsDisplayed() }
        rule.onNodeWithText(text(R.string.limit_memory_card)).assertExists()
    }

    @Test fun licencesOpenTheirText() {
        val opened = mutableListOf<String?>()
        rule.setKeyholeContent { app.keyhole.privacy.ui.more.LicencesScreen({}, { opened += it.file }) }
        rule.onNodeWithText(text(R.string.lic_gpl)).performClick()
        rule.onNodeWithText(text(R.string.lic_kotlin)).performClick()
        assertEquals(listOf("gpl-3.0.txt", null), opened)
        rule.onAllNodesWithText("CameraX").assertCountEquals(0)
    }

    @Test fun aBundledLicenceTextIsShown() {
        rule.setKeyholeContent { app.keyhole.privacy.ui.more.LicenceTextScreen("Keyhole", "gpl-3.0.txt") {} }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("GNU GENERAL PUBLIC LICENSE", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
