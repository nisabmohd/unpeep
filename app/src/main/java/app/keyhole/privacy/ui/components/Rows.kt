package app.keyhole.privacy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.checkColors
import app.keyhole.privacy.ui.theme.warningColors

/*
 * Lists and rows (docs/COMPONENTS.md). A group = rows 2 dp apart, 24 dp outer corners, 8 dp between rows.
 * Never a divider line. No buttons inside rows: tap the row.
 */

/** How an icon circle is coloured: a fixed hue, or one of the result colours. Colour is never on text. */
sealed interface CircleTone {
    data class Hue(val hue: Float) : CircleTone
    data object Neutral : CircleTone
    data object Accent : CircleTone
    data object Alert : CircleTone
    data object Warning : CircleTone
}

@Composable
fun CircleTone.colors(): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (this) {
        is CircleTone.Hue -> checkColors(hue).let { it.container to it.onContainer }
        CircleTone.Neutral -> c.surfaceContainerHigh to c.onSurfaceVariant
        CircleTone.Accent -> c.primaryContainer to c.onPrimaryContainer
        CircleTone.Alert -> c.errorContainer to c.onErrorContainer
        CircleTone.Warning -> warningColors().let { it.container to it.onContainer }
    }
}

/** The 44 dp circle with a 22 dp icon. */
@Composable
fun IconCircle(icon: Painter, tone: CircleTone, modifier: Modifier = Modifier) {
    val (bg, fg) = tone.colors()
    Box(modifier.size(Size.iconCircle).clip(Shapes.pill).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(Size.iconInCircle))
    }
}

/** A neutral circle: rows that aren't about one check or one result. */
@Composable
fun NeutralCircle(icon: Painter, modifier: Modifier = Modifier) = IconCircle(icon, CircleTone.Neutral, modifier)

/** Collects the rows of a [Grouped]; each gets the shape for its place in the group. */
class GroupedScope internal constructor() {
    internal val rows = mutableListOf<@Composable (Shape) -> Unit>()
    fun row(content: @Composable (Shape) -> Unit) { rows += content }
}

/** A group of rows: 2 dp apart, 24 dp corners outside, 8 dp between rows. In a lazy list use [Shapes.grouped]. */
@Composable
fun Grouped(modifier: Modifier = Modifier, content: GroupedScope.() -> Unit) {
    val rows = GroupedScope().apply(content).rows
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.row)) {
        rows.forEachIndexed { i, row -> row(Shapes.grouped(i, rows.size)) }
    }
}

/**
 * The one list row: 72 dp (60 dp with no second line), a leading circle, title + one line, a trailing slot
 * (a tag, a value) and a chevron when it opens something.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.surface,
    line: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    chevron: Boolean = onClick != null && trailing == null,
    onLongClick: (() -> Unit)? = null,
    longClickLabel: String? = null,
    /** A destructive row (Delete all reports): its title in the error colour. */
    alert: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceContainerLow)
            .then(
                when {
                    onLongClick != null -> Modifier.combinedClickable(role = Role.Button, onLongClickLabel = longClickLabel, onLongClick = onLongClick, onClick = onClick ?: {})
                    onClick != null -> Modifier.clickable(role = Role.Button, onClick = onClick)
                    else -> Modifier
                },
            )
            .heightIn(min = if (line == null) Size.rowOneLine else Size.row)
            .padding(horizontal = Space.card, vertical = Space.block),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.card))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = KeyholeType.cardTitle, color = if (alert) c.error else c.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (line != null) Text(line, style = KeyholeType.supporting, color = c.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.inline))
            trailing()
        }
        if (chevron) {
            Spacer(Modifier.width(Space.inline))
            Icon(painterResource(R.drawable.ic_chevron), null, tint = c.onSurfaceVariant, modifier = Modifier.size(Size.chevron))
        }
    }
}

