# Spec Delta

## Purpose

Define la pantalla de Ajustes donde el usuario elige el radio de búsqueda en kilómetros, el rango y el valor por defecto de ese radio, cómo se guarda entre aperturas de la aplicación y qué efecto tiene en el mapa al volver.

## ADDED Requirements

### Requirement: Pantalla de Ajustes del radio

La pantalla de Ajustes SHALL tener el título «Ajustes», un control deslizante del radio de búsqueda y el valor elegido con formato español, como «1,5 km». La pantalla SHALL tener una acción de volver con la descripción «Volver».

#### Scenario: Abrir los ajustes

- **GIVEN** el mapa en pantalla
- **WHEN** el usuario abre los Ajustes
- **THEN** se ve el título «Ajustes», el control del radio y el valor actual con formato, como «1 km»

#### Scenario: Volver desde los ajustes

- **GIVEN** la pantalla de Ajustes abierta
- **WHEN** el usuario pulsa «Volver» o el botón Back del sistema
- **THEN** se muestra el mapa

### Requirement: Rango y paso del radio

El radio de búsqueda MUST poder elegirse entre 0,5 km y 10 km, en pasos de 0,5 km. El valor por defecto MUST ser 1 km.

#### Scenario: Valor por defecto

- **GIVEN** que el usuario nunca ha cambiado el radio
- **WHEN** abre los Ajustes
- **THEN** el valor mostrado es 1 km

#### Scenario: Límites del control

- **GIVEN** la pantalla de Ajustes abierta
- **WHEN** el usuario mueve el control deslizante hasta sus extremos
- **THEN** el valor mínimo es 0,5 km y el máximo es 10 km, con pasos de 0,5 km

### Requirement: Persistencia del radio

La aplicación MUST guardar el radio elegido cuando el usuario suelta el control deslizante, y MUST conservarlo al cerrar y volver a abrir la aplicación. Si no hay un valor guardado, MUST usarse 1 km. Si el valor guardado está fuera del rango, MUST ajustarse al límite más cercano.

#### Scenario: Radio conservado entre aperturas

- **GIVEN** el usuario eligió 3 km y soltó el control
- **WHEN** cierra la aplicación y la vuelve a abrir
- **THEN** el radio es 3 km

#### Scenario: Valor fuera de rango

- **GIVEN** un valor guardado de 25 km
- **WHEN** la aplicación lee el radio
- **THEN** el radio usado es 10 km

#### Scenario: Sin valor guardado

- **GIVEN** que nunca se ha guardado un radio
- **WHEN** la aplicación lee el radio
- **THEN** el radio usado es 1 km

### Requirement: Efecto del cambio de radio en el mapa

Al volver al mapa con un radio distinto del que tenía, la aplicación MUST repetir la búsqueda si ya hay una ubicación. Si el radio no cambió, la aplicación MUST NOT repetir la búsqueda.

#### Scenario: Radio cambiado con ubicación

- **GIVEN** una ubicación obtenida, un radio de 1 km y resultados en el mapa
- **WHEN** el usuario cambia el radio a 2 km y vuelve al mapa
- **THEN** la búsqueda se repite con 2 km y el mapa muestra los resultados nuevos

#### Scenario: Radio sin cambios

- **GIVEN** una ubicación obtenida y un radio de 1 km
- **WHEN** el usuario abre los Ajustes, no cambia nada y vuelve al mapa
- **THEN** la búsqueda no se repite

#### Scenario: Radio cambiado sin ubicación

- **GIVEN** que la ubicación todavía no se ha obtenido
- **WHEN** el usuario cambia el radio y vuelve al mapa
- **THEN** no se busca lugares y el mapa sigue mostrando el estado de ubicación

#### Scenario: Radio guardado sin volver

- **GIVEN** que el usuario está en la pantalla de Ajustes y acaba de cambiar el radio
- **WHEN** el valor se guarda al soltar el control
- **THEN** el mapa no cambia hasta que el usuario vuelve a él
