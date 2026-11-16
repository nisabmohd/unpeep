package app.keyhole.privacy.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.theme.Alpha
import app.keyhole.privacy.ui.theme.ButtonMinText
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.warningColors

/*
 * Controls (docs/COMPONENTS.md): one main action per screen, 52 dp pills, round tonal header buttons,
 * tags that always say the result in words, chips, and the pick-one row of sheets.
 */

/** Every button: a 52 dp pill whose label is one line that shrinks to fit, never wraps. */
@Composable
private fun KeyholeButton(text: String, onClick: () -> Unit, container: Color, content: Color, modifier: Modifier, icon: Painter?, enabled: Boolean, trailing: Painter? = null) {
    val c = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Size.button),
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
            text, style = KeyholeType.cardTitle.copy(color = LocalContentColor.current, textAlign = TextAlign.Center),
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = ButtonMinText, maxFontSize = KeyholeType.cardTitle.fontSize),
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
    KeyholeButton(text, onClick, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, modifier, icon, enabled, trailing)

/** The quiet action beside it (Cancel, Skip step, Found it?). */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Painter? = null, enabled: Boolean = true) =
    KeyholeButton(text, onClick, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, modifier, icon, enabled)

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
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
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
        modifier = modifier.size(Size.iconButton),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (active) c.primaryContainer else c.surfaceContainer,
            contentColor = if (active) c.onPrimaryContainer else c.onSurface,
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
        modifier.heightIn(min = Size.tag).clip(Shapes.pill).background(bg).padding(horizontal = Space.block, vertical = Space.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(Size.iconSmall))
            Spacer(Modifier.width(Space.small))
        }
        Text(text, style = KeyholeType.caption, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A chip: place chips, filters, choices. Picked = secondaryContainer with a ✓; else outlined. 36 dp, 48 dp touch. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .heightIn(min = Size.iconButton)
            .selectable(selected, role = Role.RadioButton, indication = null, interactionSource = null) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .heightIn(min = Size.chip)
                .clip(Shapes.pill)
                .then(if (selected) Modifier.background(c.secondaryContainer) else Modifier.border(Size.outline, c.outline, Shapes.pill))
                .padding(horizontal = Space.card),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_check), null, tint = c.onSecondaryContainer, modifier = Modifier.size(Size.iconSmall))
                Spacer(Modifier.width(Space.inline))
            }
            Text(text, style = KeyholeType.label, color = if (selected) c.onSecondaryContainer else c.onSurfaceVariant, maxLines = 1, softWrap = false)
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

/** A pick-one row in a sheet: circle, title, one line, the radio on the right. The picked row is tinted edge to edge. */
@Composable
fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, line: String? = null, leading: (@Composable () -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .background(if (selected) c.surfaceContainer else Color.Transparent)
            .selectable(selected, role = Role.RadioButton) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .heightIn(min = if (line == null) Size.rowOneLine else Size.row)
            .padding(horizontal = Space.heading, vertical = Space.block),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.card))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = KeyholeType.cardTitle, color = c.onSurface)
            if (line != null) Text(line, style = KeyholeType.supporting, color = c.onSurfaceVariant)
        }
        Spacer(Modifier.width(Space.card))
        Radio(selected)
    }
}

/** The one switch (settings rows, sweep steps). The whole row toggles: pass [onToggle] to the row, not here. */
@Composable
fun KeyholeSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Switch(
        checked = checked, onCheckedChange = null, modifier = modifier,
        colors = SwitchDefaults.colors(checkedTrackColor = c.primary, checkedThumbColor = c.onPrimary, uncheckedTrackColor = c.surfaceContainerHigh, uncheckedBorderColor = c.outline, uncheckedThumbColor = c.outline),
    )
}

/** A row with a switch: the whole row toggles. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = Shapes.surface, line: String? = null, leading: (@Composable () -> Unit)? = null) {
    val haptics = LocalHapticFeedback.current
    ListRow(
        title, modifier.clip(shape).toggleable(checked, role = Role.Switch) {
            haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onToggle(it)
        },
        shape = shape, line = line, leading = leading, trailing = { KeyholeSwitch(checked) }, chevron = false,
    )
}
