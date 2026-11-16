# Keyhole — project plan

> **Name: Keyhole** (checked 15 Nov 2026: no detector app on Play, F-Droid or GitHub uses it).
> Play title: "Keyhole: Hidden Camera Finder" (≤ 30 chars). Package `app.keyhole.privacy`, repo `nisabmohd/keyhole`.

Keyhole is a small, open-source Android app that helps you check a room (Airbnb, hotel, trial room, washroom, office,
cab) for hidden cameras and trackers. It scans the Wi-Fi, nearby hotspots and Bluetooth, gives you a lens finder, an
IR finder and a magnet check, and walks you through a checklist. At the end it tells you plainly what it checked and what it could not.
**It never sends your data anywhere.**

Package `app.keyhole.privacy` · by **mid apps** · GNU GPL v3 · same stack, size budget and rules as Burrow and Weft.

**No ads · No tracking · No account · Free and open source (GPL v3)**

---

## 1. What the app is (USP, in order)

1. **Honest results.** Every finding says why it was flagged. The report lists what was checked and what could not
   be. Never "Room is safe"; say "No cameras found on this Wi-Fi".
2. **Real checks, not a gimmick.** Network fingerprints (ONVIF, RTSP, camera ports), camera hotspots, BLE trackers,
   lens glint, infrared, and a magnet check that says what a jump means. No fake EMF score.
3. **Private by design.** No account, no analytics, no AI, no crash reporting. Scan results and reports stay on the phone.
4. **One right way to do each thing.** One sweep flow, few settings, calm Material 3 Expressive design, small and fast.

**Not in the app:** logging in to cameras, default-password tries, viewing streams, an EMF "camera score", a "thermal view"
simulation, "bug/microphone detection", AI image detection, cloud sync, accounts, sharing scans online, a "safety score".

---

## 2. Features (v1)

### 2.1 Wi-Fi scan (the main check)
Finds devices on the Wi-Fi you're connected to and flags the ones that look like cameras.
- **Discovery:** ping + TCP sweep of the /24 (bounded coroutine pool), mDNS via `NsdManager`, SSDP/UPnP
  (`M-SEARCH`, then read the device XML for maker and model), **ONVIF WS-Discovery** (UDP multicast 3702;
  nearly every IP camera answers). Needs a `MulticastLock`.
- **Ports:** 554/8554 RTSP · 80/8080/8000/443 web · 37777 Dahua · 34567 XMEye · 8899/2020 ONVIF · 1935 RTMP ·
  6668 Tuya · 9527 · 5000.
- **Fingerprint:** RTSP `OPTIONS`, HTTP `Server` header + page title, UPnP model, mDNS/host name. Match against
  `community-signatures/` (see 2.8).
- **Result per device:** Likely camera · Check this · Known device (phone, TV, router), with one line on why
  ("Answers ONVIF and streams on port 554").
- **Limits shown in the app:** Android 10+ hides other devices' MAC addresses (no vendor lookup). Hotel Wi-Fi often
  isolates clients: if only the router answers, say so. A camera on another network, on mobile data or recording to a
  memory card won't appear.
- **Guard:** the scanner opens sockets **only to private, link-local and multicast addresses** (`LocalOnly.kt`).
  `ProjectRulesTest` and a unit test check this.

### 2.2 Nearby hotspots
Cheap spy cams often create their own Wi-Fi for setup.
- List nearby SSIDs and flag camera-hotspot names (`HDWiFiCam`, `IPC-*`, `V380-*`, `A9_*`, `Lookcam*`, `CAM_*`,
  `SmartLife-*`…).
- **Find it:** live signal strength, stronger as you get closer. Android limits Wi-Fi scans to 4 every 2 minutes, so
  show "Updated 20 s ago".
- Works without joining the room's Wi-Fi, which makes it useful in trial rooms.

### 2.3 Bluetooth scan
- BLE scan: camera-like names and **unknown trackers** (AirTag / Find My, Tile, SmartTag, Chipolo).
- **Find it:** signal strength gets stronger as you get closer.

### 2.4 Lens finder
- Rear camera + torch, live preview, an analyzer that rings small, very bright, round spots (lens glint).
- Tips sheet: lights off, move slowly, look at smoke detectors, clocks, chargers, vents, frames, mirrors.
- Label: "Rings possible lenses. Look closely to be sure."
- Filters for lens and infrared: No filter · Red (glint stands out) · Contrast, plus 2× zoom.
- **Proof photos:** the shutter saves a photo of the spot (app storage only, never the gallery unless exported).
  Photos show in the report and the PDF; deleting the report deletes them.

