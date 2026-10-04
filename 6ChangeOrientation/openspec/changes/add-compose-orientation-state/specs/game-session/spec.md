# Spec Delta

## MODIFIED Requirements

### Requirement: Inicio de sesión y alternancia del iniciador
Una sesión nueva MUST cargar scores persistentes mediante SharedPreferences, independientemente de la partida temporal, normalizando cada contador inválido individualmente a cero; si no existen valores válidos, el valor será cero. Su primera partida MUST iniciar con X. Toda nueva partida MUST alternar iniciador, incluso si la anterior fue abandonada.

#### Scenario: Primera partida de una sesión nueva
- **GIVEN** que no existe sesión lógica viva y las preferencias tienen contadores válidos
- **WHEN** se muestra la primera partida
- **THEN** X inicia y se muestran los scores persistidos

#### Scenario: Contadores ausentes o inválidos individualmente
- **GIVEN** que no hay snapshot temporal válido y faltan valores o algunos scores no son enteros no negativos
- **WHEN** se carga el marcador de SharedPreferences
- **THEN** cada contador inválido se normaliza a cero y los demás valores válidos se conservan

#### Scenario: Alternancia después de una partida terminada
- **GIVEN** que la partida anterior inició con X
- **WHEN** la persona solicita explícitamente nueva partida
- **THEN** la nueva partida inicia con O

#### Scenario: Alternancia después de abandonar una partida
- **GIVEN** que la partida actual está incompleta y había iniciado con O
- **WHEN** la persona solicita explícitamente nueva partida
- **THEN** la nueva partida inicia con X y la abandonada no suma puntos

### Requirement: Nueva partida y apertura de O
Nueva partida MUST limpiar tablero y resultado y cancelar espera anterior, conservar marcador persistente y dificultad, y alternar iniciador. Si O inicia, MUST esperar 1000 ms acumulados en primer plano antes de una sola jugada y luego pasar el turno a X.

#### Scenario: Nueva partida conserva el marcador
- **GIVEN** que existe marcador de sesión y partida activa o terminada
- **WHEN** se solicita nueva partida
- **THEN** se limpian marcas y resultado, se conserva marcador, y el tablero queda vacío si inicia X o vacío con turno O pendiente si inicia O

#### Scenario: O inicia la nueva partida
- **GIVEN** que la alternancia determina que O inicia
- **WHEN** comienza la nueva partida
- **THEN** el tablero está vacío, el turno corresponde a O y no hay jugada antes de 1000 ms acumulados de primer plano

#### Scenario: Apertura O tras 1000 ms
- **GIVEN** que O inicia y la apertura sigue pendiente
- **WHEN** se completan 1000 ms acumulados de primer plano y el juego puede procesar
- **THEN** O ocupa exactamente una casilla y el turno pasa a X

#### Scenario: O no mueve a los 999 ms
- **GIVEN** que O inicia
- **WHEN** solo han transcurrido 999 ms acumulados en primer plano
- **THEN** el tablero permanece vacío y el turno sigue siendo O

#### Scenario: Tiempo restante tras pausa
- **GIVEN** que O espera y ya consumió 400 ms en primer plano
- **WHEN** pasa a segundo plano y luego vuelve
- **THEN** no juega durante la pausa y juega una vez tras los 600 ms restantes en primer plano

#### Scenario: X inicia sin movimiento automático
- **GIVEN** que la alternancia determina que X inicia
- **WHEN** comienza nueva partida
- **THEN** el tablero permanece vacío y el turno corresponde a X

#### Scenario: Nueva partida invalida cualquier turno pendiente anterior
- **GIVEN** que está pendiente una respuesta o apertura de O
- **WHEN** se solicita nueva partida
- **THEN** la espera anterior se cancela y no modifica tablero nuevo; solo cabe apertura de la nueva partida si inicia O

#### Scenario: X termina en victoria o empate
- **GIVEN** que una jugada de X termina la partida
- **WHEN** se procesa el desenlace
- **THEN** no queda pendiente ni se realiza respuesta de O

