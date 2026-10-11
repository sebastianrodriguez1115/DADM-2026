# Informe de QA de extremo a extremo, primera pasada (commit `86be80a`, 2026-10-11)

Destinos: instancia A (dispositivo físico, API 35) e instancia B (emulador, API 35). Commit `86be80a`. Evidencia en esta carpeta (`setup.md`, `test-plan.md`, `reto/`, `lobby/`, `abandono/`, `riesgos/`).

## Veredicto global

**No aprobado todavía.** Hay defectos pendientes de decisión.

El flujo del reto funciona de punta a punta entre teléfono y emulador: crear, listar, unirse, jugar, ganar, empatar, cancelar, abandonar y cerrar la app. Pero hay dos defectos relevantes para una demostración (D-1 y D-2), un defecto probable (D-3) y escenarios que no se pudieron ejecutar.

| Tarea | Veredicto | Casilla |
|---|---|---|
| 8.3 | Bloqueada: 7.9 y 7.10 están marcadas por el usuario, pero 7.11 no estaba confirmada al ejecutar. Las instancias, el APK y la guarda de identidad sí están listos. | No marcada |
| 8.4 | Incompleta. Todo lo ejecutado pasa salvo la pérdida de conexión en cortes sucesivos (D-3). «Conectando…» no se pudo observar, y «Lista con juegos» y «Un juego tomado sale de la lista» exigen 3 destinos. | No marcada |
| 8.5 | Incompleta. Pasan cancelar con botón y con Back, el cierre del creador y elegir un juego recién cancelado. Están bloqueados la carrera (0 de 5 repeticiones) y el tercer jugador. No se ejecutaron el borrado desde la consola ni el fallo al crear. | No marcada |
| 8.6 | Falla por D-1 y D-2. | No marcada |
| 8.7 | Pasa: (a) a (e) ejecutados. | Marcada |

## Casos y resultados

### 8.4 (`reto/checklist.md`)

41 escenarios revisados (R01 a R19):

- 34 pasan, incluidos los dos de «Ana» desde el teléfono (validación y «  Ana  » aceptado).
- 1 pasa en parte: fallo de identidad con «Reintentar», pero sin ver «Conectando…».
- 1 falla de forma condicional (D-3).
- 4 no ejecutados o no observados, y 1 bloqueado (tercer jugador).
- Ejemplos de lo que pasa: apodo vacío, solo espacios, 21 y 20 caracteres; recordar y cambiar apodo; cancelar y Back en la primera vez; crear, esperar sin tablero, lista en vivo, unirse; marcas, turnos y fuera de turno; casilla ocupada y doble toque; victoria «Ganaste» y «Ganó Ana»; empate; sin revancha y Back sin diálogo.

### 8.5 (`lobby/resultados.md`)

- L01, L02, L03 y L06 pasan.
- L05 y L07 bloqueados.
- L04 y L08 no ejecutados.
- L09 falla en cortes sucesivos.

### 8.6 (`abandono/resultados.md`)

- M01 a M07 pasan: «Salir», Back y «Cancelar»; «Abandonar»; Back con abandono; cierre de la app en ambos sentidos; corte de red.
- M08 falla (D-1).
- M09 falla (D-2).
- M11 (residuo de nodos en consola) no se verificó.

### 8.7 (`riesgos/resultados.md`)

(a), (b), (c), (d) y (e) pasan.

## Defectos

### D-1 (alta). Quien pierde la red ve «Tu rival salió de la partida» en vez de «Sin conexión»

- Requisito: `online-match`, «Sin conexión durante la partida» (escenarios «Pérdida breve de conexión» y «Recuperación con la partida vigente»).
- Pasos: abrir B de cero, empezar una partida con A, y en B ejecutar `svc wifi disable; svc data disable`.
- Esperado: «Sin conexión» con las casillas deshabilitadas, y recuperación sola al volver la red si el servicio no declaró abandono.
- Obtenido: a los 0.49 s B muestra «Tu rival salió de la partida» con «Volver al lobby». Nunca aparece «Sin conexión» y no hay recuperación. Mientras tanto A seguía en «Turno de Luis» a los 20 s y solo vio el abandono a los 114 s.
- Se reprodujo dos veces.
- Evidencia: `abandono/M08_B_sin_conexion_en_partida_instancia-B.png` y `abandono/log.txt` (M07, M08).
- Hipótesis sin confirmar: el SDK aplica `onDisconnect().removeValue()` en su caché local, el nodo «desaparece» y `onGameVanished` pasa a abandono.
- Contradice la especificación de `online-match`, no el requerimiento del reto. Que la caída de red termine la partida es riesgo aceptado, pero el mensaje es engañoso y «Sin conexión» es inalcanzable durante una partida.

### D-2 (alta para la demostración). Con la app en segundo plano unos segundos, la partida termina

