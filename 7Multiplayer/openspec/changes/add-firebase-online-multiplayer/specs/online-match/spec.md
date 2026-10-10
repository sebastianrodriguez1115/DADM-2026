# online-match Specification

## Purpose

Define la partida en línea entre dos jugadores: asignación de marcas y turno inicial, jugadas confirmadas por el servicio y sincronizadas en ambos dispositivos, resultado, abandono del rival, salida, sonido y comportamiento ante rotación, segundo plano y falta de conexión.

## ADDED Requirements

### Requirement: Dos jugadores, marcas y turno inicial

Una partida en línea SHALL tener exactamente dos jugadores: quien creó el juego y quien se unió. Quien creó el juego MUST llevar X y MUST tener el primer turno; quien se unió MUST llevar O. Cada jugador SHALL ver qué marca lleva con el texto «Juegas con X» o «Juegas con O» y el apodo de su rival con el texto «Rival: <apodo>». El tablero SHALL comenzar vacío.

#### Scenario: Marcas y turno inicial

- **GIVEN** que Ana creó un juego y Luis se unió
- **WHEN** se muestra la partida en ambos dispositivos
- **THEN** Ana ve «Juegas con X», «Rival: Luis» y «Tu turno»; Luis ve «Juegas con O», «Rival: Ana» y «Turno de Ana»; el tablero está vacío en ambos

#### Scenario: Un tercer jugador no entra

- **GIVEN** que Ana y Luis juegan una partida
- **WHEN** una tercera persona intenta unirse a ese juego
- **THEN** no se une, su aplicación muestra «Este juego ya no está disponible» y la partida de Ana y Luis no cambia

### Requirement: Solo marca quien tiene el turno

Una jugada SHALL aceptarse únicamente si la partida está en curso, es el turno de la marca del jugador y la casilla está libre. Las casillas MUST permanecer deshabilitadas, y su estado deshabilitado MUST exponerse a las tecnologías de asistencia, cuando no es el turno del jugador, cuando la partida terminó o fue abandonada, cuando no hay conexión o mientras una jugada propia espera confirmación. Una casilla ocupada MUST NOT aceptar otra marca.

#### Scenario: Marca en su turno

- **GIVEN** que es el turno de Ana (X) y la casilla 4 está libre
- **WHEN** Ana toca la casilla 4
- **THEN** la partida acepta la jugada y la casilla 4 muestra X en ambos dispositivos

#### Scenario: Intento fuera de turno

- **GIVEN** que es el turno de Ana
- **WHEN** Luis toca una casilla libre en su dispositivo
- **THEN** la casilla no responde, el tablero no cambia en ningún dispositivo y su estado deshabilitado está expuesto

#### Scenario: Casilla ocupada

- **GIVEN** que la casilla 4 ya tiene una marca
- **WHEN** el jugador con el turno la toca
- **THEN** no se acepta jugada, la casilla conserva su marca y el turno no cambia

#### Scenario: Segunda entrada mientras una jugada espera confirmación

- **GIVEN** que Ana tocó una casilla y la jugada aún no fue confirmada
- **WHEN** Ana toca otra casilla libre
- **THEN** la segunda entrada no se acepta

### Requirement: Las jugadas se muestran cuando el servicio las confirma

La aplicación SHALL mostrar una jugada propia solo cuando el servicio la haya confirmado. Si el servicio no confirma una jugada (porque se rechaza o falla), el tablero MUST permanecer sin cambios y el turno MUST permanecer en el jugador. Un usuario que no participa en la partida MUST NOT poder alterarla, y un participante MUST NOT poder cambiar el tablero fuera de su turno.

#### Scenario: Jugada confirmada

- **GIVEN** que Ana toca una casilla libre en su turno y hay conexión
- **WHEN** el servicio confirma la jugada
- **THEN** la marca aparece en el tablero de Ana y el turno pasa a Luis

#### Scenario: Jugada no confirmada

- **GIVEN** que Ana toca una casilla libre pero el servicio rechaza o no completa la escritura
- **WHEN** termina el intento
- **THEN** el tablero de Ana no muestra la marca, el turno sigue siendo de Ana y puede intentar de nuevo

#### Scenario: Usuario que no participa

