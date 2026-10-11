# Las reglas de seguridad, regla por regla

Este documento explica `firebase/database.rules.json`, el archivo de reglas de Realtime Database del Triqui en línea. No lo modifica: lo describe tal como está. Está escrito para quien sabe programar pero no conoce el lenguaje de reglas de Firebase.

## 1. Introducción

La base de datos guarda cada partida en un nodo `/games/{gameId}`. Las reglas deciden quién puede leer y quién puede escribir ese contenido, y qué forma deben tener los datos. Siguen una idea sencilla:

- Solo los usuarios identificados leen. La app inicia sesión de forma anónima, así que "identificado" significa "tiene sesión anónima de Firebase Auth", no que tenga una cuenta.
- Solo los dos jugadores de una partida (el creador o `host`, y el invitado o `guest`) modifican o borran esa partida. Hay dos excepciones controladas: cualquier usuario identificado puede crear un juego nuevo y puede unirse a uno que esté esperando invitado.

Las reglas no sustituyen a un servidor. Protegen lo esencial, pero no hacen cumplir las reglas del Triqui (ver sección 6).

## 2. Cómo leer una regla

Las reglas son un árbol JSON cuya forma imita la de la base de datos. Cada nivel puede llevar cuatro tipos de regla, cuyo nombre empieza por punto. Las tres que usa este archivo son `.read`, `.write` y `.validate`; la cuarta, `.indexOn`, se explica en su sección. Cada regla es `true`/`false` o una expresión que da verdadero o falso.

**`.read` y `.write`** conceden permiso de leer y de escribir en ese nivel. La documentación oficial dice que "si una regla concede permisos de lectura o escritura en una ruta, también los concede a todos los nodos hijos", y que las reglas más superficiales prevalecen sobre las más profundas: los hijos solo pueden añadir privilegios, no revocar los que el padre ya concedió. Consecuencia práctica: en este archivo, el `.write` de `$gameId` vale para todo lo que cuelga de ese juego, y ningún hijo puede quitarlo. Además, las reglas no son filtros: una operación se rechaza entera si ninguna regla en esa ruta, o en una ruta superior, la concede. Por eso la lectura de la lista de juegos necesita un `.read` en `/games` y no basta con permitir leer cada juego.

**`.validate`** no concede permisos; comprueba que los datos resultantes tengan la forma correcta. Se diferencia de `.read`/`.write` en dos cosas. Primera, no se acumula hacia abajo: todas las reglas `.validate` de todos los niveles afectados deben cumplirse para que la escritura sea aceptada. Segunda, solo se evalúa sobre valores que no son nulos, así que al borrar (escribir nulo) se ignora. Por eso un participante puede borrar un juego aunque el borrado "no pasaría" la validación. Una escritura necesita pasar `.write` y, además, `.validate`.

**Variables que aparecen en las expresiones:**

- `data`: el contenido de esa ruta antes de la escritura.
- `newData`: el contenido que tendrá esa ruta si la escritura se acepta (ya fusionado con lo que había).
- `auth`: la información de la sesión. Es nula si no hay sesión; si la hay, `auth.uid` es el identificador único del usuario.
- `$gameId` (y `$other`): una variable de captura. Un nombre que empieza por `$` funciona como comodín: la regla debajo de `$gameId` se aplica a cualquier hijo de `games`, sea cual sea su clave. Las claves siempre son cadenas.
- Métodos usados: `exists()` (¿hay un valor?), `child('a/b')` (baja a un hijo), `val()` (el valor), `parent()` (sube al nodo padre), `hasChildren([...])` (¿están todos esos hijos?), `isString()`, `isNumber()` y `matches(/regex/)`.

Una nota de lectura: `===` es igualdad estricta, `!` es negación, `&&` es "y" y `||` es "o". Cuando un hijo no existe, `val()` devuelve nulo, que nunca es igual a un `uid`.

## 3. El archivo, regla por regla

### 3.1 La raíz: `.read` y `.write` en falso

```json
".read": false,
".write": false,
```

