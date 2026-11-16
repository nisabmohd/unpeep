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

/** 03 Sweep in progress: every step of [place], one after the other, beginning with [at] (null = the first). */
@Serializable data class SweepRoute(val place: Place, val at: Check? = null)

/** 04 Wi-Fi results. [sweep]: part of a sweep (then "Next" goes on), or the check run alone. */
@Serializable data class WifiResultsRoute(val place: Place, val sweep: Boolean)

/** The known and unrecognised devices of the last Wi-Fi scan. */
@Serializable data class WifiOthersRoute(val place: Place)

/** 05 Device: one device of the last Wi-Fi scan, by its address. */
@Serializable data class DeviceRoute(val ip: String, val place: Place)

/** One check by itself (Home › Or run one check). */
@Serializable data class CheckRoute(val check: Check, val place: Place)

/** A More row's screen. */
@Serializable data class MoreItemRoute(val item: MoreItem)

enum class MoreItem { APPEARANCE, LANGUAGE, SWEEP_STEPS, MARKED_KNOWN, HOW_IT_WORKS, LIMITS, PRIVACY, ABOUT, LICENCES, FOUND_CAMERA }

/** Hotspot or Bluetooth results. [sweep]: part of a sweep (then "Next" goes on), or the check run alone. */
@Serializable data class NearbyResultsRoute(val check: Check, val place: Place, val sweep: Boolean)

/** The plain networks or Bluetooth devices of the last scan. */
@Serializable data class NearbyOthersRoute(val check: Check)

/** 07 Find it: one hotspot or Bluetooth device of the last scan, by what Android calls it. */
@Serializable data class FindItRoute(val check: Check, val id: String)
