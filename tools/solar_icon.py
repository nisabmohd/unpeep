"""Draws Keyhole's icons with Solar (Linear, by 480 Design, CC BY 4.0), keeping every file name.

Each drawable in ICONS is overwritten with the Solar Linear icon of the same meaning, black (Compose's
Icon and the widgets' colorFilter tint it), viewport 24. Brand marks (launcher, notification) come from tools/keyhole_mark.py.

usage: npm pack @iconify-json/solar && tar xzf iconify-json-solar-*.tgz   # outside the repo
       python3 tools/solar_icon.py <path>/package            # every icon
       python3 tools/solar_icon.py <path>/package ic_check   # one
       python3 tools/solar_icon.py --shortcuts               # launcher shortcut icons, from the ic_* already drawn
"""
import json, os, sys
import xml.etree.ElementTree as ET

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "src", "main", "res", "drawable")

# drawable name → Solar name (Linear style, without the "-linear" suffix)
ICONS = {
    # The seven checks (domain/Check.kt)
    "ic_wifi": "wi-fi-router-minimalistic", "ic_hotspot": "routing-2", "ic_bluetooth": "bluetooth-wave",
    "ic_lens": "flashlight", "ic_infrared": "eye", "ic_magnet": "magnet", "ic_checklist": "checklist-minimalistic",
    # Nav bar
    "ic_radar": "radar-2", "ic_reports": "document-text", "ic_more": "tuning-2",
    # App chrome
    "ic_back": "arrow-left", "ic_close": "close", "ic_check": "check", "ic_chevron": "alt-arrow-right",
    "ic_help": "question-circle", "ic_share": "share", "ic_export": "export", "ic_open": "arrow-right-up",
    "ic_refresh": "refresh", "ic_trash": "trash-bin-trash", "ic_search": "magnifier", "ic_settings": "settings",
    "ic_history": "history", "ic_arrow_up": "arrow-up", "ic_arrow_right": "arrow-right", "ic_drag": "custom:grip",
    "ic_camera": "camera", "ic_gallery": "gallery", "ic_sound": "volume-loud", "ic_filter": "filter",
    # Findings and risk
    "ic_camera_device": "videocamera-record", "ic_warning": "danger-triangle", "ic_danger": "danger-circle",
    "ic_shield": "shield-check", "ic_shield_warning": "shield-warning", "ic_not_checked": "close-circle",
    "ic_smartphone": "smartphone", "ic_tv": "tv", "ic_plug": "plug-circle", "ic_walking": "walking",
    "ic_map_point": "map-point", "ic_mirror": "mirror-left", "ic_lamp": "lamp", "ic_hanger": "hanger-2",
    # Kinds of known device, trackers, Find it
    "ic_laptop": "laptop", "ic_printer": "printer", "ic_speaker": "speaker-minimalistic", "ic_gamepad": "gamepad",
    "ic_server": "server", "ic_home": "home-angle", "ic_tag": "tag", "ic_arrow_down": "arrow-down", "ic_chat": "chat-round-line",
    # Checklist, reports, help
    "ic_pen": "pen", "ic_bed": "bedside-table", "ic_clock": "clock-circle", "ic_wind": "wind", "ic_frame": "gallery-minimalistic",
    "ic_box": "box", "ic_book": "book", "ic_leaf": "leaf", "ic_fire": "fire-minimalistic", "ic_socket": "socket", "ic_usb": "usb",
    "ic_chair": "armchair", "ic_monitor": "monitor", "ic_call": "phone-calling", "ic_camera_add": "camera-add",
    "ic_notes": "notes", "ic_alarm": "alarm", "ic_bulb": "lightbulb-minimalistic", "ic_wheel": "wheel",
    "ic_check_circle": "check-circle", "ic_bluetooth_off": "bluetooth-off", "ic_link": "link", "ic_skip": "minus-circle", "ic_car": "car",
    # Places
    "ic_place_stay": "suitcase", "ic_place_trial_room": "hanger", "ic_place_washroom": "bath",
    "ic_place_office": "buildings", "ic_place_car": "bus",
    # More, settings, about
    "ic_lock": "lock-keyhole", "ic_palette": "palette-round", "ic_globe": "global", "ic_phone": "phone",
    "ic_info": "info-circle", "ic_eye_closed": "eye-closed", "ic_translate": "translation-2", "ic_code": "code",
    "ic_star": "star", "ic_mail": "letter", "ic_heart": "hand-heart", "ic_user_shield": "shield-user",
    "ic_widget": "widget", "ic_structure": "structure",
}
CUSTOM = {
    # The drag handle: a 2×3 grid of dots (Sweep steps), as the design canvas.
    "grip": '<circle cx="9" cy="6" r="1.5" fill="currentColor"/><circle cx="15" cy="6" r="1.5" fill="currentColor"/><circle cx="9" cy="12" r="1.5" fill="currentColor"/><circle cx="15" cy="12" r="1.5" fill="currentColor"/><circle cx="9" cy="18" r="1.5" fill="currentColor"/><circle cx="15" cy="18" r="1.5" fill="currentColor"/>',
}
# An "off" icon: the glyph is cut along the diagonal and a slash drawn through the gap.
SLASH_CLIP = "M2.1,0H24V21.9ZM0,2.1V24H21.9Z"
SLASH = "M3.5,3.5L20.5,20.5"

