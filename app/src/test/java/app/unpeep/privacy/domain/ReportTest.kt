package app.unpeep.privacy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a report says: findings, marked as known, and always what couldn't be checked. Never "safe". */
class ReportTest {
    private val camera = WifiDevice(DeviceEvidence("192.168.1.23", name = "IPC-3F2A", rtspPorts = setOf(554)), Verdict(Risk.LIKELY_CAMERA, listOf(Sign.Onvif, Sign.Rtsp(554)), "Xiongmai"), 0)
    private val web = WifiDevice(DeviceEvidence("192.168.1.41"), Verdict(Risk.CHECK_THIS, listOf(Sign.WebPage)), 0)
    private val tv = WifiDevice(DeviceEvidence("192.168.1.19", name = "Bravia"), Verdict(Risk.KNOWN, listOf(Sign.KnownKind("tv")), kind = "tv"), 0)
    private val markedCamera = camera.copy(verdict = Verdict(Risk.KNOWN, listOf(Sign.MarkedByUser, Sign.Onvif, Sign.Rtsp(554)), "Xiongmai"), knownAs = KnownAs.MINE)
    private val v380 = NearbyDevice(Check.HOTSPOTS, "a", "V380-1", -48, NearbyKind.CAMERA_HOTSPOT, Risk.LIKELY_CAMERA, "V380")
    private val cafe = NearbyDevice(Check.HOTSPOTS, "b", "Cafe", -70, NearbyKind.NETWORK, Risk.UNKNOWN)
    private val tile = NearbyDevice(Check.BLUETOOTH, "AA", null, -60, NearbyKind.TRACKER, Risk.CHECK_THIS, "Tile")

    private fun visit(ran: Set<Check>, block: Visit.() -> Visit = { this }) =
        Visit(Place.STAY, 1L, Place.STAY.checks(), ran).block()

    @Test fun likelyCamerasAndWorthALookAreFoundKnownDevicesAreNot() {
        val r = buildReport(visit(setOf(Check.WIFI)) { copy(wifi = listOf(tv, web, camera)) })
        assertEquals(listOf("IPC-3F2A", "192.168.1.41"), r.findings.map { it.title })
        assertEquals(listOf(Risk.LIKELY_CAMERA, Risk.CHECK_THIS), r.findings.map { it.risk })
        assertEquals(listOf("ONVIF", "RTSP:554"), r.findings.first().signs)
        assertEquals(2, r.toLookAt)
    }

    @Test fun aMarkedDeviceIsListedAsKnownWithItsSignsAndCanBeUndone() {
        val r = buildReport(visit(setOf(Check.WIFI)) { copy(wifiName = "SeaView", wifi = listOf(markedCamera)) })
        val f = r.findings.single()
        assertEquals(KnownAs.MINE, f.knownAs)
        assertFalse(f.toLookAt)
        assertEquals(Risk.LIKELY_CAMERA, f.risk)
        assertEquals("SeaView|IPC-3F2A", f.markKey)
        assertFalse("MARKED" in f.signs)
        assertEquals(0, r.toLookAt)
    }

    @Test fun nothingFromAScanThatDidNotRun() {
        val r = buildReport(visit(setOf(Check.LENS)) { copy(wifi = listOf(camera), nearby = listOf(v380, tile)) })
        assertTrue(r.findings.isEmpty())
    }

    @Test fun hotspotsAndTrackersAreFoundAndAPlainNetworkOnlyWhenAdded() {
        val r = buildReport(visit(setOf(Check.HOTSPOTS, Check.BLUETOOTH)) { copy(nearby = listOf(cafe, tile, v380, cafe.copy(id = "c", name = "Added", added = true))) })
        assertEquals(listOf("V380-1", "Added", "Tile"), r.findings.map { it.title })
        assertEquals(listOf(FindingSource.HOTSPOT, FindingSource.HOTSPOT, FindingSource.BLUETOOTH), r.findings.map { it.source })
        assertEquals("Tile", r.findings.last().label)
    }

    @Test fun checklistNotSureIsFoundWithItsNoteAndPhotoLooksFineAndSkipAreNot() {
        val answers = mapOf(
            ChecklistItem.MIRROR_LIGHT to ChecklistEntry(ChecklistAnswer.LOOKS_FINE),
            ChecklistItem.HOOKS to ChecklistEntry(ChecklistAnswer.NOT_SURE, "  loose hook  ", "/p.jpg"),
            ChecklistItem.VENTS to ChecklistEntry(ChecklistAnswer.SKIPPED),
        )
        val r = buildReport(visit(setOf(Check.CHECKLIST)) { copy(checklist = answers) })
        val f = r.findings.single()
        assertEquals(FindingSource.CHECKLIST, f.source)
        assertEquals("HOOKS", f.title)
        assertEquals("loose hook", f.note)
        assertEquals(1, f.photos)
    }

