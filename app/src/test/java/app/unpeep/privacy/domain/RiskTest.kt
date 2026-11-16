package app.unpeep.privacy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The risk rules (CLAUDE.md › Behaviour rules). Change these tests first, then `Risk.kt`. */
class RiskTest {
    private val s = Signatures(
        cameras = listOf(
            Signatures.CameraSignature("Hikvision", listOf("hikvision", "dnvrs-webs")),
            Signatures.CameraSignature("Xiongmai", listOf("xiongmai", "xmeye", "ipc-")),
            Signatures.CameraSignature("Tapo", listOf("tapo c", "tapo_cam")),
        ),
        known = listOf(
            Signatures.KnownSignature("tv", listOf("bravia", "chromecast", "roku"), listOf("_googlecast._tcp")),
            Signatures.KnownSignature("printer", listOf("laserjet", "epson"), listOf("_ipp._tcp")),
            Signatures.KnownSignature("phone", listOf("iphone", "pixel", "galaxy")),
        ),
        ports = mapOf(554 to PortKind.RTSP, 8554 to PortKind.RTSP, 80 to PortKind.WEB, 8080 to PortKind.WEB, 37777 to PortKind.CAMERA, 34567 to PortKind.CAMERA, 8899 to PortKind.ONVIF, 6668 to PortKind.OTHER),
    )
    private fun d(ip: String = "192.168.1.23", block: DeviceEvidence.() -> DeviceEvidence = { this }) = DeviceEvidence(ip).block()

    // --- Likely camera -----------------------------------------------------------------------------

    @Test fun anOnvifReplyIsALikelyCamera() {
        val v = classify(d { copy(onvif = true) }, s)
        assertEquals(Risk.LIKELY_CAMERA, v.risk)
        assertEquals(listOf<Sign>(Sign.Onvif), v.signs)
    }

    @Test fun rtspPlusACameraNameIsALikelyCamera() {
        val v = classify(d { copy(name = "IPC-3F2A", rtspPorts = setOf(554), openPorts = setOf(80, 554, 8899)) }, s)
        assertEquals(Risk.LIKELY_CAMERA, v.risk)
        assertEquals("Xiongmai", v.maker)
        assertTrue(Sign.Rtsp(554) in v.signs)
        assertTrue(Sign.CameraMaker("Xiongmai") in v.signs)
        assertTrue(Sign.CameraPort(8899) in v.signs)
        assertEquals(3, v.strongSigns)
    }

    @Test fun aCameraMakersBannerIsALikelyCamera() {
        assertEquals(Risk.LIKELY_CAMERA, classify(d { copy(httpServer = "DNVRS-Webs") }, s).risk)
        assertEquals(Risk.LIKELY_CAMERA, classify(d { copy(httpTitle = "Hikvision Login") }, s).risk)
        assertEquals(Risk.LIKELY_CAMERA, classify(d { copy(upnpManufacturer = "HIKVISION") }, s).risk)
        assertEquals(Risk.LIKELY_CAMERA, classify(d { copy(rtspServer = "Hikvision RTSP", rtspPorts = setOf(554)) }, s).risk)
    }

    @Test fun onvifBeatsAKnownSignature() {
        assertEquals(Risk.LIKELY_CAMERA, classify(d { copy(name = "Bravia", onvif = true) }, s).risk)
    }

    @Test fun theTestCameraTapoIsALikelyCamera() {
        assertEquals(Risk.LIKELY_CAMERA, classify(d { copy(name = "Tapo_Cam_1A2B", rtspPorts = setOf(554), openPorts = setOf(554, 2020)) }, s).risk)
    }

    // --- Check this --------------------------------------------------------------------------------

    @Test fun rtspWithNoMatchIsCheckThis() {
        val v = classify(d { copy(rtspPorts = setOf(8554)) }, s)
        assertEquals(Risk.CHECK_THIS, v.risk)
        assertEquals(listOf<Sign>(Sign.Rtsp(8554)), v.signs)
    }

    @Test fun anOpenRtspPortThatDidNotAnswerIsStillCheckThis() {
        val v = classify(d { copy(openPorts = setOf(554)) }, s)
        assertEquals(Risk.CHECK_THIS, v.risk)
        assertEquals(listOf<Sign>(Sign.Rtsp(554)), v.signs)
    }

    @Test fun aCameraPortWithNoMatchIsCheckThis() {
        val v = classify(d { copy(openPorts = setOf(37777)) }, s)
        assertEquals(Risk.CHECK_THIS, v.risk)
        assertEquals(listOf<Sign>(Sign.CameraPort(37777)), v.signs)
    }

    @Test fun aCameraNameAloneIsOnlyCheckThis() {
        val v = classify(d { copy(name = "IPC-9981") }, s)
        assertEquals(Risk.CHECK_THIS, v.risk)
        assertEquals("Xiongmai", v.maker)
    }

