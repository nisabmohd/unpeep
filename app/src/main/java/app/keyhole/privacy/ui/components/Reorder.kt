package app.keyhole.privacy.ui.components

import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import kotlin.math.roundToInt

/** One row of a [ReorderList]: its icon (in a circle of [tone]), title, and whether it is on. */
data class ReorderItem(val key: String, val title: String, val icon: Painter, val tone: CircleTone, val on: Boolean)

/**
 * A list to reorder and switch (Sweep steps): each row is the dotted grip, a circle, the title and the switch. Drag
 * a row by its grip to move it; tap the row to switch it. TalkBack: "Move up" / "Move down" on each row, labelled
 * with [moveUp] / [moveDown]. One look, like every grouped list: 2 dp apart, 24 / 8 dp corners.
 */
@Composable
fun ReorderList(items: List<ReorderItem>, onMove: (from: Int, to: Int) -> Unit, onToggle: (Int, Boolean) -> Unit, moveUp: String, moveDown: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val move by rememberUpdatedState(onMove)
    var dragging by remember { mutableIntStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.row)) {
        items.forEachIndexed { i, item ->
            val lifted = i == dragging
            // Lifted, the row is a card of its own: fully rounded, its shadow the same shape (no square edge showing
            // around the corners), and drawn above the rows it passes.
            val shape = if (lifted) Shapes.surface else Shapes.grouped(i, items.size)
            Row(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { if (rowHeight == 0) rowHeight = it.height }
                    .then(
                        if (lifted) {
                            Modifier.zIndex(1f).offset { IntOffset(0, offset.roundToInt()) }
                                .graphicsLayer { shadowElevation = Size.cardShadow.toPx(); this.shape = shape; clip = true }
                        } else Modifier,
                    )
                    .clip(shape)
                    .background(if (lifted) c.surfaceContainer else c.surfaceContainerLow)
                    .toggleable(item.on, role = Role.Switch) {
                        haptics.buzz(if (it) Buzz.TOGGLE_ON else Buzz.TOGGLE_OFF)
                        onToggle(i, it)
                    }
                    .semantics {
                        customActions = listOfNotNull(
                            if (i > 0) CustomAccessibilityAction(moveUp) { move(i, i - 1); true } else null,
                            if (i < items.size - 1) CustomAccessibilityAction(moveDown) { move(i, i + 1); true } else null,
                        )
                    }
                    .heightIn(min = Size.rowOneLine)
                    .padding(end = Space.card, top = Space.small, bottom = Space.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The grip: a 48 dp handle; dragging it moves the row, one slot per row height passed.
                Icon(
                    painterResource(R.drawable.ic_drag), null, tint = c.onSurfaceVariant,
                    modifier = Modifier.size(Size.iconButton).padding(Space.block).pointerInput(items.size) {
                        detectDragGestures(
                            onDragStart = { dragging = i; offset = 0f; haptics.buzz(Buzz.TICK) },
                            onDragEnd = { dragging = -1; offset = 0f },
                            onDragCancel = { dragging = -1; offset = 0f },
                        ) { change, amount ->
                            change.consume()
                            offset += amount.y
                            val step = rowHeight.coerceAtLeast(1)
                            val at = dragging
                            if (offset > step / 2 && at < items.size - 1) {
                                move(at, at + 1); dragging = at + 1; offset -= step; haptics.buzz(Buzz.TICK)
                            } else if (offset < -step / 2 && at > 0) {
                                move(at, at - 1); dragging = at - 1; offset += step; haptics.buzz(Buzz.TICK)
                            }
                        }
                    },
                )
                IconCircle(item.icon, item.tone)
                Spacer(Modifier.width(Space.card))
                Text(item.title, style = KeyholeType.cardTitle, color = c.onSurface, modifier = Modifier.weight(1f))
                KeyholeSwitch(item.on)
            }
        }
    }
}
