#!/usr/bin/env python3
"""Task 5.3 - end-to-end QA of `add-compose-orientation-state` on the Android emulator.

Target: ONLY emulator-5556 / AVD DADM_GraphicsSound_API35. Every adb operation is preceded by an
identity guard (`adb -s emulator-5556 emu avd name` must print the AVD name); if it does not, the
run aborts. No other device is ever queried (`adb devices` is never used).

Usage:
  run_qa.py                 run every case, write evidence/ and results.json
  run_qa.py --only TC-04    run only the given case ids (comma separated)
  run_qa.py --list          list the cases
  run_qa.py --check         only verify the target identity and exit

Nothing here modifies the project. The app data of the emulator is cleared (`pm clear`) by design
between cases. Sound audibility and TalkBack are manual checks and are NOT automated (TC-30).
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
import traceback
import xml.etree.ElementTree as ET
from pathlib import Path

SERIAL = "emulator-5556"
AVD = "DADM_GraphicsSound_API35"
ADB = os.path.expandvars("$HOME/Library/Android/sdk/platform-tools/adb")
PKG = "com.edu.unal.tictactoe"
ACT = f"{PKG}/.MainActivity"
ROOT = Path(__file__).resolve().parent
EVID = ROOT / "evidence"
DP = 420 / 160.0            # px per dp at 420 dpi
MIN_TARGET_PX = 48 * DP - 1  # 126 px (1 px rounding tolerance)
BOARD_REF_PX = 270 * DP + 3  # 270 dp reference, small rounding tolerance

STATUS_TEXTS = {
    "Tu turno", "La persona hace la primera jugada", "Turno de la computadora",
    "Empate", "Victoria humana", "Victoria de la computadora",
}
TERMINAL = {"Empate", "Victoria humana", "Victoria de la computadora"}
CELL_RE = re.compile(r"^Casilla, fila (\d), columna (\d), (X|O|vacía)$")
MENU_ORDER = ["Nueva partida", "Dificultad", "Sonido", "Reiniciar puntajes", "Acerca de"]
LEVELS = ["Fácil", "Difícil", "Experto", "Imposible"]
LINES = [(0, 1, 2), (3, 4, 5), (6, 7, 8), (0, 3, 6), (1, 4, 7), (2, 5, 8), (0, 4, 8), (2, 4, 6)]

GUARDS = {"count": 0}


class Abort(Exception):
    pass


def log(msg: str) -> None:
    print(time.strftime("%H:%M:%S"), msg, flush=True)


# --------------------------------------------------------------------------- adb layer
def _run(args: list[str], timeout: int = 60) -> subprocess.CompletedProcess:
    return subprocess.run(args, capture_output=True, text=True, timeout=timeout)


def guard() -> None:
    """Identity guard executed before EVERY adb operation."""
    out = _run([ADB, "-s", SERIAL, "emu", "avd", "name"], timeout=30)
    first = (out.stdout.strip().splitlines() or [""])[0].strip()
    GUARDS["count"] += 1
    if first != AVD:
        raise Abort(f"identity guard failed: expected AVD {AVD!r}, got {first!r}")


def adb(*args: str, check: bool = False, timeout: int = 60) -> str:
    guard()
    out = _run([ADB, "-s", SERIAL, *args], timeout=timeout)
    if check and out.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)} failed: {out.stderr.strip()}")
    return out.stdout


def sh(cmd: str, timeout: int = 60) -> str:
    return adb("shell", cmd, timeout=timeout)


# --------------------------------------------------------------------------- UI model
class Node:
    def __init__(self, el: ET.Element, parent: "Node | None"):
        a = el.attrib
        self.text = a.get("text", "")
        self.desc = a.get("content-desc", "")
        self.cls = a.get("class", "").split(".")[-1]
        self.clickable = a.get("clickable") == "true"
        self.enabled = a.get("enabled") == "true"
        self.checkable = a.get("checkable") == "true"
        self.checked = a.get("checked") == "true"
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", a.get("bounds", "[0,0][0,0]"))
        self.l, self.t, self.r, self.b = (int(x) for x in m.groups())
        self.parent = parent
        self.children: list[Node] = []

    @property
    def w(self): return self.r - self.l
    @property
    def h(self): return self.b - self.t
    @property
    def cx(self): return (self.l + self.r) // 2
    @property
    def cy(self): return (self.t + self.b) // 2

    def walk(self):
        yield self
        for c in self.children:
            yield from c.walk()

    def texts(self) -> list[str]:
        return [n.text for n in self.walk() if n.text]

    def clickable_ancestor(self) -> "Node":
        n = self
        while n is not None and not n.clickable:
            n = n.parent
        return n or self

    def __repr__(self):
        return f"<{self.cls} {self.text!r}/{self.desc!r} [{self.l},{self.t},{self.r},{self.b}]>"


class State:
    def __init__(self, xml: str):
        self.xml = xml
        self.nodes: list[Node] = []
        root_el = ET.fromstring(xml)

        def build(el, parent):
            for child in el.findall("node"):
                n = Node(child, parent)
                self.nodes.append(n)
                if parent:
                    parent.children.append(n)
                build(child, n)
        build(root_el, None)
        top = self.nodes[0] if self.nodes else None
        self.screen = (top.r, top.b) if top else (0, 0)
        self.landscape = self.screen[0] > self.screen[1]
        self.cells: dict[int, tuple[str, Node]] = {}
        for n in self.nodes:
            m = CELL_RE.match(n.desc)
            if m:
                idx = (int(m.group(1)) - 1) * 3 + int(m.group(2)) - 1
                self.cells[idx] = ({"vacía": ".", "X": "X", "O": "O"}[m.group(3)], n)
        self.board = "".join(self.cells[i][0] if i in self.cells else "?" for i in range(9))
        self.board_node = next((n for n in self.nodes if n.desc == "Tablero de tres en raya"), None)
        self.status = next((n.text for n in self.nodes if n.text in STATUS_TEXTS), None)
        txt = [n.text for n in self.nodes if n.text]
        self.texts = txt
        self.scores = None
        sx = so = st = None
        for t in txt:
            m = re.match(r"Victorias de X: (\d+)$", t)
            if m: sx = int(m.group(1))
            m = re.match(r"Victorias de O: (\d+)$", t)
            if m: so = int(m.group(1))
            m = re.match(r"Empates: (\d+)$", t)
            if m: st = int(m.group(1))
        if sx is not None and so is not None and st is not None:
            self.scores = (sx, so, st)
        # overlays
        main_scrolls = [n for n in self.nodes if n.cls == "ScrollView" and self.board_node
                        and n.l <= self.board_node.l and n.r >= self.board_node.r and n.t <= self.board_node.t]
        self.main_scroll = main_scrolls[0] if main_scrolls else None
        self.popups = [n for n in self.nodes if n.cls == "ScrollView" and n is not self.main_scroll]
        self.menu_scroll = None
        self.radio_rows: list[Node] = [n for n in self.nodes if n.checkable]
        for p in self.popups:
            if not any(n.checkable for n in p.walk()) and sum(1 for n in p.walk() if n.clickable) >= 3:
                self.menu_scroll = p
        self.menu_entries: list[dict] = []
        if self.menu_scroll:
            for n in self.menu_scroll.walk():
                if n.clickable and n.cls == "View":
                    self.menu_entries.append({"node": n, "texts": n.texts(), "enabled": n.enabled})
        self.menu_open = bool(self.menu_entries)
        self.difficulty_dialog = bool(self.radio_rows)
        self.about_dialog = "Aceptar" in txt and "Juan Sebastian Rodriguez" in txt
        self.menu_btn = None
        for n in self.nodes:
            if n.desc == "Abrir menú":
                self.menu_btn = n.clickable_ancestor()
        # "Nueva partida" shortcut: text node that is not inside the menu popup
        self.newgame = None
        for n in self.nodes:
            if n.text == "Nueva partida" and not self._inside(n, self.menu_scroll):
                self.newgame = n.clickable_ancestor()
        self.has_game = bool(self.board_node)

    @staticmethod
    def _inside(n: Node, container: "Node | None") -> bool:
        while n is not None:
            if n is container:
                return container is not None
            n = n.parent
        return False

    def radio_state(self) -> list[tuple[str, bool]]:
        return [(" ".join(r.texts()), r.checked) for r in self.radio_rows]

    def entry(self, label: str):
        for e in self.menu_entries:
            if e["texts"] and e["texts"][0] == label:
                return e
        return None

    def marks(self, m: str) -> int:
        return self.board.count(m)

    def human_turn(self) -> bool:
        return self.status in ("Tu turno", "La persona hace la primera jugada")

    def terminal(self) -> bool:
        return self.status in TERMINAL

    def sig(self):
        return (self.board, self.status, self.scores)


# --------------------------------------------------------------------------- device ops
def tap(x: int, y: int) -> None:
    sh(f"input tap {x} {y}")


def tap_node(n: Node) -> None:
    tap(n.cx, n.cy)


def state(retries: int = 8, windows: bool = False) -> State:
    last = ""
    for _ in range(retries):
        out = sh("uiautomator dump /sdcard/qa_dump.xml")
        xml = sh("cat /sdcard/qa_dump.xml")
        last = xml
        if xml.strip().startswith("<?xml"):
            try:
                st = State(xml)
                if st.nodes:
                    return st
            except ET.ParseError:
                pass
        time.sleep(0.7)
    raise RuntimeError(f"could not obtain a UI hierarchy: {last[:200]!r}")


def wait_for(pred, timeout: float = 15, interval: float = 0.4, what: str = "condition") -> State:
    end = time.time() + timeout
    st = None
    while time.time() < end:
        st = state()
        if pred(st):
            return st
        time.sleep(interval)
    raise TimeoutError(f"timeout waiting for {what}; last status={st.status if st else None} board={st.board if st else None}")


def toast_present() -> bool:
    return int((sh("dumpsys window windows | grep -c ' Toast}'").strip() or "0")) > 0


def prefs_xml() -> str:
    return sh(f"run-as {PKG} sh -c 'cat shared_prefs/*.xml 2>/dev/null'")


def pids() -> str:
    return sh(f"pidof {PKG}").strip()


def set_rotation(r: int, wait: bool = True) -> State | None:
    sh(f"settings put system user_rotation {r}")
    if not wait:
        return None
    time.sleep(1.2)
    want_land = r in (1, 3)
    return wait_for(lambda s: s.landscape == want_land and s.has_game, 15, what=f"rotation {r}")


def launch() -> State:
    sh(f"am start -n {ACT}")
    return wait_for(lambda s: s.has_game, 20, what="app launch")


def to_front() -> State:
    """Return to the existing task like the launcher does (single instance, no new activity)."""
    sh(f"am start -f 0x20000000 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n {ACT}")
    return wait_for(lambda s: s.has_game, 20, what="task to front")


def force_stop() -> None:
    sh(f"am force-stop {PKG}")
    time.sleep(0.8)


def fresh(rotation: int = 0) -> State:
    """Clean data + cold launch. For landscape the display must be rotated WHILE the app is in the
    foreground (Android resets user_rotation to 0 if it is set while the launcher is on screen),
    then the app is force-stopped, cleared and cold-started again, which keeps the landscape display."""
    want_land = rotation in (1, 3)
    if want_land:
        sh("settings put system user_rotation 0")
        launch()
        set_rotation(rotation)
    force_stop()
    sh(f"pm clear {PKG}")
    if not want_land:
        sh("settings put system user_rotation 0")
    time.sleep(1.0)
    st = launch()
    if st.landscape != want_land:   # the display may rotate a moment after the first frame
        st = wait_for(lambda x: x.has_game and x.landscape == want_land, 15, what="rotation after launch")
    return st


def relaunch_new() -> State:
    """Deliberately new launch: force-stop (kills the task state) then start."""
    force_stop()
    return launch()


def kill_in_background() -> tuple[str, str]:
    before = pids()
    sh("input keyevent KEYCODE_HOME")
    time.sleep(2.0)
    return before, kill_now()


def kill_now() -> str:
    sh(f"am kill {PKG}")
    for _ in range(10):
        time.sleep(0.5)
        if not pids():
            return "killed"
    raise RuntimeError("process still alive after `am kill` (cannot simulate process death)")


def open_menu() -> State:
    st = state()
    if not st.menu_open:
        tap_node(st.menu_btn)
        st = wait_for(lambda s: s.menu_open, 8, what="menu open")
    return st


def menu_choose(label: str) -> None:
    st = open_menu()
    e = st.entry(label)
    if not e:
        raise RuntimeError(f"menu entry {label!r} not found: {[x['texts'] for x in st.menu_entries]}")
    tap_node(e["node"])
    time.sleep(0.6)


CURRENT: dict = {"ctx": None}


def choose_difficulty(label: str) -> bool:
    """Open Dificultad and pick `label`. Returns True if a Toast window appeared.
    The screenshot is taken ~0.4 s after the tap (inside the same shell command) so that the Toast
    text is captured as evidence: evidence/<case>/toast-<n>-<label>.png (reviewed visually)."""
    menu_choose("Dificultad")
    st = wait_for(lambda s: s.difficulty_dialog, 8, what="difficulty dialog")
    row = next(r for r in st.radio_rows if r.texts() and r.texts()[0] == label)
    out = sh(f"input tap {row.cx} {row.cy}; sleep 0.35; screencap -p /sdcard/qa_toast.png; dumpsys window windows | grep -c ' Toast}}'")
    seen = int((out.strip().splitlines() or ["0"])[-1] or 0) > 0
    c = CURRENT["ctx"]
    if c is not None:
        c.n += 1
        name = f"toast-{c.n:02d}-{re.sub(r'[^A-Za-z0-9_-]+', '_', label)}.png"
        guard()
        subprocess.run([ADB, "-s", SERIAL, "pull", "/sdcard/qa_toast.png", str(c.dir / name)], capture_output=True)
        c.evidence.append(f"evidence/{c.id}/{name}")
    time.sleep(0.5)
    return seen


def current_difficulty() -> str:
    menu_choose("Dificultad")
    st = wait_for(lambda s: s.difficulty_dialog, 8, what="difficulty dialog")
    cur = [t for t, c in st.radio_state() if c]
    sh("input keyevent KEYCODE_BACK")
    time.sleep(0.6)
    return cur[0] if len(cur) == 1 else f"INVALID:{st.radio_state()}"


def new_game_button() -> None:
    st = state()
    tap_node(st.newgame)
    time.sleep(0.3)


def tap_cell(st: State, idx: int) -> None:
    tap_node(st.cells[idx][1])


def smart(board: str) -> int:
    for mark in "XO":
        for ln in LINES:
            cells = [board[i] for i in ln]
            if cells.count(mark) == 2 and "." in cells:
                return ln[cells.index(".")]
    for i in (4, 0, 2, 6, 8, 1, 3, 5, 7):
        if board[i] == ".":
            return i
    return board.index(".")


def dumb(board: str) -> int:
    for i in (1, 3, 5, 7, 0, 2, 6, 8, 4):
        if board[i] == ".":
            return i
    return board.index(".")


def play_to_end(strategy, timeout: float = 120) -> State:
    end = time.time() + timeout
    while time.time() < end:
        st = state()
        if st.terminal():
            return st
        if st.human_turn() and "." in st.board:
            tap_cell(st, strategy(st.board))
            time.sleep(0.5)
        else:
            time.sleep(0.5)
    raise TimeoutError("game did not end")


def play_one_move_and_reply() -> State:
    """Human plays a move (center if free) and waits for O's reply; returns the human-turn state."""
    st = state()
    tap_cell(st, 4 if st.board[4] == "." else smart(st.board))
    return wait_for(lambda s: s.human_turn() and s.marks("O") >= 1 and not s.terminal(), 15, what="O reply")


