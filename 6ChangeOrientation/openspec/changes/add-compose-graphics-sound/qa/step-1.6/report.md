# Resultado: APPROVED — sólo responsive 1.6

Validación independiente en Android físico autorizado, clases separadas, `--no-configuration-cache`, sin ajustes globales ni herramientas instaladas.

| Caso | Resultado | Tests | Exit Gradle | Evidencia |
|---|---|---:|---:|---|
| R16-01 / R16-02 | PASA | `GameBoardLayoutTest` 2/2 | 0 | `evidence/GameBoardLayoutTest.json`, `.log` |
| R16-03 | PASA | `TicTacToeScreenTest` 15/15 | 0 | `evidence/TicTacToeScreenTest.json`, `.log` |
| R16-04 | PASA, revisión estática dirigida | N/A | N/A | `evidence/constraints-review.md` |

Total **17/17**, cero fallos y omitidos. Conteos exactos no nulos provenientes de XML nuevos de cada ejecución. Exit del script **0**. Consolidado y hashes antes/después: `evidence/summary.json`; fuentes/tests revisados sin cambios. El log guarda sólo marcadores de tareas/build, y JSON conserva resultados por método sin nombres/identificadores del dispositivo.

## Defectos

Ningún defecto reproducible encontrado dentro del alcance. No se solicitan cambios de producto.

## Cobertura y límites reales

- Fuente 2; cuatro viewports a densidad 1.25; densidades 1.1/1.25/1.5 en 320×192. Tablero cuadrado/padding, nueve interiores ≥48 dp, contención y alineación; scroll de controles en altura 192. La suite de pantalla aporta controles/callbacks y claro-oscuro con fuente 2.
- Se ejecutan viewports locales Compose, **no** cambios de resolución, orientación o fuente del dispositivo. La matriz no es cartesiana; no se afirma haber probado cada densidad en cada viewport y tema.
- Se revisaron constraints y las assertions de píxeles de `GameBoardRenderingTest`; esa clase no se reejecutó. No hay evidencia nueva de render pixel-perfect de X/O para la matriz compacta. La comprobación de líneas por muestras no es una inspección exhaustiva de la imagen.
- Bounds sin clipping validan tamaño del contenido desplazable; `performScrollTo` prueba alcanzabilidad programática, no gesto humano. No se exige que tablero y todos los controles sean simultáneamente visibles a 192 dp de alto.
- TalkBack humano (6.3), audio/escucha y espera de O no están validados aquí. QA 1.5 previamente aprobado no se reabre. Tampoco se reejecutaron unit/lint/assemble ni la suite completa: los resultados del Worker no se presentan como ejecución independiente de esta sesión.
- Riesgo residual: otras densidades/ancho extremo, combinaciones no ensayadas y render compacto exhaustivo; no se observaron indicios reproducibles de fallo en el alcance autorizado.

## Plan y reejecución

Casos, técnica, precondiciones, pasos, esperado y trazabilidad a `specs/custom-game-board`: `test-plan.md`.

Desde la raíz del proyecto:

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/step-1.6/run-instrumentation.py
```

El script usa sólo Gradle wrapper, Python estándar y Compose instrumentation ya disponibles, con JAVA_HOME/ANDROID_HOME temporales. No agrega dependencias, no modifica configuración del producto, no guarda secretos/seriales y no hace commits/push. Los únicos artefactos authored de esta sesión están en esta carpeta; Gradle regeneró sus salidas habituales de build.
