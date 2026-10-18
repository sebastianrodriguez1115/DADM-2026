# Design

## Context

Ver `proposal.md` para motivación, alcance y decisiones; los requisitos observables viven en los deltas de `specs/`.

Estado actual verificado en `/Users/jsrc1115/Code/DADM-2026/8PersistenceSQLite` (en adelante `P/` = `app/src/main/java/com/edu/unal/directorio/`):

- La aplicación es el Hello World ya renombrado: `P/MainActivity.kt`, `P/ui/theme/` con `DirectorioTheme`, y `ExampleUnitTest` y `ExampleInstrumentedTest` de la plantilla. El manifest tiene `windowSoftInputMode="adjustResize"` y la orientación no está fija.
- Build: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.2.10 con el Kotlin integrado de AGP 9 (solo se aplica `org.jetbrains.kotlin.plugin.compose`, no `kotlin-android`), Compose BOM 2026.02.01, minSdk 35, compileSdk y targetSdk 37, Java 11, `org.gradle.configuration-cache=true`, catálogo en `gradle/libs.versions.toml`.
- Dependencias de la plantilla, viejas: core-ktx 1.10.1, lifecycle-runtime-ktx 2.6.1, activity-compose 1.8.0, junit 4.13.2, androidx junit 1.1.5, espresso 3.5.1. No hay `lifecycle-viewmodel-compose` ni `kotlinx-coroutines-test`.
- No se deben usar los flags `android.builtInKotlin=false` ni `android.newDsl=false` (documentación de migración a Kotlin integrado).

Verificado el 2026-10-18 por el orquestador en Google Maven y el Gradle Plugin Portal: Room 3 estable 3.0.3 (grupo `androidx.room3`, artefactos `room3-runtime`, `room3-compiler`, `room3-testing`; plugin `androidx.room3`); Room 2.8.5 (`androidx.room`) en mantenimiento; KSP `com.google.devtools.ksp` 2.3.12 como última (desde 2.3.0 su versión no va atada a la de Kotlin); `androidx.sqlite:sqlite-bundled` y `sqlite-framework` 2.7.1. Según la documentación oficial, Room 3 elimina SupportSQLite y exige un `SQLiteDriver`, solo usa KSP, sus DAO deben ser `suspend` o devolver `Flow`, sus callbacks son `suspend` con `SQLiteConnection` y no existe `room3-ktx`.

Dato verificado en los POM de Google Maven: `room3-runtime-android` 3.0.3 declara `kotlin-stdlib` 2.1.20, por lo que la librería que usa la app es legible por Kotlin 2.2.10; `room3-compiler` 3.0.3 declara `kotlin-stdlib` 2.3.20, pero corre dentro de KSP. La decisión P1 (Room 3.0.3, KSP 2.3.12, Kotlin 2.2.10) fue confirmada por el usuario el 2026-10-18.

**No verificado** (lo resuelve el grupo 1 de `tasks.md`): que el código generado compile con Kotlin 2.2.10 y que KSP funcione con el Kotlin integrado de AGP 9.4; si Kotlin 2.2.10 basta con KSP 2.3.12 y Room 3.0.3; si el bloque del plugin de Gradle de Room 3 se llama `room3 { }` o `room { }` (este diseño evita depender de él, ver D1); en qué artefacto está `AndroidSQLiteDriver`; la firma exacta de `onCreate` del callback.

## Goals / Non-Goals

**Goals:** CRUD completo de empresas sobre SQLite con Room; listado con búsqueda y filtro; confirmación al eliminar; validación y unicidad correctas con mayúsculas, tildes y eñe; pruebas automáticas en JVM y en el teléfono; un diseño pequeño, proporcional a una aplicación CRUD.

**Non-Goals:** ver Out of Scope en `proposal.md`. Además: no hay migraciones, no hay inyección de dependencias con framework y no se prueba la rotación.

## Decisions

### D1. Room 3.0.3 con KSP y sin plugin de Gradle de Room (U1, P1)

