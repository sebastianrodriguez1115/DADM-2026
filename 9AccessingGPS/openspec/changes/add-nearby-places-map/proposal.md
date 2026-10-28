# Proposal

## Why

El reto 9 pide una aplicación que, a partir de la posición GPS del usuario, muestre en un mapa los puntos de interés cercanos (hospitales, lugares turísticos, etc.) dentro de un radio en kilómetros configurable, y que cada punto muestre su nombre. La base actual del proyecto es la plantilla Hello World ya renombrada (`com.edu.unal.lugarescercanos`, «Lugares cercanos») con una sola `MainActivity` en Compose: no tiene ubicación, mapa, búsqueda ni preferencias.

## What Changes

- Obtener la ubicación actual del dispositivo con el GPS, bajo permiso en tiempo de ejecución, y mostrar sus coordenadas y su dirección (U2, P1, P2, P3).
- Mostrar un mapa de Google Maps (Maps SDK for Android con Maps Compose) centrado en la ubicación del usuario, con un marcador propio, un círculo que representa el radio de búsqueda y el logo de Google visible (U9, P6).
- Buscar con Places API (New), Nearby Search, los lugares de una categoría dentro del radio, y mostrar hasta 20 marcadores ordenados por distancia (U9, U3, P4, P5).
- Mostrar el nombre de cada lugar junto a su marcador, sin necesidad de tocarlo (U5, P7).
- Elegir la categoría con un selector sobre el mapa que muestra una categoría a la vez; la categoría inicial es «Hospitales y clínicas» (U3, P5).
- Configurar el radio de búsqueda, de 0,5 a 10 km, en una pantalla de Ajustes guardada con Jetpack DataStore Preferences (U4, P8).
- Navegar entre el mapa y los Ajustes con navegación manual, sin librería de navegación (P9).
- Leer la llave de Google Maps desde `secrets.properties`, que no se versiona, y mostrar un estado claro si falta (U9, P13).
- Mostrar de forma visible los estados de carga, permisos, ubicación desactivada, errores de búsqueda, llave faltante y resultados vacíos (P11).
- Añadir pruebas automáticas en la JVM y pruebas instrumentadas en el emulador, y registrar la verificación manual con ubicación simulada (P12, U7).

## Capabilities

### New Capabilities

- `current-location`: permisos de ubicación, lectura puntual de la ubicación del dispositivo, coordenadas y dirección de esa posición, y sus errores (P1, P2, P3).
- `nearby-places-search`: categorías con los tipos de lugar de Google, consulta a Places API (New) Nearby Search con campos de nivel Pro, filtrado, orden, límite de 20 resultados y errores de la búsqueda (P4, P5).
- `places-map`: mapa de Google Maps, logo de Google visible, lugares de Google ocultos, marcador del usuario, círculo del radio, marcadores con nombre siempre visible, selector de categoría, encuadre, botón de actualizar, llave faltante y estados visibles en el mapa (P6, P7, P11, P13).
- `search-radius-settings`: pantalla de Ajustes, rango y valor por defecto del radio, persistencia y efecto al volver al mapa (P8).

### Modified Capabilities

Ninguna: `openspec/specs/` está vacío.

## Impact

