# Verification record

Cambio: `add-firebase-online-multiplayer` (reto 7, triqui en línea con Firebase Realtime Database). Rama `reto-7-triqui-online`. Este documento está en curso: la tarea 8.8 depende del usuario y sigue pendiente, así que la tarea 8.9 (este registro) no está marcada todavía.

## 1. Resumen

- El cambio está implementado y verificado de forma automática: 390 pruebas JVM y 297 pruebas instrumentadas en 45 clases, todas en verde en el commit `5d26dfb` (secciones 2 y 3).
- La verificación con dos instancias reales contra Firebase se hizo en tres pasadas de QA. Encontró cuatro defectos: D-1, D-2 y D-4 están corregidos y verificados; D-3 sigue abierto (sección 5).
- Hay comprobaciones que solo puede hacer el usuario (tarea 8.8) y varias cosas que no se verificaron. Están en las secciones 6 y 7, sin suavizar: léelas antes de dar el cambio por cerrado.
- Estado de `tasks.md`: 73 tareas, 71 marcadas. Sin marcar: 8.8 (usuario) y 8.9 (este documento).

## 2. Comandos y resultados vigentes

Commit probado: `5d26dfb` (árbol de trabajo limpio), 2026-10-11. Los resultados de 8.1 y 8.2 son los de esta misma batería.

**Tarea 8.1, build, pruebas JVM y lint**

```
./gradlew testDebugUnitTest lintDebug assembleDebug --rerun-tasks
```

Resultado: 390 pruebas JVM, 0 fallos, 0 errores, 0 omitidas. Lint con 0 errores y 11 advertencias (`UnusedResources` 7, `UseKtx` 2, `RedundantLabel` 1, `ObsoleteSdkInt` 1). Fuente: la ejecución de la batería final, informada por el orquestador; no hay un archivo de salida guardado de este comando en `qa/` (ver sección 7).

**Tarea 8.2, batería instrumentada completa**

