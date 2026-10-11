# online-lobby Specification

## Purpose

Define el lobby del modo en línea: el apodo del jugador, la conexión con el servicio, la lista en tiempo real de juegos disponibles, la creación y cancelación de un juego, unirse a un juego y la regla de que solo se juega tras unirse.

## ADDED Requirements

### Requirement: Apodo al entrar por primera vez

Al elegir «Jugar en línea» sin un apodo guardado, la aplicación SHALL pedirlo en un diálogo con el título «Tu apodo», un campo «Apodo» y los botones «Aceptar» y «Cancelar», antes de mostrar el contenido del lobby. Si ya hay un apodo guardado, el diálogo MUST NOT aparecer. Descartar el diálogo de la primera vez (con «Cancelar» o con Back) SHALL volver a la pantalla inicial sin guardar apodo.

#### Scenario: Primera vez sin apodo

- **GIVEN** que no hay un apodo guardado
- **WHEN** la persona elige «Jugar en línea»
- **THEN** aparece el diálogo «Tu apodo» con el campo «Apodo» vacío y el lobby no muestra su lista hasta aceptar un apodo válido

#### Scenario: Con apodo guardado no se pregunta

- **GIVEN** que hay un apodo guardado
- **WHEN** la persona elige «Jugar en línea»
- **THEN** se muestra el lobby sin el diálogo de apodo

#### Scenario: Descartar el diálogo de la primera vez

- **GIVEN** que el diálogo de apodo se muestra por primera vez
- **WHEN** la persona pulsa «Cancelar» o Back
- **THEN** se muestra la pantalla inicial y no se guarda ningún apodo

### Requirement: Validación del apodo

El apodo SHALL recortarse de espacios al inicio y al final y MUST tener entre 1 y 20 caracteres después del recorte. Al pulsar «Aceptar» con un apodo que no cumple, la aplicación SHALL rechazar el valor y mostrar el texto «Escribe un apodo de 1 a 20 caracteres» sin cerrar el diálogo, y MUST guardar el valor ya recortado cuando cumple.

#### Scenario: Apodo vacío o solo espacios

- **GIVEN** que el diálogo de apodo está abierto
- **WHEN** el campo está vacío o contiene solo espacios y se intenta aceptar
- **THEN** no se guarda el apodo, el diálogo sigue abierto y se muestra «Escribe un apodo de 1 a 20 caracteres»

#### Scenario: Apodo demasiado largo

- **GIVEN** que el diálogo de apodo está abierto
- **WHEN** el campo contiene 21 caracteres tras el recorte y se intenta aceptar
- **THEN** no se guarda el apodo, el diálogo sigue abierto y se muestra «Escribe un apodo de 1 a 20 caracteres»

#### Scenario: Apodo válido con espacios sobrantes

- **GIVEN** que el diálogo de apodo está abierto
- **WHEN** la persona escribe «  Ana  » y acepta
- **THEN** se guarda «Ana», el diálogo se cierra y se muestra el lobby

#### Scenario: Apodo de 20 caracteres

- **GIVEN** que el diálogo de apodo está abierto
- **WHEN** la persona escribe exactamente 20 caracteres y acepta
- **THEN** el apodo se guarda completo

### Requirement: El apodo se recuerda y se puede cambiar

El apodo guardado SHALL conservarse al cerrar y volver a abrir la aplicación. El lobby SHALL ofrecer la acción «Cambiar apodo», que reabre el mismo diálogo con el apodo vigente y aplica la misma validación; descartarlo con «Cancelar» o Back MUST conservar el apodo vigente. Un cambio aceptado SHALL aplicarse a los juegos que se creen después, sin modificar juegos ya creados.

#### Scenario: Apodo recordado tras reabrir la aplicación

- **GIVEN** que se guardó el apodo «Ana»
- **WHEN** se cierra la tarea, se abre de nuevo la aplicación y se elige «Jugar en línea»
- **THEN** no se pide apodo y el lobby usa «Ana»

#### Scenario: Cambiar el apodo

