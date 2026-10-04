# QA tarea 1.5 — tablero nativo

Requisito original: modernizar reto de gráficos/sonido a Compose. Esta aprobación se limita a tarea 1.5, capability `specs/custom-game-board`, requisito «Casillas accionables y accesibles» y no regresión básica de dibujo (1.3/1.4). Audio, mute, espera O, 1.6+ y TalkBack humano (6.3) no forman parte del alcance.

Entorno: Android físico autorizado, APK debug `com.edu.unal.tictactoe/.MainActivity`, sin URL/credenciales. No modificar ajustes globales ni otras apps. Sólo se escriben artefactos QA aquí; Gradle genera sus salidas habituales de build. Clases separadas con `--no-configuration-cache`, XML con conteos exactos y salidas redactadas.

## Casos priorizados

Todos trazan a `specs/custom-game-board`, task 1.5, salvo QA15-07 que es regresión 1.3/1.4.

| ID / riesgo | Criterio / técnica | Precondición | Pasos | Esperado | Ejecución / resultado |
|---|---|---|---|---|---|
| QA15-01 alta | 9 zonas, etiquetas, Role.Button, 48 dp, toque único; partición por posición | Tablero vacío activo humano, fixture 360 dp | Inspeccionar 9 tags/labels/roles/bounds; tocar 0..8; tocar fuera | 9 callbacks ordenados, uno por toque; ninguno fuera; medidas >=48 dp | PASS — Interaction: `exposesNineLabeledButtonHitboxesAndEachTouchRequestsOneMove`; `evidence/interaction-results-0.xml` |
| QA15-02 alta | Clic semántico; partición por mecanismo | Casilla vacía humana | `performClick` casilla 4 | Un callback índice 4 | PASS — Interaction: `availableCellClickRequestsExactlyOneMove`; mismo XML |
| QA15-03 alta | Enter y transición vacía→ocupada; transición estados | Modo Keyboard en harness, sin cambios producto | Foco en 0, Enter; verificar disabled/no OnClick; toque posterior | Un callback total; ocupada no accionable | PASS — Interaction: `availableCellAcceptsKeyboardEnterAndDisabledCellHasNoActivationAction`; mismo XML |
| QA15-04 alta | Tab salta ocupada, Enter; partición foco | 0 vacía, 1 X, 2 vacía; modo Keyboard | Foco 0, Tab, Enter | Foco 2; un callback 2; ocupada sin OnClick | PASS — Interaction: `keyboardTabSkipsOccupiedCellAndEnterActivatesTheFocusedAvailableCell`; mismo XML |
| QA15-05 alta | Turno O / terminal; tabla decisión enabled | 9 vacías, turno O activo; luego humano terminal | Inspeccionar todas; intentar toque en 9 para cada estado | Disabled sin OnClick; cero callbacks | PASS — Interaction: `computerTurnAndTerminalStateExposeDisabledCellsWithoutCallbacks`; mismo XML |
| QA15-06 alta | Integración pantalla/Activity; regresión | Clases screen/accessibility aisladas | Ejecutar suite pantalla 15 y accesibilidad 4 | Contratos/pantalla, rechazos, semántica y touch targets sin regresión | PASS — 15/15 y 4/4; `evidence/screen-results-0.xml`, `evidence/accessibility-results-0.xml` |
| QA15-07 media | Dibujo sin Buttons/Text visuales; regresión | Render claro/oscuro existente | Ejecutar rendering 3 | Cuadrícula/vectoriales intactos | PASS — 3/3; `evidence/render-results-0.xml` |
| QA15-08 alta | Árbol accesibilidad nativo; exploración guiada y transición | Activity real nueva sesión | Volcar sólo nodos propios, tocar una vacía, repetir ocupada, tocar margen fuera, avanzar hasta terminal e intentar otra activación | 9 ubicaciones con marca/vaciedad, class Button y enabled/clickable correcto; no Text X/O; ocupado/terminal/fuera no cambian tablero | PASS — exit 0; `evidence/exploratory-result.json`, `native-*.json`, `board-*.png`; terminal con 3 vacías y rechazo en las 9 |

## Límites de cobertura

No duplicar tests unitarios del motor. No Playwright/Postman/axe: aplicación Android Compose nativa; usar instrumentación Compose y ADB. No afirmar validación humana TalkBack ni teclado físico: Enter/Tab automatizados en modo Keyboard y semántica nativa. La matriz ampliada de viewports corresponde a 1.6. O es aún síncrona: el estado intermedio O se cubre con fixture, no se espera delay/audio pendientes.

Defectos reproducibles y resultados finales se documentan en `report.md`, junto a logs, XML, JSON y recortes del tablero en `evidence/`.
