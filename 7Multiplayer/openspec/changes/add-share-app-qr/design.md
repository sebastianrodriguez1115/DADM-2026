# Design

## Context

Ver `proposal.md` para motivación, alcance y decisiones; los requisitos observables viven en `specs/share-app/spec.md`.

Estado actual:

- El lobby (`app/src/main/java/com/edu/unal/tictactoe/ui/online/lobby/OnlineLobbyScreen.kt`) ofrece «Crear juego», «Cambiar apodo» y «Volver al inicio». Las pantallas en línea no tienen menú de opciones, por decisión de diseño del cambio archivado `2026-10-11-add-firebase-online-multiplayer` (A6), que se mantiene: por eso la acción vive en el lobby y no en un menú.
- `local.properties` ya está ignorado por git (`.gitignore`). `app/google-services.json` sí se versiona, por decisión previa del usuario; no tiene relación con este enlace.
- Gradle 9.8.1 con `org.gradle.configuration-cache=true`; AGP 9.4.1; catálogo en `gradle/libs.versions.toml`.
- El usuario subió primero el APK a Firebase App Distribution; el enlace que se muestra como QR puede venir de ahí o de otra vía (ver D10).

## Goals / Non-Goals

**Goals:** mostrar desde el lobby un QR escaneable con el enlace de instalación, sin versionar el enlace; que las pantallas se prueben sin el enlace real; que el build funcione con y sin la clave y con la caché de configuración.

**Non-Goals:** compartir por otras vías, copiar el enlace, publicar en una tienda, proteger el enlace frente a quien extraiga el APK, cambiar las pantallas en línea existentes.

## Decisions

### D1. La acción vive en el lobby, no en un menú

- **Decidido por el usuario:** «debajo de cambiar apodo en la pantalla de multiplayer». Las pantallas en línea siguen sin menú de opciones (A6 del cambio archivado).
- Alternativa descartada: añadir un menú de opciones al lobby; contradice A6 y el usuario ya señaló el lugar.

### D2. Textos, posición y estados de la acción

- **Decidido por el orquestador:** texto «Compartir app»; inmediatamente debajo de «Cambiar apodo» y encima de «Volver al inicio»; mismo énfasis visual que «Cambiar apodo»; mismos estados que «Cambiar apodo» (sin conexión y conectado); siempre habilitada, porque generar y mostrar el QR no usa el servicio; ausente en las demás pantallas.
- Alternativa descartada: deshabilitarla sin conexión; no hay motivo, el QR es local y puede mostrarse a quien está al lado aunque el servicio falle.

### D3. Sin enlace, la acción no existe

- **Decidido por el orquestador:** si no hay enlace configurado, el lobby no compone la acción: ni botón deshabilitado ni texto de relleno. Alternativa descartada: botón deshabilitado o mensaje «No hay enlace configurado»; mostraría a quien instala un detalle de compilación que no puede resolver.
- «Sin enlace» incluye clave ausente, valor vacío y valor de solo espacios.

### D4. Diálogo con el QR

- **Decidido por el orquestador:** diálogo «Compartir app» con el QR, «Escanea para instalar el Triqui» y «Cerrar»; se cierra con «Cerrar», Back o tocando fuera; sobrevive a la rotación (bandera con `rememberSaveable` en la navegación).
- Back con el diálogo abierto lo cierra: lo cubre «Back descarta primero un menú o diálogo» de `game-mode-selection`; `online-lobby` ya limita «Volver al inicio con Back» al lobby sin diálogo abierto. Por eso no hay delta MODIFIED.
- El diálogo no tiene estado de ViewModel de dominio: es una bandera de interfaz.

### D5. Dibujo del QR

- **Decidido por el orquestador:** el QR codifica exactamente el enlace y se dibuja siempre con módulos oscuros sobre fondo blanco con margen de silencio (zona silenciosa de al menos 4 módulos, que es lo que recomienda la norma del QR), también con tema oscuro, porque los lectores esperan oscuro sobre claro. Cuadrado, de al menos 200 dp de lado cuando hay espacio y de hasta 280 dp como máximo, sin recorte en horizontal ni con fuente grande (el contenido del diálogo se desplaza o el lado se reduce). Descripción de accesibilidad «Código QR para instalar la app».
- Los colores del QR son constantes propias, no del tema Material. La corrección de errores y la versión del QR las decide el codificador; el nivel de corrección de errores se fija en una constante (se recomienda M) y no es observable salvo por la decodificación de ida y vuelta.
- Implementado: `encodeQr` (función pura, `ui/share/QrCode.kt`) usa `QRCodeWriter` con UTF-8, corrección M y margen de 4 módulos incluido en la matriz `QrMatrix`; devuelve nulo si el texto está vacío o no cabe. El diálogo dibuja la matriz con un `Canvas`, negro sobre blanco fijos, y la recuerda con `remember(enlace)`. Si el enlace no se puede codificar, el diálogo omite el QR.

