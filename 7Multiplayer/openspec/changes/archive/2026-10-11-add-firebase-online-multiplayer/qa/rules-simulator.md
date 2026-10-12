# Reglas de la base de datos (tarea 7.11)

**El simulador de reglas de la consola de Firebase NO se usó.** El 2026-10-11 el usuario decidió sustituirlo por la evidencia automática de `FirebaseOnlineGameServiceLiveTest`, que ejercita las reglas publicadas contra el proyecto real. Esta sustitución no equivale a haber ejecutado el simulador.

## Pasadas

- Antes de la corrección (commit `86be80a`, emulador de referencia, API 35, tras `pm clear`): 17 pruebas, 1 fallo, `rulesLetAThirdIdentityReadButNotWriteOrDeleteAGameInProgress` con `NoSuchElementException` en `awaitGame` (`instrumented/FirebaseOnlineGameServiceLiveTest.rules-rerun-0-before-fix.txt`). Causa: la identidad `live-third` observaba el juego sin haber iniciado sesión nunca (`observeGame` no inicia sesión; al ser denegada la lectura emite nulo y cierra el flujo). Se reprodujo en la primera pasada tras borrar los datos de la app, y en pasadas anteriores pasaba solo porque la sesión anónima quedaba guardada.
- Corrección: toda identidad inicia sesión al crearse (`setUp` y `secondaryIdentity`). Se añadieron 4 métodos y se eliminó el combinado `rulesRejectCreatingAGameDirectlyInPlayingWithAGuest`: 20 pruebas.
- Tres pasadas seguidas de la clase completa, la primera justo después de `pm clear`: `OK (20 tests)` las tres (`instrumented/FirebaseOnlineGameServiceLiveTest.rules-rerun-1.txt`, `-2.txt`, `-3.txt`).

Todos los rechazos se comprueban con el motivo `Permission denied` (no solo que algo falló) y las esperas agotadas se distinguen de los rechazos.

| # | Caso de 7.11 | Resultado esperado | Método que lo cubre | Resultado obtenido | Cobertura |
|---|---|---|---|---|---|
| 1 | Usuario que no participa no puede escribir un juego existente | Rechazado | `rulesLetAThirdIdentityReadButNotWriteOrDeleteAGameInProgress` (lectura permitida; escritura de `board` y borrado directos de un tercero rechazados); `rulesRejectAThirdIdentityWritingItselfAsGuestOfAGameInProgress` (escritura directa como invitado) | Pasan en las 3 pasadas | Cubre |
| 2 | Participante fuera de su turno no puede cambiar `board` | Rechazado | `rulesRejectADirectBoardWriteFromThePlayerWithoutTheTurn` (y `moveOutOfTurnIsRejectedAndTheBoardDoesNotChange` por el servicio) | Pasan en las 3 pasadas | Cubre |
| 3 | Crear un juego directamente en `playing` o con invitado se rechaza | Rechazado | `rulesRejectCreatingAGameDirectlyInPlayingWithoutAGuest` (`playing` sin invitado) y `rulesRejectCreatingAWaitingGameThatAlreadyHasAGuest` (`waiting` con invitado) | Pasan en las 3 pasadas | Cubre, cada condición por separado |
| 4 | Unirse a un `waiting` sin invitado se permite y un segundo invitado se rechaza | Primero permitido, segundo rechazado | `guestJoinsAndBothSeeTheGameInProgress` y `guestJoinsWithoutHavingTheGameCached` (permitido); `rulesRejectAThirdIdentityWritingItselfAsGuestOfAGameInProgress` (segundo invitado secuencial, rechazado); `onlyOneOfTwoSimultaneousGuestsJoins` (carrera) | Pasan en las 3 pasadas | Cubre |
| 5 | Escribir nulo sobre un nodo inexistente se permite | Permitido | `rulesAcceptDeletingAGameNodeThatDoesNotExist` | Pasa en las 3 pasadas | Cubre |
| 6 | Apodo de 21 caracteres se rechaza | Rechazado (y 20 aceptado) | `rulesRejectANicknameOf21Characters` y `rulesAcceptANicknameOf20Characters` | Pasan en las 3 pasadas | Cubre |
| 7 | Lectura de `/games` sin autenticar se rechaza | Rechazado | `rulesRejectReadingGamesWithoutASession` | Pasa en las 3 pasadas | Cubre |

Limitación: en el caso 4 secuencial, el rechazo del segundo invitado puede deberse a la cláusula de escritura o a la validación de `guest` (que impide cambiar el `uid` existente); ambas producen `Permission denied` y el invitado original se conserva en cualquier caso.

## Conclusión

Los siete casos tienen cobertura directa y pasan. Ninguna regla publicada se comportó de modo distinto a `firebase/RULES.md`. No se modificó `firebase/database.rules.json`.
