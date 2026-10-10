# Tasks

> **Contexto de integración OpenSpec (no ejecutable en este plan):** la base de `game-options` de `add-compose-menus-dialogs` ya fue sincronizada y archivada. La integración de `add-compose-graphics-sound`, incluidos sus deltas de sesión/audio y QA, es independiente: estas 33 tareas no la modifican, archivan ni declaran completada.

## 1. Preferencias durables de marcador y dificultad

- [x] 1.1 Definir una interfaz mínima fakeable para leer/escribir marcador y dificultad, con códigos explícitos Easy=0, Hard=1, Expert=2, Impossible=3; verificar códigos, defaults y aislamiento del dominio Android con pruebas unitarias.
- [x] 1.2 Aislar SharedPreferences en tests Android mediante preparación/limpieza por test y seeds explícitos antes de conectar el store al ViewModel; verificar que los tests existentes no dependen del orden ni de scores residuales.
- [x] 1.3 Implementar el adaptador SharedPreferences con application context, `apply()`, lectura defensiva por clave y normalización individual de valores ausentes, tipo incorrecto o negativos; verificar con pruebas Android que valores válidos sobreviven a entradas corruptas de otros contadores.
- [x] 1.4 Inyectar el store al GameViewModel sin exponer Context y cargar scores/dificultad al crear sesión; verificar defaults Expert/ceros, persistencia y restauración desde store fake en pruebas JVM.
- [x] 1.5 Persistir dificultad solo tras selección aceptada, manteniendo reglas de tablero vacío/apertura O pendiente/terminal y rechazando juego activo con marcas y solicitudes obsoletas; verificar aceptación, rechazo sin escritura/Toast y conservación del timer en `GameViewModelDifficultyTest`.
- [x] 1.6 Persistir una única vez el contador apropiado en la transición InProgress→terminal sin alterar IA ni alternancia; verificar X/O/empate, partida incompleta y ausencia de duplicados al repetir/restaurar terminal en `GameViewModelTest`.
- [x] 1.7 Implementar reset inmediato de solo los tres scores y persistir ceros sin reiniciar ni republicar desenlace; verificar estado activo y terminal conservan tablero, turno, resultado, iniciadores, dificultad, sonido y espera en pruebas del ViewModel.
- [x] 1.8 Documentar en el adaptador o documentación próxima que `apply()` actualiza memoria y difiere flush, sin garantía ante terminación abrupta; verificar que no se promete durabilidad de power-kill ni se usa escritura síncrona en UI.

## 2. Snapshot temporal y restauración de sesión

- [x] 2.1 Añadir restauración acotada de marcas validadas al motor sin disparar efectos de jugada; verificar tableros legales y rechazo de combinaciones ilegales en `TicTacToeGameTest`.
- [x] 2.2 Implementar codec de snapshot versionado Bundle-compatible con board, turno, resultado, iniciador actual/siguiente, soundEnabled y restante opcional de O; verificar round-trip y copia defensiva, y que no incluya score, dificultad, Job, Context ni timestamp absoluto.
- [x] 2.3 Validar snapshot completo antes de mutar estado y descartar atómicamente versiones/códigos/estados/restantes incompatibles; verificar cada clase de corrupción cae a tablero vacío/X/sound true con preferencias vigentes.
- [x] 2.4 Restaurar tablero activo y terminal sin recorrer jugada, puntuar ni emitir audio; verificar que prefs son la única fuente de score/dificultad y que un terminal no vuelve a sumar.
- [x] 2.5 Mantener el snapshot actualizado tras jugada, nueva partida, toggle de sonido y cambio relevante; verificar snapshots de transiciones y sesión nueva X con sonido true sin reanudar estado cerrado.
- [x] 2.6 Conectar SavedStateHandle real mediante Factory/CreationExtras y SavedStateRegistry de MainActivity, conservando seams para VM tests; verificar creación de producción con handle provisto por registry y ausencia de handle manual desconectado.
- [x] 2.7 Añadir prueba de wiring de producción con SavedStateRegistry real, factory y ViewModel nuevo/ViewModelStore nuevo; verificar round-trip de snapshot válido mediante SavedStateHandle creado por el framework, sin usar `scenario.recreate()` como sustituto de muerte de proceso.
- [x] 2.8 Capturar en onPause restante monotónico de O una sola vez antes del guardado framework, actualizar handle y suspender foreground; verificar idempotencia, pausa 400 ms más 600 ms y que no se descuenta dos veces.
- [x] 2.9 Restaurar espera O como estado lógico sin Job ni reloj absoluto y programarla una sola vez al RESUMED, incluso con restante cero; verificar 999/1000 ms, pausa, cancelación por newGame y ningún movimiento durante background con el harness virtual.
- [x] 2.10 Mantener sonido solo en snapshot temporal y efectos solo para nueva jugada O legítima; verificar mute restaurado, ningún replay al restaurar, sonido único tras movimiento futuro y `soundEnabled=true` en snapshot ausente/inválido.
- [x] 2.11 Probar el adaptador Android de preferencias y Bundle/snapshot con infraestructura Android real, no Bundle stub JVM; verificar códigos estables, fallback Expert y normalización independiente de scores.
- [x] 2.12 Documentar mediante KDoc o documentación próxima el formato/versionado del snapshot, captura/restauración lifecycle y frontera entre estado temporal y datos permanentes; verificar que explicita campos excluidos, fallback inválido, límites de restauración framework y ausencia de garantía ante force-stop.

