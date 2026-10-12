# Lista de comprobación de los puntos del reto (tarea 8.4)

Destinos: "instancia A (dispositivo físico, API 35)" e "instancia B (emulador, API 35)". Apodos de prueba: «Ana» (instancia A) y «Luis» (instancia B). Bitácora con marcas de hora: `log.txt` (esta carpeta). Veredictos: pasa, falla, no observado (sin veredicto).

Las capturas del teléfono están recortadas a la zona de la aplicación. Los nombres terminan en `_instancia-A.png` o `_instancia-B.png`.

## game-mode-selection

| Requisito / escenario | Veredicto | Evidencia y observaciones |
|---|---|---|
| Pantalla inicial con dos entradas / Arranque de la aplicación | pasa | `R01_pantalla_inicial_instancia-B.png` (tras `pm clear`): exactamente «Jugar contra la computadora» y «Jugar en línea», sin tablero. En la instancia A se vio igual al arrancar. |
| Pantalla inicial / Elegir la partida local | pasa | La instancia B muestra el tablero vacío con el marcador vigente («Victorias de X: 0 / de O: 0 / Empates: 0») al elegirla (`log.txt`, y `../riesgos/Re_1_marcador_B_antes_instancia-B.png` para el marcador). |
| Pantalla inicial / Elegir el modo en línea | pasa | `R02_dialogo_apodo_primera_vez_instancia-B.png` (primera vez) y `R04_lobby_apodo_20_guardado_instancia-B.png` (lobby). |

## online-lobby

| Requisito / escenario | Veredicto | Evidencia y observaciones |
|---|---|---|
| Apodo al entrar por primera vez / Primera vez sin apodo | pasa | `R02_dialogo_apodo_primera_vez_instancia-B.png`: diálogo «Tu apodo», campo «Apodo» vacío, «Cancelar» y «Aceptar», sin lista detrás. Solo en la instancia B: en la instancia A ya había un apodo guardado y no se puede borrar `pm clear` en el teléfono. |
| Apodo / Con apodo guardado no se pregunta | pasa | `R05_reabrir_lobby_sin_dialogo_instancia-B.png` (B tras `force-stop` y reabrir); en A también (entró directo al lobby). |
| Apodo / Descartar el diálogo de la primera vez | pasa | `R07_cancelar_primera_vez_vuelve_inicio_instancia-B.png` y `R07_back_primera_vez_vuelve_inicio_instancia-B.png`; al volver a elegir «Jugar en línea» el diálogo reaparece (no se guardó apodo). En el emulador un solo Back cierra el diálogo (no hay teclado). |
| Validación del apodo / vacío o solo espacios | pasa | `R03_apodo_vacio_error_instancia-B.png`, `R03_apodo_espacios_error_instancia-B.png`, `R03_apodo_espacios_error_instancia-A.png`: «Escribe un apodo de 1 a 20 caracteres», el diálogo sigue abierto. |
| Validación / demasiado largo (21) | pasa | `R03_apodo_21_error_instancia-A.png`, `R03_apodo_21_error_instancia-B.png`. |
| Validación / válido con espacios sobrantes | pasa | B: «  Luis  » se guarda como «Luis» (el diálogo de «Cambiar apodo» muestra `Luis`, `R06_apodo_guardado_recortado_Luis_instancia-B.png`); A: «  Ana  » abre el lobby. |
| Validación / 20 caracteres | pasa | B: 20 dígitos aceptados; el campo de «Cambiar apodo» devuelve los 20 (`R03_apodo_20_antes_de_aceptar_instancia-B.png`, `R04_lobby_apodo_20_guardado_instancia-B.png`, `R06_cambiar_apodo_dialogo_con_valor_instancia-B.png`). |
| Apodo se recuerda y se puede cambiar / recordado tras reabrir | pasa | `R05_reabrir_pantalla_inicial_instancia-B.png`, `R05_reabrir_lobby_sin_dialogo_instancia-B.png`. |
| Apodo / Cambiar el apodo | pasa | De 20 dígitos a «Luis»; el juego creado después se lista en A como «Juego de Luis» (`log.txt`, partida de empate). Cambio de A de su apodo anterior a «Ana»: `R06_cambiar_apodo_dialogo_con_valor_instancia-A.png`. |
| Apodo / Cancelar el cambio | pasa | «Cancelar» y Back dejan el apodo vigente y el lobby (`log.txt`, B). |
| Conexión con el servicio / Estado de carga («Conectando…») | no observado | La transición dura menos de ~0.1 s con red (cuadros de captura consecutivos cada 0.1 s sin verlo). Lo cubre `OnlineLobbyScreenTest` (8.2), no esta tanda. |
| Conexión / Fallo de identidad y reintento | pasa (parcial) | `R19_fallo_de_identidad_instancia-B.png`: sin red y sin identidad previa (`pm clear` + red cortada) aparece «No se pudo conectar con el servicio» con «Reintentar» en menos de 2 s; con la red restablecida, «Reintentar» llega al lobby en < 0.2 s. Con la red aún cortada, «Reintentar» deja la misma pantalla y no se vio «Conectando…» (demasiado breve). |
| Conexión / Pérdida de conexión en el lobby y Recuperación automática | **falla (condicional)** | Con un proceso nuevo, el primer corte da «Sin conexión» en ~2 s, «Crear juego» se ve deshabilitado (`../lobby/L08h_B_sin_conexion_acciones_instancia-B.png`) y vuelve solo al restablecer (`../lobby/L08c_*`). Los cortes siguientes en el mismo proceso no se reflejan (ver D-3 en `../report.md`). |
| Lista de juegos en tiempo real / Lista con juegos (dos juegos) | no ejecutado | Mostrar «Juego de Ana» y «Juego de Luis» a la vez exige un tercer destino (cada creador queda en la pantalla de espera). |
| Lista / Lista vacía | pasa | `R08_lobbies_vacios_instancia-A.png`, `R08_lobbies_vacios_instancia-B.png`. |
| Lista / Aparece un juego nuevo sin recargar | pasa | `R09_A_espera_B_lista_instancia-B.png`: B (en el lobby) pasa de «No hay juegos disponibles» a «Juego de Ana» sin acción. |
| Lista / Un juego tomado sale de la lista | no ejecutado | Exige un tercer destino con el lobby abierto; lo cubre `FirebaseOnlineGameServiceLiveTest` (8.2). |
| Crear un juego y esperar rival / Crear un juego | pasa | `R09_A_espera_B_lista_instancia-A.png`: «Esperando rival…» y «Cancelar», sin tablero; en B aparece «Juego de Ana». |
| Crear / Fallo al crear el juego | no ejecutado | Ver `../lobby/resultados.md` (no se pudo provocar). |
| Crear / Un rival se une | pasa | `R10_partida_inicio_instancia-A.png`, `R10_partida_inicio_instancia-B.png`: ambos pasan a la partida. |
| Unirse a un juego disponible / Unirse con éxito | pasa | Idem; el juego deja de estar en la lista (B vuelve a ver la lista vacía tras la partida). |
| Solo se juega después de unirse / Sin tablero en el lobby | pasa | `R08_lobbies_vacios_*`: ninguna casilla. |
| Solo se juega / Sin tablero mientras se espera rival | pasa | `R09_A_espera_B_lista_instancia-A.png`: solo «Esperando rival…» y «Cancelar». |
| Solo se juega / Tablero al completarse los dos jugadores | pasa | `R10_partida_inicio_*`: solo la instancia con el turno tiene casillas habilitadas (A: 9 habilitadas, B: 0). |

