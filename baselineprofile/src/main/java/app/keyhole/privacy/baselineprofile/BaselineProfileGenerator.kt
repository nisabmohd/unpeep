package app.keyhole.privacy.baselineprofile

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
 * Records the code paths of a typical session (docs/PERFORMANCE.md): open the app on Check, visit every tab.
 * Grows with the app. Run `./gradlew :app:generatePlayReleaseBaselineProfile`, then commit the result.
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
        for (tab in listOf("Reports", "More", "Check")) device.tap(By.text(tab))
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
        const val PACKAGE = "app.keyhole.privacy"
        const val TIMEOUT = 5_000L
    }
}
