package app.unpeep.privacy.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.unpeep.privacy.R
import app.unpeep.privacy.ui.theme.Alpha
import app.unpeep.privacy.ui.theme.ButtonMinText
import app.unpeep.privacy.ui.theme.UnpeepType
import app.unpeep.privacy.ui.theme.Shapes
import app.unpeep.privacy.ui.theme.Size
import app.unpeep.privacy.ui.theme.Space
import app.unpeep.privacy.ui.theme.warningColors

/*
 * Controls (docs/COMPONENTS.md): one main action per screen, 52 dp pills, round tonal header buttons,
 * tags that always say the result in words, chips, and the pick-one row of sheets.
 */

/** Every button: a 52 dp pill whose label is one line that shrinks to fit, never wraps. */
@Composable
private fun UnpeepButton(text: String, onClick: () -> Unit, container: Color, content: Color, modifier: Modifier, icon: Painter?, enabled: Boolean, trailing: Painter? = null) {
    val c = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.focusRing(Shapes.pill).heightIn(min = Size.button),
        shape = Shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = c.onSurface.copy(alpha = Alpha.disabledContainer), disabledContentColor = c.onSurface.copy(alpha = Alpha.disabledContent),
        ),
        contentPadding = PaddingValues(horizontal = Space.card, vertical = Space.inline),
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(Size.buttonIcon))
            Spacer(Modifier.width(Space.inline))
        }
        BasicText(
            text, style = UnpeepType.button.copy(color = LocalContentColor.current, textAlign = TextAlign.Center),
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = ButtonMinText, maxFontSize = UnpeepType.button.fontSize),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (trailing != null) {
            Spacer(Modifier.width(Space.inline))
            Icon(trailing, null, modifier = Modifier.size(Size.buttonIcon))
        }
    }
}

/** The one main action of a screen or sheet. [trailing]: the arrow of an action that moves on ("Next: hotspots →"). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Painter? = null, enabled: Boolean = true, trailing: Painter? = null) =
    UnpeepButton(text, onClick, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, modifier, icon, enabled, trailing)

/** The main action when it can't be undone (Delete): the error red, no icon (12d, 16a). Only ever in a confirm sheet. */
@Composable
fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    UnpeepButton(text, onClick, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError, modifier, null, true)

/** The quiet action beside it (Cancel, Skip step, Found it?). */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Painter? = null, enabled: Boolean = true) =
    UnpeepButton(text, onClick, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, modifier, icon, enabled)

/** Secondary left, primary right, equal widths. At large text they stack, primary first. */
@Composable
fun ButtonPair(secondary: @Composable (Modifier) -> Unit, primary: @Composable (Modifier) -> Unit, modifier: Modifier = Modifier) {
    if (isLargeText()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.inline)) {
            primary(Modifier.fillMaxWidth())
            secondary(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.inline)) {
            secondary(Modifier.weight(1f))
            primary(Modifier.weight(1f))
        }
    }
}

/**
 * The docked action area at the bottom of a screen: 16 dp sides, 12 dp above, and [Space.dock] below (or the
 * system bar plus 12 dp, whichever is more).
 */
@Composable
fun Dock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier.reportDockHeight().fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .padding(start = Space.screen, end = Space.screen, top = Space.block, bottom = maxOf(Space.dock, nav + Space.block)),
        verticalArrangement = Arrangement.spacedBy(Space.inline),
        content = content,
    )
}

/** Round tonal header buttons: a 48 dp circle with a 24 dp icon and a TalkBack label. */
@Composable
fun RoundIconButton(icon: Painter, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false) {
    val c = MaterialTheme.colorScheme
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.focusRing(Shapes.pill).size(Size.iconButton),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            // One "on" look (00-decisions › Torch): primary, so it reads as on at a glance, even on the viewfinder.
            containerColor = if (active) c.primary else c.surfaceContainer,
            contentColor = if (active) c.onPrimary else c.onSurface,
        ),
    ) { Icon(icon, contentDescription = label, modifier = Modifier.size(Size.icon)) }
}

/** What a tag says. Red only for a likely camera; amber for worth a look; grey for known and plain facts; the accent for good news and progress ("Nothing found", "Stronger", "4 of 7"). */
enum class TagKind { LIKELY_CAMERA, WORTH_A_LOOK, KNOWN, GOOD }

