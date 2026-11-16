package app.unpeep.privacy.ui.magnet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.unpeep.privacy.R
import app.unpeep.privacy.data.MagnetNote
import app.unpeep.privacy.data.SettingsRepository
import app.unpeep.privacy.data.SweepNotes
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.domain.Place
import app.unpeep.privacy.domain.sweepSteps
import app.unpeep.privacy.sensors.Beeper
import app.unpeep.privacy.sensors.MagnetMeter
import app.unpeep.privacy.sensors.MagnetSampler
import app.unpeep.privacy.sensors.MagnetSensor
import app.unpeep.privacy.ui.components.BarMeter
import app.unpeep.privacy.ui.components.Box24
import app.unpeep.privacy.ui.components.Buzz
import app.unpeep.privacy.ui.components.ButtonPair
import app.unpeep.privacy.ui.components.CheckIcon
import app.unpeep.privacy.ui.components.DetailScreen
import app.unpeep.privacy.ui.components.EmptyState
import app.unpeep.privacy.ui.components.LensProgress
import app.unpeep.privacy.ui.components.Note
import app.unpeep.privacy.ui.components.NoteTone
import app.unpeep.privacy.ui.components.PrimaryButton
import app.unpeep.privacy.ui.components.RoundIconButton
import app.unpeep.privacy.ui.components.SecondaryButton
import app.unpeep.privacy.ui.components.Tag
import app.unpeep.privacy.ui.components.TagKind
import app.unpeep.privacy.ui.components.TopBarAction
import app.unpeep.privacy.ui.components.rememberHaptics
import app.unpeep.privacy.ui.theme.UnpeepTheme
import app.unpeep.privacy.ui.theme.UnpeepType
import app.unpeep.privacy.ui.theme.Size
import app.unpeep.privacy.ui.theme.Space
import app.unpeep.privacy.ui.theme.ThemeMode
import app.unpeep.privacy.ui.theme.ThemeSettings
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class MagnetState(
    /** False on a phone with no magnetometer. */
    val available: Boolean = true,
    val learning: Boolean = true,
    /** 0..1 while learning. */
    val learned: Float = 0f,
    /** µT away from normal. */
    val above: Int = 0,
    val normal: Int = 0,
    val now: Int = 0,
    val jumped: Boolean = false,
    /** The last readings as µT above normal, oldest first. */
    val bars: List<Int> = emptyList(),
    val beep: Boolean = false,
    /** This reading is in the report. */
    val added: Boolean = false,
    /** The sensor went quiet for a few seconds (E05): no reading is shown rather than an old one. */
    val stopped: Boolean = false,
    val sweep: Boolean = false,
    val next: Check? = null,
    /** Bumped on each new jump, for the one buzz per jump. */
    val jumps: Int = 0,
)

/**
 * 08 Magnet (docs/SCANNING.md § Magnet). It listens to the sensor only while the screen is on top (the reading flow
 * is collected with the screen and stops a few seconds after), learns the room's normal, then shows µT above it.
 * Never a score, never "camera".
 */
