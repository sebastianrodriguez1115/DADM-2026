#!/usr/bin/env python3
"""Native black-box menu charter. Run after run-native.sh; stdlib only."""
import datetime
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

QA = pathlib.Path(__file__).resolve().parent.parent
EVIDENCE = QA / 'evidence'
ADB = '/Users/jsrc1115/Library/Android/sdk/platform-tools/adb'
SERIAL = 'emulator-5556'

def raw(*args):
    return subprocess.check_output([ADB, '-s', SERIAL, *args], timeout=30)

def guard():
    identity = (
        raw('emu', 'avd', 'name').decode().splitlines()[0].strip(),
        raw('shell', 'getprop', 'ro.build.version.sdk').decode().strip(),
        raw('shell', 'getprop', 'ro.product.cpu.abi').decode().strip(),
        raw('shell', 'getprop', 'sys.boot_completed').decode().strip(),
    )
    assert identity == ('DADM_GraphicsSound_API35', '35', 'arm64-v8a', '1'), identity
    with (EVIDENCE / 'exploratory-guards.log').open('a') as f:
        f.write(f'{datetime.datetime.now(datetime.timezone.utc).isoformat()} {SERIAL} {identity}\n')

def adb(*args):
    guard()
    return raw(*args)

def dump(name):
    data = adb('exec-out', 'uiautomator', 'dump', '/dev/tty').decode()
    xml = data[data.index('<?xml'):data.index('</hierarchy>') + len('</hierarchy>')]
    (EVIDENCE / f'{name}.xml').write_text(xml)
    return ET.fromstring(xml)

def nodes(tree):
    return list(tree.iter('node'))

def texts(tree):
    return [n.get('text') for n in nodes(tree) if n.get('text')]

def locate(tree, text=None, desc=None):
    matches = [n for n in nodes(tree) if
               (text is not None and n.get('text') == text) or
               (desc is not None and n.get('content-desc') == desc)]
    assert len(matches) == 1, (text, desc, len(matches))
    n = matches[0]
    parents = {child: parent for parent in tree.iter() for child in parent}
    while n.get('clickable') != 'true' and n in parents:
        n = parents[n]
    assert n.get('clickable') == 'true', (text, desc)
    return n

def tap(node):
    assert node.get('enabled') == 'true', node.attrib
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def screenshot(name):
    (EVIDENCE / f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p'))

def menu(tree, name):
    tap(locate(tree, desc='Abrir menú'))
    return dump(name)

def board(tree):
    return [n.get('content-desc') for n in nodes(tree)
            if n.get('content-desc', '').startswith('Casilla,')]

def score(tree):
    return [s for s in texts(tree) if s.startswith(('Victorias de', 'Empates:'))]

def log(message):
    print(message, flush=True)
    with (EVIDENCE / 'exploratory-results.log').open('a') as f:
        f.write(message + '\n')

for filename in ('exploratory-results.log', 'exploratory-guards.log'):
    (EVIDENCE / filename).unlink(missing_ok=True)
adb('shell', 'am', 'start', '-S', '-W', '-n', 'com.edu.unal.tictactoe/.MainActivity')
tree = dump('explore-start')
before_board, before_score = board(tree), score(tree)
tree = menu(tree, 'explore-sound-enabled')
labels = ['Nueva partida', 'Dificultad', 'Sonido', 'Acerca de', 'Salir']
assert [s for s in texts(tree) if s in labels] == labels
assert 'Habilitado' in texts(tree)
assert locate(tree, text='Dificultad').get('enabled') == 'true'
screenshot('menu-sound-enabled')
log('E01 PASS: real Activity menu has five labels in order, sound Habilitado and empty-board difficulty enabled.')
tap(locate(tree, text='Sonido'))
tree = dump('explore-after-mute')
assert 'Sonido' not in texts(tree)
assert board(tree) == before_board and score(tree) == before_score
tree = menu(tree, 'explore-sound-disabled')
assert 'Deshabilitado' in texts(tree)
assert locate(tree, text='Sonido').get('enabled') == 'true'
screenshot('menu-sound-disabled')
log('E02 PASS: real Activity mute closes menu, preserves board/score, displays Deshabilitado and stays actionable.')
tap(locate(tree, text='Sonido'))
tree = dump('explore-after-unmute')
tree = menu(tree, 'explore-reenabled')
assert 'Habilitado' in texts(tree)
log('E03 PASS: real Activity mute→unmute displays Habilitado (callback wiring works).')
adb('shell', 'input', 'keyevent', '4')
tree = dump('explore-game-start')
terminal = {'Empate', 'Victoria humana', 'Victoria de la computadora'}
deadline = time.monotonic() + 90
move = 0
while not terminal.intersection(texts(tree)):
    assert time.monotonic() < deadline, 'Timed out waiting for terminal game'
    cells = [n for n in nodes(tree) if n.get('content-desc', '').startswith('Casilla,')
             and n.get('enabled') == 'true' and n.get('content-desc', '').endswith('vacía')]
    if cells:
        tap(cells[0])
        move += 1
    tree = dump(f'explore-play-{move}')
result = terminal.intersection(texts(tree))
tree = menu(tree, 'explore-terminal-menu')
assert locate(tree, text='Dificultad').get('enabled') == 'true'
screenshot('menu-terminal')
tap(locate(tree, text='Dificultad'))
tree = dump('explore-terminal-difficulty-dialog')
assert all(s in texts(tree) for s in ('Fácil', 'Difícil', 'Experto', 'Imposible'))
assert 'Sonido' not in texts(tree)
log(f'E04 PASS: terminal {result}, difficulty enabled and opens four-level dialog with menu closed.')
adb('shell', 'input', 'keyevent', '4')
log('Completed; no background QA process spawned, no audio/TalkBack/persistence certification.')