### D6. Generación con ZXing

- **Decidido por el orquestador:** `com.google.zxing:core` (Apache 2.0), declarada en `gradle/libs.versions.toml` como library y usada con `implementation`. Versión elegida: 3.5.4, la última estable según los metadatos de Maven Central (`zxing` en el catálogo). Solo se necesita `core`, no `android-core` ni el escáner.
- Alternativa descartada: imagen fija del QR en los recursos; habría que regenerarla a mano con cada enlace nuevo y versionaría el enlace, contra la decisión U2.
- Alternativa descartada: codificador de QR propio; es código delicado (Reed-Solomon, máscaras) que una librería probada ya resuelve.
- La prueba de ida y vuelta de JVM puede decodificar con las clases `core` (`QRCodeReader` sobre un `BinaryBitmap` construido desde la matriz de bits), sin dependencia de pruebas adicional.

### D7. Cómo llega el enlace de `local.properties` al APK sin versionarse

- **Decidido por el usuario:** el enlace no se versiona y solo está en el APK compilado. **Decidido por el orquestador:** clave `share.url` en `local.properties`.
- Técnica elegida: en `app/build.gradle.kts`, un proveedor de contenido de archivo (`providers.fileContents`) lee `local.properties` como entrada rastreada de la caché de configuración, extrae `share.url`, recorta espacios y usa vacío si falta el archivo o la clave; `androidComponents.onVariants` lo inyecta como campo `SHARE_URL` de `BuildConfig` (con `buildFeatures.buildConfig = true`), escapando comillas y barras. Solo `MainActivity` lee `BuildConfig.SHARE_URL`.
- Requisitos que debe cumplir la solución:
  1. Se lee `local.properties` con un mecanismo compatible con la caché de configuración de Gradle (por ejemplo un proveedor de propiedades, `providers.fileContents` o equivalente, de modo que el cambio del archivo invalide la caché). No se lee el archivo con `java.io` suelto durante la configuración si eso rompe la reutilización o deja un valor viejo en la caché.
  2. Con la clave ausente, `./gradlew assembleDebug` pasa y el APK no contiene enlace (la acción no aparece). Con la clave definida, pasa y el APK sí.
  3. Cambiar el valor de la clave y recompilar produce un APK con el valor nuevo; la segunda corrida seguida con el mismo valor reutiliza la caché.
  4. El valor nunca se escribe en archivos versionados. Los archivos generados en `app/build/` no se versionan. El valor tampoco se imprime en logs ni en la salida del build.
  5. El valor se trata como cadena opaca: comillas, barras y caracteres especiales no deben romper el código generado (escapado correcto si se usa `buildConfigField`).
- Alternativa descartada: constante en el código o en `strings.xml` versionado; versiona el enlace.
- Alternativa descartada: propiedad de Gradle en `~/.gradle/gradle.properties` o variable de entorno; el usuario pidió una configuración del proyecto y `local.properties` ya es el lugar local de este proyecto.

### D8. Cómo se prueba sin el enlace real

- **Decidido por el orquestador, ajustado en la implementación:** `MainActivity` toma el enlace de `BuildConfig.SHARE_URL` y lo pasa a la capa de navegación (`shareLink`; en blanco significa «sin enlace»). El diálogo `ShareAppDialog` recibe el enlace como parámetro. La pantalla del lobby no recibe el enlace sino una acción opcional `onShareApp` (nula significa «sin enlace» y no compone el botón). La navegación guarda la bandera de diálogo abierto con `rememberSaveable` en el destino del lobby y no muestra acción ni diálogo con el enlace en blanco. Las pruebas pasan `https://example.com/invitacion`; las pruebas sin enlace pasan una acción nula o un enlace en blanco. Ninguna prueba lee `local.properties` ni el enlace real.
- El codificador recibe una cadena y devuelve una matriz de módulos, sin dependencias de Android, para probarse con JVM.
- Pruebas instrumentadas existentes del lobby: se mantienen verdes sin enlace, porque el valor por defecto de `onShareApp` es nulo y la acción no aparece.

### D9. Textos y accesibilidad

- Los textos de interfaz van en recursos de cadenas, como los demás. «Código QR para instalar la app» es la `contentDescription` del QR. La acción mide al menos 48 dp y expone etiqueta, como las demás acciones del lobby.

### D10. Vía del enlace de instalación: dos opciones, una elegida

