package app.unpeep.privacy.ui.more

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import app.unpeep.privacy.ui.components.Box24
import app.unpeep.privacy.ui.components.MovingLine
import app.unpeep.privacy.ui.components.LocalToasts
import app.unpeep.privacy.ui.hue
import androidx.compose.material3.Icon
import app.unpeep.privacy.ui.theme.Shapes
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.Image
import app.unpeep.privacy.ui.theme.WeftBrand
import app.unpeep.privacy.ui.theme.BurrowBrand
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
import app.unpeep.privacy.BuildConfig
import app.unpeep.privacy.R
import app.unpeep.privacy.data.Mark
import app.unpeep.privacy.data.ReportRepository
import app.unpeep.privacy.data.SweepNotes
import app.unpeep.privacy.data.update.UpdateCheck
import app.unpeep.privacy.data.update.UpdateResult
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.domain.KnownAs
import app.unpeep.privacy.ui.components.Buzz
import app.unpeep.privacy.ui.components.ButtonPair
import app.unpeep.privacy.ui.components.CheckIcon
import app.unpeep.privacy.ui.components.CircleTone
import app.unpeep.privacy.ui.components.DetailScreen
import app.unpeep.privacy.ui.components.EmptyState
import app.unpeep.privacy.ui.components.Grouped
import app.unpeep.privacy.ui.components.IconCircle
import app.unpeep.privacy.ui.components.UnpeepSheet
import app.unpeep.privacy.ui.components.LensHeader
import app.unpeep.privacy.ui.components.ListRow
import app.unpeep.privacy.ui.components.NeutralCircle
import app.unpeep.privacy.ui.components.Note
import app.unpeep.privacy.ui.components.NoteTone
import app.unpeep.privacy.ui.components.PrimaryButton
import app.unpeep.privacy.ui.components.SecondaryButton
import app.unpeep.privacy.ui.components.SectionLabel
import app.unpeep.privacy.ui.components.SignRow
import app.unpeep.privacy.ui.components.rememberHaptics
import app.unpeep.privacy.ui.icon
import app.unpeep.privacy.ui.label
import app.unpeep.privacy.ui.openInBrowser
import app.unpeep.privacy.ui.report.UndoSheet
import app.unpeep.privacy.ui.reports.DeleteSheet
import app.unpeep.privacy.ui.theme.UnpeepType
import app.unpeep.privacy.ui.theme.Size
import app.unpeep.privacy.ui.theme.Space
import app.unpeep.privacy.ui.welcome.MarkTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * More's screens that are mostly words: Marked as known, How Unpeep works, What Unpeep can't find, Privacy,
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

// --- How Unpeep works, What Unpeep can't find -------------------------------------------------------

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

/** More › How Unpeep works: what each check looks for, in one sentence each. */
@Composable
fun HowItWorksScreen(onBack: () -> Unit) {
    DetailScreen(onBack) {
        LensHeader(painterResource(R.drawable.ic_help), stringResource(R.string.how_unpeep_works), line = stringResource(R.string.how_unpeep_works_line))
        Spacer(Modifier.height(Space.card))
        Grouped {
            Check.entries.forEach { check ->
                row { shape -> ListRow(stringResource(check.label), shape = shape, line = stringResource(check.how), leading = { IconCircle(painterResource(check.icon), CircleTone.Hue(check.hue)) }) }
            }
        }
        Spacer(Modifier.height(Space.block))
        Note(stringResource(R.string.how_note), tone = NoteTone.HONESTY, icon = painterResource(R.drawable.ic_lock))
    }
}

/** More › What Unpeep can't find: the honest list (docs/LIMITS.md), and the checklist as the way to look by hand. */
@Composable
fun LimitsScreen(onBack: () -> Unit) {
    DetailScreen(onBack) {
        LensHeader(painterResource(R.drawable.ic_eye_closed), stringResource(R.string.what_unpeep_cant_find), line = stringResource(R.string.limits_intro))
        Spacer(Modifier.height(Space.card))
        // The four limits as rows (final v2, 13g): a title, one line, and the thing itself in a grey circle.
        Grouped {
            listOf(
                Triple(R.string.limit_memory_card_title, R.string.limit_memory_card, R.drawable.ic_memory_card),
                Triple(R.string.limit_other_network_title, R.string.limit_other_network, R.drawable.ic_antenna),
                Triple(R.string.limit_isolation_title, R.string.limit_isolation, R.drawable.ic_shield),
                Triple(R.string.limit_trackers_title, R.string.limit_trackers, R.drawable.ic_tag),
            ).forEach { (title, line, icon) ->
                row { shape -> ListRow(stringResource(title), shape = shape, line = stringResource(line), leading = { IconCircle(painterResource(icon), CircleTone.Neutral) }) }
            }
        }
    }
}