### 2.5 Infrared finder
- Dark room, front camera (rear cameras often block IR), boosted exposure; IR LEDs show as purple/white dots.
- First use: "Point a TV remote at the camera and press a button" test, so the user knows the phone can see IR.

### 2.5b Magnet (added 15 Nov 2026: every competitor has one, so ours is the honest version)
- Phone magnetometer (`TYPE_MAGNETIC_FIELD`, no permission, no library). Learns the room's normal field first,
  then shows "+14 µT above normal", a 10-second bar chart and "Electronics nearby" when it jumps (`sensors/MagnetMeter.kt`).
- Says plainly: speakers, magnets and wiring make it jump too, so a jump means electronics inside, not a camera.
- "Set normal again", optional beep, "Add to report". Never a score, never "camera detected".

### 2.6 Room checklist (the heart of the app)
One checklist per place: **Stay** (Airbnb, hotel) · **Trial room** (60-second version, no Wi-Fi needed) ·
**Washroom** · **Office** · **Cab**. Each item has a small Solar-style picture and one line.
Mark each: Looks fine · Not sure (photo + note, stays on the phone) · Skip.
Mirror check: press the torch against the mirror in the dark; if light shows behind it, be careful. The fingernail
test is unreliable, and the app says so.

### 2.7 Report and "Found something?"
- Report: place, date, what was checked (Wi-Fi, hotspots, Bluetooth, lens, IR, checklist n/m), findings, photos, **what
  could not be checked**. Saved in Room; export a PDF (`PdfDocument`) or plain text through SAF / share sheet.
- Found something: don't touch it, take photos and video, leave if you feel unsafe, tell the host or platform, call the
  police. Region help is bundled per country (India: 112, cybercrime.gov.in / 1930). Add countries by PR.

### 2.8 Community signatures
Like Burrow's `community-rules/`: `community-signatures/` holds JSON files (camera vendors, ports, banners, hotspot
names, BLE names) bundled at build time and read with kotlinx-serialization. Contributors add devices by PR.
No online updates.

### Later (TODO.md, not v1)
- Quick Settings tile "Start sweep" and a home-screen widget (plain RemoteViews, as Burrow).
- Phone check: apps with camera, mic and location access, plus a link to Android's Privacy dashboard.
- Trackers seen in more than one place (a watchlist). Until then, point to Android's own unknown-tracker alerts.

---

## 3. Stack (same as Burrow / Weft)

minSdk 26, targetSdk 36, compileSdk 36, AGP 9.1.0, Kotlin 2.4.x, KSP, version catalog (copy `libs.versions.toml`
from Burrow), JDK 17 target, CI on Temurin 21.
Compose + Material 3 (Expressive), Navigation Compose (type-safe routes), ViewModel + StateFlow, one `AppContainer`
(all `lazy`, **no DI framework**), Room (Flow DAOs, schemas in `app/schemas`), DataStore Preferences,
kotlinx-serialization, profileinstaller + `:baselineprofile` module.

**Platform APIs only:** `java.net` sockets and `DatagramSocket`, `NsdManager`, `WifiManager`, `ConnectivityManager`,
`BluetoothLeScanner`, `PdfDocument`, SAF. No networking library.

### Dependency to ask about (one)
- **CameraX** (`camera-camera2`, `camera-lifecycle`; maybe `camera-compose` for the viewfinder) for the lens and IR
  finders. Roughly +0.5–1 MB after R8. The other option is raw Camera2: more code, no library.
  **Decide before Milestone 3.**

### Flavors
| Flavor | What's different |
|---|---|
| `play` | Play In-App Review (`src/play`, stand-in `src/noPlay`). |
| `foss` | F-Droid. No Google code at all. |
| `github` | APK on GitHub Releases + the GitHub update check (as Weft/Burrow). |

