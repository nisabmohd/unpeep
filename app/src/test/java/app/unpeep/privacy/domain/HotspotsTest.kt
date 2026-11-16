package app.unpeep.privacy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotspotsTest {
    private val sigs = listOf(
        HotspotSignature(Regex("^IPC[-_]", RegexOption.IGNORE_CASE), "Xiongmai"),
        HotspotSignature(Regex("^V380[-_]", RegexOption.IGNORE_CASE), "V380"),
        HotspotSignature(Regex("^SmartLife-\\w{4}", RegexOption.IGNORE_CASE), "Tuya", weak = true),
    )

    @Test fun aCameraNameIsALikelyCamera() {
        assertEquals(HotspotVerdict(Risk.LIKELY_CAMERA, "Xiongmai"), classifyHotspot("IPC-3F2A", sigs))
        assertEquals(Risk.LIKELY_CAMERA, classifyHotspot("v380-112233", sigs).risk)
    }

    @Test fun aWeakPatternIsOnlyWorthALook() = assertEquals(Risk.CHECK_THIS, classifyHotspot("SmartLife-9A2C", sigs).risk)

    @Test fun anOrdinaryNetworkIsUnknown() {
        assertEquals(HotspotVerdict(Risk.UNKNOWN), classifyHotspot("Sea View Guest", sigs))
        assertEquals(Risk.UNKNOWN, classifyHotspot("", sigs).risk)
        assertEquals(Risk.UNKNOWN, classifyHotspot("EPIC-IPC-1", sigs).risk)
    }

    @Test fun markedKnownStaysKnown() = assertEquals(Risk.KNOWN, classifyHotspot("IPC-3F2A", sigs, markedKnown = true).risk)

    @Test fun markingAnOrdinaryNetworkChangesNothing() = assertEquals(Risk.UNKNOWN, classifyHotspot("Home", sigs, markedKnown = true).risk)

    // --- Throttle: never more than 4 scans in 2 minutes ---------------------------------------------

    @Test fun fourScansThenWait() {
        val t = ScanThrottle()
        repeat(4) { assertTrue(t.tryAcquire(it * 1000L)) }
        assertFalse(t.tryAcquire(4000))
        assertFalse(t.tryAcquire(119_999))
        assertEquals(120_000L - 5000, t.waitMs(5000))
    }

    @Test fun theWindowSlides() {
        val t = ScanThrottle()
        repeat(4) { assertTrue(t.tryAcquire(it * 10_000L)) } // 0, 10 s, 20 s, 30 s
        assertTrue(t.tryAcquire(120_000)) // the first one has aged out
        assertFalse(t.tryAcquire(120_001))
        assertEquals(9_999L, t.waitMs(120_001))
        assertTrue(t.tryAcquire(130_000))
    }

    @Test fun aRefusedScanIsNotCounted() {
        val t = ScanThrottle()
        repeat(4) { t.tryAcquire(0) }
        repeat(10) { assertFalse(t.tryAcquire(1000)) }
        assertTrue(t.tryAcquire(120_000))
    }

    @Test fun noWaitWhenAScanIsAllowed() = assertEquals(0L, ScanThrottle().waitMs(0))

    // --- Signal ------------------------------------------------------------------------------------

    @Test fun signalLevelIsClamped() {
        assertEquals(0f, signalLevel(-100), 0f)
        assertEquals(0f, signalLevel(-90), 0f)
        assertEquals(1f, signalLevel(-35), 0f)
        assertEquals(1f, signalLevel(-10), 0f)
        assertTrue(signalLevel(-50) > signalLevel(-70))
    }

    @Test fun nearnessInWords() {
        assertEquals(Nearness.FAR, nearness(-88))
        assertEquals(Nearness.NEAR, nearness(-70))
        // One scale for the word, the pills and the signal (00-decisions): Right here from −50, Close to −62, Near to −74.
        assertEquals(Nearness.VERY_CLOSE, nearness(-50))
        assertEquals(Nearness.CLOSE, nearness(-62))
        assertEquals(Nearness.NEAR, nearness(-74))
        assertEquals(Nearness.FAR, nearness(-75))
        assertEquals(Nearness.CLOSE, nearness(-60))
        assertEquals(Nearness.VERY_CLOSE, nearness(-45))
    }
}
