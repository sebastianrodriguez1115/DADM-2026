#!/usr/bin/env python3
"""Run only the authorized step-1.6 suites; retain identifier-free evidence."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

QA = Path(__file__).resolve().parent
ROOT = QA.parents[4]
EVIDENCE = QA / "evidence"
EVIDENCE.mkdir(exist_ok=True)
ENV = dict(os.environ,
           JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home",
           ANDROID_HOME="/Users/jsrc1115/Library/Android/sdk")
SUITES = [("GameBoardLayoutTest", 2), ("TicTacToeScreenTest", 15)]
SOURCES = [
    "app/src/main/java/com/edu/unal/tictactoe/ui/game/GameBoard.kt",
    "app/src/main/java/com/edu/unal/tictactoe/ui/game/TicTacToeScreen.kt",
    "app/src/androidTest/java/com/edu/unal/tictactoe/GameBoardLayoutTest.kt",
    "app/src/androidTest/java/com/edu/unal/tictactoe/TicTacToeScreenTest.kt",
    "app/src/androidTest/java/com/edu/unal/tictactoe/GameBoardRenderingTest.kt",
]


def hashes():
    return {p: hashlib.sha256((ROOT / p).read_bytes()).hexdigest() for p in SOURCES}


before = hashes()
summary = {"scope": "step-1.6", "source_sha256_before": before, "suites": []}
failed = False
for short_name, expected in SUITES:
    class_name = "com.edu.unal.tictactoe." + short_name
    command = ["./gradlew", "connectedDebugAndroidTest", "--no-configuration-cache",
               "--console=plain", "-Pandroid.testInstrumentationRunnerArguments.class=" + class_name]
    started = time.time()
    run = subprocess.run(command, cwd=ROOT, env=ENV, text=True,
                         stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    # Save only build/task markers and test counts: Gradle's device labels and
    # XML suite names may contain identifiers, so raw output is never persisted.
    safe_lines = [line for line in run.stdout.splitlines()
                  if line.startswith(("> Task ", "BUILD SUCCESSFUL", "BUILD FAILED"))
                  or line.strip().endswith("tests completed")]
    cases = []
    fresh_files = 0
    for path in (ROOT / "app/build/outputs/androidTest-results/connected").rglob("*.xml"):
        if path.stat().st_mtime < started - 1:
            continue
        tree = ET.parse(path)
        matching = [tc for tc in tree.iter("testcase")
                    if tc.attrib.get("classname") == class_name]
        if matching:
            fresh_files += 1
        for tc in matching:
            result = "passed"
            if tc.find("failure") is not None or tc.find("error") is not None:
                result = "failed"
            elif tc.find("skipped") is not None:
                result = "skipped"
            cases.append({"name": tc.attrib.get("name"),
                          "time_seconds": tc.attrib.get("time"), "result": result})
    counts = {key: sum(c["result"] == key for c in cases)
              for key in ("passed", "failed", "skipped")}
    ok = (run.returncode == 0 and len(cases) == expected and expected > 0
          and counts["passed"] == expected and fresh_files > 0)
    suite = {"class": class_name, "expected_count": expected, "count": len(cases),
             "counts": counts, "gradle_exit": run.returncode, "fresh_xml_files": fresh_files,
             "validated": ok, "elapsed_seconds": round(time.time() - started, 2),
             "command": command, "cases": cases}
    (EVIDENCE / (short_name + ".json")).write_text(json.dumps(suite, indent=2) + "\n")
    (EVIDENCE / (short_name + ".log")).write_text("\n".join(safe_lines) + "\n")
    summary["suites"].append(suite)
    failed |= not ok
    print(f"{short_name}: exit={run.returncode}, count={len(cases)}, {counts}, validated={ok}", flush=True)

summary["source_sha256_after"] = hashes()
summary["reviewed_sources_unchanged"] = before == summary["source_sha256_after"]
failed |= not summary["reviewed_sources_unchanged"]
summary["script_exit"] = int(failed)
(EVIDENCE / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
sys.exit(int(failed))
