# Reporte final de reintento — Android 3.6 y medición 3.9

## Decisión actual

**APROBADO 3.6 (host real, sin audio). APROBADO 3.9 (medición aislada Impossible).** Se levantan los bloqueos históricos QA16/QA17 por falta de dispositivo. **3.7 permanece PARCIAL/PENDIENTE**: core JVM pasa, pero no se ejecutaron los nuevos casos UI de menú abierto al vencer O ni selección obsoleta en UI/Toast (QA18). No se aprueba el cambio completo ni se modifica `tasks.md`.

El reporte `report.md`, el plan inicial y sus XML se conservan como historial de la primera QA sin dispositivo. **Este archivo es la decisión actual para 3.6/3.9.** Plan del reintento: `android-retry-plan.md` (requisitos, capabilities, técnicas, precondiciones, pasos y resultados esperados).

## Ejecución y evidencia independiente

Última corrida: **2026-10-03 12:50:22–12:52:07 UTC**, un dispositivo autorizado, SDK 35. No se guardaron seriales. Cada clase Android se ejecutó por separado con `connectedDebugAndroidTest --offline --no-configuration-cache --rerun-tasks --console=plain` y el filtro de clase correspondiente. JVM también se ejecutó fresca con esas opciones. No se toma el resultado comunicado por Worker como evidencia de esta QA.

Carpeta de evidencia definitiva relativa a esta QA:

`evidence/android-retry-20261003T125022351559Z/`

| Ejecución | Pasa / total | Fallos / errores / omitidos | Exit Gradle |
|---|---:|---|---:|
| MainActivityLifecycleTest | 4 / 4 | 0 / 0 / 0 | 0 |
| MainActivityTest | 7 / 7 | 0 / 0 / 0 | 0 |
| GameScreenTest | 6 / 6 | 0 / 0 / 0 | 0 |
| ImpossibleOpeningTimingTest | 1 / 1 | 0 / 0 / 0 | 0 |
| **Total Android** | **18 / 18** | **0 / 0 / 0** | — |
| JVM testDebugUnitTest | **108 / 108** | **0 / 0 / 0** | **0** |

Wrapper de ejecución corregido: **exit 0**. Validador independiente de XML/conteos y aritmética first/median: **exit 0**. Descubrimiento ADB: exit 0, un dispositivo. Conteos no cero, todos los casos presentes y PASA; no inferidos desde compilación o exit solamente.

Evidencia: `summary.json` registra comandos, timestamps, exits y cada método; cada subcarpeta de clase conserva `gradle.log` y `result-1.xml`. JVM conserva 14 XML (`JVM/result-*.xml`). `validation.log` registra el resultado del validador. `ImpossibleOpeningTimingTest/timing-app-tag.log` contiene únicamente la línea fresca filtrada por el tag de la aplicación `Reto4Timing` (sin logs de otras apps). Los XML/logs están redactados; el escaneo local de rutas personales/hostname sobre esta corrida no encontró datos sin redactar.

## Resultado por caso del plan

| ID | Resultado | Evidencia en carpeta definitiva |
|---|---|---|
| A36-01 / QA16 | **PASA**: pause real, cero O en background >1000 ms, restante medido al resume, una O y estado estable | `MainActivityLifecycleTest/result-1.xml`, método `pauseSuspendsPendingResponseAndResumeCompletesItOnce` |
| A36-02 / QA17 | **PASA (2 tests)**: recreate con respuesta y apertura pendientes; mismo VM, starter/next/score/dificultad/mute conservados; restante y una O | Mismo XML, `recreationDuringPendingResponseRetainsSessionWithoutDuplicatingTurn` y `recreationDuringPendingOpeningRetainsSessionAndExecutesOneOpening` |
| A36-03 / QA17 | **PASA**: finish con pendiente; VM viejo estable tras >1000 ms; nueva Activity crea otro VM vacío/score cero | Mismo XML, `finishingWithPendingTurnCancelsItAndNextActivityStartsFreshSession` |
| A36-04 | **PASA (7 tests)**: wiring de acciones, redraw sin duplicación, recreate activa/terminal y apertura, background/return, nueva sesión | `MainActivityTest/result-1.xml`; nombres y estado individual en `summary.json` |
| A36-05 | **PASA (6 tests)**: regresión menú/atajo, gate dificultad, selección aceptada/repetida, no replay Toast, viewport | `GameScreenTest/result-1.xml`; no sustituye Activity real ni UI37 |
| A39-01 | **PASA (1 test, 5 muestras)**: selección Impossible en Main, RNG fail-fast, tablero inmutable en selección, aplicación de una O fuera de ventana | `ImpossibleOpeningTimingTest/result-1.xml`, log del tag y `summary.json` |
| RJVM | **PASA (108 tests)**: regresión core/reloj/IA/estado; incluye nuevo mute conservado durante pause/resume y nueva partida | `JVM/result-*.xml`, `summary.json` y `validation.log` |
| UI37 / QA18 | **PENDIENTE, no ejecutado**: casos nuevos aún no creados de menú abierto durante vencimiento y selección obsoleta UI | Sin evidencia de pase; no usar gate básico de GameScreen ni revalidación JVM para aprobarlos |

