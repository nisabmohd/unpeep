package app.keyhole.privacy.ui.lens

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.data.ProofPhoto
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.data.SweepNotes
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.sweepSteps
import app.keyhole.privacy.optics.CameraProblem
import app.keyhole.privacy.optics.CameraSettings
import app.keyhole.privacy.optics.CameraSpec
import app.keyhole.privacy.optics.Facing
import app.keyhole.privacy.optics.FrameListener
import app.keyhole.privacy.optics.GlintFinder
import app.keyhole.privacy.optics.GlintTuning
import app.keyhole.privacy.optics.PhotoStore
import app.keyhole.privacy.optics.Spot
import app.keyhole.privacy.optics.SpotTracker
import app.keyhole.privacy.optics.upright
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the preview is shown: as it is, red (glints stand out), or with more contrast. */
enum class LensFilter { NONE, RED, CONTRAST }

enum class CameraStatus { STARTING, RUNNING, NEEDS_PERMISSION, IN_USE, FAILED, NO_CAMERA }

/** Something the screen should say once: a buzz, a line that goes away. */
enum class LensEvent { PHOTO_SAVED, PHOTO_FAILED, SPOT_FOUND, CAMERA_PROBLEM }

@Immutable
data class LensState(
    /** [Check.LENS] or [Check.INFRARED]. */
    val mode: Check = Check.LENS,
    val filter: LensFilter = LensFilter.NONE,
    val zoom2x: Boolean = false,
    /** The user's torch choice; it only lights on a camera that has one, in Lens mode. */
    val torch: Boolean = true,
    val status: CameraStatus = CameraStatus.STARTING,
    val spec: CameraSpec? = null,
    /** Steady spots, upright and as a share of the picture (0..1). */
    val spots: List<Spot> = emptyList(),
    /** Photos taken with this finder, for the report. */
    val photos: Int = 0,
    /** Null until the TV-remote test was done once. */
    val seesInfrared: Boolean? = null,
    val sweep: Boolean = false,
    /** The step after this one in a sweep; null when it is the last or the check ran alone. */
    val next: Check? = null,
    /** Bumped by Try again, so the camera opens afresh. */
    val attempt: Int = 0,
) {
    val torchOn: Boolean get() = torch && mode == Check.LENS && spec?.hasTorch == true
    val camera: CameraSettings get() = CameraSettings(torch = torchOn, zoom2x = zoom2x && spec?.canZoom2x == true, brighter = mode == Check.INFRARED)
    val facing: Facing get() = if (mode == Check.INFRARED) Facing.FRONT else Facing.BACK
}

/**
 * 08 Lens finder and Infrared. The camera itself lives with the screen (it needs the view); this keeps the choices,
 * reads the frames (on the camera's thread: [onFrame]) and saves the photos.
 */