/** A result in words, with its colour: a 26 dp pill. Never colour alone. */
@Composable
fun Tag(text: String, kind: TagKind, modifier: Modifier = Modifier, icon: Painter? = null) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = when (kind) {
        TagKind.LIKELY_CAMERA -> c.errorContainer to c.onErrorContainer
        TagKind.WORTH_A_LOOK -> warningColors().let { it.container to it.onContainer }
        TagKind.KNOWN -> c.surfaceContainerHigh to c.onSurfaceVariant
        TagKind.GOOD -> c.primaryContainer to c.onPrimaryContainer
    }
    Row(
        modifier.heightIn(min = Size.tag).clip(Shapes.pill).background(bg).padding(start = if (icon != null) Space.tagIcon else Space.tag, end = Space.tag, top = Space.small, bottom = Space.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(Size.iconSmall))
            Spacer(Modifier.width(Space.small))
        }
        Text(text, style = UnpeepType.caption, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A chip: place chips, filters, choices. Picked = secondaryContainer with a ✓; else outlined. 36 dp, 48 dp touch.
 * [fill]: the pill takes the width it is given (the lens finder's side panel, two by two), as Segmented's fill.
 */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, fill: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    Box(
        modifier
            .heightIn(min = Size.iconButton)
            .focusRing(Shapes.pill)
            .selectable(selected, role = Role.RadioButton, indication = null, interactionSource = null) {
                if (!selected) haptics.buzz(Buzz.TICK)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .then(if (fill) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = Size.chip)
                .clip(Shapes.pill)
                .then(if (selected) Modifier.background(c.secondaryContainer) else Modifier.border(Size.outline, c.outline, Shapes.pill))
                .padding(horizontal = Space.card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_check), null, tint = c.onSecondaryContainer, modifier = Modifier.size(Size.iconSmall))
                Spacer(Modifier.width(Space.inline))
            }
            Text(text, style = UnpeepType.label, color = if (selected) c.onSecondaryContainer else c.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
    }
}

/** The radio of [ChoiceRow]: a 24 dp ring, filled with a dot when picked. */
@Composable
private fun Radio(selected: Boolean) {
    val c = MaterialTheme.colorScheme
    Box(Modifier.size(Size.radio).border(Size.ring, if (selected) c.primary else c.onSurfaceVariant, Shapes.pill), contentAlignment = Alignment.Center) {
        if (selected) Box(Modifier.size(Size.radioDot).clip(Shapes.pill).background(c.primary))
    }
}

/**
 * A pick-one row in a sheet: circle, title, one line, the radio on the right. The picked row is tinted edge to edge.
 * With a circle it is 64 dp, 82 dp with a line that wraps (06, 09b). Without one it is a line of a long list (Language, Help numbers)
 * and sits closer: 52 dp, 56 dp with a line under the title (13c, 15).
 */
@Composable
fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, line: String? = null, leading: (@Composable () -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    Row(
        modifier
            .fillMaxWidth()
            .background(if (selected) c.surfaceContainer else Color.Transparent)
            .selectable(selected, role = Role.RadioButton) {
                if (!selected) haptics.buzz(Buzz.TICK)
                onClick()
            }
            .heightIn(min = when {
                leading != null -> if (line == null) Size.rowOneLine else Size.choiceRowCircle
                line == null -> Size.choiceRow
                else -> Size.choiceRowLines
            })
            .padding(horizontal = Space.heading, vertical = if (leading != null) Space.choiceCircle else Space.choice),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.card))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = UnpeepType.cardTitle, color = c.onSurface)
            if (line != null) Text(line, style = UnpeepType.supporting, color = c.onSurfaceVariant)
        }
        Spacer(Modifier.width(Space.card))
        Radio(selected)
    }
}

/** The one switch (settings rows, sweep steps). The whole row toggles: pass [onToggle] to the row, not here. */
@Composable
fun UnpeepSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Switch(
        checked = checked, onCheckedChange = null, modifier = modifier,
        colors = SwitchDefaults.colors(checkedTrackColor = c.primary, checkedThumbColor = c.onPrimary, uncheckedTrackColor = c.surfaceContainerHigh, uncheckedBorderColor = c.outline, uncheckedThumbColor = c.outline),
    )
}

