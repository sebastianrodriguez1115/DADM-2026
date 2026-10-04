#!/usr/bin/env python3
"""Independent native QA; separate class runs and append-only redacted evidence."""
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
root = qa.parents[4]
env = os.environ.copy()
env.setdefault("JAVA_HOME", "/Applications/Android Studio.app/Contents/jbr/Contents/Home")
env.setdefault("ANDROID_HOME", str(Path.home() / "Library/Android/sdk"))
adb = str(Path(env["ANDROID_HOME"]) / "platform-tools/adb")
stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
out = qa / "evidence" / ("android-retry-" + stamp)
out.mkdir(parents=True, exist_ok=False)
devices = subprocess.run([adb, "devices"], env=env, text=True, capture_output=True)
entries = [line.split() for line in devices.stdout.splitlines()[1:] if len(line.split()) >= 2]
identifiers = [entry[0] for entry in entries]
connected = [entry[0] for entry in entries if entry[1] == "device"]

def redact(text):
    for value, replacement in [(str(root), "PROJECT_REDACTED"), (str(Path.home()), "HOME_REDACTED")]:
        text = text.replace(value, replacement)
    for value in identifiers:
        text = text.replace(value, "DEVICE_REDACTED")
    text = re.sub(r'hostname="[^"]*"', 'hostname="REDACTED"', text)
    return re.sub(r"\x1b\[[0-9;]*m", "", text)

summary = {"started_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "adb_exit_code": devices.returncode, "connected_device_count": len(connected), "runs": []}
if devices.returncode or len(connected) != 1:
    summary["status"] = "BLOCKED: expected exactly one authorized device"
    (out / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(summary["status"])
    sys.exit(1)
env["ANDROID_SERIAL"] = connected[0]  # In memory only; never persist a serial.

def run(name, expected_count, android=True):
    target = out / name
    target.mkdir()
    command = ["./gradlew", "connectedDebugAndroidTest" if android else "testDebugUnitTest", "--offline", "--no-configuration-cache", "--rerun-tasks", "--console=plain"]
    if android:
        command += ["-Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe." + name]
    device_start = None
    if name == "ImpossibleOpeningTimingTest":
        read_clock = subprocess.run([adb, "shell", "date", "+%s"], env=env, capture_output=True, text=True)
        if read_clock.returncode == 0:
            device_start = int(read_clock.stdout.strip())
    started = time.time()
    result = subprocess.run(command, cwd=root, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (target / "gradle.log").write_text(redact(result.stdout))
    record = {"name": name, "command": command, "started_utc": dt.datetime.fromtimestamp(started, dt.timezone.utc).isoformat(), "ended_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "exit_code": result.returncode, "expected_tests": expected_count, "totals": {key: 0 for key in ("tests", "failures", "errors", "skipped")}, "cases": []}
    directory = root / ("app/build/outputs/androidTest-results/connected" if android else "app/build/test-results/testDebugUnitTest")
    fresh = [path for path in sorted(directory.rglob("TEST-*.xml")) if path.stat().st_mtime >= started]
    for i, path in enumerate(fresh):
        suite = ET.fromstring(path.read_text())
        for key in record["totals"]:
            record["totals"][key] += int(suite.get(key, 0))
        for case in suite.iter("testcase"):
            status = "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS"
            record["cases"].append({"class": case.get("classname"), "name": case.get("name"), "status": status, "xml": f"{name}/result-{i+1}.xml"})
        (target / f"result-{i+1}.xml").write_text(redact(path.read_text()))
    if name == "ImpossibleOpeningTimingTest":
        logs = subprocess.run([adb, "logcat", "-d", "-v", "epoch", "Reto4Timing:I", "*:S"], env=env, capture_output=True, text=True)
        pattern = re.compile(r"sdk=(\d+) samples_ns=([\d,]+) first_ms=([\d.]+) median_ms=([\d.]+)")
        lines = []
        for line in logs.stdout.splitlines():
            parts = line.split()
            if device_start is not None and parts and re.match(r"^\d+\.\d+$", parts[0]) and float(parts[0]) >= device_start and pattern.search(line):
                lines.append(line)
        (target / "timing-app-tag.log").write_text(redact("\n".join(lines)) + "\n")
        record["timing_logcat_exit_code"] = logs.returncode
        if lines:
            match = pattern.search(lines[-1])
            samples = [int(value) for value in match[2].split(",")]
            record["timing"] = {"sdk": int(match[1]), "samples_ns": samples, "first_ms_logged": float(match[3]), "median_ms_logged": float(match[4]), "first_ms_from_samples": samples[0] / 1_000_000, "median_ms_from_samples": sorted(samples)[len(samples)//2] / 1_000_000}
    record["status"] = "PASS" if result.returncode == 0 and record["totals"]["tests"] == expected_count and len(record["cases"]) == expected_count and all(case["status"] == "PASS" for case in record["cases"]) else "FAIL"
    if name == "ImpossibleOpeningTimingTest" and ("timing" not in record or len(record["timing"]["samples_ns"]) != 5):
        record["status"] = "EVIDENCE_INCOMPLETE"
    summary["runs"].append(record)
    (out / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(name, record["status"], record["totals"], "exit", result.returncode, flush=True)

for name, count in [("MainActivityLifecycleTest", 4), ("MainActivityTest", 7), ("GameScreenTest", 6), ("ImpossibleOpeningTimingTest", 1)]:
    run(name, count)
run("JVM", 108, android=False)
summary["ended_utc"] = dt.datetime.now(dt.timezone.utc).isoformat()
summary["status"] = "PASS" if all(record["status"] == "PASS" for record in summary["runs"]) else "FAIL_OR_INCOMPLETE"
(out / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
print("Evidence:", str(out.relative_to(root)), flush=True)
sys.exit(0 if summary["status"] == "PASS" else 1)
