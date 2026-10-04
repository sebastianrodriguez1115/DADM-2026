#!/usr/bin/env python3
"""Independent scan of board-only PNG; Python stdlib, no QA dependencies."""
import json
from pathlib import Path
import struct
import zlib

OUT = Path(__file__).resolve().parent / "evidence"
data = (OUT / "board-empty.png").read_bytes()
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
    for i in range(stride):
        left = row[i-bpp] if i >= bpp else 0
        above = previous[i]
        corner = previous[i-bpp] if i >= bpp else 0
        predict = 0 if method == 0 else left if method == 1 else above if method == 2 else (left+above)//2 if method == 3 else paeth(left, above, corner)
        assert method in range(5)
        row[i] = (row[i] + predict) & 255
    rows.append(row)
    previous = row

def pixel(x, y):
    return tuple(rows[y][x*bpp:x*bpp+bpp])

grid = pixel(width//3, height//2)

def runs(values):
    found, start = [], None
    for i, matches in enumerate(values + [False]):
        if matches and start is None:
            start = i
        if not matches and start is not None:
            found.append([start, i-1])
            start = None
    return found

vertical = runs([pixel(x, height//2) == grid for x in range(width)])
horizontal = runs([pixel(width//2, y) == grid for y in range(height)])
assert width == height
for size, segments in ((width, vertical), (height, horizontal)):
    assert len(segments) == 2, segments
    for division, (lo, hi) in enumerate(segments, 1):
        assert abs((lo+hi)/2 - size*division/3) <= 1
for x in (width//3, width*2//3):
    assert all(pixel(x, y) == grid for y in range(height))
for y in (height//3, height*2//3):
    assert all(pixel(x, y) == grid for x in range(width))
result = {"result": "PASS", "source": "board-empty.png", "dimensions": [width, height], "grid_color": grid, "vertical_stroke_runs_at_middle_row": vertical, "horizontal_stroke_runs_at_middle_column": horizontal, "centers_at_thirds_tolerance_px": 1, "continuous_full_height_verticals": True, "continuous_full_width_horizontals": True, "limitation": "Counts matching grid color on central axes, supplemented by visual inspection; not arbitrary image line recognition"}
(OUT / "grid-scan-result.json").write_text(json.dumps(result, indent=2))
print("PASS: exactly 2 vertical/2 horizontal color runs at thirds; all 4 full-span lines")
