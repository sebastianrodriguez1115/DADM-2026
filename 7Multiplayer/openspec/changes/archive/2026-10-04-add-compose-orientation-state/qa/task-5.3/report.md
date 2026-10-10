# Reporte de QA - tarea 5.3 (`add-compose-orientation-state`)

**Veredicto: APROBADO CON OBSERVACIONES** (sin defectos del producto; dos verificaciones manuales pendientes y limitaciones de observabilidad declaradas abajo).

- Destino: emulador `emulator-5556`, AVD `DADM_GraphicsSound_API35`, API 35, 1080x2400 @ 420 dpi, modo claro. Guard de identidad antes de cada operación `adb` (1421 comprobaciones en la ejecución completa; ningún otro dispositivo se consultó ni operó).
- APK: `app/build/outputs/apk/debug/app-debug.apk` (instalado con `install -r`).
- Ejecución: `run_qa.py` completo a las 16:36-16:55 (22 casos automatizados + 1 manual) y re-ejecución aislada de TC-12 (16:55-16:59) tras añadir captura inmediata del Toast; ambas quedaron mezcladas en `results.json`. Logs: `run.log`, `run-tc12.log`.
- Resultados: 22 pasa, 0 falla, 0 bloqueado, 0 no ejecutable, 1 manual no ejecutada (TC-30). 0 defectos.
- Ajustes de los propios tests (no del producto) antes del resultado final: (a) la primera ejecución dio 2 fallas que eran del harness: en TC-12 se leía el tablero con el menú abierto (la ventana emergente no contiene el tablero) y en TC-21 el arranque en horizontal no se aplicó (ver "Notas del entorno"); se corrigieron y se re-ejecutó todo desde cero (la evidencia de esa primera pasada se descartó).

## Resultados por caso

