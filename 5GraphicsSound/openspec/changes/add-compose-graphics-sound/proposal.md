# Proposal

## Why

El reto de gráficos y sonido pide reemplazar el tablero de botones por una representación gráfica propia y dar respuesta sonora a las jugadas, pero su ejemplo está basado en Java, `View`/XML y `MediaPlayer`, ajenos al stack Compose actual. Esta ampliación traslada la intención a una cuadrícula Canvas accesible y a efectos breves, sin alterar las reglas ni la experiencia existente fuera de lo solicitado.

## What Changes

- Incorporar un tablero 3×3 dibujado con Canvas de Compose, con marcas vectoriales X verde y O roja, manteniendo las nueve zonas accionables y accesibles.
- Añadir dos efectos cortos distintos para jugadas aceptadas, con una opción activable en el menú existente; el sonido inicia habilitado y su preferencia vive solo en la sesión.
- Diferir cada turno de O por 1000 ms de tiempo en primer plano, tanto al responder como al abrir una partida. Pausar la cuenta al perder primer plano, reanudar el tiempo restante al volver sin duplicar el turno y cancelar cualquier turno pendiente al iniciar otra partida.
- Mantener la selección de dificultad habilitada bajo la regla actual (partida terminal o tablero vacío), incluida la espera de la apertura de O; revalidar toda selección al recibirla y usar el nivel vigente cuando se ejecute el movimiento.
- Actualizar los requisitos que hoy exigen respuesta inmediata de O y menú de exactamente cuatro opciones. La secuencia de respuesta existente describe una jugada única, pero no especifica su inmediatez; la implementación observada actualmente sí es síncrona. Crear ahora el delta `game-options` tomando como base la especificación completa de `add-compose-menus-dialogs`; integrar/sincronizar/archivar ese cambio previo será precondición para integrar los deltas a main, no para redactar esta planificación.
- Mantener políticas de IA, score, alternancia, el resto de reglas y límites de sesión y diseño del resto de la interfaz sin cambios. No adoptar APIs Java/View/XML heredadas del PDF ni actualizar el stack Android/Compose.

## Capabilities

### New Capabilities
- `custom-game-board`: representación dibujada en Compose de la cuadrícula y sus marcas, con interacción y accesibilidad preservadas.
- `game-sound-effects`: efectos diferenciados de jugadas aceptadas y preferencia de sonido solo de sesión.

### Modified Capabilities
- `tic-tac-toe-game`: presentación del tablero y temporización/revalidación de turnos de O sin cambiar las reglas de juego ni la IA.
- `game-session`: conservación y pausa/reanudación del turno pendiente dentro de la sesión viva; sin movimiento causado únicamente por recreación o retorno de segundo plano.
- `game-options` (capacidad introducida por `add-compose-menus-dialogs`): ampliar de cuatro a cinco entradas y exponer la activación del sonido. Este delta se redactará ahora sobre la base de la especificación completa del cambio previo; sincronizar e integrar los deltas a main requiere primero integrar/sincronizar/archivar aquel cambio.

## Impact

Impacta `TicTacToeScreen`/`GameViewModel`, el estado de sesión y el menú Compose; requerirá pruebas de dibujo/semántica, temporización, ciclo de vida, cancelación y sonido. Se prevé Canvas Compose y un mecanismo de audio de baja latencia adecuado a efectos cortos (SoundPool), evitando reproducción con `MediaPlayer`. La preferencia de sonido se conserva durante partida nueva, rotación y segundo plano, pero no entre instancias nuevas del ViewModel; en cada sesión nueva vuelve habilitada. La espera de O contabiliza solo tiempo en primer plano y no altera el estado por abrir o recrear pantalla.

**Fuera de alcance:** cambios a la IA, score, orden de turnos o al resto de reglas y límites de sesión; rediseño del resto de la UI; persistencia de la opción de sonido; cambiar el stack o implementar código antes de completar la planificación.
