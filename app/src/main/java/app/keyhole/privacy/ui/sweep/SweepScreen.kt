package app.keyhole.privacy.ui.sweep

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
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.LensProgress
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.TopBarAction
import app.keyhole.privacy.ui.icon
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
fun SweepScreen(vm: SweepViewModel, onClose: () -> Unit, onStepDone: (Check) -> Unit, onFinished: () -> Unit) {
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
    val ask = s.nearby?.let { rememberPermissionAsk(it.check) { vm.retry() } }
    SweepContent(s, onClose, vm::skip, vm::retry, onAllow = { ask?.ask?.invoke() }, settingsOnly = ask?.settingsOnly == true)
}

@Composable
fun SweepContent(s: SweepState, onClose: () -> Unit, onSkip: () -> Unit, onRetry: () -> Unit, onAllow: () -> Unit = {}, settingsOnly: Boolean = false) {
    val notOnWifi = s.current == Check.WIFI && s.wifi.status == WifiStatus.NOT_ON_WIFI
    val needsPermission = s.nearby?.status == NearbyStatus.NEEDS_PERMISSION
    val block = s.nearby?.block?.takeIf { s.nearby.status == NearbyStatus.UNAVAILABLE }
    // One main action: what gets this step going.
    val dock: (@Composable ColumnScope.() -> Unit)? = when {
        needsPermission -> { { PrimaryButton(stringResource(if (settingsOnly) R.string.open_settings else R.string.allow), onAllow, Modifier.fillMaxWidth()) } }
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
        if (!s.single && s.steps.isNotEmpty()) {
            val at = s.steps.indexOf(s.current)
            Box24 {
                CheckRow(s.steps, state = { check ->
                    val i = s.steps.indexOf(check)
                    when {
                        i == at -> CheckState.CURRENT
                        i < at -> CheckState.DONE
                        else -> CheckState.PLAIN
                    }
                })
            }
        }
        val nearby = s.nearby
        when {
            s.current == null -> Unit
            notOnWifi -> EmptyState(painterResource(R.drawable.ic_wifi), stringResource(R.string.not_on_wifi_title), stringResource(R.string.not_on_wifi_line))
            s.current == Check.WIFI -> WifiStep(s.wifi)
            nearby != null && needsPermission -> {
                val words = nearby.check.nearbyWords
                val why = stringResource(words.permLine)
                EmptyState(painterResource(nearby.check.icon), stringResource(words.permTitle), if (settingsOnly) why + " " + stringResource(R.string.perm_denied_line) else why)
            }
            nearby != null && block != null -> EmptyState(painterResource(nearby.check.icon), stringResource(block.title), stringResource(block.line))
            nearby != null -> NearbyStep(nearby)
            // The other checks arrive with their weeks (TODO.md); until then the step says so and can be skipped.
            else -> EmptyState(painterResource(s.current.icon), stringResource(s.current.label), stringResource(R.string.stand_in_line))
        }
    }
}

/** The hotspot or Bluetooth step while it listens: the lens fills with time; only what is worth a look is listed. */
@Composable
private fun ColumnScope.NearbyStep(n: NearbySnapshot) {
    val c = MaterialTheme.colorScheme
    val flagged = n.devices.filter { it.risk == Risk.LIKELY_CAMERA || it.risk == Risk.CHECK_THIS }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(Space.lensTop))
        LensProgress(n.fraction, n.devices.size.toString(), stringResource(R.string.found_line))
        Spacer(Modifier.height(Space.lensBottom))
        Text(stringResource(n.check.nearbyWords.checking), style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset).semantics { heading() })
        Text(
            pluralStringResource(R.plurals.about_seconds_left, n.secondsLeft, n.secondsLeft),
            style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset),
        )
    }
    val likely = n.count(Risk.LIKELY_CAMERA)
    SectionLabel(stringResource(R.string.found_so_far), meta = if (likely > 0) pluralStringResource(R.plurals.likely_cameras_count, likely, likely) else null)
    if (flagged.isEmpty()) {
        Text(stringResource(R.string.nothing_flagged_yet), style = KeyholeType.supporting, color = c.onSurfaceVariant, modifier = Modifier.padding(horizontal = Space.headingInset))
    } else {
        Grouped { flagged.forEach { device -> row { shape -> NearbyListRow(device, shape = shape) } } }
    }
}

/** The Wi-Fi step while it runs: the lens fills as addresses are visited, then counts the devices read. */
@Composable
private fun ColumnScope.WifiStep(w: WifiSnapshot) {
    val c = MaterialTheme.colorScheme
    val left = pluralStringResource(R.plurals.about_seconds_left, w.secondsLeft, w.secondsLeft)
    val value = if (w.total > 0) w.checked else w.devices.size
    val line = if (w.total > 0) stringResource(R.string.of_total, w.total) else stringResource(R.string.found_line)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(Space.lensTop))
        LensProgress(w.fraction, value.toString(), line)
        Spacer(Modifier.height(Space.lensBottom))
        Text(stringResource(R.string.checking_wifi), style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset).semantics { heading() })
        Text(
            w.ssid?.let { stringResource(R.string.two_parts, it, left) } ?: left,
            style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset),
        )
    }
    val likely = w.count(Risk.LIKELY_CAMERA)
    SectionLabel(stringResource(R.string.found_so_far), meta = if (likely > 0) pluralStringResource(R.plurals.likely_cameras_count, likely, likely) else null)
    if (w.devices.isEmpty()) {
        Text(stringResource(R.string.nothing_answered_yet), style = KeyholeType.supporting, color = c.onSurfaceVariant, modifier = Modifier.padding(horizontal = Space.headingInset))
    } else {
        Grouped { w.devices.forEach { device -> row { shape -> DeviceListRow(device, shape = shape) } } }
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
