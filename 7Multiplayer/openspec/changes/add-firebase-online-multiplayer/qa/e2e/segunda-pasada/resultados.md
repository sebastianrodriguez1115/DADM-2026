# Segunda pasada de QA de extremo a extremo (tarea 9.5 y cierre de 8.4 a 8.6)

- Fecha: 2026-10-11. Commit probado: `8977186` (la corrección es `e6fdb06`). APK reconstruido (`./gradlew :app:assembleDebug`) e instalado con `install -r` en las dos instancias.
- Destinos: instancia A (dispositivo físico, API 35) e instancia B (emulador, API 35). Guarda de identidad antes de cada operación; la red del teléfono nunca se tocó; los cortes de red fueron solo en B (`svc wifi|data disable` y `enable`).
- Bitácora con marcas de hora: `log.txt`. Capturas: `capturas/` (las del teléfono, recortadas a la zona de la app; los cuadros del teléfono capturados justo al volver de la pantalla de inicio se descartaron sin conservarse, ver nota en `log.txt`).
- Veredictos: pasa, falla, no ejecutado.

## Veredicto global

Las dos correcciones funcionan: D-1 y D-2 de la primera pasada quedan **resueltos**, y en ningún caso el dispositivo que perdió la conexión mostró «Tu rival salió de la partida» (4 cortes de red y 5 salidas a segundo plano de ≥20 s en las dos instancias). Aparece un defecto **nuevo y reproducible en las dos instancias, D-4**: la pantalla encendida queda retenida después de que la app pasa por segundo plano estando en «Esperando rival…» o en una partida, y no se libera ni en el resultado, ni en el lobby, ni en la pantalla inicial (solo al reiniciar el proceso). Contradice el requisito «Pantalla encendida durante la partida». Por eso la tarea 9.5 no queda limpia.

| Tarea | ¿Cumple su «Verificar»? | Motivo |
|---|---|---|
| 9.5 | **No del todo** | Cumple: cada escenario con resultado, captura y tiempos; mensaje exacto en el dispositivo afectado en cada corte y cada segundo plano largo de ambas instancias (ninguno con «Tu rival salió de la partida»); pantalla encendida demostrada en «Esperando rival…» con 12 min 15 s sin tocar el teléfono; D-1 y D-2 resueltos; sin identificadores. Falla: (a) D-4 (defecto nuevo contra «Pantalla encendida durante la partida»); (b) la espera real sin tocar la pantalla se hizo en «Esperando rival…» pero no en la partida en curso (solo el indicador de `dumpsys`; la consigna de esta pasada pedía la espera real solo en la espera). |
| 8.4 | Sí, con una reserva | Todo escenario tiene veredicto con captura (primera pasada, `reto/checklist.md`) o la prueba automática que lo cubre con clase y método (tabla de abajo). Reserva: «Pérdida de conexión en el lobby» sigue fallando en cortes sucesivos (D-3 de la primera pasada, limitación pendiente de repetir en el dispositivo físico; no se repitió porque la consigna prohíbe tocar la red del teléfono). |
| 8.5 | Sí, por mi parte | Cancelar con botón y con Back, cierre del creador y juego recién cancelado pasaron en la primera pasada (`lobby/resultados.md`); el fallo al crear queda con su prueba automática (clase y método, abajo); la carrera y el tercer jugador los documenta otro agente (no los revisé ni los repetí); el borrado desde la consola pasa a 8.8. Esta pasada añadió la desaparición del juego en espera de la lista de A por corte de red (114.8 s) y por segundo plano (≈6 s). |
| 8.6 | Sí en lo sustancial, con una cuestión de forma | Con las decisiones revisadas: «Salir» y Back con diálogo, cierre de la app, corte de red (mensaje de conexión perdida, sin reanudar), segundo plano breve y largo en ambas instancias y Back sin diálogo en final, abandono y conexión perdida pasan; se anota la detección (tabla de tiempos). La evidencia vive en `segunda-pasada/` y no en `abandono/` (que conserva los resultados de la primera pasada y no se reescribió); decide quien marque si eso basta. El residuo de nodo en la consola es de 8.8 (usuario). |

## Puntos de la consigna, en orden

### 1. Pérdida de conexión durante la partida (corte de red en B)

A es el creador (X) y juega una casilla; B (O) pierde la red. B es un proceso nuevo en los casos 1 a 3 y un proceso que ya había sufrido cortes en el caso 4.

