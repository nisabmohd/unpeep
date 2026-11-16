package app.keyhole.privacy.ui.nearby

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.NearbyBlock
import app.keyhole.privacy.domain.NearbyDevice
import app.keyhole.privacy.domain.NearbyKind
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.Tag
import app.keyhole.privacy.ui.components.TagKind
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.wifi.tone

/*
 * How a hotspot or a Bluetooth device is shown, the same on every screen: its circle (red only for a likely
 * camera, amber for worth a look, grey for the rest), what kind of thing it is and why it is listed, in words.
 */

val NearbyKind.label: Int
    @StringRes get() = when (this) {
        NearbyKind.CAMERA_HOTSPOT -> R.string.kind_camera_hotspot
        NearbyKind.NETWORK -> R.string.kind_network
        NearbyKind.TRACKER -> R.string.kind_tracker
        NearbyKind.BLE_CAMERA -> R.string.kind_ble_camera
        NearbyKind.BLE_OTHER -> R.string.kind_ble_other
    }

val NearbyKind.icon: Int
    @DrawableRes get() = when (this) {
        NearbyKind.CAMERA_HOTSPOT, NearbyKind.BLE_CAMERA -> R.drawable.ic_camera_device
        NearbyKind.NETWORK -> R.drawable.ic_hotspot
        NearbyKind.TRACKER -> R.drawable.ic_tag
        NearbyKind.BLE_OTHER -> R.drawable.ic_bluetooth
    }

/** The name it broadcasts; a nameless Bluetooth device is called by its kind or family ("Tile", "Unnamed device"). */
@Composable
fun NearbyDevice.title(): String = name ?: label?.takeIf { kind == NearbyKind.TRACKER } ?: stringResource(R.string.unnamed_device)

/** Why it is listed, as one sentence (the same words as the report: ReportWords). */
@Composable
fun NearbyDevice.reason(): String = app.keyhole.privacy.ui.report.ReportWords.nearbyReason(androidx.compose.ui.platform.LocalContext.current.resources, kind, label, risk)

/** The tag of a row: its result in words; nothing for a plain network or device. */
@Composable
fun NearbyTag(device: NearbyDevice) {
    when (device.risk) {
        Risk.LIKELY_CAMERA -> Tag(stringResource(R.string.risk_likely_camera), TagKind.LIKELY_CAMERA)
        Risk.CHECK_THIS -> Tag(stringResource(R.string.risk_worth_a_look), TagKind.WORTH_A_LOOK)
        Risk.KNOWN -> Tag(stringResource(R.string.risk_known), TagKind.KNOWN)
        Risk.UNKNOWN -> Unit
    }
}

/**
 * A hotspot or Bluetooth device in a list. While the scan runs it shows its kind and its tag; in results it shows
 * why it is listed (and "Added to report") and opens Find it ([onClick]).
 */
@Composable
fun NearbyListRow(device: NearbyDevice, modifier: Modifier = Modifier, shape: Shape = Shapes.surface, onClick: (() -> Unit)? = null) {
    val line = when {
        onClick == null -> stringResource(device.kind.label)
        device.added -> stringResource(R.string.two_parts, device.reason(), stringResource(R.string.added_to_report))
        else -> device.reason()
    }
    ListRow(
        device.title(), modifier, shape, line = line,
        leading = { IconCircle(painterResource(device.kind.icon), device.risk.tone) },
        tag = if (onClick == null && device.risk != Risk.UNKNOWN) { { NearbyTag(device) } } else null,
        onClick = onClick,
    )
}

/** The words of a nearby check that differ between hotspots and Bluetooth. */
class NearbyWords(
    @StringRes val checking: Int,
    @PluralsRes val nearby: Int,
    @StringRes val noneTitle: Int,
    @StringRes val noneText: Int,
    @PluralsRes val others: Int,
    @StringRes val othersLine: Int,
    @StringRes val othersTitle: Int,
    @StringRes val permTitle: Int,
    @StringRes val permLine: Int,
    @StringRes val findNote: Int,
)

val Check.nearbyWords: NearbyWords
    get() = if (this == Check.BLUETOOTH) {
        NearbyWords(
            R.string.checking_bluetooth, R.plurals.bluetooth_nearby, R.string.bluetooth_none_title, R.string.bluetooth_none_text,
            R.plurals.other_bluetooth, R.string.other_bluetooth_line, R.string.other_bluetooth_title,
            R.string.perm_bluetooth_title, R.string.perm_bluetooth_line, R.string.find_note_bluetooth,
        )
    } else {
        NearbyWords(
            R.string.checking_hotspots, R.plurals.networks_nearby, R.string.hotspots_none_title, R.string.hotspots_none_text,
            R.plurals.other_networks, R.string.other_networks_line, R.string.other_networks_title,
            R.string.perm_hotspots_title, R.string.perm_hotspots_line, R.string.find_note_hotspots,
        )
    }

val NearbyBlock.title: Int
    @StringRes get() = when (this) {
        NearbyBlock.WIFI_OFF -> R.string.block_wifi_off_title
        NearbyBlock.BLUETOOTH_OFF -> R.string.block_bluetooth_off_title
        NearbyBlock.LOCATION_OFF -> R.string.block_location_off_title
        NearbyBlock.NO_BLUETOOTH -> R.string.block_no_bluetooth_title
    }

val NearbyBlock.line: Int
    @StringRes get() = when (this) {
        NearbyBlock.WIFI_OFF -> R.string.block_wifi_off_line
        NearbyBlock.BLUETOOTH_OFF -> R.string.block_bluetooth_off_line
        NearbyBlock.LOCATION_OFF -> R.string.block_location_off_line
        NearbyBlock.NO_BLUETOOTH -> R.string.block_no_bluetooth_line
    }

/** The icon of a state a check can't run in, when it isn't the check's own (Bluetooth is off, Location is off). */
val NearbyBlock.icon: Int?
    @DrawableRes get() = when (this) {
        NearbyBlock.BLUETOOTH_OFF, NearbyBlock.NO_BLUETOOTH -> R.drawable.ic_bluetooth_off
        NearbyBlock.LOCATION_OFF -> R.drawable.ic_map_point
        NearbyBlock.WIFI_OFF -> null
    }
