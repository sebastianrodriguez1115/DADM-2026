# QA 3.2 — separación X / respuesta O

**Estado vigente: APROBADO exclusivamente para 3.2 tras reintento OBS-01.** Ver resultados al final. El bloqueo y las evidencias anteriores se conservan como historial.

## Alcance y entorno
Requerimiento: tarea 3.2 y delta `specs/tic-tac-toe-game`, requisitos «Secuencia de respuesta de la computadora» y «Tablero y marcas visibles» (terminales). Leídos `openspec/config.yaml`, design, tasks y spec del cambio. Gradual: apertura síncrona (3.4), deadline/revalidación nanosegundos (3.3), foreground/lifecycle (3.5–3.6), audio (4.x) quedan fuera; no se exige el cambio completo en esta QA.

Android nativo `com.edu.unal.tictactoe/.MainActivity`; dispositivo físico autorizado conectado en preflight. Sin URL, credenciales, producción ni instalación de herramientas. JAVA_HOME JBR Android Studio y ANDROID_HOME SDK suministrados. No usar Playwright/Postman/axe (no hay web/API). Semántica/foco/teclado Compose; sin TalkBack humano.

## Casos priorizados
Todos trazan a `specs/tic-tac-toe-game`. Precondición común JVM: scheduler y dispatcher virtuales compartidos, motor determinista; sin exigencia de foreground aún. Instrumentación: dispositivo autorizado y APK debug actual.

| ID / riesgo | Requisito / técnica | Precondiciones, pasos | Esperado | Resultado |
|---|---|---|---|---|
| Q32-01 alta | Secuencia respuesta / transición | X inicial; aceptar X en 0; consultar estado antes de avanzar reloj | Solo una X, Computer, ninguna selección O síncrona | PASA — evidence/unit |
| Q32-02 alta | Espera / particiones y límite | Q32-01; X adicional en casilla vacía durante espera; avanzar 999ms, luego 1ms; avanzar 1000ms más | X rechazada; 999ms sin O; a 1000ms una O y Human; sin segunda O | PASA — evidence/unit |
| Q32-03 alta | X terminal / transición | Juego determinista; X 0,3,4,8 con respuestas entre jugadas; avanzar después de terminal y repetir entradas | Won(X), sin respuesta terminal, score X=1 estable | PASA — evidence/unit |
| Q32-04 alta | Empate terminal / partición | X 0,4,2,7,5 con respuestas; avanzar tras terminal y repetir entradas | Tie, sin respuesta terminal, score ties=1 estable | PASA — evidence/unit |
| Q32-05 alta | Cancelación / transición | Aceptar X; newGame durante espera; avanzar 1000ms | Ninguna respuesta antigua ni score fantasma; apertura síncrona actual permitida | PASA — evidence/unit |
| Q32-06 media | Entradas rechazadas / límites | Probar -1,9,MIN/MAX, X/O ocupadas y partida terminal | Snapshot intacto, sin nuevas respuestas ni score | PASA — evidence/unit |
| Q32-07 alta | O terminal / transición | X 0,2,6,5 con respuestas; repetir entradas | Won(O), score O=1 estable, no habilitar X | PASA — evidence/unit |
| Q32-08 media | UI / regresión flujo usuario | Ejecutar TicTacToeScreenTest separado (15): estados, callbacks, flujo real, terminal/Computer disabled, nueva partida | Todos pasan; sin regresión visible de estados/semántica | PASA 15/15 — evidence/TicTacToeScreenTest-retry-obs01. Historial: 13/15 dos veces en evidence/TicTacToeScreenTest y -repeat |
| Q32-09 media | UI entrada/accesibilidad / tabla de decisión | Ejecutar GameBoardInteractionTest separado (5): touch/click/Enter/Tab, ocupada/Computer/terminal/outside | Callback único en habilitada; cero en deshabilitada; nueve etiquetas/roles y ≥48dp | PASA 5/5 reintento — evidence/GameBoardInteractionTest-retry-obs01; previo 5/5 conservado |
| Q32-10 media | Regresión / particiones | Reejecutar toda suite JVM (--rerun-tasks) | 91 casos frescos; sin fallos/errors/skips | PASA 91/91 — evidence/unit |

No duplicar límites virtuales mediante ADB: no certifica 999/1000ms. Charter opcional nativo: observar X inmediata, turno Computer y nueva partida receptiva mientras espera; no inferir precisión temporal del wall clock/ADB.

