package app.keyhole.privacy.data

import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.ChecklistEntry
import app.keyhole.privacy.domain.ChecklistItem
import app.keyhole.privacy.domain.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A photo taken with the lens or infrared finder's shutter. The file is in app storage and nowhere else. */
data class ProofPhoto(val path: String, val check: Check, val at: Long)

/** A magnet reading the user added to the report. */
data class MagnetNote(val above: Int, val normal: Int, val at: Long)

/**
 * What the hands-on checks (lens, infrared, magnet) collected for the report, between their screens. In memory
 * only until reports are saved (Room, Week 5); the photo files themselves are in app storage.
 */
class SweepNotes(private val now: () -> Long = System::currentTimeMillis) {
    private val _photos = MutableStateFlow<List<ProofPhoto>>(emptyList())
    private val _magnet = MutableStateFlow<List<MagnetNote>>(emptyList())
    private val _ran = MutableStateFlow<Set<Check>>(emptySet())

    val photos: StateFlow<List<ProofPhoto>> = _photos.asStateFlow()
    val magnet: StateFlow<List<MagnetNote>> = _magnet.asStateFlow()

    /** The hands-on checks that really ran (the camera showed a picture, the magnet learned the room): the report lists the rest under "Couldn't check". */
    val ran: StateFlow<Set<Check>> = _ran.asStateFlow()

    fun markRan(check: Check) = _ran.update { it + check }

    private val _checklist = MutableStateFlow<Map<ChecklistItem, ChecklistEntry>>(emptyMap())
    private val _place = MutableStateFlow<Place?>(null)
    private var startedAt = 0L

    /** The checklist's answers in this visit. */
    val checklist: StateFlow<Map<ChecklistItem, ChecklistEntry>> = _checklist.asStateFlow()

    /** Where this visit is; null before any check ran. */
    val place: StateFlow<Place?> = _place.asStateFlow()

    /** When the visit began: its first check. */
    val started: Long get() = startedAt

    /** True once anything happened that a report could hold. */
    val hasData: Boolean get() = _ran.value.isNotEmpty() || _photos.value.isNotEmpty() || _checklist.value.isNotEmpty() || _magnet.value.isNotEmpty()

    /** A check begins at [place]: the visit starts if it hadn't. Another place starts a fresh visit only once saved. */
    fun begin(place: Place) {
        if (_place.value == null || !hasData) {
            _place.value = place
            startedAt = now()
        }
    }

    fun answer(item: ChecklistItem, entry: ChecklistEntry?) = _checklist.update { if (entry == null) it - item else it + (item to entry) }

    /** After the report is saved: the next check starts a new visit. The photo files now belong to the report. */
    fun clear() {
        _photos.value = emptyList()
        _magnet.value = emptyList()
        _ran.value = emptySet()
        _checklist.value = emptyMap()
        _place.value = null
        startedAt = 0L
    }

    fun addPhoto(photo: ProofPhoto) = _photos.update { it + photo }

    fun addMagnet(note: MagnetNote) = _magnet.update { it + note }
}
