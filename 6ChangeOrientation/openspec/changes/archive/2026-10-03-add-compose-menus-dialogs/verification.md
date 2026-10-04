# Registro de verificación

## Estado de esta revisión

- Progreso OpenSpec: **30/30 tareas**.
- 6.1, 6.2, 6.3 y 6.4 quedan verificadas; no hay tareas pendientes.
- Todo lo registrado proviene de corridas y observaciones reales; no hay placeholders.

## Build local, unitarias y lint (6.1)

- Comando exacto: `./gradlew testDebugUnitTest lintDebug assembleDebug`.
- Resultado: exit 0.
- Unitarias JVM: 83 pruebas, 0 fallos, 0 errores, 0 omitidas.
  - `TicTacToeGameTest` 24, `GameViewModelTest` 15, `GameViewModelDifficultyTest` 12,
    `ImpossiblePolicyTest` 6, `GameUiStateDifficultyTest` 6, `DifficultyPolicyTest` 5,
    `GameUiStateTest` 5, `GameModelContractsTest` 4, `DifficultyLabelsTest` 3,
    `ImpossibleExhaustiveTest` 2, `ExampleUnitTest` 1.
- Lint: 0 errores y 13 advertencias, todas heredadas de la plantilla:
  7 `UnusedResources`, 4 de versiones (`AndroidGradlePluginVersion`,
  `GradleDependency` x2, `NewerVersionAvailable`), 1 `RedundantLabel` y
  1 `ObsoleteSdkInt`.
- No se actualizaron SDK ni dependencias para ocultar advertencias.

## Suite instrumental (6.2)

- Comando exacto: `ANDROID_SERIAL=emulator-5580 ./gradlew connectedDebugAndroidTest`.
- Resultado: exit 0; 59 pruebas, 0 fallos, 0 errores, 0 omitidas.
- Emulador: AVD `opencode-reto4-api36`, Android 16/API 36, x86_64, KVM con GPU
  software, 2 vCPU, 3072 MiB y pantalla 1080x2400 a 420 dpi.
- Cobertura real: menú y diálogos (dificultad, salida, Acerca de), gate de
  dificultad vacío/terminal vs partida activa, "Sí" finaliza y "No" conserva,
  rotación/background y sesión nueva, cuatro labels, accesibilidad en
  vertical/landscape con objetivo táctil de 48 dp con tolerancia de 1 dp por
  redondeo en la prueba, e iconos norm/round/monochrome propios con renderizado
  nativo de sus recursos.
- Antes de esta corrida se ejecutaron 46 pruebas en un teléfono físico API 35;
  no se incluyen serial, UUID, modelo ni datos del dispositivo.

## 6.4: latencia de apertura de O con Impossible (observada)

- Origen: log real `Reto4Timing` de la última corrida instrumental en API 36.
- Muestra: `sdk=36 samples_ns=[119041830,43134706,46410117,20566592,17917870]`.
- Derivados: `first_ms=119.042`, `median_ms=43.135`.
- Operación medida: `GameViewModel.newGame()` con nivel `Impossible` desde tablero
  vacío (apertura de O), sobre el hilo principal.
- La prueba comprueba en cada muestra una única jugada de O (O en el índice 0 y
  8 casillas vacías) con la aserción posterior al cronómetro; no mide latencia
  de primer frame ni garantiza arranque en frío, no fija umbral y no es un
  benchmark de rendimiento. No se afirma ninguna mejora que no se haya medido.

## 6.3: Toast e identidad del launcher (revisión visual observada)

- En el emulador se observaron tres capturas reales:
  - Toast nativo mostrando `Imposible` con el icono propio de la app.
  - Selector de dificultad con los cuatro radios y solo `Imposible` seleccionado.
  - Diálogo Acerca de con la imagen propia y el autor exacto
    `Juan Sebastian Rodriguez`.
- También se revisaron tres previews nativos (normal, round y máscara
  monocromática) y el fallback PNG/WebP del launcher.
- Las previews nativas corresponden a los `Drawable` del launcher normal, round y
  máscara monocromática; no se activó la opción de iconos temáticos del Launcher
  ni se cambiaron ajustes del teléfono.
- Las capturas se guardaron temporalmente durante la revisión y no se incluyen en Git;
  no se añaden identificadores del dispositivo físico.

## Notas

- No se modificó la configuración del teléfono, ni las dependencias de la app, ni
  el AVD preexistente; se creó un AVD dedicado y se activó StayOn solo en ese
  emulador de pruebas. El paquete de imagen de API 36 ya existía, sin descargas
  nuevas.
- El cambio OpenSpec sigue sin archivar.
