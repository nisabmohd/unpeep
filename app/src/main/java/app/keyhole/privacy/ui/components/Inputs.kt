package app.keyhole.privacy.ui.components

import app.keyhole.privacy.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.drawBehind
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.platform.LocalDensity
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Inputs (docs/COMPONENTS.md): the one text field, and the one photo thumbnail.
 */

/**
 * The one text field: a 24 dp tinted pill (search) or box (notes, [singleLine] false), an optional leading icon, the
 * hint in the muted colour, [label] for TalkBack. The text grows with the font size.
 */
@Composable
fun KeyholeTextField(
    value: String, onValueChange: (String) -> Unit, hint: String, label: String, modifier: Modifier = Modifier,
    icon: Painter? = null, singleLine: Boolean = true, maxLength: Int = MAX_TEXT, imeAction: ImeAction = ImeAction.Done,
) {
    val c = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else NOTE_LINES,
        textStyle = KeyholeType.body.copy(color = c.onSurface),
        cursorBrush = SolidColor(c.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { field ->
            Row(
                Modifier.fillMaxWidth().clip(if (singleLine) Shapes.pill else Shapes.surface).background(c.surfaceContainer)
                    .heightIn(min = if (singleLine) Size.fieldLine else Size.field).padding(horizontal = Space.card, vertical = if (singleLine) Space.inline else Space.block),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = c.onSurfaceVariant, modifier = Modifier.size(Size.icon))
                    Spacer(Modifier.width(Space.block))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(hint, style = KeyholeType.body, color = c.onSurfaceVariant)
                    field()
                }
            }
        },
    )
}

/**
 * A proof photo, as a [size] square with 16 dp corners. The file is read off the main thread, scaled down to the
 * thumbnail; [onClick] opens it.
 */
@Composable
fun PhotoThumb(path: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = Size.photo) {
    val c = MaterialTheme.colorScheme
    val px = with(LocalDensity.current) { size.roundToPx() }
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path, px) { bitmap = withContext(Dispatchers.IO) { decodeScaled(path, px)?.asImageBitmap() } }
    Box(
        modifier.size(size).focusRing(Shapes.photo).clip(Shapes.photo).background(c.surfaceContainerHigh).clickable(role = Role.Image, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth()) }
    }
}

/** Reads a JPEG at about [target] pixels on its short side, so a thumbnail never holds a full photo in memory. */
fun decodeScaled(path: String, target: Int): android.graphics.Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val short = minOf(bounds.outWidth, bounds.outHeight)
    if (short <= 0) return@runCatching null
    var sample = 1
    while (short / (sample * 2) >= target) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()

private const val MAX_TEXT = 500
private const val NOTE_LINES = 3

/**
 * Takes a photo (the checklist's Not sure, 09c): a dashed square with a camera-add icon and [label] beside it.
 * The whole row is the target; TalkBack reads the label.
 */
@Composable
fun AddPhotoTile(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.focusRing(Shapes.photo, inside = true).clip(Shapes.photo).clickable(role = Role.Button, onClick = onClick).padding(end = Space.card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dash = c.outline
        Box(
            Modifier.size(Size.photoSmall).drawBehind {
                val w = Size.outline.toPx()
                val r = Shapes.photoCorner.toPx()
                drawRoundRect(
                    dash, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(w, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(Size.dash.toPx(), Size.dash.toPx()))),
                )
            },
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(R.drawable.ic_camera_add), null, tint = c.onSurfaceVariant, modifier = Modifier.size(Size.icon)) }
        Spacer(Modifier.width(Space.card))
        Text(label, style = KeyholeType.supporting, color = c.onSurfaceVariant)
    }
}
