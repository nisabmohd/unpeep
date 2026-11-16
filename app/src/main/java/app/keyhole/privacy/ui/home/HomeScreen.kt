package app.keyhole.privacy.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.ui.components.Box24
import app.keyhole.privacy.ui.components.CheckRow
import app.keyhole.privacy.ui.components.Chip
import app.keyhole.privacy.ui.components.LensStartButton
import app.keyhole.privacy.ui.components.RoundIconButton
import app.keyhole.privacy.ui.components.TabScreen
import app.keyhole.privacy.ui.checksPlural
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space

/** 02 Home: place chips · the lens Start · "Check this room" + n checks · Or run one check · the last report. */
@Composable
fun HomeScreen(vm: HomeViewModel, onStart: (Place) -> Unit, onCheck: (Check, Place) -> Unit, onHelp: () -> Unit, onReport: (Long) -> Unit, onSeeVisit: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val words = app.keyhole.privacy.ui.report.rememberReportWords()
    HomeContent(s, vm::pick, onStart, onCheck, onHelp) {
        if (s.visitChecks > 0) {
            app.keyhole.privacy.ui.components.ListRow(
                stringResource(R.string.this_visit), line = pluralStringResource(R.plurals.checks_run, s.visitChecks, s.visitChecks),
                leading = { app.keyhole.privacy.ui.components.IconCircle(painterResource(R.drawable.ic_history), app.keyhole.privacy.ui.components.CircleTone.Hue(app.keyhole.privacy.ui.theme.Hues.REPORT)) }, onClick = onSeeVisit,
            )
        }
        s.lastReport?.let { r ->
            app.keyhole.privacy.ui.components.ListRow(
                words.name(r), line = stringResource(R.string.two_parts, stringResource(R.string.last_report), words.toLookAt(r)),
                leading = { app.keyhole.privacy.ui.components.IconCircle(painterResource(R.drawable.ic_reports), app.keyhole.privacy.ui.components.CircleTone.Hue(app.keyhole.privacy.ui.theme.Hues.REPORT)) }, onClick = { onReport(r.id) },
            )
        }
    }
}

@Composable
fun HomeContent(s: HomeState, onPlace: (Place) -> Unit, onStart: (Place) -> Unit, onCheck: (Check, Place) -> Unit, onHelp: () -> Unit, lastReport: @Composable () -> Unit = {}) {
    val c = MaterialTheme.colorScheme
    TabScreen(
        stringResource(R.string.app_name),
        actions = { RoundIconButton(painterResource(R.drawable.ic_help), stringResource(R.string.how_keyhole_works), onHelp) },
    ) {
        Column(Modifier.weight(1f).widthIn(max = Size.contentMax).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Space.screen).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Space.inline),
            ) {
                Place.entries.forEach { place -> Chip(stringResource(place.label), place == s.place, { onPlace(place) }) }
            }
            Spacer(Modifier.height(Space.lensTop))
            LensStartButton(stringResource(R.string.start), { onStart(s.place) })
            Spacer(Modifier.height(Space.lensBottom))
            Text(stringResource(R.string.check_this_room), style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.heading).semantics { heading() })
            Text(
                stringResource(R.string.home_line, pluralStringResource(s.place.checksPlural, s.steps.size, s.steps.size), pluralStringResource(R.plurals.about_minutes, s.minutes, s.minutes)),
                style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.heading),
            )
            Spacer(Modifier.height(Space.heading))
            Column(Modifier.padding(horizontal = Space.screen), verticalArrangement = Arrangement.spacedBy(Space.block)) {
                Box24(title = stringResource(R.string.or_run_one_check)) { CheckRow(s.checks, onRun = { onCheck(it, s.place) }) }
                lastReport()
            }
            Spacer(Modifier.height(Space.screen))
        }
    }
}
