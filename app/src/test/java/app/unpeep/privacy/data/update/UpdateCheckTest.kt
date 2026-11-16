package app.unpeep.privacy.data.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The github build's update check: which address it asks, and how it reads the answer. */
class UpdateCheckTest {
    @Test fun versionsCompareNumberByNumber() {
        assertTrue(UpdateCheck.isNewer("0.10.0", "0.9.3"))
        assertTrue(UpdateCheck.isNewer("v1.0", "0.9.9-github"))
        assertTrue(UpdateCheck.isNewer("0.1.1", "0.1"))
        assertFalse(UpdateCheck.isNewer("0.1.0", "0.1.0-github"))
        assertFalse(UpdateCheck.isNewer("0.1.0", "0.2.0"))
        assertFalse(UpdateCheck.isNewer("junk", "0.1.0"))
    }

    @Test fun itAsksTheReposLatestReleaseOnly() = runBlocking {
        var asked = ""
        UpdateCheck("https://github.com/nisabmohd/unpeep", "0.1.0-github") { asked = it; null }.check()
        assertEquals("https://api.github.com/repos/nisabmohd/unpeep/releases/latest", asked)
    }

    @Test fun aNewerReleaseGivesItsPage() = runBlocking {
        val r = UpdateCheck("nisabmohd/unpeep", "0.1.0-github") { """{"tag_name":"v0.2.0","html_url":"https://github.com/nisabmohd/unpeep/releases/tag/v0.2.0","body":"…"}""" }.check()
        assertEquals(UpdateResult.Newer("0.2.0", "https://github.com/nisabmohd/unpeep/releases/tag/v0.2.0"), r)
    }

    @Test fun theSameReleaseIsTheLatest() = runBlocking {
        assertEquals(UpdateResult.Latest("0.1.0"), UpdateCheck("nisabmohd/unpeep", "0.1.0-github") { """{"tag_name":"v0.1.0","html_url":"x"}""" }.check())
    }

    @Test fun noAnswerOrABadAnswerFails() = runBlocking {
        assertEquals(UpdateResult.Failed, UpdateCheck("a/b", "0.1.0") { null }.check())
        assertEquals(UpdateResult.Failed, UpdateCheck("a/b", "0.1.0") { "<html>rate limited</html>" }.check())
        assertEquals(UpdateResult.Failed, UpdateCheck("a/b", "0.1.0") { "{}" }.check())
    }

    @Test fun releaseNotesBecomeWhatsNew() {
        val body = "## 1.1.0\n- Calmer **nothing found** screens\n* Fixes for large text\nSome prose\n- Find it copes with weak signals\n- a\n- b"
        val notes = UpdateCheck.notes(body)
        assertEquals(listOf("Calmer nothing found screens", "Fixes for large text", "Find it copes with weak signals", "a"), notes)
        assertEquals(emptyList<String>(), UpdateCheck.notes(null))
    }
}
