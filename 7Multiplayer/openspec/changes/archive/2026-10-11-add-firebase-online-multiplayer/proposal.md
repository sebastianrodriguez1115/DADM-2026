# Proposal

## Why

El reto 7 pide que dos jugadores jueguen al triqui a través de un servicio en la nube: cualquiera crea un juego, se muestra una lista de juegos disponibles, otra persona se une y solo entonces se puede jugar. Hoy la aplicación solo permite jugar una persona contra la computadora en un dispositivo y sin red.

## What Changes

- Añadir una pantalla inicial con dos entradas, «Jugar contra la computadora» y «Jugar en línea». Los dos modos conviven; el modo local conserva sus reglas, su marcador y su sesión.
- Añadir el modo en línea con dos pantallas nuevas: el lobby (apodo, lista de juegos disponibles en tiempo real, crear juego y espera de rival) y la partida en línea (dos jugadores, tablero compartido, turnos, resultado, abandono y salida).
- Usar Firebase Realtime Database como servicio en la nube que media la interacción, con identidad anónima de Firebase: no hay cuentas de usuario.
- Pedir un apodo una sola vez al entrar a «Jugar en línea» y recordarlo entre aperturas de la aplicación. La lista muestra cada juego como «Juego de <apodo>».
- Quien crea el juego lleva X y empieza; quien se une lleva O. Solo se puede marcar después de que un rival se una. Una jugada se muestra solo cuando el servicio la confirma.
- Añadir una acción visible «Volver al inicio» en el menú de opciones de la partida local y en el lobby, con el mismo efecto que Back (ver D17 en `design.md`).
- Sin revancha: al terminar la partida se ve el resultado y cada jugador vuelve al lobby por su cuenta.
- Si el rival se va (sale o se desconecta), la partida queda abandonada, sin ganador; quien permanece ve un aviso y vuelve al lobby. Quien pierde la conexión durante la partida ve «Se perdió la conexión. La partida terminó.» y no puede reanudarla; la aplicación mantiene la pantalla encendida mientras espera rival y durante la partida en curso (ver D16 en `design.md`).
- Lo jugado en línea no modifica el marcador local ni se muestra en la pantalla en línea.
- Las partidas en línea reproducen los mismos efectos de sonido de X y O del modo local, una vez por jugada aceptada (propia o del rival), nunca al resincronizar, gobernadas por la opción «Sonido» de sesión ya existente, sin control nuevo en las pantallas en línea.
- Navegación entre pantallas sin librería de navegación. Back desde la partida local o desde el lobby vuelve a la pantalla inicial; desde la pantalla inicial aplica la navegación normal. La pantalla actual se conserva al rotar y tras muerte del proceso; una partida en línea guardada se restaura como lobby.
- El modo local se detiene mientras su pantalla no está visible: su turno pendiente, sus jugadas y sus sonidos no avanzan, y al volver continúa desde el tiempo restante.
- Primera tarea de implementación: una prueba de build con Firebase y el plugin `google-services`, por el riesgo de compatibilidad con la versión de AGP y Gradle del proyecto, con un plan alternativo de inicialización por código (ver `design.md`).
- Reglas de seguridad de la base versionadas en el repositorio y publicadas a mano por el usuario en la consola de Firebase.

## Capabilities

### New Capabilities

- `game-mode-selection`: pantalla inicial con las dos entradas, navegación Back entre pantallas, conservación de la pantalla actual (rotación y muerte del proceso), conservación de la partida local al salir y volver, pausa del modo local mientras no es visible, orientación y accesibilidad.
- `online-lobby`: apodo (primera vez, validación, recordado, cambio), conexión e identidad anónima, lista de juegos disponibles en tiempo real, crear y cancelar un juego, desaparición del juego si su creador se va, unirse, carrera al unirse, regla de que solo se juega tras unirse, orientación y accesibilidad.
- `online-match`: dos jugadores y marcas, turno y jugadas válidas, jugadas confirmadas por el servicio y sincronizadas, resultado, fin sin revancha, abandono del rival, salida con confirmación, marcador local intacto, sonido, rotación y segundo plano, pérdida de conexión, pantalla encendida, orientación y accesibilidad.