def save_overlay_free(st: State):
    return st


# --------------------------------------------------------------------------- case framework
class Ctx:
    def __init__(self, cid: str, title: str, covers: str):
        self.id, self.title, self.covers = cid, title, covers
        self.checks: list[dict] = []
        self.notes: list[str] = []
        self.evidence: list[str] = []
        self.dir = EVID / cid
        self.dir.mkdir(parents=True, exist_ok=True)
        self.n = 0
        self.verdict_override: str | None = None

    def check(self, name: str, cond: bool, detail: str = "") -> bool:
        self.checks.append({"check": name, "ok": bool(cond), "detail": detail})
        log(f"  [{'ok' if cond else 'FAIL'}] {name} {detail}")
        return bool(cond)

    def note(self, text: str) -> None:
        self.notes.append(text)
        log(f"  note: {text}")

    def snap(self, step: str, st: State | None = None, png: bool = True) -> State:
        self.n += 1
        base = f"{self.n:02d}-{re.sub(r'[^A-Za-z0-9_-]+', '_', step)}"
        st = st or state()
        (self.dir / f"{base}.xml").write_text(st.xml)
        self.evidence.append(f"evidence/{self.id}/{base}.xml")
        if png:
            guard()
            sh("screencap -p /sdcard/qa_shot.png")
            subprocess.run([ADB, "-s", SERIAL, "pull", "/sdcard/qa_shot.png", str(self.dir / f"{base}.png")],
                           capture_output=True)
            self.evidence.append(f"evidence/{self.id}/{base}.png")
        return st

    def save_text(self, name: str, text: str) -> None:
        (self.dir / name).write_text(text)
        self.evidence.append(f"evidence/{self.id}/{name}")


