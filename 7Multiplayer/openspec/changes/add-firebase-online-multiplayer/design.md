# Design

## Context

Ver `proposal.md` para motivación, alcance, supuestos confirmados y la desviación aceptada; los requisitos observables viven en los deltas de `specs/`.

Estado actual verificado en el código (`app/src/main/java/com/edu/unal/tictactoe/`, abreviado `P/`):

- `P/MainActivity.kt` es una sola pantalla sin librería de navegación: crea `GameViewModel` con alcance de Activity, construye `MoveSoundHost(gameViewModel, AndroidMoveSoundPlayerFactory(applicationContext))` y delega en él `onResume`, `onPause` (antes de `super.onPause()`, para que el snapshot quede en el `SavedStateHandle` antes de que el framework serialice) y `onDestroy`.
- `GameViewModel` (`P/ui/game/GameViewModel.kt`) recibe `GamePreferences`, `SavedStateHandle`, motor, reloj y dispatcher. Su `onHostResumed()` retorna si ya está en primer plano y `onHostPaused()` retorna si no lo está, guarda el snapshot y limpia el sink de sonido. `MoveSoundHost.onResume()` hace `prepare()`, registra el sink y llama `onHostResumed()`; `onPause()` llama `onHostPaused()` solo si su token sigue vigente y luego `controller.pause()`. Ambos son idempotentes. `MoveSoundController` implementa `MoveSoundSink` (`play(player)`, `stop()`), recibe `playerFactory`, `isOwnerCurrent` (por defecto `{ true }`) y `soundEnabled`, y no conoce a `GameViewModel`; `prepare()` retorna si ya está en primer plano y `pause()` retorna si no lo está.
- `TicTacToeGame` (`P/game/TicTacToeGame.kt`) solo importa `java.util`, `kotlin.math` y modelos propios; es independiente de Android pero mutable. `setMove(player, location)` valida ubicación, fin de partida y ocupación, y muta el tablero; `checkForWinner()` deriva el resultado; `restoreBoard(marks)` rechaza posiciones ilegales. El motor no impone alternancia de turnos.
- `GameBoard(board, isGameActive, turn, onCellClick)` (`P/ui/game/GameBoard.kt`) habilita una casilla con `mark == Mark.Empty && isGameActive && turn == Turn.Human`; ese `Turn` es del modo local.
- La inyección es manual: `gameViewModelFactory()` (`P/ui/game/GameViewModelFactory.kt`) y la interfaz pequeña `GamePreferences` (`P/data/preferences/GamePreferences.kt`) con fakes en `app/src/test/.../data/preferences/FakeGamePreferences.kt` y `app/src/androidTest/.../InMemoryGamePreferences.kt`.
- `app/build.gradle.kts`: `minSdk = 35`, `compileSdk 37`, `targetSdk = 37`, plugins con `alias(libs.plugins.…)`; `gradle/libs.versions.toml`: AGP 9.4.1, Kotlin 2.4.21 (era 2.2.10; subido por este cambio, ver D6), Compose BOM 2026.09.00, Lifecycle 2.11.0, core-ktx 1.19.1, coroutines 1.11.0 (solo para pruebas; en `main` llega de forma transitiva). `AndroidManifest.xml` no declara permisos. Gradle 9.8.1 con `org.gradle.configuration-cache=true`. R8 no está activo en release (`optimization { enable = false }`).
- Specs vigentes que este cambio toca: `openspec/specs/game-options/spec.md` (menú de cinco entradas; sin overlay, Back aplica "la navegación normal del sistema sin interceptación global") y `openspec/specs/game-session/spec.md` ("Back aplica navegación normal y no garantiza finalizar tarea"). `game-sound-effects` sigue siendo cierto para el modo local y no se modifica.
- `app/google-services.json` existe en disco, sin versionar y sin ignorar por git (`git check-ignore` no lo reporta). Su contenido no se copia a ningún artefacto.

Los cambios archivados excluyeron a propósito lo que aquí se introduce: `openspec/changes/archive/2026-10-03-add-compose-menus-dialogs/design.md:23` deja fuera "Multijugador, navegación con librería" y `openspec/changes/archive/2026-09-13-add-tic-tac-toe-game/design.md:63` dice "No se añadirán repositories, casos de uso, DI ni capas genéricas". Este cambio es el reto 7, que pide exactamente multijugador; las excepciones se justifican en D1 y D4.

## Goals / Non-Goals

**Goals:** convivencia de los dos modos sin alterar el comportamiento local salvo Back; sincronización de partidas entre dos dispositivos con Firebase; un modelo de datos y unas reglas de seguridad que garanticen lo esencial (identidad, quién escribe, quién mueve) y dejen explícito lo que valida el cliente; un límite probable con fakes para probar sin red; que el modo local no avance (turno de la computadora, jugadas, sonido) mientras su pantalla no está visible.

