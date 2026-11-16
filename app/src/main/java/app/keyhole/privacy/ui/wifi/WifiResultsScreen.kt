package app.keyhole.privacy.ui.wifi

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.R
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.DeviceEvidence
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.domain.Sign
import app.keyhole.privacy.domain.Verdict
import app.keyhole.privacy.domain.WifiDevice
import app.keyhole.privacy.domain.WifiSnapshot
import app.keyhole.privacy.domain.WifiStatus
import app.keyhole.privacy.domain.sweepSteps
import app.keyhole.privacy.scan.WifiSession
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.Note
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.SummaryBox
import app.keyhole.privacy.ui.components.SummaryPart
import app.keyhole.privacy.ui.components.SummaryTone
import app.keyhole.privacy.ui.components.TopBarAction
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.next
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.ThemeMode
import app.keyhole.privacy.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class WifiResultsState(
    val wifi: WifiSnapshot = WifiSnapshot(),
    /** The step after Wi-Fi when this is part of a sweep; null when the check ran alone or was the last step. */
    val next: Check? = null,
    val loaded: Boolean = false,
    /** Part of a sweep: the last step ends on "See report". */
    val sweep: Boolean = false,
)

class WifiResultsViewModel(place: Place, sweep: Boolean, settings: SettingsRepository, session: WifiSession) : ViewModel() {
    val state: StateFlow<WifiResultsState> = combine(session.state, settings.settings) { w, s ->
        val steps = sweepSteps(place, s.stepOrder, s.stepsOff)
        WifiResultsState(w, if (sweep) steps.getOrNull(steps.indexOf(Check.WIFI) + 1) else null, loaded = true, sweep = sweep)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WifiResultsState(session.state.value))
}

/**
 * 04 Wi-Fi results: the note first (only the router answered, or nothing looks like a camera) · the count ·
 * Likely camera · Worth a look · the known and unrecognised devices behind one row each · Next.
 */
@Composable
fun WifiResultsScreen(vm: WifiResultsViewModel, onBack: () -> Unit, onScanAgain: () -> Unit, onDevice: (String) -> Unit, onOthers: () -> Unit, onNext: (Check?) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    // The results live in memory: after the app was closed there is nothing to show, so go back.
    val gone = s.loaded && s.wifi.status != WifiStatus.DONE
    LaunchedEffect(gone) { if (gone) onBack() }
    WifiResultsContent(s, onBack, onScanAgain, onDevice, onOthers, onNext)
}

