# game-mode-selection Specification

## Purpose

Define la pantalla inicial que permite elegir entre jugar contra la computadora y jugar en línea, la navegación Back entre las pantallas de la aplicación, la conservación de la pantalla actual y la pausa del modo local mientras su pantalla no está visible.

## ADDED Requirements

### Requirement: Pantalla inicial con dos entradas

Al abrir la aplicación de forma nueva, la interfaz SHALL mostrar una pantalla inicial con exactamente dos entradas: «Jugar contra la computadora» y «Jugar en línea». Cada entrada MUST ser visible, estar etiquetada con texto de recursos y ser accionable por toque y teclado. Elegir «Jugar contra la computadora» SHALL mostrar la partida local en el estado en que se encuentre su sesión, sin iniciar una partida nueva ni modificar marcador, dificultad o sonido. Elegir «Jugar en línea» SHALL mostrar el lobby en línea.

#### Scenario: Arranque de la aplicación

- **GIVEN** que la aplicación se abre sin estado de pantalla guardado
- **WHEN** se muestra la primera pantalla
- **THEN** aparecen exactamente las entradas «Jugar contra la computadora» y «Jugar en línea», y no aparece el tablero

#### Scenario: Elegir la partida local

- **GIVEN** que se muestra la pantalla inicial y la sesión local está en su estado inicial
- **WHEN** la persona elige «Jugar contra la computadora»
- **THEN** se muestra la partida local con el tablero vacío, el marcador y la dificultad vigentes

#### Scenario: Elegir el modo en línea

- **GIVEN** que se muestra la pantalla inicial
- **WHEN** la persona elige «Jugar en línea»
- **THEN** se muestra el lobby en línea

### Requirement: Navegación con Back entre pantallas

El botón Back SHALL aplicar la navegación de la aplicación sin finalizar la tarea salvo en la pantalla inicial. Desde la partida local SHALL volver a la pantalla inicial; desde el lobby SHALL volver a la pantalla inicial (salvo mientras el lobby espera un rival o muestra el diálogo de apodo, casos definidos en `online-lobby`); desde la pantalla de partida en línea SHALL aplicar lo definido en `online-match`. Un menú o diálogo abierto MUST descartarse primero con Back, sin cambiar de pantalla. En la pantalla inicial, Back MUST aplicar la navegación normal del sistema, sin interceptación, y puede llevar la tarea a segundo plano o finalizarla según el comportamiento habitual de Android.

#### Scenario: Back desde la partida local

- **GIVEN** que se muestra la partida local, sin menú ni diálogo abierto
- **WHEN** se pulsa Back
- **THEN** se muestra la pantalla inicial y la tarea no se finaliza

#### Scenario: Back desde el lobby

- **GIVEN** que se muestra el lobby sin espera de rival ni diálogo abierto
- **WHEN** se pulsa Back
- **THEN** se muestra la pantalla inicial

#### Scenario: Back descarta primero un menú o diálogo

- **GIVEN** que se muestra la partida local con el menú de opciones abierto
- **WHEN** se pulsa Back
- **THEN** el menú se cierra y la partida local sigue visible

#### Scenario: Back en la pantalla inicial no se intercepta

- **GIVEN** que se muestra la pantalla inicial
- **WHEN** se pulsa Back
- **THEN** se aplica la navegación normal del sistema, sin que la aplicación cambie de pantalla ni muestre un diálogo propio

### Requirement: Conservación de la pantalla actual

La aplicación SHALL conservar la pantalla actual (inicio, partida local o lobby) al rotar el dispositivo y tras la restauración de la tarea por muerte del proceso. Si la pantalla guardada es una partida en línea, la aplicación MUST restaurar el lobby en su lugar, porque una partida en línea no se reanuda tras muerte del proceso; la rotación sí conserva la partida en línea. Un lanzamiento nuevo, sin estado guardado, SHALL mostrar la pantalla inicial.

#### Scenario: Rotación en la pantalla inicial, la partida local y el lobby

- **GIVEN** que se muestra una de las pantallas inicio, partida local o lobby
- **WHEN** el dispositivo rota
- **THEN** se sigue mostrando la misma pantalla con su estado

#### Scenario: Muerte del proceso con la partida local visible

