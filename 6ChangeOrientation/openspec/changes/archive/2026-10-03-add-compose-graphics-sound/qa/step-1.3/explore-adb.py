#!/usr/bin/env python3
"""Own-app-only exploratory capture/touch; never change device settings."""
import json
import re
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

QA = Path(__file__).resolve().parent
ROOT = QA.parents[4]
OUT = QA / "evidence"
ADB = "/Users/jsrc1115/Library/Android/sdk/platform-tools/adb"
PACKAGE = "com.edu.unal.tictactoe"

def adb(*args):
    return subprocess.run([ADB, *args], capture_output=True, check=True)

def own_focus():
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        windows = adb("shell", "dumpsys", "window").stdout.decode()
        if any("mCurrentFocus=" in line and PACKAGE + ".MainActivity" in line for line in windows.splitlines()):
            return
    raise RuntimeError("MainActivity is not focused; no screenshot or touches allowed")

def hierarchy():
    own_focus()
    raw = adb("exec-out", "uiautomator", "dump", "/dev/tty").stdout.decode()
    match = re.search(r"(<\?xml.*?</hierarchy>)", raw, re.S)
    if not match:
        raise RuntimeError("No native hierarchy; no raw output saved")
    root = ET.fromstring(match.group(1))
    # Discard other applications/system chrome entirely.
    return [{key: node.get(key) for key in ("text", "content-desc", "bounds", "clickable", "enabled", "class")}
            for node in root.iter("node") if node.get("package") == PACKAGE]

def bounds(node):
    return [int(v) for v in re.findall(r"\d+", node["bounds"])]

def cells(nodes):
    return sorted([n for n in nodes if (n["content-desc"] or "").startswith("Casilla,")], key=lambda n: n["content-desc"])

def capture(nodes, name):
    board = next(n for n in nodes if n["content-desc"] == "Tablero de tres en raya")
    x1, y1, x2, y2 = bounds(board)
    own_focus()
    temporary = OUT / "own-app-temporary.png"
    try:
        temporary.write_bytes(adb("exec-out", "screencap", "-p").stdout)
        subprocess.run(["sips", "--cropToHeightWidth", str(y2-y1), str(x2-x1), "--cropOffset", str(y1), str(x1), str(temporary), "--out", str(OUT / (name + ".png"))], capture_output=True, check=True)
    finally:
        temporary.unlink(missing_ok=True)

apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
installed = subprocess.run([ADB, "shell", "pm", "path", PACKAGE], capture_output=True).returncode == 0
if not installed:
    adb("install", "-r", str(apk))  # Only delegated project package.
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
before = hierarchy()
(OUT / "native-before.json").write_text(json.dumps(before, indent=2, ensure_ascii=False))
assert len(cells(before)) == 9
assert all(n["content-desc"].endswith("vacía") for n in cells(before)), "Fresh session expected"
capture(before, "board-empty")
target = cells(before)[0]
x1, y1, x2, y2 = bounds(target)
own_focus()
adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
after = hierarchy()
(OUT / "native-after.json").write_text(json.dumps(after, indent=2, ensure_ascii=False))
if len(cells(after)) != 9:
    raise RuntimeError("Own board no longer present after tap; exploratory run blocked, no product defect inferred")
assert sum(n["content-desc"].endswith(", X") for n in cells(after)) == 1
assert sum(n["content-desc"].endswith(", O") for n in cells(after)) == 1
occupied = next(n for n in cells(after) if n["content-desc"].startswith("Casilla, fila 1, columna 1,"))
# UIAutomator exposes the label separately; enabled belongs to its clickable wrapper.
assert any(n["bounds"] == occupied["bounds"] and n["clickable"] == "true" and n["enabled"] == "false" for n in after)
capture(after, "board-after-tap")
own_focus()
adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
rejected = hierarchy()
assert after == rejected, "Occupied cell changed screen state"
for name, nodes in (("before", before), ("after", after), ("occupied-rejected", rejected)):
    (OUT / ("native-" + name + ".json")).write_text(json.dumps(nodes, indent=2, ensure_ascii=False))
(OUT / "exploratory-result.json").write_text(json.dumps({
    "result": "PASS", "checks": ["Own MainActivity focus checked before screenshots/touches", "9 empty accessible cells before", "Native tap row 1 column 1 produces one X and one current synchronous O", "Occupied row 1 column 1 disabled", "Second native tap yields identical own-app hierarchy"],
    "screenshots": ["board-empty.png", "board-after-tap.png"],
    "privacy": "Only own-app hierarchy retained; screenshots cropped to board; raw screenshot deleted; no device identifier retained",
    "settings_changes": "none", "installed": "project app-debug.apk only"
}, indent=2))
print("Exploratory PASS: native tap, occupied rejection, 2 board-only screenshots")
