package app.unpeep.privacy.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import app.unpeep.privacy.ui.theme.Size

/**
 * The one keyboard-focus look (00-focus-states): a 3 dp primary ring. Small shapes (buttons, chips, circles) get
 * it outside with a 2 dp gap; rows and tiles ([inside]) get it drawn inside their edge. Put it before `clip` and
 * the clickable. Touch never shows it; only a keyboard, D-pad or switch access moves focus.
 */
fun Modifier.focusRing(shape: Shape, inside: Boolean = false): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.primary
    this.onFocusChanged { focused = it.hasFocus }.drawWithContent {
        drawContent()
        if (focused) {
            val w = Size.focusRing.toPx()
            val grow = if (inside) -w / 2 else Size.focusGap.toPx() + w / 2
            val box = GeoSize(size.width + grow * 2, size.height + grow * 2)
            translate(-grow, -grow) {
                drawOutline(shape.createOutline(box, layoutDirection, this), color, style = Stroke(w))
            }
        }
    }
}
