#!/usr/bin/env python3
"""Native QA 5.4: serial-scoped ADB only; no Gradle device tasks."""
import collections
import datetime
import hashlib
import json
import os
import pathlib
import re
import subprocess

QA = pathlib.Path(__file__).resolve().parent.parent
ROOT = QA.parents[4]
EVIDENCE = QA / 'evidence'
EVIDENCE.mkdir(exist_ok=True)
ADB = '/Users/jsrc1115/Library/Android/sdk/platform-tools/adb'
SERIAL = 'emulator-5556'
ENV = dict(os.environ, JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home',
           ANDROID_HOME='/Users/jsrc1115/Library/Android/sdk')
ENV['PATH'] = ENV['JAVA_HOME'] + '/bin:' + ENV['ANDROID_HOME'] + '/platform-tools:' + ENV.get('PATH', '')
SUITES = [('MainActivitySoundOptionsTest', 1), ('MainActivityOptionsTest', 6),
          ('MainActivityAccessibilityTest', 4), ('MainActivityPendingTurnOptionsTest', 3),
          ('GameMenuTest', 6)]

def timestamp():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()

def fingerprint():
    paths = list((ROOT/'app/src').rglob('*'))
    qa_root = ROOT/'openspec/changes/add-compose-graphics-sound/qa'
    paths += [p for p in qa_root.rglob('*') if QA not in p.parents]
    paths += [ROOT/'openspec/changes/add-compose-graphics-sound/tasks.md', ROOT/'openspec/config.yaml',
              ROOT/'app/build.gradle.kts', ROOT/'gradle/libs.versions.toml']
    return {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(paths) if p.is_file()}

def write_json(filename, data):
    (EVIDENCE/filename).write_text(json.dumps(data, indent=2)+'\n')

def guard(operation):
    def query(*args):
        return subprocess.check_output([ADB, '-s', SERIAL, *args], env=ENV, timeout=20).decode().strip()
    identity = [query('emu', 'avd', 'name').splitlines()[0], query('shell', 'getprop', 'ro.build.version.sdk'),
                query('shell', 'getprop', 'ro.product.cpu.abi'), query('shell', 'getprop', 'sys.boot_completed')]
    record = dict(time=timestamp(), operation=operation, serial=SERIAL, avd=identity[0],
                  sdk=identity[1], abi=identity[2], boot=identity[3])
    with (EVIDENCE/'device-guards.jsonl').open('a') as f:
        f.write(json.dumps(record)+'\n')
    assert identity == ['DADM_GraphicsSound_API35', '35', 'arm64-v8a', '1'], record
    print(f'GUARD PASS {operation}: {SERIAL} {identity}', flush=True)

def run(label, command, device=False, timeout=180):
    if device:
        assert command[:3] == [ADB, '-s', SERIAL]
        guard(label)
    start = timestamp()
    with (EVIDENCE/f'{label}.log').open('w') as log:
        result = subprocess.run(command, cwd=ROOT, env=ENV, stdout=log, stderr=subprocess.STDOUT,
                                timeout=timeout)
    with (EVIDENCE/'exits.jsonl').open('a') as f:
        f.write(json.dumps(dict(label=label, start=start, finish=timestamp(), exit=result.returncode,
                               command=command))+'\n')
    return result.returncode

def validate(suite, count, exit_code):
    text = (EVIDENCE/f'{suite}.log').read_text()
    starts, passes, codes = [], [], []
    current = {}
    for line in text.splitlines():
        m = re.match(r'INSTRUMENTATION_STATUS: (class|test|numtests)=(.*)', line)
        if m:
            current[m[1]] = m[2]
        m = re.match(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)', line)
        if m:
            code = int(m[1]); codes.append(code)
            record = dict(current)
            if code == 1: starts.append(record)
            if code == 0: passes.append(record)
            current = {}
    names = [r.get('test') for r in passes]
    summary = bool(re.search(rf'^OK \({count} tests?\)\s*$', text, re.M))
    finished = bool(re.search(r'^INSTRUMENTATION_CODE: -1\s*$', text, re.M))
    failure = bool(re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed', text))
    valid = (exit_code == 0 and len(starts) == len(passes) == len(set(names)) == count
             and collections.Counter(r.get('test') for r in starts) == collections.Counter(names)
             and all(c in (0, 1) for c in codes) and summary and finished and not failure
             and all(r.get('class') == 'com.edu.unal.tictactoe.'+suite and r.get('numtests') == str(count)
                     for r in starts+passes))
    result = dict(suite=suite, expected=count, adb_exit=exit_code, starts=len(starts), passed=len(passes),
                  status_codes=codes, passed_methods=names, ok_summary=summary, runner_finished=finished,
                  failure_marker=failure, verified_pass=valid)
    write_json(f'{suite}-validation.json', result)
    print(json.dumps(result), flush=True)
    return result

for filename in ('exits.jsonl', 'device-guards.jsonl'):
    (EVIDENCE/filename).write_text('')
guard('preflight')
before = fingerprint()
write_json('protected-files-before.json', before)
assert run('git-status-before', ['git', 'status', '--short']) == 0
assert run('build', ['./gradlew', '--offline', '--no-configuration-cache',
                     ':app:assembleDebug', ':app:assembleDebugAndroidTest']) == 0, 'Build failed'
apk_paths = ['app/build/outputs/apk/debug/app-debug.apk',
             'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']
write_json('apk-sha256.json', {p: hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in apk_paths})
for label, apk in zip(('install-app', 'install-test'), apk_paths):
    rc = run(label, [ADB, '-s', SERIAL, 'install', '-r', apk], device=True)
    assert rc == 0 and re.search(r'^Success\s*$', (EVIDENCE/f'{label}.log').read_text(), re.M), label
results = []
for suite, count in SUITES:
    rc = run(suite, [ADB, '-s', SERIAL, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                    'com.edu.unal.tictactoe.'+suite,
                    'com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner'], device=True)
    results.append(validate(suite, count, rc))
after = fingerprint()
write_json('protected-files-after.json', after)
assert run('git-status-after', ['git', 'status', '--short']) == 0
summary = dict(expected_total=20, verified_pass_total=sum(r['expected'] for r in results if r['verified_pass']),
               all_suites_pass=all(r['verified_pass'] for r in results), protected_files_unchanged=before == after,
               suites=results, finish=timestamp())
write_json('summary.json', summary)
print(json.dumps(summary, indent=2), flush=True)
assert summary['all_suites_pass'] and summary['protected_files_unchanged'], 'QA did not pass; see evidence'
