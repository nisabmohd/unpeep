package app.unpeep.privacy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Hotspots and Bluetooth as the screens list them, "Not a camera?", "Add to report" and Find it. */
class NearbyTest {
    private val hotspots = listOf(
        HotspotSignature(Regex("^V380[-_]", RegexOption.IGNORE_CASE), "V380"),
        HotspotSignature(Regex("^SmartLife[-_]", RegexOption.IGNORE_CASE), "Tuya", weak = true),
    )
    private val ble = BleSignatures(
        trackers = listOf(TrackerSignature("AirTag or Find My tag", company = 76, type = 18), TrackerSignature("Tile", services = listOf("FEED"))),
        cameras = listOf(Signatures.CameraSignature("Lookcam", listOf("lookcam"))),
    )
    private fun h(ssid: String, rssi: Int, bssid: String = "02:00:00:00:00:${(-rssi).toString().padStart(2, '0')}") = SeenHotspot(ssid, bssid, rssi)

    // --- Hotspots ---------------------------------------------------------------------------------

    @Test fun cameraHotspotsComeFirstThenWorthALookThenOtherNetworksByStrength() {
        val d = judgeHotspots(listOf(h("Cafe", -60), h("SmartLife-1A2B", -70), h("Home", -40), h("V380-8A12F0", -80)), hotspots, emptyMap(), emptySet())
        assertEquals(listOf("V380-8A12F0", "SmartLife-1A2B", "Home", "Cafe"), d.map { it.name })
        assertEquals(listOf(Risk.LIKELY_CAMERA, Risk.CHECK_THIS, Risk.UNKNOWN, Risk.UNKNOWN), d.map { it.risk })
        assertEquals(listOf(NearbyKind.CAMERA_HOTSPOT, NearbyKind.CAMERA_HOTSPOT, NearbyKind.NETWORK, NearbyKind.NETWORK), d.map { it.kind })
        assertEquals("V380", d.first().label)
    }

    @Test fun aNameSeenOnSeveralAccessPointsIsOneRowWithTheStrongest() {
        val d = judgeHotspots(listOf(h("Hotel", -80, "a"), h("Hotel", -55, "b"), h("Hotel", -70, "c")), hotspots, emptyMap(), emptySet())
        assertEquals(1, d.size)
        assertEquals(-55, d.single().rssi)
        assertEquals("b", d.single().id)
    }

    @Test fun hiddenNetworksAreLeftOut() {
        assertTrue(judgeHotspots(listOf(h("", -50), h("  ", -50)), hotspots, emptyMap(), emptySet()).isEmpty())
    }

    @Test fun aMarkedCameraHotspotIsKnownAndAnOrdinaryNetworkStaysUnknown() {
        val marks = mapOf(nearbyKey(Check.HOTSPOTS, "V380-8A12F0", "x") to KnownAs.MINE, nearbyKey(Check.HOTSPOTS, "Home", "y") to KnownAs.ROOM)
        val d = judgeHotspots(listOf(h("V380-8A12F0", -50), h("Home", -40)), hotspots, marks, emptySet())
        assertEquals(Risk.KNOWN, d.first { it.name == "V380-8A12F0" }.risk)
        assertEquals(KnownAs.MINE, d.first { it.name == "V380-8A12F0" }.knownAs)
        // A network nearby is never called fine, marked or not.
        assertEquals(Risk.UNKNOWN, d.first { it.name == "Home" }.risk)
    }

    @Test fun addedToReportFollowsTheName() {
        val d = judgeHotspots(listOf(h("V380-8A12F0", -50), h("Home", -40)), hotspots, emptyMap(), setOf(nearbyKey(Check.HOTSPOTS, "V380-8A12F0", "other-bssid")))
        assertTrue(d.first { it.name == "V380-8A12F0" }.added)
        assertFalse(d.first { it.name == "Home" }.added)
    }

    // --- Bluetooth --------------------------------------------------------------------------------