- **GIVEN** que Ana y Luis juegan y un usuario autenticado que no es ninguno de los dos
- **WHEN** intenta escribir en la partida con el simulador de reglas del servicio o con un cliente propio
- **THEN** el servicio rechaza la escritura y la partida no cambia

#### Scenario: Participante fuera de su turno

- **GIVEN** que es el turno de Ana
- **WHEN** Luis intenta cambiar el tablero con un cliente propio
- **THEN** el servicio rechaza la escritura y el tablero no cambia

### Requirement: Sincronización en ambos dispositivos

Una jugada confirmada SHALL aparecer en el dispositivo del rival sin que este recargue ni toque la pantalla, y el turno MUST pasar al rival en ambos dispositivos. El texto de turno SHALL ser «Tu turno» cuando es el turno del jugador y «Turno de <apodo>» con el apodo del rival cuando es el del rival.

#### Scenario: La jugada llega al rival

- **GIVEN** que es el turno de Ana
- **WHEN** Ana juega y el servicio confirma
- **THEN** el dispositivo de Luis muestra la marca X en la casilla y pasa de «Turno de Ana» a «Tu turno» sin acción de Luis

#### Scenario: Textos de turno

- **GIVEN** que Ana (X) y Luis (O) juegan y es el turno de Luis
- **WHEN** se muestran ambas pantallas
- **THEN** Luis ve «Tu turno» y Ana ve «Turno de Luis»

### Requirement: Resultado de la partida

Cuando una jugada confirmada completa tres en línea o llena el tablero, la partida SHALL terminar y ambos jugadores MUST ver el resultado: «Ganaste» al ganador, «Ganó <apodo>» al perdedor con el apodo del ganador, y «Empate» a ambos si no hay ganador. El tablero MUST quedar bloqueado y MUST seguir mostrando la posición final. Una vez visto el resultado, MUST permanecer aunque el rival salga o se desconecte después.

#### Scenario: Victoria

- **GIVEN** que Ana (X) completa una línea en su jugada
- **WHEN** el servicio confirma
- **THEN** Ana ve «Ganaste», Luis ve «Ganó Ana» y el tablero queda bloqueado en ambos

#### Scenario: Empate

- **GIVEN** que la novena jugada llena el tablero sin línea
- **WHEN** el servicio confirma
- **THEN** ambos ven «Empate» y el tablero queda bloqueado

#### Scenario: Resultado visible tras la salida del rival

- **GIVEN** que ambos ven el resultado de una partida terminada
- **WHEN** el rival pulsa «Volver al lobby» o se desconecta
- **THEN** el otro jugador sigue viendo el mismo resultado, sin «Tu rival salió de la partida»

### Requirement: Fin de partida sin revancha

Al terminar, la partida SHALL ofrecer a cada jugador la acción «Volver al lobby» y MUST NOT ofrecer revancha. Cada jugador SHALL volver al lobby por su cuenta al activarla, o con Back, sin diálogo de confirmación; el otro jugador MUST NOT ser movido de pantalla por ello.

#### Scenario: Volver al lobby

- **GIVEN** que la partida terminó
- **WHEN** un jugador pulsa «Volver al lobby»
- **THEN** ve el lobby y no existe una acción de revancha en la pantalla de resultado

#### Scenario: Cada jugador vuelve por su cuenta

- **GIVEN** que la partida terminó y ambos ven el resultado
- **WHEN** solo uno pulsa «Volver al lobby»
- **THEN** el otro sigue viendo el resultado con «Volver al lobby» hasta que decida

#### Scenario: Back con la partida terminada

- **GIVEN** que la partida terminó
- **WHEN** se pulsa Back
- **THEN** se muestra el lobby sin diálogo de confirmación

### Requirement: Abandono del rival

Si el rival sale de una partida en curso, o deja la aplicación o pierde la conexión y el servicio lo detecta, la partida SHALL quedar abandonada sin ganador, el jugador que permanece SHALL ver el texto «Tu rival salió de la partida» con la acción «Volver al lobby», el tablero MUST quedar bloqueado y la partida MUST NOT puntuar. La detección por desconexión SHALL ser inmediata, sin periodo de gracia.

#### Scenario: El rival sale con confirmación

- **GIVEN** que la partida está en curso
- **WHEN** un jugador confirma «Abandonar»
- **THEN** el otro jugador ve «Tu rival salió de la partida» y «Volver al lobby», sin ganador

