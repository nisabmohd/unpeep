package app.keyhole.privacy.scan.net

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.net.Inet4Address
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * mDNS through Android's own `NsdManager` (docs/SCANNING.md § Wi-Fi, step 3): which devices announce a service, and
 * under what name. Android does the asking; Keyhole only listens for as long as the scan runs.
 */
class Mdns(private val nsd: NsdManager) {
    data class Hit(val ip: String, val name: String?, val type: String)

    /** Listens for [types] for [millis], calling [onHit] for each device that could be resolved to a local address. */
    suspend fun browse(types: List<String>, millis: Long, onHit: (Hit) -> Unit) {
        val found = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        val listeners = types.mapNotNull { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                override fun onServiceLost(info: NsdServiceInfo) = Unit
                override fun onServiceFound(info: NsdServiceInfo) { found.trySend(info) }
            }
            runCatching { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener) }.map { listener }.getOrNull()
        }
        try {
            // One at a time: older Android versions refuse a second resolve while one is running.
            withTimeoutOrNull(millis) { for (info in found) resolve(info)?.let(onHit) }
        } finally {
            listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
            found.close()
        }
    }

    @Suppress("DEPRECATION") // resolveService / host: the replacements need API 34; this works on every version.
    private suspend fun resolve(info: NsdServiceInfo): Hit? = withTimeoutOrNull(RESOLVE_MS) {
        suspendCancellableCoroutine { cont ->
            val listener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { if (cont.isActive) cont.resume(null) }
                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    val host = resolved.host as? Inet4Address
                    val ip = host?.takeIf { LocalOnly.isLocal(it) }?.hostAddress
                    val type = (info.serviceType ?: resolved.serviceType).orEmpty().trim('.')
                    if (cont.isActive) cont.resume(ip?.let { Hit(it, resolved.serviceName?.takeIf { n -> n.isNotBlank() }, type) })
                }
            }
            runCatching { nsd.resolveService(info, listener) }.onFailure { if (cont.isActive) cont.resume(null) }
        }
    }

    private companion object {
        const val RESOLVE_MS = 1_200L
    }
}