- La app no distingue el tipo de enlace: codifica lo que haya en `share.url`. Se consideraron dos vías:
  1. **Descarga directa del APK** (GitHub Releases): sin registro, pero sin caducidad propia; el APK queda al alcance de cualquiera que tenga el enlace, y quien instala debe permitir «instalar apps desconocidas» y confirmar la instalación, así que no es de un solo toque.
  2. **Enlace de invitación de Firebase App Distribution**: obliga a quien escanea a escribir su correo e inscribirse como tester (la documentación oficial lo define como una URL para que los testers se inscriban), y las versiones caducan a los 150 días; entonces el QR de una app ya instalada deja de servir hasta que se suba otra versión. El enlace de una versión concreta de App Distribution queda descartado en cualquier caso: solo lo abren testers que ya tienen acceso y apunta a una única versión.
- **Decidido por el usuario (2026-10-12):** la vía 1. Primero configuró la invitación de App Distribution, vio que exige registrar un correo y preguntó: «Es obligatorio el paso del correo?, no se puede descargar o instalar el APK en un solo paso?». Respuesta literal: «Vamos con GitHub Releases, llamalo triqui.apk».
- `firebase/README.md` documenta las dos vías con sus limitaciones, la clave `share.url` y su ubicación, y que, sin la clave, la acción no aparece. Los ejemplos usan `https://example.com/invitacion` o marcadores genéricos.

### D11. Dirección de una etiqueta fija, no la de «última versión»

- **Decidido por el orquestador:** el enlace apunta a la descarga del archivo `triqui.apk` adjunto a una versión con una etiqueta fija (forma genérica: `https://github.com/<usuario>/<repositorio>/releases/download/<etiqueta>/triqui.apk`), no a la dirección de «última versión». Razón: el repositorio contiene varios talleres y una versión futura de otro taller dejaría sin efecto la dirección de «última versión». Con etiqueta y nombre de archivo fijos, la dirección no cambia al reemplazar el archivo, y el QR de una app ya instalada sigue sirviendo.
- Consecuencia: el APK debe compilarse después de configurar el enlace, para que su QR apunte al sitio correcto.
- Alternativa descartada: «última versión» (se rompe con versiones de otros talleres).

## Risks / Trade-offs

- [El enlace queda extraíble del APK (cadenas del `BuildConfig` o de recursos)] → aceptado: el enlace se muestra como QR a cualquiera que abra el diálogo, de modo que no es un secreto; U2 pide no versionarlo, no ocultarlo. Con la descarga directa, el APK queda al alcance de cualquiera que tenga el enlace; se acepta, es un taller de clase. El enlace puede cambiarse reemplazando el archivo o la etiqueta.
- [Si se usara la invitación de App Distribution: obliga a registrar un correo y las versiones caducan a los 150 días, tras lo cual el QR de una app ya instalada deja de servir] → razón por la que el usuario eligió la descarga directa (D10); queda documentado en `firebase/README.md`.
- [Fuera de una tienda, quien instala debe permitir «instalar apps desconocidas» y confirmar, así que no es un solo toque] → limitación de la vía elegida, documentada; no depende de la app.
- [El APK se compiló antes de configurar el enlace y su QR apunta al sitio equivocado, o no hay QR] → D11: compilar después de configurar `share.url`; la comprobación manual (5.4) lo detecta.
- [Se configura un enlace que no descarga el APK (por ejemplo la dirección de «última versión» que otro taller deja sin efecto, o el enlace de una versión concreta de App Distribution)] → la documentación indica la forma correcta; la app no puede distinguirlos. La comprobación manual del usuario (escanear con otro teléfono, descargar e instalar) lo detecta.
- [La lectura de `local.properties` puede romper la caché de configuración o dejar un valor viejo] → requisito de D7 con verificación explícita con y sin la clave, y con el valor cambiado.
- [Un QR en tema oscuro con colores del tema no se lee] → colores fijos y margen de silencio, con prueba instrumentada (D5).
- [Un enlace muy largo produce un QR muy denso que se lee mal en pantallas pequeñas] → los enlaces de descarga de GitHub Releases son de longitud moderada; la comprobación manual lo confirma. No se acota la longitud en código.
- [Dependencia nueva en un proyecto con otras ya pinadas a las últimas versiones] → solo `core`, sin dependencias transitivas relevantes; se comprueba con `./gradlew :app:dependencies`.

## Migration Plan

Orden: (1) dependencia y lectura de la configuración; (2) codificación del QR; (3) diálogo y acción del lobby; (4) documentación; (5) batería completa y verificación; (6) comprobación manual del usuario.

Precondición del usuario para la comprobación manual: tener el APK publicado como `triqui.apk` en una versión con etiqueta fija de GitHub Releases, poner su dirección de descarga en su `local.properties` (clave `share.url`) y compilar el APK después.

Rollback: revertir el cambio (dependencia, lectura de la configuración, acción y diálogo, documentación). No hay datos remotos ni migraciones.

## Open Questions

Ninguna. Reserva conocida: el cierre tocando fuera no tiene prueba propia (comparte camino con Back).
