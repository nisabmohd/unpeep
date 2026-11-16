package app.keyhole.privacy.ui.sweep

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.NearbySnapshot
import app.keyhole.privacy.domain.NearbyStatus
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.WifiSnapshot
import app.keyhole.privacy.domain.WifiStatus
import app.keyhole.privacy.domain.sweepSteps
import app.keyhole.privacy.scan.NearbySession
import app.keyhole.privacy.scan.WifiSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SweepState(
    val place: Place,
    /** The steps of this sweep, in the user's order; one step when a single check was started from Home. */
    val steps: List<Check> = emptyList(),
    /** The step on screen; null until the steps are known. */
    val current: Check? = null,
    /** True when one check was started by itself: no check row, nothing next. */
    val single: Boolean = false,
    val wifi: WifiSnapshot = WifiSnapshot(status = WifiStatus.RUNNING),
    /** The hotspot or Bluetooth step on screen; null on any other step. */
    val nearby: NearbySnapshot? = null,
    /** The step whose scan has just ended: its results open. */
    val done: Check? = null,
    /** The last step was skipped or left: the sweep is over. */
    val finished: Boolean = false,
)

/**
 * 03 Sweep in progress. The user started it (Home's Start, or one check); it runs each step once and never by
 * itself. [at]: the step to begin with, when the sweep comes back from a step's results. [only]: one check alone.
 */
class SweepViewModel(
    private val place: Place, private val at: Check?, private val only: Check?,
    private val settings: SettingsRepository, private val wifi: WifiSession, private val hotspots: NearbySession<*>, private val bluetooth: NearbySession<*>,
) : ViewModel() {
    private data class Inner(val steps: List<Check> = emptyList(), val current: Check? = null, val run: Int = -1, val finished: Boolean = false)

    private val inner = MutableStateFlow(Inner())

    val state: StateFlow<SweepState> = combine(inner, wifi.state, hotspots.state, bluetooth.state) { i, w, h, b ->
        // What is left of an earlier scan is not this one's result.
        val myWifi = if (i.current == Check.WIFI && w.run == i.run) w else WifiSnapshot(status = WifiStatus.RUNNING)
        val myNearby = when (i.current) {
            Check.HOTSPOTS -> h
            Check.BLUETOOTH -> b
            else -> null
        }?.let { if (it.run == i.run) it else NearbySnapshot(it.check, status = NearbyStatus.RUNNING) }
        val done = when {
            i.current == Check.WIFI && myWifi.status == WifiStatus.DONE -> Check.WIFI
            myNearby?.status == NearbyStatus.DONE -> i.current
            else -> null
        }
        SweepState(place, i.steps, i.current, only != null, myWifi, myNearby, done, i.finished)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SweepState(place, single = only != null))

    init {
        viewModelScope.launch {
            val steps = if (only != null) listOf(only) else settings.settings.first().let { sweepSteps(place, it.stepOrder, it.stepsOff) }
            val first = at?.takeIf { it in steps } ?: steps.firstOrNull()
            inner.value = Inner(steps, first, finished = first == null)
            begin(first)
        }
    }

    private fun nearby(step: Check?): NearbySession<*>? = when (step) {
        Check.HOTSPOTS -> hotspots
        Check.BLUETOOTH -> bluetooth
        else -> null
    }

    private fun begin(step: Check?) {
        when (step) {
            Check.WIFI -> inner.update { it.copy(run = wifi.start(viewModelScope)) }
            Check.HOTSPOTS, Check.BLUETOOTH -> nearby(step)?.let { session -> inner.update { it.copy(run = session.start(viewModelScope)) } }
            else -> Unit
        }
    }

    /** Skip step: leave this one unfinished and go on; after the last one the sweep is over. */
    fun skip() {
        val i = inner.value
        if (i.current == Check.WIFI) wifi.cancel()
        nearby(i.current)?.cancel()
        val next = i.steps.getOrNull(i.steps.indexOf(i.current) + 1)
        // run = -1: until the next step's scan has its own number, nothing an earlier scan left counts as its result.
        inner.value = i.copy(current = next, run = -1, finished = next == null)
        begin(next)
    }

    /** Scan again: from "Not on Wi-Fi", after Allow, or after the radio was switched on. */
    fun retry() = begin(inner.value.current)

    /**
     * Back on screen (from Android's permission question or Settings): if the step was waiting for a permission
     * or a radio and can run now, it starts. It never restarts a scan that is running or done.
     */
    fun resumed() {
        val waiting = state.value.nearby?.status.let { it == NearbyStatus.NEEDS_PERMISSION || it == NearbyStatus.UNAVAILABLE }
        if (waiting && nearby(inner.value.current)?.ready() == true) retry()
    }
}
