package app.keyhole.privacy.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.keyhole.privacy.R
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.NeutralCircle
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.TabScreen
import app.keyhole.privacy.ui.components.Tile
import app.keyhole.privacy.ui.components.isLargeText
import app.keyhole.privacy.ui.nav.MoreItem
import app.keyhole.privacy.ui.theme.BodyFont
import app.keyhole.privacy.ui.theme.Hues
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.ThemeMode
import app.keyhole.privacy.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class MoreState(val theme: ThemeSettings = ThemeSettings(), val stepsOn: Int = Check.entries.size, val markedKnown: Int = 0)

class MoreViewModel(settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<MoreState> = settings.settings.map { MoreState(it.theme, Check.entries.size - it.stepsOff.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreState())
}

@Composable
fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
fun fontLabel(font: BodyFont): String = stringResource(
    when (font) {
        BodyFont.LEXEND -> R.string.font_lexend
        BodyFont.OUTFIT -> R.string.font_outfit
        BodyFont.ATKINSON -> R.string.font_atkinson
        BodyFont.SPACE_GROTESK -> R.string.font_space_grotesk
    },
)

/** 13 More: four tiles (Appearance · Language · Help numbers · Sweep steps), then Checks and App rows. */
@Composable
fun MoreScreen(vm: MoreViewModel, onOpen: (MoreItem) -> Unit, onHelpNumbers: () -> Unit = {}, helpLine: String = "") {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    TabScreen(stringResource(R.string.tab_more)) {
        Column(Modifier.weight(1f).widthIn(max = Size.contentMax).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.screen)) {
            val appearance: @Composable (Modifier) -> Unit = { m ->
                Tile(
                    painterResource(R.drawable.ic_palette), CircleTone.Accent, stringResource(R.string.appearance),
                    stringResource(R.string.two_parts, stringResource(if (s.theme.dynamicColor) R.string.colour_wallpaper else R.string.colour_accent), fontLabel(s.theme.font)),
                    { onOpen(MoreItem.APPEARANCE) }, m, value = themeModeLabel(s.theme.mode),
                )
            }
            val language: @Composable (Modifier) -> Unit = { m ->
                Tile(
                    painterResource(R.drawable.ic_globe), CircleTone.Hue(Hues.CHECKLIST), stringResource(R.string.language),
                    AppLanguage.current(context)?.let { AppLanguage.currentName(context) } ?: stringResource(R.string.system_default),
                    { onOpen(MoreItem.LANGUAGE) }, m,
                )
            }
            val help: @Composable (Modifier) -> Unit = { m ->
                Tile(painterResource(R.drawable.ic_phone), CircleTone.Hue(Hues.INFRARED), stringResource(R.string.help_numbers), helpLine, onHelpNumbers, m)
            }
            val steps: @Composable (Modifier) -> Unit = { m ->
                Tile(
                    painterResource(R.drawable.ic_more), CircleTone.Hue(Hues.BLUETOOTH), stringResource(R.string.sweep_steps), stringResource(R.string.sweep_steps_line),
                    { onOpen(MoreItem.SWEEP_STEPS) }, m, value = pluralStringResource(R.plurals.steps_on, s.stepsOn, s.stepsOn),
                )
            }
            // Two by two; one column at large text, so titles never squeeze.
            if (isLargeText()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.tileGap)) { listOf(appearance, language, help, steps).forEach { it(Modifier.fillMaxWidth()) } }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Space.tileGap)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.tileGap)) { appearance(Modifier.weight(1f)); language(Modifier.weight(1f)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.tileGap)) { help(Modifier.weight(1f)); steps(Modifier.weight(1f)) }
                }
            }
            SectionLabel(stringResource(R.string.section_checks))
            Grouped {
                row { shape ->
                    ListRow(
                        stringResource(R.string.marked_as_known), shape = shape,
                        line = if (s.markedKnown == 0) stringResource(R.string.marked_known_none) else pluralStringResource(R.plurals.marked_known_count, s.markedKnown, s.markedKnown),
                        leading = { NeutralCircle(painterResource(R.drawable.ic_shield)) }, onClick = { onOpen(MoreItem.MARKED_KNOWN) },
                    )
                }
                row { shape -> ListRow(stringResource(R.string.how_keyhole_works), shape = shape, line = stringResource(R.string.how_keyhole_works_line), leading = { NeutralCircle(painterResource(R.drawable.ic_info)) }, onClick = { onOpen(MoreItem.HOW_IT_WORKS) }) }
                row { shape -> ListRow(stringResource(R.string.what_keyhole_cant_find), shape = shape, line = stringResource(R.string.what_keyhole_cant_find_line), leading = { NeutralCircle(painterResource(R.drawable.ic_eye_closed)) }, onClick = { onOpen(MoreItem.LIMITS) }) }
                row { shape -> ListRow(stringResource(R.string.if_you_find_a_camera), shape = shape, line = stringResource(R.string.if_you_find_a_camera_line), leading = { NeutralCircle(painterResource(R.drawable.ic_camera_device)) }, onClick = { onOpen(MoreItem.FOUND_CAMERA) }) }
            }
            SectionLabel(stringResource(R.string.section_app))
            Grouped {
                row { shape -> ListRow(stringResource(R.string.privacy), shape = shape, line = stringResource(R.string.privacy_line), leading = { NeutralCircle(painterResource(R.drawable.ic_lock)) }, onClick = { onOpen(MoreItem.PRIVACY) }) }
                row { shape -> ListRow(stringResource(R.string.about), shape = shape, line = stringResource(R.string.about_line), leading = { NeutralCircle(painterResource(R.drawable.ic_info)) }, onClick = { onOpen(MoreItem.ABOUT) }) }
            }
            Spacer(Modifier.height(Space.screen))
        }
    }
}
