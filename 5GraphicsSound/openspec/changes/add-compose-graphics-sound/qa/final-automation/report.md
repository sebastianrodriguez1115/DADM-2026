# QA final — resultado automatizado

**AUTOMATED PASSED / aprobado únicamente el alcance automatizado.**

- **131/131 JVM**, 17 clases, **0 failures/errors/skips**; ejecución fresca sin build/configuration cache, `testDebugUnitTest` efectivamente ejecutado (24 tareas ejecutadas).
- **82/82 Android instrumentados**, **20/20 clases**, **82 IDs únicos**, 0 fallos/ignored/incompletos. Inventario previo completo; ninguna exclusión, incluidos lifecycle, claro/oscuro, compacto/densidad/fontScale, Enter/Tab, opciones y audio.
- **6.2 BLOCKED para el comando literal `./gradlew connectedDebugAndroidTest`**: prohibido por falta de aislamiento demostrado en este entorno. Su alcance de instrumentación sí pasó mediante runner ADB seguro, clase por clase. No se marca la tarea.
- **6.3 BLOCKED**: usuario ausente; escucha de ambos efectos, mute/background audible y TalkBack humanos **no ejecutados ni aprobados**. **No hay aprobación global del cambio ni cierre de QA manual.**

## Ejecución y evidencia

UTC 2026-10-03 **15:56:38–16:04:31**. Único target: **emulator-5556 / DADM_GraphicsSound_API35 / SDK35 / arm64-v8a / boot1**. Se verificaron 44 guards (preflight + 43 operaciones ADB), todos correctos. Nunca se consultaron ni operaron el AVD ajeno o dispositivos físicos. Sin tasks connected*, settings globales ni cambios a dependencias.

Plan con IDs, capabilities, técnicas, precondiciones, pasos y esperados: `test-plan.md`. Inventario por método previo a ejecución: `evidence/initial/inventory.json`.

| Caso | Resultado | Evidencia fresca en `evidence/initial/` |
|---|---|---|
| F01 entorno/integridad | PASS | `device-guards.jsonl`, `isolation-check.json`, `protected-before.json`, `protected-after.json`, `summary.json` |
| F02 reloj/cancelación/revalidación | PASS | `jvm-summary.json`, XML `GameViewModelCoroutineHarnessTest`, `GameViewModelLifecycleTest`, `GameViewModelDifficultyTest`, `GameViewModelTest` en `jvm-xml/` |
| F03 audio fake/controlador/host/plataforma | PASS automatizado | XML `GameViewModelMoveSoundTest`, `MoveSoundControllerTest`, `MoveSoundHostTest`; `GameScreenSoundIntegrationTest.log`, `AndroidMoveSoundPlayerLoadingTest.log` |
| F04 reglas/IA/score/alternancia | PASS | XML motor/Impossible y VM; `TicTacToeScreenTest.log`, `MainActivityTest.log`; `motor-baseline-integrity.json` |
| F05 gráficos/temas/compacto | PASS | `GameBoardRenderingTest.log`, `GameBoardLayoutTest.log`, `TicTacToeScreenTest.log` |
| F06 entrada/accesibilidad automática | PASS | `GameBoardInteractionTest.log`, `MainActivityAccessibilityTest.log` |
| F07 lifecycle real/sesión | PASS **4/4 + 7/7** | `MainActivityLifecycleTest.log`, `MainActivityTest.log` y sus `*-validation.json` |
| F08 menú/sonido/sesión/diálogos | PASS | `GameMenuTest`, `GameScreenTest`, `MainActivityOptionsTest`, `MainActivitySoundOptionsTest`, `MainActivityPendingTurnOptionsTest`, `AboutDialogTest`, `DifficultyDialogTest`, `ExitConfirmationDialogTest`: logs/validaciones por clase |
| F09 timing Impossible | PASS **1/1, cinco muestras** | `ImpossibleOpeningTimingTest.log`, `impossible-timing-logcat.log`, `impossible-timing.json` |
| F10 regresión restante | PASS | `ExampleInstrumentedTest.log`, `LauncherIconTest.log`, XML ExampleUnitTest; inventario y resumen completos |
| M01 escucha/TalkBack | **BLOCKED / no ejecutado** | Usuario ausente; sin evidencia humana, no se simula aprobación |

Desglose exacto sin duplicar conteos: `evidence/initial/jvm-counts.md` (17 clases) y `native-counts.md` (20 clases). Checkpoints de tests compound, cinco muestras de timing y métodos compartidos por varios criterios **no se cuentan como tests adicionales**. Los 73 native históricos anteriores a las ampliaciones y los 20 de QA5.4 no se suman a esta ejecución: el total actual único es **82**.

Por **cada clase nativa**, validación de identidad clase/métodos/numtests, starts y finales únicos, status1/status0, `OK(N tests)`, código runner **-1**, ausencia de marcadores de fallo; ADB exit0 no fue el único oráculo. Todos los logs y `*-validation.json` están preservados. Comandos/timestamps/exits: `commands.jsonl`; resumen: `summary.json`; runner **exit0**: `runner.log`, `runner-exit.txt`. JVM contiene XML frescos por método y conteos exactos, no resultados informados del Worker.