/** A detail row: a muted label on the left, its value on the right ("Open ports · 80 · 554 · 8899"). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ValueRow(label: String, value: String, modifier: Modifier = Modifier, shape: Shape = Shapes.surface) {
    val c = MaterialTheme.colorScheme
    FlowRow(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceContainerLow)
            .heightIn(min = Size.rowOneLine)
            .padding(horizontal = Space.card, vertical = Space.block)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = KeyholeType.body, color = c.onSurfaceVariant, modifier = Modifier.padding(end = Space.card))
        Text(value, style = KeyholeType.cardTitle, color = c.onSurface, textAlign = TextAlign.End)
    }
}

/** A reason or a step: a 24 dp icon with no circle and one sentence ("Answers as a network camera"). */
@Composable
fun SignRow(icon: Painter, text: String, modifier: Modifier = Modifier, shape: Shape = Shapes.surface, alert: Boolean = false) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(shape).background(c.surfaceContainerLow).heightIn(min = Size.rowOneLine).padding(horizontal = Space.card, vertical = Space.block),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (alert) c.error else c.onSurfaceVariant, modifier = Modifier.size(Size.lineIcon))
        Spacer(Modifier.width(Space.card))
        Text(text, style = KeyholeType.body, color = c.onSurface, modifier = Modifier.weight(1f))
    }
}

/** The label above a group: sentence case, 24 dp from the screen edge, an optional count on the right. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, meta: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(start = Space.headingInset, end = Space.headingInset, top = Space.sectionTop, bottom = Space.sectionBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = KeyholeType.label, color = c.onSurfaceVariant, modifier = Modifier.weight(1f).semantics { heading() })
        if (meta != null) Text(meta, style = KeyholeType.supporting, color = c.onSurfaceVariant)
        // A text action ("Change"): the accent colour, 48 dp to tap, the one way a section label acts.
        if (action != null) TopBarAction(action, onAction)
    }
}

/** A tinted box that is not a list ("Or run one check", the Find it meter): 24 dp corners, 16 dp inside. */
@Composable
fun Box24(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().clip(Shapes.surface).background(c.surfaceContainerLow).padding(vertical = Space.card)) {
        if (title != null) {
            Text(title, style = KeyholeType.cardTitle, color = c.onSurface, modifier = Modifier.padding(horizontal = Space.card).semantics { heading() })
            Spacer(Modifier.heightIn(min = Space.block))
        }
        content()
    }
}

/** The one note. Notes go first on a screen. [warning] for "Worth a look" news (client isolation), else neutral. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier, title: String? = null, icon: Painter = painterResource(R.drawable.ic_info), warning: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val w = warningColors()
    val bg = if (warning) w.container else c.secondaryContainer
    val fg = if (warning) w.onContainer else c.onSecondaryContainer
    Row(modifier.fillMaxWidth().clip(Shapes.surface).background(bg).padding(Space.card).semantics(mergeDescendants = true) {}) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(Size.icon))
        Spacer(Modifier.width(Space.block))
        Column(Modifier.weight(1f)) {
            if (title != null) Text(title, style = KeyholeType.cardTitle, color = fg)
            Text(text, style = KeyholeType.supporting, color = fg)
        }
    }
}

/**
 * A More tile: a circle top left, a small value top right ("System", "7 on"), title and one line below.
 * Tap the tile; it never holds a button.
 */
