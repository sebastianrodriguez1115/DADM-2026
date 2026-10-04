# Plan de pruebas - tarea 5.3 (`add-compose-orientation-state`)

Aplicación Android nativa (Kotlin + Compose). Sin Playwright ni Postman: validación de extremo a extremo con `adb` sobre el emulador.

- **Destino único**: `emulator-5556`, AVD `DADM_GraphicsSound_API35` (API 35, 1080x2400, 420 dpi, modo claro). Antes de CADA operación `adb` el script ejecuta `adb -s emulator-5556 emu avd name` y aborta si no devuelve el AVD esperado. No se consulta ni opera ningún otro dispositivo.
- **APK**: `app/build/outputs/apk/debug/app-debug.apk`, paquete `com.edu.unal.tictactoe`.
- **Ejecución**: `python3 run_qa.py` (ver sección final). Genera `results.json`, `run.log` y `evidence/<caso>/NN-paso.{png,xml}`.
- **Trazabilidad**: columna "Cubre" con capability (`specs/<capability>`) y escenario/requisito. Abreviaturas: TTT = `tic-tac-toe-game`, GS = `game-session`, GO = `game-options`.
- **Técnicas**: particiones de equivalencia (estados de partida: vacío / activa-turno humano / turno O pendiente / terminada), transición de estados (ciclo de vida: rotación, segundo plano, muerte de proceso, detención forzada), tablas de decisión (combinación estado de partida x acción de menú), valores límite (objetivos 48 dp = 126 px, tablero 270 dp = 708 px), casos negativos (taps tras desenlace, entrada deshabilitada, Atrás), pruebas exploratorias guiadas (rotación con overlays abiertos).
- **Oráculos**: jerarquía `uiautomator dump` (texto, `content-desc`, `enabled`, `checked`, bounds), `dumpsys window` (ventana Toast), `shared_prefs/game_preferences.xml` vía `run-as` (app debug), `pidof`, `logcat -b crash`.
- **Pirámide**: no se repiten las 221 JVM / 110 instrumentadas; aquí solo flujos de usuario, ciclo de vida real del proceso y evidencia visual.

## Limitaciones de observabilidad (declaradas de antemano)

1. La espera de 1000 ms de O no se puede observar con precisión de milisegundos desde `adb` (el tap tarda ~0.2-0.3 s y un `uiautomator dump` ~1 s). Los casos de "turno pendiente" comprueban el **invariante final** (una sola jugada de O, turno humano, marcador intacto) tras provocar rotación / muerte de proceso inmediatamente después del tap; los límites 999/1000 ms, 400/600 ms quedan en las pruebas JVM/instrumentadas con reloj virtual.
2. El Toast no aparece en la jerarquía de accesibilidad: se detecta como ventana `Toast` en `dumpsys window` y su texto se revisa en la captura.
3. Sonido audible y TalkBack: **manuales, no automatizados** (TC-30).

## Casos

