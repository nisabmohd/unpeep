"""Draws every copy of the Unpeep mark from one geometry (like Burrow's tools/burrow_mark.py).

The mark: a lens (two rings) struck through by one round-capped line from bottom left to top right, with a
clear gap either side of the line. 512 canvas, 36-unit strokes, centred on (256, 256). White on dark blue.
The gap is a clip, not a stroke in the background colour, so the mark sits on any colour.

Every copy gives the mark the same padding as Burrow's and Asusual's (mid apps, 2026-11-17): its longest side is
42 of the 72 dp a launcher shows (15 dp each side), 299 of the store icon's 512 px, 20 of the notification's 24.

usage: python3 tools/unpeep_mark.py            # writes everything below, then run tools/feature_graphic.py
outputs: release/icon-512.svg and icon-512.png (store, a full square; the PNG drawn with headless Chrome),
         design/logo/unpeep-*.svg, and the Android vector drawables
         app/src/main/res/drawable/ic_launcher_foreground.xml, ic_launcher_monochrome.xml, ic_notification.xml
"""
import os
import shutil
import subprocess
import tempfile

BLUE = "#173782"      # hsl(222 70% 30%), the app's primary
WHITE = "#FFFFFF"
C = 256               # centre
RINGS = (152, 84)     # ring radii, to the middle of the stroke
STROKE = 36
LINE = "M120,392 L392,120"
GAP = 18              # clear space either side of the line
MARK = 2 * RINGS[0] + STROKE   # 340: the mark's width and height, stroke included
SHARE = 42 / 72       # the mark's longest side / the side that shows (launcher mask, store icon)
STORE = round(512 * SHARE)     # 299
LAUNCHER_DP = 42
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def clip():
    """Everything but a band along the line: two triangles, one either side. The rings end inside the band's length."""
    h = (STROKE / 2 + GAP) * 2 ** 0.5          # the band's half-width, measured along x + y
    a, b = 2 * C - h, 2 * C + h                # the band is a < x + y < b
    return f"M-512,-512 L{a + 512:.3f},-512 L-512,{a + 512:.3f} Z M1024,1024 L{b - 1024:.3f},1024 L1024,{b - 1024:.3f} Z"

def ring(r):
    """A circle as two arcs: vector drawables have no <circle>."""
    return f"M{C - r},{C} A{r},{r} 0 1,1 {C + r},{C} A{r},{r} 0 1,1 {C - r},{C} Z"

def svg(bg=BLUE, fg=WHITE, rx=112, mark=STORE):
    """The mark `mark` px across, centred on a 512 square (on a tile unless bg is None)."""
    k = mark / MARK
    t = C - C * k
    rect = f'<rect width="512" height="512" rx="{rx}" fill="{bg}"/>' if bg else ""
    rings = "".join(f'<circle cx="{C}" cy="{C}" r="{r}"/>' for r in RINGS)
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">{rect}'
            f'<clipPath id="c"><path d="{clip()}"/></clipPath>'
            f'<g transform="translate({t:.3f} {t:.3f}) scale({k:.5f})">'
            f'<g fill="none" stroke="{fg}" stroke-width="{STROKE}" stroke-linecap="round">'
            f'<g clip-path="url(#c)">{rings}</g><path d="{LINE}"/></g></g></svg>\n')

def vector(viewport, mark_dp, color="#FFFFFF"):
    """Android vector drawable: the mark centred on a square viewport, mark_dp wide and tall."""
    k = mark_dp / MARK
    t = viewport / 2 - C * k
    def path(d):
        return (f'<path android:pathData="{d}" android:strokeColor="{color}" android:strokeWidth="{STROKE}"'
                f' android:strokeLineCap="round" android:fillColor="#00000000"/>')
    rings = "".join(f"\n      {path(ring(r))}" for r in RINGS)
    return (f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{viewport}dp" '
            f'android:height="{viewport}dp" android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">\n'
            f'  <group android:translateX="{t:.4f}" android:translateY="{t:.4f}" android:scaleX="{k:.6f}" android:scaleY="{k:.6f}">\n'
            f'    <group>\n      <clip-path android:pathData="{clip()}"/>{rings}\n    </group>\n'
            f'    {path(LINE)}\n  </group>\n</vector>\n')

def render_png(svg_rel, png_rel, size=512):
    """Draws an SVG to a PNG with headless Chrome."""
    with tempfile.TemporaryDirectory() as tmp:
        page = os.path.join(tmp, "page.html")
        open(page, "w").write(f'<html><body style="margin:0">{open(os.path.join(ROOT, svg_rel)).read()}</body></html>')
        shot = os.path.join(tmp, "shot.png")
        subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
                        f"--window-size={size},{size}", f"--screenshot={shot}", "file://" + page], check=True, capture_output=True)
        shutil.copy(shot, os.path.join(ROOT, png_rel))
    print("wrote", png_rel)

def write(rel, text):
    path = os.path.join(ROOT, rel); os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write(text); print("wrote", rel)

if __name__ == "__main__":
    # The store icon is a full square: Play applies its own rounded mask (as Burrow's and Weft's).
    write("release/icon-512.svg", svg(rx=0))
    render_png("release/icon-512.svg", "release/icon-512.png")
    write("design/logo/unpeep-icon.svg", svg())
    write("design/logo/unpeep-icon-round.svg", svg(rx=256))
    write("design/logo/unpeep-mark-white.svg", svg(bg=None, mark=MARK))
    write("design/logo/unpeep-mark-blue.svg", svg(bg=None, fg=BLUE, mark=MARK))
    # Adaptive icon: 108 dp canvas, mark 42 dp across: 15 dp from each edge of the 72 dp a launcher shows.
    write("app/src/main/res/drawable/ic_launcher_foreground.xml", vector(108, LAUNCHER_DP))
    write("app/src/main/res/drawable/ic_launcher_monochrome.xml", vector(108, LAUNCHER_DP, "#000000"))
    # Notification: 24 dp, mark 20 dp across (as Burrow).
    write("app/src/main/res/drawable/ic_notification.xml", vector(24, 20))
