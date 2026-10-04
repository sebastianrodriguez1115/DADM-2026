#!/usr/bin/env python3
"""Validate nonzero fresh results and timing arithmetic for one retry folder."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

folder = Path(sys.argv[1])
summary = json.loads((folder / "summary.json").read_text())
expected = {"MainActivityLifecycleTest": 4, "MainActivityTest": 7, "GameScreenTest": 6, "ImpossibleOpeningTimingTest": 1, "JVM": 108}
assert summary["connected_device_count"] == 1
assert {run["name"] for run in summary["runs"]} == set(expected)
for run in summary["runs"]:
    assert run["exit_code"] == 0 and run["status"] == "PASS", run["name"]
    totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    actual_cases = []
    for path in (folder / run["name"]).glob("result-*.xml"):
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, 0))
        for case in suite.iter("testcase"):
            assert case.find("failure") is None and case.find("error") is None and case.find("skipped") is None
            actual_cases.append(case)
    assert totals == run["totals"]
    assert totals == {"tests": expected[run["name"]], "failures": 0, "errors": 0, "skipped": 0}
    assert len(actual_cases) == expected[run["name"]]
timing = next(run["timing"] for run in summary["runs"] if run["name"] == "ImpossibleOpeningTimingTest")
samples = timing["samples_ns"]
assert len(samples) == 5 and all(value >= 0 for value in samples)
assert abs(samples[0] / 1_000_000 - timing["first_ms_logged"]) <= 0.000501
assert abs(sorted(samples)[2] / 1_000_000 - timing["median_ms_logged"]) <= 0.000501
print("PASS: 18 Android tests and 108 JVM tests; first/median agree with five measured samples")
print("3.7 UI menu/stale-selection remains PENDING; audio/manual accessibility NOT TESTED")
