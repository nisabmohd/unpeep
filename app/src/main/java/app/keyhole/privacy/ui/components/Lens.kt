package app.keyhole.privacy.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.LensSize
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
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
            drawCircle(c.outline.copy(alpha = Alpha.ringMiddle), inner, style = Stroke(stroke))
            if (glint) drawCircle(c.primary, LensSize.glint.toPx() / 2, onRing(outer, GLINT_ANGLE))
            if (progress != null) {
                val arc = LensSize.arcStroke.toPx()
                val topLeft = Offset(center.x - middle, center.y - middle)
                val box = androidx.compose.ui.geometry.Size(middle * 2, middle * 2)
                if (shown > 0f) drawArc(c.primary, ARC_START, 360f * shown, false, topLeft, box, style = Stroke(arc, cap = StrokeCap.Round))
                val dot = onRing(middle, ARC_START + 360f * shown)
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
 * small line ("11" over "of 18"; "+14" over "µT above normal").
 */
@Composable
fun LensProgress(progress: Float, value: String, line: String, modifier: Modifier = Modifier, description: String = "$value $line") {
    val c = MaterialTheme.colorScheme
    LensRings(LensSize.progress, LensSize.progressCore, modifier.clearAndSetSemantics { contentDescription = description }, progress = progress) {
        Column(
            Modifier.size(LensSize.progressCore).clip(Shapes.pill).background(c.background).padding(Space.inline),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CappedFontScale {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = KeyholeType.display, color = c.onSurface, maxLines = 1, softWrap = false)
                    Text(line, style = KeyholeType.supporting, color = c.onSurfaceVariant, maxLines = 2, textAlign = TextAlign.Center)
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
    title: String,
    modifier: Modifier = Modifier,
    line: String? = null,
    container: Color? = null,
    onContainer: Color? = null,
    tags: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(top = Space.lensTop), horizontalAlignment = Alignment.CenterHorizontally) {
        LensRings(LensSize.header, LensSize.headerCore) {
            Box(Modifier.size(LensSize.headerCore).clip(Shapes.pill).background(container ?: c.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = onContainer ?: c.onPrimaryContainer, modifier = Modifier.size(Size.icon))
            }
        }
        Spacer(Modifier.height(Space.lensBottom))
        Text(title, style = KeyholeType.title, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.heading).semantics { heading() })
        if (line != null) {
            Text(line, style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.heading).widthIn(max = Size.emptyLine))
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

/** Every empty list: the lens header and one line, a little lower on the page. */
@Composable
fun EmptyState(icon: Painter, title: String, line: String, modifier: Modifier = Modifier) {
    LensHeader(icon, title, modifier.padding(top = Space.empty), line = line)
}