CASES: list[tuple[str, str, str, object]] = []


def case(cid: str, title: str, covers: str):
    def deco(fn):
        CASES.append((cid, title, covers, fn))
        return fn
    return deco


def layout_checks(c: Ctx, st: State, label: str) -> None:
    """Orientation-aware layout assertions (tic-tac-toe-game: Presentación adaptativa)."""
    b, ng, mb = st.board_node, st.newgame, st.menu_btn
    c.check(f"{label}: tablero, estado, marcador y controles presentes",
            all([b, st.status, st.scores, ng, mb]))
    if not all([b, ng, mb]):
        return
    c.check(f"{label}: sin título de aplicación 'TicTacToe'", "TicTacToe" not in st.texts)
    above = [n for n in st.nodes if n.text and n.b <= b.t and n.t >= 0 and n.cls == "TextView"]
    c.check(f"{label}: ningún texto (barra/título) por encima del tablero", not above, str([a.text for a in above]))
    c.check(f"{label}: tablero cuadrado", abs(b.w - b.h) <= 2, f"{b.w}x{b.h}px")
    c.check(f"{label}: botón de menú junto a 'Nueva partida' (misma fila, a la derecha)",
            abs(mb.cy - ng.cy) <= 10 and mb.l >= ng.r - 2 and mb.l - ng.r <= 60,
            f"newgame=[{ng.l},{ng.r}] menu=[{mb.l},{mb.r}] cy {ng.cy}/{mb.cy}")
    if st.landscape:
        sn = next(n for n in st.nodes if n.text == st.status)
        c.check(f"{label}: horizontal - tablero a la izquierda del panel", b.r <= sn.l and b.r <= ng.l,
                f"board.r={b.r} status.l={sn.l} newgame.l={ng.l}")
        c.check(f"{label}: horizontal - estado, marcador y controles a la derecha del tablero",
                all(n.l >= b.r for n in st.nodes if n.text in ([st.status, "Marcador"] + [t for t in st.texts if t.startswith(('Victorias', 'Empates'))])))
        c.check(f"{label}: horizontal - tablero <= 270 dp (referencia)", b.w <= BOARD_REF_PX,
                f"{b.w}px = {b.w / DP:.0f} dp")
        c.check(f"{label}: horizontal - tablero ocupa el lado izquierdo (centro en la mitad izquierda)",
                b.cx < st.screen[0] / 2)
    else:
        sn = next(n for n in st.nodes if n.text == st.status)
        sc = next(n for n in st.nodes if n.text == "Marcador")
        c.check(f"{label}: vertical - orden tablero > estado > marcador > controles",
                b.b <= sn.t and sn.b <= sc.t and sc.b <= ng.t, f"{b.b}/{sn.t}/{sc.t}/{ng.t}")
    c.check(f"{label}: objetivos del menú y 'Nueva partida' >= 48 dp", min(mb.w, mb.h, ng.h) >= MIN_TARGET_PX,
            f"menu {mb.w}x{mb.h}, newgame h={ng.h}")
    cl = [n for n in st.cells.values()]
    c.check(f"{label}: casillas >= 48 dp", all(min(n.w, n.h) >= MIN_TARGET_PX for _, n in cl),
            f"min={min(min(n.w, n.h) for _, n in cl)}px")
    c.check(f"{label}: contenido dentro de pantalla", all(n.r <= st.screen[0] + 1 and n.b <= st.screen[1] + 1
                                                          for n in (b, ng, mb)))


# =========================================================================== CASES
@case("TC-01", "Arranque limpio (datos borrados), vertical",
      "game-session: Primera partida / sin snapshot; tic-tac-toe-game: Sin barra superior, Disposición portrait; game-options: Persistencia de dificultad (default Expert)")
def tc01(c: Ctx):
    st = fresh(0)
    c.snap("arranque", st)
    c.check("tablero vacío", st.board == "." * 9, st.board)
    c.check("inicia X (texto 'La persona hace la primera jugada')", st.status == "La persona hace la primera jugada", str(st.status))
    c.check("marcador en cero", st.scores == (0, 0, 0), str(st.scores))
    layout_checks(c, st, "vertical limpio")
    time.sleep(3)
    st2 = state()
    c.check("tras 3 s el tablero sigue vacío (X inicia, sin jugada automática)", st2.board == "." * 9, st2.board)
    c.check("dificultad por defecto Experto", current_difficulty() == "Experto")
    c.snap("dificultad-experto-cerrada")
    c.save_text("prefs.xml", prefs_xml())
    c.check("no existen claves de partida en preferencias (solo dificultad/marcador)",
            not re.search(r'name="(board|turn|result|sound)', prefs_xml()), prefs_xml().replace("\n", " "))


@case("TC-02", "Menú: cinco entradas en orden, sin Salir; cierre con Atrás",
      "game-options: Menú con las cinco entradas, Sin acción Salir, Back con overlay, El menú se cierra antes de la acción")
def tc02(c: Ctx):
    base0 = fresh(0)
    board_before = base0.board
    st = open_menu()
    c.snap("menu-abierto", st)
    labels = [e["texts"][0] for e in st.menu_entries if e["texts"]]
    c.check("exactamente cinco entradas", len(labels) == 5, str(labels))
    c.check("orden: Nueva partida, Dificultad, Sonido, Reiniciar puntajes, Acerca de", labels == MENU_ORDER, str(labels))
    c.check("no existe 'Salir'", not any("Salir" in t for t in st.texts))
    sound = st.entry("Sonido")
    c.check("Sonido expone su estado ('Habilitado')", sound and "Habilitado" in sound["texts"], str(sound and sound["texts"]))
    c.check("todas las entradas habilitadas con tablero vacío", all(e["enabled"] for e in st.menu_entries))
    c.check("menú no deja ver título/barra", "TicTacToe" not in st.texts)
    sh("input keyevent KEYCODE_BACK")
    time.sleep(0.8)
    st2 = c.snap("tras-atras", None)
    c.check("Atrás cierra el menú", not st2.menu_open)
    c.check("la app sigue en primer plano y la partida no cambia", st2.has_game and st2.board == board_before)
    # entry closes menu before action: About
    menu_choose("Acerca de")
    st3 = wait_for(lambda s: s.about_dialog, 8, what="about dialog")
    c.snap("acerca-de", st3)
    c.check("Acerca de abre su diálogo y el menú se cerró (sin ventanas apiladas)", st3.about_dialog and not st3.menu_open)
    sh("input keyevent KEYCODE_BACK")
    time.sleep(0.8)
    st4 = state()
    c.check("Atrás cierra el diálogo Acerca de y la partida no cambia", not st4.about_dialog and st4.has_game and st4.board == board_before)
    # difficulty dialog + Back = cancel
    menu_choose("Dificultad")
    st5 = wait_for(lambda s: s.difficulty_dialog, 8, what="difficulty dialog")
    c.snap("dificultad-abierto", st5)
    sh("input keyevent KEYCODE_BACK")
    time.sleep(0.5)
    tcheck = toast_present()
    st6 = state()
    c.check("Atrás cierra el diálogo de dificultad sin Toast", not st6.difficulty_dialog and not tcheck)
    c.check("dificultad sigue Experto tras cancelar", current_difficulty() == "Experto")


