# Spec Delta

## MODIFIED Requirements

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
