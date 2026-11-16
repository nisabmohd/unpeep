package app.keyhole.privacy.ui.sweep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import app.keyhole.privacy.ui.isPhoneLandscape
import app.keyhole.privacy.ui.skipTo
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.theme.Hues
import app.keyhole.privacy.ui.components.KeyholeSheet
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import app.keyhole.privacy.ui.hue
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.WaitingDots
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.DeviceEvidence
import app.keyhole.privacy.domain.NearbyBlock
import app.keyhole.privacy.domain.NearbySnapshot
import app.keyhole.privacy.domain.NearbyStatus
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.domain.Sign
import app.keyhole.privacy.domain.Verdict
import app.keyhole.privacy.domain.WifiDevice
import app.keyhole.privacy.domain.WifiSnapshot
import app.keyhole.privacy.domain.WifiStatus
import app.keyhole.privacy.ui.components.Box24
import app.keyhole.privacy.ui.components.Buzz
import app.keyhole.privacy.ui.components.rememberHaptics
import app.keyhole.privacy.ui.components.CheckRow
import app.keyhole.privacy.ui.components.CheckState
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.EmptyState
import app.keyhole.privacy.ui.components.EmptyUnder
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.LensProgress
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.TopBarAction
import app.keyhole.privacy.ui.icon
import app.keyhole.privacy.ui.nearby.icon
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.nearby.NearbyListRow
import app.keyhole.privacy.ui.nearby.line
import app.keyhole.privacy.ui.nearby.nearbyWords
import app.keyhole.privacy.ui.nearby.title
import app.keyhole.privacy.ui.rememberPermissionAsk
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.ThemeMode
import app.keyhole.privacy.ui.theme.ThemeSettings
import app.keyhole.privacy.ui.wifi.DeviceListRow

/**
 * 03 Sweep in progress: the check row (the current step ringed) · the progress lens · what was found so far.
 * [onStepDone] opens a step's results when its scan ends; [onFinished] when the last step is over.
 */
@Composable
fun SweepScreen(vm: SweepViewModel, onClose: () -> Unit, onStepDone: (Check) -> Unit, onFinished: () -> Unit, onDrop: () -> Unit = onClose) {
    val s by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    LaunchedEffect(s.done) {
        val done = s.done ?: return@LaunchedEffect
        // A scan that ends with something to look at buzzes stronger than one that doesn't.
        val flagged = when (done) {
            Check.WIFI -> s.wifi.devices.any { it.risk == Risk.LIKELY_CAMERA || it.risk == Risk.CHECK_THIS }
            Check.HOTSPOTS, Check.BLUETOOTH -> s.nearby?.devices.orEmpty().any { it.risk == Risk.LIKELY_CAMERA || it.risk == Risk.CHECK_THIS }
            else -> null
        }
        if (flagged != null) haptics.buzz(if (flagged) Buzz.FOUND else Buzz.DONE)
        onStepDone(done)
    }
    LaunchedEffect(s.finished) { if (s.finished) onFinished() }
    // Back from Android's permission question or from Settings: a step that was waiting starts by itself.
    LifecycleResumeEffect(Unit) {
        vm.resumed()
        onPauseOrDispose { }
    }
    // Out of sight (not just behind a permission question): a running scan stops (CLAUDE.md: never in the background).
    androidx.lifecycle.compose.LifecycleStartEffect(Unit) { onStopOrDispose { vm.stopped() } }
    val ask = s.nearby?.let { rememberPermissionAsk(it.check) { vm.retry() } }
    // Closing a sweep asks first (E07); a single check just closes.
    var stopping by rememberSaveable { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = !s.single) { stopping = true }
    val context = androidx.compose.ui.platform.LocalContext.current
    // Read each time the screen draws: Scan again after switching it off redraws.
    val aeroplane = android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    SweepContent(
        s, onClose = { if (s.single) onClose() else stopping = true }, vm::skip, vm::retry, onAllow = { ask?.ask?.invoke() }, settingsOnly = ask?.settingsOnly == true,
        onCarryOn = vm::carryOn, aeroplane = aeroplane, onSkipToHandsOn = vm::skipToHandsOn,
    )
    if (stopping) StopSweepSheet(s, onDismiss = { stopping = false }, onReport = { stopping = false; onFinished() }, onDrop = { stopping = false; onDrop() })
}

