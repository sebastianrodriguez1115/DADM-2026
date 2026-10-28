# Spec Delta

## Purpose

Define la pantalla del mapa de Google Maps: el logo de Google visible, los lugares propios de Google ocultos, la posición del usuario, el círculo del radio, los lugares con su nombre siempre visible, el selector de categoría, el encuadre, la actualización de la ubicación, la llave de API faltante y los estados que el usuario ve.

## ADDED Requirements

### Requirement: Mapa de Google Maps

La pantalla del mapa SHALL mostrar un mapa de Google Maps con el logo de Google visible, que es la atribución, sin que la interfaz lo tape. Mientras la ubicación no llega, el mapa SHALL mostrar la Plaza de Bolívar de Bogotá con zoom 12. El mapa MUST ocultar los lugares propios de Google con un estilo y MUST NOT usar un Map ID.

#### Scenario: Logo visible

- **GIVEN** el mapa está en pantalla
- **WHEN** el usuario mira el mapa
- **THEN** se ve el logo de Google y ningún elemento de la interfaz lo tapa

#### Scenario: Vista inicial sobre Bogotá

- **GIVEN** la aplicación se acaba de abrir y la ubicación aún no llega
- **WHEN** se muestra el mapa
- **THEN** el mapa está centrado en la Plaza de Bolívar de Bogotá con zoom 12

#### Scenario: Lugares propios de Google ocultos

- **GIVEN** el mapa en pantalla con el estilo de la aplicación
- **WHEN** el usuario mira el mapa
- **THEN** no aparecen los iconos de los lugares propios de Google

### Requirement: Posición del usuario y radio

Cuando hay una ubicación, el mapa MUST mostrar un marcador de «Tu ubicación», distinto del de los lugares, y un círculo semitransparente con el radio de búsqueda alrededor de ella. El mapa MUST NOT mostrar el botón de ubicación por defecto del SDK.

#### Scenario: Marcador y círculo

- **GIVEN** una ubicación obtenida y un radio de 2 km
- **WHEN** se muestra el mapa
- **THEN** aparece el marcador «Tu ubicación» y un círculo semitransparente de 2 km de radio centrado en él

#### Scenario: Círculo al cambiar el radio

- **GIVEN** el mapa muestra un círculo de 1 km
- **WHEN** el radio cambia a 3 km en los ajustes y el usuario vuelve al mapa
- **THEN** el círculo se dibuja con 3 km de radio

### Requirement: Lugares con nombre siempre visible

Cada lugar encontrado MUST mostrarse como un `MarkerComposable` con su nombre visible junto al pin, sin necesidad de tocarlo. Los nombres de más de 28 caracteres MUST cortarse y terminar con «…». Al tocar un marcador, la aplicación MUST mostrar el nombre completo y la distancia a la ubicación del usuario, como «a 350 m» o «a 1,2 km».

#### Scenario: Nombre visible sin tocar

- **GIVEN** un lugar llamado «Hospital Universitario Nacional»
- **WHEN** se muestra el mapa con sus resultados
- **THEN** el nombre aparece junto al pin sin que el usuario lo toque

#### Scenario: Nombre largo cortado

- **GIVEN** un lugar cuyo nombre tiene 40 caracteres
- **WHEN** se muestra su marcador
- **THEN** la etiqueta muestra los primeros caracteres del nombre seguidos de «…»

#### Scenario: Detalle al tocar el marcador

- **GIVEN** un lugar a 1,2 km de la ubicación del usuario
- **WHEN** el usuario toca su marcador
- **THEN** aparece una burbuja con el nombre completo y «a 1,2 km»

### Requirement: Selector de categoría

El mapa MUST mostrar un selector de categoría sobre el mapa con las cinco categorías en una fila desplazable, en la que solo puede estar seleccionada una a la vez. Al elegir una categoría, la aplicación MUST buscar los lugares de esa categoría. La categoría elegida MUST NOT guardarse entre sesiones.

#### Scenario: Cambiar de categoría

- **GIVEN** la categoría Hospitales y clínicas seleccionada y resultados en el mapa
- **WHEN** el usuario selecciona Restaurantes
- **THEN** Restaurantes queda como única categoría seleccionada, la búsqueda se repite y el mapa muestra los restaurantes

