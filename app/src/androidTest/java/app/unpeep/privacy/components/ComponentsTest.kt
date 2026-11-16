package app.unpeep.privacy.components

import androidx.compose.ui.test.onAllNodesWithText
import app.unpeep.privacy.ui.components.ToastHost
import app.unpeep.privacy.ui.components.ToastState
import app.unpeep.privacy.ui.components.NoteTone
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.unpeep.privacy.R
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.setUnpeepContent
import app.unpeep.privacy.text
import app.unpeep.privacy.ui.components.BackTopBar
import app.unpeep.privacy.ui.components.BarMeter
import app.unpeep.privacy.ui.components.Buzz
import app.unpeep.privacy.ui.components.Segment
import app.unpeep.privacy.ui.components.Segmented
import app.unpeep.privacy.ui.components.Shutter
import app.unpeep.privacy.ui.components.SpotMark
import app.unpeep.privacy.ui.components.SpotRings
import app.unpeep.privacy.ui.components.ButtonPair
import app.unpeep.privacy.ui.components.CheckRow
import app.unpeep.privacy.ui.components.CheckState
import app.unpeep.privacy.ui.components.Chip
import app.unpeep.privacy.ui.components.ChoiceRow
import app.unpeep.privacy.ui.components.EmptyState
import app.unpeep.privacy.ui.components.Grouped
import app.unpeep.privacy.ui.components.LensProgress
import app.unpeep.privacy.ui.components.LensStartButton
import app.unpeep.privacy.ui.components.ListRow
import app.unpeep.privacy.ui.components.Note
import app.unpeep.privacy.ui.components.PrimaryButton
import app.unpeep.privacy.ui.components.SecondaryButton
import app.unpeep.privacy.ui.components.SegmentMeter
import app.unpeep.privacy.ui.components.SignRow
import app.unpeep.privacy.ui.components.SummaryBox
import app.unpeep.privacy.ui.components.SummaryPart
import app.unpeep.privacy.ui.components.SummaryTone
import app.unpeep.privacy.ui.components.SwitchRow
import app.unpeep.privacy.ui.components.Tag
import app.unpeep.privacy.ui.components.TagKind
import app.unpeep.privacy.ui.components.TopBarAction
import app.unpeep.privacy.ui.components.ValueRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shared components (docs/COMPONENTS.md), each drawn alone: what it says, what TalkBack hears, that it can be
 * tapped where it should and is at least 48 dp where it can.
 */
