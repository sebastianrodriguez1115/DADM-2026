# 1.9 Búsqueda Nearby real

Prueba instrumentada temporal (ya retirada del repositorio): `Places.initializeWithNewPlacesApiEnabled`, `searchNearby` con `CircularBounds` de 1000 m en 4.6381, -74.0840, `includedTypes = ["hospital"]`, `maxResultCount = 20`, `rankPreference = DISTANCE` y campos `ID`, `DISPLAY_NAME` y `LOCATION`; espera con `Tasks.await(task, 30, SECONDS)`.

Resultado (dos ejecuciones, separadas por 90 s): **falla** con `ApiException` `statusCode` = **9011** (`PlacesStatusCodes.REQUEST_DENIED`).

Mensaje del servidor: «Places API (New) has not been used in project <número de proyecto> before or it is disabled. Enable it ... then retry.»

Significado: la API «Places API (New)» no está habilitada en el proyecto de Google Cloud de la llave. Es una acción del usuario en la consola (tarea 1.7). La llave y el mapa funcionan (la tarea 1.8 pasó). No hay número de lugares, nombre ni distancia todavía.
