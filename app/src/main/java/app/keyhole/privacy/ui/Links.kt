package app.keyhole.privacy.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import app.keyhole.privacy.BuildConfig

/** Opens a page in the phone's browser. Keyhole itself never loads it. */
fun openInBrowser(context: Context, address: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, address.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** "Keyhole got it wrong? Report it": a new issue on the project's page, written by the user in their browser. */
fun reportWrongResult(context: Context) = openInBrowser(context, BuildConfig.REPO_URL + "/issues/new")
