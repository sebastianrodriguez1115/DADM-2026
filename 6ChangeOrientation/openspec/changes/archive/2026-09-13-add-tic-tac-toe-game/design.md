## Context

La aplicación todavía muestra `Greeting` desde `MainActivity.kt`, usando
`ComponentActivity`, `setContent`, `Scaffold` y `TicTacToeTheme`. No existe motor
ni estado de juego. Compose, Material 3, `lifecycle-runtime-ktx`, JUnit 4 y
pruebas de Compose ya están disponibles. Namespace y `applicationId` son
`com.edu.unal.tictactoe`; no se modificarán. El manifiesto no fija orientación.

Este diseño complementa `proposal.md` y sigue las specs completas de
`tic-tac-toe-game` y `game-session`. Las referencias son `TicTacToe.pdf`, pp.
1-2 y 6-10, y https://cs.harding.edu/fmccown/android/TicTacToeConsole.java.

La sesión será solo memoria viva: sobrevivirá configuración y background
mientras vivan proceso y tarea, pero no muerte del proceso ni fin de la tarea.

## Goals / Non-Goals

**Goals:**

- Portar reglas e IA a Kotlin/Compose moderno, con motor reutilizable sin dependencia de Android y sin efectos de juego al recomponer.
- Conservar tablero, turno, resultado, iniciador siguiente y marcador durante la sesión, incluida la recreación por rotación.
- Hacer explícitos contratos de movimiento, resultado, terminalidad y selección de la computadora, con una superficie pequeña y comprobable.

**Non-Goals:**

- Persistencia o restauración mediante `SavedStateHandle`, `rememberSaveable`, DataStore, preferencias, Room, red, login, multijugador, niveles o minimax.
- Cambios de paquete, orientación, SDK, versiones generales, animaciones, retrasos artificiales, sonidos o reglas ajenas a las specs.

## Decisions

### Estructura y responsabilidades

`MainActivity` seguirá siendo la entrada. Obtendrá el ViewModel asociado a la Activity, aplicará `TicTacToeTheme` y mostrará la pantalla dentro de `Scaffold`; no iniciará partidas desde cada `onCreate`.

Las rutas previstas bajo el paquete actual separan el dominio de la
presentación:

- `app/src/main/java/com/edu/unal/tictactoe/game/TicTacToeGame.kt`: motor puro Kotlin.
- `app/src/main/java/com/edu/unal/tictactoe/game/models/Player.kt`: jugador humano o computadora (`X` u `O`).
- `app/src/main/java/com/edu/unal/tictactoe/game/models/Mark.kt`: marca de cada casilla (`Empty`, `X` u `O`).
- `app/src/main/java/com/edu/unal/tictactoe/game/models/GameResult.kt`: resultado del motor.
- `app/src/main/java/com/edu/unal/tictactoe/game/models/MoveResult.kt`: resultado de aplicar un movimiento.
- `app/src/main/java/com/edu/unal/tictactoe/game/models/ComputerMoveResult.kt`: resultado de seleccionar un movimiento de la computadora.
- `app/src/main/java/com/edu/unal/tictactoe/game/models/Turn.kt`: turno humano o de computadora.
- `app/src/main/java/com/edu/unal/tictactoe/game/models/Score.kt`: marcador acumulado de la sesión.
- `app/src/main/java/com/edu/unal/tictactoe/ui/game/models/GameUiState.kt`: snapshot inmutable de presentación que compone los modelos de dominio.
- `app/src/main/java/com/edu/unal/tictactoe/ui/game/GameViewModel.kt`: sesión, transiciones y `StateFlow`, como coordinación de presentación ligada al lifecycle.
- `app/src/main/java/com/edu/unal/tictactoe/ui/game/TicTacToeScreen.kt`: pantalla y tablero.

Cada modelo de nivel superior tendrá su propio archivo. Las variantes de un
`enum` o de una jerarquía sellada permanecen anidadas en el archivo de su
modelo, no se convierten en archivos adicionales. Esto responde a la
preferencia explícita por localización y responsabilidad claras, no a una
regla obligatoria de Kotlin. No habrá un contenedor global de modelos.

Los tipos de `game/` y el motor serán independientes de Android y Compose. La
presentación dependerá de esos tipos mediante `ui/game/models/GameUiState`, mientras el
dominio no importará presentación. Por eso `GameViewModel` se planifica junto
con `TicTacToeScreen` en `ui/game/`: coordina estado y lifecycle sin mezclar
esa responsabilidad con el dominio puro. No se añadirán capas nuevas para
esta separación.

