# How each check works

All scans start only when the user starts them. Nothing runs in the background.

## Wi-Fi (local network only)
1. Subnet from `ConnectivityManager` › `LinkProperties` (IPv4 /24 around the phone's address).
2. Sweep: TCP connect to a few ports per address (≤ 64 at once, 400 ms) through `scan/net/LocalOnly.connect`.
3. mDNS: `NsdManager` for `_rtsp._tcp`, `_http._tcp`, `_onvif._tcp`, `_hap._tcp`, `_googlecast._tcp`.
4. SSDP: `M-SEARCH` to 239.255.255.250:1900, read the device XML (manufacturer, model).
5. ONVIF WS-Discovery: Probe to 239.255.255.250:3702; a reply means a network camera. Needs a `MulticastLock`.
6. Per device with open ports: RTSP `OPTIONS` (554/8554), HTTP `GET /` (Server header + `<title>`). Nothing else:
   no logins, no default passwords, no streams.
7. `domain/Risk.kt` `classify()` with `assets/signatures` (rules and tests in `RiskTest`).
Code: `scan/net/WifiScanner.kt` (the steps), `Subnet`, `Tcp`, `Mdns`, `Banners` (what is sent and how answers are
read, unit-tested), `scan/WifiSession.kt` (the scan between its screens), `domain/Wifi.kt` (order, marks).
A device that takes or refuses a connection is there; silence says nothing. Every socket is tied to the Wi-Fi
network, so a guest Wi-Fi with no internet is still the one scanned. The whole scan stops at 19 s with what it has.
The Wi-Fi's name shows only when Android gives it (it needs location access, which Keyhole doesn't ask for here).
Limits: Android 10+ hides MAC addresses; hotel Wi-Fi often isolates clients (only the router answers → Note).

## Hotspots
`WifiManager.startScan()` + `scanResults`, names matched against `hotspots.json`. Throttle: ≤ 4 scans per 2 min;
show "Updated Xs ago". Find it uses RSSI.

## Bluetooth
`BluetoothLeScanner`, `neverForLocation`. Trackers by advertisement, not name: Apple Find My (company 0x004C, type
0x12), Tile (FEED/FEEC), SmartTag (FD5A), Chipolo (FE33/FE65), Find Hub (FEAA frames). **Verify each on a real tag**
before release. Cameras by name only, as a hint.

## Lens
Rear camera + torch. Analyzer on the Y plane: small, very bright, round spots that stay bright across frames →
ring them. Filters: Red (boost R, dim the rest: glint stands out), Contrast. 2× zoom. Shutter saves a photo to app storage.

## Infrared
Front camera in the dark (many rear cameras filter IR), raised exposure; IR LEDs show as purple/white dots.
First use: point a TV remote at the camera and press a button, to learn whether this phone sees IR.

## Magnet
`TYPE_MAGNETIC_FIELD`, `MagnetMeter`: median of the first 25 readings is "normal"; ≥ 10 µT away = "Jumped"
("Electronics nearby"). No permission. Never a score, never "camera".

## Checklist
Per place, in `strings.xml`. Each item: Looks fine · Not sure (photo + note) · Skip.