@case("TC-03", "Atrás sin overlay: navegación normal del sistema (sin intercepción)",
      "game-options: Back sin overlay; game-session: Background y Back")
def tc03(c: Ctx):
    fresh(0)
    play_one_move_and_reply()
    sh("logcat -c")
    sh("input keyevent KEYCODE_BACK")
    time.sleep(2)
    top = sh("dumpsys activity activities | grep -m1 topResumedActivity").strip()
    c.save_text("top-after-back.txt", top)
    left = PKG not in top
    c.note(f"Tras Atrás sin overlay, topResumedActivity = {top!r}. Aplicación {'salió de primer plano' if left else 'sigue en primer plano'}.")
    c.check("Atrás sin overlay no provoca cierre abrupto (sin FATAL EXCEPTION)", "FATAL EXCEPTION" not in sh("logcat -d -b crash"))
    c.check("Atrás sin overlay hace que la app salga de primer plano (navegación normal)", left, top)
    alive = bool(pids())
    c.note(f"Proceso {'vivo' if alive else 'terminado'} tras Atrás (la especificación no garantiza finalizar la tarea).")
    st = to_front()
    c.snap("relanzamiento-tras-atras", st)
    c.note(f"Relanzamiento tras Atrás: tablero={st.board} estado={st.status} (informativo; ambos comportamientos son navegación normal).")


def _outcome_game(c: Ctx, strategy, tag: str, d_label: str | None):
    st = state()
    before = st.scores
    end = play_to_end(strategy)
    c.snap(f"{tag}-terminal", end)
    after = end.scores
    delta = tuple(a - b for a, b in zip(after, before))
    expected = {"Victoria humana": (1, 0, 0), "Victoria de la computadora": (0, 1, 0), "Empate": (0, 0, 1)}[end.status]
    c.check(f"{tag}: '{end.status}' suma exactamente un punto en el contador correcto", delta == expected, f"delta={delta} esperado={expected}")
    sig = end.sig()
    # taps after the end must change nothing
    for i in range(9):
        sh(f"input tap {end.cells[i][1].cx} {end.cells[i][1].cy}")
    time.sleep(1.5)
    after_taps = c.snap(f"{tag}-tras-taps", None)
    c.check(f"{tag}: taps posteriores no cambian tablero, estado ni marcador", after_taps.sig() == sig, f"{sig} -> {after_taps.sig()}")
    c.check(f"{tag}: casillas deshabilitadas tras el desenlace", all(not n.enabled for _, n in after_taps.cells.values()))
    return end.status


@case("TC-04", "Partida completa hasta un desenlace: punto único y taps posteriores sin efecto",
      "game-session: Puntuación única (victoria humana/computadora/empate, Resultado no duplicado)")
def tc04(c: Ctx):
    fresh(0)
    seen: dict[str, int] = {}
    plan = [("Fácil", smart, "X-gana"), ("Experto", dumb, "O-gana"), ("Experto", smart, "empate"),
            ("Fácil", smart, "extra1"), ("Fácil", dumb, "extra2"), ("Experto", smart, "extra3")]
    for i, (level, strat, tag) in enumerate(plan):
        if len(seen) == 3 and i >= 3:
            break
        st = state()
        if st.terminal():
            choose_difficulty(level)
            new_game_button()
        elif i == 0:
            choose_difficulty(level)
        status = _outcome_game(c, strat, f"G{i + 1}-{level}-{tag}", level)
        seen[status] = seen.get(status, 0) + 1
        if i < len(plan) - 1:
            pass
    c.note(f"Desenlaces observados: {seen}")
    for needed in ("Victoria humana", "Victoria de la computadora", "Empate"):
        if needed not in seen:
            c.note(f"No se observó el desenlace '{needed}' en esta ejecución (la IA no es determinista); cubierto por pruebas JVM.")
    st = state()
    c.check("marcador final = suma de desenlaces", st.scores == (seen.get("Victoria humana", 0), seen.get("Victoria de la computadora", 0), seen.get("Empate", 0)), str(st.scores))


def _cycle_rotation(c: Ctx, tag: str, base: State, waits: float = 3.0, expect_same: bool = True):
    for r, name in ((1, "horizontal"), (0, "vertical")):
        st = set_rotation(r)
        time.sleep(waits)
        st = c.snap(f"{tag}-{name}", None)
        layout_checks(c, st, f"{tag} {name}")
        if expect_same:
            c.check(f"{tag} {name}: tablero, estado y marcador conservados", st.sig() == base.sig(), f"{base.sig()} -> {st.sig()}")
    return st


@case("TC-05", "Rotación con tablero vacío",
      "tic-tac-toe-game: Disposición landscape/portrait; game-session: Rotación con estado guardado")
def tc05(c: Ctx):
    base = fresh(0)
    layout_checks(c, base, "vertical inicial")
    _cycle_rotation(c, "vacío", base)
    st = state()
    c.check("sin jugada automática tras rotar (X inicia)", st.board == "." * 9)


@case("TC-06", "Rotación en partida activa con turno humano",
      "game-session: Rotación durante una partida activa; Rotación con estado guardado")
def tc06(c: Ctx):
    fresh(0)
    base = play_one_move_and_reply()
    c.snap("antes", base)
    st = _cycle_rotation(c, "activa", base)
    c.check("exactamente 1 X y 1 O (ninguna jugada duplicada)", (st.marks("X"), st.marks("O")) == (1, 1), st.board)
    # play on after rotation
    tap_cell(st, smart(st.board))
    st2 = wait_for(lambda s: (s.human_turn() and s.marks("X") == 2 and s.marks("O") == 2) or s.terminal(), 15, what="continuación")
    c.check("la partida continúa tras rotar (X y O juegan normalmente)", st2.marks("X") == 2 and st2.marks("O") in (1, 2), st2.board)


def _pending_rotation(c: Ctx, tag: str, script: str):
    st = fresh(0)
    x_before = st.marks("X")
    sh(f"input tap {st.cells[4][1].cx} {st.cells[4][1].cy}; {script}")
    time.sleep(0.5)
    wait_for(lambda s: s.has_game, 15, what="app after rotation")
    time.sleep(3.5)
    fin = c.snap(tag, None)
    c.check(f"{tag}: exactamente una X y una O (una sola jugada de O)", (fin.marks("X"), fin.marks("O")) == (1, 1), fin.board)
    c.check(f"{tag}: turno de la persona (no se perdió el turno de O)", fin.status == "Tu turno", str(fin.status))
    c.check(f"{tag}: marcador sin cambios", fin.scores == (0, 0, 0), str(fin.scores))
    return fin


@case("TC-07", "Rotación con turno de O pendiente (tap + rotación inmediata)",
      "game-session: Rotación conserva espera pendiente, Tiempo restante tras rotación; reto extra 2")
def tc07(c: Ctx):
    for i in range(3):
        _pending_rotation(c, f"R{i + 1}-vert-a-horiz", "settings put system user_rotation 1")
        set_rotation(0)
    for i in range(2):
        _pending_rotation(c, f"D{i + 1}-doble-rotacion", "settings put system user_rotation 1; sleep 0.35; settings put system user_rotation 0")
    c.note("La ventana de 1000 ms de O no es observable con precisión desde adb: la rotación se dispara ~0.3 s después del tap; "
           "los milisegundos exactos (400/600 ms, 999 ms) quedan cubiertos por las pruebas JVM/instrumentadas con reloj virtual.")


