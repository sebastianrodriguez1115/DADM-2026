# QA 5.4 — opción Sonido en Activity y sesión

## Requerimiento, alcance y entorno

Code review aprobado. Usuario original: modernizar PDF a Compose manteniendo estado de sesión, marcador y delay de IA en primer plano. Requerimientos concretos de tarea 5.4: cinco entradas etiquetadas, Sonido textual y accesible; true en VM/sesión nueva; toggle UI conserva demás campos; mute persiste en nueva partida, recreate con misma VM y background/resume; terminar Activity y lanzar con VM nueva vuelve a true. Menú abierto no pausa O y una dificultad obsoleta se rechaza.

Cambios delegados: `MainActivitySoundOptionsTest` nuevo (1 test compound), `MainActivityOptionsTest` (6: snapshots incluyen soundEnabled, fresh true), `MainActivityAccessibilityTest` (4: cinco entradas/stateDescription), PendingTurnOptions (3: deflake por ventanas Main únicas y acción semántica real). Producción sin cambios desde QA5.2; GameMenu (6) como regresión.

Capacities: **S** = `specs/game-sound-effects` / Opción de sonido en la sesión. **O** = `specs/game-options` / Menú principal, Selección de dificultad, Accesibilidad de opciones, Control de sonido. Leídos config.yaml, diseño Sonido/verificación, tasks5.4/6.3, specs y tests actuales; guard según qa/emulator-setup.

Exclusivamente `emulator-5556`, `DADM_GraphicsSound_API35`, API35, `arm64-v8a`, boot1. Guard antes de cada operación ADB (además de las consultas de identidad del guard). Builds offline `assembleDebug`/`assembleDebugAndroidTest` con JBR/SDK habituales. Prohibidos todos los tasks Gradle connected-device. No otras operaciones/dispositivos, dependencias, agentes ni modificaciones de source/tests/tasks/QA previa.

Precondición **A**: ActivityScenario real nueva y VM recuperada de Activity, cleanup al finalizar. **M**: GameMenu Compose host aislado. Por defecto sesión soundEnabled=true. Casos prioritarios por impacto: S01 persistencia, P01–P03 continuidad/obsolescencia, luego opciones/accesibilidad/regresión. Orden de ejecución por suites independiente de IDs; no retry automático.

## Casos: IDs, trazabilidad, técnica, precondiciones, pasos y esperados