| ID | Caso | Cubre | Técnica |
|---|---|---|---|
| TC-01 | Arranque limpio (vertical) | GS: Primera partida de una sesión nueva, Sin snapshot temporal; TTT: Disposición portrait, Sin barra superior; GO: Persistencia de dificultad (default Experto) | Partición (estado inicial) |
| TC-02 | Menú: cinco entradas en orden, sin Salir, Atrás | GO: Menú con las cinco entradas, Sin acción Salir, Back con overlay, El menú se cierra antes de la acción, Cancelar no cambia la dificultad | Caso positivo + negativo |
| TC-03 | Atrás sin overlay | GO: Back sin overlay; GS: Background y Back | Exploratoria |
| TC-04 | Partida completa hasta desenlace; taps posteriores | GS: Puntuación única (victoria humana / computadora / empate), Resultado no duplicado | Transición de estados + negativo |
| TC-05 | Rotación con tablero vacío | TTT: Disposición landscape / portrait; GS: Rotación con estado guardado | Transición |
| TC-06 | Rotación en partida activa (turno humano) | GS: Rotación durante una partida activa | Transición |
| TC-07 | Rotación con turno de O pendiente (rotación inmediata, simple y doble) | GS: Rotación conserva espera pendiente, Tiempo restante tras rotación; reto 2 del PDF | Transición + temporización |
| TC-08 | Rotación con partida terminada | GS: Rotación después de un resultado | Transición |
| TC-09 | Rotación con sonido silenciado | GS: conservación de sonido; GO: Sonido durante la partida | Transición |
| TC-10 | Rotación con menú / diálogo abierto | GO: Back con overlay; exploratoria (la persistencia de ventanas no se exige) | Exploratoria |
| TC-11 | Nueva partida: alternancia y apertura única de O (+ rotación durante la apertura) | GS: Alternancia, Nueva partida y apertura de O, abandono no puntúa; GO: Nueva partida desde el menú, apertura cierra la edición de dificultad | Transición |
| TC-12 | Dificultad: cuatro niveles, Toast, bloqueo en partida activa, edición al terminar, persistencia tras force-stop | GO: Selección de dificultad (todos los escenarios de UI), Persistencia de la dificultad; GS: Códigos de dificultad | Partición + tabla de decisión (estado x edición) |
| TC-13 | Persistencia del marcador tras force-stop | GS: Contadores persistentes, Sin persistencia permanente | Transición |
| TC-14 | Reiniciar puntajes (activa y terminada) y persistencia del cero | TTT: Acción accesible de reset; GS: Restablecimiento persistente | Tabla de decisión |
| TC-15 | Muerte de proceso: partida activa (+ sonido silenciado) | GS: Restauración soportada por muerte de proceso | Transición |
| TC-16 | Muerte de proceso: partida terminada | GS: Resultado no duplicado, Restauración soportada | Transición |
| TC-17 | Muerte de proceso: turno de O pendiente (respuesta y apertura) | GS: Restauración soportada de O; reto 2 del PDF | Transición + temporización |
| TC-18 | Force-stop y lanzamiento nuevo | GS: Sin persistencia permanente, Nueva sesión tras perder el proceso | Transición |
| TC-19 | Cierre de tarea desde Recientes | GS: Nueva sesión tras finalizar la tarea | Transición |
| TC-20 | Accesibilidad por jerarquía y objetivos >= 48 dp (vertical y horizontal) | GO: Accesibilidad de opciones y diálogos; TTT: Restricciones y accesibilidad | Valores límite |
| TC-21 | Arranque y juego en horizontal | TTT: Disposición landscape, Sin barra superior | Partición |
| TC-22 | Sin crash ni ANR en toda la ejecución | design: recreación durante la espera de O sin crash | Oráculo transversal |
| TC-30 | MANUAL: sonido audible y TalkBack | GS/GO: sonido, accesibilidad asistida | No automatizable |

## Detalle de casos (precondiciones, pasos, resultado esperado)

Convención: "fresh" = `force-stop` + `pm clear` + rotación vertical (user_rotation 0) + lanzamiento. "Estado" = (tablero, texto de estado, marcador) leído de la jerarquía.

### TC-01 Arranque limpio
- Pre: fresh. Pasos: leer jerarquía; esperar 3 s; abrir Dificultad y leer el radio marcado; leer `shared_prefs`.
- Esperado: tablero vacío; "La persona hace la primera jugada"; marcador 0/0/0; sin texto "TicTacToe" ni texto por encima del tablero; tablero cuadrado; botón de menú ("Abrir menú") en la misma fila y a la derecha de "Nueva partida"; objetivos >= 126 px; tras 3 s el tablero sigue vacío; dificultad "Experto"; sin claves de partida en preferencias.

