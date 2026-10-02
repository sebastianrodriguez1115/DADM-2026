#!/usr/bin/env python3
"""Run existing Gradle tasks; retain sanitized fresh evidence (no product edits)."""
import datetime
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[5]
QA = Path(__file__).resolve().parents[1]
ADB = str(Path.home() / 'Library/Android/sdk/platform-tools/adb')

def discover():
    p = subprocess.run([ADB, 'devices'], capture_output=True, text=True)
    devices = [x.split()[0] for x in p.stdout.splitlines()[1:]
               if len(x.split()) > 1 and x.split()[1] == 'device']
    if len(devices) != 1:
        raise RuntimeError('Exactly one authorized device required')
    return devices[0]

def sanitize(text, serial):
    return text.replace(serial, '<device>').replace(str(ROOT), '<project>').replace(str(Path.home()), '<home>')

def main():
    mode = sys.argv[1]
    serial = discover()
    env = dict(os.environ)
    env['JAVA_HOME'] = '/Applications/Android Studio.app/Contents/jbr/Contents/Home'
    env['PATH'] = env['JAVA_HOME'] + '/bin:' + env['PATH']
    env['ANDROID_SERIAL'] = serial
    tasks = ['testDebugUnitTest', 'lintDebug', 'assembleDebug'] if mode == 'local' else ['connectedDebugAndroidTest']
    cmd = ['./gradlew'] + tasks + ['--rerun-tasks', '-Pandroid.builder.sdkDownload=false', '--console=plain']
    dest = QA / 'evidence' / mode
    dest.mkdir(parents=True, exist_ok=True)
    start = datetime.datetime.now(datetime.timezone.utc).isoformat()
    if mode == 'connected':
        t = subprocess.run([ADB, '-s', serial, 'shell', "date '+%m-%d %H:%M:%S.000'"], capture_output=True, text=True)
        (dest / 'device-period-start.txt').write_text(t.stdout.strip() + '\n')
    with (dest / 'gradle.log').open('w') as log:
        p = subprocess.Popen(cmd, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        for line in p.stdout:
            clean = sanitize(line, serial)
            log.write(clean)
            log.flush()
            print(clean, end='', flush=True)
        rc = p.wait()
    summary = {'start_utc': start, 'end_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
               'command': ' '.join(cmd), 'exit_code': rc}
    (dest / 'execution.json').write_text(json.dumps(summary, indent=2) + '\n')
    print('EXIT_CODE:', rc)
    return rc

if __name__ == '__main__':
    sys.exit(main())
