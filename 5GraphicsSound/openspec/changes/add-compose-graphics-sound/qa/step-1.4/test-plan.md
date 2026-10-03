# QA incremental 1.4 — plan y resultados

Alcance: tarea 1.4, `specs/custom-game-board`, requisitos «Tablero y marcas dibujados» y regresión de «Casillas accionables y accesibles». Leídos config.yaml, design.md, tasks.md y delta de capability antes de ejecutar. El requerimiento original delegado es modernizar gráficos/sonido en Compose; aquí solo se valida sustitución de marcas por vectores. Fondos Button e IA inmediata son intencionales para este incremento; 1.5+ no se certifican.

Precondiciones comunes: código aceptado en review, un Android físico autorizado, app de prueba com.edu.unal.tictactoe, JDK de Android Studio y Android SDK indicados. Sin URL ni credenciales. No producción, ajustes globales ni otras apps. Evidencia exclusivamente en esta carpeta; se omiten identificadores y se recortan capturas al tablero.

| ID / prioridad | Requisito | Técnica | Pasos | Esperado |
|---|---|---|---|---|
| QA14-01 / alta | Vector X/O; colores ambos temas; no texto | Particiones por marca y tema (2x2) | Ejecutar GameBoardRenderingTest.rendersVectorMarksWithThemeColorsAndNoTextAcrossThemes: mostrar X/O en claro, alternar oscuro, capturar píxeles/semántica | Píxeles exactos verde/rojo del tema, O hueca, ningún nodo Text X/O |
| QA14-02 / alta | Forma y escala proporcional; sin fuente | Exploratorio guiado, particiones de tamaño/fuente | Capturar tablero de Activity real después de toque; revisar dos diagonales de X y aro circular de O. Ejecutar Screen largeFont y geometría portrait/landscape | X simétrica de dos diagonales, O hueca circular, sin recorte; marcado continúa visible con fontScale=2 y tamaño compacto |
| QA14-03 / alta | Ubicación+marca, disabled conservados | Tabla de decisión: vacía/ocupada x humano/O/terminal | Ejecutar Screen localized descriptions, occupied cells, terminal/computer turn; tocar ocupado también en Activity | Nueve ubicaciones, descripciones correctas X/O, ocupadas/O/terminal disabled, cero callbacks por toque rechazado |
| QA14-04 / alta | Regresión juego/marcador | Transición de estados | Ejecutar Screen realViewModelUiFlow: victoria X → nueva → victoria O → nueva → empate; abandono y apertura | Marcador 1/1/1 sin doble score, alternancia y nueva partida intactas |
| QA14-05 / media | Cuadrícula y entrada no interceptadas | Particiones por ubicación y regresión | Ejecutar Rendering grid+touch nueve zonas y semantic click; Screen callback/recomposition | Cuatro líneas en tercios, una solicitud por toque, click centro correcto, recomposición sin callbacks |

No duplicar motor unitario: regresión mediante flujos Compose existentes. Playwright, axe y Postman no aplican a Android nativo. TalkBack manual/teclado exhaustivo, ampliación de tamaños (1.5/1.6), audio/mute/SoundPool y umbral O de 1000 ms están fuera del incremento. No se modifica fuente/tests/tasks, ni commits/push.

## Historial — primer intento (2026-10-01)

| Caso | Resultado | Evidencia |
|---|---|---|
| QA14-01 | PASA | `evidence/render-results-0.xml`: rendersVectorMarksWithThemeColorsAndNoTextAcrossThemes, claro/oscuro, colores exactos, centro O no relleno, cero Text X/O |
| QA14-02 | BLOQUEADO | No captura nativa ni Screen ejecutados: dispositivo desconectado antes de la segunda clase. Inspección de fuente confirma Canvas con dos drawLine y drawCircle/Stroke, geometría relativa a minDimension, sin dependencia tipográfica; NO sustituye observación de forma real |
| QA14-03 | BLOQUEADO | `evidence/screen.log`, instalación fallida; ninguna prueba de descriptions/disabled ejecutada independientemente |
| QA14-04 | BLOQUEADO | `evidence/screen.log`, 0 casos; regresión de juego/marcador no certificada |
| QA14-05 | PASA parcial | `evidence/render-results-0.xml`: cuatro líneas y nueve toques correctos, semantic click centro correcto. Screen callbacks/recomposition bloqueados |

GameBoardRenderingTest: **3/3 PASS**, Gradle exit **0**. TicTacToeScreenTest: **0/15 ejecutados**, Gradle exit **0** engañoso (runner falló al instalar por dispositivo ausente). Script QA exit **1**, al verificar el conteo esperado en lugar de aceptar solo el exit de Gradle. Dos comprobaciones posteriores de ADB confirmaron **0 dispositivos autorizados**. No se reintentó instrumentación/captura contra un dispositivo inexistente.

No defectos reproducibles del producto encontrados. Observación de infraestructura: desconexión de Android tras primera clase; `screen.log` muestra `adb: device 'AUTHORIZED_DEVICE' not found` y fallo de instalación. No se atribuye a la implementación. No se infiere pase a partir de resultados del Worker ni de evidencia histórica de step-1.3.

