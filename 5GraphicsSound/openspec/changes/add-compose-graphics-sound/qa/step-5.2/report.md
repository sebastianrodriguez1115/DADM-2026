# Reporte QA — tarea 5.2

**Resultado: APROBADO para el alcance acotado de 5.2.** Sin defectos reproducibles. No constituye QA global ni aprobación de 5.3/5.4.

## Ejecución y evidencias

Fecha UTC: 2026-10-03. Instrumentación fresca entre 14:51 y 14:53 UTC, después del review aprobado. Único dispositivo operado: `emulator-5556`, identidad `DADM_GraphicsSound_API35`, SDK 35, `arm64-v8a`, boot 1. Guard inmediatamente antes de cada instalación/suite en `evidence/device-guards.log`; exploración repite guard antes de cada comando (`evidence/exploratory-guards.log`). No se operaron emulator-5554 ni dispositivos físicos.

Plan con IDs, requisitos/capabilities, técnicas, precondiciones, pasos y esperados: `test-plan.md`.

| Casos | Suite | Resultado individual | Prueba del conteo | Evidencia |
|---|---|---|---|---|
| M01, M02, M03, M04, M05, M06 | GameMenuTest | Cada caso PASS | 6 inicios / 6 finales código 0 / `OK (6 tests)` | `evidence/GameMenuTest.log` |
| S01, S02, S03, S04, S05, S06, S07 | GameScreenTest | Cada caso PASS | 7 inicios / 7 finales código 0 / `OK (7 tests)` | `evidence/GameScreenTest.log` |
| A01, A02, A03 | MainActivityPendingTurnOptionsTest | Cada caso PASS | 3 inicios / 3 finales código 0 / `OK (3 tests)` | `evidence/MainActivityPendingTurnOptionsTest.log` |
| E01, E02, E03, E04 | Exploración Activity nativa | Cada caso PASS | Assertions XML y capturas; partida terminó con victoria O | `evidence/exploratory-run.log`, `evidence/exploratory-results.log`, XML/capturas del plan |

**Total instrumentado: 16/16; exploratorio: 4/4.** Todos los códigos de estado instrumentado de casos fueron 1 (inicio) y 0 (éxito). No se confundió adb exit 0 con éxito de pruebas.

Build `:app:assembleDebug :app:assembleDebugAndroidTest`: exit 0, `BUILD SUCCESSFUL` (mayoría UP-TO-DATE; recompilación de tests Kotlin); `evidence/build.log`. Ambas instalaciones `adb -s emulator-5556 install -r`: exit 0/Success (`install-app.log`, `install-test.log`). Adb instrument exit 0 en las tres suites; exploración exit 0. Resumen en `evidence/exits.txt`; hashes APK en `evidence/apk-sha256.txt`.

Capturas inspeccionadas: `evidence/menu-sound-enabled.png`, `menu-sound-disabled.png`, `menu-terminal.png`: cinco etiquetas en orden, cinco iconos, texto de estado legible y atajo Nueva partida visible. La verificación `stateDescription` de ambas variantes viene de M03, no del screenshot.

Corroboración estática de contratos en archivos afectados (complemento, no prueba dinámica):
- `GameMenu.kt`: cinco `DropdownMenuItem` con `stringResource`/`painterResource`; sonido no usa `enabled=soundEnabled`; `expanded=false` precede cada callback. El único booleano recordado es expansión del menú, no preferencia de sonido.
- `GameScreen.kt:42–46`: pasa `uiState.soundEnabled` y callback; `MainActivity.kt:41` conecta `gameViewModel::toggleSound`. Exploración E02/E03 confirma cambio visible en Activity real.
- `GameUiState.kt:33–34`: dificultad derivada de vacío o resultado terminal. E04 confirma el terminal O real, A02/A03 ventana de apertura y obsolescencia.

## Defectos y observaciones

**Ningún defecto reproducible de producto** en el alcance ejecutado; sin severidades/steps de defecto que reportar.

Incidentes del arnés, corregidos antes de concluir: `adb shell uiautomator dump /dev/tty` no devolvió XML y fue reemplazado por `exec-out`; un wrapper zsh usó la variable reservada `status` y falló después de completar exploración. Se corrigió a `rc` y se reejecutó la exploración completa con exit 0. No son defectos del producto y no se contó aquella salida del wrapper como PASS. Evidencia exploratoria final corresponde a la reejecución corregida.

## Cobertura y riesgos residuales

- Cubierto: cinco entradas exactas/orden, etiquetas de recursos/iconos, estados textuales y `stateDescription`, Sonido accionable mute/unmute, callback único, cierre observado, disponibilidad dinámica de Dificultad, rechazo de selección obsoleta, menú abierto sin pausar O, atajo y layout compacto. En la Activity real, mute preservó tablero y score.
- El orden exacto *asignar cierre antes de callback* se corroboró en código; la suite verifica cierre tras la interacción y cierre antes de mostrar diálogo, no una medición del popup dentro del callback durante la animación.
- Accesibilidad automatizada Compose cubierta para labels/estado habilitado/deshabilitado y `stateDescription`; no se ejecutó navegación humana TalkBack ni certificación exhaustiva de acciones expuestas por lector de pantalla. No hay TalkBack humano disponible en el encargo.
- Dificultad terminal se probó con victoria de O, no victoria de X/empate en esta ronda. Sound toggling se ejercitó sobre partida vacía; no se amplió a integración fakeStop/espera en 5.3.
- **Fuera de alcance deliberadamente:** 5.3 detención síncrona real/fake de streams y sonido activo, 5.4 persistencia por nueva partida/rotación/background/nueva sesión; escucha humana/audio manual; suite global, lint, JVM rerun. Los 131 JVM informados por Worker no se presentan como ejecución QA nueva.
- No se ejecutó `MainActivityLifecycleTest` ni OBS-01 de QA 4.5; su flakiness previo no se reevalúa aquí.
- Un AVD/API/ABI y tema claro en la exploración. Otros dispositivos, SDKs, tamaños de fuente/temas del menú no quedan certificados por estas pruebas.

## Reejecución y cambios realizados

Desde raíz del proyecto:

```sh
bash openspec/changes/add-compose-graphics-sound/qa/step-5.2/scripts/run-native.sh
python3 openspec/changes/add-compose-graphics-sound/qa/step-5.2/scripts/explore-menu.py
```

La segunda orden fuerza nuevo inicio únicamente del paquete del juego autorizado (`am start -S`) y usa stdlib Python/XML + ADB; recrea evidencia exploratoria. Ambos scripts abortan ante identidad incorrecta. No usar Gradle connected-device tasks como sustituto sin validar filtro de dispositivo.

Herramientas instaladas: **ninguna**. Usados JBR/Gradle/SDK/ADB/UIAutomator/AndroidJUnitRunner existentes y Python stdlib. Sin Playwright/Postman ni nuevas dependencias. Solo se crearon/editaron artefactos en `qa/step-5.2/`; build produjo sus salidas normales generadas. Producto, tests, tasks, configuración y QA anteriores sin modificaciones; sin commits/push. Sin procesos QA en background al finalizar; el AVD preexistente se deja funcionando.