Significa: por defecto nadie lee ni escribe nada. Solo se concede acceso donde una regla más abajo lo diga expresamente. Permite: nada. Impide: por ejemplo, que alguien lea la raíz de la base entera o escriba en un nodo cualquiera que no sea `games`. Hace falta porque sin ella cualquier ruta que se olvide quedaría abierta o dependería del modo en que se creó la base.

### 3.2 `games` y su `.read`

```json
"games": {
  ".read": "auth != null",
```

Significa: cualquier usuario con sesión puede leer `/games` entero y, por la cascada, cualquier juego y cualquiera de sus campos. Permite: la app lee la lista de juegos en espera (consulta sobre `/games`) y observa su propia partida. Impide: que alguien sin sesión lea. Hace falta en `/games` y no solo en `$gameId` porque la lista es una consulta sobre `/games`, y las reglas no filtran: sin permiso en ese nivel la consulta falla entera. El precio es que todos los usuarios identificados ven todos los juegos, también los que están en curso.

En `games` no hay `.write`: ningún usuario puede escribir directamente sobre `/games` (por ejemplo, para borrar todos los juegos de golpe). La raíz lo mantiene en falso.

### 3.3 `.indexOn`

```json
".indexOn": ["status"],
```

Significa: Firebase mantiene en el servidor un índice sobre el campo `status` de los hijos de `/games`. Permite: que la consulta `orderByChild("status").equalTo("waiting")` que usa el lobby se resuelva de forma eficiente en el servidor. Impide: nada; no es una regla de seguridad, es de rendimiento. Según la documentación, sin índice la consulta "degrada su rendimiento" a medida que crecen los datos, y los índices no son obligatorios en desarrollo con las bibliotecas de cliente, pero se recomienda declararlos antes de lanzar. Va en `/games` porque es el nivel donde se hace la consulta.

### 3.4 `$gameId` y su `.write`

```json
"$gameId": {
  ".write": "auth != null && ((!data.exists() && newData.child('host/uid').val() === auth.uid && newData.child('status').val() === 'waiting' && !newData.child('guest').exists()) || (data.exists() && (data.child('host/uid').val() === auth.uid || data.child('guest/uid').val() === auth.uid)) || (data.child('status').val() === 'waiting' && !data.child('guest').exists() && newData.child('guest/uid').val() === auth.uid) || (!data.exists() && !newData.exists()))",
```

Aquí `data` y `newData` son el juego completo (`/games/{gameId}`) antes y después de la escritura. La expresión tiene la forma `auth != null && (A || B || C || D)`: hay que tener sesión y cumplir al menos una de cuatro cláusulas. Como `.write` se evalúa en este nivel, una escritura sobre un hijo (por ejemplo solo `guest`) también se juzga aquí. La cascada hace que, una vez concedido el permiso, los hijos no puedan retirarlo; lo que sí pueden es rechazar los datos con `.validate`.

**Cláusula A: crear un juego.**

```
!data.exists() && newData.child('host/uid').val() === auth.uid && newData.child('status').val() === 'waiting' && !newData.child('guest').exists()
```

Significa: el juego no existe todavía, quien escribe se declara a sí mismo como `host`, el estado inicial es `waiting` y no trae invitado. Permite: la app crea un juego con su propio uid como creador, en espera y sin invitado. Impide: crear un juego a nombre de otra persona (otro `host/uid`), o publicar directamente un juego `playing` con un invitado inventado, que se saltaría la transición de unirse. Hace falta porque, sin ella, nadie podría crear juegos (la raíz niega todo).

**Cláusula B: modificar o borrar un juego del que se es participante.**

```
data.exists() && (data.child('host/uid').val() === auth.uid || data.child('guest/uid').val() === auth.uid)
```

Significa: el juego ya existe y el uid de quien escribe es el del creador o el del invitado, según lo que había antes de la escritura. Permite: jugar (escribir el tablero), terminar la partida, cancelar el juego o salir (borrarlo). Impide: que un tercero identificado modifique o borre una partida ajena que ya tiene dos jugadores. Se mira `data` (el estado previo) y no `newData` a propósito: la pertenencia se decide por lo que ya estaba guardado, no por lo que quien escribe afirme.

