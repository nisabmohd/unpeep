package app.unpeep.privacy.scan.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.core.util.size
import app.unpeep.privacy.domain.BleAdvert
import app.unpeep.privacy.domain.shortUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Bluetooth advertisements nearby (docs/SCANNING.md § Bluetooth). It only listens: it never connects to, pairs
 * with or writes to a device. The scan runs for as long as the flow is collected and stops with it.
 * The caller has checked `Access` first.
 */
class BleScanner(context: Context) {
    private val context = context.applicationContext

    /** Every advertisement heard, as it arrives. Ends with an exception when Android refuses the scan. */
    @SuppressLint("MissingPermission") // The session asks `Access.granted` before it collects.
    fun adverts(): Flow<BleAdvert> = callbackFlow {
        val scanner = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            close()
            return@callbackFlow
        }
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) { trySend(advert(result)) }
            override fun onBatchScanResults(results: MutableList<ScanResult>) { results.forEach { trySend(advert(it)) } }
            override fun onScanFailed(errorCode: Int) { close() }
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val started = runCatching { scanner.startScan(null, settings, callback) }.isSuccess
        if (!started) close()
        awaitClose { runCatching { scanner.stopScan(callback) } }
    }

    private fun advert(result: ScanResult): BleAdvert {
        val record = result.scanRecord
        val services = buildSet {
            record?.serviceUuids?.forEach { shortUuid(it.uuid.toString())?.let(::add) }
            record?.serviceData?.keys?.forEach { shortUuid(it.uuid.toString())?.let(::add) }
        }
        val manufacturer = buildMap {
            val data = record?.manufacturerSpecificData
            if (data != null) for (i in 0 until data.size) put(data.keyAt(i), data.valueAt(i) ?: ByteArray(0))
        }
        return BleAdvert(result.device?.address.orEmpty(), record?.deviceName, result.rssi, services, manufacturer)
    }
}
