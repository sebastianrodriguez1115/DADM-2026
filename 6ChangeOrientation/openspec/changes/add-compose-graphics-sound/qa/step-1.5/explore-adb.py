#!/usr/bin/env python3
"""Explore the real Activity; only own-app hierarchy and board crops persist."""
import json
import re
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

QA = Path(__file__).resolve().parent
ROOT = QA.parents[4]
OUT = QA / 'evidence'
OUT.mkdir(parents=True, exist_ok=True)
ADB = '/Users/jsrc1115/Library/Android/sdk/platform-tools/adb'
PACKAGE = 'com.edu.unal.tictactoe'
checks = []

def adb(*args):
    result = subprocess.run([ADB, *args], capture_output=True)
    if result.returncode:
        raise RuntimeError('ADB command failed; device output not retained')
    return result.stdout

def own_focus():
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        windows = adb('shell', 'dumpsys', 'window').decode()
        if any('mCurrentFocus=' in line and PACKAGE + '.MainActivity' in line for line in windows.splitlines()):
            return
    raise RuntimeError('Own Activity not focused; no input/capture permitted')

def hierarchy(name):
    own_focus()
    raw = adb('exec-out', 'uiautomator', 'dump', '/dev/tty').decode()
    match = re.search(r'(<\?xml.*?</hierarchy>)', raw, re.S)
    if not match:
        raise RuntimeError('Native hierarchy unavailable')
    nodes = [{key: n.get(key) for key in ('text', 'content-desc', 'bounds', 'clickable', 'enabled', 'focusable', 'focused', 'class')}
             for n in ET.fromstring(match.group(1)).iter('node') if n.get('package') == PACKAGE]
    (OUT / ('native-' + name + '.json')).write_text(json.dumps(nodes, ensure_ascii=False, indent=2))
    return nodes

def bounds(node):
    return list(map(int, re.findall(r'\d+', node['bounds'])))

def cells(nodes):
    return sorted([n for n in nodes if (n['content-desc'] or '').startswith('Casilla,')], key=lambda n: n['content-desc'])

def validate(nodes):
    selected = cells(nodes)
    assert len(selected) == 9, 'Nine native cells required'
    assert not any(n['text'] in ('X', 'O') for n in nodes), 'No visual mark text'
    for index, cell in enumerate(selected):
        assert cell['content-desc'].startswith(f'Casilla, fila {index//3+1}, columna {index%3+1}, ')
        assert cell['class'] == 'android.widget.Button', 'Native button role'
        assert cell['enabled'] in ('true', 'false')
        assert cell['clickable'] == cell['enabled'], 'Disabled cell has no native click action'
    return selected

def tap(x, y):
    own_focus()
    adb('shell', 'input', 'tap', str(x), str(y))

