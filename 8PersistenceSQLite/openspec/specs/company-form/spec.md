# company-form Specification

## Purpose

Define el formulario único para crear y editar empresas: sus campos, las reglas de validación, los mensajes de error, el comportamiento con teclado y Back, y los destinos tras guardar o cancelar.

## Requirements

### Requirement: Un formulario para crear y editar

La aplicación SHALL usar el mismo formulario para crear y para editar. Al crear SHALL titularse «Nueva empresa» y mostrar los campos vacíos; al editar SHALL titularse «Editar empresa» y mostrar los valores actuales. Los campos SHALL ser: «Nombre de la empresa», «Correo electrónico», «Sitio web (opcional)», «Teléfono (opcional)», «Productos y servicios (opcional)» (de varias líneas) y un menú desplegable de selección única con la etiqueta «Clasificación» y las opciones «Consultoría», «Desarrollo a la medida» y «Fábrica de software». El formulario SHALL tener los botones «Guardar» y «Cancelar».

#### Scenario: Formulario de alta

- **GIVEN** el listado
- **WHEN** se pulsa «Agregar empresa»
- **THEN** se muestra «Nueva empresa» con los campos de texto vacíos y el menú «Clasificación» sin ninguna opción seleccionada

#### Scenario: Formulario de edición

- **GIVEN** una empresa con la clasificación «Fábrica de software»
- **WHEN** se abre su edición
- **THEN** los campos muestran sus valores y el menú «Clasificación» muestra «Fábrica de software»

#### Scenario: Elegir una clasificación

- **GIVEN** el formulario con el menú «Clasificación» sin selección
- **WHEN** se abre el menú y se elige «Consultoría»
- **THEN** el menú se cierra, muestra «Consultoría» y ofrece cambiar a otra de las tres opciones, sin permitir escribir un texto libre

### Requirement: Validación del nombre

Al pulsar «Guardar», el nombre, recortado, SHALL tener entre 1 y 100 caracteres. Si está vacío SHALL mostrarse bajo el campo «Escribe el nombre de la empresa»; si supera 100 caracteres, «El nombre admite hasta 100 caracteres»; si ya existe en otra empresa según la comparación sin mayúsculas ni tildes, «Ya existe una empresa con ese nombre».

#### Scenario: Nombre vacío o solo espacios

- **GIVEN** el formulario de alta
- **WHEN** el nombre está vacío o tiene solo espacios y se pulsa «Guardar»
- **THEN** bajo el campo aparece «Escribe el nombre de la empresa» y no se guarda nada

#### Scenario: Nombre duplicado al crear

- **GIVEN** una empresa llamada «Acme Software»
- **WHEN** se intenta crear otra con nombre «  acme software  »
- **THEN** bajo el campo aparece «Ya existe una empresa con ese nombre» y no se guarda

#### Scenario: Editar sin cambiar el nombre

- **GIVEN** la edición de «Acme Software»
- **WHEN** se cambia el teléfono y se pulsa «Guardar»
- **THEN** se guarda sin mostrar «Ya existe una empresa con ese nombre»

#### Scenario: Nombre demasiado largo

- **GIVEN** el formulario de alta
- **WHEN** el nombre tiene 101 caracteres y se pulsa «Guardar»
- **THEN** aparece «El nombre admite hasta 100 caracteres» y no se guarda

### Requirement: Validación del correo

El correo, recortado, SHALL ser obligatorio y tener formato válido: parte local con letras, dígitos y `.` `_` `%` `+` `-`, un `@` y un dominio con al menos un punto cuya última parte tenga al menos 2 letras, con un máximo de 254 caracteres. Si está vacío SHALL mostrarse «Escribe el correo electrónico»; si el formato no es válido, «Escribe un correo electrónico válido»; si ya existe en otra empresa según la comparación sin mayúsculas, «Ya existe una empresa con ese correo electrónico».

#### Scenario: Correos válidos

