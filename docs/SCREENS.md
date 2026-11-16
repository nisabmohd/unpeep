# Screens

PNG per screen in `design/screens/` (light; `HomeDark` for dark). Routes in `ui/nav/KeyholeNav.kt`.

| # | Screen | Route | Status | Notes / edge cases |
|---|---|---|---|---|
| 01 | Welcome | (first run) | todo | three lines on what it does + "Get started"; permissions asked later, just in time |
| 02 | Home | `HomeRoute` | built | place chips · lens Start · "Check this room" + n checks · Or run one check · last report |
| 03 | Sweep in progress | `SweepRoute(place)` | stand-in | check row (current ringed) · progress lens · found so far · Skip step |
| 04 | Wi-Fi results | | todo | Likely camera first; client-isolation Note when only the router answers |
| 05 | Device | | todo | lens header + tags · Why (signs) · details · Not a camera? / Find it |
| 06 | Not a camera? | sheet | todo | It's mine · Part of the room · Not sure · Report it |
| 07 | Find it | | todo | progress lens, Getting warmer meter, "Updated Xs ago" |
| 08 | Lens finder | `CheckRoute(LENS/INFRARED)` | stand-in | dark; filters No filter · Red · Contrast · 2×; Lens/Infrared; shutter saves a photo |
| 08 | Magnet | `CheckRoute(MAGNET)` | stand-in | learning normal → µT above normal, 10 s bars, honesty Note, Set normal again / Add to report |
| 09 | Checklist | `CheckRoute(CHECKLIST)` | stand-in | per place; Looks fine · Not sure (photo + note) · Skip |
| 10 | Report | | todo | lens header · check row · Found · Photos · Marked as known (Undo) · Couldn't check · Export |
| 11 | If you find a camera | | todo | don't touch, photos, leave if unsafe, tell host/platform, help numbers |
| 12 | Reports | `ReportsRoute` | built (empty) | list by date; empty state |
| 13 | More | `MoreRoute` | built | tiles Appearance · Language · Help numbers · Sweep steps; rows |
| 14 | Appearance | `MoreItemRoute` | stand-in | theme, wallpaper colours, accents, font, text size, sweep steps (reorder) |
| 15 | Help numbers | sheet | todo | region list; default from the phone's region |
| 16 | Privacy | `MoreItemRoute` | stand-in | lens header, what stays on the phone, permissions and why |
| 17 | About | `MoreItemRoute` | stand-in | Burrow's order: app, version, mid apps, source, licence, contact |
| 18 | Licences | `MoreItemRoute` | stand-in | fonts (OFL), Solar (CC BY 4.0), AndroidX (Apache 2.0) |
