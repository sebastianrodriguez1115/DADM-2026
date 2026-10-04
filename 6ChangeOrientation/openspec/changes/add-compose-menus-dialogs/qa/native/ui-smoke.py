#!/usr/bin/env python3
"""Native exploratory driver. App-only XML/screenshots; no settings changes."""
import json
from pathlib import Path
import re
import struct
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
import zlib
from importlib.machinery import SourceFileLoader

r = SourceFileLoader('runner', str(Path(__file__).with_name('run-gradle.py'))).load_module()
S = r.discover()
PKG = 'com.edu.unal.tictactoe'
DEST = r.QA / 'evidence' / 'smoke'
DEST.mkdir(parents=True, exist_ok=True)

def adb(*args, binary=False):
    p = subprocess.run([r.ADB, '-s', S] + list(args), capture_output=True, text=not binary)
    if p.returncode:
        raise RuntimeError('adb action failed: ' + r.sanitize(str(p.stderr), S))
    return p.stdout

def tree():
    text = adb('exec-out', 'uiautomator', 'dump', '/dev/stdout')
    start, end = text.find('<?xml'), text.rfind('</hierarchy>')
    if start < 0 or end < 0:
        raise RuntimeError('UI hierarchy unavailable')
    root = ET.fromstring(text[start:end + len('</hierarchy>')])
    nodes = []
    for node in root.iter('node'):
        if node.get('package') == PKG:
            nodes.append(dict(node.attrib))
    return nodes

def dump(name):
    nodes = tree()
    (DEST / (name + '.json')).write_text(json.dumps(nodes, indent=2, ensure_ascii=False) + '\n')
    for n in nodes:
        if n.get('text') or n.get('content-desc'):
            print(n.get('text') or n.get('content-desc'), n.get('bounds'), 'enabled=' + n.get('enabled',''), 'selected=' + n.get('selected',''), 'checked=' + n.get('checked',''))
    return nodes

def chunk(kind, body):
    return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xffffffff)

def crop_png(data, top_fraction=.033, bottom_fraction=.075):
    # Android screencap emits 8-bit RGB/RGBA PNG. Decode/unfilter in memory,
    # remove status/navigation strips and encode without metadata.
    offset, compressed, header = 8, b'', None
    while offset < len(data):
        size = struct.unpack('>I', data[offset:offset+4])[0]
        kind = data[offset+4:offset+8]
        body = data[offset+8:offset+8+size]
        if kind == b'IHDR': header = struct.unpack('>IIBBBBB', body)
        if kind == b'IDAT': compressed += body
        offset += size + 12
    width, height, depth, color, comp, filt, interlace = header
    if depth != 8 or color not in (2,6) or interlace != 0:
        raise RuntimeError('Unsupported PNG; no raw screenshot saved')
    bpp = 4 if color == 6 else 3
    stride = width * bpp
    raw = zlib.decompress(compressed)
    previous = bytearray(stride)
    rows = []
    for y in range(height):
        pos = y * (stride+1)
        method = raw[pos]
        row = bytearray(raw[pos+1:pos+1+stride])
        for i in range(stride):
            a = row[i-bpp] if i >= bpp else 0
            b = previous[i]
            c = previous[i-bpp] if i >= bpp else 0
            if method == 1: v = a
            elif method == 2: v = b
            elif method == 3: v = (a+b)//2
            elif method == 4:
                p = a+b-c
                pa,pb,pc = abs(p-a),abs(p-b),abs(p-c)
                v = a if pa<=pb and pa<=pc else (b if pb<=pc else c)
            elif method == 0: v = 0
            else: raise RuntimeError('Invalid PNG filter')
            row[i] = (row[i]+v)&255
        rows.append(bytes(row))
        previous = row
    trim_top = round(height*top_fraction)
    trim_bottom = round(height*bottom_fraction)
    rows = rows[trim_top:height-trim_bottom]
    ihdr = struct.pack('>IIBBBBB', width,len(rows),depth,color,comp,filt,interlace)
    body = zlib.compress(b''.join(b'\x00'+row for row in rows),6)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR',ihdr)+chunk(b'IDAT',body)+chunk(b'IEND',b'')

def capture(name):
    window = adb('shell','dumpsys','window')
    focus = [l for l in window.splitlines() if 'mCurrentFocus=' in l]
    focus_id = re.search(r'mCurrentFocus=Window\{(\w+)', window)
    focus_block = re.search(r'Window #\d+ Window\{' + focus_id.group(1) +
                            r'.*?(?=\n  Window #|\Z)', window, re.S) if focus_id else None
    app_popup = bool(focus_block and 'package=' + PKG in focus_block.group(0))
    if not focus or (PKG not in focus[0] and not app_popup):
        raise RuntimeError('App not focused; refusing screenshot')
    data = adb('exec-out','screencap','-p',binary=True)
    (DEST / (name+'.png')).write_bytes(crop_png(data))
    print('App screenshot retained:', name+'.png', '(status/navigation strips removed)')

def click(label, capture_name=None):
    deadline = time.monotonic()+12
    node = None
    while time.monotonic()<deadline:
        candidates = [n for n in tree() if n.get('text') == label or n.get('content-desc') == label]
        if candidates:
            node = candidates[-1]
            break
    if node is None:
        raise RuntimeError('App label unavailable: '+label)
    nums = list(map(int,re.findall(r'\d+',node['bounds'])))
    x,y = (nums[0]+nums[2])//2,(nums[1]+nums[3])//2
    adb('shell','input','tap',str(x),str(y))
    if capture_name:
        capture(capture_name)
    print('Tapped:',label)

if __name__ == '__main__':
    action = sys.argv[1]
    if action == 'launch':
        out = adb('shell','am','start','-W','-n',PKG+'/.MainActivity')
        print(r.sanitize(out,S))
    elif action == 'dump': dump(sys.argv[2])
    elif action == 'capture': capture(sys.argv[2])
    elif action == 'click': click(sys.argv[2],sys.argv[3] if len(sys.argv)>3 else None)
    elif action == 'back': adb('shell','input','keyevent','KEYCODE_BACK')
    elif action == 'outside': adb('shell','input','tap',sys.argv[3] if len(sys.argv)>3 else '4',sys.argv[2])
    else: raise RuntimeError('Unknown action')
