# Tercera pasada: verificación de la corrección de D-4

- Fecha: 2026-10-11. Código corregido: `f337002` (la app pide `FLAG_KEEP_SCREEN_ON` a la ventana de la Activity). Se probó con el APK de `eebfaf5` en el dispositivo físico y con el de `4a470b1` en los emuladores (la app no cambió entre ambos).
- Bitácora con marcas de hora: `log.txt` (sin capturas: eran opcionales). Los apartados 1 y 2 de abajo se apoyan en medios distintos y se presentan por separado.
- Indicadores, anotados los dos en cada lectura:
  - **Energía**: `dumpsys power`, sección «Wake Locks»: una línea `SCREEN_BRIGHT_WAKE_LOCK 'WindowManager/displayId:0' … ws=WorkSource{… com.edu.unal.tictactoe}` (uid numérico omitido). Es el indicador que decide el requisito: es el bloqueo que el gestor de ventanas mantiene para mantener la pantalla encendida y está atribuido a la app.
  - **Ventana**: `dumpsys window windows`, la línea `fl=` del bloque `Window #N … MainActivity`, que debe contener `KEEP_SCREEN_ON` solo en «Esperando rival…» y en la partida en curso.

## Veredicto

- **D-4 queda corregido** en las dos instancias emuladas (34 de 34 lecturas con ambos indicadores correctos) y, según el indicador de energía, también en el dispositivo físico en los casos 1 a 3 que llegaron a ejecutarse (10 de 10 lecturas correctas). El control de que antes fallaba (segundo plano con espera o con partida y luego terminar) ya no deja el bloqueo retenido.
- Pendiente en el dispositivo físico: el caso 4 (segundo plano largo) y «Volver al lobby» del caso 3 no se ejecutaron porque el teléfono se desconectó. El caso 4 sí se comprobó en los dos emuladores.
- La columna «ventana» del dispositivo físico no coincidió con la de energía (ver el análisis de la discrepancia). No es un defecto de la app según lo que se pudo comprobar, pero queda sin explicar del todo.
- **Tarea 9.5: sí cumple ahora su «Verificar», con una reserva.** Cumple: lista de escenarios con resultado y mediciones (segunda y tercera pasada), mensaje exacto en el dispositivo que pierde la conexión en cada corte y segundo plano largo (segunda pasada) y ahora 4 de 4 más sin «Tu rival salió de la partida» en el afectado; D-1 y D-2 resueltos; D-4 corregido; pantalla encendida demostrada sin tocarla en «Esperando rival…» (12 min 15 s en el teléfono, segunda pasada); sin identificadores. Reserva: la espera real sin tocar la pantalla en la partida en curso no se hizo (solo el indicador, que es el requisito de «Esperando rival…» y de la partida en curso); si el texto de 9.5 exige la espera real en la partida, falta.

## 1. Dispositivo físico (instancia A, dispositivo físico, API 35)

Contra la instancia B (emulador de referencia). Solo se llegaron a ejecutar los casos 1 a 3 hasta el resultado; después el usuario desconectó el teléfono.

| Caso | Pantalla | Energía | Ventana | Esperado | Resultado por indicador de energía |
|---|---|---|---|---|---|
| 1 | pantalla inicial (app recién reiniciada) | no retiene | sin `KEEP_SCREEN_ON` | no | correcto |
| 1 | lobby | no retiene | sin | no | correcto |
| 1 | «Esperando rival…» | **retiene** | sin | sí | correcto (ventana discrepa) |
| 1 | lobby tras cancelar | no retiene | sin | no | correcto |
| 2 | «Esperando rival…» tras Inicio 4 s (el juego seguía en la lista de B) | **retiene** | sin | sí | correcto (ventana discrepa) |
| 2 | lobby tras cancelar | no retiene | sin | no | correcto |
| 2 | pantalla inicial | no retiene | sin | no | correcto |
| 3 | partida en curso | **retiene** | sin | sí | correcto (ventana discrepa) |
| 3 | partida en curso tras Inicio 3 s (tablero conservado) | **retiene** | sin | sí | correcto (ventana discrepa) |
| 3 | resultado (victoria) | no retiene | sin | no | correcto |

No ejecutado en el dispositivo físico: «Volver al lobby» del caso 3 y todo el caso 4 (segundo plano de 20 s, mensaje de conexión perdida, lobby y pantalla inicial). Contra esto: en la segunda pasada, antes de la corrección, esas mismas secuencias dejaban el bloqueo retenido en el resultado, el lobby y la pantalla inicial; ahora el resultado y las pantallas posteriores comprobadas lo liberan.

## 2. Dos emuladores (instancia A, emulador secundario, API 35; instancia B, emulador de referencia, API 35)

Identidades distintas comprobadas: tras `pm clear` y el apodo «Ana» en A, B ve «Juego de Ana» y se une (B conservaba su apodo «Luis»).

Las dos instancias dieron las mismas lecturas; cada una se probó como instancia probada con la otra de compañera. Cada fila vale para A y para B; en las dos, energía y ventana coincidieron con lo esperado.

