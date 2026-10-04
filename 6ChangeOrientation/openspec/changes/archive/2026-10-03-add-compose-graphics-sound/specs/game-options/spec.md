# Spec Delta

## MODIFIED Requirements

### Requirement: Menú principal de opciones

La interfaz SHALL ofrecer un menú principal en Compose, visible y accesible durante la partida, con exactamente cinco entradas: Nueva partida, Dificultad, Sonido, Acerca de y Salir. Cada entrada MUST mostrar una etiqueta proveniente de recursos y SHALL disponer de un icono asociado. La entrada Nueva partida MUST ejecutar la misma acción que el botón de nueva partida existente; ese botón SHALL permanecer visible y accesible como atajo y MUST mantener las reglas previas de nueva partida. Al elegir una entrada habilitada, el menú SHALL cerrarse antes de ejecutar su acción o mostrar el diálogo correspondiente, evitando ventanas apiladas; una entrada deshabilitada MUST NOT ejecutar acción alguna ni abrir diálogo.

#### Scenario: Menú con las cinco entradas

- **GIVEN** que la interfaz muestra una partida activa o terminada
- **WHEN** la persona abre el menú principal
- **THEN** aparecen las cinco entradas Nueva partida, Dificultad, Sonido, Acerca de y Salir, cada una con su etiqueta y su icono

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
- **THEN** encuentra la entrada Sonido entre las cinco opciones para cambiar el sonido sin reiniciar la partida

#### Scenario: Menú y opciones no alteran la espera de O
- **GIVEN** que O tiene una jugada pendiente
- **WHEN** la persona abre o cierra el menú, o selecciona Sonido o una dificultad permitida
- **THEN** esas acciones no reinician ni adelantan el contador ni causan la jugada; la ejecución normal de O puede ocurrir durante el menú al completarse el tiempo de primer plano

### Requirement: Selección de dificultad

Al abrir la entrada Dificultad, el sistema SHALL mostrar un diálogo de selección con los cuatro niveles Easy, Hard, Expert e Impossible presentados mediante etiquetas de recursos en español: Fácil, Difícil, Experto e Imposible, en ese orden y con la dificultad vigente marcada como opción de radio seleccionada. La dificultad SHALL ser editable únicamente con el tablero vacío antes de la primera jugada (incluido el inicio de partida con X) o después de un resultado terminal. Durante una partida activa, entendida como resultado InProgress con al menos una marca, incluida la apertura automática de O, la entrada Dificultad MUST permanecer visible pero deshabilitada, MUST NOT abrir el diálogo y MUST NOT aceptar selecciones. Al elegir un nivel aceptado, SHALL actualizar la dificultad vigente, cerrar el diálogo y emitir exactamente un Toast con la etiqueta visible del nivel seleccionado. Si el diálogo se descarta sin confirmar por Atrás o por toque fuera cuando proceda, la dificultad vigente MUST permanecer sin cambios. Una selección aceptada MUST NOT reiniciar la partida ni alterar tablero, resultado, marcador, turnos o iniciadores, ni disparar un movimiento de la computadora. Si la selección ocurre con el tablero vacío antes de la primera jugada, el nivel elegido SHALL regir esa misma partida, pendiente aún de su primera jugada, incluida su eventual apertura de O; si ocurre tras un resultado terminal, SHALL aplicarse desde la próxima partida, incluida su apertura de O. En ambos casos el nivel elegido SHALL conservarse en la sesión para las partidas siguientes, sin reiniciar la partida ni alterar el iniciador. El ViewModel MUST rechazar cualquier solicitud de cambio de dificultad durante una partida activa sin alterar la dificultad ni ningún otro estado y sin emitir un Toast de confirmación.

#### Scenario: Apertura muestra el nivel vigente

- **GIVEN** que la dificultad vigente es el nivel Y y que la dificultad es editable
- **WHEN** la persona abre el diálogo de dificultad
- **THEN** el radio de la etiqueta visible de Y aparece marcado como seleccionado

#### Scenario: El selector ofrece los cuatro niveles

- **GIVEN** que la dificultad es editable y la persona abre el diálogo de dificultad
- **WHEN** el diálogo se muestra
- **THEN** aparecen exactamente cuatro radios con las etiquetas Fácil, Difícil, Experto e Imposible en ese orden, con un solo radio marcado

#### Scenario: Reapertura tras elegir Imposible

- **GIVEN** que la dificultad vigente es Imposible y que la dificultad es editable
- **WHEN** la persona reabre el diálogo de dificultad
- **THEN** el radio de la etiqueta visible Imposible aparece marcado como seleccionado

#### Scenario: La selección actualiza, cierra y notifica una sola vez

- **GIVEN** que la dificultad es editable y el diálogo está abierto con el nivel Y marcado
- **WHEN** la persona pulsa otro nivel Z
- **THEN** la dificultad vigente pasa a Z, el diálogo se cierra y se emite exactamente un Toast con la etiqueta visible del nivel Z

#### Scenario: Elegir el radio ya seleccionado

