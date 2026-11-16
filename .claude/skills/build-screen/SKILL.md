---
name: build-screen
description: Build one Keyhole screen from its design PNG so it matches exactly. Use when implementing or redoing any screen listed in docs/SCREENS.md.
---

# Build a screen from its PNG

1. Open `design/screens/NN-name.png` and its row in `docs/SCREENS.md`. List the blocks top to bottom.
2. Map every block to a component in `docs/COMPONENTS.md`. Typical shapes:
   - tab: `TabTopBar` → content → nothing docked (Home's lens Start is the hero)
   - detail: `BackTopBar` → `LensHeader` (+ tags) → `SectionLabel` + `Grouped(ListRow…)` → `Dock` with `ButtonPair`
   - check: `BackTopBar` → lens/meter → `Note` (honesty line) → docked pair
   - choice: `KeyholeSheet` with `ChoiceRow`s (never a dialog)
3. Missing a component? Add it to `ui/components` and COMPONENTS.md first, no per-screen look options.
4. UI state (immutable data class) → ViewModel (StateFlow) → composable. Previews light and dark with fake state.
5. Strings in `strings.xml` (all 7 languages; English first, then translate short). Plurals for counts.
6. Results: words + colour (`Tag`). Never "safe". Every list has its empty state. Reports keep "Couldn't check".
7. Run at 412 dp and compare side by side with the PNG; then dark, 200 % font, 360 dp width, TalkBack.
8. Tests: one `ScreensTest` case per state of the screen (made-up state in, words and actions out), a `ComponentsTest`
   case per new component, and a `SmokeTest` step that reaches the screen in the real app.
9. `./gradlew :app:testPlayDebugUnitTest` (ProjectRulesTest) and `:app:connectedPlayDebugAndroidTest`, tick TODO.md.
   Leave the commit to the owner's review.
