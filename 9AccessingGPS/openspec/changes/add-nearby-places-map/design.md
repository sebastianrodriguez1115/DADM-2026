# Design

## Context

Ver `proposal.md` para la motivación, el alcance y las decisiones de usuario (U1 a U10, con U1 corregida por U9) y del orquestador (P1 a P13). Los requisitos observables viven en los deltas de `specs/`; este documento solo explica el cómo.

Estado actual verificado en `/Users/jsrc1115/Code/DADM-2026/9AccessingGPS` (en adelante `P/` = `app/src/main/java/com/edu/unal/lugarescercanos/`):

- La app es la plantilla Hello World ya renombrada: `P/MainActivity.kt` con Compose, `P/ui/theme/` y las pruebas de ejemplo. Paquete y `applicationId` `com.edu.unal.lugarescercanos`, nombre visible «Lugares cercanos».
- Build: AGP 9.4.1 y Gradle 9.8.2 (desde U12; el plan original usaba 9.4.0 y 9.6.0), Kotlin 2.4.21 (desde U11; el plan original usaba 2.2.10) con el Kotlin integrado de AGP 9 (solo se aplica `org.jetbrains.kotlin.plugin.compose`, que arrastra el KGP 2.4.21), compileSdk y targetSdk 37, minSdk 35, Java 11, Compose BOM 2026.09.00, catálogo en `gradle/libs.versions.toml`.
- Dependencias de la plantilla, viejas: core-ktx 1.10.1, lifecycle-runtime-ktx 2.6.1, activity-compose 1.8.0, junit 4.13.2, androidx junit 1.1.5 y espresso 3.5.1. No hay `lifecycle-viewmodel-compose` ni `kotlinx-coroutines-test`.
- Versiones de las dependencias nuevas, según el encargo del 2026-10-27: `com.google.maps.android:maps-compose` 9.0.1 (8-oct-2026, la última en GitHub), `com.google.android.libraries.places:places` 6.0.2 (la última en Google Maven ese día), `com.google.android.gms:play-services-location` 21.4.0, `androidx.datastore:datastore-preferences` 1.3.0 y el plugin `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` 2.0.1.
- Imágenes de emulador disponibles: `DADM_GraphicsSound_API35` y `boticario_catalog_api36`, ambas `google_apis`, con Google Play services.

**No verificado** (lo resuelven el grupo 1 de `tasks.md` y el QA): que el plugin de secretos 2.0.1 funcione con AGP 9.4.1; que maps-compose 9.0.1 y Places 6.0.2 compilen con Kotlin 2.4.21 y compileSdk 37; que el mapa y Places respondan en el emulador con una llave válida; que `getCurrentLocation` devuelva una posición con `adb emu geo fix`; y que los tipos de lugar elegidos existan en la Table A de Google.

## Goals / Non-Goals

**Goals:** mostrar en un mapa de Google Maps los lugares con nombre de una categoría dentro de un radio configurable alrededor de la posición GPS; mantener cada decisión acotada y probable en la JVM cuando sea posible; reutilizar los patrones del taller 8 (navegación manual, `Application` con el contenedor, fábricas de ViewModel sin framework).

**Non-Goals:** ver Out of Scope en `proposal.md`. Además, no hay caché de lugares entre sesiones, no hay persistencia de resultados y no se prueba la rotación de pantalla como requisito.

## Decisions

### D1. Mapa con Maps Compose (U9, P6)