INHERITED = ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "fill-rule")


def f(v):
    return f"{float(v):g}"


def path_data(kind, a):
    if kind == "path":
        return a["d"]
    if kind in ("circle", "ellipse"):
        cx, cy = float(a["cx"]), float(a["cy"])
        rx, ry = float(a.get("r", a.get("rx"))), float(a.get("r", a.get("ry")))
        return f"M{f(cx - rx)},{f(cy)}a{f(rx)},{f(ry)} 0 1,0 {f(2 * rx)},0a{f(rx)},{f(ry)} 0 1,0 {f(-2 * rx)},0Z"
    if kind == "rect":
        x, y, w, h = (float(a.get(k, 0)) for k in ("x", "y", "width", "height"))
        r = min(float(a.get("rx", a.get("ry", 0))), w / 2, h / 2)
        if not r:
            return f"M{f(x)},{f(y)}h{f(w)}v{f(h)}h{f(-w)}Z"
        return (f"M{f(x + r)},{f(y)}h{f(w - 2 * r)}a{f(r)},{f(r)} 0 0 1 {f(r)},{f(r)}v{f(h - 2 * r)}"
                f"a{f(r)},{f(r)} 0 0 1 {f(-r)},{f(r)}h{f(-(w - 2 * r))}a{f(r)},{f(r)} 0 0 1 {f(-r)},{f(-r)}"
                f"v{f(-(h - 2 * r))}a{f(r)},{f(r)} 0 0 1 {f(r)},{f(-r)}Z")
    raise ValueError(f"unknown element {kind}")


def shapes(body):
    out = []

    def walk(el, alpha, paint):
        a = el.attrib
        if "transform" in a or "clip-path" in a or "mask" in a or "stroke-dasharray" in a:
            raise ValueError(f"unsupported attribute on <{el.tag}>")
        alpha *= float(a.get("opacity", 1))
        paint = {**paint, **{k: a[k] for k in INHERITED if k in a}}
        if el.tag == "g":
            for c in el:
                walk(c, alpha, paint)
        elif el.tag in ("path", "circle", "ellipse", "rect"):
            out.append((path_data(el.tag, a), {**paint, "alpha": alpha}))
        elif el.tag != "defs":
            raise ValueError(f"unknown element <{el.tag}>")

    for el in ET.fromstring(f"<svg>{body}</svg>"):
        walk(el, 1.0, {"fill": "currentColor"})
    return out


