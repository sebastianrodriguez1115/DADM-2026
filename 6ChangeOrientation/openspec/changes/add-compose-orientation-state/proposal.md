# Proposal

## Why

El juego debe seguir siendo funcional en orientación vertical y horizontal y no perder el progreso ante rotación o recreación por muerte de proceso soportada. También debe conservar marcador y dificultad entre aperturas, sin restaurar una partida cerrada deliberadamente.

## What Changes

- Adaptar el diseño Compose a ambas orientaciones: en horizontal, tablero cuadrado a la izquierda y estado, marcador y controles a la derecha; en vertical se conserva una presentación funcional. Eliminar la barra superior y su título en ambas orientaciones, manteniendo un menú compacto y accesible junto a los controles. El tamaño de referencia de 270 dp se adapta al espacio disponible y a la accesibilidad.
- Conservar tablero, turno, resultado, iniciador actual, próximo iniciador y cualquier turno de O pendiente al recrear la pantalla por configuración y en la restauración soportada tras muerte de proceso, mediante `SavedStateHandle` en `GameViewModel`. No guardar el estado de partida en preferencias ni reanudarlo en un nuevo lanzamiento deliberado.
- Persistir los contadores de X, O y empates entre aperturas mediante `SharedPreferences`. Añadir una acción accesible para ponerlos inmediatamente a cero y persistir el reset sin reiniciar la partida. Retirar Salir y la confirmación de salida obsoleta; conservar Nueva partida, Dificultad, Sonido y Acerca de.
- Persistir la dificultad mediante un entero estable y validado en `SharedPreferences`, restaurándola tanto en sesión nueva como tras rotación/restauración. Resolver la recreación durante la espera de O sin crash, turno perdido ni movimiento duplicado.
- Preservar los contratos actuales de alternancia, puntuación única, límites de sesión y espera de O: la recreación no debe duplicar movimiento, score ni sonido. Scores y dificultad son persistentes; la partida no lo es al iniciar una sesión deliberadamente nueva. El ajuste de Sonido conserva el comportamiento actual de sesión.
- No fijar orientación ni suprimir la recreación nativa mediante `configChanges`; implementar sobre Kotlin, Compose y Material 3, sin migrar a Java/XML.

## Capabilities

### New Capabilities
- Ninguna. Las conductas se incorporan a capacidades existentes o a `game-options`, ya introducida por cambios OpenSpec en curso.

### Modified Capabilities
- `game-session`: definir restauración del estado de partida tras recreaciones soportadas, persistencia independiente de marcador y dificultad, y límites entre restauración temporal y nuevo lanzamiento; retirar la prohibición general de persistencia y precisar sus fronteras.
- `tic-tac-toe-game`: definir presentación adaptativa portrait/landscape, accesibilidad de controles y reset inmediato/persistente del marcador sin afectar la partida.
- `game-options` (capacidad introducida por `add-compose-menus-dialogs`): retirar Salir y su diálogo, conservar las demás opciones e incorporar reset de marcador y persistencia de dificultad.

## Impact

Afecta `GameViewModel`/`GameUiState`, `TicTacToeScreen`/`GameScreen`, `GameMenu`, el host de actividad, preferencias y sus pruebas de ciclo de vida, restauración y UI. Se integra con requisitos existentes en `openspec/specs/game-session` y `openspec/specs/tic-tac-toe-game`, y con `game-options` definido en `add-compose-menus-dialogs`; la coordinación con cambios OpenSpec previos en curso es necesaria antes de sincronizar a las especificaciones principales. No se pretende modificar ni archivar esos cambios previos.
