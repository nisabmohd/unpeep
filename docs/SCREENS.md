# Screens

PNG per screen in `design/screens/` (light; `HomeDark` for dark). Routes in `ui/nav/KeyholeNav.kt`.

| # | Screen | Route | Status | Notes / edge cases |
|---|---|---|---|---|
| 01 | Welcome | (first run) | todo | three lines on what it does + "Get started"; permissions asked later, just in time |
| 02 | Home | `HomeRoute` | built | place chips · lens Start · "Check this room" + n checks · Or run one check · last report |
| 03 | Sweep in progress | `SweepRoute(place, at)` · `CheckRoute(check)` | built (Wi-Fi, Hotspots, Bluetooth steps) | check row (current ringed) · progress lens · found so far · Skip step; "Not on Wi-Fi" + Scan again; permission line + Allow / Open settings; Wi-Fi, Bluetooth or Location off + Scan again; the other steps say they are on their way |
| 04 | Wi-Fi results | `WifiResultsRoute` | built | Note first (only the router answered / nothing looks like a camera) · count · Likely camera · Worth a look · known and not recognised behind one row each (`WifiOthersRoute`) · Next / Done |
| 05 | Device | `DeviceRoute(ip)` | built | lens header + tags · Why (signs) · open ports, maker · Not a camera? · Found it? / Find its lens (both lead to stand-ins) · share as text |
| 06 | Not a camera? | sheet | built | It's mine · Part of the room · Not sure · Report it (opens the repo's issues in the browser); marks live in memory until Room |
| – | Hotspot / Bluetooth results | `NearbyResultsRoute(check)` | built (no PNG: as 04) | Note first · count + "Updated Xs ago" · Likely camera · Worth a look · Marked as known · the rest behind one row (`NearbyOthersRoute`) · Next / Done; a row opens Find it |
| 07 | Find it | `FindItRoute(check, id)` | built | "Updated Xs ago" tag · progress lens with how near in words · name, kind, dBm · meter + Stronger / Weaker / Steady · walking note · "Not heard for a while" note · Not a camera? / Add to report |
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
