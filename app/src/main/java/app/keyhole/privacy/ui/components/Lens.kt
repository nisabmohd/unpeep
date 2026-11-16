package app.keyhole.privacy.ui.components

import androidx.compose.ui.text.style.TextOverflow
import app.keyhole.privacy.ui.theme.checkColors
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.theme.Alpha
import app.keyhole.privacy.ui.theme.ButtonMinText
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.LensSize
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.lensCore
import app.keyhole.privacy.ui.theme.progressSpec
import kotlin.math.cos
import kotlin.math.sin

/*
 * The lens (docs/DESIGN.md § Keyhole's own): concentric rings and a glint, from the logo. One drawing for the
 * Start button, the sweep progress, Find it, Magnet and every detail header, so they always match.
 */

/** Where the glint sits on the outer ring, and where a progress arc starts (12 o'clock). */
private const val GLINT_ANGLE = -50f
private const val ARC_START = -90f
/** The lens core's line never shrinks below this share of a caption. */
private const val MIN_LINE = 0.85f

/**
 * The lens drawing: rings around a [core]-wide middle, in a [size] box. [middleRing] adds the third ring of the
 * big lenses. With [glint], the dot on the outer ring. With [progress] (0..1), an arc on the middle ring with its
 * dot riding the end, eased to each new value.
 */
@Composable
fun LensRings(
    size: Dp,
    core: Dp,
    modifier: Modifier = Modifier,
    middleRing: Boolean = false,
    glint: Boolean = false,
    progress: Float? = null,
    content: @Composable () -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    val shown by animateFloatAsState(progress?.coerceIn(0f, 1f) ?: 0f, progressSpec(), label = "lensProgress")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = LensSize.ringStroke.toPx()
            val outer = this.size.minDimension / 2 - stroke / 2
            val inner = core.toPx() / 2 + LensSize.coreGap.toPx()
            val middle = (outer + inner) / 2
            drawCircle(c.outline.copy(alpha = Alpha.ringFaint), outer, style = Stroke(stroke))
            if (middleRing || progress != null) drawCircle(c.outline.copy(alpha = Alpha.ringOuter), middle, style = Stroke(stroke))
            // The progress lens has a white core of its own (with a shadow), so no ring hugs it.
            if (progress == null) drawCircle(c.outline.copy(alpha = Alpha.ringMiddle), if (size <= LensSize.header) core.toPx() / 2 + LensSize.headerRing.toPx() else inner, style = Stroke(stroke))
            if (glint) drawCircle(c.primary, LensSize.glint.toPx() / 2, onRing(outer, GLINT_ANGLE))
            if (progress != null) {
                val arc = LensSize.arcStroke.toPx()
                // The arc runs just inside the middle ring's line (03: r 87 in a 220 lens), its dot on the arc's end.
                val track = if (size >= LensSize.progressLarge) LensSize.progressLargeArc.toPx() else middle - stroke
                val topLeft = Offset(center.x - track, center.y - track)
                val box = androidx.compose.ui.geometry.Size(track * 2, track * 2)
                if (shown > 0f) drawArc(c.primary, ARC_START, 360f * shown, false, topLeft, box, style = Stroke(arc, cap = StrokeCap.Round))
                val dot = onRing(track, ARC_START + 360f * shown)
                drawCircle(c.primary, LensSize.arcDot.toPx() / 2, dot)
                drawCircle(c.background, LensSize.arcDotHole.toPx() / 2, dot)
            }
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.onRing(radius: Float, degrees: Float): Offset {
    val a = Math.toRadians(degrees.toDouble())
    return Offset(center.x + radius * cos(a).toFloat(), center.y + radius * sin(a).toFloat())
}

/** Home's Start: the only filled hero in the app. A 136 dp primary core in three rings with the glint. */
@Composable
fun LensStartButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    LensRings(LensSize.start, LensSize.startCore, modifier, middleRing = true, glint = true) {
        Column(
            Modifier
                .size(LensSize.startCore)
                .focusRing(Shapes.pill)
                .shadow(LensSize.shadow, Shapes.pill, ambientColor = c.primary, spotColor = c.primary)
                .clip(Shapes.pill)
                .background(c.primary)
                .clickable(role = Role.Button, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(R.drawable.ic_radar), null, tint = c.onPrimary, modifier = Modifier.size(LensSize.startIcon))
            Spacer(Modifier.height(Space.small))
            CappedFontScale { Text(label, style = KeyholeType.cardTitle, color = c.onPrimary, maxLines = 1, softWrap = false) }
        }
    }
}

/**
 * The progress lens (sweep, Find it, Magnet): the arc on the middle ring and, in the core, a big value over a
 * small line ("11" over "of 18"). With [icon] (Find it, Magnet: the check's circle), the value is a word or a
 * reading under it ("Close" over "about 1–2 m"; "+14 µT" over "above normal") in the title size, so all three fit.
 */
