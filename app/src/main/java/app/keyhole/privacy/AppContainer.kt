package app.keyhole.privacy

import android.content.Context
import app.keyhole.privacy.data.HelpNumbers
import app.keyhole.privacy.data.ReportRepository
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.data.SignatureLoader
import app.keyhole.privacy.data.SweepNotes
import app.keyhole.privacy.data.VisitReporter
import app.keyhole.privacy.data.db.KeyholeDatabase
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.optics.PhotoStore
import app.keyhole.privacy.scan.BleSession
import app.keyhole.privacy.scan.HotspotSession
import app.keyhole.privacy.scan.NearbySession
import app.keyhole.privacy.scan.WifiSession
import app.keyhole.privacy.scan.ble.BleScanner
import app.keyhole.privacy.scan.net.WifiScanner
import app.keyhole.privacy.scan.wifi.HotspotScanner
import app.keyhole.privacy.sensors.MagnetSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Holds the app's long-lived objects: the only dependency "graph" (no DI framework).
 * Everything is lazy, so cold start only pays for what the first screen needs; signatures load on the first scan.
 */
class AppContainer(val context: Context) {
    /** Lives as long as the process, for work that isn't tied to a screen (saving a setting as a screen closes). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings by lazy { SettingsRepository(context) }
    val signatures by lazy { SignatureLoader(context) }
    val photos by lazy { PhotoStore(context) }
    val database by lazy { KeyholeDatabase.open(context) }

    /** Saved reports, and the devices marked as known. */
    val reports by lazy { ReportRepository(database, photos, appScope) }

    /** What this visit collected for its report: which checks ran, photos, magnet readings, checklist answers. */
    val notes by lazy { SweepNotes() }

    /** The Wi-Fi check between its screens. It scans only when a screen starts it. */
    val wifi by lazy {
        WifiSession(
            WifiScanner(context), signatures, appScope, reports.marksByKey,
            onMark = { key, knownAs, label -> reports.markLater(key, knownAs, label, Check.WIFI) }, onDone = { notes.markRan(Check.WIFI) },
        )
    }

    /** Nearby hotspots and Bluetooth: they listen only when a screen starts them. */
    val hotspots by lazy {
        HotspotSession(
            context, HotspotScanner(context), signatures, appScope, reports.marksByKey,
            onMark = { key, knownAs, label -> reports.markLater(key, knownAs, label, Check.HOTSPOTS) }, onDone = { notes.markRan(Check.HOTSPOTS) },
        )
    }
    val bluetooth by lazy {
        BleSession(
            context, BleScanner(context), signatures, appScope, reports.marksByKey,
            onMark = { key, knownAs, label -> reports.markLater(key, knownAs, label, Check.BLUETOOTH) }, onDone = { notes.markRan(Check.BLUETOOTH) },
        )
    }

    val magnet by lazy { MagnetSensor(context) }

    /** Turns this visit into a saved report. */
    val visit by lazy { VisitReporter(notes, wifi, hotspots, bluetooth, settings, reports) }

    /** Emergency numbers and where to report, per country (bundled, offline). */
    val helpNumbers by lazy { HelpNumbers(context) }

    /** The session of a check that listens for what is nearby. */
    fun nearby(check: Check): NearbySession<*> = if (check == Check.BLUETOOTH) bluetooth else hotspots
}
