# Performance and size

Budgets (same as Burrow/Weft):
- **APK < 5 MB** (play release, universal); aim ~3 MB. R8 full mode, minify + shrink resources, `localeFilters`,
  packaging excludes, no PNGs in the app, baseline profile committed.
- Cold start < 500 ms on a mid phone, < 900 ms low-end. `AppContainer` is all `lazy`; signatures load on first scan.
- Wi-Fi sweep of a /24 in < 20 s: ≤ 64 sockets at once, 400 ms connect timeout, coroutines on `Dispatchers.IO`.
- Camera analyzers: downscaled Y plane only, `STRATEGY_KEEP_ONLY_LATEST`, never block the preview (30 fps on a
  low-end phone). Magnet: `SENSOR_DELAY_UI`, unregister when the screen stops.
- Check with: `./gradlew :app:assemblePlayRelease` → `app/build/outputs/apk/play/release/`; Macrobenchmark via
  `:baselineprofile` (`./gradlew :app:generatePlayReleaseBaselineProfile`).