- Archivos de build: `gradle/libs.versions.toml`, `app/build.gradle.kts` y el wrapper de Gradle (`gradle/wrapper/*`, `gradlew`, `gradlew.bat`). Se actualizan las librerías de la plantilla y se añaden `maps-compose` 9.0.1, `places` 6.0.2, `play-services-location` 21.4.0, `datastore-preferences` 1.3.0, `lifecycle-viewmodel-compose` y `material-icons-core`; solo para pruebas, `kotlinx-coroutines-test`. Se añade el plugin `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` 2.0.1 (P10, P13). Se activa `buildFeatures.buildConfig` para leer la llave en Kotlin. Todo el build sube a su última estable (U11, U12, P10): AGP 9.4.1, Gradle 9.8.2 y Kotlin 2.4.21. Se quitan osmdroid y `org.json`.
- Archivos de la llave: `9AccessingGPS/local.defaults.properties`, versionado, con `MAPS_API_KEY=DEFAULT_API_KEY` para que el proyecto compile sin llave; y `9AccessingGPS/secrets.properties`, local, con la llave real, que no se versiona (P13, U9).
- Manifest: permisos `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET` y `ACCESS_NETWORK_STATE`; `meta-data` `com.google.android.geo.API_KEY` con `${MAPS_API_KEY}`; y la declaración de la clase `Application` (P2, P9, P13). No se pide ningún permiso de almacenamiento.
- Código nuevo bajo `app/src/main/java/com/edu/unal/lugarescercanos/` (dominio, datos, ubicación, búsqueda, preferencias e interfaz), una clase `Application`, textos en `app/src/main/res/values/strings.xml` y el estilo del mapa en `app/src/main/res/raw/map_style.json` (P6, P9).
- Pruebas nuevas en `app/src/test/` y `app/src/androidTest/`.
- Evidencia de verificación en `openspec/changes/add-nearby-places-map/qa/` y `openspec/changes/add-nearby-places-map/verification.md`. Ningún archivo de evidencia incluye la llave.
- `9AccessingGPS/.gitignore`: se añade `secrets.properties` (U9).
- Servicios externos: Google Maps Platform (Maps SDK for Android y Places API (New)), en un proyecto de Google Cloud con facturación activada, creado por el usuario. Nearby Search usa el SKU Pro, con 5.000 búsquedas gratis al mes y luego USD 32 por 1.000 (P4, P13). Se aplican los términos de Google y su política de Places, que obliga a mostrar sus resultados sobre un mapa de Google (ver Risks en `design.md`).

## Out of Scope

- Entrada manual de dirección o de coordenadas (U2).
- Seguimiento continuo de la ubicación.
- Rutas o navegación hasta un lugar.
- Búsqueda por texto libre.
- Detalles extra del lugar, como teléfono, horario, sitio web o fotos. Los campos de nivel Enterprise (rating, teléfono, horario, web) quedan fuera porque suben el costo (U9, P4).
- Varias categorías a la vez.
- Guardar la categoría elegida entre sesiones.
- Mapas o teselas sin conexión y precarga de zonas, que Google no permite en esta API.
- OpenStreetMap, osmdroid, MapLibre y Overpass (U9).
- Map ID y estilos en la nube (U9, P6).
- Más de 20 lugares por búsqueda o paginación (U9, P4).
- Campos de Places de nivel Enterprise (rating, teléfono, horario, web) (U9, P4).
- Reintentos automáticos.
- Ícono propio de la aplicación.
- Modo oscuro del mapa.
- Otros idiomas distintos del español.
- La conservación del estado al girar el dispositivo como requisito.

## Assumptions Confirmed by the User

Las decisiones U1 a U12 fueron confirmadas por el usuario el 2026-10-27. U1 queda corregida por U9.

- **U1.** (Corregida por U9.) Se había decidido un mapa de OpenStreetMap con osmdroid, con los lugares tomados de OpenStreetMap por la API Overpass. Google Maps quedaba descartado. Ya no rige.
- **U2.** No se incluye la opción de escribir una dirección o unas coordenadas a mano: la ubicación sale solo del GPS del dispositivo. La app muestra las coordenadas y la dirección de esa posición.
- **U3.** Los lugares se buscan por categoría, con un selector sobre el mapa que muestra una categoría a la vez. Las categorías deben dar resultados en Bogotá, donde se usará la app.
- **U4.** El radio de búsqueda se configura en una pantalla de preferencias y se guarda con Jetpack DataStore Preferences, la opción más usada hoy (el curso no exige ninguna en particular).
- **U5.** El nombre de cada lugar va siempre visible junto a su marcador, sin tener que tocarlo.
- **U6.** Todos los cambios del taller se gestionan con OpenSpec.
- **U7.** Las pruebas se hacen en el emulador con ubicación simulada en Bogotá. Al final la app se instala en el celular del usuario para presentarla en clase.
- **U8.** Paquete `com.edu.unal.lugarescercanos` y nombre visible «Lugares cercanos». Ya está hecho en la base actual.
- **U9.** Se usa Google Maps en lugar de OpenStreetMap, con Maps SDK for Android (Maps Compose) y Places API (New). El usuario crea el proyecto de Google Cloud con la facturación activada y la llave de API. El usuario acepta las diferencias de Google: máximo 20 lugares por búsqueda, tipos de lugar de Google, marcadores dibujados con `MarkerComposable`, los lugares propios de Google ocultos con un estilo, y una llave fuera de git restringida por paquete y SHA-1, con límites diarios de uso.
- **U10.** El usuario aprobó el resto del plan. P1, P2, P3, P8, P9 y P11 pasan a confirmadas el 2026-10-27. P4, P5, P6, P7, P10 y P12 se reescriben o se adaptan para Google (ver abajo) y quedan confirmadas el 2026-10-27, adaptadas a Google Maps por U9. P13 queda confirmada al elegir Google.
- **U11.** El usuario permite cambiar la versión de Kotlin (2026-10-27).
- **U12.** El usuario quita la restricción de no cambiar versiones (2026-10-27): se puede actualizar cualquier versión, incluidas AGP y Gradle, y se aplica «última estable» a todo.

