package app.unpeep.privacy.ui

import app.unpeep.privacy.ui.found.dialUri
import org.junit.Assert.assertEquals
import org.junit.Test

/** Help numbers reach the dialer as written: no digit or # lost, spacing dropped. */
class DialUriTest {
    @Test fun plainNumbers() = assertEquals("tel:112", dialUri("112"))
    @Test fun spacesAndDashesAreDropped() {
        assertEquals("tel:18002550000", dialUri("1800 255 0000"))
        assertEquals("tel:09008844", dialUri("0900-8844"))
    }
    @Test fun hashIsKeptAndEncoded() = assertEquals("tel:%239110", dialUri("#9110"))
}