// --- 16 Privacy ---------------------------------------------------------------------------------------

class PrivacyViewModel(private val reports: ReportRepository, private val notes: SweepNotes) : ViewModel() {
    /** Everything this phone keeps for Unpeep: reports, photos, marks, and what this visit had collected. */
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
    val toasts = LocalToasts.current
    val deletedText = stringResource(R.string.deleted_all)
    DetailScreen(onBack, title = stringResource(R.string.privacy)) {
        LensHeader(painterResource(R.drawable.ic_lock), stringResource(R.string.privacy_line), line = stringResource(R.string.privacy_header_line))
        SectionLabel(stringResource(R.string.why_unpeep_asks))
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
            onDelete = { haptics.buzz(Buzz.CONFIRM); vm.deleteAll { toasts.show(deletedText) }; deleting = false },
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
    var result by remember { mutableStateOf<UpdateResult?>(null) }
    val runCheck = {
        checking = true
        result = null
        scope.launch {
            val r = UpdateCheck(BuildConfig.REPO_URL, BuildConfig.VERSION_NAME).check()
            result = r
            update = when (r) {
                is UpdateResult.Newer -> "new:${r.version}|${r.page}"
                is UpdateResult.Latest -> "latest"
                UpdateResult.Failed -> "failed"
            }
            checking = false
        }
    }
    DetailScreen(onBack, title = stringResource(R.string.about)) {
        // 17: the 72 dp mark 4 dp under the bar, the name 12 dp under it, the version 2 dp under that, 20 dp to the rows.
        Column(Modifier.fillMaxWidth().padding(top = Space.small, bottom = Space.lensBottom), horizontalAlignment = Alignment.CenterHorizontally) {
            MarkTile(Size.logo)
            Spacer(Modifier.height(Space.block))
            Text(stringResource(R.string.app_name), style = UnpeepType.title, color = c.onSurface, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.version_by, BuildConfig.VERSION_NAME), style = UnpeepType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.row))
        }
        Grouped {
            if (BuildConfig.GITHUB_UPDATES) row { shape ->
                ListRow(
                    stringResource(R.string.update_check), shape = shape, line = stringResource(R.string.update_check_line),
                    leading = { NeutralCircle(painterResource(R.drawable.ic_refresh)) },
                    onClick = { runCheck() },
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
                // A problem is reported where the code lives (17): a new GitHub issue; mid apps below is the email.
                ListRow(
                    stringResource(R.string.report_problem), shape = shape, line = stringResource(R.string.report_problem_line),
                    leading = { NeutralCircle(painterResource(R.drawable.ic_mail)) }, trailing = { OpenIcon() },
                    onClick = { openInBrowser(context, BuildConfig.REPO_URL + "/issues/new") },
                )
            }
            row { shape -> ListRow(stringResource(R.string.licences), shape = shape, line = stringResource(R.string.licences_line), leading = { NeutralCircle(painterResource(R.drawable.ic_reports)) }, onClick = onLicences) }
        }
        // The maker's other apps (17), each to its own releases page: the same link in every build.
        SectionLabel(stringResource(R.string.more_from_mid_apps))
        Grouped {
            row { shape ->
                ListRow(
                    stringResource(R.string.app_burrow), shape = shape, line = stringResource(R.string.app_burrow_line),
                    leading = { AppTile(R.drawable.app_burrow_mark, BurrowBrand) }, trailing = { OpenIcon() }, onClick = { openInBrowser(context, BURROW_URL) },
                )
            }
            row { shape ->
                ListRow(
                    stringResource(R.string.app_weft), shape = shape, line = stringResource(R.string.app_weft_line),
                    leading = { AppTile(R.drawable.app_weft_mark, WeftBrand) }, trailing = { OpenIcon() }, onClick = { openInBrowser(context, WEFT_URL) },
                )
            }
        }
        // The maker, mid apps, with the contact address; a tap writes an email (as in Burrow and Weft).
        Spacer(Modifier.height(Space.block))
        ListRow(
            stringResource(R.string.mid_apps), line = stringResource(R.string.about_contact, BuildConfig.CONTACT_EMAIL),
            leading = { Image(painterResource(R.drawable.app_mid_apps_mark), null, Modifier.size(Size.iconCircle).clip(Shapes.pill)) },
            trailing = { OpenIcon() },
            onClick = { send(context, Intent(Intent.ACTION_SENDTO, ("mailto:" + BuildConfig.CONTACT_EMAIL).toUri()).putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))) },
        )
        Text(
            stringResource(R.string.made_by), style = UnpeepType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Space.lensBottom, bottom = Space.heading),
        )
    }
    if (checking || update != null) {
        UpdateSheet(
            result = result, checking = checking,
            onDismiss = { update = null; result = null; checking = false },
            onOpen = { page -> openInBrowser(context, page); update = null; result = null },
            onRetry = { runCheck() },
        )
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
        Text(text, style = UnpeepType.supporting, color = c.onSurface, modifier = Modifier.padding(horizontal = Space.headingInset))
    }
}

