# Design

## Context

Ver `proposal.md` para motivación, alcance y decisiones; los requisitos observables viven en `specs/share-app/spec.md`.

Estado actual:

- El lobby (`app/src/main/java/com/edu/unal/tictactoe/ui/online/lobby/OnlineLobbyScreen.kt`) ofrece «Crear juego», «Cambiar apodo» y «Volver al inicio». Las pantallas en línea no tienen menú de opciones, por decisión de diseño del cambio archivado `2026-10-11-add-firebase-online-multiplayer` (A6), que se mantiene: por eso la acción vive en el lobby y no en un menú.
- `local.properties` ya está ignorado por git (`.gitignore`). `app/google-services.json` sí se versiona, por decisión previa del usuario; no tiene relación con este enlace.
- Gradle 9.8.1 con `org.gradle.configuration-cache=true`; AGP 9.4.1; catálogo en `gradle/libs.versions.toml`.
- El usuario subió el APK a Firebase App Distribution y de ahí sale el enlace que se quiere mostrar como QR.

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

- **Decidido por el orquestador:** diálogo «Compartir app» con el QR, «Escanea para instalar el Triqui» y «Cerrar»; se cierra con «Cerrar», Back o tocando fuera; sobrevive a la rotación (estado guardado con el mismo mecanismo con que el lobby conserva sus diálogos, a elegir por la implementación mirando cómo lo hace hoy el diálogo de apodo).
- Back con el diálogo abierto lo cierra: lo cubre «Back descarta primero un menú o diálogo» de `game-mode-selection`; `online-lobby` ya limita «Volver al inicio con Back» al lobby sin diálogo abierto. Por eso no hay delta MODIFIED.
- El diálogo no tiene estado de ViewModel de dominio: es una bandera de interfaz.

### D5. Dibujo del QR

- **Decidido por el orquestador:** el QR codifica exactamente el enlace y se dibuja siempre con módulos oscuros sobre fondo blanco con margen de silencio (zona silenciosa de al menos 4 módulos, que es lo que recomienda la norma del QR), también con tema oscuro, porque los lectores esperan oscuro sobre claro. Cuadrado, de al menos 200 dp de lado cuando hay espacio, sin recorte en horizontal ni con fuente grande (el contenido del diálogo se desplaza o el lado se reduce). Descripción de accesibilidad «Código QR para instalar la app».
- Los colores del QR son constantes propias, no del tema Material. La corrección de errores y la versión del QR las decide el codificador; el nivel de corrección de errores se fija en una constante (se recomienda M) y no es observable salvo por la decodificación de ida y vuelta.
- Se dibuja como mapa de bits o con un `Canvas` a partir de una matriz de módulos; la técnica la elige la implementación. La matriz se calcula fuera de la composición (o se recuerda con `remember(enlace)`) para no recalcularla en cada recomposición.

### D6. Generación con ZXing

- **Decidido por el orquestador:** `com.google.zxing:core` (Apache 2.0), declarada en `gradle/libs.versions.toml` como library y usada con `implementation`. La versión se determina en la tarea 1.1 consultando el repositorio de artefactos; no se fija aquí. Solo se necesita `core`, no `android-core` ni el escáner.
- Alternativa descartada: imagen fija del QR en los recursos; habría que regenerarla a mano con cada enlace nuevo y versionaría el enlace, contra la decisión U2.
- Alternativa descartada: codificador de QR propio; es código delicado (Reed-Solomon, máscaras) que una librería probada ya resuelve.
- La prueba de ida y vuelta de JVM puede decodificar con las clases `core` (`QRCodeReader` sobre un `BinaryBitmap` construido desde la matriz de bits), sin dependencia de pruebas adicional.

### D7. Cómo llega el enlace de `local.properties` al APK sin versionarse

- **Decidido por el usuario:** el enlace no se versiona y solo está en el APK compilado. **Decidido por el orquestador:** clave `share.url` en `local.properties`.
- Requisitos de la solución (la técnica concreta de Gradle la elige la implementación; por ejemplo un campo de `BuildConfig` o un recurso generado):
  1. Se lee `local.properties` con un mecanismo compatible con la caché de configuración de Gradle (por ejemplo un proveedor de propiedades, `providers.fileContents` o equivalente, de modo que el cambio del archivo invalide la caché). No se lee el archivo con `java.io` suelto durante la configuración si eso rompe la reutilización o deja un valor viejo en la caché.
  2. Con la clave ausente, `./gradlew assembleDebug` pasa y el APK no contiene enlace (la acción no aparece). Con la clave definida, pasa y el APK sí.
  3. Cambiar el valor de la clave y recompilar produce un APK con el valor nuevo; la segunda corrida seguida con el mismo valor reutiliza la caché.
  4. El valor nunca se escribe en archivos versionados. Los archivos generados en `app/build/` no se versionan. El valor tampoco se imprime en logs ni en la salida del build.
  5. El valor se trata como cadena opaca: comillas, barras y caracteres especiales no deben romper el código generado (escapado correcto si se usa `buildConfigField`).