- Se usa `com.google.maps.android:maps-compose` 9.0.1, con el composable `GoogleMap` y un `CameraPositionState`. Según su README, `play-services-maps` ya no se declara aparte: el Maps SDK entra como dependencia transitiva. Desde Maps SDK 20.0.0 el SDK quitó `org.apache.http.legacy` de su manifest y, con el renderizador legacy (Google Play services anteriores a 25.50.34), la app se cae con `ClassNotFoundException: org.apache.http.ProtocolVersion`; por eso el manifest declara `<uses-library android:name="org.apache.http.legacy" android:required="false" />` dentro de `<application>`.
- Vista inicial: Plaza de Bolívar (4.5981, -74.0760, zoom 12) mientras llega la ubicación.
- Marcador propio «Tu ubicación» con `BitmapDescriptorFactory.HUE_AZURE`. No se usa `isMyLocationEnabled`: así el marcador coincide con la ubicación usada en la búsqueda y no aparece el botón por defecto.
- `Circle` semitransparente con el radio en metros.
- Encuadre: se calcula un `LatLngBounds` con los cuatro puntos extremos del círculo (centro ± radio) y se aplica con `CameraUpdateFactory.newLatLngBounds(bounds, padding)` solo después de `onMapLoaded`, porque antes de que el mapa tenga tamaño lanza `IllegalStateException`. Si la búsqueda termina antes de `onMapLoaded`, el encuadre espera.
- Lugares propios de Google ocultos con `MapStyleOptions`, leído de `res/raw/map_style.json` (`featureType: "poi"`, `visibility: "off"`).
- Sin Map ID: el Maps SDK móvil sin Map ID es gratis e ilimitado; con Map ID se cobra como Dynamic Maps; y el estilo JSON no se combina con estilos en la nube.
- Logo de Google visible: es la atribución y la interfaz no lo tapa. Los insets se aplican con `contentPadding` o con el `innerPadding` del `Scaffold`.
- Alternativa descartada: osmdroid, archivado desde el 2024-11-20. Alternativa descartada: MapLibre Compose, en Beta y con cambios incompatibles. Alternativa descartada: OpenStreetMap en general (U9).

### D2. Ubicación con Fused Location Provider, lectura puntual (U2, P1)

- `FusedLocationProviderClient` de `play-services-location` 21.4.0, con una llamada a `getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)` envuelta en `suspendCancellableCoroutine`. No hay `requestLocationUpdates` ni seguimiento continuo.
- Supuesto menor: la firma de `getCurrentLocation` recibe un `CancellationToken`; el token se obtiene de un `CancellationTokenSource` que se cancela si la corrutina se cancela.
- Antes de pedir la posición, `LocationManager.isLocationEnabled()` decide si se muestra el mensaje de ubicación desactivada (`current-location`).
- Un resultado nulo se trata como «No se pudo obtener la ubicación» (`current-location`). La API necesita Google Play services y puede no conseguir una posición, por ejemplo en un emulador sin ubicación simulada.
- Alternativa descartada: `LocationManager` de la plataforma con `requestSingleUpdate`. Evita la dependencia de Play services, pero se descarta porque la nota 1 del enunciado remite a developer.android.com/training/location, cuya guía recomienda el Fused Location Provider.

### D3. Permisos y estados de la ubicación (P2)

- `RequestMultiplePermissions` con `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`. El resultado se traduce a un estado: precisa, aproximada, denegada o denegada permanentemente. La denegación permanente se detecta con `shouldShowRequestPermissionRationale` después de un rechazo.
- «Abrir ajustes» abre `ACTION_APPLICATION_DETAILS_SETTINGS` con el paquete de la app.
- El manifest declara `INTERNET` y `ACCESS_NETWORK_STATE`. No se pide almacenamiento: la app no guarda teselas ni lugares en disco.
- Esta lógica vive en un estado de ubicación que `MapViewModel` consume; los textos viven en `strings.xml`.

### D4. Dirección con `Geocoder` asíncrono (P3)

- `android.location.Geocoder.getFromLocation(lat, lon, 1, GeocodeListener)`, la variante de API 33 disponible porque minSdk es 35. No hay ramas de compatibilidad.
- Se llama solo si `Geocoder.isPresent()`. El primer resultado da `getAddressLine(0)`. Si no hay backend, falla o la lista viene vacía, se muestra «Dirección no disponible». Las coordenadas con 5 decimales siempre se muestran.
- Alternativa descartada: un servicio de geocodificación de terceros (Nominatim) con una petición HTTP propia. Añadiría otra dependencia de red con su propia política de uso; el `Geocoder` del sistema basta para mostrar una dirección.

### D5. Búsqueda de lugares con Places SDK (New) (U9, P4)

- Librería `com.google.android.libraries.places:places` 6.0.2. En `LugaresApplication` se inicializa una vez con `Places.initializeWithNewPlacesApiEnabled(context, BuildConfig.MAPS_API_KEY)`.
- `PlacesSdkPlacesRepository` llama a `PlacesClient.searchNearby(SearchNearbyRequest)` con:
  - restricción `CircularBounds` centrada en la ubicación del usuario y radio = km × 1000 metros;
  - `includedTypes` según la categoría (D6);
  - `maxResultCount` = 20, el máximo de la API; no hay paginación;
  - `rankPreference` = `DISTANCE`;
  - campos `ID`, `DISPLAY_NAME` y `LOCATION`, y ningún otro.
