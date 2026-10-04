## MODIFIED Requirements

### Requirement: Prioridad y selección de la inteligencia artificial

Cuando la computadora deba jugar, MUST seleccionar la casilla según el nivel de dificultad vigente. La consulta del movimiento MUST NOT modificar el tablero y SHALL devolver un resultado tipado: la casilla libre seleccionada; o la indicación de que no hay movimientos cuando el tablero está lleno sin ganador; o la indicación de que la partida ya terminó cuando existe un ganador, incluso si el tablero está lleno. La aplicación efectiva del movimiento SHALL corresponder al flujo de juego existente y MUST NOT realizarse desde la consulta. En una partida terminal, la consulta MUST NOT seleccionar una casilla ni recurrir al azar, independientemente del nivel de dificultad.

- En **Expert**, nivel por defecto: MUST seleccionar primero una casilla que complete una victoria inmediata propia si existe. Si no puede ganar y existe al menos una casilla donde X ganaría en su siguiente movimiento, MUST seleccionar una de esas casillas amenazadas. Si hay varias amenazas simultáneas, no se garantiza bloquearlas todas. Si no existe ninguna victoria inmediata propia ni amenaza humana, MUST seleccionar una casilla libre al azar.
- En **Hard**: MUST seleccionar una casilla que complete una victoria inmediata propia si existe. Si no puede ganar, MUST seleccionar una casilla libre al azar sin bloquear deliberadamente las amenazas humanas.
- En **Easy**: MUST seleccionar siempre una casilla libre al azar, sin priorizar victorias propias ni bloqueos.
- En **Impossible**: MUST seleccionar mediante minimax con búsqueda completa hasta el estado terminal sobre el tablero 3x3, con la consulta raíz para O y alternando explícitamente el jugador por turno (O maximiza y X minimiza un mismo valor calculado desde la perspectiva de O), respetando la alternancia incluso cuando O abre la partida. La prioridad de resultado MUST ser victoria de O > empate > derrota de O. MUST NOT recurrir al azar ni usar el generador aleatorio. Si varias casillas libres comparten el valor óptimo, MUST seleccionar la de menor índice en el orden actual del tablero para que la consulta sea determinista y reproducible.

En los niveles que eligen al azar, la casilla sorteada puede completar una victoria o bloquear una amenaza por casualidad; el comportamiento MUST NOT exigir que evite esas casillas. La consulta del movimiento MUST NOT aplicar el movimiento ni alterar el estado terminal.

En **Impossible**, mientras el nivel esté activo desde el inicio de una partida, la computadora MUST NOT perder ante ninguna secuencia legal humana, comience X o comience O; puede empatar y no se le exige ganar siempre. La consulta del movimiento de Impossible MUST ser determinista y sin azar, y MUST NOT prometer rescatar posiciones ya forzosamente perdidas. La consulta MUST NOT mutar el tablero y MUST recibir la dificultad por parámetro; el motor MUST NOT albergar el guard de selección de dificultad, que corresponde únicamente al ViewModel, sin alterar los guards de victoria y empate ya establecidos.

#### Scenario: Ganar tiene prioridad sobre bloquear

- **GIVEN** que la dificultad es Expert, que O tiene una victoria inmediata disponible y que X también tiene una victoria inmediata disponible
- **WHEN** la computadora selecciona su movimiento
- **THEN** devuelve la casilla que completa la victoria de O en lugar de la que bloquearía a X

#### Scenario: Bloquear una amenaza inmediata cuando no puede ganar

- **GIVEN** que la dificultad es Expert, que O no tiene una victoria inmediata y que X tiene una única casilla que le daría la victoria en su siguiente movimiento
- **WHEN** la computadora selecciona su movimiento
- **THEN** devuelve la casilla que evita la victoria inmediata de X

#### Scenario: Amenazas humanas múltiples

- **GIVEN** que la dificultad es Expert, que O no tiene una victoria inmediata y que X tiene varias casillas que le darían la victoria en su siguiente movimiento
- **WHEN** la computadora selecciona su movimiento
- **THEN** la computadora devuelve una de esas casillas amenazadas, sin garantía de evitar otra amenaza simultánea

#### Scenario: Elegir al azar sin prioridad táctica

- **GIVEN** que la dificultad es Expert, que O no puede ganar inmediatamente y que X no puede ganar inmediatamente
- **WHEN** la computadora selecciona su movimiento
- **THEN** devuelve una casilla libre elegida al azar

#### Scenario: Hard gana cuando puede

