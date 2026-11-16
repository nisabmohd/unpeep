package app.unpeep.privacy

import androidx.compose.foundation.layout.Spacer
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.ui.nav.OpenFromOutside
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.unpeep.privacy.ui.drawEdgeToEdge
import app.unpeep.privacy.ui.more.AppLanguage
import app.unpeep.privacy.ui.nav.UnpeepNavHost
import app.unpeep.privacy.ui.theme.UnpeepTheme
import app.unpeep.privacy.ui.theme.isDark

class MainActivity : ComponentActivity() {
    /** Set by a launcher shortcut; the nav host goes there and clears it. */
    private val pending = MutableStateFlow<OpenFromOutside?>(null)

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        // Behind the bars from the first frame, icons for the system's light or dark until settings load.
        drawEdgeToEdge(dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent) // not again after a rotation
        val container = (application as UnpeepApp).container
        setContent {
            // Null until DataStore has loaded, so the first real frame already has the right theme.
            val settings by container.settings.settings.collectAsStateWithLifecycle(null)
            val current = settings ?: run {
                // As Burrow and Weft: Android's own splash (the launcher icon on the page colour), then straight into
                // the app. Until settings load, an empty frame over the window background; never a second splash.
                Spacer(Modifier.fillMaxSize())
                return@setContent
            }
            val dark = current.theme.isDark()
            LaunchedEffect(dark) { drawEdgeToEdge(dark) }
            UnpeepTheme(current.theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val open by pending.collectAsStateWithLifecycle()
                    UnpeepNavHost(container, current, open = open, onOpened = { pending.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SWEEP -> pending.value = OpenFromOutside.Sweep
            ACTION_LENS -> pending.value = OpenFromOutside.OneCheck(Check.LENS)
            ACTION_MAGNET -> pending.value = OpenFromOutside.OneCheck(Check.MAGNET)
            ACTION_REPORTS -> pending.value = OpenFromOutside.Reports
        }
    }

    companion object {
        /** Launcher shortcuts (res/xml/shortcuts.xml). */
        const val ACTION_SWEEP = "app.unpeep.privacy.action.SWEEP"
        const val ACTION_LENS = "app.unpeep.privacy.action.LENS"
        const val ACTION_MAGNET = "app.unpeep.privacy.action.MAGNET"
        const val ACTION_REPORTS = "app.unpeep.privacy.action.REPORTS"
    }
}
