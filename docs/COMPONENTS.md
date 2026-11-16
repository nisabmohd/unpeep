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
| `ListRow` | Rows.kt | the one list row: circle, title, line, trailing, chevron |
| `ValueRow` | Rows.kt | a detail row: muted label left, value right (Open ports · Maker) |
| `SignRow` | Rows.kt | a reason or step: 24 dp icon with no circle + one sentence (Why) |
| `Tile` | Rows.kt | a More tile: circle, small value, title, line |
| `IconCircle` / `NeutralCircle` | Rows.kt | 44 dp circle with a 22 dp icon |
| `SectionLabel` | Rows.kt | label above a group, optional count/action |
| `Box24` | Rows.kt | a tinted box that is not a list ("Or run one check", Find it meter) |
| `Note` | Rows.kt | the one note (goes first) |
| `SummaryBox` | Rows.kt | the count on top of a result list: big number + line, one bar split by result, the legend in words |
| `PrimaryButton` / `SecondaryButton` / `ButtonPair` | Controls.kt | one main action (an arrow after its label when it moves on); pair = secondary left, primary right |
| `RoundIconButton` | Controls.kt | round tonal header buttons |
| `Tag` (`TagKind`) | Controls.kt | Likely camera (red) · Worth a look (amber) · Known (grey) · Good (accent: Nothing found, Stronger, 4 of 7) |
| `Chip` | Controls.kt | place chips, filters (Red · Contrast), choices |
| `ChoiceRow` | Controls.kt | pick-one row in sheets, radio on the right |
| `KeyholeSwitch` / `SwitchRow` | Controls.kt | the one switch; the whole row toggles |
| `Dock` | Controls.kt | docked bottom action area |
| `SegmentMeter` | Meter.kt | the Find it meter: five pills lit by signal strength, Far · Right here underneath |
| `TabTopBar` / `BackTopBar` | Bars.kt | tab title + actions / back + title + actions |
| `TopBarAction` | Bars.kt | a text action at the end of a top bar (Skip step · Scan again) |
| `TabScreen` / `DetailScreen` | Bars.kt | the frame of a tab / of a pushed screen (bar, scrolling body, dock) |
| `KeyholeNavBar` / `NavBarSpacer` | Bars.kt | bottom nav Check · Reports · More |
| `CappedFontScale` | CappedFontScale.kt | caps text scaling inside fixed shapes (nav, lens core) |
| `KeyholeSheet` / `SheetLine` | KeyholeSheet.kt | every choice, picker and confirm; a quiet line under its rows |

To add (with their screens): `BarMeter` (Magnet bars), `Viewfinder` (lens/IR overlay + shutter),
`ChecklistItem` (Looks fine · Not sure · Skip), `PhotoThumb`, `ReorderRow` (dotted grip + switch), `ColorSwatch`.
