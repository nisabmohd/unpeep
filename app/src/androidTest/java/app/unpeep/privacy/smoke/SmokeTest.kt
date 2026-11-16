package app.unpeep.privacy.smoke

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.unpeep.privacy.MainActivity
import app.unpeep.privacy.R
import app.unpeep.privacy.plural
import app.unpeep.privacy.text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The smoke run: the real app, started as a user starts it, walked through every screen that is built
 * (docs/SCREENS.md). It passes on any phone or emulator, with or without Wi-Fi, Bluetooth or permissions: each
 * check must end on one of its honest states (results, "Not on Wi-Fi", a permission line, a radio that is off),
 * never on a crash or a blank screen. Run: `./gradlew :app:connectedPlayDebugAndroidTest`.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val MAX_DEVICES = 254
    }

    private fun waitFor(text: String, ms: Long = 5_000) = rule.waitUntilAtLeastOneExists(hasText(text), ms)

    /** Home, after Welcome when this is the first run. */
    private fun home() {
        val first = waitForAny(text(R.string.check_this_room), text(R.string.get_started), text(R.string.report_not_safe_note), ms = 10_000)
        if (first == text(R.string.get_started)) rule.onNodeWithText(text(R.string.get_started)).performClick()
        // A sweep ends on its report: back to Home.
        if (first == text(R.string.report_not_safe_note)) rule.onNodeWithContentDescription(text(R.string.back)).performClick()
        waitFor(text(R.string.check_this_room))
        // Every run starts on a stay (an earlier run may have left another place picked).
        val stay = hasText(text(R.string.place_stay)) and isSelectable()
        rule.onNode(stay).performClick()
        rule.waitUntilAtLeastOneExists(stay and isSelected(), 5_000)
    }

    /** Waits until any one of [texts] is on screen and returns it: a check may end in more than one honest way. */
    private fun waitForAny(vararg texts: String, ms: Long = 30_000): String {
        var seen: String? = null
        rule.waitUntil(ms) {
            seen = texts.firstOrNull { rule.onAllNodes(hasText(it, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            seen != null
        }
        return seen!!
    }

    private fun back() {
        rule.onNodeWithContentDescription(text(R.string.back)).performClick()
        rule.waitForIdle()
    }

    @Test fun theAppOpensOnCheckAndEveryTabShows() {
        home()
        rule.onNodeWithText(text(R.string.start)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.or_run_one_check)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.tab_reports)).performClick()
        waitForAny(text(R.string.reports_empty_title), text(R.string.reports_footer), ms = 5_000)
        rule.onNodeWithText(text(R.string.tab_more)).performClick()
        waitFor(text(R.string.marked_as_known))
        rule.onNodeWithText(text(R.string.tab_check)).performClick()
        home()
    }

    @Test fun pickingAPlaceChangesTheSweep() {
        home()
        rule.onNode(hasText(text(R.string.place_trial_room)) and isSelectable()).performClick()
        // The place is saved first and the screen follows what was saved.
        rule.waitUntilAtLeastOneExists(hasText(text(R.string.place_trial_room)) and isSelected(), 5_000)
        // A trial room has no Wi-Fi to join: six checks, and the Wi-Fi check is gone from the row.
        rule.waitUntilDoesNotExist(hasText(text(R.string.check_wifi)), 5_000)
        rule.onNode(hasText(text(R.string.place_stay)) and isSelectable()).performClick()
        waitFor(text(R.string.check_wifi))
    }

    @Test fun everyMoreRowOpensAScreenAndComesBack() {
        home()
        rule.onNodeWithText(text(R.string.tab_more)).performClick()
        for (row in listOf(R.string.appearance, R.string.sweep_steps, R.string.marked_as_known, R.string.how_unpeep_works, R.string.what_unpeep_cant_find, R.string.if_you_find_a_camera, R.string.privacy, R.string.about)) {
            waitFor(text(R.string.tab_more))
            rule.onNodeWithText(text(row)).performScrollTo().performClick()
            if (row == R.string.if_you_find_a_camera) waitFor(text(R.string.found_step_1))
            rule.waitUntilAtLeastOneExists(hasContentDescription(text(R.string.back)), 5_000)
            back()
        }
        waitFor(text(R.string.tab_more))
    }

    @Test fun theWifiCheckEndsOnResultsOrSaysItIsNotOnWifi() {
        home()
        rule.onNodeWithText(text(R.string.check_wifi)).performClick()
        val end = waitForAny(text(R.string.scan_again), text(R.string.not_on_wifi_title))
        if (end == text(R.string.not_on_wifi_title)) {
            rule.onNodeWithText(text(R.string.scan_again)).assertIsDisplayed()
            rule.onNodeWithContentDescription(text(R.string.close)).performClick()
        } else {
            // Results: this phone is always among the devices, and the check ends with Done.
            rule.onNodeWithText(text(R.string.done)).assertIsDisplayed()
            rule.onNodeWithText(text(R.string.done)).performClick()
        }
        home()
    }

    @Test fun aWifiResultOpensTheKnownDevicesAndThisPhone() {
        home()
        rule.onNodeWithText(text(R.string.check_wifi)).performClick()
        val end = waitForAny(text(R.string.done), text(R.string.not_on_wifi_title))
        if (end != text(R.string.done)) return // not on Wi-Fi: covered above
        // This phone is always on its own Wi-Fi, so "n known devices" is always there; it opens the list.
        val known = (1..MAX_DEVICES).map { plural(R.plurals.known_devices, it, it) }
        rule.onNodeWithText(waitForAny(*known.toTypedArray(), ms = 5_000)).performScrollTo().performClick()
        waitFor(text(R.string.other_devices_title))
        rule.onNodeWithText(text(R.string.sign_this_phone)).performScrollTo().performClick()
        // 05 Device, for this phone: it says why, and can't be marked or looked for.
        waitFor(text(R.string.why))
        rule.onNodeWithText(text(R.string.sign_this_phone)).assertIsDisplayed()
        back()
        back()
        waitFor(text(R.string.done))
    }

    @Test fun theHotspotCheckEndsOnResultsOrSaysWhatItNeeds() {
        home()
        rule.onNodeWithText(text(R.string.check_hotspots)).performClick()
        val end = waitForAny(text(R.string.done), text(R.string.perm_hotspots_title), text(R.string.block_wifi_off_title), text(R.string.block_location_off_title))
        when (end) {
            text(R.string.done) -> rule.onNodeWithText(text(R.string.scan_again)).assertIsDisplayed()
            text(R.string.perm_hotspots_title) -> rule.onNodeWithText(text(R.string.allow)).assertIsDisplayed()
            else -> rule.onNodeWithText(text(R.string.scan_again)).assertIsDisplayed()
        }
    }

    @Test fun theBluetoothCheckEndsOnResultsOrSaysWhatItNeeds() {
        home()
        rule.onNodeWithText(text(R.string.check_bluetooth)).performClick()
        val end = waitForAny(
            text(R.string.done), text(R.string.perm_bluetooth_title), text(R.string.block_bluetooth_off_title),
            text(R.string.block_location_off_title), text(R.string.block_no_bluetooth_title),
        )
        if (end == text(R.string.perm_bluetooth_title)) rule.onNodeWithText(text(R.string.allow)).assertIsDisplayed()
    }

    @Test fun aSweepCanBeSkippedToTheEndAndEndsOnItsReport() {
        home()
        rule.onNodeWithText(text(R.string.start)).performClick()
        // Seven steps for a stay: Skip step on the scans, Next on the hands-on checks (the infrared question answered
        // on the way), See report on the checklist. The sweep ends on its saved report.
        repeat(10) {
            val step = waitForAny(text(R.string.skip_step), text(R.string.next), text(R.string.ir_saw), text(R.string.see_report), text(R.string.report_not_safe_note), ms = 15_000)
            if (step == text(R.string.report_not_safe_note)) return@repeat
            rule.onNodeWithText(step).performClick()
            rule.waitForIdle()
        }
        waitFor(text(R.string.couldnt_check), 15_000)
        rule.onNodeWithText(text(R.string.couldnt_check)).performScrollTo().assertIsDisplayed()
        // It is on the Reports tab now.
        rule.onNodeWithContentDescription(text(R.string.back)).performClick()
        rule.onNodeWithText(text(R.string.tab_reports)).performClick()
        waitFor(text(R.string.reports_footer))
    }

    @Test fun moreOpensItsSheetsAndAboutOpensTheLicences() {
        home()
        rule.onNodeWithText(text(R.string.tab_more)).performClick()
        rule.onNodeWithText(text(R.string.language)).performClick()
        waitFor(text(R.string.language_line))
        rule.onNodeWithContentDescription(text(R.string.close)).performClick()
        rule.onNodeWithText(text(R.string.help_numbers)).performClick()
        waitFor(text(R.string.help_numbers_line))
        rule.onNodeWithContentDescription(text(R.string.close)).performClick()
        rule.onNodeWithText(text(R.string.about)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.licences)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.lic_gpl)).performClick()
        rule.waitUntilAtLeastOneExists(hasText("GNU GENERAL PUBLIC LICENSE", substring = true), 5_000)
    }

    @Test fun closingASweepMidScanGoesHome() {
        home()
        rule.onNodeWithText(text(R.string.start)).performClick()
        waitFor(text(R.string.skip_step))
        // Closing asks first (E07): stop without a report goes back Home.
        rule.onNodeWithContentDescription(text(R.string.close)).performClick()
        waitFor(text(R.string.stop_sweep_title))
        rule.onNodeWithText(text(R.string.stop_no_report)).performClick()
        home()
    }

    @Test fun closingASweepCanStillKeepGoing() {
        home()
        rule.onNodeWithText(text(R.string.start)).performClick()
        waitFor(text(R.string.skip_step))
        rule.onNodeWithContentDescription(text(R.string.close)).performClick()
        waitFor(text(R.string.stop_sweep_title))
        rule.onNodeWithText(text(R.string.keep_going)).performClick()
        waitFor(text(R.string.skip_step))
    }

    @Test fun theLensFinderOpensOnItsCameraOrSaysWhatItNeeds() {
        home()
        rule.onNodeWithText(text(R.string.check_lens)).performClick()
        val end = waitForAny(text(R.string.lens_hint), text(R.string.perm_camera_title), text(R.string.no_camera_title), text(R.string.camera_problem_title), ms = 10_000)
        if (end == text(R.string.lens_hint)) {
            // Infrared and back: the same screen, the other reading.
            rule.onNodeWithText(text(R.string.check_infrared)).performClick()
            waitForAny(text(R.string.infrared_hint), text(R.string.ir_test_title), ms = 10_000)
        }
        rule.onNodeWithContentDescription(text(R.string.back)).performClick()
        home()
    }

    @Test fun theMagnetLearnsOrSaysThePhoneHasNoSensor() {
        home()
        rule.onNodeWithText(text(R.string.check_magnet)).performScrollTo().performClick()
        val end = waitForAny(text(R.string.magnet_learning_title), text(R.string.magnet_normal_title), text(R.string.magnet_jumped_title), text(R.string.no_magnet_title), ms = 10_000)
        if (end != text(R.string.no_magnet_title)) {
            // Learning ends within a few seconds on a phone that holds still.
            waitForAny(text(R.string.magnet_normal_title), text(R.string.magnet_jumped_title), ms = 20_000)
            rule.onNodeWithText(text(R.string.set_normal_again)).performClick()
            waitFor(text(R.string.magnet_learning_title))
        }
        back()
        home()
    }

    @Test fun aSweepReachesTheLensFinderAndTheMagnetOnTheirOwnScreens() {
        home()
        rule.onNodeWithText(text(R.string.start)).performClick()
        // Skip Wi-Fi, hotspots and Bluetooth: the fourth step opens the lens finder.
        repeat(3) {
            rule.waitUntilAtLeastOneExists(hasText(text(R.string.skip_step)), 10_000)
            rule.onNodeWithText(text(R.string.skip_step)).performClick()
            rule.waitForIdle()
        }
        waitForAny(text(R.string.lens_finder), ms = 10_000)
        // Next: Lens → Infrared (same screen; the first time it asks whether the phone sees infrared) → Magnet.
        rule.onNodeWithText(text(R.string.next)).performClick()
        if (waitForAny(text(R.string.ir_saw), text(R.string.infrared_finder)) == text(R.string.ir_saw)) rule.onNodeWithText(text(R.string.ir_saw)).performClick()
        waitForAny(text(R.string.infrared_finder))
        rule.onNodeWithText(text(R.string.next)).performClick()
        waitForAny(text(R.string.magnet_learning_title), text(R.string.magnet_normal_title), text(R.string.magnet_jumped_title), text(R.string.no_magnet_title))
        // Then the checklist, on its own screen, and See report ends the sweep on its report.
        rule.onNodeWithText(text(R.string.next)).performClick()
        waitFor(text(R.string.item_mirror_light))
        rule.onNodeWithText(text(R.string.see_report)).performClick()
        waitFor(text(R.string.couldnt_check), 15_000)
    }
}
