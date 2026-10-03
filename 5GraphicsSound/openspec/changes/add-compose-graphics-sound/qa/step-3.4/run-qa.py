#!/usr/bin/env python3
"""Run existing Android-native suites; redact device identities from QA evidence."""
import datetime
import json
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[5]
QA = Path(__file__).resolve().parent
EVIDENCE = QA / "evidence"
EVIDENCE.mkdir(parents=True, exist_ok=True)
env = os.environ.copy()
env.setdefault("JAVA_HOME", "/Applications/Android Studio.app/Contents/jbr/Contents/Home")
env.setdefault("ANDROID_HOME", "/Users/jsrc1115/Library/Android/sdk")
adb = Path(env["ANDROID_HOME"]) / "platform-tools/adb"
devices = subprocess.run([str(adb), "devices"], capture_output=True, text=True, check=True)
serials = [line.split()[0] for line in devices.stdout.splitlines()[1:] if len(line.split()) >= 2]

def redact(text):
    for serial in serials:
        text = text.replace(serial, "DEVICE_REDACTED")
    return text

cases = {
    "jvm": ("testDebugUnitTest", None, "app/build/test-results/testDebugUnitTest", 96),
    "screen": ("connectedDebugAndroidTest", "com.edu.unal.tictactoe.TicTacToeScreenTest", "app/build/outputs/androidTest-results/connected/debug", 15),
    "board": ("connectedDebugAndroidTest", "com.edu.unal.tictactoe.GameBoardInteractionTest", "app/build/outputs/androidTest-results/connected/debug", 5),
}
names = sys.argv[1:] or list(cases)
bad = False
for name in names:
    task, klass, reports, expected = cases[name]
    cmd = [str(ROOT / "gradlew"), task, "--no-configuration-cache", "--rerun-tasks", "--console=plain"]
    if klass:
        cmd.append("-Pandroid.testInstrumentationRunnerArguments.class=" + klass)
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    result = subprocess.run(cmd, cwd=ROOT, env=env, capture_output=True, text=True)
    (EVIDENCE / (name + ".log")).write_text(redact(result.stdout + "\n" + result.stderr))
    dest = EVIDENCE / name
    dest.mkdir(exist_ok=True)
    entries = []
    for index, file in enumerate(sorted((ROOT / reports).rglob("TEST-*.xml"))):
        xml = file.read_text()
        (dest / (str(index) + "-" + redact(file.name))).write_text(redact(xml))
        tree = ET.fromstring(xml)
        for case in tree.iter("testcase"):
            if klass and case.get("classname") != klass:
                continue
            status = "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS"
            entries.append({"class": case.get("classname"), "test": case.get("name"), "status": status})
    summary = {"suite": name, "started_utc": started, "finished_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(), "command": cmd, "exit": result.returncode, "expected": expected, "tests": len(entries), "pass": sum(e["status"] == "PASS" for e in entries), "fail": sum(e["status"] == "FAIL" for e in entries), "skip": sum(e["status"] == "SKIP" for e in entries), "cases": entries}
    ok = result.returncode == 0 and summary["tests"] == expected and summary["pass"] == expected
    summary["accepted"] = ok
    (EVIDENCE / (name + "-summary.json")).write_text(json.dumps(summary, indent=2))
    print(json.dumps({k: v for k, v in summary.items() if k != "cases"}), flush=True)
    bad |= not ok
sys.exit(1 if bad else 0)
