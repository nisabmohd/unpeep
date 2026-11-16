# What Keyhole can't do (and says so)

The report's **Couldn't check** section draws from this list.
- Cameras that record to a memory card and send nothing: no scan can see them. The lens, infrared, magnet and
  checklist are the only help.
- Cameras on another network, on mobile data, or on a hidden Wi-Fi the phone can't see.
- Client isolation on hotel/guest Wi-Fi: other devices don't answer.
- Android 10+ hides other devices' MAC addresses, so the maker can't be looked up from it.
- Trackers that don't advertise, or advertise in a way not in `ble.json` yet.
- Phones with a strong IR filter can't see infrared LEDs (the remote test tells you).
- The lens finder rings what shines like a lens: glass, screws and jewellery shine too, and a lens that isn't lit by
  the torch, or is behind dark glass, may not shine. Its thresholds are tuned on test images so far, not real rooms.
- Wi-Fi scans are limited by Android to 4 every 2 minutes.
- Hotspots need Location allowed and switched on (Android's rule for Wi-Fi names); without it the check can't run.
- With `neverForLocation`, Android may hide beacon-style Bluetooth frames (Eddystone, so possibly Find Hub tags) from
  the scan. Verify on real tags before release.
- The magnet reacts to any electronics, magnets and wiring.
