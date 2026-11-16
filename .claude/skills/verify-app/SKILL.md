---
name: verify-app
description: Check a Keyhole build end to end before calling work done or releasing. Use after finishing a screen or feature, and before every release.
---

# Verify the app

1. `./gradlew :app:testPlayDebugUnitTest :app:testFossDebugUnitTest :app:lintPlayDebug`
2. `./gradlew :app:assemblePlayRelease` and check the APK size (< 5 MB) in `app/build/outputs/apk/play/release/`.
3. Merged manifest (`app/build/intermediates/merged_manifest/...`): only the permissions in CLAUDE.md; foss has no
   `com.google` anything.
4. On a device: first run, each tab, each built screen; light and dark; largest font; 360 dp; TalkBack labels.
5. Scans: home Wi-Fi, a phone hotspot (client isolation note), the test camera (must be Likely camera),
   a TV (must be Known). Lens/IR in a dark room. Magnet near a charger.
6. Wording pass: no "safe", no "spy", every result has a reason, every report has Couldn't check.
