package app.unpeep.privacy.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.scan.Access

/**
 * Asking Android for what a check needs, just in time (docs/PLAN.md § Permissions): the screen says in one line
 * why, the user taps Allow, Android asks. [settingsOnly]: Android no longer asks (denied twice), so Allow opens the
 * app's settings instead.
 */
@Stable
class PermissionAsk internal constructor(val settingsOnly: Boolean, val ask: () -> Unit)

/** [onResult] runs after Android's answer, and tells whether everything the check needs is now allowed. */
@Composable
fun rememberPermissionAsk(check: Check, onResult: (granted: Boolean) -> Unit): PermissionAsk {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val permissions = remember(check) { Access.permissions(check) }
    val result by rememberUpdatedState(onResult)
    var settingsOnly by rememberSaveable(check) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { answers ->
        val granted = Access.granted(context, check)
        // Denied, and Android won't show its question again: from now on only Settings can allow it.
        if (!granted && activity != null && permissions.none { activity.shouldShowRequestPermissionRationale(it) }) settingsOnly = true
        if (granted) settingsOnly = false
        result(granted)
    }
    return PermissionAsk(settingsOnly) {
        if (settingsOnly) openAppSettings(context) else launcher.launch(permissions.toTypedArray())
    }
}

/** The app's page in Android's Settings, where a permission can be allowed after "Don't allow". */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
