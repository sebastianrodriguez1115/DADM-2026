# Spec Delta

> Precondición de integración: `game-options` se define en `add-compose-menus-dialogs` y se amplía en `add-compose-graphics-sound`. Integrar ambas bases antes de sincronizar; no se modifican aquí.

## MODIFIED Requirements

### Requirement: Menú principal de opciones
La interfaz SHALL ofrecer menú Compose visible y accesible con exactamente cinco entradas: Nueva partida, Dificultad, Sonido, Reiniciar puntajes y Acerca de. Cada entrada MUST tener etiqueta de recurso e icono asociado. Nueva partida MUST equivaler a la acción existente y el botón existente permanece accesible. Al elegir una opción habilitada, el menú se cierra antes de acción/diálogo; Dificultad deshabilitada no actúa. No habrá Salir ni confirmación de salida. Back SHALL descartar menú/diálogo ordinario si corresponde y, si no hay overlay, seguir navegación normal del sistema.

#### Scenario: Menú con cinco entradas
- **GIVEN** que se muestra partida activa o terminada
- **WHEN** se abre menú
- **THEN** aparecen exactamente Nueva partida, Dificultad, Sonido, Reiniciar puntajes y Acerca de, con label e icono

#### Scenario: Nueva partida desde menú
- **GIVEN** que menú está abierto
- **WHEN** se elige Nueva partida
- **THEN** se ejecuta acción equivalente al botón: tablero limpio, marcador conservado e iniciador alternado

#### Scenario: Atajo existente
- **GIVEN** que se muestra juego
- **WHEN** se busca iniciar partida
- **THEN** el botón visible y accesible ejecuta la misma acción que menú

#### Scenario: Menú cierra antes de la acción
- **GIVEN** que se elige opción habilitada
- **WHEN** se realiza acción o se abre diálogo
- **THEN** menú ya está cerrado, sin ventanas apiladas

#### Scenario: Dificultad deshabilitada
- **GIVEN** partida activa con marca en tablero
- **WHEN** se intenta elegir Dificultad
- **THEN** no se abre diálogo y sesión no cambia

#### Scenario: Menú no afecta espera O
- **GIVEN** que O tiene turno pendiente
- **WHEN** se abre/cierra menú o se elige Sonido o dificultad permitida
- **THEN** no se reinicia ni adelanta el reloj; O puede completar su turno normal al vencer el tiempo de primer plano

#### Scenario: Sin acción Salir
- **GIVEN** que se abre menú
- **WHEN** se recorren sus entradas
- **THEN** no existe Salir ni diálogo de confirmación

#### Scenario: Back con overlay
- **GIVEN** que un menú o diálogo ordinario está abierto
- **WHEN** se pulsa Back
- **THEN** se descarta según navegación normal sin cambiar partida

#### Scenario: Back sin overlay
- **GIVEN** que no hay menú ni diálogo
- **WHEN** se pulsa Back
- **THEN** se aplica navegación normal del sistema sin interceptación global ni garantía de finalizar tarea

### Requirement: Selección de dificultad
Al abrir Dificultad SHALL presentar Easy, Hard, Expert, Impossible como Fácil, Difícil, Experto, Imposible, en ese orden, con radio vigente seleccionado. Será editable con tablero vacío antes de primera jugada (también apertura O pendiente) o tras resultado terminal; no durante juego activo con marcas, incluida apertura O ya efectuada. Una selección aceptada persiste el código estable 0/1/2/3 en SharedPreferences, cierra y emite exactamente un Toast visible; elegir nivel actual también notifica una vez. Cancelar no cambia nada. Selección no altera partida ni timer pendiente; O usa nivel vigente al jugar. Solicitudes obsoletas se rechazan.

#### Scenario: Apertura muestra nivel vigente
- **GIVEN** que la dificultad es editable y vigente es Y
- **WHEN** se abre diálogo
- **THEN** radio de Y aparece seleccionado

#### Scenario: Selector ofrece cuatro niveles
- **GIVEN** que se abre diálogo editable
- **WHEN** se muestra
- **THEN** aparecen exactamente Fácil, Difícil, Experto, Imposible en ese orden y un radio seleccionado

#### Scenario: Reapertura tras Impossible
- **GIVEN** que vigente es Impossible y editable
- **WHEN** se reabre diálogo
- **THEN** Imposible está seleccionado

#### Scenario: Cambiar nivel notifica una vez
- **GIVEN** que Y está seleccionado y se elige Z
- **WHEN** la selección se acepta
- **THEN** vigente y SharedPreferences pasan a Z, diálogo cierra y hay exactamente un Toast con etiqueta Z

#### Scenario: Elegir nivel ya seleccionado
- **GIVEN** que Y ya está seleccionado
- **WHEN** se elige Y
- **THEN** se cierra diálogo y emite un Toast de Y, sin duplicado por recomposición/recreación

#### Scenario: Cancelar selección
- **GIVEN** que diálogo muestra Y
- **WHEN** se descarta por Back o toque fuera cuando proceda
- **THEN** estado y preferencia siguen Y y no hay Toast de selección

