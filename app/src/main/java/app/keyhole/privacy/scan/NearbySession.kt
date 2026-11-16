package app.keyhole.privacy.scan

import android.content.Context
import app.keyhole.privacy.data.SignatureLoader
import app.keyhole.privacy.domain.BleAdvert
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.domain.NearbyBlock
import app.keyhole.privacy.domain.NearbyDevice
import app.keyhole.privacy.domain.NearbySnapshot
import app.keyhole.privacy.domain.NearbyStatus
import app.keyhole.privacy.domain.SeenHotspot
import app.keyhole.privacy.domain.judgeBle
import app.keyhole.privacy.domain.judgeHotspots
import app.keyhole.privacy.domain.nearbyKey
import app.keyhole.privacy.scan.ble.BleScanner
import app.keyhole.privacy.scan.wifi.HotspotScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** One signal reading of the thing being looked for (Find it). */
data class SignalReading(val rssi: Int, val at: Long)

/**
 * A check that listens for what is nearby (hotspots, Bluetooth) between its screens: the scan in progress or the
 * last one finished, what the user marked as known and what they added to the report. In memory only (reports and
 * saved marks come with Room). A scan starts only from [start], which a screen calls when the user asks.
 */
abstract class NearbySession<T>(
    val check: Check, context: Context, appScope: CoroutineScope, private val marks: StateFlow<Map<String, KnownAs>>,
    private val onMark: (key: String, knownAs: KnownAs?, label: String) -> Unit, private val onDone: () -> Unit, protected val now: () -> Long,
) {
    protected val context: Context = context.applicationContext

    private data class Raw(
        val run: Int = 0, val status: NearbyStatus = NearbyStatus.IDLE, val block: NearbyBlock? = null,
        val fraction: Float = 0f, val secondsLeft: Int = 0, val updatedAt: Long = 0,
    )

    private val raw = MutableStateFlow(Raw())
    protected val seen = MutableStateFlow<List<T>>(emptyList())
    private val added = MutableStateFlow<Set<String>>(emptySet())
    private var job: Job? = null

    val state: StateFlow<NearbySnapshot> = combine(raw, seen, marks, added) { r, s, m, a ->
        NearbySnapshot(check, r.run, r.status, r.block, r.fraction, r.secondsLeft, judge(s, m, a), r.updatedAt)
    }.flowOn(Dispatchers.Default).stateIn(appScope, SharingStarted.Eagerly, NearbySnapshot(check))

    protected abstract fun judge(seen: List<T>, marks: Map<String, KnownAs>, added: Set<String>): List<NearbyDevice>

    /** How long one scan listens. */
    protected abstract val scanMs: Long

    /** Listens and keeps [seen] up to date until cancelled; returns early when there is nothing more to wait for. */
    protected abstract suspend fun listen(onUpdated: (Long) -> Unit)

    /** The signal of [device] for as long as it is collected (Find it). */
    abstract fun watch(device: NearbyDevice): Flow<SignalReading>

    /**
     * Starts a scan in [scope] (the screen's), replacing any that is running. Without permission, or with the
     * radio off, it says so instead of scanning. Returns the scan's number.
     */
    fun start(scope: CoroutineScope): Int {
        job?.cancel()
        val run = raw.value.run + 1
        val block = if (Access.granted(context, check)) Access.block(context, check) else null
        when {
            !Access.granted(context, check) -> raw.value = Raw(run, NearbyStatus.NEEDS_PERMISSION)
            block != null -> raw.value = Raw(run, NearbyStatus.UNAVAILABLE, block)
            else -> {
                val started = now()
                seen.value = emptyList()
                raw.value = Raw(run, NearbyStatus.RUNNING, secondsLeft = seconds(scanMs))
                job = scope.launch {
                    try {
                        val ticker = launch {
                            while (isActive) {
                                delay(TICK_MS)
                                val elapsed = now() - started
                                raw.update { if (it.run == run && it.status == NearbyStatus.RUNNING) it.copy(fraction = (elapsed.toFloat() / scanMs).coerceIn(0f, 1f), secondsLeft = seconds(scanMs - elapsed)) else it }
                            }
                        }
                        withTimeoutOrNull(scanMs) { listen { at -> raw.update { if (it.run == run) it.copy(updatedAt = at) else it } } }
                        ticker.cancel()
                        raw.update { if (it.run == run) it.copy(status = NearbyStatus.DONE, fraction = 1f, secondsLeft = 0, updatedAt = if (it.updatedAt == 0L) now() else it.updatedAt) else it }
                        onDone()
                    } finally {
                        // Left half-way (Skip step, Close): nothing to show.
                        raw.update { if (it.run == run && it.status == NearbyStatus.RUNNING) Raw(run) else it }
                    }
                }
            }
        }
        return run
    }

    fun cancel() {
        job?.cancel()
    }

    /** True when a scan could start now: allowed, and the radio is on. */
    fun ready(): Boolean = Access.granted(context, check) && Access.block(context, check) == null

    /** "Not a camera?": [knownAs] null takes the mark away again. */
    fun mark(device: NearbyDevice, knownAs: KnownAs?) = onMark(nearbyKey(check, device.name, device.id), knownAs, device.name ?: device.label ?: device.id)

    /** "Add to report", and taking it out again. */
    fun setAdded(device: NearbyDevice, on: Boolean) {
        val key = nearbyKey(check, device.name, device.id)
        added.update { if (on) it + key else it - key }
    }

    private fun seconds(ms: Long): Int = ((ms.coerceAtLeast(0) + 999) / 1_000).toInt().coerceAtLeast(1)

    private companion object {
        const val TICK_MS = 250L
    }
}

