#!/usr/bin/env python3
"""Run only delegated project tests; preserve sanitized evidence within QA."""
import glob
import json
import os
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[5]
QA = Path(__file__).resolve().parent
EVIDENCE = QA / "evidence"
EVIDENCE.mkdir(exist_ok=True)
env = os.environ.copy()
env["JAVA_HOME"] = "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
env["ANDROID_HOME"] = "/Users/jsrc1115/Library/Android/sdk"
adb = str(Path(env["ANDROID_HOME"]) / "platform-tools/adb")
devices = subprocess.run([adb, "devices"], capture_output=True, text=True, env=env, check=True)
serials = [line.split()[0] for line in devices.stdout.splitlines()[1:] if len(line.split()) >= 2]
authorized = [line.split()[0] for line in devices.stdout.splitlines()[1:] if len(line.split()) >= 2 and line.split()[1] == "device"]
if len(authorized) != 1:
    raise SystemExit("Requires exactly one authorized device; no identifiers recorded.")

def sanitize(text):
    for serial in serials:
        text = text.replace(serial, "AUTHORIZED_DEVICE")
    return text

jobs = [
    ("unit", ["./gradlew", "testDebugUnitTest", "--rerun-tasks", "--console=plain"], "app/build/test-results/testDebugUnitTest/TEST-*.xml"),
    ("android-render", ["./gradlew", "connectedDebugAndroidTest", "-Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe.GameBoardRenderingTest", "--no-configuration-cache", "--console=plain"], "app/build/outputs/androidTest-results/connected/debug/**/TEST-*.xml"),
    ("android-screen", ["./gradlew", "connectedDebugAndroidTest", "-Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe.TicTacToeScreenTest", "--no-configuration-cache", "--console=plain"], "app/build/outputs/androidTest-results/connected/debug/**/TEST-*.xml"),
]
summary = {"device": "one authorized physical Android device; identifiers omitted", "runs": []}
for name, command, pattern in jobs:
    started = time.time()
    result = subprocess.run(command, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (EVIDENCE / (name + ".log")).write_text(sanitize(result.stdout))
    suites = []
    for i, path in enumerate(sorted(glob.glob(str(ROOT / pattern), recursive=True))):
        source = Path(path)
        text = sanitize(source.read_text())
        destination = EVIDENCE / (name + "-results-" + str(i) + ".xml")
        destination.write_text(text)
        tree = ET.fromstring(text)
        nodes = [tree] if tree.tag == "testsuite" else list(tree.iter("testsuite"))
        for suite in nodes:
            suites.append({"name": suite.attrib.get("name"), "tests": suite.attrib.get("tests"), "failures": suite.attrib.get("failures"), "errors": suite.attrib.get("errors"), "skipped": suite.attrib.get("skipped"), "evidence": destination.name, "cases": [dict(case.attrib, result="FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS") for case in suite.findall("testcase")]})
    summary["runs"].append({"kind": name, "command": command, "exit": result.returncode, "elapsed_seconds": round(time.time() - started, 2), "suites": suites})
    (EVIDENCE / "results-summary.json").write_text(json.dumps(summary, indent=2))
    print(name + ": exit=" + str(result.returncode) + ", tests=" + str(sum(int(s["tests"] or 0) for s in suites)), flush=True)
counts = {run["kind"]: sum(int(s["tests"] or 0) for s in run["suites"]) for run in summary["runs"]}
if counts != {"unit": 83, "android-render": 2, "android-screen": 15}:
    raise SystemExit("Expected 83 unit tests and 17 Android tests; inspect evidence before approval.")
if any(run["exit"] != 0 or any(int(s["failures"] or 0) or int(s["errors"] or 0) or int(s["skipped"] or 0) for s in run["suites"]) for run in summary["runs"]):
    raise SystemExit(1)
