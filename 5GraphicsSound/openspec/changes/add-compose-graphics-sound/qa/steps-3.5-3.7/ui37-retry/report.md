# Reporte final — cierre QA 3.7 UI37 / QA18

## Decisión

**APROBADA la tarea 3.7 en el alcance delegado (core previamente validado + UI pendiente ahora validada). UI37/QA18 queda PASA.** No se declara QA global del cambio ni aprobación de audio/accesibilidad manual. Este reporte supersede únicamente el estado pendiente de 3.7 UI en los reportes anteriores; toda evidencia y reportes históricos permanecen intactos.

## Ejecución independiente terminada

2026-10-03, **13:06:22–13:07:44 UTC**. Un teléfono autorizado; descubrimiento ADB exit 0, sin identificadores guardados. Tres comandos separados y frescos, cada uno con:

`./gradlew connectedDebugAndroidTest --offline --no-configuration-cache --rerun-tasks --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe.<clase>`

| Clase | Pasa / total XML | Fallos / errores / omitidos | Exit Gradle |
|---|---:|---|---:|
| MainActivityPendingTurnOptionsTest | **3 / 3** | 0 / 0 / 0 | **0** |
| GameScreenTest | **7 / 7** | 0 / 0 / 0 | **0** |
| MainActivityOptionsTest | **6 / 6** | 0 / 0 / 0 | **0** |
| **Total ejecutado en esta QA** | **16 / 16** | **0 / 0 / 0** | — |

Runner **exit 0**; validador de XML/conteos/casos/redacción **exit 0**. La ejecución fue foreground y finalizó antes del reporte: **sin corridas pendientes en background**. No se usa el pase informado por Worker como evidencia de esta QA.

**JVM NO reejecutada aquí.** Se referencia la corrida independiente anterior de **108/108**, core sin modificaciones según encargo: `../evidence/android-retry-20261003T125022351559Z/JVM/`, su `summary.json` y `../android-retry-report.md`. No se suma esa corrida al total actual ni se presenta como fresca de este reintento.

## Plan, resultados y evidencia

Plan con IDs, capacidades, técnicas, precondiciones, pasos y resultado esperado: `test-plan.md`.

Carpeta definitiva relativa a este reporte:

`evidence/run-20261003T130622058684Z/`

| Caso | Resultado | Evidencia / método |
|---|---|---|
| U37-01 | **PASA**: O ocurre con menú aún visible; una O, turno humano, score conservado; About sigue usable | `MainActivityPendingTurnOptionsTest/result-1.xml`, `openMenuDoesNotPauseResponseAndRemainsUsableAfterOneComputerMove` |
| U37-02 | **PASA**: selector abierto en empty-X; nueva O iniciada en Main; Impossible aceptada antes de apertura, sin marca inmediata; apertura O índice 0 con nivel vigente | Mismo XML, `acceptedDifficultyDuringEmptyOpeningAppliesToThatOpening` |
| U37-03 | **PASA**: O vence mientras selector permanece abierto; elección Hard obsoleta se rechaza y diálogo cierra; todos los campos de estado comparados permanecen iguales | Mismo XML, `difficultyDialogOpenedDuringOpeningCannotApplyAfterOHasMoved` |
| U37-04 | **PASA**: host Compose con callback observable; Hard obsoleta no cambia nivel/board/result/score/turn y emite **cero callbacks Toast** | `GameScreenTest/result-1.xml`, `staleDifficultyChoiceAfterOpeningIsRejectedWithoutToastOrStateChange` |
| U37-R1 | **PASA (7)**: nueva/atajo, gate activo, selección aceptada/repetida, no replay, layout básico y U37-04 | `GameScreenTest/result-1.xml`; U37-04 está incluido en estos 7, no se cuenta dos veces |
| U37-R2 | **PASA (6)**: opciones de salida, gate/nivel/sesión, terminal/nueva/score, About | `MainActivityOptionsTest/result-1.xml` |

`summary.json` conserva comando exacto, timestamps, exits, conteos y los 16 nombres/estados individuales. Cada clase tiene `gradle.log` redactado y XML fresco. `validation.log` registra cross-check de casos, conteos y redacción con exit 0. El colector usa recorrido recursivo `testcase`, corregido en QA previa; aquí no se produjo el falso fallo de parser. El historial de ese incidente permanece en `../evidence/collector-observation.md` y su corrida original, sin sobrescribirlos.

## Evidencia combinada de no Toast y límites

El test de **Activity real** comprueba el flujo obsoleto y ausencia de cambios de dificultad/estado, pero **no intercepta el Toast Android**. El test instrumentado de **GameScreen** comprueba que ese mismo flujo no invoca `onShowToast`, mediante callback observable. Son evidencias complementarias de rechazo sin notificación de éxito; **no se afirma observación manual ni interceptación directa de un Toast plataforma en MainActivity**.

Los selectores se abren sobre empty-X y se introduce la nueva apertura mediante `newGame()` en Main para conservar el diálogo abierto: no se describe falsamente ese setup como una acción Nueva partida del menú. El caso de menú abierto demuestra que O no queda pausado por el popup, pero su `waitUntil` de 5 s no mide un deadline exacto; 999/1000, restante y ausencia de reinicio al cambiar dificultad siguen respaldados por la JVM anterior del mismo core. Charter exploratorio: inspección del escenario antes/después de O, gate obsoleto y callback de notificación; sin afirmar sesiones manuales adicionales.

**Defectos reproducibles encontrados: ninguno.** Sin bloqueantes para 3.7 en los casos solicitados.

Riesgos residuales: un dispositivo/API, sin matriz OEM ni nuevas pruebas de performance, render completo, TalkBack/manual o escucha. **Audio/controlador/SoundPool y QA global no evaluados**. No se reejecutaron Lifecycle/ImpossibleTiming ni full connected en esta entrega; sus aprobaciones anteriores conservan su evidencia propia. No cambios a settings globales, otras apps, fuente/tests/tasks ni evidencias pasadas. No instalaciones manuales de herramientas ni emuladores; Gradle realizó el despliegue propio habitual de app/test app para la instrumentación solicitada. Sin commits/push.

## Reejecución y herramientas

Desde raíz, con JAVA_HOME/ANDROID_HOME del entorno recibido y un teléfono autorizado:

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/steps-3.5-3.7/ui37-retry/scripts/run.py
python3 openspec/changes/add-compose-graphics-sound/qa/steps-3.5-3.7/ui37-retry/scripts/check.py openspec/changes/add-compose-graphics-sound/qa/steps-3.5-3.7/ui37-retry/evidence/run-<UTC>
```

Cada ejecución crea carpeta nueva. Herramientas existentes: Python 3 estándar, Gradle wrapper, Compose Test/JUnit y ADB. **Herramientas instaladas: ninguna.** Todos los artefactos de esta entrega están dentro de `ui37-retry/` en la carpeta QA autorizada.
