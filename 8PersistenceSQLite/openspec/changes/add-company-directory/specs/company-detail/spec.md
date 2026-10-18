# company-detail Specification

## Purpose

Define el detalle de una empresa, el acceso a su edición y la eliminación con confirmación.

## ADDED Requirements

### Requirement: Detalle de una empresa

El detalle SHALL tener el título «Detalle de empresa» y mostrar el nombre de la empresa y, con su etiqueta, «Clasificación», «Correo electrónico», «Sitio web», «Teléfono» y «Productos y servicios». Los campos opcionales vacíos SHALL mostrar «No indicado». Los productos y servicios SHALL conservar los saltos de línea. La pantalla SHALL tener una acción de volver con la descripción «Volver» y SHALL poder desplazarse cuando el contenido no cabe.

#### Scenario: Empresa completa

- **GIVEN** una empresa con todos sus campos
- **WHEN** se abre su detalle
- **THEN** se ven el nombre, las clasificaciones, el correo, el sitio web, el teléfono y los productos y servicios, cada uno con su etiqueta

#### Scenario: Campos opcionales vacíos

- **GIVEN** una empresa sin URL, teléfono ni productos y servicios
- **WHEN** se abre su detalle
- **THEN** «Sitio web», «Teléfono» y «Productos y servicios» muestran «No indicado»

#### Scenario: Productos con varias líneas

- **GIVEN** una empresa cuyos productos y servicios tienen tres líneas
- **WHEN** se abre su detalle
- **THEN** se muestran las tres líneas

#### Scenario: Volver al listado

- **GIVEN** el detalle de una empresa
- **WHEN** se pulsa «Volver» o Back
- **THEN** se muestra el listado

### Requirement: Editar desde el detalle

El detalle SHALL ofrecer un botón «Editar» que abre el formulario con los datos actuales de la empresa.

#### Scenario: Abrir la edición

- **GIVEN** el detalle de «Acme Software»
- **WHEN** se pulsa «Editar»
- **THEN** se muestra el formulario con el título «Editar empresa» y los campos con los valores actuales de «Acme Software»

### Requirement: Eliminar con confirmación

El detalle SHALL ofrecer un botón «Eliminar». Al pulsarlo SHALL mostrarse un diálogo con el título «¿Eliminar empresa?», el mensaje «Se eliminará «<nombre>» de forma permanente.» y los botones «Cancelar» y «Eliminar», y la empresa MUST NOT eliminarse hasta confirmar. Al confirmar, la empresa SHALL eliminarse y la aplicación SHALL volver al listado. Si falla la eliminación, SHALL mostrarse «No se pudo eliminar la empresa. Inténtalo de nuevo.» y la empresa SHALL conservarse.

#### Scenario: Confirmar la eliminación

- **GIVEN** el detalle de «Acme Software»
- **WHEN** se pulsa «Eliminar» y luego «Eliminar» en el diálogo
- **THEN** se muestra el listado y «Acme Software» ya no aparece

#### Scenario: Cancelar la eliminación

- **GIVEN** el diálogo «¿Eliminar empresa?» abierto sobre «Acme Software»
- **WHEN** se pulsa «Cancelar»
- **THEN** el diálogo se cierra, se sigue viendo el detalle y la empresa conserva sus datos en el listado

#### Scenario: Back cierra el diálogo sin eliminar

- **GIVEN** el diálogo «¿Eliminar empresa?» abierto
- **WHEN** se pulsa Back
- **THEN** el diálogo se cierra y la empresa no se elimina

#### Scenario: El mensaje nombra la empresa

- **GIVEN** el detalle de «Acme Software»
- **WHEN** se pulsa «Eliminar»
- **THEN** el diálogo muestra «Se eliminará «Acme Software» de forma permanente.»

#### Scenario: Falla la eliminación

- **GIVEN** el diálogo de eliminación abierto y un almacenamiento que falla al eliminar
- **WHEN** se confirma
- **THEN** se muestra «No se pudo eliminar la empresa. Inténtalo de nuevo.» y la empresa sigue existiendo

### Requirement: Detalle de una empresa que ya no existe

Si la empresa mostrada deja de existir mientras se ve su detalle, la aplicación MUST NOT fallar y SHALL volver al listado.

#### Scenario: Empresa eliminada mientras se ve

- **GIVEN** el detalle de una empresa abierto
- **WHEN** la empresa deja de existir en la base
- **THEN** la aplicación vuelve al listado sin cerrarse ni mostrar un error

#### Scenario: Identificador inexistente

- **GIVEN** que se pide el detalle de un identificador que no existe
- **WHEN** se abre la pantalla
- **THEN** la aplicación vuelve al listado sin cerrarse
