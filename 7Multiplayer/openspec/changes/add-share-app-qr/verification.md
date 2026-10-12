# Verification record

Cambio: `add-share-app-qr` (acción «Compartir app» en el lobby en línea, con el código QR del enlace de instalación). Rama `reto-7-triqui-online`. La tarea 5.2 (batería completa) y este registro (5.3) están hechos; la tarea 5.4 (comprobación manual del usuario) sigue pendiente.

## 1. Resumen

- El cambio está implementado y verificado de forma automática: 400 pruebas JVM y 325 pruebas instrumentadas en 47 clases, todas en verde en el commit `b033dca` (secciones 2 y 3).
- Quedan sin marcar la tarea 5.4 (escanear el QR con otro teléfono, del usuario) y, por tanto, el cierre del cambio. Lo que no se verificó está en la sección 5, sin suavizar.
- La batería se ejecutó con la clave `share.url` ausente de `local.properties`: el camino con el enlace real solo lo cubren las pruebas que reciben un enlace ficticio.
- Estado de `tasks.md`: ver la sección 8.

## 2. Comandos y resultados vigentes

Commit probado: `b033dca` (árbol de trabajo limpio), 2026-10-11, emulador de referencia con API 35, una sola pasada. Evidencia: [qa/instrumented/README.md](qa/instrumented/README.md) y [qa/instrumented/gradle.txt](qa/instrumented/gradle.txt).

**Build, pruebas JVM y lint**

```
./gradlew testDebugUnitTest lintDebug assembleDebug --rerun-tasks
```

Resultado: 400 pruebas JVM, 0 fallos, 0 errores, 0 omitidas. Lint con 0 errores y 11 advertencias, las mismas de antes (`UnusedResources` 7, `UseKtx` 2, `RedundantLabel` 1, `ObsoleteSdkInt` 1). En dos `assembleDebug` seguidos, el segundo reutilizó la entrada de la caché de configuración.

**Batería instrumentada completa**

