package app.unpeep.privacy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Above this font scale, layouts switch to their large-text form (labels wrap under, rows stack). */
const val LARGE_TEXT = 1.3f

@Composable
fun isLargeText(): Boolean = LocalDensity.current.fontScale > LARGE_TEXT

/**
 * Limits font scaling for [content] to [max]. Only for text inside fixed-size shapes (the nav bar, the lens core)
 * that can't grow; everything else scales freely.
 */
@Composable
fun CappedFontScale(max: Float = LARGE_TEXT, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    if (density.fontScale <= max) {
        content()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(density.density, max), content = content)
    }
}
