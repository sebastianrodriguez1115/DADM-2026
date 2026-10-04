# Spec Delta

## Purpose

Define el tablero gráfico accesible del tres en raya, con cuadrícula y marcas dibujadas que se adaptan al espacio disponible.

## ADDED Requirements

### Requirement: Tablero y marcas dibujados
El tablero SHALL representar la cuadrícula y las marcas mediante dibujo gráfico, no mediante botones ni texto como representación de las casillas o marcas. X SHALL ser verde y O roja; las marcas SHALL conservar proporción sin distorsión y adaptarse al tamaño y colores del tema actual.

#### Scenario: Cuadrícula y marcas gráficas
- **WHEN** se muestra un tablero con X y O
- **THEN** la cuadrícula y las marcas se ven dibujadas, X es verde y O roja, sin números ni caracteres de texto que las representen

#### Scenario: Escala sin distorsión
- **GIVEN** que cambia el espacio disponible o el tema
- **WHEN** se muestra el tablero
- **THEN** la cuadrícula y las marcas se adaptan al espacio y a los colores actuales sin deformar X ni O

### Requirement: Casillas accionables y accesibles
Las nueve casillas SHALL ser objetivos diferenciados accionables por toque, clic y teclado, y SHALL exponer a TalkBack su ubicación, contenido y disponibilidad. En los tamaños de pantalla soportados, cada casilla SHALL conservar al menos 48 dp por lado; una casilla deshabilitada MUST NOT ser accionable.

#### Scenario: Toque en cada ubicación del tablero
- **GIVEN** que cada una de las nueve casillas está vacía durante el turno humano
- **WHEN** la persona toca una vez dentro de cada casilla
- **THEN** se solicita exactamente una jugada por toque en la ubicación tocada, y un toque fuera del tablero o una activación duplicada no solicita jugada

#### Scenario: Clic y teclado accionan casilla disponible
- **GIVEN** una casilla vacía durante el turno humano
- **WHEN** se activa mediante clic o teclado
- **THEN** se solicita una jugada en esa casilla

#### Scenario: Casilla deshabilitada no accionable
- **GIVEN** que una casilla está deshabilitada por su marca, turno o desenlace
- **WHEN** se intenta activarla por toque, clic o teclado
- **THEN** no se solicita una jugada

#### Scenario: Accesibilidad de las nueve zonas
- **GIVEN** que se recorre el tablero con TalkBack
- **WHEN** se visitan sus casillas
- **THEN** se identifican las nueve ubicaciones, su marca o vaciedad y si están disponibles