class MagnetViewModel(
    private val place: Place, private val sweep: Boolean, private val sensor: MagnetSensor, private val settings: SettingsRepository,
    private val notes: SweepNotes, private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    init {
        notes.begin(place)
    }

    private val meter = MagnetMeter()
    private val sampler = MagnetSampler()
    private val beeper = Beeper()
    private val latest = MutableStateFlow(MagnetState(available = sensor.available, sweep = sweep))
    private val added = MutableStateFlow(false)
    @Volatile private var beepOn = false

    @Volatile private var lastField = now()

    private val readings = sensor.fields().onEach { field ->
        lastField = now()
        if (latest.value.stopped) latest.update { it.copy(stopped = false) }
        val average = sampler.add(field, now()) ?: return@onEach
        val r = meter.add(average)
        val base = meter.normal
        val before = latest.value
        val jumpedNow = r.jumped && !before.jumped
        if (!r.learning && before.learning) notes.markRan(Check.MAGNET)
        if (jumpedNow && beepOn) beeper.beep()
        latest.value = before.copy(
            learning = r.learning, learned = r.learned, above = r.above, normal = base?.roundToInt() ?: 0, now = average.roundToInt(),
            jumped = r.jumped, bars = r.bars, jumps = before.jumps + if (jumpedNow) 1 else 0,
        )
    }.map { }.onStart { emit(Unit) }

    /** Once a second: has the sensor answered lately? */
    private val watchdog = kotlinx.coroutines.flow.flow {
        while (true) {
            kotlinx.coroutines.delay(WATCH_MS)
            if (sensor.available && now() - lastField > STALL_MS && !latest.value.stopped) latest.update { it.copy(stopped = true) }
            emit(Unit)
        }
    }.onStart { emit(Unit) }

    val state: StateFlow<MagnetState> = combine(latest, settings.settings, added, readings, watchdog) { l, s, a, _, _ ->
        beepOn = s.magnetBeep
        val steps = sweepSteps(place, s.stepOrder, s.stepsOff)
        l.copy(beep = s.magnetBeep, added = a, next = if (sweep) steps.getOrNull(steps.indexOf(Check.MAGNET) + 1) else null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), latest.value)

    /** "Set normal again": forget the room and learn it afresh, from where the phone is now. */
    fun setNormalAgain() {
        meter.reset()
        sampler.reset()
        added.value = false
        latest.update { MagnetState(available = it.available, sweep = sweep, jumps = it.jumps) }
    }

    fun toggleBeep() {
        viewModelScope.launch { settings.setMagnetBeep(!state.value.beep) }
    }

    /** Puts this reading in the report: how far above normal, and what normal was. */
    fun addToReport() {
        val s = state.value
        if (s.learning || s.added) return
        notes.addMagnet(MagnetNote(s.above, s.normal, now()))
        added.value = true
    }

    override fun onCleared() = beeper.release()

    companion object {
        /** The lens arc is full at this many µT above normal. */
        const val FULL = 50
    }
}

@Composable
fun MagnetScreen(vm: MagnetViewModel, onBack: () -> Unit, onNext: (Check?) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    // A jump is worth a buzz, once per jump; never a buzz while it stays up.
    LaunchedEffect(s.jumps) { if (s.jumps > 0) haptics.buzz(Buzz.FOUND) }
    MagnetContent(
        s, onBack, onNext = { onNext(s.next) },
        onBeep = { haptics.buzz(if (s.beep) Buzz.TOGGLE_OFF else Buzz.TOGGLE_ON); vm.toggleBeep() },
        onSetNormal = { haptics.buzz(Buzz.TICK); vm.setNormalAgain() },
        onAdd = { haptics.buzz(Buzz.CONFIRM); vm.addToReport() },
    )
}

/**
 * 08 Magnet: the lens with µT above normal · "Electronics nearby" or "About normal" with normal and now · the last
 * 10 seconds as bars · what a jump means · Set normal again / Add to report.
 */