**Decisión: NO APROBADO / QA BLOQUEADO** por falta de prueba Screen mínima y captura nativa real de forma proporcional. Esto no es rechazo técnico por un defecto confirmado. Solicitar reconectar el Android autorizado; continuar Screen y exploración/captura. Riesgos residuales: geometría visual real y conservación de descriptions/disabled/juego/marcador no observados en esta sesión; ambos temas sí validados por píxeles Compose, pero no capturados para inspección manual.

Reejecución desde raíz:

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/step-1.4/run-tests.py screen
python3 openspec/changes/add-compose-graphics-sound/qa/step-1.4/explore-adb.py
```

Para ambas clases: ejecutar run-tests.py sin argumento; para render solo: argumento `render`. Siempre comandos Gradle separados con `--no-configuration-cache`, conteos 3/15 y validación de clase. Revisar manualmente `evidence/board-vector-marks.png` tras captura (todavía no existe). El script exploratorio reinicia únicamente la sesión de la app objetivo, instala únicamente el APK debug y no altera ajustes globales; requiere etiquetas españolas actuales.

Herramientas: Python3, Gradle/Compose instrumentation, ADB y sips disponibles; **ninguna herramienta ni dependencia instalada** por QA. Gradle instaló los APKs del proyecto para render; la instalación Screen falló. Sin fuente/tests/tasks modificados, sin commits/push, ni cambios a evidencia histórica. Audio/mute/espera O y 1.5+ fuera de alcance como se acordó; no se aprueba el cambio completo.

## Reintento tras reconexión — resultado vigente: APROBADO 1.4

El usuario reconectó el teléfono y autorizó continuar. Se ejecutó `QA_EVIDENCE_RUN=retry-1 python3 .../run-tests.py screen`: **15/15 PASS**, cero failures/errors/skips, Gradle exit **0** y script QA exit **0**, validando clase y conteo esperado. XML UTC 2026-10-02T02:31:18 (sesión local 2026-10-01). No se sobrescribieron logs del bloqueo ni evidencia histórica.

| Caso | Resultado vigente | Evidencia / comprobaciones |
|---|---|---|
| QA14-01 | PASA | Render inicial 3/3, `evidence/render-results-0.xml`: colores exactos en claro/oscuro, O hueca, cero Text X/O; nativo oscuro con los RGB esperados |
| QA14-02 | PASA | `evidence/retry-1/board-vector-marks.png` inspeccionada: X dos diagonales simétricas, O circular hueca. `mark-geometry-result.json`: ambas centradas, sin recorte y bounds proporcionales cuadrados. Screen largeFont fontScale=2 y portrait/landscape también PASS; Canvas depende de tamaño, no fuente |
| QA14-03 | PASA | `evidence/retry-1/screen-results-0.xml`: localized descriptions, occupied disabled, terminal y turno O rechazan toques. `native-after.json`: nueve ubicaciones, X/O disabled, vacías enabled; `native-occupied-rejected.json` idéntico tras toque ocupado |
| QA14-04 | PASA | Screen realViewModelUiFlowCoversHumanComputerAndTieResults y AlternatesAfterAbandonmentWithoutScoringOrDuplicateOpening PASS: score y alternancia preservados |
| QA14-05 | PASA | Render inicial grid+touch+semantic click PASS; Screen callbacks/recomposition PASS en reintento. Activity real toque produce una X y una O síncrona y no cambia al repetir ocupado |

Exploración: `explore-adb.py` exit **0**, instaló únicamente APK debug del proyecto, verificó foco propio antes de interacción/captura y recortó screenshots al tablero. Nueve casillas vacías iniciales; un toque en fila 1/columna 1 → una X y una O; sin Text X/O, casilla ocupada disabled y repetición rechazada sin cambio de jerarquía propia. Evidencia en `evidence/retry-1/` con `board-empty.png`, `board-vector-marks.png`, jerarquías filtradas y `exploratory-result.json`.

Verificación proporcional adicional: `verify-marks-png.py` exit **0**; Python stdlib, sin dependencias. Tablero 1320x1320 px, ambas celdas 424x424. Píxeles de color exacto X ocupan 236x236 (55.66% por eje), O 248x248 (58.49%). X dentro de dos bandas diagonales, cuatro brazos presentes y centro verde; O con centro hueco y radio de píxeles rojos 88.49..123.509 px, los cuatro puntos cardinales presentes. Ambas figuras centradas y sin tocar bordes. Paleta nativa oscura: X (128,218,149), O (255,180,171), como tema esperado. Inspección humana de imagen confirma forma, no solo color.

**Defectos encontrados: ninguno.** Bloqueo de infraestructura resuelto; no se reporta como defecto del producto. Aprobación solo de 1.4, no 1.5+ ni del cambio completo.

Limitación concreta: imagen real revisada en tema/tamaño actual, no modificación de settings globales. Otros tamaños/fontScale=2 y temas están cubiertos por tests existentes de layout/visibilidad/semántica y color; no una comparación pixel-perfect de marcas en varios tamaños. La independencia tipográfica se complementó con inspección de implementación Canvas. TalkBack manual, teclado exhaustivo y las funcionalidades futuras siguen fuera de alcance.

Reejecución sin sobrescribir historial: añadir `QA_EVIDENCE_RUN=retry-2` (o nuevo nombre) a `run-tests.py`, `explore-adb.py` y `verify-marks-png.py`, este último después de la captura; comandos completos en `report.md`. Ninguna herramienta/dependencia instalada por QA; ningún código/tests/tasks, ajustes globales u otras apps modificados.
