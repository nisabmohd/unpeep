package app.keyhole.privacy.scan.net

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The hard rule: sockets only to private, link-local and local multicast addresses. */
class LocalOnlyTest {
    private fun ip(s: String) = LocalOnly.parseIpv4(s)!!
    private fun v6(s: String) = InetAddress.getByName(s)

    @Test fun privateRangesAreLocal() {
        for (a in listOf("10.0.0.1", "10.255.255.254", "172.16.0.1", "172.31.255.254", "192.168.0.1", "192.168.1.23", "192.168.255.254"))
            assertTrue(a, LocalOnly.isLocal(ip(a)))
    }

    @Test fun linkLocalIsLocal() {
        assertTrue(LocalOnly.isLocal(ip("169.254.10.20")))
        assertTrue(LocalOnly.isLocal(v6("fe80::1")))
    }

    @Test fun discoveryMulticastIsLocal() {
        assertTrue("SSDP / WS-Discovery", LocalOnly.isLocal(ip("239.255.255.250")))
        assertTrue("mDNS", LocalOnly.isLocal(ip("224.0.0.251")))
        assertTrue("mDNS v6", LocalOnly.isLocal(v6("ff02::fb")))
    }

    @Test fun publicAddressesAreRefused() {
        for (a in listOf("8.8.8.8", "1.1.1.1", "172.15.255.255", "172.32.0.1", "192.167.1.1", "192.169.1.1", "11.0.0.1", "9.255.255.255", "100.64.0.1", "140.82.112.3", "255.255.255.255"))
            assertFalse(a, LocalOnly.isLocal(ip(a)))
        assertFalse(LocalOnly.isLocal(v6("2001:4860:4860::8888")))
    }

    @Test fun globalMulticastIsRefused() {
        assertFalse(LocalOnly.isLocal(ip("224.0.1.1")))
        assertFalse(LocalOnly.isLocal(ip("232.1.1.1")))
        assertFalse(LocalOnly.isLocal(v6("ff0e::1")))
    }

    @Test fun loopbackAndAnyAreRefused() {
        assertFalse(LocalOnly.isLocal(ip("127.0.0.1")))
        assertFalse(LocalOnly.isLocal(ip("0.0.0.0")))
        assertFalse(LocalOnly.isLocal(v6("::1")))
    }

    @Test fun uniqueLocalV6IsLocal() {
        assertTrue(LocalOnly.isLocal(v6("fd12:3456:789a::1")))
        assertTrue(LocalOnly.isLocal(v6("fc00::1")))
    }

    @Test fun parseNeverTakesAHostName() {
        assertNull(LocalOnly.parseIpv4("example.com"))
        assertNull(LocalOnly.parseIpv4("192.168.1"))
        assertNull(LocalOnly.parseIpv4("192.168.1.256"))
        assertNull(LocalOnly.parseIpv4("192.168.1.1.1"))
        assertNull(LocalOnly.parseIpv4("192.168..1"))
        assertNull(LocalOnly.parseIpv4("192.168.1.-1"))
        assertNull(LocalOnly.parseIpv4(""))
        assertEquals("192.168.1.23", LocalOnly.parseIpv4("192.168.1.23")!!.hostAddress)
    }

    @Test fun connectToAPublicAddressThrowsBeforeAnySocket() {
        assertThrows(LocalOnly.NotLocalException::class.java) { LocalOnly.connect(ip("8.8.8.8"), 80, 50) }
        assertThrows(LocalOnly.NotLocalException::class.java) { LocalOnly.connect(ip("127.0.0.1"), 80, 50) }
    }

    @Test fun sendToAPublicAddressThrows() {
        LocalOnly.datagramSocket(50).use { socket ->
            assertThrows(LocalOnly.NotLocalException::class.java) { LocalOnly.send(socket, ByteArray(1), ip("8.8.8.8"), 53) }
        }
    }
}
