# Components

Screens are built only from these (`ui/components`). Missing one? Add it here and in code first, with no
per-screen look options. Two things that look alike are one component.

| Component | File | Where |
|---|---|---|
| `LensRings` | Lens.kt | the lens drawing: rings + glint, or a progress arc with its dot |
| `LensStartButton` | Lens.kt | Home's Start (the only filled hero) |
| `LensProgress` | Lens.kt | the progress lens: an arc and a big value over one line (sweep); with a check's circle above a word or reading (Find it, Magnet) |
| `LensHeader` / `EmptyState` | Lens.kt | first block of every detail screen; every empty list (lens header + line) |
| `CheckRow` / `CheckIcon` | CheckRow.kt | the seven checks: Home (labels, scrolls), sweep and report (no labels) |
| `Grouped` + `groupShape` | Rows.kt | a group of rows, 2 dp apart, 24/8 corners |
| `ListRow` | Rows.kt | the one list row: circle, title, line, trailing, chevron; long press; `alert` title for a destructive row |
| `ValueRow` | Rows.kt | a detail row: muted label left, value right (Open ports · Maker) |
| `SignRow` | Rows.kt | a reason or step: 24 dp icon with no circle + one sentence (Why) |
| `Tile` | Rows.kt | a More tile: circle, small value, title, line |
| `IconCircle` / `NeutralCircle` | Rows.kt | 44 dp circle with a 22 dp icon |
| `SectionLabel` | Rows.kt | label above a group, optional count or text action (Change) |
| `Box24` | Rows.kt | a tinted box that is not a list ("Or run one check", Find it meter) |
| `Note` | Rows.kt | the one note (goes first) |
| `ChecklistRow` / `AnswerMarkCircle` | Rows.kt | a checklist item: hued circle, title, line, its answer mark (✓ · ? · − · empty) |
| `NumberCircle` | Rows.kt | a numbered step's circle (If you find a camera) |
| `ProgressLine` | Meter.kt | how far along a list is (checklist) |
| `KeyholeTextField` | Inputs.kt | the one text field: search pill or note box |
| `PhotoThumb` | Inputs.kt | a proof photo's thumbnail, read off the main thread |
| `SummaryBox` | Rows.kt | the count on top of a result list: big number + line, one bar split by result, the legend in words |
| `PrimaryButton` / `SecondaryButton` / `ButtonPair` | Controls.kt | one main action (an arrow after its label when it moves on); pair = secondary left, primary right |
| `RoundIconButton` | Controls.kt | round tonal header buttons |
| `Tag` (`TagKind`) | Controls.kt | Likely camera (red) · Worth a look (amber) · Known (grey) · Good (accent: Nothing found, Stronger, 4 of 7) |
| `Chip` | Controls.kt | place chips, filters (Red · Contrast), choices |
| `ChoiceRow` | Controls.kt | pick-one row in sheets, radio on the right |
| `KeyholeSwitch` / `SwitchRow` | Controls.kt | the one switch; the whole row toggles |
| `Dock` | Controls.kt | docked bottom action area |
| `SegmentMeter` | Meter.kt | the Find it meter: five pills lit by signal strength, Far · Right here underneath |
| `BarMeter` | Meter.kt | the magnet's bars: one per reading, the newest on the right, accent from the jump up |
| `Segmented` / `Segment` | Controls.kt | two or three ways of one thing in one pill (Lens · Infrared) |
| `CameraPreview` | Viewfinder.kt | the camera picture, centre-cropped, in the viewfinder |
| `SpotRings` / `SpotMark` | Viewfinder.kt | rings around possible lenses, "Look here" beside the first |
| `Shutter` | Viewfinder.kt | the viewfinder's shutter (saves a proof photo) |
| `TabTopBar` / `BackTopBar` | Bars.kt | tab title + actions / back + title + actions |
| `TopBarAction` | Bars.kt | a text action at the end of a top bar (Skip step · Scan again) |
| `TabScreen` / `DetailScreen` | Bars.kt | the frame of a tab / of a pushed screen (bar, scrolling body, dock) |
| `KeyholeNavBar` / `NavBarSpacer` | Bars.kt | bottom nav Check · Reports · More |
| `CappedFontScale` | CappedFontScale.kt | caps text scaling inside fixed shapes (nav, lens core) |
| `ReorderList` / `ReorderItem` | Reorder.kt | Sweep steps: grip to drag, circle, title, switch; Move up / down for TalkBack |
| `ColorSwatch` | Controls.kt | an accent to pick: colour circle, ring and ✓ when picked |
| `FloatingCard` | Rows.kt | Welcome's tilted cards (part of the picture, silent for TalkBack) |
| `Haptics` / `Buzz` | Haptics.kt | the one way to buzz: tick · toggle · confirm · reject · found · done (MOTION.md § Haptics) |
| `KeyholeSheet` / `SheetLine` | KeyholeSheet.kt | every choice, picker and confirm; a quiet line under its rows |


