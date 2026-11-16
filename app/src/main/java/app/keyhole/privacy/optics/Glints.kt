package app.keyhole.privacy.optics

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/*
 * The lens and infrared finders' eyes (docs/SCANNING.md § Lens, § Infrared), with no Android in them: a frame's
 * brightness (the Y plane, downscaled) in, small bright round spots out. Pure, so every rule has a unit test.
 * They ring *possible* lenses; the screen always says to look closely.
 */

/** One possible lens or infrared light: its centre and radius as a share of the frame (0..1), and how bright it is. */
data class Spot(val x: Float, val y: Float, val radius: Float, val brightness: Int)

/**
 * What counts as a spot. [minLuma]: never dimmer than this (0..255). [aboveMean]: and this much brighter than the
 * frame on average, so a lit room doesn't ring every lamp. [maxAreaFraction]: a lens is small; a window isn't.
 * [minFill]: round, not a line. [ringContrast]: much brighter than what is just around it.
 */
data class GlintTuning(val minLuma: Int, val aboveMean: Int, val maxAreaFraction: Float, val minFill: Float, val ringContrast: Int) {
    companion object {
        /** A lens shines the torch back as a tiny, very bright, sharp dot. */
        val LENS = GlintTuning(minLuma = 235, aboveMean = 90, maxAreaFraction = 0.004f, minFill = 0.4f, ringContrast = 60)
        /** An infrared LED in the dark: a softer, purple or white dot on a dark frame. */
        val INFRARED = GlintTuning(minLuma = 180, aboveMean = 70, maxAreaFraction = 0.006f, minFill = 0.35f, ringContrast = 50)
    }
}

/**
 * Finds the spots of one frame. It reads only every [step]th pixel of every [step]th row, so a frame costs a few
 * thousand reads whatever the camera's size. Not thread-safe: one finder per analyzer thread.
 */
class GlintFinder(private val gridWidth: Int = GRID_WIDTH) {
    private var grid = IntArray(0)
    private var seen = BooleanArray(0)
    private var stack = IntArray(0)

    /**
     * [read] gives the brightness (0..255) at a pixel of a [width] × [height] frame. Spots come back brightest
     * first, at most [MAX_SPOTS].
     */
    fun find(width: Int, height: Int, tuning: GlintTuning, read: (x: Int, y: Int) -> Int): List<Spot> {
        if (width <= 0 || height <= 0) return emptyList()
        val step = max(1, width / gridWidth)
        val gw = max(1, width / step)
        val gh = max(1, height / step)
        val n = gw * gh
        if (grid.size < n) {
            grid = IntArray(n)
            seen = BooleanArray(n)
            stack = IntArray(n)
        }
        var total = 0L
        for (y in 0 until gh) for (x in 0 until gw) {
            val v = read(x * step, y * step).coerceIn(0, 255)
            grid[y * gw + x] = v
            total += v
        }
        java.util.Arrays.fill(seen, 0, n, false)
        val mean = (total / n).toInt()
        val threshold = max(tuning.minLuma, mean + tuning.aboveMean)
        if (threshold > 255) return emptyList()
        val maxArea = max(1, (tuning.maxAreaFraction * n).toInt())

        val spots = ArrayList<Spot>()
        for (start in 0 until n) {
            if (seen[start] || grid[start] < threshold) continue
            // Flood fill one bright patch (4 neighbours).
            var top = 0
            stack[top++] = start
            seen[start] = true
            var area = 0
            var minX = gw; var maxX = -1; var minY = gh; var maxY = -1
            var sumX = 0L; var sumY = 0L; var peak = 0
            while (top > 0) {
                val i = stack[--top]
                val x = i % gw
                val y = i / gw
                area++
                sumX += x; sumY += y
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (grid[i] > peak) peak = grid[i]
                if (x > 0) top = push(i - 1, threshold, top)
                if (x < gw - 1) top = push(i + 1, threshold, top)
                if (y > 0) top = push(i - gw, threshold, top)
                if (y < gh - 1) top = push(i + gw, threshold, top)
            }
            if (area > maxArea) continue
            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            // Round, not a line or a strip of light: square-ish and mostly filled.
            if (area >= SHAPE_FROM && (max(bw, bh).toFloat() / min(bw, bh) > MAX_ASPECT || area.toFloat() / (bw * bh) < tuning.minFill)) continue
            if (peak - ringAverage(gw, gh, minX, maxX, minY, maxY) < tuning.ringContrast) continue
            val cx = (sumX.toFloat() / area + 0.5f) / gw
            val cy = (sumY.toFloat() / area + 0.5f) / gh
            spots += Spot(cx, cy, max(bw, bh) / 2f / gw, peak)
        }
        return spots.sortedWith(compareByDescending<Spot> { it.brightness }.thenByDescending { it.radius }).take(MAX_SPOTS)
    }

    private fun push(i: Int, threshold: Int, top: Int): Int {
        if (seen[i] || grid[i] < threshold) return top
        seen[i] = true
        stack[top] = i
        return top + 1
    }

