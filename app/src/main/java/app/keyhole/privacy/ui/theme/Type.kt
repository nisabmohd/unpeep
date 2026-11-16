package app.keyhole.privacy.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.keyhole.privacy.R

/*
 * Eight text styles, two weights (400, 500). Nothing else, anywhere (docs/TOKENS.md § Type).
 * Fonts are variable TTFs in res/font (OFL, see FONTS-LICENSE.txt), one file per family.
 *
 * The designs are drawn in a browser, which renders strokes heavier than Android does, so each family is drawn a
 * step heavier than its nominal weight to match the PNGs (the same numbers as Burrow and Weft).
 */
@OptIn(ExperimentalTextApi::class)
private fun variable(@FontRes res: Int, regular: Int, medium: Int) = FontFamily(
    Font(res, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(regular))),
    Font(res, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(medium))),
)

private val LexendFamily = variable(R.font.lexend, 440, 540)
private val OutfitFamily = variable(R.font.outfit, 450, 550)
private val AtkinsonFamily = variable(R.font.atkinson, 440, 680)
private val SpaceGroteskFamily = variable(R.font.space_grotesk, 490, 620)

fun BodyFont.family(): FontFamily = when (this) {
    BodyFont.LEXEND -> LexendFamily
    BodyFont.OUTFIT -> OutfitFamily
    BodyFont.ATKINSON -> AtkinsonFamily
    BodyFont.SPACE_GROTESK -> SpaceGroteskFamily
}

/** "Aa" written in [font] (Appearance › Font): the one place a font is chosen on screen, not by the theme. */
fun fontSample(font: BodyFont, base: TextStyle): TextStyle = base.copy(fontFamily = font.family())

fun keyholeTypography(font: BodyFont, scale: Float, rtl: Boolean = false): Typography {
    val f = font.family()
    // Line height as a ratio (em), not sp: Android 14+ scales big sp values less than small ones, so a fixed sp
    // line crowds wrapped lines at large font sizes (Burrow, 2026-10-24).
    fun s(size: Int, line: Int, w: FontWeight) = TextStyle(
        fontFamily = f, fontWeight = w, fontSize = (size * scale).sp, lineHeight = (line.toFloat() / size).em,
        fontFeatureSettings = "tnum",
        // Each paragraph takes the direction of its first letter; a bare number or an IP address reads left to right.
        textDirection = TextDirection.ContentOrLtr,
        textAlign = if (rtl) TextAlign.Right else TextAlign.Left,
    )
    val display = s(32, 40, FontWeight.Medium)
    val headline = s(30, 38, FontWeight.Medium)
    val title = s(22, 28, FontWeight.Medium)
    val cardTitle = s(16, 22, FontWeight.Medium)
    val body = s(16, 24, FontWeight.Normal)
    val supporting = s(14, 20, FontWeight.Normal)
    val label = s(14, 20, FontWeight.Medium)
    val caption = s(12, 16, FontWeight.Medium)
    return Typography(
        displayLarge = display, displayMedium = display, displaySmall = display,
        headlineLarge = headline, headlineMedium = headline, headlineSmall = title,
        titleLarge = title, titleMedium = cardTitle, titleSmall = label,
        bodyLarge = body, bodyMedium = supporting, bodySmall = supporting,
        labelLarge = label, labelMedium = caption, labelSmall = caption,
    )
}

/** Nav and button labels shrink to fit long translations, never below these. */
val NavLabelMinSize = 10.sp
val ButtonMinText = 12.sp

/** The only text styles screens use. Colour is never part of a style. */
object KeyholeType {
    /** 32/40 Medium: Welcome, big counts. */
    val display: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.displayLarge

    /** 30/38 Medium: tab titles. */
    val headline: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.headlineMedium

    /** 22/28 Medium: top bars, sheets, lens headers. */
    val title: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleLarge

    /** 16/22 Medium: rows, boxes, tiles. */
    val cardTitle: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleMedium

    /** 16/24 Regular: paragraphs. */
    val body: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge

    /** 14/20 Regular: second lines. */
    val supporting: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyMedium

    /** 14/20 Medium: buttons, chips, section labels. */
    val label: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelLarge

    /** 12/16 Medium: nav, check row, tags. */
    val caption: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelMedium
}
