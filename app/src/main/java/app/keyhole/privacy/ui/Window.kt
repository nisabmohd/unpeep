package app.keyhole.privacy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * How much room the window gives (00-decisions › Tablets and landscape, T01–T07):
 * from [WIDE_DP] a navigation rail and two panes; between 600 and 840 one centred column, as on a phone;
 * a phone on its side shows the lens on the left and the list on the right.
 */
const val WIDE_DP = 840

/** From 840 dp wide: the rail, and two panes where a screen has them. */
@Composable
fun isWide(): Boolean = LocalConfiguration.current.screenWidthDp >= WIDE_DP

/** A phone on its side: wider than tall, and too short for the lens above a list. */
@Composable
fun isPhoneLandscape(): Boolean = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp && it.screenHeightDp < SHORT_DP }

private const val SHORT_DP = 480