- **GIVEN** que el lobby se muestra con el apodo «Ana»
- **WHEN** la persona usa «Cambiar apodo», escribe «Luis» y acepta
- **THEN** el apodo guardado pasa a «Luis» y los juegos creados desde entonces muestran «Juego de Luis»

#### Scenario: Cancelar el cambio de apodo

- **GIVEN** que el diálogo se abrió desde «Cambiar apodo» con el apodo «Ana»
- **WHEN** la persona pulsa «Cancelar» o Back
- **THEN** el diálogo se cierra, el apodo sigue siendo «Ana» y se sigue mostrando el lobby

### Requirement: Conexión con el servicio

Mientras la aplicación establece la identidad anónima y la conexión con el servicio al entrar al lobby, SHALL mostrar el texto «Conectando…» en lugar de la lista. Si no puede establecer la identidad, SHALL mostrar «No se pudo conectar con el servicio» con la acción «Reintentar», que MUST repetir el intento. Si la conexión se pierde estando en el lobby, SHALL mostrar «Sin conexión», MUST deshabilitar crear juego y unirse, y MUST recuperarse sola al volver la conexión sin acción de la persona.

#### Scenario: Estado de carga

- **GIVEN** que la identidad anónima aún no se ha establecido
- **WHEN** se muestra el lobby
- **THEN** aparece «Conectando…» y no aparece la lista ni «No hay juegos disponibles»

#### Scenario: Fallo de identidad y reintento

- **GIVEN** que el servicio rechaza o no completa la identidad anónima
- **WHEN** se muestra el lobby
- **THEN** aparece «No se pudo conectar con el servicio» con «Reintentar» y, al pulsar «Reintentar», se repite el intento y vuelve a aparecer «Conectando…»

#### Scenario: Pérdida de conexión en el lobby

- **GIVEN** que el lobby muestra la lista
- **WHEN** se pierde la conexión con el servicio
- **THEN** aparece «Sin conexión» y las acciones de crear y unirse quedan deshabilitadas y expuestas como deshabilitadas

#### Scenario: Recuperación automática

- **GIVEN** que el lobby muestra «Sin conexión»
- **WHEN** vuelve la conexión
- **THEN** desaparece «Sin conexión», se muestra la lista vigente y las acciones vuelven a estar habilitadas sin pulsar nada

### Requirement: Lista de juegos disponibles en tiempo real

El lobby SHALL mostrar el encabezado «Juegos disponibles» y, debajo, un elemento por cada juego que esté esperando rival, con el texto «Juego de <apodo>» donde <apodo> es el apodo de quien lo creó. La lista SHALL actualizarse sin acción de la persona cuando se crea, se toma o se retira un juego. Si no hay juegos esperando rival, SHALL mostrarse «No hay juegos disponibles». Un juego que ya tiene rival o ya terminó MUST NOT aparecer.

#### Scenario: Lista con juegos

- **GIVEN** que dos personas crearon juegos con los apodos «Ana» y «Luis» y esperan rival
- **WHEN** se muestra el lobby
- **THEN** aparecen los elementos «Juego de Ana» y «Juego de Luis» bajo «Juegos disponibles»

#### Scenario: Lista vacía

- **GIVEN** que ningún juego espera rival
- **WHEN** se muestra el lobby
- **THEN** aparece «No hay juegos disponibles»

#### Scenario: Aparece un juego nuevo sin recargar

- **GIVEN** que el lobby muestra «No hay juegos disponibles»
- **WHEN** otro dispositivo crea un juego
- **THEN** aparece «Juego de <apodo>» sin acción de la persona

#### Scenario: Un juego tomado sale de la lista

- **GIVEN** que el lobby muestra «Juego de Ana»
- **WHEN** otro jugador se une a ese juego
- **THEN** «Juego de Ana» desaparece de la lista sin acción de la persona

### Requirement: Crear un juego y esperar rival

