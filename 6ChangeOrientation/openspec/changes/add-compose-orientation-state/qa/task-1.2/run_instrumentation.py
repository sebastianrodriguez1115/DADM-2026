#!/usr/bin/env python3
"""Run the complete Android test suite and an intentionally reversed Activity-class order."""

from __future__ import annotations

import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[5]
QA_ROOT = Path(__file__).resolve().parent
RUN_DIR = sys.argv[1] if len(sys.argv) > 1 else ""
MODE = sys.argv[2] if len(sys.argv) > 2 else "acceptance"
if RUN_DIR and not re.fullmatch(r"[A-Za-z0-9_-]+", RUN_DIR):
    raise ValueError("Run directory must be a simple name")
if MODE not in {"acceptance", "isolated-all"}:
    raise ValueError("Mode must be acceptance or isolated-all")
QA = QA_ROOT / RUN_DIR if RUN_DIR else QA_ROOT
QA.mkdir(parents=True, exist_ok=True)
ADB = Path.home() / "Library/Android/sdk/platform-tools/adb"
TARGET_AVD = "DADM_GraphicsSound_API35"
APP_ID = "com.edu.unal.tictactoe"
TEST_COMPONENT = "com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner"
ENV = dict(os.environ)
ENV["ANDROID_HOME"] = str(Path.home() / "Library/Android/sdk")
ENV["ANDROID_SDK_ROOT"] = ENV["ANDROID_HOME"]
ENV["PATH"] = ENV["ANDROID_HOME"] + "/platform-tools:" + ENV.get("PATH", "")


def timestamp() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def save_json(name: str, data: object) -> None:
    (QA / name).write_text(json.dumps(data, indent=2) + "\n")


def target_serial() -> str:
    result = subprocess.run([str(ADB), "devices"], env=ENV, capture_output=True, text=True, check=True)
    serials = [line.split()[0] for line in result.stdout.splitlines()
               if re.match(r"^emulator-[0-9]+\s+device(?:\s|$)", line)]
    matches = []
    for serial in serials:
        name = subprocess.check_output([str(ADB), "-s", serial, "emu", "avd", "name"],
                                       env=ENV, text=True, stderr=subprocess.DEVNULL).strip().splitlines()[0]
        if name == TARGET_AVD:
            matches.append(serial)
    if len(matches) != 1:
        raise RuntimeError(f"Expected exactly one {TARGET_AVD}, found {matches}")
    return matches[0]


def guard(operation: str) -> str:
    serial = target_serial()
    avd = subprocess.check_output([str(ADB), "-s", serial, "emu", "avd", "name"], env=ENV,
                                  text=True, stderr=subprocess.DEVNULL).strip().splitlines()[0]
    api = subprocess.check_output([str(ADB), "-s", serial, "shell", "getprop",
                                   "ro.build.version.sdk"], env=ENV, text=True).strip()
    abi = subprocess.check_output([str(ADB), "-s", serial, "shell", "getprop",
                                   "ro.product.cpu.abi"], env=ENV, text=True).strip()
    boot = subprocess.check_output([str(ADB), "-s", serial, "shell", "getprop",
                                    "sys.boot_completed"], env=ENV, text=True).strip()
    record = dict(time=timestamp(), operation=operation, serial=serial,
                  avd=avd, api=api, abi=abi, boot=boot)
    with (QA / "device-guards.jsonl").open("a") as stream:
        stream.write(json.dumps(record) + "\n")
    if (avd, api, abi, boot) != (TARGET_AVD, "35", "arm64-v8a", "1"):
        raise RuntimeError(f"AVD identity guard failed before {operation}: {record}")
    print(f"GUARD PASS {operation}: {serial} {avd}/API{api}/{abi}/boot{boot}", flush=True)
    return serial


def device_command(label: str, args: list[str]) -> str:
    serial = guard(label)
    command = [str(ADB), "-s", serial, *args]
    log = QA / f"{label}.log"
    with log.open("w") as stream:
        result = subprocess.run(command, env=ENV, stdout=stream, stderr=subprocess.STDOUT, text=True)
    with (QA / "commands.jsonl").open("a") as stream:
        stream.write(json.dumps(dict(time=timestamp(), label=label, command=command,
                                     exit=result.returncode)) + "\n")
    print(f"{label}: exit={result.returncode}", flush=True)
    if result.returncode:
        raise RuntimeError(f"{label} failed; see {log}")
    return log.read_text()