- Versiones: `room3 = 3.0.3` (`androidx.room3:room3-runtime`, `room3-compiler` vía `ksp(...)`, `room3-testing` en `androidTestImplementation` solo si una prueba lo necesita), `ksp = 2.3.12` (plugin `com.google.devtools.ksp`), Kotlin 2.2.10 sin cambios.
- `exportSchema = false` y `version = 1`: no hay migraciones (Out of Scope), así que no se exporta el esquema ni se necesita el plugin `androidx.room3`, cuyo nombre de bloque no está verificado. Si el compilador exige un directorio de esquemas, la tarea 1.2 lo resuelve y lo registra.
- Contingencia, en este orden y registrando cada paso en `qa/room-build-probe.md`: (a) KSP 2.3.10 o 2.3.6; (b) Kotlin 2.3.20 (la versión de `kotlin-stdlib` que declara `room3-compiler` 3.0.3) junto con el plugin de Compose en la misma versión; (c) volver a Room 2.8.5 (`androidx.room`, `room-runtime`, `room-ktx`, `room-compiler`, `room-testing`). La opción (c) cambia imports y callbacks (`SupportSQLiteDatabase`) y el DAO puede quedar igual; se anota en el registro y en este documento.
- Alternativa descartada: `SQLiteOpenHelper` a mano (lo que usa el tutorial del enunciado). Es más código repetitivo (cursor, `ContentValues`, hilos) y sin comprobación de consultas en compilación; el usuario eligió Room (U1). Alternativa descartada: Room 2.8.5 de entrada, por estar en mantenimiento.

### D2. Driver: `AndroidSQLiteDriver` (P2)

Room 3 exige un `SQLiteDriver`. Se usa `AndroidSQLiteDriver`, que delega en el SQLite del sistema Android: el reto trata de la base SQLite de Android, no se agrega librería nativa ni peso al APK y el comportamiento es el del dispositivo (API 35). El artefacto que lo contiene (probablemente `androidx.sqlite:sqlite-framework`) se confirma en 1.2.

- Alternativa descartada: `BundledSQLiteDriver` (`androidx.sqlite:sqlite-bundled`). Da una versión de SQLite idéntica en todos los dispositivos, pero añade una librería nativa y su tamaño; no hace falta aquí. Si 1.2 mostrara que el driver del sistema no es utilizable con Room 3, se cambia a `BundledSQLiteDriver` y se registra.
- Las pruebas instrumentadas con base en memoria usan el mismo driver.

### D3. Modelo de datos (U4, U10, U11, P4)

Dos tablas, esquema versión 1. Decisión confirmada por el usuario el 2026-10-18 (U10 y U11), que sustituye al modelo anterior de varias clasificaciones por empresa con tabla intermedia.

`classification` (catálogo)
| Columna | Tipo | Restricción |
|---|---|---|
| `id` | INTEGER | clave primaria, sin autogenerar |
| `code` | TEXT | NOT NULL, índice único |

Tres filas fijas: `1 = CONSULTING`, `2 = CUSTOM_DEVELOPMENT`, `3 = SOFTWARE_FACTORY`.

`company`
| Columna | Tipo | Restricción |
|---|---|---|
| `id` | INTEGER | clave primaria autogenerada |
| `name` | TEXT | NOT NULL, valor recortado tal como se escribió |
| `name_key` | TEXT | NOT NULL, forma normalizada (D4) |
| `email` | TEXT | NOT NULL, recortado |
| `email_key` | TEXT | NOT NULL, forma normalizada |
| `url` | TEXT | NOT NULL, `''` si no se indicó |
| `phone` | TEXT | NOT NULL, `''` si no se indicó |
| `products` | TEXT | NOT NULL, `''` si no se indicó |
| `classification_id` | INTEGER | NOT NULL, clave foránea a `classification(id)` |

Índices: `UNIQUE(name_key)`, `UNIQUE(email_key)` e índice por `classification_id` (Room advierte si falta para la clave foránea; además sirve al filtro). La clave foránea no lleva cascada: el catálogo nunca se borra, y borrar una empresa no toca el catálogo. Room activa `PRAGMA foreign_keys` en sus conexiones; una prueba verifica que la base rechaza una clasificación inexistente. La tabla `company_classification` desaparece.

