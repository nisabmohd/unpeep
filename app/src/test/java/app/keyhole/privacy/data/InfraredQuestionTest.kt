package app.keyhole.privacy.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The TV-remote question opens by itself once: closing it is remembered, and Test again asks it again. */
class InfraredQuestionTest {
    private fun withSettings(block: suspend (SettingsRepository) -> Unit) = runBlocking {
        val dir = File.createTempFile("settings", "").apply { delete(); mkdirs() }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            block(SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "s.preferences_pb") }))
        } finally {
            scope.cancel()
            dir.deleteRecursively()
        }
    }

    @Test fun closingTheQuestionIsRemembered() = withSettings { s ->
        assertFalse(s.settings.first().infraredAsked)
        s.setInfraredAsked()
        assertTrue(s.settings.first().infraredAsked)
        assertNull(s.settings.first().seesInfrared)
    }

    @Test fun testAgainForgetsTheAnswerAndThatItWasAsked() = withSettings { s ->
        s.setSeesInfrared(false)
        s.setInfraredAsked()
        s.clearSeesInfrared()
        val after = s.settings.first()
        assertNull(after.seesInfrared)
        assertFalse(after.infraredAsked)
    }

    @Test fun anAnswerIsKept() = withSettings { s ->
        s.setSeesInfrared(true)
        assertEquals(true, s.settings.first().seesInfrared)
    }
}
