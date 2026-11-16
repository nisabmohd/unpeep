package app.unpeep.privacy.ui.lens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import app.unpeep.privacy.ui.components.FinderPanel
import app.unpeep.privacy.optics.onScreen
import app.unpeep.privacy.ui.theme.shortSpec
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxHeight
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.TextureView
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.unpeep.privacy.R
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.optics.CameraController
import app.unpeep.privacy.optics.CameraSpec
import app.unpeep.privacy.optics.Facing
import app.unpeep.privacy.optics.Spot
import app.unpeep.privacy.optics.cropScale
import app.unpeep.privacy.optics.toView
import app.unpeep.privacy.scan.Access
import app.unpeep.privacy.ui.components.BackTopBar
import app.unpeep.privacy.ui.components.Buzz
import app.unpeep.privacy.ui.components.CameraPreview
import app.unpeep.privacy.ui.components.Chip
import app.unpeep.privacy.ui.components.ChoiceRow
import app.unpeep.privacy.ui.components.CircleTone
import app.unpeep.privacy.ui.components.Dock
import app.unpeep.privacy.ui.components.EmptyState
import app.unpeep.privacy.ui.components.Grouped
import app.unpeep.privacy.ui.components.IconCircle
import app.unpeep.privacy.ui.components.UnpeepSheet
import app.unpeep.privacy.ui.components.Note
import app.unpeep.privacy.ui.components.NoteTone
import app.unpeep.privacy.ui.components.PrimaryButton
import app.unpeep.privacy.ui.components.RoundIconButton
import app.unpeep.privacy.ui.components.Segment
import app.unpeep.privacy.ui.components.Segmented
import app.unpeep.privacy.ui.components.SheetLine
import app.unpeep.privacy.ui.components.Shutter
import app.unpeep.privacy.ui.components.SignRow
import app.unpeep.privacy.ui.components.SpotMark
import app.unpeep.privacy.ui.components.SpotRings
import app.unpeep.privacy.ui.components.Tag
import app.unpeep.privacy.ui.components.TagKind
import app.unpeep.privacy.ui.components.TopBarAction
import app.unpeep.privacy.ui.components.rememberHaptics
import app.unpeep.privacy.ui.drawEdgeToEdge
import app.unpeep.privacy.ui.openAppSettings
import app.unpeep.privacy.ui.rememberPermissionAsk
import app.unpeep.privacy.ui.theme.Hues
import app.unpeep.privacy.ui.theme.UnpeepTheme
import app.unpeep.privacy.ui.theme.UnpeepType
import app.unpeep.privacy.ui.theme.RedFilter
import app.unpeep.privacy.ui.theme.Size
import app.unpeep.privacy.ui.theme.Space
import app.unpeep.privacy.ui.theme.ViewfinderTheme
import app.unpeep.privacy.ui.theme.isAppDark

/**
 * 08 Lens finder (and Infrared): the camera picture with rings around possible lenses · filters and 2× · Lens or
 * Infrared · the hint · tips, the shutter and the torch. [onNext] moves on in a sweep (null: the sweep is over).
 */
