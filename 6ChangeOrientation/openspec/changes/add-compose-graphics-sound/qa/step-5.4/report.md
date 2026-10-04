# Reporte QA 5.4

**APROBADO exclusivamente para tarea 5.4: 20/20 tests nativos PASS.** Corrida independiente fresca, sin retries, fallos de setup ni defectos reproducibles. No es QA global ni aprobación de escucha/TalkBack humano6.3.

## Plan, resultados y evidencia

Plan con IDs, capacidades/requisitos, técnicas, precondiciones, pasos, esperados y método: `test-plan.md`.

Fecha UTC **2026-10-03, 15:43:04–15:44:42**. Único target operado: **emulator-5556 / DADM_GraphicsSound_API35 / API35 / arm64-v8a / boot1**. Guard antes de cada instalación e instrumentación; ADB directo explícitamente `-s emulator-5556`. Evidencia: `evidence/device-guards.jsonl`.

| Casos | Suite | Resultado por caso | Conteo fresh / resumen runner | Evidencia |
|---|---|---|---|---|
| S01 | MainActivitySoundOptionsTest | PASS | 1 inicio / 1 éxito; `OK (1 test)` | `evidence/MainActivitySoundOptionsTest.log` |
| O01, O02, O03, O04, O05, O06 | MainActivityOptionsTest | Cada uno PASS | 6 inicios / 6 éxitos; `OK (6 tests)` | `evidence/MainActivityOptionsTest.log` |
| A01, A02, A03, A04 | MainActivityAccessibilityTest | Cada uno PASS | 4 inicios / 4 éxitos; `OK (4 tests)` | `evidence/MainActivityAccessibilityTest.log` |
| P01, P02, P03 | MainActivityPendingTurnOptionsTest | Cada uno PASS | 3 inicios / 3 éxitos; `OK (3 tests)` | `evidence/MainActivityPendingTurnOptionsTest.log` |
| M01, M02, M03, M04, M05, M06 | GameMenuTest | Cada uno PASS | 6 inicios / 6 éxitos; `OK (6 tests)` | `evidence/GameMenuTest.log` |

**S01 cuenta como un test**, aunque incluye múltiples checkpoints. Pasaron: true inicial; UI mute con etiqueta/estado semántico accesibles; demás campos intactos; mute tras nueva partida; recreate/background con identidad de VM conservada; finish por Salir/Sí y lanzamiento con VM distinta y sonido true. Tiempo runner S01: 9.437s.

Todas las suites: códigos de caso 1 (inicio) y 0 (éxito), métodos únicos/numtests/class correctos, código final runner -1 y sin marcas de fallo. Validaciones en `evidence/<Suite>-validation.json`; resumen verificable en `evidence/summary.json` y salida en `evidence/runner.log`. No se inferió PASS solo del exit0 de ADB.

Build offline `:app:assembleDebug :app:assembleDebugAndroidTest --no-configuration-cache`: **exit0 / BUILD SUCCESSFUL**, 67 tasks UP-TO-DATE; valida APKs actuales sin afirmar recompilación forzada (`evidence/build.log`). Instalaciones app/test exit0 y Success; cinco comandos de instrumentación exit0. Comandos, timestamps y exits: `evidence/exits.jsonl`; wrapper QA exit0: `evidence/runner-exit.txt`.

Hashes instalados (`evidence/apk-sha256.json`):
- Producto: `b09912b66175a81820eb6acde411c04a4ea4b710ce533e0562ee7894e0865b4b`, idéntico al APK QA5.2/5.3.
- Tests: `b9f7874a1bbb405b6328f56434f71efa106dc13ebdf1b9f7238c2c08c702ebcc`.

## Defectos y observaciones

**Ningún defecto reproducible** encontrado; no corresponde asignar severidad ni pasos de defecto. No hubo falla/flakiness en esta ronda y no se reintentó ninguna suite. Los runs previos del Worker no se cuentan como evidencia QA fresca.