Al elegir «Crear juego», la aplicación SHALL crear un juego a nombre del apodo vigente, que aparece en la lista de los demás como «Juego de <apodo>», y SHALL mostrar a quien lo creó la pantalla de espera con el texto «Esperando rival…» y la acción «Cancelar». Mientras espera, la persona MUST NOT ver un tablero accionable, y la aplicación SHALL mantener la pantalla encendida mientras se muestra «Esperando rival…», sin mantenerla encendida en el lobby ni en la pantalla inicial. Cuando un rival se une, la aplicación SHALL mostrar la partida en línea. Si el servicio no completa la creación del juego, la aplicación MUST permanecer en el lobby, SHALL mostrar el texto «No se pudo crear el juego», MUST NOT mostrar «Esperando rival…» y MUST NOT dejar ningún juego en la lista de los demás; la persona SHALL poder intentarlo de nuevo con «Crear juego», sin una acción de reintento propia.

#### Scenario: Crear un juego

- **GIVEN** que el lobby se muestra con el apodo «Ana» y hay conexión
- **WHEN** la persona pulsa «Crear juego»
- **THEN** se muestra «Esperando rival…» con «Cancelar» y, en otro dispositivo, el lobby lista «Juego de Ana»

#### Scenario: Fallo al crear el juego

- **GIVEN** que el lobby se muestra con conexión
- **WHEN** la persona pulsa «Crear juego» y el servicio no completa la creación
- **THEN** se sigue mostrando el lobby con «No se pudo crear el juego», no aparece «Esperando rival…» y el juego no figura en la lista de otros dispositivos

#### Scenario: Pantalla encendida mientras se espera

- **GIVEN** que se muestra «Esperando rival…»
- **WHEN** la persona no toca la pantalla durante más tiempo que el de apagado automático del dispositivo
- **THEN** la pantalla permanece encendida; al cancelar y volver al lobby, la aplicación deja de mantenerla encendida

#### Scenario: Un rival se une

- **GIVEN** que la persona espera rival con su juego
- **WHEN** otro jugador se une
- **THEN** se muestra la partida en línea a ambos

### Requirement: Cancelar un juego en espera

Quien espera rival SHALL poder cancelar su juego con «Cancelar» o con Back, sin diálogo de confirmación. Cancelar MUST retirar el juego de la lista de los demás y volver al lobby.

#### Scenario: Cancelar con el botón

- **GIVEN** que se muestra «Esperando rival…»
- **WHEN** la persona pulsa «Cancelar»
- **THEN** no aparece confirmación, se muestra el lobby y el juego desaparece de la lista de los demás dispositivos

#### Scenario: Cancelar con Back

- **GIVEN** que se muestra «Esperando rival…»
- **WHEN** se pulsa Back
- **THEN** no aparece confirmación, se cancela el juego y se muestra el lobby, no la pantalla inicial

### Requirement: Un juego desaparece si su creador se va

Si quien creó un juego deja la aplicación o pierde la conexión mientras espera rival y el servicio lo detecta, el juego MUST desaparecer de la lista de los demás sin que estos recarguen. Si el juego propio de quien espera desaparece por una causa ajena a su acción, la aplicación SHALL volver al lobby y mostrar «Este juego ya no está disponible». Esto incluye el caso en que el propio dispositivo pierde la conexión, o deja la aplicación en segundo plano el tiempo suficiente para que el sistema corte su red, mientras la persona espera rival: su juego desaparece y, al volver, ve el lobby con ese mensaje.

#### Scenario: El creador sale de la aplicación

- **GIVEN** que el lobby de otro dispositivo muestra «Juego de Ana»
- **WHEN** Ana cierra la aplicación mientras espera rival y el servicio detecta la desconexión
- **THEN** «Juego de Ana» desaparece de la lista sin acción de la persona

#### Scenario: El juego en espera se elimina externamente

- **GIVEN** que la persona espera rival con su juego
- **WHEN** el juego se elimina del servicio por una causa ajena a ella
- **THEN** se muestra el lobby con el mensaje «Este juego ya no está disponible»

#### Scenario: El propio dispositivo pierde la conexión mientras espera

- **GIVEN** que la persona espera rival con su juego
- **WHEN** su dispositivo pierde la conexión, o la aplicación pasa a segundo plano el tiempo suficiente para que el sistema corte su red, y luego la persona vuelve
- **THEN** se muestra el lobby con el mensaje «Este juego ya no está disponible» y el juego no figura en la lista de los demás dispositivos

