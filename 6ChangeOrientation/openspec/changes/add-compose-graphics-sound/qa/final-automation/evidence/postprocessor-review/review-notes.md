# Ajustes offline del postprocesador QA

Sin nuevas ejecuciones de producto, Gradle ni ADB. Runner seguro `run.py` intacto; no cambios a producto/tests/tasks. Reporte original y resultados131 JVM/82 native conservados; no se altera la decisión limitada AUTOMATED ni los bloqueos6.2/6.3.

Cambios:
- Docstring describe postprocesado offline con escritura de artefactos QA, sin modificar producto/dispositivos.
- Tablas nativas usan `summary.suites.passed/failed/other_results`, muestran faltantes y verificación, no infieren PASS del inventario esperado. JVM resta failures/errors/skipped de tests. Incluso una corrida fallida produce cifras verídicas antes de que el postchecker rechace su aprobación.
- Timing selecciona exactamente una línea epoch/PID `Reto4Timing` dentro del intervalo `commands.jsonl` start/finish de **esta** invocación de `ImpossibleOpeningTimingTest`. Historia fuera del intervalo se ignora; múltiples registros actuales, ausencia o ambigüedad de invocación se rechazan. No fallback «latest», margen temporal, limpieza de logcat ni consulta nueva de dispositivo. Requiere alineación de reloj host/logcat; desfase bloquea postprocesado, no se atribuye al producto.
- `initial/timing-selection.json` añade procedencia del registro actual. Sus muestras originales no cambiaron.

Validación:
- `PYTHONDONTWRITEBYTECODE=1 python3 .../scripts/test-evidence-helpers.py`: **exit0, 6/6** fixtures sintéticos offline. Historia duplicada + registro actual + uno posterior selecciona solo el actual; historia sola, duplicado actual y comandos duplicados se rechazan. Conteos sintéticos con failure/error/skip y clase no ejecutada no generan falsos PASS. **No son seis tests Android/JVM de producto adicionales.** Log: `fixture-tests.log`.
- `PYTHONDONTWRITEBYTECODE=1 python3 .../scripts/verify-evidence.py initial`: **exit0**, evidencia real anterior revalidada131/82, timing407.022/23.435ms. Log: `postcheck-initial.log`. Solo cambiaron las dos tablas para añadir columnas reales de fallos/omitidos/incompletos; se añadió timing-selection. Todo otro artefacto initial y `report.md` conservan SHA256 anterior.
- `git diff --check -- openspec/changes/add-compose-graphics-sound/qa/final-automation`: **exit0** (`git-diff-check.log`; Git solo evalúa archivos rastreados, no incorpora/stagea archivos nuevos).
- Comparación adicional con snapshot histórico detectó cambio externo en `qa/emulator-setup/README.md` (documentación actualizada de aislamiento). El primer assert de integridad global devolvió **exit1**, preservado en `integrity-check-first.json`; no fue fallo de tests ni edición de este trabajo. La comprobación final separa esa deriva documental de fuentes/tests/config: **exit0**, sin cambios de producto, `integrity-check-final.json`. No se reescribe el snapshot/resultado histórico para ocultar la deriva.

Reejecutar fixtures y postchecker con los comandos anteriores; usar etiqueta de evidencia real existente para el segundo. Herramientas instaladas: ninguna. Sin tareas/dispositivos/background/commits/stage/push. Escucha y TalkBack humanos siguen pendientes; sin aprobación QA global.