- `enum Classification { Consulting, CustomDevelopment, SoftwareFactory }` en `P/domain/Classification.kt`, con la etiqueta en español obtenida de recursos en la capa de interfaz. Cada valor lleva su `code` de texto y su `id` fijo (`Consulting(1, "CONSULTING")`, `CustomDevelopment(2, "CUSTOM_DEVELOPMENT")`, `SoftwareFactory(3, "SOFTWARE_FACTORY")`) y expone `fromId(id)`; la entidad guarda el `id` y el enum es la única fuente de los pares `id`/`code`. Se guarda el `id` (no el nombre visible ni el ordinal, que dependería del orden de declaración). Una prueba comprueba que los pares del enum coinciden con las filas del catálogo.
- El catálogo lo inserta el callback `onCreate` en toda base nueva, también en memoria, a partir del mismo enum (D8).
- Modelo de dominio: `Company(id: Long, name, email, url, phone, products, classification: Classification)` y `CompanyDraft` (sin `id`, con `classification: Classification?` para el formulario, nula hasta que se elige). `CompanyFilter` conserva `Set<Classification>`.
- Alternativas descartadas por el usuario (2026-10-18): una columna `classification` de texto en `company` sin tabla de catálogo, y mantener la tabla intermedia `company_classification`. Alternativa descartada antes (varias clasificaciones): tres columnas booleanas.
- La obligatoriedad ya la impone el esquema (`NOT NULL` y clave foránea); además la comprueban el validador y el repositorio, que no acepta un borrador sin clasificación.

### D4. Normalización de texto y unicidad (U3, U4, P5)

`SQLite` plegaría solo ASCII con `LIKE` o `COLLATE NOCASE`, lo que falla con «Á/á» y «Ñ/ñ». Por eso la comparación se hace sobre columnas normalizadas por Kotlin:

- `normalizeKey(texto) = Normalizer.normalize(texto.trim(), Form.NFD).replace(\p{Mn}+, "").lowercase(Locale.ROOT)`. Quita espacios extremos, descompone las letras acentuadas, elimina las marcas combinantes (tildes, diéresis, y la tilde de la «ñ») y pasa a minúsculas independientemente de la configuración regional. Vive en `P/domain/TextNormalizer.kt` y es Kotlin puro (`java.text.Normalizer`), probable en la JVM.
- Decisión (P5, confirmada por el usuario el 2026-10-18): también se ignoran las tildes, así que «ñ» se trata como «n» y «Peña» choca con «Pena». Alternativa: solo plegar mayúsculas con `lowercase(Locale.ROOT)` y conservar las tildes; es un cambio de una línea en `normalizeKey` y de dos escenarios.
- `name_key` y `email_key` tienen índice único. El repositorio calcula las claves al guardar; ninguna otra ruta escribe esas columnas. El correo se normaliza con la misma función (el correo en minúsculas es lo esperado; las tildes no son válidas en el formato de D5).
- La unicidad se impone en la base y además se comprueba antes de guardar: `CompanyDao.findIdByNameKey(key)` y `findIdByEmailKey(key)` devuelven el `id` que tiene esa clave; si el `id` existe y es distinto del que se edita, hay duplicado. Al editar, la propia empresa queda excluida porque su `id` coincide. La comprobación y la escritura van en una sola transacción. Si aun así la base lanza la violación de unicidad (carrera o ruta no prevista), el repositorio la captura y devuelve el mismo resultado `NameTaken` o `EmailTaken` según el índice que falló, nunca un fallo genérico.
- Alternativa descartada: `COLLATE NOCASE` en las columnas. No pliega no-ASCII. Alternativa descartada: registrar un colador personalizado; Room 3 sobre el driver no lo expone de forma simple.

### D5. Validaciones exactas (U4, P6)

Función pura `CompanyValidator.validate(draft, ...)` en `P/domain/CompanyValidator.kt`, sin `android.util.Patterns`, para probar en la JVM. Primero recorta todos los campos. Devuelve un `ValidationErrors` con un código por campo (decisión U13: el texto del error de clasificación es «Selecciona una clasificación») (no el texto: el texto vive en recursos).

| Campo | Regla |
|---|---|
| Nombre | 1 a 100 caracteres tras recortar |
| Correo | 1 a 254 caracteres y coincide con `^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$` |
| URL (opcional) | vacía, o con máximo 200 caracteres y coincide con `^(https?://)?([A-Za-z0-9-]+\.)+[A-Za-z]{2,}(:[0-9]{1,5})?(/\S*)?$` ignorando mayúsculas |
| Teléfono (opcional) | vacío, o coincide con `^\+?[0-9 ().-]+$` y tiene entre 7 y 15 dígitos (se cuentan los caracteres `0-9`) |
| Productos (opcional) | máximo 1000 caracteres tras recortar |
| Clasificación | obligatoria: debe haber exactamente una elegida (en el borrador es nula hasta elegirla) |

