"""Builds the Play Store feature graphic (1024 x 500) in the look Burrow and Weft use (all three are by mid
apps): the logo's blue as the page, a pale rounded tile with the mark in blue, the name in white and two
short lines under it. The tile and the text are centred on the page as one group, and the mark takes the
share of the tile it takes of a launcher icon (42 of 72), as in Burrow and Asusual.

usage: python3 tools/feature_graphic.py [out file]
Writes release/feature-graphic-1024x500.png and the fastlane copy. Needs Pillow; the mark is taken
from release/icon-512.png (tools/unpeep_mark.py), so it is the store icon's drawing."""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTS = [sys.argv[1]] if len(sys.argv) > 1 else [
    os.path.join(ROOT, "release", "feature-graphic-1024x500.png"),
    os.path.join(ROOT, "fastlane", "metadata", "android", "en-US", "images", "featureGraphic.png"),
]
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "lexend.ttf")
W, H = 1024, 500
SS = 2                                  # drawn at twice the size, then scaled down

BLUE = (0x17, 0x37, 0x82)              # the launcher's blue (`Brand` in Color.kt)
TILE = (0xD9, 0xE3, 0xFA)               # a pale tint of it, like Burrow's pale tile
NAME = (0xFF, 0xFF, 0xFF)
LINES = (0xD9, 0xE3, 0xFA)

TILE_Y, TILE_SIZE, TILE_R = 150, 200, 48   # Burrow's tile
GAP = 41                                  # between the tile and the text
SHARE = 42 / 72                           # the mark's longest side / the tile
NAME_TEXT = "Unpeep"
LINE_TEXTS = ["Check a room for hidden cameras.", "Private. Honest. Open source."]


def font(size, weight):
    f = ImageFont.truetype(FONT, size * SS)
    f.set_variation_by_axes([weight])
    return f


def mark(px, color):
    """The mark from release/icon-512.png (white on the blue tile), `px` wide, in [color]."""
    icon = Image.open(os.path.join(ROOT, "release", "icon-512.png")).convert("L")
    lo, hi = icon.getpixel((4, 4)), icon.getextrema()[1]
    m = icon.point(lambda v: max(0, min(255, round((v - lo) * 255 / (hi - lo)))))
    # The mark's own box (it is centred on the icon), so it can be sized on its own.
    m = m.crop(m.getbbox())
    # The mark is square; any other shape would keep its own, `px` tall.
    m = m.resize((round(px * m.width / m.height), px), Image.LANCZOS)
    out = Image.new("RGBA", m.size, color + (0,))
    out.putalpha(m)
    return out


# Centre the tile and the text as one group.
text_w = max(font(96, 600).getlength(NAME_TEXT), *(font(32, 400).getlength(t) for t in LINE_TEXTS)) / SS
TILE_X = round((W - (TILE_SIZE + GAP + text_w)) / 2)
TEXT_X = TILE_X + TILE_SIZE + GAP

img = Image.new("RGBA", (W * SS, H * SS), BLUE + (255,))
d = ImageDraw.Draw(img)
d.rounded_rectangle(
    (TILE_X * SS, TILE_Y * SS, (TILE_X + TILE_SIZE) * SS - 1, (TILE_Y + TILE_SIZE) * SS - 1),
    TILE_R * SS, fill=TILE + (255,),
)
# The mark at the launcher's share of the tile, centred exactly.
size = round(TILE_SIZE * SHARE) * SS
m = mark(size, BLUE)
img.alpha_composite(m, (TILE_X * SS + (TILE_SIZE * SS - m.width) // 2, TILE_Y * SS + (TILE_SIZE * SS - m.height) // 2))
d = ImageDraw.Draw(img)
d.text((TEXT_X * SS, 235 * SS), NAME_TEXT, font=font(96, 600), fill=NAME, anchor="ls")
d.text((TEXT_X * SS, 297 * SS), LINE_TEXTS[0], font=font(32, 400), fill=LINES, anchor="ls")
d.text((TEXT_X * SS, 341 * SS), LINE_TEXTS[1], font=font(32, 400), fill=LINES, anchor="ls")

final = img.convert("RGB").resize((W, H), Image.LANCZOS)
for out in OUTS:
    os.makedirs(os.path.dirname(out), exist_ok=True)
    final.save(out, optimize=True)
    print(out)
