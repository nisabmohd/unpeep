package app.keyhole.privacy.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.keyhole.privacy.R
import app.keyhole.privacy.ui.theme.KeyholeType
import app.keyhole.privacy.ui.theme.NavLabelMinSize
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
        actions()
    }
}

/** Top bar of a pushed screen: 72 dp, a round back button, an optional title, round actions at the end. */
@Composable
fun BackTopBar(onBack: () -> Unit, modifier: Modifier = Modifier, title: String? = null, close: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().heightIn(min = Size.topBar).padding(horizontal = Space.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.inline),
    ) {
        RoundIconButton(
            painterResource(if (close) R.drawable.ic_close else R.drawable.ic_back),
            stringResource(if (close) R.string.close else R.string.back), onBack,
        )
        Box(Modifier.weight(1f).padding(start = Space.inline)) {
            if (title != null) {
                BasicText(
                    title, style = KeyholeType.title.copy(color = MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = KeyholeType.cardTitle.fontSize, maxFontSize = KeyholeType.title.fontSize),
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        actions()
    }
}

/** The frame of a tab: status bar space, [TabTopBar], then [content]. The bottom nav lives outside, in the nav host. */
@Composable
fun TabScreen(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        TabTopBar(title, Modifier.widthIn(max = Size.contentMax), actions = actions)
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
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        BackTopBar(onBack, Modifier.widthIn(max = Size.contentMax), title, close, actions)
        Column(
            Modifier.weight(1f).widthIn(max = Size.contentMax).fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(start = Space.screen, end = Space.screen, bottom = Space.screen),
            content = content,
        )
        if (dock != null) Dock(Modifier.widthIn(max = Size.contentMax), dock) else NavBarSpacer()
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
    val haptics = LocalHapticFeedback.current
    NavigationBar(modifier, containerColor = c.surfaceContainerLow) {
        Spacer(Modifier.width(Space.navBarEnd))
        items.forEachIndexed { i, item ->
            val isSelected = i == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(i)
                },
                icon = { Icon(painterResource(item.icon), contentDescription = null, modifier = Modifier.size(Size.icon)) },
                label = {
                    CappedFontScale {
                        BasicText(
                            item.label, style = KeyholeType.caption.copy(color = LocalContentColor.current, textAlign = TextAlign.Center), maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = NavLabelMinSize, maxFontSize = KeyholeType.caption.fontSize),
                        )
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
