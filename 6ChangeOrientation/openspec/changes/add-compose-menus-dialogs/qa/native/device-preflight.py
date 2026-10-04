#!/usr/bin/env python3
"""Read only device/install preflight; does not change phone settings/data."""
import json
from pathlib import Path
import subprocess
import sys
from importlib.machinery import SourceFileLoader

runner = SourceFileLoader('runner', str(Path(__file__).with_name('run-gradle.py'))).load_module()
s = runner.discover()
def adb(*args):
    return subprocess.run([runner.ADB, '-s', s] + list(args), capture_output=True, text=True)

data = {}
for p in ['ro.build.version.sdk', 'ro.build.version.release', 'ro.product.manufacturer', 'ro.product.model']:
    data[p] = adb('shell', 'getprop', p).stdout.strip()
data['compatible_min_sdk'] = int(data['ro.build.version.sdk']) >= 35
window = adb('shell', 'dumpsys', 'window').stdout
policy = adb('shell', 'dumpsys', 'window', 'policy').stdout
data['lock_indicators'] = [l.strip() for l in (window + policy).splitlines()
                         if any(x in l for x in ['isStatusBarKeyguard=', 'mShowingLockscreen=', 'mInputRestricted=', 'mDreamingLockscreen='])]
data['packages'] = {}
for pkg in ['com.edu.unal.tictactoe', 'com.edu.unal.tictactoe.test']:
    result = adb('shell', 'pm', 'path', pkg)
    data['packages'][pkg] = {'installed': bool(result.stdout.strip())}
dest = runner.QA / 'evidence' / 'device-preflight.json'
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(json.dumps(data, indent=2) + '\n')
print(json.dumps(data, indent=2))
if not data['compatible_min_sdk']:
    print('BLOCKED: API below minSdk'); sys.exit(2)
if any(x['installed'] for x in data['packages'].values()):
    print('Existing package found: verify signing before proceeding. No install/uninstall performed.'); sys.exit(3)
