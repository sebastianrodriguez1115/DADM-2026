# Sonda de build

## Grupo 1 sin llave

Fecha de la verificación: 2026-10-27. Entorno: AGP 9.4.1 y Gradle 9.8.2 (decisión U12), Kotlin 2.4.21 (decisión U11; el Kotlin integrado de AGP 9 trae 2.2.10 por defecto y se reemplaza, ver «Configuración de Kotlin»), compileSdk y targetSdk 37, minSdk 35. Sin `secrets.properties`; la app compila con `DEFAULT_API_KEY`.

**Estado del grupo 1 sin llave: completo (tareas 1.2 a 1.5).** Resultado de la última ejecución de `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks`:

```
BUILD SUCCESSFUL in 20s
53 actionable tasks: 53 executed
```

## Versiones finales

Fuente: `./gradlew :app:dependencies --configuration debugRuntimeClasspath`, sin errores de resolución.

| Dependencia | Resuelta | Nota |
|---|---|---|
| Android Gradle Plugin | 9.4.1 | Última estable en Google Maven (9.5.0 solo tiene rc). Antes: 9.4.0. |
| Gradle (wrapper) | 9.8.2 | Última estable en services.gradle.org. Antes: 9.6.0. Se actualizó con `./gradlew wrapper --gradle-version 9.8.2 --gradle-distribution-sha256-sum <sha>` dos veces (la propiedad `distributionSha256Sum` existente obliga a indicar la suma). |
| `org.gradle.toolchains.foojay-resolver-convention` | 1.0.0 | Ya era la última estable. |
| `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` | 2.0.1 | Ya era la última estable. |
| Kotlin (plugin de Gradle y plugin de Compose) | 2.4.21 | Última estable (2.5.0 solo tiene Beta; existe `2.4.21-RC`, descartada). |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.4.21 | Subida desde 2.2.10 por la resolución de Gradle. |
| `com.google.maps.android:maps-compose` | 9.0.1 | Pide `kotlin-stdlib` 2.4.20. |
| `com.google.android.libraries.places:places` | 6.0.2 | Pide `kotlin-stdlib` 2.4.0. |
| `com.google.android.gms:play-services-location` | 21.4.0 | |
| `androidx.datastore:datastore-preferences` | 1.3.0 | Sin contingencia; compila con Kotlin 2.4.21. |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.11.0 | Misma versión que lifecycle-runtime-ktx. |
| `androidx.compose.material:material-icons-core` | 1.7.8 | Versión fijada por `compose-bom`. |
| `androidx.compose:compose-bom` | 2026.09.00 | |
| `androidx.core:core-ktx` | 1.19.1 | |
| `androidx.activity:activity-compose` | 1.13.0 | |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 | |

Solo para pruebas: `androidx.test.ext:junit` 1.3.0, `androidx.test.espresso:espresso-core` 3.7.0 y `org.jetbrains.kotlinx:kotlinx-coroutines-test` 1.11.0. No aparecen en `debugRuntimeClasspath`; compilan y pasan en `testDebugUnitTest`/`assembleDebug` sin errores.

Extracto actual de `:app:dependencies --configuration debugRuntimeClasspath`:

```
+--- org.jetbrains.kotlin:kotlin-stdlib:2.4.21
+--- androidx.compose.material:material-icons-core -> 1.7.8
+--- androidx.datastore:datastore-preferences:1.3.0
+--- com.google.maps.android:maps-compose:9.0.1
+--- com.google.android.libraries.places:places:6.0.2
\--- com.google.android.gms:play-services-location:21.4.0
```

## Configuración de Kotlin