/**
 * Stop this sweep? (E07): stop and see the report of what ran (the rest shows as not checked), or stop without one
 * (what was found is dropped). Keep going is the main action.
 */
@Composable
fun StopSweepSheet(s: SweepState, onDismiss: () -> Unit, onReport: () -> Unit, onDrop: () -> Unit) {
    val at = (s.steps.indexOf(s.current) + 1).coerceAtLeast(1)
    val total = s.steps.size
    val ran = (at - 1).coerceAtLeast(0)
    val haptics = rememberHaptics()
    KeyholeSheet(
        stringResource(R.string.stop_sweep_title), onDismiss, line = stringResource(R.string.stop_sweep_line, at, total),
        footer = { PrimaryButton(stringResource(R.string.keep_going), onDismiss, Modifier.fillMaxWidth()) },
    ) {
        Grouped(Modifier.padding(horizontal = Space.screen)) {
            row { shape ->
                ListRow(
                    stringResource(R.string.stop_and_report), shape = shape, line = pluralStringResource(R.plurals.stop_and_report_line, total, ran, total),
                    leading = { IconCircle(painterResource(R.drawable.ic_reports), CircleTone.Hue(Hues.REPORT)) }, onClick = { haptics.buzz(Buzz.CONFIRM); onReport() },
                )
            }
            row { shape ->
                ListRow(
                    stringResource(R.string.stop_no_report), shape = shape, line = stringResource(R.string.stop_no_report_line), alert = true, chevron = false,
                    leading = { IconCircle(painterResource(R.drawable.ic_trash), CircleTone.Alert) }, onClick = { haptics.buzz(Buzz.TOGGLE_OFF); onDrop() },
                )
            }
        }
    }
}

@Composable
fun SweepContent(
    s: SweepState, onClose: () -> Unit, onSkip: () -> Unit, onRetry: () -> Unit, onAllow: () -> Unit = {}, settingsOnly: Boolean = false,
    onCarryOn: () -> Unit = {}, aeroplane: Boolean = false, onSkipToHandsOn: () -> Unit = {},
) {
    val notOnWifi = s.current == Check.WIFI && s.wifi.status == WifiStatus.NOT_ON_WIFI
    val needsPermission = s.nearby?.status == NearbyStatus.NEEDS_PERMISSION
    val block = s.nearby?.block?.takeIf { s.nearby.status == NearbyStatus.UNAVAILABLE }
    // Aeroplane mode explains every radio step at once (E09), and offers the steps that need no signal.
    val handsOnNext = s.steps.drop(s.steps.indexOf(s.current) + 1).firstOrNull { it in SweepViewModel.HANDS_ON }
    val offline = aeroplane && !s.paused && (notOnWifi || (block != null && block != NearbyBlock.NO_BLUETOOTH))
    // One main action: what gets this step going.
    val dock: (@Composable ColumnScope.() -> Unit)? = when {
        s.paused -> { { PrimaryButton(stringResource(R.string.carry_on), onCarryOn, Modifier.fillMaxWidth(), trailing = painterResource(R.drawable.ic_arrow_right)) } }
        offline -> { {
            ButtonPair(
                secondary = { m -> if (handsOnNext != null && !s.single) SecondaryButton(stringResource(handsOnNext.skipTo), onSkipToHandsOn, m) },
                primary = { m -> PrimaryButton(stringResource(R.string.scan_again), onRetry, m, icon = painterResource(R.drawable.ic_refresh)) },
            )
        } }
        needsPermission -> { { PrimaryButton(stringResource(if (settingsOnly) R.string.open_settings else R.string.allow), onAllow, Modifier.fillMaxWidth(), icon = if (settingsOnly) painterResource(R.drawable.ic_settings) else null) } }
        notOnWifi || (block != null && block != NearbyBlock.NO_BLUETOOTH) -> { { PrimaryButton(stringResource(R.string.scan_again), onRetry, Modifier.fillMaxWidth(), icon = painterResource(R.drawable.ic_refresh)) } }
        else -> null
    }
    DetailScreen(
        onClose,
        title = if (s.single) s.current?.let { stringResource(it.label) } else stringResource(s.place.label),
        close = true,
        actions = { if (!s.single) TopBarAction(stringResource(R.string.skip_step), onSkip) },
        dock = dock,
    ) {
        val landscape = isPhoneLandscape()
        val checkRow: @Composable () -> Unit = {
            val at = s.steps.indexOf(s.current)
            Box24 {
                CheckRow(s.steps, steps = true, state = { check ->
                    val i = s.steps.indexOf(check)
                    when {
                        i == at -> CheckState.CURRENT
                        i < at -> CheckState.DONE
                        else -> CheckState.PLAIN
                    }
                })
            }
        }
        val showRow = !s.single && s.steps.isNotEmpty()
        // On its side (T06) a running scan puts its check row beside the lens; every other state keeps it on top.
        val running = !s.paused && !offline && !notOnWifi && !needsPermission && block == null && (s.current == Check.WIFI || s.nearby != null)
        if (showRow && !(landscape && running)) checkRow()
        val side = if (landscape && showRow) checkRow else null
        val nearby = s.nearby
        when {
            s.current == null -> Unit
            s.paused -> EmptyState(
                painterResource(s.current.icon), stringResource(R.string.paused_at, stringResource(s.current.label)),
                pluralStringResource(R.plurals.paused_line, s.steps.size, s.stepsDone, s.steps.size), hue = s.current.hue, under = if (showRow) EmptyUnder.CHECK_ROW else EmptyUnder.BAR,
            )
            offline -> EmptyState(painterResource(s.current.icon), stringResource(R.string.aeroplane_title), stringResource(R.string.aeroplane_line), hue = s.current.hue, under = if (showRow) EmptyUnder.CHECK_ROW else EmptyUnder.BAR)
            notOnWifi -> EmptyState(painterResource(R.drawable.ic_wifi), stringResource(R.string.not_on_wifi_title), stringResource(R.string.not_on_wifi_line), hue = Check.WIFI.hue, under = if (showRow) EmptyUnder.CHECK_ROW else EmptyUnder.BAR)
            s.current == Check.WIFI -> WifiStep(s.wifi, landscape, side)
            nearby != null && needsPermission -> {
                val words = nearby.check.nearbyWords
                val why = stringResource(words.permLine)
                EmptyState(painterResource(nearby.check.icon), stringResource(words.permTitle), if (settingsOnly) why + " " + stringResource(R.string.perm_denied_line) else why, hue = nearby.check.hue, under = if (showRow) EmptyUnder.CHECK_ROW else EmptyUnder.BAR)
            }
            nearby != null && block != null -> EmptyState(painterResource(block.icon ?: nearby.check.icon), stringResource(block.title), stringResource(block.line), hue = nearby.check.hue, under = if (showRow) EmptyUnder.CHECK_ROW else EmptyUnder.BAR)
            nearby != null -> NearbyStep(nearby, landscape, side)
            // Lens, Infrared, Magnet and the checklist open their own screens as soon as the sweep reaches them.
            else -> Unit
        }
    }
}

