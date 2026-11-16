package app.unpeep.privacy.domain

/**
 * How a device on the Wi-Fi is judged (CLAUDE.md › Behaviour rules). Change `RiskTest` first.
 *
 * - **Likely camera** = an ONVIF reply, or RTSP + a camera signature, or a camera maker's banner.
 * - **Check this** ("Worth a look") = RTSP or a camera port open with no match, or an unknown device with a web page.
 * - **Known** = matches a phone / TV / router / printer signature, or the user marked it.
 * - **Unknown** = answered, but shows nothing either way. Listed quietly, never called fine.
 */
enum class Risk { LIKELY_CAMERA, CHECK_THIS, KNOWN, UNKNOWN }

/** What kind of port this is (signatures/ports.json). */
enum class PortKind { RTSP, WEB, CAMERA, ONVIF, OTHER }

/** One reason a device was judged the way it was. Every finding says why. */
sealed interface Sign {
    /** True for the signs that point to a camera (drawn with the alert icon). */
    val strong: Boolean

    /** Answered ONVIF discovery: nearly only network cameras do. */
    data object Onvif : Sign { override val strong get() = true }
    /** Answers RTSP (video streaming) on [port]. */
    data class Rtsp(val port: Int) : Sign { override val strong get() = true }
    /** Its name, web page or banner names a camera maker. */
    data class CameraMaker(val maker: String) : Sign { override val strong get() = true }
    /** A port only cameras and recorders use is open. */
    data class CameraPort(val port: Int) : Sign { override val strong get() = true }
    /** Shows a web page, and nothing says what it is. */
    data object WebPage : Sign { override val strong get() = false }
    /** Matches a known kind of device. */
    data class KnownKind(val kind: String) : Sign { override val strong get() = false }
    /** The user said it isn't a camera. */
    data object MarkedByUser : Sign { override val strong get() = false }
    /** The Wi-Fi's own router. */
    data object Router : Sign { override val strong get() = false }
    /** This phone. */
    data object ThisPhone : Sign { override val strong get() = false }
}

/** Everything a device answered to discovery and a plain OPTIONS / GET. Nothing else is ever read. */
data class DeviceEvidence(
    val ip: String,
    /** Host, mDNS or UPnP friendly name. */
    val name: String? = null,
    val openPorts: Set<Int> = emptySet(),
    /** Replied to the WS-Discovery probe. */
    val onvif: Boolean = false,
    /** Ports that answered an RTSP OPTIONS with an RTSP status line. */
    val rtspPorts: Set<Int> = emptySet(),
    /** The RTSP `Server:` header, if any. */
    val rtspServer: String? = null,
    val httpServer: String? = null,
    val httpTitle: String? = null,
    val upnpManufacturer: String? = null,
    val upnpModel: String? = null,
    val mdnsServices: Set<String> = emptySet(),
    val isGateway: Boolean = false,
    val isSelf: Boolean = false,
)

data class Verdict(val risk: Risk, val signs: List<Sign>, val maker: String? = null, val kind: String? = null) {
    val strongSigns: Int get() = signs.count { it.strong }
}

/** The bundled lists (assets/signatures), already lower-cased where they are matched. */
data class Signatures(
    val cameras: List<CameraSignature> = emptyList(),
    val known: List<KnownSignature> = emptyList(),
    val ports: Map<Int, PortKind> = emptyMap(),
) {
    data class CameraSignature(val maker: String, val words: List<String>)
    data class KnownSignature(val kind: String, val words: List<String>, val services: List<String> = emptyList())

    /** The camera maker one of [texts] names, or null. Words match whole tokens' starts, so "cam" ≠ "Cameron". */
    fun cameraMaker(vararg texts: String?): String? = match(texts) { t -> cameras.firstOrNull { s -> s.words.any { containsWord(t, it) } }?.maker }

    fun knownKind(evidence: DeviceEvidence): String? {
        known.firstOrNull { s -> s.services.any { it in evidence.mdnsServices } }?.let { return it.kind }
        return match(arrayOf(evidence.name, evidence.upnpManufacturer, evidence.upnpModel, evidence.httpServer, evidence.httpTitle)) { t ->
            known.firstOrNull { s -> s.words.any { containsWord(t, it) } }?.kind
        }
    }

    private inline fun match(texts: Array<out String?>, find: (String) -> String?): String? {
        for (t in texts) if (!t.isNullOrBlank()) find(t.lowercase())?.let { return it }
        return null
    }

    companion object {
        /** [word] in [text] starting at a word edge: "hikvision" in "Hikvision-Webs", but not "cam" in "webcam". */
        fun containsWord(text: String, word: String): Boolean {
            if (word.isEmpty()) return false
            var i = text.indexOf(word)
            while (i >= 0) {
                if (i == 0 || !text[i - 1].isLetterOrDigit()) return true
                i = text.indexOf(word, i + 1)
            }
            return false
        }
    }
}