**Cláusula C: unirse a un juego en espera.**

```
data.child('status').val() === 'waiting' && !data.child('guest').exists() && newData.child('guest/uid').val() === auth.uid
```

Significa: el juego estaba en `waiting`, aún no tenía invitado, y quien escribe queda como `guest` con su propio uid. Permite: que un usuario identificado que no participa se una a un juego en espera. Impide: unirse a un juego que ya tiene invitado (la cláusula exige que no lo tenga), unirse a nombre de otro uid, o unirse a un juego que ya está en `playing`. Hace falta porque quien se une todavía no es participante, así que la cláusula B no le sirve; esta es la única puerta de entrada de un no participante a un juego existente. Nótese que la cláusula no obliga a que `status` pase a `playing` ni limita qué otros campos se tocan en la misma escritura; eso queda a la validación de cada campo (3.5 a 3.10) y al cliente.

**Cláusula D: escribir "nada" sobre un juego que no existe.**

```
!data.exists() && !newData.exists()
```

Significa: se permite escribir un valor nulo (equivale a borrar) en un nodo que no existe. No cambia ningún dato. Permite: que un usuario identificado ejecute un borrado sobre un juego ya inexistente. Impide: nada relevante; es inocua a propósito. El motivo es técnico. Según la documentación de Firebase, cuando se registra una operación `onDisconnect`, el servidor comprueba las reglas para ver que el usuario puede hacer esa escritura y las comprueba una segunda vez cuando la conexión se pierde y va a ejecutarla. La app registra `onDisconnect().removeValue()` sobre el juego antes de crearlo (así una caída entre ambos pasos no deja un juego huérfano). En ese momento el nodo no existe y la operación es un borrado de nulo sobre nulo: sin esta cláusula, A, B y C la rechazan y no se podría registrar. La misma cláusula cubre el caso de que, al dispararse, el juego ya haya sido borrado. D8 la añade también para una transacción de unirse que termina sin cambios sobre un nodo inexistente. Esa parte del comportamiento del SDK no está verificada (ver sección 7).

### 3.5 `.validate` del juego

```json
".validate": "newData.hasChildren(['host', 'status', 'board', 'turn', 'result'])",
```

Significa: todo juego que se guarde debe tener sí o sí esos cinco hijos. `guest` queda fuera de la lista porque el invitado no existe mientras se espera. `createdAt` queda fuera sin un motivo equivalente: las reglas simplemente no lo exigen, aunque la app siempre lo escribe; la consecuencia es que las reglas admitirían un juego sin `createdAt`, que la app trataría como inexistente al leerlo (`gameFromValue` devuelve nulo si falta o no es un número). Permite: la escritura completa que hace la app al crear. Impide: guardar un juego incompleto, por ejemplo sin tablero. Como `.validate` no se evalúa al borrar, esto no impide a un participante borrar el juego.

### 3.6 `host` y `guest`

```json
"host": {
  ".validate": "newData.hasChildren(['uid', 'name']) && (!data.exists() || newData.child('uid').val() === data.child('uid').val())",
  "uid": { ".validate": "newData.isString()" },
  "name": { ".validate": "newData.isString() && newData.val().length >= 1 && newData.val().length <= 20" },
  "$other": { ".validate": false }
},
```

`guest` tiene exactamente las mismas cuatro líneas, con `guest` en lugar de `host`. Se explican una vez, y valen para ambos.

