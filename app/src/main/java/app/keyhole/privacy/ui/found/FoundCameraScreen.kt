package app.keyhole.privacy.ui.found

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.R
import app.keyhole.privacy.data.HelpEntry
import app.keyhole.privacy.data.HelpNumbers
import app.keyhole.privacy.data.ReportRepository
import app.keyhole.privacy.data.SavedReport
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.ChoiceRow
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.KeyholeSheet
import app.keyhole.privacy.ui.components.KeyholeTextField
import app.keyhole.privacy.ui.components.LensHeader
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.NumberCircle
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.SheetLine
import app.keyhole.privacy.ui.openInBrowser
import app.keyhole.privacy.ui.report.rememberReportWords
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.Space
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Who to call: the chosen or the phone's country (null when not known), and whether it came from the phone. */
@Immutable
data class HelpState(val entry: HelpEntry? = null, val fromPhone: Boolean = true, val chosen: String? = null, val all: List<HelpEntry> = emptyList(), val phoneCountry: String? = null)

/** The help numbers for screen 11, sheet 15 and More's tile. */
class HelpViewModel(private val numbers: HelpNumbers, private val settings: SettingsRepository, report: ReportRepository? = null, reportId: Long? = null) : ViewModel() {
    val state: StateFlow<HelpState> = combine(settings.settings, flowOf(Unit)) { s, _ ->
        val chosen = s.helpRegion
        HelpState(numbers.entry(chosen), chosen == null, chosen, numbers.all, numbers.phoneCountry())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HelpState())

    /** The report to share from screen 11, when it was opened from one. */
    val report: StateFlow<SavedReport?> = (if (report != null && reportId != null) report.report(reportId) else flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun choose(country: String?) {
        viewModelScope.launch { settings.setHelpRegion(country) }
    }
}

/** A country's name in the app's language ("Deutschland" in German). */
fun countryName(code: String): String = Locale("", code).getDisplayCountry(Locale.getDefault()).ifBlank { code }

private fun dial(context: android.content.Context, number: String) {
    // The dialer opens with the number filled in; the user presses call. Keyhole never calls by itself.
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, dialUri(number).toUri())) }
}

/** "tel:" for [number]: digits, + * # kept (Japan's police line is #9110), spaces and dashes dropped, # encoded. */
internal fun dialUri(number: String): String = "tel:" + number.filter { it.isDigit() || it in "+*#" }.replace("#", "%23")

/**
 * 11 If you find a camera: what to do, step by step · who to call where you are (Change opens the help numbers) ·
 * Share report / Call.
 */
@Composable
fun FoundCameraScreen(vm: HelpViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val report by vm.report.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val words = rememberReportWords()
    var choosing by rememberSaveable { mutableStateOf(false) }
    FoundCameraContent(
        s, onBack, onChange = { choosing = true }, onDial = { dial(context, it) }, onSite = { openInBrowser(context, it) },
        onShare = report?.let { r ->
            {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, words.text(r))
                runCatching { context.startActivity(Intent.createChooser(send, null)) }
            }
        },
    )
    if (choosing) HelpNumbersSheet(s, onDismiss = { choosing = false }, onChoose = { vm.choose(it); choosing = false })
}

