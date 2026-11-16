package app.keyhole.privacy

import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.ui.nav.OpenFromOutside
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.welcome.MarkTile
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.keyhole.privacy.ui.drawEdgeToEdge
import app.keyhole.privacy.ui.more.AppLanguage
import app.keyhole.privacy.ui.nav.KeyholeNavHost
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.isDark

class MainActivity : ComponentActivity() {
    /** Set by a launcher shortcut; the nav host goes there and clears it. */
    private val pending = MutableStateFlow<OpenFromOutside?>(null)

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        // Behind the bars from the first frame, icons for the system's light or dark until settings load.
        drawEdgeToEdge(dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent) // not again after a rotation
        val container = (application as KeyholeApp).container
        setContent {
            // Null until DataStore has loaded, so the first real frame already has the right theme.
            val settings by container.settings.settings.collectAsStateWithLifecycle(null)
            val current = settings ?: run {
                // App start (02h): the mark on a blank page, in the system's light or dark, until settings load.
                KeyholeTheme {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                        MarkTile(Size.startMark)
                    }
                }
                return@setContent
            }
            val dark = current.theme.isDark()
            LaunchedEffect(dark) { drawEdgeToEdge(dark) }
            KeyholeTheme(current.theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val open by pending.collectAsStateWithLifecycle()
                    KeyholeNavHost(container, current, open = open, onOpened = { pending.value = null })
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
            ACTION_INFRARED -> pending.value = OpenFromOutside.OneCheck(Check.INFRARED)
            ACTION_REPORTS -> pending.value = OpenFromOutside.Reports
        }
    }

    companion object {
        /** Launcher shortcuts (res/xml/shortcuts.xml). */
        const val ACTION_SWEEP = "app.keyhole.privacy.action.SWEEP"
        const val ACTION_LENS = "app.keyhole.privacy.action.LENS"
        const val ACTION_INFRARED = "app.keyhole.privacy.action.INFRARED"
        const val ACTION_REPORTS = "app.keyhole.privacy.action.REPORTS"
    }
}
