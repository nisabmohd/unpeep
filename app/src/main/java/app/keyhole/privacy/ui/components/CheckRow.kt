package app.keyhole.privacy.ui.components

import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.requiredSize
import app.keyhole.privacy.ui.theme.Alpha
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.ui.hue
import app.keyhole.privacy.ui.icon
import app.keyhole.privacy.ui.label
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.checkColors
import app.keyhole.privacy.ui.theme.shortSpec

/*
 * The seven checks, always the same icons in the same order: Home (with labels; tap to run one), the sweep and
 * the report (no labels; a ✓ on finished ones, a ring on the current one, an outline on the ones left out).
 */

enum class CheckState { PLAIN, DONE, CURRENT, SKIPPED }

/** One check: its hue circle and Solar icon. The hue is on the circle only. */
@Composable
fun CheckIcon(
    check: Check, modifier: Modifier = Modifier, state: CheckState = CheckState.PLAIN, pressed: Boolean = false, focused: Boolean = false,
    /** The circle's size: 44 dp on Home, 40 dp or less in a progress row (the ring and badge follow). */
    circle: Dp = Size.iconCircle,
) {
    val c = MaterialTheme.colorScheme
    val colors = checkColors(check.hue)
    val ring by animateFloatAsState(if (state == CheckState.CURRENT || focused) 1f else 0f, shortSpec(), label = "checkRing")
    val halo by animateFloatAsState(if (pressed) 1f else 0f, shortSpec(), label = "checkHalo")
    Box(modifier.widthIn(max = Size.checkSlot).size(Size.checkSlot), contentAlignment = Alignment.Center) {
        // Pressed (00-focus-states): a soft halo of the circle's own colour spreads out; the circle darkens a little.
        // Home's 44 dp circle keeps its 54 dp ring; a progress row's smaller circle has its ring 2 dp outside it (03).
        val ringSize = if (circle >= Size.iconCircle) Size.checkRing else circle + (Size.focusGap + Size.ring) * 2
        Box(Modifier.requiredSize(circle + Size.pressHalo * 2).graphicsLayer { alpha = halo * Alpha.halo }.clip(Shapes.pill).background(colors.container))
        // The current step, or keyboard focus: a primary ring a little outside the circle, fading in.
        Box(Modifier.requiredSize(ringSize).graphicsLayer { alpha = ring }.border(if (focused) Size.focusRing else Size.ring, c.primary, Shapes.pill))
        Box(
            Modifier.requiredSize(circle).clip(Shapes.pill)
                .then(
                    if (state == CheckState.SKIPPED) Modifier.border(Size.outline, c.outline, Shapes.pill)
                    else Modifier.background(if (pressed) lerp(colors.container, colors.onContainer, Alpha.pressDarken) else colors.container),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(check.icon), null, tint = if (state == CheckState.SKIPPED) c.onSurfaceVariant else colors.onContainer, modifier = Modifier.size(Size.iconInCircle * (circle / Size.iconCircle)))
        }
        Box(Modifier.requiredSize(circle), contentAlignment = Alignment.BottomEnd) {
            AnimatedVisibility(state == CheckState.DONE, Modifier.offset(Size.badgeOut, Size.badgeOut), enter = scaleIn(shortSpec()), exit = scaleOut(shortSpec())) {
                // A rim of the box's own colour, so the ✓ reads as cut out of the circle it hangs from.
                Box(Modifier.size(Size.badge).clip(Shapes.pill).background(c.surfaceContainerLow).padding(Size.ring).clip(Shapes.pill).background(c.primary), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = c.onPrimary, modifier = Modifier.size(Size.badgeIcon))
                }
            }
        }
    }
}

@Composable
private fun stateWord(state: CheckState): String? = when (state) {
    CheckState.PLAIN -> null
    CheckState.DONE -> stringResource(R.string.check_state_done)
    CheckState.CURRENT -> stringResource(R.string.check_state_current)
    CheckState.SKIPPED -> stringResource(R.string.check_state_skipped)
}

/**
 * The row of checks. With [onRun]: Home's "run one check" (labels under the icons, each 62 dp wide, scrolls
 * sideways so labels never squeeze). Without: a progress row with no labels, drawn as the sweep's steps or as the
 * report's summary of what ran.
 */
