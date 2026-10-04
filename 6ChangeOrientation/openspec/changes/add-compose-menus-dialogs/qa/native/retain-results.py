#!/usr/bin/env python3
"""Retain sanitized current XML, lint findings and case outcomes."""
import datetime
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from importlib.machinery import SourceFileLoader

r = SourceFileLoader('runner', str(Path(__file__).with_name('run-gradle.py'))).load_module()
mode = sys.argv[1]
dest = r.QA / 'evidence' / mode
execution = json.loads((dest / 'execution.json').read_text())
start = datetime.datetime.fromisoformat(execution['start_utc']).timestamp()
serial = r.discover()
def sanitize_xml(text):
    tree = ET.fromstring(text)
    for node in tree.iter():
        node.attrib.update({k: r.sanitize(v, serial) for k, v in node.attrib.items()})
        if node.text:
            node.text = r.sanitize(node.text, serial)
        if node.tail:
            node.tail = r.sanitize(node.tail, serial)
    return ET.tostring(tree, encoding='unicode')
source = r.ROOT / ('app/build/test-results/testDebugUnitTest' if mode == 'local' else 'app/build/outputs/androidTest-results/connected/debug')
files = sorted(source.rglob('TEST-*.xml'))
if not files:
    raise RuntimeError('No XML found; cannot claim test success')
totals = dict(tests=0, failures=0, errors=0, skipped=0)
records = []
for i, path in enumerate(files):
    if path.stat().st_mtime < start:
        raise RuntimeError('Stale XML: ' + path.name)
    text = sanitize_xml(path.read_text())
    root = ET.fromstring(text)
    suites = [root] if root.tag == 'testsuite' else root.findall('testsuite')
    for suite in suites:
        for k in totals:
            totals[k] += int(suite.get(k, 0))
        for tc in suite.findall('testcase'):
            status = 'FAIL' if tc.find('failure') is not None or tc.find('error') is not None else ('SKIP' if tc.find('skipped') is not None else 'PASS')
            records.append(dict(classname=tc.get('classname'), name=tc.get('name'), status=status, seconds=tc.get('time')))
    output = dest / 'xml' / ('%02d-' % i + r.sanitize(path.name, serial))
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(text)
summary = dict(totals=totals, fresh_xml=True, xml_count=len(files), cases=records)
def traceability(case):
    cls = case['classname'].split('.')[-1]
    name = case['name'].lower()
    if cls.startswith('Example'):
        return ('QA01' if mode == 'local' else 'QA02', 'harness/build')
    if cls == 'ImpossibleOpeningTimingTest':
        return ('QA10', 'specs/tic-tac-toe-game')
    if cls in ('LauncherIconTest', 'AboutDialogTest'):
        return ('QA08', 'specs/game-options')
    if cls == 'MainActivityAccessibilityTest':
        return ('QA09', 'specs/game-options + specs/tic-tac-toe-game')
    if cls == 'ExitConfirmationDialogTest' or (cls == 'MainActivityOptionsTest' and name.startswith('exitdialog')):
        return ('QA06', 'specs/game-options')
    if cls == 'MainActivityOptionsTest' and name.startswith('about'):
        return ('QA08', 'specs/game-options')
    if 'Difficulty' in cls or cls in ('GameMenuTest', 'GameScreenTest'):
        if cls == 'DifficultyPolicyTest':
            return ('QA03', 'specs/tic-tac-toe-game')
        return ('QA04/QA07', 'specs/game-options')
    if cls in ('ImpossiblePolicyTest', 'ImpossibleExhaustiveTest', 'TicTacToeGameTest', 'GameModelContractsTest'):
        return ('QA03', 'specs/tic-tac-toe-game')
    return ('QA05/QA09', 'specs/game-session + specs/tic-tac-toe-game + specs/game-options')

for i, case in enumerate(records, 1):
    case['id'] = ('UT' if mode == 'local' else 'IT') + '-%03d' % i
    case['plan'], case['capability'] = traceability(case)
if mode == 'local':
    lint = r.ROOT / 'app/build/reports/lint-results-debug.xml'
    text = sanitize_xml(lint.read_text())
    (dest / 'lint-results-debug.xml').write_text(text)
    issues = ET.fromstring(text).findall('issue')
    summary['lint'] = [dict(id=x.get('id'), severity=x.get('severity'), message=x.get('message'), locations=[dict(z.attrib) for z in x.findall('location')]) for x in issues]
(dest / 'summary.json').write_text(json.dumps(summary, indent=2, ensure_ascii=False) + '\n')
(dest / 'cases.md').write_text('| ID | Plan | Capability | Class | Case | Result | Seconds |\n|---|---|---|---|---|---|---|\n' + '\n'.join('| {id} | {plan} | {capability} | {classname} | {name} | {status} | {seconds} |'.format(**x) for x in records) + '\n')
print(json.dumps({k:v for k,v in summary.items() if k!='cases'}, indent=2, ensure_ascii=False))
