# Unpeep designs, final v3

One PNG per screen or state, exported at 2× (412 × 892 dp → 824 × 1784 px unless the size column says otherwise). Light theme unless the name says dark. Lexend 400/500, Solar Linear.

Canvas: https://claude.ai/artifact/JqNdyhYNxSS23rLA9fvNpg

Prefixes: `00` design system · `01`–`18` screens, numbered like the existing PNGs · `E` errors and edge cases · `T` tablet and landscape.

## Design review, round 2

| Line | Verdict | Answer |
|---|---|---|
| Appearance › Font and Text size | Change the app | Rows with “Aa” in each font and a stepped slider, as Burrow and Weft: right. Add a fifth row, System default (“Your phone’s font”), as Burrow has. Sweep steps is drawn only on its own screen. |
| Appearance › Theme | App is right | The segmented control fills the width inside a box. 14 redrawn. |
| Lens finder | App is right | Switch camera left of the shutter, tips in the top bar, “Opening the camera”, gliding rings. All 08 boards redrawn. |
| Lens finder: picture and panel | Change the app | The picture runs edge to edge from the top, behind the status bar and top bar, and stops at a rounded panel that holds every control, so no spot is ever covered. “2 spots” sits on the picture, top left. The hint is one line; the rest moves to tips. |
| Lens finder on its side | Change the app | The same panel, on the trailing side, with everything portrait has: labelled Lens · Infrared, the filters and 2×, the hint, the photos-saved line, then Switch camera · shutter · torch. Tips and Next stay in the top bar. Lens and the torch share one icon, so they are never two round buttons in one rail. T05 redrawn. |
| Report: couldn’t check | Change the app | The standard row with a grey circle is right. Swap the repeated closed eye for an icon of the thing itself: memory card, antenna, tag. A skipped check keeps its own icon. Same on What Unpeep can’t find. |
| Report: check row | App is right | Circles shrink from 44 to 36 dp with at least 6 dp between them. 10l redrawn. |
| Report: export | Change the app | Saying it worked is a Toast: “Saved as a PDF” with Open. Failing stays an error Note with Try again. Good-news Notes use the calm container with a ✓, as you built. |
| Report: order | App is right | Header · check row · Found · Photos · Marked as known · Couldn’t check · note · dock. 10b draws it full length. |
| The PDF | Change the app | 10g stays the target: same order as the screen, photos included. Plain text is fine until then. |
| Finding detail | App is right | No photos section; “You saved 1 photo” as a sign row. 10e redrawn. |
| About | App is right | Round app icons, the mid apps contact row and the credit line, as the family does. 17 redrawn full length. |
| Rows | App is right | Hotspot and Bluetooth rows say what the device is; Wi-Fi rows give a short reason with its port. The boards already read this way. |
| Find it words | App is right | “Holding” and “Last signal” are right. |
| Magnet | App is right | 12 bars for the 10 seconds. 08m–08r redrawn. |
| Wording | Need from you | The app’s shorter copy wins. Send strings.xml or the new COPY.md and every PNG is exported again with it. |
| Toasts | App is right | 12 dp above the dock or nav. Never over a sheet, never on the viewfinder. |
| Tablets: back button | Change the app | A pane’s first screen shows no back button while both panes are on screen; deeper screens in the pane keep it. Home’s centred left column is right. |
| Launcher shortcuts | Need from you | Drawn as 02l: Solar icon in brand blue on the light blue circle. Tell me the four if they differ. |
| Stop, paused, aeroplane, dropped, stopped | App is right | Built as drawn. Only the copy differs (see Wording). |
| App start | App is right | Android’s splash only. 02h now shows that and nothing after it. |
| Tab titles | App is right | 24 dp for the title, 16 dp for round buttons. Keep. |
| Checklist items | Need from you | The app’s lists win. Send the item names per place and the icon board is finished to match. |

## Boards updated on 2026-11-16