- La URL se guarda como se escribió, sin prefijo; el detalle la muestra tal cual (sin abrirla, U8).
- La unicidad no es parte del validador puro: la comprueba el repositorio (D4) y el ViewModel del formulario traduce `NameTaken` y `EmailTaken` a errores de los campos.
- La URL acepta direcciones sin esquema porque es lo que la gente escribe; no se acepta `ftp://` ni `localhost` (se exige un dominio con punto). El teléfono es deliberadamente laxo con el formato porque hay muchos formatos nacionales. Estas reglas fueron confirmadas por el usuario el 2026-10-18 (P6).
- Textos de error exactos: los de `specs/company-form/spec.md`, que se copian a `strings.xml` sin alteración.

### D6. Navegación manual y arquitectura de capas (U5, U8, P7)

- Navegación: un `NavigationViewModel` pequeño con una pantalla actual sellada (`List`, `Detail(id)`, `Form(id: Long?)`) y `BackHandler` de `androidx.activity.compose` en `Detail` y `Form` (volver a la pantalla de origen); en `List` no hay `BackHandler` y se aplica la navegación del sistema. Es la misma solución que el taller 7 (D1 de ese diseño).
- Alternativa descartada: Navigation Compose. Para tres destinos con un único argumento (el `id`) una dependencia nueva y su manejo de pila aportan más superficie que una clase sellada.
- Sin `SavedStateHandle` ni requisito de rotación (U8). Los ViewModels tienen alcance de Activity y, al ser el diseño estándar, conservan su estado al girar sin trabajo extra; no se especifica ni se prueba. Si por la muerte del proceso la pantalla vuelve al listado, es aceptable.
- Capas:
  - `P/domain/`: `Classification.kt`, `Company.kt`, `CompanyDraft.kt`, `TextNormalizer.kt`, `CompanyValidator.kt`, `CompanyFilter.kt` (texto y conjunto de clasificaciones).
  - `P/data/`: `CompanyRepository.kt` (interfaz), `RoomCompanyRepository.kt`, y `P/data/db/` con `CompanyEntity.kt`, `ClassificationEntity.kt` (catálogo), `CompanyDao.kt`, `DirectorioDatabase.kt`, `SampleCompanies.kt`.
  - `P/ui/navigation/`: `Screen.kt`, `NavigationViewModel.kt`, `AppNavigation.kt`.
  - `P/ui/list/`: `CompanyListViewModel.kt`, `CompanyListScreen.kt`.
  - `P/ui/detail/`: `CompanyDetailViewModel.kt`, `CompanyDetailScreen.kt`, `DeleteCompanyDialog.kt`.
  - `P/ui/form/`: `CompanyFormViewModel.kt`, `CompanyFormScreen.kt`.
  - `P/DirectorioApplication.kt` (crea la base y el repositorio una vez por proceso y los expone mediante un `AppContainer` sencillo con un reinicio solo para pruebas, ver D10), `P/MainActivity.kt` (monta `AppNavigation`), y fábricas de ViewModel manuales (`viewModelFactory { initializer { } }`).
- Interfaz del repositorio: `observeCompanies(filter): Flow<List<Company>>`, `observeCompany(id): Flow<Company?>`, `save(draft, editingId: Long?): SaveResult` (`Saved(id)`, `NameTaken`, `EmailTaken`, `NameAndEmailTaken`), `delete(id)`, y `suspend` donde corresponda. La excepción inesperada se propaga como excepción y el ViewModel la traduce a «No se pudo guardar…» o «No se pudo eliminar…».
- Filtros: los guarda `CompanyListViewModel` (alcance de Activity), por lo que no se pierden al ir al detalle y volver; el listado se deriva con `combine`/`flatMapLatest` sobre el filtro y `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000))`.
- `lifecycle-viewmodel-compose` se agrega para `viewModel()` en composables, en su última estable alineada con `lifecycle-runtime-ktx`.

### D7. Consulta de listado y filtro (U3, U10)

El filtro sigue siendo de selección múltiple (U3 no cambia). El DAO devuelve un `Flow` con la consulta combinada, parametrizada con el texto ya normalizado y escapado por Kotlin y con los `id` fijos de las clasificaciones marcadas:

```sql
SELECT * FROM company
WHERE name_key LIKE '%' || :query || '%' ESCAPE '\'
  AND (:noClassFilter = 1 OR classification_id IN (:classificationIds))
ORDER BY name_key, id
```

- `query` es `normalizeKey(texto)` con `\`, `%` y `_` escapados con `\` (el campo vacío da `''` y `LIKE '%%'` coincide con todo). `noClassFilter` es 1 cuando no hay chips; entonces `classificationIds` va como lista VACÍA, sin valor de relleno: Room 3 expande la lista con `appendPlaceholders` y queda `IN ()`, que SQLite acepta y evalúa como falso, y la rama `:noClassFilter = 1` hace verdadera toda la condición (verificado desde el grupo 3 por `CompanyDaoTest.emptyChipListWithNoClassFilterReturnsEverything`, que pasa en el teléfono). **Sin chips marcados no se filtra por clasificación** y la lista queda decidida solo por la búsqueda.
- Como cada empresa tiene una sola clasificación, `classification_id IN (...)` no puede repetir empresas: ya no hace falta subconsulta ni deduplicar. «Las marcadas» es la unión; búsqueda y chips se combinan con `AND`. El orden por `name_key` (sin mayúsculas ni tildes) da el orden alfabético especificado, con `id` para desempatar de forma estable.
- La clasificación de cada empresa viene en la propia fila (`classification_id`), así que no hace falta `@Relation` ni lecturas con relaciones: el DAO devuelve `CompanyEntity` y el repositorio lo mapea con `Classification.fromId`. El `Flow` se reemite al cambiar `company`, y el catálogo es inmutable.
- Alternativa descartada: filtrar en memoria sobre `getAll()`. Funcionaría con pocas filas, pero la consigna del reto es exactamente consultar la base; la consulta SQL es más fiel y más fácil de probar con una base real.

### D8. Catálogo y datos de ejemplo en el callback de creación (U7, U11, P3)

- **Catálogo (U11).** El callback `onCreate` inserta primero las tres filas de `classification` a partir del enum, en toda base que se crea, incluidas las bases en memoria de las pruebas, porque la clave foránea de `company` las necesita. Las bases en memoria dejan de ser «sin callback»: llevan un callback que solo carga el catálogo y no los ejemplos.
- **Ejemplos.** Se cargan solo en la base en archivo, en el callback `onCreate` de la base, ejecutando SQL con parámetros sobre la `SQLiteConnection` que entrega Room 3, dentro de la transacción de creación. `onCreate` se ejecuta solo cuando el archivo de base se crea, lo que cumple «solo la primera vez» y «no reaparecen tras borrar todo» sin guardar banderas.
- Las 8 empresas viven en `P/data/db/SampleCompanies.kt` como lista de `CompanyDraft` y se insertan calculando las claves con `normalizeKey`, reutilizando el mismo código que el repositorio. Nombres ficticios; correos `contacto@<empresa>.example.com`; URLs `https://<empresa>.example.com`; teléfonos con prefijo `+57` y números de ejemplo `555-01xx`; cada una con una sola clasificación y cubriendo las tres (por ejemplo 3, 3 y 2 empresas), al menos una con productos de varias líneas y una con URL y teléfono vacíos.
- Alternativa descartada: `createFromAsset` con un `.db` empaquetado. Obliga a mantener un archivo binario que se desactualiza con el esquema y no se puede revisar en un diff.
- Las pruebas del DAO usan una base en memoria con el callback que carga solo el catálogo, así que arrancan sin empresas pero con las tres clasificaciones; la prueba de carga usa su propio archivo de base (D10) que borra antes y después, sin tocar el de la app.
- La firma de `onCreate` en Room 3 no está verificada; se confirma en 3.6. Con la contingencia (c) la firma sería la de Room 2.8.5 (`SupportSQLiteDatabase`).

### D9. Comportamiento de pantallas (P8)