## online-match

| Requisito / escenario | Veredicto | Evidencia y observaciones |
|---|---|---|
| Dos jugadores, marcas y turno inicial / Marcas y turno inicial | pasa | A: «Juegas con X», «Rival: Luis», «Tu turno»; B: «Juegas con O», «Rival: Ana», «Turno de Ana»; tablero vacío en ambos (`R10_partida_inicio_*`). El creador lleva X y empieza, también cuando el creador es B. |
| Dos jugadores / Un tercer jugador no entra | bloqueado | Requiere un tercer destino. Ver `../lobby/resultados.md`. |
| Solo marca quien tiene el turno / Marca en su turno | pasa | `R12_jugada_X_sincronizada_*`. |
| Solo marca / Intento fuera de turno | pasa | `R11_fuera_de_turno_*`: las casillas de B (sin turno) no responden, el tablero no cambia en ningún dispositivo. Nota: el estado «deshabilitado» se leyó con `uiautomator` (`enabled=false` en las 9 casillas); la exposición a TalkBack es de 8.8. |
| Solo marca / Casilla ocupada | pasa | La casilla ocupada se ve deshabilitada y el toque no cambia nada (`log.txt`). |
| Solo marca / Segunda entrada mientras una jugada espera confirmación | pasa | Tres repeticiones con dos toques separados ~0.1 s: solo se acepta una jugada (`log.txt`, líneas «Segunda entrada»). |
| Sincronización / La jugada llega al rival y Textos de turno | pasa | `R12_jugada_X_sincronizada_*`: B pasa de «Turno de Ana» a «Tu turno» sin acción. Latencia de toque a pantalla del rival: 0.29 a 0.85 s en cuatro muestras (`log.txt`). |
| Resultado / Victoria | pasa | `R14_victoria_X_*`: A «Ganaste», B «Ganó Ana»; tablero bloqueado y con la posición final; no se aceptan toques posteriores. Ganar como O: `../riesgos/Re_2_ganar_resultado_instancia-B.png`. |
| Resultado / Empate | pasa | `R17_empate_*`: ambos «Empate». |
| Resultado / Resultado visible tras la salida del rival | pasa | `R15_A_vuelve_B_sigue_en_resultado_*` y 10 repeticiones en `../riesgos/` (B conserva «Ganó Ana»). |
| Fin de partida sin revancha / Volver al lobby | pasa | `R15_*`: no hay acción de revancha; solo «Volver al lobby». |
| Fin sin revancha / Cada jugador vuelve por su cuenta | pasa | `R15_A_vuelve_B_sigue_en_resultado_instancia-B.png`: B sigue en el resultado. |
| Fin sin revancha / Back con la partida terminada | pasa | `R15_B_back_sin_dialogo_instancia-B.png`: Back lleva al lobby sin diálogo. |
| El marcador local no interviene / Sin marcador ni menú de opciones | pasa | En todas las capturas de partida solo hay tablero, «Juegas con…», «Rival…», turno o resultado y «Salir»/«Volver al lobby»; sin marcador ni «Abrir menú». |
