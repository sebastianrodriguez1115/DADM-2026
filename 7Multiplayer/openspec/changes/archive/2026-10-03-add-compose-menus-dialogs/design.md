## Context

Ver `proposal.md` para la motivación. Estado actual relevante:

- `MainActivity` es `ComponentActivity` con `setContent`, `Scaffold` y un `ViewModel` de actividad (`viewModel()`); hoy no hay menú ni diálogos.
- `TicTacToeScreen` recibe `uiState`, `onCellClick` y `onNewGame`; usa `BoxWithConstraints` + `Column` con scroll y respeta `innerPadding`.
- `TicTacToeGame.getComputerMove()` no muta el tablero ni los snapshots: la pureza aplica solo a la consulta de IA. El motor sí es con estado: guarda `cells` y lo muta con `setMove`/`clearBoard`; usa `MoveRandomizer` inyectable para los niveles aleatorios.
- `GameUiState` es una clase normal (no `data class`) con snapshot inmutable y sin `.copy()`; `GameViewModel` publica instancias nuevas y solo calcula `score` en la transición terminal.
- `game-session` ya exige conservación en memoria ante rotación/background y descarte de sesión tras `finish`/pérdida de proceso; no cambia.
- Restricción: Kotlin/Compose/Material 3, Español, sin menús XML ni `showDialog`/`onCreateDialog`, sin tocar SDK ni dependencias.
- La UI actual no expone recursos de dificultad: los labels Fácil/Difícil/Experto/Imposible son planificados, no implementados todavía.

## Goals / Non-Goals

**Goals:**
- Añadir menú Material 3 con Nueva partida, Dificultad, Acerca de (About) y Salir, integrado en `Scaffold`.
- Introducir `DifficultyLevel` (Easy/Hard/Expert/Impossible, Expert por defecto) como política de selección de la IA, conservando intactos tablero, turnos, alternancia, terminalidad y marcador.
- Incorporar `Impossible` como minimax determinista completo 3x3 dentro de la misma capacidad, sin prometer victoria siempre.
- Diálogos Compose: dificultad (radio + cierre + Toast condicionado a aceptación), salida confirmada (no descartable fuera/Atrás) y Acerca de (imagen propia + autor exacto).
- Identidad visual propia (launcher adaptativo/round e imagen de About), sin romper tests existentes.

**Non-Goals:**
- Multijugador, navegación con librería, `DataStore`/`SavedStateHandle` o persistencia entre procesos.
- Restaurar ventanas de menú/diálogo abiertas tras recreación (la dificultad sí se conserva en el VM).
- Estrategias minimax precomputadas, tablas de aperturas, librerías nuevas o concurrencia.
- Actualizar SDK, BOM o dependencias.

## Decisions

### Dificultad en dominio como parámetro, no como estado
- `getComputerMove(difficulty: DifficultyLevel = DifficultyLevel.Expert)` en `TicTacToeGame`. El parámetro con default mantiene compilando y comportándose igual a las llamadas actuales sin argumento.
- `DifficultyLevel` en `com.edu.unal.tictactoe.game.models` con constantes `Easy`, `Hard`, `Expert`, `Impossible`.
- Política: `Easy` = casilla libre aleatoria; `Hard` = victoria inmediata si existe, si no aleatoria; `Expert` = ganar > bloquear > aleatoria (la actual); `Impossible` = minimax completo 3x3 (abajo).
- Los sondeos de victoria/bloqueo reutilizan listas copiadas y solo leen el tablero; no mutan ni disparan jugadas. Se conservan los guards actuales (`Won` -> `GameAlreadyFinished`, `Tie` -> `NoMoves`, sin invocar `MoveRandomizer`).
- Alternativa descartada: `TicTacToeGame` como fuente canónica de dificultad (política de sesión en un único punto, el VM, y dominio sin estado de configuración). También se descarta Strategy por objeto/nivel (sobre-ingeniería para cuatro políticas).

