package app.unpeep.privacy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * What a buzz means (docs/MOTION.md § Haptics). One meaning, one feel, on every screen: the same thing never
 * buzzes two ways. Android's "touch feedback" setting turns them all off.
 */
enum class Buzz {
    /** Picking one of a few: a chip, a segment, a choice row. The lightest. */
    TICK,
    /** Something switched on or off: torch, zoom, beep, a sweep step. */
    TOGGLE_ON,
    TOGGLE_OFF,
    /** Something was kept: a photo saved, added to the report, marked as known. */
    CONFIRM,
    /** It didn't work: the camera won't open, a photo couldn't be saved. */
    REJECT,
    /** Worth your attention: a possible lens ringed, the magnet jumped, a scan found a likely camera. The strongest. */
    FOUND,
    /** A scan finished with nothing that needs a look. */
    DONE,
}

/** The app's haptics; get it with [rememberHaptics]. */
@Stable
class Haptics(private val feedback: HapticFeedback) {
    fun buzz(buzz: Buzz) = feedback.performHapticFeedback(type(buzz))

    companion object {
        fun type(buzz: Buzz): HapticFeedbackType = when (buzz) {
            Buzz.TICK -> HapticFeedbackType.SegmentTick
            Buzz.TOGGLE_ON -> HapticFeedbackType.ToggleOn
            Buzz.TOGGLE_OFF -> HapticFeedbackType.ToggleOff
            Buzz.CONFIRM -> HapticFeedbackType.Confirm
            Buzz.REJECT -> HapticFeedbackType.Reject
            Buzz.FOUND -> HapticFeedbackType.LongPress
            Buzz.DONE -> HapticFeedbackType.GestureEnd
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
