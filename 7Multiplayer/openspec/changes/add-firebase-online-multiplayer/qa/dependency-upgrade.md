# Subida de dependencias y wrapper (tarea 1.8)

Decisión del usuario del 2026-10-10: usar las últimas versiones estables mientras el proyecto está en desarrollo. Fecha de las consultas: 2026-10-10. Solo se consideraron versiones estables (sin alfa, beta, RC ni desarrollo).

## Versiones

| Entrada | Antes | Después | Fuente |
|---|---|---|---|
| Compose BOM | 2026.02.01 | 2026.09.00 | https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml |
| `androidx.core:core-ktx` | 1.19.0 | 1.19.1 | https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/maven-metadata.xml |
| `kotlinx-coroutines-test` | 1.9.0 | 1.11.0 | https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml |
| Gradle (wrapper) | 9.6.0 | 9.8.1 | https://services.gradle.org/versions/current (9.8.1, no snapshot); suma `dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f` de https://services.gradle.org/distributions/gradle-9.8.1-bin.zip.sha256, coincide con https://gradle.org/release-checksums/ |

## Sin cambio (ya en la última estable)

| Entrada | Versión | Fuente |
|---|---|---|
| AGP | 9.4.1 (hay 9.5.0-alpha, no estable) | https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml |
| Kotlin | 2.4.21 (la serie 2.4 termina ahí; 2.5.0-Beta1 no es estable) | https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml |
| Lifecycle (runtime-ktx, viewmodel-compose, runtime-compose) | 2.11.0 | https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx/maven-metadata.xml |
| activity-compose | 1.13.0 | https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/maven-metadata.xml |
| junit | 4.13.2 | https://repo1.maven.org/maven2/junit/junit/maven-metadata.xml |
| androidx.test.ext:junit | 1.3.0 | https://dl.google.com/dl/android/maven2/androidx/test/ext/junit/maven-metadata.xml |
| espresso-core | 3.7.0 | https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/maven-metadata.xml |
| Firebase BoM | 35.0.0 | https://dl.google.com/dl/android/maven2/com/google/firebase/firebase-bom/maven-metadata.xml |
| Plugin `google-services` | 4.5.0 | https://dl.google.com/dl/android/maven2/com/google/gms/google-services/maven-metadata.xml |