## 3. Menú, reset y opciones

- [x] 3.1 Sustituir las entradas del menú por exactamente Nueva partida, Dificultad, Sonido, Reiniciar puntajes y Acerca de, cada una con icono y etiqueta accesible; verificar orden/contenido y cierre del menú antes de acción en `GameMenuTest`.
- [x] 3.2 Conectar Reiniciar puntajes al reset no destructivo del ViewModel y recursos accesibles; verificar acción visible/etiquetada, ceros inmediatos y conservación de partida activa/terminal en pruebas Compose.
- [x] 3.3 Retirar Salir, ExitConfirmationDialog y recursos/tests exclusivamente asociados sin cambiar Back ni overlays restantes; verificar ausencia de salida en `MainActivityOptionsTest` y Back normal sin overlay.
- [x] 3.4 Preservar selección de dificultad persistente, reglas de apertura O y notificación única sin reiniciar espera; verificar menú/dialog, cuatro niveles, selección vigente y rechazo accesible en tests de opciones y apertura pendiente.
- [x] 3.5 Añadir o actualizar documentación de opciones y límite de preferencias/sesión si existe documentación próxima; verificar que describe menú de cinco entradas y no presenta la partida como almacenamiento permanente.

## 4. Layout Compose adaptable y sin barra superior

- [x] 4.1 Retirar TopAppBar, título y reserva de barra en portrait y landscape, conservando insets del Scaffold y menú compacto junto a controles; verificar ausencia de barra/título y menú en ambas orientaciones con GameScreen/MainActivity instrumentada.
- [x] 4.2 Implementar layout landscape con tablero cuadrado a la izquierda y estado, marcador y controles a la derecha, tomando 270 dp como máximo de referencia sujeto a constraints; verificar posición, cuadratura y ajuste en `GameBoardLayoutTest`.
- [x] 4.3 Mantener layout portrait funcional y fallback compacto/desplazable para ancho, alto, insets y fontScale extremos sin alturas textuales fijas; verificar alcanzabilidad del tablero y controles en tamaños compactos.
- [x] 4.4 Conservar targets táctiles mínimos de 48 dp, semantics/tags y texto escalable en ambas orientaciones; verificar dimensiones y etiquetas mediante `MainActivityAccessibilityTest` y `GameBoardRenderingTest`.
- [x] 4.5 Actualizar documentación de layout cercano para reglas portrait/landscape, insets y degradación compacta; verificar que no exige tamaño 270 dp cuando las constraints no lo permiten.

## 5. Integración transversal y aceptación

- [x] 5.1 Ejecutar suite JVM e inspección estática/build `./gradlew testDebugUnitTest lintDebug assembleDebug`; verificar regresión integrada de IA Easy/Hard/Expert/Impossible, reglas, alternancia, preferencias, lifecycle y audio.
- [x] 5.2 Construir APKs con `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` y ejecutar AndroidJUnitRunner por ADB dirigido a un único destino cuya identidad se verifique inmediatamente antes de cada instalación/instrumentación/force-stop/operación. Destino preferido: el dispositivo físico del usuario cuando esté conectado (serial, modelo, API ≥ 35, ABI y boot completo). Si no hay dispositivo físico, continuar sin bloquear sobre el emulador de referencia (AVD `DADM_GraphicsSound_API35`, API 35, ABI `arm64-v8a`, boot completo; el serial histórico `emulator-5556` no es prueba), que puede iniciarse sin aprobación previa conservando sus datos. Consultar solo si no hay ningún destino utilizable o si haría falta crear un AVD o descargar una imagen; no operar dispositivos ni emuladores distintos del destino elegido y registrar en la evidencia cuál se usó. Revisar flujos end-to-end de menú, portrait/landscape, accesibilidad automatizable, persistencia y restauración registry; revisar inventario íntegro de clases/métodos sin exclusiones, conservar XML/log y registrar resultado verificable de cobertura completa. APK selection no garantiza cobertura; sound/TalkBack manuales no se automatizan ni se inventa evidencia. No usar tareas Gradle `connected*`, `adb kill-server`, `adb disconnect` ni `-wipe-data`.
- [x] 5.3 Tras aprobar code review, solicitar QA al agente QA sobre el destino Android nativo con la preferencia de 5.2 (dispositivo físico si está conectado; si no, emulador) (no Playwright/Postman), validar flujos y evidencia capturas/logs únicamente en `openspec/changes/add-compose-orientation-state/qa/`; no aprobar ni afirmar QA completada si faltan dispositivo o evidencia.