| ID | Cap. / técnica; precondición | Pasos | Esperado | Método (suite) |
|---|---|---|---|---|
| S01 | S+O / transición de estados e invariantes; A | Consultar true; X y esperar O; toggle UI a mute; nueva partida; recreate; CREATED→RESUMED; Salir/Sí hasta DESTROYED; lanzar de nuevo | Texto/stateDescription coherentes; campos de juego intactos al mutear; mute retenido en nueva partida/recreate/background; misma VM en sesión, VM distinta y true tras finish/launch | `muteIsAccessibleAndSurvivesNewGameRecreationAndBackgroundButNotNewSession` (MainActivitySoundOptionsTest) |
| O01 | O / tabla decisión y negativo; A con X/O activos | Salir, Atrás, toque fuera, No | Diálogo no descartado por Back/fuera; No conserva VM y snapshot incluido sonido | `exitDialogBlocksBackAndOutsideThenNoPreservesTheActiveSession` (MainActivityOptionsTest) |
| O02 | O / transición; A | Salir/Sí | Activity DESTROYED | `exitDialogConfirmYesFinishesTheRealActivity` (Options) |
| O03 | O+S / transición; A | Elegir Imposible vacío, X/O, recreate/background, abrir menú y Atrás | Misma VM/snapshot, dificultad deshabilitada activa, sonido conservado | `impossibleSelectionAndGateSurviveRecreationBackgroundAndMenuBack` (Options) |
| O04 | O+S / partición terminal vs vacío/activo; A | Jugar terminal, Hard, nueva partida menú (O) y atajo (X) | Score único conservado, iniciadores alternan, Hard retenida, gates correctos | `terminalGameEnablesDifficultyAndKeepsNewGameRulesAndScore` (Options) |
| O05 | O+S / transición a sesión nueva; A | Imposible, jugar, Salir/Sí, lanzar | VM nueva, Expert, sound=true, board vacío, score0, starterX | `newSessionAfterExitStartsInExpertWithoutPersistence` (Options) |
| O06 | O+S / invariantes; A partida activa | Abrir/cerrar Acerca de | Autor/imagen visibles; snapshot incluido sonido intacto, RESUMED | `aboutDialogDuringAnActiveGameShowsTheAuthorAndKeepsTheSnapshot` (Options) |
| A01 | O / partición orientación, límite48dp; A | Forzar portrait y landscape reales, desplazar controles | Layout cuadrado, menú no cubre tablero, controles alcanzables y targets mínimos | `realPortraitAndLandscapeKeepTopBarBoardAndControlsReachable` (MainActivityAccessibilityTest) |
| A02 | O+S / tabla disponibilidad; A | X, abrir menú gate false; dos nuevas partidas y abrir gate true | Cinco entradas etiquetadas, Sonido accionable/stateDescription Habilitado, Dificultad estado correcto, targets48dp | `menuEntriesExposeLabelsAndClickActionsMatchingTheDifficultyGate` (Accessibility) |
| A03 | O / partición radios, cancelación negativa; A vacío | Abrir dificultad, revisar 4 radios, Atrás | Roles/labels/selección Expert, targets mínimos, cancelación no cambia nivel | `difficultyDialogOptionsExposeRadioSemanticsAndCancelKeepsTheValue` (Accessibility) |
| A04 | O / contrato semántico; A | Acerca de y Salir/No | Labels/roles/targets botones correctos; actividad RESUMED | `aboutAndExitDialogButtonsExposeLabelsRolesAndTouchTargets` (Accessibility) |
| P01 | O / transición espera con menú abierto; A | Abrir menú ANTES de X; aplicar X en onActivity, assert0O en misma llamada, esperar | Una O durante menú abierto, turno humano, mismo score; Acerca de usable | `openMenuDoesNotPauseResponseAndRemainsUsableAfterOneComputerMove` (MainActivityPendingTurnOptionsTest) |
| P02 | O / partición vacío, ventana Main; A selector abierto | Obtener OnClick REAL radio Imposible; misma onActivity: newGame O, assertvacío/gate, acción semántica, assertvacío; esperar | Nivel Imposible aceptado sin O inmediata; después exactamente O0 y gate false | `acceptedDifficultyDuringEmptyOpeningAppliesToThatOpening` (Pending) |
| P03 | O+S / negativo obsoleto; A selector abierto | newGame O, esperar O; intentar Hard desde UI antigua | Sin cambios de dificultad/board/result/turn/score/iniciadores/sound | `difficultyDialogOpenedDuringOpeningCannotApplyAfterOHasMoved` (Pending) |
| M01 | O / partición y orden; M | Abrir y contar todos los descendientes click | Exactamente5 y orden Nueva partida, Dificultad, Sonido, Acerca de, Salir | `menuHasExactlyFiveEntriesInSpecifiedOrder` (GameMenuTest) |
| M02 | O / tabla de acciones; M habilitado | Pulsar cada entrada con reapertura | Callback único y cierre | `eachEnabledEntryShowsItsLabelClosesMenuAndInvokesCallbackOnce` (GameMenu) |
| M03 | O+S / transición true→false→true; M | Alternar Sonido, verificar/reabrir y volver a alternar | Texto y stateDescription en ambos estados; siempre accionable, no callbacks por reapertura | `soundShowsEnabledAndDisabledTextAndSemanticStateAndRemainsActionable` (GameMenu) |
| M04 | O / negativo; M gatefalse | Intentar Dificultad | Visible/deshabilitada, cero callback, menú permanece | `disabledDifficultyStaysVisibleAndClickingItDoesNotInvokeOrClose` (GameMenu) |
| M05 | O / transición; M | Gatefalse→true→false con menú abierto | Enabled actualizado sin callbacks | `recompositionReflectsCanChangeWithoutInvokingCallbacks` (GameMenu) |
| M06 | O / transición; M | Abrir y Atrás | Menú cerrado y botón apertura disponible | `backDismissesMenuWithoutLeavingTheContent` (GameMenu) |

**S01 es un solo test compound** con múltiples checkpoints, no tests extra. Total previsto: 1+6+4+3+6=20.

## Validación, charter y límites

- Evidencia fresh por suite: estados1/0, métodos únicos/numtests/class correctos, `OK(N tests)`, código final runner-1, ausencia de fallo y exits. Nunca inferir PASS solo de adb exit0.
- Charter diagnóstico acotado: verificar runner/setup, aislamiento AVD y archivos protegidos; si hay fallo no editar tests/producto, no ocultar por reintento; reportar evidencia y clasificar solo con causa sustentada.
- S01 usa recreate, no giro físico para persistencia; A01 verifica orientación real/layout por separado. Background CREATED→RESUMED, no process death. Nueva sesión significa finish+VM nueva, no limpieza de app/datos.
- P01/P02 usan VM en Main con menú/selector real preabierto y OnClick semántico del radio real para evitar cruzar la ventana1s; no representan gesto físico de tablero a través de popup. La apertura tardía se espera por condiciones, no certifica reloj999/1000 exacto.
- No repetir JVM131, fakeStop5.3 ni QA global. Sin escucha/TalkBack humano6.3; accesibilidad automatizada Compose únicamente. No se declara ausencia exhaustiva de acción semántica en entrada deshabilitada: asserts verifican Disabled y rechazo; otras plataformas/lectores no certificados.
- Resultados finales y repro/severidades si aplica en `report.md`; evidencia y scripts exclusivamente aquí. Historia QA5.2/5.3 intacta.
