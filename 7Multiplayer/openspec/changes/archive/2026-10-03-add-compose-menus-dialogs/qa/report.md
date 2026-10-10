# Informe QA — ejecución nueva en macOS y Android físico

**Resultado: PASA el alcance ejecutado**, sin defectos reproducibles de producto, sin pruebas de suite fallidas o bloqueadas. No equivale a certificar dispositivos/API ni comprobaciones no ejecutadas.

- Fecha real: **2026-10-01 19:21–19:37 UTC−05 / 2026-10-02 00:21–00:37 UTC**.
- Commit: `677af1af639376c6a54917744710cb61a784dd73`; code review previo aprobado por orquestador. Árbol inicialmente limpio; al terminar solo se añadió `qa/` (sin producto/tests/config modificados).
- Host: macOS27.0.1 (26A434); runtime JBR OpenJDK25.0.3 de Android Studio. Gradle9.6.0/AGP9.4.1, Java11 bytecode; min35/compile37/target37.
- Dispositivo: **redacted-manufacturer redacted-model, Android15/API35**, físico, único autorizado, desbloqueado; identificadores no retenidos. Compatible con min35; no se necesitó cambiar ajustes.
- Requisitos completos y plan/casos con técnicas, precondiciones, pasos, esperado y trazabilidad: `test-plan.md`. Se leyeron las 7 páginas del PDF, config, proposal/design/tasks, deltas y specs base; delta IA prevalece.

## Comandos y resultados reales

En comandos: `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'`, anteponer `$JAVA_HOME/bin` a PATH; `ANDROID_SERIAL="$DEVICE_SERIAL"` donde la variable se obtiene localmente del único dispositivo autorizado. No contiene identificadores en artefactos.

| Ejecución | Hora UTC | Exit | Resultado actual | Evidencia |
|---|---|---:|---|---|
| `./gradlew testDebugUnitTest lintDebug assembleDebug --rerun-tasks -Pandroid.builder.sdkDownload=false --console=plain` | 00:23:00–00:23:34 | 0 | **83 tests, 0 failures, 0 errors, 0 skipped**, 11 XML frescos; build/lint/assemble correctos, 51 tareas ejecutadas | `evidence/local/{execution.json,gradle.log,summary.json,cases.md,xml/,lint-results-debug.xml}` |
| `ANDROID_SERIAL="$DEVICE_SERIAL" ./gradlew connectedDebugAndroidTest --rerun-tasks -Pandroid.builder.sdkDownload=false --console=plain` | 00:23:51–00:25:30 | 0 | **59 tests, 0 failures, 0 errors, 0 skipped**, 1 XML fresco; suite COMPLETA, 68 tareas ejecutadas | `evidence/connected/{execution.json,gradle.log,summary.json,cases.md,xml/}` |
| `adb -s "$DEVICE_SERIAL" install <app-debug.apk>`; instalación APK de test de este build | 00:27 | 0/0 | Ambos paquetes inicialmente ausentes, instalados para smoke/previews, sin conflicto de firma ni datos previos | `evidence/device-preflight.json`, `evidence/smoke/install.json` |
| `adb -s "$DEVICE_SERIAL" shell am instrument -w -e class com.edu.unal.tictactoe.LauncherIconTest com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner` | 00:27:32–00:27:34 | 0 | **2 tests adicionales PASAN**; ejecución nueva para extraer previews antes de uninstall automático; no se cuentan en los59 | `evidence/icons/{execution.json,instrumentation.log,preview_*.png}` |
| Smoke adb/UI e inspección visual de capturas reales | 00:27–00:35 | Acciones finales correctas | Menú, selector, Toast real, gate, About, score/alternancia, No/Sí y nueva sesión correctos | `evidence/smoke/*.json`, `*.png`, `results.json` |

`--rerun-tasks` y validación mtime posterior al inicio garantizan ejecución nueva; no se usaron los83/59 históricos como evidencia. No se repitieron casos fallidos de producto porque no hubo ninguno. Helpers QA iniciales necesitaron correcciones (ver abajo), no del producto.

### Lint y advertencias no bloqueantes