/**
 * A title, a line and the switch, with no box of its own: the first line of a box that holds more (Appearance ›
 * Colours from wallpaper). The whole line toggles; its 48 dp target hangs a little over its words.
 */
@Composable
fun SwitchLine(title: String, line: String, checked: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    Row(
        modifier.fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                val height = (placeable.height - Size.switchRowOver.roundToPx()).coerceAtLeast(0)
                layout(placeable.width, height) { placeable.placeRelative(0, (height - placeable.height) / 2) }
            }
            .heightIn(min = Size.iconButton)
            .clip(Shapes.surface)
            .toggleable(checked, role = Role.Switch) {
                haptics.buzz(if (it) Buzz.TOGGLE_ON else Buzz.TOGGLE_OFF)
                onToggle(it)
            }
            .padding(horizontal = Space.card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = UnpeepType.cardTitle, color = c.onSurface)
            Text(line, style = UnpeepType.supporting, color = c.onSurfaceVariant)
        }
        Spacer(Modifier.width(Space.card))
        UnpeepSwitch(checked)
    }
}

/** A row with a switch: the whole row toggles. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = Shapes.surface, line: String? = null, leading: (@Composable () -> Unit)? = null) {
    val haptics = rememberHaptics()
    ListRow(
        title, modifier.clip(shape).toggleable(checked, role = Role.Switch) {
            haptics.buzz(if (it) Buzz.TOGGLE_ON else Buzz.TOGGLE_OFF)
            onToggle(it)
        },
        shape = shape, line = line, leading = leading, trailing = { UnpeepSwitch(checked) }, chevron = false,
    )
}

/** One choice of a [Segmented] picker: its words and an optional icon. */
@Immutable
data class Segment(
    val label: String,
    val icon: Painter? = null,
    /** The label's own style, for a sample ("Aa" at three sizes); null for the usual label. */
    val style: androidx.compose.ui.text.TextStyle? = null,
    /** What TalkBack says when the label is a sample ("Small"). */
    val description: String? = null,
)

/**
 * Two or three ways of doing one thing, side by side in one pill (Lens · Infrared): the picked one is a filled pill.
 * Each is 44 dp tall inside a 52 dp pill (the whole height is its target) and reads as a radio button.
 */
