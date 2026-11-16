package app.unpeep.privacy.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavBackStackEntry

/*
 * Motion (docs/MOTION.md). Every transition in the app comes from here (`ProjectRulesTest`).
 * One motion per kind of change; nothing bounces. "Remove animations" is respected through the system scale.
 */
object Easing {
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

object Duration {
    const val SHORT = 150
    const val STANDARD = 300
}

fun <T> standardSpec(ms: Int = Duration.STANDARD): FiniteAnimationSpec<T> = tween(ms, easing = Easing.standard)

/** The progress arc eases to each new value; the ✓ badge scales in once; a check's ring fades in. */
fun <T> progressSpec(): FiniteAnimationSpec<T> = tween(Duration.STANDARD, easing = Easing.standard)
fun <T> shortSpec(): FiniteAnimationSpec<T> = tween(Duration.SHORT, easing = Easing.standard)

/** A steady loop over [ms] with no easing (WaitingDots steps through it). */
fun <T> stepSpec(ms: Int): androidx.compose.animation.core.DurationBasedAnimationSpec<T> = tween(ms, easing = androidx.compose.animation.core.LinearEasing)

/**
 * Shared axis X, as Burrow and Weft: a 30 dp-ish slide (7.5 % of the width) on the emphasized curves over 450 ms,
 * the new screen fading in just after the old one starts to leave. Nothing scales, nothing bounces.
 */
private const val AXIS_SHIFT = 30
private const val AXIS_OF = 400
private const val PUSH_MS = 450
private const val FADE_IN_DELAY = 60

typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

object NavMotion {
    val push: Enter = {
        slideInHorizontally(tween(PUSH_MS, easing = Easing.emphasizedDecelerate)) { it * AXIS_SHIFT / AXIS_OF } +
            fadeIn(tween(Duration.STANDARD, delayMillis = FADE_IN_DELAY))
    }
    val pushExit: Exit = {
        slideOutHorizontally(tween(PUSH_MS, easing = Easing.emphasizedAccelerate)) { -(it * AXIS_SHIFT / AXIS_OF) } +
            fadeOut(tween(Duration.SHORT))
    }
    // Back follows the finger (predictive back), so it is seeked rather than played: the screen underneath starts
    // showing at once and the one leaving fades over the whole swipe. The two always overlap; the middle of a slow
    // swipe never shows an empty page.
    val pop: Enter = {
        slideInHorizontally(tween(PUSH_MS, easing = Easing.emphasizedDecelerate)) { -(it * AXIS_SHIFT / AXIS_OF) } +
            fadeIn(tween(Duration.STANDARD))
    }
    val popExit: Exit = {
        slideOutHorizontally(tween(PUSH_MS, easing = Easing.emphasizedAccelerate)) { it * AXIS_SHIFT / AXIS_OF } +
            fadeOut(tween(PUSH_MS))
    }

    /** Fade through: from one bottom tab to another, and only then. Old fades out fast, new fades + scales in from 92%. */
    val tabEnter: Enter = { fadeIn(tween(FADE_IN, delayMillis = FADE_OUT)) + scaleIn(tween(FADE_IN, delayMillis = FADE_OUT), initialScale = 0.92f) }
    val tabExit: Exit = { fadeOut(tween(FADE_OUT)) }
}

private const val FADE_OUT = 90
private const val FADE_IN = 210

/** For AnimatedContent inside a screen (a sweep step changing, learning → reading on Magnet). */
fun fadeThrough(): ContentTransform =
    (fadeIn(tween(FADE_IN, delayMillis = FADE_OUT)) + scaleIn(tween(FADE_IN, delayMillis = FADE_OUT), initialScale = 0.96f)) togetherWith fadeOut(tween(FADE_OUT))
