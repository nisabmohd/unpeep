package app.keyhole.privacy.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

    @Test fun aStepThatIsNotBuiltYetSaysSo() {
        rule.setKeyholeContent { SweepContent(SweepState(Place.STAY, Check.entries, Check.MAGNET), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.stand_in_line)).assertIsDisplayed()
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
    }

    @Test fun aMarkedDeviceKeepsItsSignsAndSaysItWasMarked() {
        val marked = camera.copy(verdict = Verdict(Risk.KNOWN, listOf(Sign.MarkedByUser, Sign.Onvif), "Xiongmai"), knownAs = KnownAs.ROOM)
        rule.setKeyholeContent { DeviceContent(marked, {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.sign_marked)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.sign_onvif)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.marked_room_line)).assertExists()
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
        rule.onNodeWithText(text(R.string.two_parts, plural(R.plurals.networks_nearby, 3, 3), plural(R.plurals.updated_ago, 12, 12)), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.reason_hotspot, "V380")).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.two_parts, text(R.string.reason_hotspot_weak), text(R.string.added_to_report))).assertIsDisplayed()
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
        rule.onNodeWithText(text(R.string.reason_tracker, "Tile")).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.two_parts, plural(R.plurals.bluetooth_nearby, 2, 2), text(R.string.updated_now)), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.done)).assertIsDisplayed()
    }

    @Test fun nearbyResultsWithNothingFlaggedSayWhatThatMeans() {
        rule.setKeyholeContent { NearbyResultsContent(NearbyResultsState(nearby(Check.HOTSPOTS, cafe), null, 0, true), {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.hotspots_none_title), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun nearbyResultsWithNothingAtAllStillDraw() {
        rule.setKeyholeContent { NearbyResultsContent(NearbyResultsState(nearby(Check.BLUETOOTH), null, 0, true), {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.bluetooth_none_title), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.two_parts, plural(R.plurals.bluetooth_nearby, 0, 0), text(R.string.updated_now)), useUnmergedTree = true).assertIsDisplayed()
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
        rule.onNodeWithText(text(R.string.two_parts, text(R.string.kind_camera_hotspot), text(R.string.signal_dbm, -48))).assertIsDisplayed()
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
        rule.onNodeWithText(text(R.string.about_the_same)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.updated_now)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.find_note_bluetooth), useUnmergedTree = true).assertExists()
    }

    @Test fun findItSaysWhenTheSignalIsLostAndWhenItFades() {
        rule.setKeyholeContent { FindItContent(FindItState(tile, -92, Nearness.FAR, 1, Trend.WEAKER, 40, lost = true, loaded = true), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.find_lost_title), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.getting_colder)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.trend_weaker)).assertIsDisplayed()
        rule.onNodeWithContentDescription("${text(R.string.nearness_far)} ${text(R.string.nearness_far_line)}").assertIsDisplayed()
    }

    @Test fun findItWithNothingToFindDrawsNothing() {
        rule.setKeyholeContent { FindItContent(FindItState(loaded = true), {}, {}, {}) }
        rule.onAllNodesWithText(text(R.string.find_it)).assertCountEquals(0)
    }
}
