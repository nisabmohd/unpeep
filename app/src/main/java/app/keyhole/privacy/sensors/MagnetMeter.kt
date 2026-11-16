package app.keyhole.privacy.sensors

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The magnet check's logic, with no Android in it (docs/SCANNING.md § Magnet): the median of the first
 * [LEARN] readings is the room's "normal"; a reading [JUMP] µT or more away from it has "jumped", which means
 * electronics, a magnet or wiring nearby. Never a score, never "camera".
 */
class MagnetMeter {
    private val learning = ArrayList<Float>(LEARN)
    private val recent = ArrayDeque<Float>()

    /** The room's normal field in µT; null while still learning. */
    var normal: Float? = null
        private set

    data class Reading(
        val learning: Boolean,
        /** How far along learning is, 0..1. */
        val learned: Float,
        /** µT away from normal (never negative); 0 while learning. */
        val above: Int,
        val jumped: Boolean,
        /** The last readings as µT above normal, oldest first: the bars. */
        val bars: List<Int>,
    )

    /** One sensor event: the field's three axes in µT. */
    fun add(x: Float, y: Float, z: Float): Reading = add(sqrt(x * x + y * y + z * z))

    fun add(magnitude: Float): Reading {
        val base = normal
        if (base == null) {
            learning += magnitude
            if (learning.size >= LEARN) normal = learning.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2f }
            return Reading(learning = normal == null, learned = learning.size / LEARN.toFloat(), above = 0, jumped = false, bars = emptyList())
        }
        // A light smoothing over the last few readings, so one noisy sample doesn't flash "jumped".
        recent.addLast(magnitude)
        if (recent.size > SMOOTH) recent.removeFirst()
        val above = abs(recent.average().toFloat() - base).roundToInt()
        bars.addLast(above)
        if (bars.size > BARS) bars.removeFirst()
        return Reading(learning = false, learned = 1f, above = above, jumped = above >= JUMP, bars = bars.toList())
    }

    private val bars = ArrayDeque<Int>()

    /** "Set normal again": forget everything and learn the room afresh. */
    fun reset() {
        learning.clear(); recent.clear(); bars.clear(); normal = null
    }

    companion object {
        const val LEARN = 25
        const val JUMP = 10
        const val SMOOTH = 3
        /** Bars kept: about ten seconds at one bar per reading shown. */
        const val BARS = 24
    }
}
