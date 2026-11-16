package app.keyhole.privacy.optics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The lens and infrared finders: what counts as a possible lens, what doesn't, and where it shows on screen. */
class GlintsTest {
    /** A dark frame with bright squares drawn on it. */
    private class Frame(val w: Int, val h: Int, background: Int) {
        val px = IntArray(w * h) { background }
        fun box(x: Int, y: Int, bw: Int, bh: Int, v: Int) = apply { for (j in y until y + bh) for (i in x until x + bw) if (i in 0 until w && j in 0 until h) px[j * w + i] = v }
        val read: (Int, Int) -> Int = { x, y -> px[y * w + x] }
    }

    private val finder = GlintFinder(gridWidth = 160)

    @Test fun aSmallBrightDotInTheDarkIsASpot() {
        val f = Frame(160, 120, 20).box(80, 60, 2, 2, 255)
        val spots = finder.find(f.w, f.h, GlintTuning.LENS, f.read)
        assertEquals(1, spots.size)
        assertEquals(81f / 160, spots[0].x, 0.01f)
        assertEquals(61f / 120, spots[0].y, 0.01f)
        assertEquals(255, spots[0].brightness)
    }

    @Test fun aDarkFrameHasNoSpots() {
        val f = Frame(160, 120, 30)
        assertTrue(finder.find(f.w, f.h, GlintTuning.LENS, f.read).isEmpty())
    }

    @Test fun aWindowOrALampIsTooBigToBeALens() {
        val f = Frame(160, 120, 20).box(40, 30, 20, 20, 255)
        assertTrue(finder.find(f.w, f.h, GlintTuning.LENS, f.read).isEmpty())
    }

    @Test fun aLineOfLightIsNotRound() {
        val f = Frame(160, 120, 20).box(40, 30, 9, 1, 255)
        assertTrue(finder.find(f.w, f.h, GlintTuning.LENS, f.read).isEmpty())
    }

    @Test fun aDotOnABrightWallDoesNotStandOut() {
        // A lit room: everything is bright, so nothing is far above the average.
        val f = Frame(160, 120, 200).box(80, 60, 2, 2, 255)
        assertTrue(finder.find(f.w, f.h, GlintTuning.LENS, f.read).isEmpty())
    }

    @Test fun aSoftGlowWithBrightSurroundingsIsNotASpot() {
        val f = Frame(160, 120, 20).box(76, 56, 10, 10, 210).box(80, 60, 2, 2, 250)
        assertTrue(finder.find(f.w, f.h, GlintTuning.LENS, f.read).isEmpty())
    }

    @Test fun anInfraredDotIsDimmerThanAGlintButStillFound() {
        val f = Frame(160, 120, 15).box(30, 90, 3, 3, 200)
        assertTrue(finder.find(f.w, f.h, GlintTuning.LENS, f.read).isEmpty())
        assertEquals(1, finder.find(f.w, f.h, GlintTuning.INFRARED, f.read).size)
    }

    @Test fun severalSpotsComeBrightestFirstAndAreCapped() {
        val f = Frame(160, 120, 10)
        for (i in 0 until 10) f.box(8 + i * 15, 20, 2, 2, 240 + i)
        val spots = finder.find(f.w, f.h, GlintTuning.LENS, f.read)
        assertEquals(GlintFinder.MAX_SPOTS, spots.size)
        assertEquals(spots.sortedByDescending { it.brightness }, spots)
        assertEquals(249, spots.first().brightness)
    }

    @Test fun aBigFrameIsReadSparselyAndStillFindsTheDot() {
        val f = Frame(1280, 960, 20).box(640, 480, 16, 16, 255)
        var reads = 0
        val spots = finder.find(f.w, f.h, GlintTuning.LENS) { x, y -> reads++; f.px[y * f.w + x] }
        assertEquals(1, spots.size)
        assertTrue("reads $reads", reads <= 160 * 120)
    }

    @Test fun aDotAtTheEdgeOfTheFrameIsStillMeasured() {
        val f = Frame(160, 120, 20).box(0, 0, 2, 2, 255)
        assertEquals(1, finder.find(f.w, f.h, GlintTuning.LENS, f.read).size)
    }

    @Test fun nothingToReadIsNothingFound() {
        assertTrue(finder.find(0, 0, GlintTuning.LENS) { _, _ -> 0 }.isEmpty())
    }

    @Test fun theFinderCanBeReusedAcrossFrameSizes() {
        finder.find(1280, 960, GlintTuning.LENS) { _, _ -> 10 }
        val f = Frame(64, 48, 20).box(30, 20, 1, 1, 255)
        assertEquals(1, GlintFinder(gridWidth = 64).find(f.w, f.h, GlintTuning.LENS, f.read).size)
    }

    // --- Steady spots ---------------------------------------------------------------------------------

    private fun spot(x: Float, y: Float = 0.5f) = Spot(x, y, 0.01f, 250)

    @Test fun aSpotIsShownOnceItStays() {
        val t = SpotTracker(window = 5, need = 3)
        assertTrue(t.add(listOf(spot(0.5f))).isEmpty())
        assertTrue(t.add(listOf(spot(0.5f))).isEmpty())
        assertEquals(1, t.add(listOf(spot(0.51f))).size)
    }