- **GIVEN** que se muestra la partida local y el sistema finaliza el proceso con estado guardado
- **WHEN** la persona vuelve a la aplicación
- **THEN** se muestra la partida local restaurada según `game-session`

#### Scenario: Muerte del proceso con una partida en línea visible

- **GIVEN** que se muestra una partida en línea y el sistema finaliza el proceso con estado guardado
- **WHEN** la persona vuelve a la aplicación
- **THEN** se muestra el lobby en línea y no se muestra la partida

#### Scenario: Lanzamiento nuevo

- **GIVEN** que la tarea fue finalizada y no hay estado guardado
- **WHEN** se abre la aplicación
- **THEN** se muestra la pantalla inicial

### Requirement: La partida local se conserva al salir y volver

La partida local SHALL conservar tablero, resultado, turno, iniciador siguiente, marcador y sonido de sesión cuando la persona vuelve a la pantalla inicial o visita el modo en línea y luego elige de nuevo «Jugar contra la computadora». Volver a la partida local MUST NOT iniciar una partida nueva ni puntuar de nuevo un resultado ya registrado. Las partidas jugadas en línea MUST NOT modificar el marcador local.

#### Scenario: Volver a una partida local en curso

- **GIVEN** que hay una partida local con jugadas, que la persona pulsó Back hasta la pantalla inicial
- **WHEN** elige de nuevo «Jugar contra la computadora»
- **THEN** se muestran el mismo tablero, turno y marcador, sin una partida nueva

#### Scenario: Volver a una partida local terminada

- **GIVEN** que la partida local terminó y ya fue puntuada, y la persona volvió a la pantalla inicial
- **WHEN** elige de nuevo «Jugar contra la computadora»
- **THEN** se muestran el resultado y el marcador anteriores sin sumar otro punto

#### Scenario: El modo en línea no altera el marcador local

- **GIVEN** que el marcador local tiene valores conocidos
- **WHEN** se juega y termina una partida en línea y la persona regresa a la partida local
- **THEN** el marcador local conserva los mismos valores

### Requirement: El modo local no avanza mientras su pantalla no está visible

Mientras la pantalla de la partida local no esté visible (por mostrarse otra pantalla de la aplicación, o por estar la aplicación en segundo plano), el turno pendiente de la computadora SHALL permanecer detenido, la computadora MUST NOT realizar jugadas y el modo local MUST NOT reproducir sonidos. Al volver a mostrarse la partida local, un turno pendiente SHALL continuar desde el tiempo que le restaba, sin reiniciar su cuenta.

#### Scenario: Turno pendiente detenido al ir a la pantalla inicial

- **GIVEN** que O tiene una jugada pendiente y ya consumió 400 ms con la partida local visible
- **WHEN** la persona pulsa Back y permanece 5 segundos en la pantalla inicial
- **THEN** el tablero local no cambia y no suena ningún efecto

#### Scenario: Turno pendiente al volver

- **GIVEN** el escenario anterior
- **WHEN** la persona elige de nuevo «Jugar contra la computadora»
- **THEN** O realiza su única jugada tras los 600 ms restantes con la pantalla visible

#### Scenario: Sin avance mientras se juega en línea

- **GIVEN** que O tiene una jugada pendiente en la partida local y la persona entra al modo en línea
- **WHEN** transcurre más del tiempo restante en el lobby o en una partida en línea
- **THEN** el tablero local no cambia y el modo local no emite sonido

### Requirement: Orientación y accesibilidad de la pantalla inicial

La pantalla inicial SHALL ser operable en portrait y landscape sin fijar orientación, sin recortar sus entradas con tamaños de fuente escalados (usando desplazamiento o disposición compacta si hace falta). Cada entrada MUST medir al menos 48 dp por lado, exponer su etiqueta de recursos a las tecnologías de asistencia y poder activarse sin gestos especiales.

#### Scenario: Pantalla inicial en landscape o con fuente grande

- **GIVEN** que el dispositivo está en landscape o con la fuente escalada
- **WHEN** se muestra la pantalla inicial
- **THEN** las dos entradas son alcanzables, no están recortadas y miden al menos 48 dp

#### Scenario: Entradas etiquetadas

- **GIVEN** que se muestra la pantalla inicial
- **WHEN** se recorre con una tecnología de asistencia
- **THEN** cada entrada expone su texto como etiqueta y su acción de activación
