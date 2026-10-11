# Resultados de cancelación, carrera y creación (tarea 8.5)

Destinos: "instancia A (dispositivo físico, API 35)" e "instancia B (emulador, API 35)". Bitácora con marcas de hora: `log.txt`. Veredictos: pasa, falla, bloqueado, no ejecutado.

Los tiempos de aparición se midieron comparando capturas consecutivas de la instancia observadora (resolución ~0.1 s en B y ~0.4 s en A) y confirmando el texto con `uiautomator`.

| ID | Escenario (especificación) | Veredicto | Evidencia / medición |
|---|---|---|---|
| L01 | Cancelar con el botón (online-lobby: Cancelar un juego en espera) | pasa | `L01_A_espera_antes_de_cancelar_instancia-A.png`, `L01_B_lista_con_juego_de_Ana_instancia-B.png`, `L01_tras_cancelar_instancia-A.png`, `L01_tras_cancelar_instancia-B.png`: sin confirmación, A vuelve al lobby y «Juego de Ana» desaparece de la lista de B (primer cambio visual en B a 2.9 s en esta primera medición, que incluía ~2 s de volcado previo). |
| L02 | Cancelar con Back | pasa | `L02_tras_cancelar_con_back_instancia-A.png`, `L02_tras_cancelar_con_back_instancia-B.png`: A queda en el lobby (no en la pantalla inicial); B ve desaparecer el juego 0.43 s después del Back. |
| L03 | El creador cierra la app mientras espera (`am force-stop`) | pasa | `L03_B_lista_tras_cierre_del_creador_instancia-B.png`: «Juego de Ana» desaparece de B sin recargar, 0.38 s después del cierre. |
| L04 | El juego en espera se elimina externamente (consola) | no ejecutado | Borrar un nodo desde la consola de Firebase no es posible para esta agente y la consigna prohíbe cualquier otra vía de borrado. Lo cubre en parte `FirebaseOnlineGameServiceLiveTest` (8.2). |
| L05 | Dos elecciones casi simultáneas del mismo juego (5 repeticiones) | **bloqueado** | Requiere tres identidades a la vez (creador y dos jugadores que eligen). Solo hay dos destinos (un teléfono y un emulador). No se arrancó una segunda instancia de emulador ni se crearon usuarios adicionales porque no estaba autorizado. Se anota 0 de 5 repeticiones ejecutadas. Ver «Cobertura automática de L05 y L07 (tarea 9.4)» al final. |
| L06 | Elegir un juego recién cancelado | pasa | Carrera cancelar (A) contra elegir (B), 29 intentos en tres series con desfases de -0.1 a 0.7 s entre los toques (`log.txt`: L06, L06b, L06c). Con un desfase de 0.12 a 0.22 s, 6 intentos dejaron a B en el lobby con «Este juego ya no está disponible» (`L06c_1_mensaje_instancia-B.png`, `L06c_2_…`, `L06c_3_…`, `L06c_4_…`, `L06c_8_…`, `L06c_9_…`). En 9 intentos elegir ganó la carrera (ver L06-obs) y en 14 B quedó en el lobby sin mensaje (el elemento ya había desaparecido al llegar el toque, o la lectura se hizo cuando el aviso ya había expirado). En ningún intento B quedó unido a una partida con el creador dentro. |
| L06-obs | Cuando elegir gana por poco a cancelar | observación | En 9 de 29 intentos B entró a una partida y de inmediato vio «Tu rival salió de la partida» con «Volver al lobby», mientras A estaba en el lobby. Es coherente con «el rival salió», pero quien se unió nunca llega a jugar. No contradice la especificación. |
| L07 | Un tercer jugador no entra a una partida en curso | **bloqueado** | Requiere un tercer destino. Ver «Cobertura automática de L05 y L07 (tarea 9.4)» al final. |
| L08 | Crear un juego con la red cortada → «No se pudo crear el juego» | no ejecutado | No se pudo provocar. Con la red cortada el lobby deshabilita «Crear juego» y muestra «Sin conexión» antes de que se pueda pulsar (`L08h_B_sin_conexion_acciones_instancia-B.png`: botón atenuado, el toque no hace nada). Con el toque y el corte en el mismo comando (6 intentos, `L08f_1_crear_con_red_cortada_instancia-B.png`, `L08g_1_crear_y_cortar_instancia-B.png`) la pantalla acabó en «Sin conexión», sin «Esperando rival…» ni «No se pudo crear el juego»; tras restablecer la red no quedó juego en la lista. No se puede afirmar ni negar el escenario. |
| L09 | Sin conexión en el lobby (online-lobby: Conexión con el servicio) | pasa en el primer corte de cada proceso; **falla** en los siguientes | `L08c_B_sin_conexion_instancia-B.png` («Sin conexión» a ~2 s, recuperación automática a ~2 s de restablecer, `L08c_B_tras_recuperar_instancia-B.png`). En cortes repetidos dentro del mismo proceso no aparece «Sin conexión» (75 s de espera en tres ciclos y más de 5 minutos en otra prueba; `L08d_B_sin_conexion_estable_instancia-B.png`, `L08d_B_tras_tocar_offline_instancia-B.png`): ver D-3 en `../report.md`. |
| L10 | Lista de B vacía tras reconectar con un juego esperando | observación | Tras un corte con un toque en «Juego de Ana» justo antes de que la app notara la pérdida, y restablecer la red, B mostró «No hay juegos disponibles» durante más de un minuto mientras A seguía en «Esperando rival…» (`L08b_B_lista_vacia_con_A_esperando_instancia-B.png`, `L08b_A_sigue_esperando_instancia-A.png`); al reabrir la app en B, el juego reapareció. No se pudo reproducir de forma limpia; se reporta como observación O-1. |

