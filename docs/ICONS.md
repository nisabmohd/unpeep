# Icons

Solar **Linear** only (480 Design, CC BY 4.0, credited in About › Licences and `licenses/icons/`).
`tools/solar_icon.py` writes every `res/drawable/ic_*.xml` from the Solar set; the `ICONS` map in it is the list.
Never hand-draw or hand-edit an icon; add a line to the map and run:
```
npm pack @iconify-json/solar && tar xzf iconify-json-solar-*.tgz   # outside the repo
python3 tools/solar_icon.py <path>/package
```
Custom glyphs drawn in Solar style live in `CUSTOM` (the dotted drag grip).
Brand marks (launcher, monochrome, notification, store icon) come from `tools/keyhole_mark.py` only.

Sizes: 22 dp in 44 dp circles, 24 dp in header buttons and nav, 20 dp chevrons, 14–16 dp in tags and chips.

Checks: Wi-Fi `wi-fi-router-minimalistic` · Hotspots `routing-2` · Bluetooth `bluetooth-wave` · Lens `flashlight` ·
Infrared `eye` · Magnet `magnet` · Checklist `checklist-minimalistic`. Nav: `radar-2`, `document-text`, `tuning-2`.
