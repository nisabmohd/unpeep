# Motion

All in `ui/theme/Motion.kt`; nowhere else (`ProjectRulesTest`).
- Push/pop: shared axis X (10% shift + fade), 300 ms emphasized easing. Predictive back on.
- Tabs: fade through.
- Sheets: Material 3 default.
- Sweep progress lens: the arc eases to each new value (standard 300 ms); the dot rides the arc.
- Find it / Magnet bars: new bar slides in from the right; no bouncing.
- Check row: the current step's ring fades in; the ✓ badge scales in once.
- Respect "Remove animations": durations become 0 through the system scale.