**Non-Goals:** servidor propio, Cloud Functions, cuentas con registro, revancha, marcador en línea, periodo de gracia ante desconexión, persistencia en disco de la caché de Firebase, autoridad del servidor sobre las reglas del juego (el cliente valida; ver D8 y Riesgos), librería de navegación, DI genérica, cambiar `minSdk`. La lista, el estado de espera y la partida no se restauran tras muerte del proceso (A5).

## Decisions

### D1. Navegación manual con un ViewModel pequeño y `BackHandler`

- Cuatro pantallas: inicio, partida local, lobby (incluye el diálogo de apodo y el estado de espera de rival) y partida en línea. Se modelan con un enum `AppScreen { Start, Local, Lobby, Match }`.
- `NavigationViewModel` con alcance de Activity guarda la pantalla actual en su `SavedStateHandle` (se serializa el nombre del enum), de modo que sobrevive rotación y muerte del proceso. Al construirse, si lo guardado es `Match`, arranca en `Lobby` (A5): la partida en línea no se puede reconstruir porque su identidad vive en `OnlineMatchViewModel`, que no se guarda. Un valor guardado desconocido arranca en `Start`.
- `AppNavigation` (composable raíz) elige qué pantalla dibujar y declara los `BackHandler` de `androidx.activity.compose` (dependencia existente, `activity-compose`): `Local` → inicio; `Lobby` → inicio; `Lobby` esperando rival → cancela el juego y queda en el lobby, sin confirmación; `Match` en curso → diálogo «¿Abandonar la partida?»; `Match` terminada o abandonada → lobby; `Start` → sin handler, o sea navegación normal del sistema. Un menú o diálogo abierto consume Back antes (cada uno es una ventana propia, como hoy), así que no necesita lógica adicional.
- Alternativa descartada: librería de navegación (Navigation Compose). Para cuatro destinos sin argumentos de ruta ni deep links, una dependencia nueva y su manejo de back stack y de estado guardado aportan más superficie que un enum; además el estado de cada pantalla ya vive en ViewModels de Activity. Es una excepción acotada a la exclusión del cambio archivado `2026-10-03-add-compose-menus-dialogs`, justificada porque el reto exige un segundo modo y no hay una forma de alternar entre ambos sin algún mecanismo de pantalla actual.

Archivos: `P/ui/navigation/AppScreen.kt`, `P/ui/navigation/NavigationViewModel.kt`, `P/ui/navigation/AppNavigation.kt`, `P/ui/start/StartScreen.kt`. `MainActivity.kt` pasa a montar `AppNavigation`.

### D2. Modo local pausado sin tocar `GameViewModel` ni `MoveSoundHost`

`MainActivity` mantiene dos banderas: "Activity en RESUMED" y "pantalla actual = `Local`". Llama a `moveSoundHost.onResume()` solo cuando ambas pasan a ser verdaderas y a `moveSoundHost.onPause()` cuando deja de cumplirse cualquiera. La función que decide se invoca desde `onResume`, desde `onPause` (antes de `super.onPause()`, conservando la ruta de captura del snapshot) y desde un colector de la pantalla actual lanzado en `lifecycleScope`. Es viable porque `onHostResumed`/`onHostPaused`, `MoveSoundController.prepare`/`pause` y `MoveSoundHost.onResume`/`onPause` son idempotentes (hechos de Context). Con la pantalla local fuera de vista, `onHostPaused()` ya cancela el `Job` del turno pendiente, descuenta el tiempo transcurrido, guarda el snapshot y limpia el sink; al volver, `onHostResumed()` continúa desde el tiempo restante.

- Alternativa descartada: añadir métodos de pausa nuevos a `GameViewModel`. Duplicaría la semántica de primer plano ya probada y obligaría a tocar el contrato de `MoveSoundHost`.

### D3. ViewModels nuevos con alcance de Activity y reinicio explícito

- `OnlineLobbyViewModel` (`P/ui/online/lobby/`): apodo, lista de juegos disponibles, crear, espera de rival, cancelar, unirse, errores y estado de conexión. La lista se expone con `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000))`, de modo que la suscripción a Firebase se cierra poco después de salir del lobby y sobrevive a la rotación. Cuando el juego propio pasa a `playing` o una unión tiene éxito, publica un `MatchTicket(gameId, miMarca)` pendiente en su estado; la navegación lo consume una vez para llamar a `OnlineMatchViewModel.openMatch(...)` y pasar a `Match`.
- `OnlineMatchViewModel` (`P/ui/online/match/`): una sola instancia reutilizada. `openMatch(gameId, miMarca)` reemplaza todo el estado anterior y arranca la observación; salir cancela la observación y limpia el estado. La observación sobrevive a rotación y a segundo plano.
- Alternativas descartadas: generalizar `GameViewModel` (mezcla turnos contra la computadora, puntaje y snapshot con un flujo remoto sin relación); un ViewModel por partida con clave de `ViewModelProvider` (cada partida dejaría una instancia en el `ViewModelStore` de la Activity hasta su destrucción).
- Ambos reciben el servicio por constructor y se crean con fábricas manuales en el mismo estilo que `gameViewModelFactory()`: `P/ui/online/lobby/OnlineLobbyViewModelFactory.kt` y `P/ui/online/match/OnlineMatchViewModelFactory.kt` (el cableado de producción vive en un único objeto `P/data/online/OnlineServices.kt` que expone el servicio Firebase y el almacén de apodo).

