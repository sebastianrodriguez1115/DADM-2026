# 1.9 Búsqueda Nearby real

Prueba instrumentada temporal (ya retirada del repositorio): `Places.initializeWithNewPlacesApiEnabled`, `searchNearby` con `CircularBounds` de 1000 m en 4.6381, -74.0840, `includedTypes = ["hospital"]`, `maxResultCount = 20`, `rankPreference = DISTANCE` y campos `ID`, `DISPLAY_NAME` y `LOCATION`; espera con `Tasks.await(task, 30, SECONDS)`.

Resultado: **20 lugares** devueltos. Primero: «Clínica Nicolás de Federman», a unos **515 m** del punto (haversine con `DistanceCalculator`).

Historial: las dos primeras ejecuciones fallaron con `ApiException` `statusCode` 9011 (`REQUEST_DENIED`, Places API (New) no habilitada en el proyecto). Tras habilitarla el usuario, la ejecución pasó.
