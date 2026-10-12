# Proposal

## Why

El usuario subió el APK a Firebase App Distribution y quiere que otra persona pueda instalar la app escaneando un código QR desde la propia app. Petición literal: «Y podemos poner el QR como una opcion del menu?». Hoy compartir el enlace de instalación se hace fuera de la aplicación.

## What Changes

- Añadir en el lobby en línea la acción «Compartir app», inmediatamente debajo de «Cambiar apodo» y encima de «Volver al inicio», con el mismo énfasis visual que «Cambiar apodo». Aparece en los mismos estados que «Cambiar apodo» (sin conexión y conectado), está siempre habilitada y no aparece en ninguna otra pantalla.
- Al pulsarla se abre un diálogo «Compartir app» con el código QR del enlace de instalación, el texto «Escanea para instalar el Triqui» y la acción «Cerrar». Se cierra también con Back o tocando fuera y sigue abierto al rotar.
- El QR codifica exactamente el enlace configurado, se dibuja siempre con módulos oscuros sobre fondo blanco y con su margen de silencio (también en tema oscuro), es cuadrado y no se recorta. Expone la descripción «Código QR para instalar la app».
- El enlace no se versiona: se lee de la clave `share.url` del archivo `local.properties` del proyecto y se incrusta en el APK al compilar. Si no hay enlace configurado, la acción no existe.
- Generar el QR con la librería ZXing (`com.google.zxing:core`, Apache 2.0), declarada en el catálogo de versiones.
- Documentar en `firebase/README.md` cómo obtener y configurar el enlace y la limitación de caducidad de App Distribution.

## Capabilities

### New Capabilities

- `share-app`: acción «Compartir app» del lobby, diálogo con el código QR, contenido y dibujo del QR, ausencia de la acción sin enlace configurado, y configuración del enlace fuera del repositorio.

### Modified Capabilities

Ninguna. Se revisaron `online-lobby` y `game-mode-selection`: ninguna de sus frases pasa a ser falsa. «Volver al inicio» sigue siendo la última acción del lobby y «Crear juego» la principal; Back con el diálogo «Compartir app» abierto cae en «Back descarta primero un menú o diálogo» de `game-mode-selection` y, al estar el diálogo abierto, no aplica «Volver al inicio con Back». El requisito de accesibilidad del lobby de `online-lobby` (48 dp, etiqueta, rotación) se aplica también a la acción nueva sin cambiar su texto; el nuevo requisito lo repite para ella.

## Impact

- Código: una acción y un diálogo en el lobby (`ui/online/lobby`), un codificador de QR y un componente que lo dibuja, y la propagación del enlace desde la configuración de compilación hasta el lobby como parámetro (ver `design.md`).
- Dependencias: una nueva, `com.google.zxing:core`.
- Configuración: una clave local nueva, `share.url`, en `local.properties` (archivo ignorado por git). Compilar sin ella sigue funcionando.
- Pruebas: pruebas JVM del codificador (con ida y vuelta) y pruebas instrumentadas del lobby y del diálogo con un enlace ficticio.
- Documentación: `firebase/README.md`.
- Especificaciones vigentes: ninguna se modifica.

## Out of Scope

- Compartir por otras vías (menú de compartir del sistema, mensajería), copiar el enlace al portapapeles, mostrar el enlace como texto.
- Publicar la app en una tienda o crear el enlace de invitación (se hace en la consola de Firebase).
- Mostrar la acción en la pantalla inicial, en la partida local, en la partida en línea o en la pantalla de espera.
- Ocultar o cifrar el enlace dentro del APK (ver riesgos en `design.md`).

## Decisions of the User

- **U1**: ubicación en el lobby, «debajo de cambiar apodo en la pantalla de multiplayer».
- **U2**: el enlace no se versiona: «dejémoslo en una configuración y que solo esté en el APK compilado».

## Assumptions of the Orchestrator

Decididas por el orquestador; el usuario no las objetó ni las decidió de forma expresa. Pueden revisarse:

- **O1**: texto «Compartir app»; posición entre «Cambiar apodo» y «Volver al inicio», mismo énfasis que «Cambiar apodo»; mismos estados que «Cambiar apodo»; siempre habilitada porque no necesita el servicio; ausente en las demás pantallas.
- **O2**: sin enlace configurado la acción no existe (ni botón deshabilitado ni texto de relleno).
- **O3**: diálogo «Compartir app» con el QR, «Escanea para instalar el Triqui» y «Cerrar»; se cierra con Back o tocando fuera; sobrevive a la rotación.
- **O4**: el QR codifica exactamente el enlace; módulos oscuros sobre fondo blanco con margen de silencio incluso en tema oscuro; cuadrado, de al menos 200 dp de lado cuando hay espacio, sin recorte en horizontal ni con fuente grande; descripción «Código QR para instalar la app».
- **O5**: clave `share.url` de `local.properties`, incrustada en el APK; el valor nunca se escribe en archivos versionados, pruebas ni documentos; la caché de configuración de Gradle sigue funcionando.
- **O6**: ZXing `com.google.zxing:core`. Alternativas descartadas: imagen fija del QR en recursos y codificador propio (ver `design.md`).
- **O7**: el enlace recomendado es un enlace de invitación de Firebase App Distribution, no el de una versión concreta; las versiones caducan a los 150 días (ver `design.md` y la tarea de documentación).