- Requisito: `online-match`, «Rotación y segundo plano», escenario «Volver de segundo plano con conexión».
- Pasos: partida en curso con B recién abierta; en B, tecla HOME.
- Esperado: la partida continúa y al volver se ve la jugada del rival.
- Obtenido: a los 6.7 s A ve «Tu rival salió de la partida» y B, al volver, también. Con 2 s y 4 s en segundo plano la partida sobrevive.
- Causa observada: `dumpsys netpolicy` marca `blocked=APP_BACKGROUND` para el paquete. En API 35 el sistema le corta la red, Firebase se desconecta y `onDisconnect` retira el juego.
- En el teléfono ocurrió también una vez al apagarse la pantalla con la partida en curso.
- No se probó HOME en el teléfono para no tocar su pantalla de inicio.
- Evidencia: `abandono/log.txt` (M09, M09b, M09c) y `abandono/M09_*`.

### D-3 (media, posible artefacto del emulador). Los cortes de red sucesivos no se reflejan en la app

- Requisito: `online-lobby`, «Conexión con el servicio» (pérdida de conexión y recuperación automática).
- Con un proceso nuevo, el primer corte de red da «Sin conexión» en ~2 s, «Crear juego» queda atenuado y la app se recupera sola.
- En cortes repetidos dentro del mismo proceso no aparece «Sin conexión» (75 s en tres ciclos y más de 5 minutos en otra prueba), y la lista queda obsoleta, por ejemplo mostrando un juego ya cancelado. El aviso aparece solo cuando vuelve la red.
- El corte con `svc` o modo avión no cierra los sockets del emulador, así que no se puede descartar un artefacto. No se pudo repetir en el teléfono porque la consigna prohíbe tocar su red.
- Evidencia: `lobby/L08c_*`, `lobby/L08d_*`, `lobby/log.txt` (L08e).

## Mediciones pedidas

### Detección de desconexión

Tiempo hasta que el rival ve «Tu rival salió de la partida»:

| Causa | Tiempo |
|---|---|
| «Abandonar» | 0.41 s |
| `force-stop` del invitado | 0.78 s |
| `force-stop` del creador | 0.34 s |
| Corte de red del jugador | 114 s (una medición válida; otra prueba dio más de 34 s sin detección) |
| Segundo plano en API 35 | 6.7 s |

### Carrera al unirse (5 repeticiones)

No ejecutada. Está bloqueada porque necesita tres identidades a la vez y solo hay dos destinos. No se arrancó un segundo emulador ni se crearon usuarios, porque no estaba autorizado.

Como sustituto se midió la carrera cancelar contra elegir: 29 intentos con desfases de -0.1 a 0.7 s.

- 6 intentos dejaron a B en el lobby con «Este juego ya no está disponible» (snackbar breve).
- 9 veces ganó elegir por poco, y B vio de inmediato «Tu rival salió de la partida».
- 14 veces B quedó en el lobby sin mensaje, porque el elemento ya no estaba o el aviso había expirado.
- Nunca quedó B unido con el creador dentro.

### Fusión de la jugada final con la salida del ganador

0 de 10 mostraron «Tu rival salió de la partida»; todas conservaron «Ganó Ana».

- 5 repeticiones con el primer toque 0.25 s tras la jugada y 5 con 20 toques seguidos sin espera.
- El intervalo mínimo está acotado por la latencia de adb.

### Manejo de nulo (a)

0 fallos.

- 7 uniones desde B recién abierta con toques ciegos desde que se abre el lobby: nunca apareció «ya no está disponible».
- Las jugadas inmediatas, 14 de 14, fueron aceptadas, y A recién abierta también.
- No se puede probar con certeza que el valor no estuviera en caché.

### Latencia de una jugada hasta la pantalla del rival

0.29 a 0.85 s en cuatro muestras.

## Observaciones de usabilidad para una demostración en vivo

- La app debe quedar en primer plano y con la pantalla encendida en ambos teléfonos. Una pantalla apagada o la app en segundo plano más de ~7 s terminan la partida (D-2).
- Si se corta la red de un jugador, el otro espera casi 2 minutos viendo «Turno de <apodo>».
- «Este juego ya no está disponible» es un snackbar breve y se pierde si no se mira.
- Con el teclado abierto, Back lo cierra primero (comportamiento conocido).
- Tras un corte con un toque justo antes de que la app lo notara, la lista de B quedó vacía más de un minuto mientras A seguía esperando (observación O-1, no reproducida de forma limpia). Al reabrir la app apareció el juego.
- «Crear juego» se ve atenuado sin conexión, pero `uiautomator` no refleja `enabled=false`; la exposición a TalkBack es de la tarea 8.8.
- La app del teléfono ya tenía un apodo guardado. Se cambió a «Ana» dentro de la app. B quedó con «Luis».

## Casillas de `tasks.md`