- El objeto debe tener `uid` y `name`. Y si el jugador ya existía (`data.exists()`), el `uid` no puede cambiar. Permite: escribir un jugador nuevo con su uid y apodo, o reescribir el juego conservando los mismos uids. Impide: que alguien se haga pasar por el creador o por el invitado cambiando el uid de un jugador ya registrado. Sin esta cláusula, un participante podría reasignar el rol del otro y, con él, el permiso de la cláusula B.
- `uid` debe ser una cadena.
- `name` debe ser una cadena de entre 1 y 20 caracteres. Permite: "Ana". Impide: un apodo vacío, o de 5.000 caracteres. El recorte de espacios no lo hacen las reglas, lo hace el cliente.
- `$other` con `false` rechaza cualquier hijo distinto de `uid` y `name`. Es una lista cerrada: impide guardar campos arbitrarios bajo un jugador.

### 3.7 `status`

```json
"status": { ".validate": "newData.val() === 'waiting' || newData.val() === 'playing' || newData.val() === 'finished'" },
```

Significa: el estado solo puede ser uno de tres valores. Permite: `waiting` al crear, `playing` al unirse, `finished` al terminar. Impide: valores como `"abandoned"` o `"x"`. Hace falta porque la cláusula C y la consulta del lobby dependen del literal `'waiting'`.

### 3.8 `board`: solo cambia el tablero quien tiene el turno

```json
"board": { ".validate": "newData.isString() && newData.val().matches(/^[XO-]{9}$/) && (!data.exists() || newData.val() === data.val() || (data.parent().child('turn').val() === 'X' && data.parent().child('host/uid').val() === auth.uid) || (data.parent().child('turn').val() === 'O' && data.parent().child('guest/uid').val() === auth.uid))" },
```

La regla tiene dos partes unidas con `&&`.

*Forma.* El tablero es una cadena de exactamente nueve caracteres, cada uno `X`, `O` o `-` (casilla vacía). Permite `"X-O------"`. Impide `"XXXXXXXXXXX"` (once caracteres) o `"X-Z------"`.

*Quién puede cambiarlo.* Es la parte delicada. En esta regla, `data` es el valor previo del campo `board`, y `data.parent()` sube un nivel: es el juego completo tal como estaba antes de la escritura, de modo que se puede mirar `turn` y los uids anteriores. La condición se cumple si ocurre cualquiera de estas cuatro cosas:

1. `!data.exists()`: el tablero no existía todavía (se está creando el juego).
2. `newData.val() === data.val()`: el tablero no cambia. Esto es lo que permite escribir otros campos, o unirse, sin tocar el tablero.
3. El turno previo era `X` y quien escribe es el `host` previo (el creador juega con X).
4. El turno previo era `O` y quien escribe es el `guest` previo (el invitado juega con O).

Es decir: si el tablero cambia, quien lo cambia debe ser el dueño del turno, medido sobre el estado anterior a la jugada. Ejemplo permitido: `turn` vale `'X'`, el creador escribe un tablero con una `X` más. Ejemplo impedido: `turn` vale `'X'` y el invitado intenta escribir un tablero con una `O` más; la escritura se rechaza. Se usa el turno previo porque en la misma escritura el jugador también cambia `turn` a la otra marca; mirar el turno nuevo daría la respuesta contraria.

Lo que esta regla no hace: no comprueba que la jugada cambie exactamente una casilla ni que la casilla estuviera libre. Un jugador en su turno podría escribir un tablero totalmente distinto. Eso lo valida el cliente.

### 3.9 `turn` y `result`

```json
"turn": { ".validate": "newData.val() === 'X' || newData.val() === 'O'" },
"result": { ".validate": "newData.val() === 'none' || newData.val() === 'X' || newData.val() === 'O' || newData.val() === 'tie'" },
```

`turn` solo puede ser `X` u `O`. `result` solo puede ser `none` (en curso), `X` u `O` (ganó esa marca) o `tie` (empate). Permite los valores que escribe la app; impide, por ejemplo, `turn = 'Z'` o `result = 'draw'`. Hacen falta porque la regla del tablero (3.8) lee `turn` para decidir quién juega, y un valor extraño dejaría la partida sin dueño del turno.

### 3.10 `createdAt`

```json
"createdAt": { ".validate": "newData.isNumber()" },
```