    @Test fun aFlickerIsNeverShown() {
        val t = SpotTracker(window = 5, need = 3)
        for (i in 0 until 10) assertTrue(t.add(if (i % 3 == 0) listOf(spot(0.5f)) else emptyList()).isEmpty())
    }

    @Test fun aSpotThatGoesAwayIsDroppedAfterAWhile() {
        val t = SpotTracker(window = 5, need = 3)
        repeat(4) { t.add(listOf(spot(0.3f))) }
        assertEquals(1, t.add(emptyList()).size)
        repeat(5) { t.add(emptyList()) }
        assertTrue(t.add(emptyList()).isEmpty())
    }

    @Test fun aSpotFollowsThePhoneAsItMoves() {
        val t = SpotTracker(window = 5, need = 3, reach = 0.06f)
        var shown = emptyList<Spot>()
        for (i in 0 until 8) shown = t.add(listOf(spot(0.30f + i * 0.03f)))
        assertEquals(1, shown.size)
        assertTrue(shown[0].x > 0.4f)
    }

    @Test fun twoSpotsAreTrackedApart() {
        val t = SpotTracker()
        var shown = emptyList<Spot>()
        repeat(4) { shown = t.add(listOf(spot(0.2f), spot(0.8f))) }
        assertEquals(2, shown.size)
    }

    @Test fun aShownSpotDoesNotBlinkWhenAFrameMissesIt() {
        val t = SpotTracker(window = 8, need = 4)
        repeat(4) { t.add(listOf(spot(0.5f))) }
        assertEquals(1, t.add(listOf(spot(0.5f))).size)
        // Two frames without it, then back: it stayed on screen the whole time.
        assertEquals(1, t.add(emptyList()).size)
        assertEquals(1, t.add(emptyList()).size)
        assertEquals(1, t.add(listOf(spot(0.5f))).size)
    }

    @Test fun lookHereStaysOnTheFirstSpotWhenABrighterOneAppears() {
        val t = SpotTracker(window = 8, need = 4)
        repeat(5) { t.add(listOf(spot(0.2f).copy(brightness = 200))) }
        var shown = emptyList<app.keyhole.privacy.optics.Spot>()
        repeat(5) { shown = t.add(listOf(spot(0.2f).copy(brightness = 200), spot(0.8f).copy(brightness = 255))) }
        assertEquals(2, shown.size)
        assertTrue("the first spot keeps the label", shown[0].x < 0.5f)
        assertTrue("ids stay put", shown[0].id < shown[1].id)
    }

    @Test fun clearForgetsEverything() {
        val t = SpotTracker()
        repeat(4) { t.add(listOf(spot(0.2f))) }
        t.clear()
        assertTrue(t.add(listOf(spot(0.2f))).isEmpty())
    }

    // --- Where it shows -------------------------------------------------------------------------------

    @Test fun framesAreTurnedUprightForEachSensorOrientation() {
        assertEquals(0.2f to 0.3f, upright(0.2f, 0.3f, 0, false))
        assertEquals(0.7f to 0.2f, upright(0.2f, 0.3f, 90, false))
        assertEquals(0.8f to 0.7f, upright(0.2f, 0.3f, 180, false))
        assertEquals(0.3f to 0.8f, upright(0.2f, 0.3f, 270, false))
        assertEquals(0.7f to 0.2f, upright(0.2f, 0.3f, -270, false))
    }

    @Test fun theFrontCameraIsAMirror() {
        val (x, y) = upright(0.2f, 0.3f, 270, true)
        assertEquals(0.7f, x, 0.0001f)
        assertEquals(0.8f, y, 0.0001f)
    }

    @Test fun aWiderPictureIsCroppedAtItsSides() {
        val (sx, sy) = cropScale(4f, 3f, 9f, 16f)
        assertEquals(1f, sy, 0f)
        assertEquals((4f / 3f) / (9f / 16f), sx, 0.0001f)
    }

    @Test fun aTallerPictureIsCroppedTopAndBottom() {
        val (sx, sy) = cropScale(3f, 4f, 16f, 9f)
        assertEquals(1f, sx, 0f)
        assertTrue(sy > 1f)
    }

    @Test fun aPictureOfTheViewsShapeIsNotCroppedAndNothingIsNeverStretched() {
        assertEquals(1f to 1f, cropScale(3f, 4f, 300f, 400f))
        assertEquals(1f to 1f, cropScale(0f, 4f, 300f, 400f))
    }

    @Test fun theCentreStaysTheCentreAndEdgesFallOffScreenWhenCropped() {
        val scale = cropScale(4f, 3f, 9f, 16f)
        val (cx, cy) = toView(0.5f, 0.5f, scale)
        assertTrue(near(cx, 0.5f) && near(cy, 0.5f))
        val (ex, _) = toView(0.02f, 0.5f, scale)
        assertTrue(!onScreen(ex, 0.5f))
        assertTrue(onScreen(0f, 1f))
    }
}
