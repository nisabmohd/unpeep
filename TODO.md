# TODO

Pick the next unticked item. Build screen by screen against `design/screens/NN-*.png` (skill `build-screen`).
Tick it when built and tested; the owner reviews and commits.

## Week 0 · Set up
- [x] Copy Burrow's Gradle setup, catalog, flavors play/foss/github, signing, CI and release workflows (secrets KEYHOLE_KEYSTORE_*)
- [x] Theme (Color, Type, Dimens, Motion), fonts, AppContainer, settings store
- [x] Components: Lens, CheckRow, rows, buttons, tags, chips, ChoiceRow, KeyholeSheet, Note, top bars
- [x] Nav shell: Check · Reports · More
- [x] Run `tools/keyhole_mark.py` (launcher, notification, store icon) and `tools/solar_icon.py` (icons); strings in 7 languages
- [x] Signatures in `app/src/main/assets/signatures/` (format: community-signatures/README.md)
- [x] Tests first: LocalOnly, Risk, MagnetMeter, ProjectRules (rules a machine can check from CLAUDE.md)
- [x] First build on a device: `./gradlew :app:installPlayDebug` (emulator, API 35)

## Week 1–2 · Wi-Fi
- [x] Subnet from `LinkProperties`; sweep (≤ 64 sockets, 400 ms) through `LocalOnly`
- [x] mDNS (`NsdManager`), SSDP + device XML, ONVIF WS-Discovery (MulticastLock), ports, RTSP OPTIONS, HTTP banner
- [x] Screen 03 Sweep in progress · 04 Wi-Fi results · 05 Device · 06 Not a camera? (sheet)
- [x] Client-isolation note
- [x] Icons for known kinds (computer, speaker, printer, console, storage, smart home)

## Week 3 · Hotspots + Bluetooth
- [x] Wi-Fi scan results + hotspot patterns; throttle (4 per 2 min) with "Updated Xs ago"
- [x] BLE: trackers by service data / Apple Find My frame, cameras by name
- [x] Screen 07 Find it
- [x] Permission asked when the check starts; "Open settings" after Don't allow; radio-off and "no Bluetooth" states
- [x] Location for Hotspots on every Android version (decided 16 Nov 2026, PLAN.md § Permissions)
- [x] On-device tests: `ComponentsTest`, `ScreensTest`, `SmokeTest`

## Week 4 · Lens, Infrared, Magnet
- [x] Camera2, no library (decided 16 Nov 2026)
- [x] Lens analyzer (Y plane, drop frames), filters No filter · Red · Contrast, 2× zoom, proof photos
- [x] Infrared mode (front camera, brighter exposure, TV-remote test first)
- [x] Screen 08 Lens finder · 08 Magnet (`MagnetSensor`, `MagnetMeter`, beep)
- [x] Haptics: one helper (`Buzz`), the same feel for the same meaning everywhere

## Week 5 · Checklist, Report
- [x] Checklist per place (Stay, Trial room, Washroom, Office, Car): written and translated
- [x] Room: reports, findings, photos, marks (marks now survive a restart; More › Marked as known lists them)
- [x] Screen 09 Checklist · 10 Report (PDF export, share, rename, Undo) · 11 If you find a camera · 12 Reports · 15 Help numbers
- [x] A visit ends on its report: the end of a sweep, or "This visit so far" on Home for checks run one by one
- [x] Tests: `ReportTest`, `ReportCodesTest`, `HelpNumbersTest`; Week 5 cases in `ScreensTest` and `SmokeTest`

## Week 6 · More, release
- [x] 01 Welcome (first run) · 13 More (help numbers sheet, language sheet, marks count) · 14 Appearance (+ Sweep steps)
      · 16 Privacy (delete everything) · 17 About (GitHub update check, github build only) · 18 Licences (bundled texts)
      · Marked as known · How Keyhole works · What Keyhole can't find
