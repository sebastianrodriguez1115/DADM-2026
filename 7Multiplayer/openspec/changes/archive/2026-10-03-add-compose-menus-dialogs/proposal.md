## Why

El reto 4 pide practicar el uso de menús y diálogos en Android, pero el proyecto actual solo expone un botón de "Nueva partida" en el tres en raya. Hay que adaptar ese reto a Kotlin/Jetpack Compose/Material 3, sin reutilizar menús XML ni las APIs obsoletas `showDialog`/`onCreateDialog`, e incorporar los extras pedidos (Acerca de con imagen y autor, e icono propio), además de una dificultad "Imposible" que juegue con minimax.

## What Changes

- Añadir un menú principal con cuatro entradas: **Nueva partida**, **Dificultad**, **Acerca de (About)** y **Salir**.
- Añadir un diálogo de **Dificultad** que abre mostrando la opción actual como radio seleccionado (Easy/Hard/Expert/Impossible; por defecto **Expert**). La dificultad solo es editable con el tablero vacío antes de la primera jugada, o después de un desenlace; durante una partida activa (resultado en curso con al menos una marca, incluida la apertura automática de O) la entrada permanece visible pero deshabilitada y la lógica de sesión (ViewModel) rechaza la selección. Al elegir un nivel aceptado, el radio actualiza la dificultad, el diálogo se cierra automáticamente y se emite un **Toast** con el nivel seleccionado.
- Definir la política de jugada de la computadora por nivel: **Easy** elige siempre al azar; **Hard** gana si puede y, si no, elige al azar; **Expert** conserva la política actual (ganar > bloquear > aleatorio); **Impossible** juega con minimax de búsqueda completa hasta el desenlace en el tablero 3x3 (victoria de O > empate > derrota de O, sin azar, con desempate por menor índice).
- Añadir un diálogo de confirmación de **Salir**: "Sí" finaliza la actividad y "No" permanece; no descartable por toque fuera ni por Atrás, solo mediante sus botones.
- Añadir un diálogo **Acerca de (About)** con una imagen propia y el autor exacto `Juan Sebastian Rodriguez`.
- Renovar la identidad visual propia del tres en raya para el launcher adaptativo/round y para la imagen de Acerca de.
- Extender el contrato de inteligencia artificial sin romperlo: el requisito de prioridad fija pasa a describir la selección según la dificultad; Expert reproduce el comportamiento actual (ganar > bloquear > aleatorio) e Impossible incorpora una política minimax determinista dentro de la misma capacidad `tic-tac-toe-game`, sin crear capacidades nuevas ni alterar `game-session`.
- Decisiones propuestas conservadoras (no son aprobaciones que bloqueen la propuesta):
  - Conservar el botón "Nueva partida" existente como atajo además del menú.
  - Mantener la dificultad en memoria de la sesión, retenida al pedir nueva partida y tras rotación, con Expert al iniciar sesión y sin persistencia entre procesos.
  - Editar la dificultad únicamente antes de la primera jugada (tablero vacío, O aún no ha abierto) o tras un desenlace; ese cambio no reinicia ni altera tablero, resultado, marcador, turnos o iniciadores ni dispara jugadas. La consulta de movimiento no tiene efectos sobre el tablero y recibe la dificultad por parámetro; en partida activa la dificultad queda bloqueada, la entrada deshabilitada y la lógica de sesión (ViewModel) rechaza solicitudes directas u obsoletas. Elegir un nivel tras el desenlace prepara la próxima partida sin requerir un selector al pulsar Nueva partida.
  - No actualizar SDK ni dependencias salvo necesidad comprobada.
  - Mantener fuera multijugador, navegación y persistencia adicionales.
  - Conservar intactos el marcador, los turnos, la alternancia y la terminalidad.
  - Incorporar Impossible como política de la capacidad existente, con búsqueda completa hasta terminal en 3x3: no promete ganar siempre, puede empatar, y no obliga a victoria más rápida ni a demorar derrotas si no se añade profundidad al score. Solo puede activarse en la ventana permitida (antes de la primera jugada o tras el desenlace), no con una partida activa.

## Capabilities

### New Capabilities

- `game-options`: menú de opciones y sus diálogos Material 3 (nueva partida, dificultad con selección radio, salida confirmada y Acerca de), junto con la identidad visual propia (launcher e imagen de Acerca de).

### Modified Capabilities

- `tic-tac-toe-game`: el requisito "Prioridad y selección de la inteligencia artificial" deja de ser una política fija y pasa a depender de la dificultad seleccionada (Easy, Hard, Expert, Impossible). Expert reproduce la política actual e Impossible añade minimax determinista. La dificultad solo puede seleccionarse en la ventana permitida (antes de la primera jugada o tras el desenlace), no durante una partida activa. Los demás requisitos (tablero, turnos, desenlace, marcador, accesibilidad) no cambian.

## Impact

- **UI**: `app/src/main/java/com/edu/unal/tictactoe/MainActivity.kt` (menú y diálogos), `.../ui/game/TicTacToeScreen.kt`, `.../ui/game/GameViewModel.kt` y `.../ui/game/models/GameUiState.kt` (dificultad en memoria).
- **Dominio**: `app/src/main/java/com/edu/unal/tictactoe/game/TicTacToeGame.kt` (selección de jugada según dificultad; minimax determinista para Impossible, sin usar el generador aleatorio).
- **Recursos**: `app/src/main/res/values/strings.xml`, `drawable/` e `mipmap-anydpi/` más WebP de densidad para el icono propio.
- **Tests afectados**: `app/src/androidTest/.../MainActivityTest.kt` y `TicTacToeScreenTest.kt` (flujo del menú), `app/src/test/java/com/edu/unal/tictactoe/game/TicTacToeGameTest.kt` (políticas por nivel, incluida minimax/garantía) y `.../ui/game/GameViewModelTest.kt` (dificultad vigente).
- **Sin impacto en esta fase**: la capacidad `game-session` no cambia sus requisitos. En esta fase de planificación no se escriben `openspec/specs/`; los deltas de `tic-tac-toe-game` y `game-options` se sincronizarán a las specs principales en el paso del flujo que corresponda. Sin nuevas dependencias.
