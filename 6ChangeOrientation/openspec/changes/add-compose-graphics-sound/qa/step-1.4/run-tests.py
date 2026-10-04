#!/usr/bin/env python3
"""Independent native QA. Only sanitized outputs persist in this folder."""
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

QA = Path(__file__).resolve().parent
ROOT = QA.parents[4]
run_name = os.environ.get("QA_EVIDENCE_RUN", "")
assert not run_name or (Path(run_name).name == run_name and run_name not in (".", "..")), "Use a single evidence subfolder name"
OUT = QA / "evidence" / run_name
OUT.mkdir(parents=True, exist_ok=True)
env = os.environ.copy()
env.update(JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home",
           ANDROID_HOME="/Users/jsrc1115/Library/Android/sdk")
adb = str(Path(env["ANDROID_HOME"]) / "platform-tools/adb")
device_lines = subprocess.run([adb, "devices"], capture_output=True, text=True, check=True).stdout.splitlines()[1:]
identifiers = [line.split()[0] for line in device_lines if len(line.split()) >= 2]
assert sum(len(line.split()) >= 2 and line.split()[1] == "device" for line in device_lines) == 1, "Exactly one authorized device required"

def sanitize(text):
    for identifier in identifiers:
        text = text.replace(identifier, "AUTHORIZED_DEVICE")
    return text

summary = []
jobs = [("render", "GameBoardRenderingTest", 3), ("screen", "TicTacToeScreenTest", 15)]
if len(sys.argv) > 1:
    jobs = [job for job in jobs if job[0] == sys.argv[1]]
    assert jobs, "Use render or screen"
for label, cls, expected in jobs:
    command = ["./gradlew", "connectedDebugAndroidTest", "-Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe." + cls, "--no-configuration-cache", "--console=plain"]
    start = time.time()
    result = subprocess.run(command, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (OUT / (label + ".log")).write_text(sanitize(result.stdout))
    cases = []
    for index, source in enumerate(sorted((ROOT / "app/build/outputs/androidTest-results/connected/debug").rglob("TEST-*.xml"))):
        text = sanitize(source.read_text())
        tree = ET.fromstring(text)
        destination = OUT / (label + "-results-" + str(index) + ".xml")
        destination.write_text(text)
        for case in tree.iter("testcase"):
            cases.append({"class": case.get("classname"), "name": case.get("name"), "result": "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS", "evidence": destination.name})
    passed = result.returncode == 0 and len(cases) == expected and all(c["result"] == "PASS" and c["class"] == "com.edu.unal.tictactoe." + cls for c in cases)
    summary.append({"run": label, "command": command, "exit": result.returncode, "seconds": round(time.time()-start, 2), "expected_count": expected, "verified": passed, "cases": cases})
    (OUT / (label + "-summary.json")).write_text(json.dumps(summary[-1], indent=2))
    print(f"{label}: exit={result.returncode}; cases={len(cases)}; verified={passed}", flush=True)
raise SystemExit(0 if all(run["verified"] for run in summary) else 1)
