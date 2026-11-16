package app.keyhole.privacy.ui.reports

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.R
import app.keyhole.privacy.data.ReportRepository
import app.keyhole.privacy.data.SavedReport
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.EmptyState
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.KeyholeSheet
import app.keyhole.privacy.ui.components.KeyholeTextField
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.RoundIconButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.TabScreen
import app.keyhole.privacy.ui.components.Tag
import app.keyhole.privacy.ui.components.TagKind
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.icon
import app.keyhole.privacy.ui.report.ReportWords
import app.keyhole.privacy.ui.report.rememberReportWords
import app.keyhole.privacy.ui.theme.Hues
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ReportsState(val reports: List<SavedReport> = emptyList(), val loaded: Boolean = false)

class ReportsViewModel(private val repo: ReportRepository) : ViewModel() {
    val state: StateFlow<ReportsState> = repo.all.map { ReportsState(it, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsState())

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}

/** A place's own hue, on its circle in lists. */
val Place.hue: Float
    get() = when (this) {
        Place.STAY -> Hues.WIFI
        Place.TRIAL_ROOM -> Hues.INFRARED
        Place.WASHROOM -> Hues.CHECKLIST
        Place.OFFICE -> Hues.BLUETOOTH
        Place.CAR -> Hues.REPORT
    }

/** Reports of this week, then one group per month (newest first). */
fun groupByWhen(reports: List<SavedReport>, now: Long, thisWeek: String, monthName: (Long) -> String): List<Pair<String, List<SavedReport>>> {
    val week = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return reports.sortedByDescending { it.savedAt }.groupBy { if (it.savedAt >= week) thisWeek else monthName(it.savedAt) }.toList()
}

/** "Yesterday · stay": when, and where. */
private fun line(words: ReportWords, r: SavedReport, now: Long, res: android.content.res.Resources): String {
    val day = android.text.format.DateUtils.getRelativeTimeSpanString(r.savedAt, now, android.text.format.DateUtils.DAY_IN_MILLIS, android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE).toString()
    return res.getString(R.string.two_parts, day, res.getString(ReportWords.placeLower(r.place)))
}

/** 12 Reports: by date, each with what is still to look at; search; a long press deletes (with its photos). */
@Composable
fun ReportsScreen(vm: ReportsViewModel, onOpen: (Long) -> Unit, now: () -> Long = System::currentTimeMillis) {
    val s by vm.state.collectAsStateWithLifecycle()
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    val haptics = rememberHaptics()
    ReportsContent(s, onOpen, onDelete = { haptics.buzz(Buzz.TICK); deleting = it }, now = now())
    deleting?.let { id ->
        DeleteSheet(onDismiss = { deleting = null }, onDelete = { haptics.buzz(Buzz.CONFIRM); vm.delete(id); deleting = null })
    }
}

@Composable
fun ReportsContent(s: ReportsState, onOpen: (Long) -> Unit, onDelete: (Long) -> Unit, now: Long) {
    val c = MaterialTheme.colorScheme
    val words = rememberReportWords()
    val res = androidx.compose.ui.platform.LocalContext.current.resources
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    TabScreen(
        stringResource(R.string.tab_reports),
        actions = {
            if (s.reports.isNotEmpty()) RoundIconButton(painterResource(if (searching) R.drawable.ic_close else R.drawable.ic_search), stringResource(if (searching) R.string.close else R.string.search), {
                searching = !searching
                if (!searching) query = ""
            }, active = searching)
        },
    ) {
        Column(Modifier.weight(1f).widthIn(max = Size.contentMax).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.screen)) {
            if (searching) {
                KeyholeTextField(query, { query = it }, stringResource(R.string.search_reports), stringResource(R.string.search_reports), icon = painterResource(R.drawable.ic_search))
            }
            if (s.loaded && s.reports.isEmpty()) {
                EmptyState(painterResource(R.drawable.ic_reports), stringResource(R.string.reports_empty_title), stringResource(R.string.reports_empty_line))
                return@Column
            }
            val q = query.trim()
            val shown = if (q.isEmpty()) s.reports else s.reports.filter { r -> words.name(r).contains(q, ignoreCase = true) || r.wifiName?.contains(q, ignoreCase = true) == true }
            if (shown.isEmpty() && q.isNotEmpty()) {
                EmptyState(painterResource(R.drawable.ic_search), stringResource(R.string.no_reports_match), q)
            }
            val month = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
            groupByWhen(shown, now, stringResource(R.string.this_week)) { month.format(Date(it)) }.forEach { (title, group) ->
                SectionLabel(title, meta = group.size.toString())
                Grouped {
                    group.forEach { r ->
                        row { shape ->
                            ListRow(
                                words.name(r), shape = shape, line = line(words, r, now, res),
                                leading = { IconCircle(painterResource(r.place.icon), CircleTone.Hue(r.place.hue)) },
                                trailing = { if (r.toLookAt > 0) Tag(words.toLookAt(r), TagKind.WORTH_A_LOOK) else Tag(stringResource(R.string.nothing_found), TagKind.GOOD) },
                                onClick = { onOpen(r.id) }, onLongClick = { onDelete(r.id) }, longClickLabel = stringResource(R.string.delete),
                            )
                        }
                    }
                }
            }
            if (shown.isNotEmpty()) {
                Spacer(Modifier.height(Space.heading))
                Text(stringResource(R.string.reports_footer), style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = Space.heading))
            }
            Spacer(Modifier.height(Space.screen))
        }
    }
}

/** Deleting a report: its photos go too, and it can't be undone. */
@Composable
fun DeleteSheet(onDismiss: () -> Unit, onDelete: () -> Unit, title: Int = R.string.delete_report_title, line: Int = R.string.delete_report_line) {
    KeyholeSheet(
        stringResource(title), onDismiss, line = stringResource(line),
        footer = {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.cancel), onDismiss, m) },
                primary = { m -> PrimaryButton(stringResource(R.string.delete), onDelete, m, icon = painterResource(R.drawable.ic_trash)) },
            )
        },
    ) { Spacer(Modifier.height(Space.small)) }
}

@Preview
@Composable
private fun ReportsPreview() = KeyholeTheme {
    ReportsContent(ReportsState(listOf(app.keyhole.privacy.ui.report.previewReport()), true), {}, {}, 1_794_900_000_000)
}
