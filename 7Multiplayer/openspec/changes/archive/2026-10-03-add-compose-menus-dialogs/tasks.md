## 1. Dominio e IA por dificultad

- [x] 1.1 Crear `DifficultyLevel` (`Easy`, `Hard`, `Expert`, `Impossible`) en `com.edu.unal.tictactoe.game.models` como única definición de niveles; verificar con test unitario de constantes y orden estable de nombres.
- [x] 1.2 Cambiar la firma a `getComputerMove(difficulty: DifficultyLevel = DifficultyLevel.Expert)` conservando los guards terminales (`Won` -> `GameAlreadyFinished`, `Tie` -> `NoMoves`) y sin estado de dificultad en el dominio; verificar con test que omitir el argumento aplica Expert y que los tests actuales de `TicTacToeGameTest` siguen pasando.
- [x] 1.3 Implementar la política `Easy` (casilla libre aleatoria, sin priorizar victoria ni bloqueo); verificar con randomizador controlado que consulta al azar incluso con táctica disponible y que no muta el tablero.
- [x] 1.4 Implementar la política `Hard` (victoria inmediata si existe, si no aleatoria, sin bloquear deliberadamente); verificar con tests de "victoria antes de azar" y de "amenaza sin victoria usa el randomizador respetando el índice".
- [x] 1.5 Añadir regresiones de `Expert` (ganar > bloquear > aleatorio, amenazas múltiples) y casos terminal/`NoMoves` que en ningún nivel invocan `MoveRandomizer`; verificar que la consulta no aplica movimiento ni altera el tablero.
- [x] 1.6 Implementar `Impossible` con minimax recursivo explícito: raíz O que simula cada candidata de O y pasa X al evaluar, alternando jugador sin inferirlo por conteo de marcas; terminal desde la perspectiva de O (`+1`/`0`/`-1`), O maximiza y X minimiza, enumeración por índice creciente y mejor solo si el valor es estrictamente mayor (empate = menor índice); sin `MoveRandomizer`, sin mutar `cells` ni snapshots y conservando los guards terminales; verificar con tests dirigidos de victoria forzable, empate y no mutación.
- [x] 1.7 Añadir memoización LOCAL por consulta para Impossible con clave (estado de tablero de prueba, jugador) y valor de score final, creada y descartada dentro de la consulta, sin cache global ni inferencia de turno; verificar con tests dirigidos de horquilla defendible, posición aislada perdida, determinismo ante consulta repetida y guards terminales.
- [x] 1.8 Añadir test exhaustivo de Impossible que recorra todas las respuestas humanas legales desde el tablero vacío con AMBOS iniciadores, verificando que ningún terminal sea victoria de X y que cada respuesta sea legal; el harness vive en el test (replay del historial con turno explícito o copia de tablero) sin motor adicional ni reutilizar el minimax de producción como oráculo.

## 2. Estado de sesión y ViewModel

- [x] 2.1 Añadir `val difficulty: DifficultyLevel = DifficultyLevel.Expert` y el predicado derivado `canChangeDifficulty` (resultado no `InProgress` o tablero vacío) a `GameUiState` (clase normal, sin `data class`, sin `.copy()` y sin flag mutable independiente); verificar con test de valor por defecto, inmutabilidad del snapshot y evaluación del predicado.
- [x] 2.2 Hacer que `GameViewModel` use `_uiState.value.difficulty` como fuente canónica (sin `var` mutable adicional) y la pase a todas las consultas, incluida la apertura de O; verificar con test que la siguiente consulta usa el nivel vigente.
- [x] 2.3 Implementar `setDifficulty(level): Boolean` usando el snapshot vigente: con `true` acepta y publica SOLO el cambio de dificultad sin alterar tablero, resultado, marcador, turno, iniciador ni movimiento; con `false` rechaza la partida activa (incluido el mismo nivel o una solicitud obsoleta) sin publicar ni cambiar nada. El VM no emite Toast en ningún caso (corresponde al host, tarea 3.5); verificar con test unitario básico de aceptación y rechazo.
- [x] 2.4 Conservar la dificultad en `newGame()` junto con el marcador y la alternancia de sesión: con X iniciador el tablero queda vacío en turno humano normal y Dificultad habilitada; con O la apertura automática usa el nivel vigente y solo Dificultad queda bloqueada; verificar que una sesión nueva (VM nuevo) parte en `Expert`.
- [x] 2.5 Verificar que todas las publicaciones de estado posteriores a jugadas y transiciones conservan la dificultad elegida (sin reset accidental al construir `GameUiState`); verificar con tests de `GameViewModelTest`. La rotación/background y la nueva instancia se cubren en 5.3.
- [x] 2.6 Añadir tests unitarios edge/defensivos que separen aceptación y rechazo de `setDifficulty`: mismo nivel en ventana válida devuelve `true` y publica; el mismo nivel en partida activa devuelve `false`, y un evento obsoleto también `false`, comprobando el retorno y que estado, score y tablero permanecen sin cambios.

