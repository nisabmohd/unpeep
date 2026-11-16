package app.keyhole.privacy.ui.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.R
import app.keyhole.privacy.data.ReportRepository
import app.keyhole.privacy.data.SavedFinding
import app.keyhole.privacy.data.SavedReport
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Finding
import app.keyhole.privacy.domain.FindingSource
import app.keyhole.privacy.domain.Gap
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.report.PdfReport
import app.keyhole.privacy.ui.components.Box24
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.CheckRow
import app.keyhole.privacy.ui.components.CheckState
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.KeyholeSheet
import app.keyhole.privacy.ui.components.KeyholeTextField
import app.keyhole.privacy.ui.components.LensHeader
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.Note
import app.keyhole.privacy.ui.components.PhotoThumb
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.RoundIconButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.SignRow
import app.keyhole.privacy.ui.components.Tag
import app.keyhole.privacy.ui.components.TagKind
import app.keyhole.privacy.ui.components.colors
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.warningColors
import app.keyhole.privacy.ui.wifi.tone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReportState(val report: SavedReport? = null, val loaded: Boolean = false)

class ReportViewModel(private val id: Long, private val repo: ReportRepository) : ViewModel() {
    val state: StateFlow<ReportState> = repo.report(id).map { ReportState(it, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportState())

    fun rename(name: String) {
        viewModelScope.launch { repo.rename(id, name) }
    }

    fun undo(finding: SavedFinding) {
        viewModelScope.launch { repo.undoMark(finding) }
    }

    /** Writes the PDF to [uri] (the place the user chose); true when it worked. */
    suspend fun export(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val report = state.value.report ?: return@withContext false
        runCatching {
            val font = runCatching { ResourcesCompat.getFont(context, R.font.lexend) }.getOrNull()
            context.contentResolver.openOutputStream(uri)?.use { out -> PdfReport(ReportWords(context.resources), context.resources, font).write(report, out) } ?: error("no stream")
        }.isSuccess
    }
}

/** The icon of a finding: the check it came from, or the device's kind. */
private fun Finding.icon(): Int = when (source) {
    FindingSource.WIFI -> if (risk == Risk.LIKELY_CAMERA) R.drawable.ic_camera_device else R.drawable.ic_wifi
    FindingSource.HOTSPOT -> if (risk == Risk.LIKELY_CAMERA) R.drawable.ic_camera_device else R.drawable.ic_hotspot
    FindingSource.BLUETOOTH -> R.drawable.ic_bluetooth
    FindingSource.CHECKLIST -> if (photos > 0) R.drawable.ic_gallery else R.drawable.ic_checklist
    FindingSource.MAGNET -> R.drawable.ic_magnet
}

private fun Finding.circle(): CircleTone = if (knownAs != null) CircleTone.Neutral else risk.tone

private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(Intent.createChooser(send, null)) }
}

/**
 * 10 Report: lens header (what to look at, how many checks) · the check row and what was skipped · Found · Photos ·
 * Marked as known (Undo) · Couldn't check (always) · Found something? / Export.
 */
@Composable
fun ReportScreen(vm: ReportViewModel, onBack: () -> Unit, onFinding: (Long) -> Unit, onPhoto: (String) -> Unit, onFoundSomething: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val gone = s.loaded && s.report == null
    LaunchedEffect(gone) { if (gone) onBack() }
    val report = s.report ?: return
    val context = LocalContext.current
    val words = rememberReportWords()
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var renaming by rememberSaveable { mutableStateOf(false) }
    var undoing by rememberSaveable { mutableStateOf<Long?>(null) }
    var exportResult by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF)) { uri ->
        if (uri != null) scope.launch {
            val ok = vm.export(context, uri)
            haptics.buzz(if (ok) Buzz.CONFIRM else Buzz.REJECT)
            exportResult = ok
        }
    }
    ReportContent(
        report, words, onBack, onShare = { share(context, words.text(report)) }, onRename = { renaming = true },
        onFinding = { f -> if (f.finding.toLookAt) onFinding(f.id) else undoing = f.id }, onPhoto = onPhoto,
        onFoundSomething = onFoundSomething, onExport = { export.launch(words.name(report).replace(Regex("[\\\\/:*?\"<>|]"), " ") + ".pdf") },
        exportResult = exportResult,
    )
    if (renaming) RenameSheet(report.name.ifBlank { "" }, onDismiss = { renaming = false }, onSave = { haptics.buzz(Buzz.CONFIRM); vm.rename(it); renaming = false })
    undoing?.let { id ->
        val finding = report.findings.firstOrNull { it.id == id }
        if (finding == null) undoing = null else UndoSheet(words.title(finding.finding), onDismiss = { undoing = null }, onUndo = { haptics.buzz(Buzz.TOGGLE_OFF); vm.undo(finding); undoing = null })
    }
}