#### Scenario: El rival se desconecta

- **GIVEN** que la partida está en curso
- **WHEN** el dispositivo del rival cierra la aplicación o pierde la red y el servicio lo detecta
- **THEN** el otro jugador ve «Tu rival salió de la partida» y «Volver al lobby», sin ganador

#### Scenario: Volver tras el abandono

- **GIVEN** que se muestra «Tu rival salió de la partida»
- **WHEN** la persona pulsa «Volver al lobby» o Back
- **THEN** se muestra el lobby sin diálogo de confirmación

### Requirement: Salir de una partida en curso con confirmación

Con la partida en curso, la pantalla SHALL ofrecer la acción «Salir», y tanto «Salir» como Back MUST mostrar el diálogo «¿Abandonar la partida?» con las acciones «Cancelar» y «Abandonar» en lugar de salir directamente. «Cancelar» MUST cerrar el diálogo y continuar la partida sin cambios. «Abandonar» SHALL abandonar la partida y mostrar el lobby.

#### Scenario: Salir con el botón

- **GIVEN** que la partida está en curso
- **WHEN** la persona pulsa «Salir»
- **THEN** aparece «¿Abandonar la partida?» con «Cancelar» y «Abandonar» y la partida sigue en curso detrás

#### Scenario: Back en curso

- **GIVEN** que la partida está en curso
- **WHEN** se pulsa Back
- **THEN** aparece «¿Abandonar la partida?» y no se cambia de pantalla

#### Scenario: Cancelar el abandono

- **GIVEN** que aparece «¿Abandonar la partida?»
- **WHEN** la persona pulsa «Cancelar»
- **THEN** el diálogo se cierra y el tablero, el turno y la partida continúan sin cambios

#### Scenario: Confirmar el abandono

- **GIVEN** que aparece «¿Abandonar la partida?»
- **WHEN** la persona pulsa «Abandonar»
- **THEN** se muestra el lobby y el rival ve «Tu rival salió de la partida»

### Requirement: El marcador local no interviene

Una partida en línea, sea cual sea su resultado, MUST NOT modificar el marcador local ni su persistencia, y la pantalla en línea MUST NOT mostrar el marcador. La pantalla en línea SHALL NOT tener menú de opciones: las opciones de dificultad y de reinicio de puntajes pertenecen al modo local.

#### Scenario: Marcador local intacto

- **GIVEN** que el marcador local vale 2 victorias de X, 1 victoria de O y 0 empates
- **WHEN** se gana, pierde o empata una partida en línea
- **THEN** el marcador local sigue valiendo 2 victorias de X, 1 victoria de O y 0 empates al volver a la partida local y tras reabrir la aplicación

#### Scenario: Sin marcador ni menú de opciones

- **GIVEN** que se muestra una partida en línea
- **WHEN** se revisa la pantalla
- **THEN** no hay marcador ni menú de opciones con Dificultad o Reiniciar puntajes

### Requirement: Sonido de las partidas en línea

La partida en línea SHALL reproducir los mismos efectos de X y de O que el modo local, como máximo una vez por jugada aceptada, sea propia o del rival, cuando la jugada se confirma y se muestra. MUST NOT reproducir efectos al resincronizar el estado ya existente (por ejemplo al abrir la partida, rotar o volver de segundo plano) ni por un abandono o un resultado sin jugada nueva. El sonido SHALL regirse por la opción «Sonido» de la sesión del menú del modo local, sin control nuevo en las pantallas en línea, y MUST NOT reproducirse con la aplicación en segundo plano.

#### Scenario: Jugada propia confirmada

- **GIVEN** que el sonido está habilitado y la aplicación en primer plano
- **WHEN** el servicio confirma una jugada propia
- **THEN** se reproduce una vez el efecto de la marca del jugador

#### Scenario: Jugada del rival

- **GIVEN** que el sonido está habilitado y la aplicación en primer plano
- **WHEN** llega la jugada confirmada del rival
- **THEN** se reproduce una vez el efecto de la marca del rival

#### Scenario: Sin sonido al abrir o rotar

- **GIVEN** que la partida tiene marcas en el tablero
- **WHEN** se abre la partida o el dispositivo rota
- **THEN** no se reproduce ningún efecto

#### Scenario: Sonido silenciado

