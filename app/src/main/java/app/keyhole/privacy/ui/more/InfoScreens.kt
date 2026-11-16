package app.keyhole.privacy.ui.more

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.BuildConfig
import app.keyhole.privacy.R
import app.keyhole.privacy.data.Mark
import app.keyhole.privacy.data.ReportRepository
import app.keyhole.privacy.data.SweepNotes
import app.keyhole.privacy.data.update.UpdateCheck
import app.keyhole.privacy.data.update.UpdateResult
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.CheckIcon
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.EmptyState
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.KeyholeSheet
import app.keyhole.privacy.ui.components.LensHeader
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.NeutralCircle
import app.keyhole.privacy.ui.components.Note
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.SignRow
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.icon
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.openInBrowser
import app.keyhole.privacy.ui.report.UndoSheet
import app.keyhole.privacy.ui.reports.DeleteSheet
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.welcome.MarkTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * More's screens that are mostly words: Marked as known, How Keyhole works, What Keyhole can't find, Privacy,
 * About, Licences. Each opens with the same small lens header as every detail screen.
 */

// --- Marked as known ------------------------------------------------------------------------------------

class MarkedKnownViewModel(private val reports: ReportRepository) : ViewModel() {
    val marks: StateFlow<List<Mark>> = reports.markList

    fun remove(mark: Mark) {
        viewModelScope.launch { reports.mark(mark.key, null, mark.label, mark.check) }
    }
}

/** More › Marked as known: every device marked "Not a camera?", by check; tap one to take the mark away. */
@Composable
fun MarkedKnownScreen(vm: MarkedKnownViewModel, onBack: () -> Unit) {
    val marks by vm.marks.collectAsStateWithLifecycle()
    var undoing by rememberSaveable { mutableStateOf<String?>(null) }
    val haptics = rememberHaptics()
    MarkedKnownContent(marks, onBack, onMark = { undoing = it.key })
    undoing?.let { key ->
        val mark = marks.firstOrNull { it.key == key }
        if (mark == null) undoing = null else UndoSheet(mark.label, onDismiss = { undoing = null }, onUndo = { haptics.buzz(Buzz.TOGGLE_OFF); vm.remove(mark); undoing = null })
    }
}

@Composable
fun MarkedKnownContent(marks: List<Mark>, onBack: () -> Unit, onMark: (Mark) -> Unit) {
    DetailScreen(onBack, title = stringResource(R.string.marked_as_known)) {
        if (marks.isEmpty()) {
            EmptyState(painterResource(R.drawable.ic_shield), stringResource(R.string.marked_known_none), stringResource(R.string.marked_known_empty_line))
            return@DetailScreen
        }
        Note(stringResource(R.string.marked_known_note))
        listOf(Check.WIFI, Check.HOTSPOTS, Check.BLUETOOTH).forEach { check ->
            val group = marks.filter { it.check == check }
            if (group.isEmpty()) return@forEach
            SectionLabel(stringResource(check.label), meta = group.size.toString())
            Grouped {
                group.forEach { m ->
                    row { shape ->
                        ListRow(
                            m.label, shape = shape, line = stringResource(if (m.knownAs == KnownAs.ROOM) R.string.marked_room else R.string.marked_mine),
                            leading = { IconCircle(painterResource(check.icon), CircleTone.Neutral) }, onClick = { onMark(m) }, chevron = false,
                        )
                    }
                }
            }
        }
    }
}

// --- How Keyhole works, What Keyhole can't find -------------------------------------------------------

private val Check.how: Int
    get() = when (this) {
        Check.WIFI -> R.string.how_wifi
        Check.HOTSPOTS -> R.string.how_hotspots
        Check.BLUETOOTH -> R.string.how_bluetooth
        Check.LENS -> R.string.how_lens
        Check.INFRARED -> R.string.how_infrared
        Check.MAGNET -> R.string.how_magnet
        Check.CHECKLIST -> R.string.how_checklist
    }

