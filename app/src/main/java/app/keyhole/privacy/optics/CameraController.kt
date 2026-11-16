package app.keyhole.privacy.optics

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/** Which camera a finder wants: the rear one with its torch (lens), or the front one, which often sees infrared. */
enum class Facing { BACK, FRONT }

/** The camera Keyhole picked, and what it can do. */
data class CameraSpec(
    val id: String,
    val facing: Facing,
    /** Clockwise degrees that turn the sensor's frames upright on a phone held upright. */
    val sensorOrientation: Int,
    val hasTorch: Boolean,
    val preview: Size,
    val analysis: Size,
    val maxZoom: Float,
    val activeArray: Rect?,
    val exposureMax: Int,
) {
    val canZoom2x: Boolean get() = maxZoom >= ZOOM_2X
    /** The preview after turning upright: portrait for a sideways sensor. */
    val uprightWidth: Int get() = if (sensorOrientation % 180 == 0) preview.width else preview.height
    val uprightHeight: Int get() = if (sensorOrientation % 180 == 0) preview.height else preview.width

    companion object {
        const val ZOOM_2X = 2f
    }
}

/** What the finder asks the camera for, changed while it runs. */
data class CameraSettings(val torch: Boolean = false, val zoom2x: Boolean = false, val brighter: Boolean = false)

enum class CameraProblem { NO_CAMERA, IN_USE, FAILED }

/** One analysis frame's brightness, valid only during the callback. */
fun interface FrameListener {
    fun onFrame(width: Int, height: Int, read: (x: Int, y: Int) -> Int)
}

/**
 * The finders' camera through Camera2 (no library: decided 16 Nov 2026, docs/PLAN.md § Stack). It shows a preview
 * on a [SurfaceTexture], hands small brightness frames to a [FrameListener] on its own thread (one at a time: a frame
 * that arrives while the last one is being read is dropped, so the preview never waits), and turns the torch and
 * zoom on and off. Nothing is recorded: the shutter saves what the screen shows (`PhotoStore`).
 */
class CameraController(context: Context) {
    private val manager: CameraManager? = context.applicationContext.getSystemService(CameraManager::class.java)
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var previewSurface: Surface? = null
    private var spec: CameraSpec? = null
    @Volatile private var settings = CameraSettings()
    private val busy = AtomicBoolean(false)

    /** The camera facing [facing], or the other one when the phone has none that way; null without any camera. */
    fun pick(facing: Facing): CameraSpec? {
        val m = manager ?: return null
        val ids = runCatching { m.cameraIdList.toList() }.getOrDefault(emptyList())
        val specs = ids.mapNotNull { id -> runCatching { spec(m, id) }.getOrNull() }
        return specs.firstOrNull { it.facing == facing } ?: specs.firstOrNull()
    }

