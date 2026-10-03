# QA 1.4 — APROBADO

Solo incremento 1.4 de marcas vectoriales, capability `specs/custom-game-board`. Reanudado tras reconexión autorizada; historial de bloqueo conservado en `report-first-attempt.md`, logs iniciales y sección histórica de `test-plan.md`.

| Validación independiente | Resultado / exit | Evidencia |
|---|---|---|
| GameBoardRenderingTest | 3/3 PASS, Gradle 0, sin fallos/skips | `evidence/render-results-0.xml`, `evidence/render.log` |
| TicTacToeScreenTest (reintento) | 15/15 PASS, Gradle y script QA 0, sin fallos/skips | `evidence/retry-1/screen-results-0.xml`, `screen.log`, `screen-summary.json` |
| Activity real / toque / ocupado | PASS, script 0 | `evidence/retry-1/exploratory-result.json`, `native-before.json`, `native-after.json`, `native-occupied-rejected.json` |
| Inspección visual + geometría PNG nativa | PASS, script 0 | `evidence/retry-1/board-vector-marks.png`, `mark-geometry-result.json` |

X observada con dos trazos diagonales verdes simétricos; O con aro rojo circular hueco. Tema oscuro nativo: RGB X=128/218/149, O=255/180/171; claro/oscuro exactos cubiertos por instrumentación. Tablero 1320x1320 px, celdas 424x424; X 236x236 y O 248x248 px de color exacto, centradas, sin recorte/distorsión. No Text X/O en casillas. Descripciones ubicación+marca, disabled/rechazo de ocupadas y terminal/O permanecen; Screen valida juego, score X/O/empate y alternancia. Fuente Canvas usa geometría relativa al tamaño, no fuentes.

**Defectos: ninguno encontrado.** Desconexión anterior fue bloqueo de entorno, no defecto del producto. El primer Screen con cero casos nunca se contabilizó como aprobado.

Cobertura residual: captura nativa inspeccionada en un tamaño/tema actual (oscuro); ambos temas y fontScale=2/tamaños compactos cubiertos por tests Compose existentes, con assertions de visibilidad/semántica/layout, no comparación píxel-a-píxel de marcas a varios tamaños. No se afirma TalkBack manual, teclado exhaustivo, SoundPool/mute ni espera O: son tareas posteriores. Fondos Button e IA inmediata intencionales. No aprueba el cambio completo.

Plan y trazabilidad por caso: `test-plan.md` (QA14-01..05 todos PASS tras reintento).

Reejecutar desde raíz usando un nombre de evidencia nuevo:

```sh
QA_EVIDENCE_RUN=retry-2 python3 openspec/changes/add-compose-graphics-sound/qa/step-1.4/run-tests.py
QA_EVIDENCE_RUN=retry-2 python3 openspec/changes/add-compose-graphics-sound/qa/step-1.4/explore-adb.py
QA_EVIDENCE_RUN=retry-2 python3 openspec/changes/add-compose-graphics-sound/qa/step-1.4/verify-marks-png.py
```

Herramientas instaladas: **ninguna**. Python stdlib, Gradle/Compose instrumentation, ADB y sips existentes. Solo APKs del proyecto instalados, sin ajustes globales ni otras apps. Capturas recortadas al tablero, sin serial/datos privados. Playwright/Postman no aplican. Solo QA step-1.4 actualizado, fuente/tests/tasks y evidencia 1.3 intactas; sin commits/push.
