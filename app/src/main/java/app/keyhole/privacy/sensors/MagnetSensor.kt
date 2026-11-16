package app.keyhole.privacy.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.math.sqrt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The phone's magnetometer (docs/SCANNING.md § Magnet): `TYPE_MAGNETIC_FIELD`, no permission. It listens only while
 * [fields] is collected and unregisters with it, so nothing runs when the screen is gone.
 */
class MagnetSensor(context: Context) {
    private val manager: SensorManager? = context.applicationContext.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /** False on a phone with no magnetometer: the check can't run, and the report says so. */
    val available: Boolean get() = sensor != null

    /** The strength of the field in µT, as fast as the sensor gives it at UI rate. */
    fun fields(): Flow<Float> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                trySend(sqrt(x * x + y * y + z * z))
            }
        }
        val registered = sensor != null && manager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI) == true
        if (!registered) close()
        awaitClose { manager?.unregisterListener(listener) }
    }
}

/**
 * Turns the sensor's fast stream into one steady reading per [windowMs]: the average of what arrived in that time.
 * One reading is one bar on the screen, so 24 bars are about ten seconds.
 */
class MagnetSampler(private val windowMs: Long = WINDOW_MS) {
    private var sum = 0.0
    private var count = 0
    private var windowStart = -1L

    /** Adds a field strength heard at [at]; returns the window's average when the window is full, else null. */
    fun add(magnitude: Float, at: Long): Float? {
        if (windowStart < 0 || at < windowStart) windowStart = at
        sum += magnitude
        count++
        if (at - windowStart < windowMs) return null
        val average = (sum / count).toFloat()
        sum = 0.0
        count = 0
        windowStart = at
        return average
    }

    fun reset() {
        sum = 0.0
        count = 0
        windowStart = -1
    }

    companion object {
        const val WINDOW_MS = 400L
    }
}

/** The optional beep on a jump (More › Magnet, the screen's sound button). One short tone, never a siren. */
class Beeper {
    private var tone: ToneGenerator? = null

    fun beep() {
        runCatching {
            val t = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME).also { tone = it }
            t.startTone(ToneGenerator.TONE_PROP_BEEP, MS)
        }
    }

    fun release() {
        runCatching { tone?.release() }
        tone = null
    }

    private companion object {
        const val VOLUME = 60
        const val MS = 120
    }
}