| ID | Caso | Veredicto | Checks ok | Evidencia |
|---|---|---|---|---|
| TC-01 | Arranque limpio (datos borrados), vertical | **pasa** | 15/15 | [`evidence/TC-01/`](evidence/TC-01/) (5 archivos) |
| TC-02 | Menú: cinco entradas en orden, sin Salir; cierre con Atrás | **pasa** | 12/12 | [`evidence/TC-02/`](evidence/TC-02/) (8 archivos) |
| TC-03 | Atrás sin overlay: navegación normal del sistema (sin intercepción) | **pasa** | 2/2 | [`evidence/TC-03/`](evidence/TC-03/) (3 archivos) |
| TC-04 | Partida completa hasta un desenlace: punto único y taps posteriores sin efecto | **pasa** | 10/10 | [`evidence/TC-04/`](evidence/TC-04/) (12 archivos) |
| TC-05 | Rotación con tablero vacío | **pasa** | 33/33 | [`evidence/TC-05/`](evidence/TC-05/) (4 archivos) |
| TC-06 | Rotación en partida activa con turno humano | **pasa** | 25/25 | [`evidence/TC-06/`](evidence/TC-06/) (6 archivos) |
| TC-07 | Rotación con turno de O pendiente (tap + rotación inmediata) | **pasa** | 15/15 | [`evidence/TC-07/`](evidence/TC-07/) (10 archivos) |
| TC-08 | Rotación con partida terminada: sin punto ni jugada duplicados | **pasa** | 24/24 | [`evidence/TC-08/`](evidence/TC-08/) (6 archivos) |
| TC-09 | Rotación con sonido silenciado | **pasa** | 6/6 | [`evidence/TC-09/`](evidence/TC-09/) (2 archivos) |
| TC-10 | Rotación con menú o diálogo abierto | **pasa** | 4/4 | [`evidence/TC-10/`](evidence/TC-10/) (6 archivos) |
| TC-11 | Nueva partida: alternancia del iniciador y apertura única de O | **pasa** | 10/10 | [`evidence/TC-11/`](evidence/TC-11/) (10 archivos) |
| TC-12 | Dificultad: cuatro niveles, Toast, bloqueo en partida activa, edición al terminar | **pasa** | 28/28 | [`evidence/TC-12/`](evidence/TC-12/) (28 archivos) |
| TC-13 | Persistencia del marcador tras detención forzada | **pasa** | 4/4 | [`evidence/TC-13/`](evidence/TC-13/) (4 archivos) |
| TC-14 | Reiniciar puntajes: en partida activa y terminada; el cero persiste | **pasa** | 12/12 | [`evidence/TC-14/`](evidence/TC-14/) (10 archivos) |
| TC-15 | Muerte de proceso en segundo plano: partida activa (y sonido silenciado) | **pasa** | 6/6 | [`evidence/TC-15/`](evidence/TC-15/) (4 archivos) |
| TC-16 | Muerte de proceso con partida terminada: sin punto duplicado | **pasa** | 2/2 | [`evidence/TC-16/`](evidence/TC-16/) (4 archivos) |
| TC-17 | Muerte de proceso con turno de O pendiente (tap + HOME + kill) | **pasa** | 7/7 | [`evidence/TC-17/`](evidence/TC-17/) (8 archivos) |
| TC-18 | Detención forzada y lanzamiento nuevo: partida nueva, marcador y dificultad conservados, sonido activo | **pasa** | 6/6 | [`evidence/TC-18/`](evidence/TC-18/) (4 archivos) |
| TC-19 | Cierre de la tarea desde Recientes: partida nueva, marcador y dificultad conservados | **pasa** | 3/3 | [`evidence/TC-19/`](evidence/TC-19/) (4 archivos) |
| TC-20 | Accesibilidad por jerarquía: etiquetas y objetivos táctiles >= 48 dp (vertical y horizontal) | **pasa** | 18/18 | [`evidence/TC-20/`](evidence/TC-20/) (8 archivos) |
| TC-21 | Arranque limpio en horizontal y juego en horizontal | **pasa** | 25/25 | [`evidence/TC-21/`](evidence/TC-21/) (6 archivos) |
| TC-22 | Estabilidad: sin crash ni ANR durante toda la ejecución | **pasa** | 2/2 | [`evidence/TC-22/`](evidence/TC-22/) (2 archivos) |
| TC-30 | MANUAL: sonido audible y recorrido TalkBack | **manual, no ejecutada** | 0/0 | [`evidence/TC-30/`](evidence/TC-30/) (0 archivos) |

Cada carpeta contiene capturas `.png` y volcados de jerarquía `.xml` con el nombre `NN-paso`; los checks con su detalle están en `results.json` (campo `checks`). Evidencias destacadas:

- Arranque limpio: `evidence/TC-01/01-arranque.png`; disposición horizontal: `evidence/TC-21/01-arranque-horizontal.png`, `evidence/TC-05/` (vacío-horizontal / vacío-vertical); menú en horizontal con Dificultad deshabilitada: `evidence/TC-21/03-menu-horizontal.png`.
- Toast con el nivel elegido (revisado visualmente: "Fácil", "Imposible", "Difícil", "Experto" aparecen en el Toast): `evidence/TC-12/toast-*.png`.
- Preferencias persistidas (`difficulty_code` 2 y 3): `evidence/TC-12/prefs-experto.xml`; marcador antes/después de force-stop: `evidence/TC-13/prefs-antes.xml`, `prefs-despues.xml`; ceros: `evidence/TC-14/A-prefs.xml`, `B-prefs.xml`.
- Cierre de la tarea desde Recientes: `evidence/TC-19/01-recientes.png`, `02-tras-deslizar.png`.
- Crash/ANR: `evidence/TC-22/logcat-crash.txt` (vacío de excepciones fatales).

### Observaciones por caso (verificado vs. no verificable)

