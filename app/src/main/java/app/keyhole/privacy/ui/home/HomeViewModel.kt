package app.keyhole.privacy.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.sweepSteps
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HomeState(
    val place: Place = Place.STAY,
    /** The sweep's steps for this place, in the user's order. */
    val steps: List<Check> = Place.STAY.checks(),
    /** Every check this place has, for "Or run one check" (a step switched off can still be run by itself). */
    val checks: List<Check> = Place.STAY.checks(),
    val minutes: Int = Place.STAY.minutes(),
    val loaded: Boolean = false,
    val lastReport: app.keyhole.privacy.data.SavedReport? = null,
    /** Checks done in this visit that aren't in a report yet. */
    val visitChecks: Int = 0,
)

class HomeViewModel(private val settings: SettingsRepository, reports: app.keyhole.privacy.data.ReportRepository, notes: app.keyhole.privacy.data.SweepNotes) : ViewModel() {
    val state: StateFlow<HomeState> = kotlinx.coroutines.flow.combine(settings.settings, reports.all, notes.ran, notes.checklist) { s, all, ran, answers ->
        val steps = sweepSteps(s.place, s.stepOrder, s.stepsOff)
        val visit = (ran + if (answers.isNotEmpty()) setOf(Check.CHECKLIST) else emptySet()).size
        HomeState(s.place, steps, s.place.checks(), s.place.minutes(steps), loaded = true, lastReport = all.firstOrNull(), visitChecks = visit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun pick(place: Place) { viewModelScope.launch { settings.setPlace(place) } }
}