No Drive backup (there's nothing worth backing up). `dependenciesInfo` off for F-Droid.

### Internet: different from Burrow
Android needs `INTERNET` for **any** socket, including on your own Wi-Fi, so every flavor declares it. The rule is:
**the scanner only talks to the local network** (enforced by `LocalOnly.kt` + test); `github` also asks GitHub
about updates. Copy, listing and PRIVACY.md say "never sends your data anywhere", never "no internet".

### Permissions (asked just in time, each with one line why)
`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`,
`CHANGE_WIFI_STATE`, `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` (Hotspots check only, see below),
`BLUETOOTH_SCAN` (`neverForLocation`), `BLUETOOTH` + `BLUETOOTH_ADMIN` (`maxSdkVersion 30`), `CAMERA`.
**Location (decided 16 Nov 2026):** Android gives Wi-Fi scan results (`startScan` / `getScanResults`) only to apps with
precise location access and with Location switched on, on every version; `NEARBY_WIFI_DEVICES` does not cover them
(checked on Android 15). So the Hotspots check asks for Location when it starts, with the line "Android only lists
Wi-Fi names for apps with precise location access… It never reads where you are." Nothing else asks for it (Bluetooth
only before Android 12), no code reads a position (`ProjectRulesTest`), and never in the background.
No storage permission (SAF). Before raising targetSdk past 36, check Android's new local-network permission rules.

### Size and speed budgets (from Burrow's PERFORMANCE.md)
- APK (play, release, universal) **< 5 MB**, aim ~3 MB. R8 full, minify + shrink resources, locale filters,
  packaging excludes, baseline profile committed.
- Cold start < 500 ms mid phone / < 900 ms low-end. A full /24 scan in under 20 s on a normal home router.
- Camera analyzer keeps the preview at 30 fps on a ₹8,000 phone (analyze downscaled Y plane only, drop frames).
- Vector drawables only, no PNGs in the app.

### Languages
Same seven as Burrow/Weft: `en, de, es, fr, hi, ja, pt-rBR`. `localeFilters` + `generateLocaleConfig`, in-app
language picker, `bundle { language { enableSplit = false } }`, pseudo-locales on in debug.
Checklist text lives in `strings.xml` too, so it gets translated.

---

## 4. Design (final, see the "Keyhole — app design" canvas)

- **Same family as Burrow and Weft:** big tab title + round tonal header buttons, bottom nav (Check · Reports · More),
  grouped tinted rows (2 dp gaps, 24 dp outer / 8 dp inner corners), sentence-case section labels, secondary + primary
  button pair docked at the bottom, Weft-style sheets (handle, title left, round close, radio choice rows), Burrow's
  More tiles and About order, the same Welcome layout.
- **Keyhole's own identity: the lens.** Concentric rings + a glint (from the logo) on the Start button, the scan
  progress ring, Find it and every detail header. The seven checks are one row of hue icons on Home, the sweep and the report.
- **Colour:** dark blue, seed hue 222 (primary hsl(222 70% 30%)), dynamic colour optional. Each check keeps its own
  hue on its icon only: Wi-Fi 232, Hotspots 150, Bluetooth 268, Lens 34, Infrared 338, Magnet 92, Checklist 186.
  Red only for "Likely camera", amber for "Worth a look", always with words.
- **Type:** Lexend default (Outfit, Atkinson, Space Grotesk selectable), weights 400/500, eight styles:
  display 32, headline 30, title 22, card title 16, body 16, supporting 14, label 14, caption 12.
