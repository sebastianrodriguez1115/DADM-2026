#!/usr/bin/env python3
"""Fresh JVM + every inventoried native class, owned AVD only. Never retries."""
import collections
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

QA = Path(__file__).resolve().parent.parent
ROOT = QA.parents[4]
ADB = '/Users/jsrc1115/Library/Android/sdk/platform-tools/adb'
SERIAL = 'emulator-5556'
RUN = sys.argv[1] if len(sys.argv) > 1 else 'initial'
assert re.fullmatch(r'[a-zA-Z0-9_-]+', RUN)
OUT = QA / 'evidence' / RUN
OUT.mkdir(parents=True, exist_ok=False)
ENV = dict(os.environ, JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home',
           ANDROID_HOME='/Users/jsrc1115/Library/Android/sdk')
ENV['PATH'] = ENV['JAVA_HOME'] + '/bin:' + ENV['ANDROID_HOME'] + '/platform-tools:' + ENV.get('PATH', '')

def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()

def save(name, data):
    (OUT / name).write_text(json.dumps(data, indent=2) + '\n')

def inventory(directory):
    result = {}
    for p in sorted((ROOT / directory).rglob('*.kt')):
        text = p.read_text()
        methods = re.findall(r'@Test\b(?:(?!\bfun\b).)*\bfun\s+([A-Za-z0-9_]+)\s*\(', text, re.S)
        if methods:
            package = re.search(r'^package\s+(\S+)', text, re.M).group(1)
            clazz = re.search(r'^class\s+(\w+)', text, re.M).group(1)
            result[package + '.' + clazz] = dict(file=str(p.relative_to(ROOT)), methods=methods,
                                                count=len(methods), sha256=hashlib.sha256(p.read_bytes()).hexdigest())
    return result

def fingerprint():
    paths = list((ROOT / 'app/src').rglob('*')) + list((ROOT / 'gradle').rglob('*'))
    paths += list((ROOT / 'openspec/changes/add-compose-graphics-sound/specs').rglob('*'))
    prior_qa = ROOT / 'openspec/changes/add-compose-graphics-sound/qa'
    paths += [p for p in prior_qa.rglob('*') if QA not in p.parents]
    paths += [ROOT / p for p in ('app/build.gradle.kts', 'build.gradle.kts', 'settings.gradle.kts',
                               'openspec/config.yaml', 'openspec/changes/add-compose-graphics-sound/proposal.md',
                               'openspec/changes/add-compose-graphics-sound/design.md')]
    # tasks.md/integration-status.md deliberately excluded: authorized parallel 6.4 documentation.
    return {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(set(paths)) if p.is_file()}

def guard(operation):
    values = []
    for args in [('emu', 'avd', 'name'), ('shell', 'getprop', 'ro.build.version.sdk'),
                 ('shell', 'getprop', 'ro.product.cpu.abi'), ('shell', 'getprop', 'sys.boot_completed')]:
        value = subprocess.check_output([ADB, '-s', SERIAL, *args], env=ENV, timeout=20).decode().strip()
        values.append(value.splitlines()[0])
    record = dict(time=now(), operation=operation, serial=SERIAL, avd=values[0], sdk=values[1],
                  abi=values[2], boot=values[3])
    with (OUT / 'device-guards.jsonl').open('a') as f:
        f.write(json.dumps(record) + '\n')
    assert values == ['DADM_GraphicsSound_API35', '35', 'arm64-v8a', '1'], record
    print('GUARD PASS ' + operation, flush=True)

def run(label, command, device=False, timeout=600):
    assert not any('connected' in arg.lower() for arg in command), 'Device Gradle tasks prohibited'
    if device:
        assert command[:3] == [ADB, '-s', SERIAL]
        guard(label)
    started = now()
    with (OUT / (label + '.log')).open('w') as f:
        result = subprocess.run(command, cwd=ROOT, env=ENV, stdout=f, stderr=subprocess.STDOUT, timeout=timeout)
    with (OUT / 'commands.jsonl').open('a') as f:
        f.write(json.dumps(dict(label=label, command=command, start=started, finish=now(), exit=result.returncode)) + '\n')
    print(label + ': exit=' + str(result.returncode), flush=True)
    return result.returncode

