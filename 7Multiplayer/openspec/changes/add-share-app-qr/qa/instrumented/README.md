# Batería completa del cambio add-share-app-qr (tarea 5.2)

- Commit probado: `b033dca` (árbol de trabajo limpio).
- Fecha: 2026-10-11.
- Medio: emulador de referencia, API 35. Guarda de identidad (nombre del AVD, API >= 35, arranque completo) antes de cada operación.
- Condición: `local.properties` no contiene la clave `share.url` (solo `sdk.dir`), comprobado antes de empezar; el archivo no se modificó.
- Gradle (ver `gradle.txt`): `testDebugUnitTest lintDebug assembleDebug --rerun-tasks` en `BUILD SUCCESSFUL`; 400 pruebas JVM, 0 fallos, 0 errores, 0 omitidas; lint 0 errores y 11 advertencias. Dos `assembleDebug` seguidos: la segunda reutilizó la entrada de la caché de configuración.
- Compilación: `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`; ambos APK instalados con `install -r`; el APK de pruebas instalado tiene la misma suma que el recién construido y contiene `ShareAppDialogTest`.
- Comando por clase: `adb -s <emulador> shell am instrument -w -e class com.edu.unal.tictactoe.<Clase> com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner`.
- Inventario: 47 clases con `@Test` y 325 métodos `@Test` (sin `@Ignore`). Una sola pasada, una instrumentación por clase, sin exclusiones ni repeticiones.

| Clase | Declaradas | Ejecutadas | Resultado | Tiempo (runner) |
|---|---|---|---|---|
| `AboutDialogTest` | 3 | 3 | OK | 2.656 s |
| `AndroidMoveSoundPlayerLoadingTest` | 1 | 1 | OK | 0.04 s |
| `AppNavigationBackTest` | 4 | 4 | OK | 3.468 s |
| `ExampleInstrumentedTest` | 1 | 1 | OK | 0.002 s |
| `DifficultyDialogTest` | 6 | 6 | OK | 6.55 s |
| `FirebaseOnlineGameServiceLiveTest` | 23 | 23 | OK | 77.701 s |
| `FirebaseInitializationTest` | 1 | 1 | OK | 0.006 s |
| `GameBoardInteractionTest` | 7 | 7 | OK | 8.087 s |
| `GameBoardRenderingTest` | 4 | 4 | OK | 5.167 s |
| `GameMenuTest` | 8 | 8 | OK | 9.556 s |
| `GameBoardLayoutTest` | 4 | 4 | OK | 7.149 s |
| `GamePreferencesIsolationTest` | 2 | 2 | OK | 0.021 s |
| `GameScreenSoundIntegrationTest` | 1 | 1 | OK | 5.368 s |
| `GamePreferencesResidueWriterTest` | 1 | 1 | OK | 0.004 s |
| `GameViewModelSavedStateWiringTest` | 7 | 7 | OK | 245.064 s |
| `ImpossibleOpeningTimingTest` | 1 | 1 | OK | 0.094 s |
| `GameScreenTest` | 12 | 12 | OK | 40.281 s |
| `GameSessionSnapshotBundleTest` | 4 | 4 | OK | 0.023 s |
| `KeepScreenOnTest` | 4 | 4 | OK | 4.819 s |
| `LauncherIconTest` | 2 | 2 | OK | 0.057 s |
| `KeepScreenOnBackgroundTest` | 4 | 4 | OK | 37.516 s |
| `LocalGameRetentionTest` | 4 | 4 | OK | 14.117 s |
| `MainActivityAccessibilityTest` | 4 | 4 | OK | 10.805 s |
| `LeaveMatchDialogTest` | 9 | 9 | OK | 8.55 s |
| `LocalBackToStartTest` | 2 | 2 | OK | 4.875 s |
| `MainActivityNavigationStateTest` | 9 | 9 | OK | 146.771 s |
| `MainActivityLifecycleTest` | 4 | 4 | OK | 12.586 s |
| `MainActivityOptionsTest` | 8 | 8 | OK | 29.235 s |
| `MainActivityLocalPauseTest` | 4 | 4 | OK | 18.824 s |
| `MainActivityTest` | 7 | 7 | OK | 27.205 s |
| `MainActivityPendingTurnOptionsTest` | 3 | 3 | OK | 6.941 s |
| `MainActivitySoundOptionsTest` | 1 | 1 | OK | 6.044 s |
| `NicknameDialogTest` | 12 | 12 | OK | 14.018 s |
| `OnlineLobbyRotationTest` | 3 | 3 | OK | 4.849 s |
| `OnlineDoesNotAffectLocalScoreTest` | 3 | 3 | OK | 4.724 s |
| `OnlineLobbyNavigationTest` | 20 | 20 | OK | 20.274 s |
| `OnlineLobbyScreenTest` | 25 | 25 | OK | 26.675 s |
| `OnlineMatchScreenTest` | 26 | 26 | OK | 30.348 s |
| `OnlineMatchFlowTest` | 17 | 17 | OK | 22.648 s |
| `OnlineMatchRotationTest` | 6 | 6 | OK | 10.745 s |
| `OnlineMatchSoundIntegrationTest` | 5 | 5 | OK | 9.158 s |
| `ShareAppDialogTest` | 7 | 7 | OK | 6.237 s |
| `SharedPreferencesNicknameStoreTest` | 7 | 7 | OK | 0.031 s |
| `SharedPreferencesGamePreferencesTest` | 7 | 7 | OK | 0.084 s |
| `WaitingForOpponentScreenTest` | 5 | 5 | OK | 3.925 s |
| `StartScreenTest` | 10 | 10 | OK | 8.011 s |
| `TicTacToeScreenTest` | 17 | 17 | OK | 43.445 s |
| **Total** | **325** | **325** | 47 clases OK, 0 fallos, 0 omitidas | 968 s de pared (16 min 8 s) |