- [x] Permissions explained when each check starts (no separate permission sheets: the check's own screen says why)
- [x] Accessibility tests: 200 % text at 360 dp on Home, Welcome, Report, Checklist, Appearance; every tap ≥ 48 dp and
      named (`AccessibilityTest`); dark mode drawn
- [x] Release text: `fastlane/metadata/android/en-US` (title, short, full, changelog 1), `CHANGELOG.md` § 0.1.0,
      PRIVACY.md, release/data-safety.md
- [x] Release build: R8 full, minify + shrink, APK < 5 MB
- [x] Design brief for the missing designs: `~/Downloads/keyhole-design-brief.zip` (`build/design-brief/`)

## Week 7 · Everything still open
Everything ran on emulators only (Pixel API 35 headless, Medium Phone API 36.1). Nothing has run on a real phone.

### Release (owner)
- [ ] Review and commit Weeks 5–6 (nothing after "camera setup, lens finder screens done" is committed)
- [ ] Commit the baseline profile (generated 16 Nov 2026 on the API 35 emulator: `app/src/main/generated/baselineProfiles`,
      `./gradlew :app:generateBaselineProfile`; journey: Welcome, tabs, a report, Appearance, Privacy, Magnet); regenerate
      on a real phone before release
- [ ] Store graphics: feature graphic 1024 × 500, phone screenshots (once the new designs are built); `tools/store_shots.py`
      doesn't exist yet
- [ ] Play Console: listing from `fastlane/`, data safety from `release/data-safety.md`, content rating
- [ ] F-Droid: submit (metadata is in `fastlane/`); check the foss APK has no Google code (CI does)
- [ ] Tag v0.1.0 (the release workflow reads `CHANGELOG.md` § 0.1.0)
- [ ] Set `PLAY_LISTED = true` once live, so About shows "Rate on Google Play"; wire `PlayReview` (written in
      `src/play`, called nowhere yet) to that row
- [ ] About › "More from mid apps" (Burrow, Weft): needs their icons and store links
- [ ] Lint: dependency-update and AGP-version warnings left as they are (updating needs a decision)

### On a real phone
- [ ] Install and run on a real phone; then on Android 8–9, 11 and 12 (minSdk 26)
- [ ] Haptics: feel every `Buzz`; "found" not too strong or too often; Android's touch-feedback setting turns them off
- [ ] Wi-Fi: home Wi-Fi (a TV comes out Known, this phone "This phone", the router "Router"); the test camera (V380 /
      Tapo, still to buy) comes out Likely camera; mDNS, SSDP and ONVIF have only been unit-tested; a phone hotspot or
      hotel Wi-Fi shows the isolation note; a full /24 finishes under 20 s; "Not on Wi-Fi" on mobile data only
- [ ] Hotspots near a real camera hotspot (V380 / A9 / HDWiFiCam) → Likely camera; Find it while walking towards it;
      the 4-scans-in-2-minutes throttle and "Updated Xs ago"
- [ ] Bluetooth has never heard a real device: verify AirTag / Find My, Tile, SmartTag, Chipolo, Find Hub, Pebblebee;
      check whether `neverForLocation` hides Find Hub (Eddystone) frames
- [ ] Permission paths: Allow; Don't allow; twice → Open settings → back allowed; Location off; Wi-Fi off; Bluetooth off;
      Android 11 and older (Bluetooth asks for Location there)
- [ ] Lens finder: tune the glint thresholds in real dark rooms on 2–3 phones (only synthetic images so far); rings
      line up with the spot, back and front camera (the front camera's mirroring is assumed); torch; 2× (the Android
      8–10 crop path is untested); proof photo quality
- [ ] Infrared: a TV remote on the front camera; real IR LEDs in the dark
- [ ] Magnet near chargers, speakers and smoke detectors; the beep is audible; a phone without a magnetometer
- [ ] Camera busy in another app → Try again; checklist "Add a photo" through the phone's camera app (needs the
      camera permission first: Android's rule)
- [ ] PDF export: open the file on a phone and a computer; long reports split across pages; photos print
- [ ] Update check (github build) against a real release; play and foss never show the row
- [ ] Tablet: Android 16 ignores the lens finder's portrait lock on large screens (picture and rings need the display
      rotation too)

### Words and data
- [ ] Translations (de, es, fr, hi, ja, pt-BR) were written by Claude: a native speaker reads each, especially the
      honesty lines, the checklist and the help steps
- [ ] Help numbers (`assets/help/numbers.json`, 50 countries): check each number and site against an official source;
      add countries by pull request
- [ ] Checklist content per place: read by someone who has checked rooms by hand
- [ ] Database: version 1, schema exported to `app/schemas`; any change from here needs a migration and a test

### Design (delivered 16 Nov 2026: `design/screens/`, 145 PNGs; built against them)
- [x] Shared: Note (five tones), Toast, focus ring, pressed states, checklist icons and hues, WaitingDots, QuietLine,
      StepSlider (text size, as Weft), AddPhotoTile, KeyholeNavRail
- [x] Screens 01–18 and their states (sweep, results, device, find it, lens, magnet, checklist, report, finding
      detail, found camera, reports, more, appearance, privacy, about with More from mid apps)
- [x] Error states: scan limit (E01), Wi-Fi dropped (E03), magnet stopped (E05), results / report gone toasts
      (E06, E13), stop sheet (E07), paused (E08), aeroplane mode (E09)
- [x] Tablet / landscape: rail and two panes from 840 dp (T01–T04), sweep (T06) and lens finder (T05) on their side
- [x] Large text and 360 dp: audited with `DesignShots#overflowAudit`
- [ ] Not built yet: 10f full-screen photo and 10g PDF restyle, 12b/12c search states, 15a no-match, 17a–d update
      sheet states, E10 long names, E14 notes in dark, E16 right-to-left, E17 German check, 02i–k pressed shots
- [ ] Checklist content: the design's item lists differ from the app's in places (e.g. "Key fob and tags", "Wall
      clock"); icons and hues follow the board, items stay as written until someone decides
- [ ] Photos per finding: finding detail can't show its own photos (photos belong to the report, not a finding;
      linking them needs a database change)
- [ ] Run `DesignShots` on a real tablet and in landscape on a phone

## Later
- Quick Settings tile "Start sweep", home-screen widget
- Phone check: apps with camera, mic and location access
- Trackers seen in more than one place (watchlist)