/** Nearby hotspots: the Wi-Fi names around, matched against signatures/hotspots.json. */
class HotspotSession(
    context: Context, private val scanner: HotspotScanner, private val signatures: SignatureLoader, appScope: CoroutineScope,
    marks: StateFlow<Map<String, KnownAs>>, onMark: (String, KnownAs?, String) -> Unit, onDone: () -> Unit = {}, now: () -> Long = System::currentTimeMillis,
) : NearbySession<SeenHotspot>(Check.HOTSPOTS, context, appScope, marks, onMark, onDone, now) {
    override val scanMs = 8_000L

    override fun judge(seen: List<SeenHotspot>, marks: Map<String, KnownAs>, added: Set<String>) = judgeHotspots(seen, signatures.hotspots, marks, added)

    override suspend fun listen(onUpdated: (Long) -> Unit) {
        val started = now()
        // Android rationed this scan away: what it already has is all there is, so don't make the user wait.
        val fresh = scanner.waitMs() == 0L
        withTimeoutOrNull(if (fresh) scanMs else STALE_MS) {
            // Until new results arrive for the scan just asked for.
            scanner.results().onEach { list ->
                seen.value = list.hotspots
                if (list.updatedAt > 0) onUpdated(list.updatedAt)
            }.first { it.updatedAt >= started }
            // Done, as soon as the lens has had a moment.
            delay((MIN_MS - (now() - started)).coerceAtLeast(0))
        }
    }

    /** The strongest access point carrying the name, each time Android has new results; a new scan every 30 s. */
    override fun watch(device: NearbyDevice): Flow<SignalReading> = channelFlow {
        launch {
            while (isActive) {
                delay(RESCAN_MS)
                scanner.requestScan()
            }
        }
        var last = 0L
        scanner.results().collect { list ->
            val best = list.hotspots.filter { it.ssid == device.name }.maxByOrNull { it.rssi } ?: return@collect
            // The same results delivered twice are one reading.
            val at = if (list.updatedAt > 0) list.updatedAt else now()
            if (at != last) {
                last = at
                send(SignalReading(best.rssi, at))
            }
        }
    }

    private companion object {
        const val MIN_MS = 1_500L
        const val STALE_MS = 1_500L
        /** 4 scans in 2 minutes is Android's ration: one every 30 s uses exactly that. */
        const val RESCAN_MS = 30_000L
    }
}

/** Bluetooth: trackers by what they advertise, cameras by name, from signatures/ble.json. */
class BleSession(
    context: Context, private val scanner: BleScanner, private val signatures: SignatureLoader, appScope: CoroutineScope,
    marks: StateFlow<Map<String, KnownAs>>, onMark: (String, KnownAs?, String) -> Unit, onDone: () -> Unit = {}, now: () -> Long = System::currentTimeMillis,
) : NearbySession<BleAdvert>(Check.BLUETOOTH, context, appScope, marks, onMark, onDone, now) {
    override val scanMs = 8_000L

    override fun judge(seen: List<BleAdvert>, marks: Map<String, KnownAs>, added: Set<String>) = judgeBle(seen, signatures.ble, marks, added)

    override suspend fun listen(onUpdated: (Long) -> Unit) {
        val byAddress = LinkedHashMap<String, BleAdvert>()
        var shown = 0L
        try {
            scanner.adverts().collect { a ->
                if (a.address.isEmpty()) return@collect
                byAddress[a.address] = merge(byAddress[a.address], a)
                // Devices advertise several times a second: refresh the list a few times a second, not on each one.
                val at = now()
                if (at - shown >= REFRESH_MS) {
                    shown = at
                    seen.value = byAddress.values.toList()
                    onUpdated(at)
                }
            }
        } finally {
            // Whatever was heard between the last refresh and the end of the scan.
            seen.value = byAddress.values.toList()
        }
    }

    /** A device says its name in one packet and its services in another: keep both. */
    private fun merge(old: BleAdvert?, new: BleAdvert): BleAdvert =
        if (old == null) new else new.copy(name = new.name ?: old.name, services = old.services + new.services, manufacturer = old.manufacturer + new.manufacturer)

    /** About one reading a second: the average of what was heard in that second, so one stray packet doesn't jump. */
    override fun watch(device: NearbyDevice): Flow<SignalReading> = channelFlow {
        var sum = 0
        var count = 0
        var windowStart = now()
        scanner.adverts().collect { a ->
            if (a.address != device.id) return@collect
            sum += a.rssi
            count++
            val at = now()
            if (at - windowStart >= WINDOW_MS) {
                send(SignalReading(sum / count, at))
                sum = 0
                count = 0
                windowStart = at
            }
        }
    }

    private companion object {
        const val REFRESH_MS = 400L
        const val WINDOW_MS = 1_000L
    }
}
