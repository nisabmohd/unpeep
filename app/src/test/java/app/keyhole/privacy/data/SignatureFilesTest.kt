package app.keyhole.privacy.data

import app.keyhole.privacy.domain.BleAdvert
import app.keyhole.privacy.domain.BleKind
import app.keyhole.privacy.domain.DeviceEvidence
import app.keyhole.privacy.domain.PortKind
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.domain.classify
import app.keyhole.privacy.domain.classifyBle
import app.keyhole.privacy.domain.classifyHotspot
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The real bundled lists: they parse, hold no risky words, and judge well-known devices the right way. */
class SignatureFilesTest {
    private val root: File = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
    private val dir = File(root, "app/src/main/assets/signatures")
    private val loader = SignatureLoader { File(dir, it).readText() }
    private val s get() = loader.signatures

    @Test fun everyFileParses() {
        assertTrue(s.cameras.size >= 20)
        assertTrue(s.known.size >= 6)
        assertTrue(loader.hotspots.size >= 20)
        assertTrue(loader.ble.trackers.size >= 5)
    }

    @Test fun thePlanPortsAreAllThere() {
        for (p in listOf(554, 8554, 80, 8080, 8000, 443, 37777, 34567, 8899, 2020, 1935, 6668, 9527, 5000)) assertTrue("$p", p in s.ports)
        assertEquals(PortKind.RTSP, s.ports[554])
        assertEquals(PortKind.CAMERA, s.ports[37777])
        assertEquals(PortKind.ONVIF, s.ports[8899])
    }

    @Test fun wordsAreSpecific() {
        val all = s.cameras.flatMap { it.words } + s.known.flatMap { it.words } + loader.ble.cameras.flatMap { it.words }
        val short = all.filter { it.length < 3 }
        assertTrue("words shorter than 3 letters match too much: $short", short.isEmpty())
        for (bad in listOf("cam", "camera", "ip", "tv", "phone", "wifi", "smart")) assertFalse("\"$bad\" is too broad", bad in all)
        assertEquals("every word is lower case", all, all.map { it.lowercase() })
    }

    @Test fun noCameraWordAlsoMatchesAKnownDevice() {
        val clash = s.cameras.flatMap { it.words }.filter { w -> s.known.any { k -> w in k.words } }
        assertTrue("in both cameras.json and known.json: $clash", clash.isEmpty())
    }

    @Test fun wellKnownCamerasAreLikelyCameras() {
        val cams = listOf(
            DeviceEvidence("192.168.1.23", name = "IPC-3F2A", rtspPorts = setOf(554), openPorts = setOf(80, 554, 8899)),
            DeviceEvidence("192.168.1.24", httpServer = "DNVRS-Webs"),
            DeviceEvidence("192.168.1.25", httpTitle = "NETSurveillance WEB"),
            DeviceEvidence("192.168.1.26", name = "Tapo_C200_1A2B", rtspPorts = setOf(554)),
            DeviceEvidence("192.168.1.27", upnpManufacturer = "Dahua", upnpModel = "DH-IPC-HFW1230S"),
            DeviceEvidence("192.168.1.28", httpServer = "uc-httpd 1.0.0"),
            DeviceEvidence("192.168.1.29", onvif = true),
            DeviceEvidence("192.168.1.30", rtspServer = "GStreamer RTSP server", rtspPorts = setOf(8554)),
        )
        for (c in cams) assertEquals(c.toString(), Risk.LIKELY_CAMERA, classify(c, s).risk)
    }

    @Test fun householdDevicesAreKnown() {
        val known = listOf(
            DeviceEvidence("192.168.1.40", name = "Living-room Chromecast", openPorts = setOf(8080, 443)),
            DeviceEvidence("192.168.1.41", name = "Camerons-iPhone"),
            DeviceEvidence("192.168.1.42", name = "HP LaserJet Pro", openPorts = setOf(80, 443)),
            DeviceEvidence("192.168.1.43", mdnsServices = setOf("_airplay._tcp"), openPorts = setOf(5000)),
            DeviceEvidence("192.168.1.44", upnpManufacturer = "Sonos, Inc.", openPorts = setOf(80)),
            DeviceEvidence("192.168.1.45", name = "BRAVIA 4K", openPorts = setOf(80, 8080)),
            DeviceEvidence("192.168.1.46", name = "PlayStation 5"),
        )
        for (k in known) assertEquals(k.toString(), Risk.KNOWN, classify(k, s).risk)
    }

    @Test fun cameraHotspotsAreFlagged() {
        for (n in listOf("HDWiFiCam-112233", "IPC-3F2A", "V380-889900", "A9_4455", "Lookcam_01", "CAM-77AB", "MV12345678", "GW_AP_1234567"))
            assertEquals(n, Risk.LIKELY_CAMERA, classifyHotspot(n, loader.hotspots).risk)
        assertEquals(Risk.CHECK_THIS, classifyHotspot("SmartLife-9A2C", loader.hotspots).risk)
    }

    @Test fun ordinaryNetworksAreNotFlagged() {
        for (n in listOf("Sea View Guest", "JioFiber-4G", "Airtel_9876", "NETGEAR42", "iPhone", "Campus-WiFi", "Camila's Wi-Fi", "DIRECT-roku", "Cameron", "IPCONFIG", "TP-Link_5G", "eduroam"))
            assertEquals(n, Risk.UNKNOWN, classifyHotspot(n, loader.hotspots).risk)
    }

    @Test fun trackersAreFoundByAdvertisement() {
        fun ad(services: Set<String> = emptySet(), m: Map<Int, ByteArray> = emptyMap()) = BleAdvert("AA:BB:CC:DD:EE:FF", null, -60, services, m)
        assertEquals(BleKind.TRACKER, classifyBle(ad(m = mapOf(0x004C to byteArrayOf(0x12, 0x19))), loader.ble).kind)
        for (svc in listOf("FEED", "FEEC", "FD5A", "FE33", "FE65", "FEAA")) assertEquals(svc, BleKind.TRACKER, classifyBle(ad(services = setOf(svc)), loader.ble).kind)
        assertEquals(BleKind.OTHER, classifyBle(ad(services = setOf("180F")), loader.ble).kind)
    }

    @Test fun hotspotPatternsAreAnchored() {
        val loose = File(dir, "hotspots.json").readLines().filter { "\"pattern\"" in it && "\"pattern\": \"^" !in it }
        assertTrue("patterns must start with ^: $loose", loose.isEmpty())
    }
}