### Trazabilidad a pruebas existentes
- Q32-01/02: `GameViewModelCoroutineHarnessTest.acceptedXPublishesComputerTurnAndWaitsWithoutAcceptingMoreHumanInput` (comprueba estado inmediato, rechazo, 999+1ms y ausencia de duplicado).
- Q32-03/04: `GameViewModelCoroutineHarnessTest.terminalHumanWinAndTieNeverSelectOrScheduleAComputerMove` más `GameViewModelTest.humanVictoryIsDetectedBeforeComputerCanMove` / `humanFillingTheLastCellWithNoWinProducesATieWithoutComputerMove` (score y todas las entradas terminales repetidas).
- Q32-05: `GameViewModelCoroutineHarnessTest.startingANewGameCancelsThePendingResponseJob`.
- Q32-06: `GameViewModelTest.invalidAndOccupiedLocationsAreExactNoOpsWithoutInvokingComputer`, tests terminales y `humanInputDuringComputerTurnIsRejectedReentrantlyBeforeComputerMove`.
- Q32-07: `GameViewModelTest.computerVictoryLeavesComputerTurnAndDoesNotEnableHumanMoves`.
- Q32-08/09: clases instrumentadas completas por separado; se distingue UI con snapshots estáticos de flujos con ViewModel real.
- Q32-10: toda suite JVM, incluidos motor, IA Impossible, dificultad, snapshots y conservación de sonido (estado únicamente, no reproducción).

## Ejecución / evidencia
Script reutilizable: `python3 openspec/changes/add-compose-graphics-sound/qa/step-3.2/run-tests.py unit` y `.../run-tests.py compose`. Captura logs, XML frescos y exit/counts en `evidence/<suite>/`; IDs de dispositivo redactados. Reejecuta tareas sin configuration cache, una clase instrumentada por invocación. No modifica fuentes/tests/config/tasks ni QA anterior; Gradle genera outputs habituales.

## Resultado inicial (historial)
**BLOQUEADO para aprobación global QA 3.2** por Q32-08. Los contratos funcionales de 3.2 cubiertos por reloj virtual pasan; no se reprodujo defecto del producto. No aprobar la suite instrumentada completa: dos flujos todavía asumen O síncrona. No se corrigieron tests del proyecto por restricción expresa del encargo.

### Resultados reales (2026-10-02)
| Ejecución fresca | Pasan / total | Failures / errors / skipped | Exit Gradle | Evidencia |
|---|---|---|---|---|
| testDebugUnitTest | 91/91 | 0/0/0 | 0 | `evidence/unit/summary.json`, `gradle.log` y XML por clase |
| TicTacToeScreenTest | 13/15 | 2/0/0 | 1 | `evidence/TicTacToeScreenTest/summary.json`, `gradle.log`, `TEST-SM-G998B_-_15.xml` |
| GameBoardInteractionTest | 5/5 | 0/0/0 | 0 | `evidence/GameBoardInteractionTest/summary.json`, `gradle.log`, `TEST-SM-G998B_-_15.xml` |
| TicTacToeScreenTest repetición | 13/15 | 2/0/0 | 1 | `evidence/TicTacToeScreenTest-repeat/summary.json`, `gradle.log`, `TEST-SM-G998B_-_15.xml` |

### OBS-01 — bloqueo de sincronización en tests instrumentados (impacto medio)
No es defecto del producto: el estado Disabled obtenido es precisamente el requerido por 3.2 durante la espera. Se reprodujo idénticamente en dos ejecuciones frescas.

Pasos: ejecutar `./gradlew --no-configuration-cache --rerun-tasks connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe.TicTacToeScreenTest` con el dispositivo autorizado.
- `realViewModelUiFlowCoversHumanComputerAndTieResults`: secuencia 0,3,4,8 falla intentando la segunda X en cell_3 (línea 389).
- `realViewModelUiFlowAlternatesAfterAbandonmentWithoutScoringOrDuplicateOpening`: X en 0 seguida de 4 falla en cell_4 (línea 427).
- Ambos helpers `clickCell` afirman enabled en línea 562 tras solo `waitForIdle()` (línea 564), que no espera el delay de ViewModel.
- Esperado del **test adaptado**: observar Computer/Disabled después de X; esperar por condición Human o desenlace antes de la siguiente jugada. Obtenido: siguiente X intentada durante espera, assertion `(is enabled)` sobre nodo `[Disabled]`.
- Requerimiento trazado: 3.2 / `specs/tic-tac-toe-game`, «O espera tras X válida». Acción para Orchestrator/Worker: migrar la sincronización de estos dos flujos (no habilitar X durante Computer, no introducir esperas fijas) y repetir QA.

