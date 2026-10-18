# Verification record

Cambio: `add-company-directory` (reto 8, directorio de empresas con Room/SQLite).

## 1. Resumen

- El cambio está implementado y verificado: 112 pruebas JVM y 94 instrumentadas, 0 fallos, más QA manual en teléfono físico (Android 15, API 35) sin defectos.
- Commit probado: `4270877` (sin cambios locales en el código). Este registro y la evidencia de `qa/` son documentación posterior.
- Incluye la corrección del usuario (U10 a U15): cada empresa tiene exactamente una clasificación (catálogo `classification`, columna `classification_id`); el filtro del listado sigue siendo de selección múltiple. La suite y el QA se repitieron sobre ese código final.

## 2. Comandos y resultados

Entorno: `JAVA_HOME` del JBR de Android Studio; teléfono físico, Android 15, API 35. Detalle en [qa/suite-final.md](qa/suite-final.md).

| Sigla | Comando | Resultado |
|---|---|---|
| PJ | `./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` | BUILD SUCCESSFUL. 112 pruebas, 0 fallos. Lint: 0 errores, 12 advertencias (de la plantilla y de versiones de herramientas) |
| PI | `./gradlew :app:connectedDebugAndroidTest` | BUILD SUCCESSFUL. 94 pruebas, 0 fallos |
| E2E | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.directorio.DirectorioEndToEndTest` | BUILD SUCCESSFUL. 1 prueba, 0 fallos |

La prueba de build de Room y la subida de librerías están en [qa/room-build-probe.md](qa/room-build-probe.md).

## 3. Cobertura por requisito

Clases de `app/src/test` (JVM) y `app/src/androidTest` (instrumentadas, en el teléfono). «QA» remite a la sección 4.

### company-storage

| Requisito | Cobertura |
|---|---|
| Datos de una empresa | `RoomCompanyRepositoryTest`, `CompanyDaoTest`, `CompanyValidatorTest` |
| Catálogo de clasificaciones | `CompanyDaoTest` (tres filas y clave foránea), `ClassificationTest` |
| Persistencia entre aperturas | `SampleDataTest`, `AppResetTest`, `RoomCompanyRepositoryTest`; QA 8.4 |
| Comparación sin distinguir mayúsculas ni tildes | `TextNormalizerTest`, `CompanyDaoTest` |
| Unicidad de nombre y de correo | `RoomCompanyRepositoryTest`, `CompanyDaoTest`, `FakeCompanyRepositoryTest`; QA-2 |
| Datos de ejemplo solo en la primera creación | `SampleCompaniesTest`, `SampleDataTest`, `AppResetTest`; QA 8.3 y 8.4 |

### company-list

| Requisito | Cobertura |
|---|---|
| Listado ordenado de empresas | `CompanyListViewModelTest`, `CompanyDaoTest`, `DirectorioListFlowTest`; QA 8.3 |
| Mensajes de lista vacía | `CompanyListViewModelTest`, `CompanyListScreenTest`; QA 8.4 y QA-11 |
| Búsqueda por nombre | `CompanyListViewModelTest`, `CompanyDaoTest`, `CompanyListScreenTest` |
| Filtro por clasificación | `CompanyListViewModelTest`, `CompanyDaoTest`, `CompanyListScreenTest`; QA-4 |
| Búsqueda y clasificación se combinan | `CompanyListViewModelTest`, `CompanyDaoTest`, `DirectorioEndToEndTest`; QA 8.3 |
| Los filtros se conservan al ir al detalle y volver | `CompanyListViewModelTest`, `DirectorioListFlowTest`; QA-7 |
| Acceso al detalle y al alta | `CompanyListScreenTest` (incluye objetivos táctiles de 48 dp), `NavigationViewModelTest`; QA 8.5 |

### company-detail

| Requisito | Cobertura |
|---|---|
| Detalle de una empresa | `CompanyDetailScreenTest`, `CompanyDetailViewModelTest`, `DirectorioDetailFlowTest`; QA 8.3 |
| Editar desde el detalle | `CompanyDetailScreenTest`, `DirectorioDetailFlowTest`, `NavigationViewModelTest`; QA-3 |
| Eliminar con confirmación | `CompanyDetailScreenTest`, `CompanyDetailViewModelTest`, `DirectorioDetailFlowTest`, `DirectorioEndToEndTest`; QA 8.3, QA-5 y QA-9 |
| Detalle de una empresa que ya no existe | `CompanyDetailViewModelTest` (solo automática) |

### company-form

| Requisito | Cobertura |
|---|---|
| Un formulario para crear y editar | `CompanyFormScreenTest`, `CompanyFormViewModelTest`, `DirectorioFormFlowTest`; QA-1 y QA-3 |
| Validación del nombre | `CompanyValidatorTest`, `CompanyFormViewModelTest` |
| Validación del correo | `CompanyValidatorTest`, `CompanyFormViewModelTest` |
| Validación de la URL | `CompanyValidatorTest` (solo automática) |
| Validación del teléfono | `CompanyValidatorTest` (solo automática) |
| Validación de productos y servicios | `CompanyValidatorTest`, `CompanyFormScreenTest` |
| Clasificación obligatoria | `CompanyValidatorTest`, `CompanyFormViewModelTest`, `CompanyFormScreenTest`; QA-1 |
| Errores por campo | `CompanyFormViewModelTest`, `CompanyFormScreenTest`; QA 8.3 y QA-2 |
| Destinos tras guardar o cancelar | `NavigationViewModelTest`, `DirectorioFormFlowTest`, `CompanyFormViewModelTest`; QA-6 y QA-8 |
| Uso con teclado y orientación | `CompanyFormScreenTest` (desplazamiento hasta «Guardar»); QA 8.5 (giro de pantalla fuera de alcance, U8) |

## 4. QA manual

Teléfono físico, Android 15, API 35, aplicación debug recién instalada. Resultado: todo pasa, sin defectos.

- Tarea 8.3, capturas guiadas (listado inicial con 8 empresas, filtro combinado, formulario con errores, menú de clasificación, detalle y diálogo de eliminación): [qa/8-3-capturas.md](qa/8-3-capturas.md). PASA.
- Tarea 8.4, regla de «no reaparecen»: tras eliminar las 8 empresas y `force-stop` sigue vacía («Aún no hay empresas registradas»); tras `pm clear` vuelven los 8 ejemplos: [qa/ejemplos-primera-vez.md](qa/ejemplos-primera-vez.md). PASA.
- Tarea 8.5, teclado abierto, descripciones de los botones solo de ícono y textos fuera de `strings.xml`: [qa/teclado-y-accesibilidad.md](qa/teclado-y-accesibilidad.md). PASA.
- Nueve casos adicionales (QA-1 a QA-9: alta con clasificación, duplicados, edición, filtro con dos chips, Back y Cancelar en diálogo y edición, etc.): [qa/qa-manual.md](qa/qa-manual.md). Todos pasan.

## 5. No verificado a mano

| Qué | Por qué | Cobertura |
|---|---|---|
| Tildes y «ñ» escritas en la interfaz | `adb input text` no las permite | Pruebas automáticas (`TextNormalizerTest`, `CompanyDaoTest`, `RoomCompanyRepositoryTest`) |
| Formatos de URL, teléfono y correo, y límite de 100 caracteres del nombre | Combinatoria amplia para hacerla a mano | `CompanyValidatorTest` |
| Doble toque en «Guardar» y fallos de almacenamiento | No reproducibles de forma fiable con `adb` | `CompanyFormViewModelTest`, `CompanyDetailViewModelTest`, `RoomCompanyRepositoryTest` |
| Giro de pantalla | Fuera de alcance (U8) | Ninguna |
| Abrir el detalle de una empresa inexistente | No hay forma de provocarlo desde la interfaz | `CompanyDetailViewModelTest` |

## 6. Observaciones y riesgos

Ninguna es un defecto.

- Con la densidad de pantalla del teléfono (ampliada), el tercer chip queda recortado en el borde, pero la fila se desplaza.
- El formulario no tiene flecha de volver; solo «Cancelar» y Back, lo que la spec permite.
- Con el teclado abierto, el primer Back solo lo cierra.
- Riesgo menor: la traducción de una violación de unicidad lanzada por la propia base dentro de la transacción de `save` no tiene prueba de extremo a extremo. La comprobación previa en la misma transacción la hace casi imposible; si ocurriera, el usuario vería «No se pudo guardar la empresa. Inténtalo de nuevo.» sin cierre de la app. La función `toSaveResult` sí está probada con la excepción real (`RoomCompanyRepositoryTest`, `uniqueViolationFromTheDatabaseIsTranslatedByColumn`).

## 7. Versiones finales

- Room 3.0.3 (`androidx.room3`) y KSP 2.3.12.
- Kotlin 2.2.10 (el Kotlin integrado de AGP 9.4.0) y Gradle 9.6.0.
- Driver `AndroidSQLiteDriver`, de `androidx.sqlite:sqlite-framework` 2.7.1.
- Compose BOM 2026.09.00, core-ktx 1.19.1, lifecycle 2.11.0, activity-compose 1.13.0, coroutines-test 1.11.0.
- minSdk 35, compileSdk y targetSdk 37.

## 8. Commits

- `5f92fea`: grupo 1, Room 3 en el build y librerías actualizadas.
- `ac8f33d`: grupo 2, dominio, normalización y validación.
- `2770fa6`: grupo 3, persistencia con Room.
- `73211b8`: grupo 4, textos y navegación.
- `b9591fc`: grupo 5, listado.
- `3aa42be`: grupo 6, formulario.
- `717ddbd`: grupo 7, detalle y eliminación.
- `4270877`: grupo 9, una sola clasificación por empresa.
