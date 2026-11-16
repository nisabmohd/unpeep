package app.keyhole.privacy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space
import app.keyhole.privacy.ui.theme.shortSpec
import app.keyhole.privacy.ui.theme.standardSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** One toast: what happened, its icon, and Undo when the action can be taken back. */
@Immutable
data class ToastData(val text: String, @DrawableRes val icon: Int, val action: String?, val onAction: (() -> Unit)?, val id: Long)

/**
 * The app's one toast (review 2 › Events and states): something that just happened, with at most one action (a report
 * deleted, marked as known, saved as a PDF, everything deleted). Something still true is a Note. Earlier wording:
 * a device marked as known, removed from a report) or where something went away (results cleared, a report gone).
 * Everything else stays a Note or a changed line. One at a time; a new one replaces the old.
 */
@Stable
class ToastState {
    var current by mutableStateOf<ToastData?>(null)
        private set

    /** The height of the dock on screen now, so the toast sits 12 dp above it (0 when there is none). */
    internal var dock by mutableStateOf(0.dp)
    private var next = 0L

    /** [undo]: the action is Undo (the usual one); [action] + [onAction] for another one ("Open"). */
    fun show(
        text: String, undo: (() -> Unit)? = null, action: String? = null, onAction: (() -> Unit)? = null,
        @DrawableRes icon: Int = if (undo != null || onAction != null) R.drawable.ic_check_circle else R.drawable.ic_info,
    ) {
        current = ToastData(text, icon, action, onAction ?: undo, next++)
    }

    fun dismiss(id: Long) {
        if (current?.id == id) current = null
    }
}

val LocalToasts = staticCompositionLocalOf { ToastState() }

/** A [Dock] tells the toast how tall it is while it is on screen. */
@Composable
internal fun Modifier.reportDockHeight(): Modifier {
    val toasts = LocalToasts.current
    val density = LocalDensity.current
    DisposableEffect(toasts) { onDispose { toasts.dock = 0.dp } }
    return onSizeChanged { toasts.dock = with(density) { it.height.toDp() } }
}

/**
 * Where toasts show: over the screens, 12 dp above the dock, or above the nav bar ([overNav]: the bar sits right
 * under this box) or the system bar. Stays 4 s, longer when TalkBack asks for it; swipe it left or right to dismiss.
 */
@Composable
fun BoxScope.ToastHost(state: ToastState, overNav: Boolean) {
    val toast = state.current
    val a11y = LocalAccessibilityManager.current
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            val ms = a11y?.calculateRecommendedTimeoutMillis(TOAST_MS, containsIcons = true, containsText = true, containsControls = toast.onAction != null) ?: TOAST_MS
            delay(ms)
            state.dismiss(toast.id)
        }
    }
    val system = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val above = when {
        state.dock > 0.dp -> state.dock
        overNav -> 0.dp
        else -> system
    }
    AnimatedVisibility(
        toast != null,
        Modifier.align(Alignment.BottomCenter).padding(bottom = above + Space.block).padding(horizontal = Space.screen).widthIn(max = Size.contentMax),
        enter = fadeIn(shortSpec()) + slideInVertically(standardSpec()) { it / 2 },
        exit = fadeOut(shortSpec()),
    ) {
        val shown = remember { mutableStateOf(toast) }
        if (toast != null) shown.value = toast
        shown.value?.let { t -> key(t.id) { Toast(t, onDismiss = { state.dismiss(t.id) }) } }
    }
}

/** The toast itself: inverse surface pill, icon, words, Undo (its own 48 dp target). */
@Composable
private fun Toast(toast: ToastData, onDismiss: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var width by remember { mutableStateOf(1) }
    Row(
        Modifier.fillMaxWidth()
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .alpha(1f - (abs(offset.value) / width).coerceIn(0f, 1f))
            .pointerInput(toast.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offset.value) > width * SWIPE_AWAY) {
                                offset.animateTo(if (offset.value > 0) width.toFloat() else -width.toFloat(), standardSpec())
                                onDismiss()
                            } else {
                                offset.animateTo(0f, standardSpec())
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f, standardSpec()) } },
                ) { change, drag ->
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + drag) }
                }
            }
            .heightIn(min = Size.toast).clip(Shapes.pill).background(c.inverseSurface)
            .semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Polite }
            .padding(start = Space.card + Space.small, end = if (toast.onAction != null) Space.headingInset else Space.heading),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(toast.icon), null, tint = c.inverseOnSurface, modifier = Modifier.size(Size.buttonIcon))
        Spacer(Modifier.width(Space.block))
        Text(toast.text, style = KeyholeType.label, color = c.inverseOnSurface, modifier = Modifier.weight(1f).padding(vertical = Space.block))
        val act = toast.onAction
        if (act != null) {
            Box(
                Modifier.focusRing(Shapes.pill).heightIn(min = Size.iconButton).clip(Shapes.pill)
                    .clickable(role = Role.Button) { haptics.buzz(Buzz.CONFIRM); act(); onDismiss() }
                    .padding(horizontal = Space.card),
                contentAlignment = Alignment.Center,
            ) { Text(toast.action ?: stringResource(R.string.undo), style = KeyholeType.label, color = c.inversePrimary) }
        }
    }
}

private const val TOAST_MS = 4_000L
private const val SWIPE_AWAY = 0.4f
