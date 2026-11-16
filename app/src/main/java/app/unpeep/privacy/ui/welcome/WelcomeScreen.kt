package app.unpeep.privacy.ui.welcome

import app.unpeep.privacy.ui.theme.checkColors
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.ui.hue
import app.unpeep.privacy.ui.icon
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import app.unpeep.privacy.R
import app.unpeep.privacy.ui.components.CircleTone
import app.unpeep.privacy.ui.components.Dock
import app.unpeep.privacy.ui.components.FloatingCard
import app.unpeep.privacy.ui.components.LensRings
import app.unpeep.privacy.ui.components.PrimaryButton
import app.unpeep.privacy.ui.theme.Hues
import app.unpeep.privacy.ui.theme.UnpeepTheme
import app.unpeep.privacy.ui.theme.UnpeepType
import app.unpeep.privacy.ui.theme.LensSize
import app.unpeep.privacy.ui.theme.Shapes
import app.unpeep.privacy.ui.theme.Size
import app.unpeep.privacy.ui.theme.Space
import androidx.compose.ui.graphics.Color

/**
 * The mark on its tile (Welcome, About, the navigation rail). The tile follows the theme: the brand blue with the
 * default colours in light, the accent or the phone's dynamic colour otherwise, as a themed launcher icon does.
 */
@Composable
fun MarkTile(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Box(modifier.size(size).clip(Shapes.logo(size)).background(c.primary).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        // The adaptive icon's one-colour layer is 108 units with the mark 48 across in its middle: drawn 1.5×, the mark is two thirds of the tile.
        Icon(painterResource(R.drawable.ic_launcher_monochrome), null, tint = c.onPrimary, modifier = Modifier.requiredSize(size * MARK_SCALE))
    }
}

private const val MARK_SCALE = 1.5f
private const val TILT = -4f

/**
 * 01 Welcome (first run): the mark · a picture of what Unpeep does · what it is in two lines · Get started.
 * Permissions are asked later, when each check starts.
 */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.weight(1f).widthIn(max = Size.contentMax).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Space.heading))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MarkTile(Size.logoSmall)
                Spacer(Modifier.width(Space.card))
                Text(stringResource(R.string.app_name), style = UnpeepType.title, color = c.onSurface)
            }
            Spacer(Modifier.height(Space.heading))
            // The picture: the lens, a likely camera and a count of devices. Silent for TalkBack: the words say it.
            Box(Modifier.fillMaxWidth().height(Size.art).clip(Shapes.surface).background(c.primaryContainer).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                // Two wider rings run off the picture's edges, as the room goes on past what the phone sees.
                val ring = c.outlineVariant
                Canvas(Modifier.size(Size.artLens)) {
                    val stroke = Stroke(LensSize.ringStroke.toPx())
                    for (k in ART_OUTER_RINGS) drawCircle(ring, radius = size.minDimension / 2 * k, style = stroke)
                }
                LensRings(Size.artLens, Size.artCore, middleRing = true, glint = true) {
                    Box(Modifier.size(Size.artCore).clip(Shapes.pill).background(c.primary), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_radar), null, tint = c.onPrimary, modifier = Modifier.size(LensSize.startIcon))
                    }
                }
                // Three of the checks, sitting on the rings.
                ArtCheck(Check.LENS, Modifier.offset(Size.artCheckLens.first, Size.artCheckLens.second))
                ArtCheck(Check.BLUETOOTH, Modifier.offset(Size.artCheckBluetooth.first, Size.artCheckBluetooth.second))
                ArtCheck(Check.MAGNET, Modifier.offset(Size.artCheckMagnet.first, Size.artCheckMagnet.second))
                FloatingCard(
                    painterResource(R.drawable.ic_camera_device), CircleTone.Alert, stringResource(R.string.welcome_art_camera), stringResource(R.string.risk_likely_camera), TILT,
                    Modifier.align(Alignment.TopStart).padding(Space.card),
                )
                FloatingCard(
                    painterResource(R.drawable.ic_wifi), CircleTone.Hue(Hues.WIFI), pluralStringResource(R.plurals.welcome_art_devices, ART_DEVICES, ART_DEVICES), stringResource(R.string.welcome_art_on_wifi), -TILT / 2,
                    Modifier.align(Alignment.BottomEnd).padding(Space.card),
                )
            }
            Spacer(Modifier.height(Space.welcomeTitleTop))
            Text(stringResource(R.string.welcome_title), style = UnpeepType.display, color = c.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.welcomeTitleInset).semantics { heading() })
            Spacer(Modifier.height(Space.block))
            Text(stringResource(R.string.welcome_line), style = UnpeepType.body, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.headingInset))
            Spacer(Modifier.height(Space.heading))
        }
        Dock(Modifier.widthIn(max = Size.contentMax)) {
            PrimaryButton(stringResource(R.string.get_started), onStart, Modifier.fillMaxWidth())
            Text(stringResource(R.string.welcome_footer), style = UnpeepType.supporting, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

private const val ART_DEVICES = 18

/** The wider rings, as a share of the lens's own outer ring. */
private val ART_OUTER_RINGS = listOf(1.5f, 1.85f)

/** A check's circle in the picture: its hue, its icon, smaller than in a row. */
@Composable
private fun ArtCheck(check: Check, modifier: Modifier = Modifier) {
    val colors = checkColors(check.hue)
    Box(modifier.size(Size.artCheck).clip(Shapes.pill).background(colors.container), contentAlignment = Alignment.Center) {
        Icon(painterResource(check.icon), null, tint = colors.onContainer, modifier = Modifier.size(Size.iconSmall))
    }
}

@Preview
@Composable
private fun WelcomePreview() = UnpeepTheme { WelcomeScreen {} }