**0 errores / 13 warnings**: RedundantLabel (`AndroidManifest.xml:17`); versión Gradle (`gradle-wrapper.properties:5`); core/Compose BOM/Kotlin más recientes (`libs.versions.toml:3,10,9`); ObsoleteSdkInt (`ui/theme/Theme.kt:44`); 7 colores sin usar (`res/values/colors.xml:3–9`). Son observaciones de mantenimiento, no incumplimientos reproducibles del requerimiento. No se actualizaron versiones.

Build empaqueta `libandroidx.graphics.path.so` sin strip. JBR25 emite warning de `sun.misc.Unsafe` usado por protobuf3.25.5 durante instrumentación; no impidió ejecutar tests. No se instalaron/actualizaron JDK, SDK ni dependencias de herramientas; resolución normal Gradle permitida, sin descargas explícitas observadas en salida.

## Matriz y observación visual propia

| Caso | Ejecutado | Pasa | Falla/bloqueado | Evidencia/cobertura |
|---|---|---|---|---|
| QA01/QA02 | Build+unitarias+suite física completa | Sí | 0 | XML actuales y casos individuales citados arriba |
| QA03 | Políticas/guards/pureza/minimax exhaustivo ambos iniciadores | Sí, unitarias | 0 | `local/cases.md` y XML dominio; no se infiere memoización estructural del black-box (code review previo) |
| QA04 | Gate vacío/activo/O/terminal, same/stale, Toast aceptación/no replay | Sí, VM+Compose+Activity; smoke parcial directo | 0 | `connected/cases.md`; capturas `smoke/03-difficulty.png`, `04-toast-change.png`, `06-toast-same.png`, `10-gate.png`; Toast **Imposible** realmente visible al cambiar y repetir nivel |
| QA05 | Nueva partida/alternancia/score/retención/nueva sesión/lifecycle | Sí | 0 | ActivityScenario actual; smoke `16-terminal.png`, `19-menu-new-o.png`, `22-retained-impossible.png`, `24-new-session-expert.png`; score O1 conservado, unaO abre y luego X vacío; nueva sesión Expert/cero |
| QA06 | Salir Back/outside/No/Sí; demás Back normal | Sí | 0 | ActivityScenario confirma DESTROYED; smoke `13-exit-still-open.png`, `14-exit-no-state.json`, `23-finish.json`; tablero/score comparados iguales tras No |
| QA07 | Menú4 iconos, radios4/current/reopen/cancel, gate real | Sí | 0 | `02-menu.png`, `03-difficulty.png`, `05-impossible-current.json`, `07-cancel-back.json`, `08b-cancel-outside-center.json`, `11-gate-reject.json`. Cancel center/outside y Back cierran; gate no abre ni cierra menú. El estado disabled está en la fila padre, no en el TextView hijo |
| QA08 | About + identidad normal/round/mono | Sí | 0 | `12-about.png`: imagen propia + autor exacto; 3 previews `icons/preview_*.png` inspeccionadas, coherentes y no plantilla |
| QA09 | Layout/accesibilidad semántica48dp, portrait/landscape real | Sí, automatizado y portrait visual | 0 | `MainActivityAccessibilityTest.realPortraitAndLandscapeKeepTopBarBoardAndControlsReachable` pasó en hardware real, restaura requestedOrientation; fuentes grandes/temas en composición local; no cambio global |
| QA10 | Timing actual main-thread O/Impossible | Sí | 0 | `connected/reto4-timing.log`, `timing-provenance.json` |

Las capturas se inspeccionaron visualmente: no recortes del tablero/autor/opciones en portrait; X verde y O roja, menú con cuatro iconos y nivel vigente seleccionado. Esto fue **interacción remota adb más inspección de capturas**, no toque manual humano. Rotación/recreación/background son pruebas automatizadas reales, no se inventa observación manual adicional.

### Timing: datos del período actual

Log UTP capturado por la suite durante `ImpossibleOpeningTimingTest`, **2026-10-01 19:24:41.350 local**; archivo original mtime posterior al inicio confirmado y procedencia retenida. Cinco muestras (ms): **105.735077, 34.017269, 24.619961, 31.829616, 27.764923**. `first_ms=105.735`, `median_ms=31.830`.

