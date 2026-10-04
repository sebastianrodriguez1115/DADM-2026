# QA — incremento 1.3: Canvas del tablero

## Alcance y base

- Requerimiento original: adaptación gradual Graphics and Sound a Kotlin/Compose; este QA valida **solo 1.3**, cuadrícula Canvas con dos verticales y dos horizontales en tercios, sin interceptar las nueve casillas, preservando UI/juego.
- Trazabilidad: `tasks.md` 1.3; `design.md` «Tablero gráfico y entrada»; capability `specs/custom-game-board`, requisitos «Tablero y marcas dibujados» (solo cuadrícula) y «Casillas accionables y accesibles» (preservación actual).
- Text/Buttons provisionales (1.4/1.5), audio, mute y espera O no se consideran defectos de este incremento.
- Leído `openspec/config.yaml` (sin reglas activas). No se encontraron AGENTS.md/CLAUDE.md en el proyecto ni sus ancestros.
- Android nativo, applicationId `com.edu.unal.tictactoe`, MainActivity, dispositivo físico autorizado por ADB. Sin URL/API/credenciales. Playwright, axe y Postman **N/A**: no validan esta UI nativa ni existe API.
- Archivos examinados: GameBoard.kt, TicTacToeScreen.kt, GameBoardRenderingTest.kt y TicTacToeScreenTest.kt. Código/test/configuración producto no se modifican.

## Casos priorizados

Precondición común: Android conectado/autorizado; JAVA_HOME de Android Studio, ANDROID_HOME del SDK indicado; APK debug y APK de pruebas del proyecto instalables. Instrumentación aislada por regla Compose. No modificar ajustes globales del teléfono.

| ID / riesgo | Requerimiento | Técnica | Pasos | Esperado | Resultado |
|---|---|---|---|---|---|
| QA13-01 / alto | 1.3, cuadrícula gráfica | Valores límite geométricos; oráculo de píxeles | Ejecutar `rendersFourThirdsGridLinesWithoutBlockingNineCellActions`; comprobar color en x/y 1/3 y 2/3 y puntos interiores de control. Complementar captura de tablero real. | Cuatro trazos visibles en tercios; puntos interiores no son cuadrícula. | PASA: render XML, board-empty.png y grid-scan-result.json |
| QA13-02 / alto | 1.3, no interceptar entrada; nueve ubicaciones | Particiones: las nueve celdas | Mismo test: tocar centro de cada celda vacía una vez. | Secuencia de callbacks exactamente 0..8, uno por toque. | PASA: render XML |
| QA13-03 / alto | 1.3, preservar semántica/layout | Particiones; contrato semántico | Ejecutar `cellsRemainSemanticallyClickable` y `rendersNineCellsAndLocalizedCellDescription`. | Clic semántico central solicita 4; nueve celdas enabled y nueve acciones, descripción localizada preservada. | PASA: render y Screen XML |
| QA13-04 / alto | Preservar juego y callbacks | Transición de estados | Suite Screen: callbacks por acción, recomposición sin eventos, flujos VM victoria X/O/empate y abandono/nueva partida. | Marcador, alternancia, apertura y estado se preservan; no eventos espontáneos/duplicados. | PASA: Screen XML |
| QA13-05 / alto | Preservar rechazo de entrada | Tabla de decisión: ocupado / turno O / terminal | Suite Screen: tocar X/O ocupadas y nueve celdas en victoria X/O/empate y turno computadora; nueva partida. | Cero callbacks de celdas deshabilitadas; nueva partida sigue disponible. | PASA: Screen XML |
| QA13-06 / medio | Preservar layout y tema | Límites; pairwise existente | Suite Screen: orden tablero/estado/score/botón; celdas cuadradas; 320×480, 360×260, fuente 2× y ambos temas. | Geometría contenida y mínimo 48 dp; scroll/controles accesibles; marcas provisionales conservadas. | PASA: Screen XML |
| QA13-07 / medio | Preservar estado/juego | Regresión complementaria, sin duplicar E2E | Ejecutar `testDebugUnitTest --rerun-tasks`. | Todas las pruebas unitarias existentes pasan. | PASA: 83/83; unit XML/log |
| QA13-08 / medio | 1.3, tablero integrado | Exploratorio guiado | Lanzar MainActivity; verificar foreground propio antes de capturar; inspeccionar tablero/celdas y, si viable, toque real ADB en celda vacía y repetir en ocupada. | Cuadrícula completa; toque aceptado y ocupado no altera tablero. | PARCIAL: captura y toque aceptado pasan; repetición ocupada bloqueada por pérdida de foco. Ver exploratory-observation.md |

## Limitaciones previstas / cobertura residual