## 3. Recursos, menú y diálogos Compose

- [x] 3.1 Añadir strings en español para las cuatro entradas, los labels `Fácil`/`Difícil`/`Experto`/`Imposible`, el texto del Toast, Acerca de, autor exacto `Juan Sebastian Rodriguez` y botones de salida; verificar que compilan y que los labels coinciden con las etiquetas visibles del enum en inglés.
- [x] 3.2 Implementar `GameMenu` con las cuatro entradas (Nueva partida, Dificultad, Acerca de, Salir), iconos y `stringResource`, cerrando el menú antes de ejecutar la acción o abrir el diálogo SOLO al seleccionar una entrada habilitada; una entrada deshabilitada no ejecuta ni cierra el menú por una selección rechazada. Usar `enabled = uiState.canChangeDifficulty` en Dificultad (deshabilitada visible y accesible que no abre diálogo ni dispara callback); verificar con test Compose de las cuatro entradas, del cierre previo solo en habilitadas y del estado deshabilitado.
- [x] 3.3 Integrar el menú en el `topBar` de `Scaffold` en `MainActivity` sin romper viewport, scroll ni `padding`, conservando el botón con tag `tic_tac_toe_new_game`; verificar con test Compose de layout y de que el botón sigue visible y accionable.
- [x] 3.4 Implementar `DifficultyDialog` con los cuatro radios ordenados y el nivel vigente al abrir, selección solo en ventana válida mediante callback `(DifficultyLevel) -> Boolean`, cierre automático y cancelación por Atrás/toque fuera sin cambiar la dificultad; verificar con tests Compose de apertura, reapertura con `Imposible`, cancelación y rechazo sin cambio.
- [x] 3.5 Emitir el Toast en el host solo tras aceptación (`true`), exactamente una vez por pulsación e incluso al elegir el nivel vigente, vía `onShowToast` inyectado (sin parámetro composable por defecto); con `false` cerrar el diálogo obsoleto sin Toast ni cambio de estado; verificar con test Compose que el callback de selección se captura, que no se reemite al recomponer/recrear/cancelar y que el render real de Android queda como verificación manual.
- [x] 3.6 Implementar `ExitConfirmationDialog` ("No" cierra conservando estado, "Sí" invoca `onExit`, no descartable por Atrás ni toque fuera); verificar con tests Compose de botones y no descarte.

## 4. Identidad visual y About

- [x] 4.1 Implementar `AboutDialog` con la imagen vectorial propia del tres en raya, el autor exacto `Juan Sebastian Rodriguez` y botón aceptar que cierra sin cambios; verificar con test Compose de imagen, autor y cierre.
- [x] 4.2 Reemplazar foreground/background/`monochrome` del launcher adaptativo y los WebP por densidad (normal y redonda) por activos propios coherentes con About; verificar por inspección de recursos y manualmente que no queda la plantilla de Android.

## 5. Integración de la Activity y pruebas instrumentadas

- [x] 5.1 Cablear en `MainActivity` los callbacks de menú (`onNewGame`, `onDifficultySelected: (DifficultyLevel) -> Boolean`, `onShowToast` real con contexto capturado en composición y `onExit = { finish() }`) sin cast de `Context` a `Activity`; verificar con prueba instrumentada de humo.
- [x] 5.2 Con `ActivityScenario` verificar el flujo de Salir: "No" cierra y preserva la partida, "Sí" finaliza la actividad real, el diálogo no se descarta por Atrás ni por toque fuera, y Atrás descarta menú/diálogos distintos de Salir sin interceptación global.
- [x] 5.3 Con `ActivityScenario` verificar lifecycle real para dificultad y gate derivado: recreación y regreso de segundo plano conservan dificultad y `canChangeDifficulty`, una instancia nueva inicia en `Expert`, y la apertura de O deja Dificultad bloqueada; comprobar que recreación/background no desbloquean la selección durante una partida activa y que tras el terminal vuelve a ser editable.
- [x] 5.4 Verificar accesibilidad y layout: etiquetas textuales de menú y diálogos, alcanzabilidad de controles incluida la entrada deshabilitada, y viewport en vertical/landscape con el `topBar`; verificar con pruebas Compose de accesibilidad y layout.

## 6. Verificación final

- [x] 6.1 Ejecutar `./gradlew testDebugUnitTest lintDebug assembleDebug` y corregir fallos; verificar que compila, pasa lint y los tests unitarios.
- [x] 6.2 Ejecutar `./gradlew connectedDebugAndroidTest` si hay emulador/dispositivo disponible; si el entorno está bloqueado, no declarar la tarea completada.
- [x] 6.3 Verificar manualmente el render del Toast y las variantes normal/redonda/monocromática del icono del launcher; registrar el resultado observado.
- [x] 6.4 Medir manualmente en emulador/dispositivo la latencia de la apertura de O con `Impossible` desde el tablero vacío y registrar el resultado observado; no asertar timing frágil ni diseñar framework de benchmark o concurrencia.
