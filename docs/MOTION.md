# Motion

All in `ui/theme/Motion.kt`; nowhere else (`ProjectRulesTest`).
- Push/pop: shared axis X (10% shift + fade), 300 ms emphasized easing. Predictive back on.
- Tabs: fade through.
- Sheets: Material 3 default.
- Sweep progress lens: the arc eases to each new value (standard 300 ms); the dot rides the arc.
- Find it / Magnet bars: new bar slides in from the right; no bouncing.
- Check row: the current step's ring fades in; the ✓ badge scales in once.
- Respect "Remove animations": durations become 0 through the system scale.

## Haptics
One meaning, one feel (`ui/components/Haptics.kt`, `Buzz`); never a buzz for its own sake, never in a loop.
- **Tick:** picking one of a few (chips, segments, choice rows, sheet choices).
- **Toggle on / off:** torch, 2×, beep, a sweep step's switch.
- **Confirm:** something kept: proof photo saved, added to the report, marked as known.
- **Reject:** it didn't work: camera won't open, a photo couldn't be saved.
- **Found** (strongest): worth attention: a new possible lens ringed, the magnet jumps, a scan ends with a likely camera
  or something worth a look, Find it gets clearly stronger.
- **Done:** a scan ends with nothing that needs a look.
Android's touch-feedback setting turns them all off.
