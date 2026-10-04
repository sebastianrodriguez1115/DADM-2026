#!/usr/bin/env python3
"""Cross-check saved XML, counts, cases and redaction for a completed UI37 run."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

folder = Path(sys.argv[1])
summary = json.loads((folder / "summary.json").read_text())
expected = {"MainActivityPendingTurnOptionsTest": 3, "GameScreenTest": 7, "MainActivityOptionsTest": 6}
assert summary["status"] == "PASS" and summary["connected_device_count"] == 1
assert {run["name"] for run in summary["runs"]} == set(expected)
for run in summary["runs"]:
    assert run["exit_code"] == 0 and run["status"] == "PASS"
    totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    actual = []
    for path in (folder / run["name"]).glob("result-*.xml"):
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, 0))
        for case in suite.iter("testcase"):
            assert case.find("failure") is None and case.find("error") is None and case.find("skipped") is None
            actual.append(case.get("name"))
    assert totals == run["totals"] == {"tests": expected[run["name"]], "failures": 0, "errors": 0, "skipped": 0}
    assert len(actual) == len(set(actual)) == expected[run["name"]]
    assert set(actual) == {case["name"] for case in run["cases"]}
for path in folder.rglob("*"):
    if path.is_file():
        text = path.read_text()
        assert "/Users/" not in text and "Mac-Studio" not in text, path.name
        if path.suffix == ".xml":
            suite = ET.parse(path).getroot()
            assert all(element.get("hostname", "REDACTED") == "REDACTED" for element in suite.iter())
            assert all(element.get("value") == "DEVICE_REDACTED" for element in suite.iter("property") if element.get("name") == "device")
print("PASS: XML fresh saved results match summary: 3 + 7 + 6 = 16 tests; zero failures/errors/skips")
print("PASS: individual cases, counts and redaction; JVM NOT RERUN")