El test instrumentado de píxeles muestrea cuatro posiciones y dos controles. Se complementó con inspección visual y script independiente sobre PNG de 1320×1320: exactamente dos segmentos del color de cuadrícula por eje central, centrados en tercios con tolerancia 1 px, y continuidad completa de los cuatro trazos. No es reconocimiento exhaustivo de líneas de colores arbitrarios. El clic semántico de render cubre el centro; las nueve acciones se verifican además en Screen. No se certifica TalkBack manual/teclado ni render de líneas por ambos temas/todos tamaños: cobertura final corresponde a 1.5/1.6/6.2. No se ejecutan pruebas web/API ficticias. El rechazo táctil ADB exploratorio quedó sin ejecutar; su regresión está cubierta por instrumentación real PASADA.

## Ejecución y evidencias

**Decisión: APPROVED para el incremento 1.3 únicamente.** No se aprueba el cambio completo ni tareas pendientes. Ningún defecto de producto reproducible encontrado.

| Ejecución independiente final | Exit | Resultado | Evidencia |
|---|---:|---|---|
| `./gradlew testDebugUnitTest --rerun-tasks --console=plain` | 0 | 83 PASA; 0 fallos/errores/omitidas; 24 tareas ejecutadas | `evidence/unit.log`, `unit-results-0.xml`…`unit-results-10.xml` |
| `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe.GameBoardRenderingTest --no-configuration-cache --console=plain` | 0 | 2 PASA; 0 fallos/errores/omitidas | `evidence/android-render.log`, `android-render-results-0.xml` |
| `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.tictactoe.TicTacToeScreenTest --no-configuration-cache --console=plain` | 0 | 15 PASA; 0 fallos/errores/omitidas | `evidence/android-screen.log`, `android-screen-results-0.xml` |
| `python3 verify-grid-png.py` | 0 | Geometría/completitud de cuatro líneas PASA | `evidence/grid-scan-result.json`, `board-empty.png` |
| `python3 explore-adb.py` | 1 | Parcial, no defecto: captura inicial + una jugada real válidas; pérdida de foco antes de captura post-jugada/repetición | `evidence/native-before.json`, `native-after.json`, `exploratory-observation.md` |

Los XML registran ejecución UTC `2026-10-02` (fecha local del encargo `2026-10-01`). `evidence/results-summary.json` contiene nombres, estado y duración de **cada** prueba ejecutada.

### Observación de ejecución (no defecto de producto)

El comando delegado con filtro de dos clases separado por coma terminó exit 0, pero solo produjo 2 tests de render, tanto con caché como con `--no-configuration-cache`. No se atribuye una causa no demostrada. Se conservan `android-initial-cache.log`/`android-initial-cache-results.xml` y `android.log`/`android-results-0.xml`. Por esa razón se ejecutaron **ambas clases por separado** y se verificaron sus conteos reales 2+15, sin inferir pases del BUILD SUCCESSFUL. El runner de QA falla si no obtiene 83/2/15 o si hay fallos/errores/omitidas.

### Trazabilidad de los 15 casos Screen (todos PASA)

- QA13-03: `rendersNineCellsAndLocalizedCellDescription`.
- QA13-04: `cellAndNewGameCallbacksAreOnlyInvokedByActions`, `recompositionDoesNotInvokeCallbacksOrChangeSnapshot`, `realViewModelUiFlowCoversHumanComputerAndTieResults`, `realViewModelUiFlowAlternatesAfterAbandonmentWithoutScoringOrDuplicateOpening`, `rendersActiveTurnStatesFromResources`, `rendersComputerTurnFromResources`, `rendersTieFromResources`, `rendersComputerResultFromResources`.
- QA13-05: `occupiedCellsKeepTheirMarksAndAreDisabled`, `terminalResultsAndComputerTurnRejectCellTouchesButAllowNewGame`.
- QA13-06: `rendersBoardStatusScoreAndNewGameInOrder`, `cellsArePositiveSquareAndStayInsideTheBoard`, `boardAndControlsAdaptToPortraitAndLandscapeConstraints`, `largeFontKeepsTerminalMarksVisibleAndControlsReachableInBothThemes`.

### Reejecución / herramientas

Desde la raíz del proyecto:

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/step-1.3/run-tests.py
# Exploratorio opcional, dispositivo sin interrupciones de foco:
python3 openspec/changes/add-compose-graphics-sound/qa/step-1.3/explore-adb.py
python3 openspec/changes/add-compose-graphics-sound/qa/step-1.3/verify-grid-png.py
```

El runner exporta JAVA_HOME/ANDROID_HOME solo para sus procesos, exige exactamente un dispositivo autorizado y elimina seriales de logs/XML. Se usaron Gradle, AndroidJUnitRunner/Compose Test, ADB, Python 3.9 estándar y `sips` ya disponibles. **Ninguna dependencia/herramienta instalada**; solo APK app/test del proyecto por instrumentación y APK debug del proyecto para captura. Ningún ajuste global del teléfono modificado. Sin modificaciones a producto/tests app/tasks, commit/push, sync/archive.
