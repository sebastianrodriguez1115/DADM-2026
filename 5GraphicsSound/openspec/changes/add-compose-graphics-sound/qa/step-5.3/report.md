# Reporte QA 5.3

**APROBADO en el alcance delegado: integración fake 1/1 y regresión GameMenu 6/6 PASS.** Sin defectos reproducibles. No constituye aprobación de escucha manual 6.3, persistencia 5.4 ni QA global.

## Plan y resultados

Plan completo (requisitos/capabilities, técnicas, precondiciones, pasos, esperados): `test-plan.md`.

Ejecución fresca UTC 2026-10-03, inicio 15:23:24, nuevo test 15:23:26 y regresión 15:23:39. Único dispositivo: **emulator-5556 / DADM_GraphicsSound_API35 / SDK35 / arm64-v8a / boot1**. Identidad comprobada inmediatamente antes de ambas instalaciones y de cada suite (`evidence/device-guards.log`).

| Caso/checkpoint | Resultado | Evidencia |
|---|---|---|
| I01 — X/O habilitados llegan al fake | PASS | `evidence/GameScreenSoundIntegrationTest.log` |
| I02 — false y stop de tres streams dentro de la misma acción Main, cero activos sin esperar recomposición | PASS | Mismo método de integración, assertions dentro de `runOnIdle` |
| I03 — snapshots de juego iguales tras toggle y O pendiente completa hasta X2/O2 | PASS | Mismo método de integración |
| I04 — O mute no agrega play; unmute no replay | PASS | Mismo método de integración |
| I05 — siguiente X aceptada añade exactamente un play/stream | PASS | Mismo método de integración |
| R01 — cinco entradas exactas/orden | PASS | `evidence/GameMenuTest.log` |
| R02 — callbacks únicos y cierre | PASS | Mismo log de regresión |
| R03 — texto/stateDescription y acción habilitada también mute | PASS | Mismo log de regresión |
| R04 — dificultad deshabilitada sin callback | PASS | Mismo log de regresión |
| R05 — disponibilidad actualizada sin callbacks espontáneos | PASS | Mismo log de regresión |
| R06 — Atrás descarta menú | PASS | Mismo log de regresión |

I01–I05 son checkpoints secuenciales de **un test instrumentado**, no cinco ejecuciones independientes. Conteo total real: **7 tests**.

- `GameScreenSoundIntegrationTest`: 1 inicio (status 1), 1 final exitoso (status 0), `OK (1 test)`, código final runner -1, sin marca de fallo; duración runner 11.16s.
- `GameMenuTest`: 6 inicios, 6 finales exitosos, `OK (6 tests)`, código final runner -1, sin marca de fallo.
- Validación automatizada de counts/métodos/códigos/resumen: `evidence/GameScreenSoundIntegrationTest-validation.json`, `evidence/GameMenuTest-validation.json`. No se tomó adb exit0 como éxito suficiente.
- Build offline `:app:assembleDebug :app:assembleDebugAndroidTest` con `--no-configuration-cache`: exit0, BUILD SUCCESSFUL (`evidence/build.log`). Ambas instalaciones exit0 y Success (`install-app.log`, `install-test.log`). Adb instrument exit0 para ambas suites. Todo en `evidence/exits.txt`.
- Hash SHA256 del APK de producto coincide exactamente con QA 5.2: `b09912b66175a81820eb6acde411c04a4ea4b710ce533e0562ee7894e0865b4b`. APK test actualizado; hashes en `evidence/apk-sha256.txt`.

## Defectos e incidente de aislamiento

**Sin defectos de producto reproducidos**; no hay severidades ni repro steps que reportar. La configuración Main del nuevo test pasó en esta ejecución final; sin fallos de setup ni crashes.

Incidente previo **informado por Orchestrator**, no generado ni reproducido por esta QA: Worker ejecutó un task Gradle connected-device contra instrucciones; AGP ignoró la selección por `-Pandroid.injected.device.serial` y alcanzó indirectamente el AVD ajeno emulator-5554/API36. **Esos resultados se excluyen como evidencia.** Selección por propiedad Gradle no aísla el comando en este entorno. Esta QA no ejecutó ningún task connected-device, ni consultó/operó/limpió el AVD ajeno o dispositivos físicos: solo `adb -s emulator-5556` protegido por identidad exacta.

## Límites y riesgos residuales

- Probado: acción semántica real del menú Sonido conectado a GameScreen/VM/host/controlador, publicación mute y stop síncronos antes de recomposición, preservación de todos los campos comparados, O pendiente termina sin sonido fake, ausencia de replay inmediato al habilitar y una reproducción fake por la siguiente X aceptada.
- El menú se abre antes de una segunda X. Esa X se aplica directamente al VM en el mismo evento Main que el **OnClick real obtenido de la UI**, aislando snapshots antes/después del toggle y evitando la ventana flaky de 1s. No se afirma un gesto físico de X con popup abierto; la primera y la siguiente X sí usan acciones del tablero renderizado.
- `IndefiniteFakePlayer` mantiene streams artificialmente activos y permite comprobar stop de forma determinista. **No hay evidencia de audio audible ni de SoundPool real** en este test; no confundir llamada fake con sonido escuchado.
- El test espera hasta 5s para O; prueba continuidad/conteo pero **no certifica deadline exacto 999/1000ms ni por sí solo ausencia de reinicio del contador**. Esa precisión se atribuye exclusivamente a cobertura JVM previa informada (131), no reejecutada ni contada como evidencia nativa nueva.
- Pendientes/fuera de esta entrega: escucha manual de efectos/mute/background y TalkBack humano de 6.3; persistencia/integración Activity real de 5.4; pruebas globales de lifecycle, owners/pools obsoletos, ready/error, otros dispositivos/SDKs. No se ejecutó OBS-01 ni suite lifecycle flaky.
- Sin capturas/traces/video adicionales: no falló ningún test y la evidencia de comportamiento son assertions nativas, logs del runner y conteos fresh; screenshots no demostrarían stop síncrono del fake.

## Reejecución y herramientas

Desde raíz:

```sh
bash openspec/changes/add-compose-graphics-sound/qa/step-5.3/scripts/run-native.sh
```

El script construye offline, verifica identidad antes de instalar/instrumentar y ejecuta separadamente las dos clases mediante AndroidJUnitRunner. Aborta ante identidad incorrecta o prueba no verificable; no usa tasks Gradle connected-device. Reejecutar sobrescribe evidencia de esta carpeta únicamente.

Herramientas instaladas: **ninguna**. Usados JBR Android Studio, Gradle/SDK/ADB/AndroidJUnitRunner existentes y Python stdlib para conteos/hashes. Sin Playwright/Postman, agentes nuevos, cambios de settings ni dependencias.

Writes QA únicamente en `qa/step-5.3/` (además de salidas generadas normales del build). Hashes antes/después de todos los archivos `app/src`, `tasks.md` y QA 5.2 coinciden: `evidence/protected-files-before.json`, `protected-files-after.json`, `protected_files_unchanged=1`. No se modificó producto/tests/tasks ni QA anterior. Sin commits/push y sin procesos QA en background en curso; AVD preexistente se deja funcionando.
