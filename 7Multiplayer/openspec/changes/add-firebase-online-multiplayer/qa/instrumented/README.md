> Nota: la pasada válida es la de `pasada-final/`; esta carpeta se conserva como registro del fallo (ya corregido) de `FirebaseOnlineGameServiceLiveTest`.

# Batería instrumentada completa (tarea 8.2)

- Fecha: 2026-10-11 (pasada completa iniciada a las 10:31, hora local).
- Destino: emulador de referencia, API 35 (arranque limpio sin snapshot, sin `-wipe-data`, en modo `-no-window`).
- Versión de la app probada: `86be80a` (HEAD, árbol de trabajo limpio salvo esta evidencia).
- Guarda de identidad antes de cada operación de adb: `emu avd name` = `DADM_GraphicsSound_API35`, API >= 35, `sys.boot_completed` = 1. Todas las operaciones usaron `adb -s <emulador>`.
- Internet del emulador comprobado antes de empezar (resolución y respuesta de `firebase.google.com` desde el emulador).

## Comandos

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s <emulador> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <emulador> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# una instrumentación por clase, las 43 del inventario, sin exclusiones:
adb -s <emulador> shell am instrument -w -e class com.edu.unal.tictactoe.<Clase> com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner
```

## Resultado

Inventario: 43 clases con `@Test` en `app/src/androidTest/java/com/edu/unal/tictactoe/`, 277 métodos `@Test` (cuenta por líneas `@Test`; ninguna clase usa `@Ignore` ni parametrización). Ejecutadas las 43, 277 pruebas, 276 pasan y 1 falla en la primera pasada; 0 omitidas. Tiempo total de la batería: 849 s (14 min 9 s) de pared, más 38 s de la repetición de una clase.

| Clase | Declaradas | Ejecutadas | Resultado | Tiempo (runner) | Ejecutadas = declaradas |
|---|---|---|---|---|---|
| `AboutDialogTest` | 3 | 3 | OK | 3.051 s | sí |
| `AppNavigationBackTest` | 4 | 4 | OK | 4.792 s | sí |
| `ExampleInstrumentedTest` | 1 | 1 | OK | 0.007 s | sí |
| `AndroidMoveSoundPlayerLoadingTest` | 1 | 1 | OK | 0.035 s | sí |
| `DifficultyDialogTest` | 6 | 6 | OK | 6.717 s | sí |
| `FirebaseOnlineGameServiceLiveTest` | 17 | 17 | FALLA (1 fallo) (ver nota 1) | 40.395 s | sí |
| `FirebaseInitializationTest` | 1 | 1 | OK | 0.004 s | sí |
| `GameBoardInteractionTest` | 7 | 7 | OK | 8.759 s | sí |
| `GameBoardRenderingTest` | 4 | 4 | OK | 5.268 s | sí |
| `GameBoardLayoutTest` | 4 | 4 | OK | 6.74 s | sí |
| `GamePreferencesIsolationTest` | 2 | 2 | OK | 0.036 s | sí |
| `GameMenuTest` | 6 | 6 | OK | 7.96 s | sí |
| `GamePreferencesResidueWriterTest` | 1 | 1 | OK | 0.007 s | sí |
| `GameScreenTest` | 11 | 11 | OK | 37.336 s | sí |
| `GameScreenSoundIntegrationTest` | 1 | 1 | OK | 5.264 s | sí |
| `GameViewModelSavedStateWiringTest` | 7 | 7 | OK | 245.673 s | sí |
| `GameSessionSnapshotBundleTest` | 4 | 4 | OK | 0.024 s | sí |
| `ImpossibleOpeningTimingTest` | 1 | 1 | OK | 0.083 s | sí |
| `LeaveMatchDialogTest` | 9 | 9 | OK | 9.191 s | sí |
| `LauncherIconTest` | 2 | 2 | OK | 0.054 s | sí |
| `MainActivityAccessibilityTest` | 4 | 4 | OK | 10.462 s | sí |
| `LocalGameRetentionTest` | 4 | 4 | OK | 13.883 s | sí |
| `MainActivityLocalPauseTest` | 4 | 4 | OK | 18.464 s | sí |
| `MainActivityLifecycleTest` | 4 | 4 | OK | 13.163 s | sí |
| `MainActivityOptionsTest` | 8 | 8 | OK | 29.011 s | sí |
| `MainActivityNavigationStateTest` | 9 | 9 | OK | 147.611 s | sí |
| `MainActivityPendingTurnOptionsTest` | 3 | 3 | OK | 6.836 s | sí |
| `MainActivitySoundOptionsTest` | 1 | 1 | OK | 5.955 s | sí |
| `NicknameDialogTest` | 12 | 12 | OK | 14.002 s | sí |
| `MainActivityTest` | 7 | 7 | OK | 24.54 s | sí |
| `OnlineDoesNotAffectLocalScoreTest` | 3 | 3 | OK | 4.671 s | sí |
| `OnlineLobbyNavigationTest` | 14 | 14 | OK | 13.929 s | sí |
| `OnlineLobbyScreenTest` | 16 | 16 | OK | 15.471 s | sí |
| `OnlineLobbyRotationTest` | 2 | 2 | OK | 3.061 s | sí |
| `OnlineMatchFlowTest` | 14 | 14 | OK | 19.084 s | sí |
| `OnlineMatchRotationTest` | 5 | 5 | OK | 9.357 s | sí |
| `OnlineMatchSoundIntegrationTest` | 5 | 5 | OK | 9.176 s | sí |
| `OnlineMatchScreenTest` | 24 | 24 | OK | 29.105 s | sí |
| `SharedPreferencesGamePreferencesTest` | 7 | 7 | OK | 0.081 s | sí |
| `SharedPreferencesNicknameStoreTest` | 7 | 7 | OK | 0.043 s | sí |
| `StartScreenTest` | 10 | 10 | OK | 8.883 s | sí |
| `WaitingForOpponentScreenTest` | 5 | 5 | OK | 4.28 s | sí |
| `TicTacToeScreenTest` | 17 | 17 | OK | 42.277 s | sí |
| **Total** | **277** | **277** | 276 OK + 1 fallo en primera pasada | 849 s de pared (batería) | |

## Nota 1: `FirebaseOnlineGameServiceLiveTest` inestable

- Primera pasada (`FirebaseOnlineGameServiceLiveTest.run1.txt`): 17 ejecutadas, 1 fallo, `rulesLetAThirdIdentityReadButNotWriteOrDeleteAGameInProgress`:
  `java.util.NoSuchElementException: Expected at least one element matching the predicate` en `awaitGame` (FirebaseOnlineGameServiceLiveTest.kt:299, la primera lectura de la tercera identidad).
- Repetición única (`FirebaseOnlineGameServiceLiveTest.run2.txt`): `OK (17 tests)`. La clase es, por tanto, inestable (1 de 2).
- Diagnóstico (no se modificó nada): `observeGame` termina con `onCancelled` (emite nulo y cierra) si el servidor deniega la lectura. En ese método la identidad `live-third` nunca se firma antes de observar (no se toca `third.uid`, a diferencia de `onlyOneOfTwoSimultaneousGuestsJoins`), así que el oyente puede adjuntarse antes de que termine el inicio de sesión anónimo y recibir permiso denegado, tras lo cual el flujo termina sin un juego no nulo. Parece una carrera de la prueba (falta de espera de la identidad), no un defecto de producción ni del entorno de red; es una hipótesis, no se reprodujo ni se instrumentó.

## Incidencias de entorno durante la ejecución

Una pasada previa quedó incompleta porque el emulador se cerró a mitad de la batería (`error: closed` en `MainActivitySoundOptionsTest`); el emulador se reinició sin borrar datos y, al restaurarse un estado antiguo, tenía instalado un APK de pruebas desactualizado (clases `Online*` no encontradas). Esa pasada se descartó y no se conserva ninguna de sus salidas; la evidencia de esta carpeta proviene de una sola pasada completa posterior, tras reinstalar ambos APK. Un corte posterior de la sesión obligó a repetir la batería desde cero (sin cambios de código).

## Conclusión

8.2 **no** se marca: hay un fallo (inestable) en la primera pasada. Cobertura del inventario completa y pruebas ejecutadas = declaradas en las 43 clases.
