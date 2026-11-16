package app.unpeep.privacy.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code paths of a typical session (docs/PERFORMANCE.md): Welcome on first run, every tab, a report,
 * Appearance and Privacy, and the magnet check (no permission needed).
 * Grows with the app. Run `./gradlew :app:generateBaselineProfile`, then commit the result.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, maxIterations = 3, stableIterations = 2, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.finishWelcome()
        // The usual session: every tab, a report if there is one, a few More screens, a check's results.
        for (tab in listOf("Reports", "More", "Check")) device.tap(By.text(tab))
        device.tap(By.text("Reports"))
        device.tap(By.textContains("·"))
        device.pressBack()
        device.tap(By.text("More"))
        for (row in listOf("Appearance", "Privacy")) {
            device.tap(By.text(row))
            device.pressBack()
        }
        device.tap(By.text("Check"))
        device.tap(By.text("Magnet"))
        device.pressBack()
    }

    private fun UiDevice.finishWelcome() {
        if (!wait(Until.hasObject(By.text("Get started")), 2_000L)) return
        tap(By.text("Get started"))
    }

    private fun UiDevice.tap(selector: androidx.test.uiautomator.BySelector) {
        repeat(3) {
            try {
                wait(Until.findObject(selector), TIMEOUT)?.click()
                waitForIdle()
                return
            } catch (_: StaleObjectException) {
                waitForIdle()
            }
        }
    }

    private companion object {
        const val PACKAGE = "app.unpeep.privacy"
        const val TIMEOUT = 5_000L
    }
}
