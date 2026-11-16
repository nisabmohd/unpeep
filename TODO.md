# TODO

Pick the next unticked item. Build screen by screen against `design/screens/NN-*.png` (skill `build-screen`).
Tick it when built and tested; the owner reviews and commits.

## Week 0 · Set up
- [x] Copy Burrow's Gradle setup, catalog, flavors play/foss/github, signing, CI and release workflows (secrets KEYHOLE_KEYSTORE_*)
- [x] Theme (Color, Type, Dimens, Motion), fonts, AppContainer, settings store
- [x] Components: Lens, CheckRow, rows, buttons, tags, chips, ChoiceRow, KeyholeSheet, Note, top bars
- [x] Nav shell: Check · Reports · More, stand-in screen for every route not built yet
- [x] Run `tools/keyhole_mark.py` (launcher, notification, store icon) and `tools/solar_icon.py` (icons); strings in 7 languages
- [x] Signatures in `app/src/main/assets/signatures/` (format: community-signatures/README.md)
- [x] Tests first: LocalOnly, Risk, MagnetMeter, ProjectRules (rules a machine can check from CLAUDE.md)
- [x] First build on a device: `./gradlew :app:installPlayDebug` (emulator, API 35)
- [ ] Buy a test camera (V380 or Tapo) and, if possible, a cheap "charger cam"

## Week 1–2 · Wi-Fi
- [x] Subnet from `LinkProperties`; sweep (≤ 64 sockets, 400 ms) through `LocalOnly`
- [x] mDNS (`NsdManager`), SSDP + device XML, ONVIF WS-Discovery (MulticastLock), ports, RTSP OPTIONS, HTTP banner
- [x] Screen 03 Sweep in progress · 04 Wi-Fi results · 05 Device · 06 Not a camera? (sheet)
- [x] Client-isolation note
- [x] Icons for known kinds (computer, speaker, printer, console, storage, smart home) via `tools/solar_icon.py`

## Week 3 · Hotspots + Bluetooth
- [x] Wi-Fi scan results + hotspot patterns; throttle (4 per 2 min) with "Updated Xs ago"
- [x] BLE: trackers by service data / Apple Find My frame, cameras by name (`BleScanner`, `judgeBle`)
- [x] Screen 07 Find it
- [x] Permission asked when the check starts, one line why; "Open settings" after Don't allow; Wi-Fi / Bluetooth /
      Location off and "no Bluetooth" states
- [x] Location for Hotspots on every Android version (Android's rule for Wi-Fi names; decided 16 Nov 2026, PLAN.md § Permissions)
- [x] On-device tests: `ComponentsTest`, `ScreensTest`, `SmokeTest` (`./gradlew :app:connectedPlayDebugAndroidTest`)

## Week 4 · Lens, Infrared, Magnet
- [x] Ask: CameraX or Camera2 → Camera2 (decided 16 Nov 2026: no dependency)
- [x] Lens analyzer (Y plane, drop frames), filters No filter · Red · Contrast, 2× zoom, proof photos
- [x] Infrared mode (front camera, brighter exposure, TV-remote test first)
- [x] Screen 08 Lens finder · 08 Magnet (`MagnetSensor` feeding `MagnetMeter`, beep)
- [x] Haptics: one helper (`Buzz`), the same feel for the same meaning everywhere (MOTION.md § Haptics)
- [x] Tests: `GlintsTest`, `CameraSizesTest`, `MagnetSamplerTest`; lens / magnet / haptics cases in `ComponentsTest`,
      `ScreensTest`, `SmokeTest`

## Not tested yet · needs review (Weeks 0–4)
Everything so far ran on one emulator (Pixel, Android 15 / API 35) only. Tick an item when it was done on a real
phone, or reviewed and either fixed or accepted.

### On a real phone (nothing has run on one yet)
- [ ] Install and run on a real phone; then on Android 8–9, 11 and 12 (only API 35 so far; minSdk is 26)
- [ ] Haptics: the emulator doesn't vibrate. Feel every `Buzz` on a phone; check "found" (new spot, magnet jump,
      Find it stronger) isn't too strong or too often; check Android's touch-feedback setting turns them all off

### Wi-Fi (Week 1–2)
- [ ] Home Wi-Fi: real devices found, a TV comes out Known, this phone is "This phone", the router is "Router"
- [ ] The test camera (V380 / Tapo) comes out Likely camera with its reasons (needs the camera: Week 0 item)
- [ ] mDNS, SSDP + device description and ONVIF have only been unit-tested: nothing on the emulator answers them
- [ ] A phone hotspot or hotel Wi-Fi: only the router answers → the client-isolation note shows
- [ ] A full /24 with many devices finishes under 20 s; a guest Wi-Fi with no internet is still the one scanned
- [ ] "Not on Wi-Fi" on a phone on mobile data only