| Caso | Pantalla | Energía | Ventana | Esperado |
|---|---|---|---|---|
| 1 | pantalla inicial (app recién reiniciada) | no retiene | sin `KEEP_SCREEN_ON` | no retenida |
| 1 | lobby | no retiene | sin | no retenida |
| 1 | «Esperando rival…» | retiene | `KEEP_SCREEN_ON` | retenida |
| 1 | lobby tras cancelar | no retiene | sin | no retenida |
| 2 | «Esperando rival…» tras Inicio 4 s (juego en la lista del otro) | retiene | `KEEP_SCREEN_ON` | retenida |
| 2 | lobby tras cancelar | no retiene | sin | no retenida |
| 2 | pantalla inicial | no retiene | sin | no retenida |
| 3 | partida en curso | retiene | `KEEP_SCREEN_ON` | retenida |
| 3 | partida en curso tras Inicio 3 s (tablero conservado) | retiene | `KEEP_SCREEN_ON` | retenida |
| 3 | resultado (victoria), instancia probada | no retiene | sin | no retenida |
| 3 | resultado (victoria), compañera | no retiene | sin | no retenida |
| 3 | lobby tras «Volver al lobby» (instancia probada y compañera) | no retiene | sin | no retenida |
| 4 | partida en curso | retiene | `KEEP_SCREEN_ON` | retenida |
| 4 | tras Inicio 20 s: «Se perdió la conexión. La partida terminó.» (la compañera: «Tu rival salió de la partida») | no retiene | sin | no retenida |
| 4 | lobby tras «Volver al lobby» | no retiene | sin | no retenida |
| 4 | pantalla inicial | no retiene | sin | no retenida |

Resumen: 17 lecturas por instancia probada (34 en total), 0 defectos. Regresiones observadas de paso: el juego sigue en la lista de la otra instancia tras 4 s fuera con «Esperando rival…», la partida se conserva tras 3 s fuera, y tras 20 s fuera quien salió ve exactamente el mensaje de conexión perdida y su rival «Tu rival salió de la partida».

## 3. Explicación de la discrepancia entre indicadores en el dispositivo físico

Lo observado: en el dispositivo físico, la línea `fl=` de la ventana nunca contenía `KEEP_SCREEN_ON` (ni en «Esperando rival…» ni en la partida), mientras el bloqueo de energía estaba retenido y se liberaba exactamente en las pantallas esperadas. En los emuladores los dos indicadores coinciden en todas las lecturas.

Análisis y evidencia:
- **No es un error del lector al elegir la línea de otra ventana**: en los emuladores el bloque `Window #N … MainActivity` es único y contiene una sola línea `fl=` (y una `pfl=`); se imprimieron todas las líneas `fl=`/`pfl=` del bloque durante «Esperando rival…» y era la misma. No hay ventana de arranque aparte de la Activity (una sola ventana del paquete).
- **No es un retraso de la ventana**: prueba de retardo en el emulador A (120 lecturas simultáneas de ambos indicadores en un mismo comando, cada ~0.03 s, tras tocar «Crear juego»): a 0.41 s la ventana ya trae `KEEP_SCREEN_ON` y a 0.44 s se adquiere el bloqueo de energía; la ventana se adelanta ~0.03 s y después ambos coinciden hasta el final de la lectura (4 s). Al cancelar, ambos pasan a «no» desde la primera lectura (0.19 s). En el teléfono el desfase era sostenido durante segundos y en todas las lecturas retenidas, no un retardo.
- **Ya existía antes de la corrección y es independiente del cambio de código**: en la segunda pasada, con el código anterior (`view.keepScreenOn`), el mismo lector dio «ninguna ventana con KEEP_SCREEN_ON» en el teléfono mientras el bloqueo de energía estaba retenido, y en el emulador de referencia con ese mismo código sí encontró `KEEP_SCREEN_ON` (lo vi en esa sesión y no quedó en un archivo; se anota aquí como recuerdo).
- Conclusión: no se pudo reproducir en emuladores y no puede ser un defecto de la corrección (el mismo comportamiento se veía con el código anterior). Lo más probable es una diferencia en cómo el volcado de `dumpsys window` del dispositivo físico presenta las banderas de la ventana (formato propio del fabricante) o que las expone en otra estructura; no lo pude confirmar porque el teléfono se desconectó antes de poder volcar el bloque completo. El requisito de la pantalla encendida lo decide el bloqueo de energía, y ese indicador fue correcto en todas las lecturas del teléfono. No lo cuento como defecto ni lo descarto: queda como limitación del lector en ese dispositivo, anotada.
- El lector sí se corrigió durante la pasada (la primera versión no encontraba la línea `fl=` porque está en una línea aparte de `ty=`; ya extrae la de la ventana correcta). Esa corrección es la que explica que al principio ambos indicadores aparecieran en blanco; no explica la discrepancia del teléfono, que se mantiene tras la corrección.

## Defectos nuevos

Ninguno.

## No ejecutado

- En el dispositivo físico: «Volver al lobby» del caso 3 y el caso 4 completo (teléfono desconectado).
- Espera real de más de 10 min en la partida en curso (no pedida; solo indicador).
- Verificar en el teléfono el formato del volcado de ventana, para cerrar la discrepancia del lector.

## Confirmaciones finales

- Ambas instancias emuladas con ping correcto, sin modo avión, wifi activo; esta pasada no cortó la red.
- Sin juegos de prueba esperando: las listas de las dos instancias mostraron «No hay juegos disponibles» al final; ambas apps quedaron reiniciadas en la pantalla inicial.
- Sin procesos en segundo plano míos (el envío periódico de `KEYCODE_WAKEUP` y la espera larga están terminados); los emuladores siguen encendidos.
- Sin números de serie, puertos, modelos ni valores de `google-services.json` en esta carpeta (`grep` sin coincidencias). Un volcado de `uiautomator` hecho por descuido en el teléfono con la pantalla de inicio visible se filtró en memoria y no se guardó.
- No se tocó la primera ni la segunda pasada ni `tasks.md`; no se marcó ninguna casilla.