@Composable
fun CheckRow(
    checks: List<Check>, modifier: Modifier = Modifier, state: (Check) -> CheckState = { CheckState.PLAIN }, onRun: ((Check) -> Unit)? = null,
    /** The sweep's steps as it runs (03): 40 dp circles from edge to edge. Without it, the report's row of what ran (10): 44 dp in equal shares. */
    steps: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    if (onRun == null && steps) {
        // Progress (03): 40 dp circles spread edge to edge inside the box's 16 dp padding. On a narrow row they
        // shrink evenly to 36 dp, keeping at least 6 dp between them (10l); narrower still, the row scrolls sideways.
        val density = LocalDensity.current
        var width by remember { mutableIntStateOf(0) }
        val gaps = Size.checkGap * (checks.size - 1).coerceAtLeast(0)
        val fit = if (width == 0 || checks.isEmpty()) Size.checkCircle else (with(density) { width.toDp() } - gaps) / checks.size
        val circle = fit.coerceIn(Size.checkCircleMin, Size.checkCircle)
        val scrolls = width > 0 && fit < Size.checkCircleMin
        Row(
            modifier.fillMaxWidth().padding(horizontal = Space.card).onSizeChanged { width = it.width }
                .then(if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            horizontalArrangement = when {
                scrolls -> Arrangement.spacedBy(Size.checkGap)
                checks.size > 1 -> Arrangement.SpaceBetween
                else -> Arrangement.Center
            },
        ) {
            checks.forEach { check ->
                val name = stringResource(check.label)
                val s = state(check)
                val word = stateWord(s)
                CheckIcon(check, Modifier.width(circle).height(Size.checkCircle).clearAndSetSemantics { contentDescription = if (word == null) name else "$name, $word" }, s, circle = circle)
            }
        }
        return
    }
    if (onRun == null) {
        // Progress: every check fits the width, each in an equal share with at least a gap between circles; the
        // circles shrink a little on a narrow row rather than touch (a large display size), never overlap.
        val density = LocalDensity.current
        var width by remember { mutableIntStateOf(0) }
        val share = if (width == 0 || checks.isEmpty()) Size.iconCircle else with(density) { (width / checks.size).toDp() }
        // 44 dp at 412 dp, shrinking evenly to 36 dp with at least 6 dp between circles (10l); narrower than that,
        // fixed 36 dp circles and the row scrolls sideways.
        val circle = (share - Size.checkGap).coerceIn(Size.checkCircleMin, Size.iconCircle)
        val scrolls = width > 0 && share - Size.checkGap < Size.checkCircleMin
        Row(
            modifier.fillMaxWidth().padding(horizontal = Space.inline).onSizeChanged { width = it.width }
                .then(if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
        ) {
            checks.forEach { check ->
                val name = stringResource(check.label)
                val s = state(check)
                val word = stateWord(s)
                val slot = if (scrolls) Modifier.width(Size.checkCircleMin + Size.checkGap) else Modifier.weight(1f)
                CheckIcon(check, slot.height(Size.iconCircle).clearAndSetSemantics { contentDescription = if (word == null) name else "$name, $word" }, s, circle = circle)
            }
        }
        return
    }
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Space.inline)) {
        checks.forEach { check ->
            val name = stringResource(check.label)
            // The whole slot is the target, but touch and focus show on the circle only; the label never changes (00-focus-states).
            val touch = remember { MutableInteractionSource() }
            val pressed by touch.collectIsPressedAsState()
            val focused by touch.collectIsFocusedAsState()
            Column(
                Modifier.widthIn(min = Size.checkSlot)
                    .clickable(interactionSource = touch, indication = null, role = Role.Button, onClickLabel = stringResource(R.string.run_check, name)) { onRun(check) }
                    .padding(bottom = Space.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CheckIcon(check, Modifier.height(Size.iconButton), pressed = pressed, focused = focused)
                Spacer(Modifier.height(Space.small))
                Text(name, style = KeyholeType.caption, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.width(Size.checkSlot))
            }
        }
    }
}
