package app.keyhole.privacy.scan

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.NearbyBlock

/**
 * What Android must allow before a check can list what is nearby (docs/PLAN.md § Permissions). Asked just in time,
 * when the check starts, each with one line why. Location is asked for by the Hotspots check only (and by Bluetooth
 * before Android 12), because Android won't list what is nearby without it; Keyhole never reads a position.
 */
object Access {
    /**
     * The permissions to ask for [check] on this Android version; empty when it needs none.
     * Hotspots: every Android version only hands Wi-Fi scan results to apps with precise location access (Android
     * 13's "nearby Wi-Fi devices" permission does not cover `startScan` / `getScanResults`). From Android 12 the
     * approximate permission must be asked for in the same breath, or Android ignores the request.
     */
    fun permissions(check: Check, sdk: Int = Build.VERSION.SDK_INT): List<String> = when (check) {
        Check.HOTSPOTS -> if (sdk >= Build.VERSION_CODES.S) listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) else listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        // Android 12+: "nearby devices", declared neverForLocation. Before: Bluetooth scans count as location.
        Check.BLUETOOTH -> if (sdk >= Build.VERSION_CODES.S) listOf(BLUETOOTH_SCAN) else listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        Check.LENS, Check.INFRARED -> listOf(Manifest.permission.CAMERA)
        Check.WIFI, Check.MAGNET, Check.CHECKLIST -> emptyList()
    }

    /** The permissions that must be granted for [check] to work: "approximate" alone is not enough for hotspots. */
    fun required(check: Check, sdk: Int = Build.VERSION.SDK_INT): List<String> = permissions(check, sdk) - Manifest.permission.ACCESS_COARSE_LOCATION

    fun granted(context: Context, check: Check): Boolean =
        required(check).all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    /** Why [check] can't list anything right now even with permission, or null when it can. */
    fun block(context: Context, check: Check): NearbyBlock? = when (check) {
        Check.HOTSPOTS -> when {
            context.applicationContext.getSystemService(WifiManager::class.java)?.isWifiEnabled != true -> NearbyBlock.WIFI_OFF
            needsLocationOn(Check.HOTSPOTS) && !locationOn(context) -> NearbyBlock.LOCATION_OFF
            else -> null
        }
        Check.BLUETOOTH -> {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            when {
                adapter == null || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> NearbyBlock.NO_BLUETOOTH
                !adapter.isEnabled -> NearbyBlock.BLUETOOTH_OFF
                needsLocationOn(Check.BLUETOOTH) && !locationOn(context) -> NearbyBlock.LOCATION_OFF
                else -> null
            }
        }
        else -> null
    }

    /** Android returns no Wi-Fi scan results while Location is off; Bluetooth needs it on only before Android 12. */
    fun needsLocationOn(check: Check, sdk: Int = Build.VERSION.SDK_INT): Boolean = when (check) {
        Check.HOTSPOTS -> true
        Check.BLUETOOTH -> sdk < Build.VERSION_CODES.S
        else -> false
    }

    /** `Manifest.permission.BLUETOOTH_SCAN`, which only exists from Android 12; asked for only there. */
    const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"

    private fun locationOn(context: Context): Boolean {
        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) lm.isLocationEnabled else lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}
