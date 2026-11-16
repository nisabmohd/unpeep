package app.keyhole.privacy.scan.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import app.keyhole.privacy.domain.DeviceEvidence
import app.keyhole.privacy.domain.PortKind
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/** Where a scan is. */
enum class ScanStatus { RUNNING, NOT_ON_WIFI, DONE, DROPPED }

/** One step of a scan: how far it is, and everything the devices have answered so far. */
data class ScanUpdate(
    val status: ScanStatus,
    val ssid: String? = null,
    /** 0..1 over the whole scan. */
    val fraction: Float = 0f,
    /** Devices whose answers have been read, of [total] found; both 0 while the scan is still finding devices. */
    val checked: Int = 0,
    val total: Int = 0,
    val devices: List<DeviceEvidence> = emptyList(),
)

/**
 * The Wi-Fi scan (docs/SCANNING.md § Wi-Fi). It runs only while someone collects it, never by itself:
 * 1. the /24 around the phone, from `LinkProperties`;
 * 2. a knock on a few ports of every address (≤ 64 sockets at once, 400 ms), with mDNS, SSDP and ONVIF
 *    discovery running beside it;
 * 3. the remaining ports of the devices that are there;
 * 4. for each device, a plain RTSP `OPTIONS` and a plain `GET /`. Nothing else: no logins, no streams.
 * Every socket goes through [LocalOnly] and is tied to the Wi-Fi, so nothing leaves the local network.
 */
class WifiScanner(context: Context) {
    private val context = context.applicationContext

    /** Whether the phone is on a Wi-Fi right now; asked before a scan starts, so "Not on Wi-Fi" shows without a scan. */
    fun onWifi(): Boolean = WifiNetwork.current(context) != null

    fun scan(ports: Map<Int, PortKind>): Flow<ScanUpdate> = channelFlow {
        val link = WifiNetwork.current(context)
        if (link == null) {
            send(ScanUpdate(ScanStatus.NOT_ON_WIFI))
            return@channelFlow
        }
        val run = Run(link, ports, this)
        // Whatever happens, the scan ends inside its budget with what it has.
        withTimeoutOrNull(BUDGET_MS) { run.all() }
        // The phone left the Wi-Fi half-way (E03): what answered is kept, but the scan says it didn't finish.
        val still = WifiNetwork.current(context)
        val status = if (still == null || still.network != link.network) ScanStatus.DROPPED else ScanStatus.DONE
        send(ScanUpdate(status, link.ssid, 1f, run.found.size, run.found.size, run.devices()))
    }.conflate().flowOn(Dispatchers.IO)

