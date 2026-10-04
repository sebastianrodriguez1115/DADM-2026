# QA parcial — tarea 3.4 (regresión 3.3)

## Requerimiento y alcance

Fuente: requerimiento delegado de modernización Kotlin/Compose y `design.md`, `tasks.md`, `specs/game-session/spec.md`, `specs/tic-tac-toe-game/spec.md` de `add-compose-graphics-sound`; `openspec/config.yaml` sin reglas adicionales.

Validar apertura O vacía inmediatamente, turno computadora y X bloqueada, dificultad editable en vacío, exactamente una O a los 1000 ms (ninguna a 999), cancelación de respuesta/apertura antes de limpiar nueva partida, sin marcas/puntos fantasma. Regresión del reloj monotónico compartido: despertar prematuro/submilisegundo, ceil y despacho 400 ms tardío. No se aprueba el cambio completo: foreground real/pausa/rotación y onCleared (3.5/3.6), validación ampliada 3.7, audio 4.x, menú/integración 5.x y migración de timing 3.9 siguen pendientes.

Entorno: Android nativo `com.edu.unal.tictactoe/.MainActivity`, dispositivo físico ADB autorizado; sin URL ni credenciales. JVM con scheduler/clock virtual del proyecto; Compose instrumentation, clases separadas. Playwright/Postman/axe web no aplican. Sin instalar SDK/herramientas, sin tocar ajustes del dispositivo ni otras apps. Solo artefactos propios en esta carpeta. Ejecuciones Gradle generan sus outputs convencionales de build; se copian XML/logs redactados aquí.

## Casos priorizados

Precondición común JVM: ViewModel nuevo y harness `GameViewModelCoroutineTestHarness`, reloj virtual alineado con scheduler; no supone lifecycle implementado. Precondición UI: dispositivo conectado/desbloqueado y runner AndroidJUnit4. Se reutilizan pruebas existentes para no duplicar cobertura.

| ID / riesgo | Requerimiento / capability | Técnica | Pasos y resultado esperado | Prueba existente |
|---|---|---|---|---|
| QA34-01 alta | Apertura O; `specs/game-session`, `specs/tic-tac-toe-game` | Transición / límites | Nueva partida O; leer estado, avanzar 999 y 1 ms: nueve vacías+Computer+editable, cero selección antes, una O+Human al vencer | `OOpeningUsesSameOneSecondWaitAndIsCancelledByNewXGame`, `newGamesAlternateOpenersAndRepeatedReadsDoNotStartAnotherGame` |
| QA34-02 alta | Invalida turno anterior; `specs/game-session` | Transición / negativo | Respuesta X t=0, nueva O t=400; observar t=1000 y 1399 sin O, t=1400 una O propia y score intacto | `newOpeningDoesNotRunAtDeadlineOfCancelledResponse` |
| QA34-03 alta | Cancelación apertura/respuesta; `specs/game-session` | Transición / carrera límite | Cancelar apertura con nueva X inmediatamente; avanzar 1000, vacío+Human+score intacto. Nueva partida justo en deadline antes de despachar: callback antiguo sin efectos | `OOpeningUsesSameOneSecondWaitAndIsCancelledByNewXGame`, `newGameAtOldDeadlineCancelsCallbackNotYetDispatched`, `startingANewGameCancelsThePendingResponseJob` |
| QA34-04 alta | Secuencia O; `specs/tic-tac-toe-game` | Límites / negativo | X válida, otra X rechazada; 999 sin O, +1 una O, otros 1000 sin duplicado | `acceptedXPublishesComputerTurnAndWaitsWithoutAcceptingMoreHumanInput` |
| QA34-05 alta | No antes del umbral (3.3); `specs/tic-tac-toe-game` | Límites subms / error temporal | Wake con reloj 999.5 ms: no O; reprogramar +1 y reloj 1000: una O. Despacho a 400 consume parte de los 1000, resta 600 | `earlySubMillisecondWakeIsRoundedUpAndNeverMovesBeforeMonotonicDeadline`, `coroutineDispatchDelayConsumesTimeFromAcceptanceBeforeSchedulingDelay` |
| QA34-06 alta | Terminal/score/nueva partida; `specs/game-session`, `specs/tic-tac-toe-game` | Tabla terminal / transición | Victoria X, empate y victoria O; avanzar/entradas inválidas sin respuesta/puntos extra; nueva partida limpia preservando score | `terminalHumanWinAndTieNeverSelectOrScheduleAComputerMove`, pruebas terminales y score de `GameViewModelTest` |
| QA34-07 media | Editable durante apertura / política IA; `specs/game-session` | Particiones | Impossible fijado antes de apertura: vacío editable, una O a 1000, cero RNG, luego dificultad bloqueada; regresión todas políticas IA | `openingMoveUsesTheCanonicalDifficultyAndAvoidsTheRandomizerForImpossible`, `ImpossiblePolicyTest`, `ImpossibleExhaustiveTest` |
| QA34-08 media | Conservación de mute (regresión 2.x); estado sesión | Transición | Alternar preferencia y transiciones/nueva partida/movimientos aceptados/dificultad aceptada-rechazada: conservar valor sin alterar score/tablero indebidamente | `GameViewModelSoundTest` |
| QA34-09 alta | Flujo UI apertura/respuestas y score; ambas capabilities | E2E / transición | Ejecutar `TicTacToeScreenTest` completa aislada: apertura esperada por condición, alternancia/abandono, resultados/score, bloqueo Computer/terminal, nueve semánticas y tamaños/temas | 15 tests |
| QA34-10 media | Nueve hitboxes accesibles / bloqueo; `specs/custom-game-board`, `specs/tic-tac-toe-game` | Particiones / negativo / accesibilidad | Ejecutar `GameBoardInteractionTest` aislada: touch/click/Enter una acción, Tab omite ocupada, objetivos >=48dp y roles/etiquetas, fuera/ocupada/Computer/terminal cero callbacks | 5 tests |