- **Icons:** Solar Linear only, 22 dp in 44 dp circles, 20 dp chevrons; dotted grip for drag handles.
- **Sheets first:** every choice, picker and confirm is a bottom sheet. No dialogs.
- **False positives:** every finding has "Not a camera?" (It's mine / Part of the room / Not sure), the report lists
  "Marked as known" with Undo, More › Marked as known manages them, "Report it" helps fix the device list.
- **Never "safe":** the report always says what was checked and what couldn't be.
- **Every language:** no country assumed; help numbers come from the phone's region; labels never squeeze or overlap
  (the check row scrolls sideways at large text).
- **Logo:** a keyhole drawn the Burrow/Weft way (white strokes, round caps, dark blue). Final: "Lens, open" (lens ring, glint, two legs), centred;
  one geometry in `tools/keyhole_mark.py` writes every copy.

## 5. Screens (canvas)

01 Welcome · 02 Home (place chips, lens Start, run one check, last report) · 03 Sweep in progress · 04 Wi-Fi results ·
05 Device · 06 Not a camera? (sheet) · 07 Find it · 08 Lens finder (Lens / Infrared, filters, photo) · 08 Magnet · 09 Checklist · 10 Report ·
11 If you find a camera · 12 Reports tab · 13 More tab · 14 Appearance (theme, colour, font, sweep steps) ·
15 Help numbers (sheet) · 16 Privacy · 17 About (mid apps) · 18 Licences · plus Home dark, the design system and logo options.

## 6. Repo layout (mirrors Burrow)

```
CLAUDE.md  AGENTS.md  README.md  PRIVACY.md  CHANGELOG.md  CONTRIBUTING.md  TODO.md  LICENSE (GPL-3.0)
app/
  build.gradle.kts  proguard-rules.pro  lint.xml  schemas/
  src/main/java/app/keyhole/privacy/
    ui/          theme (Color, Type, Dimens, Motion), components, screens
    scan/        net (discovery, ports, fingerprint, LocalOnly), wifi (hotspots), ble
    optics/      lens + IR analyzers, photo capture
    sensors/     MagnetMeter (pure) + MagnetSensor
    checklist/   models, place lists
    report/      builder, PDF export
    data/        Room, DataStore, signature loader
    domain/      pure logic + unit tests (risk rules, matching)
    AppContainer.kt
  src/play/  src/noPlay/  src/foss/  src/github/
baselineprofile/
community-signatures/   cameras.json  ports.json  hotspots.json  ble.json  README.md
design/                 tokens.json  INDEX.md  screens/  logo/  icons/solar-icons.json
docs/                   DESIGN.md TOKENS.md COMPONENTS.md SCREENS.md MOTION.md PERFORMANCE.md COPY.md ICONS.md
                        SCANNING.md (how each check works) LIMITS.md
release/                listing.md  data-safety.md  content-rating.md  README.md
fastlane/metadata/android/en-US/
licenses/fonts/  licenses/icons/
tools/                  keyhole_mark.py  solar_icon.py  store_shots.py  smoke.py  capture.py  device.py  add_strings.py
.claude/skills/         build-screen  add-feature  verify-app  release  signatures
.github/workflows/      ci.yml  release.yml   (copy Burrow's; secrets KEYHOLE_KEYSTORE_*)
```
Release files go in `build/dist/v<version>/`; assets end `-github.apk` with `SHA256SUMS.txt` (update check).

---

## 7. Roadmap

### Week 0: Set up
- [ ] Final name + seed hue + logo (`tools/keyhole_mark.py`)
- [ ] Copy Burrow's Gradle setup, catalog, flavors, signing, CI/release workflows, `.gitignore`, `lint.xml`
- [ ] Theme files, fonts, `AppContainer`, nav shell, `KeyholeSheet`, centred header, More screen
- [ ] CLAUDE.md, AGENTS.md, TODO.md, docs skeleton
- [ ] Buy a test camera (V380 or Tapo, ₹1–2k) and a cheap "charger cam" if possible

### Week 1–2: Wi-Fi scan
- [ ] Subnet from `LinkProperties`, `LocalOnly` guard + test
- [ ] Ping/TCP sweep, mDNS, SSDP, WS-Discovery, port scan, banners
- [ ] `community-signatures/` + matcher in `domain/` with unit tests
- [ ] Screens 04, 05; client-isolation notice; test on home Wi-Fi, a phone hotspot and the test camera

### Week 3: Hotspots + Bluetooth
- [ ] Screens 06, 07, Find it, tracker detection, scan throttle handling

### Week 4: Lens + Infrared
- [ ] CameraX decision, analyzers, filters, proof photos, screen 08, remote test, field test in a dark room
- [ ] Magnet: MagnetSensor, screen 08 Magnet, beep, field test near chargers and smoke detectors

### Week 5: Checklist + Keyhole + Report
- [ ] Checklist content for all places (write carefully, translate), screens 03, 10, 11, 12, PDF export

### Week 6: Release
- [ ] First-run 01, permission rationale sheets, 7 languages, accessibility (48 dp, TalkBack labels)
- [ ] Baseline profile, size check < 5 MB, smoke suite
- [ ] PRIVACY.md, release/listing.md (avoid "spy" in the listing), data-safety.md ("No data collected"),
      store shots via `tools/store_shots.py`, F-Droid metadata, GitHub release, Obtainium badge

---

## 8. Risks

- **False confidence.** The biggest risk. Wording and the "Couldn't check" list stop it.
- **Lens finder false positives** (screws, glass): always "look closely", tune thresholds on real rooms.
- **Play review:** network scanning + "hidden camera" can draw a look. Explain every permission, privacy-protection
  framing, no "spy" keywords, Data safety = nothing collected.
- **Hotel client isolation** hides devices: detect and say so.
- **Phones differ** (IR filters, flash placement): that's why the remote test exists.