@Composable
fun FoundCameraContent(s: HelpState, onBack: () -> Unit, onChange: () -> Unit, onDial: (String) -> Unit, onSite: (String) -> Unit, onShare: (() -> Unit)?) {
    val emergency = s.entry?.emergency ?: HelpNumbers.FALLBACK
    DetailScreen(
        onBack,
        dock = {
            if (onShare != null) {
                ButtonPair(
                    secondary = { m -> SecondaryButton(stringResource(R.string.share_report), onShare, m) },
                    primary = { m -> PrimaryButton(stringResource(R.string.call_number, emergency), { onDial(emergency) }, m, icon = painterResource(R.drawable.ic_call)) },
                )
            } else {
                PrimaryButton(stringResource(R.string.call_number, emergency), { onDial(emergency) }, Modifier.fillMaxWidth(), icon = painterResource(R.drawable.ic_call))
            }
        },
    ) {
        LensHeader(painterResource(R.drawable.ic_heart), stringResource(R.string.if_you_find_a_camera), line = stringResource(R.string.found_camera_line))
        Spacer(Modifier.height(Space.block))
        Grouped {
            listOf(
                R.string.found_step_1 to R.string.found_step_1_line, R.string.found_step_2 to R.string.found_step_2_line,
                R.string.found_step_3 to R.string.found_step_3_line, R.string.found_step_4 to R.string.found_step_4_line,
            ).forEachIndexed { i, (title, line) ->
                row { shape -> ListRow(stringResource(title), shape = shape, line = stringResource(line), leading = { NumberCircle(i + 1) }) }
            }
        }
        SectionLabel(stringResource(R.string.help_where_you_are), action = stringResource(R.string.change), onAction = onChange)
        Grouped {
            row { shape ->
                val line = when {
                    s.entry == null -> stringResource(R.string.emergency_unknown_line, emergency)
                    s.fromPhone -> stringResource(R.string.emergency_line_phone, emergency, countryName(s.entry.country))
                    else -> stringResource(R.string.emergency_line, emergency, countryName(s.entry.country))
                }
                ListRow(stringResource(R.string.emergency), shape = shape, line = line, leading = { IconCircle(painterResource(R.drawable.ic_call), CircleTone.Accent) }, onClick = { onDial(emergency) })
            }
            val entry = s.entry
            if (entry != null && entry.police != entry.emergency) row { shape ->
                ListRow(stringResource(R.string.police), shape = shape, line = entry.police, leading = { IconCircle(painterResource(R.drawable.ic_shield), CircleTone.Accent) }, onClick = { onDial(entry.police) })
            }
            if (entry?.line != null) row { shape ->
                ListRow(stringResource(R.string.helpline), shape = shape, line = entry.line, leading = { IconCircle(painterResource(R.drawable.ic_phone), CircleTone.Accent) }, onClick = { onDial(entry.line) })
            }
            if (entry?.site != null) row { shape ->
                ListRow(
                    stringResource(R.string.report_to_police), shape = shape, line = stringResource(R.string.report_to_police_line),
                    leading = { IconCircle(painterResource(R.drawable.ic_user_shield), CircleTone.Accent) }, onClick = { onSite(entry.site) },
                )
            }
        }
    }
}

/** 15 Help numbers: search, the phone's region first, then every country in the list. [onChoose] gets null for the phone's region. */
@Composable
fun HelpNumbersSheet(s: HelpState, onDismiss: () -> Unit, onChoose: (String?) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val sorted = s.all.sortedBy { countryName(it.country) }
    val q = query.trim()
    val shown = if (q.isEmpty()) sorted else sorted.filter { countryName(it.country).contains(q, ignoreCase = true) || it.country.equals(q, ignoreCase = true) }
    KeyholeSheet(stringResource(R.string.help_numbers), onDismiss, line = stringResource(R.string.help_numbers_line)) {
        Box(Modifier.padding(horizontal = Space.screen, vertical = Space.inline)) {
            KeyholeTextField(query, { query = it }, stringResource(R.string.search_countries), stringResource(R.string.search_countries), icon = painterResource(R.drawable.ic_search))
        }
        Column(Modifier.selectableGroup()) {
            if (q.isEmpty()) {
                val phone = s.phoneCountry?.let { code -> s.all.firstOrNull { it.country == code } }
                ChoiceRow(
                    stringResource(R.string.use_phone_region), s.chosen == null, { onChoose(null) },
                    line = phone?.let { stringResource(R.string.two_parts, countryName(it.country), it.emergency) } ?: stringResource(R.string.emergency_unknown_line, HelpNumbers.FALLBACK),
                )
            }
            shown.forEach { e -> ChoiceRow(countryName(e.country), s.chosen == e.country, { onChoose(e.country) }, line = e.emergency) }
        }
        if (shown.isEmpty()) SheetLine(stringResource(R.string.no_countries_found))
        Spacer(Modifier.height(Space.block))
    }
}

@Preview
@Composable
private fun FoundCameraPreview() = KeyholeTheme {
    FoundCameraContent(HelpState(HelpEntry("DE", "112", "110"), true), {}, {}, {}, {}, {})
}