### Requirement: Puntuación única de partidas terminadas
La sesión MUST registrar una vez cada desenlace e incrementar y persistir únicamente el contador correspondiente en SharedPreferences. Una partida incompleta MUST NOT modificar contadores.

#### Scenario: Victoria humana puntuada
- **GIVEN** que una partida termina con victoria X
- **WHEN** se registra el desenlace
- **THEN** aumenta y persiste solo el contador X

#### Scenario: Victoria de computadora puntuada
- **GIVEN** que una partida termina con victoria O
- **WHEN** se registra el desenlace
- **THEN** aumenta y persiste solo el contador O

#### Scenario: Empate puntuado
- **GIVEN** que una partida termina en empate
- **WHEN** se registra el desenlace
- **THEN** aumenta y persiste solo el contador de empates

#### Scenario: Resultado no duplicado
- **GIVEN** que el resultado ya fue registrado
- **WHEN** se repiten taps o se restaura la pantalla
- **THEN** no vuelve a incrementarse ningún contador

### Requirement: Conservación del estado durante la sesión viva
A efectos de esta capacidad, una sesión viva es el estado en memoria de la misma instancia lógica; el estado MUST conservar tablero, turno, resultado, iniciador siguiente, estado de sonido y tiempo restante de O durante recreación de configuración o restauración temporal soportada. La vuelta a primer plano MUST NOT iniciar partida o repetir acciones; sí podrá completar una única jugada O que ya estaba pendiente al vencer su tiempo restante.

#### Scenario: Rotación durante una partida activa
- **GIVEN** que existe partida activa con tablero, turno e iniciador
- **WHEN** la pantalla se recrea por rotación
- **THEN** se restauran tablero y turno sin reaplicar movimiento, puntuación ni sonido

#### Scenario: Rotación después de un resultado
- **GIVEN** que terminó una partida y su contador ya fue actualizado
- **WHEN** la pantalla se recrea
- **THEN** resultado y marcador se conservan sin nuevo punto ni sonido

#### Scenario: Regreso desde segundo plano
- **GIVEN** que el juego pasa a segundo plano sin finalizar sesión
- **WHEN** vuelve a primer plano
- **THEN** conserva el estado y solo completa O si había turno pendiente y venció su tiempo restante; esa nueva jugada tiene efectos normales una sola vez

#### Scenario: Pausa y continuación del tiempo restante
- **GIVEN** que O espera y parte de 1000 ms ya transcurrió
- **WHEN** pierde primer plano y luego vuelve
- **THEN** no juega ni reproduce audio en background y reanuda desde el tiempo restante sin reiniciar cuenta

#### Scenario: Rotación conserva espera pendiente
- **GIVEN** que O tiene jugada pendiente
- **WHEN** la pantalla rota
- **THEN** conserva tiempo restante y realiza una única jugada al completar 1000 ms acumulados en primer plano

#### Scenario: Tiempo restante tras pausa
- **GIVEN** que O consumió 400 ms en primer plano
- **WHEN** permanece en segundo plano
- **THEN** restan 600 ms y no se juega durante la pausa

#### Scenario: Tiempo restante tras rotación
- **GIVEN** que O consumió 400 ms antes de rotación
- **WHEN** termina la recreación
- **THEN** restan 600 ms y O no juega hasta consumirlos en primer plano

#### Scenario: Cancelación de espera al cerrar sesión
- **GIVEN** que O tiene jugada pendiente
- **WHEN** la sesión se cierra deliberadamente o finaliza
- **THEN** se cancela y no se restaura al comenzar otra sesión

#### Scenario: Restauración soportada de O
- **GIVEN** que el snapshot temporal soportado contiene espera O y su tiempo restante
- **WHEN** el host restaurado vuelve a RESUMED
- **THEN** O espera lo restante; la restauración por sí sola no mueve, puntúa ni reproduce sonido, pero la jugada legítima posterior sí sigue reglas normales una sola vez

