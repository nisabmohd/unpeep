package app.keyhole.privacy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Wi-Fi results as the screens list them, and "Not a camera?". */
class WifiTest {
    private val s = Signatures(
        cameras = listOf(Signatures.CameraSignature("Xiongmai", listOf("ipc-"))),
        known = listOf(Signatures.KnownSignature("tv", listOf("bravia")), Signatures.KnownSignature("phone", listOf("pixel"))),
        ports = mapOf(554 to PortKind.RTSP, 80 to PortKind.WEB, 8080 to PortKind.WEB),
    )
    private val camera = DeviceEvidence("192.168.1.23", name = "IPC-3F2A", openPorts = setOf(80, 554), rtspPorts = setOf(554))
    private val tv = DeviceEvidence("192.168.1.19", name = "Bravia")
    private val web = DeviceEvidence("192.168.1.41", openPorts = setOf(8080))
    private val quiet = DeviceEvidence("192.168.1.9")
    private val quiet2 = DeviceEvidence("192.168.1.10")
    private val router = DeviceEvidence("192.168.1.1", isGateway = true)
    private val phone = DeviceEvidence("192.168.1.14", isSelf = true)

    @Test fun likelyCamerasComeFirstThenWorthALookThenTheRest() {
        val d = judge(listOf(tv, quiet2, router, web, quiet, camera), s, "SeaView_Guest", emptyMap(), 1L)
        assertEquals(listOf("192.168.1.23", "192.168.1.41", "192.168.1.9", "192.168.1.10", "192.168.1.1", "192.168.1.19"), d.map { it.ip })
        assertEquals(listOf(Risk.LIKELY_CAMERA, Risk.CHECK_THIS, Risk.UNKNOWN, Risk.UNKNOWN, Risk.KNOWN, Risk.KNOWN), d.map { it.risk })
    }

    @Test fun aMarkedDeviceIsKnownButKeepsItsSigns() {
        val marks = mapOf(markKey("SeaView_Guest", camera) to KnownAs.MINE)
        val d = judge(listOf(camera), s, "SeaView_Guest", marks, 1L).single()
        assertEquals(Risk.KNOWN, d.risk)
        assertEquals(KnownAs.MINE, d.knownAs)
        assertEquals(Sign.MarkedByUser, d.verdict.signs.first())
        assertTrue(Sign.Rtsp(554) in d.verdict.signs)
    }

    @Test fun aMarkFollowsTheNameNotTheAddress() {
        val marks = mapOf(markKey("SeaView_Guest", camera) to KnownAs.ROOM)
        val moved = camera.copy(ip = "192.168.1.77")
        assertEquals(KnownAs.ROOM, judge(listOf(moved), s, "SeaView_Guest", marks, 1L).single().knownAs)
    }

    @Test fun aMarkOnOneWifiSaysNothingAboutAnother() {
        val marks = mapOf(markKey("SeaView_Guest", camera) to KnownAs.MINE)
        val d = judge(listOf(camera), s, "Other_Wifi", marks, 1L).single()
        assertNull(d.knownAs)
        assertEquals(Risk.LIKELY_CAMERA, d.risk)
    }

    @Test fun aNamelessDeviceIsMarkedByItsAddress() {
        val marks = mapOf(markKey(null, web) to KnownAs.MINE)
        assertEquals(KnownAs.MINE, judge(listOf(web), s, null, marks, 1L).single().knownAs)
        assertNull(judge(listOf(quiet), s, null, marks, 1L).single().knownAs)
    }

    @Test fun onlyTheRouterAndThisPhoneLooksIsolated() {
        assertTrue(looksIsolated(listOf(router, phone)))
        assertFalse(looksIsolated(listOf(router, phone, tv)))
    }

    @Test fun theSnapshotCountsAndFinds() {
        val snap = WifiSnapshot(devices = judge(listOf(camera, tv, web, router), s, null, emptyMap(), 1L))
        assertEquals(1, snap.count(Risk.LIKELY_CAMERA))
        assertEquals(1, snap.count(Risk.CHECK_THIS))
        assertEquals(2, snap.count(Risk.KNOWN))
        assertEquals("IPC-3F2A", snap.device("192.168.1.23")?.name)
        assertNull(snap.device("192.168.1.99"))
    }

    @Test fun timeLeftIsAGuessInsideTheBudget() {
        // Half done after 6 s: about 6 s more.
        assertEquals(6, secondsLeft(0.5f, 6_000, 20_000))
        // Barely started: no guess yet, so what is left of the budget.
        assertEquals(19, secondsLeft(0.01f, 1_000, 20_000))
        // A slow scan never promises more than the budget has left, and never less than a second.
        assertEquals(2, secondsLeft(0.2f, 18_000, 20_000))
        assertEquals(1, secondsLeft(0.99f, 19_900, 20_000))
        assertEquals(1, secondsLeft(0.5f, 25_000, 20_000))
    }
}
