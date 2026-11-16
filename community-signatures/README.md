# Community signatures

Keyhole recognises devices from small JSON lists bundled in the app: `app/src/main/assets/signatures/`.
There are no online updates: a new list ships with the next version.

| File | What it holds |
|---|---|
| `cameras.json` | camera makers: words found in a device's name, web page title, HTTP Server header, UPnP or mDNS answer |
| `ports.json` | ports checked on each device (camera services, web pages) |
| `known.json` | phones, computers, TVs, speakers, routers, printers: never shown as cameras |
| `hotspots.json` | Wi-Fi names cheap cameras broadcast (regular expressions) |
| `ble.json` | trackers by what they advertise, cameras by name |

## Adding a device
Open a pull request with:
1. The change to the right file.
2. Evidence: a screenshot or text of what the device shows (name, page title, Server header, hotspot name,
   or the advertisement from nRF Connect), and the device's make and model.
3. A test case in `app/src/test/.../RiskTest.kt` if it's a camera or a known device.

Keep words specific. "hikvision" is good; "cam" would match "camera app" and phones named "Cameron".
A device wrongly flagged as a camera goes into `known.json`.

Never add anything that would make Keyhole log in to a device, try a password or open a stream.
