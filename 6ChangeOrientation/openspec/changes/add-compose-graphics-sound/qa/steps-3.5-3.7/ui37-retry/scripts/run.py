#!/usr/bin/env python3
"""Fresh, separate UI37 class runs; no overwriting historical QA evidence."""
import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

qa = Path(__file__).resolve().parents[1]
root = qa.parents[5]
env = os.environ.copy()
env.setdefault("JAVA_HOME", "/Applications/Android Studio.app/Contents/jbr/Contents/Home")
env.setdefault("ANDROID_HOME", str(Path.home() / "Library/Android/sdk"))
adb = str(Path(env["ANDROID_HOME"]) / "platform-tools/adb")
out = qa / "evidence" / ("run-" + dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ"))
out.mkdir(parents=True, exist_ok=False)
devices = subprocess.run([adb, "devices"], env=env, capture_output=True, text=True)
entries = [line.split() for line in devices.stdout.splitlines()[1:] if len(line.split()) >= 2]
connected = [entry[0] for entry in entries if entry[1] == "device"]
summary = {"started_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "adb_exit_code": devices.returncode, "connected_device_count": len(connected), "runs": [], "jvm": "NOT_RERUN: prior independent 108/108, unchanged core per assignment"}

def save():
    (out / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")

def redact(text):
    for value, replacement in [(str(root), "PROJECT_REDACTED"), (str(Path.home()), "HOME_REDACTED")]:
        text = text.replace(value, replacement)
    for entry in entries:
        text = text.replace(entry[0], "DEVICE_REDACTED")
    text = re.sub(r'hostname="[^"]*"', 'hostname="REDACTED"', text)
    return re.sub(r"\x1b\[[0-9;]*m", "", text)

if devices.returncode or len(connected) != 1:
    summary["status"] = "BLOCKED: need exactly one authorized device"
    save()
    print(summary["status"])
    sys.exit(1)
env["ANDROID_SERIAL"] = connected[0]  # Never stored in artifacts.
for name, count in [("MainActivityPendingTurnOptionsTest", 3), ("GameScreenTest", 7), ("MainActivityOptionsTest", 6)]:
    target = out / name
    target.mkdir()
    command = ["./gradlew", "connectedDebugAndroidTest", "--offline", "--no-configuration-cache", "--rerun-tasks", "--console=plain", "-Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe." + name]
    started = time.time()
    result = subprocess.run(command, cwd=root, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (target / "gradle.log").write_text(redact(result.stdout))
    record = {"name": name, "command": command, "started_utc": dt.datetime.fromtimestamp(started, dt.timezone.utc).isoformat(), "ended_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "exit_code": result.returncode, "expected_tests": count, "totals": {key: 0 for key in ("tests", "failures", "errors", "skipped")}, "cases": []}
    fresh = [path for path in sorted((root / "app/build/outputs/androidTest-results/connected").rglob("TEST-*.xml")) if path.stat().st_mtime >= started]
    for i, path in enumerate(fresh):
        suite = ET.fromstring(path.read_text())
        for key in record["totals"]:
            record["totals"][key] += int(suite.get(key, 0))
        for case in suite.iter("testcase"):
            status = "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS"
            record["cases"].append({"class": case.get("classname"), "name": case.get("name"), "status": status, "xml": f"{name}/result-{i+1}.xml"})
        (target / f"result-{i+1}.xml").write_text(redact(path.read_text()))
    record["status"] = "PASS" if result.returncode == 0 and record["totals"] == {"tests": count, "failures": 0, "errors": 0, "skipped": 0} and len(record["cases"]) == count and all(case["status"] == "PASS" and case["class"] == "com.edu.unal.tictactoe." + name for case in record["cases"]) else "FAIL_OR_INCOMPLETE"
    summary["runs"].append(record)
    save()
    print(name, record["status"], record["totals"], "exit", result.returncode, flush=True)
summary["ended_utc"] = dt.datetime.now(dt.timezone.utc).isoformat()
summary["status"] = "PASS" if all(record["status"] == "PASS" for record in summary["runs"]) else "FAIL_OR_INCOMPLETE"
save()
print("Evidence:", str(out.relative_to(root)))
sys.exit(0 if summary["status"] == "PASS" else 1)
