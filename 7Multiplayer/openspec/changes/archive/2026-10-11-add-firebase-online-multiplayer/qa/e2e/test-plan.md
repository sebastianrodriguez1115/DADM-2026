# Plan de pruebas de extremo a extremo (tareas 8.3 a 8.7)

- Aplicación: `com.edu.unal.tictactoe`, commit `86be80a`, rama `reto-7-triqui-online`. Backend: el proyecto real de Firebase del usuario.
- Destinos: instancia A (dispositivo físico, API 35) e instancia B (emulador, API 35). Preparación en `setup.md`.
- Herramienta: adb (`am`, `input`, `uiautomator dump`, `screencap`, `svc` solo en B). Playwright y Postman no aplican a una aplicación Android nativa.
- Fuente de los casos: requisitos y escenarios de `specs/game-mode-selection`, `specs/online-lobby`, `specs/online-match` y los deltas de `game-options` y `game-session` (Back), más las tareas 8.3 a 8.7. Cada caso cita el requisito (capability) que cubre.
- Técnicas: particiones de equivalencia y valores límite (apodo vacío, solo espacios, 1, 20 y 21 caracteres), transición de estados (espera, partida, resultado, abandono), tabla de decisión (quién cierra, cómo y cuándo), carreras temporizadas y charters exploratorios (segundo plano y corte de red).
- Prioridad por riesgo: abandono y desconexión, carrera al unirse y manejo de nulos (A), flujo del reto (A), apodo y rotación (B).
- Los casos de sonido y TalkBack son de la tarea 8.8 (usuario) y no están en este plan.

Los pasos detallados y las capturas de cada caso están en la lista de cada carpeta; aquí va el índice con requisito, técnica, resultado y evidencia.

## Casos de `reto/` (8.4) — detalle en `reto/checklist.md`

| ID | Requisito (capability) | Técnica | Precondiciones | Pasos y resultado esperado | Resultado | Evidencia |
|---|---|---|---|---|---|---|
| R01 | Pantalla inicial con dos entradas (game-mode-selection) | Estado inicial | B con `pm clear` | Abrir la app: solo «Jugar contra la computadora» y «Jugar en línea», sin tablero | pasa | `reto/R01_*` |
| R02 | Apodo al entrar por primera vez (online-lobby) | Estado inicial | Sin apodo guardado (B) | «Jugar en línea»: diálogo «Tu apodo», campo vacío, sin lista | pasa | `reto/R02_*` |
| R03 | Validación del apodo (online-lobby) | Valores límite | Diálogo abierto | Vacío, solo espacios, 21 caracteres: mensaje «Escribe un apodo de 1 a 20 caracteres», diálogo abierto; 20 y «  Luis  » (guarda «Luis»): se acepta | pasa | `reto/R03_*`, `R04_*`, `R06_*` |
| R04 | El apodo se recuerda y se puede cambiar (online-lobby) | Transición | Apodo guardado | `force-stop`, reabrir: sin diálogo; «Cambiar apodo»: valor vigente, validación igual, «Cancelar» y Back lo conservan | pasa | `reto/R05_*`, `R06_*` |
| R05 | Descartar el diálogo de la primera vez (online-lobby) | Transición | Sin apodo | «Cancelar» y Back vuelven a la pantalla inicial sin guardar | pasa | `reto/R07_*` |
| R06 | Conexión con el servicio (online-lobby) | Estado/error | Sin identidad y sin red (B) | «No se pudo conectar con el servicio» y «Reintentar» | pasa (parcial; «Conectando…» no observado) | `reto/R19_*` |
| R07 | Lista de juegos en tiempo real; Crear; Unirse; Solo se juega tras unirse | Transición | A y B en el lobby | Vacía; A crea («Esperando rival…», sin tablero); B ve «Juego de Ana» sin recargar; B se une; ambos ven el tablero | pasa | `reto/R08_*` a `R10_*` |
| R08 | Marcas, turnos, jugadas, sincronización (online-match) | Tabla de decisión | Partida en curso | X crea y empieza; textos «Juegas con…», «Rival…», «Tu turno»/«Turno de…»; fuera de turno y casilla ocupada no responden; doble toque acepta una jugada; la jugada llega en 0.3 a 0.85 s | pasa | `reto/R10_*` a `R12_*` |
| R09 | Resultado y fin sin revancha (online-match) | Transición | Partida en curso | Victoria («Ganaste» y «Ganó Ana»), empate; sin revancha; cada jugador vuelve por su cuenta; Back sin diálogo | pasa | `reto/R13_*` a `R17_*` |