@Composable
fun LensProgress(progress: Float, value: String, line: String, modifier: Modifier = Modifier, description: String = "$value $line", icon: (@Composable () -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    // Two sizes, set by what the core holds (final v2): a count fits 220 (core 124); an icon, a value and a line
    // (Find it, Magnet) take 248 (core 152), so the value never wraps or touches the ring.
    val ring = if (icon != null) LensSize.progressLarge else LensSize.progress
    val core = if (icon != null) LensSize.progressLargeCore else LensSize.progressCore
    LensRings(ring, core, modifier.clearAndSetSemantics { contentDescription = description }, progress = progress) {
        Column(
            Modifier.size(core).shadow(LensSize.coreShadow, Shapes.pill, ambientColor = c.primary, spotColor = c.primary)
                .clip(Shapes.pill).background(lensCore())
                
                // The core's own ring, the lens rings' line (03).
                .drawWithContent {
                    drawContent()
                    val line = LensSize.ringStroke.toPx()
                    drawCircle(c.surfaceContainerHigh, size.minDimension / 2 - line / 2, style = Stroke(line))
                }
                .padding(Space.inline),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CappedFontScale {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (icon != null) {
                        icon()
                        Spacer(Modifier.height(Space.inline))
                    }
                    // One line that shrinks to fit the core, never clipped: "Right here", "+14 µT", long translations.
                    // A count is 30/38 over a 12/16 line, 2 dp apart (03).
                    val style = if (icon != null) KeyholeType.title else KeyholeType.headline
                    BasicText(
                        value, style = style.copy(color = c.onSurface, textAlign = TextAlign.Center), maxLines = 1, softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = ButtonMinText, maxFontSize = style.fontSize),
                    )
                    val lineStyle = KeyholeType.small
                    if (icon == null) Spacer(Modifier.height(Space.row))
                    BasicText(
                        line, style = lineStyle.copy(color = c.onSurfaceVariant, textAlign = TextAlign.Center), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(minFontSize = KeyholeType.caption.fontSize * MIN_LINE, maxFontSize = lineStyle.fontSize),
                        modifier = Modifier.widthIn(max = core - Space.card * 2),
                    )
                }
            }
        }
    }
}

/**
 * The first block of every detail screen and every empty state: the small lens with a coloured core and icon,
 * a title, one line, and tags. [container] / [onContainer] colour the core (a check's hue, red for a likely camera);
 * left out, the core is primaryContainer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LensHeader(
    icon: Painter,
    title: String?,
    modifier: Modifier = Modifier,
    line: String? = null,
    container: Color? = null,
    onContainer: Color? = null,
    tags: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    // 12 dp under the top bar on every detail board (05, 10, 11, 13f).
    Column(modifier.fillMaxWidth().padding(top = Space.block), horizontalAlignment = Alignment.CenterHorizontally) {
        LensRings(LensSize.header, LensSize.headerCore) {
            Box(Modifier.size(LensSize.headerCore).clip(Shapes.pill).background(container ?: c.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = onContainer ?: c.onPrimaryContainer, modifier = Modifier.size(Size.headerIcon))
            }
        }
        Spacer(Modifier.height(Space.card))
        if (title != null) {
            Text(title, style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset).semantics { heading() })
        }
        if (line != null) {
            Text(line, style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, 
                // 4 dp under the title; 2 dp when tags follow (05, 10), so title, line and tags read as one block.
                // Without a title the line takes the title's place (04e).
                modifier = Modifier.padding(top = if (title == null) Space.none else if (tags != null) Space.row else Space.small).padding(horizontal = Space.heading).widthIn(max = Size.emptyLine),
            )
        }
        if (tags != null) {
            Spacer(Modifier.height(Space.block))
            FlowRow(
                Modifier.padding(horizontal = Space.screen),
                horizontalArrangement = Arrangement.spacedBy(Space.inline, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Space.inline),
            ) { tags() }
        }
    }
}

/** What sits above an [EmptyState], which sets how far down its lens starts (as the boards draw each). */
enum class EmptyUnder {
    /** A top bar: 56 dp (04e, 12a, 13e). */
    BAR,
    /** The sweep's check row: 48 dp (03b, 03c). */
    CHECK_ROW,
    /** A results summary box: 40 dp (04h). */
    SUMMARY,
}

/**
 * A list or a check with nothing to show: the lens header, lower on the page.
 * [hue]: the check it belongs to, which colours the core (Bluetooth is off → Bluetooth's purple).
 */
@Composable
fun EmptyState(icon: Painter, title: String?, line: String, modifier: Modifier = Modifier, hue: Float? = null, under: EmptyUnder = EmptyUnder.BAR) {
    val colors = hue?.let { checkColors(it) }
    val top = when (under) {
        EmptyUnder.BAR -> Space.empty
        EmptyUnder.CHECK_ROW -> Space.emptyUnderRow
        EmptyUnder.SUMMARY -> Space.emptyUnderSummary
    }
    LensHeader(icon, title, modifier.padding(top = top), line = line, container = colors?.container, onContainer = colors?.onContainer)
}
