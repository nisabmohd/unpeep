# Play Store release assets

Graphics and listing text for the Play Store, laid out as Burrow's and Weft's `release/` folders are.

## Graphics

| Asset | Play requirement | File | How it is made |
| --- | --- | --- | --- |
| App icon | 512 × 512 PNG, 32-bit, up to 1 MB. A full square: Play applies the rounded mask | `icon-512.png` (source `icon-512.svg`) | `python3 tools/keyhole_mark.py` writes the SVG (white mark on #173782); render it with headless Chrome at 512 × 512 |
| Feature graphic | 1024 × 500 JPEG or 24-bit PNG, no transparency | `feature-graphic-1024x500.png` | `python3 tools/feature_graphic.py` (also writes `fastlane/metadata/android/en-US/images/featureGraphic.png`) |
| Phone screenshots | 2–8 images, 9:16, each side 320–3840 px, long side at most 2× the short side | `phone-screenshots/01–08` (1080 × 1920) | `python3 tools/store_shots.py <folder of raw shots>` (also writes `fastlane/.../phoneScreenshots/1–8.png`) |
| 7-inch and 10-inch tablet screenshots | Optional | – | Not made |
| Promo video | Optional | – | Not made |

The look is the family's: a flat page in the app's blue tint, a two-line caption in Lexend and a plain phone with a
thin rim; the last shot is dark. Nothing else on the page. Raw emulator shots are 1080 × 2400 (20:9), which Play
rejects, so each one sits in a 9:16 frame under its caption.

### Taking the raw shots

A 1080 × 2400 emulator at 420 dpi, gesture navigation, status bar in demo mode (9:30, full battery, Wi-Fi only):

```
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
./gradlew :app:connectedPlayDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.shots=1 \
  -Pandroid.testInstrumentationRunnerArguments.class=app.keyhole.privacy.screens.DesignShots#storeShots
python3 tools/store_shots.py "app/build/outputs/connected_android_test_additional_output/playDebugAndroidTest/connected/<device>"
adb shell am broadcast -a com.android.systemui.demo -e command exit
```

`DesignShots#storeShots` draws the real screens with made-up state (default blue, Lexend), because an emulator has
no camera network to find: a sweep in progress, a likely camera, a report. The lens finder's picture is a drawn dim
room, since the emulator has no camera picture worth showing.

| # | Screen | Title / line |
| --- | --- | --- |
| 1 | Home | Check a room for cameras / Seven checks in one calm sweep. |
| 2 | Sweep, checking the Wi-Fi | See what is on the Wi-Fi / Keyhole asks every device and lists what answers. |
| 3 | Wi-Fi results | Likely cameras come first / Known devices are set aside, so the list stays short. |
| 4 | Device | Every finding says why / The signs Keyhole saw, in plain words. |
| 5 | Lens finder | Find a lens by its glint / The torch and the camera ring what shines back. |
| 6 | Find it | Walk towards a signal / A meter leads you to a hotspot or a tracker. |
| 7 | Report | An honest report / What was checked, and what couldn't be. |
| 8 | Home, dark | Private. Yours. / Keyhole never sends your data anywhere. |

Captions follow `docs/COPY.md` and the listing rules: no fear words, never "safe", "100%" or "detects all".

## Text

- `listing.md`: title, where the short and full descriptions live, category and tags.
- `data-safety.md`: the Play Console data safety answers.
- `content-rating.md`: the IARC questionnaire answers.
