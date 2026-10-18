# Suite final (tarea 8.1) y recorrido de extremo a extremo (tarea 8.2)

Este registro reemplaza por completo el anterior al grupo 9, que describía el código con varias clasificaciones por empresa.

- Fecha: 2026-10-18.
- Código: commit base `4270877` («Give each company exactly one classification»), sin cambios locales en el código.
- Entorno: `JAVA_HOME` del JBR de Android Studio; teléfono físico, Android 15, API 35. Antes de empezar se desinstaló la app (`adb uninstall com.edu.unal.directorio`).

## Tarea 8.2: `DirectorioEndToEndTest`

Comando: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.edu.unal.directorio.DirectorioEndToEndTest`

Resultado: BUILD SUCCESSFUL. 1 prueba, 0 fallos, 0 errores, 0 omitidas.

## Tarea 8.1: suite completa

### Pruebas JVM y build

Comando: `./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`

Resultado: BUILD SUCCESSFUL. 112 pruebas, 0 fallos, 0 errores, 0 omitidas.

| Clase | Pruebas |
|---|---|
| data.db.SampleCompaniesTest | 7 |
| data.FakeCompanyRepositoryTest | 8 |
| domain.ClassificationTest | 3 |
| domain.CompanyValidatorTest | 30 |
| domain.TextNormalizerTest | 8 |
| ExampleUnitTest | 1 |
| ui.detail.CompanyDetailViewModelTest | 12 |
| ui.form.CompanyFormViewModelTest | 21 |
| ui.list.CompanyListViewModelTest | 15 |
| ui.navigation.NavigationViewModelTest | 7 |
| **Total** | **112** |

Lint (`lintDebug`): 0 errors, 12 warnings: 7 UnusedResources, 2 AndroidGradlePluginVersion, 1 NewerVersionAvailable, 1 ObsoleteSdkInt, 1 RedundantLabel. Ninguno es un error.

### Pruebas instrumentadas

Comando: `./gradlew :app:connectedDebugAndroidTest` (sin filtro, teléfono físico, Android 15, API 35)

Resultado: BUILD SUCCESSFUL. 94 pruebas, 0 fallos, 0 errores, 0 omitidas.

| Clase | Pruebas |
|---|---|
| AppResetTest | 3 |
| CompanyDaoTest | 24 |
| CompanyDetailScreenTest | 9 |
| CompanyFormScreenTest | 15 |
| CompanyListScreenTest | 8 |
| DirectorioDetailFlowTest | 6 |
| DirectorioEndToEndTest | 1 |
| DirectorioFormFlowTest | 2 |
| DirectorioListFlowTest | 3 |
| ExampleInstrumentedTest | 1 |
| RoomCompanyRepositoryTest | 18 |
| RoomSmokeTest | 1 |
| SampleDataTest | 3 |
| **Total** | **94** |

## Correcciones

Correcciones: ninguna.
