# QA 5.3 — integración de mute síncrono

## Requerimiento y alcance

Code review aprobado; producción idéntica a la aceptada en QA 5.2. Cambio delegado: nuevo `GameScreenSoundIntegrationTest.kt` (1 método) y checkbox 5.3. Se exige Sonido UI → GameScreen → VM → MoveSoundHost → Controller → backend fake: publicar `soundEnabled=false` y detener streams en la misma acción Main, antes de esperar recomposición. Debe conservar tablero/turno/resultado/score/iniciadores/dificultad y espera pendiente; O pendiente termina mientras mute sin reproducción; unmute no hace replay y siguiente X aceptada reproduce una vez.

Trazabilidad: `specs/game-sound-effects` — Efectos de jugadas aceptadas (mute detiene, omisión mientras deshabilitado, sin cola), Opción de sonido en la sesión; `specs/game-options` — Control de sonido y Menú/opciones no alteran espera de O. Consultados diseño §§ entrega de audio/opción Sonido y tareas 5.3/6.3; especificación de sonido actual y nuevo test completo.

Solo Android nativo. Instrumentación nueva del test de integración (1) y regresión GameMenu (6). Prohibidos todos los tasks Gradle connected-device: selección por propiedad no aísla el dispositivo en este entorno. Nada de limpieza/consulta/operación sobre AVD ajeno.

## Precondiciones

- Único target: emulator-5556 / DADM_GraphicsSound_API35 / SDK35 / arm64-v8a / boot1. Guard inmediatamente antes de cada instalación e instrumentación.
- APKs debug y test construidos offline con JBR Android Studio, `--no-configuration-cache`; instalaciones `adb -s emulator-5556 install -r`.
- Integración: Compose host aislado, VM Easy/determinista, host reanudado, ambas cargas fake completas en Main. `IndefiniteFakePlayer` mantiene streams artificialmente activos hasta stop/release; no simula expiración audible.
- Esperas de la prueba por condiciones (`waitUntil`, timeout 5s), no sleeps fijos. Teardown en Main pausa/destruye host y limpia ViewModelStore.

## Casos derivados por riesgo

I01–I05 son checkpoints del **mismo único método instrumentado**, no cinco tests independientes. Cada checkpoint depende de los anteriores. Método: `menuMuteStopsInSameMainEventPreservesTurnAndSuppressesUntilUnmuted`.

| ID | Requisito / riesgo | Técnica | Pasos | Esperado |
|---|---|---|---|---|
| I01 | Efecto X/O listo y habilitado; alto | Partición ready+enabled+foreground | Pulsar primera X en tablero; esperar respuesta O | Fake registra una X y una O, con streams activos |
| I02 | Publicación false y stop síncrono; alto | Transición enabled→mute, snapshot antes/después | Abrir menú antes de nueva X; extraer OnClick semántico real de Sonido; en un solo `runOnIdle` Main aplicar X no terminal, capturar estado y ejecutar acción Sonido | En esa misma llamada, antes de recomposición: false, stop de los 3 streams, cero activos |
| I03 | Conservación de partida/espera; alto | Invariantes + transición O pendiente | Comparar snapshots inmediatamente antes/después de mute; esperar tablero X2/O2 y turno humano | Board/turn/result/score/starter/nextStarter/difficulty/canChange idénticos tras toggle; O pendiente completa sin duplicarse |
| I04 | Sin reproducción mientras mute, sin replay; alto | Negativo + transición mute→unmute | Verificar conteos tras O mute; reabrir y pulsar Sonido | Plays siguen X2/O1 (=3); cero streams durante mute; habilitar no reproduce nada |
| I05 | Nueva jugada tras unmute; alto | Transición reenabled→accepted | Pulsar siguiente celda vacía en tablero | Un solo play X adicional (=4 total, X3/O1), un stream activo |

Regresión: cada caso usa host GameMenu Compose independiente; capability `specs/game-options`.

| ID | Requisito / técnica | Pasos | Esperado / método GameMenuTest |
|---|---|---|---|
| R01 | Exactamente cinco entradas/orden; partición | Abrir, contar descendientes click y comparar orden | 5 etiquetas correctas; `menuHasExactlyFiveEntriesInSpecifiedOrder` |
| R02 | Callback único y cierre; tabla de acciones | Abrir/pulsar cada entrada habilitada | Un callback por entrada y cierre; `eachEnabledEntryShowsItsLabelClosesMenuAndInvokesCallbackOnce` |
| R03 | Estado textual/semántico de Sonido; transición | Habilitado→Deshabilitado→Habilitado con reapertura | StateDescription/texto correctos, siempre enabled; `soundShowsEnabledAndDisabledTextAndSemanticStateAndRemainsActionable` |
| R04 | Dificultad no accionable; negativo | Tocar entrada deshabilitada | Cero callback, menú permanece; `disabledDifficultyStaysVisibleAndClickingItDoesNotInvokeOrClose` |
| R05 | Disponibilidad dinámica; transición | Cambiar canChange false→true→false | Enabled actualizado, cero callbacks; `recompositionReflectsCanChangeWithoutInvokingCallbacks` |
| R06 | Descarte menú; transición | Abrir y Atrás | Cierre y apertura aún disponible; `backDismissesMenuWithoutLeavingTheContent` |

## Evidencia y límites previstos

- Logs fresh por suite, conteo de estados 1/0, resumen `OK(N tests)`, código final del runner, JSON de validación, exits y guard. No basta adb exit0.
- Sin duplicar E2E de 5.2 ni ejecutar otras suites; charter acotado: observar diagnóstico del runner y comprobar que los siete métodos fueron ejecutados, sin crashes/setup failures, con archivos protegidos intactos.
- Este test prueba que O pendiente continúa, **no mide ni certifica deadline exacto 999/1000 ms**, ni detectaría por sí solo un reinicio del contador que también venciera dentro de 5s. Precisión/no reinicio dependen de cobertura JVM previa (131 reportados), no de esta corrida nativa.
- No prueba SoundPool/audio audible, escucha manual 6.3, TalkBack humano, MainActivity real/persistencia 5.4, otro SDK/device, ready/error/owner lifecycle global.
- Resultados y defectos se completan en `report.md` tras ejecución.