    private fun spec(m: CameraManager, id: String): CameraSpec? {
        val c = m.getCameraCharacteristics(id)
        val facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> Facing.FRONT
            CameraCharacteristics.LENS_FACING_BACK -> Facing.BACK
            else -> return null
        }
        val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return null
        val previews = map.getOutputSizes(SurfaceTexture::class.java)?.toList().orEmpty()
        val yuv = map.getOutputSizes(ImageFormat.YUV_420_888)?.toList().orEmpty()
        val preview = CameraSizes.preview(previews.map { Dim(it.width, it.height) })?.let { Size(it.width, it.height) } ?: return null
        val analysis = CameraSizes.analysis(yuv.map { Dim(it.width, it.height) }, Dim(preview.width, preview.height))?.let { Size(it.width, it.height) } ?: return null
        val zoom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.upper else null
        return CameraSpec(
            id, facing, c.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0, c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
            preview, analysis, zoom ?: c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f,
            c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE), c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.upper ?: 0,
        )
    }

    @Volatile private var generation = 0
    /** Counted down once the last opened camera has closed. */
    @Volatile private var closing: java.util.concurrent.CountDownLatch? = null

    /**
     * Opens [spec] and starts the preview. [onRunning] or [onProblem] is called once, on the camera's thread.
     * The caller has the camera permission; a missing one is reported as [CameraProblem.FAILED].
     */
    @SuppressLint("MissingPermission")
    fun start(spec: CameraSpec, texture: SurfaceTexture, settings: CameraSettings, frames: FrameListener, onRunning: () -> Unit, onProblem: (CameraProblem) -> Unit) {
        stop()
        // Each start has its own number: a late callback from a camera already closed (switching Lens ↔ Infrared,
        // or back ↔ front) must never report a problem with the camera that replaced it.
        val gen = ++generation
        val running = { if (gen == generation) onRunning() }
        val problem = { p: CameraProblem -> if (gen == generation) onProblem(p) }
        // The camera being closed: the next one opens once it has (some phones can't open two at once).
        val previous = closing
        this.spec = spec
        this.settings = settings
        val t = HandlerThread("keyhole-camera").also { it.start() }
        val h = Handler(t.looper)
        thread = t
        handler = h
        texture.setDefaultBufferSize(spec.preview.width, spec.preview.height)
        val surface = Surface(texture).also { previewSurface = it }
        val images = ImageReader.newInstance(spec.analysis.width, spec.analysis.height, ImageFormat.YUV_420_888, MAX_IMAGES).also { reader = it }
        images.setOnImageAvailableListener({ r ->
            val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            // Still reading the last frame: drop this one.
            if (!busy.compareAndSet(false, true)) {
                image.close()
                return@setOnImageAvailableListener
            }
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val row = plane.rowStride
                val pixel = plane.pixelStride
                val limit = buffer.limit()
                frames.onFrame(image.width, image.height) { x, y ->
                    val i = y * row + x * pixel
                    if (i < limit) buffer.get(i).toInt() and 0xFF else 0
                }
            } catch (e: IllegalStateException) {
                // The camera closed while the frame was read.
            } finally {
                image.close()
                busy.set(false)
            }
        }, h)
        val closed = java.util.concurrent.CountDownLatch(1)
        closing = closed
        h.post {
            runCatching { previous?.await(CLOSE_WAIT_MS, java.util.concurrent.TimeUnit.MILLISECONDS) }
            if (gen != generation) {
                closed.countDown()
                return@post
            }
            try {
                manager?.openCamera(spec.id, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        if (gen != generation) {
                            camera.close()
                            return
                        }
                        device = camera
                        createSession(camera, surface, images.surface, running, problem)
                    }
                    override fun onClosed(camera: CameraDevice) = closed.countDown()
                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        if (device == camera) device = null
                        problem(CameraProblem.IN_USE)
                    }
                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        if (device == camera) device = null
                        problem(if (error == ERROR_CAMERA_IN_USE || error == ERROR_MAX_CAMERAS_IN_USE) CameraProblem.IN_USE else CameraProblem.FAILED)
                    }
                }, h) ?: run { closed.countDown(); problem(CameraProblem.NO_CAMERA) }
            } catch (e: CameraAccessException) {
                closed.countDown(); problem(CameraProblem.IN_USE)
            } catch (e: SecurityException) {
                closed.countDown(); problem(CameraProblem.FAILED)
            } catch (e: IllegalArgumentException) {
                closed.countDown(); problem(CameraProblem.NO_CAMERA)
            }
        }
    }

    @Suppress("DEPRECATION") // createCaptureSession(List): the SessionConfiguration form needs API 28; this works from 26.
    private fun createSession(camera: CameraDevice, preview: Surface, analysis: Surface, onRunning: () -> Unit, onProblem: (CameraProblem) -> Unit) {
        try {
            camera.createCaptureSession(listOf(preview, analysis), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    if (device != camera) {
                        s.close()
                        return
                    }
                    session = s
                    if (repeat()) onRunning() else onProblem(CameraProblem.FAILED)
                }
                override fun onConfigureFailed(s: CameraCaptureSession) = onProblem(CameraProblem.FAILED)
            }, handler)
        } catch (e: CameraAccessException) {
            onProblem(CameraProblem.IN_USE)
        } catch (e: IllegalStateException) {
            onProblem(CameraProblem.FAILED)
        }
    }

    /** Turns the torch, zoom or brightness on or off while the preview runs. */
    fun update(settings: CameraSettings) {
        this.settings = settings
        handler?.post { repeat() }
    }

    private fun repeat(): Boolean {
        val camera = device ?: return false
        val s = session ?: return false
        val spec = spec ?: return false
        val preview = previewSurface ?: return false
        val analysis = reader?.surface ?: return false
        return try {
            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(preview)
                addTarget(analysis)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.FLASH_MODE, if (settings.torch && spec.hasTorch) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF)
                // Infrared: a brighter picture, so faint LEDs show.
                set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, if (settings.brighter) spec.exposureMax else 0)
                val zoom = if (settings.zoom2x && spec.canZoom2x) CameraSpec.ZOOM_2X else 1f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    set(CaptureRequest.CONTROL_ZOOM_RATIO, zoom)
                } else {
                    spec.activeArray?.let { a -> CameraSizes.crop(a.width(), a.height(), zoom).let { b -> set(CaptureRequest.SCALER_CROP_REGION, Rect(b.left, b.top, b.right, b.bottom)) } }
                }
            }.build()
            s.setRepeatingRequest(request, null, handler)
            true
        } catch (e: CameraAccessException) {
            false
        } catch (e: IllegalStateException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /** Closes everything; safe to call more than once and from any thread. */
    fun stop() {
        generation++ // anything the closing camera still says is ignored
        val h = handler
        val t = thread
        val s = session
        val d = device
        val r = reader
        val p = previewSurface
        session = null
        device = null
        reader = null
        previewSurface = null
        handler = null
        thread = null
        if (h != null) {
            h.post {
                runCatching { s?.close() }
                runCatching { d?.close() }
                runCatching { r?.close() }
                runCatching { p?.release() }
                t?.quitSafely()
            }
        } else {
            runCatching { s?.close() }
            runCatching { d?.close() }
            runCatching { r?.close() }
            runCatching { p?.release() }
        }
        busy.set(false)
    }

    companion object {
        private const val MAX_IMAGES = 2
    }
}