    @Test fun anAddedMagnetReadingKeepsItsNumbers() {
        val f = buildReport(visit(setOf(Check.MAGNET)) { copy(magnet = listOf(MagnetReading(14, 48))) }).findings.single()
        assertEquals(FindingSource.MAGNET, f.source)
        assertEquals("14", f.detail)
        assertEquals("48", f.note)
    }

    @Test fun everyCheckThatDidNotRunIsInCouldntCheck() {
        val r = buildReport(visit(setOf(Check.WIFI, Check.HOTSPOTS, Check.BLUETOOTH, Check.LENS, Check.MAGNET, Check.CHECKLIST)))
        assertEquals(listOf<Gap>(Gap.NotRun(Check.INFRARED), Gap.OtherNetwork, Gap.MemoryCard), r.gaps)
        assertEquals(6, r.ran.size)
    }

    @Test fun couldntCheckIsNeverEmptyEvenWhenEverythingRan() {
        val r = buildReport(visit(Check.entries.toSet()))
        assertTrue(Gap.MemoryCard in r.gaps)
        assertTrue(r.gaps.none { it is Gap.NotRun })
    }

    @Test fun aSweepWhereNothingRanStillHasAReport() {
        val r = buildReport(visit(emptySet()))
        assertEquals(Place.STAY.checks().size, r.gaps.count { it is Gap.NotRun })
        assertEquals(0, r.toLookAt)
        assertTrue(r.ran.isEmpty())
    }

    @Test fun isolationAndBlockedInfraredAreSaid() {
        val r = buildReport(visit(setOf(Check.WIFI, Check.INFRARED)) { copy(wifiIsolated = true, seesInfrared = false) })
        assertTrue(Gap.Isolated in r.gaps)
        assertTrue(Gap.NoInfrared in r.gaps)
    }

    @Test fun isolationIsNotSaidWhenWifiDidNotRun() {
        assertFalse(Gap.Isolated in buildReport(visit(setOf(Check.LENS)) { copy(wifiIsolated = true) }).gaps)
    }

    @Test fun theCheckRowFollowsThePlannedOrder() {
        val planned = listOf(Check.MAGNET, Check.WIFI, Check.LENS)
        val r = buildReport(Visit(Place.OFFICE, 1L, planned, setOf(Check.LENS, Check.MAGNET)))
        assertEquals(listOf(Check.MAGNET, Check.LENS), r.ran)
        assertEquals(listOf<Gap>(Gap.NotRun(Check.WIFI), Gap.MemoryCard), r.gaps)
    }

    @Test fun signsSurviveStorage() {
        val signs = listOf(Sign.Onvif, Sign.Rtsp(554), Sign.CameraMaker("Hik:vision"), Sign.CameraPort(37777), Sign.WebPage, Sign.KnownKind("tv"), Sign.MarkedByUser, Sign.Router, Sign.ThisPhone)
        assertEquals(signs, signCodes(signs).map { parseSign(it) })
        assertNull(parseSign("SOMETHING_NEW"))
        assertNull(parseSign("RTSP:abc"))
    }

    // --- Checklist --------------------------------------------------------------------------------

    @Test fun everyPlaceHasAChecklistWithNoItemTwice() {
        for (place in Place.entries) {
            val items = checklistItems(place)
            assertTrue("$place", items.isNotEmpty())
            assertEquals("$place", items.size, items.toSet().size)
        }
    }

    @Test fun theTrialRoomChecklistIsShortAndStartsWithTheMirror() {
        assertEquals(7, checklistItems(Place.TRIAL_ROOM).size)
        assertEquals(ChecklistSection.MIRROR, checklist(Place.TRIAL_ROOM).first().first)
        assertEquals(ChecklistItem.MIRROR_LIGHT, checklistItems(Place.TRIAL_ROOM).first())
    }

    @Test fun aCarHasNoMirrorLightTest() {
        assertFalse(ChecklistItem.MIRROR_LIGHT in checklistItems(Place.CAR))
    }

    @Test fun skippedCountsAsDoneAndOtherPlacesItemsDoNot() {
        val answers = mapOf(
            ChecklistItem.MIRROR_LIGHT to ChecklistEntry(ChecklistAnswer.SKIPPED),
            ChecklistItem.HOOKS to ChecklistEntry(ChecklistAnswer.LOOKS_FINE),
            ChecklistItem.CAR_SEATS to ChecklistEntry(ChecklistAnswer.LOOKS_FINE),
        )
        assertEquals(2, checklistDone(Place.TRIAL_ROOM, answers))
        assertEquals(0, checklistDone(Place.OFFICE, answers))
    }

    @Test fun everyItemIsUsedSomewhere() {
        val used = Place.entries.flatMap(::checklistItems).toSet()
        assertEquals(ChecklistItem.entries.toSet(), used)
    }
}
