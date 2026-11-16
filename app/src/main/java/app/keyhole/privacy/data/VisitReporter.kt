package app.keyhole.privacy.data

import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.MagnetReading
import app.keyhole.privacy.domain.Visit
import app.keyhole.privacy.domain.buildReport
import app.keyhole.privacy.domain.sweepSteps
import app.keyhole.privacy.scan.NearbySession
import app.keyhole.privacy.scan.WifiSession
import kotlinx.coroutines.flow.first

/**
 * Turns what this visit collected into a saved report (docs/PLAN.md § 2.7), then starts a fresh visit. Only checks
 * that ran in this visit count: an older scan's results never slip into a new report.
 */
class VisitReporter(
    private val notes: SweepNotes, private val wifi: WifiSession, private val hotspots: NearbySession<*>, private val bluetooth: NearbySession<*>,
    private val settings: SettingsRepository, private val reports: ReportRepository, private val now: () -> Long = System::currentTimeMillis,
) {
    /** Stop without a report: nothing is saved, and the next check starts a new visit. */
    fun drop() = notes.drop()

    /** Saves the report and returns its id. */
    suspend fun save(): Long {
        val s = settings.settings.first()
        val place = notes.place.value ?: s.place
        val checklist = notes.checklist.value
        val ran = notes.ran.value + if (checklist.isNotEmpty()) setOf(Check.CHECKLIST) else emptySet()
        val w = wifi.state.value
        val visit = Visit(
            place = place,
            startedAt = notes.started.takeIf { it > 0 } ?: now(),
            // Every check the place has, in the user's order; the ones switched off show as not run.
            planned = sweepSteps(place, s.stepOrder, emptySet()),
            ran = ran,
            wifiName = w.ssid,
            wifi = w.devices,
            wifiIsolated = w.isolated,
            nearby = hotspots.state.value.devices + bluetooth.state.value.devices,
            checklist = checklist,
            magnet = notes.magnet.value.map { MagnetReading(it.above, it.normal) },
            photos = notes.photos.value.map { it.path } + checklist.values.mapNotNull { it.photoPath },
            seesInfrared = s.seesInfrared,
        )
        val id = reports.save(buildReport(visit))
        notes.clear()
        return id
    }
}
