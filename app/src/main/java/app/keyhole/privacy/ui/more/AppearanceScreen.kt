package app.keyhole.privacy.ui.more

import app.keyhole.privacy.ui.components.StepSlider
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.KeyholeType
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import app.keyhole.privacy.ui.components.ListRow
import app.keyhole.privacy.ui.components.Grouped
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.fontSample
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import android.os.Build
import app.keyhole.privacy.R
import app.keyhole.privacy.data.Settings
import app.keyhole.privacy.data.SettingsRepository
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.ui.components.Box24
import app.keyhole.privacy.ui.components.ChoiceRow
import app.keyhole.privacy.ui.components.Chip
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.ColorSwatch
import app.keyhole.privacy.ui.components.DetailScreen
import app.keyhole.privacy.ui.components.KeyholeSheet
import app.keyhole.privacy.ui.components.Note
import app.keyhole.privacy.ui.components.ReorderItem
import app.keyhole.privacy.ui.components.ReorderList
import app.keyhole.privacy.ui.components.SectionLabel
import app.keyhole.privacy.ui.components.Segment
import app.keyhole.privacy.ui.components.Segmented
import app.keyhole.privacy.ui.components.SwitchRow
import app.keyhole.privacy.ui.components.TopBarAction
import app.keyhole.privacy.ui.hue
import app.keyhole.privacy.ui.icon
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.theme.AccentHues
import app.keyhole.privacy.ui.theme.BodyFont
import app.keyhole.privacy.ui.theme.KeyholeTheme
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.TextSize
import app.keyhole.privacy.ui.theme.ThemeMode
import app.keyhole.privacy.ui.theme.isAppDark
import app.keyhole.privacy.ui.theme.seedColorScheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<Settings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun mode(m: ThemeMode) = launch { settings.setThemeMode(m) }
    fun dynamic(on: Boolean) = launch { settings.setDynamicColor(on) }
    fun accent(hue: Float) = launch { settings.setAccentHue(hue) }
    fun font(f: BodyFont) = launch { settings.setFont(f) }
    fun textSize(t: TextSize) = launch { settings.setTextSize(t) }

    /** Moves a step; the order saved is always all seven checks. */
    fun move(from: Int, to: Int) {
        val order = state.value?.stepOrder?.toMutableList() ?: return
        if (from !in order.indices || to !in order.indices) return
        order.add(to, order.removeAt(from))
        launch { settings.setStepOrder(order) }
    }

    fun step(check: Check, on: Boolean) = launch { settings.setStepOn(check, on) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

/** An accent's name for TalkBack, in the order of [AccentHues]. */
private val ACCENT_NAMES = listOf(R.string.colour_accent, R.string.accent_purple, R.string.accent_pink, R.string.accent_coral, R.string.accent_green, R.string.accent_teal)

@Composable
fun textSizeLabel(t: TextSize): String = stringResource(
    when (t) {
        TextSize.SMALL -> R.string.text_size_small
        TextSize.DEFAULT -> R.string.text_size_default
        TextSize.LARGE -> R.string.text_size_large
    },
)

/** 14 Appearance: theme · wallpaper colours or an accent · font and text size · sweep steps. */
@Composable
fun AppearanceScreen(vm: AppearanceViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val settings = s ?: return
    AppearanceContent(settings, onBack, vm::mode, vm::dynamic, vm::accent, vm::font, onTextSize = vm::textSize, onMove = vm::move, onStep = vm::step)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceContent(
    s: Settings, onBack: () -> Unit, onMode: (ThemeMode) -> Unit, onDynamic: (Boolean) -> Unit, onAccent: (Float) -> Unit, onFont: (BodyFont) -> Unit,
    onTextSize: (TextSize) -> Unit, onMove: (Int, Int) -> Unit, onStep: (Check, Boolean) -> Unit,
) {
    val dark = isAppDark()
    DetailScreen(onBack, title = stringResource(R.string.appearance)) {
        Box24(title = stringResource(R.string.theme)) {
            Segmented(
                listOf(Segment(themeModeLabel(ThemeMode.SYSTEM)), Segment(themeModeLabel(ThemeMode.LIGHT)), Segment(themeModeLabel(ThemeMode.DARK))),
                s.theme.mode.ordinal, { onMode(ThemeMode.entries[it]) }, Modifier.padding(horizontal = Space.card), fill = true,
            )
        }
        Spacer(Modifier.height(Space.block))
        Box24 {
            // Wallpaper colours exist from Android 12; before, only the accents.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(stringResource(R.string.colour_wallpaper_title), s.theme.dynamicColor, onDynamic, line = stringResource(R.string.colour_wallpaper_line))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.card).selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
                AccentHues.forEachIndexed { i, hue ->
                    ColorSwatch(seedColorScheme(hue, dark).primary, stringResource(ACCENT_NAMES[i]), !s.theme.dynamicColor && s.theme.accentHue == hue, { onAccent(hue) })
                }
            }
        }
        Spacer(Modifier.height(Space.block))
        // Font (as Burrow): each one written in itself, with what it is for; the picked one has a ✓.
        SectionLabel(stringResource(R.string.font))
        Grouped(Modifier.selectableGroup()) {
            BodyFont.entries.forEach { f ->
                row { shape ->
                    val on = s.theme.font == f
                    ListRow(
                        fontLabel(f), shape = shape, line = stringResource(f.line),
                        leading = {
                            Box(Modifier.size(Size.iconCircle).clip(Shapes.pill).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.font_sample), style = fontSample(f, KeyholeType.cardTitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        trailing = { if (on) Icon(painterResource(R.drawable.ic_check), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Size.icon)) },
                        onClick = { onFont(f) }, selected = on,
                    )
                }
            }
        }
        // Text size (as Weft): its name and the size now, then a slider that stops at each size.
        Spacer(Modifier.height(Space.block))
        val sizes = TextSize.entries
        StepSlider(
            stringResource(R.string.text_size), textSizeLabel(s.theme.textSize), sizes.indexOf(s.theme.textSize), sizes.size,
            onStep = { onTextSize(sizes[it]) },
        )
        SweepStepsSection(s, onMove, onStep)
    }
}

/** The steps of a sweep, to switch and reorder; on Appearance and on their own screen (More › Sweep steps). */
@Composable
fun SweepStepsSection(s: Settings, onMove: (Int, Int) -> Unit, onStep: (Check, Boolean) -> Unit) {
    SectionLabel(stringResource(R.string.sweep_steps), meta = stringResource(R.string.drag_to_reorder))
    ReorderList(
        s.stepOrder.map { check -> ReorderItem(check.name, stringResource(check.label), painterResource(check.icon), CircleTone.Hue(check.hue), check !in s.stepsOff) },
        onMove = onMove, onToggle = { i, on -> onStep(s.stepOrder[i], on) },
        moveUp = stringResource(R.string.move_up), moveDown = stringResource(R.string.move_down),
    )
}

/** More › Sweep steps: the same list, with what it means first. */
@Composable
fun SweepStepsScreen(vm: AppearanceViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val settings = s ?: return
    DetailScreen(onBack, title = stringResource(R.string.sweep_steps)) {
        Note(stringResource(R.string.sweep_steps_note))
        SweepStepsSection(settings, vm::move, vm::step)
    }
}

/** More › Language: the phone's language, or one of the seven Keyhole ships. Applied at once. */
@Composable
fun LanguageSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val current = AppLanguage.current(context)
    KeyholeSheet(stringResource(R.string.language), onDismiss, line = stringResource(R.string.language_line)) {
        Column(Modifier.selectableGroup()) {
            ChoiceRow(stringResource(R.string.system_default), current == null, { activity?.let { AppLanguage.set(it, null) }; onDismiss() })
            AppLanguage.LANGUAGES.forEach { l ->
                ChoiceRow(l.nativeName, current == l.tag, { activity?.let { AppLanguage.set(it, l.tag) }; onDismiss() }, line = l.englishName.takeIf { it != l.nativeName })
            }
        }
        Spacer(Modifier.height(Space.block))
    }
}

@Preview
@Composable
private fun AppearancePreview() = KeyholeTheme { AppearanceContent(Settings(), {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }) }

/** What each font is for (Appearance › Font). */
private val BodyFont.line: Int
    @androidx.annotation.StringRes get() = when (this) {
        BodyFont.LEXEND -> R.string.font_lexend_line
        BodyFont.OUTFIT -> R.string.font_outfit_line
        BodyFont.ATKINSON -> R.string.font_atkinson_line
        BodyFont.SPACE_GROTESK -> R.string.font_space_grotesk_line
    }
