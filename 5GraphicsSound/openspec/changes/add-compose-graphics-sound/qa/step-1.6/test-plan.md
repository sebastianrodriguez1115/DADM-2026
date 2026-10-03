# QA independiente — paso 1.6

## Alcance y entorno

Requerimiento: `specs/custom-game-board` (escala sin distorsión y nueve objetivos ≥48 dp), diseño «Tablero gráfico y entrada», tarea 1.6. Sólo responsive del tablero nativo Compose; Playwright/Postman no aplican. Dispositivo físico autorizado, sin registrar identificadores ni modificar ajustes globales. Java/SDK mediante variables de entorno temporales.

Precondiciones comunes: revisión de código aceptada, dispositivo Android conectado y autorizado, proyecto compilable. Se ejecutan las clases por separado con `--no-configuration-cache`; cada clase exige conteo exacto no nulo y resultados XML frescos.

| ID | Riesgo / técnica | Pasos | Esperado | Estado |
|---|---|---|---|---|
| R16-01 | Alto; valores límite y particiones de viewport | Ejecutar `GameBoardLayoutTest.hitTargetsStayAtLeast48DpInCompactAndResponsiveViewports`: 320×192, 320×480, 480×192, 480×320 dp, densidad 1.25 y fuente 2; alternar tema. | Cuadrado, padding horizontal ≥16 dp, nueve celdas contenidas ≥48 dp, filas/columnas alineadas; estado, marcador y nueva partida alcanzables por scroll en altura 192. | PASA — `evidence/GameBoardLayoutTest.json` |
| R16-02 | Alto; límites de redondeo | Ejecutar `GameBoardLayoutTest.fractionalDensitiesDoNotRoundHitTargetsBelow48Dp`: 1.1/1.25/1.5, viewport 320×192 dp y fuente 2. | Cada objetivo ≥ceil(48×densidad) px, geometría cuadrada y alineada. | PASA — `evidence/GameBoardLayoutTest.json` |
| R16-03 | Medio; regresión / transición de tamaño y tema | Ejecutar los 15 casos de `TicTacToeScreenTest`, incluidos constraints portrait/landscape, fuente 2 y claro/oscuro, scroll y nueva partida. | Sin regresión de layout y controles; callbacks conectados. Casos negativos de ocupación/terminal/O incluidos en la clase, sin reabrir QA 1.5. | PASA 15/15 — `evidence/TicTacToeScreenTest.json` |
| R16-04 | Alto; revisión dirigida de constraints y píxeles | Revisar helper mínimo, medición fija del tablero, padding real de celda, pesos 3×3 y assertions de layout/render. | Redondeo no reduce hitbox bajo 48 dp; identificar alcance y límites de la evidencia render. | PASA (revisión, no ejecución render) — `evidence/constraints-review.md` |

Charter exploratorio acotado: buscar falsas aprobaciones por tolerancias, clipping, densidad local versus densidad del host, geometría semántica versus dibujo y cobertura incompleta de scroll. No modificar tests del producto.

## Fuera de alcance

Audio, espera de O, tareas pendientes, QA histórica, integración/sync/archive, escucha y TalkBack humano (6.3). No se afirma matriz cartesiana viewport×densidad×tema: las clases existentes prueban particiones representativas.

## Reejecución

`python3 openspec/changes/add-compose-graphics-sound/qa/step-1.6/run-instrumentation.py`

Usa Gradle wrapper y Compose instrumentation existentes; no instala herramientas ni dependencias. Evidencia normalizada sin identificadores del dispositivo en `evidence/`.

## Resultado

**APPROVED para el alcance 1.6 delegado.** 17/17 tests instrumentados pasan, cero fallos/omitidos; exit Gradle 0 para cada clase y exit del script 0. Resultados frescos, conteos exactos >0 y hashes de fuentes revisadas sin cambios. No se identificaron defectos reproducibles. Límites y riesgos residuales en `report.md` y `evidence/constraints-review.md`; no constituye aprobación de las tareas futuras ni de QA manual 6.3.
