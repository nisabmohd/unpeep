package app.keyhole.privacy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize

/**
 * How much room the window gives (00-decisions › Tablets and landscape, T01–T07):
 * from [WIDE_DP] a navigation rail and two panes; between 600 and 840 one centred column, as on a phone;
 * a phone on its side shows the lens on the left and the list on the right.
 */
const val WIDE_DP = 840

/** From 840 dp wide: the rail, and two panes where a screen has them. */
@Composable
fun isWide(): Boolean = windowSize().width.value >= WIDE_DP

/** A phone on its side: wider than tall, and too short for the lens above a list. */
@Composable
fun isPhoneLandscape(): Boolean = windowSize().let { it.width > it.height && it.height.value < SHORT_DP }

/** The window's size in dp, as Compose measures it (not `Configuration.screenWidthDp`, which rounds and changes with insets). */
@Composable
fun windowSize(): DpSize {
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { DpSize(size.width.toDp(), size.height.toDp()) }
}

private const val SHORT_DP = 480