- Listado: `Scaffold` con barra de título, campo de búsqueda, fila de `FilterChip` de selección múltiple desplazable horizontalmente, `LazyColumn` y botón flotante «+». Con estado de vacío en dos variantes según haya o no empresas en total (el ViewModel expone `totalCount` o un indicador `hasAnyCompany` junto con la lista filtrada).
- Detalle: `Column` desplazable; observa la empresa por `id`; si el `Flow` emite `null` (por ejemplo, tras eliminarla), el ViewModel emite la señal de volver al listado y no falla.
- Formulario (U12): `Column` con `verticalScroll`, `imePadding()` y errores como `supportingText` con `isError`; la clasificación es un `ExposedDropdownMenuBox` de Material 3 con un `OutlinedTextField` de solo lectura (`readOnly = true`, etiqueta «Clasificación», `ExposedDropdownMenuDefaults.TrailingIcon` y `menuAnchor`) y un `ExposedDropdownMenu` con tres `DropdownMenuItem` («Consultoría», «Desarrollo a la medida», «Fábrica de software»); sin selección el campo queda vacío, el error «Selecciona una clasificación» va como `supportingText` con `isError` y desaparece al elegir. Alternativas descartadas por el usuario: opciones tipo radio y casillas. Los demás textos del formulario son los de la spec. Un único `CompanyFormViewModel` con `open(id: Long?)` que carga la empresa a editar; al guardar con éxito emite el destino (listado al crear, detalle al editar).
- Eliminar: confirmación en `AlertDialog` (`DeleteCompanyDialog`). El estado del diálogo vive en el ViewModel del detalle.
- Accesibilidad: el botón «+» lleva `contentDescription` «Agregar empresa»; la acción de volver, «Volver»; el borrado de búsqueda, «Borrar búsqueda». Objetivos de al menos 48 dp. Los chips y las opciones del menú llevan su etiqueta como texto.
- Todos los textos están en `strings.xml` y las pruebas los obtienen de los recursos o de los literales de las specs.

### D10. Estrategia de pruebas (U9)

- JVM (`./gradlew :app:testDebugUnitTest`): `TextNormalizerTest`, `CompanyValidatorTest`, `SampleCompaniesTest` (cumplen el validador y no repiten claves), `NavigationViewModelTest`, `CompanyListViewModelTest`, `CompanyDetailViewModelTest`, `CompanyFormViewModelTest`, todos con `FakeCompanyRepository` en `app/src/test/.../data/` y `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`, `Dispatchers.setMain`).
- Instrumentadas en el teléfono (`./gradlew :app:connectedDebugAndroidTest`): `CompanyDaoTest` (base en memoria con el catálogo: CRUD, unicidad, clave foránea del catálogo, filtros con mayúsculas, tildes, eñe, unión de clasificaciones, combinación), `RoomCompanyRepositoryTest`, `SampleDataTest` (archivo real: primera creación, no reaparece), y pruebas de pantalla con `createComposeRule` para `CompanyListScreen`, `CompanyDetailScreen`, `CompanyFormScreen` con estado y callbacks como parámetros, más cuatro clases de flujo sobre `MainActivity` con la base real en archivo: `DirectorioListFlowTest`, `DirectorioFormFlowTest`, `DirectorioDetailFlowTest` y `DirectorioEndToEndTest` (la última, el recorrido completo).
- **Pruebas afectadas por la corrección de una clasificación (U10 a U15).** Se reescriben o amplían: `CompanyValidatorTest`, `SampleCompaniesTest`, `FakeCompanyRepositoryTest`, `CompanyListViewModelTest`, `CompanyFormViewModelTest`, `CompanyDetailViewModelTest`, `CompanyDaoTest`, `RoomCompanyRepositoryTest`, `SampleDataTest`, `AppResetTest` (solo si usa clasificaciones), `CompanyListScreenTest`, `CompanyFormScreenTest`, `CompanyDetailScreenTest`, `DirectorioListFlowTest`, `DirectorioFormFlowTest`, `DirectorioDetailFlowTest` y `DirectorioEndToEndTest`. Las pruebas de atomicidad de `RoomCompanyRepositoryTest` que instalaban un disparador sobre `company_classification` se **reemplazan**: con una clasificación por columna ya no hay segunda escritura que pueda fallar sola, pero se conserva la garantía de que un fallo durante el guardado no deja un estado a medias. Las nuevas instalan un disparador `BEFORE INSERT` y `BEFORE UPDATE` sobre `company` que aborta (`RAISE(ABORT, ...)`) para un nombre concreto, y comprueban que un alta fallida no deja filas y que una edición fallida deja intacta la fila original, incluida su clasificación.
- **Aislamiento de las pruebas que usan la base real.** `connectedDebugAndroidTest` corre todas las clases en un mismo proceso de instrumentación y `DirectorioApplication` crea la base una sola vez por proceso, así que borrar el archivo con la base abierta no la reinicia. Mecanismo único para las cuatro clases de flujo: el `AppContainer` tiene una función `resetForTests()` que cierra la base, ejecuta `context.deleteDatabase(<nombre de la base de la app>)` y reconstruye la base y el repositorio; al reconstruir corre el callback de creación y vuelven los 8 ejemplos. Los ViewModels leen el repositorio del contenedor al crearse, y cada lanzamiento de la Activity tiene ViewModels nuevos, por lo que usan la base reconstruida. Un ayudante `A/TestAppReset.kt` ofrece `resetAppDatabase()` (arranca con los 8 ejemplos) y `resetAppDatabaseEmpty()` (reinicia y luego elimina todas las empresas por el repositorio). Las pruebas de flujo usan `createEmptyComposeRule()`, llaman al ayudante en `@Before` y solo después lanzan `ActivityScenario.launch(MainActivity::class.java)`, que cierran en `@After`; no usan `createAndroidComposeRule<MainActivity>()`, porque lanza la Activity antes de `@Before`. Cada prueba de flujo declara en su cabecera con qué estado arranca (8 ejemplos o vacía).
- `SampleDataTest` no usa el contenedor ni el archivo de la app: usa su propio nombre de archivo (`directorio-sampledata-test.db`), construye `DirectorioDatabase` directamente con el callback de creación, simula «reabrir» cerrando y construyendo de nuevo con el mismo nombre, y borra su archivo antes y después.
- Convención de ejecución: se usa `connectedDebugAndroidTest`, porque hay un único teléfono, está autorizado instalar y desinstalar la aplicación y Gradle gestiona la instalación del APK de pruebas. Para una clase, `-Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.directorio.<Clase>`. El análisis de resultados se hace en `app/build/reports/androidTests/connected/`.

