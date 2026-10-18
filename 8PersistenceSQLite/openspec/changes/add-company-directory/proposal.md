# Proposal

## Why

El reto 8 pide una aplicación que gestione un directorio de empresas de desarrollo de software con una base de datos SQLite: crear, consultar, modificar y eliminar empresas, listarlas, filtrarlas por nombre y por clasificación, y pedir confirmación antes de eliminar. La base actual del proyecto es un Hello World (`com.edu.unal.directorio`) sin datos, sin pantallas y sin persistencia.

## What Changes

- Añadir la persistencia local de empresas sobre SQLite, usando Room 3 como capa de acceso (ver D1 en `design.md`). Una empresa tiene nombre, URL, teléfono, correo, productos y servicios, y una o más clasificaciones («Consultoría», «Desarrollo a la medida», «Fábrica de software»).
- Imponer que el nombre y el correo sean únicos, sin distinguir mayúsculas y sin contar espacios al inicio ni al final; la unicidad la garantiza la base y además se comprueba antes de guardar para mostrar un mensaje claro en el campo.
- Cargar unas 8 empresas ficticias de ejemplo únicamente cuando la base se crea por primera vez; si la persona las borra todas, no reaparecen.
- Añadir tres pantallas en español: listado (búsqueda por nombre, chips de clasificación de selección múltiple y botón «+»), detalle (con «Editar» y «Eliminar», este último con diálogo de confirmación) y un único formulario para crear y editar con validación por campo.
- Navegar entre pantallas de forma manual con un ViewModel pequeño y `BackHandler`, sin librería de navegación (D6). Los filtros del listado se conservan al ir al detalle y volver.
- Añadir pruebas automáticas: JVM para validación y ViewModels con repositorio falso; instrumentadas en el teléfono para el DAO con base en memoria y para las pantallas.

## Capabilities

### New Capabilities

- `company-storage`: modelo de empresa, persistencia en SQLite, unicidad de nombre y correo, comparación de texto sin distinguir mayúsculas ni tildes y carga de datos de ejemplo en la primera creación.
- `company-list`: listado ordenado, mensajes de vacío, búsqueda por nombre, filtro por clasificaciones y acceso a detalle y alta.
- `company-detail`: detalle de una empresa, acceso a edición y eliminación con confirmación.
- `company-form`: formulario único de alta y edición, reglas de validación, mensajes de error por campo y destinos tras guardar o cancelar.

### Modified Capabilities

Ninguna: `openspec/specs/` está vacío.

## Impact

- Archivos de build: `gradle/libs.versions.toml`, `build.gradle.kts` (raíz, si hace falta declarar KSP) y `app/build.gradle.kts` (KSP, Room 3, driver SQLite, `lifecycle-viewmodel-compose`, pruebas). Ver D1 y D2.
- Código nuevo bajo `app/src/main/java/com/edu/unal/directorio/` (`data/`, `domain/`, `ui/`), una clase `Application`, y su declaración en `AndroidManifest.xml`.
- Recursos: `app/src/main/res/values/strings.xml` con todos los textos de la interfaz.
- Pruebas nuevas en `app/src/test/` y `app/src/androidTest/`; las plantillas `ExampleUnitTest` y `ExampleInstrumentedTest` se pueden conservar o adaptar (el paquete ya es `com.edu.unal.directorio`).
- Evidencia de verificación en `openspec/changes/add-company-directory/qa/` y `verification.md`.

## Out of Scope

- Abrir el sitio web, llamar o escribir un correo desde el detalle (rechazado por el usuario).
- Un ícono propio de la aplicación (rechazado por el usuario).
- La conservación del estado al girar el dispositivo como requisito: si el diseño estándar con ViewModel lo conserva sin trabajo extra está bien, pero no se especifica ni se prueba (D6).
- Migraciones de base de datos (solo existe la versión 1), exportación o importación de datos, copias de seguridad, sincronización en la nube.
- Ordenamientos alternativos, paginación, vista de tabla o mapa, imágenes o logotipos de empresas.
- Deshacer una eliminación.
- Librería de navegación, inyección de dependencias con framework, modo oscuro específico o localización a otros idiomas.

## Assumptions Confirmed by the User

Confirmadas el 2026-10-18.

- **U1.** La persistencia usa Room, en lugar de `SQLiteOpenHelper` escrito a mano.
- **U2.** Una empresa tiene una o más clasificaciones entre «Consultoría», «Desarrollo a la medida» y «Fábrica de software»; al menos una es obligatoria.
- **U3.** Filtros: campo de búsqueda por nombre (el texto puede estar en cualquier parte del nombre y no distingue mayúsculas); chips de clasificación de selección múltiple (con varias marcadas se muestran las empresas que tengan al menos una de ellas; sin chips marcados no se filtra por clasificación); la búsqueda y las clasificaciones se combinan y una empresa debe cumplir ambas.
- **U4.** Nombre: obligatorio y único. Correo: obligatorio, único y con formato válido. URL y teléfono: opcionales, con validación de formato si se llenan. Productos y servicios: texto libre de varias líneas, opcional. La unicidad no distingue mayúsculas e ignora espacios al inicio y al final.
- **U5.** Pantallas: listado (búsqueda, chips y botón «+»); detalle al tocar una empresa (con «Editar» y «Eliminar», este con diálogo de confirmación); un mismo formulario para crear y editar.
- **U6.** Paquete `com.edu.unal.directorio`, nombre «Directorio de empresas» e interfaz en español. Ya está hecho en la base actual.
- **U7.** Unas 8 empresas ficticias se cargan solo cuando la base se crea por primera vez; si la persona borra todas, no reaparecen.
- **U8.** Rechazados como extras: abrir la web, llamar o escribir un correo desde el detalle; un ícono propio; conservar el estado al girar como requisito. Ver Out of Scope.
- **U9.** Las pruebas son automáticas y se ejecutan en el teléfono. Está autorizado instalar y desinstalar la aplicación y borrar sus datos.
- **P1.** Room 3.0.3 con KSP 2.3.12 y Kotlin 2.2.10. Room 3 es la rama activa (Room 2.8.5 está en mantenimiento) y no hay `room3-ktx`. La compatibilidad con ese KSP y ese compilador la prueba el grupo 1 de `tasks.md` (prueba de build). Contingencia en orden: KSP 2.3.10 o 2.3.6; Kotlin 2.3.20 con el plugin de Compose; volver a Room 2.8.5. (Confirmada el 2026-10-18.)
- **P5.** La búsqueda y la unicidad ignoran mayúsculas, espacios al inicio y al final, y tildes: «tecnologia» encuentra «Tecnología» y «Peña» choca con «Pena». La «ñ» se trata como «n». La comparación se hace sobre columnas normalizadas en Kotlin porque SQLite solo pliega ASCII (D4). (Confirmada el 2026-10-18.)
- **P6.** Reglas de formato de D5: la URL acepta direcciones con o sin `http(s)://`; el teléfono admite 7 a 15 dígitos con `+`, espacios, guiones, puntos y paréntesis; el correo sigue una expresión sencilla. (Confirmada el 2026-10-18.)
- **P8.** Crear vuelve al listado; editar vuelve al detalle de la empresa editada; «Cancelar» y Back descartan los cambios sin preguntar. (Confirmada el 2026-10-18.)
- **P9.** Las librerías de la plantilla (AndroidX, BOM de Compose y de pruebas) se suben a su última estable, como en el taller 7 (decisión del 2026-10-10); las dependencias nuevas entran directamente en su última estable, alineadas con su familia. Gradle, AGP y Kotlin no cambian por esto. (Confirmada el 2026-10-18.)
