# QA independiente — tarea 1.5

**APROBADA exclusivamente tarea 1.5.** QA15-01..08 PASS; 27/27 instrumentadas y exploración nativa exit 0. Sin defectos reproducibles de producto.

## Alcance y plan

Casos QA15-01..08 con trazabilidad, técnica, precondiciones, pasos y expectativas: `test-plan.md`. Capability `specs/custom-game-board`; aprobación exclusivamente de nueve hitboxes y su interacción/accesibilidad, con regresión de dibujo 1.3/1.4.

## Resultados instrumentados

| Clase | Casos PASS / esperados | Exit | Evidencia |
|---|---:|---:|---|
| GameBoardInteractionTest | 5/5 | 0 | `evidence/interaction.log`, `interaction-results-0.xml`, `interaction-summary.json` |
| TicTacToeScreenTest | 15/15 | 0 | `evidence/screen.log`, `screen-results-0.xml`, `screen-summary.json` |
| MainActivityAccessibilityTest | 4/4 | 0 | `evidence/accessibility.log`, `accessibility-results-0.xml`, `accessibility-summary.json` |
| GameBoardRenderingTest | 3/3 | 0 | `evidence/render.log`, `render-results-0.xml`, `render-summary.json` |

Total **27/27**, sin skips. Cada clase se ejecutó aisladamente con `--no-configuration-cache`; el runner exige conteo y classname exactos además de exit 0. QA15-01..07 PASS. Enter/Tab se verifican automatizadamente con el input mode Keyboard y solicitud de foco en el harness existente, sin añadir focusable/fallback al producto.

## Exploración nativa

**QA15-08 PASS, exit 0** (`evidence/exploratory-result.json`). Nueve etiquetas fila/columna/vacía, class `android.widget.Button`, foco y enabled/clickable diferenciados, cambio a marcas X/O, rechazo de ocupado y de toques fuera por izquierda/derecha/arriba. En la ejecución final, victoria de O con **tres casillas vacías**: todas las nueve siguen presentes, disabled/nonclickable; tocar las nueve no altera tablero ni score. Evidencia: `native-before.json`, `native-after-one-move.json`, `native-occupied-rejected-*.json`, `native-outside-*.json`, `native-progress-2.json`, `native-terminal-rejected-0..8.json` y `board-terminal.png`. Bounds reales: **113.07 dp por lado** para las nueve casillas (`evidence/native-target-sizes.json`).

Inspección visual de `board-empty.png` y `board-vector-marks.png`: cuadrícula de dos líneas verticales y dos horizontales; marcas vectoriales verde/roja, sin fondos de botones ni caracteres Text. Disponibilidad se entrega mediante enabled/disabled nativo y semántico; no se exige duplicarla en el texto de la etiqueta. Canvas decorativo no aparece como botón ni intercepta los toques.

Corrección exclusivamente del harness: primera exploración llegó a un empate válido; falló una aserción que exigía casillas vacías al terminar. Se quitó esa precondición inválida y se registra el número de vacías. Detalle: `evidence/harness-correction.md`. No es defecto del producto. Terminal con nueve casillas vacías está cubierto por QA15-05.

## Defectos y limitaciones

Sin defectos reproducibles de producto detectados. No se valida TalkBack humano: se inspeccionan las semánticas Compose y el árbol accesible nativo, no la locución/gestos del servicio. La prueba humana queda en 6.3. No teclado físico: Enter/Tab automatizados bastan para el alcance indicado.

No se prueban audio, mute de sesión, espera 999/1000 ms ni lifecycle diferido (aún pendientes); O real es actualmente síncrona y el bloqueo por turno O se comprueba con fixture. No se aprueba 1.6 ni su matriz ampliada de viewports. No se reejecutaron aquí 83 unitarias/lint/assemble: esos resultados pertenecen al code review recibido; esta evidencia no los presenta como QA independiente. Riesgos residuales: servicio TalkBack humano, otros dispositivos/input modes y tamaños compactos futuros.

## Reejecución y cambios

Con exactamente un dispositivo Android autorizado, desde raíz del proyecto:

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/step-1.5/run-tests.py
python3 openspec/changes/add-compose-graphics-sound/qa/step-1.5/explore-adb.py
```

Opcionalmente `run-tests.py interaction|screen|accessibility|render`. Los scripts usan JAVA_HOME de Android Studio y ANDROID_HOME indicados en el encargo. Ejecutar secuencialmente: la exploración instala APK debug y reinicia sólo la app para nueva sesión; no ejecutarla mientras corre instrumentación. Herramientas reutilizadas: Python 3 stdlib, Gradle, Compose Test/JUnit, Android SDK/ADB y `sips`. **No se instaló ninguna dependencia/herramienta nueva**. Playwright/Postman/axe no aplican a Android nativo. Sin cambios globales, otras apps, commits, producto, tests/tasks del proyecto ni QA histórica; seriales redactados y capturas recortadas al tablero.