Las propuestas P1 a P13 son del orquestador y se diseñan en `design.md`. Las que dicen «(Confirmada el 2026-10-27.)» se aprobaron tal cual. Las que dicen «adaptada a Google Maps por U9» se reescribieron para Google, y P11 lleva además el estado de llave faltante de P13.

- **P1. (Confirmada el 2026-10-27.)** Ubicación con `FusedLocationProviderClient` de `com.google.android.gms:play-services-location` 21.4.0, la API a la que apunta la nota 1 del enunciado. Se pide una lectura puntual con `getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, ...)`, sin seguimiento continuo. Se lee al abrir la app una vez concedido el permiso, y al pulsar «Actualizar ubicación». Un resultado nulo es el error «No se pudo obtener la ubicación». Antes de pedirla se comprueba si la ubicación del dispositivo está activada.
- **P2. (Confirmada el 2026-10-27.)** `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` se piden en tiempo de ejecución. Si solo se concede la aproximada, la app sigue funcionando y lo indica. Si se deniega, muestra un mensaje y el botón «Conceder permiso». Si la denegación es permanente, el botón pasa a «Abrir ajustes». En el manifest también van `INTERNET` y `ACCESS_NETWORK_STATE`; no se pide ningún permiso de almacenamiento.
- **P3. (Confirmada el 2026-10-27.)** La dirección sale de `android.location.Geocoder` en su variante asíncrona, solo si `Geocoder.isPresent()`. Si falla, no hay servicio o no hay resultados, se muestra «Dirección no disponible» y las coordenadas, con 5 decimales, siguen visibles en la tarjeta «Tu ubicación».
- **P4. (Confirmada el 2026-10-27, adaptada a Google Maps por U9.)** Búsqueda con Places SDK for Android (New), librería `com.google.android.libraries.places:places` 6.0.2, la última en Google Maven el 2026-10-27.
  - Inicialización en `Application`: `Places.initializeWithNewPlacesApiEnabled(context, apiKey)`.
  - Búsqueda: `PlacesClient.searchNearby(SearchNearbyRequest)`, con restricción `CircularBounds` centrada en la ubicación del usuario y radio = km × 1000; `includedTypes` según la categoría; `maxResultCount` = 20, el máximo de la API, sin paginación; `rankPreference` = `DISTANCE`.
  - Campos: solo `ID`, `DISPLAY_NAME` y `LOCATION`, para quedar en el SKU Pro (5.000 búsquedas gratis al mes; luego USD 32 por 1.000). Se prohíben los campos Enterprise, como rating, teléfono, horario o web, porque suben el costo.
  - Resultados: se descartan los lugares sin nombre o sin ubicación. La distancia de cada lugar se calcula con haversine en Kotlin puro, para la burbuja «a 350 m» o «a 1,2 km». Se conserva el orden por distancia.
  - Errores (`ApiException` y su `statusCode`), sin reintentos automáticos y con «Reintentar»: red o tiempo de espera, «Sin conexión a internet»; `OVER_QUERY_LIMIT` o cuota agotada, «El servicio de lugares alcanzó su límite. Intenta más tarde»; `REQUEST_DENIED` o llave inválida, «La llave de Google Maps no es válida o no tiene permisos»; cualquier otro, «El servicio de lugares no respondió».
  - Una búsqueda nueva cancela la anterior. Si la `Task` de Places no se puede cancelar, se ignora el resultado viejo.
  - Reemplaza la consulta anterior a Overpass. Desaparecen `HttpURLConnection`, `org.json`, el parser JSON, el límite de 100 y el `out center 300` (U9).
  - La política de Places obliga a mostrar sus resultados sobre un mapa de Google: https://developers.google.com/maps/documentation/places/web-service/policies
