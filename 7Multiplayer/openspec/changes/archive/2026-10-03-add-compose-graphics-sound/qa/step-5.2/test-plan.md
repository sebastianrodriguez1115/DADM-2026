# QA 5.2 — menú Compose de cinco entradas

## Alcance y precondiciones

Code review aprobado por Orchestrator. Requerimiento original delegado: exactamente Nueva partida, Dificultad, Sonido, Acerca de, Salir en ese orden; etiquetas/iconos de recursos; estado Sonido Habilitado/Deshabilitado visible y `stateDescription`, siempre accionable incluso mute; callback único con cierre previo; fuente `GameUiState` sin booleano de sonido recordado adicional, callback hasta VM desde MainActivity; dificultad editable en vacío/terminal y deshabilitada tras apertura O; menú sin pausa.

Trazabilidad: `specs/game-options` (Menú principal, Selección de dificultad, Accesibilidad, Control de sonido), `specs/game-sound-effects` (solo representación de opción de sesión). Consultados config.yaml, design.md, tasks.md, ambos deltas y qa/emulator-setup/README.md.

Entorno exclusivo: emulator-5556 / DADM_GraphicsSound_API35 / SDK 35 / arm64-v8a / boot 1, guardado antes de cada instalación e instrumentación. Build offline con JBR Android Studio. Cada test Compose crea contenido propio; suites ejecutadas separadamente. Sin Playwright/Postman por tratarse de Android nativo. Resultado final: **16/16 pruebas instrumentadas PASS**, más cuatro comprobaciones exploratorias PASS; detalles en `report.md`.

Precondición común **M**: host GameMenu Compose aislado. **S**: GameScreen con ViewModel controlado. **A**: Activity real nueva y VM de esa Activity. Esperas basadas en idleness/condiciones (no sleeps fijos).

## Casos priorizados

| ID | Riesgo / requerimiento | Técnica; precondición | Pasos | Esperado | Test / evidencia |
|---|---|---|---|---|---|
| M01 | Alto; opciones: cinco entradas/orden | Partición; M, dificultad habilitada | Abrir menú, contar todos los descendientes con acción click, leer etiquetas y posiciones | Exactamente 5, orden indicado, etiquetas de recursos | GameMenuTest.menuHasExactlyFiveEntriesInSpecifiedOrder |
| M02 | Alto; opciones: acción única/cierre | Tabla de decisión; M | Reabrir y pulsar cada opción habilitada | Cada callback una vez y menú cerrado | GameMenuTest.eachEnabledEntryShowsItsLabelClosesMenuAndInvokesCallbackOnce |
| M03 | Alto; opciones: Sonido + accesibilidad | Transición true→false→true; M | Abrir, comprobar texto/stateDescription, alternar; reabrir y alternar estando mute | Habilitado/Deshabilitado correspondientes; accionable en ambos, sin callback al reabrir | GameMenuTest.soundShowsEnabledAndDisabledTextAndSemanticStateAndRemainsActionable |
| M04 | Alto; opciones: dificultad deshabilitada | Negativo; M, canChange=false | Abrir e intentar toque en Dificultad | Visible/deshabilitada; cero callback y menú permanece | GameMenuTest.disabledDifficultyStaysVisibleAndClickingItDoesNotInvokeOrClose |
| M05 | Medio; opciones: actualización de disponibilidad | Transición false→true→false; M | Reconfigurar canChange con menú abierto | Disponibilidad actualizada, cero callbacks espontáneos | GameMenuTest.recompositionReflectsCanChangeWithoutInvokingCallbacks |
| M06 | Bajo; opciones: descarte | Transición abierto→cerrado; M | Abrir y Atrás | Menú cerrado, botón de apertura disponible | GameMenuTest.backDismissesMenuWithoutLeavingTheContent |
| S01 | Medio; opciones: acceso/atajo | Partición layout normal; S | Examinar botón menú, tablero y nueva partida | Menú accesible, encima de tablero, atajo visible/habilitado | GameScreenTest.topBarMenuIsAccessibleAndDoesNotCoverTheBoard |
| S02 | Alto; opciones: acción Nueva partida | Equivalencia menú/atajo; S | Nueva partida desde menú, esperar apertura O, luego atajo | Misma acción, score conservado, alternancia correcta | GameScreenTest.menuAndShortcutNewGameRunTheSameActionPreservingScore |
| S03 | Medio; opciones: selección permitida | Partición vacío; S | Seleccionar Imposible, repetir mismo nivel | VM actualizado, ventanas cerradas, un aviso por selección; tablero/score intactos | GameScreenTest.difficultyEntryUpdatesVmClosesWindowsAndToastsOnceEvenWhenRepeatingLevel |
| S04 | Alto; opciones: rechazo activo | Negativo; S con X ya aplicada | Abrir y tocar Dificultad | Deshabilitada, sin diálogo/aviso/cambio nivel | GameScreenTest.disabledDifficultyDuringActiveGameDoesNotOpenOrNotify |
| S05 | Alto; opciones: selección obsoleta | Transición vacío→O colocada; S | Abrir selector, newGame O, esperar O, elegir nivel | Rechazo sin aviso ni cambio de juego | GameScreenTest.staleDifficultyChoiceAfterOpeningIsRejectedWithoutToastOrStateChange |
| S06 | Medio; opciones: no duplicación | Transición/recreación de host Compose; S | Elegir nivel, recomponer y cambiar key del host | Aviso no repetido; dificultad conservada | GameScreenTest.hostRecompositionAndRecreationDoNotReplayTheToast |
| S07 | Medio; opciones: controles alcanzables | Límite viewport 320×360 dp; S | Mostrar menú, desplazar tablero y atajo | Tablero cuadrado, controles accesibles | GameScreenTest.boardStaysSquareAndNewGameIsReachableInsideTopBarViewport |
| A01 | Alto; opciones: menú no pausa | Transición espera→O con menú abierto; A | X, abrir menú y esperar O, abrir Acerca de | Una O, mismo score, menú usable y cerrado antes de diálogo | MainActivityPendingTurnOptionsTest.openMenuDoesNotPauseResponseAndRemainsUsableAfterOneComputerMove |
| A02 | Alto; opciones: apertura editable | Partición vacío con espera; A | Abrir selector, newGame O, elegir Imposible antes de O | Sin apertura inmediata; O usa nivel elegido y cierra edición tras colocar marca | MainActivityPendingTurnOptionsTest.acceptedDifficultyDuringEmptyOpeningAppliesToThatOpening |
| A03 | Alto; opciones: obsoleta al vencer | Negativo/transición; A | Mantener selector abierto hasta O y elegir Difícil | Ningún campo de sesión alterado | MainActivityPendingTurnOptionsTest.difficultyDialogOpenedDuringOpeningCannotApplyAfterOHasMoved |

