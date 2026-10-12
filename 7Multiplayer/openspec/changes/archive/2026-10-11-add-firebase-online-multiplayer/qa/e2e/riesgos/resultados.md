# Resultados de riesgos y escenarios transversales (tarea 8.7)

Destinos: "instancia A (dispositivo físico, API 35)" e "instancia B (emulador, API 35)". Bitácora con marcas de hora: `log.txt`. Veredictos: pasa, falla.

## (a) Manejo de nulo en transacciones: pasa

- Unirse desde una instancia recién abierta: 7 repeticiones. En cada una la instancia B se cerró con `force-stop`, se abrió, se entró al lobby y se tocó sin pausa (toques ciegos cada 0.15 s sobre el primer elemento desde que se abre el lobby) mientras A esperaba rival. Resultado en las 7: B entra a la partida; nunca aparece «Este juego ya no está disponible» (`log.txt`, «R(a)»; `Ra_1_resultado_instancia-A.png`, `Ra_1_resultado_instancia-B.png`).
- Jugar desde una instancia recién abierta: tras cada una de esas 7 uniones, la primera jugada de A (X) y la respuesta de B (O) se aceptaron en el acto (14 de 14). Con A recién abierta (2 repeticiones, `force-stop` en el teléfono) también se aceptaron la primera jugada de A y la de B.
- Alcance: los toques ciegos llegan con la lista recién poblada, lo más cerca de «sin valor en caché» que permite adb; no se puede demostrar con certeza que el valor no estuviera en caché.

## (b) Fusión de la jugada final con la salida del ganador: 0 de 10

A completa la línea y pulsa «Volver al lobby» de inmediato (5 repeticiones con el primer toque 0.25 s después de la jugada y 5 con 20 toques seguidos sin espera). En las 10, la instancia B vio el resultado «Ganó Ana» y lo conservó (`Rb_1_B_ve_resultado_o_abandono_instancia-B.png`; detalle en `log.txt`, «R(b)»). El riesgo aceptado del diseño no se materializó: 0 de 10 mostraron «Tu rival salió de la partida» en lugar del resultado. Mide con limitaciones: el intervalo mínimo entre la jugada y la salida está acotado por la latencia de adb y de la pantalla de A (la salida solo es posible cuando A ya ve el botón).

## (c) Sin avance del modo local: pasa (el sonido no se pudo comprobar)

- Con el turno de O pendiente (apertura de O tras «Nueva partida», ~0.1 s consumido): lobby 6 s, volver. Primer cuadro a 0.39 s: tablero sin la jugada de O y «Turno de la computadora» (`Rc_2_al_volver_O_pendiente_sin_jugar_instancia-B.png`); O juega a 1.10 s del toque, ~0.7 s después (`Rc_3_O_juega_tras_el_tiempo_restante_instancia-B.png`). Una primera pasada (8 s en el lobby, lectura del tablero por volcado ~2 s después de volver) no era concluyente porque O ya había jugado al leer; la medición válida es la de los cuadros de captura.
- Con una partida en línea de ~25 s entre medias: al volver, primer cuadro a 0.17 s con tablero vacío y «Turno de la computadora» (`Rc_4_tras_partida_en_linea_tablero_local_vacio_instancia-B.png`); O juega a 0.87 s (`Rc_5_O_juega_tras_el_tiempo_restante_instancia-B.png`).
- La ausencia de sonido no se puede comprobar con adb; la verifica el usuario.

## (d) Muerte del proceso: pasa

`HOME`, `am kill`, relanzar con `am start` (el proceso es nuevo en los tres casos):

- Partida en línea visible: se llega al lobby, no a la partida (`Rd_partida_en_linea_no_se_restaura_lobby_instancia-B.png`).
- Lobby visible: se restaura el lobby (`Rd_lobby_restaurado_instancia-B.png`).
- Partida local visible: se restaura el tablero (`Rd_local_restaurada_instancia-B.png`).

## (e) El marcador local no cambia: pasa

Instancia B, marcador local antes: «X: 0, O: 1, Empates: 0» (`Re_1_marcador_B_antes_instancia-B.png`). Se jugaron tres partidas en línea: perder (`Re_2_perder_resultado_instancia-B.png`), ganar (`Re_2_ganar_resultado_instancia-B.png`) y empatar (`Re_2_empatar_resultado_instancia-B.png`). Después: «X: 0, O: 1, Empates: 0» (`Re_3_marcador_B_despues_instancia-B.png`) y lo mismo tras cerrar y reabrir la aplicación (`log.txt`). La instancia A tenía el marcador en cero (`Re_1_marcador_A_antes_instancia-A.png`) y no se reconsultó tras sus partidas; la comprobación se apoya en la instancia B.

## Resumen

(a) a (e) ejecutados, todos pasan. Tarea 8.7: marcada.
