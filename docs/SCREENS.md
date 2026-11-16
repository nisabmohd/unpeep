# Screens

PNG per screen in `design/screens/` (light; `HomeDark` for dark). Routes in `ui/nav/UnpeepNav.kt`.

| # | Screen | Route | Status | Notes / edge cases |
|---|---|---|---|---|
| 01 | Welcome | `WelcomeRoute` (first run) | built | mark + name · picture (lens, two floating cards) · title + line · Get started · footer; permissions asked later, just in time |
| 02 | Home | `HomeRoute` | built | place chips · lens Start · "Check this room" + n checks · Or run one check · last report |
| 03 | Sweep in progress | `SweepRoute(place, at)` · `CheckRoute(scan)` | built | check row · progress lens · found so far · Skip step; Not on Wi-Fi; permission, radio-off states; hands-on steps open their own screens |
| 04 | Wi-Fi results | `WifiResultsRoute` | built | Note first (only the router answered / nothing looks like a camera) · count · Likely camera · Worth a look · known and not recognised behind one row each (`WifiOthersRoute`) · Next / Done |
| 05 | Device | `DeviceRoute(ip)` | built | lens header + tags · Why (signs) · open ports, maker · Not a camera? · Found it? / Find its lens (both lead to stand-ins) · share as text |
| 06 | Not a camera? | sheet | built | It's mine · Part of the room · Not sure · Report it (opens the repo's issues in the browser); marks live in memory until Room |
| – | Hotspot / Bluetooth results | `NearbyResultsRoute(check)` | built (no PNG: as 04) | Note first · count + "Updated Xs ago" · Likely camera · Worth a look · Marked as known · the rest behind one row (`NearbyOthersRoute`) · Next / Done; a row opens Find it |
| 07 | Find it | `FindItRoute(check, id)` | built | "Updated Xs ago" tag · progress lens with how near in words · name, kind, dBm · meter + Stronger / Weaker / Steady · walking note · "Not heard for a while" note · Not a camera? / Add to report |
| 08 | Lens finder | `LensRoute(place, sweep, start)` · `CheckRoute(LENS/INFRARED)` | built | dark; held upright; picture + rings ("Look here"); filters No filter · Red · Contrast · 2×; Lens/Infrared; hint + photos saved; tips · shutter · torch; camera permission, in use, failed, no camera; TV-remote sheet on first Infrared; "may not see infrared" note; Next in a sweep |
| 08 | Magnet | `MagnetRoute(place, sweep)` · `CheckRoute(MAGNET)` | built | learning % → µT above normal, "Electronics nearby" / "About normal", normal · now, 10 s bars + Jumped / Steady, honesty Note, Set normal again / Add to report, beep; no sensor; Next in a sweep |
| 09 | Checklist | `ChecklistRoute(place, sweep)` · `CheckRoute(CHECKLIST)` | built | per place; tag n of m; progress line; sections; answer sheet Looks fine · Not sure (note + photo) · Skip; Next / See report |
| 10 | Report | `ReportRoute(id)` | built | lens header + tags · check row + skipped line · Found · Photos · Marked as known (Undo) · Couldn't check · not-safe Note · Found something? / Export (PDF); Rename, Share; finding detail (`FindingRoute`), photo (`PhotoRoute`) |
| 11 | If you find a camera | `FoundCameraRoute(reportId)` | built | 4 steps · Help where you are (Change → 15) · Emergency / Police / Helpline / official site · Share report / Call |
| 12 | Reports | `ReportsRoute` | built | this week, then by month; tag to look at / Nothing found; search; long press → delete sheet; empty state; footer |
| 13 | More | `MoreRoute` | built | tiles Appearance · Language · Help numbers · Sweep steps; rows |
| 14 | Appearance | `MoreItemRoute(APPEARANCE)` | built | theme (Segmented), wallpaper colours (Android 12+), accents, font chips, text size sheet, sweep steps (drag + switch); also More › Sweep steps |
| 15 | Help numbers | sheet | built | search; Use my phone's region; every country in assets/help/numbers.json |
| 16 | Privacy | `MoreItemRoute(PRIVACY)` | built | lens header, why each permission (Location: hotspots only), policy link, Delete all reports (sheet) |
| 17 | About | `MoreItemRoute(ABOUT)` | built | mark, version · by mid apps, Check for updates (github only), source, privacy, rate (Play listed only), contact, licences; "More from mid apps" not built |
| 18 | Licences | `MoreItemRoute(LICENCES)` · `LicenceRoute` | built | App, Libraries, Icons and fonts; each opens its full text from assets/licences |
