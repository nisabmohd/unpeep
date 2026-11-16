<p align="center"><img src="release/icon-512.svg" width="96" alt="Unpeep"></p>

<h1 align="center">Unpeep</h1>
<p align="center">Check a room for hidden cameras and trackers. Free, open source, no ads, no tracking.</p>

Unpeep helps you check a hotel room, an Airbnb, a trial room, a washroom, an office or a car before you relax in it.

- **Wi-Fi:** finds devices on the Wi-Fi you joined and flags the ones that look like cameras (ONVIF, RTSP, camera
  ports, camera makers), each with the reason why.
- **Hotspots:** finds the setup Wi-Fi that cheap cameras broadcast, and helps you walk to it (Find it).
- **Bluetooth:** finds AirTags, Tiles, SmartTags, Chipolos and Find Hub tags, and Bluetooth cameras.
- **Lens:** your torch makes a camera lens shine back; Unpeep rings the bright spots. Red and contrast filters, zoom, photos.
- **Infrared:** night-vision cameras glow on your phone's camera in the dark.
- **Magnet:** shows when the magnetic field jumps near an object, which means electronics inside (never "a camera").
- **Checklist:** where cameras hide in each kind of place, and how to check a mirror.
- **Report:** what was checked, what was found, photos, and **what couldn't be checked**. Export a PDF.

Unpeep never says a room is "safe". No app can find a camera that records to a memory card and sends nothing;
the report says so.

## Private by design
No account, no analytics, no ads, no crash reporting, no AI. Results and photos stay on your phone.
The scanner only talks to the local network you are on. See [PRIVACY.md](PRIVACY.md).

## Download
- Google Play: coming soon
- F-Droid: coming soon (the `foss` build, no Google code)
- GitHub Releases: signed APKs with `SHA256SUMS.txt`; works with Obtainium

## Build
JDK 17+, Android SDK 36.
```
./gradlew :app:assemblePlayDebug        # or Foss / Github
./gradlew :app:testPlayDebugUnitTest
```

## Help make it better
Unpeep learns cameras from community device lists in `app/src/main/assets/signatures/`.
Found a camera it missed, or a device it got wrong? See [community-signatures/README.md](community-signatures/README.md).
Translations and checklist improvements are welcome: [CONTRIBUTING.md](CONTRIBUTING.md).

## Licence
GNU GPL v3. Icons: Solar by 480 Design (CC BY 4.0). Fonts: SIL Open Font License (see `licenses/`).
Made by **mid apps** — also [Burrow](https://github.com/nisabmohd/burrow) and [Weft](https://github.com/nisabmohd/weft).