```
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s <emulador> install -r <APK de la app> y <APK de pruebas>
adb -s <emulador> shell am instrument -w -e class com.edu.unal.tictactoe.<Clase> com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado: 45 clases y 297 métodos `@Test`, una instrumentación por clase, una sola pasada, sin exclusiones ni repeticiones; las 297 ejecutadas, todas en `OK`, 0 fallos, 0 omitidas, 925 s de pared. Destino: emulador de referencia, API 35, con la identidad verificada antes de cada operación. Evidencia: [qa/instrumented/pasada-final/README.md](qa/instrumented/pasada-final/README.md) y las 45 salidas de esa carpeta (la suma de sus líneas `OK (N tests)` es 297). La batería anterior, con 43 clases y 277 pruebas y un fallo ya corregido de `FirebaseOnlineGameServiceLiveTest`, se conserva como registro en [qa/instrumented/README.md](qa/instrumented/README.md).

## 3. Evidencia por tarea

Todos los enlaces apuntan a archivos que existen.

| Tarea | Qué demuestra | Evidencia |
|---|---|---|
| 1.4 y 1.5 | El build con el plugin `google-services` y la caché de configuración, y la inicialización de Firebase en el dispositivo | [qa/firebase-build-probe.md](qa/firebase-build-probe.md) |
| 1.8 | Actualización de dependencias y de Gradle | [qa/dependency-upgrade.md](qa/dependency-upgrade.md) |
| 7.11 | Reglas de seguridad probadas con la prueba automática en vivo (sustituye al simulador de la consola) | [qa/rules-simulator.md](qa/rules-simulator.md), [firebase/RULES.md](../../../firebase/RULES.md) |
| 7.12 | Prueba en vivo `FirebaseOnlineGameServiceLiveTest` | [qa/instrumented/pasada-final/FirebaseOnlineGameServiceLiveTest.txt](qa/instrumented/pasada-final/FirebaseOnlineGameServiceLiveTest.txt) |
| 8.1 | Build, JVM y lint | sección 2 |
| 8.2 | Batería instrumentada completa | [qa/instrumented/pasada-final/README.md](qa/instrumented/pasada-final/README.md) |
| 8.3 | Preparación con dos instancias y precondiciones | [qa/e2e/setup.md](qa/e2e/setup.md), [qa/e2e/test-plan.md](qa/e2e/test-plan.md) |
| 8.4 | Puntos del reto | [qa/e2e/reto/checklist.md](qa/e2e/reto/checklist.md) (primera pasada) y la tabla de pruebas automáticas de [qa/e2e/segunda-pasada/resultados.md](qa/e2e/segunda-pasada/resultados.md) |
| 8.5 | Cancelación y carrera | [qa/e2e/lobby/resultados.md](qa/e2e/lobby/resultados.md) |
| 8.6 | Abandono, desconexión y segundo plano | primera pasada en [qa/e2e/abandono/resultados.md](qa/e2e/abandono/resultados.md); resultados revisados en [qa/e2e/segunda-pasada/resultados.md](qa/e2e/segunda-pasada/resultados.md) |
| 8.7 | Riesgos que el diseño manda comprobar | [qa/e2e/riesgos/resultados.md](qa/e2e/riesgos/resultados.md) |
| 9.4 | Carrera al unirse y tercer jugador | [qa/e2e/lobby/carrera-automatizada-pasada-1.txt](qa/e2e/lobby/carrera-automatizada-pasada-1.txt), [qa/e2e/lobby/carrera-automatizada-pasada-2.txt](qa/e2e/lobby/carrera-automatizada-pasada-2.txt) y la sección «Cobertura automática de L05 y L07» de [qa/e2e/lobby/resultados.md](qa/e2e/lobby/resultados.md) |
| 9.5 | Segunda y tercera pasada de QA | [qa/e2e/segunda-pasada/resultados.md](qa/e2e/segunda-pasada/resultados.md), [qa/e2e/tercera-pasada/resultados.md](qa/e2e/tercera-pasada/resultados.md) |
| Informe de la primera pasada | Veredicto y defectos D-1 a D-3 | [qa/e2e/report.md](qa/e2e/report.md) |

La evidencia de 8.8 no existe todavía y no se enlaza (sección 6).

## 4. Decisiones, supuestos y sustituciones de evidencia

**Versionado de `app/google-services.json` (tarea 1.7).** El usuario decidió el 2026-10-09 versionarlo en git. Se comprobó que no está ignorado (`git check-ignore app/google-services.json` no imprime nada y termina con código 1) y que está rastreado (`git ls-files` lo lista). La base documental (identificadores no secretos; la seguridad depende de las reglas y de la restricción de la clave de API en la consola) está en `design.md`, «Open Questions». Ningún artefacto de evidencia copia su contenido (sección 10).

**Supuestos A2 a A6.** El usuario los confirmó el 2026-10-09 (`proposal.md`, «Assumptions Confirmed by the User»). Tras la verificación con dos instancias, sigue vigente:

- A2 (abandono por desconexión, sin periodo de gracia). Se confirmó el 2026-10-11 con una aclaración (D16): la detección es inmediata si el rival abandona o cierra la aplicación, y del orden de minutos si su red se corta de golpe; una caída de red o más de unos segundos en segundo plano terminan la partida. Quien pierde la conexión ve «Se perdió la conexión. La partida terminó.».
- A3 (confirmación al salir de una partida en curso), A4 (apodo editable), A5 (sin restaurar la partida en línea tras muerte del proceso) y A6 (sin menú de opciones en las pantallas en línea): sin cambios; verificados con dos instancias (8.4, 8.6 y 8.7).

**Decidido por el usuario el 2026-10-11** (`design.md`, D16):

1. La opción mínima tras la primera pasada: mantener el modelo sin periodo de gracia, mantener la pantalla encendida en «Esperando rival…» y en la partida en curso, mensaje propio para quien pierde la conexión, y aceptar que salir de la aplicación más de unos segundos con una partida en curso la termina.
2. Dar por cumplida la tarea 7.11 con la prueba automática en lugar del simulador de reglas de la consola.
3. Dar por cubiertos la carrera al unirse y el tercer jugador con la prueba automática contra Firebase real, repetida 5 veces.
4. Comprobar él mismo el sonido cuando se pruebe el multijugador.
5. Desconectar el teléfono y seguir con emuladores.

**Propuesto por el orquestador y no objetado por el usuario** (no son decisiones expresas suyas):

- No ejecutar TalkBack, manteniendo el criterio de los talleres anteriores: el orquestador avisó de que así lo entendía y el usuario no respondió a ese punto.
- Registrar con pruebas automáticas los escenarios que dos instancias no pueden producir y pasar el borrado desde la consola a la sesión manual: el orquestador lo comunicó al usuario como un desvío y le pidió que dijera si prefería otra cosa; no hubo objeción.
- Cerrar las tareas 8.4 a 8.6 y 9.5 con las reservas anotadas: decisión del orquestador al revisar la evidencia.

**Sustituciones de evidencia y quién las decidió:**

- Tarea 7.11 (decidido por el usuario): las reglas se probaron con `FirebaseOnlineGameServiceLiveTest` contra el proyecto real, no con el simulador de reglas de la consola. No equivale a haber ejecutado el simulador (lo dice [qa/rules-simulator.md](qa/rules-simulator.md)).
- La carrera de dos jugadores y el tercer jugador (decidido por el usuario; necesitan tres identidades y solo había dos dispositivos): se cubren con `FirebaseOnlineGameServiceLiveTest`, con la carrera repetida 5 veces, dos pasadas guardadas (9.4).
- Los escenarios que dos instancias no pueden producir (propuesto por el orquestador y no objetado) se cubren con pruebas automáticas, con clase y método en la tabla de [qa/e2e/segunda-pasada/resultados.md](qa/e2e/segunda-pasada/resultados.md): «Conectando…», la lista con dos juegos y el fallo al crear. «Un juego tomado sale de la lista» se cubre con `aThirdIdentityObservingTheListSeesTheGameAppearAndDisappearWhenTheGuestJoins`, posterior a esa tabla.
- Sonido (decidido por el usuario: lo comprobará a mano) y TalkBack (propuesto por el orquestador y no objetado: no se ejecuta, como en los talleres anteriores); ver la sección 6.

## 5. Defectos encontrados y cómo se resolvieron

| Defecto | Encontrado en | Qué ocurría | Resolución |
|---|---|---|---|
| D-1 | Primera pasada (`86be80a`) | Quien perdía la red veía «Tu rival salió de la partida», no «Sin conexión», a 0,49 s | Nueva fase `ConnectionLost` y mensaje propio, commit `e6fdb06`. Verificado en la segunda pasada: 4 de 4 cortes de red con el mensaje correcto, ninguno con «Tu rival salió de la partida». |
| D-2 | Primera pasada | Con la app en segundo plano 6,7 s la partida terminaba para ambos | No se evita (decisión 5 de D16): ahora quien sale ve el mensaje de conexión perdida (`e6fdb06`). Verificado en la segunda pasada: 3 s conservan la partida; 20 s la terminan con el mensaje correcto y el rival ve el abandono a los 7 u 8 s. Limitación aceptada. |
| D-3 | Primera pasada | En el emulador, el segundo corte de red en el mismo proceso no mostró «Sin conexión» en el lobby | **Abierto.** No se corrige en este cambio; queda pendiente de repetir en un dispositivo físico (8.8). |
| D-4 | Segunda pasada (`8977186`) | Tras pasar la app por segundo plano (3 s bastan), la pantalla quedaba retenida encendida en el resultado, el lobby y la pantalla inicial | Corregido en `f337002` (la bandera se pide a la ventana de la Activity, con un contador de peticiones). Verificado en la tercera pasada con las reservas de la sección 7. Detalle en D16. |

Otros commits del proceso: la revisión del plan tras la primera pasada fue `8977186`; la cobertura de la carrera y del tercer jugador, `39b0b9c`; la prueba de la lista vista por una tercera identidad, `eebfaf5`; los registros de la segunda y tercera pasada, `6e2b91f` y `5d26dfb`.

## 6. Estado de la tarea 8.8 (pendiente del usuario)

La tarea 8.8 no se ha ejecutado y no se inventa ningún resultado. La componen:

- Comprobar a mano el sonido: efectos de X y O una vez por jugada propia y del rival, sin sonido al abrir o rotar, con «Sonido» deshabilitado ni con la app en segundo plano.
- Borrar desde la consola un juego en espera y ver en el lobby «Este juego ya no está disponible».
- Confirmar en la consola que no quedan juegos de prueba.
- Alternar el modo avión dos veces en el lobby de un dispositivo físico, para resolver D-3.

El teléfono de pruebas se desconectó el 2026-10-11, a mitad de la tercera pasada; las comprobaciones que necesitan un dispositivo físico quedan pendientes hasta que haya uno. TalkBack no se ejecuta, por una propuesta del orquestador que el usuario no objetó (no es una decisión expresa suya): queda como no verificado. Cuando el usuario reporte, el resultado irá a `qa/manual-sound-talkback.md` (todavía no existe).

## 7. No verificado

Esta lista es completa hasta donde se sabe; nada de lo siguiente debe darse por comprobado.

- El sonido real (reproducción de audio) y TalkBack: no se oyen ni se ejecutan en la automatización.
- D-3 en un dispositivo físico: solo se vio fallar en el emulador (primera pasada) y no se repitió.
- Espera real de más de 10 minutos sin tocar la pantalla con la partida en curso. Solo se hizo en «Esperando rival…» (735 s en el dispositivo físico, segunda pasada); en la partida en curso solo se leyó el indicador del sistema.
- D-4 en el dispositivo físico: confirmado según el indicador de energía en los casos 1 a 3 (10 de 10 lecturas); el caso de segundo plano largo y «Volver al lobby» del caso 3 no se ejecutaron porque el teléfono se desconectó. Confirmado completo solo en los dos emuladores (34 de 34 lecturas).
- La discrepancia del indicador de ventana en el dispositivo físico: la línea de banderas de la ventana nunca contenía `KEEP_SCREEN_ON` mientras el bloqueo de energía estaba retenido. No se reprodujo en emuladores, ya aparecía con el código anterior y queda sin explicar; el requisito lo decide el indicador de energía.
- El orden de llegada de los avisos (el de conexión perdida antes del juego nulo): comprobado solo de forma empírica en 4 cortes de red y varias salidas a segundo plano, en los dos tipos de dispositivo. No se leyó en el código del SDK, y las pruebas automáticas fijan ese orden a mano con el servicio falso.
- Las comprobaciones de consola de 8.8: el borrado externo de un juego en espera, que no quedan juegos de prueba, y el residuo de nodos tras las pasadas (las listas de ambas instancias se vieron vacías, pero la consola no se revisó).
- Un fallo real de Firebase al crear un juego: el fallo al crear solo se cubre con pruebas con servicio falso, y con la red cortada el botón queda deshabilitado, así que no se pudo provocar.
- La carrera automática demuestra exclusión mutua (exactamente un ganador por repetición), no un reparto equilibrado de ganadores: dentro de cada pasada ganó siempre la misma identidad.
- El tiempo de detección tras un corte brusco de red lo decide el servicio: 115,7 s con la red del otro cortada hasta el final (114 s en la primera pasada); otra medición de la primera pasada dio más de 34 s sin detección.
- El comando de 8.1 no tiene un archivo de salida guardado en `qa/`; el resultado es el informado por quien lo ejecutó. La batería instrumentada de 8.2 conserva la salida cruda de `am instrument` por clase, no archivos XML.
- La salida de la ejecución de `aThirdIdentityObservingTheListSeesTheGameAppearAndDisappearWhenTheGuestJoins` no está guardada por separado; la clase `FirebaseOnlineGameServiceLiveTest` aparece con 23 pruebas en `OK` en la pasada final.
- Los cortes de red se hicieron solo en la instancia emulada; la red del teléfono nunca se tocó.
- Rotación en las pantallas en línea con las dos instancias: la cubren las pruebas instrumentadas, no las pasadas de QA.

## 8. Notas y limitaciones conocidas

Reverificadas ahora contra el repositorio:

- La regla del diseño ancho está duplicada: `OnlineMatchScreen.kt` y `TicTacToeScreen.kt` contienen cada una su propio cálculo del tablero en diseño ancho.
- Las holguras de las reglas de seguridad están documentadas en `firebase/RULES.md`, sección 6 («Lo que las reglas no comprueban»): el cliente valida las jugadas, no el servicio; la sección existe.
- Una prueba de aislamiento (`GamePreferencesIsolationTest`) nombra el archivo `unrelated_test_preferences`: es el origen del residuo mencionado abajo.
- `app/google-services.json` está rastreado por git.

No reverificadas (no se ejecutó Gradle ni adb para este documento):

- Dos advertencias de Kotlin 2.4 de «llamada segura innecesaria» en pruebas anteriores a este cambio. [qa/dependency-upgrade.md](qa/dependency-upgrade.md) anota «sin advertencias nuevas del compilador de Kotlin» tras la subida, pero no las cuenta.
- Un aviso de obsolescencia de Gradle 10 de origen no identificado: está anotado en [qa/dependency-upgrade.md](qa/dependency-upgrade.md); con `--warning-mode all` no se listó el origen.
- Con el teclado abierto hacen falta dos Back para cerrar el diálogo de apodo: está anotado como comportamiento conocido en [qa/e2e/report.md](qa/e2e/report.md), pero no se volvió a comprobar.
- Una prueba de aislamiento dejó un archivo `unrelated_test_preferences.xml` en los datos de la app del teléfono: no se volvió a mirar el teléfono (está desconectado).

## 9. Incidencias de proceso que afectan a la evidencia

- Una batería instrumentada intermedia terminó con un fallo de `FirebaseOnlineGameServiceLiveTest`, ya corregido; se conserva en `qa/instrumented/` como registro.
- En la primera pasada, el uso del teléfono por el usuario interrumpió pruebas: el teléfono se bloqueó una vez en pleno bloque, otras apps pasaron a primer plano y algunos intentos se invalidaron (anotados en la nota de validez de [qa/e2e/abandono/resultados.md](qa/e2e/abandono/resultados.md) y en el informe). En la segunda pasada, un primer intento de espera real no fue válido porque la app salió de primer plano y se repitió.
- El teléfono se desconectó a mitad de la tercera pasada; el resto se hizo con dos emuladores (el emulador de la instancia B y un emulador secundario).
- El agente de QA leyó una vez por descuido la estructura de la pantalla de inicio del teléfono; ese contenido no se guardó en la evidencia.
- En la segunda pasada se descartó un indicador demasiado laxo de pantalla encendida; en la tercera se corrigió el lector del indicador de ventana (ambos anotados en sus informes).

## 10. Ausencia de identificadores

Búsqueda de identificadores de dispositivo en todo `qa/` (texto y metadatos de las capturas) con el mismo patrón en cada ejecución: números de serie de emulador, propiedades de producto, fabricantes y marcas, modelos y puertos. Se excluyen las palabras «vivo» y «oppo» del patrón porque «vivo» es una palabra española («en vivo») y daría coincidencias falsas.

```
grep -rniE -a 'emulator-[0-9]{4}|ro\.product|ro\.serialno|ro\.build|\b([p]ixel|[s]amsung|[x]iaomi|[m]otorola|[o]neplus|[g]alaxy|[h]uawei|[r]edmi|[r]ealme)\b|\bSM-[A-Z][0-9]{3,4}|localhost:[0-9]{4}|\b[5]554\b|\b[5]556\b' qa/
```

Resultado: sin coincidencias (ejecutado el 2026-10-11 desde esta carpeta). Los corchetes de las marcas en el comando evitan que este documento coincida con su propio patrón; el mismo patrón sobre este documento tampoco da coincidencias.

Valores de `app/google-services.json`: los 7 valores de texto de 8 o más caracteres del archivo se compararon sin imprimirse y se contó dónde aparecen. Seis de ellos (número y identificador del proyecto, dirección de la base, bucket, identificador de la aplicación y clave de API) aparecen 0 veces en `qa/` y en este documento. El séptimo es el nombre del paquete de la app, que no es un secreto y aparece de forma natural en las evidencias.

## 11. Estado final de `tasks.md`

73 tareas, 71 marcadas. Sin marcar: 8.8 (comprobaciones manuales del usuario) y 8.9 (este documento, pendiente de 8.8).