- Los campos quedan en el SKU Pro (5.000 búsquedas gratis al mes; luego USD 32 por 1.000). Los campos Enterprise, como rating, teléfono, horario o web, suben el costo y quedan prohibidos.
- `NearbySearchParams` es un objeto puro de Kotlin con el radio en metros, los tipos, 20 y `DISTANCE`. Se prueba en la JVM; el adaptador lo traduce a `CircularBounds` y `SearchNearbyRequest`.
- Filtrado en `PlaceSearchRules` (Kotlin puro): se descartan los lugares sin nombre o sin ubicación; la distancia se calcula con `DistanceCalculator` (haversine, sin `android.location.Location`); se descartan los lugares a más de radio (supuesto menor, ver Risks); se ordena por distancia de menor a mayor y se toman como máximo 20.
- Errores: el adaptador toma `ApiException.statusCode` y lo pasa a `PlacesErrorMapper`, una función pura que devuelve el tipo de error. Red o tiempo de espera → «Sin conexión a internet»; `OVER_QUERY_LIMIT` → «El servicio de lugares alcanzó su límite. Intenta más tarde»; `REQUEST_DENIED` o llave inválida → «La llave de Google Maps no es válida o no tiene permisos»; cualquier otro → «El servicio de lugares no respondió». No hay reintentos automáticos; el botón «Reintentar» repite la búsqueda.
- Cancelación: una búsqueda nueva cancela la anterior (ver D9). Si la `Task` de Places no se puede cancelar, el resultado viejo se ignora por el identificador de búsqueda.
- Supuesto menor: los códigos exactos de `ApiException.statusCode` que corresponden a `OVER_QUERY_LIMIT` y `REQUEST_DENIED` se confirman con la documentación de Places SDK 6.0.2 en el grupo 2.
- Alternativa descartada: Places web service por HTTP. Se prefiere el SDK, porque respeta la restricción por paquete y SHA-1 y evita escribir el cliente HTTP y el parser.
- La política de Places obliga a mostrar sus resultados sobre un mapa de Google: https://developers.google.com/maps/documentation/places/web-service/policies

### D6. Categorías como valores del dominio (P5)

- `enum class PlaceCategory` en `P/domain/` con las cinco categorías en orden, y para cada una su lista de tipos de lugar de Google. La etiqueta visible va en `strings.xml`.
- Tipos según la Table A de https://developers.google.com/maps/documentation/places/web-service/place-types: `hospital` (y `medical_clinic` si existe), `pharmacy`, `restaurant`, `tourist_attraction`, `museum`, `art_gallery` y `park`. La tarea 1.6 verifica cada tipo y registra el resultado en `C/qa/place-types.md`.
- Si la Table A no tiene tipo de clínica, la categoría se renombra a «Hospitales» y las specs y la propuesta se actualizan. Mientras la verificación no esté hecha, el nombre sigue siendo «Hospitales y clínicas».
- La categoría seleccionada al abrir es `HospitalsAndClinics`. La selección vive en `MapViewModel` y no se guarda (`places-map`).

### D7. Marcadores con el nombre siempre visible (U5, P7)

- Cada lugar es un `MarkerComposable` con un pin y debajo una etiqueta (`Surface` redondeada con `Text`) con el nombre, cortado con «…» a partir de 28 caracteres.
- `title` es el nombre completo y `snippet` la distancia (p. ej. «a 350 m»), para la burbuja al tocar.
- Los `keys` del `MarkerComposable` incluyen el id y el nombre, para no reutilizar bitmaps viejos.
- Alternativa descartada: solo `title`, porque en Google se ve únicamente al tocar, y U5 pide el nombre siempre visible.
- Las etiquetas pueden superponerse en zonas densas; se acepta (Risks).

### D8. Radio en DataStore Preferences (U4, P8)

