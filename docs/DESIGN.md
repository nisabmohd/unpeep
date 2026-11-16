# Design rules

The canvas "Keyhole — app design" and `design/screens/*.png` are the source of truth. This file is why it looks that way.

## 1. Family, not a copy
Keyhole looks made by the same person as Burrow and Weft (mid apps), with its own face.
- **Shared with the family:** big tab title on the left + round tonal header buttons · bottom nav of three
  (Check · Reports · More) · grouped tinted rows, 2 dp apart, 24 dp outside / 8 dp inside corners, no dividers ·
  sentence-case section labels · secondary + primary button pair docked at the bottom · Weft-style sheets
  (handle, title left, round close, radio choice rows) · Burrow's More tiles and About order · the same Welcome layout.
- **Keyhole's own:** the **lens** (concentric rings + a glint, from the logo) on Start, the sweep progress, Find it,
  Magnet and every detail header · the **seven check icons** in one row · dark blue · Lexend.

## 2. Colour
Seed hue **222** (primary hsl(222 70% 30%)); dynamic colour optional (Android 12+). Six accents in Appearance.
Each check keeps its hue **on its icon circle only**, never on text:
Wi-Fi 232 · Hotspots 150 · Bluetooth 268 · Lens 34 · Infrared 338 · Magnet 92 · Checklist 186.
Results are always words + colour: **red only for "Likely camera"** (and errors), **amber for "Worth a look"**,
neutral grey for known devices. Never tint a whole screen.

## 3. Shape and space
4 dp grid · 16 dp screen margin · 24 dp surfaces · circles and pills for everything small or tappable ·
44 dp icon circles with 22 dp icons · rows 72 dp (60 dp with no second line) · buttons 52 dp pills.

## 4. Type
Lexend (Outfit, Atkinson Hyperlegible, Space Grotesk in Appearance), weights 400/500 only, eight styles
(`KeyholeType`): display 32 · headline 30 · title 22 · card title 16 · body 16 · supporting 14 · label 14 · caption 12.

## 5. Structure
- **One hero:** the lens Start on Home. Everything else opens with the **lens header** (small rings, coloured core,
  title, one line, tags).
- **One main action per screen**, docked at the bottom. **No buttons inside cards.**
- **Sheets first:** every choice, picker and confirmation is a `KeyholeSheet`. No dialogs (`ProjectRulesTest`).
- **Notes go first**, in the one `Note`.
- **Every list has an empty state** (lens header + line).

## 6. Honesty is design
- Never "safe", "clean", "no cameras". Say what was checked: "No cameras found on this Wi-Fi".
- Every report has **Couldn't check**.
- Every finding has **Not a camera?** (It's mine · Part of the room · Not sure) and the report lists
  **Marked as known** with Undo. More › Marked as known manages them; "Report it" helps fix the device list.
- The magnet says "Electronics nearby", never "camera".
- No fear words ("spy", "danger", "threat"), no exclamation marks.

## 7. For everyone
No country assumed: help numbers come from the phone's region (sheet to change). Labels never squeeze or overlap:
the check row keeps 62 dp per check and scrolls sideways; labels end in "…". Text grows with the font size; 48 dp
touch targets; every icon button has a TalkBack label.
