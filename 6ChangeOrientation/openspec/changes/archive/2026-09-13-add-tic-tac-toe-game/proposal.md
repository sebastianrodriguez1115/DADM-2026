## Why

La aplicación actual solo presenta la pantalla de ejemplo de Android y no ofrece el juego del PDF. Este cambio permitirá entregar un tres en raya funcional y moderno con Kotlin y Compose, manteniendo el paquete existente y las reglas esenciales de la fuente original.

## What Changes

- Reemplazar la pantalla de saludo por una interfaz moderna en Compose y Material 3 para una partida de tres en raya.
- Presentar un tablero de nueve casillas con X verde y O roja, interacción para movimientos válidos, estado visible y bloqueo al terminar la partida.
- Implementar un motor separado de la interfaz, con turnos humano X y computadora O, detección de victoria y empate, y una IA que priorice ganar, bloquear y elegir al azar, sin minimax.
- Adaptar el comportamiento del PDF y de `TicTacToeConsole.java` a una experiencia sin `Scanner`, `main` ni bucle bloqueante de consola.
- Añadir una acción de nueva partida que limpie el tablero y el resultado sin borrar el marcador de la sesión.
- Alternar el iniciador entre partidas y mostrar contadores acumulados de victorias de X, victorias de O y empates.
- Conservar la partida en curso y los contadores durante la rotación, y conservar el marcador entre partidas de una sesión, sin persistencia permanente entre sesiones.
- Externalizar los textos visibles a recursos Android de strings y plurales cuando corresponda.
- Cubrir las reglas del motor y los flujos principales de la interfaz con las pruebas locales y de Compose disponibles.

## Capabilities

### New Capabilities

- `tic-tac-toe-game`: Reglas del juego, turnos, IA y presentación e interacción del tablero y su estado.
- `game-session`: Nueva partida, alternancia del iniciador, marcador acumulado de victorias de X/O y empates, y conservación del estado de la sesión.

### Modified Capabilities

No hay capacidades existentes ni specs base que modificar.

## Impact

- `MainActivity.kt` y nuevas clases Kotlin bajo `com.edu.unal.tictactoe`, separando el motor del juego de la UI Compose.
- Recursos de interfaz, incluidos `strings.xml` y recursos de plurales si son necesarios.
- Pruebas unitarias del motor y pruebas de UI Compose para tablero, estado, nueva partida y marcador.
- La configuración actual de Kotlin, Compose, Material 3, lifecycle y testing será el punto de partida; cualquier ajuste de dependencias se definirá posteriormente, sin asumir la disponibilidad directa de artefactos lifecycle no declarados.
- No se introducen multijugador, niveles de dificultad, login, red, base de datos ni persistencia entre sesiones.