| Caso | Proceso de B | Mensaje exacto en B y cuándo | Aparecen «Tu rival salió…» o «Sin conexión» | Tablero | Qué ve A y cuándo | Tras restablecer la red | Salida |
|---|---|---|---|---|---|---|---|
| CUT1 | nuevo | «Se perdió la conexión. La partida terminó.» con «Volver al lobby» a 0.38 s del corte (cuadro a 0.24 s: «Tu turno»; a 0.38 s: el mensaje) | no, ni en volcados ni en los cuadros distintos | bloqueado (9 casillas deshabilitadas) | «Tu rival salió de la partida» a los 115.7 s con la red de B cortada todo ese tiempo | B sigue igual (sin reanudar) | «Volver al lobby» → lobby sin diálogo |
| CUT2 | nuevo | mismo mensaje, ~0.45 s | no | bloqueado | no lo ve durante los 40 s del corte; lo ve antes de 8 s después de restablecer (≤ ~49 s) | igual | Back → lobby sin diálogo |
| CUT3 | nuevo | mismo mensaje, ~0.44 s | no | bloqueado | igual que CUT2 | igual | «Volver al lobby» → lobby |
| CUT4 | con cortes previos | mismo mensaje, ~0.37 s | no | bloqueado | igual que CUT2 | igual | «Volver al lobby» → lobby |

Evidencia: `capturas/CUT1_B_antes_del_corte_instancia-B.png`, `capturas/CUT1_B_mensaje_conexion_perdida_instancia-B.png`, `capturas/CUT1_B_tras_restablecer_instancia-B.png`, `capturas/CUT2_*`, `capturas/CUT3_*`, `capturas/CUT4_*`, `capturas/P3_B_conexion_perdida_instancia-B.png`; secuencias de textos en `log.txt` (líneas `CUT1` a `CUT4`).

Observaciones:
- El orden de llegada, que era el riesgo expreso: en 4 de 4 cortes el aviso de conexión perdida precede al juego nulo; no hubo ningún caso con «Tu rival salió de la partida» en el dispositivo que perdió la conexión.
- En CUT2 a CUT4 la red de B se restableció a los ~41 s y A ya mostraba el abandono en la lectura siguiente (8 s después). No se puede separar si lo provocó la reconexión de B o la detección del servicio; la única medición con la red de B cortada hasta el final es la de CUT1 (115.7 s), coherente con los ~114 s de la primera pasada. Es la limitación aceptada («del orden de minutos»), no un defecto.
- Con la partida ya terminada (resultado «Ganó Ana» en B), un corte de red de 12 s no cambia nada: B conserva el resultado, sin «Se perdió la conexión» ni «Tu rival salió» (`capturas/CUT_TERMINADA_B_conserva_resultado_instancia-B.png`).

### 2. Segundo plano con una partida en curso (HOME y `am start`)

| Caso | Instancia que sale | Ausencia | Qué ve al volver | Qué ve el rival y cuándo |
|---|---|---|---|---|
| BG_B_3s | B | 3 s | la partida conservada («Tu turno», casillas habilitadas) | nada cambia («Turno de Luis») |
| BG_B_20s | B | 20 s | «Se perdió la conexión. La partida terminó.» con «Volver al lobby», casillas deshabilitadas | A ve «Tu rival salió de la partida» a ~7 s de la salida |
| BG_A_3s | A | 3 s | la partida conservada («Turno de Luis») | nada cambia («Tu turno») |
| BG_A_20s | A | 20 s | «Se perdió la conexión. La partida terminó.» con «Volver al lobby» | B ve «Tu rival salió de la partida» a ~8 s de la salida |

Evidencia: `capturas/BG_B_3s_*`, `capturas/BG_B_20s_*`, `capturas/BG_A_3s_*`, `capturas/BG_A_20s_*`. En la instancia A solo se envió `KEYCODE_HOME` y se volvió con `am start`; no se tomó ningún volcado ni captura mientras se veía la pantalla de inicio (el rival se observó desde la instancia B).

### 3. Pantalla encendida

**Indicador usado.** `dumpsys power`, sección «Wake Locks»: una línea `SCREEN_BRIGHT_WAKE_LOCK 'WindowManager/displayId:0' ... ws=WorkSource{<uid> com.edu.unal.tictactoe}`. Es fiable porque ese lock lo crea el gestor de ventanas (no la app, que no declara permisos de wake lock) cuando la ventana visible tiene `keepScreenOn`, y el `WorkSource` lo atribuye a la app; sin esa bandera la línea no aparece (comprobado con la app recién iniciada: «sin lock»). Además el campo `ACQ=` indica cuánto hace que se adquirió, lo que permite distinguir un lock continuo de uno nuevo. Se descartó un primer indicador más laxo (cualquier lock con el nombre del paquete) que daba «retiene» en todas las pantallas (nota en `log.txt`). El código usa `view.keepScreenOn` (`KeepScreenOn.kt`).

