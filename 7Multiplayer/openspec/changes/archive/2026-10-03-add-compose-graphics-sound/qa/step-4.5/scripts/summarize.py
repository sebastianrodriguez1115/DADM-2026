"""Read-only product validation; write structured results exclusively in QA."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import re
import struct
import wave
import xml.etree.ElementTree as ET

QA = Path(__file__).resolve().parents[1]
ROOT = QA.parents[4]
EXPECTED = {"AndroidMoveSoundPlayerLoadingTest": 1, "MainActivityLifecycleTest": 4,
            "MainActivityTest": 7, "MainActivityPendingTurnOptionsTest": 3,
            "TicTacToeScreenTest": 15}

def native(path):
    text = path.read_text().replace("\r", "")
    bundle = {}
    tests = []
    codes = []
    for line in text.splitlines():
        if line.startswith("INSTRUMENTATION_STATUS: "):
            key, _, value = line[len("INSTRUMENTATION_STATUS: "):].partition("=")
            bundle[key] = value
        elif line.startswith("INSTRUMENTATION_STATUS_CODE: "):
            code = int(line.split(": ", 1)[1])
            codes.append(code)
            if code != 1:
                tests.append({"class": bundle.get("class"), "test": bundle.get("test"),
                              "status_code": code, "result": "PASS" if code == 0 else "FAIL"})
            bundle = {}
    result_codes = re.findall(r"^INSTRUMENTATION_CODE: (-?\d+)$", text, re.M)
    junit = re.search(r"OK \((\d+) tests?\)", text)
    expected = EXPECTED[path.stem]
    valid = (len(tests) == expected and codes.count(1) == expected and
             codes.count(0) == expected and all(c in (0, 1) for c in codes) and
             result_codes == ["-1"] and junit is not None and int(junit[1]) == expected and
             all(t["test"] and t["class"] for t in tests) and
             not re.search(r"FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=|Process crashed", text))
    result = {"class": path.stem, "expected": expected, "passed": codes.count(0),
              "status_codes": codes, "instrumentation_result_codes": result_codes,
              "junit_ok_count": int(junit[1]) if junit else None,
              "valid": valid, "tests": tests, "raw_evidence": path.name}
    path.with_suffix(".json").write_text(json.dumps(result, indent=2) + "\n")
    assert valid, result
    return result

def assets():
    expected = {"x": "5c0a56baea4104a4ecb14af1de61d1c8a6833fb6cb44670c3dbccce71020a2c0",
                "o": "3330ef2de3a0cdc68f0fc24338884ee09aefece601a62f41f3b28d92f6dddcf5"}
    out = []
    for mark, digest in expected.items():
        path = ROOT / f"app/src/main/res/raw/game_move_{mark}.wav"
        data = path.read_bytes()
        with wave.open(str(path)) as w:
            params = w.getparams()
            samples = struct.unpack("<" + "h" * params.nframes, w.readframes(params.nframes))
        middle = samples[600:-600]
        rising = sum(a <= 0 < b for a, b in zip(middle, middle[1:]))
        hz = rising * params.framerate / len(middle)
        entry = {"mark": mark, "sha256": hashlib.sha256(data).hexdigest(),
                 "channels": params.nchannels, "width_bytes": params.sampwidth,
                 "rate": params.framerate, "frames": params.nframes,
                 "duration_ms": 1000 * params.nframes / params.framerate,
                 "estimated_hz": hz, "peak": max(map(abs, samples)),
                 "rms": math.sqrt(sum(s*s for s in samples)/len(samples)),
                 "first": samples[0], "last": samples[-1]}
        assert entry["sha256"] == digest
        assert (params.nchannels, params.sampwidth, params.framerate, params.nframes, params.comptype) == (1, 2, 44100, 7938, "NONE")
        assert abs(hz - (880 if mark == "x" else 440)) < 10
        assert entry["peak"] < 32767 and samples[0] == samples[-1] == 0
        out.append(entry)
    assert out[0]["sha256"] != out[1]["sha256"]
    (QA / "evidence/assets.json").write_text(json.dumps(out, indent=2) + "\n")
    return out

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--native", type=Path)
    args = parser.parse_args()
    if args.native:
        print(json.dumps(native(args.native)))
        return
    suites = []
    individual = []
    for path in sorted((QA / "evidence/jvm").glob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        suite = {"name": root.attrib["name"], **{k: int(root.attrib.get(k, 0)) for k in ("tests", "failures", "errors", "skipped")}}
        suites.append(suite)
        for tc in root.findall("testcase"):
            individual.append({"class": tc.attrib["classname"], "test": tc.attrib["name"],
                               "result": "FAIL" if tc.find("failure") is not None or tc.find("error") is not None else "SKIP" if tc.find("skipped") is not None else "PASS",
                               "evidence": "jvm/" + path.name})
    totals = {k: sum(s[k] for s in suites) for k in ("tests", "failures", "errors", "skipped")}
    assert totals == {"tests": 131, "failures": 0, "errors": 0, "skipped": 0}, totals
    native_results = [native(QA / f"evidence/{name}.log") for name in EXPECTED]
    result = {"jvm_totals": totals, "jvm_suites": suites, "jvm_tests": individual,
              "native_passed": sum(r["passed"] for r in native_results),
              "native_classes": native_results, "assets": assets()}
    (QA / "evidence/summary.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"jvm": totals, "native_passed": result["native_passed"], "assets": "PASS"}))

if __name__ == "__main__":
    main()
