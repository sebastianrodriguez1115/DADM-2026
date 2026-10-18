# company-storage Specification

## Purpose

Define qué datos guarda el directorio sobre cada empresa, cómo se conservan en la base de datos del dispositivo, las reglas de unicidad, la comparación de texto sin distinguir mayúsculas ni tildes y la carga de datos de ejemplo.

## Requirements

### Requirement: Datos de una empresa

Una empresa SHALL tener nombre, correo de contacto, URL de la página web, teléfono de contacto, productos y servicios, y una clasificación. El nombre y el correo MUST estar presentes; la URL, el teléfono y los productos y servicios SHALL ser opcionales y pueden quedar vacíos. Los textos SHALL guardarse sin espacios al inicio ni al final. La clasificación SHALL ser exactamente una de «Consultoría», «Desarrollo a la medida» y «Fábrica de software», y una empresa MUST tener una y solo una.

#### Scenario: Empresa con todos los campos

- **GIVEN** una base vacía
- **WHEN** se guarda una empresa con nombre, correo, URL, teléfono, productos y servicios y la clasificación «Fábrica de software»
- **THEN** al consultarla se obtienen los mismos valores y exactamente esa clasificación

#### Scenario: Empresa solo con campos obligatorios

- **GIVEN** una base vacía
- **WHEN** se guarda una empresa con nombre, correo y clasificación, y sin URL, teléfono ni productos y servicios
- **THEN** la empresa se guarda y esos tres campos quedan vacíos

#### Scenario: Empresa sin clasificación

- **GIVEN** una base vacía
- **WHEN** se intenta guardar una empresa sin clasificación
- **THEN** la empresa no se guarda

#### Scenario: Espacios alrededor del texto

- **GIVEN** una base vacía
- **WHEN** se guarda una empresa con nombre "  Acme Software  " y correo " contacto@acme.example.com "
- **THEN** se almacenan "Acme Software" y "contacto@acme.example.com"

### Requirement: Catálogo de clasificaciones

La base de datos SHALL tener un catálogo de clasificaciones con exactamente tres filas fijas, identificadas por un código estable y único: 1 = `CONSULTING` («Consultoría»), 2 = `CUSTOM_DEVELOPMENT` («Desarrollo a la medida») y 3 = `SOFTWARE_FACTORY` («Fábrica de software»). Cada empresa SHALL referenciar su clasificación con una clave foránea obligatoria hacia el catálogo, de modo que la base rechace una empresa sin clasificación o con una clasificación inexistente. El catálogo SHALL cargarse al crear cualquier base, incluidas las bases en memoria de las pruebas, y MUST NOT depender de que se carguen los datos de ejemplo. La tabla solo guarda el código; los textos en español salen de los recursos de la aplicación.

#### Scenario: Catálogo en una base nueva

- **GIVEN** una base recién creada, sin datos de ejemplo
- **WHEN** se consulta el catálogo
- **THEN** contiene exactamente tres filas con los códigos `CONSULTING`, `CUSTOM_DEVELOPMENT` y `SOFTWARE_FACTORY`

#### Scenario: La base exige una clasificación existente

- **GIVEN** una base con el catálogo cargado
- **WHEN** se inserta directamente una empresa con una clasificación que no está en el catálogo
- **THEN** la base rechaza la inserción y no se guarda ninguna empresa

### Requirement: Persistencia entre aperturas

Las empresas guardadas SHALL conservarse en una base SQLite local del dispositivo y MUST seguir disponibles después de cerrar la aplicación y volver a abrirla. Crear, modificar y eliminar una empresa SHALL reflejarse en una consulta posterior sin recargar manualmente.

#### Scenario: Los datos sobreviven al reinicio de la aplicación

- **GIVEN** que se guardó una empresa
- **WHEN** se cierra el proceso de la aplicación y se abre de nuevo
- **THEN** la empresa sigue en el listado con los mismos datos

#### Scenario: Modificar conserva la identidad

- **GIVEN** una empresa guardada
- **WHEN** se modifican su teléfono y su clasificación
- **THEN** al consultarla aparecen los valores nuevos, no se crea una segunda empresa y su clasificación anterior deja de aparecer

#### Scenario: Eliminar quita la empresa y deja intacto el catálogo

- **GIVEN** una empresa guardada con la clasificación «Consultoría»
- **WHEN** se elimina
- **THEN** ya no se obtiene al consultarla ni aparece en ningún filtro, y el catálogo sigue teniendo sus tres clasificaciones

