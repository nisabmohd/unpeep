package app.keyhole.privacy.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.EmptyState

/** A route that isn't built yet: the lens header and one honest line, so every row leads somewhere. */
@Composable
fun StandInScreen(title: String, line: String?, onBack: () -> Unit) {
    DetailScreen(onBack) {
        EmptyState(painterResource(R.drawable.ic_structure), title, line?.let { stringResource(R.string.stand_in_line_for, it) } ?: stringResource(R.string.stand_in_line))
    }
}