### TC-02 Menú
- Pre: fresh. Pasos: abrir menú; leer entradas; Atrás; abrir menú y elegir Acerca de; Atrás; abrir Dificultad; Atrás.
- Esperado: exactamente 5 entradas en orden Nueva partida, Dificultad, Sonido (con estado "Habilitado"), Reiniciar puntajes, Acerca de; ninguna "Salir"; todas habilitadas con tablero vacío; Atrás cierra el menú sin alterar la partida; Acerca de se abre con el menú ya cerrado; Atrás cierra el diálogo; Atrás en Dificultad cancela sin Toast y sin cambiar el nivel (Experto).

### TC-03 Atrás sin overlay
- Pre: fresh + una jugada. Pasos: Atrás sin nada abierto; `topResumedActivity`; volver a la tarea.
- Esperado: navegación normal del sistema (la app sale de primer plano), sin crash; no se exige finalizar la tarea. Se registra si el proceso/estado se conserva (informativo).

### TC-04 Desenlaces y puntuación única
- Pre: fresh. Pasos: jugar partidas completas con estrategias (inteligente en Fácil buscando victoria de X; "torpe" en Experto buscando victoria de O; inteligente en Experto buscando empate; extras si falta algún desenlace); tras cada desenlace tocar las 9 casillas.
- Esperado: el contador correspondiente aumenta exactamente 1 y los otros no cambian; taps posteriores no cambian tablero, estado ni marcador; casillas deshabilitadas. Si la IA (no determinista) no produce algún desenlace se anota como no observado.

### TC-05 a TC-10 Rotación
- TC-05: fresh, rotar a horizontal y volver; layout horizontal (tablero a la izquierda, <= 270 dp = 708 px, panel a la derecha) y vertical; estado idéntico; X sigue iniciando sin jugada automática.
- TC-06: una jugada X + respuesta O; rotar H-V; mismo estado, 1 X y 1 O tras 3 s en cada orientación; la partida continúa.
- TC-07: fresh; `input tap` sobre el centro y rotación en el mismo comando (3 repeticiones) y rotación doble 0.35 s después del tap (2 repeticiones); tras 3.5 s: exactamente 1 X y 1 O, "Tu turno", marcador 0/0/0.
- TC-08: partida terminada (Fácil); rotar varias veces; mismo tablero, resultado y marcador; sin punto extra.
- TC-09: silenciar Sonido; rotar 4 veces; el menú muestra "Deshabilitado" en cada orientación; tablero y marcador intactos.
- TC-10: con menú / Dificultad / Acerca de abiertos, rotar; sin crash, partida intacta, se registra si el overlay se conserva (no exigido); si se conserva, Atrás lo cierra.

### TC-11 Nueva partida
- Pre: fresh. Pasos: partida 1 (X) con una jugada y abandonarla; Nueva partida (botón) -> O abre; esperar; Nueva partida (menú) -> X; Nueva partida -> O; Nueva partida (X) y luego Nueva partida + rotación inmediata (O).
- Esperado: alternancia X, O, X, O, ...; la apertura de O ocurre una sola vez (1 O, 0 X, "Tu turno" y sin cambios tras 3.5 s); la partida abandonada no puntúa; Dificultad queda deshabilitada tras la apertura; X inicia sin movimiento automático; la rotación durante la apertura no la duplica ni la pierde.

### TC-12 Dificultad
- Pre: fresh. Pasos: abrir diálogo; elegir Fácil, Imposible, Difícil, Experto, Experto de nuevo (mismo radio) comprobando Toast y radio marcado al reabrir; Back (cancelar); rotar tras elegir; force-stop + relanzar; partida activa (Dificultad deshabilitada, tap no abre diálogo); partida terminada (habilitada, cambio aceptado).
- Esperado: 4 radios en orden Fácil, Difícil, Experto, Imposible, uno marcado; cada selección cierra el diálogo y emite un Toast (ventana Toast; texto del nivel visible en captura); el Toast no se repite tras rotar; preferencia `difficulty_code` 2 para Experto y 3 para Imposible; persiste tras detención forzada; deshabilitada en partida activa con marcas y sin efecto al tocarla; habilitada tras el desenlace.

