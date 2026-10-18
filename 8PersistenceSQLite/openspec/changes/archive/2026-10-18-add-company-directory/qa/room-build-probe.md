# Prueba de build con Room 3 (tareas 1.1 a 1.5)

## Versiones finales

| Elemento | Versión | Coordenadas |
|---|---|---|
| Room | 3.0.3 | `androidx.room3:room3-runtime`, `androidx.room3:room3-compiler` (vía `ksp`) |
| KSP | 2.3.12 | plugin `com.google.devtools.ksp` (declarado con `apply false` en la raíz) |
| Kotlin | 2.2.10 (sin cambios) | Kotlin integrado de AGP 9.4.0; solo se aplica `org.jetbrains.kotlin.plugin.compose` |
| Driver | `AndroidSQLiteDriver` | clase `androidx.sqlite.driver.AndroidSQLiteDriver`, en el artefacto `androidx.sqlite:sqlite-framework` 2.7.1 (`sqlite-framework-android`) |

`room3-runtime` 3.0.3 trae `androidx.sqlite:sqlite`, `sqlite-async` y `sqlite-framework` 2.7.1 de forma transitiva; el driver se declaró igualmente de forma explícita. Gradle 9.6.0, sin ningún flag de compatibilidad (`android.builtInKotlin`, `android.newDsl`, `android.disallowKotlinSourceSets` no se tocaron; `gradle.properties` sin cambios).

## Resultados