- **GIVEN** que la opción «Sonido» del modo local está deshabilitada
- **WHEN** se confirma una jugada en la partida en línea
- **THEN** no se reproduce ningún efecto

#### Scenario: Jugada con la aplicación en segundo plano

- **GIVEN** que la aplicación está en segundo plano
- **WHEN** el rival juega
- **THEN** no se reproduce ningún efecto, y al volver a primer plano no suena ninguno por esa jugada

### Requirement: Rotación y segundo plano

La partida en línea SHALL conservarse al rotar el dispositivo, con tablero, turno, resultado y apodos, sin repetir jugadas ni sonidos. Al pasar la aplicación a segundo plano, la partida SHALL continuar mientras se conserve la conexión; al volver, el jugador SHALL ver el estado vigente. Si la conexión se perdió y el servicio dio por terminada la partida, al volver la persona SHALL ver el estado de abandono, con el mismo texto «Tu rival salió de la partida» y la acción «Volver al lobby», en lugar de la partida en curso.

#### Scenario: Rotación en curso

- **GIVEN** que hay una partida con jugadas
- **WHEN** el dispositivo rota
- **THEN** se muestran el mismo tablero, turno y apodos, sin sonidos nuevos

#### Scenario: Volver de segundo plano con conexión

- **GIVEN** que la aplicación pasó a segundo plano y se conservó la conexión
- **WHEN** el rival jugó durante ese tiempo y la persona vuelve
- **THEN** ve el tablero con la jugada del rival y el turno vigente

#### Scenario: Volver de segundo plano tras perder la conexión

- **GIVEN** que la aplicación pasó a segundo plano y el servicio dio por abandonada la partida por la desconexión
- **WHEN** la persona vuelve
- **THEN** ve el abandono con «Volver al lobby» en lugar de la partida en curso

### Requirement: Sin conexión durante la partida

Mientras no haya conexión con el servicio durante una partida en línea en curso, la pantalla SHALL mostrar «Sin conexión», las casillas MUST permanecer deshabilitadas y ninguna jugada MUST aceptarse. Al volver la conexión mientras la partida sigue existiendo, el aviso SHALL desaparecer solo y la entrada SHALL habilitarse de nuevo si es el turno del jugador.

#### Scenario: Pérdida breve de conexión

- **GIVEN** que es el turno de la persona en una partida en curso
- **WHEN** se pierde la conexión
- **THEN** aparece «Sin conexión», las casillas quedan deshabilitadas y expuestas como deshabilitadas

#### Scenario: Recuperación con la partida vigente

- **GIVEN** que se muestra «Sin conexión» y la partida sigue existiendo en el servicio
- **WHEN** vuelve la conexión
- **THEN** desaparece «Sin conexión» y, si es su turno, la persona puede marcar sin acción adicional

### Requirement: Orientación y accesibilidad de la partida en línea

La pantalla de la partida en línea SHALL ser operable en portrait y landscape sin fijar orientación, con las mismas reglas adaptativas de la partida local: en landscape suficientemente ancho, el tablero cuadrado a la izquierda y el estado y las acciones a la derecha; con poco espacio o fuente escalada, desplazamiento o disposición compacta sin recortar contenido. Las nueve casillas MUST medir al menos 48 dp por lado y exponer a TalkBack su ubicación, contenido y disponibilidad; los textos de turno y resultado, «Salir», «Volver al lobby» y las acciones del diálogo MUST exponer una etiqueta de recursos y su estado deshabilitado cuando lo estén.

#### Scenario: Partida en landscape

- **GIVEN** que el dispositivo está en landscape con una partida en curso
- **WHEN** se muestra la pantalla
- **THEN** el tablero aparece a la izquierda y el estado y las acciones a la derecha, sin recortes

#### Scenario: Casillas accesibles

- **GIVEN** que se muestra una partida en curso
- **WHEN** se recorre el tablero con TalkBack
- **THEN** cada casilla expone su ubicación, su contenido y si está disponible, y mide al menos 48 dp

#### Scenario: Estado del resultado expuesto

- **GIVEN** que la partida terminó
- **WHEN** se recorre la pantalla con una tecnología de asistencia
- **THEN** el texto del resultado y la acción «Volver al lobby» son alcanzables y las casillas se exponen como deshabilitadas