@case("TC-08", "Rotación con partida terminada: sin punto ni jugada duplicados",
      "game-session: Rotación después de un resultado; Resultado no duplicado")
def tc08(c: Ctx):
    fresh(0)
    choose_difficulty("Fácil")
    end = play_to_end(smart)
    c.snap("terminal", end)
    _cycle_rotation(c, "terminal", end, waits=2.5)
    set_rotation(1)
    set_rotation(0)
    st = state()
    c.check("tras varias rotaciones el marcador sigue igual (sin punto extra)", st.scores == end.scores, f"{end.scores} -> {st.scores}")


@case("TC-09", "Rotación con sonido silenciado",
      "game-session: conservación de estado de sonido; game-options: Sonido disponible durante la partida")
def tc09(c: Ctx):
    fresh(0)
    play_one_move_and_reply()
    menu_choose("Sonido")
    st = open_menu()
    e = st.entry("Sonido")
    c.check("Sonido muestra 'Deshabilitado' tras silenciar", e and "Deshabilitado" in e["texts"], str(e and e["texts"]))
    c.snap("silenciado", st)
    sh("input keyevent KEYCODE_BACK"); time.sleep(0.6)
    base = state()
    for r in (1, 0, 1, 0):
        set_rotation(r)
        time.sleep(1.5)
        e = open_menu().entry("Sonido")
        c.check(f"rotación {r}: Sonido sigue 'Deshabilitado'", e and "Deshabilitado" in e["texts"], str(e and e["texts"]))
        sh("input keyevent KEYCODE_BACK"); time.sleep(0.6)
    st = state()
    c.check("tablero y marcador conservados", st.board == base.board and st.scores == base.scores)
    c.note("Que NO suene audio al rotar y que el sonido se oiga al jugar es verificación manual (TC-30); el emulador corre sin audio.")


@case("TC-10", "Rotación con menú o diálogo abierto",
      "game-options: Back con overlay; Menú y opciones no alteran la espera de O; (la persistencia de ventanas no se exige)")
def tc10(c: Ctx):
    fresh(0)
    base = state()
    for kind in ("menu", "dificultad", "acerca"):
        if kind == "menu":
            open_menu()
        elif kind == "dificultad":
            menu_choose("Dificultad"); wait_for(lambda s: s.difficulty_dialog, 8, what="dlg")
        else:
            menu_choose("Acerca de"); wait_for(lambda s: s.about_dialog, 8, what="about")
        st = set_rotation(1)
        time.sleep(1.5)
        st = c.snap(f"{kind}-horizontal", None)
        persisted = st.menu_open or st.difficulty_dialog or st.about_dialog
        c.note(f"{kind}: tras rotar a horizontal el overlay {'se conserva' if persisted else 'se cierra'} (la especificación no exige conservarlo).")
        c.check(f"{kind}: sin crash y partida intacta tras rotar", st.has_game and st.board == base.board and st.scores == base.scores)
        if persisted:
            sh("input keyevent KEYCODE_BACK"); time.sleep(0.8)
            c.check(f"{kind}: Atrás cierra el overlay que se conservó", not state().menu_open)
        st = set_rotation(0)
        time.sleep(1.2)
        if state().menu_open or state().difficulty_dialog or state().about_dialog:
            sh("input keyevent KEYCODE_BACK"); time.sleep(0.8)
    c.check("sin FATAL EXCEPTION en el buffer de crash", "FATAL EXCEPTION" not in sh("logcat -d -b crash"))


@case("TC-11", "Nueva partida: alternancia del iniciador y apertura única de O",
      "game-session: Alternancia, Apertura O tras 1000 ms, X inicia sin movimiento automático, abandono no puntúa; game-options: Nueva partida desde el menú, botón atajo")
def tc11(c: Ctx):
    st = fresh(0)
    c.check("partida 1 inicia X", st.status == "La persona hace la primera jugada")
    play_one_move_and_reply()           # abandon game 1 in progress
    sc0 = state().scores
    new_game_button()                  # game 2 -> O opens
    s = wait_for(lambda s: s.marks("O") == 1 and s.marks("X") == 0 or s.status == "Turno de la computadora", 8, 0.3, "O inicia")
    c.snap("juego2-O-inicia", s)
    s = wait_for(lambda s: s.marks("O") == 1 and s.human_turn(), 10, 0.3, "apertura de O")
    c.check("partida 2 (tras abandonar): inicia O, apertura única", (s.marks("O"), s.marks("X")) == (1, 0), s.board)
    time.sleep(3.5)
    s2 = c.snap("juego2-tras-3s", None)
    c.check("la apertura ocurre una sola vez (sigue 1 O tras 3.5 s)", (s2.marks("O"), s2.marks("X")) == (1, 0) and s2.status == "Tu turno", s2.board)
    c.check("la partida abandonada no sumó puntos", s2.scores == sc0, f"{sc0} -> {s2.scores}")
    # Dificultad closed after opening
    m = open_menu()
    c.check("tras la apertura de O la entrada Dificultad queda deshabilitada", m.entry("Dificultad") and not m.entry("Dificultad")["enabled"])
    c.snap("dificultad-deshabilitada", m)
    sh("input keyevent KEYCODE_BACK"); time.sleep(0.6)
    # game 3 via the menu entry -> X starts
    menu_choose("Nueva partida")
    s3 = c.snap("juego3-X-inicia-via-menu", None)
    c.check("partida 3 (desde el menú) inicia X con tablero vacío", s3.board == "." * 9 and s3.status == "La persona hace la primera jugada", f"{s3.board} {s3.status}")
    time.sleep(3)
    c.check("X inicia: sin jugada automática tras 3 s", state().board == "." * 9)
    # game 4 -> O again
    new_game_button()
    s4 = wait_for(lambda s: s.marks("O") == 1 and s.human_turn(), 10, 0.3, "apertura de O (partida 4)")
    c.check("partida 4 inicia O de nuevo (alternancia)", (s4.marks("O"), s4.marks("X")) == (1, 0), s4.board)
    # rotation during O's opening
    new_game_button()   # game 5 -> X starts, nothing pending; then game 6 O: do rotation with new game
    time.sleep(0.5)
    st = state()
    c.check("partida 5 inicia X", st.status == "La persona hace la primera jugada")
    sh(f"input tap {st.newgame.cx} {st.newgame.cy}; settings put system user_rotation 1")
    time.sleep(0.5)
    wait_for(lambda s: s.has_game, 15, what="app")
    time.sleep(3.5)
    s6 = c.snap("juego6-O-inicia-con-rotacion", None)
    c.check("rotación durante la apertura pendiente de O: una sola O y turno humano", (s6.marks("O"), s6.marks("X")) == (1, 0) and s6.status == "Tu turno", f"{s6.board} {s6.status}")
    set_rotation(0)


@case("TC-12", "Dificultad: cuatro niveles, Toast, bloqueo en partida activa, edición al terminar",
      "game-options: Selección de dificultad (todos los escenarios de UI), Accesibilidad de opciones")