| Tarea | Comando | Resultado |
|---|---|---|
| 1.1 | `./gradlew help` | BUILD SUCCESSFUL |
| 1.1 | `./gradlew :app:dependencies --configuration debugRuntimeClasspath` | `androidx.room3:room3-runtime:3.0.3` y `androidx.sqlite:sqlite-framework:2.7.1` resueltos; `kotlin-stdlib:2.1.20 -> 2.2.10` (sin conflicto) |
| 1.2 | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL (`kspDebugKotlin` y `compileDebugKotlin` bien); existen `DirectorioDatabase_Impl.kt` y `CompanyDao_Impl.kt` en `app/build/generated/ksp/debug/kotlin/com/edu/unal/directorio/data/db/` |
| 1.3 | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.directorio.RoomSmokeTest` | BUILD SUCCESSFUL; 1 prueba, 0 fallos, 0 errores (teléfono con Android 15, API 35) |
| 1.4 | `./gradlew :app:assembleDebug` dos veces | corrida 1 BUILD SUCCESSFUL, "Configuration cache entry stored"; corrida 2 BUILD SUCCESSFUL, "Configuration cache entry reused" |
| final | `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` | BUILD SUCCESSFUL |

## Hechos que resolvió la prueba

- **El código generado compila con Kotlin 2.2.10** y KSP 2.3.12 funciona con el Kotlin integrado de AGP 9.4 sin ajustes: `kspDebugKotlin` y `compileDebugKotlin` pasan.
- **Directorio de esquemas: no se pidió.** Con `exportSchema = false` el compilador no exige `room.schemaLocation` ni el plugin de Gradle de Room.
- **Sin `@ConstructedBy`:** en una app solo Android el compilador generó `DirectorioDatabase_Impl` y no hizo falta declarar un constructor `expect/actual`. Las bases se crean con `Room.databaseBuilder(context, DirectorioDatabase::class.java, nombre)` y `Room.inMemoryDatabaseBuilder(context, DirectorioDatabase::class.java)`, seguidos de `.setDriver(AndroidSQLiteDriver())`.
- **Firma real de `onCreate`** (de `javap` sobre `classes.jar` de `room3-runtime-android` 3.0.3, clase `androidx.room3.RoomDatabase.Callback`):

      open suspend fun onCreate(connection: androidx.sqlite.SQLiteConnection)
      // JVM: Object onCreate(SQLiteConnection, Continuation<? super Unit>)

  Es `suspend`, recibe `SQLiteConnection` (no `SupportSQLiteDatabase`) y devuelve `Unit`. `onOpen` y `onDestructiveMigration` tienen la misma forma. Se registra con `RoomDatabase.Builder.addCallback(...)`. Otras funciones del `Builder` vistas: `setDriver(SQLiteDriver)`, `setQueryCoroutineContext(CoroutineContext)`, `build()`.
- **Paquetes de la API usada:** `androidx.room3.{Database, Entity, PrimaryKey, Dao, Insert, Query, Room, RoomDatabase}`.
- **Coroutines:** `kotlinx-coroutines` llega de forma transitiva; la prueba de humo usa `runBlocking`, `async`, `first` y `withTimeout` sin agregar `kotlinx-coroutines-test`.

## Advertencias

Ninguna de KSP, de Room ni de Kotlin en las corridas de `assembleDebug` (también con `app/build` borrado y `--no-build-cache`). La corrida de `connectedDebugAndroidTest` muestra avisos de `sun.misc.Unsafe` de protobuf, que vienen de las herramientas de Android y no de Room ni de KSP.

## Plan alternativo (1.5)

No hizo falta: 1.2 a 1.4 pasaron con la combinación inicial (Room 3.0.3, KSP 2.3.12, Kotlin 2.2.10). No se probó KSP 2.3.10 ni 2.3.6, ni Kotlin 2.3.20, ni Room 2.8.5, y `design.md` no cambia.

## Archivos de prueba

Los tipos `CompanyEntity`, `CompanyDao` y `DirectorioDatabase` de `data/db/` son mínimos y los reemplaza el grupo 3. `RoomSmokeTest` abre una base en memoria con `AndroidSQLiteDriver`, inserta una fila, la lee y comprueba que el `Flow` emite.

## Subida de librerías y dependencias nuevas (1.6 y 1.7, P9)

Fecha: 2026-10-18. Gradle, AGP, Kotlin, KSP, Room y sqlite no cambian. Última versión estable = la mayor sin sufijo alpha, beta ni rc en el índice del repositorio.

| Librería | Anterior | Nueva | Fuente |
|---|---|---|---|
| `androidx.core:core-ktx` | 1.10.1 | 1.19.1 | Google Maven, `androidx/core/group-index.xml` |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.1 | 2.11.0 (2.12.0 solo alpha) | Google Maven, `androidx/lifecycle/group-index.xml` |
| `androidx.activity:activity-compose` | 1.8.0 | 1.13.0 (1.14.0 solo alpha) | Google Maven, `androidx/activity/group-index.xml` |
| `androidx.compose:compose-bom` | 2026.02.01 | 2026.09.00 | Google Maven, `androidx/compose/group-index.xml` |
| `junit:junit` | 4.13.2 | 4.13.2 (ya era la última) | Maven Central, `maven-metadata.xml` |
| `androidx.test.ext:junit` | 1.1.5 | 1.3.0 (1.4.0 solo alpha) | Google Maven, `androidx/test/ext/group-index.xml` |
| `androidx.test.espresso:espresso-core` | 3.5.1 | 3.7.0 (3.8.0 solo alpha) | Google Maven, `androidx/test/espresso/group-index.xml` |

Dependencias nuevas:

| Librería | Versión | Fuente | Notas |
|---|---|---|---|
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.11.0 | Google Maven | Misma referencia de versión que `lifecycle-runtime-ktx` (`lifecycleRuntimeKtx`); última estable también 2.11.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 | Maven Central | `testImplementation`; referencia `coroutines` |
| `androidx.compose.material:material-icons-core` | 1.7.8 (BOM) | pom del BOM 2026.09.00 | Sin versión en el catálogo, la gestiona el BOM |

Hallazgos:

- **Íconos:** `material-icons-core` NO llega de forma transitiva con el BOM nuevo (ni `debugCompileClasspath` ni `debugRuntimeClasspath` lo traían antes de agregarlo). D9 necesita «+», volver y borrar búsqueda, así que se agregó como `implementation` sin versión; el BOM lo resuelve a 1.7.8. El núcleo incluye `Add`, `ArrowBack` (`AutoMirrored`) y `Clear`/`Close`; no hace falta `material-icons-extended`.
- **Coroutines:** no se agregó `kotlinx-coroutines-core`; llega transitivo (1.9.0 en runtime de la app) y `kotlinx-coroutines-test` 1.11.0 sube el núcleo a 1.11.0 en el classpath de pruebas JVM.
- Ninguna librería exigió otro Kotlin, AGP, Gradle o compileSdk; no hubo cambios en el código de la plantilla.

Verificación:

| Comando | Resultado |
|---|---|
| `./gradlew :app:testDebugUnitTest :app:assembleDebug` (tras 1.6) | BUILD SUCCESSFUL |
| PI `ExampleInstrumentedTest` | BUILD SUCCESSFUL; 1 prueba, 0 fallos, 0 errores |
| `./gradlew :app:assembleDebug :app:compileDebugUnitTestKotlin` (1.7) | BUILD SUCCESSFUL |
| PI `RoomSmokeTest` (regresión) | BUILD SUCCESSFUL; 1 prueba, 0 fallos, 0 errores |
| `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` | BUILD SUCCESSFUL; lint antes: 0 errors, 18 warnings; después: 0 errors, 12 warnings |
