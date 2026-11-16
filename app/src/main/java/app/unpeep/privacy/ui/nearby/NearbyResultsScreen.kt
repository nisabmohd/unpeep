package app.unpeep.privacy.ui.nearby

import app.unpeep.privacy.ui.components.keepTogether
import app.unpeep.privacy.ui.components.LocalToasts
import app.unpeep.privacy.ui.hue
import app.unpeep.privacy.ui.components.QuietLine
import app.unpeep.privacy.ui.components.LensHeader
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
import app.unpeep.privacy.R
import app.unpeep.privacy.data.SettingsRepository
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.domain.NearbyDevice
import app.unpeep.privacy.domain.NearbyKind
import app.unpeep.privacy.domain.NearbySnapshot
import app.unpeep.privacy.domain.NearbyStatus
import app.unpeep.privacy.domain.Place
import app.unpeep.privacy.domain.Risk
import app.unpeep.privacy.domain.sweepSteps
import app.unpeep.privacy.scan.NearbySession
import app.unpeep.privacy.ui.components.CircleTone
import app.unpeep.privacy.ui.components.DetailScreen
import app.unpeep.privacy.ui.components.EmptyState
import app.unpeep.privacy.ui.components.Grouped
import app.unpeep.privacy.ui.components.IconCircle
import app.unpeep.privacy.ui.components.ListRow
import app.unpeep.privacy.ui.components.Note
import app.unpeep.privacy.ui.components.NoteTone
import app.unpeep.privacy.ui.components.PrimaryButton
import app.unpeep.privacy.ui.components.SectionLabel
import app.unpeep.privacy.ui.components.SummaryBox
import app.unpeep.privacy.ui.components.SummaryPart
import app.unpeep.privacy.ui.components.SummaryTone
import app.unpeep.privacy.ui.components.TopBarAction
import app.unpeep.privacy.ui.icon
import app.unpeep.privacy.ui.label
import app.unpeep.privacy.ui.next
import app.unpeep.privacy.ui.theme.UnpeepTheme
import app.unpeep.privacy.ui.theme.Space
import app.unpeep.privacy.ui.theme.ThemeMode
import app.unpeep.privacy.ui.theme.ThemeSettings
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
    /** Seconds until Android allows another Wi-Fi scan (4 every 2 minutes, E01); 0 when Scan again works now. */
    val scanWait: Int = 0,
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
        NearbyResultsState(
            n, if (sweep) steps.getOrNull(steps.indexOf(n.check) + 1) else null, secondsSince(n.updatedAt, t), loaded = true, sweep = sweep,
            scanWait = ((session.waitMs() + 999) / 1000).toInt(),
        )
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
    val toasts = LocalToasts.current
    val cleared = stringResource(R.string.results_cleared)
    LaunchedEffect(gone) {
        if (gone) {
            if (s.nearby.status == NearbyStatus.IDLE) toasts.show(cleared)
            onBack()
        }
    }
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
        actions = {
            if (s.scanWait > 0) TopBarAction(pluralStringResource(R.plurals.scan_again_in, s.scanWait, s.scanWait), onScanAgain, enabled = false)
            else TopBarAction(stringResource(R.string.scan_again), onScanAgain)
        },
        dock = {
            if (s.next != null) {
                PrimaryButton(stringResource(s.next.next), { onNext(s.next) }, Modifier.fillMaxWidth(), trailing = painterResource(R.drawable.ic_arrow_right))
            } else {
                PrimaryButton(stringResource(if (s.sweep) R.string.see_report else R.string.done), { onNext(null) }, Modifier.fillMaxWidth())
            }
        },
    ) {
        // Notes go first: Android's ration of Wi-Fi scans is used up, so these are the last results (E01).
        if (s.scanWait > 0) {
            Note(stringResource(R.string.scan_paused_text), title = stringResource(R.string.scan_paused_title), tone = NoteTone.WARNING)
            Spacer(Modifier.height(Space.block))
        }
        val count = n.devices.size
        // Nothing found (00-decisions): the plain result, one row for what was heard, one quiet line. Nothing heard at
        // all: the count box at 0 and the check's own empty state.
        val nothing = likely.isEmpty() && look.isEmpty() && known.isEmpty()
        if (nothing && count > 0) {
            LensHeader(
                painterResource(R.drawable.ic_check_circle), stringResource(words.noneTitle),
                line = pluralStringResource(if (n.check == Check.BLUETOOTH) R.plurals.bluetooth_heard else R.plurals.networks_heard, count, count) + " " + stringResource(R.string.none_like_camera),
            )
            Spacer(Modifier.height(Space.heading))
            ListRow(
                pluralStringResource(words.others, other.size, other.size), line = stringResource(words.othersLine),
                leading = { IconCircle(painterResource(n.check.icon), CircleTone.Neutral) }, onClick = onOthers,
            )
            QuietLine(stringResource(words.noneText))
            return@DetailScreen
        }
        SummaryBox(
            stringResource(R.string.two_parts, pluralStringResource(words.nearby, count, count), updatedText(s.updatedSecondsAgo).keepTogether()),
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
        if (count == 0) {
            EmptyState(painterResource(n.check.icon), stringResource(R.string.nothing_flagged_yet), stringResource(words.noneText), hue = n.check.hue, under = app.unpeep.privacy.ui.components.EmptyUnder.SUMMARY)
        }
        if (other.isNotEmpty()) {
            Spacer(Modifier.height(Space.card))
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
            EmptyState(painterResource(n.check.icon), null, stringResource(R.string.nothing_flagged_yet))
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
private fun HotspotResultsPreview() = UnpeepTheme { NearbyResultsContent(NearbyResultsState(previewHotspots(), Check.BLUETOOTH, 12, true), {}, {}, {}, {}, {}) }

@Preview
@Composable
private fun BluetoothResultsDarkPreview() = UnpeepTheme(ThemeSettings(mode = ThemeMode.DARK)) { NearbyResultsContent(NearbyResultsState(previewBluetooth(), null, 0, true), {}, {}, {}, {}, {}) }
