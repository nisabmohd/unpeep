package app.keyhole.privacy.ui.nav

import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Place
import kotlinx.serialization.Serializable

/* Type-safe routes (docs/SCREENS.md). The three tabs first, then what they push. */
@Serializable data object HomeRoute
@Serializable data object ReportsRoute
@Serializable data object MoreRoute

/** 01 Welcome (first run). */
@Serializable data object WelcomeRoute

/** 03 Sweep in progress: every step of [place], one after the other. */
@Serializable data class SweepRoute(val place: Place)

/** One check by itself (Home › Or run one check). */
@Serializable data class CheckRoute(val check: Check, val place: Place)

/** A More row's screen. */
@Serializable data class MoreItemRoute(val item: MoreItem)

enum class MoreItem { APPEARANCE, LANGUAGE, SWEEP_STEPS, MARKED_KNOWN, HOW_IT_WORKS, LIMITS, PRIVACY, ABOUT, LICENCES, FOUND_CAMERA }
