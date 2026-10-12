## MODIFIED Requirements

### Requirement: Menú principal de opciones

La interfaz SHALL ofrecer un menú principal en Compose, visible y accesible durante la partida, con exactamente seis entradas: Nueva partida, Dificultad, Sonido, Reiniciar puntajes, Acerca de y Volver al inicio, esta última siempre la última. Cada entrada MUST mostrar una etiqueta proveniente de recursos y SHALL disponer de un icono asociado. La entrada Nueva partida MUST ejecutar la misma acción que el botón de nueva partida existente; ese botón SHALL permanecer visible y accesible como atajo y MUST mantener las reglas previas de nueva partida. Al elegir una entrada habilitada, el menú SHALL cerrarse antes de ejecutar su acción o mostrar el diálogo correspondiente, evitando ventanas apiladas; una entrada deshabilitada MUST NOT ejecutar acción alguna ni abrir diálogo. La entrada Volver al inicio MUST ejecutar la acción definida en `game-mode-selection` («Acción visible para volver a la pantalla inicial»), es siempre accionable y no equivale a Salir. No habrá Salir ni confirmación de salida. Back SHALL descartar un menú o diálogo ordinario según el comportamiento normal y, sin overlay, volver a la pantalla inicial conservando la partida local, sin finalizar la tarea.

#### Scenario: Menú con las cinco entradas

- **GIVEN** que la interfaz muestra una partida activa o terminada
- **WHEN** la persona abre el menú principal
- **THEN** aparecen las cinco entradas Nueva partida, Dificultad, Sonido, Reiniciar puntajes y Acerca de, en ese orden y cada una con su etiqueta y su icono, seguidas de Volver al inicio como sexta y última entrada

#### Scenario: Nueva partida desde el menú respeta las reglas previas

- **GIVEN** que el menú principal está abierto
- **WHEN** la persona elige Nueva partida
- **THEN** se ejecuta la misma acción que el botón existente: se limpia el tablero, se conserva el marcador y se alterna el iniciador según las reglas vigentes

#### Scenario: El botón existente permanece como atajo

- **GIVEN** que la interfaz muestra el juego
- **WHEN** la persona busca iniciar una nueva partida
- **THEN** encuentra el botón de nueva partida visible y accesible y, al usarlo, ejecuta la misma acción que la entrada del menú

#### Scenario: El menú se cierra antes de la acción

- **GIVEN** que el menú principal está abierto
- **WHEN** la persona elige una de sus entradas habilitadas
- **THEN** el menú se cierra antes de ejecutar la acción o de abrir el diálogo correspondiente, sin ventanas apiladas

#### Scenario: Entrada deshabilitada no ejecuta acción

- **GIVEN** que el menú principal está abierto con la entrada Dificultad deshabilitada
- **WHEN** la persona intenta elegir Dificultad
- **THEN** no se abre ningún diálogo, no se ejecuta ninguna acción y la partida y la sesión permanecen sin cambios

#### Scenario: Sonido está disponible durante la partida

- **GIVEN** que la interfaz muestra una partida activa o terminada
- **WHEN** la persona abre el menú principal
- **THEN** encuentra la entrada Sonido entre las seis opciones para cambiar el sonido sin reiniciar la partida

#### Scenario: Menú y opciones no alteran la espera de O

- **GIVEN** que O tiene una jugada pendiente
- **WHEN** la persona abre o cierra el menú, o selecciona Sonido o una dificultad permitida
- **THEN** esas acciones no reinician ni adelantan el contador ni causan la jugada; la ejecución normal de O puede ocurrir durante el menú al completarse el tiempo de primer plano

#### Scenario: Volver al inicio desde el menú

- **GIVEN** que se muestra la partida local con el menú principal abierto
- **WHEN** la persona elige Volver al inicio
- **THEN** el menú se cierra y se muestra la pantalla inicial, sin diálogo de confirmación, y la partida local conserva su estado

#### Scenario: Sin acción Salir

- **GIVEN** que se abre el menú principal
- **WHEN** se recorren sus entradas
- **THEN** no existe Salir ni diálogo de confirmación de salida; Volver al inicio no finaliza la tarea

#### Scenario: Back con overlay

- **GIVEN** que un menú o diálogo ordinario está abierto
- **WHEN** se pulsa Back
- **THEN** se descarta según navegación normal sin cambiar partida

#### Scenario: Back sin overlay

- **GIVEN** que se muestra la partida local y no hay menú ni diálogo
- **WHEN** se pulsa Back
- **THEN** se muestra la pantalla inicial, la partida local conserva su estado y la tarea no se finaliza

### Requirement: Accesibilidad de opciones y diálogos

Las entradas del menú, incluida Volver al inicio, y los controles de los diálogos SHALL ser visibles y alcanzables, y MUST exponer una etiqueta textual proveniente de recursos para su uso con tecnologías de asistencia, sin requerir gestos especiales ni botones físicos. Cuando una entrada del menú esté deshabilitada, MUST exponer su estado deshabilitado a las tecnologías de asistencia y MUST NOT presentarse como accionable.

#### Scenario: Entradas del menú etiquetadas

- **GIVEN** que el menú principal está abierto
- **WHEN** se recorre con una tecnología de asistencia
- **THEN** cada una de las entradas, incluida Volver al inicio, expone su etiqueta textual y su estado (habilitada o deshabilitada), y expone su acción ejecutable solo cuando está habilitada

#### Scenario: Controles de diálogo alcanzables

- **GIVEN** que un diálogo del menú está abierto
- **WHEN** se recorre con una tecnología de asistencia
- **THEN** sus opciones y botones son alcanzables y exponen una etiqueta textual

#### Scenario: Entrada deshabilitada no accionable

- **GIVEN** que la entrada Dificultad está deshabilitada durante una partida activa
- **WHEN** se recorre el menú con una tecnología de asistencia
- **THEN** la entrada expone su etiqueta y su estado deshabilitado y no se ofrece como acción ejecutable