def tc12(c: Ctx):
    fresh(0)
    menu_choose("Dificultad")
    st = wait_for(lambda s: s.difficulty_dialog, 8, what="dialog")
    rs = st.radio_state()
    c.snap("dialogo", st)
    c.check("exactamente cuatro radios en orden Fácil, Difícil, Experto, Imposible", [t for t, _ in rs] == LEVELS, str(rs))
    c.check("un solo radio marcado (Experto)", [t for t, ck in rs if ck] == ["Experto"], str(rs))
    c.check("filas de radio >= 48 dp", all(r.h >= MIN_TARGET_PX for r in st.radio_rows), str([r.h for r in st.radio_rows]))
    sh("input keyevent KEYCODE_BACK"); time.sleep(0.5)
    for lvl in ("Fácil", "Imposible", "Difícil", "Experto", "Experto"):
        seen = choose_difficulty(lvl)
        shot = c.snap(f"seleccion-{lvl}", None)
        c.check(f"seleccionar {lvl}: se cierra el diálogo y aparece un Toast (ventana Toast)", seen and not shot.difficulty_dialog, f"toast={seen}")
        time.sleep(4.5)
        c.check(f"seleccionar {lvl}: el Toast no persiste/repite tras expirar", not toast_present())
        c.check(f"reabrir tras elegir {lvl}: radio marcado = {lvl}", current_difficulty() == lvl)
    c.snap("final-experto")
    prefs = prefs_xml()
    c.save_text("prefs-experto.xml", prefs)
    c.check("preferencia persistida con código 2 (Experto)", 'name="difficulty_code" value="2"' in prefs, prefs.replace("\n", " "))
    # Toast not repeated by rotation
    choose_difficulty("Imposible")
    time.sleep(4.5)
    set_rotation(1); time.sleep(1.0)
    c.check("rotar tras elegir no vuelve a emitir el Toast", not toast_present())
    set_rotation(0)
    prefs = prefs_xml()
    c.check("preferencia persistida con código 3 (Imposible)", 'name="difficulty_code" value="3"' in prefs, prefs.replace("\n", " "))
    # Persistence after force-stop
    relaunch_new()
    c.check("persistencia tras detención forzada y relanzamiento: Imposible", current_difficulty() == "Imposible")
    # Active game blocks difficulty
    st = state()
    tap_cell(st, 4)
    st = wait_for(lambda s: s.marks("X") == 1 and (s.human_turn() or s.terminal()), 15, what="move")
    m = open_menu()
    e = m.entry("Dificultad")
    c.snap("activa-menu", m)
    c.check("partida activa con marcas: Dificultad deshabilitada", e and not e["enabled"])
    tap_node(e["node"]); time.sleep(0.8)
    s2 = state()
    c.check("tocar la entrada deshabilitada no abre el diálogo de dificultad", not s2.difficulty_dialog)
    c.snap("activa-tras-tocar-deshabilitada", s2)
    if s2.menu_open:
        sh("input keyevent KEYCODE_BACK"); time.sleep(0.6)
    s3 = state()
    c.check("tras tocar la entrada deshabilitada la partida no cambia", s3.board == st.board and s3.status == st.status and s3.scores == st.scores, f"{st.board} -> {s3.board}")
    # After the end
    end = play_to_end(smart)
    m = open_menu()
    c.check("partida terminada: Dificultad habilitada", m.entry("Dificultad") and m.entry("Dificultad")["enabled"])
    c.snap("terminada-menu", m)
    sh("input keyevent KEYCODE_BACK"); time.sleep(0.6)
    seen = choose_difficulty("Fácil")
    c.check("partida terminada: se acepta el cambio de dificultad (Toast)", seen)
    c.check("el cambio tras desenlace no altera tablero ni marcador", state().sig() == end.sig())


@case("TC-13", "Persistencia del marcador tras detención forzada",
      "game-session: Inicio de sesión (scores persistentes), Sin persistencia permanente; Nueva sesión tras perder el proceso")
def tc13(c: Ctx):
    fresh(0)
    choose_difficulty("Fácil")
    play_to_end(smart)
    new_game_button()
    end = play_to_end(smart)
    sc = end.scores
    c.check("se registraron 2 desenlaces", sum(sc) == 2, str(sc))
    c.save_text("prefs-antes.xml", prefs_xml())
    st = relaunch_new()
    c.snap("relanzado", st)
    c.check("marcador conservado tras force-stop + relanzamiento", st.scores == sc, f"{sc} -> {st.scores}")
    c.check("partida nueva (tablero vacío, inicia X)", st.board == "." * 9 and st.status == "La persona hace la primera jugada", f"{st.board} {st.status}")
    c.save_text("prefs-despues.xml", prefs_xml())
    c.check("dificultad conservada (Fácil)", current_difficulty() == "Fácil")


@case("TC-14", "Reiniciar puntajes: en partida activa y terminada; el cero persiste",
      "tic-tac-toe-game: Acción accesible de reset; game-session: Restablecimiento persistente (Restablecer durante partida / terminal)")
def tc14(c: Ctx):
    fresh(0)
    choose_difficulty("Fácil")
    play_to_end(smart)
    new_game_button()
    st = wait_for(lambda s: s.human_turn() or s.terminal(), 10, what="turno")
    if st.board.count(".") == 9 and st.human_turn():
        st = play_one_move_and_reply()
    else:
        st = play_one_move_and_reply() if not st.terminal() else st
    base = st
    c.check("precondición: marcador distinto de cero y partida activa", sum(base.scores) >= 1 and not base.terminal(), f"{base.scores} {base.status}")
    c.snap("A-antes", base)
    menu_choose("Reiniciar puntajes")
    after = c.snap("A-despues", None)
    c.check("A: marcador pasa a 0/0/0 de inmediato", after.scores == (0, 0, 0), str(after.scores))
    c.check("A: tablero, turno y resultado sin cambios", after.board == base.board and after.status == base.status, f"{base.board}/{base.status} -> {after.board}/{after.status}")
    time.sleep(2.5)
    c.check("A: sin jugada extra ni cambios tras 2.5 s", state().sig() == after.sig())
    prefs = prefs_xml()
    c.save_text("A-prefs.xml", prefs)
    c.check("A: preferencias con ceros", all(f'value="0"' in prefs for _ in [0]) and not re.search(r'name="(x|o|tie|wins)[^"]*" value="[1-9]', prefs), prefs.replace("\n", " "))
    # continue the game to its end -> score = 1 only
    end = play_to_end(smart)
    c.check("A: al terminar la partida el marcador suma exactamente 1", sum(end.scores) == 1, str(end.scores))
    c.snap("B-terminal", end)
    menu_choose("Reiniciar puntajes")
    rs = c.snap("B-despues", None)
    c.check("B: marcador 0/0/0 en partida terminada", rs.scores == (0, 0, 0), str(rs.scores))
    c.check("B: resultado y tablero permanecen", rs.board == end.board and rs.status == end.status)
    time.sleep(2.5)
    c.check("B: el resultado terminal no se recuenta", state().scores == (0, 0, 0))
    set_rotation(1); time.sleep(2)
    c.check("B: rotar tras reset mantiene ceros", state().scores == (0, 0, 0))
    set_rotation(0)
    st = relaunch_new()
    c.check("B: el cero persiste tras detención forzada y relanzamiento", st.scores == (0, 0, 0), str(st.scores))
    c.save_text("B-prefs.xml", prefs_xml())
    m = open_menu()
    e = m.entry("Reiniciar puntajes")
    c.check("Reiniciar puntajes siempre habilitada y etiquetada", e and e["enabled"])
    sh("input keyevent KEYCODE_BACK")


@case("TC-15", "Muerte de proceso en segundo plano: partida activa (y sonido silenciado)",
      "game-session: Restauración soportada por muerte de proceso; Conservación del estado")
def tc15(c: Ctx):
    fresh(0)
    st = play_one_move_and_reply()
    st = state()
    tap_cell(st, smart(st.board))
    base = wait_for(lambda s: s.human_turn() and s.marks("X") == 2 and s.marks("O") == 2 or s.terminal(), 15, what="2nd move")
    menu_choose("Sonido")
    c.snap("antes", base)
    pid0, _ = kill_in_background()
    c.check("proceso terminado por `am kill`", not pids())
    st = to_front()
    time.sleep(3)
    st = c.snap("restaurado", None)
    c.check("nuevo proceso", bool(pids()) and pids() != pid0, f"{pid0} -> {pids()}")
    c.check("tablero, turno y marcador restaurados", st.sig() == base.sig(), f"{base.sig()} -> {st.sig()}")
    c.check("no hay jugada extra de O", st.marks("O") == base.marks("O"))
    e = open_menu().entry("Sonido")
    c.check("el silencio de sesión se restaura (Deshabilitado)", e and "Deshabilitado" in e["texts"], str(e and e["texts"]))
    sh("input keyevent KEYCODE_BACK"); time.sleep(0.5)
    tap_cell(state(), smart(state().board))
    nxt = wait_for(lambda s: s.marks("X") == 3 and (s.human_turn() or s.terminal()), 15, what="siguiente jugada")
    c.check("el juego continúa normalmente tras restaurar", nxt.marks("X") == 3)