- **GIVEN** que la dificultad es Hard y que O tiene una victoria inmediata disponible
- **WHEN** la computadora selecciona su movimiento
- **THEN** devuelve la casilla que completa la victoria de O

#### Scenario: Hard no bloquea deliberadamente

- **GIVEN** que la dificultad es Hard, que O no tiene una victoria inmediata y que X podría ganar en su siguiente movimiento
- **WHEN** la computadora selecciona su movimiento
- **THEN** la computadora devuelve una casilla libre al azar sin que se le exija la casilla amenazada, pudiendo bloquear solo por casualidad

#### Scenario: Easy elige siempre al azar

- **GIVEN** que la dificultad es Easy y que existen victorias o amenazas inmediatas en el tablero
- **WHEN** la computadora selecciona su movimiento
- **THEN** la computadora devuelve una casilla libre al azar sin priorizar ganar ni bloquear, pudiendo ganar o bloquear solo por casualidad

#### Scenario: Nivel por defecto Expert

- **GIVEN** que no se ha seleccionado una dificultad explícita
- **WHEN** la computadora selecciona su movimiento
- **THEN** aplica la política del nivel Expert

#### Scenario: Impossible prioriza la victoria cuando el resultado óptimo es ganar

- **GIVEN** que la dificultad es Impossible y que, desde el estado actual, O puede forzar una victoria
- **WHEN** la computadora selecciona su movimiento
- **THEN** devuelve una casilla cuyo desenlace óptimo es la victoria de O

#### Scenario: Impossible fuerza el empate cuando no puede garantizar la victoria

- **GIVEN** que la dificultad es Impossible y que, desde el estado actual, O no puede forzar una victoria pero sí evitar una derrota
- **WHEN** la computadora selecciona su movimiento
- **THEN** devuelve una casilla cuyo desenlace óptimo es el empate y no una derrota de O

#### Scenario: Impossible evita perder ante una horquilla cuando existe defensa

- **GIVEN** que la dificultad es Impossible, que una defensa ingenua permitiría a X forzar una victoria por horquilla y que existe al menos una respuesta que evita la derrota
- **WHEN** la computadora selecciona su movimiento
- **THEN** devuelve una casilla cuyo desenlace óptimo no es una derrota de O

#### Scenario: Desempate determinista por menor índice

- **GIVEN** que la dificultad es Impossible y que varias casillas libres comparten el valor óptimo
- **WHEN** la computadora selecciona su movimiento, incluso en consultas repetidas sobre el mismo estado
- **THEN** devuelve siempre la de menor índice en el orden actual del tablero y no invoca el generador aleatorio

#### Scenario: Impossible no pierde desde el inicio con cualquier iniciador

- **GIVEN** que la dificultad es Impossible y que está activa desde el inicio de la partida, comience X o comience O
- **WHEN** se juega cualquier secuencia legal de movimientos humanos y la computadora decide en cada turno
- **THEN** el desenlace nunca es victoria de X; puede ser victoria de O o empate

#### Scenario: Consulta de motor en una posición ya perdida no promete rescate

- **GIVEN** que la dificultad es Impossible y que el estado del motor ya es una derrota forzada para O
- **WHEN** se consulta el movimiento de la computadora sobre ese estado aislado
- **THEN** devuelve una casilla consistente con el valor óptimo desde ese estado y el contrato total del solver no garantiza evitar la derrota ni altera tablero, turnos ni marcador; además, esa consulta aislada no habilita cambiar la dificultad con una partida activa, restringido por `game-options`

#### Scenario: La consulta no modifica el tablero

- **GIVEN** un tablero en partida activa y cualquier nivel de dificultad
- **WHEN** se consulta el movimiento de la computadora sin aplicarlo
- **THEN** la instantánea del tablero permanece igual y la casilla devuelta, si existe, estaba libre y no queda ocupada por la consulta

#### Scenario: Consulta tras una victoria

- **GIVEN** que existe un ganador y la partida terminó, incluso si el tablero está lleno, con cualquier nivel de dificultad
- **WHEN** se consulta el movimiento de la computadora
- **THEN** el resultado tipado indica que la partida ya terminó, no se selecciona casilla, ningún nivel recurre al azar y el tablero permanece sin cambios

#### Scenario: Consulta tras un empate sin ganador

- **GIVEN** que el tablero está lleno sin ganador y con cualquier nivel de dificultad
- **WHEN** se consulta el movimiento de la computadora
- **THEN** el resultado tipado indica que no hay movimientos, no se selecciona casilla, ningún nivel recurre al azar y el tablero permanece sin cambios
