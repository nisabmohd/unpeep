---
name: signatures
description: Add or fix camera, hotspot, Bluetooth or known-device signatures in Keyhole. Use when a device was missed or wrongly flagged.
---

# Signatures

Files: `app/src/main/assets/signatures/` (cameras, ports, known, hotspots, ble). Format and rules:
`community-signatures/README.md`.
1. Get evidence: the device's name, HTTP Server header / page title, UPnP model, open ports, hotspot name or BLE
   advertisement (nRF Connect). No guesses.
2. Prefer specific words ("hikvision") to short ones ("cam"); a word that also matches phones or TVs is wrong.
3. Add a `RiskTest` case for the device first, then the signature, then run the tests.
4. A device wrongly flagged as a camera: add it to `known.json`, never weaken the camera rules.
