# QA final UI37 / QA18 — tarea 3.7

## Alcance y precondiciones

Requerimiento: una espera O acumula solo foreground, el menú/dialog no la pausa ni provoca movimientos por sí mismo; una dificultad permitida sobre tablero vacío rige la apertura sin adelantarla/reiniciarla; al vencer O se rechaza cualquier selección UI capturada cuando la dificultad era editable, sin cambio de sesión ni Toast. Oráculo: deltas `specs/game-options` (menú/opciones no alteran espera, dificultad editable durante apertura, selección obsoleta/rechazo), `specs/game-session` (apertura/estado), `specs/tic-tac-toe-game` (O única), diseño y task 3.7 ya leídos en QA previa. Code review aprobado y core VM/Activity sin cambios según encargo.

App Android nativa `com.edu.unal.tictactoe/.MainActivity`; teléfono autorizado, mismo JBR/SDK. Instancias aisladas por tests; host Compose de prueba con ViewModel resumed en Main. Sin URL/credenciales, Playwright/Postman N/A. Riesgo alto: selección obsoleta cambia nivel/estado o notifica éxito; luego pausa indebida/duplicación O y regresión de opciones. Pruebas de caja negra con transiciones, particiones editable/no editable y decisión antes/después de apertura.

| ID | Capability / técnica | Precondición y pasos | Esperado | Método/clase |
|---|---|---|---|---|
| U37-01 | `specs/game-options`, `specs/tic-tac-toe-game`; transición | Activity real; X válida; abrir menú con O pendiente; esperar O y seleccionar About | O juega con menú aún visible, una O, turno humano, score igual; menú sigue usable y About abre | `MainActivityPendingTurnOptionsTest.openMenuDoesNotPauseResponseAndRemainsUsableAfterOneComputerMove` |
| U37-02 | `specs/game-options`, `specs/game-session`; partición editable/antes | Abrir selector en empty-X; `newGame()` en Main crea empty-O; pulsar Impossible antes de apertura | Cambio aceptado sin O inmediata; apertura usa nivel vigente, O índice 0, gate deshabilitado después | `MainActivityPendingTurnOptionsTest.acceptedDifficultyDuringEmptyOpeningAppliesToThatOpening` |
| U37-03 | `specs/game-options`; partición obsoleta/después | Selector abierto en empty-X; `newGame()` Main empty-O; esperar apertura; pulsar Hard obsoleta | Diálogo cierra; tablero/turno/resultado/starter/next/score/nivel/mute iguales al estado posterior a apertura | `MainActivityPendingTurnOptionsTest.difficultyDialogOpenedDuringOpeningCannotApplyAfterOHasMoved` |
| U37-04 | `specs/game-options`; negativo/contrato callback | Mismo escenario en host Compose con callback Toast observable; pulsar Hard después de O | Rechazo, Expert permanece, tablero/resultado/score/turno iguales y cero callbacks Toast | `GameScreenTest.staleDifficultyChoiceAfterOpeningIsRejectedWithoutToastOrStateChange` |
| U37-R1 | `specs/game-options`; regresión particiones/estado | Ejecutar 7 tests de GameScreen incluyendo el nuevo | Acciones básicas, gate activo, selección aceptada/repetida, no replay, layout básico y U37-04 pasan | `GameScreenTest` (7) |
| U37-R2 | `specs/game-options`, `specs/game-session`; regresión transiciones | Ejecutar Options: salida cancelar/confirmar, dificultad/retención/gate, terminal/nueva/score, nueva sesión y About | 6 tests pasan sin regresión de opciones/estado | `MainActivityOptionsTest` (6) |

Evidencia combinada de no Toast: Activity valida el flujo real y estado sin efectos; el host GameScreen instrumentado valida cero invocaciones del callback que en Activity dispara Toast. No se intercepta ni observa manualmente el Toast plataforma: declarar esa distinción, no afirmar una comprobación directa inexistente.

Charter: revisar diferencias entre antes/después de apertura, retención de selector/menú y cierre sin efectos. Los tiempos exactos 999/1000, 400+600 y el no reinicio por dificultad están cubiertos por JVM anterior: no duplicar una afirmación de timing exacto basada solo en el waitUntil Android de 5 s.

Ejecución prevista: comandos separados con `--offline --no-configuration-cache --rerun-tasks`; XML frescos de 3/7/6 tests, conteos no cero, estados individuales, logs redactados y exits. Evidencia nueva `evidence/run-<UTC>/`, sin tocar artefactos anteriores. JVM 108/108 previa se referencia, **no se reejecuta**. No QA global, audio/escucha/TalkBack manual ni instalaciones de herramientas, source/tests/tasks o settings; sin commits/push.

## Resultado final

U37-01–U37-04, U37-R1 y U37-R2 **PASA**. Corridas frescas: MainActivityPendingTurnOptionsTest **3/3**, GameScreenTest **7/7**, MainActivityOptionsTest **6/6**; total **16/16**, cero fallos/error/skip, todos los comandos/runner/validador exit 0. **3.7 aprobada; UI37/QA18 cerrada como PASA**, sin QA global. Evidencia y límites de Toast/timing: `report.md`.
