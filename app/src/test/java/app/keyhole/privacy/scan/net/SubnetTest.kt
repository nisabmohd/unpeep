package app.keyhole.privacy.scan.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The scan visits the /24 around the phone, and nothing else. */
class SubnetTest {
    @Test fun aScanVisitsThe24AroundThePhone() {
        val hosts = Subnet.hosts("192.168.1.14")
        assertEquals(253, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertFalse("the phone itself is not knocked on", "192.168.1.14" in hosts)
        assertFalse("192.168.1.0" in hosts)
        assertFalse("192.168.1.255" in hosts)
    }

    @Test fun everyAddressVisitedIsLocal() {
        for (ip in listOf("10.0.2.16", "172.20.10.3", "192.168.43.7")) {
            assertTrue(Subnet.hosts(ip).all { LocalOnly.isLocal(LocalOnly.parseIpv4(it)!!) })
        }
    }

    @Test fun somethingThatIsNotAnAddressVisitsNothing() {
        assertTrue(Subnet.hosts("fe80::1").isEmpty())
        assertTrue(Subnet.hosts("example.com").isEmpty())
    }
}