No se añadirán repositories, casos de uso, DI ni capas genéricas. Para nueve
casillas locales solo aumentarían acoplamiento y costo. Mezclar motor con UI
también se descarta porque dificulta probar reglas y adaptar la pantalla.

### Contrato del motor

El motor tendrá `Player.X` y `Player.O`, con símbolos explícitos `'X'` y `'O'`, y `Mark.Empty`, `Mark.X` y `Mark.O`. Así conserva el significado del tutorial sin números de consola. Su tablero privado tendrá nueve posiciones y expondrá solo una copia defensiva, un snapshot `List<Mark>`, nunca una vista del tablero mutable.

Las cuatro operaciones equivalentes al PDF serán `clearBoard()`,
`setMove(player, location)`, `getComputerMove()` y `checkForWinner()`:

- `clearBoard()` deja las nueve posiciones en `Empty`.
- `setMove` devuelve `MoveResult.Applied`, `MoveResult.InvalidLocation`, `MoveResult.Occupied` o `MoveResult.GameAlreadyFinished`. Solo `Applied` muta. La ubicación válida es 0..8 y `Player` elimina un caso de jugador inválido, sin `null` ni retornos ambiguos.
- `checkForWinner()` devuelve `GameResult.InProgress`, `GameResult.Tie` o `GameResult.Won(Player)`. El mapeo explícito al tutorial es 0, 1, 2 y 3 para activo, empate, X y O. Comprueba líneas antes de tablero lleno, priorizando victoria en la última casilla.
- `getComputerMove()` es selector, no movimiento, y devuelve `ComputerMoveResult.Selected(index)`, `ComputerMoveResult.NoMoves` o `ComputerMoveResult.GameAlreadyFinished`. Si ya hay ganador devuelve `GameAlreadyFinished`, aunque el tablero esté lleno; si no hay ganador y no quedan casillas libres devuelve `NoMoves`; en partida activa devuelve `Selected(index)` libre y válido. Luego el llamador debe ejecutar `setMove(Player.O, index)`.

El motor no lleva turnos de sesión ni marcador, responsabilidad exclusiva del ViewModel. Rechaza movimientos terminales y no muta fuera de operaciones explícitas. Para ganar y bloquear recorre las casillas 0..8 y elige la primera victoria o bloqueo inmediato, como la fuente original. Solo si no hay táctica elige uniformemente entre las casillas libres usando el azar inyectable; con amenazas múltiples no garantiza bloquearlas todas. Las sondas usan copias o colocación y restauración garantizadas, por lo que seleccionar nunca deja el tablero mutado.

Esto adapta la fuente, no la traduce literalmente: se eliminan `Scanner`, `main`, el constructor con bucle bloqueante y la consola; `Empty` reemplaza los dígitos de posiciones vacías. El azar se inyectará con una abstracción pequeña como `MoveRandomizer`, productiva con `kotlin.random.Random` y controlable en pruebas. Se descarta un `Int` con `-1` o `null` porque oculta falta de jugadas.

### Estado y ciclo de vida de la sesión

`GameViewModel`, creado con `viewModel()` y dueño de la Activity, encapsulará el
motor y mantendrá un `MutableStateFlow<GameUiState>`. El snapshot inmutable incluirá tablero,
`Turn.Human` o `Turn.Computer`, resultado, iniciador, iniciador siguiente y
`Score(xWins, oWins, ties)`.

La inicialización del ViewModel crea una sola sesión con marcador cero y X
como primer iniciador. `newGame()` limpia marcas y resultado, conserva el
marcador, alterna el iniciador incluso si la partida estaba incompleta y
actualiza el siguiente. Si inicia O, selecciona y aplica exactamente una O y
deja turno humano; si inicia X, deja tablero vacío y habilita X.

`onHumanMove(location)` solo actúa en partida activa y turno humano. Aplica X,
comprueba terminalidad antes de O, y solo si continúa selecciona y aplica una
O. Comprueba de nuevo antes de habilitar X. Toda entrada inválida, ocupada,
fuera de rango, terminal o durante turno de O deja estado y tablero intactos
y no dispara IA.

Las transiciones serán síncronas y serializadas en el hilo principal. No se
lanzarán tareas asíncronas para las jugadas ni se introducirán delays: el turno de computadora puede ser transitorio y no se
promete un frame ni duración mínima. El marcador se incrementa solo al pasar
de activo a terminal, nunca al renderizar; el resultado evita doble puntuación
por taps, recolección o recreación.