- **P5. (Confirmada el 2026-10-27, adaptada a Google Maps por U9.)** Cinco categorías, en este orden, con «Hospitales y clínicas» seleccionada al abrir la app. Los tipos de Google salen de la Table A de https://developers.google.com/maps/documentation/places/web-service/place-types:
  - Hospitales y clínicas: `hospital`, más un tipo de clínica si existe en la Table A (p. ej. `medical_clinic`). Si no existe, la categoría pasa a llamarse «Hospitales» y usa solo `hospital`; ese cambio obliga a actualizar las specs y la propuesta.
  - Farmacias: `pharmacy`.
  - Restaurantes: `restaurant`.
  - Sitios turísticos: `tourist_attraction`, `museum` y `art_gallery`, solo los que estén en la Table A.
  - Parques: `park`.
  - La tarea del grupo 1 verifica cada tipo contra la Table A y registra el resultado. La elección de la categoría no cambia el selector ni se guarda entre sesiones.
- **P6. (Confirmada el 2026-10-27, adaptada a Google Maps por U9.)** Mapa con `com.google.maps.android:maps-compose` 9.0.1, con el composable `GoogleMap` y `CameraPositionState`. Según su README ya no hace falta declarar `play-services-maps` aparte.
  - Vista inicial en la Plaza de Bolívar (4.5981, -74.0760, zoom 12) mientras llega la ubicación.
  - Marcador propio «Tu ubicación» con un color distinto (p. ej. `BitmapDescriptorFactory.HUE_AZURE`). No se usa la capa `isMyLocationEnabled`: así el marcador coincide con la ubicación usada en la búsqueda y no aparece el botón por defecto.
  - `Circle` semitransparente con el radio en metros.
  - Encuadre con un `LatLngBounds` de los cuatro puntos extremos del círculo (centro ± radio), aplicado con `CameraUpdateFactory.newLatLngBounds(bounds, padding)` solo después de `onMapLoaded`, porque antes de que el mapa tenga tamaño lanza `IllegalStateException`.
  - Se ocultan los lugares propios de Google con `MapStyleOptions`, leído de `res/raw/map_style.json` (`featureType: "poi"`, `visibility: "off"`).
  - Sin Map ID: el Maps SDK móvil sin Map ID es gratis e ilimitado; con Map ID se cobra como Dynamic Maps; y el estilo JSON no se combina con estilos en la nube.
  - El logo de Google, que es la atribución, debe quedar visible: la interfaz no lo tapa. Los insets se aplican con `contentPadding` o el `innerPadding` del `Scaffold`.
