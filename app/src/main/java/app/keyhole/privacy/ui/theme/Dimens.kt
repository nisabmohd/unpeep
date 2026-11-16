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
    val choiceCircle = 10.dp  // above and below a ChoiceRow with a circle
    val tag = 10.dp           // each side of a Tag's words
    val choice = 6.dp         // above and below the words of a ChoiceRow without a circle
    val sheetEmpty = 40.dp    // under a sheet's "nothing matches" words
    val dock = 28.dp          // under the docked buttons when there is no system bar inset
    val sectionTop = 24.dp    // above a section label
    val sectionBottom = 16.dp // a section label → its group
    val lensTop = 24.dp       // above a lens (header, progress, Start)
    val lensBottom = 20.dp    // a lens → its title
    val navBarEnd = 8.dp      // at each end of the bottom nav
    val empty = 44.dp         // above an empty state: with the lens header's own 12 dp, 56 dp under a top bar
    val emptyUnderRow = 36.dp // the same under the sweep's check row: 48 dp in all
    val emptyUnderSummary = 28.dp // and under a results summary box: 40 dp in all
    val none = 0.dp
    val summaryTop = 20.dp    // a SummaryBox: above its count (04)
    val summaryBar = 13.dp    // its sentence → its bar
    val summaryLegend = 14.dp // its bar → its legend
    val tileGap = 12.dp       // between More tiles
    val welcomeTop = 72.dp    // above Welcome's mark
    val welcomeTitleTop = 36.dp   // Welcome's picture → its title
    val welcomeTitleInset = 48.dp // each side of Welcome's title, so it breaks as 01 does
    val barGap = 3.dp         // between waiting dots, and between a summary bar's stretches
    val note = 14.dp          // above and below a Note's words (a two-line Note is 68 dp)
    val meterGap = 8.dp       // between the magnet's bars
    val meterLabels = 10.dp   // the Find it meter's pills → the words under them
    val shutterSide = 40.dp   // each side of the shutter row (08)
    val tagIcon = 8.dp        // before a Tag's icon
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
    val choiceRowCircle = 64.dp // a ChoiceRow with a circle and a line (06, 09b)
    val choiceRow = 52.dp      // a ChoiceRow without a circle (a long list: Language, Help numbers)
    val choiceRowLines = 56.dp // the same with a line under its title
    val radio = 24.dp          // ChoiceRow's radio
    val radioDot = 12.dp
    val badge = 20.dp          // the ✓ on a finished check: 16 dp of primary inside a 2 dp rim of the box's colour
    val badgeOut = 5.dp        // how far that ✓ hangs outside its circle, down and to the end
    val badgeIcon = 12.dp
    val checkSlot = 62.dp      // one check in the check row
    val checkRing = 54.dp      // the ring around a focused check on Home
    val checkCircle = 40.dp    // a check in a progress row (sweep, report); its ring sits 2 dp outside it
    val checkCircleMin = 36.dp // the smallest a progress row's circle gets before the row scrolls (10l)
    val checkGap = 6.dp        // the least space between two circles of a progress row
    val lineIcon = 24.dp       // a sign's icon in a one-line row (Why)
    /** Lines drawn around things: an outlined chip (1), a ring (radio, current check: 2). */
    val outline = 1.dp
    val ring = 2.dp
    /** Medium and wider screens: content never stretches past this; it sits centred. */
    val contentMax = 640.dp
    val emptyLine = 340.dp     // an empty state's or header's line never runs wider
    val logo = 72.dp           // the mark on About (17)
    val headerIcon = 28.dp     // the icon in a lens header's core
    val railMark = 40.dp       // the mark at the top of the navigation rail (T01)
    val swatch = 44.dp         // an accent swatch; a picked one keeps its size, its ring sits 2 dp outside it (14)
    val segment = 44.dp        // one segment of a Segmented, 4 dp inside its pill
    val switchRowOver = 6.dp   // how far a switch row's 48 dp target hangs over its 42 dp of words (Appearance)
    val none = 0.dp         // an accent swatch (Appearance)
    val meter = 48.dp          // the magnet's bars at their tallest (08n, 08o)
    val meterBar = 4.dp        // a bar at normal: a flat dash
    val shutter = 72.dp        // the viewfinder's shutter
    val finderPanel = 288.dp   // the lens finder's control panel under the picture (final v2)
    val panelCorner = 28.dp    // its top corners (the sheets' corners)
    val finderPanelSide = 272.dp // the same panel on the trailing side, on its side (T05)
    val shutterInner = 58.dp
    val glintRing = 40.dp      // the ring around a possible lens
    val glintGlow = 72.dp      // the soft glow behind that ring
    val photo = 96.dp          // a proof photo's thumbnail
    val photoSmall = 72.dp     // a photo in the answer sheet, and the tile that takes one
    val dash = 4.dp            // a dashed outline's dash and gap
    val noteCircle = 40.dp     // a Note's icon circle
    val toast = 56.dp          // the least height of a Toast
    val topBarActions = 200.dp // the most a top bar's actions take, so its title always shows
    val actionOverhang = 28.dp // a text action's 48 dp target, less the 20 dp line it sits on (SectionLabel)
    val focusRing = 3.dp       // the keyboard-focus ring (00-focus-states)
    val pressHalo = 6.dp       // the halo a pressed check circle spreads
    val focusGap = 2.dp        // between a small shape and its focus ring
    val switchWidth = 52.dp
    val dot = 8.dp
    val waitingDot = 6.dp      // WaitingDots
    val mark = 36.dp           // a checklist item's answer mark
    val progressLine = 4.dp    // the checklist's progress line, the moving line
    val summaryBar = 6.dp      // a SummaryBox's bar
    val field = 56.dp          // a note box's least height
    val fieldLine = 52.dp      // a one-line field: the search pill, a name (15)
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
    val progressLarge = 248.dp     // when the core holds an icon, a value and a line (Find it, Magnet)
    val progressLargeCore = 152.dp
    val progressLargeArc = 98.dp   // its arc's radius (07, 08n)
    val header = 100.dp          // as the PNGs draw it (05, 10, 16)
    val headerCore = 64.dp
    val ringStroke = 1.5.dp
    val arcStroke = 4.dp
    val glint = 10.dp          // the dot on the Start's ring
    val arcDot = 13.dp         // the dot riding the progress arc: a 3 dp line around a 7 dp hole
    val arcDotHole = 7.dp
    val startIcon = 36.dp
    val coreGap = 6.dp         // the core → the innermost ring (the big lenses)
    val headerRing = 7.25.dp   // the same in a lens header: its inner ring is 80 dp across
    val shadow = 10.dp
    val coreShadow = 6.dp      // under the progress lens's white core
}

object Shapes {
    val surface = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sidePanel = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp) // the lens finder's panel on its side (T05)
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
