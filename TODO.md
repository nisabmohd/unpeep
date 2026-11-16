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
- [ ] Subnet from `LinkProperties`; sweep (≤ 64 sockets, 400 ms) through `LocalOnly`
- [ ] mDNS (`NsdManager`), SSDP + device XML, ONVIF WS-Discovery (MulticastLock), ports, RTSP OPTIONS, HTTP banner
- [ ] Screen 03 Sweep in progres/mos · 04 Wi-Fi results · 05 Device · 06 Not a camera? (sheet)
- [ ] Client-isolation note; test on home Wi-Fi, a phone hotspot and the test camera

## Week 3 · Hotspots + Bluetooth
- [ ] Wi-Fi scan results + hotspot patterns; throttle (4 per 2 min) with "Updated Xs ago"
- [ ] BLE: trackers by service data / Apple Find My frame, cameras by name; verify on real tags
- [ ] Screen 07 Find it

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