Debe ser un número (la app escribe la marca de tiempo del servidor, con la que el lobby ordena la lista). Impide, por ejemplo, un texto. Las reglas no comprueban que sea una fecha razonable.

### 3.11 `$other` bajo el juego

```json
"$other": { ".validate": false }
```

Cualquier hijo del juego que no sea `host`, `guest`, `status`, `board`, `turn`, `result` o `createdAt` se rechaza. Es una lista cerrada de campos, igual que `$other` en `host` y `guest`. Impide que alguien use el nodo como almacén de datos arbitrarios.

## 4. Quién puede hacer qué

"Participante" significa creador (`host`) o invitado (`guest`) de ese juego concreto. Las filas "creó el juego" y "se unió" se refieren a su propia partida. Las celdas salen de las reglas citadas arriba, no de pruebas ejecutadas.

| Acción | Sin identificar | Identificado, no participa | Quien creó el juego | Quien se unió |
|---|---|---|---|---|
| Leer la lista de juegos | No | Sí | Sí | Sí |
| Crear un juego | No | Sí (cláusula A) | Sí (cláusula A, juego nuevo) | Sí (cláusula A, juego nuevo) |
| Unirse a un juego en espera | No | Sí (cláusula C) | No aplica: ya es participante | No aplica: ya es participante |
| Unirse a un juego que ya tiene dos jugadores | No | No (C exige que no haya invitado) | No aplica | No aplica |
| Jugar en su turno | No | No | Sí, si `turn` es `X` (cláusula B y regla de `board`) | Sí, si `turn` es `O` (cláusula B y regla de `board`) |
| Jugar fuera de turno | No | No | No: la regla de `board` rechaza el cambio de tablero | No: la regla de `board` rechaza el cambio de tablero |
| Borrar el juego | No | No (salvo si no existe: cláusula D) | Sí (cláusula B) | Sí (cláusula B) |

Dos matices. Primero, "fuera de turno" solo bloquea cambiar el tablero: un participante sigue pudiendo escribir otros campos del juego (`turn`, `status`, `result`) porque la cláusula B no restringe campos. Segundo, la fila "unirse a un juego en espera" para un participante no tiene sentido: el creador técnicamente puede escribir un `guest` en su propio juego por la cláusula B, pero las reglas no lo distinguen de otra modificación.

## 5. Qué operación de la app usa qué regla

Lo que sigue sale del código de `FirebaseOnlineGameService.kt` y de `FirebaseOnlineGameMapping.kt`.

- **Crear.** La app obtiene una clave con `push()`, registra `onDisconnect().removeValue()` sobre ese nodo todavía inexistente (cláusula D) y, cuando el servidor lo confirma, escribe el juego completo con `setValue` (cláusula A y las validaciones de forma). Al dispararse la desconexión, el creador ya es participante (cláusula B).
- **Listar.** Consulta `/games` con `orderByChild("status").equalTo("waiting")`: usa el `.read` de `games` y el `.indexOn` sobre `status`. Para ver su propia partida, la app escucha `/games/{gameId}`, que también cubre ese `.read`.
- **Unirse.** Una transacción sobre el juego que escribe `guest` y `status = playing` (cláusula C y validaciones de `guest` y `status`). Si la transacción termina sin cambios sobre un nodo que ya no existe, es una escritura de nulo sobre nulo (cláusula D). Luego el invitado registra su propio `onDisconnect().removeValue()`, ya como participante (cláusula B).
- **Jugar.** Una transacción que reescribe el juego completo con el nuevo `board`, `turn`, `result` y, si termina, `status = finished`. Se autoriza por la cláusula B, y la regla de `board` exige que quien lo hace sea el dueño del turno previo.
- **Cancelar o salir.** `removeValue()` sobre el juego (cláusula B si existe; cláusula D si el otro jugador ya lo borró), seguido de `onDisconnect().cancel()`. Que cancelar una operación `onDisconnect` pase por las reglas no está en la documentación consultada; ver sección 7.
- **Borrado automático al perder la conexión.** Es el `onDisconnect().removeValue()` ya registrado; el servidor lo ejecuta tras volver a comprobar las reglas, y el participante cumple la cláusula B.

