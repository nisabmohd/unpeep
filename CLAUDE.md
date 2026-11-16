# Unpeep — project rules

Unpeep is an open-source Android app that helps you check a room for hidden cameras and trackers. It scans the Wi-Fi,
nearby hotspots and Bluetooth, finds lenses and infrared lights with the camera, checks the magnetic field, walks you through a checklist and
tells you plainly what it checked and what it couldn't. **It never sends your data anywhere.**
Package `app.unpeep.privacy` · by **mid apps** · GNU GPL v3 · Play title "Unpeep: Hidden Camera Finder".
**Logo:** one geometry, `tools/unpeep_mark.py`, writes every copy (launcher, monochrome, `ic_notification`, store icon,
feature graphic, site), each centred on its canvas. Never hand-edit those files; change the script and run it.

Read before working: `docs/PLAN.md`, `docs/DESIGN.md`, `docs/TOKENS.md`, `docs/COMPONENTS.md`, `docs/SCREENS.md`
(with `design/screens/*.png`), `docs/SCANNING.md`, `docs/LIMITS.md`, `docs/PERFORMANCE.md`, `docs/COPY.md`,
`docs/ICONS.md`, `docs/MOTION.md`. Today's work: `TODO.md`.
The PNGs in `design/screens/` are the source of truth for layout, sizes and colours; the live canvas is
"Keyhole — app design" (https://claude.ai/artifact/WpPebqz1aUhk9EQwNC1n5z).

## What the app is (USP, in order)
1. **Honest results.** Every finding says why. The report lists what was checked and what couldn't be.
   Never "Room is safe" / "No cameras"; say "No cameras found on this Wi-Fi".
2. **Real checks.** ONVIF / RTSP / camera ports and banners, camera hotspots, BLE trackers, lens glint, infrared, magnet (a jump means electronics, never "camera").
3. **Private by design.** No account, no analytics, no AI, no crash reporting. Results stay on the phone.
4. **One right way to do each thing.** Few settings, calm design, small and fast.

Not in the app: logging in to cameras, password tries, viewing streams, an EMF "camera score", a "thermal view", "bug detection", a "safety score",
AI detection, cloud sync, accounts, online signature updates.

## Hard rules
- **Local network only.** Every flavor has `INTERNET` because Android needs it for any socket. The scanner opens
  sockets only to private (10/8, 172.16/12, 192.168/16), link-local and multicast addresses, through
  `scan/net/LocalOnly.kt`. Never create a socket anywhere else. `github` also asks GitHub for updates, nothing else.
  `ProjectRulesTest` checks that no other file opens a socket.
- **Detect, never intrude.** Read only what a device answers to discovery and a plain `OPTIONS` / `GET /`. No logins,
  no default passwords, no streams, no writes.
- **Ask before adding any dependency.** No DI framework (one `AppContainer`), no networking library, no chart
  library (Canvas), no image library, no analytics, no WorkManager, no `material-icons-extended`.
  CameraX only once approved.
- `foss` never contains Google code. Play-only code in `src/play` (stand-in `src/noPlay`).
- All user-facing text in `strings.xml` (plurals for counts), checklist text too. No raw colour outside `Color.kt`,
  no raw text size/weight outside `Type.kt`, no raw spacing/size/shape outside `Dimens.kt`, no `tween` outside `Motion.kt`.
- **One look per element:** the same thing on two screens uses one shared component with no per-screen look options.
- Screens use only components in `docs/COMPONENTS.md`. Missing one? Add it there first. UI without a PNG goes on
  TODO.md as "Design review: …".
- Sheets first: every choice, picker and confirm is a bottom sheet (`UnpeepSheet`). No dialogs.
- Every finding can be marked "Not a camera?"; the report lists what was marked, with Undo.
- Icons: Solar Linear vector drawables only (`tools/solar_icon.py`, CC BY 4.0, credited in About › Licences).
- Release: R8 full, minify + shrink resources, baseline profile committed. **APK < 5 MB.**
- Build and run after every change; one commit per working step. Never `./gradlew clean` bare: use `:app:clean`.
- Release files go in `build/dist/v<version>/`.

## Design rules (short form of docs/DESIGN.md)
- Seed hue 222 dark blue in `Color.kt`, dynamic colour optional, Lexend default (Outfit, Atkinson, Space Grotesk), weights 400/500,
  4 dp grid, 16 dp margin, 24 dp surfaces, circles/pills for small tappable things.
- Family patterns from Burrow/Weft (tab title + tonal buttons, nav Check · Reports · More, grouped rows, sheets, About order).
- Unpeep's identity is the lens (rings + glint): Start button, scan progress, Find it, detail headers. The seven checks (Wi-Fi 232, Hotspots 150, Bluetooth 268, Lens 34, Infrared 338, Magnet 92, Checklist 186) are one icon row.
- Every detail screen (device, report, privacy, help) opens with the same small lens header.
- Result colours: red only for "Likely camera" and errors, amber for "Check this", neutral for known devices.
  Never tint a whole screen red.
- **No buttons inside cards.** One main action per screen.
- Notes go first, in the one `Note` component.
- Short plain words. No exclamation marks, no "Oops", no fear words ("spy", "danger") in the UI.
- Every clickable element ≥ 48 dp with a TalkBack label. Text grows with font size.
- Motion: shared axis X push/pop, fade through for tabs, predictive back.

## Stack
minSdk 26, targetSdk 36, AGP 9.1.0, Kotlin 2.x, KSP, version catalog. Compose + Material 3, Navigation Compose
(type-safe routes), ViewModel + StateFlow, Room (Flow DAOs), DataStore Preferences, kotlinx-serialization
(`app/src/main/assets/signatures/*.json`). Scanning: `java.net` sockets, `DatagramSocket` + `MulticastLock`, `NsdManager`,
`WifiManager`, `BluetoothLeScanner`. Report: `PdfDocument` + SAF. Flavors `play`, `foss`, `github`.
Languages: en, de, es, fr, hi, ja, pt-rBR.

## Behaviour rules
- A scan never runs in the background or on a schedule; only when the user starts it.
- Network scan: /24 of the current Wi-Fi, ≤ 64 sockets at once, 400 ms connect timeout, finishes < 20 s.
- Risk: **Likely camera** = ONVIF reply, or RTSP + a camera signature, or a camera vendor banner.
  **Check this** = RTSP/camera port open with no match, or an unknown device with a web UI. **Known** = matches a
  phone/TV/router/printer signature. Rules live in `domain/Risk.kt` with unit tests; change the tests first.
- If only the router answers, show the client-isolation note.
- Wi-Fi scan throttle: never call `startScan()` more than 4 times in 2 minutes; show "Updated Xs ago".
- Lens/IR analyzers read the downscaled Y plane only and drop frames; the preview never stutters.
- Photos (lens/IR shutter, checklist) and notes stay in app storage; deleting a report deletes them. Export only through the share sheet / SAF.
- Magnet: learn normal first, then show µT above normal; "Electronics nearby" on a jump, never "camera". No score.
- The report always has a "Couldn't check" section.

## Working with Claude
- Start each session with `TODO.md`: pick the next unticked item, build it screen by screen against its PNG.
- Skills in `.claude/skills/`: `build-screen`, `add-feature`, `signatures`, `verify-app`, `release`.
- After each screen: run, compare with the PNG (light), check dark mode and the largest font, fix, commit, tick the box.
