package app.unpeep.privacy.domain

/** A Wi-Fi name cheap cameras broadcast for setup (signatures/hotspots.json). */
data class HotspotSignature(val pattern: Regex, val maker: String, val weak: Boolean = false)

/** One network seen in a Wi-Fi scan. */
data class SeenHotspot(val ssid: String, val bssid: String, val rssi: Int, val frequency: Int = 0, val open: Boolean = false)

data class HotspotVerdict(val risk: Risk, val maker: String? = null)

/**
 * A name that matches a camera pattern is a likely camera; a [HotspotSignature.weak] pattern (smart plugs use it
 * too) is only worth a look. Everything else is just a network nearby: unknown, never "fine".
 */
fun classifyHotspot(ssid: String, signatures: List<HotspotSignature>, markedKnown: Boolean = false): HotspotVerdict {
    val match = signatures.firstOrNull { it.pattern.containsMatchIn(ssid) }
    return when {
        match == null -> HotspotVerdict(Risk.UNKNOWN)
        markedKnown -> HotspotVerdict(Risk.KNOWN, match.maker)
        match.weak -> HotspotVerdict(Risk.CHECK_THIS, match.maker)
        else -> HotspotVerdict(Risk.LIKELY_CAMERA, match.maker)
    }
}

/**
 * Android allows 4 Wi-Fi scans every 2 minutes (docs/LIMITS.md). This keeps count so `startScan()` is never
 * called a fifth time; the screen shows "Updated Xs ago" meanwhile.
 */
class ScanThrottle(private val max: Int = MAX_SCANS, private val windowMs: Long = WINDOW_MS) {
    private val times = ArrayDeque<Long>()

    /** True (and counted) when a scan may start at [now]. */
    @Synchronized fun tryAcquire(now: Long): Boolean {
        prune(now)
        if (times.size >= max) return false
        times.addLast(now)
        return true
    }

    /** Milliseconds until the next scan is allowed; 0 when one is allowed now. */
    @Synchronized fun waitMs(now: Long): Long {
        prune(now)
        return if (times.size < max) 0 else (times.first() + windowMs - now).coerceAtLeast(0)
    }

    private fun prune(now: Long) {
        while (times.isNotEmpty() && now - times.first() >= windowMs) times.removeFirst()
    }

    companion object {
        const val MAX_SCANS = 4
        const val WINDOW_MS = 120_000L
    }
}

/**
 * Signal strength as 0..1 for the Find it lens and meter: −90 dBm (far) → 0, −35 dBm (touching) → 1.
 * Stronger means closer; walls and bodies bend it, so it guides, it doesn't measure.
 */
fun signalLevel(rssi: Int): Float = ((rssi - RSSI_FAR).toFloat() / (RSSI_NEAR - RSSI_FAR)).coerceIn(0f, 1f)

/** How near, in words (Find it). */
enum class Nearness { FAR, NEAR, CLOSE, VERY_CLOSE }

fun nearness(rssi: Int): Nearness = when {
    rssi >= -50 -> Nearness.VERY_CLOSE
    rssi >= -62 -> Nearness.CLOSE
    rssi >= -74 -> Nearness.NEAR
    else -> Nearness.FAR
}

private const val RSSI_FAR = -90
private const val RSSI_NEAR = -35
