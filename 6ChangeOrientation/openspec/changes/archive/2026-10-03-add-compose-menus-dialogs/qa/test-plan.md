# QA nueva — add-compose-menus-dialogs

Inicio real: 2026-10-02 00:21 UTC (2026-10-01 local). Commit `677af1af639376c6a54917744710cb61a784dd73`; árbol inicialmente limpio. Code review aceptado por el orquestador. No se reutilizan resultados anteriores.

## Fuentes y entorno

Leídos completos: `reto4.pdf` (7 páginas), `openspec/config.yaml`, proposal/design/tasks, ambos deltas y specs base game-session/tic-tac-toe-game. Prevalece delta IA. Android nativo, sin URL/API/credenciales; Playwright, axe y Postman no aplican.
Host macOS 27.0.1 (26A434), JBR OpenJDK 25.0.3 de Android Studio; Gradle wrapper 9.6.0, AGP 9.4.1, bytecode Java 11, minSdk35/compile37/target37. Un dispositivo físico Samsung Galaxy S21 Ultra (Android15/API35), sin publicar identificadores. SDK existente: plataforma android-37.0 y build-tools36.0.0.

## Plan por riesgo

Precondición común: build del commit y dispositivo autorizado/desbloqueado compatible, sin firma incompatible; usar exclusivamente ese dispositivo. Pruebas aisladas existentes Compose/Espresso/ActivityScenario. Sin cambios globales ni borrado de datos. Resultados siguientes corresponden solo a esta ejecución nueva.

| ID/prioridad | Requerimiento/capability | Técnica | Pasos | Esperado | Resultado |
|---|---|---|---|---|---|
| QA01 alta | AC1 build; specs/tic-tac-toe-game | Regresión + particiones/límites | Ejecutar unitarias reales, lint, assemble con rerun; sumar XML actuales | Build y tests sin fallos; registrar lint | PASA: 83/83, exit0; 13 warnings lint |
| QA02 alta | AC2; todas las capabilities | Regresión instrumentada | Ejecutar suite completa real; conservar primera ejecución antes de repetir fallidos | Todos los casos pasan en dispositivo | PASA: 59/59, exit0 |
| QA03 alta | IA por nivel; specs/tic-tac-toe-game | Tabla de decisión + exhaustivo | Suites dominio: táctica/azar, terminal, pureza, minimax con ambos iniciadores y menor índice | Políticas correctas; Impossible legal/no pierde/no RNG/no mutación | PASA: XML unitarios actuales, cases.md |
| QA04 alta | Gate/Toast; specs/game-options | Transición de estados + equivalencias | Vacío/activo/O abre/terminal; same/stale; aceptación/cancel/recreate en suites VM/UI | Solo acepta vacío/terminal; Toast único en aceptación, ninguno en rechazo/cancel/lifecycle | PASA: VM/UI suites + smoke gate/Toast real |
| QA05 alta | Sesión; specs/game-session, game-options | Transición de estados | Nueva partida por menú/atajo; abandono; terminal/taps; recreate/background; sesión nueva | Alternancia, score único, retención sesión; nuevo Expert/cero/X | PASA: ActivityScenario + smoke 09–25 |
| QA06 alta | Confirmación salida; specs/game-options | Tabla de decisión | Suite y smoke: Back/outside/No/Sí; Back en menú/otros diálogos | Salir solo por botones; No preserva; Sí finish; otros descartan normal | PASA: suites y smoke 13/14/23 |
| QA07 media | Menú/dificultad/Toast real; specs/game-options | Exploratorio guiado | Smoke adb/UI nativa: cuatro iconos, cuatro radios/current, cambiar/same/cancel, jugar y gate | Render legible, cierre apropiado, Toast visible real | PASA: capturas 02/03/04/06/10 |
| QA08 media | About/identidad; specs/game-options | Inspección visual | Autor exacto/imagen; previews normal/round/mono generadas por suite nativa | Motivo propio coherente, no plantilla; aceptar no cambia sesión | PASA: About captura12; 3 previews nativos actuales |
| QA09 media | Accesibilidad/layout; specs/game-options, tic-tac-toe-game | Límites + pairwise | Suites semántica/48dp; portrait/landscape Activity restaurada; fuentes grandes/temas locales | Controles alcanzables/etiquetados; gate accesible; tablero cuadrado | PASA automatizado; portrait inspeccionado en captura; TalkBack real no probado |
| QA10 media | AC4 latencia apertura Impossible | Medición descriptiva | Retener Reto4Timing del período actual de suite; first/median y cinco muestras | Una O, turno humano; describir sin umbral ni cold-start | PASA: first105.735ms/median31.830ms; sin SLA |

Exploratorio: coherencia de ventanas, espacio visual/scroll y notificación real en el hardware actual, sin duplicar exhaustive IA en E2E. Evidencias solo de la app, sin notificaciones/seriales. Process death/TalkBack real/launcher temático no se fuerzan; documentar límites.

## Reejecución

`export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'; PATH="$JAVA_HOME/bin:$PATH" ./gradlew testDebugUnitTest lintDebug assembleDebug --rerun-tasks -Pandroid.builder.sdkDownload=false --console=plain`

`ANDROID_SERIAL="$DEVICE_SERIAL" JAVA_HOME=... PATH=... ./gradlew connectedDebugAndroidTest --rerun-tasks -Pandroid.builder.sdkDownload=false --console=plain`

`DEVICE_SERIAL` se obtiene localmente del único dispositivo autorizado, nunca se guarda en evidencia. `android.builder.sdkDownload=false` impide instalar SDK sin consentimiento. Dependencias existentes pueden descargarse normalmente; documentar si ocurre. Helper `native/run-gradle.py` conserva salida sanitizada y exit code.

## Resultados y evidencias

Informe final y límites: `report.md`. Desglose individual 83 casos: `evidence/local/cases.md`; 59 casos: `evidence/connected/cases.md`; XML y sumas actuales en esas carpetas. Cobertura IA/gate/estado se valida en la capa correspondiente, no se duplica exhaustive IA en E2E. Smoke es interacción remota adb más inspección visual de capturas propias del dispositivo; no se afirma que una persona haya tocado físicamente el teléfono. Previews: ejecución nativa adicional de 2 casos para retener activos antes de desinstalación automática de Gradle, no sustituye la suite completa.
