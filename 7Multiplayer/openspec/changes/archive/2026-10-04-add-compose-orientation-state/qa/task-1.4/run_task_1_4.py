#!/usr/bin/env python3
"""Task 1.4 native runner: physical device preferred, AVD fallback, guarded per ADB operation.

Reuses the task-1.2 runner module unmodified and replaces its guard and device_command, and reuses its target_serial for the emulator fallback.

Usage:
  run_task_1_4.py <new-simple-evidence-directory>   real run (writes to the target)
  run_task_1_4.py --check                           read-only: resolve target, guard once, print identity
"""
from __future__ import annotations

import importlib.util
import json
import re
import subprocess
import sys
from pathlib import Path

USAGE = "Usage: run_task_1_4.py <new-simple-evidence-directory> | --check"
MIN_API = 35  # minSdk of the app

if len(sys.argv) != 2 or not (sys.argv[1] == "--check" or re.fullmatch(r"[A-Za-z0-9_-]+", sys.argv[1])):
    print(USAGE, file=sys.stderr)
    raise SystemExit(2)

CHECK = sys.argv[1] == "--check"
evidence_root = Path(__file__).resolve().parent
output = evidence_root / sys.argv[1]
if not CHECK and output.exists():
    raise SystemExit(f"Refusing to overwrite existing evidence: {output}")

runner = Path(__file__).resolve().parents[1] / "task-1.2" / "run_instrumentation.py"
sys.argv = [str(runner)]
spec = importlib.util.spec_from_file_location("guarded_task_1_2_runner", runner)
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

PINNED: dict = {}
REDACTED_SERIAL = "physical-device"
REDACTED_MODEL = "redacted"


def mask_identity(identity: dict) -> dict:
    """Copy of an identity/guard record that is safe to write or print (physical device only is masked)."""
    masked = dict(identity)
    if masked.get("kind", masked.get("target")) == "physical":
        masked["serial"] = REDACTED_SERIAL
        if "model" in masked:
            masked["model"] = REDACTED_MODEL
    return masked


def mask_command(command: list[str], physical: bool) -> list[str]:
    """Copy of an ADB command where the argument after -s is masked for a physical device."""
    if not physical:
        return list(command)
    return [REDACTED_SERIAL if index and command[index - 1] == "-s" else part
            for index, part in enumerate(command)]


def mask_text(text: str, pinned: dict) -> str:
    """Replace the real serial and model of a physical device inside free text."""
    if pinned.get("kind") == "physical":
        for value, marker in ((pinned.get("serial"), REDACTED_SERIAL), (pinned.get("model"), REDACTED_MODEL)):
            if value:
                text = text.replace(value, marker)
    return text


def adb_out(serial: str | None, *args: str) -> str:
    command = [str(base.ADB)] + (["-s", serial] if serial else []) + list(args)
    return subprocess.run(command, env=base.ENV, capture_output=True, text=True, check=True,
                          timeout=60).stdout.strip()


def device_states() -> dict[str, str]:
    states = {}
    for line in adb_out(None, "devices").splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2:
            states[parts[0]] = parts[1]
    return states


def read_identity(serial: str, kind: str) -> dict:
    prop = lambda name: adb_out(serial, "shell", "getprop", name).strip()
    identity = dict(kind=kind, serial=serial, model=prop("ro.product.model"),
                    api=prop("ro.build.version.sdk"), abi=prop("ro.product.cpu.abi"),
                    boot=prop("sys.boot_completed"))
    if kind == "emulator":
        identity["avd"] = adb_out(serial, "emu", "avd", "name").splitlines()[0].strip()
    return identity


def resolve_target() -> dict:
    states = device_states()
    physical = [s for s, st in states.items() if st == "device" and not re.fullmatch(r"emulator-\d+", s)]
    if len(physical) > 1:
        raise RuntimeError(f"Ambiguous physical targets ({len(physical)}), refusing to operate any")
    if len(physical) == 1:
        identity = read_identity(physical[0], "physical")
        if not identity["api"].isdigit() or int(identity["api"]) < MIN_API:
            raise RuntimeError(f"Physical target API below {MIN_API}: {mask_identity(identity)}")
        if identity["boot"] != "1":
            raise RuntimeError(f"Physical target not booted: {mask_identity(identity)}")
        return identity
    identity = read_identity(base.target_serial(), "emulator")  # exactly one target AVD or raises
    if (identity["avd"], identity["api"], identity["abi"], identity["boot"]) != (
            base.TARGET_AVD, "35", "arm64-v8a", "1"):
        raise RuntimeError(f"Emulator identity mismatch: {identity}")
    return identity


def guard(operation: str, record_to_disk: bool = True) -> str:
    serial = PINNED["serial"]
    state = device_states().get(serial)
    current = read_identity(serial, PINNED["kind"]) if state == "device" else {}
    record = dict(time=base.timestamp(), operation=operation, target=PINNED["kind"], serial=serial,
                  state=state, **{k: current.get(k) for k in ("model", "api", "abi", "boot", "avd")})
    shown = mask_identity(record)
    if record_to_disk:
        with (base.QA / "device-guards.jsonl").open("a") as stream:
            stream.write(json.dumps(shown) + "\n")
    if state != "device" or current != PINNED:
        differing = sorted(k for k in set(PINNED) | set(current) if PINNED.get(k) != current.get(k))
        raise RuntimeError(f"Device identity guard failed before {operation}: state={state} "
                           f"differing fields={differing}")
    pinned = mask_identity(PINNED)
    print(f"GUARD PASS {operation}: [{pinned['kind']}] {pinned['serial']} {pinned['model']}/API{pinned['api']}/"
          f"{pinned['abi']}/boot{pinned['boot']}", flush=True)
    return serial


def with_user_zero(args: list[str]) -> list[str]:
    """Physical device only: add --user 0 to install, am instrument and am force-stop."""
    if PINNED["kind"] != "physical":
        return args
    if args[0] == "install":
        return ["install", "--user", "0", *args[1:]]
    if args[:2] == ["shell", "am"] and len(args) > 2 and args[2] in ("instrument", "force-stop"):
        return [*args[:3], "--user", "0", *args[3:]]
    return args


def device_command(label: str, args: list[str]) -> str:
    serial = guard(label)
    command = [str(base.ADB), "-s", serial, *with_user_zero(args)]
    log = base.QA / f"{label}.log"
    with log.open("w") as stream:
        result = subprocess.run(command, env=base.ENV, stdout=stream, stderr=subprocess.STDOUT, text=True)
    with (base.QA / "commands.jsonl").open("a") as stream:
        stream.write(json.dumps(dict(time=base.timestamp(), label=label,
                                     command=mask_command(command, PINNED["kind"] == "physical"),
                                     exit=result.returncode)) + "\n")
    print(f"{label}: exit={result.returncode}", flush=True)
    if result.returncode:
        raise RuntimeError(f"{label} failed; see {log}")
    return log.read_text()


PINNED.update(resolve_target())
base.guard = guard
base.device_command = device_command

if CHECK:
    guard("check", record_to_disk=False)
    print(json.dumps(mask_identity(PINNED), indent=2))
    raise SystemExit(0)

base.QA_ROOT = evidence_root
base.QA = output
base.QA.mkdir(parents=True, exist_ok=False)
try:
    sys.exit(base.main())
except Exception as error:
    base.save_json("aborted.json", dict(time=base.timestamp(), error=mask_text(repr(error), PINNED)))
    raise