- **GIVEN** que la dificultad es editable y el diálogo está abierto con el nivel Y ya marcado
- **WHEN** la persona pulsa ese mismo radio Y
- **THEN** la dificultad permanece en Y, el diálogo se cierra y se emite exactamente un Toast con la etiqueta visible de Y, sin repetirse por recomposición ni recreación

#### Scenario: Cancelar no cambia la dificultad

- **GIVEN** que la dificultad es editable y el diálogo está abierto con el nivel Y
- **WHEN** se descarta por Atrás o por toque fuera cuando proceda
- **THEN** la dificultad vigente sigue siendo Y y no se emite un Toast de nivel seleccionado

#### Scenario: La entrada está habilitada antes de la primera jugada

- **GIVEN** una partida nueva con el tablero vacío y O aún no ha abierto
- **WHEN** la persona abre el menú y elige Dificultad
- **THEN** la entrada está habilitada, el diálogo se abre y una selección aceptada actualiza la dificultad sin colocar ninguna marca

#### Scenario: La entrada está habilitada tras un desenlace

- **GIVEN** una partida ya terminada por victoria o empate
- **WHEN** la persona abre el menú y elige Dificultad
- **THEN** la entrada está habilitada y el diálogo se abre mostrando el nivel vigente

#### Scenario: Rechazo durante una partida activa

- **GIVEN** una partida activa con al menos una marca, con la dificultad vigente Y y el tablero, marcador, turnos e iniciador definidos
- **WHEN** la persona intenta cambiar la dificultad, o el ViewModel recibe una solicitud directa u obsoleta
- **THEN** la dificultad sigue siendo Y, no se altera ningún otro estado, no se emite Toast de confirmación y no se dispara un movimiento de la computadora

#### Scenario: La apertura automática de O cierra la ventana de edición

- **GIVEN** que la dificultad es editable y la alternancia determina que O inicia la nueva partida
- **WHEN** transcurren 1000 ms de primer plano y O realiza su única jugada inicial
- **THEN** la apertura usa la dificultad vigente y, tras ella, la entrada Dificultad queda deshabilitada durante el resto de la partida activa

#### Scenario: El nivel elegido rige la partida iniciada con él

- **GIVEN** que la dificultad vigente cambió a Z en una ventana permitida (tablero vacío antes de la primera jugada o tras un desenlace)
- **WHEN** la computadora juega en una partida iniciada con ese nivel, incluida la apertura de O de esa partida
- **THEN** la selección del movimiento usa el nivel Z

#### Scenario: Sin repetición de la notificación por recomposición o recreación

- **GIVEN** que se emitió un Toast por una selección de dificultad
- **WHEN** la interfaz se recompone o la pantalla se recrea
- **THEN** no se vuelve a emitir la notificación de esa selección

#### Scenario: Dificultad editable durante espera de apertura
- **GIVEN** que una partida nueva con O iniciador tiene el tablero vacío y su apertura está pendiente
- **WHEN** la persona elige una dificultad permitida antes de la primera jugada
- **THEN** la entrada está habilitada, no se realiza la apertura anticipadamente y la jugada de O usa la dificultad vigente al ejecutarse

#### Scenario: Selección obsoleta durante apertura pendiente
- **GIVEN** que el tablero ya no está vacío cuando llega una solicitud de cambio de dificultad
- **WHEN** el ViewModel recibe la selección obsoleta
- **THEN** la rechaza sin alterar la dificultad ni el estado y sin disparar el movimiento de O

### Requirement: Accesibilidad de opciones y diálogos

Las entradas del menú y los controles de los diálogos SHALL ser visibles y alcanzables, y MUST exponer una etiqueta textual proveniente de recursos para su uso con tecnologías de asistencia, sin requerir gestos especiales ni botones físicos. Cuando una entrada del menú esté deshabilitada, MUST exponer su estado deshabilitado a las tecnologías de asistencia y MUST NOT presentarse como accionable.

#### Scenario: Entradas del menú etiquetadas

- **GIVEN** que el menú principal está abierto
- **WHEN** se recorre con una tecnología de asistencia
- **THEN** cada una de las cinco entradas expone su etiqueta textual y su estado (habilitada o deshabilitada), y expone su acción ejecutable solo cuando está habilitada

#### Scenario: Controles de diálogo alcanzables

- **GIVEN** que un diálogo del menú está abierto
- **WHEN** se recorre con una tecnología de asistencia
- **THEN** sus opciones y botones son alcanzables y exponen una etiqueta textual

#### Scenario: Entrada deshabilitada no accionable

- **GIVEN** que la entrada Dificultad está deshabilitada durante una partida activa
- **WHEN** se recorre el menú con una tecnología de asistencia
- **THEN** la entrada expone su etiqueta y su estado deshabilitado y no se ofrece como acción ejecutable

## ADDED Requirements

### Requirement: Control de sonido
La opción Sonido SHALL alternar el estado habilitado del sonido y SHALL comunicar su estado actual de forma textual y accesible.

#### Scenario: Alternar sonido desde el menú
- **GIVEN** que el menú está abierto durante una partida
- **WHEN** la persona activa o desactiva Sonido
- **THEN** cambia el estado de la opción sin reiniciar ni alterar tablero, turno o marcador