Deflake de Pending validado dinámicamente: P01 abre menú antes de aplicar X en Main y O llega con menú abierto; P02 obtiene OnClick semántico real del radio Imposible y ejecuta newGame+selección+asserts de vacío en una misma llamada Main, luego observa exactamente O en casilla0; P03 conserva rechazo UI obsoleto con snapshots completos. Estas ventanas no se interpretan como taps físicos a través del popup.

**Historia de aislamiento preservada:** según incidente informado por Orchestrator y registrado en QA5.3, Worker ejecutó anteriormente un task Gradle connected-device que ignoró selección `-Pandroid.injected.device.serial` y alcanzó indirectamente emulator-5554/API36 ajeno. Esos resultados siguen excluidos. Esta QA no ejecutó ningún task connected-device, no asumió otros mecanismos de aislamiento y no consultó/operó/limpió ese AVD ni dispositivos físicos. QA5.2/5.3 y demás QA anteriores se conservaron intactas.

## Cobertura y límites

- Cubiertos requisitos5.4: cinco entradas/orden/labels, texto y `stateDescription` de Sonido, opción siempre accionable en ambos estados; true en sesión nueva; mute conserva juego/dificultad/score; nueva partida conserva preferencia; recreate y background/resume con misma VM; finish/launch con VM nueva no restaura mute. Continuidad con menú abierto y rechazo obsoleto permanecen PASS.
- Options snapshots ahora incluyen soundEnabled, preservan score y reglas de nueva partida; Accessibility verifica orientación real portrait/landscape, controles alcanzables, targets48dp, radios/roles/labels. Accesibilidad es **semántica Compose automatizada**, no certificación humana de TalkBack.
- Persistencia mute probada mediante `scenario.recreate()`; orientación real se prueba en A01 para layout por separado, no mute durante giro físico. Background es CREATED→RESUMED. No se probó process death/restauración por sistema ni cierre forzado: contrato evaluado como sesión VM viva y finish+launch nuevo.
- Gate deshabilitado: se probaron estado Disabled y rechazo de interacción. No se certifica ausencia exhaustiva de acciones anunciadas por cada lector de pantalla; esto requiere evaluación adicional/humana.
- Las esperas nativas observan transición de O y unicidad, **no certifican deadline exacto999/1000ms**. No se reejecutaron ni atribuyeron aquí los JVM131 previos; tampoco se reejecutó fakeStop5.3 o OBS-01/lifecycle flaky.
- Fuera de alcance: escucha manual de ambos efectos, mute/background audible y TalkBack humano6.3; ready/error/owner/pool global; QA global, lint, otras APIs/ABIs/dispositivos. Un AVD/API35 en esta ronda; no se declara completion de6.3.
- Sin screenshots/traces/videos adicionales: ningún test falló; assertions y logs nativos son la evidencia de identidad VM/estado que una captura visual no demostraría.

## Reejecución, herramientas y preservación

Desde raíz del proyecto:

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/step-5.4/scripts/run-native.py
```

Script reutilizable guarda logs/validaciones, aborta ante identidad incorrecta y no reintenta automáticamente. Solo build Gradle offline y luego instalaciones/instrumentaciones ADB scoped. No sustituir por tasks connected-device.

Herramientas instaladas: **ninguna**. Usados JBR Android Studio, Gradle/SDK/ADB/AndroidJUnitRunner existentes y Python stdlib. Sin Playwright/Postman, SDK installs, nuevos agentes, dependencias ni ajustes globales.

Writes de QA solo `qa/step-5.4/`, además de salidas normales generadas del build. Hashes de `app/src` (producto/tests), tasks/config/Gradle y todas las QA anteriores coinciden antes/después (`evidence/protected-files-before.json`, `protected-files-after.json`, `protected_files_unchanged=true` en summary). Sin modificaciones de producto/tests/tasks ni commits/push. Todos los procesos QA foreground terminaron; sin trabajos QA en background pendientes. AVD preexistente se deja funcionando.
