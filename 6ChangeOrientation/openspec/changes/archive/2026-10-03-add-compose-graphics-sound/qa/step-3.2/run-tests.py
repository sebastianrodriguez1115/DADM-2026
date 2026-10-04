"""Run fresh project tests without editing product/test configuration; redact device IDs."""
from pathlib import Path
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[5]
QA = Path(__file__).resolve().parent
ENV = dict(os.environ, JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home',
           ANDROID_HOME='/Users/jsrc1115/Library/Android/sdk')
ADB = ENV['ANDROID_HOME'] + '/platform-tools/adb'
devices = subprocess.run([ADB, 'devices'], capture_output=True, text=True, env=ENV)
rows = [line.split() for line in devices.stdout.splitlines()[1:] if line.strip()]
serials = [row[0] for row in rows]
suffix = sys.argv[2] if len(sys.argv) > 2 else ''
if suffix and not re.fullmatch(r'[a-zA-Z0-9_-]+', suffix):
    raise SystemExit('Evidence label must contain only letters, digits, underscores or hyphens')

def redact(text):
    for serial in serials:
        text = text.replace(serial, '[DEVICE]')
    return text

def run(label, arguments, result_dir, expected):
    if suffix:
        label += '-' + suffix
    evidence = QA / 'evidence' / label
    if evidence.exists():
        raise SystemExit('Evidence already exists; supply a new label to preserve history')
    evidence.mkdir(parents=True)
    start = time.time()
    cmd = ['./gradlew', '--no-configuration-cache', '--rerun-tasks'] + arguments
    completed = subprocess.run(cmd, cwd=ROOT, env=ENV, capture_output=True, text=True)
    (evidence / 'gradle.log').write_text(redact(completed.stdout + completed.stderr))
    results = []
    for path in sorted((ROOT / result_dir).rglob('TEST-*.xml')):
        if path.stat().st_mtime < start:
            continue
        text = redact(path.read_text())
        suite = ET.fromstring(text)
        name = suite.attrib.get('name', path.stem)
        (evidence / (re.sub(r'[^a-zA-Z0-9._-]', '_', name) + '.xml')).write_text(text)
        results.append(dict(name=name, tests=int(suite.attrib.get('tests', 0)),
                            failures=int(suite.attrib.get('failures', 0)),
                            errors=int(suite.attrib.get('errors', 0)),
                            skipped=int(suite.attrib.get('skipped', 0))))
    totals = {key: sum(r[key] for r in results) for key in ['tests', 'failures', 'errors', 'skipped']}
    summary = dict(command=cmd, exit=completed.returncode, elapsed_seconds=round(time.time()-start, 2),
                   expected_tests=expected, totals=totals, suites=results,
                   accepted=completed.returncode == 0 and totals['tests'] == expected
                   and totals['failures'] == totals['errors'] == totals['skipped'] == 0)
    (evidence / 'summary.json').write_text(json.dumps(summary, indent=2))
    print(label, json.dumps({k: v for k, v in summary.items() if k != 'suites'}), flush=True)
    return summary['accepted']

mode = sys.argv[1] if len(sys.argv) > 1 else 'unit'
if mode == 'unit':
    accepted = run('unit', ['testDebugUnitTest'], 'app/build/test-results/testDebugUnitTest', 91)
elif mode in ['compose', 'screen-repeat']:
    if not any(len(row) > 1 and row[1] == 'device' for row in rows):
        print('BLOCKED: no authorized connected device', flush=True)
        sys.exit(2)
    accepted = True
    classes = [('TicTacToeScreenTest', 15)] if mode == 'screen-repeat' else [('TicTacToeScreenTest', 15), ('GameBoardInteractionTest', 5)]
    for name, count in classes:
        passed = run(name + ('-repeat' if mode == 'screen-repeat' else ''), ['connectedDebugAndroidTest',
            '-Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe.' + name],
            'app/build/outputs/androidTest-results/connected/debug', count)
        accepted = accepted and passed
else:
    raise SystemExit('usage: run-tests.py unit|compose|screen-repeat [new-evidence-label]')
sys.exit(0 if accepted else 1)
