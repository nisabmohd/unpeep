package app.keyhole.privacy.scan

import android.Manifest
import app.keyhole.privacy.domain.Check
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What each check asks Android for, per version (docs/PLAN.md § Permissions). Location: the Hotspots check only. */
class AccessTest {
    @Test fun hotspotsAskForLocationOnEveryVersionBecauseAndroidListsWifiNamesToNoAppWithoutIt() {
        // From Android 12 "approximate" must be asked for together with "precise", or the request is ignored.
        for (sdk in 31..36) assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), Access.permissions(Check.HOTSPOTS, sdk))
        for (sdk in 26..30) assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), Access.permissions(Check.HOTSPOTS, sdk))
    }

    @Test fun approximateLocationAloneIsNotEnoughForHotspots() {
        for (sdk in 26..36) assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), Access.required(Check.HOTSPOTS, sdk))
        assertEquals(listOf(Access.BLUETOOTH_SCAN), Access.required(Check.BLUETOOTH, 34))
    }

    @Test fun bluetoothAsksForScanFromAndroid12AndLocationBefore() {
        assertEquals(listOf(Access.BLUETOOTH_SCAN), Access.permissions(Check.BLUETOOTH, 31))
        assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), Access.permissions(Check.BLUETOOTH, 30))
    }

    @Test fun theCameraChecksAskForTheCameraOnly() {
        assertEquals(listOf(Manifest.permission.CAMERA), Access.permissions(Check.LENS, 36))
        assertEquals(listOf(Manifest.permission.CAMERA), Access.permissions(Check.INFRARED, 26))
    }

    @Test fun wifiMagnetAndChecklistAskForNothing() {
        for (check in listOf(Check.WIFI, Check.MAGNET, Check.CHECKLIST)) for (sdk in 26..36) assertTrue(Access.permissions(check, sdk).isEmpty())
    }

    @Test fun onlyHotspotsEverAskForLocationFromAndroid12() {
        for (check in Check.entries - Check.HOTSPOTS) for (sdk in 31..36) assertFalse("$check on $sdk", Access.permissions(check, sdk).any { "LOCATION" in it })
    }

    @Test fun locationMustBeOnOnlyWhereScansCountAsLocation() {
        for (sdk in 26..36) assertTrue(Access.needsLocationOn(Check.HOTSPOTS, sdk))
        assertTrue(Access.needsLocationOn(Check.BLUETOOTH, 30))
        assertFalse(Access.needsLocationOn(Check.BLUETOOTH, 31))
        assertFalse(Access.needsLocationOn(Check.WIFI, 26))
    }
}