### Hotspots and Bluetooth (Week 3)
- [ ] Hotspots near a real camera hotspot (V380 / A9 / HDWiFiCam) → Likely camera; Find it while walking towards it
- [ ] The 4-scans-in-2-minutes throttle and "Updated Xs ago" on a real phone (the emulator doesn't ration scans)
- [ ] Bluetooth has never heard a real device (the emulator has none). Verify each tracker family on a real tag:
      AirTag / Find My, Tile, SmartTag, Chipolo, Find Hub, Pebblebee
- [ ] Whether `neverForLocation` hides Find Hub (Eddystone) frames (docs/LIMITS.md)
- [ ] Permission paths on a real phone: Allow; Don't allow; Don't allow twice → "Open settings" → back allowed
- [ ] Location switched off → "Location is off"; Wi-Fi off; Bluetooth off; Android 11 and older (Bluetooth asks
      for Location there, untested)

### Lens, Infrared, Magnet (Week 4)
- [ ] Glint thresholds are tuned on synthetic images only: tune in real dark rooms (smoke detector, clock, charger
      cam, a phone camera) on 2–3 phones; count false rings (screws, glass, jewellery)
- [ ] Rings line up with the spot on the picture, back and front camera (the front camera's mirroring is assumed)
- [ ] Torch, 2× zoom (Android 11+ zoom ratio; the crop path for Android 8–10 is untested) and proof photo quality
- [ ] Infrared: a TV remote shows on the front camera; real IR LEDs in the dark; the brighter exposure helps
- [ ] Magnet near chargers, speakers and smoke detectors; the beep is audible; a phone without a magnetometer
- [ ] Camera in use by another app → "Can't open the camera" → Try again; camera permission denied twice → Settings
- [ ] Tablet: Android 16 ignores the portrait lock on large screens, so the picture and rings need the display
      rotation too (test on a tablet or resizable emulator)

### Look and accessibility (all built screens)
- [ ] Dark mode checked on Wi-Fi sweep and Hotspot results only: also Device, Not a camera?, Find it, Magnet, sheets
- [ ] 200 % font (only 150 % checked, on Hotspot results), 360 dp width, landscape on phones (outside the lens finder)
- [ ] TalkBack walk-through of every screen (labels are in, but nobody has listened yet)
- [ ] Right-to-left (pseudo-locale ar-XB) and the longest language (de) on buttons, chips and the check row

### Words
- [ ] Translations (de, es, fr, hi, ja, pt-BR) were written by Claude: have a native speaker read each, especially
      the honesty lines (isolation note, "Nothing nearby looks like…", the magnet note, permission lines)

### Design review (UI with no PNG, or that differs from it)
- [ ] Wi-Fi: "Other devices" list (behind "n known devices"), "Not on Wi-Fi", the "not recognised" group and legend
- [ ] Hotspot and Bluetooth results (built like 04) and their "Other networks / Bluetooth devices" lists
- [ ] Permission and switched-off states (Wi-Fi, Bluetooth, Location, no Bluetooth) in the sweep
- [ ] Find it: "Not heard for a while" note, Weaker / Steady tags; the meter follows the words, so −48 dBm reads
      "Right here" where the PNG says "Close"
- [ ] Lens finder: camera permission / in use / no camera states, tips sheet, TV-remote sheet, "may not see infrared"
      note; the torch's on colour (the shared round button uses primaryContainer, the PNG draws primary)
- [ ] Magnet: the learning state (percent learned)
- [ ] 16 Privacy says "Location, Android 12 and older" and "Nearby devices and Wi-Fi": Hotspots now asks for Location
      on every version, so that card needs new words (Week 6)
- [ ] 18 Licences lists CameraX; the app uses Camera2 (part of Android), so that row goes (Week 6)

### Not finished on purpose (lands in a later week)
- [ ] Marks ("Not a camera?"), "Add to report", proof photos and magnet readings live in memory until reports are
      saved (Week 5); More › Marked as known still counts 0; the photo files themselves are already in app storage
- [ ] Checklist step and the screens behind More still show the "on its way" stand-in (Weeks 5–6)
- [ ] Baseline profile not regenerated since the new screens (Week 6)
- [ ] Lint: 12 dependency-update and 3 AGP-version warnings left as they are (updating needs a decision)

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