Valores con ambas instancias recién reiniciadas (`log.txt`, líneas `KEEP`):

| Pantalla | Instancia A | Instancia B |
|---|---|---|
| Pantalla inicial | sin lock | sin lock |
| Modo local | (no medido en A) | sin lock |
| Lobby | sin lock | sin lock |
| «Esperando rival…» | **retiene** | **retiene** (medido también en solitario) |
| Partida en curso | **retiene** | **retiene** |
| Resultado (victoria) | sin lock | sin lock |
| Lobby tras el resultado | sin lock | sin lock |
| Abandono del rival | sin lock | (lobby) sin lock |
| Conexión perdida por corte de red | n/a (la red del teléfono no se toca) | sin lock |
| Cancelar la espera, Back en la espera | n/a | sin lock tras cada uno |

**Espera real en la instancia A.** Con A recién reiniciada, en «Esperando rival…», sin ningún evento de entrada (envío periódico de `KEYCODE_WAKEUP` suspendido, lecturas solo por `dumpsys` cada 60 s): 735 s (12 min 15 s, más que el apagado automático de 10 min). Resultado: pantalla `Awake`, sin keyguard, foco en la app y lock de la app presente en cada lectura; al final B seguía mostrando «Juego de Ana» en su lista (`capturas/P3_A_espera_larga_fin_instancia-A.png`). Un primer intento no fue válido porque la app salió de primer plano a los ~2 min (el teléfono quedó en la pantalla de inicio por uso del usuario) y se repitió. En la partida en curso solo se midió el indicador (no se hizo la espera real).

### 4. Pérdida de conexión mientras se espera rival

| Caso | Quién | Qué ve al volver o al perder la red | Desaparece de la lista del otro |
|---|---|---|---|
| WAITCUT | B crea y se le corta la red (proceso nuevo) | lobby con «Sin conexión» y, a 0.6 s, el aviso «Este juego ya no está disponible» (visible; desaparece a los ~4 s); `capturas/WAITCUT_B_lobby_con_aviso_y_sin_conexion_instancia-B.png` | de la lista de A a los 114.8 s (detección del servicio; la red de B siguió cortada) |
| WAITBG_B | B crea y pasa 20 s en segundo plano | lobby; a 0.62 s «Sin conexión» con el aviso «Este juego ya no está disponible», y a 2.4 s el lobby conectado con el aviso aún visible (`capturas/WAITBG_B_*`) | de la lista de A a ~6.4 s de la salida |
| WAITBG_A | A crea y pasa 20 s en segundo plano | lobby; a 0.56 s «Sin conexión» con el aviso, a 1.9 s el lobby con el aviso (`capturas/WAITBG_A_cuadro*`) | de la lista de B a ~6 s de la salida |

El aviso llegó a verse en los tres casos (las capturas de cuadros lo muestran; el primer volcado de `uiautomator`, ~2 s después, a veces ya no lo mostraba).

### 5. Regresión rápida

| Caso | Resultado | Evidencia |
|---|---|---|
| REG1 victoria completa; Back sin diálogo en el resultado | pasa: «Ganaste» / «Ganó Ana»; Back lleva al lobby sin diálogo | `capturas/REG1_*` |
| REG2 «Salir» con diálogo y «Abandonar» | pasa: «¿Abandonar la partida?» con «Cancelar» y «Abandonar»; el otro ve «Tu rival salió de la partida» a 0.29 s; Back en el abandono lleva al lobby sin diálogo | `capturas/REG2_B_ve_abandono_instancia-B.png` |
| REG3 `force-stop` de una instancia | pasa: el otro ve «Tu rival salió de la partida» a 0.82 s; Back sin diálogo | `capturas/REG3_A_ve_abandono_instancia-A.png` |

## Defecto nuevo

### D-4 (alta). La pantalla encendida queda retenida tras pasar por segundo plano, en pantallas donde no debe