### D4. Interfaz `OnlineGameService` con implementación Firebase y fakes

`P/data/online/OnlineGameService.kt` define el límite. Operaciones: asegurar identidad anónima (devuelve mi uid o falla); observar conexión (`Flow<Boolean>`); observar juegos disponibles (`Flow<List<OnlineGame>>`); crear (devuelve el id); cancelar; unirse (resultado tipado `Joined` o `NoLongerAvailable`); observar un juego (`Flow<OnlineGame?>`, emite nulo si el nodo deja de existir); jugar (resultado tipado `Accepted` o `Rejected`); salir. Modelos en `P/data/online/OnlineGame.kt` (`OnlineGame`, `OnlinePlayer(uid, name)`, `OnlineStatus`) y la conversión del tablero de texto a `List<Mark>` en `P/data/online/BoardCodec.kt`.

- Implementación: `P/data/online/FirebaseOnlineGameService.kt`. Las `Task` de Firebase se convierten a `suspend` con un envoltorio propio sobre `suspendCancellableCoroutine` (`P/data/online/FirebaseTasks.kt`); los listeners (`.info/connected`, consulta de juegos, nodo de un juego) se envuelven en `callbackFlow` y se retiran en `awaitClose`. No se añade `kotlinx-coroutines-play-services`.
- Fakes: `app/src/test/java/com/edu/unal/tictactoe/data/online/FakeOnlineGameService.kt` y `app/src/androidTest/java/com/edu/unal/tictactoe/FakeOnlineGameService.kt`, con la misma convención de duplicación que `FakeGamePreferences` e `InMemoryGamePreferences`.
- Justificación de la excepción a "sin repositories": el cambio archivado la formuló para un juego sin I/O. Aquí hay un SDK externo con listeners, transacciones y estados de red que no se pueden ejecutar en pruebas JVM ni controlar en pruebas instrumentadas; una interfaz pequeña es la mínima separación que permite probar los ViewModels con fakes, igual que `GamePreferences`. No hay casos de uso, DI ni capas genéricas.

### D5. Realtime Database con autenticación anónima

