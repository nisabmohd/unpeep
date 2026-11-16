package app.keyhole.privacy.scan.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the scanner asks and how it reads the answers (docs/SCANNING.md § Wi-Fi). */
class BannersTest {
    // --- What Keyhole sends: discovery, a plain OPTIONS, a plain GET. Nothing else. -----------------

    @Test fun theRtspQuestionIsOnlyOptions() {
        val q = Banners.rtspOptions("192.168.1.23", 554)
        assertTrue(q.startsWith("OPTIONS rtsp://192.168.1.23:554/ RTSP/1.0\r\n"))
        assertTrue(q.endsWith("\r\n\r\n"))
        for (word in listOf("DESCRIBE", "SETUP", "PLAY", "Authorization")) assertFalse(word, word in q)
    }

    @Test fun theWebQuestionIsAPlainGet() {
        val q = Banners.httpGet("192.168.1.23")
        assertTrue(q.startsWith("GET / HTTP/1.0\r\nHost: 192.168.1.23\r\n"))
        assertFalse("Authorization" in q)
        assertFalse("Cookie" in q)
    }

    @Test fun theOnvifProbeAsksForVideoDevicesOnly() {
        val p = Banners.onvifProbe("1234")
        assertTrue("NetworkVideoTransmitter" in p)
        assertTrue("uuid:1234" in p)
    }

    // --- RTSP ---------------------------------------------------------------------------------------

    @Test fun anRtspReplyIsReadWithItsServer() {
        val r = Banners.rtsp("RTSP/1.0 200 OK\r\nCSeq: 1\r\nServer: Hikvision RTSP\r\nPublic: OPTIONS\r\n\r\n")
        assertEquals("Hikvision RTSP", r?.server)
    }

    @Test fun aLockedRtspPortStillSpeaksRtsp() {
        assertNotNull(Banners.rtsp("RTSP/1.0 401 Unauthorized\r\nCSeq: 1\r\n\r\n"))
    }

    @Test fun aWebServerOnAnRtspPortIsNotRtsp() {
        assertNull(Banners.rtsp("HTTP/1.1 400 Bad Request\r\n\r\n"))
        assertNull(Banners.rtsp(""))
    }

    // --- HTTP ---------------------------------------------------------------------------------------

    @Test fun aWebPageGivesItsServerAndTitle() {
        val h = Banners.http("HTTP/1.1 200 OK\r\nserver: DNVRS-Webs\r\nContent-Type: text/html\r\n\r\n<html><head><TITLE>\n  Login &amp; view </TITLE></head>")
        assertEquals("DNVRS-Webs", h?.server)
        assertEquals("Login & view", h?.title)
    }

    @Test fun aPageWithNoTitleOrServerIsStillAPage() {
        val h = Banners.http("HTTP/1.0 302 Found\r\nLocation: /login\r\n\r\n")
        assertNotNull(h)
        assertNull(h?.server)
        assertNull(h?.title)
    }

    @Test fun headersInTheBodyAreNotHeaders() {
        assertNull(Banners.header("HTTP/1.1 200 OK\r\nX: y\r\n\r\nServer: fake", "Server"))
    }

    @Test fun aLongTitleIsCut() {
        val h = Banners.http("HTTP/1.1 200 OK\r\n\r\n<title>${"x".repeat(500)}</title>")
        assertEquals(80, h?.title?.length)
    }

    // --- SSDP / UPnP --------------------------------------------------------------------------------

    @Test fun aDescriptionAddressMustBeAPlainLocalIp() {
        assertEquals(Banners.Target("192.168.1.19", 8008, "/ssdp/device-desc.xml"), Banners.target("http://192.168.1.19:8008/ssdp/device-desc.xml"))
        assertEquals(Banners.Target("192.168.1.1", 80, "/"), Banners.target("http://192.168.1.1"))
        // A host name would mean a DNS lookup, and https a certificate: neither is read.
        assertNull(Banners.target("http://camera.example.com/desc.xml"))
        assertNull(Banners.target("https://192.168.1.19/desc.xml"))
        assertNull(Banners.target("http://192.168.1.19:99999/desc.xml"))
    }

    @Test fun theSsdpLocationHeaderIsFoundInAnyCase() {
        val reply = "HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=1800\r\nLocation: http://192.168.1.19:8008/d.xml\r\nST: upnp:rootdevice\r\n\r\n"
        assertEquals("http://192.168.1.19:8008/d.xml", Banners.header(reply, "LOCATION"))
    }

    @Test fun aDeviceDescriptionGivesNameMakerAndModel() {
        val u = Banners.upnp("<root><device><friendlyName>Living-room TV</friendlyName><manufacturer>Sony</manufacturer><modelName>BRAVIA 4K</modelName></device></root>")
        assertEquals(Banners.Upnp("Living-room TV", "Sony", "BRAVIA 4K"), u)
        assertEquals(Banners.Upnp(null, null, null), Banners.upnp("<root/>"))
    }

    // --- ONVIF --------------------------------------------------------------------------------------

    @Test fun anOnvifReplyIsRecognisedAndNamed() {
        val reply = "<d:ProbeMatch><d:Types>dn:NetworkVideoTransmitter</d:Types><d:Scopes>onvif://www.onvif.org/type/video_encoder onvif://www.onvif.org/name/IPC%203F2A onvif://www.onvif.org/hardware/X</d:Scopes></d:ProbeMatch>"
        assertTrue(Banners.isOnvif(reply))
        assertEquals("IPC 3F2A", Banners.onvifName(reply))
    }

    @Test fun aPrintersDiscoveryReplyIsNotOnvif() {
        assertFalse(Banners.isOnvif("<d:ProbeMatch><d:Types>wsdp:Device wprt:PrintDeviceType</d:Types></d:ProbeMatch>"))
    }
}
