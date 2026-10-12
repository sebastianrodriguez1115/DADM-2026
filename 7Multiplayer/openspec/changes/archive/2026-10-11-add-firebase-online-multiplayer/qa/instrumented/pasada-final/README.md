# Pasada final de la batería instrumentada (tarea 8.2)

- Commit probado: `5d26dfb` (árbol de trabajo limpio).
- Fecha: 2026-10-11.
- Medio: emulador de referencia, API 35. Guarda de identidad (nombre del AVD, API >= 35, arranque completo) antes de cada operación.
- Compilación: `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`; ambos APK instalados con `install -r`; el APK de pruebas instalado tiene la misma suma que el recién construido.
- Comando por clase: `adb -s <emulador> shell am instrument -w -e class com.edu.unal.tictactoe.<Clase> com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner`.
- Inventario: 45 clases con `@Test` y 297 métodos `@Test` (sin `@Ignore`). Una sola pasada, una instrumentación por clase, sin exclusiones ni repeticiones.

| Clase | Declaradas | Ejecutadas | Resultado | Tiempo (runner) |
|---|---|---|---|---|
| `AboutDialogTest` | 3 | 3 | OK | 3.333 s |
| `AppNavigationBackTest` | 4 | 4 | OK | 4.334 s |
| `ExampleInstrumentedTest` | 1 | 1 | OK | 0.002 s |
| `AndroidMoveSoundPlayerLoadingTest` | 1 | 1 | OK | 0.033 s |
| `DifficultyDialogTest` | 6 | 6 | OK | 6.838 s |
| `FirebaseOnlineGameServiceLiveTest` | 23 | 23 | OK | 67.069 s |
| `FirebaseInitializationTest` | 1 | 1 | OK | 0.003 s |
| `GameBoardInteractionTest` | 7 | 7 | OK | 8.374 s |
| `GameBoardRenderingTest` | 4 | 4 | OK | 5.09 s |
| `GamePreferencesIsolationTest` | 2 | 2 | OK | 0.026 s |
| `GameMenuTest` | 6 | 6 | OK | 7.654 s |
| `GameBoardLayoutTest` | 4 | 4 | OK | 6.696 s |
| `GameViewModelSavedStateWiringTest` | 7 | 7 | OK | 245.825 s |
| `GameScreenSoundIntegrationTest` | 1 | 1 | OK | 5.305 s |
| `GameScreenTest` | 11 | 11 | OK | 36.982 s |
| `GamePreferencesResidueWriterTest` | 1 | 1 | OK | 0.01 s |
| `KeepScreenOnTest` | 4 | 4 | OK | 4.685 s |
| `ImpossibleOpeningTimingTest` | 1 | 1 | OK | 0.096 s |
| `GameSessionSnapshotBundleTest` | 4 | 4 | OK | 0.023 s |
| `MainActivityAccessibilityTest` | 4 | 4 | OK | 10.46 s |
| `LauncherIconTest` | 2 | 2 | OK | 0.06 s |
| `LeaveMatchDialogTest` | 9 | 9 | OK | 8.413 s |
| `KeepScreenOnBackgroundTest` | 4 | 4 | OK | 38.552 s |
| `MainActivityLifecycleTest` | 4 | 4 | OK | 12.372 s |
| `MainActivityOptionsTest` | 8 | 8 | OK | 29.361 s |
| `MainActivityLocalPauseTest` | 4 | 4 | OK | 18.673 s |
| `LocalGameRetentionTest` | 4 | 4 | OK | 13.068 s |
| `MainActivityPendingTurnOptionsTest` | 3 | 3 | OK | 7.214 s |
| `NicknameDialogTest` | 12 | 12 | OK | 13.344 s |
| `MainActivitySoundOptionsTest` | 1 | 1 | OK | 6.025 s |
| `MainActivityNavigationStateTest` | 9 | 9 | OK | 146.976 s |
| `OnlineDoesNotAffectLocalScoreTest` | 3 | 3 | OK | 4.757 s |
| `OnlineLobbyScreenTest` | 16 | 16 | OK | 15.876 s |
| `OnlineLobbyNavigationTest` | 14 | 14 | OK | 14.076 s |
| `MainActivityTest` | 7 | 7 | OK | 23.44 s |
| `OnlineMatchFlowTest` | 17 | 17 | OK | 21.779 s |
| `OnlineMatchSoundIntegrationTest` | 5 | 5 | OK | 9.118 s |
| `OnlineMatchRotationTest` | 6 | 6 | OK | 11.07 s |
| `OnlineLobbyRotationTest` | 2 | 2 | OK | 3.57 s |
| `SharedPreferencesGamePreferencesTest` | 7 | 7 | OK | 0.286 s |
| `OnlineMatchScreenTest` | 26 | 26 | OK | 32.402 s |
| `SharedPreferencesNicknameStoreTest` | 7 | 7 | OK | 0.055 s |
| `WaitingForOpponentScreenTest` | 5 | 5 | OK | 3.849 s |
| `StartScreenTest` | 10 | 10 | OK | 8.305 s |
| `TicTacToeScreenTest` | 17 | 17 | OK | 43.731 s |
| **Total** | **297** | **297** | 45 clases OK, 0 fallos, 0 omitidas | 925 s de pared (15 min 25 s) |