Evidencia de cada caso: `evidence/<Suite>.log`, nombres de método y códigos fresh de inicio/final; no se considera suficiente adb exit=0.

## Charter exploratorio y límites

- Comprobar recursos, iconos y cableado de estado/callback a partir de archivos afectados, sin modificar producto. Cierre asignado antes de callback se puede corroborar estáticamente; los tests prueban cierre tras la acción, no observabilidad del popup exactamente dentro del callback.
- Exploración ejecutada sobre Activity real mediante ADB/UIAutomator; capturas y XML conservados.
- No duplicar cobertura unitaria de motor/reloj/audio ni certificar integración fakeStop (5.3), persistencia por rotación/background/nueva sesión (5.4), lifecycle OBS-01, QA global, escucha manual o TalkBack humano. Semántica Compose automatizada sí está en alcance.
- Estado terminal de Dificultad complementado por E04 sobre victoria de O; victoria humana/empate no se recorrieron en esta ronda.

## Casos exploratorios ejecutados

Precondición común: nueva Activity real, inicio del paquete propio con `am start -S -W` (solo ese paquete), sesión inicial vacía. Caja negra por XML de accesibilidad y capturas; guard antes de cada comando de dispositivo. Capability `specs/game-options`, y opción textual de `specs/game-sound-effects` para E02/E03.

| ID | Requisito / técnica | Pasos | Esperado / resultado | Evidencia |
|---|---|---|---|---|
| E01 | Cinco entradas, etiquetas/iconos, dificultad vacío; partición + observación visual | Abrir menú y comparar etiquetas en orden, inspeccionar captura | PASS: cinco etiquetas ordenadas, cinco iconos visibles, Habilitado, Dificultad enabled | `evidence/explore-sound-enabled.xml`, `evidence/menu-sound-enabled.png` |
| E02 | Sonido siempre accionable, sin reinicio; transición true→false | Pulsar Sonido, verificar cierre/tablero/score, reabrir | PASS: mismo tablero/score, menú cerrado tras acción, Deshabilitado y enabled | `evidence/explore-after-mute.xml`, `evidence/explore-sound-disabled.xml`, `evidence/menu-sound-disabled.png` |
| E03 | Sonido cableado hasta VM; transición false→true | Pulsar Sonido estando mute, reabrir | PASS: Habilitado visible de nuevo | `evidence/explore-reenabled.xml` |
| E04 | Dificultad editable terminal; partición terminal | Jugar casillas vacías disponibles hasta terminal, abrir menú y Dificultad | PASS: Victoria de la computadora; Dificultad enabled; diálogo con cuatro niveles y menú cerrado | `evidence/explore-terminal-menu.xml`, `evidence/menu-terminal.png`, `evidence/explore-terminal-difficulty-dialog.xml` |

Resultados por caso y ejecución en `report.md`; resumen exploratorio en `evidence/exploratory-results.log`.
