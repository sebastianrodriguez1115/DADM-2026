# nearby-places-search Specification

## Purpose

Define las categorías de lugares que la aplicación puede buscar con los tipos de lugar de Google, cómo se consulta Places API (New) dentro del radio, qué campos se piden, cómo se filtran, ordenan y limitan los resultados, y qué ocurre cuando la búsqueda falla.

## Requirements

### Requirement: Categorías de lugares

La aplicación SHALL ofrecer exactamente cinco categorías, en este orden: Hospitales y clínicas, Farmacias, Restaurantes, Sitios turísticos y Parques. Al abrirse, la categoría seleccionada MUST ser Hospitales y clínicas. Cada categoría MUST corresponder a un conjunto de tipos de lugar de Google, y los nombres visibles MUST estar en español.

#### Scenario: Categorías disponibles

- **GIVEN** la aplicación recién abierta
- **WHEN** se muestran las categorías
- **THEN** aparecen Hospitales y clínicas, Farmacias, Restaurantes, Sitios turísticos y Parques, en ese orden, y Hospitales y clínicas está seleccionada

#### Scenario: Hospitales y clínicas incluye los dos tipos

- **GIVEN** un lugar de tipo `hospital` y otro de tipo de clínica (según la verificación de la Table A en `tasks.md`), ambos con nombre y dentro del radio
- **WHEN** se busca la categoría Hospitales y clínicas
- **THEN** la petición incluye los dos tipos y ambos lugares aparecen en los resultados

#### Scenario: Sitios turísticos incluye los tipos de interés turístico

- **GIVEN** la categoría Sitios turísticos
- **WHEN** la aplicación arma la petición
- **THEN** la petición incluye `tourist_attraction`, `museum` y `art_gallery`, solo los que estén en la Table A, y no incluye `lodging`

### Requirement: Consulta de lugares dentro del radio

La aplicación MUST buscar los lugares de la categoría seleccionada con Places API (New), Nearby Search, usando una restricción circular centrada en la ubicación actual con radio igual al radio configurado en metros. La búsqueda MUST pedir como máximo 20 resultados, ordenados por distancia (`DISTANCE`), y MUST NOT paginar.

#### Scenario: Búsqueda con resultados

- **GIVEN** una ubicación en Bogotá, un radio de 1 km y la categoría Farmacias
- **WHEN** la aplicación busca los lugares
- **THEN** la petición se envía a Nearby Search con un círculo de 1000 metros, el tipo `pharmacy`, `maxResultCount` de 20 y `rankPreference` de `DISTANCE`, y la respuesta se muestra como resultados

#### Scenario: Sin paginación

- **GIVEN** una búsqueda que devuelve 20 lugares
- **WHEN** la aplicación procesa la respuesta
- **THEN** no pide una segunda página y muestra los 20 lugares

### Requirement: Campos de nivel Pro

La búsqueda MUST pedir solo los campos `ID`, `DISPLAY_NAME` y `LOCATION`. La búsqueda MUST NOT pedir campos de nivel Enterprise, como rating, teléfono, horario o sitio web, porque aumentan el costo.

#### Scenario: Solo campos Pro

- **GIVEN** una búsqueda en curso
- **WHEN** la aplicación construye la petición
- **THEN** los campos pedidos son exactamente `ID`, `DISPLAY_NAME` y `LOCATION`

### Requirement: Filtrado, orden y límite de resultados

La aplicación MUST descartar los lugares sin nombre o sin ubicación, y los lugares que quedan a más de radio de la ubicación. Los resultados restantes MUST ordenarse por distancia de menor a mayor y MUST mostrarse como máximo 20. La distancia MUST calcularse con la fórmula de haversine.

#### Scenario: Lugares sin nombre o sin ubicación

- **GIVEN** una respuesta con un lugar sin nombre, un lugar sin ubicación y un lugar con nombre y ubicación
- **WHEN** la aplicación procesa la respuesta
- **THEN** solo el lugar con nombre y ubicación aparece en los resultados

#### Scenario: Lugar fuera del radio

- **GIVEN** un radio de 1 km y un lugar cuya ubicación está a 1,4 km de la ubicación del usuario
- **WHEN** la aplicación procesa la respuesta
- **THEN** el lugar no aparece en los resultados

#### Scenario: Orden por distancia

- **GIVEN** tres lugares a 900 m, 200 m y 500 m dentro del radio
- **WHEN** se muestran los resultados
- **THEN** aparecen en el orden 200 m, 500 m y 900 m

#### Scenario: Límite de 20 resultados

- **GIVEN** 25 lugares con nombre dentro del radio
- **WHEN** se muestran los resultados
- **THEN** se muestran los 20 más cercanos y no más

### Requirement: Errores de la búsqueda

Cuando la búsqueda falla, la aplicación MUST mostrar un mensaje según la causa y MUST ofrecer el botón «Reintentar». La aplicación MUST NOT reintentar la búsqueda de forma automática.

#### Scenario: Sin conexión

- **GIVEN** el dispositivo no tiene conexión a internet
- **WHEN** la aplicación busca los lugares
- **THEN** se muestra «Sin conexión a internet» con el botón «Reintentar»

#### Scenario: Tiempo de espera agotado

- **GIVEN** el servicio no responde antes del tiempo de espera
- **WHEN** la aplicación busca los lugares
- **THEN** se muestra «Sin conexión a internet» con el botón «Reintentar»

#### Scenario: Cuota agotada

- **GIVEN** el servicio responde con `OVER_QUERY_LIMIT` o la cuota diaria está agotada
- **WHEN** la aplicación busca los lugares
- **THEN** se muestra «El servicio de lugares alcanzó su límite. Intenta más tarde» con el botón «Reintentar»

#### Scenario: Llave inválida o sin permisos

- **GIVEN** el servicio responde con `REQUEST_DENIED`, o la llave de Google Maps no es válida o no tiene permisos para Places API (New)
- **WHEN** la aplicación busca los lugares
- **THEN** se muestra «La llave de Google Maps no es válida o no tiene permisos» con el botón «Reintentar»

#### Scenario: Otro error del servicio

- **GIVEN** el servicio responde con un código de error distinto de los anteriores
- **WHEN** la aplicación busca los lugares
- **THEN** se muestra «El servicio de lugares no respondió» con el botón «Reintentar»

#### Scenario: Sin reintentos automáticos

- **GIVEN** una búsqueda que acaba de fallar
- **WHEN** el usuario no pulsa «Reintentar»
- **THEN** la aplicación no vuelve a buscar

#### Scenario: Reintentar

- **GIVEN** un error de búsqueda mostrado en pantalla
- **WHEN** el usuario pulsa «Reintentar»
- **THEN** la aplicación repite la búsqueda con la misma categoría y el mismo radio

#### Scenario: Búsqueda nueva cancela la anterior

- **GIVEN** una búsqueda en curso
- **WHEN** el usuario cambia de categoría o de radio
- **THEN** la búsqueda anterior se cancela y solo cuenta el resultado de la nueva

#### Scenario: Resultado tardío descartado

- **GIVEN** una búsqueda cancelada cuya `Task` no se pudo cancelar
- **WHEN** su resultado llega después de la búsqueda nueva
- **THEN** el resultado viejo se ignora y no se muestra