### Requirement: Comparación sin distinguir mayúsculas ni tildes

Toda comparación de texto de nombres y correos, tanto para la unicidad como para la búsqueda, SHALL ignorar las mayúsculas y las minúsculas, también en letras como «Á/á» y «Ñ/ñ», SHALL ignorar los espacios al inicio y al final, y SHALL ignorar las tildes y la diéresis, de modo que «Tecnología» y «tecnologia» se consideran iguales. La «ñ» se trata como «n».

#### Scenario: Letras acentuadas y eñe

- **GIVEN** una empresa llamada «Ñandú Ágil»
- **WHEN** se compara con «ñandú ágil» y con «NANDU AGIL»
- **THEN** ambas comparaciones se consideran iguales

### Requirement: Unicidad de nombre y de correo

El nombre y el correo de una empresa MUST ser únicos en el directorio según la regla de comparación anterior. La unicidad SHALL imponerla la propia base de datos, de modo que ninguna ruta de código pueda guardar un duplicado, y la aplicación SHALL comprobarla además antes de guardar para informar cuál campo choca. Al modificar una empresa, la comprobación SHALL excluir a la propia empresa.

#### Scenario: Nombre duplicado con distinta capitalización

- **GIVEN** una empresa llamada «Acme Software»
- **WHEN** se intenta guardar otra con nombre «ACME software»
- **THEN** no se guarda y se informa que el nombre ya existe

#### Scenario: Nombre duplicado con espacios sobrantes

- **GIVEN** una empresa llamada «Acme Software»
- **WHEN** se intenta guardar otra con nombre «  Acme Software   »
- **THEN** no se guarda y se informa que el nombre ya existe

#### Scenario: Correo duplicado con distinta capitalización

- **GIVEN** una empresa con correo «contacto@acme.example.com»
- **WHEN** se intenta guardar otra con correo « Contacto@ACME.example.com »
- **THEN** no se guarda y se informa que el correo ya existe

#### Scenario: Editar sin cambiar el nombre ni el correo

- **GIVEN** una empresa guardada
- **WHEN** se modifica solo su teléfono y se guarda con el mismo nombre y correo
- **THEN** el guardado tiene éxito y no se informa ningún duplicado

#### Scenario: Cambiar el nombre solo en la capitalización

- **GIVEN** una empresa llamada «Acme software»
- **WHEN** se modifica su nombre a «Acme Software»
- **THEN** el guardado tiene éxito

#### Scenario: Editar hacia el nombre de otra empresa

- **GIVEN** dos empresas «Acme Software» y «Beta Labs»
- **WHEN** se modifica «Beta Labs» para que se llame «acme software»
- **THEN** no se guarda y se informa que el nombre ya existe

#### Scenario: La base rechaza un duplicado aunque la comprobación previa se omita

- **GIVEN** una empresa llamada «Acme Software»
- **WHEN** se inserta directamente en la base otra con el mismo nombre normalizado
- **THEN** la base rechaza la inserción y no queda ningún duplicado

### Requirement: Datos de ejemplo solo en la primera creación

Cuando la base de datos se cree por primera vez, la aplicación SHALL cargar 8 empresas ficticias de ejemplo, que cumplen las mismas validaciones que cualquier empresa, usan correos y direcciones web de dominios reservados para ejemplos (subdominios de `example.com`) y cubren las tres clasificaciones, con una clasificación cada una. La aplicación MUST NOT volver a cargarlas en aperturas posteriores, ni siquiera si la persona elimina todas las empresas.

#### Scenario: Primera apertura

- **GIVEN** una instalación nueva sin base de datos
- **WHEN** se abre la aplicación
- **THEN** el listado muestra 8 empresas de ejemplo

#### Scenario: No reaparecen tras borrar todo

- **GIVEN** que la aplicación cargó los datos de ejemplo y la persona eliminó las 8 empresas
- **WHEN** se cierra el proceso y se abre la aplicación de nuevo
- **THEN** el listado muestra «Aún no hay empresas registradas» y no se vuelven a cargar empresas

#### Scenario: Una base existente no recibe ejemplos

- **GIVEN** una base ya creada con una sola empresa propia
- **WHEN** se abre la aplicación de nuevo
- **THEN** la base contiene únicamente esa empresa

#### Scenario: Los ejemplos cumplen las reglas

- **GIVEN** los datos de ejemplo
- **WHEN** cada empresa de ejemplo se valida con las reglas del formulario
- **THEN** ninguna produce error y no hay nombres ni correos repetidos
