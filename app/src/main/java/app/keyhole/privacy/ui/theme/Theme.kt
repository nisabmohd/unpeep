package app.keyhole.privacy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LocalDarkTheme = staticCompositionLocalOf { false }
private val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

@Composable
fun ThemeSettings.isDark(): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun KeyholeTheme(settings: ThemeSettings = ThemeSettings(), content: @Composable () -> Unit) {
    val dark = settings.isDark()
    val context = LocalContext.current
    val dynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        else -> remember(settings.accentHue, dark) { seedColorScheme(settings.accentHue, dark) }
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val type = remember(settings.font, settings.textSize, rtl) { keyholeTypography(settings.font, settings.textSize.scale, rtl) }
    CompositionLocalProvider(LocalDarkTheme provides dark, LocalThemeSettings provides settings) {
        MaterialTheme(colorScheme = colors, typography = type, content = content)
    }
}

/** A check's colours follow light/dark only. */
@Composable @ReadOnlyComposable
fun checkColors(hue: Float): CheckColors = checkColors(hue, LocalDarkTheme.current)

/** The progress lens's core: white in light, the page's colour in dark. */
@Composable @ReadOnlyComposable
fun lensCore(): Color = if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerLowest else LensCore

@Composable @ReadOnlyComposable
fun warningColors(): WarningColors = warningColors(LocalDarkTheme.current)

/**
 * The viewfinder's theme (lens and infrared finders): always dark, whatever the app's theme, as a camera screen is;
 * the same accent (or wallpaper colours) as the rest of the app, on the viewfinder's near-black page.
 */
@Composable
fun ViewfinderTheme(content: @Composable () -> Unit) {
    val settings = LocalThemeSettings.current
    val context = LocalContext.current
    val dynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = if (dynamic) dynamicDarkColorScheme(context) else remember(settings.accentHue) { seedColorScheme(settings.accentHue, dark = true) }
    val colors = base.copy(background = Viewfinder.page, surface = Viewfinder.page, onBackground = Viewfinder.content, onSurface = Viewfinder.content, onSurfaceVariant = Viewfinder.muted)
    CompositionLocalProvider(LocalDarkTheme provides true) {
        // Typography and shapes carry over from the app's theme (MaterialTheme's defaults are the current ones).
        MaterialTheme(colorScheme = colors, content = content)
    }
}

/** True while the app (not the viewfinder) is dark: what the system bars go back to when a viewfinder closes. */
@Composable @ReadOnlyComposable
fun isAppDark(): Boolean = LocalDarkTheme.current