private const val BURROW_URL = "https://github.com/nisabmohd/burrow"
private const val WEFT_URL = "https://github.com/nisabmohd/weft"

/** Another app's icon in a row: its mark on its own colour, in the same 44 dp circle as every row's icon. */
@Composable
private fun AppTile(@androidx.annotation.DrawableRes mark: Int, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(Size.iconCircle).clip(Shapes.pill).background(color), contentAlignment = Alignment.Center) {
        // Launcher foregrounds are 108 units with the mark in the middle: at the circle's own size the mark sits
        // well inside it, smaller than on the launcher.
        Image(painterResource(mark), null, modifier = Modifier.requiredSize(Size.iconCircle * APP_MARK_SCALE))
    }
}

private const val APP_MARK_SCALE = 1.4f

/**
 * Check for updates (17a–17d, github build): checking, with a moving line · on the latest version (Done) · a new
 * version with what's new (Later / Open release page) · couldn't check (Close / Try again).
 */
@Composable
private fun UpdateSheet(result: UpdateResult?, checking: Boolean, onDismiss: () -> Unit, onOpen: (String) -> Unit, onRetry: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val r = if (checking) null else result
    UnpeepSheet(
        when (r) {
            null -> stringResource(R.string.update_checking)
            is UpdateResult.Newer -> stringResource(R.string.update_available)
            is UpdateResult.Latest -> stringResource(R.string.update_latest_title)
            UpdateResult.Failed -> stringResource(R.string.update_failed_title)
        },
        onDismiss = onDismiss,
        line = when (r) {
            null -> stringResource(R.string.update_asking)
            is UpdateResult.Newer -> stringResource(R.string.update_versions, r.version, UpdateCheck.plain(BuildConfig.VERSION_NAME))
            is UpdateResult.Latest -> stringResource(R.string.version_line, UpdateCheck.plain(BuildConfig.VERSION_NAME))
            UpdateResult.Failed -> stringResource(R.string.update_failed_line)
        },
        footer = when (r) {
            null -> null
            is UpdateResult.Newer -> { {
                ButtonPair(
                    secondary = { m -> SecondaryButton(stringResource(R.string.later), onDismiss, m) },
                    primary = { m -> PrimaryButton(stringResource(R.string.update_open), { onOpen(r.page) }, m) },
                )
            } }
            is UpdateResult.Latest -> { { PrimaryButton(stringResource(R.string.done), onDismiss, Modifier.fillMaxWidth()) } }
            UpdateResult.Failed -> { {
                ButtonPair(
                    secondary = { m -> SecondaryButton(stringResource(R.string.close), onDismiss, m) },
                    primary = { m -> PrimaryButton(stringResource(R.string.try_again), onRetry, m) },
                )
            } }
        },
    ) {
        when {
            r == null -> {
                // Asking GitHub: a line that keeps moving, never a spinner (17a).
                MovingLine(Modifier.padding(horizontal = Space.heading, vertical = Space.block))
            }
            r is UpdateResult.Newer && r.notes.isNotEmpty() -> Box24(Modifier.padding(horizontal = Space.screen), title = stringResource(R.string.whats_new)) {
                r.notes.forEach { n ->
                    Row(Modifier.padding(horizontal = Space.card, vertical = Space.small), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(Size.waitingDot).clip(Shapes.pill).background(c.primary))
                        Spacer(Modifier.width(Space.block))
                        Text(n, style = UnpeepType.supporting, color = c.onSurface)
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.block))
    }
}