#### Scenario: Editable antes de primera jugada X
- **GIVEN** partida X con tablero vacío
- **WHEN** se elige Dificultad
- **THEN** diálogo permite elegir nivel sin colocar marca

#### Scenario: Editable durante apertura O pendiente
- **GIVEN** partida iniciada por O, tablero aún vacío y temporizador de apertura pendiente
- **WHEN** se abre selector y se elige Z
- **THEN** entrada permite el cambio y persiste Z sin colocar O, anticipar/reiniciar el reloj ni alterar turno/iniciadores; O utilizará Z al vencer el tiempo original

#### Scenario: Editable después de resultado
- **GIVEN** que la partida terminó
- **WHEN** se elige Dificultad
- **THEN** diálogo abre con nivel vigente y el cambio persistido regirá próxima partida incluida apertura O

#### Scenario: Rechazo durante partida con marcas
- **GIVEN** partida activa con al menos una marca y dificultad Y
- **WHEN** usuario o solicitud directa/obsoleta intenta Z
- **THEN** se rechaza sin cambiar dificultad, preferencia, estado, timer, movimiento o Toast

#### Scenario: Apertura O cierra edición
- **GIVEN** tablero vacío con apertura O pendiente
- **WHEN** transcurren 1000 ms acumulados de primer plano y O juega
- **THEN** se usa nivel vigente y Dificultad queda deshabilitada mientras haya marcas y juego activo

#### Scenario: Notificación no se repite
- **GIVEN** que se notificó una elección
- **WHEN** hay recomposición o recreación
- **THEN** no se reemite Toast

#### Scenario: Ventana de edición no mueve O
- **GIVEN** que O espera con tablero vacío y resta tiempo
- **WHEN** se abre o cierra selector o cambia dificultad permitida
- **THEN** O no juega por esas acciones y conserva reloj restante

### Requirement: Conservación de la dificultad en la sesión
SharedPreferences SHALL ser la autoridad única y permanente de dificultad, incluso ante recreación/restauración temporal; el código entero estable 0 Easy, 1 Hard, 2 Expert, 3 Impossible se carga tanto al iniciar como al restaurar. Valor ausente, corrupto o desconocido usa Expert. Solo selección aceptada se persiste. El snapshot temporal MUST NOT contener segunda copia de dificultad. La dificultad SHALL conservarse en nuevas partidas y el diálogo no necesita quedar abierto al recrear.

#### Scenario: Se retiene al pedir nueva partida
- **GIVEN** que dificultad Y está persistida
- **WHEN** se solicita nueva partida
- **THEN** Y sigue vigente

#### Scenario: Se carga al recrear o volver
- **GIVEN** que SharedPreferences contiene Y
- **WHEN** hay rotación, restauración soportada o regreso a primer plano
- **THEN** dificultad vigente es Y sin duplicar Toast

#### Scenario: Imposible persiste
- **GIVEN** que se aceptó Impossible
- **WHEN** se abre nueva partida o se recrea host
- **THEN** se carga Impossible

#### Scenario: Códigos estables
- **GIVEN** una preferencia entera 0, 1, 2 o 3
- **WHEN** se carga dificultad
- **THEN** se interpreta respectivamente Easy, Hard, Expert o Impossible

#### Scenario: Ausente o corrupta usa Expert
- **GIVEN** que falta preferencia o no contiene código conocido
- **WHEN** se inicia o restaura
- **THEN** se usa Expert sin crash

#### Scenario: Cambio rechazado no persiste
- **GIVEN** que estado no permite cambiar dificultad
- **WHEN** se recibe solicitud
- **THEN** ni valor vigente ni SharedPreferences cambian

#### Scenario: No se requiere restaurar diálogo abierto
- **GIVEN** que diálogo de Y estaba abierto
- **WHEN** host se recrea
- **THEN** SharedPreferences determina Y y no se requiere mantener diálogo abierto

### Requirement: Accesibilidad de opciones y diálogos
Las cinco entradas y controles SHALL ser visibles/alcanzables y exponer etiquetas textuales de recursos para tecnologías de asistencia; las opciones deshabilitadas exponen su estado y no una acción.

#### Scenario: Entradas etiquetadas
- **GIVEN** que menú está abierto
- **WHEN** se recorre con tecnología asistiva
- **THEN** cada una de cinco opciones expone etiqueta y estado y acción solo si habilitada

#### Scenario: Controles de diálogo alcanzables
- **GIVEN** que un diálogo está abierto
- **WHEN** se navega con tecnología asistiva
- **THEN** sus opciones y botones son alcanzables y etiquetados

#### Scenario: Dificultad deshabilitada accesible
- **GIVEN** que Dificultad está deshabilitada por partida activa con marcas
- **WHEN** se recorre menú
- **THEN** expone etiqueta/estado deshabilitado sin acción ejecutable

## REMOVED Requirements

### Requirement: Confirmación de salida
**Reason**: Se elimina la opción Salir y su confirmación obsoleta; no debe ofrecerse salida explícita desde el menú.
**Migration**: Abandonar la pantalla mediante Back/navegación normal del sistema; no se intercepta Back ni se garantiza que finalice tarea.
