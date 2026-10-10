#!/usr/bin/env python3
"""Board-only native PNG geometry checks (stdlib; no global/device changes)."""
import json
import math
import os
from pathlib import Path
import re
import struct
import zlib

QA = Path(__file__).resolve().parent
run_name = os.environ.get("QA_EVIDENCE_RUN", "retry-1")
assert Path(run_name).name == run_name and run_name not in (".", "..")
OUT = QA / "evidence" / run_name
data = (OUT / "board-vector-marks.png").read_bytes()
assert data[:8] == b"\x89PNG\r\n\x1a\n"
pos, compressed = 8, bytearray()
while pos < len(data):
    length = struct.unpack(">I", data[pos:pos+4])[0]
    kind, payload = data[pos+4:pos+8], data[pos+8:pos+8+length]
    if kind == b"IHDR":
        width, height, depth, color, compression, filtering, interlace = struct.unpack(">IIBBBBB", payload)
    elif kind == b"IDAT":
        compressed.extend(payload)
    pos += length + 12
assert depth == 8 and color in (2, 6) and interlace == 0
bpp = 3 if color == 2 else 4
stride = width * bpp
raw = zlib.decompress(compressed)
rows, previous = [], bytearray(stride)

def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
    return a if pa <= pb and pa <= pc else b if pb <= pc else c

for y in range(height):
    start = y * (stride + 1)
    method = raw[start]
    row = bytearray(raw[start+1:start+1+stride])
    assert method in range(5)
    for i in range(stride):
        left = row[i-bpp] if i >= bpp else 0
        above = previous[i]
        corner = previous[i-bpp] if i >= bpp else 0
        predict = 0 if method == 0 else left if method == 1 else above if method == 2 else (left+above)//2 if method == 3 else paeth(left, above, corner)
        row[i] = (row[i] + predict) & 255
    rows.append(row)
    previous = row

def pixel(x, y):
    return tuple(rows[y][x*bpp:x*bpp+3])

def bounds(node):
    return [int(v) for v in re.findall(r"\d+", node["bounds"])]

nodes = json.loads((OUT / "native-after.json").read_text())
bx, by, br, bb = bounds(next(n for n in nodes if n["content-desc"] == "Tablero de tres en raya"))
assert width == height == br-bx == bb-by
palettes = {"light": {"X": (20, 108, 46), "O": (179, 38, 30)}, "dark": {"X": (128, 218, 149), "O": (255, 180, 171)}}
results = []
for mark in ("X", "O"):
    node = next(n for n in nodes if n["content-desc"].startswith("Casilla,") and n["content-desc"].endswith(", " + mark))
    x1, y1, x2, y2 = bounds(node)
    x1, x2, y1, y2 = x1-bx, x2-bx, y1-by, y2-by
    cw, ch = x2-x1, y2-y1
    assert abs(cw-ch) <= 1
    cx, cy = (x1+x2-1)/2, (y1+y2-1)/2
    counts = {theme: sum(pixel(x, y) == palette[mark] for y in range(y1, y2) for x in range(x1, x2)) for theme, palette in palettes.items()}
    theme = max(counts, key=counts.get)
    expected = palettes[theme][mark]
    points = [(x, y) for y in range(y1, y2) for x in range(x1, x2) if pixel(x, y) == expected]
    assert len(points) > 100
    lo_x, hi_x = min(x for x,y in points), max(x for x,y in points)
    lo_y, hi_y = min(y for x,y in points), max(y for x,y in points)
    mw, mh = hi_x-lo_x+1, hi_y-lo_y+1
    assert abs(mw-mh) <= 2
    assert abs((lo_x+hi_x)/2-cx) <= 1 and abs((lo_y+hi_y)/2-cy) <= 1
    assert .45 < mw/cw < .65 and .45 < mh/ch < .65
    assert lo_x > x1 and hi_x < x2-1 and lo_y > y1 and hi_y < y2-1
    if mark == "X":
        deviations = [min(abs((x-cx)-(y-cy)), abs((x-cx)+(y-cy))) for x,y in points]
        assert max(deviations) <= .085 * min(cw,ch)
        delta = .20 * min(cw,ch)
        assert all(pixel(round(cx+sx*delta), round(cy+sy*delta)) == expected for sx in (-1,1) for sy in (-1,1))
        assert pixel(round(cx), round(cy)) == expected
        assert pixel(round(cx+delta), round(cy)) != expected
        assert pixel(round(cx), round(cy+delta)) != expected
        details = {"two_diagonal_arms_sampled": True, "all_exact_color_pixels_within_diagonal_bands": True, "max_band_deviation_px": max(deviations)}
    else:
        radii = [math.hypot(x-cx,y-cy) for x,y in points]
        assert min(radii) >= .205 * min(cw,ch) and max(radii) <= .295 * min(cw,ch)
        assert pixel(round(cx), round(cy)) != expected
        assert all(pixel(round(cx+sx*.25*cw), round(cy+sy*.25*ch)) == expected for sx,sy in ((1,0),(-1,0),(0,1),(0,-1)))
        details = {"hollow_center": True, "four_cardinal_ring_points_sampled": True, "exact_color_radial_range_px": [round(min(radii),3), round(max(radii),3)]}
    results.append({"mark": mark, "theme_palette": theme, "rgb": expected, "cell_size_px": [cw,ch], "mark_size_px": [mw,mh], "relative_size": [round(mw/cw,4),round(mh/ch,4)], "centered_unclipped": True, "details": details})
result = {"result": "PASS", "source": "board-vector-marks.png", "board_size_px": [width,height], "marks": results, "limitation": "Single native size/current theme; exact-color geometry samples supplemented by human inspection. Other theme tested by Compose instrumentation; not proof of all screen sizes."}
(OUT / "mark-geometry-result.json").write_text(json.dumps(result, indent=2))
print("PASS: native X two diagonal bands; O circular hollow annulus; both centered, square bounds and unclipped")