- Requisito: online-match, «Pantalla encendida durante la partida»: al terminar la partida (resultado, abandono, conexión perdida) y «en cualquier pantalla distinta de la partida en curso» la app NO debe mantener la pantalla encendida (escenario «Partida terminada»); online-lobby, «Crear un juego y esperar rival» (solo «Esperando rival…» lo mantiene).
- Pasos (mismo resultado en A y en B): (1) reiniciar la app (`force-stop`, `am start`); (2) lobby → «Esperando rival…» (o una partida en curso); (3) `KEYCODE_HOME` durante 3 s o más y volver con `am start`; (4) terminar la partida o cancelar la espera y revisar `dumpsys power` en cada pantalla.
- Esperado: el lock `SCREEN_BRIGHT_WAKE_LOCK 'WindowManager…'` atribuido a la app desaparece en cuanto la pantalla deja de ser «Esperando rival…» o la partida en curso.
- Obtenido: el lock se vuelve a adquirir al regresar y se queda (el `ACQ` sigue creciendo, es el mismo lock) en el resultado, el lobby y la pantalla inicial (A, 3 min en la última lectura; B, hasta el final de la prueba). Medido en: partida con segundo plano de 3 s y fin normal (B: lock retenido en el resultado y en el lobby; A sin segundo plano: sin fuga); partida con segundo plano de 20 s y mensaje de conexión perdida (A y B: lock retenido en esa pantalla, en el lobby y en la pantalla inicial); espera con segundo plano de 20 s (B: lock retenido en el lobby). Pasar después por «Esperando rival…» y cancelar no lo libera. Solo reiniciar el proceso lo limpia. Sin segundo plano no hay fuga (lobby, espera, partida, resultado, abandono y corte de red en B se liberan bien).
- Efecto: la pantalla de quien volvió de segundo plano no se apaga nunca mientras la app siga en primer plano, también en el lobby y en la pantalla inicial. Para una demostración basta con cerrar y reabrir la app.
- Hipótesis sin confirmar: `KeepScreenOn` pone `view.keepScreenOn = false` al salir de la composición; si eso ocurre con la ventana detenida o la vista recreada al volver, la bandera anterior se reaplica sin que ninguna composición posterior la quite. No se miró el código del framework.
- Evidencia: `log.txt` (líneas `KEEP`, `LEAK`, `LEAK-B`, `LEAK2`).

## Defectos de la primera pasada

- **D-1 resuelto**: quien pierde la red ve «Se perdió la conexión. La partida terminó.» (CUT1 a CUT4).
- **D-2 resuelto**: el segundo plano largo ya no deja «Tu rival salió de la partida» a quien salió; ve el mensaje de conexión perdida (BG_A_20s, BG_B_20s, WAITBG_*). La partida sigue terminando para ambos tras unos 7 s fuera (limitación aceptada, decisión 5 de D16).
- **D-3 se mantiene** como limitación pendiente de repetir en el dispositivo físico (no se repitió aquí): cortes sucesivos de red en el lobby de B no mostraron «Sin conexión» en la primera pasada. En esta pasada no se probó el lobby sin conexión.

## Limitaciones que se mantienen (no son defectos)

- La partida termina cuando un participante se desconecta, sin periodo de gracia: una caída de red o unos 7 s fuera de la app la terminan.
- Detección del rival cuando la red del otro se corta de golpe: del orden de minutos (115.7 s con la red de B cortada hasta el final).
- Sonido (lo comprueba el usuario).

## Escenarios que dos instancias no producen y la prueba automática que los cubre

Leí el cuerpo de cada método citado. Ruta base de las pruebas JVM: `app/src/test/java/com/edu/unal/tictactoe/`; instrumentadas: `app/src/androidTest/java/com/edu/unal/tictactoe/`.

