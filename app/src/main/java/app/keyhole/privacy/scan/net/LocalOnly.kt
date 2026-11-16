package app.keyhole.privacy.scan.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.Socket

/**
 * The one door to the network (CLAUDE.md › Hard rules): every socket the scanner opens goes through here, and only
 * to private (10/8, 172.16/12, 192.168/16), link-local and multicast addresses. Anything else throws before a
 * single packet leaves. `ProjectRulesTest` checks that no other file creates a socket.
 */
object LocalOnly {
    class NotLocalException(address: String) : SecurityException("Keyhole only talks to the local network, not $address")

    /** True for addresses that never leave the local network. Loopback is not a room's device, so it is refused. */
    fun isLocal(address: InetAddress): Boolean = when {
        address.isLoopbackAddress || address.isAnyLocalAddress -> false
        address.isMulticastAddress -> isLocalMulticast(address)
        address.isSiteLocalAddress && address is Inet4Address -> true
        address.isLinkLocalAddress -> true
        // IPv6 unique local (fc00::/7): the private range of IPv6.
        address !is Inet4Address -> (address.address[0].toInt() and 0xFE) == 0xFC
        else -> false
    }

    /**
     * Multicast that stays on the link or the site: IPv4 224.0.0.0/24 (mDNS) and 239/8 (SSDP, WS-Discovery);
     * IPv6 with interface-, link- or site-local scope. Global multicast is refused.
     */
    private fun isLocalMulticast(a: InetAddress): Boolean =
        if (a is Inet4Address) {
            val b = a.address
            val first = b[0].toInt() and 0xFF
            first == 239 || (first == 224 && b[1].toInt() == 0 && b[2].toInt() == 0)
        } else {
            a.isMCNodeLocal || a.isMCLinkLocal || a.isMCSiteLocal
        }

    /** Parses a dotted IPv4 address without ever asking DNS (a host name would be a lookup outside the network). */
    fun parseIpv4(ip: String): InetAddress? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        val bytes = ByteArray(4)
        for (i in 0 until 4) {
            val p = parts[i]
            if (p.isEmpty() || p.length > 3 || !p.all { it in '0'..'9' }) return null
            val n = p.toInt()
            if (n > 255) return null
            bytes[i] = n.toByte()
        }
        return InetAddress.getByAddress(bytes)
    }

    fun require(address: InetAddress) {
        if (!isLocal(address)) throw NotLocalException(address.hostAddress ?: "?")
    }

    /** A TCP connection to a local address; the caller closes it. */
    fun connect(address: InetAddress, port: Int, timeoutMs: Int): Socket {
        require(address)
        val socket = Socket()
        try {
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(address, port), timeoutMs)
        } catch (e: Throwable) {
            runCatching { socket.close() }
            throw e
        }
        return socket
    }

    /** A UDP socket for discovery; it can only send through [send]. */
    fun datagramSocket(timeoutMs: Int): DatagramSocket = DatagramSocket().apply { soTimeout = timeoutMs }

    fun multicastSocket(timeoutMs: Int): MulticastSocket = MulticastSocket().apply { soTimeout = timeoutMs }

    /** Sends [data] to a local or local-multicast address. */
    fun send(socket: DatagramSocket, data: ByteArray, address: InetAddress, port: Int) {
        require(address)
        socket.send(DatagramPacket(data, data.size, address, port))
    }
}