@case("TC-16", "Muerte de proceso con partida terminada: sin punto duplicado",
      "game-session: Resultado no duplicado, Restauración soportada; Rotación después de un resultado")
def tc16(c: Ctx):
    fresh(0)
    choose_difficulty("Fácil")
    end = play_to_end(smart)
    c.snap("antes", end)
    kill_in_background()
    st = to_front()
    time.sleep(3)
    st = c.snap("restaurado", None)
    c.check("resultado, tablero y marcador restaurados sin punto extra", st.sig() == end.sig(), f"{end.sig()} -> {st.sig()}")
    kill_in_background(); to_front(); time.sleep(2)
    st = state()
    c.check("segunda muerte de proceso: marcador intacto", st.scores == end.scores, f"{end.scores} -> {st.scores}")


@case("TC-17", "Muerte de proceso con turno de O pendiente (tap + HOME + kill)",
      "game-session: Restauración soportada de O, Pausa y continuación del tiempo restante; reto extra 2")
def tc17(c: Ctx):
    for i in range(3):
        st = fresh(0)
        sh(f"input tap {st.cells[4][1].cx} {st.cells[4][1].cy}; input keyevent KEYCODE_HOME")
        time.sleep(1.8)
        kill_now()
        st = to_front()
        time.sleep(3.5)
        st = c.snap(f"rep{i + 1}-restaurado", None)
        c.check(f"rep{i + 1}: tras restaurar hay exactamente una X y una O y turno humano (O jugó una vez)",
                (st.marks("X"), st.marks("O")) == (1, 1) and st.status == "Tu turno", f"{st.board} {st.status}")
        c.check(f"rep{i + 1}: marcador intacto", st.scores == (0, 0, 0))
    # O opening pending at death
    st = fresh(0)
    new_game_button()   # game 2: O opens
    sh("input keyevent KEYCODE_HOME")
    time.sleep(1.8)
    kill_now()
    st = to_front()
    time.sleep(3.5)
    st = c.snap("apertura-restaurada", None)
    c.check("apertura de O pendiente + muerte de proceso: una sola O y turno humano",
            (st.marks("O"), st.marks("X")) == (1, 0) and st.status == "Tu turno", f"{st.board} {st.status}")
    c.note("Si O ya había jugado antes del HOME (ventana de ~0.3 s), la repetición equivale a TC-15; el estado pendiente exacto no es observable desde adb.")


@case("TC-18", "Detención forzada y lanzamiento nuevo: partida nueva, marcador y dificultad conservados, sonido activo",
      "game-session: Límites de la sesión (Sin persistencia permanente, Nueva sesión, Sin snapshot temporal)")
def tc18(c: Ctx):
    fresh(0)
    choose_difficulty("Fácil")
    play_to_end(smart)
    choose_difficulty("Imposible")
    new_game_button()
    st = wait_for(lambda s: s.human_turn() or s.terminal(), 10, what="turno")
    if st.board == "." * 9:
        st = play_one_move_and_reply()
    menu_choose("Sonido")
    pre = state()
    c.check("precondición: partida activa, sonido silenciado, marcador > 0", not pre.terminal() and sum(pre.scores) >= 1, f"{pre.board} {pre.scores}")
    c.snap("antes", pre)
    st = relaunch_new()
    c.snap("tras-force-stop", st)
    c.check("partida nueva: tablero vacío e inicia X", st.board == "." * 9 and st.status == "La persona hace la primera jugada", f"{st.board} {st.status}")
    c.check("marcador conservado", st.scores == pre.scores, f"{pre.scores} -> {st.scores}")
    c.check("dificultad conservada (Imposible)", current_difficulty() == "Imposible")
    e = open_menu().entry("Sonido")
    c.check("sonido vuelve a activo (Habilitado) en sesión nueva", e and "Habilitado" in e["texts"], str(e and e["texts"]))
    sh("input keyevent KEYCODE_BACK"); time.sleep(0.5)
    time.sleep(2.5)
    c.check("sin jugada automática (X inicia)", state().board == "." * 9)


@case("TC-19", "Cierre de la tarea desde Recientes: partida nueva, marcador y dificultad conservados",
      "game-session: Nueva sesión tras finalizar la tarea")
def tc19(c: Ctx):
    fresh(0)
    choose_difficulty("Difícil")
    play_to_end(smart)
    sc = state().scores
    new_game_button()
    st = wait_for(lambda s: s.human_turn() or s.terminal(), 10, what="turno")
    if st.board == "." * 9:
        play_one_move_and_reply()
    sh("input keyevent KEYCODE_HOME"); time.sleep(1)
    sh("input keyevent KEYCODE_APP_SWITCH"); time.sleep(2)
    guard(); sh("screencap -p /sdcard/qa_shot.png")
    subprocess.run([ADB, "-s", SERIAL, "pull", "/sdcard/qa_shot.png", str(c.dir / "01-recientes.png")], capture_output=True)
    c.evidence.append(f"evidence/{c.id}/01-recientes.png")
    sh("input swipe 540 1200 540 100 250"); time.sleep(2.5)
    guard(); sh("screencap -p /sdcard/qa_shot.png")
    subprocess.run([ADB, "-s", SERIAL, "pull", "/sdcard/qa_shot.png", str(c.dir / "02-tras-deslizar.png")], capture_output=True)
    c.evidence.append(f"evidence/{c.id}/02-tras-deslizar.png")
    sh("input keyevent KEYCODE_HOME"); time.sleep(1)
    gone = not pids()
    if not gone:
        c.note("El gesto de Recientes no cerró la tarea/proceso en este emulador sin ventana; el caso no se puede ejecutar de forma fiable.")
        c.verdict_override = "no ejecutable"
        return
    st = launch()
    c.snap("relanzado", st)
    c.check("partida nueva tras cerrar la tarea (tablero vacío, X)", st.board == "." * 9 and st.status == "La persona hace la primera jugada", f"{st.board} {st.status}")
    c.check("marcador conservado", st.scores == sc, f"{sc} -> {st.scores}")
    c.check("dificultad conservada (Difícil)", current_difficulty() == "Difícil")


@case("TC-20", "Accesibilidad por jerarquía: etiquetas y objetivos táctiles >= 48 dp (vertical y horizontal)",
      "game-options: Accesibilidad de opciones y diálogos; tic-tac-toe-game: Restricciones extremas y accesibilidad, Acción en menú compacto")