Build de ambos APK **exit0**, instalaciones **Success/exit0**; `build-apks.log`, `install-app.log`, `install-test.log`, `apk-sha256.json`. APK producto SHA256 `a2822931c7f12c27ce0fe67cec2f139b420d47a98ec47a4ce9784ba69cb3554a`; test `b9f7874a1bbb405b6328f56434f71efa106dc13ebdf1b9f7238c2c08c702ebcc`. Lint0err/14warnings de 6.1 es contexto del Worker, **no lint reejecutado por esta QA**.

## Timing y límites del oráculo

Impossible en Main del **AVD API35**, muestras de selección: **407021625, 44876208, 23435000, 22518542, 20329125 ns**. **First 407.022 ms; median 23.435 ms**. Selección aislada: excluye setup/aplicación y espera1000ms; test verifica una O por muestra y cero RNG. No hay assertion de presupuesto de rendimiento; estos números no certifican fluidez/cold-start, latencia de primer frame ni hardware físico.

**999/1000 ms y exactamente400+600 ms son evidencia JVM con reloj/scheduler virtual alineados.** Native lifecycle usa intervalo foreground **medido** y ventanas/jitter; valida restante y unicidad, no precisión de un milisegundo por ADB. Los cuatro lifecycle tests actuales pasan en el primer intento; no se dispone del intervalo exacto de cada pase porque el test solo lo imprime si falla.

## Defectos e historial de fallos

**Ningún defecto de producto reproducido. Esta tanda: 0 tests fallidos, 0 reintentos, 0 exclusiones.** No hay severidades/pasos de defecto que asignar.

- **OBS-01 histórica, test temporal:** QA4.5 observó `pauseSuspendsPendingResponseAndResumeCompletesItOnce` con **1012ms** fuera de precondición350..800 (`MainActivityLifecycleTest.kt:76`, status-2), antes de validar el restante. Evidencia conservada en `../step-4.5/evidence/attempt-1/MainActivityLifecycleTest.log` y `.json`; luego pasó sin cambios. **No se reprodujo ahora**; no se declara corregida la sensibilidad del harness. Esta QA no omitió la clase ni reintentó hasta verde.
- Fallos históricos de sincronización de Screen en QA3.2 se conservaron y fueron resueltos en la fase previa; Screen actual pasa15/15. No se reciclan esos pases como resultado nuevo.
- Incidente de aislamiento QA5.3: un task connected del Worker operó indirectamente AVD ajeno al ignorar selección por propiedad. Esos resultados siguen excluidos; esta QA usa solo ADB `-s` con guard. Ninguna limpieza/consulta de aquel AVD.
- Postcheck local: primer `git show` usó ruta sin prefijo del módulo y falló; se corrigió **el comando QA**, no producto/tests. Verificación final contra3119001 pasó para **12 archivos** (motor/modelos/políticas y ImpossiblePolicy/Exhaustive). No hubo repetición de tests por ello.

## Cobertura residual y preservación

Cobertura automatizada completa de los métodos actuales, no exhaustividad de todos los dispositivos/lectores/eventos posibles. SoundPool real prueba carga de ambos recursos y ausencia de crash; fakes prueban intentos/backend play/stop/owner/pool/readiness/errors/no replay. **Nada de esto demuestra sonido audible**, audio de hardware o TalkBack humano. Background instrumentado es CREATED→RESUMED; recreate conserva VM. Orientación real/layout se prueba por separado; no process death/restauración persistente ni teclado físico. UI pending tests usan ventanas Main/acciones semánticas reales para evitar transporte >1s, no taps físicos a través de popups. Compacto/densidades/temas son particiones representativas, no matriz cartesiana universal. Riesgos residuales: OBS-01 bajo latencia alta, escucha/accesibilidad humana pendiente y plataformas distintas a API35/arm64.

Hashes antes/después iguales para fuentes/tests/config/specs/QA histórica; motor y los dos tests Impossible iguales a3119001. tasks/documentación integration-status se excluyeron del guard por escritura6.4 paralela autorizada; **esta QA no los editó**. Únicas escrituras propias: esta carpeta QA y salidas generadas normales Gradle/runner. Sin commits/stage/push/sync/archive; integración base game-options depende del workflow futuro, reportado por6.4. Todos los procesos QA foreground finalizaron; sin trabajos QA background pendientes. AVD preexistente se deja funcionando.

## Reejecución y herramientas

Desde raíz del módulo, usar una etiqueta inédita para no sobrescribir historial:

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/final-automation/scripts/run.py nueva-etiqueta
python3 openspec/changes/add-compose-graphics-sound/qa/final-automation/scripts/verify-evidence.py nueva-etiqueta
```

No sustituir por connected*. Runner inventaría todas las clases actuales, fuerza JVM fresco, construye ambos APK y ejecuta cada clase con ADB guardado; conserva primeros fallos y continúa las clases independientes. Herramientas instaladas: **ninguna**. Python stdlib, JBR Studio, Gradle wrapper, SDK/ADB y AndroidJUnitRunner existentes. Playwright/Postman/axe web N/A.
