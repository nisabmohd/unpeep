package app.keyhole.privacy.scan

import app.keyhole.privacy.data.SignatureLoader
import app.keyhole.privacy.domain.DeviceEvidence
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.domain.WifiDevice
import app.keyhole.privacy.domain.WifiSnapshot
import app.keyhole.privacy.domain.WifiStatus
import app.keyhole.privacy.domain.judge
import app.keyhole.privacy.domain.looksIsolated
import app.keyhole.privacy.domain.markKey
import app.keyhole.privacy.domain.secondsLeft
import app.keyhole.privacy.scan.net.ScanStatus
import app.keyhole.privacy.scan.net.WifiScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Wi-Fi check between its screens: the scan in progress or the last one finished, and what the user marked as
 * known. It lives in memory only (reports and saved marks come with Room); a scan starts only from [start], which
 * a screen calls when the user asks.
 */
class WifiSession(
    private val scanner: WifiScanner, private val signatures: SignatureLoader, appScope: CoroutineScope,
    private val marks: StateFlow<Map<String, KnownAs>>, private val onMark: (key: String, knownAs: KnownAs?, label: String) -> Unit,
    private val onDone: () -> Unit = {}, private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Raw(
        val run: Int = 0, val status: WifiStatus = WifiStatus.IDLE, val ssid: String? = null, val fraction: Float = 0f,
        val checked: Int = 0, val total: Int = 0, val secondsLeft: Int = 0, val devices: List<DeviceEvidence> = emptyList(), val at: Long = 0,
    )

    private val raw = MutableStateFlow(Raw())
    private var job: Job? = null

    val state: StateFlow<WifiSnapshot> = combine(raw, marks) { r, m ->
        WifiSnapshot(
            r.run, r.status, r.ssid, r.fraction, r.checked, r.total, r.secondsLeft,
            devices = judge(r.devices, signatures.signatures, r.ssid, m, r.at),
            isolated = r.status == WifiStatus.DONE && looksIsolated(r.devices),
        )
    }.flowOn(Dispatchers.Default).stateIn(appScope, SharingStarted.Eagerly, WifiSnapshot())

    /** Starts a scan in [scope] (the screen's), replacing any that is running. Returns its number. */
    fun start(scope: CoroutineScope): Int {
        job?.cancel()
        val run = raw.value.run + 1
        val started = now()
        raw.value = Raw(run, WifiStatus.RUNNING, secondsLeft = (BUDGET_MS / 1_000).toInt(), at = started)
        job = scope.launch {
            try {
                val ports = withContext(Dispatchers.Default) { signatures.signatures.ports }
                scanner.scan(ports).collect { u ->
                    val at = now()
                    val status = when (u.status) {
                        ScanStatus.RUNNING -> WifiStatus.RUNNING
                        ScanStatus.NOT_ON_WIFI -> WifiStatus.NOT_ON_WIFI
                        ScanStatus.DONE -> WifiStatus.DONE
                    }
                    raw.value = Raw(run, status, u.ssid, u.fraction, u.checked, u.total, secondsLeft(u.fraction, at - started, BUDGET_MS), u.devices, at)
                    if (status == WifiStatus.DONE) onDone()
                }
            } finally {
                // Left half-way (Skip step, Close): nothing to show.
                raw.update { if (it.run == run && it.status == WifiStatus.RUNNING) Raw(run) else it }
            }
        }
        return run
    }

    fun cancel() {
        job?.cancel()
    }

    /** "Not a camera?": [knownAs] null takes the mark away again. Kept on the phone (More › Marked as known). */
    fun mark(device: WifiDevice, knownAs: KnownAs?) = onMark(markKey(raw.value.ssid, device.evidence), knownAs, device.name ?: device.ip)

    private companion object {
        const val BUDGET_MS = 20_000L
    }
}
