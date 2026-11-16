package app.unpeep.privacy.domain

/** A tracker family, recognised by what it advertises, never by its name (docs/SCANNING.md § Bluetooth). */
data class TrackerSignature(
    val name: String,
    /** Bluetooth company id of the manufacturer data (Apple = 0x004C), with [type] as its first byte. */
    val company: Int? = null,
    val type: Int? = null,
    /** 16-bit service UUIDs, upper-case hex ("FEED"). */
    val services: List<String> = emptyList(),
)

data class BleSignatures(val trackers: List<TrackerSignature> = emptyList(), val cameras: List<Signatures.CameraSignature> = emptyList())

/** What one advertisement carried. */
data class BleAdvert(
    val address: String,
    val name: String? = null,
    val rssi: Int = 0,
    /** 16-bit service UUIDs seen in the service list or service data, upper-case hex. */
    val services: Set<String> = emptySet(),
    /** Manufacturer data by company id. */
    val manufacturer: Map<Int, ByteArray> = emptyMap(),
)

enum class BleKind { TRACKER, CAMERA, OTHER }

data class BleVerdict(val kind: BleKind, val risk: Risk, val label: String? = null)

/**
 * A tracker nearby is worth a look (it may be yours, or the room's key finder); a camera-like name is only a hint,
 * so it is worth a look too, never "likely". Everything else is listed quietly.
 */
fun classifyBle(a: BleAdvert, s: BleSignatures, markedKnown: Boolean = false): BleVerdict {
    val tracker = s.trackers.firstOrNull { t ->
        t.services.any { it in a.services } ||
            (t.company != null && a.manufacturer[t.company]?.let { data -> t.type == null || (data.isNotEmpty() && (data[0].toInt() and 0xFF) == t.type) } == true)
    }
    if (tracker != null) return BleVerdict(BleKind.TRACKER, if (markedKnown) Risk.KNOWN else Risk.CHECK_THIS, tracker.name)
    val name = a.name?.lowercase()
    val camera = if (name.isNullOrBlank()) null else s.cameras.firstOrNull { c -> c.words.any { Signatures.containsWord(name, it) } }
    if (camera != null) return BleVerdict(BleKind.CAMERA, if (markedKnown) Risk.KNOWN else Risk.CHECK_THIS, camera.maker)
    return BleVerdict(BleKind.OTHER, Risk.UNKNOWN)
}

/** The 16-bit short form of a Bluetooth base UUID ("0000feed-0000-1000-8000-00805f9b34fb" → "FEED"), or null. */
fun shortUuid(uuid: String): String? {
    val u = uuid.lowercase()
    return if (u.length == 36 && u.startsWith("0000") && u.endsWith("-0000-1000-8000-00805f9b34fb")) u.substring(4, 8).uppercase() else null
}