03 and 03a–03h, E03, E07, E08 (the sweep's check row), 13c (Language: closer rows, 52 dp, 56 dp with a second line),
15 and 15a (Help numbers: the search stays, the countries scroll under it). Rendered from the canvas's HTML at 2×.
In 15 the canvas's markup had the rows 16 dp in from the sheet's edges (the list sat inside the search field's
wrapper); the PNG here is rendered with that fixed, so the rows run edge to edge as in 13c. The canvas still needs
the same fix.

## Rules decided

| Rule | Decision |
|---|---|
| Tablets and landscape | From 840 dp: a navigation rail and two panes for Home, results, Reports and More. Between 600 and 840 dp: one centred column, 640 dp at most. Phone landscape: the lens on the left, the list on the right. |
| Lens finder | Picture and panel. The picture starts at the top edge, behind the status bar and the top bar. Every control sits on a rounded panel below it (beside it in landscape), so a ring is never covered. Only the top bar and the spot count sit on the picture. |
| Loading states | None added. Android’s splash, then the app. The camera says “Opening the camera”; export turns the button into “Saving the PDF…”. |
| Find it: which word is right | The build is. Word, pills and signal share one scale: Right here from −50 dBm, Close to −62, Near to −74, Far below. |
| Torch “on” colour | Primary, as 08 draws it. On the dark viewfinder it has to read as on at a glance. |
| Segmented | One control. In a box or a form it fills the width; floating over the viewfinder it hugs its labels. |
| LensProgress size | Two sizes. 220 (core 124) when the core holds a count, as in the sweep. 248 (core 152) when it holds an icon, a value and a line, as 07 and 08 draw Find it and Magnet. The value never wraps and never touches the ring. |
| Events and states | Something that just happened is a Toast (report deleted, marked as known, saved as a PDF, everything deleted), with at most one action. Something that is still true is a Note (an error to fix, a limit, a warning). |
| Note | Four looks: calm (info, and good news with a ✓), warning, error, honesty. The accent container is not used for Notes, so a bright accent never makes a loud block. |
| Not checked | One look everywhere: a grey circle and a muted line. The icon says what it is: the skipped check’s own icon, a memory card, an antenna for other networks, a tag for quiet trackers. An outlined ring in the check row, a grey dot in the PDF. No ✕, no amber. |
| Nothing found | A lens header with the plain result, the rows for what couldn’t be checked, one honesty Note. |
| Text at 200 % | Small text doubles; titles grow less. Buttons grow to 72 dp and stack with the primary on top; tags drop under the text; More tiles go to one column; nav labels, top-bar actions and the lens core stop at 130 %. |
| Check row | In the report: 44 dp circles at 412 dp, shrinking evenly to 36 dp at 360 dp with at least 6 dp between them. In the sweep (03, updated 2026-11-16): 40 dp circles spread edge to edge inside the box's 16 dp padding, a 2 dp ring 2 dp outside the current one, a 20 dp ✓ hanging 5 dp off a finished one; they shrink to 36 dp the same way. Narrower than that, either row scrolls. |
| Toast placement | 12 dp above the dock or the nav bar. Never over a sheet (the sheet closes first) and never on the viewfinder (the photo line and Notes speak there). |
| Right to left | Rows, bars, chevrons and the progress line mirror. The lens glint, the progress arc and the meter’s Far → Right here direction don’t. |

## New rows for COMPONENTS.md

File names are guesses; correct them to where each lives in the code.

| Component | File | Where |
|---|---|---|
| `Note` (changed) | Rows.kt | the one note; four looks (calm for info and good news with a ✓ · warning · error · honesty): 40 dp icon circle centred on the text, optional title, one optional text action; for something that is still true |
| `Toast` (new) | Controls.kt | says something just happened, with at most one action (Undo, Open); 4 s, 12 dp above the dock or nav, swipe left or right to dismiss; never over a sheet or the viewfinder |
| `LensProgress` (changed) | Lens.kt | two sizes: 220 (core 124) for a count; 248 (core 152) when the core holds an icon, a value and a line (Find it, Magnet) |
| `FinderPanel` (new) | LensFinderScreen.kt | the rounded panel under the camera picture (beside it in landscape, 272 dp wide) that holds filters, Lens · Infrared, the one-line hint, the photos-saved line and Switch camera · shutter · torch; 288 dp tall in portrait, 28 dp corners |

## Files

| File | What it shows | Size (dp) |
|---|---|---|
| `screens/00-design-system-additions.png` | Design system: components that existed only in code, plus Note and Toast | 1760 × 1960 |
| `screens/00-focus-states.png` | Touch, pressed and focus states | 1760 × 1180 |
| `screens/00-decisions.png` | Answers to the open questions | 1760 × 960 |
| `screens/00-review-2.png` | Design review, round 2: every line answered | 1760 × 1320 |
| `screens/00-checklist-icons.png` | Checklist items: icon and hue per item | 1760 × 760 |
| `screens/01-welcome.png` | Welcome | 412 × 892 |
| `screens/02-home.png` | Home | 412 × 892 |
| `screens/02b-home-visit.png` | Home: this visit so far and last report | 412 × 892 |
| `screens/02c-home-nothing-found.png` | Home: last report, nothing found | 412 × 892 |
| `screens/02d-home-dark.png` | Home dark | 412 × 892 |
| `screens/02e-home-first-run.png` | Home: first run, no reports yet | 412 × 892 |
| `screens/02f-home-360.png` | Home at 360 dp | 360 × 780 |
| `screens/02g-home-200.png` | Home at 200 % text | 412 × 892 |
| `screens/02h-home-loading.png` | App start: Android’s own splash, nothing after it | 412 × 892 |
| `screens/02i-home-check-pressed.png` | Home: a check pressed | 412 × 892 |
| `screens/02j-home-check-focused.png` | Home: a check focused | 412 × 892 |
| `screens/02k-home-start-pressed.png` | Home: Start pressed | 412 × 892 |
| `screens/02l-launcher-shortcuts.png` | Launcher shortcuts (long press on the app icon) | 412 × 892 |
| `screens/03-sweep-wifi.png` | Sweep: checking the Wi-Fi | 412 × 892 |
| `screens/03a-sweep-wifi-empty.png` | Sweep: nothing has answered yet | 412 × 892 |
| `screens/03b-sweep-not-on-wifi.png` | Sweep: not on Wi-Fi | 412 × 892 |
| `screens/03c-sweep-permission.png` | Sweep: permission needed | 412 × 892 |
| `screens/03d-sweep-permission-settings.png` | Sweep: permission denied twice | 412 × 892 |
| `screens/03e-sweep-radio-off.png` | Sweep: Bluetooth switched off | 412 × 892 |
| `screens/03f-sweep-no-hardware.png` | Sweep: this phone has no Bluetooth | 412 × 892 |
| `screens/03g-sweep-hotspots.png` | Sweep: hotspots, total not known yet | 412 × 892 |
| `screens/03h-sweep-location-off.png` | Sweep: Location switched off | 412 × 892 |
| `screens/04-wifi-results.png` | Wi-Fi results | 412 × 892 |
| `screens/04a-wifi-nothing-found.png` | Wi-Fi results: nothing found | 412 × 892 |
| `screens/04b-wifi-hidden-name.png` | Wi-Fi results: name hidden, not recognised group | 412 × 892 |
| `screens/04c-wifi-results-200.png` | Wi-Fi results at 200 % text | 412 × 892 |
| `screens/04d-other-devices.png` | Other devices | 412 × 892 |
| `screens/04e-other-devices-empty.png` | Other devices: empty | 412 × 892 |
| `screens/04f-hotspot-results.png` | Hotspot results | 412 × 892 |
| `screens/04g-bluetooth-nothing-found.png` | Bluetooth results: nothing found | 412 × 892 |
| `screens/04h-hotspot-zero.png` | Hotspot results: zero networks | 412 × 892 |
| `screens/04i-other-networks.png` | Other networks | 412 × 892 |
| `screens/05-device.png` | Device: likely camera | 412 × 892 |
| `screens/05a-device-not-recognised.png` | Device: not recognised | 412 × 892 |
| `screens/05b-device-this-phone.png` | Device: this phone, no dock | 412 × 892 |
| `screens/05c-device-known.png` | Device: marked as known, no dock | 412 × 892 |
| `screens/06-not-a-camera-sheet.png` | Not a camera? sheet | 412 × 892 |
| `screens/06a-not-a-camera-not-sure.png` | Not a camera? sheet: Not sure turns the button to Done | 412 × 892 |
| `screens/06b-not-a-camera-dark.png` | Not a camera? sheet dark | 412 × 892 |
| `screens/06c-not-a-camera-200.png` | Not a camera? sheet at 200 % text | 412 × 892 |
| `screens/07-find-it.png` | Find it: stronger | 412 × 892 |
| `screens/07a-find-it-weaker.png` | Find it: weaker | 412 × 892 |
| `screens/07b-find-it-steady-added.png` | Find it: steady, right here, added to report | 412 × 892 |
| `screens/07c-find-it-not-heard.png` | Find it: not heard for a while | 412 × 892 |
| `screens/07d-find-it-200.png` | Find it at 200 % text | 412 × 892 |
| `screens/08-lens-finder.png` | Lens finder | 412 × 892 |
| `screens/08a-lens-permission.png` | Lens finder: camera permission | 412 × 892 |
| `screens/08b-lens-camera-in-use.png` | Lens finder: camera in use | 412 × 892 |
| `screens/08c-lens-camera-failed.png` | Lens finder: camera failed | 412 × 892 |
| `screens/08d-lens-no-camera.png` | Lens finder: no camera | 412 × 892 |
| `screens/08e-lens-starting.png` | Lens finder: camera starting | 412 × 892 |
| `screens/08f-lens-infrared.png` | Infrared finder | 412 × 892 |
| `screens/08g-infrared-may-not-see.png` | Infrared finder: this phone may not see infrared | 412 × 892 |
| `screens/08h-lens-photo-failed.png` | Lens finder: the photo couldn’t be saved | 412 × 892 |
| `screens/08i-lens-sweep.png` | Lens finder in a sweep: Next, torch off, 0 spots, photos saved | 412 × 892 |
| `screens/08j-lens-tips-sheet.png` | Lens finder: tips sheet | 412 × 892 |
| `screens/08k-lens-tv-remote-sheet.png` | Infrared finder: TV remote sheet | 412 × 892 |
| `screens/08z-lens-low-spot.png` | Lens finder: a spot low in the frame stays clear of the controls | 412 × 892 |
| `screens/08l-lens-360.png` | Lens finder at 360 dp | 360 × 780 |
| `screens/08m-magnet-learning.png` | Magnet: learning | 412 × 892 |
| `screens/08n-magnet-reading.png` | Magnet: about normal | 412 × 892 |
| `screens/08o-magnet-jump.png` | Magnet: electronics nearby | 412 × 892 |
| `screens/08p-magnet-added-sweep.png` | Magnet in a sweep: added to report, Next | 412 × 892 |
| `screens/08q-magnet-no-sensor.png` | Magnet: no magnet sensor | 412 × 892 |
| `screens/08r-magnet-200.png` | Magnet at 200 % text | 412 × 892 |
| `screens/09-checklist.png` | Checklist | 412 × 892 |
| `screens/09a-checklist-answered.png` | Checklist: answered rows | 412 × 892 |
| `screens/09b-checklist-answer-sheet.png` | Checklist: answer sheet | 412 × 892 |
| `screens/09c-checklist-not-sure-sheet.png` | Checklist: Not sure with note and photo | 412 × 892 |
| `screens/09d-checklist-200.png` | Checklist at 200 % text | 412 × 892 |
| `screens/10-report.png` | Report: 1 to look at (top of the screen) | 412 × 892 |
| `screens/10a-report-nothing-found.png` | Report: nothing found | 412 × 892 |
| `screens/10b-report-full.png` | Report, full length: header · check row · Found · Photos · Marked as known · Couldn’t check · note · dock | 412 × 1572 |
| `screens/10c-report-rename-sheet.png` | Report: rename sheet | 412 × 892 |
| `screens/10d-report-undo-sheet.png` | Report: undo a mark sheet | 412 × 892 |
| `screens/10e-finding-detail.png` | Finding detail: no photos section | 412 × 892 |
| `screens/10f-photo-full.png` | Photo, full screen | 412 × 892 |
| `screens/10g-report-pdf.png` | The PDF, A4 | 595 × 842 |
| `screens/10h-report-export-saved.png` | Report: saved as a PDF, said by a toast | 412 × 892 |
| `screens/10i-report-export-failed.png` | Report: the PDF couldn’t be saved, an error Note that stays | 412 × 892 |
| `screens/10j-report-exporting.png` | Report: while the PDF is written | 412 × 892 |
| `screens/10k-report-200.png` | Report header at 200 % text | 412 × 892 |
| `screens/10l-report-360.png` | Report at 360 dp: check circles shrink to 36 dp | 360 × 780 |
| `screens/11-if-you-find-a-camera.png` | If you find a camera | 412 × 892 |
| `screens/11a-found-camera-no-report.png` | If you find a camera: opened without a report, police and helpline rows | 412 × 892 |
| `screens/15-help-numbers-sheet.png` | Help numbers sheet | 412 × 892 |
| `screens/15a-help-numbers-no-match.png` | Help numbers sheet: no country matches | 412 × 892 |
| `screens/15b-help-numbers-region-unknown.png` | Help numbers: phone region unknown | 412 × 892 |
| `screens/12-reports.png` | Reports | 412 × 892 |
| `screens/12a-reports-empty.png` | Reports: no reports yet | 412 × 892 |
| `screens/12b-reports-search.png` | Reports: search | 412 × 892 |
| `screens/12c-reports-no-match.png` | Reports: no report matches | 412 × 892 |
| `screens/12d-reports-delete-sheet.png` | Reports: delete sheet after a long press | 412 × 892 |
| `screens/12e-reports-dark.png` | Reports dark | 412 × 892 |
| `screens/12f-reports-200.png` | Reports at 200 % text | 412 × 892 |
| `screens/13-more.png` | More | 412 × 892 |
| `screens/13a-more-dark.png` | More dark | 412 × 892 |
| `screens/13b-more-200.png` | More at 200 % text: tiles in one column | 412 × 892 |
| `screens/13c-language-sheet.png` | Language sheet | 412 × 892 |
| `screens/13d-marked-as-known.png` | Marked as known | 412 × 892 |
| `screens/13e-marked-empty.png` | Marked as known: nothing marked yet | 412 × 892 |
| `screens/13f-how-it-works.png` | How Unpeep works | 412 × 892 |
| `screens/13g-cant-find.png` | What Unpeep can’t find | 412 × 892 |
| `screens/14-appearance.png` | Appearance, full length: theme fills the width, font rows with System default, text-size slider | 412 × 1044 |
| `screens/14a-text-size-large.png` | Appearance: text size dragged to Large | 412 × 892 |
| `screens/14b-sweep-steps.png` | Sweep steps | 412 × 892 |
| `screens/14c-sweep-steps-dragging.png` | Sweep steps: a row being dragged | 412 × 892 |
| `screens/16-privacy.png` | Privacy: redesigned permission card | 412 × 892 |
| `screens/16a-privacy-delete-sheet.png` | Privacy: delete all reports sheet | 412 × 892 |
| `screens/16b-privacy-deleted.png` | Privacy: everything was deleted, said by a toast | 412 × 892 |
| `screens/17-about.png` | About, full length: round app icons, mid apps contact row, credit line | 412 × 1100 |
| `screens/17a-update-checking.png` | About: checking for updates | 412 × 892 |
| `screens/17b-update-latest.png` | About: on the latest version | 412 × 892 |
| `screens/17c-update-available.png` | About: new version | 412 × 892 |
| `screens/17d-update-failed.png` | About: couldn’t check | 412 × 892 |
| `screens/18-licences.png` | Licences (CameraX row removed) | 412 × 892 |
| `screens/18a-licence-text.png` | Licence text | 412 × 892 |
| `screens/E01-scan-limit.png` | Wi-Fi: Android’s scan limit reached | 412 × 892 |
| `screens/E02-client-isolation.png` | Wi-Fi: only the router answered | 412 × 892 |
| `screens/E03-scan-failed.png` | Sweep: the Wi-Fi dropped during the scan | 412 × 892 |
| `screens/E04-camera-blocked.png` | Lens finder: camera permission denied twice | 412 × 892 |
| `screens/E05-sensor-stopped.png` | Magnet: readings stopped | 412 × 892 |
| `screens/E06-results-gone.png` | Results opened after the app was closed: back to Home with a toast | 412 × 892 |
| `screens/E07-leave-sweep-sheet.png` | Sweep: stop sheet | 412 × 892 |
| `screens/E08-sweep-paused.png` | Sweep: paused after a call or another app | 412 × 892 |
| `screens/E09-everything-off.png` | Sweep: aeroplane mode | 412 × 892 |
| `screens/E10-long-names.png` | Long and repeated names | 412 × 892 |
| `screens/E11-many-found.png` | Wi-Fi: a very large network | 412 × 892 |
| `screens/E12-no-checks-ran.png` | Report: every check was skipped | 412 × 892 |
| `screens/E13-report-gone.png` | A report that no longer exists: back to Reports with a toast | 412 × 892 |
| `screens/E14-errors-dark.png` | Notes in dark | 412 × 892 |
| `screens/E15-toasts.png` | Toast with Undo, over the nav bar | 412 × 892 |
| `screens/E15a-toast-swipe.png` | Toast: being swiped away | 412 × 892 |
| `screens/E15b-toast-dock.png` | Toast over a dock: marked as known | 412 × 892 |
| `screens/E16-rtl.png` | Right to left | 412 × 892 |
| `screens/E17-german.png` | German lengths: labels fit or end in “…” | 412 × 892 |
| `screens/T01-home-tablet.png` | Home on a tablet (from 840 dp) | 1024 × 768 |
| `screens/T02-reports-two-pane.png` | Reports on a tablet: list and report | 1024 × 768 |
| `screens/T03-results-two-pane.png` | Wi-Fi results on a tablet | 1024 × 768 |
| `screens/T04-more-master-detail.png` | More on a tablet: list and Appearance | 1024 × 768 |
| `screens/T05-lens-landscape.png` | Lens finder on its side: every portrait control, on a side panel | 892 × 412 |
| `screens/T05a-infrared-landscape-sweep.png` | Infrared finder on its side, in a sweep: Next in the top bar | 892 × 412 |
| `screens/T06-sweep-landscape.png` | Sweep in phone landscape: lens left, list right | 892 × 412 |
| `screens/T07-centred-column.png` | Between 600 and 840 dp: one centred column, 640 dp at most | 720 × 892 |