## 6. Lo que las reglas no comprueban

Según D8 del diseño, las reglas validan la autenticación, quién escribe o borra un juego existente, la transición de unirse, quién cambia el tablero y la forma y el rango de los campos. No comprueban:

- que una jugada cambie exactamente una casilla libre;
- que `turn`, `result` y `status` sean coherentes con el tablero (por ejemplo, que `result` sea `X` solo si `X` completó una línea);
- que el apodo esté recortado de espacios;
- que un participante no reescriba `status`, `turn` o `result` de su propia partida;
- (observado al leer las reglas, no está listado en D8) que al unirse el estado pase a `playing`, ni que la creación arranque con tablero vacío, `turn = 'X'` y `result = 'none'`: la cláusula A solo fija `host`, `status` y la ausencia de `guest`.

La consecuencia, aceptada en el diseño porque no hay servidor, es doble. Un participante malintencionado podría falsear su propia partida (declararse ganador, forzar el turno, escribir un tablero imposible), porque el cliente es quien aplica las reglas del juego. Y cualquier usuario identificado puede leer todos los juegos, incluidos los que están en curso. Un tercero, en cambio, no puede modificar una partida ajena que ya tiene dos jugadores.

## 7. Estado de verificación

Estas reglas están escritas y publicadas en la consola, pero aún no se han probado en el simulador de reglas (Rules Playground) ni contra la app. Todo lo explicado arriba sale de la lectura de las reglas, de la documentación oficial y del código, no de ejecuciones. Se van a ejercitar en una prueba con dos dispositivos que recorra crear, listar, unirse, jugar, cancelar, salir y cerrar la app.

Puntos que esa prueba debe confirmar expresamente, por no estar respaldados por la documentación consultada:

- que registrar `onDisconnect().removeValue()` sobre un nodo inexistente pasa por la cláusula D y es aceptado;
- que una transacción de unirse que termina sin cambios sobre un nodo inexistente es aceptada;
- que `onDisconnect().cancel()` no es rechazado tras borrar el juego;
- que `createdAt` escrito con la marca de tiempo del servidor pasa `newData.isNumber()`.

Si se cambia `database.rules.json` hay que publicar de nuevo su contenido en la consola de Firebase (pestaña Reglas) y actualizar este documento; el archivo del repositorio no se sincroniza solo con la consola.

## 8. Fuentes consultadas

Consultadas durante la redacción:

- https://firebase.google.com/docs/database/security/core-syntax : cascada de `.read` y `.write` (los hijos no pueden revocar), `.validate` no cascada y todas deben cumplirse, las reglas no son filtros, variables `$`.
- https://firebase.google.com/docs/database/security : tipos de regla (`.read`, `.write`, `.validate`, `.indexOn`), `.validate` solo sobre valores no nulos y por tanto ignorado al borrar.
- https://firebase.google.com/docs/database/security/rules-conditions : `data` (antes de la escritura), `newData` (después), `auth` (nulo antes de autenticarse), `parent()`, `exists()`, `hasChildren()`, `matches()`.
- https://firebase.google.com/docs/database/security/indexing-data : qué hace `.indexOn`, que se declara en el nivel de la consulta y que no es obligatorio en desarrollo.
- https://firebase.google.com/docs/database/android/offline-capabilities : el servidor comprueba las reglas al registrar `onDisconnect` y otra vez al ejecutarlo.

Documentos del propio repositorio: `openspec/changes/add-firebase-online-multiplayer/design.md` (D7 y D8), `FirebaseOnlineGameService.kt` y `FirebaseOnlineGameMapping.kt`.

No verificado con la documentación: el efecto exacto de `.validate` sobre campos que no se tocan en una escritura parcial, cómo se resuelve `ServerValue.TIMESTAMP` dentro de las reglas, y si `onDisconnect().cancel()` pasa por las reglas. Lo que dependa de ello está marcado en la sección 7.
