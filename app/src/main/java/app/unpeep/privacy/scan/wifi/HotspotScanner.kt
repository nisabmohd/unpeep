package app.unpeep.privacy.scan.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import app.unpeep.privacy.domain.ScanThrottle
import app.unpeep.privacy.domain.SeenHotspot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** The Wi-Fi names around, as Android last heard them, and when that was. */
data class HotspotList(val hotspots: List<SeenHotspot>, val updatedAt: Long)

/**
 * Nearby hotspots (docs/SCANNING.md § Hotspots): `WifiManager.startScan()` and its results. It only listens; it
 * never joins a network. Android allows 4 scans in 2 minutes, so every scan goes through one [ScanThrottle]; when
 * a scan isn't allowed, the last results are shown with their age ("Updated Xs ago").
 * The caller has checked `Access` first.
 */
class HotspotScanner(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val context = context.applicationContext
    private val wifi: WifiManager? = this.context.getSystemService(WifiManager::class.java)
    private val throttle = ScanThrottle()

    /** When Android last delivered new results to this app. */
    @Volatile private var updatedAt = 0L

    /** Milliseconds until another scan may be asked for; 0 when one may start now. */
    fun waitMs(): Long = throttle.waitMs(now())

    /**
     * Asks for one scan if the throttle allows it, and emits the list each time Android has new results, for as
     * long as it is collected. The first emission is what Android already has.
     */
    fun results(): Flow<HotspotList> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                // EXTRA_RESULTS_UPDATED false: Android refused or failed the scan and the list is the old one.
                if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true)) updatedAt = now()
                trySend(read())
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(read())
        requestScan()
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }.conflate()

    /** One more scan, if Android's ration allows it. Returns false when it has to wait. */
    @Suppress("DEPRECATION") // startScan: still the only way for an app to ask; Android rations it, and so do we.
    fun requestScan(): Boolean {
        if (!throttle.tryAcquire(now())) return false
        return runCatching { wifi?.startScan() == true }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission") // The session asks `Access.granted` before it collects.
    private fun read(): HotspotList {
        val results: List<ScanResult> = runCatching { wifi?.scanResults }.getOrNull().orEmpty()
        return HotspotList(results.map(::seen), updatedAt)
    }

    @Suppress("DEPRECATION") // SSID: wifiSsid needs API 33; the name is all Unpeep reads.
    private fun seen(r: ScanResult): SeenHotspot {
        val caps = r.capabilities.orEmpty()
        val open = SECURED.none { it in caps }
        return SeenHotspot(r.SSID.orEmpty().removeSurrounding("\""), r.BSSID.orEmpty(), r.level, r.frequency, open)
    }

    private companion object {
        val SECURED = listOf("WEP", "PSK", "EAP", "SAE", "OWE")
    }
}
