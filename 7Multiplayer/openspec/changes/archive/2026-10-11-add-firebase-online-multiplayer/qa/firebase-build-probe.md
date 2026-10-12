# Prueba de build con Firebase (tareas 1.1 a 1.4 y 1.7)

## Versión del plugin `google-services` (1.1)

- Elegida: `4.5.0` (la más reciente publicada: `<release>4.5.0</release>`, `lastUpdated` 2026-06-16).
- Fuentes:
  - https://dl.google.com/dl/android/maven2/com/google/gms/google-services/maven-metadata.xml (Maven de Google; última 4.5.0)
  - https://plugins.gradle.org/m2/com/google/gms/google-services/maven-metadata.xml (portal de plugins; release 4.5.0)
  - https://firebase.google.com/docs/android/setup (declara el plugin `com.google.gms.google-services`; la página muestra 4.5.0 entre las versiones citadas).
- BoM `35.0.0` resuelve `firebase-auth:25.0.0` y `firebase-database:23.0.0`.

## Resultados

| Paso | Comando | Resultado |
|---|---|---|
| Línea base (sin cambios) | `./gradlew testDebugUnitTest assembleDebug` | BUILD SUCCESSFUL |
| 1.1 | `./gradlew help` | BUILD SUCCESSFUL |
| 1.2 | `./gradlew :app:dependencies --configuration debugRuntimeClasspath` | lista `firebase-auth -> 25.0.0` y `firebase-database -> 23.0.0` (BoM 35.0.0) |
| 1.3 | `./gradlew :app:processDebugManifest` | BUILD SUCCESSFUL; el manifest fusionado de debug contiene `android.permission.INTERNET` |
| 1.4 corrida 1 | `./gradlew assembleDebug` | **BUILD FAILED** (`:app:compileDebugKotlin`), "Configuration caché entry stored" |
| 1.4 corrida 2 | `./gradlew assembleDebug` | **BUILD FAILED**, "Configuration caché entry reused" |
| 1.4 verificación | `./gradlew testDebugUnitTest lintDebug assembleDebug` | **BUILD FAILED** en `:app:compileDebugKotlin` (lint no llegó a ejecutarse) |

## Fallo de 1.4

Error literal:

    e: .../firebase-auth-25.0.0-api.jar!/META-INF/java.com.google.android.gmscore.integ.client.firebase-auth-api_firebase-auth-api.kotlin_module Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.4.0, expected version is 2.2.0.

Diagnóstico: el fallo NO proviene del plugin `google-services`. La tarea `:app:processDebugGoogleServices` se ejecuta sin error y la caché de configuración se almacena y se reutiliza con el plugin. La causa es que `firebase-auth:25.0.0` (BoM 35.0.0) tiene metadatos Kotlin 2.4.0 y el proyecto compila con Kotlin 2.2.10 (el compilador acepta como máximo metadatos una versión mayor que la suya). No se cambió Kotlin ni se aplicó el plan alternativo (1.6), según lo indicado. 1.4 queda sin marcar.

Advertencias del plugin: ninguna observada.

## Git (1.7)

`git check-ignore -v app/google-services.json` no imprime nada (código de salida 1). El archivo está rastreado (`git ls-files` lo lista) y sin cambios. No se copió su contenido.

## Sondeo de alternativas

Solo medición: `gradle/libs.versions.toml` se restauró a `firebaseBom = "35.0.0"`, `kotlin = "2.2.10"`, `googleServices = "4.5.0"`. Línea base de lint (árbol sin cambios de Firebase): 0 errores, 16 advertencias.

### Sondeo A: bajar el BoM, Kotlin 2.2.10

Comando por intento: `./gradlew :app:compileDebugKotlin`. Lista de versiones: https://dl.google.com/dl/android/maven2/com/google/firebase/firebase-bom/maven-metadata.xml (la última es 35.0.0; la última 34.x es 34.19.0).

| Intento | BoM | firebase-auth | firebase-database | Compila |
|---|---|---|---|---|
| 1 | 34.19.0 | 24.2.0 | 22.0.2 | Sí |
| (extra) | 34.10.0 | 24.0.1 | 22.0.1 | Sí |
| (extra) | 34.5.0 | 24.0.1 | 22.0.1 | Sí |
| (extra) | 34.0.0 | 24.0.0 | 22.0.0 | Sí |
| (extra) | 33.16.0 | 23.2.1 | 21.0.0 | Sí |

El primer intento (34.19.0) ya pasa; los demás son informativos. Con 34.19.0:

- `./gradlew testDebugUnitTest lintDebug assembleDebug`: BUILD SUCCESSFUL; 221 pruebas, 0 fallos.
- Lint: 0 errores, 17 advertencias (16 de la línea base + 1 nueva: `A newer version of com.google.firebase:firebase-bom than 34.19.0 is available: 35.0.0 [GradleDependency]`).
- `./gradlew assembleDebug` repetido: "Configuration cache entry reused" (la primera corrida tras otra combinación de tareas almacena una entrada nueva; las siguientes la reutilizan).

### Sondeo B: BoM 35.0.0, subir Kotlin

Versiones estables (solo `kotlin` cambia en el catálogo; el plugin de Compose comparte versión): última 2.3.x = 2.3.21, última 2.4.x = 2.4.21 (2.5.0-Beta1 es la última publicada pero no estable). Fuente: https://repo1.maven.org/maven2/org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/maven-metadata.xml

| Intento | Kotlin | Resultado |
|---|---|---|
| 1 | 2.3.21 | `testDebugUnitTest lintDebug assembleDebug` BUILD SUCCESSFUL; 221 pruebas, 0 fallos |
| 2 | 2.4.21 | no necesario (el intento 1 compiló) |

Con 2.3.21, sin otros cambios de build:

- Sin advertencias del compilador ni mensajes de AGP o Gradle sobre la versión de Kotlin.
- Lint: 0 errores, 16 advertencias; la única diferencia con la línea base es el texto de la advertencia existente `NewerVersionAvailable` (ahora dice 2.3.21 en vez de 2.2.10; sugiere 2.4.20).
- `./gradlew assembleDebug` repetido: "Configuration cache entry reused".
- No se comprobó en un dispositivo ni con emulador que la app funcione en tiempo de ejecución.

## Decisión del usuario y verificación final de 1.4 (2026-10-10)

Decisión: subir Kotlin a la última 2.4.x estable y dejar el BoM de Firebase en 35.0.0 (criterio: versiones lo más altas posible mientras el proyecto está en desarrollo). Versión final: Kotlin `2.4.21` (última estable de la serie 2.4; en Maven Central también existe 2.4.21-RC, no estable). Fuente: https://repo1.maven.org/maven2/org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/maven-metadata.xml. Solo cambió `kotlin` en el catálogo.

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug` (1.ª) | BUILD SUCCESSFUL; "Configuration cache entry stored" |
| `./gradlew assembleDebug` (2.ª) | BUILD SUCCESSFUL; "Configuration cache entry reused" |
| `./gradlew testDebugUnitTest lintDebug assembleDebug` | BUILD SUCCESSFUL; 221 pruebas, 0 fallos |

- Advertencias del compilador o del plugin: ninguna.
- Lint: 0 errores, 15 advertencias (línea base: 16). Ninguna nueva; desaparece la de `NewerVersionAvailable` del plugin de Compose.
- Tarea 1.4 cumplida.

## Inicialización de Firebase en el dispositivo (1.5) y plan alternativo (1.6)

Destino: emulador de referencia, API 35 (identidad verificada antes de cada operación: nombre del AVD, API, ABI y arranque completo).

Prueba creada: `app/src/androidTest/java/com/edu/unal/tictactoe/FirebaseInitializationTest.kt`. Comprueba que `FirebaseApp.getInstance()` existe en el proceso de la app y que `options.applicationId`, `options.apiKey` y `options.databaseUrl` no están vacíos; no imprime ni incluye esos valores en los mensajes de aserción.

| Paso | Comando | Resultado |
|---|---|---|
| Build | `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| Instalación | `adb -s <destino verificado> install -r` (APK de la app y de pruebas) | Success |
| 1.5 | `adb -s <destino verificado> shell am instrument -w -e class com.edu.unal.tictactoe.FirebaseInitializationTest com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner` | `OK (1 test)` |

Tarea 1.5 cumplida con el plugin `google-services` 4.5.0.

### Constancia de 1.6

Con 1.4 y 1.5 aprobadas con el plugin, el plan alternativo (inicializar con `FirebaseOptions` por código) no hizo falta y no se aplicó. El plugin funciona con AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.21 y la caché de configuración, y `FirebaseApp` se inicializa en el proceso de la app.

### Comprobación del compilador Kotlin 2.4.21 en instrumentadas

Misma forma de ejecución, clase existente sin modificar: `com.edu.unal.tictactoe.MainActivityTest` -> `OK (7 tests)`, 0 fallos.
