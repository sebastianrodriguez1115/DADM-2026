#!/usr/bin/env python3
"""Postprocesado offline: escribe artefactos QA sin modificar producto/dispositivos."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET
from evidence_helpers import jvm_counts_table, native_counts_table, select_current_timing

QA = Path(__file__).resolve().parent.parent
ROOT = QA.parents[4]
OUT = QA / 'evidence' / (sys.argv[1] if len(sys.argv) > 1 else 'initial')

def save(name, data):
    (OUT / name).write_text(json.dumps(data, indent=2) + '\n')

prefix = subprocess.check_output(['git', 'rev-parse', '--show-prefix'], cwd=ROOT).decode().strip()
paths = sorted(list((ROOT / 'app/src/main/java/com/edu/unal/tictactoe/game').rglob('*.kt')) +
               [ROOT / ('app/src/test/java/com/edu/unal/tictactoe/game/' + name + '.kt')
                for name in ('ImpossiblePolicyTest', 'ImpossibleExhaustiveTest')])
rows = []
for p in paths:
    relative = str(p.relative_to(ROOT))
    base = subprocess.check_output(['git', 'show', '3119001:' + prefix + relative], cwd=ROOT)
    current = p.read_bytes()
    rows.append(dict(path=relative, baseline_sha256=hashlib.sha256(base).hexdigest(),
                     current_sha256=hashlib.sha256(current).hexdigest(), unchanged=base == current))
save('motor-baseline-integrity.json', dict(baseline='3119001', all_unchanged=all(r['unchanged'] for r in rows), files=rows))

inventory = json.loads((OUT / 'inventory.json').read_text())
summary = json.loads((OUT / 'summary.json').read_text())
commands = [json.loads(line) for line in (OUT / 'commands.jsonl').read_text().splitlines()]
guards = [json.loads(line) for line in (OUT / 'device-guards.jsonl').read_text().splitlines()]
device_ops = [r['label'] for r in commands if r['command'][0].endswith('/adb')]
guard_ops = [r['operation'] for r in guards]
check = dict(guard_count=len(guards), device_operation_count=len(device_ops),
             every_device_operation_guarded=all(guard_ops.count(x) == 1 for x in device_ops),
             only_owned_serial=all(r['command'][1:3] == ['-s', 'emulator-5556'] for r in commands
                                   if r['command'][0].endswith('/adb')),
             no_connected_gradle_tasks=all(not any('connected' in x.lower() for x in r['command']) for r in commands),
             all_device_identity_correct=all((r['avd'], r['sdk'], r['abi'], r['boot']) ==
                                             ('DADM_GraphicsSound_API35', '35', 'arm64-v8a', '1') for r in guards))
save('isolation-check.json', check)
selected = select_current_timing((OUT / 'impossible-timing-logcat.log').read_text(), commands)
ns = selected['samples_ns']
save('timing-selection.json', selected)
save('impossible-timing.json', dict(environment='DADM_GraphicsSound_API35 emulator; not physical hardware',
                                  sdk=35, samples_ns=ns, first_ms=ns[0]/1e6, median_ms=sorted(ns)[2]/1e6,
                                  measurement='Main-thread Impossible selection only; excludes setup/apply/1000ms delay'))
(OUT / 'native-counts.md').write_text(native_counts_table(inventory, summary))
jvm_suites = [ET.parse(p).getroot() for p in sorted((OUT / 'jvm-xml').glob('*.xml'))]
(OUT / 'jvm-counts.md').write_text(jvm_counts_table(jvm_suites))
assert all(r['unchanged'] for r in rows) and all(v for k, v in check.items() if isinstance(v, bool))
assert summary['jvm_verified_pass'] and summary['native_all_verified'] and summary['protected_files_unchanged']
print(json.dumps(dict(baseline_files_unchanged=len(rows), isolation=check,
                     jvm=summary['jvm_counts'], native=summary['native_passed'], timing_ns=ns), indent=2))