### Minimax determinista en Impossible
- Recurrencia que recibe un tablero DE PRUEBA y el jugador explícito a mover; nunca infiere el turno por conteo de marcas, porque O también puede abrir.
- Raíz en O: `getComputerMove` simula cada candidata de O y evalúa el árbol pasando X explícitamente, alternando en cada nivel. O maximiza, X minimiza.
- Evaluación terminal desde la perspectiva de O: victoria de O = `+1`, empate = `0`, victoria de X = `-1`. El score solo refleja el desenlace, sin profundidad ni timing: no promete ganar lo antes posible ni demorar derrotas.
- Los libres se enumeran por índice creciente; se reemplaza la mejor casilla solo si el valor es estrictamente mayor. Con empate de valor gana la de menor índice.
- Desde el inicio no pierde con cualquier iniciador (X u O abren), puede empatar y no se le exige ganar siempre. Guards terminales intactos y Impossible nunca usa `MoveRandomizer`.
- La recurrencia trabaja sobre copias o mutate/restore de un tablero local, sin tocar `cells` ni los snapshots.

### Memoización local por consulta
- Se adopta memoización local creada y descartada dentro de cada consulta, con clave (estado de tablero de prueba, jugador) y valor (score final sin profundidad). No hay cache global ni inferencia de turno.
- Explora el árbol completo hasta terminal, sin límite de profundidad; el cache no altera el contrato observable.
- Objetivo: no reevaluar los mismos estados y hacer viable el test exhaustivo. Alternativa descartada: tablas precomputadas, librerías de estrategia o concurrencia.
- Se mantiene la ejecución sincrónica actual del tablero 3x3. No se afirma que quepa en UI ni latencia garantizada sin medir: se planea medir la latencia de apertura desde tablero vacío. Si el resultado lo exige, se ajusta la implementación sin cambiar el contrato; no se expande a concurrencia ahora.

### Dificultad única en el VM y expuesta por `GameUiState`
- `GameViewModel` es dueño de la dificultad. La fuente canónica es `_uiState.value.difficulty` (sin `var` mutable adicional en el VM) y ese valor se pasa a `getComputerMove` en todas las consultas, incluida la apertura de O; el default de la firma sigue siendo Expert.
- `GameUiState` gana `val difficulty: DifficultyLevel = DifficultyLevel.Expert`; como es clase normal, el VM construye instancias nuevas copiando explícitamente los campos vigentes (`board`, `turn`, `result`, `starter`, `nextStarter`, `score`, `difficulty`).
- Predicado derivado único de editabilidad, expuesto por `GameUiState` (p. ej. `canChangeDifficulty = result != GameResult.InProgress || board.all { it == Mark.Empty }`), sin flag independiente. El VM lo reevalúa con el snapshot vigente en cada solicitud.
- `setDifficulty(level): Boolean` publica estado nuevo y devuelve `true` solo si la ventana lo permite. Con partida activa (InProgress con al menos una marca) devuelve `false` sin publicar cambios: no altera tablero, score, turno, iniciador, nivel ni movimiento. La UI emite Toast solo con `true`.
- Elegir el mismo nivel vigente en ventana válida sigue aceptando, cerrando y emitiendo un único Toast; el mismo nivel en partida activa se rechaza con `false`. Antes de la primera jugada el nivel rige la partida aún no empezada; tras un terminal configura la siguiente sin borrar el resultado. No hay dificultad pendiente durante una partida activa.
- `newGame()` conserva `difficulty` y la alternancia de sesión; con iniciador X deja el tablero vacío en turno humano normal y la dificultad habilitada; si O abre, la apertura automática usa el nivel vigente y solo la entrada Dificultad queda bloqueada. No hay selector obligatorio previo ni cambios en `game-session`. Una instancia nueva de VM parte en Expert; rotación/background conservan el nivel. Sin persistencia adicional.
- `SavedStateHandle`/`DataStore` no se usan: la dificultad vive solo en memoria de la sesión y `game-session` no exige persistirla. Restaurar tablero o marcador entre sesiones sí contradiría `game-session` y queda fuera de alcance.