```
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s <emulador> shell am instrument -w -e class com.edu.unal.tictactoe.<Clase> com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado: 47 clases y 325 métodos `@Test`, una instrumentación por clase, sin exclusiones ni repeticiones; todos ejecutados y en `OK`. Las salidas por clase están en [qa/instrumented/](qa/instrumented/). Las clases que cubren este cambio: [ShareAppDialogTest](qa/instrumented/ShareAppDialogTest.txt) (7), [OnlineLobbyScreenTest](qa/instrumented/OnlineLobbyScreenTest.txt) (25), [OnlineLobbyNavigationTest](qa/instrumented/OnlineLobbyNavigationTest.txt) (20) y [OnlineLobbyRotationTest](qa/instrumented/OnlineLobbyRotationTest.txt) (3).

**Validación del cambio**

```
openspec validate "add-share-app-qr" --strict
```

Resultado: `Change 'add-share-app-qr' is valid`.

## 3. Evidencia por tarea

| Tarea | Evidencia |
|---|---|
| 1.1 Dependencia | Build y PJ en verde; `QrCodeTest` usa la librería. ZXing `core` 3.5.4. |
| 1.2 Lectura de `share.url` | Con la clave ausente: PJ pasa y `BuildConfig.SHARE_URL` queda vacío; `local.properties` ignorado por `.gitignore`. El resto, en la sección 5. |
| 1.3 Enlace hasta la navegación | `OnlineLobbyNavigationTest.withoutALinkTheLobbyOffersNoShareAction`. |
| 2.1 y 2.2 Codificador | `QrCodeTest` (10 pruebas JVM): `theMatrixIsSquareAndBiggerThanTheMinimumVersion`, `theQuietZoneIsLightOnAllFourSides`, `theFirstModuleInsideTheQuietZoneIsDarkBecauseOfTheFinderPattern`, `theRoundTripReturnsExactlyTheLink`, `aLongLinkWithParametersRoundTripsUnchanged`, `aLinkWithNonAsciiCharactersRoundTripsUnchanged`, `theSameLinkAlwaysGivesTheSameMatrix`, `anEmptyLinkGivesNoMatrix`, `aLinkThatDoesNotFitAnyQrGivesNoMatrix`, `aNonEmptyLinkGivesAMatrix`. |
| 2.3 y 3.2 Diálogo y QR | `ShareAppDialogTest`: `showsTheTitleTheQrTheTextAndTheCloseAction`, `theQrExposesItsDescriptionToAssistiveTechnologies`, `theQrIsSquareAndAtLeast200DpWhenThereIsRoom`, `closeIsEnabledMeasuresAtLeast48DpAndInvokesOnlyItsCallback`, `theQrAndCloseStayReachableWithALargeFont`, `theQrDrawnOnScreenDecodesToExactlyTheLink`, `theQrIsBlackOnWhiteWithALightQuietZoneInLightAndDarkThemes`. |
| 3.1 Cadenas | `showsTheTitleTheQrTheTextAndTheCloseAction`; lint sin errores. |
| 3.3 Acción en el lobby | `OnlineLobbyScreenTest`: `shareAppSitsBetweenChangeNicknameAndBackToStartOfflineAndConnected`, `shareAppIsAlwaysEnabledMeasuresAtLeast48DpAndInvokesOnlyItsCallback`, `shareAppIsAbsentWhileConnectingAndAfterTheIdentityFailure`, `withoutALinkThereIsNoShareAppAndTheRestOfTheLobbyIsUnchanged`, `shareAppStaysReachableInPortraitAndLandscapeWithLargeFont`. |
| 3.4 Navegación y rotación | `OnlineLobbyNavigationTest`: `shareAppOpensTheDialogAndCloseLeavesTheLobbyAsItWas`, `backWithTheShareDialogOpenClosesOnlyTheDialog`, `withoutALinkTheLobbyOffersNoShareAction`, `theShareActionAppearsNowhereOutsideTheLobby`; `OnlineLobbyRotationTest.rotatingWithTheShareDialogOpenKeepsItOpenAndTheQrReachable`. |
| 4.1 Documentación | Lectura de `firebase/README.md`: clave `share.url`, enlace de invitación, caducidad de 150 días, sin clave no hay acción; solo el enlace de ejemplo. |
| 5.1 Validación | Sección 2. |
| 5.2 Batería | Sección 2. |

## 4. Cobertura de la especificación

Requisitos y escenarios de `specs/share-app/spec.md` contra la evidencia:

- **Acción en el lobby.** Posición de la acción y Presente sin conexión: `shareAppSitsBetweenChangeNicknameAndBackToStartOfflineAndConnected`. Ausente en otras pantallas: `theShareActionAppearsNowhereOutsideTheLobby` cubre la pantalla inicial y la pantalla de espera; **la partida local y la partida en línea no tienen prueba** (ver sección 5). Las demás acciones no cambian: `withoutALinkThereIsNoShareAppAndTheRestOfTheLobbyIsUnchanged` y las pruebas existentes del lobby, que siguen en verde.
- **Sin enlace configurado.** Compilación sin enlace: `withoutALinkTheLobbyOffersNoShareAction` y `withoutALinkThereIsNoShareAppAndTheRestOfTheLobbyIsUnchanged`. Valor vacío: la prueba usa el enlace vacío; **el de solo espacios y el build con valor vacío no tienen evidencia en el repositorio**.
- **Diálogo.** Abrir y Cerrar con la acción: `shareAppOpensTheDialogAndCloseLeavesTheLobbyAsItWas`. Cerrar con Back: `backWithTheShareDialogOpenClosesOnlyTheDialog`. Rotación: `OnlineLobbyRotationTest.rotatingWithTheShareDialogOpenKeepsItOpenAndTheQrReachable` (comprueba que el diálogo sigue abierto, con el QR y «Cerrar» visibles, tras rotar a horizontal y volver a vertical, y que un solo «Cerrar» lo cierra y deja el lobby; no comprueba el título ni el texto tras rotar, ni que el diálogo no se duplique más allá de que «Cerrar» lo cierre). El diálogo no altera el lobby: `shareAppOpensTheDialogAndCloseLeavesTheLobbyAsItWas`. **Cerrar tocando fuera: sin prueba propia.**
- **Contenido y dibujo del QR.** El QR codifica el enlace: `theRoundTripReturnsExactlyTheLink` y `theQrDrawnOnScreenDecodesToExactlyTheLink`. Colores fijos: `theQrIsBlackOnWhiteWithALightQuietZoneInLightAndDarkThemes`. Tamaño mínimo: `theQrIsSquareAndAtLeast200DpWhenThereIsRoom`. Landscape y fuente grande, por dos pruebas distintas: `OnlineLobbyRotationTest.rotatingWithTheShareDialogOpenKeepsItOpenAndTheQrReachable` abre el diálogo, rota a horizontal y vuelve a vertical, y en cada orientación (`assertShareDialogShown()`) comprueba que el diálogo sigue presente, que el QR es visible tras desplazarse hasta él, que mide al menos 200 dp de ancho, que es cuadrado (diferencia de lados menor que 1 dp) y que «Cerrar» está visible; `ShareAppDialogTest.theQrAndCloseStayReachableWithALargeFont` abre el diálogo en una ventana de 320 x 480 con la fuente al doble y comprueba que el QR y el texto «Escanea para instalar el Triqui» son visibles tras desplazarse y que «Cerrar» está visible. Ninguna prueba combina horizontal y fuente grande a la vez para el diálogo; `shareAppStaysReachableInPortraitAndLandscapeWithLargeFont` cubre esa combinación solo para la acción del lobby, no para el diálogo. Descripción para accesibilidad: `theQrExposesItsDescriptionToAssistiveTechnologies` y `closeIsEnabledMeasuresAtLeast48DpAndInvokesOnlyItsCallback`.
- **Configuración fuera del repositorio.** Compilar sin la clave: batería completa con la clave ausente. Caché de configuración: segundo `assembleDebug` reutilizó la entrada, solo con la clave ausente. El valor no se versiona: `local.properties` ignorado. Compilar con la clave: **sin evidencia en el repositorio**.

## 5. No verificado

Sin suavizar:

- **Escaneo con una cámara real** (tarea 5.4, pendiente del usuario). Solo se comprobó que el QR dibujado en pantalla se decodifica al enlace exacto, con el decodificador de ZXing.
- **Cierre del diálogo tocando fuera:** sin prueba propia; comparte el camino `onDismissRequest` con Back, que sí la tiene.
- **Las pruebas nuevas se vieron fallar por compilación, no por aserción:** no hay evidencia de que cada aserción detecte su defecto.
- **Build con la clave presente o vacía y la invalidación de la caché al cambiar el valor:** los comprobó el implementador en una copia del proyecto, sin evidencia en el repositorio.
- **Énfasis visual de la acción** («mismo énfasis que Cambiar apodo»): comprobado leyendo el código (ambos botones contorneados), sin prueba.
- **Valor de solo espacios:** sin prueba propia; lo decide `isBlank()` en la navegación y el recorte en el build.
- **Máximo de 280 dp del QR:** sin prueba.
- **Ausencia de la acción en la partida local y en la partida en línea:** sin prueba propia (solo se probaron la pantalla inicial y la de espera); estructuralmente solo el lobby la compone.
- **Diálogo en horizontal con fuente grande a la vez:** ninguna prueba combina las dos condiciones; solo se prueban por separado (rotación sin fuente escalada; fuente al doble en vertical, en una ventana pequeña).
- **Contenido del diálogo tras rotar:** la prueba de rotación no comprueba el título «Compartir app», el texto «Escanea para instalar el Triqui» ni la descripción del QR después de rotar, solo la presencia del diálogo, el QR (tamaño y forma) y «Cerrar».
- **Condición de la batería:** se ejecutó con la clave `share.url` ausente, así que el camino con el enlace real solo lo cubren las pruebas con un enlace ficticio. Un APK compilado con el enlace real no forma parte de esta evidencia.

## 6. Decisiones y quién las tomó

**Decididas por el usuario**
- La acción vive en el lobby, «debajo de cambiar apodo en la pantalla de multiplayer».
- El enlace no se versiona y solo está en el APK compilado («en una configuración»).

**Decididas por el orquestador** (el usuario no las objetó ni las decidió de forma expresa)
- Texto «Compartir app», posición y estados; sin enlace la acción no existe.
- Diálogo con «Escanea para instalar el Triqui» y «Cerrar»; colores fijos del QR.
- ZXing `core` (versión 3.5.4 elegida en la implementación) y clave `share.url` de `local.properties`.
- Recomendar el enlace de invitación de App Distribution, no el de una versión concreta.

## 7. Limitaciones conocidas

- El enlace queda extraíble del APK; no es un secreto, solo no se versiona.
- Las versiones subidas a App Distribution caducan a los 150 días: el QR de una app ya instalada deja de servir hasta que se suba otra versión.
- Si se configura el enlace de una versión concreta en lugar del de invitación, el QR no sirve a quien no sea ya tester, y apunta solo a esa versión.

## 8. Ausencia de identificadores y estado final

Búsqueda sobre la carpeta del cambio: ver el resultado literal al final de esta sección. El estado final de `tasks.md`: 15 tareas, 14 marcadas (1.1 a 1.3, 2.1 a 2.3, 3.1 a 3.4, 4.1, 5.1, 5.2 y 5.3); sin marcar solo la 5.4.

Comprobaciones sobre toda la carpeta del cambio, `qa/` incluida:

- Patrones buscados: `firebaseapp`, `firebaseio`, `appdistribution.firebase`, `/Users/`, `emulator-<número>`, puertos `:5xxx`, `Pixel`, `SM-…` y `ro.serialno`. Resultado literal: sin coincidencias.
- Valores de `app/google-services.json` (identificador del proyecto, número del proyecto, identificador de la app y clave de API), comparados sin imprimirlos: `none project_id`, `none mobilesdk_app_id`, `none current_key`, `none project_number`. Se comprobó fuera de `qa/` y, aparte, sobre toda la carpeta.
