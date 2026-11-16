package app.keyhole.privacy.ui.sweep

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.WifiSnapshot
import app.keyhole.privacy.domain.WifiStatus
import app.keyhole.privacy.domain.sweepSteps
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
    /** The last step was skipped or left: the sweep is over. */
    val finished: Boolean = false,
)

/**
 * 03 Sweep in progress. The user started it (Home's Start, or one check); it runs each step once and never by
 * itself. [at]: the step to begin with, when the sweep comes back from a step's results. [only]: one check alone.
 */
class SweepViewModel(private val place: Place, private val at: Check?, private val only: Check?, private val settings: SettingsRepository, private val wifi: WifiSession) : ViewModel() {
    private data class Inner(val steps: List<Check> = emptyList(), val current: Check? = null, val run: Int = -1, val finished: Boolean = false)

    private val inner = MutableStateFlow(Inner())

    val state: StateFlow<SweepState> = combine(inner, wifi.state) { i, w ->
        // What is left of an earlier scan is not this one's result.
        val mine = if (w.run == i.run) w else WifiSnapshot(status = WifiStatus.RUNNING)
        SweepState(place, i.steps, i.current, only != null, mine, i.finished)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SweepState(place, single = only != null))

    init {
        viewModelScope.launch {
            val steps = if (only != null) listOf(only) else settings.settings.first().let { sweepSteps(place, it.stepOrder, it.stepsOff) }
            val first = at?.takeIf { it in steps } ?: steps.firstOrNull()
            inner.value = Inner(steps, first, finished = first == null)
            begin(first)
        }
    }

    private fun begin(step: Check?) {
        if (step == Check.WIFI) inner.update { it.copy(run = wifi.start(viewModelScope)) }
    }

    /** Skip step: leave this one unfinished and go on; after the last one the sweep is over. */
    fun skip() {
        val i = inner.value
        if (i.current == Check.WIFI) wifi.cancel()
        val next = i.steps.getOrNull(i.steps.indexOf(i.current) + 1)
        inner.value = i.copy(current = next, finished = next == null)
        begin(next)
    }

    /** Scan again, from "Not on Wi-Fi". */
    fun retry() = begin(inner.value.current)
}
