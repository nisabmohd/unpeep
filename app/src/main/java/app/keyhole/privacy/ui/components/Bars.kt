package app.keyhole.privacy.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import app.keyhole.privacy.ui.theme.ButtonMinText
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.Shapes
import app.keyhole.privacy.ui.theme.Size
import app.keyhole.privacy.ui.theme.Space

/*
 * App chrome: the same on every screen, so titles, buttons and the nav sit in exactly the same place everywhere.
 */

/** Top bar of a tab: 72 dp, the headline title 24 dp from the start, round tonal buttons at the end. */
@Composable
fun TabTopBar(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().heightIn(min = Size.topBar).padding(start = Space.heading, end = Space.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.inline),
    ) {
        Text(
            title, style = KeyholeType.headline, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        CappedFontScale { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.inline)) { actions() } }
    }
}

/** Top bar of a pushed screen: 72 dp, a round back button, an optional title, round actions at the end. */
@Composable
fun BackTopBar(onBack: () -> Unit, modifier: Modifier = Modifier, title: String? = null, close: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    // The first screen of a tablet's second pane has nowhere to go back to while both panes show (review 2).
    val inPane = LocalFirstInPane.current
    Row(
        modifier.fillMaxWidth().heightIn(min = Size.topBar).padding(horizontal = Space.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.inline),
    ) {
        if (!inPane) {
            RoundIconButton(
                painterResource(if (close) R.drawable.ic_close else R.drawable.ic_back),
                stringResource(if (close) R.string.close else R.string.back), onBack,
            )
        }
        // The title starts 8 dp after the round button: 72 dp from the screen edge on every board.
        Box(Modifier.weight(1f)) {
            if (title != null) {
                BasicText(
                    title, style = KeyholeType.title.copy(color = MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = KeyholeType.cardTitle.fontSize, maxFontSize = KeyholeType.title.fontSize),
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        // Actions stop growing at 130 % and never take more than their share, so the title always shows.
        CappedFontScale {
            Row(Modifier.widthIn(max = Size.topBarActions), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.inline)) { actions() }
        }
    }
}

/** A text action at the end of a top bar ("Skip step", "Scan again"): the accent colour, one line, a 48 dp pill to tap. */
@Composable
fun TopBarAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier.heightIn(min = Size.iconButton).focusRing(Shapes.pill).clip(Shapes.pill).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = Space.block),
        contentAlignment = Alignment.Center,
    ) {
        // Waiting (E01: "Scan again in 48 s"): muted, and not a target.
        BasicText(
            text, style = KeyholeType.label.copy(color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = ButtonMinText, maxFontSize = KeyholeType.label.fontSize),
        )
    }
}

/** The frame of a tab: status bar space, [TabTopBar], then [content]. The bottom nav lives outside, in the nav host. */
@Composable
fun TabScreen(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Beside the rail (wide) the title runs the whole width, as T01 draws it; else one centred column.
        TabTopBar(title, if (app.keyhole.privacy.ui.isWide()) Modifier else Modifier.widthIn(max = Size.contentMax), actions = actions)
        content()
    }
}

/**
 * The frame of a pushed screen: [BackTopBar], a scrolling body with the 16 dp margin, and an optional [dock]
 * (the one main action) pinned under it.
 */
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    close: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    dock: (@Composable ColumnScope.() -> Unit)? = null,
    scroll: Boolean = true,
    /** Two panes on a wide window (T03): the body uses the whole width instead of one 640 dp column. */
    panes: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val max = if (panes) androidx.compose.ui.unit.Dp.Unspecified else Size.contentMax
    Column(modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        BackTopBar(onBack, Modifier.widthIn(max = max), title, close, actions)
        Column(
            Modifier.weight(1f).widthIn(max = max).fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(start = Space.screen, end = Space.screen, bottom = Space.screen),
            content = content,
        )
        if (dock != null) Dock(Modifier.widthIn(max = Size.contentMax), dock) else NavBarSpacer()
        // (With panes the dock stays one column wide, under the left pane, as T03 draws it.)
    }
}

/** Room for the system navigation bar under a screen with nothing docked. */
@Composable
fun NavBarSpacer() = Spacer(Modifier.navigationBarsPadding())

/** One bottom-nav destination. */
@Immutable
data class NavItem(val label: String, @DrawableRes val icon: Int)

/**
 * Bottom nav: 80 dp on surfaceContainerLow, three items (Check · Reports · More); the selected one sits in a
 * primaryContainer pill. Labels shrink to fit long translations and are capped at 1.3× font scale.
 */
@Composable
fun KeyholeNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    NavigationBar(modifier, containerColor = c.surfaceContainerLow) {
        Spacer(Modifier.width(Space.navBarEnd))
        items.forEachIndexed { i, item ->
            val isSelected = i == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) haptics.buzz(Buzz.TICK)
                    onSelect(i)
                },
                icon = { Icon(painterResource(item.icon), contentDescription = null, modifier = Modifier.size(Size.icon)) },
                label = {
                    CappedFontScale {
                        // One size for all three labels (none shrunk alone); "…" only if a translation can't fit.
                        Text(item.label, style = KeyholeType.caption, color = LocalContentColor.current, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.onPrimaryContainer, selectedTextColor = c.onSurface, indicatorColor = c.primaryContainer,
                    unselectedIconColor = c.onSurfaceVariant, unselectedTextColor = c.onSurfaceVariant,
                ),
            )
        }
        Spacer(Modifier.width(Space.navBarEnd))
    }
}

/**
 * The navigation rail of a wide window (T01–T04): the mark on top, then Check · Reports · More down the side,
 * the selected one in a primaryContainer pill, as the bottom nav draws it.
 */
@Composable
fun KeyholeNavRail(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    androidx.compose.material3.NavigationRail(
        modifier.fillMaxHeight(), containerColor = c.surfaceContainerLow,
        header = { app.keyhole.privacy.ui.welcome.MarkTile(Size.railMark, Modifier.padding(vertical = Space.heading)) },
    ) {
        items.forEachIndexed { i, item ->
            val isSelected = i == selected
            androidx.compose.material3.NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) haptics.buzz(Buzz.TICK)
                    onSelect(i)
                },
                icon = { Icon(painterResource(item.icon), contentDescription = null, modifier = Modifier.size(Size.icon)) },
                label = {
                    CappedFontScale {
                        // One size for all three labels (none shrunk alone); "…" only if a translation can't fit.
                        Text(item.label, style = KeyholeType.caption, color = LocalContentColor.current, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                },
                colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                    selectedIconColor = c.onPrimaryContainer, selectedTextColor = c.onSurface, indicatorColor = c.primaryContainer,
                    unselectedIconColor = c.onSurfaceVariant, unselectedTextColor = c.onSurfaceVariant,
                ),
            )
        }
    }
}

/** True inside a tablet's second pane for its first screen: no back button there (T02, T04). Deeper screens keep it. */
val LocalFirstInPane = androidx.compose.runtime.compositionLocalOf { false }