@Composable
fun Tile(icon: Painter, tone: CircleTone, title: String, line: String, onClick: () -> Unit, modifier: Modifier = Modifier, value: String? = null) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier.clip(Shapes.surface).background(c.surfaceContainerLow).clickable(role = Role.Button, onClick = onClick).heightIn(min = Size.tile).padding(Space.card),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            IconCircle(icon, tone)
            Spacer(Modifier.weight(1f))
            if (value != null) Text(value, style = KeyholeType.caption, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.heightIn(min = Space.card).weight(1f, fill = false))
        Spacer(Modifier.size(Space.card))
        Text(title, style = KeyholeType.cardTitle, color = c.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(line, style = KeyholeType.supporting, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** What one part of a [SummaryBox] counts: a likely camera (red), worth a look (amber), or everything else. */
enum class SummaryTone { ALERT, WARNING, NEUTRAL }

@Immutable
data class SummaryPart(val count: Int, val label: String, val tone: SummaryTone)

/**
 * The count at the top of a result list: one sentence whose number is drawn big ("18 devices on SeaView_Guest";
 * the sentence comes whole from strings.xml, so each language puts the number where it belongs), one bar split by
 * [parts], and what each colour means in words underneath. Never colour alone. [count] is the number as it appears
 * in [text].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryBox(text: String, count: String, parts: List<SummaryPart>, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val warning = warningColors().solid
    fun colour(tone: SummaryTone) = when (tone) {
        SummaryTone.ALERT -> c.error
        SummaryTone.WARNING -> warning
        SummaryTone.NEUTRAL -> c.surfaceContainerHighest
    }
    val big = KeyholeType.display.toSpanStyle().copy(color = c.onSurface)
    val sentence = remember(text, count, big) {
        buildAnnotatedString {
            append(text)
            val at = text.indexOf(count)
            if (count.isNotEmpty() && at >= 0) addStyle(big, at, at + count.length)
        }
    }
    Column(
        modifier.fillMaxWidth().clip(Shapes.surface).background(c.surfaceContainerLow).padding(Space.card).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Space.block),
    ) {
        Text(sentence, style = KeyholeType.body, color = c.onSurfaceVariant)
        // One segment per colour: the grey ones (known, not recognised) are one stretch of the bar.
        val shown = parts.filter { it.count > 0 }.groupBy { it.tone }.map { (tone, same) -> SummaryPart(same.sumOf { it.count }, "", tone) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.small)) {
            if (shown.isEmpty()) Box(Modifier.weight(1f).height(Size.dot).clip(Shapes.pill).background(colour(SummaryTone.NEUTRAL)))
            shown.forEach { Box(Modifier.weight(it.count.toFloat()).height(Size.dot).clip(Shapes.pill).background(colour(it.tone))) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.card), verticalArrangement = Arrangement.spacedBy(Space.small)) {
            parts.forEach { part ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(Size.dot).clip(Shapes.pill).background(colour(part.tone)))
                    Spacer(Modifier.width(Space.inline))
                    Text(part.label, style = KeyholeType.supporting, color = c.onSurfaceVariant)
                }
            }
        }
    }
}

/** How a checklist item was answered, as its mark: ✓ looks fine, ? not sure, − skipped, an empty ring when not yet. */
enum class AnswerMark { NONE, FINE, NOT_SURE, SKIPPED }

/** The 40 dp mark at the end of a checklist row. */
@Composable
fun AnswerMarkCircle(mark: AnswerMark, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val w = warningColors()
    val box = modifier.size(Size.mark).clip(Shapes.pill)
    when (mark) {
        AnswerMark.NONE -> Box(modifier.size(Size.mark).border(Size.ring, c.outline, Shapes.pill))
        AnswerMark.FINE -> Box(box.background(c.primary), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_check), null, tint = c.onPrimary, modifier = Modifier.size(Size.buttonIcon))
        }
        AnswerMark.NOT_SURE -> Box(box.background(w.container), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_help), null, tint = w.onContainer, modifier = Modifier.size(Size.buttonIcon))
        }
        AnswerMark.SKIPPED -> Box(box.background(c.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_skip), null, tint = c.onSurfaceVariant, modifier = Modifier.size(Size.buttonIcon))
        }
    }
}

/**
 * One thing to look at (checklist): its hued circle, title, one line, and how it was answered. Tap the row to
 * answer; [state] is what TalkBack says about the answer ("Looks fine").
 */
@Composable
fun ChecklistRow(icon: Painter, hue: Float, title: String, line: String, mark: AnswerMark, state: String?, onClick: () -> Unit, modifier: Modifier = Modifier, shape: Shape = Shapes.surface) {
    ListRow(
        title, modifier.semantics { if (state != null) stateDescription = state }, shape, line = line,
        leading = { IconCircle(icon, CircleTone.Hue(hue)) }, trailing = { AnswerMarkCircle(mark) }, onClick = onClick, chevron = false,
    )
}

/** A step of a numbered list ("1 Don't touch or unplug it"): the number in an accent circle. */
@Composable
fun NumberCircle(number: Int, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Box(modifier.size(Size.iconCircle).clip(Shapes.pill).background(c.primaryContainer), contentAlignment = Alignment.Center) {
        Text(number.toString(), style = KeyholeType.cardTitle, color = c.onPrimaryContainer)
    }
}

/**
 * A floating card of Welcome's picture ("IPC-3F2A · Likely camera"): a white card with a circle, a title and a line,
 * tilted by [tilt] degrees. Part of a picture, so TalkBack skips it.
 */
@Composable
fun FloatingCard(icon: Painter, tone: CircleTone, title: String, line: String, tilt: Float, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.widthIn(max = Size.floatCard).graphicsLayer { rotationZ = tilt; shadowElevation = Size.cardShadow.toPx(); shape = Shapes.surface; clip = true }
            .background(c.surface).padding(Space.block).clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(icon, tone)
        Spacer(Modifier.width(Space.block))
        Column {
            Text(title, style = KeyholeType.cardTitle, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = KeyholeType.supporting, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
