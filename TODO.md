# TODO

Pick the next unticked item. Build screen by screen against `design/screens/NN-*.png` (skill `build-screen`).
Tick it when built and tested; the owner reviews and commits.

## Done
Weeks 0–6 (set-up, Wi-Fi, Hotspots, Bluetooth, Lens, Infrared, Magnet, Checklist, Report, More) and every design
round up to **final v3** (`design/screens/`, `design/README.md`): screens 01–18 and their states, error states
E01–E17, tablets and landscape T01–T06, PDF (10g), update sheet (17a–d), toasts for events and Notes for states.
Boards updated 2026-11-16 (sweep check row, Language, Help numbers) are built, and every shared component was
measured against its board at 412 × 892 dp (text lines are full line height now, as the boards draw them).
Tests: unit and rule tests, plus 137 on-device tests (`ComponentsTest`, `ScreensTest`, `AccessibilityTest`, `SmokeTest`).

## On a real phone
Done on a OnePlus (Android 17): Welcome, Home, Appearance, Magnet (real sensor), Lens + photo + Switch camera,
Hotspots with Location, Bluetooth permission and "Bluetooth is off", Checklist, Report, PDF export.
- [ ] Wi-Fi scan on a home network (a TV comes out Known, this phone "This phone", the router "Router"); a test camera
      (V380 / Tapo, still to buy) comes out Likely camera; a phone hotspot or hotel Wi-Fi shows the isolation note; a
      full /24 finishes under 20 s; "Not on Wi-Fi" on mobile data only. mDNS, SSDP and ONVIF are only unit-tested
- [ ] Bluetooth with real devices: AirTag / Find My, Tile, SmartTag, Chipolo, Find Hub, Pebblebee; does
      `neverForLocation` hide Find Hub (Eddystone) frames
- [ ] Hotspots near a real camera hotspot (V380 / A9 / HDWiFiCam) → Likely camera; Find it while walking towards it
- [ ] Haptics: feel every `Buzz`; Android's touch-feedback setting turns them off
- [ ] Lens: tune glint thresholds in real dark rooms on 2–3 phones; rings line up on back and front camera; 2× on
      Android 8–10; landscape on a tablet (Android 16 ignores the portrait lock on large screens)
- [ ] Infrared: a TV remote on the front camera; real IR LEDs in the dark
- [ ] Magnet near chargers, speakers and smoke detectors; the beep is audible; a phone without a magnetometer
- [ ] Permission paths on Android 8–9, 11 and 12: Allow; Don't allow; twice → Open settings; Location, Wi-Fi, Bluetooth off
- [ ] PDF: long reports split across pages; open on a computer; photos print
- [ ] Update check (github build) against a real release; play and foss never show the row
- [ ] `DesignShots` on a real tablet and in landscape on a phone

## Release (owner)
- [ ] Baseline profile: regenerate on a real phone (`./gradlew :app:generateBaselineProfile`) and commit
- [x] Store graphics: `release/` has the icon, the feature graphic and eight phone screenshots, made by
      `tools/feature_graphic.py` and `tools/store_shots.py` (how: `release/README.md`); owner to look them over
- [ ] Play Console: listing from `fastlane/`, data safety from `release/data-safety.md`, content rating
- [ ] F-Droid: submit (metadata in `fastlane/`); CI checks the foss APK has no Google code
- [ ] Tag v0.1.0 (the release workflow reads `CHANGELOG.md` § 0.1.0)
- [ ] Once live: `PLAY_LISTED = true` so About shows "Rate on Google Play"; wire `PlayReview` (in `src/play`) to that row
- [ ] About › More from mid apps: check the Burrow and Weft store links
- [ ] Lint: dependency-update and AGP-version warnings (updating needs a decision); the German "das das" typo
      warning is correct German. Everything else lint reported is fixed

## Next
- [ ] "Enjoying Keyhole?" sheet, as Weft's `SupportPromptSheet` / Burrow's review ask: Rate (play), Star on GitHub,
      Sponsor, Maybe later. Shown at a calm moment (a report just saved), never during a scan; after a few reports and
      7+ days of use, at most weekly, never again after 4 × Maybe later, not for 180 days after Rate / Star / Sponsor
      (rule `SupportPrompt.shouldShow`, unit-tested). Play build opens Play's own rating card (`PlayReview`); foss and
      github show Star and Sponsor only. Needs a design board ("Design review: support sheet")

## Later
- Checklist items: the board names a few the app doesn't ("Key fob and tags", "Wall clock"); decide which list wins
- Photos per finding: finding detail shows "You saved N photos"; showing them needs a database change
- Quick Settings tile "Start sweep", home-screen widget
- Phone check: apps with camera, mic and location access
- Trackers seen in more than one place (watchlist)
