package app.keyhole.privacy.data

import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Finding
import app.keyhole.privacy.domain.FindingSource
import app.keyhole.privacy.domain.Gap
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.ui.reports.groupByWhen
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** How reports are stored, and how the Reports tab groups them. */
class ReportCodesTest {
    @Test fun checksSurviveStorageInOrderAndUnknownOnesAreDropped() {
        val list = listOf(Check.MAGNET, Check.WIFI, Check.CHECKLIST)
        assertEquals(list, ReportCodes.checks(ReportCodes.checks(list)))
        assertEquals(listOf(Check.WIFI), ReportCodes.checks("WIFI,SONAR"))
        assertTrue(ReportCodes.checks("").isEmpty())
    }

    @Test fun everyGapSurvivesStorage() {
        val gaps = Check.entries.map { Gap.NotRun(it) } + listOf(Gap.Isolated, Gap.NoInfrared, Gap.MemoryCard, Gap.OtherNetwork)
        assertEquals(gaps, ReportCodes.gaps(ReportCodes.gaps(gaps)))
        assertEquals(listOf<Gap>(Gap.MemoryCard), ReportCodes.gaps("NEW_KIND,MEMORY_CARD,NOT_RUN:SONAR"))
    }

    @Test fun aFindingSurvivesStorageWithAMakerHoldingAComma() {
        val f = Finding(FindingSource.WIFI, "IPC-3F2A", "192.168.1.23", Risk.LIKELY_CAMERA, listOf("ONVIF", "MAKER:Foo, Inc."), "Foo, Inc.", "x|IPC", KnownAs.ROOM, "", 0)
        val back = ReportCodes.finding(ReportCodes.finding(7, f, 0).copy(id = 3))
        assertEquals(3L, back.id)
        assertEquals(f, back.finding)
    }

    @Test fun aFindingFromANewerVersionStillReads() {
        val e = ReportCodes.finding(1, Finding(FindingSource.MAGNET, "MAGNET"), 0).copy(source = "SONAR", risk = "MAYBE", knownAs = "SOMEONE", signs = "")
        val f = ReportCodes.finding(e).finding
        assertEquals(FindingSource.WIFI, f.source)
        assertEquals(Risk.CHECK_THIS, f.risk)
        assertEquals(null, f.knownAs)
        assertTrue(f.signs.isEmpty())
    }

    private fun report(at: Long, id: Long = at) = SavedReport(id, "", Place.STAY, at, at, emptyList(), emptyList(), null, emptyList())

    @Test fun reportsOfThisWeekComeFirstThenOneGroupPerMonth() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 18, 12, 0) }.timeInMillis
        val day = 24L * 60 * 60 * 1000
        val list = listOf(report(now - 40 * day), report(now - day), report(now - 35 * day), report(now - 20 * day))
        val groups = groupByWhen(list, now, "This week") { at -> Calendar.getInstance().apply { timeInMillis = at }.get(Calendar.MONTH).toString() }
        assertEquals("This week", groups.first().first)
        assertEquals(1, groups.first().second.size)
        assertEquals(groups.flatMap { it.second }.map { it.savedAt }, groups.flatMap { it.second }.map { it.savedAt }.sortedDescending())
        assertEquals(list.size, groups.sumOf { it.second.size })
    }

    @Test fun noReportsNoGroups() {
        assertTrue(groupByWhen(emptyList(), 0L, "This week") { "" }.isEmpty())
    }
}
