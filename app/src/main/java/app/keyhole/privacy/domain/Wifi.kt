package app.keyhole.privacy.domain

/** What the user said a device is ("Not a camera?"). Not sure = no mark: it stays on the list. */
enum class KnownAs { MINE, ROOM }

/** One device on the Wi-Fi, judged: what it answered, what that means, and when it was seen. */
data class WifiDevice(val evidence: DeviceEvidence, val verdict: Verdict, val seenAt: Long, val knownAs: KnownAs? = null) {
    val ip: String get() = evidence.ip
    /** The name it gives itself, or its model; null when it has neither (the screen then shows its address). */
    val name: String? get() = evidence.name ?: evidence.upnpModel
    val risk: Risk get() = verdict.risk
}

enum class WifiStatus { IDLE, RUNNING, NOT_ON_WIFI, DONE }

/** The Wi-Fi check as the screens see it. */
data class WifiSnapshot(
    /** Which scan this is; a screen that started scan n ignores what is left of scan n − 1. */
    val run: Int = 0,
    val status: WifiStatus = WifiStatus.IDLE,
    /** The Wi-Fi's name, when Android tells it. */
    val ssid: String? = null,
    val fraction: Float = 0f,
    val checked: Int = 0,
    val total: Int = 0,
    val secondsLeft: Int = 0,
    /** Likely camera first, then Worth a look, then the rest; by address inside a group. */
    val devices: List<WifiDevice> = emptyList(),
    /** Only the router (and this phone) answered: the Wi-Fi probably keeps devices apart. */
    val isolated: Boolean = false,
) {
    fun count(risk: Risk): Int = devices.count { it.risk == risk }
    fun device(ip: String): WifiDevice? = devices.firstOrNull { it.ip == ip }
}

/** What a mark is kept under: the device's own name where it has one (addresses change), on this Wi-Fi. */
fun markKey(ssid: String?, evidence: DeviceEvidence): String = "${ssid.orEmpty()}|${evidence.name ?: evidence.upnpModel ?: evidence.ip}"

/** Judges every device and puts them in the order the screens list them. */
fun judge(evidence: List<DeviceEvidence>, signatures: Signatures, ssid: String?, marks: Map<String, KnownAs>, seenAt: Long): List<WifiDevice> =
    evidence.map { e ->
        val mark = marks[markKey(ssid, e)]
        WifiDevice(e, classify(e, signatures, markedKnown = mark != null), seenAt, mark)
    }.sortedWith(compareBy({ riskOrder(it.risk) }, { ipSortKey(it.ip) }))

/** Seconds a scan still needs, from how far it got in [elapsedMs]; never more than what is left of [budgetMs]. */
fun secondsLeft(fraction: Float, elapsedMs: Long, budgetMs: Long): Int {
    val cap = (budgetMs - elapsedMs).coerceAtLeast(1_000)
    val guess = if (fraction > 0.05f) (elapsedMs / fraction - elapsedMs).toLong() else cap
    return ((guess.coerceIn(1_000, cap) + 999) / 1_000).toInt()
}
