package app.keyhole.privacy.ui.magnet

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
import app.keyhole.privacy.R
import app.keyhole.privacy.data.MagnetNote
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.data.SweepNotes
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.sweepSteps
import app.keyhole.privacy.sensors.Beeper
import app.keyhole.privacy.sensors.MagnetMeter
import app.keyhole.privacy.sensors.MagnetSampler
import app.keyhole.privacy.sensors.MagnetSensor
import app.keyhole.privacy.ui.components.BarMeter
import app.keyhole.privacy.ui.components.Box24
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.CheckIcon
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.EmptyState
import app.keyhole.privacy.ui.components.LensProgress
import app.keyhole.privacy.ui.components.Note
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.RoundIconButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.Tag
import app.keyhole.privacy.ui.components.TagKind
import app.keyhole.privacy.ui.components.TopBarAction
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.ThemeMode
import app.keyhole.privacy.ui.theme.ThemeSettings
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
    private val meter = MagnetMeter()
    private val sampler = MagnetSampler()
    private val beeper = Beeper()
    private val latest = MutableStateFlow(MagnetState(available = sensor.available, sweep = sweep))
    private val added = MutableStateFlow(false)
    @Volatile private var beepOn = false

    private val readings = sensor.fields().onEach { field ->
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

    val state: StateFlow<MagnetState> = combine(latest, settings.settings, added, readings) { l, s, a, _ ->
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
            if (s.sweep) TopBarAction(stringResource(R.string.next), onNext)
        },
        dock = if (s.available) { {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.set_normal_again), onSetNormal, m, enabled = !s.learning) },
                primary = { m -> PrimaryButton(stringResource(if (s.added) R.string.added_to_report else R.string.add_to_report), onAdd, m, enabled = !s.learning && !s.added) },
            )
        } } else null,
    ) {
        if (!s.available) {
            EmptyState(painterResource(R.drawable.ic_magnet), stringResource(R.string.no_magnet_title), stringResource(R.string.block_no_bluetooth_line))
            return@DetailScreen
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(Space.lensTop))
            val icon: @Composable () -> Unit = { CheckIcon(Check.MAGNET, Modifier.height(Size.iconCircle)) }
            if (s.learning) {
                LensProgress(s.learned, stringResource(R.string.magnet_learning_value, (s.learned * PERCENT).roundToInt()), stringResource(R.string.magnet_learning_unit), icon = icon)
            } else {
                LensProgress(s.above.toFloat() / MagnetViewModel.FULL, stringResource(R.string.magnet_above_value, s.above), stringResource(R.string.magnet_above_line), icon = icon)
            }
            Spacer(Modifier.height(Space.lensBottom))
            val title = when {
                s.learning -> R.string.magnet_learning_title
                s.jumped -> R.string.magnet_jumped_title
                else -> R.string.magnet_normal_title
            }
            Text(stringResource(title), style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset).semantics { heading() })
            Text(
                if (s.learning) stringResource(R.string.magnet_learning_line) else stringResource(R.string.magnet_now_line, s.normal, s.now),
                style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset),
            )
        }
        Spacer(Modifier.height(Space.block))
        Box24 {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.card), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.last_10_seconds), style = KeyholeType.cardTitle, color = c.onSurface, modifier = Modifier.weight(1f).semantics { heading() })
                when {
                    s.learning -> Unit
                    s.jumped -> Tag(stringResource(R.string.magnet_jumped), TagKind.WORTH_A_LOOK, icon = painterResource(R.drawable.ic_arrow_up))
                    else -> Tag(stringResource(R.string.trend_steady), TagKind.KNOWN)
                }
            }
            Spacer(Modifier.height(Space.block))
            BarMeter(s.bars, MagnetMeter.BARS, MagnetMeter.JUMP, MagnetViewModel.FULL, stringResource(R.string.magnet_chart, s.above))
        }
        Spacer(Modifier.height(Space.block))
        Note(stringResource(R.string.magnet_note))
    }
}

private const val PERCENT = 100

@Preview
@Composable
private fun MagnetPreview() = KeyholeTheme {
    MagnetContent(MagnetState(learning = false, learned = 1f, above = 14, normal = 48, now = 62, jumped = true, bars = listOf(1, 1, 2, 1, 2, 3, 4, 12, 15, 13, 16, 18, 17)), {}, {}, {}, {}, {})
}

@Preview
@Composable
private fun MagnetLearningDarkPreview() = KeyholeTheme(ThemeSettings(mode = ThemeMode.DARK)) {
    MagnetContent(MagnetState(learned = 0.4f, sweep = true), {}, {}, {}, {}, {})
}