def tap_cell(node):
    x1, y1, x2, y2 = bounds(node)
    tap((x1+x2)//2, (y1+y2)//2)

def snapshot(nodes):
    return [(n['text'], n['content-desc'], n['enabled'], n['clickable'], n['bounds']) for n in nodes]

def capture(nodes, name):
    board = next(n for n in nodes if n['content-desc'] == 'Tablero de tres en raya')
    x1, y1, x2, y2 = bounds(board)
    own_focus()
    temporary = OUT / 'own-app-temporary.png'
    try:
        temporary.write_bytes(adb('exec-out', 'screencap', '-p'))
        subprocess.run(['sips', '--cropToHeightWidth', str(y2-y1), str(x2-x1), '--cropOffset', str(y1), str(x1), str(temporary), '--out', str(OUT / (name + '.png'))], capture_output=True, check=True)
    finally:
        temporary.unlink(missing_ok=True)

def run():
    lines = adb('devices').decode().splitlines()[1:]
    assert sum(len(line.split()) >= 2 and line.split()[1] == 'device' for line in lines) == 1, 'One authorized device required'
    adb('install', '-r', str(ROOT / 'app/build/outputs/apk/debug/app-debug.apk'))
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    before = hierarchy('before')
    empty = validate(before)
    assert all(c['enabled'] == 'true' and c['content-desc'].endswith('vacía') for c in empty)
    checks.append('Nine independently labeled, enabled native Buttons; empty labels, no Text X/O')
    density_output = adb('shell', 'wm', 'density').decode()
    density = int(re.findall(r'(?:Physical|Override) density:\s*(\d+)', density_output)[-1]) / 160
    sizes = []
    for c in empty:
        x1, y1, x2, y2 = bounds(c)
        sizes.append([(x2-x1)/density, (y2-y1)/density])
    assert all(w >= 48 and h >= 48 for w, h in sizes), 'Native targets >=48dp'
    (OUT / 'native-target-sizes.json').write_text(json.dumps({'density_scale': density, 'cell_sizes_dp': sizes}, indent=2))
    checks.append('Native cell bounds >=48dp in current physical viewport')
    capture(before, 'board-empty')
    board = next(n for n in before if n['content-desc'] == 'Tablero de tres en raya')
    bx1, by1, bx2, by2 = bounds(board)
    for index, (x, y) in enumerate(((bx1-8, (by1+by2)//2), (bx2+8, (by1+by2)//2), ((bx1+bx2)//2, by1-8))):
        tap(x, y)
        outside = hierarchy('outside-' + str(index))
        assert snapshot(before) == snapshot(outside), 'Outside tap changed state'
    checks.append('Outside left/right/top taps leave board and score unchanged')
    tap_cell(empty[0])
    after = hierarchy('after-one-move')
    marked = validate(after)
    assert sum(c['content-desc'].endswith(', X') for c in marked) == 1
    assert sum(c['content-desc'].endswith(', O') for c in marked) == 1
    assert all(c['enabled'] == 'false' and c['clickable'] == 'false' for c in marked if not c['content-desc'].endswith('vacía'))
    checks.append('One touch applies exactly one X plus current synchronous O; occupied X/O disabled/nonclickable')
    capture(after, 'board-vector-marks')
    for index, occupied in enumerate(c for c in marked if c['enabled'] == 'false'):
        tap_cell(occupied)
        rejected = hierarchy('occupied-rejected-' + str(index))
        assert snapshot(after) == snapshot(rejected), 'Occupied tap changed board/score'
    checks.append('Repeat touches on both X and O leave board and score unchanged')
    current = after
    for index in range(8):
        selected = validate(current)
        if all(c['enabled'] == 'false' for c in selected):
            break
        tap_cell(next(c for c in selected if c['enabled'] == 'true'))
        current = hierarchy('progress-' + str(index))
    terminal = validate(current)
    assert all(c['enabled'] == 'false' and c['clickable'] == 'false' for c in terminal)
    assert any(n['text'] in ('Empate', 'Victoria humana', 'Victoria de la computadora') for n in current), 'Terminal status required'
    empty_count = sum(c['content-desc'].endswith('vacía') for c in terminal)
    capture(current, 'board-terminal')
    for index, cell in enumerate(terminal):
        tap_cell(cell)
        rejected = hierarchy('terminal-rejected-' + str(index))
        assert snapshot(current) == snapshot(rejected), 'Terminal tap changed board/score'
    checks.append(f'Terminal ({empty_count} empty cells): nine disabled nonclickable Buttons; all nine touches rejected')

try:
    run()
except Exception as error:
    (OUT / 'exploratory-result.json').write_text(json.dumps({'result': 'FAIL', 'completed_checks': checks, 'error': str(error)}, ensure_ascii=False, indent=2))
    print('Native exploratory FAIL; see own-app evidence')
    raise SystemExit(1)
else:
    (OUT / 'exploratory-result.json').write_text(json.dumps({'result': 'PASS', 'checks': checks, 'privacy': 'Own-app nodes/board crops only; no serials', 'settings_changes': 'none', 'installed': 'Project debug APK only', 'limitations': 'No human TalkBack; no physical keyboard; O currently synchronous'}, ensure_ascii=False, indent=2))
    print('Native exploratory PASS; see evidence/exploratory-result.json')
