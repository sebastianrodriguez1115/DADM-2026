# company-list Specification

## Purpose

Define el listado de empresas, su orden, los mensajes cuando no hay nada que mostrar, la búsqueda por nombre, el filtro por clasificaciones y el acceso al detalle y al alta.

## ADDED Requirements

### Requirement: Listado ordenado de empresas

La pantalla inicial SHALL mostrar el título «Directorio de empresas» y la lista de todas las empresas, ordenada alfabéticamente por nombre sin distinguir mayúsculas ni tildes. Cada elemento SHALL mostrar el nombre de la empresa y sus clasificaciones. La lista SHALL actualizarse sola cuando se crea, modifica o elimina una empresa.

#### Scenario: Orden alfabético

- **GIVEN** empresas llamadas «beta Labs», «Álamo Tech» y «Cobalto»
- **WHEN** se abre el listado
- **THEN** se muestran en el orden «Álamo Tech», «beta Labs», «Cobalto»

#### Scenario: Cada elemento muestra sus clasificaciones

- **GIVEN** una empresa con las clasificaciones «Consultoría» y «Fábrica de software»
- **WHEN** aparece en el listado
- **THEN** su elemento muestra el nombre y los textos «Consultoría» y «Fábrica de software»

#### Scenario: El listado refleja una nueva empresa

- **GIVEN** el listado abierto
- **WHEN** se crea una empresa desde el formulario
- **THEN** al volver al listado la empresa aparece en su posición alfabética

### Requirement: Mensajes de lista vacía

Cuando no haya empresas registradas, el listado SHALL mostrar «Aún no hay empresas registradas» y la pista «Toca + para agregar la primera.». Cuando haya empresas pero ninguna cumpla la búsqueda y los filtros, SHALL mostrar «Ninguna empresa coincide con la búsqueda». Los dos mensajes MUST ser distintos y nunca aparecer a la vez.

#### Scenario: Directorio vacío

- **GIVEN** que no hay ninguna empresa
- **WHEN** se abre el listado
- **THEN** se muestran «Aún no hay empresas registradas» y «Toca + para agregar la primera.» y no se muestra «Ninguna empresa coincide con la búsqueda»

#### Scenario: Sin coincidencias

- **GIVEN** que hay empresas registradas
- **WHEN** la búsqueda es «zzzz»
- **THEN** se muestra «Ninguna empresa coincide con la búsqueda» y no se muestra «Aún no hay empresas registradas»

### Requirement: Búsqueda por nombre

El listado SHALL ofrecer un campo de búsqueda con la etiqueta «Buscar por nombre». Una empresa SHALL coincidir cuando el texto escrito, sin espacios al inicio ni al final, aparezca en cualquier parte de su nombre, sin distinguir mayúsculas ni tildes. Un campo vacío o solo con espacios no filtra. Los caracteres `%`, `_` y `\` SHALL tratarse como texto normal. Cuando el campo tiene texto SHALL mostrarse una acción con la descripción «Borrar búsqueda» que lo vacía.

#### Scenario: Texto en medio del nombre

- **GIVEN** empresas «Acme Software» y «Beta Labs»
- **WHEN** se escribe «me sof»
- **THEN** solo se muestra «Acme Software»

#### Scenario: Sin distinguir mayúsculas ni tildes

- **GIVEN** una empresa «Tecnología Ñandú»
- **WHEN** se escribe «TECNOLOGIA» y luego «ñandu»
- **THEN** en ambos casos se muestra «Tecnología Ñandú»

#### Scenario: Caracteres especiales como texto

- **GIVEN** empresas «Acme Software» y «Cien%Digital»
- **WHEN** se escribe «%»
- **THEN** solo se muestra «Cien%Digital»

#### Scenario: Borrar la búsqueda

- **GIVEN** el campo de búsqueda con el texto «acme»
- **WHEN** se pulsa «Borrar búsqueda»
- **THEN** el campo queda vacío y se muestran todas las empresas que cumplan los chips marcados

### Requirement: Filtro por clasificación

Debajo de la búsqueda SHALL haber un chip por clasificación, «Consultoría», «Desarrollo a la medida» y «Fábrica de software», de selección múltiple e independiente. Con uno o más chips marcados, el listado SHALL mostrar las empresas que tengan al menos una de las clasificaciones marcadas. Sin ningún chip marcado, no se filtra por clasificación. Una empresa con varias clasificaciones SHALL aparecer una sola vez.

#### Scenario: Un chip

- **GIVEN** empresas con clasificaciones: A {Consultoría}, B {Desarrollo a la medida}, C {Consultoría, Fábrica de software}
- **WHEN** se marca «Consultoría»
- **THEN** se muestran A y C

#### Scenario: Varios chips son una unión

- **GIVEN** las mismas empresas A, B y C
- **WHEN** se marcan «Consultoría» y «Desarrollo a la medida»
- **THEN** se muestran A, B y C, y C aparece una sola vez

#### Scenario: Desmarcar todos los chips

- **GIVEN** el chip «Consultoría» marcado
- **WHEN** se desmarca
- **THEN** se muestran todas las empresas que cumplan la búsqueda

### Requirement: Búsqueda y clasificación se combinan

Cuando hay texto de búsqueda y chips marcados a la vez, el listado SHALL mostrar solo las empresas que cumplan ambos criterios.

#### Scenario: Búsqueda más chip

- **GIVEN** A «Acme Software» {Consultoría}, B «Acme Labs» {Desarrollo a la medida}, C «Beta» {Consultoría}
- **WHEN** se escribe «acme» y se marca «Consultoría»
- **THEN** solo se muestra A

#### Scenario: Combinación sin resultados

- **GIVEN** las mismas empresas A, B y C
- **WHEN** se escribe «beta» y se marca «Desarrollo a la medida»
- **THEN** se muestra «Ninguna empresa coincide con la búsqueda»

### Requirement: Los filtros se conservan al ir al detalle y volver

Al abrir el detalle de una empresa y volver al listado, el texto de búsqueda y los chips marcados SHALL conservarse tal como estaban.

#### Scenario: Volver desde el detalle

- **GIVEN** el listado con la búsqueda «acme» y el chip «Consultoría» marcado
- **WHEN** se toca una empresa y luego se vuelve con Back
- **THEN** el campo conserva «acme», el chip sigue marcado y la lista es la misma

### Requirement: Acceso al detalle y al alta

Tocar una empresa del listado SHALL abrir su detalle. El listado SHALL tener un botón «+» con la descripción de accesibilidad «Agregar empresa» que abre el formulario de alta. Todo botón que solo tenga ícono MUST llevar descripción de accesibilidad. Los objetivos táctiles SHALL medir al menos 48 dp.

#### Scenario: Abrir el detalle

- **GIVEN** el listado con la empresa «Acme Software»
- **WHEN** se toca su elemento
- **THEN** se muestra el detalle de «Acme Software»

#### Scenario: Abrir el alta

- **GIVEN** el listado
- **WHEN** se pulsa el botón con la descripción «Agregar empresa»
- **THEN** se muestra el formulario con el título «Nueva empresa»

#### Scenario: Back en el listado

- **GIVEN** el listado como pantalla actual
- **WHEN** se pulsa Back
- **THEN** se aplica la navegación normal del sistema, sin interceptarla