/**
 * Judges one device. [markedKnown]: the user said it isn't a camera ("Not a camera?").
 * A mark never hides the signs: the report still lists the device under "Marked as known".
 */
fun classify(e: DeviceEvidence, s: Signatures, markedKnown: Boolean = false): Verdict {
    val signs = mutableListOf<Sign>()
    if (e.onvif) signs += Sign.Onvif
    e.rtspPorts.sorted().forEach { signs += Sign.Rtsp(it) }
    // A banner (web page, server header, UPnP, RTSP server) naming a camera maker is proof by itself;
    // a name alone ("IPC-3F2A") needs RTSP beside it.
    val bannerMaker = s.cameraMaker(e.httpServer, e.httpTitle, e.upnpManufacturer, e.upnpModel, e.rtspServer)
    val nameMaker = s.cameraMaker(e.name)
    val maker = bannerMaker ?: nameMaker
    if (maker != null) signs += Sign.CameraMaker(maker)
    val cameraPorts = e.openPorts.filter { s.ports[it] == PortKind.CAMERA || s.ports[it] == PortKind.ONVIF }.sorted()
    cameraPorts.forEach { signs += Sign.CameraPort(it) }
    val rtspOpen = e.rtspPorts.isNotEmpty() || e.openPorts.any { s.ports[it] == PortKind.RTSP }
    val web = e.httpServer != null || e.httpTitle != null || e.openPorts.any { s.ports[it] == PortKind.WEB }

    if (markedKnown) return Verdict(Risk.KNOWN, listOf(Sign.MarkedByUser) + signs, maker)
    if (e.isSelf) return Verdict(Risk.KNOWN, listOf(Sign.ThisPhone))

    val likely = e.onvif || bannerMaker != null || (e.rtspPorts.isNotEmpty() && nameMaker != null)
    if (likely) return Verdict(Risk.LIKELY_CAMERA, signs, maker)

    val kind = s.knownKind(e)
    if (kind != null) return Verdict(Risk.KNOWN, listOf(Sign.KnownKind(kind)), kind = kind)
    if (e.isGateway) return Verdict(Risk.KNOWN, listOf(Sign.Router))

    if (rtspOpen || cameraPorts.isNotEmpty() || nameMaker != null) {
        if (rtspOpen && e.rtspPorts.isEmpty()) e.openPorts.filter { s.ports[it] == PortKind.RTSP }.sorted().forEach { signs += Sign.Rtsp(it) }
        return Verdict(Risk.CHECK_THIS, signs, maker)
    }
    if (web) return Verdict(Risk.CHECK_THIS, listOf(Sign.WebPage))
    return Verdict(Risk.UNKNOWN, emptyList())
}

/** Only the router (and this phone) answered: the Wi-Fi probably keeps devices apart (client isolation). */
fun looksIsolated(devices: List<DeviceEvidence>): Boolean = devices.none { !it.isGateway && !it.isSelf }

/** Likely camera first, then Check this, Unknown, Known; inside one group by address. */
fun riskOrder(risk: Risk): Int = when (risk) {
    Risk.LIKELY_CAMERA -> 0
    Risk.CHECK_THIS -> 1
    Risk.UNKNOWN -> 2
    Risk.KNOWN -> 3
}

/** Sorts addresses by number, so .9 comes before .10. */
fun ipSortKey(ip: String): Long = ip.split('.').fold(0L) { acc, part -> acc * 256 + (part.toLongOrNull() ?: 0) }
