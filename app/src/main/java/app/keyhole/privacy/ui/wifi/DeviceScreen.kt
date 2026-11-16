package app.keyhole.privacy.ui.wifi

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.domain.WifiDevice
import app.keyhole.privacy.scan.WifiSession
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.reportWrongResult
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.LensHeader
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.RoundIconButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.SignRow
import app.keyhole.privacy.ui.components.Tag
import app.keyhole.privacy.ui.components.TagKind
import app.keyhole.privacy.ui.components.ValueRow
import app.keyhole.privacy.ui.components.colors
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.ThemeMode
import app.keyhole.privacy.ui.theme.ThemeSettings
import java.util.Date
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class DeviceState(val device: WifiDevice? = null, val loaded: Boolean = false)

class DeviceViewModel(private val ip: String, private val session: WifiSession) : ViewModel() {
    val state: StateFlow<DeviceState> = session.state.map { DeviceState(it.device(ip), loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceState(session.state.value.device(ip)))

    /** "Not a camera?": null (Not sure) keeps it on the list. */
    fun mark(knownAs: KnownAs?) {
        state.value.device?.let { session.mark(it, knownAs) }
    }
}

/**
 * 05 Device: the lens header with its result in words · Why (every sign, one sentence each) · what it answered ·
 * Not a camera? · for a device that may be a camera, Found it? and Find its lens.
 */
@Composable
fun DeviceScreen(vm: DeviceViewModel, onBack: () -> Unit, onFoundIt: () -> Unit, onFindLens: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val gone = s.loaded && s.device == null
    LaunchedEffect(gone) { if (gone) onBack() }
    val device = s.device ?: return
    var asking by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val shareText = shareText(device)
    DeviceContent(device, onBack, onShare = { share(context, shareText) }, onNotACamera = { asking = true }, onFoundIt, onFindLens)
    if (asking) {
        NotACameraSheet(
            device.knownAs,
            onDismiss = { asking = false },
            onMark = { haptics.buzz(Buzz.CONFIRM); vm.mark(it); asking = false },
            onReport = { reportWrongResult(context) },
        )
    }
}

@Composable
fun DeviceContent(device: WifiDevice, onBack: () -> Unit, onShare: () -> Unit, onNotACamera: () -> Unit, onFoundIt: () -> Unit, onFindLens: () -> Unit) {
    val context = LocalContext.current
    val seen = stringResource(R.string.seen_at, DateFormat.getTimeFormat(context).format(Date(device.seenAt)))
    val (container, onContainer) = device.risk.tone.colors()
    // Anything not known can be looked for (05, 05a); known devices and this phone have no dock (05b, 05c).
    val mayBeCamera = device.risk != Risk.KNOWN
    DetailScreen(
        onBack,
        actions = { RoundIconButton(painterResource(R.drawable.ic_share), stringResource(R.string.share), onShare) },
        dock = if (mayBeCamera) { {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.found_it), onFoundIt, m) },
                primary = { m -> PrimaryButton(stringResource(R.string.find_its_lens), onFindLens, m, icon = painterResource(R.drawable.ic_lens)) },
            )
        } } else null,
    ) {
        LensHeader(
            painterResource(device.icon), device.title,
            line = if (device.name != null) stringResource(R.string.two_parts, device.ip, seen) else seen,
            container = container, onContainer = onContainer,
            tags = {
                when (device.risk) {
                    Risk.LIKELY_CAMERA -> Tag(stringResource(R.string.risk_likely_camera), TagKind.LIKELY_CAMERA, icon = painterResource(R.drawable.ic_warning))
                    Risk.CHECK_THIS -> Tag(stringResource(R.string.risk_worth_a_look), TagKind.WORTH_A_LOOK)
                    Risk.KNOWN -> Tag(stringResource(device.kind ?: R.string.risk_known), TagKind.KNOWN)
                    Risk.UNKNOWN -> Tag(stringResource(R.string.risk_not_recognised), TagKind.KNOWN)
                }
                val strong = device.verdict.strongSigns
                if (strong > 0) Tag(pluralStringResource(R.plurals.strong_signs, strong, strong), TagKind.KNOWN)
            },
        )
        SectionLabel(stringResource(R.string.why))
        val signs = device.verdict.signs
        Grouped {
            if (signs.isEmpty()) row { shape -> SignRow(painterResource(R.drawable.ic_info), stringResource(R.string.sign_none), shape = shape) }
            signs.forEach { sign ->
                // The alert icon only where the result is "Likely camera": red means nothing else.
                val alert = sign.strong && device.risk == Risk.LIKELY_CAMERA
                row { shape -> SignRow(painterResource(if (sign.strong) R.drawable.ic_danger else R.drawable.ic_info), signText(sign), shape = shape, alert = alert) }
            }
        }
        Spacer(Modifier.height(Space.block))
        val ports = device.evidence.openPorts.sorted()
        val maker = device.verdict.maker ?: device.evidence.upnpManufacturer
        Grouped {
            row { shape -> ValueRow(stringResource(R.string.open_ports), if (ports.isEmpty()) stringResource(R.string.none_found) else ports.joinToString(" · "), shape = shape) }
            // Android hides other devices' hardware addresses, so most makers can't be read at all: say so (05a).
            if (maker != null || device.risk != Risk.KNOWN) row { shape ->
                ValueRow(
                    stringResource(R.string.maker),
                    when {
                        maker == null -> stringResource(R.string.maker_hidden)
                        device.verdict.maker != null -> stringResource(R.string.maker_likely, maker)
                        else -> maker
                    },
                    shape = shape,
                )
            }
            if (!device.evidence.isSelf) row { shape ->
                ListRow(
                    stringResource(R.string.not_a_camera), shape = shape,
                    line = stringResource(
                        when (device.knownAs) {
                            KnownAs.MINE -> R.string.marked_mine_line
                            KnownAs.ROOM -> R.string.marked_room_line
                            null -> R.string.not_a_camera_line
                        },
                    ),
                    leading = { IconCircle(painterResource(R.drawable.ic_shield), CircleTone.Accent) }, onClick = onNotACamera,
                )
            }
        }
    }
}

/** What "Share" sends: the device, its result and every reason, in plain words. */
@Composable
private fun shareText(device: WifiDevice): String {
    val lines = mutableListOf(
        if (device.name != null) stringResource(R.string.two_parts, device.title, device.ip) else device.ip,
        stringResource(device.risk.label),
    )
    device.verdict.signs.forEach { lines += "- " + signText(it) }
    val ports = device.evidence.openPorts.sorted()
    if (ports.isNotEmpty()) lines += stringResource(R.string.two_parts, stringResource(R.string.open_ports), ports.joinToString(", "))
    lines += stringResource(R.string.share_footer)
    return lines.joinToString("\n")
}

private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(Intent.createChooser(send, null)) }
}

@Preview
@Composable
private fun DevicePreview() = KeyholeTheme { DeviceContent(previewWifi().devices[0], {}, {}, {}, {}, {}) }

@Preview
@Composable
private fun DeviceKnownDarkPreview() = KeyholeTheme(ThemeSettings(mode = ThemeMode.DARK)) { DeviceContent(previewWifi().devices[4], {}, {}, {}, {}, {}) }