- **P7. (Confirmada el 2026-10-27, adaptada a Google Maps por U9.)** Cada lugar tiene un `MarkerComposable`: un pin y debajo una etiqueta (`Surface` redondeada con `Text`) con el nombre, cortado con «…» a partir de 28 caracteres. `title` es el nombre completo y `snippet` la distancia, para la burbuja al tocar. Los `keys` del `MarkerComposable` incluyen el id y el nombre, para no reutilizar bitmaps viejos. `title` solo no basta, porque en Google se ve únicamente al tocar.
- **P8. (Confirmada el 2026-10-27.)** La pantalla «Ajustes» se abre desde un ícono de engranaje en la barra superior del mapa. Tiene un `Slider` de 0,5 a 10 km en pasos de 0,5 km, con 1 km por defecto y el valor con formato es-CO («1,5 km»). Se guarda al soltar el slider. Al volver al mapa con un radio distinto, la búsqueda se repite si ya hay ubicación.
- **P9. (Confirmada el 2026-10-27.)** Navegación manual como en el taller 8 (`Screen`, `NavigationViewModel` y `BackHandler`). Una clase `Application` crea las implementaciones de la ubicación, la dirección, los lugares y las preferencias, detrás de interfaces que permiten usar fakes en las pruebas. El dominio es Kotlin puro. Todos los textos van en `strings.xml`.
- **P10. (Confirmada el 2026-10-27, adaptada a Google Maps por U9 y a las versiones del build por U11 y U12.)** Las librerías de la plantilla (AndroidX, BOM de Compose y de pruebas) se suben a su última versión estable, como en los talleres 7 y 8. Las dependencias nuevas entran en su última estable: `maps-compose` 9.0.1, `places` 6.0.2, `play-services-location` 21.4.0 y `datastore-preferences` 1.3.0. Se añade el plugin `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` 2.0.1; su compatibilidad con AGP 9.4 no está verificada. Si no funciona, se lee `secrets.properties` a mano en `app/build.gradle.kts` y se pasa la llave con `manifestPlaceholders["MAPS_API_KEY"]` y `buildConfigField`. Kotlin sube a la última estable 2.4.x (2.4.21) y el plugin `org.jetbrains.kotlin.plugin.compose` a la misma versión, porque `maps-compose` 9.0.1 y `places` 6.0.2 exigen `kotlin-stdlib` 2.4.x, que Kotlin 2.2.10 no puede leer (adaptada por U11). Por U12 rige el principio de «última estable para todo»: AGP 9.4.1, Gradle 9.8.2, Kotlin 2.4.21, `org.gradle.toolchains.foojay-resolver-convention` 1.0.0 y el plugin de secretos 2.0.1 (las dos últimas ya eran las más recientes). Si DataStore 1.3.0 no compila, se usa la última 1.2.x. Si el mapa o Places no funcionan en el emulador con una llave válida, se detiene el trabajo y se consulta al usuario.
- **P11. (Confirmada el 2026-10-27, con el estado de llave faltante añadido por U9 y P13.)** Estados visibles en el mapa: cargando la ubicación y cargando lugares; sin permiso; permiso permanentemente denegado; ubicación del dispositivo desactivada; ubicación no obtenida; errores de búsqueda (P4); llave de Google Maps no configurada, con el texto «Falta configurar la llave de Google Maps», en la que no se busca lugares; sin resultados, con el texto «No se encontraron lugares de «<categoría>» a menos de <radio>»; y el contador «<n> lugares» cuando hay resultados.
- **P12. (Confirmada el 2026-10-27, adaptada a Google Maps por U9.)** Pruebas en la JVM de `NearbySearchParams` (radio en metros, tipos, 20 resultados y `DISTANCE`), del filtrado y orden de `PlaceSearchRules`, de la distancia, de las reglas del radio, del mapeo de códigos de estado a tipos de error, y de los ViewModels con fakes. Pruebas instrumentadas en el emulador de la pantalla de Ajustes y de los estados del mapa. QA manual en el emulador con ubicación simulada en la Universidad Nacional de Colombia, con capturas y registro en `qa/` y `verification.md`.
- **P13. (Confirmada el 2026-10-27 al elegir Google.)** Llave de API, con la siguiente división de trabajo.
  - Lo que hace el usuario: crea el proyecto de Google Cloud con la facturación activada; habilita «Maps SDK for Android» y «Places API (New)»; crea una llave restringida a apps Android (paquete `com.edu.unal.lugarescercanos` y la SHA-1 del keystore de depuración de este Mac, `2D:6A:B6:1D:33:A9:DC:21:BC:34:3D:D9:09:A3:28:35:15:36:0D:A7`) y solo a esas dos APIs; fija una cuota diaria baja para Nearby Search de Places API (New), p. ej. 200 por día, como tope de costo; y guarda la llave en `9AccessingGPS/secrets.properties` como `MAPS_API_KEY=...`, archivo que no se versiona.
  - Lo que hace la app: se versiona `9AccessingGPS/local.defaults.properties` con `MAPS_API_KEY=DEFAULT_API_KEY`, para que compile sin llave; el manifest declara `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}"/>`; Places se inicializa con `BuildConfig.MAPS_API_KEY`; y si la llave es `DEFAULT_API_KEY`, el mapa muestra «Falta configurar la llave de Google Maps» y no intenta buscar (estado añadido a P11 y a `places-map`).
  - La llave nunca aparece en artefactos, registros, evidencia de QA ni commits.
