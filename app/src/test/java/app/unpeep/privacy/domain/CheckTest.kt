package app.unpeep.privacy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckTest {
    @Test fun thereAreSevenChecks() = assertEquals(7, Check.entries.size)

    @Test fun aStayRunsEveryCheck() = assertEquals(Check.entries, Place.STAY.checks())

    @Test fun aTrialRoomNeedsNoWifi() {
        assertFalse(Check.WIFI in Place.TRIAL_ROOM.checks())
        assertEquals(6, Place.TRIAL_ROOM.checks().size)
    }

    @Test fun everyPlaceHasTheChecklistAndTheLens() {
        for (p in Place.entries) {
            assertTrue("$p", Check.CHECKLIST in p.checks())
            assertTrue("$p", Check.LENS in p.checks())
        }
    }

    @Test fun aStayTakesAboutFiveMinutes() = assertEquals(5, Place.STAY.minutes())

    @Test fun minutesNeverBelowOne() = assertEquals(1, Place.STAY.minutes(listOf(Check.HOTSPOTS)))

    @Test fun stepsFollowTheUsersOrder() {
        val order = listOf(Check.CHECKLIST, Check.WIFI, Check.LENS)
        val steps = sweepSteps(Place.STAY, order)
        assertEquals(listOf(Check.CHECKLIST, Check.WIFI, Check.LENS), steps.take(3))
        assertEquals(7, steps.size)
        assertEquals(steps.size, steps.toSet().size)
    }

    @Test fun switchedOffStepsAreLeftOut() {
        val steps = sweepSteps(Place.STAY, off = setOf(Check.MAGNET, Check.INFRARED))
        assertEquals(5, steps.size)
        assertFalse(Check.MAGNET in steps)
    }

    @Test fun aPlaceNeverRunsAChecksItDoesNotHave() {
        assertFalse(Check.WIFI in sweepSteps(Place.CAR, listOf(Check.WIFI, Check.LENS)))
    }
}
