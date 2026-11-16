package app.unpeep.privacy.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import app.unpeep.privacy.BuildConfig

/** Opens a page in the phone's browser. Unpeep itself never loads it. */
fun openInBrowser(context: Context, address: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, address.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** "Unpeep got it wrong? Report it": a new issue on the project's page, written by the user in their browser. */
fun reportWrongResult(context: Context) = openInBrowser(context, BuildConfig.REPO_URL + "/issues/new")
