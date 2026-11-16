package app.keyhole.privacy.ui.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space

/** The share of the screen a sheet may take; the rest stays visible above it. */
private const val MAX_SHEET_FRACTION = 0.9f

/**
 * Every choice, picker and confirm in the app (no dialogs): opens fully, sits on the bottom edge, and leaves the
 * top 10% of the screen visible however much it holds. Handle, [title] on the left with one muted [line], a round
 * close button; long content scrolls inside. The same sheet as Weft and Burrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyholeSheet(title: String, onDismiss: () -> Unit, line: String? = null, footer: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val screen = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val navBar = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background,
        shape = Shapes.sheet,
        dragHandle = { Box(Modifier.padding(top = Space.block).size(width = Size.handle, height = Space.grid).clip(Shapes.pill).background(c.outline)) },
    ) {
        // heightIn keeps within the room the sheet is given too, so the smaller of the two wins.
        val max = screen * MAX_SHEET_FRACTION - Size.handleArea - navBar
        Box {
            // The sheet takes focus itself, so opening one never pops up the keyboard.
            // imePadding: a sheet with a text field (Rename) lifts above the keyboard.
            Column(Modifier.heightIn(max = max).imePadding().nestedScroll(remember { ScrollStaysInside() }).focusable()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = Space.heading, end = Space.screen, top = Space.block, bottom = Space.card),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f).padding(top = Space.inline, end = Space.card)) {
                        Text(title, style = KeyholeType.title, color = c.onSurface, modifier = Modifier.semantics { heading() })
                        if (line != null) Text(line, style = KeyholeType.supporting, color = c.onSurfaceVariant, modifier = Modifier.padding(top = Space.small))
                    }
                    RoundIconButton(painterResource(R.drawable.ic_close), stringResource(R.string.close), onDismiss)
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
                if (footer != null) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = Space.screen, end = Space.screen, top = Space.block, bottom = Space.block),
                        verticalArrangement = Arrangement.spacedBy(Space.inline), content = footer,
                    )
                }
            }
        }
    }
}

/**
 * The sheet scroll rule (from Weft): a scroll or fling that moved the sheet's content stops when the content
 * reaches its top or end, so it never drags or closes the sheet. A new drag that starts with the content already
 * at its top still moves the sheet, so it can be pulled down on purpose.
 */
private class ScrollStaysInside : NestedScrollConnection {
    private var scrolledContent = false

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (consumed.y != 0f) scrolledContent = true
        return when {
            source == NestedScrollSource.SideEffect -> available
            scrolledContent -> available
            else -> Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val keep = scrolledContent || consumed.y != 0f
        scrolledContent = false
        return if (keep) available else Velocity.Zero
    }
}

/**
 * A quiet line inside a sheet, under its rows ("Keyhole got it wrong? Report it…"); tappable when [onClick] is set.
 * [leaves]: it opens something outside Keyhole (the browser), shown by ↗ at the end (06).
 */
@Composable
fun SheetLine(text: String, modifier: Modifier = Modifier, icon: Painter? = null, onClick: (() -> Unit)? = null, leaves: Boolean = false) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(horizontal = Space.inline)
            .then(if (onClick != null) Modifier.focusRing(Shapes.surface, inside = true) else Modifier)
            .clip(Shapes.surface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = Size.iconButton).padding(horizontal = Space.card, vertical = Space.inline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.onSurfaceVariant, modifier = Modifier.size(Size.chevron))
            Spacer(Modifier.width(Space.block))
        }
        Text(text, style = KeyholeType.supporting, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (leaves) {
            Spacer(Modifier.width(Space.block))
            Icon(painterResource(R.drawable.ic_open), null, tint = c.onSurfaceVariant, modifier = Modifier.size(Size.chevron))
        }
    }
}
