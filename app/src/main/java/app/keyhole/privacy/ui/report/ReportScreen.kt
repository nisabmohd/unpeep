package app.keyhole.privacy.ui.report

import app.keyhole.privacy.ui.icon
import app.keyhole.privacy.ui.components.LocalToasts
import androidx.compose.runtime.remember
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
import app.keyhole.privacy.ui.components.NoteTone
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

    /** Finding detail › Remove from report; [onRemoved] gets the way back for the toast's Undo. */
    fun removeFinding(findingId: Long, onRemoved: (undo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            val gone = repo.removeFinding(findingId) ?: return@launch
            onRemoved { repo.writes.launch { repo.restoreFinding(gone) } }
        }
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

/** Opens the saved PDF in whatever reads PDFs on the phone. */
private fun openPdf(context: Context, uri: Uri) {
    val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, PDF).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(view) }
}

internal fun share(context: Context, text: String) {
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
    val toasts = LocalToasts.current
    val deleted = stringResource(R.string.report_gone)
    LaunchedEffect(gone) {
        if (gone) {
            toasts.show(deleted) // E13: back on Reports, it says why
            onBack()
        }
    }
    val report = s.report ?: return
    val context = LocalContext.current
    val words = rememberReportWords()
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var renaming by rememberSaveable { mutableStateOf(false) }
    var undoing by rememberSaveable { mutableStateOf<Long?>(null) }
    var exporting by remember { mutableStateOf(false) }
    val savedText = stringResource(R.string.export_done)
    val openText = stringResource(R.string.open)
    val failedText = stringResource(R.string.export_failed)
    val retryText = stringResource(R.string.try_again)
    val pdfName = words.name(report).replace(Regex("[\\\\/:*?\"<>|]"), " ") + ".pdf"
    // Try again opens the same "save as" (the launcher can't name itself while it is being made).
    val retry = remember { arrayOf<() -> Unit>({}) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF)) { uri ->
        if (uri != null) scope.launch {
            exporting = true
            val ok = try { vm.export(context, uri) } finally { exporting = false }
            haptics.buzz(if (ok) Buzz.CONFIRM else Buzz.REJECT)
            // Either way a toast says what just happened (one look for events): saved, with Open (10h); not saved,
            // with Try again (10i).
            if (ok) toasts.show(savedText, action = openText, onAction = { openPdf(context, uri) })
            else toasts.show(failedText, action = retryText, onAction = { retry[0]() }, icon = R.drawable.ic_info)
        }
    }
    retry[0] = { export.launch(pdfName) }
    ReportContent(
        report, words, onBack, onShare = { share(context, words.text(report)) }, onRename = { renaming = true },
        onFinding = { f -> if (f.finding.toLookAt) onFinding(f.id) else undoing = f.id }, onPhoto = onPhoto,
        onFoundSomething = onFoundSomething, onExport = { export.launch(pdfName) },
        exporting = exporting,
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
    onPhoto: (String) -> Unit, onFoundSomething: () -> Unit, onExport: () -> Unit,
    exporting: Boolean = false, onOpenPdf: (() -> Unit)? = null,
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
                // While the PDF is written the button says so (10j) and waits.
                primary = { m ->
                    PrimaryButton(stringResource(if (exporting) R.string.saving_pdf else R.string.export), onExport, m, icon = painterResource(R.drawable.ic_export), enabled = !exporting)
                },
            )
        },
    ) {
        LensHeader(
            painterResource(if (look) R.drawable.ic_shield_warning else R.drawable.ic_check_circle), words.name(report), line = words.whenLine(report),
            container = if (look) w.container else null, onContainer = if (look) w.onContainer else null,
            tags = {
                if (look) Tag(words.toLookAt(report), TagKind.WORTH_A_LOOK, icon = painterResource(R.drawable.ic_danger))
                else Tag(stringResource(R.string.nothing_found), TagKind.GOOD, icon = painterResource(R.drawable.ic_check_circle))
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
            // The same row as every other list, calm (10b): a title, a muted line, and in a grey circle the skipped check's
            // own icon or the thing no scan can see (memory card, antenna, shield). Never an alarm colour.
            report.gaps.forEach { g ->
                row { shape ->
                    ListRow(words.gapTitle(g), shape = shape, line = words.gapLine(g), leading = { IconCircle(painterResource(ReportWords.gapIcon(g)), CircleTone.Neutral) })
                }
            }
        }
        Spacer(Modifier.height(Space.block))
        Note(stringResource(R.string.report_not_safe_note), tone = NoteTone.HONESTY)
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
