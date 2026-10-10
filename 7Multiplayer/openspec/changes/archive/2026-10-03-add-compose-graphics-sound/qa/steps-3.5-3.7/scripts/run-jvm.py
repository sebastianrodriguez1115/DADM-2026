#!/usr/bin/env python3
"""Run existing JVM tests afresh; store only redacted QA evidence here."""
import datetime
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

qa = Path(__file__).resolve().parents[1]
root = qa.parents[4]
evidence = qa / "evidence"
evidence.mkdir(parents=True, exist_ok=True)
env = os.environ.copy()
env.setdefault("JAVA_HOME", "/Applications/Android Studio.app/Contents/jbr/Contents/Home")
env.setdefault("ANDROID_HOME", str(Path.home() / "Library/Android/sdk"))

def redact(text):
    for value, replacement in [(str(root), "<PROJECT>"), (str(Path.home()), "<HOME>")]:
        text = text.replace(value, replacement)
    text = re.sub(r'hostname="[^"]*"', 'hostname="REDACTED"', text)
    return re.sub(r"\x1b\[[0-9;]*m", "", text)

command = ["./gradlew", "testDebugUnitTest", "--offline", "--no-configuration-cache", "--rerun-tasks", "--console=plain"]
started = datetime.datetime.now(datetime.timezone.utc)
result = subprocess.run(command, cwd=root, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
ended = datetime.datetime.now(datetime.timezone.utc)
(evidence / "jvm-run.log").write_text(redact(result.stdout))
summary = {"command": command, "started_utc": started.isoformat(), "ended_utc": ended.isoformat(), "exit_code": result.returncode, "suites": [], "totals": {key: 0 for key in ("tests", "failures", "errors", "skipped")}}
if result.returncode == 0:
    output = evidence / "jvm-xml"
    output.mkdir(exist_ok=True)
    for old in output.glob("TEST-*.xml"):
        old.unlink()
    for path in sorted((root / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml")):
        suite = ET.fromstring(path.read_text())
        record = {"name": suite.get("name"), **{key: int(suite.get(key, 0)) for key in summary["totals"]}}
        record["cases"] = [{"name": case.get("name"), "status": "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS"} for case in suite.findall("testcase")]
        for key in summary["totals"]:
            summary["totals"][key] += record[key]
        summary["suites"].append(record)
        (output / path.name).write_text(redact(path.read_text()))

# Read-only discovery; never persist device identifiers or full adb output.
adb = Path(env["ANDROID_HOME"]) / "platform-tools/adb"
if adb.exists():
    devices = subprocess.run([str(adb), "devices"], env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    states = [line.split()[1] for line in devices.stdout.splitlines()[1:] if len(line.split()) >= 2]
    summary["android_environment"] = {"adb_exit_code": devices.returncode, "device_count": states.count("device"), "offline_count": states.count("offline"), "unauthorized_count": states.count("unauthorized"), "instrumentation": "NOT_RUN"}
else:
    summary["android_environment"] = {"adb_available": False, "instrumentation": "NOT_RUN"}
(evidence / "jvm-summary.json").write_text(json.dumps(summary, indent=2) + "\n")
print(json.dumps({key: value for key, value in summary.items() if key != "suites"}, indent=2))
sys.exit(result.returncode if result.returncode else (0 if summary["totals"]["tests"] > 0 and not any(summary["totals"][key] for key in ("failures", "errors", "skipped")) else 1))