@Composable
fun LensScreen(vm: LensViewModel, onBack: () -> Unit, onNext: (Check?) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val event by vm.events.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val haptics = rememberHaptics()
    val appDark = isAppDark()

    // A camera screen: held upright (the picture and the rings line up), with light bar icons on its dark page.
    // Android 16 ignores the lock on large screens: tablets are on TODO.md (design review + device test).
    @SuppressLint("SourceLockedOrientationActivity")
    DisposableEffect(activity) {
        val before = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.drawEdgeToEdge(dark = true)
        onDispose {
            if (before != null) activity.requestedOrientation = before
            activity?.drawEdgeToEdge(appDark)
        }
    }

    // Allowed? Checked each time the screen comes back (from Android's question or from Settings).
    LifecycleResumeEffect(Unit) {
        vm.setGranted(Access.granted(context, Check.LENS))
        onPauseOrDispose { }
    }
    val ask = rememberPermissionAsk(Check.LENS) { vm.setGranted(it) }

    // The camera: opened while the screen is on top, closed the moment it isn't.
    val controller = remember { CameraController(context) }
    var texture by remember { mutableStateOf<TextureView?>(null) }
    val spec = remember(s.facing, s.attempt) { controller.pick(s.facing) }
    LaunchedEffect(spec) { vm.onSpec(spec) }
    val granted = s.status != CameraStatus.NEEDS_PERMISSION
    LifecycleResumeEffect(granted, texture, spec, s.attempt) {
        val surface = texture?.surfaceTexture
        // Asked here too, not only from the state: on a first run the state starts as "starting" before the
        // permission check has run, and opening the camera without it ends in "Can't open the camera".
        if (granted && Access.granted(context, Check.LENS) && surface != null && spec != null) {
            controller.start(spec, surface, vm.state.value.camera, vm, onRunning = vm::onRunning, onProblem = vm::onProblem)
        }
        onPauseOrDispose { controller.stop() }
    }
    LaunchedEffect(s.camera) { controller.update(s.camera) }
    DisposableEffect(Unit) { onDispose { controller.stop() } }
    LaunchedEffect(texture, s.filter) { texture?.setLayerPaint(filterPaint(s.filter)) }

    // One buzz per event, the same feel everywhere (MOTION.md § Haptics).
    var photoFailed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(event) {
        when (event?.second) {
            LensEvent.PHOTO_SAVED -> { haptics.buzz(Buzz.CONFIRM); photoFailed = false }
            LensEvent.PHOTO_FAILED -> { haptics.buzz(Buzz.REJECT); photoFailed = true }
            LensEvent.SPOT_FOUND -> haptics.buzz(Buzz.FOUND)
            LensEvent.CAMERA_PROBLEM -> haptics.buzz(Buzz.REJECT)
            null -> Unit
        }
    }

    var tips by rememberSaveable { mutableStateOf(false) }
    // The TV-remote question opens by itself once, the first time Infrared runs. Answered or closed, it stays closed;
    // Tips › Test again asks it again.
    var testDismissed by rememberSaveable { mutableStateOf(false) }
    val askTest = s.mode == Check.INFRARED && s.seesInfrared == null && !s.infraredAsked && !testDismissed && s.status == CameraStatus.RUNNING

    ViewfinderTheme {
        LensContent(
            s, onBack = onBack,
            onNext = { if (s.next == Check.INFRARED && s.mode == Check.LENS) vm.setMode(Check.INFRARED) else onNext(s.next) },
            onMode = vm::setMode, onFilter = vm::setFilter,
            onZoom = { haptics.buzz(if (s.zoom2x) Buzz.TOGGLE_OFF else Buzz.TOGGLE_ON); vm.toggleZoom() },
            onTorch = { haptics.buzz(if (s.torch) Buzz.TOGGLE_OFF else Buzz.TOGGLE_ON); vm.toggleTorch() },
            onTips = { tips = true },
            onFlip = { haptics.buzz(Buzz.TICK); vm.flip() },
            onShutter = { vm.onPhoto(texture?.bitmap) },
            onAllow = { if (ask.settingsOnly) openAppSettings(context) else ask.ask() }, settingsOnly = ask.settingsOnly,
            onRetry = vm::retry, photoFailed = photoFailed,
            preview = { modifier -> if (spec != null) CameraPreview(spec.uprightWidth, spec.uprightHeight, { texture = it }, modifier, stringResource(R.string.camera_preview)) },
        )
        if (tips) LensTipsSheet(s.mode, onDismiss = { tips = false }, onTestAgain = { tips = false; testDismissed = false; vm.resetInfraredTest() })
        if (askTest) InfraredTestSheet(onDismiss = { testDismissed = true; vm.skipInfraredTest() }, onAnswer = { sees -> haptics.buzz(Buzz.TICK); vm.setSeesInfrared(sees); testDismissed = true })
    }
}