### Requirement: Unirse a un juego disponible

Al elegir un elemento «Juego de <apodo>» con conexión, la aplicación SHALL unir a la persona como segundo jugador de ese juego y mostrar la partida en línea a ambos jugadores. Un juego MUST aceptar como máximo un segundo jugador.

#### Scenario: Unirse con éxito

- **GIVEN** que el lobby muestra «Juego de Ana» y Ana espera rival
- **WHEN** la persona elige ese elemento
- **THEN** la persona y Ana ven la partida en línea y el juego deja de estar en la lista

### Requirement: Juego no disponible al unirse

Si al unirse el juego ya fue tomado por otra persona o fue retirado, la aplicación SHALL mostrar «Este juego ya no está disponible», MUST permanecer en el lobby y MUST NOT unir a la persona a ninguna partida. Solo una de dos personas que eligen el mismo juego a la vez SHALL quedar unida.

#### Scenario: Dos personas eligen el mismo juego

- **GIVEN** que dos dispositivos muestran «Juego de Ana»
- **WHEN** ambos lo eligen casi al mismo tiempo
- **THEN** exactamente uno ve la partida en línea y el otro ve «Este juego ya no está disponible» y permanece en el lobby

#### Scenario: Juego retirado antes de unirse

- **GIVEN** que el lobby muestra «Juego de Ana» y Ana lo cancela
- **WHEN** la persona elige ese elemento antes de que desaparezca de su pantalla
- **THEN** aparece «Este juego ya no está disponible» y la persona permanece en el lobby

### Requirement: Solo se juega después de unirse a un juego

La aplicación MUST NOT permitir marcar casillas en línea hasta que el juego tenga dos jugadores. El lobby y la pantalla de espera SHALL NOT mostrar un tablero accionable. Quien crea un juego y espera rival SHALL NOT poder marcar; el tablero en línea SHALL aparecer solo cuando el juego tenga a sus dos jugadores.

#### Scenario: Sin tablero en el lobby

- **GIVEN** que la persona está en el lobby sin haberse unido a un juego
- **WHEN** revisa la pantalla
- **THEN** no hay un tablero ni casillas accionables

#### Scenario: Sin tablero mientras se espera rival

- **GIVEN** que la persona creó un juego y espera rival
- **WHEN** revisa la pantalla
- **THEN** solo ve «Esperando rival…» y «Cancelar», sin tablero accionable

#### Scenario: Tablero al completarse los dos jugadores

- **GIVEN** que una persona creó un juego y otra se unió
- **WHEN** se muestra la partida
- **THEN** ambas ven el tablero y solo quien tiene el turno puede marcar

### Requirement: Orientación y accesibilidad del lobby

El lobby, el diálogo de apodo y la pantalla de espera SHALL ser operables en portrait y landscape sin fijar orientación y sin recortar contenido con fuentes escaladas (con desplazamiento o disposición compacta si hace falta). Cada acción MUST medir al menos 48 dp, exponer una etiqueta de recursos y exponer su estado deshabilitado a las tecnologías de asistencia cuando lo esté; los elementos de la lista MUST ser alcanzables y exponer su texto «Juego de <apodo>» como etiqueta.

#### Scenario: Lobby en landscape con una lista larga

- **GIVEN** que el dispositivo está en landscape y hay más juegos de los que caben
- **WHEN** se muestra el lobby
- **THEN** la lista se desplaza, ningún elemento queda recortado de forma inalcanzable y cada acción mide al menos 48 dp

#### Scenario: Acciones deshabilitadas expuestas

- **GIVEN** que el lobby muestra «Sin conexión»
- **WHEN** se recorre con una tecnología de asistencia
- **THEN** «Crear juego» y los elementos de la lista exponen su etiqueta y su estado deshabilitado y no se ofrecen como acción ejecutable

#### Scenario: Rotación en el lobby

- **GIVEN** que el lobby muestra la lista, o la pantalla de espera
- **WHEN** el dispositivo rota
- **THEN** se mantiene la misma pantalla, sin duplicar ni cancelar el juego en espera