@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule val rule = createComposeRule()

    private val touch = 48.dp

    // --- Buttons and bars ---------------------------------------------------------------------------

    @Test fun primaryButtonTapsAndIsTallEnough() {
        var taps = 0
        rule.setUnpeepContent { PrimaryButton("Next: hotspots", { taps++ }, trailing = painterResource(R.drawable.ic_arrow_right)) }
        rule.onNodeWithText("Next: hotspots").assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(touch).performClick()
        assertEquals(1, taps)
    }

    @Test fun aDisabledButtonDoesNotTap() {
        var taps = 0
        rule.setUnpeepContent { PrimaryButton("Export", { taps++ }, enabled = false) }
        rule.onNodeWithText("Export").assertIsNotEnabled().performClick()
        assertEquals(0, taps)
    }

    @Test fun aButtonPairShowsBothAndEachTapsItsOwn() {
        val taps = mutableListOf<String>()
        rule.setUnpeepContent {
            ButtonPair(
                secondary = { m -> SecondaryButton("Cancel", { taps += "cancel" }, m) },
                primary = { m -> PrimaryButton("Mark as known", { taps += "mark" }, m) },
            )
        }
        rule.onNodeWithText("Cancel").assertIsEnabled().performClick()
        rule.onNodeWithText("Mark as known").performClick()
        assertEquals(listOf("cancel", "mark"), taps)
    }

    @Test fun topBarActionTapsAndIsATouchTarget() {
        var taps = 0
        rule.setUnpeepContent { TopBarAction("Skip step", { taps++ }) }
        rule.onNodeWithText("Skip step").assertHasClickAction().assertHeightIsAtLeast(touch).performClick()
        assertEquals(1, taps)
    }

    @Test fun theBackBarHasALabelledBackButtonAndClosesWhenAsked() {
        var backs = 0
        rule.setUnpeepContent {
            Column {
                BackTopBar({ backs++ }, title = "Wi-Fi")
                BackTopBar({ backs += 10 }, title = "Stay", close = true)
            }
        }
        rule.onNodeWithText("Wi-Fi").assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.back)).assertHeightIsAtLeast(touch).assertWidthIsAtLeast(touch).performClick()
        rule.onNodeWithContentDescription(text(R.string.close)).performClick()
        assertEquals(11, backs)
    }

    @Test fun theStartLensTaps() {
        var taps = 0
        rule.setUnpeepContent { LensStartButton("Start", { taps++ }) }
        rule.onNodeWithText("Start").performClick()
        assertEquals(1, taps)
    }

    // --- Results in words ---------------------------------------------------------------------------

    @Test fun tagsAlwaysSayTheirResultInWords() {
        rule.setUnpeepContent {
            Column {
                Tag("Likely camera", TagKind.LIKELY_CAMERA, icon = painterResource(R.drawable.ic_warning))
                Tag("Worth a look", TagKind.WORTH_A_LOOK)
                Tag("TV", TagKind.KNOWN)
                Tag("Stronger", TagKind.GOOD)
            }
        }
        for (word in listOf("Likely camera", "Worth a look", "TV", "Stronger")) rule.onNodeWithText(word).assertIsDisplayed()
    }

    @Test fun theSummaryBoxSaysTheCountAndEveryPartInWords() {
        rule.setUnpeepContent {
            SummaryBox(
                "18 devices on SeaView_Guest", "18",
                listOf(SummaryPart(1, "1 likely camera", SummaryTone.ALERT), SummaryPart(2, "2 worth a look", SummaryTone.WARNING), SummaryPart(15, "15 known", SummaryTone.NEUTRAL)),
            )
        }
        // The sentence is one text (its number only drawn bigger), and the legend says each colour in words.
        for (word in listOf("18 devices on SeaView_Guest", "1 likely camera", "2 worth a look", "15 known")) rule.onNodeWithText(word, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun theSummaryBoxDrawsWithNothingFoundAndWithANumberThatIsNotInTheSentence() {
        rule.setUnpeepContent {
            Column {
                SummaryBox("0 Wi-Fi networks nearby", "0", listOf(SummaryPart(0, "0 likely cameras", SummaryTone.ALERT), SummaryPart(0, "0 others", SummaryTone.NEUTRAL)))
                SummaryBox("No number here", "7", emptyList())
            }
        }
        // Nothing counted: the legend is one quiet line, not a row of zeros.
        // Both boxes count nothing, so both say so.
        rule.onAllNodesWithText(text(R.string.summary_nothing), useUnmergedTree = true).assertCountEquals(2)
        rule.onAllNodesWithText("0 likely cameras", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("No number here", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun theProgressLensReadsAsOneSentence() {
        rule.setUnpeepContent { LensProgress(0.6f, "11", "of 18") }
        rule.onNodeWithContentDescription("11 of 18").assertIsDisplayed()
    }

    @Test fun theProgressLensWithAnIconStillReadsItsWords() {
        rule.setUnpeepContent { LensProgress(0.8f, "Close", "about 1–2 m", icon = { app.unpeep.privacy.ui.components.CheckIcon(Check.HOTSPOTS) }) }
        rule.onNodeWithContentDescription("Close about 1–2 m").assertIsDisplayed()
    }

    @Test fun theProgressLensSurvivesValuesOutsideItsRange() {
        rule.setUnpeepContent {
            Column {
                LensProgress(-1f, "0", "found")
                LensProgress(7f, "99", "of 99")
            }
        }
        rule.onNodeWithContentDescription("0 found").assertExists()
        rule.onNodeWithContentDescription("99 of 99").assertExists()
    }

    @Test fun theMeterSaysWhatItShows() {
        rule.setUnpeepContent { SegmentMeter(4, 5, "Far", "Right here", description = "Close, about 1–2 m") }
        rule.onNodeWithContentDescription("Close, about 1–2 m").assertIsDisplayed()
    }

    @Test fun theMeterDrawsWhenEmptyFullAndOutOfRange() {
        rule.setUnpeepContent {
            Column {
                SegmentMeter(0, 5, "Far", "Right here", description = "none")
                SegmentMeter(5, 5, "Far", "Right here", description = "full")
                SegmentMeter(9, 5, "Far", "Right here", description = "over")
                SegmentMeter(-3, 1, "Far", "Right here", description = "under")
            }
        }
        for (d in listOf("none", "full", "over", "under")) rule.onNodeWithContentDescription(d).assertExists()
    }

    // --- The check row ------------------------------------------------------------------------------

    @Test fun theProgressRowNamesEveryCheckWithItsState() {
        val steps = Check.entries
        rule.setUnpeepContent {
            CheckRow(steps, Modifier.fillMaxWidth(), state = { c ->
                when {
                    c == Check.HOTSPOTS -> CheckState.CURRENT
                    c == Check.WIFI -> CheckState.DONE
                    c == Check.INFRARED -> CheckState.SKIPPED
                    else -> CheckState.PLAIN
                }
            })
        }
        rule.onNodeWithContentDescription("${text(R.string.check_wifi)}, ${text(R.string.check_state_done)}").assertIsDisplayed()
        rule.onNodeWithContentDescription("${text(R.string.check_hotspots)}, ${text(R.string.check_state_current)}").assertIsDisplayed()
        rule.onNodeWithContentDescription("${text(R.string.check_infrared)}, ${text(R.string.check_state_skipped)}").assertIsDisplayed()
        // All seven fit the width: the last one is on screen without scrolling.
        rule.onNodeWithContentDescription(text(R.string.check_checklist)).assertIsDisplayed()
    }

    @Test fun theRunRowStartsTheCheckThatWasTapped() {
        val ran = mutableListOf<Check>()
        rule.setUnpeepContent { CheckRow(listOf(Check.WIFI, Check.HOTSPOTS, Check.BLUETOOTH), onRun = { ran += it }) }
        rule.onNodeWithText(text(R.string.check_bluetooth)).performClick()
        rule.onNodeWithText(text(R.string.check_wifi)).performClick()
        assertEquals(listOf(Check.BLUETOOTH, Check.WIFI), ran)
    }

    @Test fun aProgressRowWithOneStepOrNoneStillDraws() {
        rule.setUnpeepContent {
            Column {
                CheckRow(listOf(Check.MAGNET), state = { CheckState.CURRENT })
                CheckRow(emptyList())
            }
        }
        rule.onAllNodesWithContentDescription("${text(R.string.check_magnet)}, ${text(R.string.check_state_current)}").assertCountEquals(1)
    }

    // --- Rows ---------------------------------------------------------------------------------------

    @Test fun aListRowWithOnClickOpensAndOneWithoutDoesNot() {
        var opened = 0
        rule.setUnpeepContent {
            Grouped {
                row { shape -> ListRow("IPC-3F2A", shape = shape, line = "Answers as a network camera", onClick = { opened++ }) }
                row { shape -> ListRow("Pixel 8", shape = shape, line = "192.168.1.14") }
            }
        }
        rule.onNodeWithText("IPC-3F2A").assertHeightIsAtLeast(touch).performClick()
        rule.onNodeWithText("Pixel 8").performClick()
        assertEquals(1, opened)
    }

    @Test fun aVeryLongTitleStillShowsItsLine() {
        rule.setUnpeepContent { ListRow("A device with a very long name that goes on and on and on and on and on and on", line = "Second line", onClick = {}) }
        rule.onNodeWithText("Second line").assertIsDisplayed()
    }

    @Test fun valueAndSignRowsShowTheirWords() {
        rule.setUnpeepContent {
            Column {
                ValueRow("Open ports", "80 · 554 · 8899")
                SignRow(painterResource(R.drawable.ic_danger), "Streams video on port 554", alert = true)
            }
        }
        rule.onNodeWithText("80 · 554 · 8899", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Streams video on port 554").assertIsDisplayed()
    }

    @Test fun aNoteReadsItsTitleAndTextTogether() {
        rule.setUnpeepContent { Note("This Wi-Fi may keep devices apart.", title = "Only the router answered", tone = NoteTone.WARNING) }
        rule.onNodeWithText("Only the router answered", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("This Wi-Fi may keep devices apart.", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun aNoteActionIsItsOwnButton() {
        var tapped = 0
        rule.setUnpeepContent { Note("It's in your Downloads folder.", title = "Saved as a PDF", tone = NoteTone.GOOD, action = "Open", onAction = { tapped++ }) }
        rule.onNodeWithText("Open").assertHasClickAction().performClick()
        assertEquals(1, tapped)
        rule.onNodeWithText("Saved as a PDF", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun aFailedExportToastOffersTryAgain() {
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val toasts = ToastState()
        var tries = 0
        rule.setUnpeepContent { androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) { ToastHost(toasts, overNav = false) } }
        rule.runOnIdle { toasts.show(ctx.getString(app.unpeep.privacy.R.string.export_failed), action = ctx.getString(app.unpeep.privacy.R.string.try_again), onAction = { tries++ }, icon = app.unpeep.privacy.R.drawable.ic_info) }
        rule.onNodeWithText(ctx.getString(app.unpeep.privacy.R.string.export_failed), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(ctx.getString(app.unpeep.privacy.R.string.try_again)).performClick()
        assertEquals(1, tries)
    }

    @Test fun aToastShowsWithUndoAndUndoRunsOnce() {
        val toasts = ToastState()
        var undone = 0
        rule.setUnpeepContent {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) { ToastHost(toasts, overNav = true) }
        }
        rule.runOnIdle { toasts.show("Report deleted", undo = { undone++ }) }
        rule.onNodeWithText("Report deleted").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.undo)).performClick()
        rule.waitForIdle()
        assertEquals(1, undone)
        rule.onNodeWithText("Report deleted").assertDoesNotExist()
    }

    @Test fun aToastWithoutUndoGoesAwayByItself() {
        val toasts = ToastState()
        rule.mainClock.autoAdvance = false
        rule.setUnpeepContent { androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) { ToastHost(toasts, overNav = false) } }
        rule.runOnIdle { toasts.show("Those results were cleared when Unpeep closed") }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Those results were cleared when Unpeep closed").assertExists()
        rule.mainClock.advanceTimeBy(12_000)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("Those results were cleared when Unpeep closed").assertDoesNotExist()
    }

    @Test fun anEmptyStateSaysWhatIsMissing() {
        rule.setUnpeepContent { EmptyState(painterResource(R.drawable.ic_wifi), "Not on Wi-Fi", "Join the room's Wi-Fi, then scan again.") }
        rule.onNodeWithText("Not on Wi-Fi").assertIsDisplayed()
        rule.onNodeWithText("Join the room's Wi-Fi, then scan again.").assertIsDisplayed()
    }

    // --- Choices ------------------------------------------------------------------------------------

    @Test fun aChipIsSelectedOnlyWhenPicked() {
        var picked = ""
        rule.setUnpeepContent {
            Column {
                Chip("Stay", true, { picked = "stay" })
                Chip("Office", false, { picked = "office" })
            }
        }
        rule.onNodeWithText("Stay").assertIsSelected()
        rule.onNodeWithText("Office").assertHeightIsAtLeast(touch).performClick()
        assertEquals("office", picked)
    }

    @Test fun aChoiceRowPicksAndASwitchRowToggles() {
        var picked = false
        var on = true
        rule.setUnpeepContent {
            Column {
                ChoiceRow("Part of the room", false, { picked = true }, line = "The TV, a speaker or the router")
                SwitchRow("Wi-Fi", on, { on = it })
            }
        }
        rule.onNodeWithText("Part of the room").performClick()
        rule.onNodeWithText("Wi-Fi").performClick()
        assertTrue(picked)
        assertEquals(false, on)
    }

    // --- Week 4: viewfinder, picker, bars, haptics ----------------------------------------------------

    /** Records every buzz instead of buzzing. */
    private class Recorder : androidx.compose.ui.hapticfeedback.HapticFeedback {
        val types = mutableListOf<androidx.compose.ui.hapticfeedback.HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) { types += hapticFeedbackType }
    }

    private fun withHaptics(recorder: Recorder, content: @androidx.compose.runtime.Composable () -> Unit) = rule.setUnpeepContent {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalHapticFeedback provides recorder, content = content)
    }

    @Test fun eachMeaningHasItsOwnFeel() {
        val types = Buzz.entries.map { app.unpeep.privacy.ui.components.Haptics.type(it) }
        assertEquals("no two meanings feel the same", types.size, types.toSet().size)
    }

    @Test fun pickingAChipTicksButPickingThePickedOneDoesNot() {
        val r = Recorder()
        withHaptics(r) {
            Column {
                Chip("Stay", true, {})
                Chip("Office", false, {})
            }
        }
        rule.onNodeWithText("Stay").performClick()
        assertTrue(r.types.isEmpty())
        rule.onNodeWithText("Office").performClick()
        assertEquals(listOf(app.unpeep.privacy.ui.components.Haptics.type(Buzz.TICK)), r.types)
    }

    @Test fun aSwitchRowFeelsOnAndOff() {
        val r = Recorder()
        var on = false
        withHaptics(r) { SwitchRow("Beep", on, { on = it }) }
        rule.onNodeWithText("Beep").performClick()
        assertEquals(listOf(app.unpeep.privacy.ui.components.Haptics.type(Buzz.TOGGLE_ON)), r.types)
    }

    @Test fun theSegmentedPickerPicksAndTicks() {
        val r = Recorder()
        var picked = 0
        withHaptics(r) { Segmented(listOf(Segment("Lens"), Segment("Infrared")), picked, { picked = it }) }
        rule.onNodeWithText("Lens").assertIsSelected().assertHeightIsAtLeast(touch)
        rule.onNodeWithText("Infrared").performClick()
        assertEquals(1, picked)
        assertEquals(listOf(app.unpeep.privacy.ui.components.Haptics.type(Buzz.TICK)), r.types)
    }

    @Test fun theShutterHasALabelAndDoesNothingWhileTheCameraStarts() {
        var shots = 0
        rule.setUnpeepContent {
            app.unpeep.privacy.ui.theme.ViewfinderTheme {
                Column {
                    Shutter(painterResource(R.drawable.ic_camera), "Save a photo", { shots++ })
                    Shutter(painterResource(R.drawable.ic_camera), "Waiting", { shots += 10 }, enabled = false)
                }
            }
        }
        rule.onNodeWithContentDescription("Save a photo").assertHeightIsAtLeast(touch).performClick()
        rule.onNodeWithContentDescription("Waiting").performClick()
        assertEquals(1, shots)
    }

    @Test fun spotRingsSayHowManyAndLabelTheFirstAndSkipThoseOffScreen() {
        rule.setUnpeepContent {
            app.unpeep.privacy.ui.theme.ViewfinderTheme {
                SpotRings(listOf(SpotMark(0.3f, 0.4f), SpotMark(0.7f, 0.6f), SpotMark(1.4f, 0.5f)), "Look here", "2 possible lenses ringed")
            }
        }
        rule.onNodeWithContentDescription("2 possible lenses ringed").assertExists()
    }

    @Test fun noSpotsSayNothing() {
        rule.setUnpeepContent { app.unpeep.privacy.ui.theme.ViewfinderTheme { SpotRings(emptyList(), "Look here", "none ringed") } }
        rule.onAllNodesWithContentDescription("none ringed").assertCountEquals(0)
    }

    @Test fun theBarMeterDrawsEmptyFullAndOverflowing() {
        rule.setUnpeepContent {
            Column {
                BarMeter(emptyList(), 24, 10, 50, "empty")
                BarMeter(List(40) { it * 3 }, 24, 10, 50, "many")
                BarMeter(listOf(-5, 0, 200), 24, 10, 0, "odd")
            }
        }
        for (d in listOf("empty", "many", "odd")) rule.onNodeWithContentDescription(d).assertExists()
    }
}
