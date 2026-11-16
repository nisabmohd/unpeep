package app.keyhole.privacy.ui.report

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.keyhole.privacy.R
import app.keyhole.privacy.data.SavedReport
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.ChecklistItem
import app.keyhole.privacy.domain.Finding
import app.keyhole.privacy.domain.FindingSource
import app.keyhole.privacy.domain.Gap
import app.keyhole.privacy.domain.NearbyKind
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.domain.Sign
import app.keyhole.privacy.domain.parseSign
import app.keyhole.privacy.ui.checklist.look
import app.keyhole.privacy.ui.icon
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.wifi.kindLabel
import app.keyhole.privacy.ui.wifi.label
import java.util.Date

/**
 * The words of a report, in one place for the report screen, its share text and its PDF, so all three always say
 * the same thing. Works from [Resources], so it runs outside Compose too (the PDF is drawn off the main thread).
 */
class ReportWords(private val res: Resources, private val is24Hour: Boolean = false) {
    private fun s(id: Int, vararg args: Any): String = res.getString(id, *args)

    /** "Sun 15 Nov" (10), in the phone's own order; the year only when it isn't this year. */
    fun date(at: Long): String {
        val locale = res.configuration.locales[0]
        val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val year = java.util.Calendar.getInstance().apply { timeInMillis = at }.get(java.util.Calendar.YEAR)
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, if (year == thisYear) "EEEdMMM" else "EEEdMMMyyyy")
        return java.text.SimpleDateFormat(pattern, locale).format(Date(at))
    }
    fun time(at: Long): String = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(Date(at))

    /** The report's name, or its place ("Trial room") until the user gives it one; the date is always on the line below. */
    fun name(r: SavedReport): String = r.name.ifBlank { s(r.place.label) }

    fun whenLine(r: SavedReport): String = s(R.string.two_parts, date(r.savedAt), time(r.savedAt))

    fun checksOf(r: SavedReport): String = res.getQuantityString(R.plurals.checks_of, r.planned.size, r.ran.size, r.planned.size)

    /** "Infrared was skipped", "2 checks were skipped", or "Every check ran". */
    fun skippedLine(r: SavedReport): String {
        val skipped = r.planned.filter { it !in r.ran }
        return when (skipped.size) {
            0 -> s(R.string.all_checks_ran)
            1 -> s(R.string.step_was_skipped, s(skipped.single().label))
            else -> res.getQuantityString(R.plurals.checks_were_skipped, skipped.size, skipped.size)
        }
    }

    fun title(f: Finding): String = when (f.source) {
        FindingSource.CHECKLIST -> ChecklistItem.entries.firstOrNull { it.name == f.title }?.let { s(it.look.title) } ?: f.title
        FindingSource.MAGNET -> s(R.string.magnet_finding)
        else -> f.title
    }

    fun source(f: Finding): String = s(
        when (f.source) {
            FindingSource.WIFI -> R.string.source_wifi
            FindingSource.HOTSPOT -> R.string.source_hotspot
            FindingSource.BLUETOOTH -> R.string.source_bluetooth
            FindingSource.CHECKLIST -> R.string.source_checklist
            FindingSource.MAGNET -> R.string.source_magnet
        },
    )

    /** "Wi-Fi · Likely camera", "Checklist · not sure · 1 photo", "Magnet · +14 µT above a normal of 48 µT". */
    fun line(f: Finding): String = when (f.source) {
        FindingSource.CHECKLIST -> listOfNotNull(source(f), s(R.string.not_sure_lower), f.photos.takeIf { it > 0 }?.let { res.getQuantityString(R.plurals.photo_count, it, it) }).joinToString(" · ")
        FindingSource.MAGNET -> s(R.string.two_parts, source(f), s(R.string.magnet_finding_line, f.detail.toIntOrNull() ?: 0, f.note.toIntOrNull() ?: 0))
        // Inside a line the result reads lower case (10: "Wi-Fi · likely camera").
        else -> s(R.string.two_parts, source(f), s(if (f.risk == app.keyhole.privacy.domain.Risk.LIKELY_CAMERA) R.string.risk_likely_lower else R.string.risk_look_lower))
    }

    /** The reasons, one sentence each. */
    fun reasons(f: Finding): List<String> = when (f.source) {
        FindingSource.WIFI -> f.signs.mapNotNull { parseSign(it) }.map { sign(res, it) }
        FindingSource.HOTSPOT, FindingSource.BLUETOOTH -> f.signs.mapNotNull { k -> NearbyKind.entries.firstOrNull { it.name == k } }.map { nearbyReason(res, it, f.label, f.risk) }
        FindingSource.CHECKLIST -> listOfNotNull(ChecklistItem.entries.firstOrNull { it.name == f.title }?.let { s(it.look.line) })
        FindingSource.MAGNET -> listOf(s(R.string.magnet_finding_line, f.detail.toIntOrNull() ?: 0, f.note.toIntOrNull() ?: 0))
    }

    fun gap(g: Gap): String = when (g) {
        is Gap.NotRun -> s(R.string.gap_not_run, s(g.check.label))
        Gap.Isolated -> s(R.string.gap_isolated)
        Gap.NoInfrared -> s(R.string.gap_no_infrared)
        Gap.MemoryCard -> s(R.string.gap_memory_card)
        Gap.OtherNetwork -> s(R.string.gap_other_network)
    }

    /** A gap as a row (10b): a short title, and [gapLine] under it. */
    fun gapTitle(g: Gap): String = when (g) {
        is Gap.NotRun -> s(g.check.label)
        Gap.Isolated -> s(R.string.limit_isolation_title)
        Gap.NoInfrared -> s(R.string.limit_infrared_title)
        Gap.MemoryCard -> s(R.string.limit_memory_card_title)
        Gap.OtherNetwork -> s(R.string.limit_other_network_title)
    }

    fun gapLine(g: Gap): String = when (g) {
        is Gap.NotRun -> s(R.string.gap_skipped_line)
        Gap.Isolated -> s(R.string.limit_isolation)
        Gap.NoInfrared -> s(R.string.limit_infrared)
        Gap.MemoryCard -> s(R.string.gap_memory_card_line)
        Gap.OtherNetwork -> s(R.string.gap_other_network_line)
    }

    fun toLookAt(r: SavedReport): String = if (r.toLookAt > 0) res.getQuantityString(R.plurals.to_look_at, r.toLookAt, r.toLookAt) else s(R.string.nothing_found)

    /** The report as plain text, for the share sheet. */
    fun text(r: SavedReport): String = buildString {
        appendLine(name(r))
        appendLine(whenLine(r))
        appendLine(s(R.string.two_parts, toLookAt(r), checksOf(r)))
        val found = r.findings.filter { it.finding.toLookAt }
        if (found.isNotEmpty()) {
            appendLine()
            appendLine(s(R.string.found))
            found.forEach { f ->
                appendLine("- " + title(f.finding) + " (" + line(f.finding) + ")")
                reasons(f.finding).forEach { appendLine("  " + it) }
                if (f.finding.note.isNotBlank() && f.finding.source == FindingSource.CHECKLIST) appendLine("  " + f.finding.note)
            }
        }
        val known = r.findings.filter { !it.finding.toLookAt }
        if (known.isNotEmpty()) {
            appendLine()
            appendLine(s(R.string.marked_as_known))
            known.forEach { appendLine("- " + title(it.finding) + " (" + line(it.finding) + ")") }
        }
        appendLine()
        appendLine(s(R.string.couldnt_check))
        r.gaps.forEach { appendLine("- " + gap(it)) }
        appendLine()
        appendLine(s(R.string.report_not_safe_note))
        append(s(R.string.share_footer))
    }

    companion object {
        /** What no scan can see shows the thing itself (final v2); a skipped check shows its own icon. */
        @androidx.annotation.DrawableRes
        fun gapIcon(g: Gap): Int = when (g) {
            is Gap.NotRun -> g.check.icon
            Gap.Isolated -> R.drawable.ic_shield
            Gap.NoInfrared -> R.drawable.ic_infrared
            Gap.MemoryCard -> R.drawable.ic_memory_card
            Gap.OtherNetwork -> R.drawable.ic_antenna
        }

        /** One reason of a Wi-Fi device, as a sentence. */
        fun sign(res: Resources, sign: Sign): String = when (sign) {
            Sign.Onvif -> res.getString(R.string.sign_onvif)
            is Sign.Rtsp -> res.getString(R.string.sign_rtsp, sign.port)
            is Sign.CameraMaker -> res.getString(R.string.sign_camera_maker)
            is Sign.CameraPort -> res.getString(R.string.sign_camera_port, sign.port)
            Sign.WebPage -> res.getString(R.string.sign_web_page)
            is Sign.KnownKind -> res.getString(R.string.sign_known_kind, kindLabel(sign.kind)?.let { res.getString(it) } ?: sign.kind)
            Sign.MarkedByUser -> res.getString(R.string.sign_marked)
            Sign.Router -> res.getString(R.string.sign_router)
            Sign.ThisPhone -> res.getString(R.string.sign_this_phone)
        }

        /** Why a hotspot or Bluetooth device is listed, as a sentence. */
        fun nearbyReason(res: Resources, kind: NearbyKind, label: String?, risk: Risk): String = when (kind) {
            NearbyKind.CAMERA_HOTSPOT -> if (risk == Risk.CHECK_THIS) res.getString(R.string.reason_hotspot_weak) else res.getString(R.string.reason_hotspot, label.orEmpty())
            NearbyKind.NETWORK -> res.getString(R.string.reason_network)
            NearbyKind.TRACKER -> res.getString(R.string.reason_tracker, label.orEmpty())
            NearbyKind.BLE_CAMERA -> res.getString(R.string.reason_ble_camera)
            NearbyKind.BLE_OTHER -> res.getString(R.string.reason_ble_other)
        }

        /** "stay", "trial room": a place inside a sentence. */
        fun placeLower(place: Place): Int = when (place) {
            Place.STAY -> R.string.place_lower_stay
            Place.TRIAL_ROOM -> R.string.place_lower_trial_room
            Place.WASHROOM -> R.string.place_lower_washroom
            Place.OFFICE -> R.string.place_lower_office
            Place.CAR -> R.string.place_lower_car
        }
    }
}

@Composable
fun rememberReportWords(): ReportWords {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) { ReportWords(context.resources, DateFormat.is24HourFormat(context)) }
}