/** The hotspot or Bluetooth step while it listens: the lens fills with time; only what is worth a look is listed. */
@Composable
private fun ColumnScope.NearbyStep(n: NearbySnapshot, landscape: Boolean = false, side: (@Composable () -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    val flagged = n.devices.filter { it.risk == Risk.LIKELY_CAMERA || it.risk == Risk.CHECK_THIS }
    val lensPart: @Composable ColumnScope.() -> Unit = {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(Space.lensTop))
        LensProgress(n.fraction, n.devices.size.toString(), stringResource(R.string.found_line))
        Spacer(Modifier.height(Space.lensBottom))
        Text(stringResource(n.check.nearbyWords.checking), style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset).semantics { heading() })
        Text(
            pluralStringResource(R.plurals.about_seconds_left, n.secondsLeft, n.secondsLeft),
            style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.row).padding(horizontal = Space.screen),
        )
    }
    }
    val listPart: @Composable ColumnScope.() -> Unit = {
    val likely = n.count(Risk.LIKELY_CAMERA)
    val bluetooth = n.check == Check.BLUETOOTH
    val heard = n.devices.size
    SectionLabel(
        stringResource(R.string.found_so_far),
        meta = if (likely > 0) pluralStringResource(R.plurals.likely_cameras_count, likely, likely) else pluralStringResource(if (bluetooth) R.plurals.bluetooth_count else R.plurals.networks_count, heard, heard),
    )
    if (flagged.isEmpty()) {
        WaitingRow(n.check, stringResource(R.string.nothing_flagged_yet), pluralStringResource(if (bluetooth) R.plurals.bluetooth_so_far else R.plurals.networks_so_far, heard, heard))
    } else {
        Grouped { flagged.forEach { device -> row { shape -> NearbyListRow(device, shape = shape) } } }
    }
    }
    if (landscape) {
        // T06: the lens on the left, the check row and what was found on the right.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.screen), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { lensPart() }
            Column(Modifier.weight(1.2f)) {
                side?.invoke()
                listPart()
            }
        }
    } else {
        lensPart()
        listPart()
    }
}