| Escenario | Prueba | Qué comprueba realmente |
|---|---|---|
| «Conectando…» (online-lobby: Estado de carga) | `OnlineLobbyScreenTest.connectingShowsOnlyTheConnectingText` (instrumentada) | Con `LobbyConnection.Connecting` y dos juegos en el estado, muestra «Conectando…» y ni la lista, ni el encabezado, ni «No hay juegos disponibles», ni «Crear juego», ni reintento. |
| «Conectando…» persiste hasta tener identidad | `ui/online/lobby/OnlineLobbyViewModelTest.slowIdentityKeepsConnectingAndNeverTimesOutBeforeItEstablishes` (JVM) | Con el inicio de sesión retenido, a los 10 s sigue en `Connecting`; al liberarlo pasa a `Connected` y las acciones se habilitan. |
| «Conectando…» reaparece tras «Reintentar» | `ui/online/lobby/OnlineLobbyViewModelTest.identityFailureThenRetryGoesBackToConnectingAndThenConnects` (JVM) | Tras `IdentityFailed`, `onRetryConnection` vuelve a `Connecting` y luego `Connected` (dos llamadas de inicio de sesión). |
| Lista con dos juegos | `OnlineLobbyScreenTest.connectedWithGamesShowsHeaderAndOneItemPerGame` (instrumentada) | Con «Ana» y «Luis», muestra el encabezado «Juegos disponibles» y «Juego de Ana» y «Juego de Luis», sin «No hay juegos disponibles». |
| Lista con dos juegos (modelo de vista) | `ui/online/lobby/OnlineLobbyViewModelTest.gamesListShowsTwoGamesNewestFirst` (JVM) | Dos dispositivos crean juegos y la lista del lobby tiene ambos, el más reciente primero. |
| Un juego tomado sale de la lista | `ui/online/lobby/OnlineLobbyViewModelTest.newGameAppearsAndTakenGameLeavesWithoutReloading` (JVM), `data/online/FakeOnlineGameServiceTest.hostSeesPlayingWhenRivalJoinsAndGameLeavesTheList` (JVM) y, contra Firebase real, `FirebaseOnlineGameServiceLiveTest.guestJoinsAndBothSeeTheGameInProgress` (instrumentada; su última aserción espera que el juego salga de la lista) | Observan la lista de quien se une, no la de una tercera identidad. Para la lista de otra identidad con Firebase real solo existe `FirebaseOnlineGameServiceLiveTest.cancelledGameLeavesTheListAndItsNodeIsRemoved`, que prueba la retirada por cancelación. No encontré una prueba que observe, desde una tercera identidad, que un juego tomado sale de la lista. |
| Fallo al crear el juego | `ui/online/lobby/OnlineLobbyViewModelTest.failedCreationSignalsAndDoesNotWaitThenSecondAttemptSucceeds` (JVM) | Con `failNextCreate`, emite `CreateFailed`, no hay `waitingGameId`, no queda juego en la lista y el segundo intento funciona. |
| Fallo al crear (interfaz completa) | `OnlineLobbyNavigationTest.aFailedCreationShowsTheMessageAndStaysInTheLobby` (instrumentada) | Muestra «No se pudo crear el juego», sigue en el lobby, no muestra «Esperando rival…» y `availableHosts()` queda vacío. |
| Fallo al crear (mensaje) | `OnlineLobbyScreenTest.createFailedIsShownOverTheLobbyAndReportedOnce` (instrumentada) | El mensaje se muestra una vez y «Crear juego» sigue habilitado. |
| Fallo al crear (servicio) | `data/online/FakeOnlineGameServiceTest.failedCreateLeavesNoGameAndNextCreateWorks` (JVM) | La creación fallida lanza `OnlineServiceException`, no deja juego y la siguiente funciona. |

Límite de esa cobertura: todas las pruebas del fallo al crear usan el servicio falso; ninguna prueba contra Firebase real provoca un fallo de creación.

Carrera al unirse y tercer jugador: ya cubiertos y documentados por otro agente en `../lobby/resultados.md` (sección «Cobertura automática de L05 y L07») y en `../lobby/carrera-automatizada-pasada-*.txt`; no se repiten aquí.

## No ejecutado y por qué

- Espera real de más de 10 min en la partida en curso (la consigna pedía la real solo en «Esperando rival…»; en la partida solo el indicador).
- Lobby sin conexión (D-3) en el dispositivo físico: la consigna prohíbe tocar su red.
- Residuo de nodos en la consola: lo revisa el usuario (8.8).
- Rotación, sonido y TalkBack.

## Confirmaciones finales

- Red de B restablecida y comprobada: `ping` correcto, `airplane_mode_on=0`, wifi y datos activos. La red de A nunca se tocó.
- Sin juegos de prueba: el lobby de B mostró «No hay juegos disponibles» al final y ambas apps quedaron reiniciadas en la pantalla inicial. Los juegos en curso o abandonados se retiran solos; no se revisó la consola.
- Evidencia sin identificadores: `grep` de serial, modelo, fabricante, producto y marca de ambos destinos sobre texto y metadatos de todos los archivos de esta carpeta, sin coincidencias; sin valores de `google-services.json`; ninguna captura de otras apps del usuario.
- Procesos en segundo plano terminados (envío periódico de `KEYCODE_WAKEUP` y la espera larga); el emulador quedó encendido. No se modificó `tasks.md` ni nada de la primera pasada.