def inventory() -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    for path in sorted((ROOT / "app/src/androidTest").rglob("*.kt")):
        text = path.read_text()
        package = re.search(r"^package\s+(\S+)", text, re.M)
        if not package:
            continue
        for clazz_match in re.finditer(r"^class\s+(\w+)", text, re.M):
            start = clazz_match.end()
            next_class = re.search(r"^class\s+\w+", text[start:], re.M)
            body = text[start:start + next_class.start()] if next_class else text[start:]
            methods = re.findall(r"@Test\b(?:(?!\bfun\b).)*\bfun\s+([A-Za-z0-9_]+)\s*\(",
                                 body, re.S)
            if methods:
                result[f"{package.group(1)}.{clazz_match.group(1)}"] = methods
    return result


def instrument(label: str, clazz: str | None = None, method: str | None = None) -> str:
    args = ["shell", "am", "instrument", "-w", "-r"]
    if clazz:
        selector = clazz + (f"#{method}" if method else "")
        args += ["-e", "class", selector]
    return device_command(label, args + [TEST_COMPONENT])


def parse_result(label: str, output: str, expected: dict[str, list[str]]) -> dict:
    events: list[dict] = []
    current: dict[str, str] = {}
    for line in output.splitlines():
        match = re.match(r"INSTRUMENTATION_STATUS: (class|test|numtests|stack)=(.*)", line)
        if match:
            current[match.group(1)] = match.group(2)
        code = re.match(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", line)
        if code:
            events.append({**current, "code": int(code.group(1))})
            current = {}
    expected_ids = {f"{clazz}#{method}" for clazz, methods in expected.items() for method in methods}
    starts = [event for event in events if event["code"] == 1]
    ends = [event for event in events if event["code"] != 1]
    actual_ids = [event.get("class", "") + "#" + event.get("test", "") for event in ends]
    passed = [event for event in ends if event["code"] == 0]
    failed = [event for event in ends if event["code"] in (-1, -2)]
    complete = len(starts) == len(ends) == len(expected_ids) and set(actual_ids) == expected_ids
    marker = re.search(r"^OK \((\d+) tests?\)\s*$", output, re.M)
    failure_marker = bool(re.search(r"FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed",
                                    output))
    summary = dict(label=label, expected=len(expected_ids), starts=len(starts), ended=len(ends),
                   passed=len(passed), failed=len(failed), complete_inventory=complete,
                   actual_ids=actual_ids, expected_ids=sorted(expected_ids),
                   ok_count=int(marker.group(1)) if marker else None,
                   failure_marker=failure_marker,
                   verified=(complete and len(passed) == len(expected_ids) and marker is not None
                             and int(marker.group(1)) == len(expected_ids) and not failure_marker))
    save_json(f"{label}-summary.json", summary)
    root = ET.Element("testsuite", name=label, tests=str(len(ends)), failures=str(len(failed)),
                      errors=str(len(ends) - len(passed) - len(failed)), skipped="0")
    for event in ends:
        case = ET.SubElement(root, "testcase", classname=event.get("class", ""),
                             name=event.get("test", ""))
        if event["code"] in (-1, -2):
            failure = ET.SubElement(case, "failure", message=event.get("stack", "instrumentation failure"))
            failure.text = event.get("stack", "")
    ET.indent(root)
    ET.ElementTree(root).write(QA / f"{label}.xml", encoding="utf-8", xml_declaration=True)
    return summary


def capture_focus_diagnostics(label: str) -> None:
    """Read, but do not clear, the target AVD's log buffer immediately after a test class."""
    raw = device_command(label + "-logcat", ["shell", "logcat", "-d", "-v", "epoch"])
    excerpt = [line for line in raw.splitlines()
               if re.search(r"focus|window|RootViewPicker|Espresso|InputDispatcher", line, re.I)]
    (QA / (label + "-logcat-focus-excerpt.txt")).write_text("\n".join(excerpt) + "\n")


def run_class_isolated_suite(tests: dict[str, list[str]]) -> int:
    """Run each inventoried class once in its own runner invocation, stopping only our app."""
    diagnostic_class = "com.edu.unal.tictactoe.DifficultyDialogTest"
    order = [diagnostic_class] + [clazz for clazz in sorted(tests) if clazz != diagnostic_class]
    class_results = []
    for index, clazz in enumerate(order, start=1):
        label = f"isolated-{index:02d}-{clazz.rsplit('.', 1)[-1]}"
        device_command(label + "-force-stop", ["shell", "am", "force-stop", APP_ID])
        output = instrument(label, clazz)
        class_results.append(parse_result(label, output, {clazz: tests[clazz]}))
        if clazz == diagnostic_class:
            capture_focus_diagnostics(label)

    expected_ids = {f"{clazz}#{method}" for clazz, methods in tests.items() for method in methods}
    actual_ids = [test_id for result in class_results for test_id in result["actual_ids"]]
    exact_coverage = len(actual_ids) == len(expected_ids) and set(actual_ids) == expected_ids
    total_expected = sum(len(methods) for methods in tests.values())
    total_passed = sum(result["passed"] for result in class_results)
    total_failed = sum(result["failed"] for result in class_results)
    suite = ET.Element("testsuite", name="class-isolated-android-suite",
                       tests=str(len(actual_ids)), failures=str(total_failed), errors="0", skipped="0",
                       tests_expected=str(total_expected), classes_expected=str(len(tests)))
    isolated_target_test = None
    for result in class_results:
        root = ET.parse(QA / f"{result['label']}.xml").getroot()
        for case in root.findall("testcase"):
            copied = ET.SubElement(suite, "testcase", case.attrib)
            for child in case:
                child_copy = ET.SubElement(copied, child.tag, child.attrib)
                child_copy.text = child.text
            if (case.get("classname") == diagnostic_class and
                    case.get("name") == "backDismissesWithoutSelectionOrToast"):
                isolated_target_test = "failed" if case.find("failure") is not None else "passed"
    ET.indent(suite)
    ET.ElementTree(suite).write(QA / "isolated-suite.xml", encoding="utf-8", xml_declaration=True)
    summary = dict(mode="isolated-all", class_count=len(tests), expected_classes=len(tests),
                   test_count=total_expected, starts=sum(result["starts"] for result in class_results),
                   passed=total_passed, failed=total_failed, exact_global_method_coverage=exact_coverage,
                   diagnostic_class=diagnostic_class,
                   diagnostic_method="backDismissesWithoutSelectionOrToast",
                   diagnostic_method_result=isolated_target_test,
                   classes=class_results,
                   verified=(len(class_results) == len(tests) and exact_coverage and
                             total_passed == total_expected and total_failed == 0 and
                             isolated_target_test == "passed"))
    save_json("isolated-suite-summary.json", summary)
    print(json.dumps({key: value for key, value in summary.items() if key != "classes"}, indent=2))
    return 0 if summary["verified"] else 1


def main() -> int:
    tests = inventory()
    total = sum(map(len, tests.values()))
    save_json("inventory.json", dict(classes=tests, class_count=len(tests), test_count=total))
    print(f"Inventory: {len(tests)} Android test classes, {total} methods", flush=True)

    if MODE == "isolated-all":
        return run_class_isolated_suite(tests)

    for label, apk in (("install-app", ROOT / "app/build/outputs/apk/debug/app-debug.apk"),
                       ("install-test", ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk")):
        output = device_command(label, ["install", "-r", str(apk)])
        if not re.search(r"^Success\s*$", output, re.M):
            raise RuntimeError(f"Install output did not report Success: {label}")

    device_command("full-suite-force-stop", ["shell", "am", "force-stop", APP_ID])
    full_output = instrument("full-suite")
    full = parse_result("full-suite", full_output, tests)

    activity_classes = [
        "com.edu.unal.tictactoe.MainActivityTest",
        "com.edu.unal.tictactoe.MainActivityOptionsTest",
        "com.edu.unal.tictactoe.MainActivityLifecycleTest",
        "com.edu.unal.tictactoe.MainActivitySoundOptionsTest",
        "com.edu.unal.tictactoe.MainActivityPendingTurnOptionsTest",
        "com.edu.unal.tictactoe.MainActivityAccessibilityTest",
    ]
    reverse_results = []
    for clazz in reversed(activity_classes):
        label = "reverse-" + clazz.rsplit(".", 1)[-1]
        device_command(label + "-force-stop", ["shell", "am", "force-stop", APP_ID])
        contaminated = instrument(label + "-contaminate", "com.edu.unal.tictactoe.GamePreferencesResidueWriterTest",
                                  "writeContaminatedGamePreferences")
        # Confirm the contaminator actually ran before this class's @Before clears and seeds.
        if "OK (1 test)" not in contaminated:
            raise RuntimeError("Preference contaminator did not report its single passing test")
        device_command(label + "-pre-class-force-stop", ["shell", "am", "force-stop", APP_ID])
        class_tests = {clazz: tests[clazz]}
        output = instrument(label, clazz)
        reverse_results.append(parse_result(label, output, class_tests))

    summary = dict(inventory_classes=len(tests), inventory_tests=total, full_suite=full,
                   reverse_activity_order=[item["label"] for item in reverse_results],
                   reverse_classes=reverse_results,
                   all_verified=full["verified"] and all(item["verified"] for item in reverse_results))
    save_json("summary.json", summary)
    return 0 if summary["all_verified"] else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        save_json("aborted.json", dict(time=timestamp(), error=repr(error)))
        raise