- Solo se cambió `kotlin = "2.4.21"` en `gradle/libs.versions.toml`. El plugin `org.jetbrains.kotlin.plugin.compose` (declarado con `apply false` en el `build.gradle.kts` raíz) usa esa versión y arrastra `kotlin-gradle-plugin` 2.4.21, que reemplaza al 2.2.10 de AGP 9.4.0: `./gradlew buildEnvironment` muestra `kotlin-gradle-plugin:2.2.10 -> 2.4.21`.
- Por U12 AGP y Gradle también suben (ver «Versiones finales»); `build.gradle.kts` y `settings.gradle.kts` no cambiaron. No se usó `-Xskip-metadata-version-check` ni se forzó la versión de `kotlin-stdlib`.
- **Evidencia del compilador:** no se encontró una línea de `--info` que imprima la versión del compilador usado por `compileDebugKotlin`. Que se usó Kotlin 2.4.21 se infiere de tres hechos: el classpath de build (`buildEnvironment`) resuelve el plugin de Gradle a 2.4.21, `kotlin-compiler-embeddable` 2.4.21 está en la caché de Gradle, y el error de metadatos del primer intento ("compiler version 2.2.0") desapareció sin cambiar nada más que la versión de Kotlin.

## Plugin de secretos

Resultado: **aplicado, sin contingencia.** Plugin `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` 2.0.1, con `propertiesFileName = "secrets.properties"` y `defaultPropertiesFileName = "local.defaults.properties"`. Funciona con AGP 9.4.1.

- `BuildConfig.MAPS_API_KEY` = `"DEFAULT_API_KEY"` (`app/build/generated/source/buildConfig/debug/.../BuildConfig.java`).
- Manifest fusionado de debug (`app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`): los cuatro permisos (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET`, `ACCESS_NETWORK_STATE`), ningún `READ_EXTERNAL_STORAGE` ni `WRITE_EXTERNAL_STORAGE`, y `<meta-data android:name="com.google.android.geo.API_KEY" android:value="DEFAULT_API_KEY" />`. Además hay `ACCESS_WIFI_STATE`, que aporta una librería.

`secrets.properties` no existe y no se creó. No se escribió ninguna llave real.

## Contingencia de DataStore

No se aplicó. `datastore-preferences` 1.3.0 se resuelve y compila.

## Lint

`app/build/reports/lint-results-debug.txt` con AGP 9.4.1 y Gradle 9.8.2: **0 errores, 9 advertencias.** Ninguna proviene de estos cambios:

- 7 `UnusedResources`: colores `purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`, `black` y `white` de la plantilla.
- 1 `RedundantLabel` en el manifest (actividad principal de la plantilla).
- 1 `ObsoleteSdkInt`.

Con AGP 9.4.0 y Gradle 9.6.0 eran 11 advertencias; las dos de más (`AndroidGradlePluginVersion`) avisaban de que había AGP 9.4.1 y Gradle 9.8.2, y desaparecieron con la actualización.

Advertencia de Gradle: «Deprecated Gradle features were used in this build, making it incompatible with Gradle 10». Con `--warning-mode all` aparece `Configuration.setVisible(boolean)` deprecado (se elimina en Gradle 11). No se identificó el plugin que la causa; no bloquea el build.

Compatibilidad: AGP 9.4.1 con Gradle 9.8.2 compila y pasa pruebas y lint. No se consultó la tabla de compatibilidad de la documentación de AGP, solo se comprobó el build.

## Historia: primer intento

Con `maps-compose` 9.0.1 y `places` 6.0.2 y Kotlin 2.2.10, `:app:compileDebugKotlin` falló:

```
e: Incompatible classes were found in dependencies. Remove them from the classpath or use '-Xskip-metadata-version-check' to suppress errors
e: ...MainActivity.kt:18:9 Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin. The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0.
```

Causa: `places` 6.0.2 pide `kotlin-stdlib` 2.4.0 y `maps-compose` 9.0.1 pide 2.4.20 (la 9.0.0 pide 2.4.10 y la 8.4.0 en adelante, 2.4.x). Gradle subió la stdlib a 2.4.20 y el compilador 2.2.x no puede leerla.

Contingencia probada y descartada: bajar a `maps-compose` 8.3.1 (pide `kotlin-stdlib` 2.3.21) y `places` 5.3.0 (pide 2.3.0). Con ellas la stdlib resolvía a 2.3.21 y `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` terminaba con `BUILD SUCCESSFUL` con Kotlin 2.2.10. Se revirtió porque el usuario decidió subir Kotlin.

Decisión U11 (usuario, 2026-10-27): se permite cambiar la versión de Kotlin. Se mantienen las últimas estables de ambas librerías y se sube Kotlin a 2.4.21.