    private inner class Run(val link: WifiLink, val ports: Map<Int, PortKind>, val out: ProducerScope<ScanUpdate>) {
        val found = ConcurrentHashMap<String, DeviceEvidence>()
        private val gate = Semaphore(MAX_SOCKETS)
        private val tcp: (Socket) -> Unit = { link.network.bindSocket(it) }
        private val udp: (DatagramSocket) -> Unit = { link.network.bindSocket(it) }

        fun devices(): List<DeviceEvidence> = found.values.toList()

        private fun merge(ip: String, change: (DeviceEvidence) -> DeviceEvidence) {
            found.compute(ip) { _, old -> change(old ?: DeviceEvidence(ip, isGateway = ip == link.gateway)) }
        }

        private fun report(fraction: Float, checked: Int = 0, total: Int = 0) {
            out.trySend(ScanUpdate(ScanStatus.RUNNING, link.ssid, fraction.coerceIn(0f, 1f), checked, total, devices()))
        }

        suspend fun all() = coroutineScope {
            merge(link.ip) { it.copy(isSelf = true) }
            report(0f)
            val discovery = async { discover() }

            // 2. Who is there: a device that takes or refuses a connection is there; silence says nothing.
            val first = FIRST_PORTS.filter { it in ports }.ifEmpty { ports.keys.take(FIRST_PORTS.size) }
            val hosts = Subnet.hosts(link.ip)
            val visited = AtomicInteger()
            hosts.map { ip ->
                async {
                    val address = LocalOnly.parseIpv4(ip) ?: return@async
                    gate.withPermit {
                        for (port in first) {
                            when (Tcp.probe(address, port, CONNECT_MS, tcp)) {
                                PortState.OPEN -> merge(ip) { it.copy(openPorts = it.openPorts + port) }
                                PortState.CLOSED -> merge(ip) { it }
                                PortState.SILENT -> Unit
                            }
                        }
                    }
                    report(FIND_SHARE * visited.incrementAndGet() / hosts.size)
                }
            }.awaitAll()
            discovery.await()

            // 3. The other ports, only on devices that are there.
            val rest = ports.keys - first.toSet()
            val present = found.keys.filter { it != link.ip }
            val knocked = AtomicInteger()
            present.flatMap { ip ->
                val address = LocalOnly.parseIpv4(ip) ?: return@flatMap emptyList()
                rest.map { port ->
                    async {
                        if (gate.withPermit { Tcp.probe(address, port, CONNECT_MS, tcp) } == PortState.OPEN) merge(ip) { it.copy(openPorts = it.openPorts + port) }
                        report(FIND_SHARE + PORT_SHARE * knocked.incrementAndGet() / (present.size * rest.size))
                    }
                }
            }.awaitAll()

            // 4. What each device says about itself.
            val total = found.size
            val checked = AtomicInteger(1) // this phone needs no questions
            report(FIND_SHARE + PORT_SHARE, checked.get(), total)
            present.map { ip ->
                async {
                    fingerprint(ip)
                    val n = checked.incrementAndGet()
                    report(FIND_SHARE + PORT_SHARE + (1f - FIND_SHARE - PORT_SHARE) * n / total, n, total)
                }
            }.awaitAll()
        }

        /** A plain RTSP `OPTIONS` on the RTSP ports and a plain `GET /` on the web ports that are open. */
        private suspend fun fingerprint(ip: String) {
            val address = LocalOnly.parseIpv4(ip) ?: return
            val open = found[ip]?.openPorts.orEmpty()
            for (port in open.filter { ports[it] == PortKind.RTSP }.sorted()) {
                val reply = gate.withPermit { Tcp.ask(address, port, Banners.rtspOptions(ip, port), CONNECT_MS, READ_MS, prepare = tcp) { "\r\n\r\n" in it } }
                val rtsp = reply?.let(Banners::rtsp) ?: continue
                merge(ip) { it.copy(rtspPorts = it.rtspPorts + port, rtspServer = it.rtspServer ?: rtsp.server) }
            }
            // 443 would need TLS; the plain ports say the same things.
            for (port in open.filter { ports[it] == PortKind.WEB && it != TLS_PORT }.sorted()) {
                val reply = gate.withPermit { Tcp.ask(address, port, Banners.httpGet(ip), CONNECT_MS, READ_MS, prepare = tcp) { "</title>" in it.lowercase() } }
                val http = reply?.let(Banners::http) ?: continue
                merge(ip) { it.copy(httpServer = it.httpServer ?: http.server, httpTitle = it.httpTitle ?: http.title) }
                if (http.server != null && http.title != null) break
            }
        }

        /** mDNS, SSDP and ONVIF side by side, with the multicast lock Android needs to hear the replies. */
        private suspend fun discover() {
            val lock = context.getSystemService(WifiManager::class.java)?.createMulticastLock(LOCK_TAG)?.apply { setReferenceCounted(false) }
            runCatching { lock?.acquire() }
            try {
                coroutineScope {
                    launch { runCatching { mdns() } }
                    launch { runCatching { ssdp() } }
                    launch { runCatching { onvif() } }
                }
            } finally {
                runCatching { lock?.release() }
            }
        }

        private suspend fun mdns() {
            val nsd = context.getSystemService(NsdManager::class.java) ?: return
            Mdns(nsd).browse(MDNS_TYPES, DISCOVERY_MS) { hit ->
                merge(hit.ip) { it.copy(name = it.name ?: hit.name, mdnsServices = it.mdnsServices + hit.type) }
                report(0f)
            }
        }

        /** UPnP: ask who is here, then read each device's description for its name, maker and model. */
        private suspend fun ssdp() {
            val locations = LinkedHashMap<String, String>()
            multicast(Banners.SSDP_SEARCH, Banners.SSDP_PORT) { ip, text ->
                merge(ip) { it }
                Banners.header(text, "LOCATION")?.let { locations.putIfAbsent(ip, it) }
            }
            coroutineScope {
                locations.entries.take(MAX_DESCRIPTIONS).forEach { (ip, location) ->
                    launch {
                        val target = Banners.target(location) ?: return@launch
                        val address = LocalOnly.parseIpv4(target.ip) ?: return@launch
                        val xml = gate.withPermit {
                            Tcp.ask(address, target.port, Banners.httpGet(target.ip, target.path), CONNECT_MS, READ_MS, MAX_DESCRIPTION, tcp) { "</device>" in it }
                        } ?: return@launch
                        val upnp = Banners.upnp(xml)
                        merge(ip) { it.copy(name = it.name ?: upnp.name, upnpManufacturer = upnp.manufacturer, upnpModel = upnp.model) }
                    }
                }
            }
        }

        /** ONVIF WS-Discovery: a reply means a network camera or recorder. */
        private fun onvif() {
            multicast(Banners.onvifProbe(UUID.randomUUID().toString()), Banners.ONVIF_PORT) { ip, text ->
                if (Banners.isOnvif(text)) merge(ip) { it.copy(onvif = true, name = it.name ?: Banners.onvifName(text)) }
            }
        }

        /** Sends [message] to the discovery group and hands every reply to [onReply] until [DISCOVERY_MS] is over. */
        private fun multicast(message: String, port: Int, onReply: (ip: String, text: String) -> Unit) {
            val group: InetAddress = LocalOnly.parseIpv4(Banners.GROUP) ?: return
            LocalOnly.datagramSocket(RECEIVE_MS, udp).use { socket ->
                val data = message.toByteArray(Charsets.UTF_8)
                repeat(SENDS) { LocalOnly.send(socket, data, group, port) }
                val buffer = ByteArray(MAX_PACKET)
                val end = System.nanoTime() + DISCOVERY_MS * 1_000_000
                while (System.nanoTime() < end) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (e: SocketTimeoutException) {
                        continue
                    }
                    val from = packet.address as? Inet4Address ?: continue
                    if (!LocalOnly.isLocal(from)) continue
                    onReply(from.hostAddress ?: continue, String(packet.data, 0, packet.length, Charsets.UTF_8))
                    report(0f)
                }
            }
        }
    }

    private companion object {
        /** CLAUDE.md › Behaviour rules: ≤ 64 sockets at once, 400 ms connect timeout, finished in under 20 s. */
        const val MAX_SOCKETS = 64
        const val CONNECT_MS = 400
        const val BUDGET_MS = 19_000L
        const val READ_MS = 1_200
        const val DISCOVERY_MS = 3_500L
        const val RECEIVE_MS = 400
        const val SENDS = 2
        const val MAX_PACKET = 8 * 1024
        const val MAX_DESCRIPTIONS = 32
        const val MAX_DESCRIPTION = 32 * 1024
        const val TLS_PORT = 443
        const val LOCK_TAG = "keyhole:scan"
        /** Knocked on first, on every address: the web page, video and one camera port. */
        val FIRST_PORTS = listOf(80, 554, 8080)
        /** The share of the progress ring for finding devices, and for their other ports; the rest is reading answers. */
        const val FIND_SHARE = 0.55f
        const val PORT_SHARE = 0.15f
        /** docs/SCANNING.md § Wi-Fi, step 3, plus the two that tell a TV and a printer apart from a camera. */
        val MDNS_TYPES = listOf("_rtsp._tcp", "_http._tcp", "_onvif._tcp", "_hap._tcp", "_googlecast._tcp", "_airplay._tcp", "_ipp._tcp")
    }
}
