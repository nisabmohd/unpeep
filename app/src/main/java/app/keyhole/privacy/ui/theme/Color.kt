package app.keyhole.privacy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Keyhole colour, exactly as the canvas draws it (design/tokens.json, docs/TOKENS.md).
 * Everything comes from one seed hue; the default is dark blue, 222.
 *
 * Roles (never use a colour outside its role):
 * - background: the page, sheets.
 * - surfaceContainerLow: rows, boxes, tiles, the nav bar.
 * - surfaceContainer: round header buttons, neutral icon circles, the picked row of a sheet, the Known tag.
 * - surfaceContainerHigh: the lens rings' inner line, pressed states.
 * - primary: the one main action per screen, the lens Start, the progress arc.
 * - primaryContainer: the nav pill, the neutral lens core.
 * - secondaryContainer: the secondary button, a picked chip.
 * - error / errorContainer: "Likely camera" and errors. Nothing else is red.
 * - warning (Keyhole extra): "Worth a look".
 */
private fun tone(hue: Float, s: Int, l: Float): Color = Color.hsl(hue, s / 100f, l / 100f)

/** Hue of "Likely camera" and errors. */
private const val ERROR_HUE = 356f

fun seedColorScheme(hue: Float, dark: Boolean): ColorScheme = if (dark) {
    val bg = tone(hue, 22, 6.5f)
    val text = tone(hue, 24, 92f)
    darkColorScheme(
        primary = tone(hue, 86, 79f),
        onPrimary = tone(hue, 62, 16f),
        primaryContainer = tone(hue, 44, 25f),
        onPrimaryContainer = tone(hue, 90, 92f),
        secondary = tone(hue, 86, 79f),
        onSecondary = tone(hue, 62, 16f),
        secondaryContainer = tone(hue, 20, 23f),
        onSecondaryContainer = tone(hue, 40, 91f),
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = tone(hue, 16, 15.5f),
        onSurfaceVariant = tone(hue, 12, 72f),
        surfaceContainerLowest = bg,
        surfaceContainerLow = tone(hue, 20, 11f),
        surfaceContainer = tone(hue, 16, 15.5f),
        surfaceContainerHigh = tone(hue, 14, 21f),
        surfaceContainerHighest = tone(hue, 13, 25f),
        outline = tone(hue, 10, 36f),
        outlineVariant = tone(hue, 14, 21f),
        error = tone(ERROR_HUE, 90, 78f),
        onError = tone(ERROR_HUE, 60, 16f),
        errorContainer = tone(ERROR_HUE, 42, 25f),
        onErrorContainer = tone(ERROR_HUE, 90, 92f),
        scrim = Color(0x99000000),
    )
} else {
    val bg = tone(hue, 40, 98.2f)
    val text = tone(hue, 34, 11f)
    lightColorScheme(
        primary = tone(hue, 70, 30f),
        onPrimary = Color.White,
        primaryContainer = tone(hue, 82, 91.5f),
        onPrimaryContainer = tone(hue, 72, 16f),
        secondary = tone(hue, 70, 30f),
        onSecondary = Color.White,
        secondaryContainer = tone(hue, 34, 87.5f),
        onSecondaryContainer = tone(hue, 36, 16f),
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = tone(hue, 30, 91.5f),
        onSurfaceVariant = tone(hue, 12, 37f),
        surfaceContainerLowest = bg,
        surfaceContainerLow = tone(hue, 36, 94.8f),
        surfaceContainer = tone(hue, 30, 91.5f),
        surfaceContainerHigh = tone(hue, 22, 87.5f),
        surfaceContainerHighest = tone(hue, 20, 84f),
        outline = tone(hue, 14, 72f),
        outlineVariant = tone(hue, 22, 87.5f),
        error = tone(ERROR_HUE, 66, 42f),
        onError = Color.White,
        errorContainer = tone(ERROR_HUE, 86, 92f),
        onErrorContainer = tone(ERROR_HUE, 62, 22f),
        scrim = Color(0x6B0C1020),
    )
}

/** "Worth a look": the amber Material lacks. solid = icons and bars, container + onContainer = tags and circles. */
@Immutable
data class WarningColors(val solid: Color, val container: Color, val onContainer: Color)

fun warningColors(dark: Boolean) = if (dark) {
    WarningColors(tone(38f, 92, 62f), tone(36f, 38, 21f), tone(40f, 92, 86f))
} else {
    WarningColors(tone(34f, 92, 46f), tone(40f, 92, 88f), tone(30f, 70, 20f))
}

/**
 * The fixed colours of one check, from its hue (docs/DESIGN.md § Colour): on its icon circle only, never on text.
 * They follow light/dark only, never the accent or dynamic colour, so Wi-Fi is always blue.
 */
@Immutable
data class CheckColors(val container: Color, val onContainer: Color)

fun checkColors(hue: Float, dark: Boolean) = if (dark) {
    CheckColors(tone(hue, 32, 23f), tone(hue, 74, 84f))
} else {
    CheckColors(tone(hue, 74, 90f), tone(hue, 64, 26f))
}

/** Hues of the seven checks and of icon circles that take a fixed colour (More tiles, sheet choices). */
object Hues {
    const val WIFI = 232f
    const val HOTSPOTS = 150f
    const val BLUETOOTH = 268f
    const val LENS = 34f
    const val INFRARED = 338f
    const val MAGNET = 92f
    const val CHECKLIST = 186f
    /** A report (Home's last report, Reports rows): the amber of paper. */
    const val REPORT = 40f
}

/** The seed. */
const val DEFAULT_ACCENT_HUE = 222f

/** Accent swatches offered in Appearance (the first is the default). */
val AccentHues = listOf(DEFAULT_ACCENT_HUE, 268f, 338f, 12f, 150f, 186f)

/** The viewfinder (Lens finder, Infrared) is always dark, whatever the theme: a camera screen. */
@Immutable
data class ViewfinderColors(val page: Color, val content: Color, val muted: Color, val control: Color, val ring: Color)

val Viewfinder = ViewfinderColors(
    page = Color(0xFF05070B), content = Color(0xFFF1F3F9), muted = Color(0xFFAEB4C4),
    control = Color(0x33FFFFFF), ring = Color(0xFFFFC94D),
)

/** The red filter of the lens finder: what multiplies the preview so glints stand out. */
val RedFilter = Color(0xFFFF2A2A)

/**
 * Every see-through colour has one of these names: the same meaning is always the same strength.
 * `ProjectRulesTest` keeps raw alphas out of screens.
 */
object Alpha {
    /** The lens' outer ring (the faintest). */
    const val ringOuter = 0.55f
    /** The lens' outermost ring when there are three. */
    const val ringFaint = 0.3f
    /** The lens' middle ring. */
    const val ringMiddle = 0.8f
    /** The track under a progress arc and under meter bars. */
    const val track = 0.5f
    /** A meter bar that isn't the newest. */
    const val barOld = 0.45f
    /** Material's disabled container and content. */
    const val disabledContainer = 0.10f
    const val disabledContent = 0.38f
    /** The soft shadow under the lens Start. */
    const val shadow = 0.28f
    /** The red / contrast filter over the preview. */
    const val filter = 0.55f
}