/** The Wi-Fi step while it runs: the lens fills as addresses are visited, then counts the devices read. */
@Composable
private fun ColumnScope.WifiStep(w: WifiSnapshot, landscape: Boolean = false, side: (@Composable () -> Unit)? = null) {
    val lensPart: @Composable ColumnScope.() -> Unit = {
    val c = MaterialTheme.colorScheme
    val left = pluralStringResource(R.plurals.about_seconds_left, w.secondsLeft, w.secondsLeft)
    // Before anything answers, the lens counts what was found (0); then how far through the addresses it is.
    val counting = w.total > 0 && w.devices.isNotEmpty()
    val value = if (counting) w.checked else w.devices.size
    val line = if (counting) stringResource(R.string.of_total, w.total) else stringResource(R.string.found_line)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(Space.lensTop))
        LensProgress(w.fraction, value.toString(), line)
        Spacer(Modifier.height(Space.lensBottom))
        Text(stringResource(R.string.checking_wifi), style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset).semantics { heading() })
        Text(
            w.ssid?.let { stringResource(R.string.two_parts, it, left) } ?: left,
            style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.row).padding(horizontal = Space.screen),
        )
    }
    }
    val listPart: @Composable ColumnScope.() -> Unit = {
    val likely = w.count(Risk.LIKELY_CAMERA)
    SectionLabel(
        stringResource(R.string.found_so_far),
        meta = when {
            likely > 0 -> pluralStringResource(R.plurals.likely_cameras_count, likely, likely)
            w.devices.isEmpty() && w.total > 0 -> pluralStringResource(R.plurals.asked_of, w.checked, w.checked, w.total)
            else -> null
        },
    )
    if (w.devices.isEmpty()) {
        WaitingRow(Check.WIFI, stringResource(R.string.nothing_answered_yet), stringResource(R.string.nothing_answered_line))
    } else {
        Grouped { w.devices.forEach { device -> row { shape -> DeviceListRow(device, shape = shape) } } }
    }
    }
    if (landscape) {
        // T06: the lens on the left, the check row and what was found on the right.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.screen), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { lensPart() }
            Column(Modifier.weight(1.2f)) {
                side?.invoke()
                listPart()
            }
        }
    } else {
        lensPart()
        listPart()
    }
}

private fun previewDevices(): List<WifiDevice> = listOf(
    WifiDevice(DeviceEvidence("192.168.1.23", name = "IPC-3F2A"), Verdict(Risk.LIKELY_CAMERA, listOf(Sign.Onvif, Sign.Rtsp(554))), 0),
    WifiDevice(DeviceEvidence("192.168.1.19", name = "Living-room TV"), Verdict(Risk.KNOWN, listOf(Sign.KnownKind("tv")), kind = "tv"), 0),
    WifiDevice(DeviceEvidence("192.168.1.14", name = "Pixel 8"), Verdict(Risk.KNOWN, listOf(Sign.KnownKind("phone")), kind = "phone"), 0),
)

@Preview
@Composable
private fun SweepPreview() = KeyholeTheme {
    SweepContent(
        SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.RUNNING, "SeaView_Guest", 0.61f, 11, 18, 10, previewDevices())),
        onClose = {}, onSkip = {}, onRetry = {},
    )
}

@Preview
@Composable
private fun SweepDarkPreview() = KeyholeTheme(ThemeSettings(mode = ThemeMode.DARK)) {
    SweepContent(SweepState(Place.STAY, Check.entries, Check.WIFI, wifi = WifiSnapshot(1, WifiStatus.NOT_ON_WIFI)), onClose = {}, onSkip = {}, onRetry = {})
}

/** The one row while nothing is listed yet: the check's circle, what is happening, and the waiting dots. */
@Composable
private fun WaitingRow(check: Check, title: String, line: String) {
    Grouped { row { shape -> ListRow(title, shape = shape, line = line, leading = { IconCircle(painterResource(check.icon), CircleTone.Hue(check.hue)) }, trailing = { WaitingDots() }) } }
}
