package app.keyhole.privacy.ui.lens

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
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.optics.CameraController
import app.keyhole.privacy.optics.CameraSpec
import app.keyhole.privacy.optics.Facing
import app.keyhole.privacy.optics.Spot
import app.keyhole.privacy.optics.cropScale
import app.keyhole.privacy.optics.toView
import app.keyhole.privacy.scan.Access
import app.keyhole.privacy.ui.components.BackTopBar
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.CameraPreview
import app.keyhole.privacy.ui.components.Chip
import app.keyhole.privacy.ui.components.ChoiceRow
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.Dock
import app.keyhole.privacy.ui.components.EmptyState
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.KeyholeSheet
import app.keyhole.privacy.ui.components.Note
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.RoundIconButton
import app.keyhole.privacy.ui.components.Segment
import app.keyhole.privacy.ui.components.Segmented
import app.keyhole.privacy.ui.components.SheetLine
import app.keyhole.privacy.ui.components.Shutter
import app.keyhole.privacy.ui.components.SignRow
import app.keyhole.privacy.ui.components.SpotMark
import app.keyhole.privacy.ui.components.SpotRings
import app.keyhole.privacy.ui.components.Tag
import app.keyhole.privacy.ui.components.TagKind
import app.keyhole.privacy.ui.components.TopBarAction
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.drawEdgeToEdge
import app.keyhole.privacy.ui.openAppSettings
import app.keyhole.privacy.ui.rememberPermissionAsk
import app.keyhole.privacy.ui.theme.Hues
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.RedFilter
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.ViewfinderTheme
import app.keyhole.privacy.ui.theme.isAppDark

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
        if (granted && surface != null && spec != null) {
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
    var testDismissed by rememberSaveable { mutableStateOf(false) }
    val askTest = s.mode == Check.INFRARED && s.seesInfrared == null && !testDismissed && s.status == CameraStatus.RUNNING

    ViewfinderTheme {
        LensContent(
            s, onBack = onBack,
            onNext = { if (s.next == Check.INFRARED && s.mode == Check.LENS) vm.setMode(Check.INFRARED) else onNext(s.next) },
            onMode = vm::setMode, onFilter = vm::setFilter,
            onZoom = { haptics.buzz(if (s.zoom2x) Buzz.TOGGLE_OFF else Buzz.TOGGLE_ON); vm.toggleZoom() },
            onTorch = { haptics.buzz(if (s.torch) Buzz.TOGGLE_OFF else Buzz.TOGGLE_ON); vm.toggleTorch() },
            onTips = { tips = true },
            onShutter = { vm.onPhoto(texture?.bitmap) },
            onAllow = { if (ask.settingsOnly) openAppSettings(context) else ask.ask() }, settingsOnly = ask.settingsOnly,
            onRetry = vm::retry, photoFailed = photoFailed,
            preview = { modifier -> if (spec != null) CameraPreview(spec.uprightWidth, spec.uprightHeight, { texture = it }, modifier, stringResource(R.string.camera_preview)) },
        )
        if (tips) LensTipsSheet(s.mode, onDismiss = { tips = false }, onTestAgain = { tips = false; testDismissed = false; vm.resetInfraredTest() })
        if (askTest) InfraredTestSheet(onDismiss = { testDismissed = true }, onAnswer = { sees -> haptics.buzz(Buzz.TICK); vm.setSeesInfrared(sees); testDismissed = true })
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
    onRetry: () -> Unit, photoFailed: Boolean = false, preview: @Composable (Modifier) -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    val running = s.status == CameraStatus.RUNNING || s.status == CameraStatus.STARTING
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        BackTopBar(
            onBack, Modifier.widthIn(max = Size.contentMax),
            title = stringResource(if (s.mode == Check.INFRARED) R.string.infrared_finder else R.string.lens_finder), close = s.sweep,
            actions = {
                if (running) Tag(pluralStringResource(R.plurals.spots_count, s.spots.size, s.spots.size), TagKind.KNOWN)
                if (s.sweep) TopBarAction(stringResource(R.string.next), onNext)
            },
        )
        var box by remember { mutableStateOf(IntSize.Zero) }
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { box = it }) {
            when (s.status) {
                CameraStatus.NEEDS_PERMISSION -> CameraMessage(R.drawable.ic_camera, R.string.perm_camera_title, if (settingsOnly) R.string.perm_denied_line else R.string.perm_camera_line)
                CameraStatus.IN_USE -> CameraMessage(R.drawable.ic_camera, R.string.camera_problem_title, R.string.camera_in_use_line)
                CameraStatus.FAILED -> CameraMessage(R.drawable.ic_camera, R.string.camera_problem_title, R.string.camera_failed_line)
                CameraStatus.NO_CAMERA -> CameraMessage(R.drawable.ic_camera, R.string.no_camera_title, R.string.block_no_bluetooth_line)
                CameraStatus.STARTING, CameraStatus.RUNNING -> {
                    preview(Modifier.fillMaxSize())
                    val spec = s.spec
                    if (spec != null && box.width > 0) {
                        val scale = cropScale(spec.uprightWidth.toFloat(), spec.uprightHeight.toFloat(), box.width.toFloat(), box.height.toFloat())
                        val marks = s.spots.map { val (x, y) = toView(it.x, it.y, scale); SpotMark(x, y) }
                        SpotRings(marks, stringResource(R.string.look_here), pluralStringResource(R.plurals.spots_ringed, marks.size, marks.size))
                    }
                    // Notes go first: on top of the picture.
                    Column(Modifier.padding(Space.screen), verticalArrangement = Arrangement.spacedBy(Space.inline)) {
                        if (s.mode == Check.INFRARED && s.seesInfrared == false) {
                            Note(stringResource(R.string.ir_blocked_text), title = stringResource(R.string.ir_blocked_title), icon = painterResource(R.drawable.ic_warning), warning = true)
                        }
                        if (photoFailed) Note(stringResource(R.string.photo_not_saved), icon = painterResource(R.drawable.ic_warning), warning = true)
                    }
                }
            }
        }
        LensControls(s, running, onMode, onFilter, onZoom, onTorch, onTips, onShutter)
        when (s.status) {
            CameraStatus.NEEDS_PERMISSION -> Dock(Modifier.widthIn(max = Size.contentMax)) {
                PrimaryButton(stringResource(if (settingsOnly) R.string.open_settings else R.string.allow), onAllow, Modifier.fillMaxWidth())
            }
            CameraStatus.IN_USE, CameraStatus.FAILED -> Dock(Modifier.widthIn(max = Size.contentMax)) {
                PrimaryButton(stringResource(R.string.try_again), onRetry, Modifier.fillMaxWidth(), icon = painterResource(R.drawable.ic_refresh))
            }
            else -> Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun CameraMessage(icon: Int, title: Int, line: Int) {
    Column(Modifier.fillMaxSize().padding(horizontal = Space.screen)) {
        EmptyState(painterResource(icon), stringResource(title), stringResource(line))
    }
}

/** Filters and 2× · Lens or Infrared · the hint · tips, the shutter and the torch. */
@Composable
private fun LensControls(
    s: LensState, running: Boolean, onMode: (Check) -> Unit, onFilter: (LensFilter) -> Unit,
    onZoom: () -> Unit, onTorch: () -> Unit, onTips: () -> Unit, onShutter: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.widthIn(max = Size.contentMax).fillMaxWidth().padding(top = Space.block),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.block),
    ) {
        if (running) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Space.screen).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Space.inline),
            ) {
                Chip(stringResource(R.string.filter_none), s.filter == LensFilter.NONE, { onFilter(LensFilter.NONE) })
                Chip(stringResource(R.string.filter_red), s.filter == LensFilter.RED, { onFilter(LensFilter.RED) })
                Chip(stringResource(R.string.filter_contrast), s.filter == LensFilter.CONTRAST, { onFilter(LensFilter.CONTRAST) })
                if (s.spec?.canZoom2x == true) Chip(stringResource(R.string.zoom_2x), s.zoom2x, onZoom)
            }
        }
        Segmented(
            listOf(Segment(stringResource(R.string.check_lens), painterResource(R.drawable.ic_lens)), Segment(stringResource(R.string.check_infrared), painterResource(R.drawable.ic_infrared))),
            if (s.mode == Check.INFRARED) 1 else 0, { onMode(if (it == 1) Check.INFRARED else Check.LENS) },
        )
        if (running) {
            Column(Modifier.padding(horizontal = Space.heading), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(if (s.mode == Check.INFRARED) R.string.infrared_hint else R.string.lens_hint), style = KeyholeType.body, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                if (s.photos > 0) Text(pluralStringResource(R.plurals.photos_saved, s.photos, s.photos), style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.heading, vertical = Space.block), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                RoundIconButton(painterResource(R.drawable.ic_help), stringResource(R.string.tips), onTips)
                Shutter(painterResource(R.drawable.ic_camera), stringResource(R.string.take_photo), onShutter, enabled = s.status == CameraStatus.RUNNING)
                if (s.mode == Check.LENS && s.spec?.hasTorch == true) {
                    RoundIconButton(painterResource(R.drawable.ic_lens), stringResource(R.string.torch), onTorch, active = s.torch)
                } else {
                    Spacer(Modifier.size(Size.iconButton))
                }
            }
        }
    }
}