- **GIVEN** el formulario de alta
- **WHEN** se escribe «ventas@acme.example.com» o «a.b+c@sub.example.co»
- **THEN** no aparece error en el campo

#### Scenario: Correos inválidos

- **GIVEN** el formulario de alta
- **WHEN** se escribe «acme», «acme@», «@acme.com», «a b@acme.com», «a@acme» o «a@acme.c» y se pulsa «Guardar»
- **THEN** aparece «Escribe un correo electrónico válido» y no se guarda

#### Scenario: Correo vacío

- **GIVEN** el formulario de alta
- **WHEN** el correo está vacío y se pulsa «Guardar»
- **THEN** aparece «Escribe el correo electrónico»

#### Scenario: Correo duplicado

- **GIVEN** una empresa con correo «ventas@acme.example.com»
- **WHEN** se intenta crear otra con «VENTAS@Acme.example.com»
- **THEN** aparece «Ya existe una empresa con ese correo electrónico» y no se guarda

### Requirement: Validación de la URL

La URL SHALL ser opcional. Si se llena, recortada, SHALL ser una dirección web válida con o sin el prefijo `http://` o `https://`: un dominio con al menos un punto cuya última parte tenga al menos 2 letras, un puerto opcional y una ruta opcional sin espacios, con un máximo de 200 caracteres. Cualquier otro esquema (por ejemplo `ftp://`) o un texto con espacios MUST rechazarse con «Escribe una dirección web válida, por ejemplo https://ejemplo.com». El valor se guarda como se escribió, sin agregarle prefijo.

#### Scenario: URL válida con y sin esquema

- **GIVEN** el formulario de alta
- **WHEN** se escribe «https://www.acme.example.com/productos» o «acme.example.com»
- **THEN** no aparece error en el campo

#### Scenario: URL inválida

- **GIVEN** el formulario de alta
- **WHEN** se escribe «ftp://acme.com», «acme», «acme .com» o «http://» y se pulsa «Guardar»
- **THEN** aparece «Escribe una dirección web válida, por ejemplo https://ejemplo.com» y no se guarda

#### Scenario: URL vacía

- **GIVEN** el formulario de alta con los demás campos válidos
- **WHEN** la URL está vacía o con espacios y se pulsa «Guardar»
- **THEN** se guarda con la URL vacía

### Requirement: Validación del teléfono

El teléfono SHALL ser opcional. Si se llena, recortado, SHALL contener solo dígitos, espacios, guiones, puntos y paréntesis, con un `+` opcional únicamente al inicio, y tener entre 7 y 15 dígitos. En otro caso SHALL mostrarse «Escribe un teléfono válido, de 7 a 15 dígitos».

#### Scenario: Teléfonos válidos

- **GIVEN** el formulario de alta
- **WHEN** se escribe «601 555 0123», «+57 (601) 555-0123» o «3105550123»
- **THEN** no aparece error en el campo

#### Scenario: Teléfonos inválidos

- **GIVEN** el formulario de alta
- **WHEN** se escribe «12345», «555-CALL-NOW», «1234567890123456» o «57+6015550123» y se pulsa «Guardar»
- **THEN** aparece «Escribe un teléfono válido, de 7 a 15 dígitos» y no se guarda

### Requirement: Validación de productos y servicios

Los productos y servicios SHALL ser opcionales, de texto libre con saltos de línea y de hasta 1000 caracteres después del recorte. Si se excede SHALL mostrarse «Los productos y servicios admiten hasta 1000 caracteres».

#### Scenario: Texto con varias líneas

- **GIVEN** el formulario de alta con los demás campos válidos
- **WHEN** los productos y servicios tienen tres líneas y se pulsa «Guardar»
- **THEN** se guarda y el detalle muestra las tres líneas

### Requirement: Clasificación obligatoria

El formulario SHALL exigir exactamente una clasificación elegida en el menú «Clasificación». Si no hay ninguna al pulsar «Guardar», SHALL mostrarse «Selecciona una clasificación» bajo el menú y no se guarda. Cada una de las tres opciones es válida.