@Composable
fun Segmented(segments: List<Segment>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, fill: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    // A fixed pill of two or three: its words stop growing at 130 %, as the nav bar's do (00-decisions).
    CappedFontScale {
        Row(
            // Filling a box (14) the segments touch, on the box's lighter track; floating over the viewfinder they sit 4 dp apart.
            modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier).clip(Shapes.pill).background(if (fill) c.surfaceContainer else c.surfaceContainerHigh)
                .padding(horizontal = Space.small).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(if (fill) Size.none else Space.small),
        ) {
            segments.forEachIndexed { i, s ->
                val picked = i == selected
                Row(
                    Modifier
                        // The target is the pill's full 52 dp; the picked colour is the 44 dp inside it.
                        .clip(Shapes.pill)
                        .selectable(picked, role = Role.RadioButton) {
                            if (!picked) haptics.buzz(Buzz.TICK)
                            onSelect(i)
                        }
                        .then(if (s.description != null) Modifier.semantics { contentDescription = s.description } else Modifier)
                        .padding(vertical = Space.small)
                        .heightIn(min = Size.segment)
                        .focusRing(Shapes.pill)
                        .clip(Shapes.pill)
                        .background(if (picked) c.onSurface else Color.Transparent)
                        .padding(horizontal = if (isLargeText()) Space.block else if (fill) Space.card else Space.heading)
                        // Filling the width (Appearance), each segment takes an equal share; otherwise each hugs its
                        // label (a weight, even unfilled, would cap every segment at an equal share and cut "Infrared").
                        .then(if (fill) Modifier.weight(1f) else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (s.icon != null) {
                        Icon(s.icon, null, tint = if (picked) c.surface else c.onSurface, modifier = Modifier.size(Size.buttonIcon))
                        Spacer(Modifier.width(Space.inline))
                    }
                    val style = s.style ?: UnpeepType.label
                    // One size for every segment (never one shrunk beside the others); "…" only if a translation can't fit.
                    Text(s.label, style = style, color = if (picked) c.surface else c.onSurface, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * An accent to pick (Appearance): a 48 dp circle of the colour; the picked one has a ring around it and a ✓.
 * [label] is what TalkBack says ("Dark blue").
 */
@Composable
fun ColorSwatch(color: Color, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    // 44 dp, the same picked or not; the picked one's ring is drawn 2 dp outside it, over the gap to its neighbours.
    val ring = c.onSurface
    // Its target is 48 dp, hanging 2 dp over each side of the 44 dp it takes in the row.
    Box(
        modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val side = Size.swatch.roundToPx()
                layout(side, side) { placeable.placeRelative((side - placeable.width) / 2, (side - placeable.height) / 2) }
            }
            .size(Size.iconButton)
            .focusRing(Shapes.pill)
            .clip(Shapes.pill)
            .selectable(selected, role = Role.RadioButton) { if (!selected) haptics.buzz(Buzz.TICK); onClick() }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(Size.swatch)
                .drawBehind {
                    if (selected) {
                        val line = Size.ring.toPx()
                        drawCircle(ring, size.minDimension / 2 + Size.focusGap.toPx() + line / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(line))
                    }
                }
                .clip(Shapes.pill).background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(painterResource(R.drawable.ic_check), null, tint = c.surface, modifier = Modifier.size(Size.swatchCheck))
        }
    }
}

/**
 * A choice of a few steps on one scale (Appearance › Text size, as Weft): [title] and the [value] now on one line,
 * a slider under it that stops at each of [count] steps. A tick on each step; TalkBack reads the value.
 */
@Composable
fun StepSlider(
    title: String, value: String, index: Int, count: Int, onStep: (Int) -> Unit, modifier: Modifier = Modifier,
    /** The name of each step, under the slider; the picked one in medium weight (14a). */
    stepNames: List<String> = emptyList(),
    /** One quiet line under it ("On top of your phone's font size"). */
    caption: String? = null,
    /** A small and a large letter at the ends, for a size scale. */
    sizeEnds: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    Column(modifier.fillMaxWidth().clip(Shapes.surface).background(c.surfaceContainerLow).padding(horizontal = Space.card, vertical = Space.block)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = UnpeepType.cardTitle, color = c.onSurface, modifier = Modifier.weight(1f).semantics { heading() })
            Text(value, style = UnpeepType.supporting, color = c.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (sizeEnds) Text("A", style = UnpeepType.caption, color = c.onSurfaceVariant, modifier = Modifier.clearAndSetSemantics { }.padding(end = Space.inline))
        androidx.compose.material3.Slider(
            value = index.toFloat(),
            onValueChange = { v ->
                val i = v.roundToInt().coerceIn(0, count - 1)
                if (i != index) {
                    haptics.buzz(Buzz.TICK)
                    onStep(i)
                }
            },
            valueRange = 0f..(count - 1).toFloat(),
            steps = (count - 2).coerceAtLeast(0),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceContainerHighest,
                activeTickColor = c.onPrimary, inactiveTickColor = c.onSurfaceVariant,
            ),
            modifier = Modifier.weight(1f).semantics { stateDescription = value },
        )
            if (sizeEnds) Text("A", style = UnpeepType.title, color = c.onSurfaceVariant, modifier = Modifier.clearAndSetSemantics { }.padding(start = Space.inline))
        }
        if (stepNames.isNotEmpty()) {
            // Each name sits under its own stop; the picked one is medium weight.
            Row(Modifier.fillMaxWidth().padding(horizontal = if (sizeEnds) Space.heading else Size.none).clearAndSetSemantics { }) {
                stepNames.forEachIndexed { i, n ->
                    Text(
                        n, style = if (i == index) UnpeepType.caption else UnpeepType.small,
                        color = if (i == index) c.onSurface else c.onSurfaceVariant, maxLines = 1,
                        textAlign = when (i) { 0 -> TextAlign.Start; stepNames.lastIndex -> TextAlign.End; else -> TextAlign.Center },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (caption != null) {
            Spacer(Modifier.height(Space.inline))
            Text(caption, style = UnpeepType.supporting, color = c.onSurfaceVariant)
        }
    }
}