- Alternativa descartada: constante en el código o en `strings.xml` versionado; versiona el enlace.
- Alternativa descartada: propiedad de Gradle en `~/.gradle/gradle.properties` o variable de entorno; el usuario pidió una configuración del proyecto y `local.properties` ya es el lugar local de este proyecto.

### D8. Cómo se prueba sin el enlace real

- **Decidido por el orquestador:** la pantalla del lobby y el diálogo reciben el enlace como parámetro (nulo o vacío significa «sin enlace»); `MainActivity` o el punto de composición raíz lo toma de la configuración de compilación. Las pruebas del lobby y del diálogo pasan `https://example.com/invitacion`; las pruebas sin enlace pasan nulo. Ninguna prueba lee `local.properties` ni el enlace real.
- El codificador recibe una cadena y devuelve una matriz de módulos, sin dependencias de Android, para probarse con JVM.
- Pruebas instrumentadas existentes del lobby: se mantienen verdes sin enlace, porque los valores por defecto de los parámetros nuevos hacen que la acción no aparezca.

### D9. Textos y accesibilidad

- Los textos de interfaz van en recursos de cadenas, como los demás. «Código QR para instalar la app» es la `contentDescription` del QR. La acción mide al menos 48 dp y expone etiqueta, como las demás acciones del lobby.

### D10. Enlace recomendado y documentación

- **Decidido por el orquestador:** el enlace recomendado es un enlace de invitación de Firebase App Distribution, no el enlace de una versión concreta. Razón: el enlace de una versión solo lo abren testers que ya tienen acceso y apunta a una única versión, así que el QR de un APK nuevo llevaría a la versión anterior. El enlace de invitación sigue sirviendo cuando se sube un APK nuevo.
- Limitación conocida: las versiones subidas a App Distribution caducan a los 150 días; entonces el QR de una app ya instalada deja de servir hasta que se suba otra versión.
- `firebase/README.md` documenta: la clave `share.url` y su ubicación, qué enlace usar (invitación) y cuál no, la caducidad a los 150 días y que, sin la clave, la acción no aparece. Los ejemplos usan `https://example.com/invitacion`.

## Risks / Trade-offs

- [El enlace queda extraíble del APK (cadenas del `BuildConfig` o de recursos)] → aceptado: el enlace se muestra como QR a cualquiera que abra el diálogo, de modo que no es un secreto; U2 pide no versionarlo, no ocultarlo. Un enlace de invitación puede revocarse o regenerarse desde la consola de Firebase.
- [Las versiones de App Distribution caducan a los 150 días y el QR de una app ya instalada deja de servir] → documentado en `firebase/README.md` (D10); remedio: subir otra versión.
- [Se configura por error el enlace de una versión concreta en lugar del de invitación] → la documentación indica cuál usar; la app no puede distinguirlos. La comprobación manual del usuario (escanear con otro teléfono y llegar a la página de instalación) lo detecta.
- [La lectura de `local.properties` puede romper la caché de configuración o dejar un valor viejo] → requisito de D7 con verificación explícita con y sin la clave, y con el valor cambiado.
- [Un QR en tema oscuro con colores del tema no se lee] → colores fijos y margen de silencio, con prueba instrumentada (D5).
- [Un enlace muy largo produce un QR muy denso que se lee mal en pantallas pequeñas] → los enlaces de invitación son cortos; la comprobación manual lo confirma. No se acota la longitud en código.
- [Dependencia nueva en un proyecto con otras ya pinadas a las últimas versiones] → solo `core`, sin dependencias transitivas relevantes; se comprueba con `./gradlew :app:dependencies`.

## Migration Plan

Orden: (1) dependencia y lectura de la configuración; (2) codificación del QR; (3) diálogo y acción del lobby; (4) documentación; (5) batería completa y verificación; (6) comprobación manual del usuario.

Precondición del usuario para la comprobación manual: tener un enlace de invitación de App Distribution y ponerlo en su `local.properties` (clave `share.url`), sin compartirlo con quien implementa.

Rollback: revertir el cambio (dependencia, lectura de la configuración, acción y diálogo, documentación). No hay datos remotos ni migraciones.

## Open Questions

Ninguna. La técnica concreta de Gradle para D7 se deja a la implementación bajo los requisitos de D7.