- Marcada: 8.7, porque su «Verificar» se cumple de verdad.
- No marcadas: 8.3 (faltaba 7.11); 8.4 (hay escenarios sin veredicto y D-3); 8.5 (hay bloqueos y no ejecutados); 8.6 (D-1 y D-2).

## Qué no se pudo ejecutar

- L05 (carrera, 5 repeticiones) y L07 (tercer jugador): faltan identidades.
- L04 (borrado del juego en espera desde la consola): la consigna prohíbe otra vía de borrado.
- L08 (fallo al crear): con la red cortada el botón se deshabilita antes del toque, así que no se pudo provocar (6 intentos).
- «Lista con juegos» y «Un juego tomado sale de la lista»: necesitan 3 destinos.
- «Conectando…» y su reaparición tras «Reintentar»: demasiado breves, menos de 0.1 s.
- Revisión de residuos de nodos en la consola.
- Rotación de las pantallas en línea, que queda para las pruebas instrumentadas (8.2); y sonido y TalkBack, que son de 8.8.
- El sonido no se puede oír, así que (c) solo comprueba tablero y tiempo.

## Incidentes de la tanda

- El teléfono se bloqueó y apagó la pantalla una vez en pleno bloque. No se desbloqueó; se esperó a que volviera encendido y sin keyguard, y se reanudó.
- Después se envió `KEYCODE_WAKEUP` periódicamente solo cuando la pantalla estaba encendida y desbloqueada; no se cambiaron ajustes.
- El usuario usó el teléfono varias veces (otras apps en primer plano). No se tocaron esas apps; se esperó.
- Esas interrupciones y el segundo plano dejaron partidas de A como abandono.
- Una captura de A tomada en una de esas interrupciones mostraba otra app del usuario. Se descartó y no está en la evidencia.
- La conexión adb del teléfono estuvo unos 12 minutos caída; se reanudó con la guarda de identidad al reaparecer.
- El intento de documentar M05 (creador A) y M07 (A), y el primer M09, quedó confundido o inválido. Está anotado en la nota de validez de `abandono/log.txt` y sus capturas se descartaron.

## Confirmaciones finales

- Red de B restablecida: `ping` correcto; `airplane_mode_on=0`, wifi y datos activos. La red del teléfono nunca se tocó.
- Juegos de prueba: ninguna instancia tiene un juego esperando; B mostró «No hay juegos disponibles» al final y ambas apps quedaron en la pantalla inicial. Los juegos en curso o abandonados se retiran solos, pero no se revisó la consola, así que un nodo residual en otro estado no se descarta.
- Evidencia sin identificadores: búsqueda de serial, modelo, fabricante, producto y marca de ambos destinos sobre toda la carpeta (texto y metadatos PNG), sin coincidencias reales. Solo se menciona el nombre del archivo `google-services.json`, sin ningún valor.

## Herramientas y re-ejecución

- No se instaló nada. Se usó `adb` (`am`, `input`, `uiautomator dump`, `screencap`, `svc` solo en B), `sips` para recortar capturas y Python 3 estándar.
- Los guiones auxiliares quedaron en un directorio temporal y no se versionan.
- Para re-ejecutar: construir el APK, `install -r` en ambos destinos con la guarda de `setup.md`, y seguir los pasos de cada `resultados.md` o `checklist.md`.

---

## Notas de la revisión del orquestador (2026-10-11)

Esta sección no forma parte del informe de QA.

- D-1, causa validada contra el código: `OnlineMatchViewModel` pasa a abandono en cuanto el juego observado llega nulo (`onGameVanished`), sin mirar el estado de la conexión, y el servicio registra `onDisconnect().removeValue()` sobre el nodo del juego. Que el SDK aplique esa operación en su copia local al detectar la desconexión es la explicación coherente con lo medido (B ve el abandono a los 0.49 s mientras el servidor conserva el juego), pero no se comprobó en el código del SDK.
- D-2: no se revisó código; la causa es la observada por QA. Consecuencia para el diseño: con el juego ligado a la conexión, cualquier pausa de la app de más de unos segundos termina la partida.
- D-3, sin confirmar como defecto de la app: `OnlineLobbyViewModel` sigue recolectando el estado de conexión durante toda la vida del lobby, así que el aviso depende de que el SDK detecte el corte. Queda pendiente repetirlo en el dispositivo físico.
- Evidencia: las 26 capturas de la instancia A están recortadas a la zona de la app; se revisaron todas y ninguna muestra barra de estado, notificaciones ni otras apps. La búsqueda de identificadores de ambos destinos y de valores de Firebase sobre `qa/e2e/` no dio coincidencias.
- La precondición 7.11 se cierra aparte, con la prueba automática contra Firebase real; ver `../rules-simulator.md` y `setup.md`.
- Cifras de 8.4 corregidas contra `reto/checklist.md`: el informe original de QA decía 31 escenarios y 24 que pasan; el conteo de la lista de comprobación da 41 y 34. Las categorías que no pasan coincidían.