## Risks / Trade-offs

- **Reinstalación en el teléfono por el cambio de esquema en la versión 1** (U14, confirmado el 2026-10-18). La corrección cambia el esquema (tabla `classification`, columna `classification_id`, sin `company_classification`) sin subir la versión, porque la app no está publicada y no hay migración. Room guarda una identidad (hash) del esquema en cada base y, al detectar que la base existente en el teléfono tiene otra identidad con la misma versión, falla al abrirla. Antes de probar en el teléfono hay que desinstalar la aplicación (`adb uninstall com.edu.unal.directorio`) o limpiar sus datos (`adb shell pm clear com.edu.unal.directorio`); se pierden los datos de prueba. Las pruebas con bases en memoria o con su propio archivo no se ven afectadas, pero las de flujo usan la base de la app: si el teléfono conserva la base antigua, la primera ejecución tras el cambio puede fallar al abrirla, así que debe partir de una instalación limpia (desinstalar antes).
- **Compatibilidad de Room 3 con Kotlin 2.2.10 y KSP 2.3.12** (P1). Mitigación: grupo 1 como prueba de build con contingencia ordenada a, b y c (D1). Si se llega a (c), el resto del diseño no cambia salvo imports y el callback.
- **Plugin de Gradle y caché de configuración**: KSP con la caché activa puede emitir advertencias o fallar. Mitigación: 1.4 corre `assembleDebug` dos veces y registra si se reutiliza la entrada de caché.
- **Normalización de tildes elimina distinciones reales** («ñ» y «n»). Aceptado por el usuario el 2026-10-18 (P5).
- **Carga de ejemplo y el callback**: si la firma de `onCreate` difiere de lo esperado, se ajusta en 3.6; el riesgo es solo de compilación.
- **Pruebas en el teléfono físico**: borran los datos de la aplicación (autorizado, U9). La evidencia no incluye números de serie.
- **Versiones de AndroidX viejas** (P9). El usuario confirmó subirlas a su última estable (P9, 2026-10-18); la tarea 1.6 lo hace, incluido el BOM de Compose, sin tocar Gradle, AGP ni Kotlin. Una combinación inesperada de versiones nuevas puede romper el build o las pruebas; se detecta ahí mismo y se registra en `qa/room-build-probe.md`.
- **Unicidad con carrera**: un solo usuario y un solo proceso hacen la carrera casi imposible, pero el índice único y la captura de la violación (D4) la cubren.
