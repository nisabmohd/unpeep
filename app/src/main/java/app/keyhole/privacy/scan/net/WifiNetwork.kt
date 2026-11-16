package app.keyhole.privacy.scan.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address

/** The Wi-Fi the phone is on: its own address, the router's, the name if Android tells it, and the network itself. */
class WifiLink(val ip: String, val gateway: String?, val ssid: String?, val network: Network)

/** Reads the current Wi-Fi from `ConnectivityManager` › `LinkProperties` (docs/SCANNING.md § Wi-Fi, step 1). */
object WifiNetwork {
    fun current(context: Context): WifiLink? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        // The Wi-Fi even when mobile data is the phone's default (a guest Wi-Fi with a sign-in page).
        @Suppress("DEPRECATION")
        val candidates = listOfNotNull(cm.activeNetwork) + cm.allNetworks
        for (network in candidates.distinct()) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
            val link = cm.getLinkProperties(network) ?: continue
            val ip = link.linkAddresses.map { it.address }.filterIsInstance<Inet4Address>().firstOrNull { LocalOnly.isLocal(it) }?.hostAddress ?: continue
            val gateway = link.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress
            return WifiLink(ip, gateway, ssid(context, caps), network)
        }
        return null
    }

    /** The Wi-Fi's name. Android hides it from apps without location access, so it is often null; that's fine. */
    private fun ssid(context: Context, caps: NetworkCapabilities): String? {
        val info: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            caps.transportInfo as? WifiInfo
        } else {
            @Suppress("DEPRECATION")
            context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo
        }
        val name = info?.ssid?.removeSurrounding("\"")?.trim()
        return name?.takeIf { it.isNotEmpty() && it != UNKNOWN_SSID }
    }

    private const val UNKNOWN_SSID = "<unknown ssid>"
}
