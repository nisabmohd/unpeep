package app.keyhole.privacy.smoke

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.keyhole.privacy.MainActivity
import app.keyhole.privacy.R
import app.keyhole.privacy.plural
import app.keyhole.privacy.text
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

    private fun home() {
        waitFor(text(R.string.check_this_room))
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
        waitFor(text(R.string.reports_empty_title))
        rule.onNodeWithText(text(R.string.tab_more)).performClick()
        waitFor(text(R.string.marked_as_known))
        rule.onNodeWithText(text(R.string.tab_check)).performClick()
        home()
    }

    @Test fun pickingAPlaceChangesTheSweep() {
        home()
        rule.onNodeWithText(text(R.string.place_trial_room)).performClick()
        // The place is saved first and the screen follows what was saved.
        rule.waitUntilAtLeastOneExists(hasText(text(R.string.place_trial_room)) and isSelected(), 5_000)
        // A trial room has no Wi-Fi to join: six checks, and the Wi-Fi check is gone from the row.
        rule.waitUntilDoesNotExist(hasText(text(R.string.check_wifi)), 5_000)
        rule.onNodeWithText(text(R.string.place_stay)).performClick()
        waitFor(text(R.string.check_wifi))
    }

    @Test fun everyMoreRowOpensAScreenAndComesBack() {
        home()
        rule.onNodeWithText(text(R.string.tab_more)).performClick()
        for (row in listOf(R.string.appearance, R.string.language, R.string.sweep_steps, R.string.marked_as_known, R.string.how_keyhole_works, R.string.what_keyhole_cant_find, R.string.if_you_find_a_camera, R.string.privacy, R.string.about)) {
            waitFor(text(R.string.tab_more))
            rule.onNodeWithText(text(row)).performScrollTo().performClick()
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

    @Test fun aSweepStartsOnItsFirstStepAndCanBeSkippedToTheEnd() {
        home()
        rule.onNodeWithText(text(R.string.start)).performClick()
        waitFor(text(R.string.skip_step))
        // Seven steps for a stay: skipping each one ends the sweep back on Home, whatever state each step was in.
        repeat(7) {
            rule.waitUntilAtLeastOneExists(hasText(text(R.string.skip_step)), 10_000)
            rule.onNodeWithText(text(R.string.skip_step)).performClick()
            rule.waitForIdle()
        }
        home()
    }

    @Test fun closingASweepMidScanGoesHome() {
        home()
        rule.onNodeWithText(text(R.string.start)).performClick()
        waitFor(text(R.string.skip_step))
        rule.onNodeWithContentDescription(text(R.string.close)).performClick()
        home()
    }
}
