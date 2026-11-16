package app.keyhole.privacy.ui.nearby

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.domain.METER_SEGMENTS
import app.keyhole.privacy.domain.NearbyDevice
import app.keyhole.privacy.domain.Nearness
import app.keyhole.privacy.domain.Trend
import app.keyhole.privacy.domain.meterLevel
import app.keyhole.privacy.domain.nearness
import app.keyhole.privacy.domain.signalLevel
import app.keyhole.privacy.domain.trend
import app.keyhole.privacy.scan.NearbySession
import app.keyhole.privacy.scan.SignalReading
import app.keyhole.privacy.ui.components.Box24
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.reportWrongResult
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.CheckIcon
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.LensProgress
import app.keyhole.privacy.ui.components.Note
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.SegmentMeter
import app.keyhole.privacy.ui.components.Tag
import app.keyhole.privacy.ui.components.TagKind
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.ThemeMode
import app.keyhole.privacy.ui.theme.ThemeSettings
import app.keyhole.privacy.ui.wifi.NotACameraSheet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

@Immutable
data class FindItState(
    /** Null when the thing is no longer in the session (the app was closed): the screen goes back. */
    val device: NearbyDevice? = null,
    val rssi: Int = 0,
    val nearness: Nearness = Nearness.FAR,
    /** Lit segments of the meter, 0..[METER_SEGMENTS]. */
    val level: Int = 0,
    val trend: Trend = Trend.STEADY,
    val updatedSecondsAgo: Int = 0,
    /** Not heard for a while: the last signal is shown, and the screen says so. */
    val lost: Boolean = false,
    val loaded: Boolean = false,
)

/**
 * 07 Find it. It listens for one hotspot or Bluetooth device only while the screen is showing (the signal flow is
 * collected with the screen and stops a few seconds after it goes away), and turns the readings into words.
 */
class FindItViewModel(private val id: String, private val session: NearbySession<*>, now: () -> Long = System::currentTimeMillis) : ViewModel() {
    /** The thing as it was when the screen opened: a new scan may drop it from the list, the search goes on. */
    private val opened: NearbyDevice? = session.state.value.device(id)

    private val readings = (opened?.let(session::watch) ?: emptyFlow())
        .scan(listOfNotNull(opened?.let { SignalReading(it.rssi, session.state.value.updatedAt.takeIf { at -> at > 0 } ?: now()) })) { all, r -> (all + r).takeLast(KEEP) }

    val state: StateFlow<FindItState> = combine(session.state, readings, everySecond(now)) { snapshot, all, t ->
        // Marks and "Add to report" come from the session; the signal from the readings.
        val device = snapshot.device(id) ?: opened?.let { o -> snapshot.devices.firstOrNull { it.name != null && it.name == o.name } } ?: opened
        val last = all.lastOrNull()
        if (device == null || last == null) return@combine FindItState(loaded = true)
        val ago = secondsSince(last.at, t)
        FindItState(device, last.rssi, nearness(last.rssi), meterLevel(last.rssi), trend(all.map { it.rssi }), ago, lost = ago >= lostAfter(device.check), loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), FindItState(opened, opened?.rssi ?: 0, nearness(opened?.rssi ?: FAR_RSSI), meterLevel(opened?.rssi ?: FAR_RSSI)))

    fun mark(knownAs: KnownAs?) {
        state.value.device?.let { session.mark(it, knownAs) }
    }

    fun toggleAdded() {
        state.value.device?.let { session.setAdded(it, !it.added) }
    }

    private companion object {
        const val KEEP = 20
        const val STOP_AFTER_MS = 3_000L
        const val FAR_RSSI = -100
        /** Hotspots refresh every 30 s at best; Bluetooth about once a second. */
        fun lostAfter(check: Check): Int = if (check == Check.BLUETOOTH) 15 else 75
    }
}

@Composable
fun FindItScreen(vm: FindItViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val gone = s.loaded && s.device == null
    LaunchedEffect(gone) { if (gone) onBack() }
    val device = s.device ?: return
    var asking by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberHaptics()
    // Clearly stronger: worth a buzz, once each time it turns that way.
    LaunchedEffect(s.trend) { if (s.trend == Trend.STRONGER) haptics.buzz(Buzz.FOUND) }
    val context = LocalContext.current
    FindItContent(s, onBack, onNotACamera = { asking = true }, onToggleAdded = { haptics.buzz(if (device.added) Buzz.TOGGLE_OFF else Buzz.CONFIRM); vm.toggleAdded() })
    if (asking) {
        NotACameraSheet(
            device.knownAs,
            onDismiss = { asking = false },
            onMark = { haptics.buzz(Buzz.CONFIRM); vm.mark(it); asking = false },
            onReport = { reportWrongResult(context) },
        )
    }
}

