package app.unpeep.privacy.ui.wifi

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.unpeep.privacy.R
import app.unpeep.privacy.domain.Risk
import app.unpeep.privacy.domain.Sign
import app.unpeep.privacy.domain.WifiDevice
import app.unpeep.privacy.ui.components.CircleTone
import app.unpeep.privacy.ui.components.IconCircle
import app.unpeep.privacy.ui.components.ListRow
import app.unpeep.privacy.ui.components.Tag
import app.unpeep.privacy.ui.components.TagKind
import app.unpeep.privacy.ui.theme.Shapes

/*
 * How a device on the Wi-Fi is shown, the same on every screen: its circle (red only for a likely camera, amber
 * for worth a look, grey for the rest), its icon, its result in words and its reasons as sentences.
 */

val Risk.label: Int
    @StringRes get() = when (this) {
        Risk.LIKELY_CAMERA -> R.string.risk_likely_camera
        Risk.CHECK_THIS -> R.string.risk_worth_a_look
        Risk.KNOWN -> R.string.risk_known
        Risk.UNKNOWN -> R.string.risk_not_recognised
    }

val Risk.tone: CircleTone
    get() = when (this) {
        Risk.LIKELY_CAMERA -> CircleTone.Alert
        Risk.CHECK_THIS -> CircleTone.Warning
        Risk.KNOWN, Risk.UNKNOWN -> CircleTone.Neutral
    }

/** The name of a kind in signatures/known.json, or null for one this version has no word for. */
@StringRes
fun kindLabel(kind: String?): Int? = when (kind) {
    "phone" -> R.string.kind_phone
    "computer" -> R.string.kind_computer
    "tv" -> R.string.kind_tv
    "speaker" -> R.string.kind_speaker
    "printer" -> R.string.kind_printer
    "router" -> R.string.kind_router
    "console" -> R.string.kind_console
    "storage" -> R.string.kind_storage
    "home" -> R.string.kind_home
    else -> null
}

/** What kind of known thing this is, in one or two words ("TV", "Router", "This phone"); null when nothing says. */
val WifiDevice.kind: Int?
    @StringRes get() = when {
        evidence.isSelf -> R.string.kind_this_phone
        verdict.kind != null -> kindLabel(verdict.kind)
        evidence.isGateway && risk == Risk.KNOWN -> R.string.kind_router
        else -> null
    }

val WifiDevice.icon: Int
    @DrawableRes get() = when (risk) {
        Risk.LIKELY_CAMERA -> R.drawable.ic_camera_device
        Risk.CHECK_THIS -> if (verdict.signs == listOf(Sign.WebPage)) R.drawable.ic_help else R.drawable.ic_plug
        Risk.UNKNOWN -> R.drawable.ic_help
        Risk.KNOWN -> when (kind) {
            R.string.kind_this_phone, R.string.kind_phone -> R.drawable.ic_smartphone
            R.string.kind_tv -> R.drawable.ic_tv
            R.string.kind_router -> R.drawable.ic_wifi
            R.string.kind_computer -> R.drawable.ic_laptop
            R.string.kind_printer -> R.drawable.ic_printer
            R.string.kind_speaker -> R.drawable.ic_speaker
            R.string.kind_console -> R.drawable.ic_gamepad
            R.string.kind_storage -> R.drawable.ic_server
            R.string.kind_home -> R.drawable.ic_home
            else -> R.drawable.ic_plug
        }
    }

/** The device's name, or its address when it has none. */
val WifiDevice.title: String get() = name ?: ip

/** One reason, as a sentence (the same words as the report: ReportWords). */
@Composable
fun signText(sign: Sign): String = app.unpeep.privacy.ui.report.ReportWords.sign(androidx.compose.ui.platform.LocalContext.current.resources, sign)

/** The first reason, for a row's second line; a device with no reason says so. */
/**
 * A row's line (04): the reason in a few words, with the port where it tells something ("Answers as a camera ·
 * port 554", "Has a web page on port 8080"). The device screen gives every reason in full.
 */
@Composable
fun WifiDevice.rowReason(): String {
    val signs = verdict.signs
    val rtsp = signs.filterIsInstance<Sign.Rtsp>().firstOrNull()?.port
    val webPort = evidence.openPorts.firstOrNull { it in WEB_PORTS && it != DEFAULT_WEB_PORT }
    return when {
        verdict.risk == Risk.LIKELY_CAMERA && rtsp != null -> stringResource(R.string.row_camera_port, rtsp)
        verdict.risk == Risk.LIKELY_CAMERA -> stringResource(R.string.row_camera)
        Sign.WebPage in signs && webPort != null -> stringResource(R.string.row_web_port, webPort)
        else -> reason()
    }
}

private val WEB_PORTS = setOf(80, 8000, 8080, 8443, 443, 81, 88, 8888)
private const val DEFAULT_WEB_PORT = 80

@Composable
fun WifiDevice.reason(): String = verdict.signs.firstOrNull()?.let { signText(it) } ?: stringResource(R.string.sign_none)

/** The tag of a device: its result in words, or what kind of known thing it is. */
@Composable
fun DeviceTag(device: WifiDevice) {
    when (device.risk) {
        Risk.LIKELY_CAMERA -> Tag(stringResource(R.string.risk_likely_camera), TagKind.LIKELY_CAMERA)
        Risk.CHECK_THIS -> Tag(stringResource(R.string.risk_worth_a_look), TagKind.WORTH_A_LOOK)
        Risk.KNOWN -> Tag(stringResource(device.kind ?: R.string.risk_known), TagKind.KNOWN)
        Risk.UNKNOWN -> Unit
    }
}

/**
 * A device in a list. While the scan runs it shows the address and its tag; in results it shows why and opens the
 * device ([onClick]).
 */
@Composable
fun DeviceListRow(device: WifiDevice, modifier: Modifier = Modifier, shape: Shape = Shapes.surface, onClick: (() -> Unit)? = null) {
    ListRow(
        device.title, modifier, shape,
        line = if (onClick == null) device.ip.takeIf { device.name != null } else device.rowReason(),
        leading = { IconCircle(painterResource(device.icon), device.risk.tone) },
        tag = if (onClick == null && device.risk != Risk.UNKNOWN) { { DeviceTag(device) } } else null,
        onClick = onClick,
    )
}
