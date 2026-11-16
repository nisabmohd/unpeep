package app.keyhole.privacy.data

import app.keyhole.privacy.data.db.FindingEntity
import app.keyhole.privacy.data.db.KeyholeDatabase
import app.keyhole.privacy.data.db.MarkEntity
import app.keyhole.privacy.data.db.PhotoEntity
import app.keyhole.privacy.data.db.ReportEntity
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Finding
import app.keyhole.privacy.domain.FindingSource
import app.keyhole.privacy.domain.Gap
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.ReportDraft
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.optics.PhotoStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.room.withTransaction

/** A saved report as the screens see it. */
data class SavedReport(
    val id: Long,
    val name: String,
    val place: Place,
    val startedAt: Long,
    val savedAt: Long,
    val planned: List<Check>,
    val ran: List<Check>,
    val wifiName: String?,
    val gaps: List<Gap>,
    val findings: List<SavedFinding> = emptyList(),
    val photos: List<String> = emptyList(),
    val toLookAt: Int = 0,
)

/** A finding of a saved report, with its row id (Undo needs it). */
data class SavedFinding(val id: Long, val finding: Finding)

/** A device marked as known, for More › Marked as known. */
data class Mark(val key: String, val knownAs: KnownAs, val label: String, val check: Check, val at: Long)

/** How report fields are stored: enum names and short codes, so a newer version's values never break an older one. */
object ReportCodes {
    fun checks(list: List<Check>): String = list.joinToString(",") { it.name }
    fun checks(text: String): List<Check> = text.split(',').mapNotNull { n -> Check.entries.firstOrNull { it.name == n } }

    fun gap(g: Gap): String = when (g) {
        is Gap.NotRun -> "NOT_RUN:${g.check.name}"
        Gap.Isolated -> "ISOLATED"
        Gap.NoInfrared -> "NO_INFRARED"
        Gap.MemoryCard -> "MEMORY_CARD"
        Gap.OtherNetwork -> "OTHER_NETWORK"
    }

    fun gap(code: String): Gap? = when {
        code.startsWith("NOT_RUN:") -> Check.entries.firstOrNull { it.name == code.removePrefix("NOT_RUN:") }?.let { Gap.NotRun(it) }
        code == "ISOLATED" -> Gap.Isolated
        code == "NO_INFRARED" -> Gap.NoInfrared
        code == "MEMORY_CARD" -> Gap.MemoryCard
        code == "OTHER_NETWORK" -> Gap.OtherNetwork
        else -> null
    }

    fun gaps(list: List<Gap>): String = list.joinToString(",") { gap(it) }
    fun gaps(text: String): List<Gap> = text.split(',').mapNotNull { gap(it) }

    fun finding(reportId: Long, f: Finding, position: Int) = FindingEntity(
        reportId = reportId, source = f.source.name, title = f.title, detail = f.detail, risk = f.risk.name,
        signs = f.signs.joinToString("\n"), label = f.label, markKey = f.markKey, knownAs = f.knownAs?.name, note = f.note, photos = f.photos, position = position,
    )

    fun finding(e: FindingEntity) = SavedFinding(
        e.id,
        Finding(
            FindingSource.entries.firstOrNull { it.name == e.source } ?: FindingSource.WIFI, e.title, e.detail,
            Risk.entries.firstOrNull { it.name == e.risk } ?: Risk.CHECK_THIS, e.signs.split('\n').filter { it.isNotEmpty() }, e.label, e.markKey,
            e.knownAs?.let { k -> KnownAs.entries.firstOrNull { it.name == k } }, e.note, e.photos,
        ),
    )
}

/** Reports and marks on this phone. Deleting a report deletes its photo files too. */
class ReportRepository(private val db: KeyholeDatabase, private val photos: PhotoStore, scope: CoroutineScope, private val now: () -> Long = System::currentTimeMillis) {
    private val reports = db.reports()
    private val marks = db.marks()

