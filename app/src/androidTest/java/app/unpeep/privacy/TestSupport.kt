package app.unpeep.privacy

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import app.unpeep.privacy.ui.theme.UnpeepTheme
import app.unpeep.privacy.ui.theme.ThemeMode
import app.unpeep.privacy.ui.theme.ThemeSettings

/** The app's own words, so tests follow strings.xml instead of repeating it. */
fun text(@StringRes id: Int, vararg args: Any): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, *args)

/** Draws [content] as the app does: in the Unpeep theme on the page colour. */
fun ComposeContentTestRule.setUnpeepContent(dark: Boolean = false, content: @Composable () -> Unit) = setContent {
    UnpeepTheme(ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
    }
}
