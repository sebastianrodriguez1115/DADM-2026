# Spec Delta

## Purpose

Define cómo la aplicación obtiene la posición actual del dispositivo, pide el permiso de ubicación, muestra sus coordenadas y su dirección, y qué ve el usuario cuando la ubicación no está disponible.

## ADDED Requirements

### Requirement: Permiso de ubicación

La aplicación SHALL pedir el permiso de ubicación en tiempo de ejecución la primera vez que la necesite. Si el usuario concede la ubicación aproximada pero no la precisa, la aplicación MUST seguir funcionando y MUST indicar que la ubicación es aproximada.

#### Scenario: Permiso concedido

- **GIVEN** la aplicación se abre por primera vez y el permiso no se ha concedido
- **WHEN** el usuario concede el permiso de ubicación precisa
- **THEN** la aplicación obtiene la ubicación y muestra el mapa centrado en ella

#### Scenario: Solo ubicación aproximada

- **GIVEN** el usuario concede solo la ubicación aproximada
- **WHEN** la aplicación obtiene la ubicación
- **THEN** la aplicación muestra la ubicación y avisa que es aproximada

#### Scenario: Permiso denegado

- **GIVEN** el usuario deniega el permiso de ubicación sin marcar que no vuelva a preguntar
- **WHEN** la aplicación muestra el mapa
- **THEN** se muestra el mensaje de que la aplicación necesita la ubicación y el botón «Conceder permiso», que vuelve a pedir el permiso

#### Scenario: Denegación permanente

- **GIVEN** el permiso de ubicación se ha denegado de forma permanente y el sistema ya no muestra su diálogo
- **WHEN** la aplicación muestra el mapa
- **THEN** el botón cambia a «Abrir ajustes» y, al pulsarlo, se abren los ajustes de la aplicación en el sistema

### Requirement: Ubicación del dispositivo desactivada

La aplicación MUST comprobar si la ubicación del dispositivo está activada antes de pedir la posición. Si está desactivada, la aplicación MUST mostrar un mensaje que lo indique y MUST NOT mostrar lugares ni coordenadas inventadas.

#### Scenario: Ubicación desactivada

- **GIVEN** el permiso está concedido pero la ubicación del dispositivo está desactivada
- **WHEN** la aplicación intenta obtener la ubicación
- **THEN** se muestra el mensaje de que la ubicación del dispositivo está desactivada y no se muestran lugares

### Requirement: Lectura puntual de la ubicación

La aplicación SHALL leer la ubicación actual una vez al abrirse, cuando el permiso está concedido, y cada vez que el usuario pulse «Actualizar ubicación». La aplicación MUST NOT seguir la ubicación de forma continua.

#### Scenario: Lectura al abrir

- **GIVEN** el permiso está concedido y la ubicación del dispositivo está activada
- **WHEN** se abre la aplicación
- **THEN** la aplicación lee la ubicación una vez y muestra un indicador de carga mientras la obtiene

#### Scenario: Actualizar ubicación

- **GIVEN** la aplicación ya muestra una ubicación
- **WHEN** el usuario pulsa «Actualizar ubicación»
- **THEN** la aplicación vuelve a leer la ubicación y actualiza las coordenadas, la dirección y el mapa

#### Scenario: Ubicación no obtenida

- **GIVEN** el permiso está concedido y la ubicación del dispositivo está activada
- **WHEN** el sistema no devuelve ninguna ubicación
- **THEN** se muestra el error «No se pudo obtener la ubicación» con la opción de actualizar, y no se muestran lugares

### Requirement: Coordenadas y dirección de la posición

Cuando hay una ubicación, la aplicación SHALL mostrar en una tarjeta «Tu ubicación», encima del mapa, la latitud y la longitud con 5 decimales y la dirección de esa posición. La dirección SHALL obtenerse solo si el dispositivo tiene un servicio de direcciones disponible.

#### Scenario: Coordenadas y dirección disponibles

- **GIVEN** una ubicación obtenida en la que el servicio de direcciones responde
- **WHEN** se muestra la tarjeta «Tu ubicación»
- **THEN** se ven las coordenadas con 5 decimales y la dirección de la posición

#### Scenario: Dirección no disponible

- **GIVEN** una ubicación obtenida en la que el servicio de direcciones no tiene backend, falla o no devuelve resultados
- **WHEN** se muestra la tarjeta «Tu ubicación»
- **THEN** la dirección muestra «Dirección no disponible» y las coordenadas siguen visibles

#### Scenario: Sin servicio de direcciones en el dispositivo

- **GIVEN** el dispositivo no tiene servicio de direcciones
- **WHEN** se obtiene una ubicación
- **THEN** no se intenta resolver la dirección y se muestra «Dirección no disponible» con las coordenadas visibles