### Menú y diálogos en Compose
- Menú de desbordamiento en `Scaffold` `topBar` con `IconButton` + `DropdownMenu` de cuatro entradas con iconos y `stringResource`; se acepta `@OptIn(ExperimentalMaterial3Api::class)` local si el BOM actual lo exige y no hay alternativa estable.
- Componentes en `ui/game` separados (`GameMenu`, `DifficultyDialog`, `ExitConfirmationDialog`, `AboutDialog`) para no inflar `TicTacToeScreen`; los dos últimos usan `AlertDialog` Material 3.
- Estado de menú/diálogos con `remember` local en la UI (efímero, no se preserva tras recreación; no lo exige el comportamiento actual). La dificultad no vive en la UI.
- El menú se cierra antes de abrir un diálogo o ejecutar la acción de una entrada habilitada, y solo puede haber una ventana activa a la vez (menú o diálogo).
- La entrada Dificultad usa `enabled = uiState.canChangeDifficulty`: está deshabilitada solo cuando ese predicado es `false` (partida activa con al menos una marca). Deshabilitada no abre diálogo ni dispara callback. Si un estado obsoleto dejara el modal abierto, el VM revalida al seleccionar y el rechazo cierra sin Toast; la selección nunca se representa como aceptada.
- Atrás normal descarta el resto de menús/diálogos; solo el de Salir fija `dismissOnBackPress = false`. No hay manejador global de Atrás.
- Se conserva el botón "Nueva partida" con tag `tic_tac_toe_new_game` como atajo; el menú añade su propia entrada.
- El callback de salida se propaga como `onExit: () -> Unit` hasta `MainActivity`, que ejecuta `finish()`; la UI no hace cast de `Context` a `Activity`. En `ExitConfirmationDialog`: "No" cierra y preserva estado; "Sí" invoca `onExit`; solo ese diálogo usa `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`. Sin `BackHandler` global.

### Dificultad: selección, cierre y Toast
- Al abrir, el radio marca la dificultad actual de `uiState.difficulty`.
- `onDifficultySelected: (DifficultyLevel) -> Boolean` es el contrato del VM: devuelve `true` si aceptó y `false` si rechazó. La UI cierra el diálogo en la pulsación y emite el Toast exactamente una vez solo si el retorno es `true`; con `false` no hay Toast ni estado nuevo. Mismo nivel vigente en ventana válida devuelve `true` y emite un único Toast; mismo nivel en partida activa devuelve `false` sin efecto.
- La decisión se resuelve en el evento de pulsación, nunca por recomposición, cancelación ni `LaunchedEffect`. No hay callback de notificación sobre un rechazo.
- `LocalContext.current` se obtiene durante la composición del host y se captura en el manejador; no se accede a APIs composables dentro del callback ordinario.
- Para probar la emisión sin depender del Toast de plataforma, el host inyecta `onShowToast: (DifficultyLevel) -> Unit` (seam proporcional, sin arquitectura nueva), invocado solo tras aceptación; `MainActivity` lo cablea al Toast real y los tests capturan el evento. El render real del Toast queda como verificación manual.
- Localización de la UI en español: identidades `Easy/Hard/Expert/Impossible` presentadas vía recursos como Fácil/Difícil/Experto/Imposible; el Toast usa el mismo label. Las specs se alinean; no cambian las políticas ni el autor exacto.

### Identidad visual
- Imagen de About como vector propio (`drawable`) más `R.string.about_author` con el valor exacto `Juan Sebastian Rodriguez`.
- Launcher: sustituir el foreground vectorial y el color de fondo por un icono propio de tres en raya coherente con About, y regenerar/reemplazar los WebP por densidad para no dejar iconos plantilla de Android; se mantienen las referencias actuales del Manifest (`@mipmap/ic_launcher`, `@mipmap/ic_launcher_round`) y el `monochrome`. La generación de activos es tarea de implementación.

