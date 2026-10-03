# Resultado QA — fase 4.5 y regresión 4.1–4.4

**APROBADA la verificación automatizada de 4.5**, con una observación de estabilidad de instrumentación. **No es aprobación integral de 6.3**: escucha humana y TalkBack siguen pendientes (usuario ausente). Menú Sonido 5.x fuera del alcance actual.

Ejecución propia: 2026-10-03, 14:25:16–14:29:39 UTC. No se reutiliza como resultado ninguna instrumentación anterior del Worker ni evidencia del AVD ajeno.

## Plan y resultados

Plan con IDs, prioridades, requisitos/capabilities, técnica, precondiciones, pasos y esperado: `test-plan.md`.

| Caso | Resultado | Evidencia (relativa a esta carpeta) |
|---|---|---|
| ENV | PASS: AVD exacto/API35/arm64-v8a/boot1 en cada guard | `evidence/device-identity.log` (11 verificaciones) |
| A01 | PASS: hashes coinciden, tonos distintos, PCM16/mono/44100/180ms, sin clipping | `evidence/assets.json`; procedencia original en `tools/audio/README.md` |
| H01–H03 | PASS, 3/3 host: ready, owner viejo, restante0 inline/no replay, mute síncrono | `evidence/jvm/TEST-com.edu.unal.tictactoe.ui.game.audio.MoveSoundHostTest.xml` |
| V01–V03 | PASS, 9/9 VM: Applied/terminal/apertura, drops/owners/fallos, pausa antes del coste stop | `evidence/jvm/TEST-com.edu.unal.tictactoe.ui.game.GameViewModelMoveSoundTest.xml` |
| C01–C03 | PASS, 11/11 controlador: condiciones, errores, stale pool/owner, idempotencia | `evidence/jvm/TEST-com.edu.unal.tictactoe.ui.game.audio.MoveSoundControllerTest.xml` |
| S01 | PASS: suite JVM completa 131/131, cero failure/error/skipped | `evidence/summary.json`, `evidence/jvm/` (17 suites) |
| N01 | PASS, 1/1 carga SoundPool; dos callbacks status0/IDs distintos | `evidence/AndroidMoveSoundPlayerLoadingTest.{log,json}` |
| N02 | PASS final, 4/4; primer intento 3/4 por precondición temporal, ver OBS-01 | `evidence/MainActivityLifecycleTest.{log,json}`; `evidence/attempt-1/` |
| N03 | PASS, 7/7 Activity/sesión/recreación/background/terminal | `evidence/MainActivityTest.{log,json}` |
| N04 | PASS, 3/3 menú no pausa/dificultad vigente u obsoleta | `evidence/MainActivityPendingTurnOptionsTest.{log,json}` |
| N05 | PASS, 15/15 regresión pantalla, semántica, tamaños, temas | `evidence/TicTacToeScreenTest.{log,json}` |
| M01 | PENDIENTE, no ejecutado | Usuario ausente; escucha/TalkBack humana 6.3 |

**Conteos:** 131 JVM frescos aprobados; pasada nativa final **30/30**. Contando también la pasada inicial detenida: 35 ejecuciones nativas = 34 aprobadas + 1 fallo de precondición del test (sin ocultarlo). IDs individuales y resultado por test en JSON/XML.

**Exits:** Gradle0 (72 tareas ejecutadas, `--rerun-tasks`), app install0, test install0; todos los `adb am instrument` retornaron0. El primer script retornó1 al detectar status-2/FAILURES pese a adb0. Repetición y resumen final retornaron0. Cada clase final tiene N status1 + N status0, `INSTRUMENTATION_RESULT` con `OK (N tests)` y `INSTRUMENTATION_CODE=-1` (éxito del runner Android; **no basta por sí solo**, también aparece en el intento fallido). Evidencia: `evidence/gradle.log`, `evidence/exits.log`, logs nativos y sus JSON. No se genera XML nativo mediante Gradle: se conservan bundles crudos y casos estructurados.

## Defectos y observaciones

**Defectos reproducibles del producto: ninguno encontrado.**

**OBS-01 — inestabilidad de precondición temporal en test existente (no defecto de producto).**
- Reproducción del intento observado: ejecutar clase `MainActivityLifecycleTest` en el AVD autorizado; `pauseSuspendsPendingResponseAndResumeCompletesItOnce` registra X, duerme400ms y solicita CREATED.
- Esperado por el harness: pausa medida entre350–800ms para disponer de un resto significativo. Obtenido inicialmente: **1012ms**, assertion en `MainActivityLifecycleTest.kt:76`, status-2. La prueba terminó antes de comprobar ausencia de O en background o restante al reanudar; este fallo no demuestra incumplimiento del producto.
- La misma clase, mismo APK/AVD y sin cambio de código/configuración pasó4/4 al repetir; no se reprodujo el fallo. JVM de límite exacto400+600 y host también pasan. No se alteró el test del repositorio (prohibido por el encargo).
- Evidencia original conservada: `evidence/attempt-1/MainActivityLifecycleTest.log` y `.json`. Riesgo residual: sensibilidad a latencia de instrumentación; recomendable estabilizar el harness en una tarea posterior autorizada, sin relajar el contrato1000ms.

## Cobertura y límites

- Fakes distinguen intento VM de llamada backendplay y verifican orden preparar/registrar/reanudar, terminal/apertura, ready/no-ready, mute, owner/pool obsoleto, stop/release y no cola/replay. **No demuestran sonido audible.** La prueba plataforma demuestra solo carga asincrónica de ambos recursos; Activity real valida lifecycle/estado sin crash.
- Preferencias y reloj de sesión se validan con JVM y recreación real. La acción de menú Sonido, mute desde UI y accesibilidad del nuevo ítem se evaluarán en5.x. No se reportan como faltantes defectuosos aquí.
- No se ejecutó escucha real, TalkBack manual, suite instrumentada completa, clases dedicadas a render/teclado/hitboxes ni lint en este paso; la regresión gráfica ejecutada fue Screen15. Corresponden a otras fases/QA integral. No se afirma validación de salida de audio del emulador ni del hardware físico.
- No sync/archive/integración a main. No se modificaron código, tests, configuración, tasks ni QA anterior. Ningún dispositivo ajeno fue operado; no instalaciones SDK/herramientas, cambios globales ni borrados. No commits/push.
- No procesos QA en background pendientes. El AVD preexistente autorizado se deja encendido; no se detuvo ningún emulador.

## Reejecución

Desde la raíz del proyecto, conservar la evidencia actual antes de sobrescribir logs:

```sh
bash openspec/changes/add-compose-graphics-sound/qa/step-4.5/scripts/run.sh
# Solo repetir instrumentación sobre los APK ya instalados:
bash openspec/changes/add-compose-graphics-sound/qa/step-4.5/scripts/run.sh --native-only
```

El script usa Gradle offline sin configuration cache únicamente para JVM/build APK; instala con adb-r y ejecuta cada clase explícitamente sobre5556, con guard inmediatamente anterior. Ante identidad distinta o fallo de resultados, aborta. `scripts/summarize.py` verifica conteos/estados/runner y WAV con biblioteca estándar Python. **Herramientas instaladas: ninguna** (Gradle wrapper, adb, runner Android y Python disponibles). Playwright/Postman N/A.
