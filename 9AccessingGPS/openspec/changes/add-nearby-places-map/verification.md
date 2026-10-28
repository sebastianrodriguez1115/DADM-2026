# Registro de verificación

Cambio: `add-nearby-places-map` (lugares cercanos con Maps Compose y Places SDK).

## 1. Resumen

- Estado final: tareas 7.1 a 7.6 cerradas. La 7.4 (API 36, opcional) quedó **omitida** (sección 5).
- Última corrida automática, tras corregir el defecto D1: 97 pruebas JVM y 22 pruebas instrumentadas, 0 fallos, 0 errores y 0 omitidas. Lint con 0 errores y 9 advertencias. Cifras confirmadas en los reportes de `app/build/test-results/testDebugUnitTest/` y `app/build/outputs/androidTest-results/connected/debug/` (2026-10-27, 21:28 y 21:29 hora local). Código probado: `c16dd81`, último commit que toca `app/`, con el árbol limpio.
- Las pruebas instrumentadas corrieron solo en el emulador AVD `DADM_GraphicsSound_API35` (API 35, Android 15). El celular físico no participó en la verificación automática; se usó en la 7.5 (sección 6).
- Defecto encontrado y corregido: **D1** (severidad media), en el QA de errores (sección 4). Con la ubicación del dispositivo desactivada no había botón «Actualizar ubicación» y el mensaje no se quitaba al reactivarla. Corregido en `c16dd81` y verificado en el re-test.
- **C-07** (solo ubicación aproximada) no se pudo verificar en el emulador; se verificó en el celular del usuario (sección 6): PASA.
- Observación **O-1** (baja, celular): ver sección 6.
- Riesgo aceptado por el usuario (**U13**): «Hospitales y clínicas» incluye tipos amplios (`medical_clinic`) y se aceptan las etiquetas superpuestas en zonas densas. No se cuenta como defecto (sección 3).
- Revisión de evidencia (7.6, sección 7): sin números de serie, identificadores del dispositivo ni la llave. Las coincidencias del `grep` son falsos positivos documentados.

## 2. Verificación automática (tarea 7.1)

> Registro de la corrida de la tarea 7.1, hecha sobre `dd3b664` y antes de corregir D1: 92 pruebas JVM y 21 instrumentadas. Las cifras finales están en la sección 1.

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

**Omitida.** Motivo: el único AVD de API 36 disponible pertenece a otro proyecto y no se usó; la app se verificó en un dispositivo real con Android 15 (7.5) y en el emulador de API 35 (7.1 a 7.3).

## 6. Celular del usuario (7.5)

- Dispositivo: Samsung Galaxy S21 Ultra (SM-G998B), Android 15. Google Play services 26.41.31.
- Instalada con `./gradlew :app:installDebug` (keystore de depuración de este Mac), 2026-10-27. Detalle en `qa/7-5-celular.md`.
- Resultado: 12 de 12 casos PASAN (instalación y apertura sin fallos, diálogo de permisos, C-07 solo aproximada, mapa con tarjeta y círculo, 5 categorías, cambio de radio que repite la búsqueda, permiso preciso).
- C-07 (solo aproximada): la app sigue funcionando, muestra la ubicación y el aviso «Ubicación aproximada: los lugares y las distancias pueden ser menos precisos.». PASA.
- Observación O-1 (baja): al conceder la ubicación precisa con `pm grant` con la app abierta, el aviso de aproximada persiste hasta reiniciar la app. No se reproduce por la vía normal del usuario.
- Sin capturas ni datos de ubicación en el registro.

## 7. Revisión de evidencia (7.6)

Comandos ejecutados desde `9AccessingGPS/`. Solo leen archivos; no usaron el celular ni el emulador.

| Comando | Resultado |
|---|---|
| `grep -RniIE` de la verificación de 7.6 (patrones: número de serie, identificador de emulador, 16 caracteres hexadecimales y formato de la llave de Google Maps) sobre `qa/` y `verification.md` | Dos coincidencias, ambas en `qa/build-probe.md` (líneas 102 y 103). Falsos positivos, ver abajo |
| El mismo `grep` sobre el cambio completo (control) | Además de las dos anteriores: `tasks.md:73` (un identificador de emulador en el comando documentado de 6.8) y `tasks.md:82` (la propia verificación de 7.6). `proposal.md`, `design.md` y `specs/` sin coincidencias |
| Mismo `grep` sobre las capturas `qa/*.png` (sin `-I`) | Sin coincidencias en PNG |
| `git ls-files 9AccessingGPS \| grep -E "secrets.properties\|local.properties"` | Sin resultados (ningún archivo de llaves versionado) |

Correcciones hechas en esta revisión:

- `qa/3-5-persistencia.md`, línea 5: la frase que nombraba el identificador del emulador se cambió por «el comando de adb se dirigió al emulador del AVD indicado, sin registrar su identificador». Sentido igual, sin el identificador.
- `tasks.md`, 7.6: el comando de verificación ahora usa `grep -RniIE`, con `-I` para saltar archivos binarios.

Falsos positivos documentados:

- `qa/build-probe.md`, líneas 102 y 103: las rutas de caché de Gradle (`~/.gradle/caches/9.8.2/transforms/<hash>/`, una en `places-6.0.2-api.jar` y otra en `play-services-basement-18.9.0`) contienen un hash hexadecimal de 16 o más caracteres. Son nombres de directorio de caché de Gradle, no números de serie, identificadores de dispositivo ni llaves. No se editaron porque `build-probe.md` no forma parte de los archivos de este cierre.

Con esas dos excepciones, el `grep` no encuentra nada. Ninguna captura, ningún registro ni este documento contiene la llave de Google Maps.

## 8. Observaciones y riesgos

- Ninguna prueba falló en la última corrida. Las advertencias de lint y de compilación de la sección 2 no bloquean el cambio.
- D1 quedó corregido y verificado (sección 4).
- O-1 (sección 6) es un riesgo bajo del celular y no se reproduce por la vía normal del usuario. No es la misma O-1 de `qa/7-qa-manual.md`, que describe las etiquetas superpuestas (riesgo U13).
- U13 (sección 3) es un riesgo aceptado por el usuario.
- El encabezado de `qa/7-qa-manual.md` todavía cita `dd3b664` como commit probado. El re-test del 2026-10-27 se hizo con la corrección de D1, que está en `c16dd81`.
- `ExampleUnitTest` y `ExampleInstrumentedTest` son pruebas de la plantilla y siguen en la suite.
