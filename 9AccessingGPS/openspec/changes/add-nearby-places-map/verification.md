# Registro de verificación

Cambio: `add-nearby-places-map` (lugares cercanos con Maps Compose y Places SDK).

## 1. Resumen

- Verificación automática completa desde cero (tarea 7.1): 92 pruebas JVM y 21 pruebas instrumentadas, 0 fallos, 0 errores y 0 omitidas. Lint sin errores y con 9 advertencias.
- Commit probado: `dd3b664` (sin cambios locales en el código al iniciar esta verificación). Este registro y la tarea 7.1 son documentación posterior.
- Las pruebas instrumentadas corrieron solo en el emulador AVD `DADM_GraphicsSound_API35` (API 35, Android 15). El celular físico no participó en esta verificación.
- Pendientes: QA manual en el emulador (7.2), QA de errores (7.3), API 36 (7.4, opcional), celular (7.5) y revisión de evidencia (7.6).

## 2. Verificación automática (tarea 7.1)

Comando exacto, ejecutado desde `9AccessingGPS/` con el emulador como único destino (variable de selección de dispositivo fijada al emulador; su identificador se omite de este registro) y con el JBR de Android Studio:

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest
```

Sigla de la verificación: **VA** (verificación automática).

| Fase | Resultado |
|---|---|
| `clean` | Ejecutada (`> Task :app:clean`) |
| `:app:testDebugUnitTest` | PASA. 92 pruebas, 0 fallos, 0 errores |
| `:app:lintDebug` | PASA. 0 errores, 9 advertencias |
| `:app:assembleDebug` | PASA |
| `:app:connectedDebugAndroidTest` | PASA. 21 pruebas, 0 fallos, 0 errores, 0 omitidas, en el AVD `DADM_GraphicsSound_API35` |

Resultado global de Gradle: `BUILD SUCCESSFUL in 23s` y `87 actionable tasks: 87 executed`. Código de salida 0.

### 2.1 Pruebas JVM por clase

Fuente: `app/build/test-results/testDebugUnitTest/`. Total: 92 pruebas, 18 clases, 0 fallos, 0 errores, 0 omitidas.

| Clase | Pruebas |
|---|---|
| `ExampleUnitTest` (plantilla) | 1 |
| `domain.DistanceCalculatorTest` | 3 |
| `domain.NearbySearchParamsTest` | 5 |
| `domain.PlaceCategoryTest` | 7 |
| `domain.PlaceSearchRulesTest` | 7 |
| `domain.PlacesErrorMapperTest` | 6 |
| `domain.RadiusRulesTest` | 6 |
| `location.AddressResolverMappingTest` | 4 |
| `location.CoordinateFormatterTest` | 3 |
| `location.LocationPermissionStateTest` | 5 |
| `ui.map.CameraBoundsTest` | 5 |
| `ui.map.DistanceFormatterTest` | 3 |
| `ui.map.MapStyleTest` | 1 |
| `ui.map.MapViewModelLocationTest` | 9 |
| `ui.map.MapViewModelSearchTest` | 15 |
| `ui.map.PlaceLabelTextTest` | 4 |
| `ui.navigation.NavigationViewModelTest` | 3 |
| `ui.settings.SettingsViewModelTest` | 5 |
| **Total** | **92** |

### 2.2 Pruebas instrumentadas por clase

Fuente: `app/build/outputs/androidTest-results/connected/debug/`. Total: 21 pruebas, 7 clases, 0 fallos, 0 errores, 0 omitidas. Dispositivo: AVD `DADM_GraphicsSound_API35`.

| Clase | Pruebas |
|---|---|
| `CategoryChipsTest` | 3 |
| `ExampleInstrumentedTest` (plantilla) | 1 |
| `FusedLocationProviderTest` | 1 |
| `MapStatesTest` | 8 |
| `NavigationTest` | 1 |
| `RadiusPreferencesTest` | 3 |
| `ui.settings.SettingsScreenTest` | 4 |
| **Total** | **21** |

### 2.3 Lint

Fuente: `app/build/reports/lint-results-debug.txt`. Resultado: 0 errores, 9 advertencias.

| Tipo | Cantidad | Ubicación |
|---|---|---|
| `UnusedResources` | 7 | `app/src/main/res/values/colors.xml` (`purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`, `black`, `white`) |
| `ObsoleteSdkInt` | 1 | `app/src/main/java/com/edu/unal/lugarescercanos/ui/theme/Theme.kt:44` |
| `RedundantLabel` | 1 | `app/src/main/AndroidManifest.xml:29` |

### 2.4 Versiones del entorno

| Elemento | Versión |
|---|---|
| Android Gradle Plugin | 9.4.1 |
| Gradle (wrapper) | 9.8.2 |
| Kotlin | 2.4.21 |
| compileSdk | 37 |
| minSdk | 35 |
| targetSdk | 37 |
| Emulador (AVD) | `DADM_GraphicsSound_API35`, API 35 (Android 15) |

### 2.5 Advertencias de compilación

Durante `compileDebugAndroidTestKotlin` Kotlin avisó que `createComposeRule` y `createAndroidComposeRule` están obsoletos en cuatro archivos de prueba instrumentada: `app/src/androidTest/java/com/edu/unal/lugarescercanos/CategoryChipsTest.kt:27`, `MapStatesTest.kt:30`, `NavigationTest.kt:18` y `ui/settings/SettingsScreenTest.kt:22`. No son fallos y no cambian el resultado.

## 3. QA manual en el emulador (7.2)

Detalle de casos, pasos y resultados: `qa/7-qa-manual.md`. Dispositivo: AVD `DADM_GraphicsSound_API35`, ubicación simulada en la Universidad Nacional (4,6381; -74,0840).

Resumen: los casos de 7.2 pasan, salvo C-07 (solo ubicación aproximada), que no se pudo verificar en el emulador y se verifica en el celular (7.5); no bloquea 7.2. Re-test del 2026-10-27 tras corregir D1 en la sección 4.

| Flujo | Resultado | Capturas |
|---|---|---|
| Diálogo de permiso, denegado y «Conceder permiso» | PASA | `qa/7-2-01-dialogo-permiso.png`, `qa/7-2-02-permiso-denegado.png`, `qa/7-2-03-dialogo-segunda-vez.png` |
| Denegación permanente y «Abrir ajustes» | PASA | `qa/7-2-05-denegacion-permanente.png`, `qa/7-2-06-abrir-ajustes.png` |
| Permiso preciso, tarjeta «Tu ubicación» con dirección y coordenadas | PASA | `qa/7-2-09-permiso-preciso-resultados.png` |
| Solo ubicación aproximada | NO VERIFICADO: el emulador no entrega ubicación a una app con solo `ACCESS_COARSE_LOCATION`; se prueba en el celular (7.5) | `qa/7-2-08-solo-aproximada.png`, `qa/7-2-08b-aproximada-no-obtenida.png` |
| Actualizar ubicación y «Dirección no disponible» | PASA | `qa/7-2-25-sin-resultados.png`, `qa/7-2-26-direccion-no-disponible.png` |
| Cinco categorías con contador (20 lugares en cada una con 1 km) | PASA | `qa/7-2-12-farmacias.png`, `qa/7-2-13-restaurantes.png`, `qa/7-2-14-sitios-turisticos.png`, `qa/7-2-15-parques.png` |
| Nombres en los pines, recorte a 28 caracteres, burbuja con nombre y distancia | PASA | `qa/7-2-10-burbuja-pin.png`, `qa/7-2-11-burbuja-nombre-largo.png` |
| Marcador azul, círculo del radio y encuadre; logo de Google; lugares de Google ocultos | PASA | `qa/7-2-09-permiso-preciso-resultados.png`, `qa/7-2-19-reapertura-3km-categoria-reiniciada.png` |
| Ajustes: valor por defecto, límites 0,5 y 10 km, formato es-CO | PASA | `qa/7-2-16-ajustes-1km.png`, `qa/7-2-17-ajustes-minimo.png`, `qa/7-2-18-ajustes-maximo.png` |
| Persistencia tras cerrar a la fuerza y categoría que no se recuerda | PASA | `qa/7-2-19-reapertura-3km-categoria-reiniciada.png`, `qa/7-2-20-ajustes-persistido-3km.png` |
| Radio cambiado repite la búsqueda; radio sin cambios no la repite | PASA | `qa/7-2-23-radio-2km-busqueda-repetida.png`, `qa/7-2-18b-tras-volver-05km.png`, `qa/7-2-22b-radio-sin-cambios-sin-red.png` |
| Back del sistema en Ajustes | PASA | `qa/7-2-21-back-sistema-ajustes.png` |
| Contador «1 lugar» y «Sin resultados» | PASA | `qa/7-2-24-contador-singular-1-lugar.png`, `qa/7-2-25-sin-resultados.png` |

Las etiquetas superpuestas son un riesgo aceptado (U13) y no se cuentan como defecto.

## 4. QA de errores (7.3)

Detalle: `qa/7-qa-manual.md`, casos E-01 a E-07.

Resumen: primera pasada con 1 fallo (D1, media). D1 fue corregido y el re-test del 2026-10-27 pasa; ver más abajo.

| Flujo | Resultado | Capturas |
|---|---|---|
| Sin red: «Sin conexión a internet» con «Reintentar», sin pines | PASA | `qa/7-3-sin-conexion.png` |
| Sin reintento automático al volver la red | PASA | `qa/7-3-02-sin-red-sin-reintento-automatico-tras-reconectar.png` |
| «Reintentar» con red repite la búsqueda y vuelven los resultados | PASA | `qa/7-3-03-reintentar-con-red.png` |
| Llave faltante (copia sin `secrets.properties`): «Falta configurar la llave de Google Maps», sin búsqueda | PASA | `qa/7-3-sin-llave.png` |
| Ubicación del dispositivo desactivada: mensaje, sin lugares ni coordenadas | PASA | `qa/7-3-04-ubicacion-desactivada.png` |
| Recuperación tras volver a activar la ubicación | FALLA en la primera pasada (D1); PASA en el re-test | `qa/7-3-05-defecto-desactivada-sin-boton-tras-reactivar.png` |

Historial del defecto D1 (severidad media, corregido): con la ubicación del dispositivo desactivada la tarjeta de ubicación no muestra el botón «Actualizar ubicación», y al volver a activar la ubicación el mensaje sigue; solo se recupera cerrando la app a la fuerza. Requisitos: `places-map`, «Actualizar la ubicación desde el mapa», y `current-location`, «Lectura puntual de la ubicación».

Re-test del 2026-10-27 con el árbol de trabajo corregido, reinstalado en el AVD `DADM_GraphicsSound_API35` (`qa/7-qa-manual.md`, sección 3.1):

| Caso | Resultado | Capturas |
|---|---|---|
| E-07: desactivada muestra el mensaje y el botón «Actualizar ubicación» | PASA | `qa/7-3-06-desactivada-con-boton.png` |
| E-07b: tras activar la ubicación, el botón devuelve tarjeta y resultados | PASA | `qa/7-3-07-recuperada-con-boton.png` |
| E-08: recuperación automática al volver a la app | PASA | `qa/7-3-08-recuperada-al-volver.png` |
| R-01: Ajustes sin cambiar el radio no repite la búsqueda | PASA | `qa/7-3-09-regresion-radio-sin-cambios.png` |
| R-02: segundo plano y volver con ubicación lista no repite la búsqueda | PASA | `qa/7-3-10-regresion-segundo-plano-sin-red.png` |
| R-03: radio cambiado repite la búsqueda | PASA | `qa/7-3-11-regresion-radio-cambiado-repite-busqueda.png` |

D1 queda corregido y verificado. C-07 (solo ubicación aproximada) queda para la tarea 7.5 en el celular.

Estado final del emulador: versión normal instalada (con la llave real), ubicación activada y modo avión desactivado. La copia sin llave quedó fuera del proyecto, en la carpeta temporal de la sesión.

## 5. API 36, opcional (7.4)

Pendiente.

## 6. Celular del usuario (7.5)

Pendiente.

## 7. Revisión de evidencia (7.6)

Pendiente.

## 8. Observaciones y riesgos

- Ninguna prueba falló. Las advertencias de lint y de compilación de la sección 2 no bloquean el cambio.
- `ExampleUnitTest` y `ExampleInstrumentedTest` son pruebas de la plantilla y siguen en la suite.