- `RadiusPreferences` (interfaz) y su implementación con `androidx.datastore:datastore-preferences` 1.3.0 en un `DataStore<Preferences>` del contexto de la aplicación. La clave es `search_radius_km` (Float).
- El repositorio expone `Flow<Float>`: si no hay valor devuelve 1 km y ajusta al rango de 0,5 a 10 km cualquier valor que llegue fuera de él. Esas reglas son funciones puras (`RadiusRules`) probadas en la JVM, incluido el formato es-CO.
- Se guarda al soltar el `Slider` (`onValueChangeFinished`), no en cada movimiento.
- Alternativa descartada: `SharedPreferences`, que es la API antigua y bloquea en la carga inicial. Alternativa descartada: Room/SQLite, excesivo para guardar un único valor.
- El efecto en el mapa (repetir la búsqueda al volver con un radio distinto) se decide cuando el mapa vuelve a estar visible, comparando el radio guardado con el último radio buscado; no se reacciona en vivo al cambio. Así «Radio guardado sin volver» de `search-radius-settings` se cumple sin lógica extra. Supuesto menor.

### D9. Navegación manual, contenedor y ViewModels (P9)

- Navegación como en el taller 8: una clase `Screen` sellada con `Map` y `Settings`, un `NavigationViewModel` y un `BackHandler` en Ajustes. No hay librería de navegación.
- `LugaresApplication` crea una vez el contenedor, con las implementaciones de `LocationProvider` (Fused), `AddressResolver` (Geocoder), `PlacesRepository` (Places SDK) y `RadiusPreferences` (DataStore), detrás de interfaces. Las pruebas sustituyen esas interfaces por fakes. Places se inicializa aquí (D5).
- `MapViewModel` y `SettingsViewModel` exponen `StateFlow` de UI. Las fábricas se escriben a mano con `viewModelFactory { initializer { } }`, sin framework de inyección, como en el taller 8.
- La búsqueda de lugares se modela como un `Job` dentro de `MapViewModel`. Cada búsqueda nueva cancela la anterior (`Job.cancel()` y la cancelación de la `Task` de Places), y el `Job` de la búsqueda se cancela también al cerrarse el ViewModel. Cada búsqueda lleva un identificador; un resultado con un identificador viejo se descarta.
- Si la llave es `DEFAULT_API_KEY`, `MapViewModel` pasa al estado «Falta configurar la llave de Google Maps» y no inicia ninguna búsqueda (D11).
- Textos en `strings.xml`; el dominio (`PlaceCategory`, `Place`, `NearbySearchParams`, `PlaceSearchRules`, `PlacesErrorMapper`, `DistanceCalculator` y `RadiusRules`) es Kotlin puro.
- Alternativa descartada: Hilt o Koin. El taller 8 no los usa y el reto no los pide.

### D10. Librerías y contingencias del grupo 1 (P10)

- Las librerías de la plantilla se suben a su última estable. Kotlin sube a 2.4.21 y el plugin de Compose a la misma versión (U11): `maps-compose` 9.0.1 y `places` 6.0.2 exigen `kotlin-stdlib` 2.4.x, que Kotlin 2.2.10 no puede leer. Por U12 también suben AGP a 9.4.1 y Gradle a 9.8.2, y los plugins del build quedan en su última estable (foojay-resolver-convention 1.0.0, secretos 2.0.1): el principio es «última estable para todo» (U11, U12). Alternativa descartada: bajar a `maps-compose` 8.3.1 y `places` 5.3.0, que sí compilan con Kotlin 2.2.10.
- Dependencias nuevas, en su última estable: `maps-compose` 9.0.1, `places` 6.0.2, `play-services-location` 21.4.0 y `datastore-preferences` 1.3.0. Además `lifecycle-viewmodel-compose`, `material-icons-core`, y solo para pruebas `kotlinx-coroutines-test`.
- Plugin `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` 2.0.1, que lee `secrets.properties` y, si no existe, `local.defaults.properties`. Su compatibilidad con AGP 9.4 no está verificada. Contingencia: si falla, se lee `secrets.properties` a mano en `app/build.gradle.kts` y se pasa la llave con `manifestPlaceholders["MAPS_API_KEY"]` y `buildConfigField`.
- `buildFeatures.buildConfig = true`, para leer la llave en Kotlin.
- Contingencias del grupo 1: si DataStore 1.3.0 no resuelve o no compila, se usa la última 1.2.x. Si el plugin de secretos no funciona con AGP 9.4, se aplica la contingencia manual. Si el mapa o Places no funcionan en el emulador con una llave válida, se detiene el trabajo y se consulta al usuario, en lugar de cambiar de SDK en silencio. Todo se registra en `C/qa/build-probe.md`. Por U12, `compileOptions` y el `jvmTarget` de Kotlin pasan a Java 17, porque `maps-compose` 9.0.1 está compilado para JVM 17 y con Java 11 no compila `rememberCameraPositionState`.

