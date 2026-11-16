package app.keyhole.privacy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import app.keyhole.privacy.ui.theme.Alpha
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space

/*
 * Meters (docs/COMPONENTS.md): how strong something is, drawn, always with its meaning in words beside it.
 */

/**
 * The Find it meter: [segments] pills, the first [level] lit, each a step stronger than the one before, with what
 * the two ends mean underneath ("Far" · "Right here"). [description] is what TalkBack says for the whole meter.
 */
@Composable
fun SegmentMeter(level: Int, segments: Int, startLabel: String, endLabel: String, description: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val lit = level.coerceIn(0, segments)
    Column(modifier.fillMaxWidth().padding(horizontal = Space.card).clearAndSetSemantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(Space.inline)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.small)) {
            repeat(segments) { i ->
                val strength = if (segments <= 1) 1f else Alpha.meterFaint + (1f - Alpha.meterFaint) * i / (segments - 1)
                Box(Modifier.weight(1f).height(Size.dot).clip(Shapes.pill).background(if (i < lit) c.primary.copy(alpha = strength) else c.surfaceContainerHighest))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(startLabel, style = KeyholeType.supporting, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(endLabel, style = KeyholeType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}
