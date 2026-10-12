# Segunda pasada final de la batería instrumentada (tarea 8.2 / 10.3)

- Commit probado: `f59f264` (árbol de trabajo limpio), que incluye la acción «Volver al inicio».
- Fecha: 2026-10-11.
- Medio: emulador de referencia, API 35. Guarda de identidad (nombre del AVD, API >= 35, arranque completo) antes de cada operación.
- Compilación: `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`; ambos APK instalados con `install -r`; el APK de pruebas instalado tiene la misma suma que el recién construido y contiene `LocalBackToStartTest`.
- Comando por clase: `adb -s <emulador> shell am instrument -w -e class com.edu.unal.tictactoe.<Clase> com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner`.
- Tarea 8.1 sobre este commit: ver `gradle-8.1.txt` (390 pruebas JVM, 0 fallos, 0 errores, 0 omitidas; lint 0 errores y 11 advertencias).
- Inventario: 46 clases con `@Test` y 308 métodos `@Test` (sin `@Ignore`). Una sola pasada, una instrumentación por clase, sin exclusiones ni repeticiones.

| Clase | Declaradas | Ejecutadas | Resultado | Tiempo (runner) |
|---|---|---|---|---|
| `AboutDialogTest` | 3 | 3 | OK | 3.234 s |
| `AppNavigationBackTest` | 4 | 4 | OK | 3.682 s |
| `DifficultyDialogTest` | 6 | 6 | OK | 7.314 s |
| `ExampleInstrumentedTest` | 1 | 1 | OK | 0.003 s |
| `AndroidMoveSoundPlayerLoadingTest` | 1 | 1 | OK | 0.029 s |
| `FirebaseOnlineGameServiceLiveTest` | 23 | 23 | OK | 57.989 s |
| `FirebaseInitializationTest` | 1 | 1 | OK | 0.006 s |
| `GameBoardInteractionTest` | 7 | 7 | OK | 7.919 s |
| `GameBoardRenderingTest` | 4 | 4 | OK | 5.355 s |
| `GameBoardLayoutTest` | 4 | 4 | OK | 6.749 s |
| `GamePreferencesIsolationTest` | 2 | 2 | OK | 0.026 s |
| `GamePreferencesResidueWriterTest` | 1 | 1 | OK | 0.01 s |
| `GameScreenTest` | 12 | 12 | OK | 41.082 s |
| `GameViewModelSavedStateWiringTest` | 7 | 7 | OK | 246.477 s |
| `GameMenuTest` | 8 | 8 | OK | 10.487 s |
| `GameSessionSnapshotBundleTest` | 4 | 4 | OK | 0.017 s |
| `KeepScreenOnTest` | 4 | 4 | OK | 4.642 s |
| `LeaveMatchDialogTest` | 9 | 9 | OK | 8.671 s |
| `KeepScreenOnBackgroundTest` | 4 | 4 | OK | 37.951 s |
| `GameScreenSoundIntegrationTest` | 1 | 1 | OK | 5.34 s |
| `LocalGameRetentionTest` | 4 | 4 | OK | 14.006 s |
| `MainActivityLifecycleTest` | 4 | 4 | OK | 12.535 s |
| `LocalBackToStartTest` | 2 | 2 | OK | 4.993 s |
| `ImpossibleOpeningTimingTest` | 1 | 1 | OK | 0.094 s |
| `MainActivityPendingTurnOptionsTest` | 3 | 3 | OK | 6.879 s |
| `MainActivityLocalPauseTest` | 4 | 4 | OK | 18.595 s |
| `MainActivityNavigationStateTest` | 9 | 9 | OK | 147.482 s |
| `LauncherIconTest` | 2 | 2 | OK | 0.055 s |
| `OnlineDoesNotAffectLocalScoreTest` | 3 | 3 | OK | 4.743 s |
| `MainActivitySoundOptionsTest` | 1 | 1 | OK | 6.026 s |
| `MainActivityTest` | 7 | 7 | OK | 25.196 s |
| `MainActivityAccessibilityTest` | 4 | 4 | OK | 10.678 s |
| `OnlineMatchFlowTest` | 17 | 17 | OK | 23.012 s |
| `OnlineLobbyNavigationTest` | 16 | 16 | OK | 16.429 s |
| `OnlineLobbyRotationTest` | 2 | 2 | OK | 3.118 s |
| `MainActivityOptionsTest` | 8 | 8 | OK | 29.227 s |
| `OnlineMatchRotationTest` | 6 | 6 | OK | 10.683 s |
| `OnlineMatchScreenTest` | 26 | 26 | OK | 32.293 s |
| `SharedPreferencesNicknameStoreTest` | 7 | 7 | OK | 0.04 s |
| `NicknameDialogTest` | 12 | 12 | OK | 14.178 s |
| `TicTacToeScreenTest` | 17 | 17 | OK | 43.327 s |
| `OnlineLobbyScreenTest` | 20 | 20 | OK | 22.538 s |
| `StartScreenTest` | 10 | 10 | OK | 9.148 s |
| `OnlineMatchSoundIntegrationTest` | 5 | 5 | OK | 8.646 s |
| `WaitingForOpponentScreenTest` | 5 | 5 | OK | 3.857 s |
| `SharedPreferencesGamePreferencesTest` | 7 | 7 | OK | 0.084 s |
| **Total** | **308** | **308** | 46 clases OK, 0 fallos | 940 s de pared (15 min 40 s) |
