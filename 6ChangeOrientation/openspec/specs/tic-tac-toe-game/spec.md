# tic-tac-toe-game Specification

## Purpose

Esta capacidad define el comportamiento observable del tres en raya, desde el tablero y los turnos hasta los desenlaces y la respuesta de la computadora, tomando como referencia el PDF (páginas 1-10) y la fuente Java consultada.

## Requirements

### Requirement: Tablero y marcas visibles
El juego SHALL limpiar las marcas y el resultado de la partida anterior al comenzar una nueva partida, y SHALL mostrar un tablero de tres por tres sin números ni identificadores de consola, con cuadrícula y marcas representadas gráficamente y no mediante botones o texto como representación. Al inicio de una partida con X, el tablero SHALL estar vacío y corresponder el turno humano; al inicio con O, SHALL estar vacío, corresponder el turno de computadora y bloquear X hasta que, tras 1000 ms de primer plano, O realice exactamente una jugada salvo cancelación. La marca X humana SHALL mostrarse en verde y la marca O de la computadora SHALL mostrarse en rojo, y ambas SHALL permanecer visibles en sus casillas ocupadas, escalables sin distorsión y adaptadas al tamaño disponible y colores del tema actual.

#### Scenario: Estado inicial según el iniciador
- **GIVEN** que la nueva partida ya limpió las marcas y la sesión determinó quién inicia
- **WHEN** se muestra el tablero
- **THEN** si inicia X, las nueve casillas están vacías y el turno corresponde a X; si inicia O, las nueve casillas están vacías, el turno corresponde a computadora, X no puede jugar y no se muestra ningún número

#### Scenario: Apertura de O tras espera
- **GIVEN** que O inicia una partida y la apertura sigue pendiente
- **WHEN** transcurren 1000 ms de primer plano sin cancelación
- **THEN** se realiza exactamente una jugada de O, el turno pasa a X y el tablero coincide con la apertura descrita en game-session

#### Scenario: Marcas conservan su identidad visual
- **GIVEN** que una partida contiene movimientos de ambos participantes
- **WHEN** se muestra el tablero
- **THEN** cada X visible es verde, cada O visible es roja y ninguna marca ocupada desaparece

#### Scenario: Victoria humana o empate no programa O
- **GIVEN** que una jugada de X termina la partida por victoria o empate
- **WHEN** se procesa el desenlace terminal
- **THEN** no se programa ni realiza una jugada de O

### Requirement: Movimientos humanos y turnos

El juego SHALL permitir que la persona coloque únicamente X en una casilla libre durante su turno y mientras la partida esté activa. Toda entrada rechazada MUST dejar sin cambios el tablero y el turno, y MUST NOT iniciar una respuesta de la computadora.

#### Scenario: Movimiento humano válido

- **GIVEN** que la partida está activa, es el turno humano y la casilla seleccionada está libre
- **WHEN** la persona selecciona esa casilla
- **THEN** se coloca una X y el juego pasa a comprobar el desenlace antes de permitir una respuesta de la computadora

#### Scenario: Casilla ocupada rechazada

- **GIVEN** que la persona selecciona una casilla que ya contiene X u O
- **WHEN** intenta colocar una marca
- **THEN** la selección es rechazada, el tablero y el turno permanecen iguales y la computadora no realiza ninguna jugada

#### Scenario: Entrada durante turno de computadora rechazada

- **GIVEN** que la partida está activa pero corresponde jugar a la computadora
- **WHEN** la persona intenta seleccionar una casilla
- **THEN** no se coloca X, el tablero permanece igual y no se altera el turno de la computadora

### Requirement: Desenlace y bloqueo de partidas

El juego MUST detectar una victoria cuando un participante completa tres marcas en una fila, columna o diagonal. MUST declarar empate únicamente cuando el tablero está lleno y no existe una victoria, dando prioridad a la victoria incluso cuando ocupa la última casilla. Después de un desenlace, el estado SHALL ser terminal y MUST NOT admitir más jugadas.

#### Scenario: Victoria en cualquier línea válida

- **GIVEN** que un participante completa tres marcas en una fila, columna o diagonal
- **WHEN** se comprueba el tablero
- **THEN** se declara ganador a ese participante, se muestra un estado comprensible y la partida queda bloqueada

#### Scenario: Empate con tablero lleno

- **GIVEN** que las nueve casillas están ocupadas y ningún participante tiene tres marcas en línea
- **WHEN** se comprueba el tablero
- **THEN** se declara empate, se muestra un estado comprensible y la partida queda bloqueada

#### Scenario: Victoria prioritaria en la última casilla

- **GIVEN** que la última casilla libre completa una línea de tres marcas
- **WHEN** se comprueba el tablero lleno
- **THEN** se declara la victoria y no el empate

#### Scenario: X llena la última casilla sin ganador

- **GIVEN** que X ocupa la última casilla libre sin completar una línea de tres
- **WHEN** se comprueba el tablero lleno
- **THEN** se declara empate y no se realiza ninguna respuesta de O

#### Scenario: Jugada posterior al desenlace rechazada

- **GIVEN** que la partida terminó por victoria o empate
- **WHEN** la persona intenta seleccionar una casilla
- **THEN** no cambia el tablero, no cambia el resultado y no se ejecuta ninguna respuesta de la computadora