/** More › How Keyhole works: what each check looks for, in one sentence each. */
@Composable
fun HowItWorksScreen(onBack: () -> Unit) {
    DetailScreen(onBack) {
        LensHeader(painterResource(R.drawable.ic_help), stringResource(R.string.how_keyhole_works), line = stringResource(R.string.how_keyhole_works_line))
        Spacer(Modifier.height(Space.block))
        Grouped {
            Check.entries.forEach { check ->
                row { shape -> ListRow(stringResource(check.label), shape = shape, line = stringResource(check.how), leading = { CheckIcon(check, Modifier.height(Size.iconCircle)) }) }
            }
        }
        Spacer(Modifier.height(Space.block))
        Note(stringResource(R.string.how_note), icon = painterResource(R.drawable.ic_lock))
    }
}

/** More › What Keyhole can't find: the honest list (docs/LIMITS.md), and the checklist as the way to look by hand. */
@Composable
fun LimitsScreen(onBack: () -> Unit, onChecklist: () -> Unit) {
    DetailScreen(onBack) {
        LensHeader(painterResource(R.drawable.ic_eye_closed), stringResource(R.string.what_keyhole_cant_find), line = stringResource(R.string.limits_intro))
        Spacer(Modifier.height(Space.block))
        Grouped {
            listOf(
                R.string.limit_memory_card, R.string.limit_other_network, R.string.limit_isolation, R.string.limit_mac, R.string.limit_trackers,
                R.string.limit_infrared, R.string.limit_hotspot_scans, R.string.limit_magnet, R.string.limit_lens,
            ).forEach { line -> row { shape -> SignRow(painterResource(R.drawable.ic_not_checked), stringResource(line), shape = shape) } }
        }
        Spacer(Modifier.height(Space.block))
        ListRow(
            stringResource(R.string.check_checklist), line = stringResource(R.string.limits_by_hand),
            leading = { CheckIcon(Check.CHECKLIST, Modifier.height(Size.iconCircle)) }, onClick = onChecklist,
        )
    }
}

// --- 16 Privacy ---------------------------------------------------------------------------------------

class PrivacyViewModel(private val reports: ReportRepository, private val notes: SweepNotes) : ViewModel() {
    /** Everything this phone keeps for Keyhole: reports, photos, marks, and what this visit had collected. */
    fun deleteAll(done: () -> Unit) {
        viewModelScope.launch {
            reports.deleteAll()
            notes.clear()
            done()
        }
    }
}

/** 16 Privacy: nothing leaves the phone · why each permission · the policy · Delete all reports. */
@Composable
fun PrivacyScreen(vm: PrivacyViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var deleting by rememberSaveable { mutableStateOf(false) }
    var deleted by rememberSaveable { mutableStateOf(false) }
    DetailScreen(onBack) {
        if (deleted) Note(stringResource(R.string.deleted_all))
        LensHeader(painterResource(R.drawable.ic_lock), stringResource(R.string.privacy_line), line = stringResource(R.string.privacy_header_line))
        SectionLabel(stringResource(R.string.why_keyhole_asks))
        Grouped {
            row { shape -> ListRow(stringResource(R.string.check_wifi), shape = shape, line = stringResource(R.string.perm_row_wifi_line), leading = { NeutralCircle(painterResource(R.drawable.ic_wifi)) }) }
            row { shape -> ListRow(stringResource(R.string.perm_row_location), shape = shape, line = stringResource(R.string.perm_row_location_line), leading = { NeutralCircle(painterResource(R.drawable.ic_map_point)) }) }
            row { shape -> ListRow(stringResource(R.string.check_bluetooth), shape = shape, line = stringResource(R.string.perm_row_bluetooth_line), leading = { NeutralCircle(painterResource(R.drawable.ic_bluetooth)) }) }
            row { shape -> ListRow(stringResource(R.string.perm_row_camera), shape = shape, line = stringResource(R.string.perm_row_camera_line), leading = { NeutralCircle(painterResource(R.drawable.ic_camera)) }) }
        }
        Spacer(Modifier.height(Space.block))
        Grouped {
            row { shape ->
                ListRow(
                    stringResource(R.string.privacy_policy), shape = shape, line = stringResource(R.string.privacy_policy_line),
                    leading = { NeutralCircle(painterResource(R.drawable.ic_reports)) }, onClick = { openInBrowser(context, BuildConfig.REPO_URL + "/blob/main/PRIVACY.md") },
                )
            }
            row { shape ->
                ListRow(
                    stringResource(R.string.delete_all), shape = shape, line = stringResource(R.string.delete_all_line), alert = true,
                    leading = { IconCircle(painterResource(R.drawable.ic_trash), CircleTone.Alert) }, onClick = { deleting = true }, chevron = false,
                )
            }
        }
    }
    if (deleting) {
        DeleteSheet(
            onDismiss = { deleting = false },
            onDelete = { haptics.buzz(Buzz.CONFIRM); vm.deleteAll { deleted = true }; deleting = false },
            title = R.string.delete_all_title, line = R.string.delete_all_sheet_line,
        )
    }
}