## Casos de `lobby/` (8.5) — detalle en `lobby/resultados.md`

| ID | Requisito | Técnica | Resultado | Evidencia |
|---|---|---|---|---|
| L01 a L03 | Cancelar un juego en espera; Un juego desaparece si su creador se va (online-lobby) | Transición | pasa | `lobby/L01_*` a `L03_*` |
| L04 | El juego en espera se elimina externamente (online-lobby) | Transición | no ejecutado (consola) | — |
| L05 | Dos personas eligen el mismo juego (online-lobby; D7) | Carrera, 5 repeticiones | bloqueado (faltan identidades) | — |
| L06 | Juego retirado antes de unirse (online-lobby) | Carrera temporizada | pasa (6 de 29 intentos con el mensaje) | `lobby/L06c_*` |
| L07 | Un tercer jugador no entra (online-match) | Escenario negativo | bloqueado | — |
| L08 | Fallo al crear el juego (online-lobby) | Error | no ejecutado (no se pudo provocar) | `lobby/L08f_*`, `L08g_*`, `L08h_*` |
| L09 | Pérdida de conexión y recuperación en el lobby (online-lobby) | Estado/error | pasa en el primer corte; falla en los siguientes (D-3) | `lobby/L08c_*`, `L08d_*` |

## Casos de `abandono/` (8.6) — detalle en `abandono/resultados.md`

| ID | Requisito | Técnica | Resultado | Evidencia |
|---|---|---|---|---|
| M01, M02 | Salir con confirmación: «Salir», Back, «Cancelar» (online-match; game-mode-selection: Back) | Transición | pasa | `abandono/M01_*`, `M02_*` |
| M03, M04 | Abandonar; Volver tras el abandono; Back con abandono (online-match) | Transición | pasa | `abandono/M03_*`, `M04_*` |
| M05, M06 | El rival se desconecta: cierre de la app (online-match; online-lobby) | Tabla de decisión | pasa (0.34 a 0.78 s) | `abandono/M05_*`, `M06_*` |
| M07 | El rival pierde la red (online-match) | Estado/error | pasa con observación (114 s) | `abandono/M07b_*` |
| M08 | Sin conexión durante la partida (online-match) | Estado/error | **falla** (D-1) | `abandono/M08_*` |
| M09, M10 | Rotación y segundo plano (online-match) | Exploratorio | **falla** a partir de ~6 s (D-2) | `abandono/M09_*` |
| M11 | Residuo de nodo (D7) | Verificación | no verificado en consola | — |

## Casos de `riesgos/` (8.7) — detalle en `riesgos/resultados.md`

| ID | Requisito | Técnica | Resultado | Evidencia |
|---|---|---|---|---|
| RA | Manejo de nulo en transacciones (D7) | Carrera, 7 + 9 repeticiones | pasa | `riesgos/Ra_*` |
| RB | Fusión de la jugada final con la salida del ganador (D9) | Carrera, 10 repeticiones | pasa (0 de 10 abandonos) | `riesgos/Rb_*` |
| RC | El modo local no avanza fuera de pantalla (game-mode-selection) | Valores límite de tiempo | pasa (sonido sin comprobar) | `riesgos/Rc_*` |
| RD | Muerte del proceso (game-mode-selection) | Transición | pasa | `riesgos/Rd_*` |
| RE | El marcador local no interviene (online-match; game-mode-selection) | Invariante | pasa | `riesgos/Re_*` |

## Cómo re-ejecutar

1. `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` y `./gradlew :app:assembleDebug`.
2. `adb -s <destino> install -r app/build/outputs/apk/debug/app-debug.apk` en el teléfono y en el emulador (con la guarda de identidad de `setup.md`).
3. Los guiones usados eran de Python 3 estándar sobre adb y no se versionan; los pasos de cada caso están en las listas de resultados y las marcas de hora en cada `log.txt`.