### Requirement: Secuencia de respuesta de la computadora
Tras una X válida, el juego MUST comprobar primero si la partida terminó y, solo si continúa, MUST programar una única jugada de O para no antes de completar 1000 ms acumulados en primer plano. Durante la espera SHALL indicar Turno computadora y bloquear jugadas humanas. Tras esa jugada de O, MUST volver a comprobar el desenlace antes de habilitar el siguiente turno humano; la demora MUST mantener la interfaz receptiva.

#### Scenario: X termina la partida
- **GIVEN** que la persona realiza una X válida que completa una línea
- **WHEN** finaliza el procesamiento de la X
- **THEN** se declara la victoria humana, no se programa ni realiza ninguna jugada de O y la partida queda terminal

#### Scenario: O responde una sola vez
- **GIVEN** que una X válida no termina la partida
- **WHEN** transcurren al menos 1000 ms de primer plano desde la aceptación de X y el juego puede procesar la acción
- **THEN** se realiza exactamente una jugada de O y después se comprueba el desenlace; antes del umbral no se realiza

#### Scenario: O espera tras X válida
- **GIVEN** que hay una respuesta de O pendiente
- **WHEN** la interfaz muestra la espera
- **THEN** comunica Turno computadora, rechaza entradas humanas y permanece receptiva

#### Scenario: O termina la partida
- **GIVEN** que la respuesta de O completa una línea
- **WHEN** finaliza el procesamiento de O
- **THEN** se declara la victoria de la computadora y no se habilitan más jugadas humanas

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

### Requirement: Marcador visible

La interfaz SHALL mostrar debajo del estado de la partida tres contadores rotulados y visibles: victorias de X, victorias de O y empates. Después de cada partida terminada, SHALL actualizar únicamente el contador correspondiente y mantener visibles los tres.

#### Scenario: Mostrar marcador sin alterar contadores

- **GIVEN** que se muestra una partida activa o finalizada con sus contadores actuales
- **WHEN** la interfaz muestra el marcador debajo del estado
- **THEN** aparecen los tres rótulos con sus valores actuales y ningún contador cambia por mostrarlos

#### Scenario: Registrar un nuevo desenlace

- **GIVEN** que una partida acaba de terminar y su desenlace aún no fue registrado
- **WHEN** se registra por primera vez ese desenlace
- **THEN** aumenta en uno únicamente el contador correspondiente y los tres contadores permanecen visibles

### Requirement: Estado y acciones accesibles en la interfaz

La interfaz SHALL mostrar un estado comprensible para el turno activo y para cada resultado de victoria o empate. SHALL ofrecer una acción visible y accesible para iniciar una nueva partida sin depender de un botón físico de menú.

#### Scenario: Estado durante una partida activa

- **GIVEN** que una partida está activa
- **WHEN** cambia el turno o se muestra el tablero
- **THEN** la interfaz comunica comprensiblemente si corresponde jugar a la persona o a la computadora

#### Scenario: Estado tras terminar

- **GIVEN** que la partida termina por victoria o empate
- **WHEN** se actualiza la interfaz
- **THEN** se comunica comprensiblemente el resultado y permanece disponible la acción de nueva partida

#### Scenario: Nueva partida accesible

- **GIVEN** que la interfaz está mostrando una partida activa o terminada
- **WHEN** la persona busca iniciar otra partida
- **THEN** encuentra una acción visible y accesible sin usar un botón físico de menú

### Requirement: Presentación adaptativa por orientación
La interfaz SHALL ser operable en portrait y landscape sin fijar orientación ni impedir recreación nativa. En landscape organizará un tablero cuadrado, con 270 dp como referencia adaptable a constraints, a la izquierda y estado, marcador y controles a la derecha; ante límites de espacio o fontScale podrá usar scroll o disposición compacta sin recortar contenido ni reducir objetivos bajo 48 dp.

#### Scenario: Disposición landscape
- **GIVEN** que el área horizontal tiene espacio suficiente para dos paneles legibles y targets mínimos
- **WHEN** se presenta el juego
- **THEN** el tablero cuadrado aparece a la izquierda y estado, marcador y controles se organizan a la derecha

#### Scenario: Disposición portrait
- **GIVEN** que el área disponible es vertical
- **WHEN** se presenta el juego
- **THEN** tablero, estado, marcador y controles permanecen disponibles mediante composición vertical funcional

#### Scenario: Restricciones extremas y accesibilidad
- **GIVEN** que dimensiones o fontScale impiden mostrar todo simultáneamente
- **WHEN** se adapta la presentación
- **THEN** scroll o modo compacto permite alcanzar contenido sin recortes ni objetivos interactivos menores de 48 dp

#### Scenario: Sin barra superior
- **GIVEN** que se muestra cualquiera de las orientaciones
- **WHEN** se renderiza la pantalla
- **THEN** no existe TopAppBar ni título de aplicación y el menú compacto permanece accesible junto a controles

### Requirement: Acción accesible de reset de marcador
El menú compacto SHALL incluir «Reiniciar puntajes» con icono y etiqueta textual accesible, que ponga inmediatamente a cero los tres contadores sin reiniciar ni alterar la partida.

#### Scenario: Acción en menú compacto
- **GIVEN** que se abre menú de opciones
- **WHEN** se recorre con tecnología asistiva
- **THEN** Reiniciar puntajes aparece con icono, etiqueta y acción accesible

#### Scenario: Reset conserva juego
- **GIVEN** que la partida está activa o terminal
- **WHEN** se usa Reiniciar puntajes
- **THEN** los contadores pasan inmediatamente a cero sin cambiar tablero, turno, resultado o iniciadores
