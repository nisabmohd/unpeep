package app.keyhole.privacy.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.components.EmptyState
import app.keyhole.privacy.ui.components.TabScreen
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space

class ReportsViewModel : ViewModel()

/** 12 Reports: the list by date; until the first sweep, the empty state. */
@Composable
fun ReportsScreen(@Suppress("UNUSED_PARAMETER") vm: ReportsViewModel) {
    TabScreen(stringResource(R.string.tab_reports)) {
        Column(Modifier.weight(1f).widthIn(max = Size.contentMax).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.screen)) {
            EmptyState(painterResource(R.drawable.ic_reports), stringResource(R.string.reports_empty_title), stringResource(R.string.reports_empty_line))
        }
    }
}