### Requirement: Límites de la sesión en memoria
Un snapshot temporal de SavedStateHandle MUST restaurar únicamente recreación soportada y nunca ser almacén permanente. Sin snapshot válido, se inicia tablero vacío y X; SharedPreferences es la única autoridad de scores y dificultad, incluso tras muerte de proceso soportada. SoundEnabled se conserva solo en snapshot temporal y vuelve a true en sesión nueva. No se restaura una partida tras cierre deliberado, force-stop o nuevo lanzamiento sin snapshot; background no equivale a cierre. Código de dificultad estable: 0 Easy, 1 Hard, 2 Expert, 3 Impossible; códigos ausentes o inválidos usan Expert. Back aplica navegación normal y no garantiza finalizar tarea.

#### Scenario: Rotación con estado guardado
- **GIVEN** que el host se recrea por configuración con estado temporal válido
- **WHEN** se recrea la pantalla
- **THEN** se recuperan tablero, turno, resultado, iniciadores, sonido y restante O sin efectos duplicados

#### Scenario: Restauración soportada por muerte de proceso
- **GIVEN** que Android recrea el host con snapshot temporal válido
- **WHEN** se restaura
- **THEN** se restaura estado de partida y sonido de sesión, mientras scores/dificultad provienen de SharedPreferences

#### Scenario: Sin snapshot temporal
- **GIVEN** que se abre sin snapshot válido
- **WHEN** comienza el juego
- **THEN** tablero vacío, X, sonido true y scores/dificultad cargados de SharedPreferences o sus defaults

#### Scenario: Snapshot corrupto
- **GIVEN** que el snapshot tiene campos ausentes, inválidos o incompatibles
- **WHEN** se intenta restaurar
- **THEN** no hay crash ni aplicación parcial; se inicia tablero vacío con X y prefs válidas

#### Scenario: Nueva sesión tras finalizar la tarea
- **GIVEN** que finalizó la tarea y no hay snapshot válido
- **WHEN** se vuelve a abrir
- **THEN** no se restaura la partida y scores/dificultad persistidos válidos permanecen

#### Scenario: Sin persistencia permanente
- **GIVEN** que el usuario finaliza deliberadamente o fuerza detención
- **WHEN** vuelve a abrir sin snapshot temporal
- **THEN** no se restaura tablero ni turno; scores/dificultad persistidos sí

#### Scenario: Nueva sesión tras perder el proceso
- **GIVEN** que el proceso anterior se perdió y no hay snapshot temporal válido
- **WHEN** comienza una nueva sesión
- **THEN** el tablero inicia vacío con X y scores/dificultad proceden de preferencias válidas o defaults

#### Scenario: Background y Back
- **GIVEN** que se envía a background o se pulsa Back
- **WHEN** Android conserva la tarea
- **THEN** no se fuerza terminación ni se reinicia estado por el mero background

#### Scenario: Códigos de dificultad
- **GIVEN** que se carga un código entero persistido
- **WHEN** vale 0, 1, 2 o 3
- **THEN** se asigna respectivamente Easy, Hard, Expert o Impossible

#### Scenario: Código de dificultad desconocido
- **GIVEN** que el código falta o no vale 0, 1, 2 o 3
- **WHEN** se carga
- **THEN** se usa Expert sin crash

## ADDED Requirements

### Requirement: Restablecimiento persistente del marcador
El sistema SHALL restablecer inmediatamente y persistir a cero los tres contadores sin modificar ninguna otra parte de la partida o sesión.

#### Scenario: Restablecer durante partida
- **GIVEN** una partida activa con marcador
- **WHEN** se elige Reiniciar puntajes
- **THEN** scores visibles y persistidos pasan a cero sin cambiar tablero, turno, resultado, iniciadores, dificultad, espera ni sonido

#### Scenario: Restablecer terminal
- **GIVEN** que una partida terminal ya fue puntuada
- **WHEN** se restablece marcador
- **THEN** resultado permanece y no vuelve a contabilizarse