@Composable
fun MagnetContent(s: MagnetState, onBack: () -> Unit, onNext: () -> Unit, onBeep: () -> Unit, onSetNormal: () -> Unit, onAdd: () -> Unit) {
    val c = MaterialTheme.colorScheme
    DetailScreen(
        onBack,
        title = stringResource(R.string.check_magnet),
        close = s.sweep,
        actions = {
            if (s.available) RoundIconButton(painterResource(R.drawable.ic_sound), stringResource(R.string.beep_on_jump), onBeep, active = s.beep)
            if (s.sweep) TopBarAction(stringResource(if (s.next == null) R.string.see_report else R.string.next), onNext)
        },
        dock = if (s.available) { {
            if (s.stopped) {
                // E05: nothing to add; setting normal again is the way back.
                ButtonPair(
                    secondary = { m -> SecondaryButton(stringResource(R.string.add_to_report), onAdd, m, enabled = false) },
                    primary = { m -> PrimaryButton(stringResource(R.string.set_normal_again), onSetNormal, m) },
                )
            } else {
                ButtonPair(
                    secondary = { m -> SecondaryButton(stringResource(R.string.set_normal_again), onSetNormal, m, enabled = !s.learning) },
                    primary = { m -> PrimaryButton(stringResource(if (s.added) R.string.added_to_report else R.string.add_to_report), onAdd, m, enabled = !s.learning && !s.added) },
                )
            }
        } } else null,
    ) {
        if (!s.available) {
            EmptyState(painterResource(R.drawable.ic_magnet), stringResource(R.string.no_magnet_title), stringResource(R.string.block_no_bluetooth_line), hue = app.unpeep.privacy.ui.theme.Hues.MAGNET)
            return@DetailScreen
        }
        if (s.stopped) {
            Note(stringResource(R.string.magnet_stopped_text), title = stringResource(R.string.magnet_stopped_title), tone = NoteTone.ERROR)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(Space.block))
                LensProgress(0f, stringResource(R.string.magnet_no_value), stringResource(R.string.magnet_no_reading_unit), icon = { CheckIcon(Check.MAGNET, Modifier.size(Size.checkCircleMin), circle = Size.checkCircleMin) })
                Spacer(Modifier.height(Space.lensBottom))
                Text(stringResource(R.string.magnet_no_reading_title), style = UnpeepType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.magnet_last_line, s.normal, s.now), style = UnpeepType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            return@DetailScreen
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(Space.block))
            val icon: @Composable () -> Unit = { CheckIcon(Check.MAGNET, Modifier.size(Size.checkCircleMin), circle = Size.checkCircleMin) }
            if (s.learning) {
                LensProgress(s.learned, stringResource(R.string.magnet_learning_value, (s.learned * PERCENT).roundToInt()), stringResource(R.string.magnet_learning_unit), icon = icon)
            } else {
                LensProgress(s.above.toFloat() / MagnetViewModel.FULL, stringResource(R.string.magnet_above_value, s.above), stringResource(R.string.magnet_above_line), icon = icon)
            }
            Spacer(Modifier.height(Space.card))
            val title = when {
                s.learning -> R.string.magnet_learning_title
                s.jumped -> R.string.magnet_jumped_title
                else -> R.string.magnet_normal_title
            }
            Text(stringResource(title), style = UnpeepType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset).semantics { heading() })
            Text(
                if (s.learning) stringResource(R.string.magnet_learning_line) else stringResource(R.string.magnet_now_line, s.normal, s.now),
                style = UnpeepType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.row).padding(horizontal = Space.headingInset),
            )
        }
        Spacer(Modifier.height(Space.lensBottom))
        Box24 {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.card), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.last_10_seconds), style = UnpeepType.cardTitle, color = c.onSurface, modifier = Modifier.weight(1f).semantics { heading() })
                when {
                    s.learning -> Unit
                    s.jumped -> Tag(stringResource(R.string.magnet_jumped), TagKind.WORTH_A_LOOK, icon = painterResource(R.drawable.ic_arrow_up))
                    else -> Tag(stringResource(R.string.trend_steady), TagKind.KNOWN)
                }
            }
            Spacer(Modifier.height(Space.block))
            // Two readings to a bar (the stronger): about a dozen bars still cover the last ten seconds.
            BarMeter(s.bars.chunked(2) { it.max() }, MagnetMeter.BARS / 2, MagnetMeter.JUMP, MagnetViewModel.FULL, stringResource(R.string.magnet_chart, s.above))
        }
        Spacer(Modifier.height(Space.block))
        Note(stringResource(R.string.magnet_note), tone = NoteTone.HONESTY, icon = painterResource(R.drawable.ic_info))
    }
}

private const val PERCENT = 100
private const val WATCH_MS = 1_000L
/** No reading for this long and the sensor counts as stopped. */
private const val STALL_MS = 4_000L

@Preview
@Composable
private fun MagnetPreview() = UnpeepTheme {
    MagnetContent(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(1, 1, 2, 1, 2, 3, 4, 12, 15, 13, 16, 18, 17)), {}, {}, {}, {}, {})
}

@Preview
@Composable
private fun MagnetLearningDarkPreview() = UnpeepTheme(ThemeSettings(mode = ThemeMode.DARK)) {
    MagnetContent(MagnetState(learned = 0.4f, sweep = true), {}, {}, {}, {}, {})
}