#### Scenario: Ninguna clasificación

- **GIVEN** el formulario con nombre y correo válidos y el menú «Clasificación» sin selección
- **WHEN** se pulsa «Guardar»
- **THEN** aparece «Selecciona una clasificación» bajo el menú y no se guarda

#### Scenario: Cada clasificación es válida

- **GIVEN** el formulario con nombre y correo válidos
- **WHEN** se elige «Desarrollo a la medida» y se pulsa «Guardar»
- **THEN** la empresa se guarda con esa única clasificación

#### Scenario: El error se limpia al elegir

- **GIVEN** el error «Selecciona una clasificación» visible
- **WHEN** se elige una opción del menú
- **THEN** el error desaparece

### Requirement: Errores por campo

Todos los errores de validación SHALL mostrarse en el campo correspondiente y a la vez, no solo el primero. Un error SHALL desaparecer cuando la persona edita ese campo. Si no hay errores, «Guardar» SHALL guardar los valores recortados. Mientras se guarda, «Guardar» SHALL deshabilitarse para evitar duplicados por doble toque. Si el guardado falla por un motivo distinto de la validación, SHALL mostrarse «No se pudo guardar la empresa. Inténtalo de nuevo.» y el formulario conserva lo escrito.

#### Scenario: Varios errores a la vez

- **GIVEN** el formulario de alta vacío
- **WHEN** se pulsa «Guardar»
- **THEN** aparecen a la vez «Escribe el nombre de la empresa», «Escribe el correo electrónico» y «Selecciona una clasificación»

#### Scenario: El error se limpia al editar

- **GIVEN** el error «Escribe el nombre de la empresa» visible
- **WHEN** se escribe una letra en el nombre
- **THEN** el error de ese campo desaparece

#### Scenario: Doble toque en Guardar

- **GIVEN** el formulario con datos válidos
- **WHEN** se pulsa «Guardar» dos veces seguidas
- **THEN** se crea una sola empresa

#### Scenario: Falla el guardado

- **GIVEN** el formulario con datos válidos y un almacenamiento que falla al guardar
- **WHEN** se pulsa «Guardar»
- **THEN** aparece «No se pudo guardar la empresa. Inténtalo de nuevo.», el formulario conserva lo escrito y se puede volver a intentar

### Requirement: Destinos tras guardar o cancelar

Tras crear una empresa, la aplicación SHALL volver al listado. Tras editar, SHALL volver al detalle de esa empresa con los datos actualizados. «Cancelar» y Back SHALL descartar los cambios sin preguntar y volver a la pantalla de origen (listado al crear, detalle al editar).

#### Scenario: Guardar un alta

- **GIVEN** el formulario de alta con datos válidos
- **WHEN** se pulsa «Guardar»
- **THEN** se muestra el listado con la empresa nueva

#### Scenario: Guardar una edición

- **GIVEN** la edición de «Acme Software» con el teléfono cambiado
- **WHEN** se pulsa «Guardar»
- **THEN** se muestra el detalle de «Acme Software» con el teléfono nuevo

#### Scenario: Cancelar descarta

- **GIVEN** la edición con el nombre modificado
- **WHEN** se pulsa «Cancelar» o Back
- **THEN** se muestra el detalle con el nombre original y no se pregunta nada

#### Scenario: Back en el alta

- **GIVEN** el formulario de alta con datos escritos
- **WHEN** se pulsa Back
- **THEN** se muestra el listado y no se crea ninguna empresa

### Requirement: Uso con teclado y orientación

El formulario SHALL poder desplazarse y SHALL poder completarse con el teclado en pantalla abierto, sin que este oculte el campo que se edita ni los botones. La orientación MUST NOT estar fija.

#### Scenario: Teclado abierto

- **GIVEN** el formulario con el teclado abierto sobre «Productos y servicios (opcional)»
- **WHEN** se desplaza el formulario
- **THEN** se puede llegar al menú «Clasificación» y a «Guardar»