/** How to look for a lens, and (Infrared) the remote test again. */
@Composable
private fun LensTipsSheet(mode: Check, onDismiss: () -> Unit, onTestAgain: () -> Unit) {
    KeyholeSheet(stringResource(R.string.tips_title), onDismiss, line = stringResource(R.string.tips_line)) {
        Grouped(Modifier.padding(horizontal = Space.screen)) {
            row { shape -> SignRow(painterResource(R.drawable.ic_lamp), stringResource(R.string.tip_dark), shape = shape) }
            row { shape -> SignRow(painterResource(R.drawable.ic_walking), stringResource(R.string.tip_slow), shape = shape) }
            row { shape -> SignRow(painterResource(R.drawable.ic_search), stringResource(R.string.tip_places), shape = shape) }
            row { shape -> SignRow(painterResource(R.drawable.ic_lens), stringResource(R.string.tip_glint), shape = shape) }
            if (mode == Check.INFRARED) row { shape -> SignRow(painterResource(R.drawable.ic_infrared), stringResource(R.string.tip_infrared), shape = shape) }
        }
        Spacer(Modifier.height(Space.block))
        if (mode == Check.INFRARED) SheetLine(stringResource(R.string.test_again), icon = painterResource(R.drawable.ic_refresh), onClick = onTestAgain)
        Spacer(Modifier.height(Space.block))
    }
}

/** The first time Infrared opens: can this phone's camera see infrared at all? */
@Composable
private fun InfraredTestSheet(onDismiss: () -> Unit, onAnswer: (Boolean) -> Unit) {
    KeyholeSheet(stringResource(R.string.ir_test_title), onDismiss, line = stringResource(R.string.ir_test_line)) {
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
private fun LensPreview() = KeyholeTheme {
    ViewfinderTheme {
        LensContent(
            LensState(status = CameraStatus.RUNNING, spec = previewSpec(), spots = listOf(Spot(0.4f, 0.3f, 0.01f, 250), Spot(0.7f, 0.6f, 0.01f, 240)), photos = 1, filter = LensFilter.RED),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, false, {},
        )
    }
}

@Preview
@Composable
private fun LensPermissionPreview() = KeyholeTheme {
    ViewfinderTheme { LensContent(LensState(status = CameraStatus.NEEDS_PERMISSION, sweep = true), {}, {}, {}, {}, {}, {}, {}, {}, {}, false, {}) }
}
