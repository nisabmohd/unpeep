package app.keyhole.privacy.domain

/** The seven checks, in the order a sweep runs them (docs/SCANNING.md). */
enum class Check { WIFI, HOTSPOTS, BLUETOOTH, LENS, INFRARED, MAGNET, CHECKLIST }

/** Where the room is. Each place has its own checks and checklist (docs/PLAN.md § 2.6). */
enum class Place {
    STAY, TRIAL_ROOM, WASHROOM, OFFICE, CAR;

    /**
     * The checks that make sense here, in sweep order. A trial room, a washroom and a car are not places where
     * you join the Wi-Fi, so the Wi-Fi scan is left out and the sweep is the short version.
     */
    fun checks(): List<Check> = when (this) {
        STAY, OFFICE -> Check.entries
        TRIAL_ROOM, WASHROOM, CAR -> Check.entries - Check.WIFI
    }

    /** About how long the sweep takes, in minutes (shown on Home). */
    fun minutes(enabled: Collection<Check> = checks()): Int =
        enabled.sumOf { SECONDS.getValue(it) }.let { (it + 59) / 60 }.coerceAtLeast(1)

    private companion object {
        val SECONDS = mapOf(
            Check.WIFI to 25, Check.HOTSPOTS to 15, Check.BLUETOOTH to 15, Check.LENS to 60,
            Check.INFRARED to 45, Check.MAGNET to 40, Check.CHECKLIST to 90,
        )
    }
}

/**
 * The steps of a sweep: the place's checks, in the user's order ([order], More › Sweep steps), without the ones
 * switched off ([off]). A check the order doesn't list keeps its usual place at the end.
 */
fun sweepSteps(place: Place, order: List<Check> = Check.entries, off: Set<Check> = emptySet()): List<Check> {
    val all = order.distinct() + (Check.entries - order.toSet())
    val allowed = place.checks().toSet()
    return all.filter { it in allowed && it !in off }
}
