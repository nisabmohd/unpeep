# TODO

Pick the next unticked item. Build screen by screen against `design/screens/NN-*.png` (skill `build-screen`).
Tick it and commit after each one.

## Week 0 · Set up
- [x] Copy Burrow's Gradle setup, catalog, flavors play/foss/github, signing, CI and release workflows (secrets KEYHOLE_KEYSTORE_*)
- [x] Theme (Color, Type, Dimens, Motion), fonts, AppContainer, settings store
- [x] Components: Lens, CheckRow, rows, buttons, tags, chips, ChoiceRow, KeyholeSheet, Note, top bars
- [x] Nav shell: Check · Reports · More, stand-in screen for every route not built yet
- [x] Run `tools/keyhole_mark.py` (launcher, notification, store icon) and `tools/solar_icon.py` (icons); strings in 7 languages
- [x] Signatures in `app/src/main/assets/signatures/` (format: community-signatures/README.md)
- [x] Tests first: LocalOnly, Risk, MagnetMeter, ProjectRules (rules a machine can check from CLAUDE.md)
- [x] First build on a device: `./gradlew :app:installPlayDebug` (emulator, API 35; a real phone still to do)
- [ ] Buy a test camera (V380 or Tapo) and, if possible, a cheap "charger cam"

## Week 1–2 · Wi-Fi
- [x] Subnet from `LinkProperties`; sweep (≤ 64 sockets, 400 ms) through `LocalOnly`
- [x] mDNS (`NsdManager`), SSDP + device XML, ONVIF WS-Discovery (MulticastLock), ports, RTSP OPTIONS, HTTP banner
- [x] Screen 03 Sweep in progress · 04 Wi-Fi results · 05 Device · 06 Not a camera? (sheet)
- [x] Client-isolation note
- [x] Icons for known kinds (computer, speaker, printer, console, storage, smart home) via `tools/solar_icon.py`
- [ ] Test on home Wi-Fi, a phone hotspot and the test camera (so far: emulator Wi-Fi with a stand-in camera on the host)
- [ ] Design review: "Other devices" list (behind "n known devices"), the "Not on Wi-Fi" state, the "not recognised"
      group and legend entry
- [ ] "Marked as known" is kept in memory until Room (Week 5); More › Marked as known still counts 0

## Week 3 · Hotspots + Bluetooth
- [x] Wi-Fi scan results + hotspot patterns; throttle (4 per 2 min) with "Updated Xs ago"
- [x] BLE: trackers by service data / Apple Find My frame, cameras by name (`BleScanner`, `judgeBle`)
- [x] Screen 07 Find it
- [x] Permission asked when the check starts, one line why; "Open settings" after Don't allow; Wi-Fi / Bluetooth /
      Location off and "no Bluetooth" states
- [x] Location for Hotspots on every Android version (Android's rule for Wi-Fi names; decided 16 Nov 2026, PLAN.md § Permissions)
- [x] On-device tests: `ComponentsTest`, `ScreensTest`, `SmokeTest` (`./gradlew :app:connectedPlayDebugAndroidTest`)
- [ ] Verify each tracker family on a real tag (AirTag, Tile, SmartTag, Chipolo, Find Hub); check whether
      `neverForLocation` hides Find Hub (Eddystone) frames (docs/LIMITS.md)
- [ ] Test Hotspots near a real camera hotspot (V380 / A9) and Find it while walking
- [ ] Design review: Hotspot and Bluetooth results (built as 04), their "Other networks / Bluetooth devices" lists,
      the permission and switched-off states, Find it's "Not heard for a while" note and Weaker / Steady tags
- [ ] Design review: 16 Privacy says "Location, Android 12 and older" and "Nearby devices and Wi-Fi"; Hotspots now
      asks for Location on every version, so that card needs new words before Week 6
- [ ] "Add to report" is kept in memory until the report exists (Week 5)

## Week 4 · Lens, Infrared, Magnet
- [ ] Ask: CameraX or Camera2 (CLAUDE.md › Ask before adding any dependency)
- [ ] Lens analyzer (Y plane, drop frames), filters No filter · Red · Contrast, 2× zoom, proof photos
- [ ] Infrared mode (front camera, TV-remote test first)
- [ ] Screen 08 Lens finder · 08 Magnet (`MagnetSensor` feeding `MagnetMeter`, beep)

## Week 5 · Checklist, Report
- [ ] Checklist per place (Stay, Trial room, Washroom, Office, Car): write, then translate
- [ ] Room: reports, findings, photos, marked as known
- [ ] Screen 09 Checklist · 10 Report (PDF export) · 11 If you find a camera · 12 Reports · 15 Help numbers

## Week 6 · More, release
- [ ] 01 Welcome + permission sheets · 13 More · 14 Appearance · 16 Privacy · 17 About · 18 Licences
- [ ] Accessibility pass (48 dp, TalkBack, 200% font, 360 dp width), dark mode pass
- [ ] Baseline profile, APK < 5 MB, release/listing.md, data safety, store shots, F-Droid, v0.1.0 tag

## Later
- Quick Settings tile "Start sweep", home-screen widget
- Phone check: apps with camera, mic and location access
- Trackers seen in more than one place (watchlist)