Mide `GameViewModel.newGame()` en hilo principal desde tablero vacío con VM nuevo por muestra; valida unaO en índice0, turno humano, score0, Impossible y gate cerrado. **No es first-frame, cold-start ni benchmark estadístico/SLA**. La primera muestra supera varios presupuestos de frame: riesgo residual de jank por ejecución sincrónica, no defecto contra un umbral inexistente. No se promete latencia en otros dispositivos.

## Defectos y problemas del harness

**Defectos reproducibles de producto: ninguno encontrado.** No hay severidad/archivo/línea de defecto que atribuir.

Incidencias QA corregidas sin tocar tests existentes: sanitización XML por sustitución simple generó XML inválido (se corrigió con XML escaping); `/dev/tty` no devolvió hierarchy al host (se cambió a `exec-out ... /dev/stdout`); popup Compose usa título no identificable por paquete (se comprobó paquete propietario de la ventana antes de capturar). `adb input tap 4 1500` no descartó el selector en un intento; tap inequívocamente fuera en `(720,800)` sí lo descartó y los tests existentes de outside pasaron. No se declara bug por ese intento de borde sin diagnóstico. Los Toasts iniciales se capturaron durante fade, aun legibles; el de same-level está claramente visible.

Fecha del dispositivo previa a suite quedó vacía por quoting de shell en helper; consulta post-run del ring buffer no encontró timing. **No se usó como evidencia**: se recuperaron solo las líneas Reto4Timing del log app/UTP del test actual con freshness validada. Helper quedó corregido para futuras ejecuciones; no se limpió logcat global.

## Límites y riesgos residuales

- **No probado**: TalkBack real/foco hablado (no activar ajustes globales); semántica/targets sí probados. En dump nativo el radio seleccionado aparece `checked=true/clickable=false`, aunque tap directo same-level funciona y tests Compose exponen click; compatibilidad con acciones TalkBack requiere validación dedicada, no se reporta como defecto sin reproducción.
- **No probado**: iconos en launcher real/tema monocromático aplicado/otros masks; previews nativos normal/round/monochrome sí probados. No se abrió launcher ajeno ni cambiaron iconos temáticos.
- **No probado**: process death forzado, otros dispositivos/API35–37, tablets/foldables ni benchmark frío/frame-time. Sesión nueva tras finish, recreación/rotación/background sí probadas.
- No API/auth para probar; Playwright/axe/Postman no aplican. No se instalaron herramientas; Python3/adb/Gradle/JBR ya existentes.
- Gradle connected hizo uninstall automático de los paquetes QA inicialmente ausentes. Luego se reinstalaron APK app+test para smoke/previews. **Ambos quedan instalados; actividad terminada mediante Salir/Sí**. No se desinstaló app preexistente, limpió datos ni tocaron apps ajenas. No commits/push/sync/archive.

## Reejecutar

Desde raíz del proyecto, verificar dispositivo/lock y firma si ahora hay APK instalada; no borrar ni desinstalar para resolver errores. Helpers usan el único autorizado y todos los comandos dirigidos usan `-s`/ANDROID_SERIAL:

```sh
QA=openspec/changes/add-compose-menus-dialogs/qa
PYTHONDONTWRITEBYTECODE=1 python3 "$QA/native/run-gradle.py" local
PYTHONDONTWRITEBYTECODE=1 python3 "$QA/native/retain-results.py" local
PYTHONDONTWRITEBYTECODE=1 python3 "$QA/native/run-gradle.py" connected
PYTHONDONTWRITEBYTECODE=1 python3 "$QA/native/retain-results.py" connected
```

Estos helpers sobrescriben su carpeta de resultados: conservar la evidencia previa antes de repetir. Para smoke, instalar APK propias con firma verificada si corresponde, y usar `native/ui-smoke.py launch`, `click '<etiqueta>' [captura]`, `dump <nombre>`, `capture <nombre>`, `back` y `outside <y> <x>` siguiendo QA06–QA08/steps anteriores. Coordenadas de outside solo válidas para este layout; usar bounds nuevos en otro equipo. Iconos: comando `am instrument` de tabla y leer solo `/sdcard/Android/data/com.edu.unal.tictactoe/files/icon-verification/preview_*.png` antes de uninstall automático. Sin dependencias nuevas.
