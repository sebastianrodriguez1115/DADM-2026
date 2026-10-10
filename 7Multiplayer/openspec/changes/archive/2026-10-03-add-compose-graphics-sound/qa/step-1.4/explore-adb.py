#!/usr/bin/env python3
"""Native own-app exploratory capture; never change global settings."""
import json
import os
import re
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

QA = Path(__file__).resolve().parent
ROOT = QA.parents[4]
run_name = os.environ.get("QA_EVIDENCE_RUN", "")
assert not run_name or (Path(run_name).name == run_name and run_name not in (".", "..")), "Use a single evidence subfolder name"
OUT = QA / "evidence" / run_name
OUT.mkdir(parents=True, exist_ok=True)
ADB = "/Users/jsrc1115/Library/Android/sdk/platform-tools/adb"
PACKAGE = "com.edu.unal.tictactoe"

def adb(*args):
    result = subprocess.run([ADB, *args], capture_output=True)
    if result.returncode:
        raise RuntimeError("ADB command failed; raw device output not retained")
    return result.stdout

def own_focus():
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        windows = adb("shell", "dumpsys", "window").decode()
        if any("mCurrentFocus=" in line and PACKAGE + ".MainActivity" in line for line in windows.splitlines()):
            return
    raise RuntimeError("Own MainActivity not focused; no capture or touches allowed")

def hierarchy():
    own_focus()
    raw = adb("exec-out", "uiautomator", "dump", "/dev/tty").decode()
    match = re.search(r"(<\?xml.*?</hierarchy>)", raw, re.S)
    if not match:
        raise RuntimeError("Native hierarchy unavailable; no raw output retained")
    root = ET.fromstring(match.group(1))
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
        temporary.write_bytes(adb("exec-out", "screencap", "-p"))
        subprocess.run(["sips", "--cropToHeightWidth", str(y2-y1), str(x2-x1), "--cropOffset", str(y1), str(x1), str(temporary), "--out", str(OUT / (name + ".png"))], capture_output=True, check=True)
    finally:
        temporary.unlink(missing_ok=True)

device_lines = adb("devices").decode().splitlines()[1:]
assert sum(len(line.split()) >= 2 and line.split()[1] == "device" for line in device_lines) == 1, "One authorized device required; identifiers omitted"
adb("install", "-r", str(ROOT / "app/build/outputs/apk/debug/app-debug.apk"))
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
before = hierarchy()
assert len(cells(before)) == 9 and all(n["content-desc"].endswith("vacía") for n in cells(before))
capture(before, "board-empty")
x1, y1, x2, y2 = bounds(cells(before)[0])
own_focus()
adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
after = hierarchy()
assert len(cells(after)) == 9
assert sum(n["content-desc"].endswith(", X") for n in cells(after)) == 1
assert sum(n["content-desc"].endswith(", O") for n in cells(after)) == 1
assert not any(n["text"] in ("X", "O") for n in after)
occupied = next(n for n in cells(after) if n["content-desc"].startswith("Casilla, fila 1, columna 1,"))
assert any(n["bounds"] == occupied["bounds"] and n["clickable"] == "true" and n["enabled"] == "false" for n in after)
capture(after, "board-vector-marks")
own_focus()
adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
rejected = hierarchy()
assert after == rejected, "Occupied tap changed own-app state"
for name, nodes in (("before", before), ("after", after), ("occupied-rejected", rejected)):
    (OUT / ("native-" + name + ".json")).write_text(json.dumps(nodes, indent=2, ensure_ascii=False))
(OUT / "exploratory-result.json").write_text(json.dumps({"result": "PASS", "checks": ["nine empty descriptions", "native tap produces one X and one synchronous O", "no visible Text X/O", "occupied cell disabled; repeat tap unchanged"], "screenshots": ["board-empty.png", "board-vector-marks.png"], "privacy": "Own-app hierarchy only; board-only crops; no identifiers", "settings_changes": "none", "installation": "Only project debug APK"}, indent=2))
print("Native exploratory PASS; review board-vector-marks.png for geometry")