- Frente a Firestore: Realtime Database ofrece `onDisconnect`, que ejecuta en el servidor una operación registrada cuando la conexión del cliente termina de forma limpia o no, y es lo que permite detectar abandono sin servidor propio ([offline-capabilities](https://firebase.google.com/docs/database/android/offline-capabilities)). Firestore no tiene un equivalente directo.
- Frente a un servidor propio: no hay backend que escribir ni desplegar y el proyecto de Firebase ya existe.
- La identidad es la de Firebase Auth anónimo: el SDK conserva el uid entre aperturas de la app. La caché en disco de Realtime Database no se activa (`setPersistenceEnabled` no se llama), para no mostrar juegos obsoletos.

### D6. Build y manifest

- `gradle/libs.versions.toml`: versión del BoM de Firebase `35.0.0` (vigente al 2026-10-07 según la documentación oficial), libraries `firebase-bom`, `firebase-auth` y `firebase-database` (sin sufijo `-ktx`: desde el BoM 34.0.0 no existen módulos KTX) y el plugin `google-services`. No se fija un número de versión del plugin en este diseño: la primera tarea lo decide al probarlo.
- `build.gradle.kts` raíz: `alias(libs.plugins.google.services) apply false`; `app/build.gradle.kts`: `alias(libs.plugins.google.services)`, `implementation(platform(libs.firebase.bom))`, `implementation(libs.firebase.auth)`, `implementation(libs.firebase.database)`.
- `AndroidManifest.xml`: `<uses-permission android:name="android.permission.INTERNET" />`. Auth exige `minSdk` 24 o superior; con 35 se cumple.
- No se añaden reglas keep: R8 no está activo.
- Kotlin sube de 2.2.10 a 2.4.21 (última 2.4.x estable en Maven Central al 2026-10-10; decisión del usuario del 2026-10-10, que prefiere las versiones más altas mientras el proyecto está en desarrollo). Motivo: `firebase-auth` 25.0.0 (BoM 35.0.0) trae metadatos de Kotlin 2.4.0 y con Kotlin 2.2.10 `:app:compileDebugKotlin` falla con «Module was compiled with an incompatible version of Kotlin». Alternativa descartada: bajar el BoM a 34.19.0 (`firebase-auth` 24.2.0, `firebase-database` 22.0.2), que también compilaba con Kotlin 2.2.10. Solo cambia `kotlin` en el catálogo (el plugin de Compose comparte esa versión).
- Dependencias y wrapper al día (tarea 1.8): Compose BOM 2026.09.00, core-ktx 1.19.1, `kotlinx-coroutines-test` 1.11.0 y Gradle 9.8.1 (AGP 9.4 exige Gradle 9.6.0 o superior), por decisión del usuario del 2026-10-10 de usar las últimas versiones estables mientras el proyecto está en desarrollo; el detalle y la verificación están en `qa/dependency-upgrade.md`.
- Riesgo verificado: el plugin `google-services` 4.5.0 funciona con AGP 9.4.1, Gradle 9.6.0 y la caché de configuración (`assembleDebug` dos veces seguidas, la segunda reutiliza la caché; evidencia en `qa/firebase-build-probe.md`). El plan alternativo (quitar el plugin e inicializar con `FirebaseOptions` por código, con `apiKey`, `applicationId` y `databaseUrl` leídos de un `BuildConfig` o recurso, lo que haría versionar esos valores en código) queda documentado como no necesario, salvo que falle la inicialización de `FirebaseApp` en el dispositivo (tarea 1.5).

### D7. Modelo de datos: un nodo por juego, que existe mientras sus participantes sigan conectados

    /games/{gameId}
      status:    "waiting" | "playing" | "finished"
      host:      { uid, name }
      guest:     { uid, name }      (ausente mientras espera)
      board:     "---------"        (9 caracteres: '-', 'X', 'O')
      turn:      "X" | "O"
      result:    "none" | "X" | "O" | "tie"
      createdAt: marca de tiempo del servidor

- **Crear:** se obtiene la clave con `push()`, se registra primero `onDisconnect().removeValue()` sobre ese nodo (aún no existe; así una caída entre las dos operaciones no deja un juego huérfano en la lista), se espera su confirmación (si falla, no se crea el juego y se informa el error) y después se escribe el nodo con `status = waiting`, `board = "---------"`, `turn = "X"`, `result = "none"` y `createdAt = ServerValue.TIMESTAMP`. Quien crea lleva X y empieza.
- **`onDisconnect` y reglas:** el servidor comprueba las reglas de seguridad dos veces, al establecer la operación y de nuevo al dispararla ([offline-capabilities](https://firebase.google.com/docs/database/android/offline-capabilities)). Los tres casos quedan cubiertos por D8: quien crea la establece sobre un nodo inexistente (borrado de nulo sobre nulo, permitido por la cláusula inocua) y al dispararse ya es participante; el invitado es participante en ambas comprobaciones; y si el nodo ya fue borrado al dispararse, vuelve a aplicar la cláusula inocua.
- **Listar:** consulta `orderByChild("status").equalTo("waiting")` sobre `/games`, con `.indexOn` en las reglas; el cliente ordena por `createdAt` descendente.
- **Unirse:** `runTransaction` sobre el nodo. El manejador aborta si el nodo existe pero no está en `waiting` o ya tiene `guest`; si no, escribe `guest` y `status = playing`. Si el valor entrante es nulo, el manejador devuelve éxito sin modificar el dato (el SDK puede invocarlo con nulo antes de tener el valor del servidor y lo reintenta con el real; abortar en ese caso reportaría "ya no disponible" de forma errónea). El resultado se interpreta con el snapshot confirmado: unido si en él `guest.uid` es mi uid y `status` es `playing`; en cualquier otro caso, `NoLongerAvailable`. Ese éxito sin cambios sobre un nodo inexistente es una escritura de nulo sobre nulo, que las reglas admiten por la cláusula inocua de D8. Cualquier error de la transacción (permiso denegado, red, cancelación) se traduce también a `NoLongerAvailable`. El comportamiento con nulo no está verificado contra el SDK y se comprueba en la verificación de extremo a extremo (Riesgos). El invitado registra también `onDisconnect().removeValue()`.
- **Jugar:** `runTransaction` con `fireLocalEvents = false`, de modo que el tablero solo muestre jugadas confirmadas (firma `runTransaction(Transaction.Handler handler, boolean fireLocalEvents)` en la [referencia de `DatabaseReference`](https://firebase.google.com/docs/reference/android/com/google/firebase/database/DatabaseReference); con `false`, los eventos locales solo se disparan para el estado final de la transacción y no para estados intermedios). El manejador valida `status = playing`, que `turn` coincida con mi marca y que la casilla esté libre (D9), y escribe `board`, `turn`, `result` y, si terminó, `status = finished`. Un valor nulo se trata como en "Unirse", y tanto un snapshot confirmado sin mi jugada como cualquier error de la transacción se traducen a `Rejected`.
- **Salir o cancelar:** `removeValue()` del nodo y `onDisconnect().cancel()`.
- **Abandono:** no hay estado "abandoned" en la base. El cliente muestra abandono si el nodo desaparece mientras el último estado conocido era `playing` o si el primer valor observado tras `openMatch` es nulo (la partida solo se abre cuando ya estaba en `playing`). Un resultado terminal (`finished`) es pegajoso en el cliente: una desaparición posterior no lo cambia. Si a quien espera se le borra su juego, el lobby vuelve a su lista y muestra «Este juego ya no está disponible».
- **Limpieza automática:** el nodo se borra cuando cualquier participante sale o se desconecta.
- Alternativa descartada: marcas de presencia por jugador con periodo de gracia (`/presence/{uid}`, tiempo de espera antes de declarar abandono). Resolvería caídas breves de red pero exige un temporizador compartido, un estado intermedio y más reglas; A2 acepta el riesgo a cambio de simplicidad.
- Riesgos propios de este modelo (ver Riesgos): una caída de red termina la partida (A2); si la jugada final y la salida del ganador llegan fusionadas al otro cliente, este vería abandono en vez del resultado, porque Realtime Database garantiza el estado final y no cada estado intermedio.

### D8. Reglas de seguridad versionadas y publicadas a mano

Archivo previsto: `7Multiplayer/firebase/database.rules.json` (no se crea en este cambio de planificación). El usuario lo publica en la consola de Firebase y lo prueba en el simulador de reglas antes de la verificación de extremo a extremo. La base se creó en modo bloqueado y las reglas en modo de prueba caducan, así que sin publicarlas la aplicación no puede leer ni escribir.

Borrador revisado (no verificado en el simulador de reglas):

    {
      "rules": {
        ".read": false,
        ".write": false,
        "games": {
          ".read": "auth != null",
          ".indexOn": ["status"],
          "$gameId": {
            ".write": "auth != null && ((!data.exists() && newData.child('host/uid').val() === auth.uid && newData.child('status').val() === 'waiting' && !newData.child('guest').exists()) || (data.exists() && (data.child('host/uid').val() === auth.uid || data.child('guest/uid').val() === auth.uid)) || (data.child('status').val() === 'waiting' && !data.child('guest').exists() && newData.child('guest/uid').val() === auth.uid) || (!data.exists() && !newData.exists()))",
            ".validate": "newData.hasChildren(['host', 'status', 'board', 'turn', 'result'])",
            "host": {
              ".validate": "newData.hasChildren(['uid', 'name']) && (!data.exists() || newData.child('uid').val() === data.child('uid').val())",
              "uid": { ".validate": "newData.isString()" },
              "name": { ".validate": "newData.isString() && newData.val().length >= 1 && newData.val().length <= 20" },
              "$other": { ".validate": false }
            },
            "guest": {
              ".validate": "newData.hasChildren(['uid', 'name']) && (!data.exists() || newData.child('uid').val() === data.child('uid').val())",
              "uid": { ".validate": "newData.isString()" },
              "name": { ".validate": "newData.isString() && newData.val().length >= 1 && newData.val().length <= 20" },
              "$other": { ".validate": false }
            },
            "status": { ".validate": "newData.val() === 'waiting' || newData.val() === 'playing' || newData.val() === 'finished'" },
            "board": { ".validate": "newData.isString() && newData.val().matches(/^[XO-]{9}$/) && (!data.exists() || newData.val() === data.val() || (data.parent().child('turn').val() === 'X' && data.parent().child('host/uid').val() === auth.uid) || (data.parent().child('turn').val() === 'O' && data.parent().child('guest/uid').val() === auth.uid))" },
            "turn": { ".validate": "newData.val() === 'X' || newData.val() === 'O'" },
            "result": { ".validate": "newData.val() === 'none' || newData.val() === 'X' || newData.val() === 'O' || newData.val() === 'tie'" },
            "createdAt": { ".validate": "newData.isNumber()" },
            "$other": { ".validate": false }
          }
        }
      }
    }

Correcciones respecto del borrador recibido, con su motivo:

1. La cláusula de creación ahora exige también `status === 'waiting'` y la ausencia de `guest`. Sin ellas, quien crea podría publicar directamente un juego `playing` con un invitado arbitrario, saltándose la transición de unirse.
2. Se añadieron `uid` y `name` dentro de `host` y `guest` (tipo de cadena; apodo de 1 a 20 caracteres con `length`) y `$other` falso. El borrador solo validaba que existieran, de modo que se podían guardar hijos de cualquier tipo y longitud. Esto vuelve cierta una validación de la longitud del apodo en el servidor, que el borrador daba por no validada; el recorte de espacios sigue siendo del cliente (las reglas no recortan).
3. Se añadió a `.write` la cláusula inocua `(!data.exists() && !newData.exists())`: un usuario autenticado puede escribir nulo sobre un nodo inexistente. No cambia ningún dato y es necesaria en dos casos que las otras tres cláusulas rechazaban: establecer `onDisconnect().removeValue()` antes de que el nodo exista (o dispararlo cuando ya fue borrado) y confirmar una transacción que no modifica un nodo inexistente (D7).
4. Se mantienen sin cambios, por ser correctos según la [documentación de reglas](https://firebase.google.com/docs/database/security/rules-conditions): `data`, `newData`, `data.parent()`, `exists()`, `hasChildren([...])`, `matches(/regex/)`, `isString()` e `isNumber()` existen; `.validate` no cascada y se ignora al borrar (valor nulo), lo que permite a un participante borrar el nodo aunque no pase la validación; `.read` en `/games` es necesario para la consulta porque las reglas no son filtros (un `.read` más profundo no sustituye al de `/games`); `.indexOn` va en `/games`, el nivel de la consulta ([indexing-data](https://firebase.google.com/docs/database/security/indexing-data)).
5. Se aceptó sin cambio la validación de `board` con `data.parent()`: dentro de `.validate`, `data` es el valor previo de `board` y `data.parent()` el nodo del juego previo, de modo que comprueba que solo el dueño del turno anterior cambie el tablero.

Qué validan: autenticación; que solo los dos participantes escriban o borren un juego existente (la única escritura abierta a cualquier usuario autenticado es la inocua de nulo sobre un nodo inexistente); la transición de unirse (solo sobre un juego `waiting` sin invitado); que solo quien tiene el turno cambie el tablero; la forma y el rango de los campos (incluido el apodo de 1 a 20 caracteres). Qué NO validan: que la jugada cambie exactamente una casilla libre; la coherencia de `turn`, `result` y `status` con el tablero; el recorte de espacios del apodo; que un participante no reescriba `status`, `turn` o `result`. Eso lo valida el cliente (D9). Consecuencia aceptada: un participante malintencionado podría falsear su propia partida; no se protege frente a trampas porque no hay servidor. Además, cualquier usuario autenticado puede leer todos los juegos, incluidos los que están en curso.

### D9. Validación de la jugada en el cliente con el motor existente

Dentro de la transacción de jugar, se usa una instancia de trabajo de `TicTacToeGame`: `restoreBoard` con el tablero actual, `setMove(jugador, casilla)`, `checkForWinner()`. Un `restoreBoard` falso, un `setMove` distinto de `Applied`, `status ≠ playing` o `turn` distinto de mi marca abortan la transacción (resultado `Rejected`). Con la jugada aplicada se escriben `board`, `result` (`X`, `O`, `tie` o `none`) y `turn` (la otra marca; sin cambio si terminó) y, al terminar, `status = finished`. La alternancia la impone el campo `turn`, porque el motor no la impone. Como `restoreBoard` solo comprueba la legalidad intrínseca de la posición (diferencia de marcas de a lo sumo una, en cualquier sentido, y un único ganador coherente con el conteo) y no quién empezó ni de quién es el turno, un tablero escrito por un participante con forma legal pero incoherente con `turn` no se detecta; es el límite descrito en D8.

### D10. `GameBoard` neutral

En `P/ui/game/GameBoard.kt` se añade un composable `internal` neutral con firma (tablero, habilitado, callback) que contiene el dibujo y la lógica de casillas actual. El `GameBoard(board, isGameActive, turn, onCellClick)` existente se reduce a delegar en él con `isGameActive && turn == Turn.Human`. Las llamadas y pruebas existentes no cambian. La pantalla en línea usa el neutral con "habilitado = partida en curso y es mi turno y estoy conectado". Alternativa descartada: mapear "mi turno" a `Turn.Human` en el modo en línea, que haría depender el significado de un tipo local de una convención ajena (la persona siempre es X).

### D11. Apodo persistido

Interfaz pequeña `NicknameStore` (`readNickname(): String?`, `writeNickname(String)`) en `P/data/online/NicknameStore.kt`, con implementación `SharedPreferencesNicknameStore.kt` (archivo de preferencias propio) y fakes (`FakeNicknameStore` en `src/test` y en `src/androidTest`). `GamePreferences` no se toca: sus semánticas (puntaje y dificultad del modo local) no deben mezclarse con el apodo. La validación (recorte de espacios, 1 a 20 caracteres) es una función pura en `P/ui/online/lobby/NicknameValidator.kt`, probada en JVM. El uid lo conserva el SDK de Firebase Auth.

### D12. Ciclo de vida y conexión

- La identidad anónima se asegura al entrar al lobby si no hay usuario; el resultado alimenta el estado de carga y el error con «Reintentar».
- La conexión se observa con `.info/connected`. Hace falta porque, sin conexión, Realtime Database no falla: encola las escrituras y los listeners no entregan datos nuevos (de ahí que la documentación recomiende `.info/connected` para detectarlo; ver [offline-capabilities](https://firebase.google.com/docs/database/android/offline-capabilities)). Mientras no haya conexión, lobby y partida muestran «Sin conexión», deshabilitan sus acciones y se recuperan solos al volver. La documentación advierte que `.info/connected` en falso de un cliente no implica lo mismo para el otro.
- Rotación: los tres ViewModels de Activity sobreviven; no se reabre ninguna observación. Segundo plano: la partida continúa mientras el proceso conserve la conexión; si se pierde, aplica A2. Muerte del proceso: A5; al regresar solo se restaura la pantalla guardada (D1).

### D13. Estrategia de pruebas

- JVM con fakes y el arnés de corrutinas existente (`app/src/test/java/com/edu/unal/tictactoe/ui/game/GameViewModelCoroutineTestHarness.kt`): `NavigationViewModel` (restauración, `Match` → `Lobby`), `OnlineLobbyViewModel`, `OnlineMatchViewModel` (confirmación, abandono, terminal pegajoso, regla de sonido), `NicknameValidator` y `BoardCodec`.
- Compose instrumentado con fakes: inicio, lobby y partida en línea, en portrait y landscape.
- Adaptación de las instrumentadas existentes: sus `MainActivity*` asumen que el tablero es la pantalla de arranque. Clases afectadas, verificadas en `app/src/androidTest/java/com/edu/unal/tictactoe/`: `MainActivityTest`, `MainActivityOptionsTest`, `MainActivitySoundOptionsTest`, `MainActivityPendingTurnOptionsTest`, `MainActivityAccessibilityTest`, `MainActivityLifecycleTest` y `GameViewModelSavedStateWiringTest` (lanzan `MainActivity`); además `MainActivityOptionsTest`, `AboutDialogTest` y `DifficultyDialogTest` usan `pressBackAsUser` de `TestUserNavigation.kt`, y `RestoredSessionHost.kt` usa la factory de producción. Se resuelve con un ayudante de prueba que pulsa «Jugar contra la computadora» tras lanzar la Activity, sin puertas traseras en producción; el escenario vigente de Back sin overlay se actualiza al nuevo comportamiento (vuelve al inicio).
- Verificación de extremo a extremo de la implementación Firebase con dos instancias (dos emuladores, o emulador más dispositivo), tras publicar las reglas y activar la autenticación anónima; incluye la comprobación del manejo de nulo en transacciones (D7) y la fusión de la jugada final con la salida del ganador.
- Evidencia en `openspec/changes/add-firebase-online-multiplayer/qa/`, sin números de serie ni modelos de dispositivo. Los flujos manuales de sonido y TalkBack siguen siendo manuales.

### D14. Textos y accesibilidad

Todos los textos van en `app/src/main/res/values/strings.xml` con los literales de `specs/`. Los objetivos táctiles miden al menos 48 dp, cada control expone una etiqueta de recursos y los controles deshabilitados exponen su estado a las tecnologías de asistencia. El tablero en línea conserva la semántica de casillas del tablero local (ubicación, contenido, disponibilidad).

### D15. Sonido de las partidas en línea

- `MainActivity` crea un segundo `MoveSoundController` con `playerFactory = AndroidMoveSoundPlayerFactory(applicationContext)`, `soundEnabled = { gameViewModel.uiState.value.soundEnabled }` e `isOwnerCurrent` por defecto. Lo prepara cuando la Activity está en RESUMED y la pantalla es la partida en línea, lo pausa en caso contrario (mismo mecanismo de banderas que D2) y lo libera en `onDestroy`. Solo uno de los dos controladores está preparado a la vez.
- `OnlineMatchViewModel` no guarda el controlador. Expone un `SharedFlow<Player>` de jugadas aceptadas con `replay = 0`; la pantalla lo recoge solo en RESUMED y lo traduce a `controller.play(jugador)`. Lo que emite mientras no hay recolector se descarta, por lo que no hay sonido en segundo plano ni se acumula.
- Regla de emisión: solo cuando el tablero confirmado tiene exactamente una marca más que el último tablero conocido; el jugador emitido es el dueño de esa marca nueva. Nunca se emite con el primer estado tras `openMatch` ni al resincronizar (estados con el mismo número de marcas o más de una marca nueva). Aplica igual a la jugada propia y a la del rival: ambas suenan al ser confirmadas, una sola vez.
- Alternativa descartada: generalizar `MoveSoundHost` o compartir su controlador; ligaría su ciclo de vida al de la sesión local y chocaría con D2.

## Risks / Trade-offs

- [El plugin `google-services` podía no funcionar con AGP 9.4.1, Gradle 9.6.0 y la caché de configuración] → verificado con el plugin 4.5.0 (tarea 1.4); el plan alternativo con `FirebaseOptions` por código (D6) solo se usa si falla la inicialización en el dispositivo (tarea 1.5).
- [El compilador de Kotlin cambió de 2.2.10 a 2.4.21 para todo el código existente y las pruebas instrumentadas aún no se han vuelto a ejecutar con él] → las pruebas JVM (221) y el build pasan; las instrumentadas se cubren al adaptar y ejecutar las clases en el grupo 3 y en la suite completa de 8.2.
- [La autenticación anónima podría no estar activada en la consola de Firebase (sin verificar)] → precondición del Migration Plan; la aplicación muestra el error con «Reintentar» en vez de colgarse.
- [Una caída de red termina la partida porque el abandono es inmediato (A2)] → riesgo aceptado por el usuario (A2, confirmado el 2026-10-09), revisable si las pruebas lo aconsejan; la alternativa con periodo de gracia está descrita y descartada en D7.
- [La jugada final y la salida del ganador pueden llegar fusionadas al otro cliente, que vería abandono en vez del resultado] → el terminal es pegajoso una vez visto y salir requiere una acción manual tras ver el resultado; la fusión es improbable pero posible y se acepta, y se prueba en la verificación de extremo a extremo.
- [Un participante puede falsear `status`, `turn` o `result` de su propia partida; cualquier usuario autenticado lee todos los juegos] → aceptado para un contexto de clase sin servidor; el cliente valida (D9) y las reglas limitan lo esencial (D8).
- [El manejo del valor nulo en `runTransaction` no está verificado contra el SDK] → el manejador lo trata como "valor aún no conocido" y el resultado se interpreta con el snapshot confirmado; se comprueba con dos instancias antes de dar la tarea por cerrada (D7, D13).
- [Con `fireLocalEvents = false` el tablero propio se actualiza tras la confirmación del servidor, con una latencia visible] → es el comportamiento pedido (solo jugadas confirmadas); la entrada se deshabilita mientras hay una jugada pendiente de confirmar.
- [Un juego huérfano podría quedar en la lista si el servidor nunca registró el `onDisconnect`] → se registra antes de escribir el nodo, lo que las reglas admiten por la cláusula inocua (D7, D8); si el registro falla, no se crea el juego; un residuo se borra a mano desde la consola.
- [Las reglas publicadas a mano pueden divergir del archivo versionado] → el archivo es la fuente y la verificación de extremo a extremo se hace con ellas publicadas.
- [El plan gratuito admite 100 conexiones simultáneas] → suficiente para una clase; si se supera, nuevas conexiones fallan y aparece «Sin conexión».
- [Las instrumentadas `MainActivity*` fallan al cambiar la pantalla de arranque] → ayudante de prueba y adaptación explícita (D13).

## Migration Plan

Precondiciones del usuario, antes de la verificación de extremo a extremo: activar el proveedor de inicio de sesión anónimo en la consola de Firebase y publicar `firebase/database.rules.json`.

Orden: (1) prueba de build con Firebase y plugin, con el plan alternativo; (2) contratos y fakes (`OnlineGameService`, `NicknameStore`, modelos, `BoardCodec`); (3) navegación e inicio, con la adaptación de las pruebas existentes que dependa de la pantalla de arranque; (4) lobby; (5) partida en línea y `GameBoard` neutral; (6) sonido; (7) implementación Firebase y reglas; (8) adaptación del resto de pruebas existentes; (9) verificación de extremo a extremo y QA.

Rollback: revertir el cambio (código, dependencias, permiso `INTERNET` y archivo de reglas). Los datos remotos son efímeros (cada juego se borra al terminar); un residuo se borra desde la consola. El modo local no depende de nada de lo añadido salvo la pantalla de arranque.

## Open Questions

Ninguna. Decisión del usuario (2026-10-09): `app/google-services.json` se versiona en git. Base documental: Firebase indica que el archivo contiene identificadores únicos pero no secretos y que las claves de API de servicios de Firebase no necesitan tratarse como secretos y es seguro incluirlas en código o en archivos de configuración ([api-keys](https://firebase.google.com/docs/projects/api-keys), [setup Android](https://firebase.google.com/docs/android/setup)). Matiz de la misma documentación ([learn-more](https://firebase.google.com/docs/projects/learn-more)): para proyectos de código abierto generalmente no recomienda incluirlo, porque sus usuarios deberían crear su propio proyecto de Firebase; es una razón de pertenencia del proyecto, no de secreto, y no aplica a un repositorio de curso con un solo proyecto de Firebase. La seguridad de los datos depende de las reglas (D8) y de la clave de API restringida en la consola.
