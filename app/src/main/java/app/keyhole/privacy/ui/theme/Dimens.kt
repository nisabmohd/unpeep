package app.keyhole.privacy.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * Spacing, sizes and the shape rule (docs/TOKENS.md).
 * Two shapes only: 24 dp surfaces, and circles/pills for everything small or tappable.
 * Never write a raw dp in a screen: use the names below. Space.* is a gap, Size.* is a size.
 */
object Space {
    val grid = 4.dp
    val screen = 16.dp        // side margin of every screen
    val block = 12.dp         // between blocks on a page
    val row = 2.dp            // between rows of one group
    val card = 16.dp          // inner padding of rows, boxes, tiles, notes
    val heading = 24.dp       // where heading text starts: tab titles, sheet titles, section labels
    /** Heading text sits this far inside the 16 dp content edge, so every heading starts 24 dp from the screen edge. */
    val headingInset = 8.dp
    val inline = 8.dp         // between buttons, chips or tags in one row
    val small = 4.dp          // a title → its line; an icon → its label in a tag
    val dock = 28.dp          // under the docked buttons when there is no system bar inset
    val sectionTop = 20.dp    // above a section label
    val sectionBottom = 8.dp  // a section label → its group
    val lensTop = 24.dp       // above a lens (header, progress, Start)
    val lensBottom = 20.dp    // a lens → its title
    val navBarEnd = 8.dp      // at each end of the bottom nav
    val empty = 40.dp         // above an empty state
    val tileGap = 12.dp       // between More tiles
    val welcomeTop = 72.dp    // above Welcome's mark
    val welcomeTitleTop = 36.dp   // Welcome's picture → its title
    val welcomeTitleInset = 48.dp // each side of Welcome's title, so it breaks as 01 does
    val barGap = 3.dp         // between waiting dots
    val meterGap = 4.dp       // between the magnet's bars
}

object Size {
    val row = 72.dp
    val rowOneLine = 60.dp
    val iconCircle = 44.dp
    val iconInCircle = 22.dp
    val icon = 24.dp           // header buttons and nav
    val chevron = 20.dp
    val iconSmall = 16.dp      // tags and chips
    val iconButton = 48.dp
    val button = 52.dp
    val buttonIcon = 20.dp
    val chip = 36.dp
    val tag = 26.dp
    val topBar = 72.dp
    val nav = 80.dp
    val tile = 120.dp          // the least height of a More tile
    val handle = 32.dp         // a sheet's drag handle (4 dp high: Space.grid)
    val handleArea = 12.dp
    val radio = 24.dp          // ChoiceRow's radio
    val radioDot = 12.dp
    val badge = 18.dp          // the ✓ on a finished check
    val badgeIcon = 12.dp
    val checkSlot = 62.dp      // one check in the check row
    val checkRing = 54.dp      // the ring around the current check
    val lineIcon = 24.dp       // a sign's icon in a one-line row (Why)
    /** Lines drawn around things: an outlined chip (1), a ring (radio, current check: 2). */
    val outline = 1.dp
    val ring = 2.dp
    /** Medium and wider screens: content never stretches past this; it sits centred. */
    val contentMax = 640.dp
    val emptyLine = 330.dp     // an empty state's or header's line never runs wider
    val logo = 96.dp           // the mark on Welcome and About
    val railMark = 40.dp       // the mark at the top of the navigation rail (T01)
    val startMark = 72.dp      // the mark alone on a blank page while the app starts (02h)
    val swatch = 48.dp         // an accent swatch (Appearance)
    val meter = 48.dp          // the magnet's bars at their tallest (08n, 08o)
    val meterBar = 4.dp        // a bar at normal: a flat dash
    val shutter = 72.dp        // the viewfinder's shutter
    val shutterInner = 58.dp
    val glintRing = 40.dp      // the ring around a possible lens
    val glintGlow = 72.dp      // the soft glow behind that ring
    val photo = 96.dp          // a proof photo's thumbnail
    val photoSmall = 72.dp     // a photo in the answer sheet, and the tile that takes one
    val dash = 4.dp            // a dashed outline's dash and gap
    val noteCircle = 40.dp     // a Note's icon circle
    val toast = 56.dp          // the least height of a Toast
    val topBarActions = 200.dp // the most a top bar's actions take, so its title always shows
    val focusRing = 3.dp       // the keyboard-focus ring (00-focus-states)
    val pressHalo = 6.dp       // the halo a pressed check circle spreads
    val focusGap = 2.dp        // between a small shape and its focus ring
    val switchWidth = 52.dp
    val dot = 8.dp
    val waitingDot = 6.dp      // WaitingDots
    val mark = 40.dp           // a checklist item's answer mark
    val progressLine = 6.dp    // the checklist's progress line
    val field = 56.dp          // a text field's least height
    val logoSmall = 44.dp      // the mark beside "Keyhole" on Welcome
    val swatchCheck = 22.dp
    val art = 300.dp           // Welcome's picture
    val artLens = 220.dp
    val artCore = 104.dp
    val artCheck = 36.dp       // a check's circle in Welcome's picture
    // Where the three check circles sit, from the picture's centre (01-welcome).
    val artCheckLens = 110.dp to (-60).dp
    val artCheckBluetooth = (-74).dp to 48.dp
    val artCheckMagnet = (-34).dp to 120.dp
    val floatCard = 200.dp     // a floating card on Welcome's picture (its widest)
    val cardShadow = 6.dp      // a floating card's or a lifted row's shadow
}

/** The lens drawing (Lens.kt): diameters of the outer ring and of the core, and its strokes. */
object LensSize {
    val start = 236.dp
    val startCore = 136.dp
    val progress = 220.dp
    val progressCore = 124.dp
    val header = 100.dp          // as the PNGs draw it (05, 10, 16)
    val headerCore = 64.dp
    val ringStroke = 1.5.dp
    val arcStroke = 5.dp
    val glint = 10.dp          // the dot on the Start's ring
    val arcDot = 12.dp         // the dot riding the progress arc
    val arcDotHole = 5.dp
    val startIcon = 36.dp
    val coreGap = 6.dp         // the core → the innermost ring
    val shadow = 10.dp
    val coreShadow = 6.dp      // under the progress lens's white core
}

object Shapes {
    val surface = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = CircleShape
    /** The mark's tile (About, Welcome): the app icon's corner, 112/512 of its side. */
    fun logo(side: androidx.compose.ui.unit.Dp) = RoundedCornerShape(side * 112f / 512f)
    /** A proof photo: a picture inside a 24 dp surface, so a step smaller. */
    val photoCorner = 20.dp
    val photo = RoundedCornerShape(photoCorner)
    /** Grouped list: 24 dp on the outside of the group, 8 dp between its rows. */
    fun grouped(index: Int, count: Int): RoundedCornerShape {
        val top = if (index == 0) 24.dp else 8.dp
        val bottom = if (index == count - 1) 24.dp else 8.dp
        return RoundedCornerShape(top, top, bottom, bottom)
    }
}
