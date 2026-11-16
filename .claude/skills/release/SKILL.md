---
name: release
description: Ship an Unpeep version to GitHub, F-Droid and Google Play. Use when the user asks to release or tag a version.
---

# Release

Follow `docs/RELEASING.md` exactly. Before tagging, run the `verify-app` skill. Never tag with a failing test,
an APK ≥ 5 MB, or a CHANGELOG without the version's section. Local artifacts go in `build/dist/v<version>/`.