## Resumen

- Pasan: L01, L02, L03, L06 y la recuperación automática del primer corte (L09 parcial).
- Bloqueados: L05 (carrera, 0/5 repeticiones) y L07 (tercer jugador): faltan identidades.
- No ejecutados: L04 (borrado desde la consola) y L08 (fallo al crear).
- Falla: L09 en cortes sucesivos.
- Tarea 8.5: no se marca.

## Cobertura automática de L05 y L07 (tarea 9.4)

**Por qué no se ejecutaron con dos instancias.** Ambos escenarios necesitan tres identidades a la vez (el creador y dos jugadores que eligen el mismo juego, o la partida en curso y un tercero). Solo había dos destinos, así que L05 y L07 quedaron bloqueados en la primera pasada.

**Decisión del usuario (2026-10-11).** La carrera y el tercer jugador se dan por cubiertos con una prueba automática contra el Firebase real, con la carrera repetida 5 veces, en lugar de con tres dispositivos.

**Métodos que los cubren** (clase `com.edu.unal.tictactoe.FirebaseOnlineGameServiceLiveTest`, identidades anónimas independientes mediante instancias secundarias de `FirebaseApp`, reglas publicadas):

- L05: `theJoinRaceRepeated5TimesAlwaysHasExactlyOneWinner`. Antes de las repeticiones, una comprobación previa hace que cada identidad («invitado A» e «invitado B») se una sola, por el servicio, a un juego propio y obtenga `Joined` (falla diciendo cuál no pudo). En cada repetición crea un juego nuevo y las dos identidades llaman a `joinGame` a la vez, alternando el orden de lanzamiento (impares: primero A; pares: primero B); exige exactamente un `Joined` y un `NoLongerAvailable`, y que el juego quede en curso con el ganador como invitado. No se exige un ganador concreto. Cada fallo nombra la repetición. Se conserva sin cambios `onlyOneOfTwoSimultaneousGuestsJoins` (una repetición).
- L07: `aThirdIdentityCannotJoinAGameInProgressThroughTheService` (camino del servicio: el tercero obtiene `NoLongerAvailable`; el juego conserva su invitado original, su estado `Playing`, el tablero vacío y el turno de X) y, ya existente, `rulesRejectAThirdIdentityWritingItselfAsGuestOfAGameInProgress` (escritura directa rechazada por las reglas).

**Resultado de cada repetición de la carrera** (línea `INSTRUMENTATION_STATUS: live-race-round-N` de la salida):

| Pasada | Repetición | Se lanzó primero | Ganó (`Joined`) | Otro |
|---|---|---|---|---|
| 1 | 1 | invitado A | invitado B | `NoLongerAvailable` |
| 1 | 2 | invitado B | invitado B | `NoLongerAvailable` |
| 1 | 3 | invitado A | invitado B | `NoLongerAvailable` |
| 1 | 4 | invitado B | invitado B | `NoLongerAvailable` |
| 1 | 5 | invitado A | invitado B | `NoLongerAvailable` |
| 2 | 1 | invitado A | invitado A | `NoLongerAvailable` |
| 2 | 2 | invitado B | invitado A | `NoLongerAvailable` |
| 2 | 3 | invitado A | invitado A | `NoLongerAvailable` |
| 2 | 4 | invitado B | invitado A | `NoLongerAvailable` |
| 2 | 5 | invitado A | invitado A | `NoLongerAvailable` |

En las 10 repeticiones el juego quedó en curso con el ganador como invitado y hubo exactamente un `Joined` y un `NoLongerAvailable`. Ambas identidades ganaron alguna vez (B en la pasada 1, A en la pasada 2), y la comprobación previa confirma que cada una puede unirse sola, así que perder la carrera no se debe a una incapacidad de unirse. Observación, sin maquillar: dentro de cada pasada el ganador fue siempre el mismo (B en la 1, A en la 2) aunque se alternó quién se lanzó primero; el orden de lanzamiento no decide, y lo que sí cambia entre pasadas (no se investigó la causa; podría ser la latencia de cada identidad en ese proceso) hace que cada pasada vea un solo ganador. El resultado exigido (exactamente un ganador por juego) se cumple en las 10. El tercer jugador obtuvo `NoLongerAvailable` en ambas pasadas (`live-third-player`).

**Ejecución.** Emulador de referencia, API 35, clase completa dos veces seguidas: ambas terminan en `OK (22 tests)` (20 previos más los 2 nuevos). Salidas guardadas, sin identificadores ni valores de configuración:

- `carrera-automatizada-pasada-1.txt`
- `carrera-automatizada-pasada-2.txt`

La clase borra sus juegos al terminar y comprueba que no queda ninguno de sus identidades.
