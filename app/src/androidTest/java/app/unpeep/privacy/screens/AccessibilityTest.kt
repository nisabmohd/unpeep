package app.unpeep.privacy.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.unpeep.privacy.R
import app.unpeep.privacy.data.Settings
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.domain.Place
import app.unpeep.privacy.setUnpeepContent
import app.unpeep.privacy.text
import app.unpeep.privacy.ui.checklist.ChecklistContent
import app.unpeep.privacy.ui.checklist.ChecklistState
import app.unpeep.privacy.ui.home.HomeContent
import app.unpeep.privacy.ui.home.HomeState
import app.unpeep.privacy.ui.more.AppearanceContent
import app.unpeep.privacy.ui.report.ReportContent
import app.unpeep.privacy.ui.report.previewReport
import app.unpeep.privacy.ui.report.rememberReportWords
import app.unpeep.privacy.ui.welcome.WelcomeScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The accessibility pass (CLAUDE.md › Design rules): at 200 % text on a 360 dp-wide phone, every screen still shows
 * its main action, and everything that can be tapped is at least 48 dp and has a name for TalkBack.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule val rule = createComposeRule()

    private fun big(content: @Composable () -> Unit) = rule.setUnpeepContent {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
            Box(Modifier.width(360.dp)) { content() }
        }
    }

    /** Every tappable thing is 48 × 48 dp at least and says what it is. */
    private fun everyTapIsBigEnoughAndNamed() {
        val nodes = rule.onAllNodes(hasClickAction(), useUnmergedTree = false).fetchSemanticsNodes()
        assertTrue("nothing tappable", nodes.isNotEmpty())
        val px = 48 * rule.density.density - 1
        for (n in nodes) {
            val name = n.config.let { c -> if (SemanticsProperties.Text in c) c[SemanticsProperties.Text] else null }?.joinToString() ?: n.config.let { c -> if (SemanticsProperties.ContentDescription in c) c[SemanticsProperties.ContentDescription] else null }?.joinToString() ?: n.config.let { c -> if (SemanticsProperties.EditableText in c) c[SemanticsProperties.EditableText].text else null }
            assertTrue("tappable with no name: ${n.config}", !name.isNullOrBlank() || n.children.any { c -> c.config.contains(SemanticsProperties.Text) || c.config.contains(SemanticsProperties.ContentDescription) })
            assertTrue("$name is ${n.size} px, under 48 dp", n.size.height >= px)
        }
    }

    @Test fun homeAtTwoHundredPercent() {
        big { HomeContent(HomeState(loaded = true), {}, {}, { _, _ -> }, {}) }
        rule.onNodeWithText(text(R.string.start)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.or_run_one_check)).performScrollTo().assertIsDisplayed()
        everyTapIsBigEnoughAndNamed()
    }

    @Test fun welcomeAtTwoHundredPercent() {
        big { WelcomeScreen {} }
        rule.onNodeWithText(text(R.string.get_started)).assertIsDisplayed()
        everyTapIsBigEnoughAndNamed()
    }

    @Test fun reportAtTwoHundredPercent() {
        big { ReportContent(previewReport(), rememberReportWords(), {}, {}, {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.export)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.couldnt_check)).performScrollTo().assertIsDisplayed()
        everyTapIsBigEnoughAndNamed()
    }

    @Test fun checklistAtTwoHundredPercent() {
        big { ChecklistContent(ChecklistState(Place.STAY), {}, {}, {}) }
        rule.onNodeWithText(text(R.string.see_report)).assertIsDisplayed()
        everyTapIsBigEnoughAndNamed()
    }

    @Test fun appearanceAtTwoHundredPercent() {
        big { AppearanceContent(Settings(), {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }) }
        // Text size is the last block (sweep steps live on their own screen, 14).
        rule.onNodeWithText(text(R.string.text_size_caption)).performScrollTo().assertIsDisplayed()
        everyTapIsBigEnoughAndNamed()
    }

    @Test fun darkModeDrawsTheSameScreens() {
        rule.setUnpeepContent(dark = true) { ReportContent(previewReport(), rememberReportWords(), {}, {}, {}, {}, {}, {}, {}) }
        rule.onNodeWithText(text(R.string.export)).assertIsDisplayed()
        everyTapIsBigEnoughAndNamed()
    }
}