Nota sobre el wrapper: lint sugería 9.8.0, pero la última estable publicada es 9.8.1. Las notas de AGP (https://developer.android.com/build/releases/about-agp) indican que AGP 9.4 exige como mínimo Gradle 9.6.0; 9.8.1 lo cumple. `gradle/gradle-daemon-jvm.properties` (JDK 25) no se tocó. La tarea `wrapper` se ejecutó dos veces; además de la versión y la suma, la tarea añadió `retries=0` y `retryBackOffMs=500` en `gradle-wrapper.properties`, y actualizó `gradle-wrapper.jar`, `gradlew` y `gradlew.bat`. No se revirtió ninguna subida.

## Comandos y resultados

| Paso | Comando | Resultado |
|---|---|---|
| Línea base | `./gradlew testDebugUnitTest lintDebug assembleDebug` | BUILD SUCCESSFUL |
| (a) Compose BOM y core-ktx | `./gradlew testDebugUnitTest lintDebug assembleDebug` | BUILD SUCCESSFUL |
| (b) coroutines-test | `./gradlew testDebugUnitTest lintDebug assembleDebug` | BUILD SUCCESSFUL |
| (e) Gradle 9.8.1 | `./gradlew wrapper --gradle-version 9.8.1 --gradle-distribution-sha256-sum <suma>` (x2), luego `./gradlew testDebugUnitTest lintDebug assembleDebug` | BUILD SUCCESSFUL |
| Completo | `./gradlew testDebugUnitTest lintDebug assembleDebug --rerun-tasks` | BUILD SUCCESSFUL; 324 pruebas JVM, 0 fallos, 0 omitidas |
| Caché | `./gradlew assembleDebug` dos veces | primera "Configuration cache entry stored", segunda "Configuration cache entry reused" |
| Lint | `lintDebug` | 0 errores, 11 advertencias (`UnusedResources` 7, `UseKtx` 2, `ObsoleteSdkInt` 1, `RedundantLabel` 1); desaparecieron las de versiones atrasadas |

Aviso de Gradle: "Deprecated Gradle features were used in this build, making it incompatible with Gradle 10"; con `--warning-mode all` y la caché reutilizada no se listó el origen. Sin advertencias nuevas del compilador de Kotlin.

## Batería instrumentada (dispositivo físico, API 35)

Instalación de `app-debug.apk` y `app-debug-androidTest.apk` con `adb install -r`; una instrumentación por clase (`am instrument -w -e class ...`), con la identidad del destino verificada antes de cada operación. Tiempo total de las 34 clases: 742 s.

| Clase | Pruebas | Resultado |
|---|---|---|
| AboutDialogTest | 3 | OK |
| AndroidMoveSoundPlayerLoadingTest | 1 | OK |
| ExampleInstrumentedTest | 1 | OK |
| AppNavigationBackTest | 4 | OK |
| DifficultyDialogTest | 6 | OK |
| GameBoardInteractionTest | 5 | OK |
| FirebaseInitializationTest | 1 | OK |
| GamePreferencesIsolationTest | 2 | OK |
| GameMenuTest | 6 | OK |
| GameBoardLayoutTest | 4 | OK |
| GameBoardRenderingTest | 4 | OK |
| GameScreenTest | 11 | OK |
| GamePreferencesResidueWriterTest | 1 | OK |
| GameScreenSoundIntegrationTest | 1 | OK |
| GameSessionSnapshotBundleTest | 4 | OK |
| ImpossibleOpeningTimingTest | 1 | OK |
| MainActivityLifecycleTest | 4 | OK |
| GameViewModelSavedStateWiringTest | 7 | OK |
| LauncherIconTest | 2 | OK |
| MainActivityAccessibilityTest | 4 | OK |
| MainActivityPendingTurnOptionsTest | 3 | OK |
| LocalGameRetentionTest | 4 | OK |
| MainActivityLocalPauseTest | 4 | OK |
| MainActivityOptionsTest | 8 | OK |
| OnlineLobbyScreenTest | 16 | OK |
| MainActivityNavigationStateTest | 9 | OK |
| MainActivitySoundOptionsTest | 1 | OK |
| NicknameDialogTest | 12 | **FALLA 1** |
| SharedPreferencesNicknameStoreTest | 7 | OK |
| MainActivityTest | 7 | OK |
| SharedPreferencesGamePreferencesTest | 7 | OK |
| TicTacToeScreenTest | 17 | OK |
| StartScreenTest | 10 | OK |
| WaitingForOpponentScreenTest | 5 | OK |

### Fallo: `NicknameDialogTest.backInvokesTheDismissCallback`

Mensaje literal:

    androidx.compose.ui.test.ComposeTimeoutException: Condition still not satisfied after 5000 ms
        at androidx.compose.ui.test.junit4.AndroidComposeTestRule.waitUntil(AndroidComposeTestRule.android.kt:500)
        at com.edu.unal.tictactoe.NicknameDialogTest.backInvokesTheDismissCallback(NicknameDialogTest.kt:178)

Reproducible (falló en las dos repeticiones posteriores de la clase). Para saber si lo causa la subida, se restauró temporalmente `gradle/libs.versions.toml` a las versiones anteriores (HEAD), se reconstruyó, se instaló y se ejecutó la clase: falla igual (1 de 12). No es una regresión de esta subida; es previo y propio del dispositivo físico (la prueba espera que el gesto Atrás llegue al diálogo con `dismissals > 0`). Se restauró el catálogo con las versiones nuevas. No se cambió código de pruebas; por eso la tarea 1.8 queda sin marcar. Las demás 33 clases pasan.

## Cierre del fallo de `NicknameDialogTest.backInvokesTheDismissCallback`

- Causa: error de la prueba, no de producción. En un dispositivo físico el teclado en pantalla estaba abierto cuando la prueba pulsaba Back (`mInputShown=true` con el diálogo abierto), y entonces el primer Back solo cierra el teclado y el diálogo sigue abierto. En un emulador con teclado físico eso no ocurre. Era previo a la subida de dependencias.
- Corrección en `A/NicknameDialogTest.kt`: antes de pulsar Back, la prueba lee la visibilidad del teclado (`WindowInsetsCompat.Type.ime()`); si está abierto, lo cierra con un Back real, espera a que se oculte y comprueba que ese Back no descartó el diálogo. Después pulsa Back real con el diálogo abierto y sin teclado, y espera el descarte con `waitUntil`. No se llama directamente al callback ni se debilita la aserción.
- Resultado: `NicknameDialogTest` en `OK (12 tests)` tres veces seguidas y también justo después de `OnlineLobbyScreenTest` y de `SharedPreferencesNicknameStoreTest`, en el dispositivo físico (API 35).
