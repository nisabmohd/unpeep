---
name: add-feature
description: Add a new feature or check to Keyhole the right way (plan, rules, design, code, tests, docs). Use for anything not already a screen in docs/SCREENS.md.
---

# Add a feature

1. Check it against CLAUDE.md › "Not in the app" and the hard rules (local network only, detect never intrude,
   no new dependency without asking). If it needs a library or permission, stop and ask.
2. Write it into `docs/PLAN.md` (what, why, limits) and `TODO.md`.
3. Design first: add or change the screen on the canvas, export the PNG to `design/screens/`, add a row to SCREENS.md.
   No PNG yet → TODO.md "Design review: …".
4. Pure logic goes in `domain/` or `sensors/` with unit tests first (like `RiskTest`, `MagnetMeterTest`).
5. Build the UI with the `build-screen` skill. A new check also needs: `Check` enum entry (hue, icon, label),
   Solar icon in `tools/solar_icon.py`, strings in 7 languages, `Place.checks()`, report and "Couldn't check" lines.
6. Update PRIVACY.md and `release/data-safety.md` if anything about data or permissions changed.