def native_result(clazz, expected, rc):
    text = (OUT / (clazz.split('.')[-1] + '.log')).read_text()
    events, current = [], {}
    for line in text.splitlines():
        m = re.match(r'INSTRUMENTATION_STATUS: (class|test|numtests|stack)=(.*)', line)
        if m:
            current[m[1]] = m[2]
        m = re.match(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)', line)
        if m:
            events.append(dict(current, code=int(m[1])))
            current = {}
    starts = [e for e in events if e['code'] == 1]
    ends = [e for e in events if e['code'] != 1]
    ids = lambda records: [e.get('class', '') + '#' + e.get('test', '') for e in records]
    wanted = {clazz + '#' + method for method in expected['methods']}
    passed = [e for e in ends if e['code'] == 0]
    count = expected['count']
    ok = bool(re.search(rf'^OK \({count} tests?\)\s*$', text, re.M))
    final_code = re.findall(r'^INSTRUMENTATION_CODE: (-?\d+)\s*$', text, re.M)
    failure = bool(re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed', text))
    complete = (set(ids(starts)) == set(ids(ends)) == wanted and len(starts) == len(ends) == count
                and all(e.get('numtests') == str(count) for e in events))
    valid = (rc == 0 and complete and len(passed) == count and ok and final_code == ['-1'] and not failure)
    result = dict(clazz=clazz, expected=count, adb_exit=rc, complete_inventory=complete,
                  starts=len(starts), passed=len(passed), failed=sum(e['code'] in (-1, -2) for e in ends),
                  other_results=sum(e['code'] not in (0, -1, -2) for e in ends),
                  status_counts=dict(collections.Counter(e['code'] for e in events)),
                  ok_summary=ok, final_codes=final_code, failure_marker=failure,
                  verified_pass=valid, events=events)
    save(clazz.split('.')[-1] + '-validation.json', result)
    print(f"{clazz}: {len(passed)}/{count} PASS, verified={valid}", flush=True)
    return result

def main():
    native, jvm = inventory('app/src/androidTest'), inventory('app/src/test')
    save('inventory.json', dict(native=native, jvm=jvm,
                               native_total=sum(x['count'] for x in native.values()),
                               jvm_total=sum(x['count'] for x in jvm.values()), time=now()))
    before = fingerprint()
    save('protected-before.json', before)
    guard('preflight')
    rc = run('jvm', ['./gradlew', '--offline', '--no-configuration-cache', '--no-build-cache',
                     '--rerun-tasks', '--console=plain', ':app:testDebugUnitTest'])
    xml_dir = OUT / 'jvm-xml'
    xml_dir.mkdir()
    cases, counts = [], collections.Counter()
    for p in sorted((ROOT / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
        shutil.copy2(p, xml_dir / p.name)
        suite = ET.parse(p).getroot()
        for field in ('tests', 'failures', 'errors', 'skipped'):
            counts[field] += int(suite.get(field, 0))
        for case in suite.findall('testcase'):
            cases.append(dict(id=case.get('classname') + '#' + case.get('name'),
                              status='FAIL' if case.find('failure') is not None or case.find('error') is not None
                              else 'SKIP' if case.find('skipped') is not None else 'PASS'))
    wanted = {c + '#' + m for c, data in jvm.items() for m in data['methods']}
    jvm_pass = (rc == 0 and counts['tests'] == len(wanted) == len(cases) and
                {c['id'] for c in cases} == wanted and all(c['status'] == 'PASS' for c in cases))
    save('jvm-summary.json', dict(exit=rc, counts=dict(counts), classes=len(jvm), verified_pass=jvm_pass, cases=cases))
    assert run('build-apks', ['./gradlew', '--offline', '--no-configuration-cache', '--console=plain',
                              ':app:assembleDebug', ':app:assembleDebugAndroidTest']) == 0, 'APK build failed'
    apks = ['app/build/outputs/apk/debug/app-debug.apk',
            'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']
    save('apk-sha256.json', {p: hashlib.sha256((ROOT / p).read_bytes()).hexdigest() for p in apks})
    for label, apk in zip(('install-app', 'install-test'), apks):
        assert run(label, [ADB, '-s', SERIAL, 'install', '-r', apk], device=True) == 0
        assert re.search(r'^Success\s*$', (OUT / (label + '.log')).read_text(), re.M)
    results = []
    for clazz, expected in native.items():
        label = clazz.split('.')[-1]
        # Restore only our app process between independent class invocations; never change global settings.
        assert run(label + '-reset', [ADB, '-s', SERIAL, 'shell', 'am', 'force-stop',
                                     'com.edu.unal.tictactoe'], device=True) == 0
        rc = run(label, [ADB, '-s', SERIAL, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', clazz,
                        'com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner'], device=True)
        results.append(native_result(clazz, expected, rc))
        if label == 'ImpossibleOpeningTimingTest':
            run('impossible-timing-logcat', [ADB, '-s', SERIAL, 'logcat', '-d', '-v', 'epoch',
                                           '-s', 'Reto4Timing:I', '*:S'], device=True)
    after = fingerprint()
    save('protected-after.json', after)
    summary = dict(finish=now(), jvm_verified_pass=jvm_pass, jvm_counts=dict(counts),
                   native_expected=sum(x['count'] for x in native.values()), native_classes=len(native),
                   native_passed=sum(x['passed'] for x in results), native_failed=sum(x['failed'] for x in results),
                   native_other_results=sum(x['other_results'] for x in results),
                   native_all_verified=all(x['verified_pass'] for x in results),
                   unique_native_end_ids=len({e.get('class', '') + '#' + e.get('test', '')
                                              for r in results for e in r['events'] if e['code'] != 1}),
                   protected_files_unchanged=before == after,
                   protected_changes=[p for p in set(before) | set(after) if before.get(p) != after.get(p)],
                   suites=[{k: v for k, v in r.items() if k != 'events'} for r in results])
    save('summary.json', summary)
    print(json.dumps(summary, indent=2), flush=True)
    return 0 if jvm_pass and summary['native_all_verified'] and before == after else 1

try:
    sys.exit(main())
except Exception as exc:
    save('aborted.json', dict(time=now(), error=repr(exc)))
    raise
