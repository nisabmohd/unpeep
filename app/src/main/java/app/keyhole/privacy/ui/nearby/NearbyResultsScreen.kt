package app.keyhole.privacy.ui.nearby

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
import app.keyhole.privacy.domain.NearbyDevice
import app.keyhole.privacy.domain.NearbyKind
import app.keyhole.privacy.domain.NearbySnapshot
import app.keyhole.privacy.domain.NearbyStatus
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.domain.sweepSteps
import app.keyhole.privacy.scan.NearbySession
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.EmptyState
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
import app.keyhole.privacy.ui.icon
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.next
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.ThemeMode
import app.keyhole.privacy.ui.theme.ThemeSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@Immutable
data class NearbyResultsState(
    val nearby: NearbySnapshot,
    /** The step after this one when it is part of a sweep; null when the check ran alone or was the last step. */
    val next: Check? = null,
    /** Seconds since Android last refreshed the list. */
    val updatedSecondsAgo: Int = 0,
    val loaded: Boolean = false,
    /** Part of a sweep: the last step ends on "See report". */
    val sweep: Boolean = false,
)

/** Ticks once a second while a screen shows an age ("Updated 12 s ago"). */
internal fun everySecond(now: () -> Long) = flow {
    while (true) {
        emit(now())
        delay(1_000)
    }
}

internal fun secondsSince(at: Long, now: Long): Int = if (at <= 0) 0 else ((now - at) / 1_000).toInt().coerceAtLeast(0)

class NearbyResultsViewModel(place: Place, sweep: Boolean, settings: SettingsRepository, session: NearbySession<*>, now: () -> Long = System::currentTimeMillis) : ViewModel() {
    val state: StateFlow<NearbyResultsState> = combine(session.state, settings.settings, everySecond(now)) { n, s, t ->
        val steps = sweepSteps(place, s.stepOrder, s.stepsOff)
        NearbyResultsState(n, if (sweep) steps.getOrNull(steps.indexOf(n.check) + 1) else null, secondsSince(n.updatedAt, t), loaded = true, sweep = sweep)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NearbyResultsState(session.state.value))
}

/**
 * Hotspot and Bluetooth results, built as 04 Wi-Fi results: the note first (nothing looks like a camera) · the
 * count · Likely camera · Worth a look · Marked as known · everything else behind one row · Next.
 * A row opens Find it.
 */
@Composable
fun NearbyResultsScreen(vm: NearbyResultsViewModel, onBack: () -> Unit, onScanAgain: () -> Unit, onFind: (String) -> Unit, onOthers: () -> Unit, onNext: (Check?) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    // The results live in memory: after the app was closed there is nothing to show, so go back.
    val gone = s.loaded && s.nearby.status != NearbyStatus.DONE
    LaunchedEffect(gone) { if (gone) onBack() }
    NearbyResultsContent(s, onBack, onScanAgain, onFind, onOthers, onNext)
}

@Composable
fun NearbyResultsContent(s: NearbyResultsState, onBack: () -> Unit, onScanAgain: () -> Unit, onFind: (String) -> Unit, onOthers: () -> Unit, onNext: (Check?) -> Unit) {
    val n = s.nearby
    val words = n.check.nearbyWords
    val likely = n.devices.filter { it.risk == Risk.LIKELY_CAMERA }
    val look = n.devices.filter { it.risk == Risk.CHECK_THIS }
    val known = n.devices.filter { it.risk == Risk.KNOWN }
    val other = n.devices.filter { it.risk == Risk.UNKNOWN }
    DetailScreen(
        onBack,
        title = stringResource(n.check.label),
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
        if (likely.isEmpty() && look.isEmpty()) {
            Note(stringResource(words.noneText), title = stringResource(words.noneTitle))
            Spacer(Modifier.height(Space.block))
        }
        val count = n.devices.size
        SummaryBox(
            stringResource(R.string.two_parts, pluralStringResource(words.nearby, count, count), updatedText(s.updatedSecondsAgo)),
            count.toString(),
            listOf(
                SummaryPart(likely.size, pluralStringResource(R.plurals.likely_cameras_count, likely.size, likely.size), SummaryTone.ALERT),
                SummaryPart(look.size, pluralStringResource(R.plurals.worth_a_look_count, look.size, look.size), SummaryTone.WARNING),
                SummaryPart(known.size + other.size, pluralStringResource(R.plurals.others_count, known.size + other.size, known.size + other.size), SummaryTone.NEUTRAL),
            ),
        )
        NearbySection(stringResource(R.string.risk_likely_camera), likely, onFind)
        NearbySection(stringResource(R.string.risk_worth_a_look), look, onFind)
        NearbySection(stringResource(R.string.marked_as_known), known, onFind)
        if (other.isNotEmpty()) {
            Spacer(Modifier.height(Space.block))
            ListRow(
                pluralStringResource(words.others, other.size, other.size), line = stringResource(words.othersLine),
                leading = { IconCircle(painterResource(n.check.icon), CircleTone.Neutral) }, onClick = onOthers,
            )
        }
    }
}

