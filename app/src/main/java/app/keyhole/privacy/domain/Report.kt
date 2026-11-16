package app.keyhole.privacy.domain

/*
 * A report (docs/PLAN.md § 2.7, CLAUDE.md › Behaviour rules): what was found, what was marked as known, and what
 * couldn't be checked. Built here from what the checks left behind, with no Android in it, so every rule has a test.
 * It never says a room is safe: it always has a "Couldn't check" list.
 */

/** Where a finding came from. */
enum class FindingSource { WIFI, HOTSPOT, BLUETOOTH, CHECKLIST, MAGNET }

/**
 * One thing in the report. [title] is a device's name, a Wi-Fi name, or (CHECKLIST) the item's enum name; [detail]
 * is an address, a signal in dBm, or µT above normal. [signs] are the reasons, as [Sign] codes ([signCodes]).
 * [markKey] lets "Undo" take a mark away again; [knownAs] is set when it was marked as known.
 */
data class Finding(
    val source: FindingSource,
    val title: String,
    val detail: String = "",
    val risk: Risk = Risk.CHECK_THIS,
    val signs: List<String> = emptyList(),
    val label: String? = null,
    val markKey: String? = null,
    val knownAs: KnownAs? = null,
    val note: String = "",
    val photos: Int = 0,
) {
    /** In "Found" (to look at), not in "Marked as known". */
    val toLookAt: Boolean get() = knownAs == null
}

/** One line of "Couldn't check". Each has its words in strings.xml. */
sealed interface Gap {
    /** A check of the place that didn't run (skipped, switched off in Sweep steps, or not possible here). */
    data class NotRun(val check: Check) : Gap
    /** Only the router answered: other devices on this Wi-Fi couldn't be seen. */
    data object Isolated : Gap
    /** The phone's camera blocks infrared (the TV-remote test). */
    data object NoInfrared : Gap
    /** Always: a camera that records to a memory card and sends nothing can't be seen by any scan. */
    data object MemoryCard : Gap
    /** Always when Wi-Fi ran: a camera on another network or on mobile data isn't on this Wi-Fi. */
    data object OtherNetwork : Gap
}

/** What a visit (one sweep, or checks run one by one) left for its report. */
data class Visit(
    val place: Place,
    val startedAt: Long,
    /** The checks the place has, in the user's order: what the report's check row shows. */
    val planned: List<Check>,
    /** The checks that really ran. */
    val ran: Set<Check>,
    val wifiName: String? = null,
    val wifi: List<WifiDevice> = emptyList(),
    val wifiIsolated: Boolean = false,
    val nearby: List<NearbyDevice> = emptyList(),
    val checklist: Map<ChecklistItem, ChecklistEntry> = emptyMap(),
    val magnet: List<MagnetReading> = emptyList(),
    val photos: List<String> = emptyList(),
    val seesInfrared: Boolean? = null,
)

/** A magnet reading the user added. */
data class MagnetReading(val above: Int, val normal: Int)

/** The report, ready to save and show. */
data class ReportDraft(
    val place: Place,
    val startedAt: Long,
    val planned: List<Check>,
    val ran: List<Check>,
    val wifiName: String?,
    val findings: List<Finding>,
    val gaps: List<Gap>,
    val photos: List<String>,
) {
    val toLookAt: Int get() = findings.count { it.toLookAt }
}