### TC-13 Marcador persistente
- Pre: fresh, Fácil, 2 partidas. Pasos: force-stop + lanzar. Esperado: marcador igual, partida nueva (vacía, X), dificultad Fácil conservada.

### TC-14 Reiniciar puntajes
- Pre: marcador distinto de cero. A) partida activa: menú -> Reiniciar puntajes: 0/0/0 inmediato, tablero/estado intactos, sin cambios 2.5 s después, preferencias en cero; terminar la partida suma exactamente 1. B) partida terminada: reset -> 0/0/0, resultado intacto, no se recuenta, ceros tras rotar y tras force-stop + lanzar. La entrada siempre habilitada.

### TC-15 a TC-17 Muerte de proceso
- Secuencia: HOME, `am kill`, verificar que `pidof` está vacío, volver con `am start -f 0x20000000 -a MAIN -c LAUNCHER` (misma tarea, sin crear instancia nueva).
- TC-15: partida activa con 2 X + 2 O y sonido silenciado; esperado: pid nuevo, mismo tablero/turno/marcador, sin jugada extra, sonido sigue "Deshabilitado", el juego continúa.
- TC-16: partida terminada; esperado: mismo resultado y marcador (sin punto duplicado), también tras una segunda muerte.
- TC-17: tap + HOME en el mismo comando, `am kill`, volver (3 repeticiones) y nueva partida con apertura de O pendiente + HOME + kill; esperado: una sola jugada de O, "Tu turno", marcador intacto.

### TC-18 Force-stop
- Pre: marcador > 0, dificultad Imposible, partida activa, sonido silenciado. Esperado tras `force-stop` + lanzar: tablero vacío, X inicia sin jugada automática, marcador y dificultad iguales, sonido "Habilitado".

### TC-19 Cierre de tarea (Recientes)
- Pre: igual que TC-18 con Difícil. Pasos: HOME, APP_SWITCH, deslizar la tarjeta, lanzar. Esperado: partida nueva; marcador y dificultad conservados. Si el emulador no permite cerrar desde Recientes: no ejecutable.

### TC-20 Accesibilidad
- Vertical y horizontal: etiquetas "Casilla, fila r, columna c, estado" (9); "Tablero de tres en raya"; "Abrir menú"; "Nueva partida"; 5 entradas con texto; Dificultad `enabled=false` en partida activa; botón "Aceptar" de Acerca de; todos los objetivos >= 126 px (48 dp).

### TC-21 Horizontal desde el arranque
- Pre: fresh con user_rotation 1. Esperado: disposición horizontal correcta, juego operativo, menú con 5 entradas, Atrás lo cierra, vuelta a vertical conserva la partida.

### TC-22 Estabilidad
- Revisa `logcat -b crash` y eventos `am_anr` al final.

### TC-30 Manual (no ejecutada por el agente)
- Sonido audible (X y O al jugar, silencio con Sonido deshabilitado, un solo sonido por jugada tras rotar/restaurar) y recorrido TalkBack.

## Cómo re-ejecutar

```
cd /Users/jsrc1115/Code/DADM-2026/6ChangeOrientation
python3 openspec/changes/add-compose-orientation-state/qa/task-5.3/run_qa.py              # todo
python3 .../run_qa.py --only TC-07,TC-17                                                   # casos concretos
python3 .../run_qa.py --check                                                              # solo identidad
```
Requisitos: Python 3 (sin dependencias externas), `adb` en `$HOME/Library/Android/sdk/platform-tools`, emulador `emulator-5556` (AVD `DADM_GraphicsSound_API35`) ya encendido y APK debug construido. El script instala el APK con `install -r`, fija rotación manual y, al terminar, deja `user_rotation=0` y `accelerometer_rotation=1`. No se instaló ninguna herramienta.
