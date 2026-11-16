"""Draws every copy of the Keyhole mark from one geometry (like Burrow's tools/burrow_mark.py).

The mark: a lens ring with a glint, opening into two splayed legs, drawn with one 26-unit round-capped
stroke on Burrow's grid (512 canvas, content scaled 1.21318 about the centre). White on dark blue.

usage: python3 tools/keyhole_mark.py            # writes everything below
outputs: release/icon-512.svg (store, a full square; render it to icon-512.png with headless Chrome),
         design/logo/keyhole-*.svg, and the Android vector drawables
         app/src/main/res/drawable/ic_launcher_foreground.xml, ic_launcher_monochrome.xml, ic_notification.xml
"""
import os, re

BLUE = "#173782"      # hsl(222 70% 30%), the app's primary
WHITE = "#FFFFFF"
PATHS = '<circle cx="256" cy="217" r="80"/><path d="M230,292.66 L206,375"/><path d="M282,292.66 L306,375"/><path d="M228,189 h0"/>'
GROUP = 'transform="translate(-54.574 -54.574) scale(1.21318)" fill="none" stroke="{fg}" stroke-width="26" stroke-linecap="round" stroke-linejoin="round"'
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def svg(bg=BLUE, fg=WHITE, rx=112):
    rect = f'<rect width="512" height="512" rx="{rx}" fill="{bg}"/>' if bg else ""
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">{rect}<g {GROUP.format(fg=fg)}>{PATHS}</g></svg>\n'

def path_data():
    out = re.findall(r'd="([^"]+)"', PATHS)
    m = re.search(r'<circle cx="([\d.]+)" cy="([\d.]+)" r="([\d.]+)"', PATHS)
    if m:  # vector drawables have no <circle>: draw the ring as two arcs
        cx, cy, r = map(float, m.groups())
        out.insert(0, f"M{cx-r},{cy} A{r},{r} 0 1,1 {cx+r},{cy} A{r},{r} 0 1,1 {cx-r},{cy} Z")
    return out

MARK_H = 264   # height of the drawing, stroke included, in Burrow-grid units (centred on 256,256)

def vector(viewport, mark_dp, color="#FFFFFF"):
    """Android vector drawable: the mark centred on a square viewport, mark_dp tall."""
    k = 1.21318 * mark_dp / (MARK_H * 1.21318)
    t = viewport / 2 - 256 * k
    paths = "".join(
        f'\n    <path android:pathData="{d}" android:strokeColor="{color}" android:strokeWidth="26"'
        f' android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"/>' for d in path_data())
    return (f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{viewport}dp" '
            f'android:height="{viewport}dp" android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">\n'
            f'  <group android:translateX="{t:.4f}" android:translateY="{t:.4f}" android:scaleX="{k:.6f}" android:scaleY="{k:.6f}">'
            f'{paths}\n  </group>\n</vector>\n')

def write(rel, text):
    path = os.path.join(ROOT, rel); os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write(text); print("wrote", rel)

if __name__ == "__main__":
    # The store icon is a full square: Play applies its own rounded mask (as Burrow's and Weft's).
    write("release/icon-512.svg", svg(rx=0))
    write("design/logo/keyhole-icon.svg", svg())
    write("design/logo/keyhole-icon-round.svg", svg(rx=256))
    write("design/logo/keyhole-mark-white.svg", svg(bg=None))
    write("design/logo/keyhole-mark-blue.svg", svg(bg=None, fg=BLUE))
    # Adaptive icon: 108 dp canvas, mark 44 dp tall inside the 66 dp safe zone (as Burrow).
    write("app/src/main/res/drawable/ic_launcher_foreground.xml", vector(108, 44))
    write("app/src/main/res/drawable/ic_launcher_monochrome.xml", vector(108, 44, "#000000"))
    # Notification: 24 dp, mark 20 dp tall (as Burrow).
    write("app/src/main/res/drawable/ic_notification.xml", vector(24, 20))
