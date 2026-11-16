package app.unpeep.privacy.domain

/** What a thing seen over the air is: a Wi-Fi name or a Bluetooth advertisement, sorted into what it means. */
enum class NearbyKind {
    /** A Wi-Fi name cheap cameras broadcast for setup. */
    CAMERA_HOTSPOT,
    /** Any other Wi-Fi network: a neighbour's router, a phone's hotspot. */
    NETWORK,
    /** A tracker (AirTag, Tile…), recognised by what it advertises. */
    TRACKER,
    /** A Bluetooth name that reads like a camera's. Only a hint. */
    BLE_CAMERA,
    /** Any other Bluetooth device: earbuds, watches, TVs. */
    BLE_OTHER,
}

/**
 * One hotspot or Bluetooth device, judged. [id] is what Android calls it (the hotspot's BSSID, the Bluetooth
 * address) and never leaves the phone; [label] is the maker or tracker family the signature names.
 */
data class NearbyDevice(
    val check: Check,
    val id: String,
    val name: String?,
    val rssi: Int,
    val kind: NearbyKind,
    val risk: Risk,
    val label: String? = null,
    val knownAs: KnownAs? = null,
    /** "Add to report": the user wants this one in the report whatever its result. */
    val added: Boolean = false,
)

enum class NearbyStatus {
    IDLE,
    RUNNING,
    DONE,
    /** Android hasn't been allowed to list what is nearby. */
    NEEDS_PERMISSION,
    /** The radio (or, before Android 13, Location) is switched off, or the phone has none. */
    UNAVAILABLE,
}

/** Why a nearby check can't run right now. */
enum class NearbyBlock { WIFI_OFF, BLUETOOTH_OFF, LOCATION_OFF, NO_BLUETOOTH }

/** The hotspot or Bluetooth check as the screens see it. */
data class NearbySnapshot(
    val check: Check,
    val run: Int = 0,
    val status: NearbyStatus = NearbyStatus.IDLE,
    val block: NearbyBlock? = null,
    val fraction: Float = 0f,
    val secondsLeft: Int = 0,
    /** Likely camera first, then Worth a look, then the rest; the strongest signal first inside a group. */
    val devices: List<NearbyDevice> = emptyList(),
    /** When the list was last refreshed by Android (Wi-Fi scans are rationed: "Updated Xs ago"). */
    val updatedAt: Long = 0,
) {
    fun count(risk: Risk): Int = devices.count { it.risk == risk }
    fun device(id: String): NearbyDevice? = devices.firstOrNull { it.id == id }
}

/** What a mark is kept under: the name where there is one (Bluetooth addresses change), else the id. */
fun nearbyKey(check: Check, name: String?, id: String): String = "${check.name}|${name?.takeIf { it.isNotBlank() } ?: id}"

/** Judges the Wi-Fi names seen: one row per name (the strongest of its access points), camera names first. */
fun judgeHotspots(seen: List<SeenHotspot>, signatures: List<HotspotSignature>, marks: Map<String, KnownAs>, added: Set<String>): List<NearbyDevice> =
    seen.filter { it.ssid.isNotBlank() }
        .groupBy { it.ssid }.map { (_, same) -> same.maxBy { it.rssi } }
        .map { h ->
            val key = nearbyKey(Check.HOTSPOTS, h.ssid, h.bssid)
            val mark = marks[key]
            val v = classifyHotspot(h.ssid, signatures, markedKnown = mark != null)
            NearbyDevice(Check.HOTSPOTS, h.bssid, h.ssid, h.rssi, if (v.maker != null) NearbyKind.CAMERA_HOTSPOT else NearbyKind.NETWORK, v.risk, v.maker, mark, key in added)
        }.sortedWith(nearbyOrder)

/** Judges the Bluetooth advertisements seen: trackers and camera-like names are worth a look, the rest is listed quietly. */
fun judgeBle(seen: List<BleAdvert>, signatures: BleSignatures, marks: Map<String, KnownAs>, added: Set<String>): List<NearbyDevice> =
    seen.map { a ->
        val key = nearbyKey(Check.BLUETOOTH, a.name, a.address)
        val mark = marks[key]
        val v = classifyBle(a, signatures, markedKnown = mark != null)
        val kind = when (v.kind) {
            BleKind.TRACKER -> NearbyKind.TRACKER
            BleKind.CAMERA -> NearbyKind.BLE_CAMERA
            BleKind.OTHER -> NearbyKind.BLE_OTHER
        }
        NearbyDevice(Check.BLUETOOTH, a.address, a.name?.takeIf { it.isNotBlank() }, a.rssi, kind, v.risk, v.label, mark, key in added)
    }.sortedWith(nearbyOrder)

private val nearbyOrder: Comparator<NearbyDevice> = compareBy<NearbyDevice> { riskOrder(it.risk) }.thenByDescending { it.rssi }.thenBy { it.name ?: it.id }

/** Which way the signal is going while walking around (Find it). */
enum class Trend { STRONGER, WEAKER, STEADY }

/**
 * Compares the newest reading with the average of the ones before it (up to [TREND_WINDOW]). A change smaller than
 * [TREND_STEP] dBm is noise: bodies and walls move a signal that much without anyone taking a step.
 */
fun trend(readings: List<Int>): Trend {
    if (readings.size < 2) return Trend.STEADY
    val before = readings.dropLast(1).takeLast(TREND_WINDOW).average()
    val change = readings.last() - before
    return when {
        change >= TREND_STEP -> Trend.STRONGER
        change <= -TREND_STEP -> Trend.WEAKER
        else -> Trend.STEADY
    }
}

/**
 * How many of the Find it meter's [METER_SEGMENTS] segments are lit. It follows [nearness], so the meter and the
 * word beside it always agree: none when out of reach, one or two when far, then one more for near, close and
 * right here.
 */
fun meterLevel(rssi: Int): Int = when {
    rssi <= RSSI_GONE -> 0
    else -> when (nearness(rssi)) {
        Nearness.VERY_CLOSE -> METER_SEGMENTS
        Nearness.CLOSE -> METER_SEGMENTS - 1
        Nearness.NEAR -> METER_SEGMENTS - 2
        Nearness.FAR -> if (rssi >= RSSI_FAINT) 2 else 1
    }
}

const val METER_SEGMENTS = 5
private const val TREND_WINDOW = 3
private const val TREND_STEP = 4
/** At this and below nothing is really heard any more. */
private const val RSSI_GONE = -95
/** Far, but clearly there. */
private const val RSSI_FAINT = -85
