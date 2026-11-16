package app.unpeep.privacy.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagnetMeterTest {
    private fun learned(normal: Float = 45f) = MagnetMeter().also { m -> repeat(MagnetMeter.LEARN) { m.add(normal) } }

    @Test fun learnsNormalFromTheFirstReadings() {
        val m = MagnetMeter()
        repeat(MagnetMeter.LEARN - 1) {
            val r = m.add(45f)
            assertTrue(r.learning)
            assertEquals(0, r.above)
            assertFalse(r.jumped)
        }
        assertNull(m.normal)
        val last = m.add(45f)
        assertFalse(last.learning)
        assertEquals(1f, last.learned, 0f)
        assertEquals(45f, m.normal!!, 0.001f)
    }

    @Test fun learningProgressGrows() {
        val m = MagnetMeter()
        val first = m.add(40f).learned
        val second = m.add(40f).learned
        assertTrue(second > first)
        assertEquals(2f / MagnetMeter.LEARN, second, 0.0001f)
    }

    @Test fun normalIsTheMedianSoOneSpikeDoesNotMoveIt() {
        val m = MagnetMeter()
        repeat(MagnetMeter.LEARN - 1) { m.add(45f) }
        m.add(400f)
        assertEquals(45f, m.normal!!, 0.001f)
    }

    @Test fun quietRoomDoesNotJump() {
        val m = learned()
        repeat(20) { i ->
            val r = m.add(45f + (i % 3) - 1)
            assertFalse(r.jumped)
            assertTrue(r.above <= 1)
        }
    }

    @Test fun aSteadyRiseOfTenJumps() {
        val m = learned()
        var r = m.add(59f)
        repeat(MagnetMeter.SMOOTH) { r = m.add(59f) }
        assertEquals(14, r.above)
        assertTrue(r.jumped)
    }

    @Test fun justUnderTheLineIsNotAJump() {
        val m = learned()
        var r = m.add(54f)
        repeat(MagnetMeter.SMOOTH) { r = m.add(54f) }
        assertEquals(9, r.above)
        assertFalse(r.jumped)
    }

    @Test fun aDropJumpsTooAndAboveIsNeverNegative() {
        val m = learned(60f)
        var r = m.add(40f)
        repeat(MagnetMeter.SMOOTH) { r = m.add(40f) }
        assertEquals(20, r.above)
        assertTrue(r.jumped)
    }

    @Test fun oneNoisySampleIsSmoothed() {
        val m = learned()
        m.add(45f); m.add(45f)
        val r = m.add(60f) // (45 + 45 + 60) / 3 = 50 → +5
        assertEquals(5, r.above)
        assertFalse(r.jumped)
    }

    @Test fun barsKeepOnlyTheLastOnes() {
        val m = learned()
        var r = m.add(45f)
        repeat(MagnetMeter.BARS * 2) { r = m.add(45f) }
        assertEquals(MagnetMeter.BARS, r.bars.size)
    }

    @Test fun resetLearnsAgain() {
        val m = learned()
        m.add(80f)
        m.reset()
        assertNull(m.normal)
        assertTrue(m.add(80f).learning)
        repeat(MagnetMeter.LEARN - 1) { m.add(80f) }
        assertNotNull(m.normal)
        assertEquals(0, m.add(80f).above)
    }

    @Test fun axesBecomeAMagnitude() {
        val m = MagnetMeter()
        repeat(MagnetMeter.LEARN) { m.add(3f, 4f, 0f) }
        assertEquals(5f, m.normal!!, 0.001f)
    }
}
