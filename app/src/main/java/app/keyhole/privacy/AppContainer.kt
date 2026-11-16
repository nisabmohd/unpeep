package app.keyhole.privacy

import android.content.Context
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.data.SignatureLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Holds the app's long-lived objects: the only dependency "graph" (no DI framework).
 * Everything is lazy, so cold start only pays for what the first screen needs; signatures load on the first scan.
 */
class AppContainer(val context: Context) {
    val settings by lazy { SettingsRepository(context) }
    val signatures by lazy { SignatureLoader(context) }

    /** Lives as long as the process, for work that isn't tied to a screen (saving a setting as a screen closes). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
