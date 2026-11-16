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
)

class HomeViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<HomeState> = settings.settings.map { s ->
        val steps = sweepSteps(s.place, s.stepOrder, s.stepsOff)
        HomeState(s.place, steps, s.place.checks(), s.place.minutes(steps), loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun pick(place: Place) { viewModelScope.launch { settings.setPlace(place) } }
}
