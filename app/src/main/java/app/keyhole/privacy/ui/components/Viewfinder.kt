package app.keyhole.privacy.ui.components

import app.keyhole.privacy.ui.theme.Alpha
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import app.keyhole.privacy.optics.cropScale
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.Viewfinder

/*
 * The viewfinder (docs/COMPONENTS.md): the camera picture, the rings around possible lenses, and the shutter.
 * Always drawn in ViewfinderTheme (dark, whatever the app's theme).
 */

/**
 * The camera picture: a TextureView that fills its space with no bars (centre crop). [contentWidth] × [contentHeight]
 * is the picture's upright size; the view is made that shape, large enough to cover, and the overflow is clipped
 * (a stretch transform can escape the clip once a filter paint is set). [onTexture] gets the view when its surface
 * is ready and null when it goes away.
 */
@Composable
fun CameraPreview(contentWidth: Int, contentHeight: Int, onTexture: (TextureView?) -> Unit, modifier: Modifier = Modifier, description: String? = null) {
    val listener = rememberUpdatedState(onTexture)
    Box(
        modifier.fillMaxSize().clipToBounds().then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) = listener.value(this@apply)
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            listener.value(null)
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                    }
                }
            },
            // As large as it must be to cover the box in the picture's shape, centred; the box clips the rest.
            modifier = Modifier.layout { measurable, room ->
                val (sx, sy) = cropScale(contentWidth.toFloat(), contentHeight.toFloat(), room.maxWidth.toFloat(), room.maxHeight.toFloat())
                val w = (room.maxWidth * sx).roundToInt()
                val h = (room.maxHeight * sy).roundToInt()
                val view = measurable.measure(Constraints.fixed(w, h))
                layout(room.maxWidth, room.maxHeight) { view.place((room.maxWidth - w) / 2, (room.maxHeight - h) / 2) }
            },
        )
    }
}

/** A possible lens on screen: its centre as a share of the view (0..1). */
@Immutable
data class SpotMark(val x: Float, val y: Float)

/**
 * Rings around possible lenses: a ring for each, and [label] ("Look here") beside the first. Marks off the view are
 * not drawn. [description] is what TalkBack says for them all ("2 possible lenses ringed").
 */
@Composable
fun SpotRings(marks: List<SpotMark>, label: String, description: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var room by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier.fillMaxSize().onSizeChanged { room = it }.clearAndSetSemantics { if (marks.isNotEmpty()) contentDescription = description }) {
        val w = with(density) { room.width.toDp() }
        val h = with(density) { room.height.toDp() }
        marks.filter { it.x in 0f..1f && it.y in 0f..1f }.forEachIndexed { i, m ->
            val cx = w * m.x
            val cy = h * m.y
            // A soft glow, then the ring (08): the eye finds it at a glance on a dark picture.
            Box(Modifier.offset(cx - Size.glintGlow / 2, cy - Size.glintGlow / 2).size(Size.glintGlow).clip(Shapes.pill).background(c.primary.copy(alpha = Alpha.glow)))
            Box(Modifier.offset(cx - Size.glintRing / 2, cy - Size.glintRing / 2).size(Size.glintRing).border(Size.ring, c.primary, Shapes.pill))
            Box(Modifier.offset(cx - Size.dot / 2, cy - Size.dot / 2).size(Size.dot).clip(Shapes.pill).background(Viewfinder.content))
            if (i == 0) {
                Text(
                    label, style = KeyholeType.label, color = c.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(cx + Size.glintRing / 2, cy - Size.glintRing).clip(Shapes.pill).background(c.primary).padding(horizontal = Space.block, vertical = Space.small),
                )
            }
        }
    }
}

/** The shutter: a ring with the accent circle and a camera icon inside; [label] for TalkBack. */
@Composable
fun Shutter(icon: Painter, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier.size(Size.shutter).focusRing(Shapes.pill).border(Size.ring, Viewfinder.content, Shapes.pill).clip(Shapes.pill)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) {
                onClick()
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(Size.shutterInner).clip(Shapes.pill).background(if (enabled) c.primary else c.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (enabled) c.onPrimary else c.onSurfaceVariant, modifier = Modifier.size(Size.icon))
        }
    }
}