class LensViewModel(
    private val place: Place, private val sweep: Boolean, start: Check,
    private val settings: SettingsRepository, private val notes: SweepNotes, private val store: PhotoStore, private val now: () -> Long = System::currentTimeMillis,
) : ViewModel(), FrameListener {
    private val inner = MutableStateFlow(LensState(mode = if (start == Check.INFRARED) Check.INFRARED else Check.LENS, sweep = sweep))
    private val _events = MutableStateFlow<Pair<Int, LensEvent>?>(null)

    /** The latest one-off event with a running number, so the same event twice is still seen twice. */
    val events: StateFlow<Pair<Int, LensEvent>?> = _events

    val state: StateFlow<LensState> = combine(inner, settings.settings, notes.photos) { i, s, photos ->
        val steps = sweepSteps(place, s.stepOrder, s.stepsOff)
        i.copy(
            photos = photos.count { it.check == i.mode },
            seesInfrared = s.seesInfrared,
            next = if (sweep) steps.getOrNull(steps.indexOf(i.mode) + 1) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inner.value)

    private fun emit(e: LensEvent) = _events.update { (it?.first ?: 0).plus(1) to e }

    /**
     * Lens or Infrared. The camera restarts only when the other mode needs another camera ([onSpec]); a phone with
     * one camera keeps it running and only the reading changes.
     */
    fun setMode(mode: Check) {
        tracker.clear()
        inner.update { if (it.mode == mode) it else it.copy(mode = mode, spots = emptyList()) }
        if (inner.value.status == CameraStatus.RUNNING) notes.markRan(mode)
    }
    fun setFilter(filter: LensFilter) = inner.update { it.copy(filter = filter) }
    fun toggleZoom() = inner.update { it.copy(zoom2x = !it.zoom2x) }
    fun toggleTorch() = inner.update { it.copy(torch = !it.torch) }
    fun retry() = inner.update { it.copy(status = CameraStatus.STARTING, attempt = it.attempt + 1) }

    fun setGranted(granted: Boolean) = inner.update {
        when {
            !granted -> it.copy(status = CameraStatus.NEEDS_PERMISSION, spots = emptyList())
            it.status == CameraStatus.NEEDS_PERMISSION -> it.copy(status = CameraStatus.STARTING)
            else -> it
        }
    }

    /** The camera picked for the mode; a different one means the camera restarts. */
    fun onSpec(spec: CameraSpec?) {
        tracker.clear()
        inner.update {
            when {
                spec == null -> it.copy(spec = null, status = CameraStatus.NO_CAMERA, spots = emptyList())
                spec == it.spec -> it
                it.status == CameraStatus.NEEDS_PERMISSION -> it.copy(spec = spec)
                else -> it.copy(spec = spec, status = CameraStatus.STARTING, spots = emptyList())
            }
        }
    }

    fun onRunning() {
        inner.update { if (it.status == CameraStatus.NEEDS_PERMISSION) it else it.copy(status = CameraStatus.RUNNING) }
        notes.markRan(inner.value.mode)
    }

    fun onProblem(problem: CameraProblem) {
        inner.update {
            it.copy(
                spots = emptyList(),
                status = when (problem) {
                    CameraProblem.NO_CAMERA -> CameraStatus.NO_CAMERA
                    CameraProblem.IN_USE -> CameraStatus.IN_USE
                    CameraProblem.FAILED -> CameraStatus.FAILED
                },
            )
        }
        emit(LensEvent.CAMERA_PROBLEM)
    }

    /** The TV-remote test's answer; kept, so it is asked once. */
    fun setSeesInfrared(sees: Boolean) {
        viewModelScope.launch { settings.setSeesInfrared(sees) }
    }

    /** "Test again": forget the answer, so the TV-remote question comes back. */
    fun resetInfraredTest() {
        viewModelScope.launch { settings.clearSeesInfrared() }
    }

    // --- Frames: the camera's thread ----------------------------------------------------------------

    private val finder = GlintFinder()
    private val tracker = SpotTracker()

    override fun onFrame(width: Int, height: Int, read: (x: Int, y: Int) -> Int) {
        val s = inner.value
        val spec = s.spec ?: return
        val found = finder.find(width, height, if (s.mode == Check.INFRARED) GlintTuning.INFRARED else GlintTuning.LENS, read)
        val steady = tracker.add(found).map { spot ->
            val (x, y) = upright(spot.x, spot.y, spec.sensorOrientation, mirror = spec.facing == Facing.FRONT)
            spot.copy(x = x, y = y)
        }
        val before = inner.value.spots.size
        inner.update { if (it.mode == s.mode && it.spec == spec) it.copy(spots = steady) else it }
        if (steady.size > before) emit(LensEvent.SPOT_FOUND)
    }

    // --- Photos -------------------------------------------------------------------------------------

    /** Saves what the screen shows; the file stays in app storage and joins the report. */
    fun onPhoto(bitmap: Bitmap?) {
        val mode = inner.value.mode
        viewModelScope.launch {
            val at = now()
            val file = bitmap?.let { store.save(it, at) }
            bitmap?.recycle()
            if (file == null) {
                emit(LensEvent.PHOTO_FAILED)
            } else {
                notes.addPhoto(ProofPhoto(file.path, mode, at))
                emit(LensEvent.PHOTO_SAVED)
            }
        }
    }
}
