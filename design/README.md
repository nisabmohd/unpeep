# Keyhole designs

One PNG per screen or state, exported at 2× (412 × 892 dp → 824 × 1784 px unless the size column says otherwise). Light theme unless the name says dark. Lexend 400/500, Solar Linear.

Canvas: https://claude.ai/artifact/JqNdyhYNxSS23rLA9fvNpg

Prefixes: `00` design system · `01`–`18` screens, numbered like the existing PNGs · `E` errors and edge cases · `T` tablet and landscape.

## New rows for COMPONENTS.md

| Component | File | Where |
|---|---|---|
| `Note` (changed) | Rows.kt | the one note, five tones (info · good · warning · error · honesty): 40 dp icon circle centred on the text, optional title, one optional text action; still goes first |
| `Toast` (new) | Controls.kt | a short confirmation, only where an action can be undone or something went away; 4 s, 12 dp above the dock or nav, swipe left or right to dismiss, optional Undo |

## Files

| File | What it shows | Size (dp) |
|---|---|---|
| `screens/00-design-system-additions.png` | Design system: components that existed only in code, plus Note and Toast | 1760 × 1960 |
| `screens/00-focus-states.png` | Touch, pressed and focus states | 1760 × 1180 |
| `screens/00-decisions.png` | Answers to the open questions | 1760 × 900 |
| `screens/00-checklist-icons.png` | Checklist items: icon and hue per item | 1760 × 760 |
| `screens/01-welcome.png` | Welcome | 412 × 892 |
| `screens/02-home.png` | Home | 412 × 892 |
| `screens/02b-home-visit.png` | Home: this visit so far and last report | 412 × 892 |
| `screens/02c-home-nothing-found.png` | Home: last report, nothing found | 412 × 892 |
| `screens/02d-home-dark.png` | Home dark | 412 × 892 |
| `screens/02e-home-first-run.png` | Home: first run, no reports yet | 412 × 892 |
| `screens/02f-home-360.png` | Home at 360 dp | 360 × 780 |
| `screens/02g-home-200.png` | Home at 200 % text | 412 × 892 |
| `screens/02h-home-loading.png` | App start: blank until settings load | 412 × 892 |
| `screens/02i-home-check-pressed.png` | Home: a check pressed | 412 × 892 |
| `screens/02j-home-check-focused.png` | Home: a check focused | 412 × 892 |
| `screens/02k-home-start-pressed.png` | Home: Start pressed | 412 × 892 |
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
| `screens/10-report.png` | Report: 1 to look at | 412 × 892 |
| `screens/10a-report-nothing-found.png` | Report: nothing found | 412 × 892 |
| `screens/10b-report-bottom.png` | Report, scrolled: marked as known, couldn’t check, closing note | 412 × 892 |
| `screens/10c-report-rename-sheet.png` | Report: rename sheet | 412 × 892 |
| `screens/10d-report-undo-sheet.png` | Report: undo a mark sheet | 412 × 892 |
| `screens/10e-finding-detail.png` | Finding detail | 412 × 892 |
| `screens/10f-photo-full.png` | Photo, full screen | 412 × 892 |
| `screens/10g-report-pdf.png` | The PDF, A4 | 595 × 842 |
| `screens/10h-report-export-saved.png` | Report: saved as a PDF | 412 × 892 |
| `screens/10i-report-export-failed.png` | Report: the PDF couldn’t be saved | 412 × 892 |
| `screens/10j-report-exporting.png` | Report: while the PDF is written | 412 × 892 |
| `screens/10k-report-200.png` | Report header at 200 % text | 412 × 892 |
| `screens/10l-report-360.png` | Report at 360 dp | 360 × 780 |
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
| `screens/13f-how-it-works.png` | How Keyhole works | 412 × 892 |
| `screens/13g-cant-find.png` | What Keyhole can’t find | 412 × 892 |
| `screens/14-appearance.png` | Appearance | 412 × 892 |
| `screens/14a-text-size-sheet.png` | Text size sheet | 412 × 892 |
| `screens/14b-sweep-steps.png` | Sweep steps | 412 × 892 |
| `screens/14c-sweep-steps-dragging.png` | Sweep steps: a row being dragged | 412 × 892 |
| `screens/16-privacy.png` | Privacy: redesigned permission card | 412 × 892 |
| `screens/16a-privacy-delete-sheet.png` | Privacy: delete all reports sheet | 412 × 892 |
| `screens/16b-privacy-deleted.png` | Privacy: everything was deleted | 412 × 892 |
| `screens/17-about.png` | About | 412 × 892 |
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
| `screens/T05-lens-landscape.png` | Lens finder in landscape: controls in a side rail | 892 × 412 |
| `screens/T06-sweep-landscape.png` | Sweep in phone landscape: lens left, list right | 892 × 412 |
| `screens/T07-centred-column.png` | Between 600 and 840 dp: one centred column, 640 dp at most | 720 × 892 |
