# game-session Specification

## Purpose

Esta capacidad define cómo se agrupan las partidas en una sesión temporal, cómo se alterna el iniciador y cómo se conserva y actualiza el marcador sin almacenamiento permanente entre sesiones.

## Requirements

### Requirement: Inicio de sesión y alternancia del iniciador

Una sesión nueva MUST comenzar con los contadores de X, O y empates en cero, y su primera partida MUST iniciar con el humano X. Cada solicitud explícita de nueva partida MUST alternar el iniciador respecto de la partida anterior, incluso si la partida anterior fue abandonada sin terminar.

#### Scenario: Primera partida de una sesión nueva

- **GIVEN** que no existe una sesión viva al abrir el juego
- **WHEN** se muestra la primera partida
- **THEN** X inicia y los contadores de victorias de X, victorias de O y empates muestran cero

#### Scenario: Alternancia después de una partida terminada

- **GIVEN** que la partida anterior inició con X
- **WHEN** la persona solicita explícitamente una nueva partida
- **THEN** la nueva partida inicia con O

#### Scenario: Alternancia después de abandonar una partida

- **GIVEN** que la partida actual está incompleta y había iniciado con O
- **WHEN** la persona solicita explícitamente una nueva partida
- **THEN** la nueva partida inicia con X y la partida abandonada no suma a ningún contador

### Requirement: Nueva partida y apertura de O
La acción de nueva partida MUST limpiar todas las marcas y el resultado de la partida anterior, y MUST conservar los contadores acumulados de la sesión. Cuando el iniciador sea O, la computadora MUST realizar exactamente una jugada inicial después de completar 1000 ms de primer plano desde el inicio de esa partida y después MUST quedar el turno humano. La espera no congela la selección de dificultad permitida por sus reglas vigentes; O usa el nivel vigente al ejecutar la jugada.

#### Scenario: Nueva partida conserva el marcador
- **GIVEN** que existe un marcador de sesión y una partida activa o terminada
- **WHEN** la persona solicita una nueva partida
- **THEN** se eliminan las marcas y el resultado de la partida anterior, los contadores permanecen sin cambios y el estado muestra nueve casillas vacías si inicia X o una O con turno humano tras su apertura si inicia O

#### Scenario: O inicia la nueva partida
- **GIVEN** que la alternancia determina que O inicia
- **WHEN** se completan 1000 ms de primer plano desde el inicio de la nueva partida y el juego puede procesar la acción
- **THEN** O ocupa exactamente una casilla libre, no antes del umbral, y el turno pasa a X

#### Scenario: X inicia sin movimiento automático
- **GIVEN** que la alternancia determina que X inicia
- **WHEN** comienza la nueva partida
- **THEN** el tablero permanece vacío y el turno corresponde a X

#### Scenario: Nueva partida invalida cualquier turno pendiente anterior
- **GIVEN** que está pendiente una respuesta de O tras X o una apertura de O
- **WHEN** se solicita otra nueva partida, incluso rápidamente tras la anterior
- **THEN** todo turno pendiente anterior queda cancelado y nunca coloca una marca ni registra un punto en el tablero nuevo; solo puede ocurrir una apertura de O propia de la nueva partida si inicia O

#### Scenario: X termina en victoria o empate
- **GIVEN** que la jugada humana termina la partida por victoria o empate
- **WHEN** se procesa el desenlace
- **THEN** no queda pendiente ni se realiza respuesta alguna de O

### Requirement: Puntuación única de partidas terminadas

La sesión MUST registrar exactamente una vez cada partida que termine, incrementando únicamente el contador correspondiente a la victoria de X, la victoria de O o el empate. Una partida incompleta MUST NOT modificar ningún contador.

#### Scenario: Victoria humana puntuada

- **GIVEN** que una partida termina con victoria de X
- **WHEN** se registra el desenlace
- **THEN** el contador de X aumenta en uno y los contadores de O y empates no cambian

#### Scenario: Victoria de computadora puntuada

- **GIVEN** que una partida termina con victoria de O
- **WHEN** se registra el desenlace
- **THEN** el contador de O aumenta en uno y los contadores de X y empates no cambian

#### Scenario: Empate puntuado

- **GIVEN** que una partida termina en empate
- **WHEN** se registra el desenlace
- **THEN** el contador de empates aumenta en uno y los contadores de X y O no cambian

#### Scenario: Resultado no duplicado

