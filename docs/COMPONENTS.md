# Components

Screens are built only from these (`ui/components`). Missing one? Add it here and in code first, with no
per-screen look options. Two things that look alike are one component.

| Component | File | Where |
|---|---|---|
| `LensRings` | Lens.kt | the lens drawing: rings + glint, or a progress arc with its dot |
| `LensStartButton` | Lens.kt | Home's Start (the only filled hero) |
| `LensProgress` | Lens.kt | the progress lens: an arc and a big value over one line (sweep); with a check's circle above a word or reading (Find it, Magnet); two sizes set by content: 220 (core 124) for a count, 248 (core 152) when the core holds an icon, a value and a line |
| `LensHeader` / `EmptyState` (`EmptyUnder`) | Lens.kt | first block of every detail screen, 12 dp under the top bar, title 16 dp under the lens, line 4 dp under the title (2 dp when tags follow); every empty list (lens header + line), its lens 56 dp under a top bar, 48 dp under the sweep's check row, 40 dp under a summary box |
| `CheckRow` / `CheckIcon` | CheckRow.kt | the seven checks: Home (labels, scrolls); the sweep's steps (`steps`: 40 dp circles edge to edge in a 16 dp box, a ring 2 dp outside the current one, a 20 dp ✓ hanging 5 dp off a finished one, shrinking to 36 dp); the report's row of what ran (44 dp in equal shares) |
| `Grouped` + `groupShape` | Rows.kt | a group of rows, 2 dp apart, 24/8 corners |
| `ListRow` | Rows.kt | the one list row: circle, title, line, trailing, chevron; long press; `alert` title for a destructive row |
| `ValueRow` | Rows.kt | a detail row: muted label left, value right, both 14 sp (Open ports · Maker) |
| `SignRow` | Rows.kt | a reason or step: 24 dp icon with no circle + one sentence (Why) |
| `Tile` | Rows.kt | a More tile: circle, small value, title, line |
| `IconCircle` / `NeutralCircle` | Rows.kt | 44 dp circle with a 22 dp icon |
| `SectionLabel` | Rows.kt | label above a group, optional count or text action (Change); 24 dp above and 16 dp below its line, 12 dp above when it is the first thing under a top bar (`first`) |
| `Box24` | Rows.kt | a tinted box that is not a list ("Or run one check", Find it meter) |
| `Note` (`NoteTone`) | Rows.kt | the one note, five tones (info · good · warning · error · honesty): 40 dp icon circle centred on the text, optional title, one optional text action; still goes first |
| `QuietLine` | Rows.kt | one quiet centred line under a result: what the scan can't see |
| `WaitingDots` | Rows.kt | three dots lighting in turn, at the end of the one row a scan shows while nothing is listed yet |
| `Toast` / `ToastHost` / `ToastState` (`LocalToasts`) | Toast.kt | a short confirmation, only where an action can be undone or something went away; 4 s (longer with TalkBack), 12 dp above the dock or nav, swipe left or right to dismiss, optional Undo |
| `focusRing` | Focus.kt | the one keyboard-focus look: 3 dp primary ring, outside small shapes with a 2 dp gap, inside rows and tiles |
| `ChecklistRow` / `AnswerMarkCircle` | Rows.kt | a checklist item: hued circle, title, line, its answer mark (✓ · ? · − · empty) |
| `NumberCircle` | Rows.kt | a numbered step's circle (If you find a camera) |
| `ProgressLine` | Meter.kt | how far along a list is (checklist) |
| `MovingLine` | Meter.kt | something on its way with no knowable end (asking GitHub): a thin line that keeps moving; never a spinner |
| `KeyholeTextField` | Inputs.kt | the one text field: search pill or note box |
| `PhotoThumb` | Inputs.kt | a proof photo's thumbnail, read off the main thread |
| `AddPhotoTile` | Inputs.kt | takes a photo: a dashed square with a camera-add icon, its label beside it (checklist Not sure) |
| `SummaryBox` | Rows.kt | the count on top of a result list: a 30 sp number beside a 14 sp line, one 6 dp bar split by result, the legend in words (12 sp) |
| `PrimaryButton` / `SecondaryButton` / `DestructiveButton` / `ButtonPair` | Controls.kt | one main action (an arrow after its label when it moves on); the red one for a delete that can't be undone, in its confirm sheet only; pair = secondary left, primary right; labels 15 sp |
| `RoundIconButton` | Controls.kt | round tonal header buttons |
| `Tag` (`TagKind`) | Controls.kt | Likely camera (red) · Worth a look (amber) · Known (grey) · Good (accent: Nothing found, Stronger, 4 of 7) |
| `Chip` | Controls.kt | place chips, filters (Red · Contrast), choices |
| `ChoiceRow` | Controls.kt | pick-one row in sheets, radio on the right; with a circle a full 72 dp row, without one a line of a long list (52 dp, 56 dp with a second line: Language, Help numbers) |
| `KeyholeSwitch` / `SwitchRow` / `SwitchLine` | Controls.kt | the one switch; the whole row toggles; `SwitchLine` is the same without a box, as the first line of a box that holds more |
| `Dock` | Controls.kt | docked bottom action area |
| `SegmentMeter` | Meter.kt | the Find it meter: five pills lit by signal strength, Far · Right here underneath |
| `BarMeter` | Meter.kt | the magnet's bars: one per reading, the newest on the right, accent from the jump up |
| `StepSlider` | Controls.kt | a few steps on one scale: title and the value now, a slider that stops at each (Appearance › Text size, as Weft) |
| `Segmented` / `Segment` | Controls.kt | two or three ways of one thing in one pill (Lens · Infrared) |
| `CameraPreview` | Viewfinder.kt | the camera picture, centre-cropped, in the viewfinder |
| `SpotRings` / `SpotMark` | Viewfinder.kt | rings around possible lenses, "Look here" beside the first |
| `Shutter` | Viewfinder.kt | the viewfinder's shutter (saves a proof photo) |
| `FinderPanel` | Viewfinder.kt | the lens finder's rounded panel under the picture (beside it on its side) holding every control; 288 dp, 28 dp top corners; on its side 272 dp wide on the trailing edge, leading corners round |
| `TabTopBar` / `BackTopBar` | Bars.kt | tab title + actions / back + title + actions |
| `TopBarAction` | Bars.kt | a text action at the end of a top bar (Skip step · Scan again) |
| `TabScreen` / `DetailScreen` | Bars.kt | the frame of a tab / of a pushed screen (bar, scrolling body, dock) |
| `KeyholeNavBar` / `NavBarSpacer` | Bars.kt | bottom nav Check · Reports · More |
| `KeyholeNavRail` | Bars.kt | the same three, down the side of a wide window (from 840 dp), the mark on top |
| `CappedFontScale` | CappedFontScale.kt | caps text scaling inside fixed shapes (nav, lens core) |
| `ReorderList` / `ReorderItem` | Reorder.kt | Sweep steps: grip to drag, circle, title, switch; Move up / down for TalkBack |
| `ColorSwatch` | Controls.kt | an accent to pick: colour circle, ring and ✓ when picked |
| `FloatingCard` | Rows.kt | Welcome's tilted cards (part of the picture, silent for TalkBack) |
| `Haptics` / `Buzz` | Haptics.kt | the one way to buzz: tick · toggle · confirm · reject · found · done (MOTION.md § Haptics) |
| `KeyholeSheet` / `SheetLine` / `SheetEmpty` | KeyholeSheet.kt | every choice, picker and confirm (`pinned`: a search field that stays while the list scrolls); a quiet line under its rows; the words a sheet's list shows when nothing matches |


