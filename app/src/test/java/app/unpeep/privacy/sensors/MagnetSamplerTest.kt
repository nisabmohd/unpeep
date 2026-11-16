package app.unpeep.privacy.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** One steady reading per window from the sensor's fast stream. */
class MagnetSamplerTest {
    @Test fun aWindowGivesTheAverageOfWhatArrivedInIt() {
        val s = MagnetSampler(400)
        assertNull(s.add(40f, 0))
        assertNull(s.add(50f, 100))
        assertNull(s.add(60f, 300))
        assertEquals(50f, s.add(50f, 400)!!, 0.001f)
    }

    @Test fun theNextWindowStartsAfresh() {
        val s = MagnetSampler(400)
        s.add(10f, 0)
        assertEquals(15f, s.add(20f, 400)!!, 0.001f)
        assertNull(s.add(90f, 500))
        assertEquals(90f, s.add(90f, 800)!!, 0.001f)
    }

    @Test fun aSlowSensorStillGivesOneReadingPerSample() {
        val s = MagnetSampler(400)
        assertNull(s.add(45f, 0))
        assertEquals(46f, s.add(47f, 1_000)!!, 0.001f)
        assertEquals(50f, s.add(50f, 2_000)!!, 0.001f)
    }

    @Test fun aClockThatJumpsBackDoesNotStallIt() {
        val s = MagnetSampler(400)
        s.add(45f, 10_000)
        assertNull(s.add(45f, 5_000))
        assertEquals(45f, s.add(45f, 5_400)!!, 0.001f)
    }

    @Test fun resetForgetsTheHalfWindow() {
        val s = MagnetSampler(400)
        s.add(200f, 0)
        s.reset()
        assertNull(s.add(40f, 1_000))
        assertEquals(40f, s.add(40f, 1_400)!!, 0.001f)
    }
}