// --- 17 About -----------------------------------------------------------------------------------------

private fun send(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** 17 About, in Burrow's order: the mark, version, updates (GitHub build), source, privacy, rating, contact, licences. */
@Composable
fun AboutScreen(onBack: () -> Unit, onPrivacy: () -> Unit, onLicences: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // What GitHub said, kept across rotation: "new:<version>|<page>", "latest" or "failed"; null while asking or closed.
    var update by rememberSaveable { mutableStateOf<String?>(null) }
    var checking by rememberSaveable { mutableStateOf(false) }
    DetailScreen(onBack, title = stringResource(R.string.about)) {
        Column(Modifier.fillMaxWidth().padding(vertical = Space.heading), horizontalAlignment = Alignment.CenterHorizontally) {
            MarkTile(Size.logo)
            Spacer(Modifier.height(Space.block))
            Text(stringResource(R.string.app_name), style = KeyholeType.headline, color = c.onSurface, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.version_by, BuildConfig.VERSION_NAME), style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Grouped {
            if (BuildConfig.GITHUB_UPDATES) row { shape ->
                ListRow(
                    stringResource(R.string.update_check), shape = shape, line = stringResource(R.string.update_check_line),
                    leading = { NeutralCircle(painterResource(R.drawable.ic_refresh)) },
                    onClick = {
                        checking = true
                        scope.launch {
                            val r = UpdateCheck(BuildConfig.REPO_URL, BuildConfig.VERSION_NAME).check()
                            update = when (r) {
                                is UpdateResult.Newer -> "new:${r.version}|${r.page}"
                                is UpdateResult.Latest -> "latest"
                                UpdateResult.Failed -> "failed"
                            }
                            checking = false
                        }
                    },
                )
            }
            row { shape ->
                ListRow(
                    stringResource(R.string.source_code), shape = shape, line = BuildConfig.REPO_URL.removePrefix("https://"),
                    leading = { NeutralCircle(painterResource(R.drawable.ic_code)) }, trailing = { OpenIcon() }, onClick = { openInBrowser(context, BuildConfig.REPO_URL) },
                )
            }
            row { shape -> ListRow(stringResource(R.string.data_stays), shape = shape, line = stringResource(R.string.privacy_line), leading = { NeutralCircle(painterResource(R.drawable.ic_shield)) }, onClick = onPrivacy) }
            if (BuildConfig.PLAY_LISTED) row { shape ->
                ListRow(
                    stringResource(R.string.rate_play), shape = shape, line = stringResource(R.string.rate_play_line),
                    leading = { NeutralCircle(painterResource(R.drawable.ic_star)) }, trailing = { OpenIcon() }, onClick = { openInBrowser(context, BuildConfig.PLAY_STORE_URL) },
                )
            }
            row { shape ->
                ListRow(
                    stringResource(R.string.report_problem), shape = shape, line = BuildConfig.CONTACT_EMAIL,
                    leading = { NeutralCircle(painterResource(R.drawable.ic_mail)) }, trailing = { OpenIcon() },
                    onClick = { send(context, Intent(Intent.ACTION_SENDTO, ("mailto:" + BuildConfig.CONTACT_EMAIL).toUri())) },
                )
            }
            row { shape -> ListRow(stringResource(R.string.licences), shape = shape, line = stringResource(R.string.licences_line), leading = { NeutralCircle(painterResource(R.drawable.ic_reports)) }, onClick = onLicences) }
        }
    }
    if (checking || update != null) {
        val u = update
        val newer = u?.startsWith("new:") == true
        KeyholeSheet(
            when {
                u == null -> stringResource(R.string.update_checking)
                newer -> stringResource(R.string.update_new_title, u.removePrefix("new:").substringBefore('|'))
                u == "latest" -> stringResource(R.string.update_latest_title)
                else -> stringResource(R.string.update_failed_title)
            },
            onDismiss = { update = null; checking = false },
            line = when {
                u == null -> stringResource(R.string.update_note)
                newer -> stringResource(R.string.update_new_line)
                u == "latest" -> stringResource(R.string.version_by, BuildConfig.VERSION_NAME)
                else -> stringResource(R.string.update_failed_line)
            },
            footer = if (newer) { {
                ButtonPair(
                    secondary = { m -> SecondaryButton(stringResource(R.string.cancel), { update = null }, m) },
                    primary = { m -> PrimaryButton(stringResource(R.string.update_open), { openInBrowser(context, u.substringAfter('|')); update = null }, m) },
                )
            } } else null,
        ) { Spacer(Modifier.height(Space.block)) }
    }
}

/** The ↗ at the end of a row that opens outside the app. */
@Composable
private fun OpenIcon() {
    androidx.compose.material3.Icon(painterResource(R.drawable.ic_open), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.height(Size.chevron))
}

// --- 18 Licences --------------------------------------------------------------------------------------

/** A licence: what it covers, its name, and its text in assets/licences (or a short text for Apache 2.0). */
@Immutable
data class Licence(val title: String, val name: String, val file: String?)

/** 18 Licences: the app, the libraries, the icons and fonts. Each opens its full text, bundled in the app. */
@Composable
fun LicencesScreen(onBack: () -> Unit, onOpen: (Licence) -> Unit) {
    val app = Licence(stringResource(R.string.app_name), stringResource(R.string.lic_gpl), "gpl-3.0.txt")
    val libraries = listOf(
        Licence(stringResource(R.string.lic_androidx), stringResource(R.string.lic_apache), null),
        Licence(stringResource(R.string.lic_kotlin), stringResource(R.string.lic_apache), null),
    )
    val art = listOf(
        Licence(stringResource(R.string.lic_solar), stringResource(R.string.lic_cc_by), "solar-cc-by-4.0.txt"),
        Licence(stringResource(R.string.lic_fonts), stringResource(R.string.lic_ofl), "fonts-ofl.txt"),
        Licence(stringResource(R.string.lic_atkinson), stringResource(R.string.lic_ofl), "atkinson-ofl.txt"),
    )
    DetailScreen(onBack, title = stringResource(R.string.licences)) {
        SectionLabel(stringResource(R.string.section_app))
        Grouped { row { shape -> ListRow(app.title, shape = shape, line = app.name, onClick = { onOpen(app) }) } }
        SectionLabel(stringResource(R.string.section_libraries))
        Grouped { libraries.forEach { l -> row { shape -> ListRow(l.title, shape = shape, line = l.name, onClick = { onOpen(l) }) } } }
        SectionLabel(stringResource(R.string.section_icons_fonts))
        Grouped { art.forEach { l -> row { shape -> ListRow(l.title, shape = shape, line = l.name, onClick = { onOpen(l) }) } } }
    }
}

/** One licence's full text, read from the app's own files (no internet). */
@Composable
fun LicenceTextScreen(title: String, file: String?, onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val apache = stringResource(R.string.lic_apache_text)
    val text by produceState(if (file == null) apache else "", file) {
        if (file != null) value = withContext(Dispatchers.IO) { runCatching { context.assets.open("licences/$file").bufferedReader().use { it.readText() } }.getOrDefault("") }
    }
    DetailScreen(onBack, title = title) {
        Text(text, style = KeyholeType.supporting, color = c.onSurface, modifier = Modifier.padding(horizontal = Space.headingInset))
    }
}