- **TC-01/02/20/21 (presentación y menú):** sin título ni texto por encima del tablero; botón de menú ("Abrir menú") a la derecha de "Nueva partida" en la misma fila (vertical y horizontal); menú con exactamente cinco entradas en orden, ninguna "Salir", Sonido expone "Habilitado/Deshabilitado"; Dificultad aparece `enabled=false` en partida activa; objetivos de 126x126 px (= 48 dp) en menú, casillas (>= 226 px), filas de radio y entradas. En horizontal el tablero mide 709 px = 270 dp, es cuadrado y está a la izquierda; estado, marcador y controles a la derecha. En vertical el tablero ocupa todo el ancho útil (996 px) y el orden es tablero > estado > marcador > controles.
- **TC-03:** con la app en la raíz de la tarea y sin overlay, Atrás la lleva a segundo plano/launcher sin crash y sin interceptación. Al volver, la actividad se había finalizado: tablero vacío y X inicia (el proceso seguía vivo). Es navegación normal del sistema; la especificación no garantiza finalizar la tarea, por lo que se considera conforme (informativo).
- **TC-04:** se observaron los tres desenlaces (victoria humana, victoria de la computadora, empate), cada uno sumó exactamente 1 al contador correcto y 9 taps posteriores no cambiaron nada; marcador final 1/1/1.
- **TC-07 / TC-17 (turno de O pendiente):** 5 rotaciones (3 simples, 2 dobles) con rotación disparada ~0.3 s después del tap y 3 muertes de proceso con tap+HOME+kill, más una muerte con apertura de O pendiente: en todos los casos quedó exactamente una X y una O (o solo una O en la apertura), turno humano y marcador intacto; ninguna jugada duplicada ni perdida. **No verificable end-to-end:** que la espera estuviera realmente pendiente en el instante de la rotación/kill ni los milisegundos exactos (999/1000, 400/600 ms); `adb` no permite esa precisión. Esa parte la cubren las pruebas JVM/instrumentadas con reloj virtual.
- **TC-10 (observación, no defecto):** menú, diálogo de Dificultad y diálogo Acerca de **se cierran** al rotar. La especificación declara expresamente que no se exige conservar el diálogo de dificultad y no exige conservar las demás ventanas; la partida quedó intacta y sin crash.
- **TC-12:** el Toast se detecta como ventana `Toast` (no aparece en la jerarquía de accesibilidad) y su texto se revisó en la captura; se comprobó que elegir el mismo radio también emite Toast, que cancelar con Atrás no lo emite y que rotar tras elegir no lo repite. No se puede contar "exactamente un Toast" por encima de "hay Toast y no se repite tras expirar".
- **TC-13/14/15/16/18/19:** el marcador y la dificultad sobreviven a force-stop y a cierre de tarea; el reset deja ceros inmediatos con la partida intacta (activa y terminada) y persistentes; la restauración tras muerte de proceso (verificada con `pidof` vacío tras `am kill` y pid distinto después) devuelve tablero, turno, marcador y sonido silenciado sin jugada ni punto extra; force-stop/cierre de tarea dan partida nueva con sonido "Habilitado".

## Defectos

Ninguno. No se encontró ningún comportamiento que contradiga los escenarios de los tres specs en lo observable por `adb`.

## Notas del entorno (no son defectos de la app)

1. Al fijar `user_rotation=1` mientras está el launcher en pantalla, Android lo restablece a 0 al arrancar la app; el harness rota con la app en primer plano y luego hace force-stop + pm clear + arranque en frío (`fresh(1)`).
2. `uiautomator dump` solo vuelca la ventana activa: con menú/diálogo abierto el tablero no aparece; los checks de tablero se hacen siempre con el overlay cerrado.
3. `am start` normal tras `am kill` crea una instancia nueva encima de la tarea restaurada; para volver a la tarea existente se usa `am start -f 0x20000000 -a MAIN -c LAUNCHER -n ...` (single top).
4. Ajustes del emulador: al inicio estaban `accelerometer_rotation=1` y `user_rotation=0`; al terminar quedaron exactamente así (comprobado). El emulador no se reinició ni se borró (`-wipe-data` no usado); solo se limpiaron los datos de la app con `pm clear`.