/** A width × height, without Android, so the size choices are unit-tested. */
data class Dim(val width: Int, val height: Int) {
    val area: Int get() = width * height
    val shape: Float get() = width.toFloat() / height
}

/** A crop of the sensor: left, top, right, bottom. */
data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** How the finders pick their camera sizes and the 2× crop. */
object CameraSizes {
    /** The largest 4:3 preview up to 1920 (or the largest up to it), so it looks sharp and stays light. */
    fun preview(sizes: List<Dim>): Dim? {
        val fit = sizes.filter { it.width <= MAX_PREVIEW && it.height <= MAX_PREVIEW }
        return fit.filter { abs(it.shape - FOUR_THREE) < SHAPE_SLACK }.maxByOrNull { it.area } ?: fit.maxByOrNull { it.area } ?: sizes.minByOrNull { it.area }
    }

    /** The smallest analysis size of the preview's shape at least 320 wide: enough to see a dot, cheap to read. */
    fun analysis(sizes: List<Dim>, preview: Dim): Dim? {
        val same = sizes.filter { abs(it.shape - preview.shape) < SHAPE_SLACK }
        return same.filter { it.width >= MIN_ANALYSIS }.minByOrNull { it.area } ?: same.maxByOrNull { it.area } ?: sizes.minByOrNull { abs(it.width - MIN_ANALYSIS) }
    }

    /** The centre of a [width] × [height] sensor that shows [zoom] times closer. */
    fun crop(width: Int, height: Int, zoom: Float): Box {
        if (zoom <= 1f) return Box(0, 0, width, height)
        val w = (width / zoom).toInt()
        val h = (height / zoom).toInt()
        val left = (width - w) / 2
        val top = (height - h) / 2
        return Box(left, top, left + w, top + h)
    }

    private const val MAX_PREVIEW = 1920
    private const val MIN_ANALYSIS = 320
    private const val FOUR_THREE = 4f / 3f
    private const val SHAPE_SLACK = 0.02f
}

/** How long a new camera waits for the old one to close before opening anyway. */
private const val CLOSE_WAIT_MS = 1_500L