@Composable
fun ReportContent(
    report: SavedReport, words: ReportWords, onBack: () -> Unit, onShare: () -> Unit, onRename: () -> Unit, onFinding: (SavedFinding) -> Unit,
    onPhoto: (String) -> Unit, onFoundSomething: () -> Unit, onExport: () -> Unit, exportResult: Boolean? = null,
) {
    val c = MaterialTheme.colorScheme
    val w = warningColors()
    val look = report.toLookAt > 0
    DetailScreen(
        onBack,
        actions = {
            RoundIconButton(painterResource(R.drawable.ic_pen), stringResource(R.string.rename), onRename)
            RoundIconButton(painterResource(R.drawable.ic_share), stringResource(R.string.share), onShare)
        },
        dock = {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.found_something), onFoundSomething, m) },
                primary = { m -> PrimaryButton(stringResource(R.string.export), onExport, m, icon = painterResource(R.drawable.ic_export)) },
            )
        },
    ) {
        // Notes go first: how the export went.
        when (exportResult) {
            true -> Note(stringResource(R.string.export_done))
            false -> Note(stringResource(R.string.export_failed), icon = painterResource(R.drawable.ic_warning), warning = true)
            null -> Unit
        }
        LensHeader(
            painterResource(if (look) R.drawable.ic_shield_warning else R.drawable.ic_shield), words.name(report), line = words.whenLine(report),
            container = if (look) w.container else null, onContainer = if (look) w.onContainer else null,
            tags = {
                if (look) Tag(words.toLookAt(report), TagKind.WORTH_A_LOOK, icon = painterResource(R.drawable.ic_shield_warning)) else Tag(stringResource(R.string.nothing_found), TagKind.GOOD)
                Tag(words.checksOf(report), TagKind.KNOWN)
            },
        )
        Spacer(Modifier.height(Space.block))
        Box24 {
            CheckRow(report.planned, state = { check -> if (check in report.ran) CheckState.DONE else CheckState.SKIPPED })
            Spacer(Modifier.height(Space.inline))
            Text(words.skippedLine(report), style = KeyholeType.supporting, color = c.onSurfaceVariant, modifier = Modifier.padding(horizontal = Space.card))
        }
        val found = report.findings.filter { it.finding.toLookAt }
        if (found.isNotEmpty()) {
            SectionLabel(stringResource(R.string.found), meta = found.size.toString())
            Grouped {
                found.forEach { f ->
                    row { shape ->
                        ListRow(
                            words.title(f.finding), shape = shape, line = words.line(f.finding),
                            leading = { IconCircle(painterResource(f.finding.icon()), f.finding.circle()) }, onClick = { onFinding(f) },
                        )
                    }
                }
            }
        }
        if (report.photos.isNotEmpty()) {
            SectionLabel(stringResource(R.string.photos), meta = report.photos.size.toString())
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.inline)) {
                report.photos.forEachIndexed { i, path ->
                    PhotoThumb(path, stringResource(R.string.two_parts, stringResource(R.string.photo), (i + 1).toString()), onClick = { onPhoto(path) })
                }
            }
        }
        val known = report.findings.filter { !it.finding.toLookAt }
        if (known.isNotEmpty()) {
            SectionLabel(stringResource(R.string.marked_as_known), meta = known.size.toString())
            Grouped {
                known.forEach { f ->
                    row { shape ->
                        ListRow(
                            words.title(f.finding), shape = shape,
                            line = stringResource(if (f.finding.knownAs == KnownAs.ROOM) R.string.marked_room_undo else R.string.marked_mine_undo),
                            leading = { IconCircle(painterResource(f.finding.icon()), CircleTone.Neutral) }, onClick = { onFinding(f) }, chevron = false,
                        )
                    }
                }
            }
        }
        SectionLabel(stringResource(R.string.couldnt_check))
        Grouped {
            report.gaps.forEach { g -> row { shape -> SignRow(painterResource(R.drawable.ic_not_checked), words.gap(g), shape = shape) } }
        }
        Spacer(Modifier.height(Space.block))
        Note(stringResource(R.string.report_not_safe_note))
    }
}

/** Naming a report: one field, Save. An empty name goes back to the place and date. */
@Composable
fun RenameSheet(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    KeyholeSheet(
        stringResource(R.string.rename_title), onDismiss, line = stringResource(R.string.rename_line),
        footer = {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.cancel), onDismiss, m) },
                primary = { m -> PrimaryButton(stringResource(R.string.save), { onSave(name) }, m) },
            )
        },
    ) {
        Box(Modifier.padding(horizontal = Space.screen, vertical = Space.inline)) {
            KeyholeTextField(name, { name = it }, stringResource(R.string.report_name_hint), stringResource(R.string.rename), maxLength = MAX_NAME)
        }
    }
}

/** Taking a "Not a camera?" mark away from the report (and from the phone). */
@Composable
fun UndoSheet(title: String, onDismiss: () -> Unit, onUndo: () -> Unit) {
    KeyholeSheet(
        stringResource(R.string.undo_title), onDismiss, line = stringResource(R.string.undo_line),
        footer = {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.cancel), onDismiss, m) },
                primary = { m -> PrimaryButton(stringResource(R.string.undo), onUndo, m) },
            )
        },
    ) {
        Text(title, style = KeyholeType.cardTitle, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = Space.heading))
    }
}

private const val PDF = "application/pdf"
private const val MAX_NAME = 80

internal fun previewReport() = SavedReport(
    1, "Sea View Hotel, room 304", Place.STAY, 0, 1_794_800_000_000, Check.entries, Check.entries - Check.INFRARED, "SeaView_Guest",
    listOf(Gap.NotRun(Check.INFRARED), Gap.OtherNetwork, Gap.MemoryCard),
    listOf(
        SavedFinding(1, Finding(FindingSource.WIFI, "IPC-3F2A", "192.168.1.23", Risk.LIKELY_CAMERA, listOf("ONVIF", "RTSP:554"), "Xiongmai", "x|IPC-3F2A")),
        SavedFinding(2, Finding(FindingSource.CHECKLIST, "SMOKE_DETECTOR", note = "Dot on the side", photos = 1)),
        SavedFinding(3, Finding(FindingSource.WIFI, "Living-room TV", "192.168.1.19", Risk.CHECK_THIS, listOf("WEB"), markKey = "x|tv", knownAs = KnownAs.ROOM)),
    ),
    emptyList(), toLookAt = 2,
)

@Preview
@Composable
private fun ReportPreview() = KeyholeTheme {
    ReportContent(previewReport(), rememberReportWords(), {}, {}, {}, {}, {}, {}, {})
}