## Cobertura respecto a los escenarios de los specs

**Cubierto (E2E, ver casos):**
- `tic-tac-toe-game`: Disposición landscape y portrait, Sin barra superior, Acción en menú compacto (entrada con icono y etiqueta; icono visible en `TC-21/03-menu-horizontal.png`), Reset conserva juego.
- `game-session`: Primera partida y alternancia (incl. abandono sin puntos), Nueva partida y apertura de O (una sola vez), Puntuación única (los tres desenlaces, taps posteriores), Rotación activa / terminal / espera pendiente (invariante final), Restauración soportada tras muerte de proceso (activa, terminal, O pendiente, sonido), Sin snapshot: force-stop, cierre de tarea y lanzamiento nuevo, Background y Back, Códigos de dificultad (0..3 observados en prefs, defecto Experto), Restablecimiento persistente (activa y terminal).
- `game-options`: Menú con cinco entradas, Sin Salir, Back con y sin overlay, El menú se cierra antes de la acción, Entrada deshabilitada no ejecuta acción, Selección de dificultad (cuatro niveles, un radio, Toast, mismo radio, cancelar, deshabilitada en activa, habilitada tras desenlace, habilitada antes de la primera jugada), Persistencia de dificultad, Accesibilidad por jerarquía (etiquetas, estado deshabilitado, 48 dp).

**No cubierto o solo parcialmente, con motivo:**
- **Manual, no ejecutada (TC-30):** sonido realmente audible / silencio / un solo sonido por jugada tras rotar o restaurar (el emulador corre sin audio) y recorrido con TalkBack.
- Tiempos exactos de la espera de O (999/1000 ms, 400/600 ms, "sin jugada antes de 1000 ms") y estado "Turno de la computadora" visible durante la espera: no observables con precisión desde `adb`; cubiertos por pruebas JVM/instrumentadas.
- Escenario "Dificultad editable durante la espera de apertura de O" y "Selección obsoleta durante apertura pendiente": requieren actuar dentro de la ventana de 1 s; solo se comprobó el cierre de la ventana después de la apertura (TC-11).
- Snapshot corrupto / versión desconocida / claves de preferencias inválidas (contadores negativos o de tipo erróneo, código de dificultad desconocido): no inyectables desde el usuario sin modificar datos de la app; cubiertos por pruebas JVM.
- Restricciones extremas: `fontScale` grande, insets/recortes, ventanas pequeñas o multi-ventana, tema oscuro, otro tamaño de pantalla: solo se probó 1080x2400 @ 420 dpi, fontScale 1, modo claro.
- Muerte de proceso con menú/diálogo abierto, y combinación muerte de proceso + rotación: no ejecutadas.
- Persistencia ante terminación abrupta antes del flush de `apply()`: fuera de garantía según `design.md`; no probada.
- Durabilidad de "no se rompe nada con la sesión de un dispositivo físico": no aplica (solo emulador, por encargo).

**Riesgos residuales:** (1) carrera real entre rotación/kill y temporizador de O, solo comprobada por invariante final con 5 + 4 repeticiones (muestra pequeña); (2) la IA no es determinista, por lo que los desenlaces que se observaron pueden variar entre ejecuciones; (3) el audio sigue sin verificar.

## Cómo re-ejecutar

```
cd /Users/jsrc1115/Code/DADM-2026/6ChangeOrientation
python3 openspec/changes/add-compose-orientation-state/qa/task-5.3/run_qa.py            # todo (~25 min)
python3 openspec/changes/add-compose-orientation-state/qa/task-5.3/run_qa.py --only TC-07,TC-17   # casos concretos (se mezclan en results.json)
python3 openspec/changes/add-compose-orientation-state/qa/task-5.3/run_qa.py --check    # solo identidad del destino
```
Herramientas instaladas: ninguna (Python 3 estándar + `adb` existente). El script no guarda credenciales ni identificadores de otros dispositivos.