La pantalla usará `collectAsStateWithLifecycle()`. Recomposición, recolección y
`onCreate` solo leen o conectan estado. Sin `SavedStateHandle`,
`rememberSaveable` ni almacenamiento permanente, un nuevo ViewModel tras perder
proceso o tarea empieza con marcador cero y X. Volver del background no crea
partidas ni movimientos.

### UI Compose y recursos

`TicTacToeScreen` recibirá un snapshot y callbacks de acciones. No creará motor
ni ViewModel, ni ejecutará jugadas al renderizar. Mostrará, en este orden,
tablero 3x3, estado, tres contadores y `OutlinedButton` visible `Nueva partida`.
Es una acción accesible, no un botón físico ni menú antiguo. Las nueve celdas
serán botones cuadrados con constraints y `aspectRatio(1f)`, adaptables al
espacio disponible, sin tamaños fijos del PDF que impidan rotación.

Las celdas ocupadas quedarán disabled pero conservarán texto legible: X verde
y O rojo, también en colores disabled explícitos. Se mantendrá el tema dinámico
con contraste suficiente y semántica accesible por casilla. Estados,
resultados, acción, contadores y rótulos irán en `strings.xml`, con plurales si
corresponde, nunca hard-codeados en composables. Se usarán `sp` y tipografía
Material existente, con ajustes menores de legibilidad y espaciado, sin
features nuevas.

### Dependencias y pruebas previstas

Se mantendrán Compose Material 3, BOM, Activity Compose y el catálogo actual.
Solo se añadirán al catálogo y módulo `lifecycle-viewmodel-compose` y
`lifecycle-runtime-compose`, ambos con lifecycle `2.11.0`, para `viewModel()` y
`collectAsStateWithLifecycle()`. No se actualizarán SDK, AGP, Kotlin, BOM ni
otras dependencias.

JUnit local cubrirá los contratos de los modelos en sus archivos propios, además
del motor y `ui/game/GameViewModel`: reglas, límites, ocupación, terminalidad,
victoria prioritaria al llenar la última casilla y empate solo sin victoria,
IA ganar/bloquear/azar sin mutación, falta de jugadas,
apertura X/O, alternancia incompleta, puntuación única y secuencia X antes de
O. Fixtures y `MoveRandomizer` controlado excluyen pruebas probabilísticas.
Las pruebas unitarias de modelos se mantendrán bajo
`app/src/test/java/com/edu/unal/tictactoe/game/models/`, las del motor bajo
`app/src/test/java/com/edu/unal/tictactoe/game/` y las del ViewModel bajo
`app/src/test/java/com/edu/unal/tictactoe/ui/game/`; las pruebas del snapshot
se mantendrán bajo `app/src/test/java/com/edu/unal/tictactoe/ui/game/models/`. Compose
Android cubrirá marcas, colores, estados, marcador, nueva partida,
recreación de Activity, retorno de background y nueva instancia de sesión.
El estado de fixtures se preparará con APIs públicas y la inyección de motor y
azar será por constructor o `ViewModelProvider.Factory` de prueba, sin backdoors
productivos ni delays.

## Risks / Trade-offs

- **[Pérdida deliberada tras muerte de proceso o fin de tarea]** → Estado solo
  en ViewModel, con pruebas de nueva sesión en cero y con X.
- **[IA no invencible]** → Mantener ganar, bloquear y azar, sin minimax, y
  verificar prioridades con fixtures deterministas.
- **[Turno O casi no visible]** → Mantener transición síncrona sin duración
  artificial; el estado final sigue siendo coherente y accesible.
- **[Contraste disabled insuficiente]** → Fijar colores de contenido disabled y
  verificar tema claro y oscuro.
- **[ViewModel con propietario incorrecto]** → Obtenerlo de la Activity, nunca
  crearlo dentro de recomposición.

## Migration Plan

1. Sustituir el saludo por motor, modelos en archivos individuales, ViewModel de
   presentación y pantalla, conservando
   paquete, namespace y `applicationId`.
2. Conectar recursos y dependencias lifecycle mínimas, sin datos ni migración
   de almacenamiento.
3. Añadir las pruebas previstas y retirar solo el ejemplo que quede sin uso.

El rollback revierte únicamente archivos y dependencias de este cambio y
restaura el scaffold anterior. No toca cambios staged del usuario,
configuración ajena ni archivos de otros proyectos.

## Open Questions

No quedan decisiones funcionales abiertas. Espaciado, iconografía y tamaños
menores de texto pueden ajustarse durante implementación sin cambiar specs,
arquitectura ni tareas.