Exploración guiada: comprobar evidencias de cancelación cercana a deadlines y regresión de snapshots/score/Impossible, sin probar APIs privadas ni duplicar tests del proyecto. Cambiar dificultad a mitad de apertura y múltiples wake tempranos queda para cobertura ampliada 3.7; no se considera probado por la prueba de Impossible fijado antes de comenzar.

## Ejecución y resultados

**APROBADO exclusivamente alcance 3.4 y regresión 3.3.** Ejecución independiente el 2026-10-02, 15:22–15:23 UTC, contra dispositivo físico Android 15. Sin bloqueo del teléfono. Conteos verificados en XML `testcase` (no inferidos de BUILD SUCCESSFUL), clases instrumentadas separadas; ningún caso omitido. Runner global exit 0. Logs confirman tareas de test ejecutadas con `--no-configuration-cache --rerun-tasks`, no recuperadas de configuración/cache.

| Suite | Ejecutados / pasan | Fallan / omitidos | Exit | Evidencia |
|---|---|---|---|---|
| JVM completa | 96 / 96 | 0 / 0 | 0 | `evidence/jvm.log`, `evidence/jvm-summary.json`, `evidence/jvm/*.xml` |
| `TicTacToeScreenTest` | 15 / 15 | 0 / 0 | 0 | `evidence/screen.log`, `evidence/screen-summary.json`, `evidence/screen/0-TEST-SM-G998B - 15.xml` |
| `GameBoardInteractionTest` | 5 / 5 | 0 / 0 | 0 | `evidence/board.log`, `evidence/board-summary.json`, `evidence/board/0-TEST-SM-G998B - 15.xml` |

| Caso | Resultado | Evidencia (test names en JSON y XML) |
|---|---|---|
| QA34-01 | PASA | `evidence/jvm/7-TEST-com.edu.unal.tictactoe.ui.game.GameViewModelCoroutineHarnessTest.xml`, `evidence/jvm/10-TEST-com.edu.unal.tictactoe.ui.game.GameViewModelTest.xml` |
| QA34-02 | PASA | JVM XML 7, `newOpeningDoesNotRunAtDeadlineOfCancelledResponse` |
| QA34-03 | PASA | JVM XML 7, tests de cancelación/apertura y deadline |
| QA34-04 | PASA | JVM XML 7, `acceptedXPublishesComputerTurnAndWaitsWithoutAcceptingMoreHumanInput` |
| QA34-05 | PASA | JVM XML 7, tests subms y dispatch tardío |
| QA34-06 | PASA | JVM XML 7 y 10, terminales/score |
| QA34-07 | PASA | `evidence/jvm/8-TEST-com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest.xml`, XML 2 (`ImpossibleExhaustiveTest`: 2/2) y 3 (`ImpossiblePolicyTest`: 6/6) |
| QA34-08 | PASA | `evidence/jvm/9-TEST-com.edu.unal.tictactoe.ui.game.GameViewModelSoundTest.xml` (2/2) |
| QA34-09 | PASA | `evidence/screen-summary.json` y XML pantalla (15/15) |
| QA34-10 | PASA | `evidence/board-summary.json` y XML interacción (5/5) |

Desglose JVM: ExampleUnitTest 1, DifficultyPolicyTest 5, ImpossibleExhaustiveTest 2, ImpossiblePolicyTest 6, TicTacToeGameTest 24, GameModelContractsTest 4, DifficultyLabelsTest 3, GameViewModelCoroutineHarnessTest 10, GameViewModelDifficultyTest 12, GameViewModelSoundTest 2, GameViewModelTest 15, GameUiStateDifficultyTest 6, GameUiStateTest 6 = **96**.

**Defectos reproducibles:** ninguno en este alcance. Observación no bloqueante: Gradle/protobuf emite warning de uso de `sun.misc.Unsafe` deprecado durante instrumentación; no fallo del producto. No se atribuye a esta implementación.

Integridad: fuentes/tests/config/specs leídos permanecieron idénticos por SHA-256 (`evidence/input-sha256.json`, `evidence/input-integrity.json`). Seriales ADB ausentes en artefactos: verificación de redacción PASA. No se generaron screenshots/traces/videos por no existir fallos; evidencia nativa son logs y XML por test, no traces web. Herramientas instaladas: ninguna.

## Reejecución

Desde raíz del módulo: `python3 openspec/changes/add-compose-graphics-sound/qa/step-3.4/run-qa.py` (o argumentos `jvm`, `screen`, `board`). Define `JAVA_HOME` y `ANDROID_HOME` si difieren de las rutas del encargo. El script ejecuta clases instrumentadas separadas, con `--no-configuration-cache --rerun-tasks`, y exige conteos exactos; exit 0 solo si todos pasan.

## Cobertura excluida / riesgos

No fullActivityOptions ni ImpossibleOpeningTimingTest: aún no migrados, fuera del alcance solicitado. Tampoco Activity real lifecycle, background/rotación, audio/SoundPool, escucha manual, TalkBack manual ni suite instrumentada completa. Pruebas semánticas Compose no equivalen a validación manual con TalkBack. No lint/assemble independientes en esta tanda: el encargo prioriza 96 JVM y 15+5 instrumentados; compilación/build de instrumentación ocurre con esas tareas. Sin sync/archive/commits/push.
