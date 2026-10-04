# Reporte QA parcial — 3.5–3.7

## Decisión

**APROBACIÓN PARCIAL de lógica 3.5 y del core JVM 3.6/3.7. BLOQUEO de integración/UI 3.6/3.7.** No marcar estas tareas completas ni considerar aprobado el conjunto del cambio. El test JVM cuyo nombre menciona rotación reutiliza el mismo VM: no prueba recreación Android.

## Ejecución independiente

Última corrida fresca: 2026-10-03, 12:20:36–12:20:39 UTC.

`./gradlew testDebugUnitTest --offline --no-configuration-cache --rerun-tasks --console=plain`

- Gradle y runner: **exit 0**; `testDebugUnitTest` ejecutado, no reutilizado.
- XML: **107 tests, 0 failures, 0 errors, 0 skipped**, 14 clases.
- Validador de criterios: **exit 0** para QA01–QA15; 28 métodos trazados. QA16–QA18 quedan explícitamente BLOCKED, no PASA ni skipped de JUnit.
- ADB discovery de solo lectura: **exit 0, 0 dispositivos**, 0 offline/unauthorized; sin identificadores guardados. No se ejecutó instrumentación. Ausencia de AVD/sysimages recibida como contexto; no se instalaron/crearon/iniciaron emuladores ni se cambiaron settings.

| Clase | Tests | Fallos / errores / omitidos |
|---|---:|---|
| GameViewModelLifecycleTest | 7 | 0 / 0 / 0 |
| GameViewModelDifficultyTest | 15 | 0 / 0 / 0 |
| GameViewModelSoundTest | 3 | 0 / 0 / 0 |
| GameViewModelCoroutineHarnessTest | 10 | 0 / 0 / 0 |
| GameViewModelTest | 15 | 0 / 0 / 0 |
| ImpossiblePolicyTest | 6 | 0 / 0 / 0 |
| ImpossibleExhaustiveTest | 2 | 0 / 0 / 0 |
| TicTacToeGameTest | 24 | 0 / 0 / 0 |
| DifficultyPolicyTest | 5 | 0 / 0 / 0 |
| GameModelContractsTest | 4 | 0 / 0 / 0 |
| GameUiStateDifficultyTest | 6 | 0 / 0 / 0 |
| GameUiStateTest | 6 | 0 / 0 / 0 |
| DifficultyLabelsTest | 3 | 0 / 0 / 0 |
| ExampleUnitTest | 1 | 0 / 0 / 0 |

## Evidencia y defectos

Plan, técnicas, precondiciones, pasos, resultados y capabilities: `test-plan.md`.

- Log redactado: `evidence/jvm-run.log`.
- Comando, tiempos, exit, conteos, casos y descubrimiento Android: `evidence/jvm-summary.json`.
- Resultado por criterio y método/XML: `evidence/criterion-results.json`.
- 14 XML frescos redactados: `evidence/jvm-xml/TEST-*.xml`.

**Defectos reproducibles encontrados: ninguno.** No se asigna severidad de producto al bloqueo ambiental. El fallo previo de MainActivityTest por jerarquías tras desconexión fue contexto recibido, no reproducido aquí: no se reporta como defecto ni se transforma en un pase. Tampoco se toma compilación previa de MainActivityLifecycleTest (4 tests) como ejecución; ni lint/assemble anteriores como QA independiente actual.

## Cobertura y riesgo residual

Verificados en JVM: 400+600 ms, 999/1000, sub-ms/ceil, foreground inicialmente false, callbacks lifecycle repetidos, restante cero, cancelación al deadline/nueva partida/clear, revalidación postselección por pause/generación/dificultad, rechazo obsoleto en VM, terminal/score único y preferencia mute. Regresión de políticas Impossible: 8 tests pasa; no se modificaron esas fuentes.

**Sin validar y bloqueante:** Activity real RESUMED, background/return, recreación durante apertura/respuesta, retención efectiva de VM, cierre de tarea, menú abierto sin pausa y selección obsoleta desde UI/Toast. Se necesitan dispositivo y ejecución Android tras el último fix. MainActivityLifecycleTest (4) y MainActivityTest (7), junto con cobertura aplicable de opciones, quedan pendientes; aquí ejecutados **0 Android tests**.

Fuera del alcance actual: render Canvas/colores/hit targets/teclado y UI compacta, TalkBack/manual, escucha de efectos, SoundPool/readiness/owner/release y medición Android de Impossible. No se afirma validación de audio ni accesibilidad. JVM no certifica wiring host, orden real de callbacks o comportamiento de plataforma. No se ejecutaron lint, assemble, OpenSpec validate, sync/archive ni integración a main en esta QA; se mantiene la precondición pendiente de resolver la base de menús (`add-compose-menus-dialogs`/`game-options`) antes de integración.

## Reejecución y herramientas

Desde raíz del proyecto, con JAVA_HOME apuntando al JBR de Android Studio y ANDROID_HOME al SDK local (el runner usa los valores recibidos si no se exportan):

```sh
python3 openspec/changes/add-compose-graphics-sound/qa/steps-3.5-3.7/scripts/run-jvm.py
python3 openspec/changes/add-compose-graphics-sound/qa/steps-3.5-3.7/scripts/check-criteria.py
```

Herramientas utilizadas: Python 3 estándar, Gradle wrapper/JUnit/kotlinx-coroutines-test existentes y ADB disponible. **Instalaciones: ninguna.** Scripts/evidencia únicamente dentro de esta carpeta QA; no cambios al producto/configuración/tasks ni QA anteriores, sin commits/push. La ejecución Gradle genera sus outputs normales de build; la evidencia entregada se copia/redacta en esta carpeta.