@Composable
fun WifiResultsContent(s: WifiResultsState, onBack: () -> Unit, onScanAgain: () -> Unit, onDevice: (String) -> Unit, onOthers: () -> Unit, onNext: (Check?) -> Unit) {
    val w = s.wifi
    val likely = w.devices.filter { it.risk == Risk.LIKELY_CAMERA }
    val look = w.devices.filter { it.risk == Risk.CHECK_THIS }
    val known = w.devices.filter { it.risk == Risk.KNOWN }
    val other = w.devices.filter { it.risk == Risk.UNKNOWN }
    DetailScreen(
        onBack,
        title = stringResource(R.string.check_wifi),
        actions = { TopBarAction(stringResource(R.string.scan_again), onScanAgain) },
        dock = {
            if (s.next != null) {
                PrimaryButton(stringResource(s.next.next), { onNext(s.next) }, Modifier.fillMaxWidth(), trailing = painterResource(R.drawable.ic_arrow_right))
            } else {
                PrimaryButton(stringResource(if (s.sweep) R.string.see_report else R.string.done), { onNext(null) }, Modifier.fillMaxWidth())
            }
        },
    ) {
        // Notes go first.
        if (w.isolated) {
            Note(stringResource(R.string.isolation_text), title = stringResource(R.string.isolation_title), icon = painterResource(R.drawable.ic_warning), warning = true)
            Spacer(Modifier.height(Space.block))
        } else if (likely.isEmpty() && look.isEmpty()) {
            Note(stringResource(R.string.wifi_none_text), title = stringResource(R.string.wifi_none_title))
            Spacer(Modifier.height(Space.block))
        }
        val count = w.devices.size
        SummaryBox(
            w.ssid?.let { pluralStringResource(R.plurals.devices_on_wifi, count, count, it) } ?: pluralStringResource(R.plurals.devices_on_this_wifi, count, count),
            count.toString(),
            buildList {
                add(SummaryPart(likely.size, pluralStringResource(R.plurals.likely_cameras_count, likely.size, likely.size), SummaryTone.ALERT))
                add(SummaryPart(look.size, pluralStringResource(R.plurals.worth_a_look_count, look.size, look.size), SummaryTone.WARNING))
                add(SummaryPart(known.size, pluralStringResource(R.plurals.known_count, known.size, known.size), SummaryTone.NEUTRAL))
                if (other.isNotEmpty()) add(SummaryPart(other.size, pluralStringResource(R.plurals.not_recognised_count, other.size, other.size), SummaryTone.NEUTRAL))
            },
        )
        if (likely.isNotEmpty()) {
            SectionLabel(stringResource(R.string.risk_likely_camera), meta = likely.size.toString())
            Grouped { likely.forEach { d -> row { shape -> DeviceListRow(d, shape = shape, onClick = { onDevice(d.ip) }) } } }
        }
        if (look.isNotEmpty()) {
            SectionLabel(stringResource(R.string.risk_worth_a_look), meta = look.size.toString())
            Grouped { look.forEach { d -> row { shape -> DeviceListRow(d, shape = shape, onClick = { onDevice(d.ip) }) } } }
        }
        if (known.isNotEmpty() || other.isNotEmpty()) {
            Spacer(Modifier.height(Space.block))
            Grouped {
                if (known.isNotEmpty()) row { shape ->
                    ListRow(
                        pluralStringResource(R.plurals.known_devices, known.size, known.size), shape = shape, line = kindsLine(known),
                        leading = { IconCircle(painterResource(R.drawable.ic_smartphone), CircleTone.Neutral) }, onClick = onOthers,
                    )
                }
                if (other.isNotEmpty()) row { shape ->
                    ListRow(
                        pluralStringResource(R.plurals.other_devices, other.size, other.size), shape = shape, line = stringResource(R.string.other_devices_line),
                        leading = { IconCircle(painterResource(R.drawable.ic_help), CircleTone.Neutral) }, onClick = onOthers,
                    )
                }
            }
        }
    }
}

/** "Phone · TV · Router": the kinds among the known devices, each once, in the order they are listed. */
@Composable
private fun kindsLine(known: List<WifiDevice>): String? {
    val kinds = known.map { it.kind ?: R.string.risk_known }.distinct().take(MAX_KINDS).map { stringResource(it) }
    return kinds.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private const val MAX_KINDS = 4

internal fun previewWifi(): WifiSnapshot = WifiSnapshot(
    1, WifiStatus.DONE, "SeaView_Guest", 1f, 6, 6, 0,
    listOf(
        WifiDevice(DeviceEvidence("192.168.1.23", name = "IPC-3F2A", openPorts = setOf(80, 554, 8899)), Verdict(Risk.LIKELY_CAMERA, listOf(Sign.Onvif, Sign.Rtsp(554), Sign.CameraMaker("Xiongmai")), "Xiongmai"), 0),
        WifiDevice(DeviceEvidence("192.168.1.41", openPorts = setOf(8080)), Verdict(Risk.CHECK_THIS, listOf(Sign.WebPage)), 0),
        WifiDevice(DeviceEvidence("192.168.1.52", name = "Tuya device", openPorts = setOf(6668, 554)), Verdict(Risk.CHECK_THIS, listOf(Sign.Rtsp(554))), 0),
        WifiDevice(DeviceEvidence("192.168.1.9"), Verdict(Risk.UNKNOWN, emptyList()), 0),
        WifiDevice(DeviceEvidence("192.168.1.19", name = "Living-room TV"), Verdict(Risk.KNOWN, listOf(Sign.KnownKind("tv")), kind = "tv"), 0),
        WifiDevice(DeviceEvidence("192.168.1.1", isGateway = true), Verdict(Risk.KNOWN, listOf(Sign.Router)), 0),
    ),
)

@Preview
@Composable
private fun WifiResultsPreview() = KeyholeTheme {
    WifiResultsContent(WifiResultsState(previewWifi(), Check.HOTSPOTS, true), {}, {}, {}, {}, {})
}

@Preview
@Composable
private fun WifiResultsIsolatedDarkPreview() = KeyholeTheme(ThemeSettings(mode = ThemeMode.DARK)) {
    WifiResultsContent(WifiResultsState(previewWifi().let { it.copy(devices = it.devices.takeLast(1), isolated = true) }, null, true), {}, {}, {}, {}, {})
}