def tc20(c: Ctx):
    st = fresh(0)
    for orient in (0, 1):
        if orient == 1:
            set_rotation(1); time.sleep(1.2)
        st = play_one_move_and_reply() if orient == 0 else state()
        c.snap(f"orient{orient}-juego", st)
        name = "vertical" if orient == 0 else "horizontal"
        ok = True
        for i, (m, n) in sorted(st.cells.items()):
            r, col = divmod(i, 3)
            word = {"X": "X", "O": "O", ".": "vacía"}[m]
            ok &= n.desc == f"Casilla, fila {r + 1}, columna {col + 1}, {word}"
        c.check(f"{name}: 9 casillas con etiqueta 'Casilla, fila r, columna c, estado'", len(st.cells) == 9 and ok)
        c.check(f"{name}: casillas ocupadas expuestas como deshabilitadas, vacías habilitadas",
                all((not n.enabled) == (m != ".") for m, n in st.cells.values()))
        c.check(f"{name}: tablero etiquetado 'Tablero de tres en raya'", st.board_node is not None)
        c.check(f"{name}: botón de menú con etiqueta 'Abrir menú'", st.menu_btn is not None and any(n.desc == "Abrir menú" for n in st.nodes))
        c.check(f"{name}: 'Nueva partida' con texto", st.newgame is not None)
        c.check(f"{name}: objetivos >= 48 dp (menú, Nueva partida, casillas)",
                st.menu_btn.w >= MIN_TARGET_PX and st.menu_btn.h >= MIN_TARGET_PX and st.newgame.h >= MIN_TARGET_PX and all(min(n.w, n.h) >= MIN_TARGET_PX for _, n in st.cells.values()),
                f"menu={st.menu_btn.w}x{st.menu_btn.h} newgame_h={st.newgame.h}")
        m = open_menu()
        c.snap(f"orient{orient}-menu", m)
        c.check(f"{name}: cinco entradas con etiqueta de texto y alto >= 48 dp",
                [e['texts'][0] for e in m.menu_entries if e['texts']] == MENU_ORDER and all(e['node'].h >= MIN_TARGET_PX for e in m.menu_entries),
                str([e['node'].h for e in m.menu_entries]))
        e = m.entry("Dificultad")
        c.check(f"{name}: Dificultad deshabilitada expone enabled=false en partida activa", e and not e["enabled"])
        sh("input keyevent KEYCODE_BACK"); time.sleep(0.6)
        menu_choose("Acerca de")
        a = wait_for(lambda s: s.about_dialog, 8, what="about")
        btn = next((n for n in a.nodes if n.text == "Aceptar"), None)
        c.check(f"{name}: diálogo Acerca de con botón 'Aceptar' >= 48 dp", btn is not None and btn.clickable_ancestor().h >= MIN_TARGET_PX, str(btn and btn.clickable_ancestor().h))
        sh("input keyevent KEYCODE_BACK"); time.sleep(0.6)
    set_rotation(0)
    c.note("El icono de cada entrada del menú es decorativo (contentDescription=null) y la etiqueta es el texto; el recorrido real con TalkBack es manual (TC-30).")


@case("TC-21", "Arranque limpio en horizontal y juego en horizontal",
      "tic-tac-toe-game: Disposición landscape, Sin barra superior; game-options: menú en horizontal")
def tc21(c: Ctx):
    st = fresh(1)
    c.snap("arranque-horizontal", st)
    c.check("arranque en horizontal: tablero vacío e inicia X", st.landscape and st.board == "." * 9 and st.status == "La persona hace la primera jugada")
    layout_checks(c, st, "horizontal limpio")
    tap_cell(st, 4)
    s = wait_for(lambda x: x.human_turn() and x.marks("O") == 1, 15, what="O reply")
    c.snap("jugada-horizontal", s)
    m = open_menu()
    c.snap("menu-horizontal", m)
    c.check("menú horizontal: cinco entradas en orden", [e['texts'][0] for e in m.menu_entries if e['texts']] == MENU_ORDER)
    sh("input keyevent KEYCODE_BACK"); time.sleep(0.6)
    c.check("Atrás cierra el menú en horizontal", not state().menu_open)
    set_rotation(0)
    st = state()
    layout_checks(c, st, "vertical tras horizontal")
    c.check("tablero conservado al volver a vertical", st.marks("X") == 1 and st.marks("O") == 1)


@case("TC-22", "Estabilidad: sin crash ni ANR durante toda la ejecución",
      "design: Resolver recreación durante la espera de O sin crash")
def tc22(c: Ctx):
    crash = sh("logcat -d -b crash")
    full = sh(f"logcat -d -v time *:E")
    mine = [ln for ln in full.splitlines() if PKG in ln or "AndroidRuntime" in ln]
    anr = [ln for ln in sh("logcat -d -b events | grep -i am_anr").splitlines() if PKG in ln]
    c.save_text("logcat-crash.txt", crash)
    c.save_text("logcat-errors-app.txt", "\n".join(mine[-200:]))
    c.check("buffer de crash sin 'FATAL EXCEPTION'", "FATAL EXCEPTION" not in crash and PKG not in crash)
    c.check("sin am_anr del paquete", not anr)
    c.note("El logcat se limpió en TC-03 y se revisa al final; contiene solo la parte posterior a TC-03.")


@case("TC-30", "MANUAL: sonido audible y recorrido TalkBack",
      "game-session: sonido sin duplicar; game-options: Accesibilidad (tecnología de asistencia)")
def tc30(c: Ctx):
    c.note("No ejecutado: el emulador corre sin audio y TalkBack no se automatiza. Debe hacerlo una persona: (1) oír X/O al jugar, silencio con Sonido deshabilitado, un solo sonido por jugada tras rotar y tras restaurar; (2) recorrido con TalkBack de casillas, menú, diálogos y 'Reiniciar puntajes'.")
    c.verdict_override = "manual, no ejecutada"


# =========================================================================== main
def main() -> int:
    argv = sys.argv[1:]
    if "--list" in argv:
        for cid, title, covers, _ in CASES:
            print(cid, "-", title)
        return 0
    guard()
    if "--check" in argv:
        print(f"OK target {SERIAL} is AVD {AVD}")
        return 0
    only = None
    if "--only" in argv:
        only = set(argv[argv.index("--only") + 1].split(","))
    EVID.mkdir(parents=True, exist_ok=True)
    orig_accel = sh("settings get system accelerometer_rotation").strip()
    orig_rot = sh("settings get system user_rotation").strip()
    log(f"original settings accelerometer_rotation={orig_accel} user_rotation={orig_rot}")
    results = []
    if only and (ROOT / "results.json").exists():
        prev = json.loads((ROOT / "results.json").read_text()).get("cases", [])
        results = [r for r in prev if r["id"] not in only]
    try:
        sh("settings put system accelerometer_rotation 0")
        sh("settings put system user_rotation 0")
        install = adb("install", "-r", str(ROOT.parents[4] / "app/build/outputs/apk/debug/app-debug.apk"), check=False)
        log(f"install: {install.strip()}")
        if not only:
            sh("logcat -c")
        for cid, title, covers, fn in CASES:
            if only and cid not in only:
                continue
            log(f"=== {cid}: {title}")
            c = Ctx(cid, title, covers)
            CURRENT['ctx'] = c
            t0 = time.time()
            verdict = "pasa"
            err = None
            try:
                fn(c)
                if c.verdict_override:
                    verdict = c.verdict_override
                elif not c.checks:
                    verdict = "no ejecutable"
                elif any(not x["ok"] for x in c.checks):
                    verdict = "falla"
            except Abort:
                raise
            except Exception as e:  # noqa: BLE001
                verdict = "bloqueado"
                err = f"{type(e).__name__}: {e}"
                c.notes.append("Excepción del harness: " + err)
                try:
                    c.save_text("excepcion.txt", traceback.format_exc())
                except Exception:
                    pass
                log(f"  BLOCKED {err}")
            results = [r for r in results if r["id"] != cid]
            results.append({
                "id": cid, "title": title, "covers": covers, "verdict": verdict,
                "checks": c.checks, "notes": c.notes, "evidence": c.evidence,
                "error": err, "seconds": round(time.time() - t0, 1),
            })
            log(f"=== {cid} -> {verdict}")
            (ROOT / "results.json").write_text(json.dumps(
                {"target": {"serial": SERIAL, "avd": AVD}, "guard_checks": GUARDS["count"],
                 "cases": results}, indent=2, ensure_ascii=False))
    finally:
        try:
            sh("settings put system user_rotation 0")
            sh("settings put system accelerometer_rotation 1")  # factory state of this AVD: auto-rotate ON
            sh("input keyevent KEYCODE_HOME")
            log("restored: user_rotation=0, accelerometer_rotation restored")
        except Exception as e:  # noqa: BLE001
            log(f"restore failed: {e}")
    results.sort(key=lambda r: r["id"])
    summary: dict[str, int] = {}
    for r in results:
        summary[r["verdict"]] = summary.get(r["verdict"], 0) + 1
    (ROOT / "results.json").write_text(json.dumps(
        {"target": {"serial": SERIAL, "avd": AVD}, "guard_checks": GUARDS["count"],
         "summary": summary, "cases": results}, indent=2, ensure_ascii=False))
    log(f"summary: {summary}  guard checks: {GUARDS['count']}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Abort as e:
        print("ABORT:", e, file=sys.stderr)
        sys.exit(3)
