package app.keyhole.privacy.ui.wifi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Risk
import app.keyhole.privacy.domain.WifiSnapshot
import app.keyhole.privacy.scan.WifiSession
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.EmptyState
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.theme.KeyholeTheme
import kotlinx.coroutines.flow.StateFlow

class WifiOthersViewModel(session: WifiSession) : ViewModel() {
    val state: StateFlow<WifiSnapshot> = session.state
}

/** The devices behind "15 known devices" and "3 devices not recognised": each opens like any other device. */
@Composable
fun WifiOthersScreen(vm: WifiOthersViewModel, onBack: () -> Unit, onDevice: (String) -> Unit) {
    val w by vm.state.collectAsStateWithLifecycle()
    WifiOthersContent(w, onBack, onDevice)
}

@Composable
fun WifiOthersContent(w: WifiSnapshot, onBack: () -> Unit, onDevice: (String) -> Unit) {
    val known = w.devices.filter { it.risk == Risk.KNOWN }
    val other = w.devices.filter { it.risk == Risk.UNKNOWN }
    DetailScreen(onBack, title = stringResource(R.string.other_devices_title)) {
        if (known.isEmpty() && other.isEmpty()) {
            EmptyState(painterResource(R.drawable.ic_wifi), stringResource(R.string.other_devices_title), stringResource(R.string.nothing_answered_yet))
        }
        if (other.isNotEmpty()) {
            SectionLabel(stringResource(R.string.risk_not_recognised), meta = other.size.toString())
            Grouped { other.forEach { d -> row { shape -> DeviceListRow(d, shape = shape, onClick = { onDevice(d.ip) }) } } }
        }
        if (known.isNotEmpty()) {
            SectionLabel(stringResource(R.string.risk_known), meta = known.size.toString())
            Grouped { known.forEach { d -> row { shape -> DeviceListRow(d, shape = shape, onClick = { onDevice(d.ip) }) } } }
        }
    }
}

@Preview
@Composable
private fun WifiOthersPreview() = KeyholeTheme { WifiOthersContent(previewWifi(), {}, {}) }