    @Test fun anUnknownDeviceWithAWebPageIsCheckThis() {
        assertEquals(Risk.CHECK_THIS, classify(d { copy(openPorts = setOf(80)) }, s).risk)
        val v = classify(d { copy(httpTitle = "Login") }, s)
        assertEquals(Risk.CHECK_THIS, v.risk)
        assertEquals(listOf<Sign>(Sign.WebPage), v.signs)
        assertEquals(0, v.strongSigns)
    }

    // --- Known -------------------------------------------------------------------------------------

    @Test fun aTvIsKnown() {
        val v = classify(d { copy(name = "Living-room Chromecast", openPorts = setOf(8080)) }, s)
        assertEquals(Risk.KNOWN, v.risk)
        assertEquals("tv", v.kind)
    }

    @Test fun aKnownServiceIsKnown() {
        assertEquals("tv", classify(d { copy(mdnsServices = setOf("_googlecast._tcp")) }, s).kind)
        assertEquals("printer", classify(d { copy(mdnsServices = setOf("_ipp._tcp"), openPorts = setOf(80)) }, s).kind)
    }

    @Test fun aKnownDeviceWithAWebPageStaysKnown() {
        assertEquals(Risk.KNOWN, classify(d { copy(name = "HP LaserJet", openPorts = setOf(80, 8080)) }, s).risk)
    }

    @Test fun theRouterIsKnown() {
        val v = classify(d("192.168.1.1") { copy(isGateway = true, openPorts = setOf(80)) }, s)
        assertEquals(Risk.KNOWN, v.risk)
        assertEquals(listOf<Sign>(Sign.Router), v.signs)
    }

    @Test fun aRouterThatAnswersOnvifIsStillACamera() {
        assertEquals(Risk.LIKELY_CAMERA, classify(d { copy(isGateway = true, onvif = true) }, s).risk)
    }

    @Test fun thisPhoneIsKnown() {
        assertEquals(listOf<Sign>(Sign.ThisPhone), classify(d { copy(isSelf = true, openPorts = setOf(80)) }, s).signs)
    }

    @Test fun markedByTheUserIsKnownButKeepsItsSigns() {
        val v = classify(d { copy(onvif = true) }, s, markedKnown = true)
        assertEquals(Risk.KNOWN, v.risk)
        assertEquals(Sign.MarkedByUser, v.signs.first())
        assertTrue(Sign.Onvif in v.signs)
    }

    // --- Unknown -----------------------------------------------------------------------------------

    @Test fun aQuietDeviceIsUnknownNeverFine() {
        val v = classify(d(), s)
        assertEquals(Risk.UNKNOWN, v.risk)
        assertTrue(v.signs.isEmpty())
    }

    @Test fun anOtherPortAloneIsUnknown() {
        assertEquals(Risk.UNKNOWN, classify(d { copy(openPorts = setOf(6668)) }, s).risk)
    }

    // --- Word matching -----------------------------------------------------------------------------

    @Test fun wordsMatchAtAWordEdgeOnly() {
        assertTrue(Signatures.containsWord("hikvision-webs", "hikvision"))
        assertTrue(Signatures.containsWord("my ipc-3f2a", "ipc-"))
        assertFalse(Signatures.containsWord("webcam", "cam"))
        assertFalse(Signatures.containsWord("epic-games", "ipc-"))
        assertFalse(Signatures.containsWord("anything", ""))
    }

    @Test fun aPhoneNamedLikeAWordInsideIsNotACamera() {
        assertEquals(Risk.KNOWN, classify(d { copy(name = "Camerons-iPhone") }, s).risk)
    }

    @Test fun matchingIgnoresCase() {
        assertEquals("Hikvision", s.cameraMaker("HIKVISION DS-2CD"))
        assertEquals(null, s.cameraMaker(null, "", "nothing"))
    }

    // --- Lists -------------------------------------------------------------------------------------

    @Test fun onlyTheRouterAnsweringLooksIsolated() {
        assertTrue(looksIsolated(listOf(d("192.168.1.1") { copy(isGateway = true) }, d("192.168.1.7") { copy(isSelf = true) })))
        assertTrue(looksIsolated(emptyList()))
        assertFalse(looksIsolated(listOf(d("192.168.1.1") { copy(isGateway = true) }, d("192.168.1.9"))))
    }

    @Test fun likelyCamerasComeFirst() {
        val sorted = listOf(Risk.KNOWN, Risk.UNKNOWN, Risk.CHECK_THIS, Risk.LIKELY_CAMERA).sortedBy(::riskOrder)
        assertEquals(listOf(Risk.LIKELY_CAMERA, Risk.CHECK_THIS, Risk.UNKNOWN, Risk.KNOWN), sorted)
    }

    @Test fun addressesSortByNumber() {
        assertEquals(listOf("192.168.1.9", "192.168.1.10", "192.168.1.100"), listOf("192.168.1.100", "192.168.1.10", "192.168.1.9").sortedBy(::ipSortKey))
    }
}