#### Scenario: Categoría no se recuerda

- **GIVEN** el usuario seleccionó Parques y cierra la aplicación
- **WHEN** vuelve a abrir la aplicación
- **THEN** Hospitales y clínicas vuelve a estar seleccionada

### Requirement: Encuadre de los resultados

Después de cada búsqueda con resultados, el mapa MUST encuadrarse para mostrar el círculo completo del radio. El encuadre MUST aplicarse solo después de que el mapa esté cargado.

#### Scenario: Encuadre tras buscar

- **GIVEN** una ubicación y un radio de 5 km, con el mapa ya cargado
- **WHEN** termina una búsqueda con resultados
- **THEN** el mapa se encuadra para que se vea el círculo completo de 5 km

#### Scenario: Encuadre antes de cargar el mapa

- **GIVEN** una búsqueda que termina antes de que el mapa se haya cargado
- **WHEN** el mapa termina de cargarse
- **THEN** el mapa se encuadra para mostrar el círculo completo, sin que la aplicación falle

### Requirement: Actualizar la ubicación desde el mapa

El mapa MUST ofrecer un botón «Actualizar ubicación» que vuelve a leer la posición del usuario, como se define en `current-location`.

#### Scenario: Botón visible

- **GIVEN** el mapa en pantalla
- **WHEN** el usuario mira la pantalla
- **THEN** se ve el botón «Actualizar ubicación»

### Requirement: Acceso a los ajustes

El mapa MUST ofrecer un ícono de engranaje en la barra superior que abre la pantalla de Ajustes.

#### Scenario: Abrir ajustes

- **GIVEN** el mapa en pantalla
- **WHEN** el usuario pulsa el ícono de engranaje
- **THEN** se abre la pantalla de Ajustes

### Requirement: Llave de Google Maps no configurada

Si la llave de Google Maps es el valor por defecto `DEFAULT_API_KEY`, la aplicación MUST mostrar el texto «Falta configurar la llave de Google Maps» y MUST NOT iniciar ninguna búsqueda de lugares.

#### Scenario: Llave faltante

- **GIVEN** una compilación sin llave configurada, con `DEFAULT_API_KEY`
- **WHEN** se muestra el mapa
- **THEN** se ve «Falta configurar la llave de Google Maps» y no se busca ningún lugar

#### Scenario: Llave configurada

- **GIVEN** una llave válida en `secrets.properties`
- **WHEN** se muestra el mapa
- **THEN** la aplicación no muestra el texto de llave faltante y puede buscar lugares

### Requirement: Estados visibles en el mapa

El mapa MUST mostrar un estado claro en cada una de estas situaciones: carga de la ubicación, carga de lugares, sin permiso, permiso denegado de forma permanente, ubicación del dispositivo desactivada, ubicación no obtenida, error de búsqueda, llave de Google Maps no configurada y sin resultados. Cuando hay resultados, MUST mostrar el contador «<n> lugares», en singular «1 lugar» cuando hay exactamente un resultado.

#### Scenario: Cargando lugares

- **GIVEN** una búsqueda en curso
- **WHEN** el mapa se muestra
- **THEN** se ve un indicador de carga de lugares

#### Scenario: Sin resultados

- **GIVEN** una búsqueda terminada sin lugares con nombre dentro del radio, en la categoría Parques y con radio de 1 km
- **WHEN** se muestra el resultado
- **THEN** se muestra «No se encontraron lugares de «Parques» a menos de 1 km» y no hay marcadores de lugares

#### Scenario: Contador de resultados

- **GIVEN** una búsqueda que devuelve 12 lugares
- **WHEN** se muestra el resultado
- **THEN** se muestra el contador «12 lugares»

#### Scenario: Contador de un solo resultado

- **GIVEN** una búsqueda que devuelve 1 lugar
- **WHEN** se muestra el resultado
- **THEN** se muestra el contador «1 lugar» en singular, y no «1 lugares»

#### Scenario: Error de búsqueda

- **GIVEN** una búsqueda que falla por falta de conexión
- **WHEN** se muestra el resultado
- **THEN** se muestra «Sin conexión a internet» con el botón «Reintentar» y no hay marcadores de lugares