@Composable
private fun NearbySection(title: String, devices: List<NearbyDevice>, onFind: (String) -> Unit) {
    if (devices.isEmpty()) return
    SectionLabel(title, meta = devices.size.toString())
    Grouped { devices.forEach { d -> row { shape -> NearbyListRow(d, shape = shape, onClick = { onFind(d.id) }) } } }
}

/** "Updated 12 s ago", or "Updated just now" in the first seconds. */
@Composable
fun updatedText(secondsAgo: Int): String =
    if (secondsAgo < JUST_NOW) stringResource(R.string.updated_now) else pluralStringResource(R.plurals.updated_ago, secondsAgo, secondsAgo)

private const val JUST_NOW = 3

class NearbyOthersViewModel(session: NearbySession<*>) : ViewModel() {
    val state: StateFlow<NearbySnapshot> = session.state
}

/** The plain networks or Bluetooth devices behind "12 other networks": each opens Find it like any other. */
@Composable
fun NearbyOthersScreen(vm: NearbyOthersViewModel, onBack: () -> Unit, onFind: (String) -> Unit) {
    val n by vm.state.collectAsStateWithLifecycle()
    NearbyOthersContent(n, onBack, onFind)
}

@Composable
fun NearbyOthersContent(n: NearbySnapshot, onBack: () -> Unit, onFind: (String) -> Unit) {
    val words = n.check.nearbyWords
    val other = n.devices.filter { it.risk == Risk.UNKNOWN }
    DetailScreen(onBack, title = stringResource(words.othersTitle)) {
        if (other.isEmpty()) {
            EmptyState(painterResource(n.check.icon), stringResource(words.othersTitle), stringResource(R.string.nothing_flagged_yet))
        } else {
            Note(stringResource(words.othersLine))
            Spacer(Modifier.height(Space.block))
            Grouped { other.forEach { d -> row { shape -> NearbyListRow(d, shape = shape, onClick = { onFind(d.id) }) } } }
        }
    }
}

internal fun previewHotspots(): NearbySnapshot = NearbySnapshot(
    Check.HOTSPOTS, 1, NearbyStatus.DONE, fraction = 1f,
    devices = listOf(
        NearbyDevice(Check.HOTSPOTS, "a", "V380-8A12F0", -48, NearbyKind.CAMERA_HOTSPOT, Risk.LIKELY_CAMERA, "V380"),
        NearbyDevice(Check.HOTSPOTS, "b", "SmartLife-1A2B", -70, NearbyKind.CAMERA_HOTSPOT, Risk.CHECK_THIS, "Tuya", added = true),
        NearbyDevice(Check.HOTSPOTS, "c", "SeaView_Guest", -40, NearbyKind.NETWORK, Risk.UNKNOWN),
        NearbyDevice(Check.HOTSPOTS, "d", "Cafe", -81, NearbyKind.NETWORK, Risk.UNKNOWN),
    ),
)

internal fun previewBluetooth(): NearbySnapshot = NearbySnapshot(
    Check.BLUETOOTH, 1, NearbyStatus.DONE, fraction = 1f,
    devices = listOf(
        NearbyDevice(Check.BLUETOOTH, "AA", null, -60, NearbyKind.TRACKER, Risk.CHECK_THIS, "Tile"),
        NearbyDevice(Check.BLUETOOTH, "BB", "Buds", -50, NearbyKind.BLE_OTHER, Risk.UNKNOWN),
    ),
)

@Preview
@Composable
private fun HotspotResultsPreview() = KeyholeTheme { NearbyResultsContent(NearbyResultsState(previewHotspots(), Check.BLUETOOTH, 12, true), {}, {}, {}, {}, {}) }

@Preview
@Composable
private fun BluetoothResultsDarkPreview() = KeyholeTheme(ThemeSettings(mode = ThemeMode.DARK)) { NearbyResultsContent(NearbyResultsState(previewBluetooth(), null, 0, true), {}, {}, {}, {}, {}) }