El oráculo temporal Android valida intervalos medidos, no presupone exactamente 400 ms: foreground antes de pause/recreate debe estar entre 350 y 800 ms; restante es 1000 ms menos ese intervalo. O debe observarse tras resume dentro de `max(remaining−150 ms,0)` y `min(remaining+300 ms,950 ms)`. Estos límites toleran jitter de instrumentación y distinguen pérdida de restante/reinicio completo. No se guardaron los valores internos de esos intervalos porque los tests no los imprimen; el XML acredita las aserciones ejecutadas, no valores inventados. Los límites exactos 999/1000 y 400+600 ms se complementan con JVM virtual.

## Medición real 3.9

Cinco muestras de **solo `getComputerMove(DifficultyLevel.Impossible)` en Main**, en nanosegundos:

`187204923, 48367538, 35670231, 33546846, 30513307`

- **First: 187.205 ms** (valor exacto desde muestra: 187.204923 ms).
- **Median: 35.670 ms** (valor exacto desde muestras: 35.670231 ms).
- SDK registrado: **35**; cinco muestras comprobadas y first/median recalculados por el validador.

Inspección del test y ejecución confirman: motor nuevo vacío para cada muestra; `Looper` Main afirmado; timestamp inicial inmediatamente antes de selección y final inmediatamente después; ningún `newGame`/ViewModel/delay de 1000 ms dentro de ventana; setup, aserciones y `setMove(O, index)` fuera de ella. La selección deja nueve vacías; aplicación posterior produce una O, ocho vacías, sin X y resultado InProgress. Cualquier uso de RNG falla el test. **No hay afirmación de startup, cold start, render, rendimiento general ni cumplimiento de un umbral arbitrario.**

## Defectos, historial y límites

**Defectos reproducibles del producto encontrados: ninguno.** No hay defecto bloqueante para 3.6 o 3.9 en este alcance.

Primera corrida de reintento (`evidence/android-retry-20261003T124759114559Z/`) preservada íntegra: los comandos Gradle pasaron, pero el wrapper terminó **exit 1** y marcó falsamente FAIL al no recorrer el contenedor Android `testsuites/testsuite/testcase`. Se corrigió únicamente tooling QA y se repitieron las mismas clases; la segunda corrida arriba pasó con wrapper/validador exit 0. Véase `evidence/collector-observation.md`. No se reporta ese parser como defecto de app ni se oculta su primera evidencia.

Riesgos residuales: un dispositivo/API, sin matriz OEM/API; recreación por ActivityScenario, no prueba manual de giro físico o muerte de proceso (esta última no exige persistencia de sesión). No se ejecutó full connected en esta QA: son 18 tests seleccionados, no 73. Options6 opcional no ejecutado. 3.7 UI sigue sin evidencia específica. Assets 4.1 no validan reproducción: **no se evaluaron controlador/play/SoundPool, audio, escucha manual ni TalkBack**. Sin afirmación de aprobación de accesibilidad general, render completo o fases 4.x/5.x. No se cambiaron settings globales, otras apps ni se creó/inició emulador.

## Reejecución y herramientas

Desde raíz, con un dispositivo autorizado y JAVA_HOME/ANDROID_HOME del entorno recibido:

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/steps-3.5-3.7/scripts/run-android-retry.py
python3 openspec/changes/add-compose-graphics-sound/qa/steps-3.5-3.7/scripts/check-android-retry.py openspec/changes/add-compose-graphics-sound/qa/steps-3.5-3.7/evidence/android-retry-<UTC>
```

El runner genera carpeta nueva, no sobrescribe evidencia antigua. Python 3 estándar, Gradle wrapper/JUnit/Compose Test y ADB existentes; **instalaciones: ninguna**. Artefactos QA solo en esta carpeta; sin modificar fuente, tests de producto, tasks/configuración ni QA previas; sin commits/push. Gradle generó sus outputs normales y desplegó app/test app propios para instrumentación. Todas las ejecuciones de esta entrega finalizaron; no quedan corridas QA en background.
