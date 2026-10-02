## Purpose

Esta capacidad define el comportamiento observable del tres en raya, desde el tablero y los turnos hasta los desenlaces y la respuesta de la computadora, tomando como referencia el PDF (páginas 1-10) y la fuente Java consultada.

## ADDED Requirements

### Requirement: Tablero y marcas visibles

El juego SHALL limpiar las marcas y el resultado de la partida anterior al comenzar una nueva partida, y SHALL mostrar un tablero de tres por tres sin números ni identificadores de consola. Si inicia X, el estado visible SHALL tener nueve casillas vacías; si inicia O, SHALL tener exactamente una O y el turno humano. La marca X humana SHALL mostrarse en verde y la marca O de la computadora SHALL mostrarse en rojo, y ambas SHALL permanecer visibles en sus casillas ocupadas.

#### Scenario: Estado inicial según el iniciador

- **GIVEN** que la nueva partida ya limpió las marcas y la sesión determinó quién inicia
- **WHEN** se muestra el tablero
- **THEN** si inicia X, las nueve casillas están vacías; si inicia O, hay exactamente una O, el turno corresponde a X y no se muestra ningún número

#### Scenario: Marcas conservan su identidad visual

- **GIVEN** que una partida contiene movimientos de ambos participantes
- **WHEN** se muestra el tablero
- **THEN** cada X visible es verde, cada O visible es roja y ninguna marca ocupada desaparece

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

Tras una X válida, el juego MUST comprobar primero si la partida terminó y, solo si continúa, MUST ejecutar una única jugada de O. Tras esa jugada de O, MUST volver a comprobar el desenlace antes de habilitar el siguiente turno humano.

#### Scenario: X termina la partida

- **GIVEN** que la persona realiza una X válida que completa una línea
- **WHEN** finaliza el procesamiento de la X
- **THEN** se declara la victoria humana, no se realiza ninguna jugada de O y la partida queda terminal

#### Scenario: O responde una sola vez

- **GIVEN** que una X válida no termina la partida
- **WHEN** el juego procesa el turno de la computadora
- **THEN** se realiza exactamente una jugada de O y después se comprueba el desenlace

#### Scenario: O termina la partida

- **GIVEN** que la respuesta de O completa una línea
- **WHEN** finaliza el procesamiento de O
- **THEN** se declara la victoria de la computadora y no se habilitan más jugadas humanas

### Requirement: Prioridad y selección de la inteligencia artificial

Cuando la computadora deba jugar, MUST elegir primero una casilla que complete una victoria inmediata propia si existe. Si no puede ganar y existe al menos una casilla donde X ganaría en su siguiente movimiento, MUST ocupar una de esas casillas amenazadas. Si hay varias amenazas simultáneas, no se garantiza bloquearlas todas. Si no existe ninguna victoria inmediata propia ni amenaza humana, MUST elegir una casilla libre al azar.

#### Scenario: Ganar tiene prioridad sobre bloquear

- **GIVEN** que O tiene una victoria inmediata disponible y X también tiene una victoria inmediata disponible
- **WHEN** la computadora elige su movimiento
- **THEN** ocupa la casilla que completa la victoria de O en lugar de bloquear a X

#### Scenario: Bloquear una amenaza inmediata cuando no puede ganar

- **GIVEN** que O no tiene una victoria inmediata y X tiene una única casilla que le daría la victoria en su siguiente movimiento
- **WHEN** la computadora elige su movimiento
- **THEN** ocupa la casilla que evita la victoria inmediata de X

#### Scenario: Amenazas humanas múltiples

- **GIVEN** que O no tiene una victoria inmediata y X tiene varias casillas que le darían la victoria en su siguiente movimiento
- **WHEN** la computadora elige su movimiento
- **THEN** O ocupa una de esas casillas amenazadas, sin garantía de evitar otra amenaza simultánea

#### Scenario: Elegir al azar sin prioridad táctica

- **GIVEN** que O no puede ganar inmediatamente y X no puede ganar inmediatamente
- **WHEN** la computadora elige su movimiento
- **THEN** ocupa una casilla libre elegida al azar

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