### D11. Llave de API fuera de git (U9, P13)

- La persona que usa el proyecto crea la llave en Google Cloud (ver P13 en `proposal.md`). La llave queda restringida a apps Android con el paquete `com.edu.unal.lugarescercanos` y la SHA-1 del keystore de depuración de este Mac, y solo a Maps SDK for Android y Places API (New). Tiene una cuota diaria baja para Nearby Search, p. ej. 200 por día, como tope de costo.
- La llave se guarda en `9AccessingGPS/secrets.properties` como `MAPS_API_KEY=...`. Ese archivo está en `.gitignore`.
- `9AccessingGPS/local.defaults.properties` se versiona con `MAPS_API_KEY=DEFAULT_API_KEY`, para que el proyecto compile sin llave.
- El manifest declara `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}"/>`. Places se inicializa con `BuildConfig.MAPS_API_KEY` (D5).
- Si la llave es `DEFAULT_API_KEY`, el mapa muestra «Falta configurar la llave de Google Maps» y no intenta buscar. Es el estado de P11 y de `places-map`.
- La llave nunca aparece en artefactos, registros, evidencia de QA ni commits. El grep de la tarea 7.6 busca el patrón de llaves de Google (`AIza[0-9A-Za-z_-]{35}`).
- Alternativa descartada: una llave sin restricciones. Google responsabiliza al dueño del cobro por el abuso de llaves sin restringir.
- Alternativa descartada: escribir la llave en `local.properties` o en el código. Es fácil de filtrar y no se separa del código versionado.

## Risks / Trade-offs

- **Costo y facturación**: el SKU Pro de Nearby Search tiene cupo gratis mensual, y después se cobra por búsqueda. → Se mitiga con la llave restringida, la cuota diaria y solo campos Pro. Google responsabiliza al dueño del cobro por abuso de llaves sin restringir.
- **Fuga de la llave**: si la llave se filtra, otra persona puede consumir la cuota. → `secrets.properties` va ignorado por git, la llave está restringida por paquete y SHA-1, y ningún artefacto ni registro la contiene (D11).
- **Límite de 20 resultados**: con radios grandes en zonas densas, solo se ven los 20 lugares más cercanos. → Se acepta; el encargo lo aprobó como diferencia de Google (U9).
- **Compatibilidad sin verificar**: el plugin de secretos con AGP 9.4.1, Places 6.0.2 y maps-compose 9.0.1 con Kotlin 2.4.21 y compileSdk 37. → El grupo 1 lo prueba antes de seguir y registra las contingencias (D10).
- **Kotlin 2.4 con AGP 9.4.1 y Gradle 9.8.2**: AGP 9.4.1 trae por defecto el KGP 2.2.10 y se usa un KGP más nuevo. → Se verificó en el grupo 1 que el build compila; cualquier incompatibilidad de AGP con Kotlin 2.4 se registra en `C/qa/build-probe.md`.
- **Google Play services**: el emulador y el celular lo necesitan para la ubicación y para el mapa. → Se usan imágenes `google_apis`.
- **Resultados viejos**: una búsqueda antigua que llega tarde se descarta por identificador (D5, D9).
- **SHA-1 distinta**: si el APK se compila en otra máquina, cambia la huella y habría que registrarla en la llave. → La instalación en el celular (tarea 7.5) usa la misma SHA-1, porque se compila en este Mac.
- **Filtro por radio en el cliente**: el encargo no pide descartar en el cliente los lugares fuera del radio; el filtro de `PlaceSearchRules` es una defensa adicional. → Se mantiene para que el escenario «Lugar fuera del radio» de la spec se cumpla. Si el usuario no lo quiere, se quita.
- **Geocoder sin backend**: el `Geocoder` puede no tener backend o fallar sin red. → Solo se muestran las coordenadas y «Dirección no disponible» (P3).
- **Aproximación de la ubicación**: con solo ubicación aproximada, el radio y los lugares son menos precisos. → Se avisa al usuario (`current-location`).
- **Etiquetas superpuestas**: en zonas densas, las etiquetas de nombre pueden superponerse. → Se acepta; el límite de 20 lo acota.

## Migration Plan

No hay datos existentes que migrar: la app no tiene persistencia previa, y DataStore se crea en el primer uso. Para revertir, basta con revertir los commits de los grupos del cambio, en orden inverso; cada grupo se versiona por separado (U6 y `tasks.md`).