/**
 * 07 Find it: when the signal was last updated · the lens filling as it gets stronger, with how near in words ·
 * what it is · the meter and which way the signal is going · how to walk · Not a camera? / Add to report.
 */
@Composable
fun FindItContent(s: FindItState, onBack: () -> Unit, onNotACamera: () -> Unit, onToggleAdded: () -> Unit) {
    val device = s.device ?: return
    val c = MaterialTheme.colorScheme
    val near = stringResource(s.nearness.word)
    val distance = stringResource(s.nearness.distance)
    DetailScreen(
        onBack,
        title = stringResource(R.string.find_it),
        actions = { Tag(updatedText(s.updatedSecondsAgo), TagKind.KNOWN) },
        dock = {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.not_a_camera), onNotACamera, m) },
                primary = { m -> PrimaryButton(stringResource(if (device.added) R.string.remove_from_report else R.string.add_to_report), onToggleAdded, m) },
            )
        },
    ) {
        // Notes go first.
        if (s.lost) {
            Note(stringResource(R.string.find_lost_text), title = stringResource(R.string.find_lost_title), icon = painterResource(R.drawable.ic_warning), warning = true)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(Space.lensTop))
            LensProgress(signalLevel(s.rssi), near, distance, icon = { CheckIcon(device.check, Modifier.height(Size.iconCircle)) })
            Spacer(Modifier.height(Space.lensBottom))
            Text(device.title(), style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset).semantics { heading() })
            Text(
                stringResource(R.string.two_parts, stringResource(device.kind.label), stringResource(R.string.signal_dbm, s.rssi)),
                style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset),
            )
        }
        Spacer(Modifier.height(Space.block))
        Box24 {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.card), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(s.trend.heading), style = KeyholeType.cardTitle, color = c.onSurface, modifier = Modifier.weight(1f).semantics { heading() })
                when (s.trend) {
                    Trend.STRONGER -> Tag(stringResource(R.string.trend_stronger), TagKind.GOOD, icon = painterResource(R.drawable.ic_arrow_up))
                    Trend.WEAKER -> Tag(stringResource(R.string.trend_weaker), TagKind.KNOWN, icon = painterResource(R.drawable.ic_arrow_down))
                    Trend.STEADY -> Tag(stringResource(R.string.trend_steady), TagKind.KNOWN)
                }
            }
            Spacer(Modifier.height(Space.block))
            SegmentMeter(s.level, METER_SEGMENTS, stringResource(R.string.nearness_far), stringResource(R.string.nearness_very_close), description = stringResource(R.string.two_parts, near, distance))
        }
        Spacer(Modifier.height(Space.block))
        Note(stringResource(device.check.nearbyWords.findNote), icon = painterResource(R.drawable.ic_walking))
    }
}

private val Nearness.word: Int
    get() = when (this) {
        Nearness.VERY_CLOSE -> R.string.nearness_very_close
        Nearness.CLOSE -> R.string.nearness_close
        Nearness.NEAR -> R.string.nearness_near
        Nearness.FAR -> R.string.nearness_far
    }

private val Nearness.distance: Int
    get() = when (this) {
        Nearness.VERY_CLOSE -> R.string.nearness_very_close_line
        Nearness.CLOSE -> R.string.nearness_close_line
        Nearness.NEAR -> R.string.nearness_near_line
        Nearness.FAR -> R.string.nearness_far_line
    }

private val Trend.heading: Int
    get() = when (this) {
        Trend.STRONGER -> R.string.getting_warmer
        Trend.WEAKER -> R.string.getting_colder
        Trend.STEADY -> R.string.about_the_same
    }

@Preview
@Composable
private fun FindItPreview() = KeyholeTheme {
    FindItContent(FindItState(previewHotspots().devices[0], -48, Nearness.CLOSE, 4, Trend.STRONGER, 12, loaded = true), {}, {}, {})
}

@Preview
@Composable
private fun FindItLostDarkPreview() = KeyholeTheme(ThemeSettings(mode = ThemeMode.DARK)) {
    FindItContent(FindItState(previewBluetooth().devices[0], -88, Nearness.FAR, 1, Trend.WEAKER, 40, lost = true, loaded = true), {}, {}, {})
}
