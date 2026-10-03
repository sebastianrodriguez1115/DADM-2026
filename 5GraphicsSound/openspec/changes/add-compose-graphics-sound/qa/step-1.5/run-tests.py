#!/usr/bin/env python3
"""Independent class-isolated Android QA, with exact count and serial redaction."""
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

QA = Path(__file__).resolve().parent
ROOT = QA.parents[4]
OUT = QA / 'evidence'
OUT.mkdir(parents=True, exist_ok=True)
env = os.environ.copy()
env.update(JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home',
           ANDROID_HOME='/Users/jsrc1115/Library/Android/sdk')
adb = str(Path(env['ANDROID_HOME']) / 'platform-tools/adb')
lines = subprocess.run([adb, 'devices'], capture_output=True, text=True, check=True).stdout.splitlines()[1:]
identifiers = [line.split()[0] for line in lines if len(line.split()) >= 2]
assert sum(len(line.split()) >= 2 and line.split()[1] == 'device' for line in lines) == 1, 'One authorized device required'

def redact(text):
    for identifier in identifiers:
        text = text.replace(identifier, 'AUTHORIZED_DEVICE')
    return text

jobs = [('interaction', 'GameBoardInteractionTest', 5),
        ('screen', 'TicTacToeScreenTest', 15),
        ('accessibility', 'MainActivityAccessibilityTest', 4),
        ('render', 'GameBoardRenderingTest', 3)]
if len(sys.argv) > 1:
    jobs = [job for job in jobs if job[0] == sys.argv[1]]
    assert jobs, 'Use interaction/screen/accessibility/render'
summary = []
for label, cls, count in jobs:
    command = ['./gradlew', 'connectedDebugAndroidTest',
               '-Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe.' + cls,
               '--no-configuration-cache', '--console=plain']
    start = time.monotonic()
    run = subprocess.run(command, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (OUT / (label + '.log')).write_text(redact(run.stdout))
    cases = []
    for i, source in enumerate(sorted((ROOT / 'app/build/outputs/androidTest-results/connected/debug').rglob('TEST-*.xml'))):
        text = redact(source.read_text())
        tree = ET.fromstring(text)
        name = label + '-results-' + str(i) + '.xml'
        (OUT / name).write_text(text)
        for case in tree.iter('testcase'):
            cases.append({'class': case.get('classname'), 'name': case.get('name'),
                          'result': 'FAIL' if case.find('failure') is not None or case.find('error') is not None else 'SKIP' if case.find('skipped') is not None else 'PASS',
                          'evidence': name})
    ok = run.returncode == 0 and len(cases) == count and all(c['result'] == 'PASS' and c['class'] == 'com.edu.unal.tictactoe.' + cls for c in cases)
    result = {'run': label, 'command': command, 'exit': run.returncode, 'seconds': round(time.monotonic()-start, 2), 'expected_count': count, 'verified': ok, 'cases': cases}
    summary.append(result)
    (OUT / (label + '-summary.json')).write_text(json.dumps(result, indent=2))
    print(f'{label}: exit={run.returncode}; count={len(cases)}/{count}; verified={ok}', flush=True)
    if not ok:
        break
raise SystemExit(0 if all(r['verified'] for r in summary) else 1)