def drawable(solar, els, slash=False):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        (f'<!-- Drawn for Keyhole in Solar Linear style ({solar}). Tinted where it is drawn. -->' if "custom:" in solar
         else f'<!-- Solar {solar} (Linear) by 480 Design, CC BY 4.0. Tinted where it is drawn. -->'),
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">',
    ]
    if slash:
        lines.append(f'    <group>\n    <clip-path android:pathData="{SLASH_CLIP}" />')
    for d, p in els:
        attrs = []
        if p.get("fill", "none") != "none":
            attrs.append('android:fillColor="#FF000000"')
            if p.get("fill-rule") == "evenodd":
                attrs.append('android:fillType="evenOdd"')
            if p["alpha"] != 1:
                attrs.append(f'android:fillAlpha="{f(p["alpha"])}"')
        if p.get("stroke", "none") != "none":
            attrs.append('android:strokeColor="#FF000000"')
            attrs.append(f'android:strokeWidth="{p.get("stroke-width", "1")}"')
            if p.get("stroke-linecap") in ("round", "square"):
                attrs.append(f'android:strokeLineCap="{p["stroke-linecap"]}"')
            if p.get("stroke-linejoin") in ("round", "bevel"):
                attrs.append(f'android:strokeLineJoin="{p["stroke-linejoin"]}"')
            if p["alpha"] != 1:
                attrs.append(f'android:strokeAlpha="{f(p["alpha"])}"')
        if attrs:
            lines.append(f'    <path {" ".join(attrs)} android:pathData="{d}" />')
    if slash:
        lines.append("    </group>")
        lines.append(f'    <path android:strokeColor="#FF000000" android:strokeWidth="1.5" android:strokeLineCap="round" android:pathData="{SLASH}" />')
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def main(pkg, names):
    icons = json.load(open(os.path.join(os.path.expanduser(pkg), "icons.json")))["icons"]
    problems = []
    for name in names or sorted(ICONS):
        solar = ICONS[name]
        slash = solar.startswith("slash:")
        base = solar.removeprefix("slash:")
        key = f"{base}-linear"
        try:
            if base.startswith("custom:"):
                els = shapes(CUSTOM[base.removeprefix("custom:")])
            elif key not in icons:
                raise ValueError("not in the set")
            else:
                els = shapes(icons[key]["body"])
        except ValueError as e:
            problems.append(f"{name}: {key}: {e}")
            continue
        open(os.path.join(OUT, f"{name}.xml"), "w").write(drawable(solar, els, slash))
    if problems:
        sys.exit("\n".join(problems))


# Launcher shortcuts (res/xml/shortcuts.xml): shortcut name → the drawn icon whose glyph it shows.
SHORTCUTS = {"sweep": "ic_radar", "lens": "ic_lens", "infrared": "ic_infrared", "reports": "ic_reports"}


def shortcuts():
    """Each shortcut: an adaptive icon (fixed brand container behind, the Solar glyph in front, about 40 dp of 108)."""
    import re
    for short, icon in SHORTCUTS.items():
        src = open(os.path.join(OUT, f"{icon}.xml")).read()
        paths = [re.sub(r'#FF000000', "@color/shortcut_icon", line.strip()) for line in src.splitlines() if line.strip().startswith(("<path", "<group", "</group"))]
        solar = re.search(r"Solar ([\w-]+) \(Linear\)", src).group(1)
        fg = "\n".join([
            '<?xml version="1.0" encoding="utf-8"?>',
            f'<!-- Launcher shortcut "{short}": Solar {solar} (Linear) by 480 Design, CC BY 4.0, from {icon}.xml. Written by tools/solar_icon.py. -->',
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
            '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">',
            '    <group android:translateX="34" android:translateY="34" android:scaleX="1.6667" android:scaleY="1.6667">',
            *["        " + line for line in paths],
            "    </group>",
            "</vector>",
        ]) + "\n"
        open(os.path.join(OUT, f"shortcut_{short}_fg.xml"), "w").write(fg)
        open(os.path.join(OUT, f"shortcut_{short}.xml"), "w").write(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- Launcher shortcut "{short}" (Solar, written by tools/solar_icon.py). -->\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@color/shortcut_background" />\n'
            f'    <foreground android:drawable="@drawable/shortcut_{short}_fg" />\n'
            "</adaptive-icon>\n")


if __name__ == "__main__":
    if sys.argv[1:2] == ["--shortcuts"]:
        shortcuts()
        sys.exit(0)
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2:])