    /** Every report, newest first, with how many findings each still has to look at. */
    val all: Flow<List<SavedReport>> = combine(reports.reports(), reports.toLookAt()) { list, counts ->
        val byId = counts.associate { it.reportId to it.count }
        list.map { saved(it).copy(toLookAt = byId[it.id] ?: 0) }
    }

    /** The marks by key, for the scans: they judge every device against them. */
    val marksByKey: StateFlow<Map<String, KnownAs>>

    /** The marks, kept in memory for the scans (they judge every device against them). */
    val markList: StateFlow<List<Mark>> = marks.marks().map { list ->
        list.mapNotNull { m ->
            val k = KnownAs.entries.firstOrNull { it.name == m.knownAs } ?: return@mapNotNull null
            Mark(m.key, k, m.label, Check.entries.firstOrNull { it.name == m.check } ?: Check.WIFI, m.at)
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        marksByKey = markList.map { list -> list.associate { it.key to it.knownAs } }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    }

    /** The app's scope, for writes that must finish after the screen that asked is gone (a toast's Undo). */
    val writes = scope

    fun report(id: Long): Flow<SavedReport?> = combine(reports.report(id), reports.findings(id), reports.photos(id)) { r, f, p ->
        r?.let { e -> saved(e).copy(findings = f.map(ReportCodes::finding), photos = p.map { it.path }, toLookAt = f.count { it.knownAs == null }) }
    }

    /** Saves [draft] as one report, with its findings and photos, all or nothing. */
    suspend fun save(draft: ReportDraft): Long = db.withTransaction {
        val id = reports.insert(
            ReportEntity(
                place = draft.place.name, startedAt = draft.startedAt, savedAt = now(), planned = ReportCodes.checks(draft.planned),
                ran = ReportCodes.checks(draft.ran), wifiName = draft.wifiName, gaps = ReportCodes.gaps(draft.gaps),
            ),
        )
        reports.insertFindings(draft.findings.mapIndexed { i, f -> ReportCodes.finding(id, f, i) })
        reports.insertPhotos(draft.photos.mapIndexed { i, path -> PhotoEntity(reportId = id, path = path, position = i) })
        id
    }

    suspend fun rename(id: Long, name: String) = reports.rename(id, name.trim().take(MAX_NAME))

    /** "Undo" in Marked as known: the finding is to look at again, and the device isn't marked any more. */
    suspend fun undoMark(finding: SavedFinding) {
        reports.setKnownAs(finding.id, null)
        finding.finding.markKey?.let { marks.remove(it) }
    }

    suspend fun mark(key: String, knownAs: KnownAs?, label: String, check: Check) {
        if (knownAs == null) marks.remove(key) else marks.put(MarkEntity(key, knownAs.name, label, check.name, now()))
    }

    /** The same, from a screen that doesn't wait for it ("Not a camera?" sheet): written in the app's scope. */
    fun markLater(key: String, knownAs: KnownAs?, label: String, check: Check) {
        writes.launch { mark(key, knownAs, label, check) }
    }

    /** Finding detail › Remove from report. Returns what was removed, so the toast's Undo can put it back. */
    suspend fun removeFinding(id: Long): FindingEntity? = reports.finding(id)?.also { reports.deleteFinding(id) }

    suspend fun restoreFinding(finding: FindingEntity) = reports.insertFinding(finding)

    suspend fun delete(id: Long) {
        val paths = reports.photoPaths(id)
        reports.delete(id)
        paths.forEach { photos.delete(it) }
    }

    /** Privacy › Delete all reports: every report, every photo, and the marks. */
    suspend fun deleteAll() {
        val paths = reports.allPhotoPaths()
        reports.deleteAll()
        marks.clear()
        paths.forEach { photos.delete(it) }
    }

    private fun saved(e: ReportEntity) = SavedReport(
        e.id, e.name, Place.entries.firstOrNull { it.name == e.place } ?: Place.STAY, e.startedAt, e.savedAt,
        ReportCodes.checks(e.planned), ReportCodes.checks(e.ran), e.wifiName, ReportCodes.gaps(e.gaps),
    )

    private companion object {
        const val MAX_NAME = 80
    }
}
