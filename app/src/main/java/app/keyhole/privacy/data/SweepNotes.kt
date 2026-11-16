package app.keyhole.privacy.data

import app.keyhole.privacy.domain.Check
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
class SweepNotes {
    private val _photos = MutableStateFlow<List<ProofPhoto>>(emptyList())
    private val _magnet = MutableStateFlow<List<MagnetNote>>(emptyList())
    private val _ran = MutableStateFlow<Set<Check>>(emptySet())

    val photos: StateFlow<List<ProofPhoto>> = _photos.asStateFlow()
    val magnet: StateFlow<List<MagnetNote>> = _magnet.asStateFlow()

    /** The hands-on checks that really ran (the camera showed a picture, the magnet learned the room): the report lists the rest under "Couldn't check". */
    val ran: StateFlow<Set<Check>> = _ran.asStateFlow()

    fun markRan(check: Check) = _ran.update { it + check }

    fun addPhoto(photo: ProofPhoto) = _photos.update { it + photo }

    fun addMagnet(note: MagnetNote) = _magnet.update { it + note }
}