    /** The average brightness on a ring just outside the patch's box: what the patch stands out from. */
    private fun ringAverage(gw: Int, gh: Int, minX: Int, maxX: Int, minY: Int, maxY: Int): Int {
        val margin = max(RING_MARGIN, max(maxX - minX, maxY - minY) + 1)
        val left = minX - margin; val right = maxX + margin
        val top = minY - margin; val bottom = maxY + margin
        var sum = 0L
        var count = 0
        fun add(x: Int, y: Int) {
            if (x in 0 until gw && y in 0 until gh) {
                sum += grid[y * gw + x]
                count++
            }
        }
        for (x in left..right) { add(x, top); add(x, bottom) }
        for (y in top + 1 until bottom) { add(left, y); add(right, y) }
        // A patch filling the frame to its edges has nothing around it: it doesn't stand out.
        return if (count == 0) 255 else (sum / count).toInt()
    }

    companion object {
        /** About 160 × 120 samples, whatever the camera gives. */
        const val GRID_WIDTH = 160
        const val MAX_SPOTS = 6
        private const val SHAPE_FROM = 4
        private const val MAX_ASPECT = 2f
        private const val RING_MARGIN = 2
    }
}

/**
 * Keeps the spots that stay: a glint from a lens is there frame after frame, a flicker or a passing reflection is
 * not. A spot is shown once it was seen in [need] of the last [window] frames, near the same place.
 */
class SpotTracker(private val window: Int = WINDOW, private val need: Int = NEED, private val reach: Float = REACH) {
    private class Track(var x: Float, var y: Float, var radius: Float, var brightness: Int, var hits: Int)

    private val tracks = ArrayList<Track>()

    /** Adds one frame's spots; returns the steady ones, brightest first. */
    fun add(spots: List<Spot>): List<Spot> {
        val mask = (1 shl window) - 1
        val unmatched = spots.toMutableList()
        for (t in tracks) {
            val nearest = unmatched.minByOrNull { hypot(it.x - t.x, it.y - t.y) }
            t.hits = (t.hits shl 1) and mask
            if (nearest != null && hypot(nearest.x - t.x, nearest.y - t.y) <= reach) {
                unmatched -= nearest
                // Follow it smoothly as the phone moves.
                t.x += (nearest.x - t.x) * FOLLOW
                t.y += (nearest.y - t.y) * FOLLOW
                t.radius = nearest.radius
                t.brightness = nearest.brightness
                t.hits = t.hits or 1
            }
        }
        tracks.removeAll { it.hits == 0 }
        unmatched.forEach { tracks += Track(it.x, it.y, it.radius, it.brightness, 1) }
        return tracks.filter { Integer.bitCount(it.hits) >= need }
            .sortedByDescending { it.brightness }
            .take(GlintFinder.MAX_SPOTS)
            .map { Spot(it.x, it.y, it.radius, it.brightness) }
    }

    fun clear() = tracks.clear()

    companion object {
        const val WINDOW = 5
        const val NEED = 3
        const val REACH = 0.06f
        private const val FOLLOW = 0.5f
    }
}

/**
 * Where a point of a camera frame shows on screen. Frames come in the sensor's own orientation; [rotation] (0, 90,
 * 180, 270, clockwise) turns them upright, and the front camera's preview is a mirror, so its points flip too.
 */
fun upright(x: Float, y: Float, rotation: Int, mirror: Boolean): Pair<Float, Float> {
    val (ux, uy) = when (((rotation % 360) + 360) % 360) {
        90 -> (1f - y) to x
        180 -> (1f - x) to (1f - y)
        270 -> y to (1f - x)
        else -> x to y
    }
    return (if (mirror) 1f - ux else ux) to uy
}

/**
 * How much a picture of [contentWidth] × [contentHeight] is stretched on each axis to fill a [viewWidth] ×
 * [viewHeight] view without bars (centre crop), when it is first stretched to the view's size: one of the two is
 * 1, the other ≥ 1.
 */
fun cropScale(contentWidth: Float, contentHeight: Float, viewWidth: Float, viewHeight: Float): Pair<Float, Float> {
    if (contentWidth <= 0f || contentHeight <= 0f || viewWidth <= 0f || viewHeight <= 0f) return 1f to 1f
    val content = contentWidth / contentHeight
    val view = viewWidth / viewHeight
    return if (content > view) (content / view) to 1f else 1f to (view / content)
}

/** A point of the upright frame (0..1) as a point of the view (0..1), after [cropScale]. Off the view = outside 0..1. */
fun toView(x: Float, y: Float, scale: Pair<Float, Float>): Pair<Float, Float> = (0.5f + (x - 0.5f) * scale.first) to (0.5f + (y - 0.5f) * scale.second)

/** True when a point of the view is on screen. */
fun onScreen(x: Float, y: Float): Boolean = x in 0f..1f && y in 0f..1f

/** The difference between two angles, for tests and drift checks. */
internal fun near(a: Float, b: Float, by: Float = 0.001f) = abs(a - b) <= by
