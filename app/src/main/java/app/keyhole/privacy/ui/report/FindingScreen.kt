package app.keyhole.privacy.ui.report

import androidx.compose.ui.res.pluralStringResource
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.components.LocalToasts
import app.keyhole.privacy.domain.Finding
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.FindingSource
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.ui.components.BackTopBar
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.LensHeader
import app.keyhole.privacy.ui.components.Note
import app.keyhole.privacy.ui.components.RoundIconButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.SignRow
import app.keyhole.privacy.ui.components.Tag
import app.keyhole.privacy.ui.components.TagKind
import app.keyhole.privacy.ui.components.ValueRow
import app.keyhole.privacy.ui.components.colors
import app.keyhole.privacy.ui.components.decodeScaled
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.Viewfinder
import app.keyhole.privacy.ui.wifi.label
import app.keyhole.privacy.ui.wifi.tone
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One finding of a saved report: what it is, why it was flagged, your note. The same header as every detail screen. */
@Composable
fun FindingScreen(vm: ReportViewModel, findingId: Long, onBack: () -> Unit, onFoundIt: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val finding = s.report?.findings?.firstOrNull { it.id == findingId }
    val gone = s.loaded && finding == null
    LaunchedEffect(gone) { if (gone) onBack() }
    val f = finding?.finding ?: return
    val context = LocalContext.current
    val toasts = LocalToasts.current
    val haptics = rememberHaptics()
    val removed = stringResource(R.string.removed_from_report)
    val words = rememberReportWords()
    FindingContent(
        f, onBack,
        onShare = { share(context, (listOf(words.title(f), words.line(f)) + words.reasons(f)).joinToString("\n")) },
        onRemove = {
            haptics.buzz(Buzz.TOGGLE_OFF)
            vm.removeFinding(findingId) { undo -> toasts.show(removed, undo = undo) }
            onBack()
        },
        onFoundIt = onFoundIt,
    )
}

/** One finding of a report: lens header with its result, why, its note or address; Remove from report / Found it? (10e). */
@Composable
fun FindingContent(f: Finding, onBack: () -> Unit, onShare: () -> Unit = {}, onRemove: () -> Unit = {}, onFoundIt: () -> Unit = {}) {
    val words = rememberReportWords()
    val (container, onContainer) = (if (f.knownAs != null) Risk.KNOWN else f.risk).tone.colors()
    DetailScreen(
        onBack,
        actions = { RoundIconButton(painterResource(R.drawable.ic_share), stringResource(R.string.share), onShare) },
        dock = {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.remove_from_report), onRemove, m) },
                primary = { m -> PrimaryButton(stringResource(R.string.found_it), onFoundIt, m) },
            )
        },
    ) {
        LensHeader(
            painterResource(
                when (f.source) {
                    FindingSource.WIFI, FindingSource.HOTSPOT -> if (f.risk == Risk.LIKELY_CAMERA) R.drawable.ic_camera_device else R.drawable.ic_wifi
                    FindingSource.BLUETOOTH -> R.drawable.ic_bluetooth
                    FindingSource.CHECKLIST -> R.drawable.ic_checklist
                    FindingSource.MAGNET -> R.drawable.ic_magnet
                },
            ),
            words.title(f), line = words.line(f), container = container, onContainer = onContainer,
            tags = {
                when {
                    f.knownAs != null -> Tag(stringResource(R.string.marked_as_known), TagKind.KNOWN)
                    f.risk == Risk.LIKELY_CAMERA -> Tag(stringResource(f.risk.label), TagKind.LIKELY_CAMERA, icon = painterResource(R.drawable.ic_warning))
                    else -> Tag(stringResource(Risk.CHECK_THIS.label), TagKind.WORTH_A_LOOK)
                }
                // A checklist answer says how it was answered (10e: "Not sure").
                if (f.source == FindingSource.CHECKLIST && f.knownAs == null) Tag(stringResource(R.string.not_sure), TagKind.KNOWN)
            },
        )
        SectionLabel(stringResource(R.string.why))
        Grouped {
            words.reasons(f).forEach { reason -> row { shape -> SignRow(painterResource(R.drawable.ic_info), reason, shape = shape, alert = f.risk == Risk.LIKELY_CAMERA && f.knownAs == null) } }
            if (f.photos > 0) row { shape -> SignRow(painterResource(R.drawable.ic_gallery), pluralStringResource(R.plurals.you_saved_photos, f.photos, f.photos), shape = shape) }
        }
        if (f.note.isNotBlank() && f.source == FindingSource.CHECKLIST) {
            Spacer(Modifier.height(Space.block))
            Note(f.note, title = stringResource(R.string.finding_note), icon = painterResource(R.drawable.ic_notes))
        }
        if (f.source == FindingSource.WIFI) {
            Spacer(Modifier.height(Space.block))
            Grouped { row { shape -> ValueRow(stringResource(R.string.address), f.detail, shape = shape) } }
        }
    }
}

/** A proof photo, as large as the screen allows, on the viewfinder's dark page; Share sends the file through the share sheet. */
@Composable
fun PhotoScreen(path: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val px = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) { bitmap = withContext(Dispatchers.IO) { decodeScaled(path, px)?.asImageBitmap() } }
    app.keyhole.privacy.ui.theme.ViewfinderTheme {
        Column(Modifier.fillMaxSize().background(Viewfinder.page).statusBarsPadding()) {
            BackTopBar(onBack, title = stringResource(R.string.photo), actions = {
                RoundIconButton(painterResource(R.drawable.ic_share), stringResource(R.string.share), {
                    val file = File(path)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                        val send = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        runCatching { context.startActivity(Intent.createChooser(send, null)) }
                    }
                })
            })
            Box(Modifier.fillMaxSize().padding(Space.screen), contentAlignment = Alignment.Center) {
                bitmap?.let { Image(it, stringResource(R.string.photo), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}