/** The preview's look: as it is, red with the other colours dimmed (glints stand out), or more contrast. */
private fun filterPaint(filter: LensFilter): Paint? = when (filter) {
    LensFilter.NONE -> null
    LensFilter.RED -> Paint().apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    RedFilter.red * RED_BOOST, 0f, 0f, 0f, 0f,
                    0f, RedFilter.green, 0f, 0f, 0f,
                    0f, 0f, RedFilter.blue, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
    LensFilter.CONTRAST -> Paint().apply {
        val shift = (1f - CONTRAST) / 2f * CHANNEL_MAX
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    CONTRAST, 0f, 0f, 0f, shift,
                    0f, CONTRAST, 0f, 0f, shift,
                    0f, 0f, CONTRAST, 0f, shift,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
}

private const val RED_BOOST = 1.3f
private const val CONTRAST = 1.8f
private const val CHANNEL_MAX = 255f

@Composable
fun LensContent(
    s: LensState, onBack: () -> Unit, onNext: () -> Unit, onMode: (Check) -> Unit, onFilter: (LensFilter) -> Unit,
    onZoom: () -> Unit, onTorch: () -> Unit, onTips: () -> Unit, onShutter: () -> Unit, onAllow: () -> Unit, settingsOnly: Boolean,
    onRetry: () -> Unit, photoFailed: Boolean = false, preview: @Composable (Modifier) -> Unit = {}, onFlip: () -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    val running = s.status == CameraStatus.RUNNING || s.status == CameraStatus.STARTING
    // On its side (T05, large screens ignore the portrait lock): the controls move to a panel on the trailing side.
    val sideways = app.unpeep.privacy.ui.windowSize().let { it.width > it.height }
    // The spots on screen: the camera sees a little more than the picture shows (centre crop), so the count and the
    // rings are the same spots, never "3 spots" with nothing ringed.
    var box by remember { mutableStateOf(IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val marks = s.spec?.takeIf { box.width > 0 }?.let { spec ->
        val scale = cropScale(spec.uprightWidth.toFloat(), spec.uprightHeight.toFloat(), box.width.toFloat(), box.height.toFloat())
        val clear = if (sideways || box.height == 0) 1f else 1f - with(density) { Size.panelCorner.toPx() } / box.height
        s.spots.map { val (x, y) = toView(it.x, it.y, scale); SpotMark(x, y, it.id) }.filter { onScreen(it.x, it.y) && it.y <= clear }
    }.orEmpty()
    val topBar: @Composable () -> Unit = {
        BackTopBar(
            onBack, Modifier.widthIn(max = Size.contentMax),
            title = stringResource(if (s.mode == Check.INFRARED) R.string.infrared_finder else R.string.lens_finder), close = s.sweep,
            // Back, title, Next in a sweep, tips (review 2): the spot count sits on the picture, so all of it fits 360 dp.
            actions = {
                if (s.sweep) TopBarAction(stringResource(if (s.next == null) R.string.see_report else R.string.next), onNext)
                if (!sideways) RoundIconButton(painterResource(R.drawable.ic_help), stringResource(R.string.tips), onTips)
            },
        )
    }
    val picture: @Composable (Modifier) -> Unit = { m ->
        Box(m.onSizeChanged { box = it }) {
            when (s.status) {
                CameraStatus.NEEDS_PERMISSION -> CameraMessage(R.drawable.ic_camera, R.string.perm_camera_title, if (settingsOnly) R.string.perm_denied_line else R.string.perm_camera_line)
                CameraStatus.IN_USE -> CameraMessage(R.drawable.ic_camera, R.string.camera_problem_title, R.string.camera_in_use_line)
                CameraStatus.FAILED -> CameraMessage(R.drawable.ic_camera, R.string.camera_problem_title, R.string.camera_failed_line)
                CameraStatus.NO_CAMERA -> CameraMessage(R.drawable.ic_camera, R.string.no_camera_title, R.string.block_no_bluetooth_line)
                CameraStatus.STARTING, CameraStatus.RUNNING -> {
                    // The picture shows only once this camera is running, and fades in; switching Lens ↔ Infrared
                    // never shows the other camera's last frame, stretched (08e: "Opening the camera").
                    val shown by animateFloatAsState(if (s.status == CameraStatus.RUNNING) 1f else 0f, shortSpec(), label = "picture")
                    preview(Modifier.fillMaxSize().graphicsLayer { alpha = shown })
                    if (s.status == CameraStatus.STARTING) {
                        Text(
                            stringResource(R.string.opening_camera), style = UnpeepType.supporting, color = c.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    SpotRings(marks, stringResource(R.string.look_here), pluralStringResource(R.plurals.spots_ringed, marks.size, marks.size))
                    // On the picture, under the top bar: the spot count, then any Note.
                    val spots: @Composable () -> Unit = { Tag(pluralStringResource(R.plurals.spots_count, marks.size, marks.size), TagKind.KNOWN) }
                    Column(
                        Modifier.statusBarsPadding().padding(top = Size.topBar).padding(horizontal = Space.screen, vertical = Space.block),
                        verticalArrangement = Arrangement.spacedBy(Space.inline),
                    ) {
                        spots()
                        if (s.mode == Check.INFRARED && s.seesInfrared == false) {
                            Note(stringResource(R.string.ir_blocked_text), title = stringResource(R.string.ir_blocked_title), tone = NoteTone.WARNING)
                        }
                        if (photoFailed) Note(stringResource(R.string.photo_not_saved), tone = NoteTone.ERROR)
                    }
                }
            }
        }
    }
    if (sideways) {
        // T05: the picture on the left with the top bar (and tips) over it; every control on a panel on the trailing
        // side: Lens · Infrared, the filters two by two, then Switch camera · shutter · torch at the bottom. Lens and
        // the torch share an icon, so they are never two round buttons in one rail.
        Row(Modifier.fillMaxSize().background(c.background)) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                picture(Modifier.fillMaxSize())
                Row(Modifier.fillMaxWidth().statusBarsPadding().align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { topBar() }
                    RoundIconButton(painterResource(R.drawable.ic_help), stringResource(R.string.tips), onTips, Modifier.padding(end = Space.screen))
                }
            }
            FinderPanel(Modifier.navigationBarsPadding(), sideways = true) {
                Column(
                    Modifier.fillMaxHeight().statusBarsPadding().padding(vertical = Space.screen),
                    verticalArrangement = Arrangement.spacedBy(Space.block),
                ) {
                LensModes(s, onMode, Modifier.padding(horizontal = Space.screen))
                if (running) LensFilterGrid(s, onFilter, onZoom, Modifier.padding(horizontal = Space.screen))
                if (running) LensHint(s, Modifier.fillMaxWidth().padding(horizontal = Space.screen), oneLine = false)
                Spacer(Modifier.weight(1f))
                when (s.status) {
                    CameraStatus.NEEDS_PERMISSION -> PrimaryButton(stringResource(if (settingsOnly) R.string.open_settings else R.string.allow), onAllow, Modifier.fillMaxWidth().padding(horizontal = Space.screen))
                    CameraStatus.IN_USE, CameraStatus.FAILED -> PrimaryButton(stringResource(R.string.try_again), onRetry, Modifier.fillMaxWidth().padding(horizontal = Space.screen), icon = painterResource(R.drawable.ic_refresh))
                    else -> if (running) ShutterControls(s, onTorch, onFlip, onShutter, Modifier.padding(horizontal = Space.screen))
                }
                }
            }
        }
        return
    }
    // Picture and panel (final v2): the picture runs from the top edge, behind the status bar and the top bar, and
    // stops at a rounded panel that holds every control, so a ring is never covered. Only the top bar and the spot
    // count sit on the picture.
    Box(Modifier.fillMaxSize().background(c.background)) {
        val panel = Size.finderPanel + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // Under the panel's rounded corners the picture still shows; spots there are never ringed (see marks).
        picture(Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = panel - Size.panelCorner))
        Box(Modifier.statusBarsPadding()) { topBar() }
        FinderPanel(Modifier.align(Alignment.BottomCenter).heightIn(min = panel)) {
            LensControls(s, running, onMode, onFilter, onZoom, onTorch, onFlip, onShutter)
            when (s.status) {
                CameraStatus.NEEDS_PERMISSION -> PrimaryButton(stringResource(if (settingsOnly) R.string.open_settings else R.string.allow), onAllow, Modifier.fillMaxWidth().padding(horizontal = Space.screen))
                CameraStatus.IN_USE, CameraStatus.FAILED -> PrimaryButton(stringResource(R.string.try_again), onRetry, Modifier.fillMaxWidth().padding(horizontal = Space.screen), icon = painterResource(R.drawable.ic_refresh))
                else -> Unit
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun CameraMessage(icon: Int, title: Int, line: Int) {
    Column(Modifier.fillMaxSize().padding(horizontal = Space.screen)) {
        EmptyState(painterResource(icon), stringResource(title), stringResource(line))
    }
}

/** No filter · Red · Contrast · 2×, in one row that scrolls sideways when it must. */
@Composable
private fun LensFilters(s: LensState, onFilter: (LensFilter) -> Unit, onZoom: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.inline)) {
        Chip(stringResource(R.string.filter_none), s.filter == LensFilter.NONE, { onFilter(LensFilter.NONE) })
        Chip(stringResource(R.string.filter_red), s.filter == LensFilter.RED, { onFilter(LensFilter.RED) })
        Chip(stringResource(R.string.filter_contrast), s.filter == LensFilter.CONTRAST, { onFilter(LensFilter.CONTRAST) })
        if (s.spec?.canZoom2x == true) Chip(stringResource(R.string.zoom_2x), s.zoom2x, onZoom)
    }
}

/**
 * The hint, and how many photos were saved for the report. Upright it is one line (the rest is in tips); in the
 * narrower side panel it may wrap (T05a).
 */
@Composable
private fun LensHint(s: LensState, modifier: Modifier, oneLine: Boolean) {
    val c = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(if (s.mode == Check.INFRARED) R.string.infrared_hint else R.string.lens_hint), style = UnpeepType.supporting,
            color = c.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = if (oneLine) 1 else Int.MAX_VALUE,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        if (s.photos > 0) Text(pluralStringResource(R.plurals.photos_saved, s.photos, s.photos), style = UnpeepType.supporting, color = c.primary, textAlign = TextAlign.Center)
    }
}

/** The filters two by two, filling the side panel (T05). */
@Composable
private fun LensFilterGrid(s: LensState, onFilter: (LensFilter) -> Unit, onZoom: () -> Unit, modifier: Modifier = Modifier) {
    val chips = buildList<@Composable (Modifier) -> Unit> {
        add { m -> Chip(stringResource(R.string.filter_none), s.filter == LensFilter.NONE, { onFilter(LensFilter.NONE) }, m, fill = true) }
        add { m -> Chip(stringResource(R.string.filter_red), s.filter == LensFilter.RED, { onFilter(LensFilter.RED) }, m, fill = true) }
        add { m -> Chip(stringResource(R.string.filter_contrast), s.filter == LensFilter.CONTRAST, { onFilter(LensFilter.CONTRAST) }, m, fill = true) }
        if (s.spec?.canZoom2x == true) add { m -> Chip(stringResource(R.string.zoom_2x), s.zoom2x, onZoom, m, fill = true) }
    }
    Column(modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(Space.inline)) {
        chips.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.inline)) {
                pair.forEach { it(Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Switch camera · shutter · torch: the same row upright and on its side. */
@Composable
private fun ShutterControls(s: LensState, onTorch: () -> Unit, onFlip: () -> Unit, onShutter: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        // Switch camera on the left, as a phone's camera app has it; the torch on the right.
        RoundIconButton(painterResource(R.drawable.ic_flip_camera), stringResource(R.string.switch_camera), onFlip)
        Shutter(painterResource(R.drawable.ic_camera), stringResource(R.string.take_photo), onShutter, enabled = s.status == CameraStatus.RUNNING)
        if (s.mode == Check.LENS && s.spec?.hasTorch == true) {
            RoundIconButton(painterResource(R.drawable.ic_lens), stringResource(R.string.torch), onTorch, active = s.torch)
        } else {
            Spacer(Modifier.size(Size.iconButton))
        }
    }
}

/** Lens · Infrared: the one labelled control for the two finders (it hugs its labels over the viewfinder). */
@Composable
private fun LensModes(s: LensState, onMode: (Check) -> Unit, modifier: Modifier = Modifier, fill: Boolean = false) {
    Segmented(
        listOf(Segment(stringResource(R.string.check_lens), painterResource(R.drawable.ic_lens)), Segment(stringResource(R.string.check_infrared), painterResource(R.drawable.ic_infrared))),
        if (s.mode == Check.INFRARED) 1 else 0, { onMode(if (it == 1) Check.INFRARED else Check.LENS) }, modifier, fill = fill,
    )
}

/** Filters and 2× · Lens or Infrared · the hint · tips, the shutter and the torch. */
@Composable
private fun LensControls(
    s: LensState, running: Boolean, onMode: (Check) -> Unit, onFilter: (LensFilter) -> Unit,
    onZoom: () -> Unit, onTorch: () -> Unit, onFlip: () -> Unit, onShutter: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.widthIn(max = Size.contentMax).fillMaxWidth().padding(top = Space.heading),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.block),
    ) {
        if (running) LensFilters(s, onFilter, onZoom, Modifier.padding(horizontal = Space.screen))
        LensModes(s, onMode)
        if (running) {
            LensHint(s, Modifier.padding(horizontal = Space.heading), oneLine = true)
            ShutterControls(s, onTorch, onFlip, onShutter, Modifier.padding(horizontal = Space.shutterSide, vertical = Space.block))
        }
    }
}

/** How to look for a lens, and (Infrared) the remote test again. */
@Composable
internal fun LensTipsSheet(mode: Check, onDismiss: () -> Unit, onTestAgain: () -> Unit) {
    UnpeepSheet(stringResource(R.string.tips_title), onDismiss, line = stringResource(R.string.tips_line)) {
        Grouped(Modifier.padding(horizontal = Space.screen)) {
            row { shape -> SignRow(painterResource(R.drawable.ic_lamp), stringResource(R.string.tip_dark), shape = shape) }
            row { shape -> SignRow(painterResource(R.drawable.ic_walking), stringResource(R.string.tip_slow), shape = shape) }
            row { shape -> SignRow(painterResource(R.drawable.ic_search), stringResource(R.string.tip_places), shape = shape) }
            row { shape -> SignRow(painterResource(R.drawable.ic_lens), stringResource(R.string.tip_glint), shape = shape) }
            row { shape -> SignRow(painterResource(R.drawable.ic_camera), stringResource(R.string.tip_photo), shape = shape) }
            if (mode == Check.INFRARED) row { shape -> SignRow(painterResource(R.drawable.ic_infrared), stringResource(R.string.tip_infrared), shape = shape) }
        }
        Spacer(Modifier.height(Space.block))
        if (mode == Check.INFRARED) SheetLine(stringResource(R.string.test_again), icon = painterResource(R.drawable.ic_refresh), onClick = onTestAgain)
        Spacer(Modifier.height(Space.block))
    }
}

/** The first time Infrared opens: can this phone's camera see infrared at all? */
@Composable
internal fun InfraredTestSheet(onDismiss: () -> Unit, onAnswer: (Boolean) -> Unit) {
    UnpeepSheet(stringResource(R.string.ir_test_title), onDismiss, line = stringResource(R.string.ir_test_line)) {
        Column(Modifier.selectableGroup()) {
            ChoiceRow(
                stringResource(R.string.ir_saw), false, { onAnswer(true) }, line = stringResource(R.string.ir_saw_line),
                leading = { IconCircle(painterResource(R.drawable.ic_infrared), CircleTone.Hue(Hues.INFRARED)) },
            )
            ChoiceRow(
                stringResource(R.string.ir_not_seen), false, { onAnswer(false) }, line = stringResource(R.string.ir_not_seen_line),
                leading = { IconCircle(painterResource(R.drawable.ic_eye_closed), CircleTone.Neutral) },
            )
        }
        Spacer(Modifier.height(Space.block))
    }
}

private fun previewSpec() = CameraSpec("0", Facing.BACK, 90, true, android.util.Size(1440, 1080), android.util.Size(320, 240), 8f, null, 2)

@Preview
@Composable
private fun LensPreview() = UnpeepTheme {
    ViewfinderTheme {
        LensContent(
            LensState(status = CameraStatus.RUNNING, spec = previewSpec(), spots = listOf(Spot(0.4f, 0.3f, 0.01f, 250), Spot(0.7f, 0.6f, 0.01f, 240)), photos = 1, filter = LensFilter.RED),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, false, {},
        )
    }
}

@Preview
@Composable
private fun LensPermissionPreview() = UnpeepTheme {
    ViewfinderTheme { LensContent(LensState(status = CameraStatus.NEEDS_PERMISSION, sweep = true), {}, {}, {}, {}, {}, {}, {}, {}, {}, false, {}) }
}
