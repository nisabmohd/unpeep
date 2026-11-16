package app.keyhole.privacy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleTest {
    private val s = BleSignatures(
        trackers = listOf(
            TrackerSignature("AirTag or Find My tag", company = 76, type = 18),
            TrackerSignature("Tile", services = listOf("FEED", "FEEC")),
        ),
        cameras = listOf(Signatures.CameraSignature("V380", listOf("v380"))),
    )
    private fun ad(name: String? = null, services: Set<String> = emptySet(), m: Map<Int, ByteArray> = emptyMap()) = BleAdvert("AA:BB:CC:DD:EE:FF", name, -60, services, m)

    @Test fun aFindMyFrameIsATracker() {
        val v = classifyBle(ad(m = mapOf(76 to byteArrayOf(0x12, 0x19, 0x10))), s)
        assertEquals(BleKind.TRACKER, v.kind)
        assertEquals(Risk.CHECK_THIS, v.risk)
        assertEquals("AirTag or Find My tag", v.label)
    }

    @Test fun otherAppleDataIsNotATracker() {
        // 0x10 = Nearby Info (every iPhone), 0x07 = AirPods: not Find My.
        assertEquals(BleKind.OTHER, classifyBle(ad(m = mapOf(76 to byteArrayOf(0x10, 0x05))), s).kind)
        assertEquals(BleKind.OTHER, classifyBle(ad(m = mapOf(76 to byteArrayOf())), s).kind)
    }

    @Test fun aTileIsFoundByItsServiceNotItsName() {
        assertEquals("Tile", classifyBle(ad(name = "anything", services = setOf("FEED")), s).label)
        assertEquals(BleKind.OTHER, classifyBle(ad(name = "Tile"), s).kind)
    }

    @Test fun aCameraNameIsOnlyAHint() {
        val v = classifyBle(ad(name = "V380-1234"), s)
        assertEquals(BleKind.CAMERA, v.kind)
        assertEquals(Risk.CHECK_THIS, v.risk)
    }

    @Test fun anythingElseIsUnknown() {
        assertEquals(BleVerdict(BleKind.OTHER, Risk.UNKNOWN), classifyBle(ad(name = "Mi Band 7"), s))
        assertEquals(Risk.UNKNOWN, classifyBle(ad(), s).risk)
    }

    @Test fun markedKnownIsKnown() {
        assertEquals(Risk.KNOWN, classifyBle(ad(services = setOf("FEEC")), s, markedKnown = true).risk)
        assertEquals(Risk.KNOWN, classifyBle(ad(name = "v380 cam"), s, markedKnown = true).risk)
    }

    @Test fun shortUuids() {
        assertEquals("FEED", shortUuid("0000feed-0000-1000-8000-00805f9b34fb"))
        assertEquals("FD5A", shortUuid("0000FD5A-0000-1000-8000-00805F9B34FB"))
        assertNull(shortUuid("12345678-1234-1234-1234-123456789abc"))
        assertNull(shortUuid("feed"))
    }
}