    @Test fun trackersAndCameraNamesAreWorthALookAndTheRestIsQuiet() {
        val d = judgeBle(
            listOf(
                BleAdvert("AA", "Buds", -40),
                BleAdvert("BB", null, -70, manufacturer = mapOf(76 to byteArrayOf(18, 25))),
                BleAdvert("CC", "LookCam-22", -60),
                BleAdvert("DD", " ", -50, services = setOf("FEED")),
            ),
            ble, emptyMap(), emptySet(),
        )
        assertEquals(listOf("DD", "CC", "BB", "AA"), d.map { it.id })
        assertEquals(listOf(NearbyKind.TRACKER, NearbyKind.BLE_CAMERA, NearbyKind.TRACKER, NearbyKind.BLE_OTHER), d.map { it.kind })
        assertEquals(listOf(Risk.CHECK_THIS, Risk.CHECK_THIS, Risk.CHECK_THIS, Risk.UNKNOWN), d.map { it.risk })
        assertEquals("Tile", d.first().label)
        assertNull("a blank name is no name", d.first().name)
        // Bluetooth never says "likely camera": a name is only a hint.
        assertTrue(d.none { it.risk == Risk.LIKELY_CAMERA })
    }

    @Test fun aNamelessTrackerIsMarkedByItsAddress() {
        val marks = mapOf(nearbyKey(Check.BLUETOOTH, null, "BB") to KnownAs.MINE)
        val d = judgeBle(listOf(BleAdvert("BB", null, -70, manufacturer = mapOf(76 to byteArrayOf(18))), BleAdvert("EE", null, -70, manufacturer = mapOf(76 to byteArrayOf(18)))), ble, marks, emptySet())
        assertEquals(Risk.KNOWN, d.first { it.id == "BB" }.risk)
        assertEquals(Risk.CHECK_THIS, d.first { it.id == "EE" }.risk)
    }

    @Test fun marksForOneCheckSayNothingAboutTheOther() {
        assertFalse(nearbyKey(Check.HOTSPOTS, "IPC-1", "x") == nearbyKey(Check.BLUETOOTH, "IPC-1", "x"))
    }

    @Test fun theSnapshotCountsAndFinds() {
        val s = NearbySnapshot(Check.HOTSPOTS, devices = judgeHotspots(listOf(h("V380-1", -50, "a"), h("Home", -40, "b")), hotspots, emptyMap(), emptySet()))
        assertEquals(1, s.count(Risk.LIKELY_CAMERA))
        assertEquals(1, s.count(Risk.UNKNOWN))
        assertEquals("V380-1", s.device("a")?.name)
        assertNull(s.device("zz"))
    }

    // --- Find it ----------------------------------------------------------------------------------

    @Test fun oneReadingIsNoTrend() {
        assertEquals(Trend.STEADY, trend(emptyList()))
        assertEquals(Trend.STEADY, trend(listOf(-60)))
    }

    @Test fun aClearRiseIsStrongerAndAClearFallIsWeaker() {
        assertEquals(Trend.STRONGER, trend(listOf(-70, -68, -60)))
        assertEquals(Trend.WEAKER, trend(listOf(-50, -52, -60)))
    }

    @Test fun aWobbleIsSteady() {
        assertEquals(Trend.STEADY, trend(listOf(-60, -62, -59)))
        assertEquals(Trend.STEADY, trend(listOf(-60, -60)))
    }

    @Test fun onlyTheLastFewReadingsCount() {
        // Long ago it was far away; lately it has not moved.
        assertEquals(Trend.STEADY, trend(listOf(-90, -90, -90, -50, -50, -50, -50)))
    }

    @Test fun theMeterFillsAsTheSignalGrows() {
        assertEquals(0, meterLevel(-100))
        assertEquals(0, meterLevel(-95))
        assertEquals(1, meterLevel(-90))
        assertEquals(2, meterLevel(-85))
        assertEquals(3, meterLevel(-70))
        assertEquals(4, meterLevel(-55))
        assertEquals(METER_SEGMENTS, meterLevel(-50))
        assertEquals(METER_SEGMENTS, meterLevel(-20))
        val levels = (-100..-20).map(::meterLevel)
        assertEquals("never goes down as the signal grows", levels.sorted(), levels)
    }

    @Test fun theMeterAlwaysAgreesWithTheWordBesideIt() {
        for (rssi in -94..-20) {
            val level = meterLevel(rssi)
            when (nearness(rssi)) {
                Nearness.VERY_CLOSE -> assertEquals("$rssi", METER_SEGMENTS, level)
                Nearness.CLOSE -> assertEquals("$rssi", 4, level)
                Nearness.NEAR -> assertEquals("$rssi", 3, level)
                Nearness.FAR -> assertTrue("$rssi", level in 1..2)
            }
        }
    }
}