### Estrategia de pruebas y verificación
- Unitarias de dominio (`TicTacToeGameTest`): cuatro políticas. Easy invoca `MoveRandomizer` y elige según índice sobre los libres aun habiendo jugada táctica (se admite coincidencia, se asegura la llamada y el mapeo); Hard, sin victoria propia, invoca `MoveRandomizer` incluso habiendo amenaza humana y respeta el índice (puede coincidir con la casilla táctica, no se le exige bloquear); Expert mantiene los casos actuales; Impossible cubre victoria forzable, empate, defensa de horquilla cuando existe, derrota aislada del motor, determinismo por menor índice, ausencia de RNG, pureza y guards terminales.
- Impossible se explora además de forma exhaustiva: todas las jugadas humanas legales con ambos iniciadores desde el tablero vacío, verificando que ningún terminal sea victoria de X y que cada respuesta sea legal. El harness vive en el test (replay del historial o copia de test), sin motor adicional ni reutilizar el minimax de producción como oráculo.
- Unitarias de estado/VM (`GameUiStateTest`, `GameViewModelTest`): default Expert; inmutabilidad; editable con tablero vacío y tras terminal; rechazo en partida activa, incluida la opción vigente y solicitudes obsoletas, con score/tablero intactos; apertura de O bloquea y X vacío habilita; `newGame` conserva dificultad y score; sesión nueva en Expert; la siguiente consulta usa la dificultad vigente (incluida apertura O).
- Compose (`TicTacToeScreenTest` y pruebas nuevas de menú/diálogos): apertura con radio actual y reapertura, cancelación sin cambio, cuatro entradas del menú, entrada deshabilitada accesible sin diálogo ni Toast, mismo nivel aceptado con un único Toast, cancelar/recrear sin emisión, botones de Salir, contenido de About y autor.
- Instrumentadas (`MainActivityTest`): `ActivityScenario` valida Activity y sesión (finish real "Sí", permanencia "No", recreación/background conservan dificultad, nueva instancia Expert) y el gate de dificultad con lifecycle real. La no reemisión se valida en el host Compose con el callback capturado, no espiando el Toast de plataforma; el render real es verificación manual.
- Se conservan pruebas previas de marcador, Nueva partida, Salir, About, autor, accesibilidad y layout; se añade cobertura nueva sin sustituir la anterior.
- El Toast puede complicar el assert externo en plataformas modernas: se prueba el callback/evento y se registra verificación manual del render, sin afirmar que ya se ejecutó.
- Proceso previsto para la etapa apply (no ejecutado aquí): `./gradlew testDebugUnitTest lintDebug assembleDebug` y `./gradlew connectedDebugAndroidTest` si hay dispositivo.

## Risks / Trade-offs

- [Tests existentes de IA asumen Expert] -> default Expert en la firma y conservar los guards evita romperlos; las pruebas de nivel se añaden alrededor.
- [Confundir "puro" con el motor sin estado] -> la pureza se limita a la consulta; `cells` se muta solo por `setMove`/`clearBoard` y los snapshots nunca cambian.
- [Dos fuentes de dificultad (VM vs dominio)] -> el dominio no almacena dificultad; solo la recibe por parámetro y el VM es el único dueño.
- [`topBar` reduce el viewport del tablero] -> `Scaffold` ya entrega `innerPadding`; `TicTacToeScreen` mantiene scroll y su cálculo de tamaño; se valida en pruebas de layout.
- [Toast difícil de asertar y riesgo de duplicado por recomposición] -> emitir en el evento de pulsación vía callback inyectable, no en `LaunchedEffect`; el render real es manual.
- [Latencia de minimax no medida] -> memo local por consulta y ejecución sincrónica del 3x3; se planea medir la apertura desde tablero vacío sin afirmar latencia garantizada.
- [API experimental de Material 3 (`TopAppBar`)] -> opt-in lo más local posible; si el BOM lo permite, usar componentes estables.
- [Iconos WebP por densidad y `monochrome`] -> reemplazo completo en implementación; verificar variantes redondeada/adaptativa para no dejar plantilla.
- [Accesibilidad de menú/diálogos] -> etiquetas `stringResource`, radios con semántica de selección y orden de foco verificados en pruebas Compose.
- [Estado obsoleto del modal] -> el VM revalida y el rechazo cierra sin Toast; el radio nunca se marca como aceptado.