- **GIVEN** que una partida ya terminó y su resultado fue registrado
- **WHEN** se repiten taps sobre el tablero o se recrea la pantalla
- **THEN** el resultado y sus contadores no vuelven a incrementarse

### Requirement: Conservación del estado durante la sesión viva
A efectos de esta capacidad, una sesión viva es el estado en memoria asociado a la misma instancia lógica del juego; incluye las recreaciones por cambios de configuración y deja de existir al finalizar la tarea o perder el proceso. Mientras exista, el sistema MUST conservar el tablero, el turno, el resultado, el iniciador siguiente y los contadores, así como el tiempo restante de cualquier jugada de O pendiente. Volver desde segundo plano dentro de esa sesión MUST NOT iniciar una partida, mover una marca ni registrar un punto adicional por el mero retorno. Una espera pendiente MUST acumular únicamente tiempo en primer plano, suspenderse al perderlo y continuar desde el tiempo restante al volver, sin duplicar la jugada.

#### Scenario: Rotación durante una partida activa
- **GIVEN** que existe una partida activa con tablero, turno e iniciador definidos
- **WHEN** la pantalla se recrea por rotación
- **THEN** se restauran el tablero y el turno sin movimiento automático ni cambio de iniciador

#### Scenario: Rotación después de un resultado
- **GIVEN** que una partida terminó y su contador ya fue actualizado
- **WHEN** la pantalla se recrea por rotación
- **THEN** se conserva el resultado y el marcador sin registrar otro punto

#### Scenario: Regreso desde segundo plano
- **GIVEN** que el juego pasa a segundo plano sin finalizar la sesión
- **WHEN** la persona vuelve al juego
- **THEN** se conserva el estado previo y no se dispara una nueva partida ni una jugada adicional salvo completar el tiempo restante de una jugada O que ya estaba pendiente

#### Scenario: Pausa y continuación del tiempo restante
- **GIVEN** que O espera una jugada y parte de su umbral de 1000 ms ya transcurrió en primer plano
- **WHEN** el juego pierde primer plano y luego vuelve
- **THEN** no se realizan jugadas ni se reproduce audio en segundo plano, y O actúa solo después de acumular el tiempo restante, sin reiniciar la cuenta ni duplicar la jugada

#### Scenario: Rotación conserva espera pendiente
- **GIVEN** que O tiene una jugada pendiente durante una sesión viva
- **WHEN** la pantalla rota
- **THEN** se conserva el tiempo restante y se realiza una única jugada al completar el umbral acumulado en primer plano

#### Scenario: Tiempo restante tras pausa
- **GIVEN** que O espera y ya consumió 400 ms del umbral en primer plano
- **WHEN** el juego permanece en segundo plano
- **THEN** quedan 600 ms pendientes y no se realiza ninguna jugada durante la pausa

#### Scenario: Tiempo restante tras rotación
- **GIVEN** que O espera y ya consumió 400 ms del umbral en primer plano
- **WHEN** la pantalla rota
- **THEN** se conservan 600 ms pendientes y O no juega hasta que se completen en primer plano

#### Scenario: Cancelación de espera al cerrar sesión
- **GIVEN** que O tiene una jugada pendiente
- **WHEN** se cierra explícitamente el juego o finaliza la sesión
- **THEN** se cancela la jugada pendiente y no se restaura al iniciar otra sesión

### Requirement: Límites de la sesión en memoria

Al abrir el juego sin una sesión viva, el sistema MUST iniciar una sesión nueva con marcador cero y X como iniciador de la primera partida. SHALL NOT restaurar el tablero ni el marcador de una sesión anterior después de finalizar la tarea, perder el proceso o cerrar explícitamente el juego, y SHALL NOT reiniciar la sesión solo por enviar la aplicación a segundo plano.

#### Scenario: Nueva sesión tras perder el proceso

- **GIVEN** que el proceso anterior se perdió y se abre el juego sin una sesión viva
- **WHEN** comienza la nueva sesión
- **THEN** los contadores vuelven a cero y la primera partida inicia con X

#### Scenario: Nueva sesión tras finalizar la tarea

- **GIVEN** que la tarea de la aplicación fue finalizada y no queda una sesión viva
- **WHEN** se abre nuevamente el juego
- **THEN** se inicia una sesión nueva con marcador cero y X como iniciador

#### Scenario: Sin persistencia permanente

- **GIVEN** que la aplicación se abre en una sesión distinta de la anterior
- **WHEN** se muestra el marcador
- **THEN** no se recuperan los contadores ni el estado de la sesión anterior desde almacenamiento permanente