### Modified Capabilities

- `game-options`: el requisito «Menú principal de opciones» cambia solo en lo relativo a Back: sin overlay, Back desde la partida local vuelve a la pantalla inicial conservando la partida.
- `game-session`: el requisito «Límites de la sesión en memoria» cambia solo en lo relativo a Back: desde la partida local vuelve a la pantalla inicial sin reiniciar la sesión.

`game-sound-effects` no se modifica: sus requisitos siguen siendo ciertos para el modo local y la opción «Sonido» de sesión es la misma.

## Impact

- Código: nueva navegación entre pantallas y pantalla inicial; lobby y partida en línea con sus ViewModels; un límite con el servicio en la nube con implementación Firebase; almacén del apodo; adaptación mínima del tablero y de `MainActivity` (ver `design.md` para nombres y rutas).
- Dependencias: Firebase (BoM, autenticación y Realtime Database) y el plugin `google-services`; permiso `INTERNET` en el manifest; subida de Kotlin a 2.4.x (2.4.21), porque los artefactos del BoM 35.0.0 traen metadatos de Kotlin 2.4.
- Configuración externa: archivo de reglas versionado, que el usuario publica a mano; proveedor de inicio de sesión anónimo, que debe estar activado en la consola de Firebase y no está verificado.
- Pruebas: nuevas pruebas JVM y de Compose con fakes; adaptación de las pruebas instrumentadas existentes que asumen el tablero como pantalla de arranque; verificación de extremo a extremo con dos instancias.
- Especificaciones vigentes: `game-options` y `game-session` (solo Back).

## Out of Scope

- Cuentas de usuario con registro; revancha; marcador en línea; periodo de gracia ante desconexión; servidor propio o backend separado; librería de navegación; bajar `minSdk` (se mantiene en 35); restaurar una partida en línea tras muerte del proceso; menú de opciones en las pantallas en línea; protección frente a trampas de un participante (el servicio no valida las reglas del juego; ver `design.md`).

## Assumptions Confirmed by the User

Decisiones del planificador que el usuario confirmó el 2026-10-09; pueden revisarse si las pruebas lo aconsejan. Figuran como decisiones con alternativa descartada en `design.md`:

- **A2**: el abandono también se detecta por desconexión, sin periodo de gracia añadido por la aplicación (inmediato si el rival abandona o cierra la aplicación; si su red se corta de golpe, lo decide el servicio y puede tardar del orden de minutos; ver D16). Riesgo aceptado: una caída de red, o más de unos segundos en segundo plano, termina la partida. Alternativa descartada: presencia por jugador con periodo de gracia.
- **A3**: salir de una partida en línea en curso (Back o «Salir») pide confirmación, porque abandona la partida. Cancelar un juego en espera de rival no la pide.
- **A4**: el apodo se puede cambiar desde el lobby reabriendo el mismo diálogo, con validación de recorte de espacios y de 1 a 20 caracteres.
- **A5**: tras muerte del proceso no se restaura una partida en línea: se llega al lobby. La rotación sí la conserva.
- **A6**: las pantallas en línea no tienen menú de opciones. Dificultad y reinicio de puntajes son solo del modo local.

Además, los specs fijan textos de interfaz que no estaban decididos («Conectando…», «No se pudo conectar con el servicio», «Cambiar apodo», «Tu apodo», «Apodo», «Aceptar», «Escribe un apodo de 1 a 20 caracteres», «No se pudo crear el juego», «Juegas con X» / «Juegas con O», «Rival: <apodo>»); pueden cambiarse sin afectar el resto del cambio.

## Deviation from Specification

El enunciado dice que la aplicación debe mostrar al inicio una lista de juegos disponibles. El usuario aceptó que, al convivir los dos modos, la primera pantalla sea la pantalla inicial con dos entradas y que la lista de juegos quede a un toque de distancia («Jugar en línea» lleva al lobby con la lista).