### Cobertura y límites
- Límites 999/1000ms y respuesta única demostrados con scheduler virtual; no mediante ADB lento. Terminales/score único y cancelación pasan en JVM.
- UI estática y entrada táctil/clic/teclado/semántica pasan; los dos flujos de UI real no llegan al desenlace por OBS-01. Riesgo residual: integración dinámica de extremo a extremo sin evidencia completa.
- Charter nativo opcional intentado pero no concluyente: la captura se mostró escalada y una superposición del sistema impidió observar los nodos del app. Se descartaron esas capturas y el script exploratorio; no cuentan como evidencia ni defecto. No se cambiaron ajustes globales ni se interactuó deliberadamente con otras apps. APK debug instalado para lanzar la Activity (evidencia limitada a `evidence/native/launch.json`); esto no es instalación de herramientas.
- Sin prueba humana de TalkBack ni de escucha; Playwright/Postman/axe y autenticación/API N/A. Sin lint/full instrumentation porque no forman parte de esta QA acotada.
- Apertura diferida, deadline nanosegundos, foreground/rotación/background y SoundPool/mute efectivo pendientes y fuera de alcance; no se reportan como defectos 3.2.

### Reejecución / herramientas
`python3 openspec/changes/add-compose-graphics-sound/qa/step-3.2/run-tests.py unit <etiqueta-nueva>`

`python3 openspec/changes/add-compose-graphics-sound/qa/step-3.2/run-tests.py compose <etiqueta-nueva>`

`python3 openspec/changes/add-compose-graphics-sound/qa/step-3.2/run-tests.py screen-repeat <etiqueta-nueva>`

Sin herramientas/dependencias/SDK nuevos. Usados Gradle Wrapper, JBR y SDK/ADB existentes, Python estándar y Compose AndroidJUnitRunner del proyecto. El script final devuelve 1 si falla Gradle o counts/assertions, preserva ejecuciones por etiqueta, redacta seriales y valida counts >0; no reutiliza XML antiguos. No commits/push ni cambios en fuentes/tests/config/tasks o QA previa.

## Reintento OBS-01 — 2026-10-02
Encargo: reejecutar Q32-08 y Q32-09 tras corrección de sincronización aprobada en code review; conservar JVM previa 91/91 (fuente sin cambios según Orchestrator). Mismo entorno y alcance 3.2. No reutilizar resultados del Worker como aprobación QA.

Lectura de tests actualizados: `clickCellAndAwaitComputerResponse` espera por condición X aplicada y turno Human o resultado terminal; el flujo de abandono comprueba rechazo en Computer antes de esperar y realizar la segunda X. Sin `Thread.sleep`. Se ejecutan de nuevo ambas clases completas y separadas, tareas frescas, counts exactos >0; artefactos nuevos bajo sufijo `retry-obs01`. El script ahora rechaza sobrescribir carpetas de evidencia existentes.

Comando de esta ejecución: `python3 openspec/changes/add-compose-graphics-sound/qa/step-3.2/run-tests.py compose retry-obs01`.

### Resultado vigente: APROBADO QA 3.2
| Ejecución independiente fresca | Pasan / total | Failures / errors / skipped | Exit Gradle | Evidencia |
|---|---|---|---|---|
| TicTacToeScreenTest, retry-obs01 | 15/15 | 0/0/0 | 0 | `evidence/TicTacToeScreenTest-retry-obs01/summary.json`, `gradle.log`, `TEST-SM-G998B_-_15.xml` |
| GameBoardInteractionTest, retry-obs01 | 5/5 | 0/0/0 | 0 | `evidence/GameBoardInteractionTest-retry-obs01/summary.json`, `gradle.log`, `TEST-SM-G998B_-_15.xml` |

El script de reintento terminó con exit 0. Ambas invocaciones usaron `--no-configuration-cache --rerun-tasks` y filtros de clase separados. JVM **91/91, exit 0** retenida de ejecución previa (`evidence/unit/`), no reejecutada por este encargo; no se atribuye a este reintento.

**OBS-01 cerrada:** los dos flujos anteriormente fallidos pasan ahora en dispositivo con el helper condicionado, incluidos desenlaces humano/computadora/empate y rechazo de entrada durante Computer seguido de abandono sin score ni apertura duplicada. Q32-01 a Q32-10 pasan con la combinación de evidencia virtual previa e instrumentación actual. Sin defectos de producto reproducidos ni bloqueos abiertos en este alcance.

Cobertura actual: queda resuelta la limitación histórica de integración dinámica de esos dos flujos. Se mantienen sin probar/de alcance posterior deadline nanosegundos, apertura diferida, lifecycle/foreground/background/rotación y reproducción SoundPool; sin TalkBack humano ni escucha. No se aprueba el cambio completo, solo 3.2. Charter nativo opcional anterior sigue no concluyente y no se utiliza como evidencia.

Reejecución: `python3 openspec/changes/add-compose-graphics-sound/qa/step-3.2/run-tests.py compose <etiqueta-nueva>`; usar etiqueta inédita para preservar historial. Sin herramientas/dependencias/SDK instalados en este reintento. Solo se actualizaron artefactos de `qa/step-3.2`; sin cambios a producto/tests/tasks/config ni commits/push.