/** Builds the report of [visit]: findings (most serious first), then everything that couldn't be checked. */
fun buildReport(visit: Visit): ReportDraft {
    val findings = mutableListOf<Finding>()
    if (Check.WIFI in visit.ran) {
        visit.wifi.filter { it.knownAs != null || it.risk == Risk.LIKELY_CAMERA || it.risk == Risk.CHECK_THIS }.forEach { d ->
            val marked = d.knownAs != null
            findings += Finding(
                FindingSource.WIFI, d.name ?: d.ip, d.ip,
                // A marked device keeps the risk it had before, for the report's honesty: shown under "Marked as known".
                risk = if (marked) riskWithoutMark(d) else d.risk,
                signs = signCodes(d.verdict.signs.filter { it != Sign.MarkedByUser }), label = d.verdict.maker,
                markKey = markKey(visit.wifiName, d.evidence), knownAs = d.knownAs,
            )
        }
    }
    visit.nearby.filter { it.check in visit.ran && (it.knownAs != null || it.added || it.risk == Risk.LIKELY_CAMERA || it.risk == Risk.CHECK_THIS) }.forEach { d ->
        findings += Finding(
            if (d.check == Check.BLUETOOTH) FindingSource.BLUETOOTH else FindingSource.HOTSPOT,
            d.name ?: d.label ?: d.id, "${d.rssi}", risk = if (d.risk == Risk.KNOWN || d.risk == Risk.UNKNOWN) Risk.CHECK_THIS else d.risk,
            signs = listOf(d.kind.name), label = d.label, markKey = nearbyKey(d.check, d.name, d.id), knownAs = d.knownAs,
        )
    }
    visit.checklist.filter { it.value.answer == ChecklistAnswer.NOT_SURE }.forEach { (item, entry) ->
        findings += Finding(FindingSource.CHECKLIST, item.name, note = entry.note.trim(), photos = if (entry.photoPath != null) 1 else 0)
    }
    // A magnet reading: µT above normal as its detail, the normal it was measured against as its note.
    visit.magnet.forEach { m -> findings += Finding(FindingSource.MAGNET, FindingSource.MAGNET.name, "${m.above}", Risk.CHECK_THIS, note = "${m.normal}") }
    val ordered = findings.sortedWith(compareBy({ riskOrder(it.risk) }, { it.source.ordinal }))

    val gaps = mutableListOf<Gap>()
    visit.planned.filter { it !in visit.ran }.forEach { gaps += Gap.NotRun(it) }
    if (Check.WIFI in visit.ran && visit.wifiIsolated) gaps += Gap.Isolated
    if (Check.INFRARED in visit.ran && visit.seesInfrared == false) gaps += Gap.NoInfrared
    if (Check.WIFI in visit.ran) gaps += Gap.OtherNetwork
    gaps += Gap.MemoryCard

    return ReportDraft(
        visit.place, visit.startedAt, visit.planned, visit.planned.filter { it in visit.ran }, visit.wifiName,
        ordered, gaps, visit.photos,
    )
}

private fun riskWithoutMark(d: WifiDevice): Risk {
    val strong = d.verdict.signs.any { it is Sign.Onvif || it is Sign.CameraMaker } || (d.verdict.signs.any { it is Sign.Rtsp } && d.verdict.maker != null)
    return when {
        strong -> Risk.LIKELY_CAMERA
        d.verdict.signs.any { it.strong || it == Sign.WebPage } -> Risk.CHECK_THIS
        else -> Risk.UNKNOWN
    }
}

/** Signs as short codes for storage ("ONVIF", "RTSP:554", "MAKER:Hikvision"); [parseSign] reads them back. */
fun signCodes(signs: List<Sign>): List<String> = signs.map { s ->
    when (s) {
        Sign.Onvif -> "ONVIF"
        is Sign.Rtsp -> "RTSP:${s.port}"
        is Sign.CameraMaker -> "MAKER:${s.maker}"
        is Sign.CameraPort -> "PORT:${s.port}"
        Sign.WebPage -> "WEB"
        is Sign.KnownKind -> "KIND:${s.kind}"
        Sign.MarkedByUser -> "MARKED"
        Sign.Router -> "ROUTER"
        Sign.ThisPhone -> "PHONE"
    }
}

fun parseSign(code: String): Sign? {
    val value = code.substringAfter(':', "")
    return when (code.substringBefore(':')) {
        "ONVIF" -> Sign.Onvif
        "RTSP" -> value.toIntOrNull()?.let { Sign.Rtsp(it) }
        "MAKER" -> Sign.CameraMaker(value)
        "PORT" -> value.toIntOrNull()?.let { Sign.CameraPort(it) }
        "WEB" -> Sign.WebPage
        "KIND" -> Sign.KnownKind(value)
        "MARKED" -> Sign.MarkedByUser
        "ROUTER" -> Sign.Router
        "PHONE" -> Sign.ThisPhone
        else -> null
    }
}
