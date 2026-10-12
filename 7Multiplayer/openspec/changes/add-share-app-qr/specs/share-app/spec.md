## ADDED Requirements

### Requirement: Acción «Compartir app» en el lobby

Cuando la compilación incluye un enlace de instalación, el lobby SHALL mostrar la acción «Compartir app» inmediatamente debajo de «Cambiar apodo» y encima de «Volver al inicio», con el mismo énfasis visual que «Cambiar apodo». La acción SHALL mostrarse en los mismos estados del lobby en que se muestra «Cambiar apodo» (sin conexión y conectado), MUST estar siempre habilitada porque no depende del servicio, y MUST NOT mostrarse en ninguna otra pantalla. La acción MUST medir al menos 48 dp y exponer su etiqueta «Compartir app» a las tecnologías de asistencia como acción ejecutable. «Crear juego» MUST seguir siendo la acción principal del lobby y «Volver al inicio» MUST conservar su comportamiento.

#### Scenario: Posición de la acción

- **GIVEN** que la compilación incluye un enlace y el lobby está conectado
- **WHEN** se revisa la pantalla
- **THEN** «Compartir app» aparece debajo de «Cambiar apodo» y encima de «Volver al inicio», con el mismo énfasis visual que «Cambiar apodo», habilitada y expuesta como acción ejecutable

#### Scenario: Presente sin conexión

- **GIVEN** que la compilación incluye un enlace y el lobby muestra «Sin conexión»
- **WHEN** se revisa la pantalla
- **THEN** «Compartir app» aparece habilitada y «Crear juego» sigue deshabilitado

#### Scenario: Ausente en otras pantallas

- **GIVEN** que la compilación incluye un enlace
- **WHEN** se muestra la pantalla inicial, la partida local, la pantalla de espera de rival o la partida en línea
- **THEN** no aparece «Compartir app»

#### Scenario: Las demás acciones del lobby no cambian

- **GIVEN** que la compilación incluye un enlace y el lobby está conectado
- **WHEN** la persona usa «Crear juego» o «Volver al inicio»
- **THEN** cada una conserva el comportamiento definido en `online-lobby` y `game-mode-selection`

### Requirement: Sin enlace configurado no hay acción

Si la compilación no incluye un enlace de instalación (la clave no está definida o su valor está vacío), el lobby MUST NOT mostrar «Compartir app», ni como botón deshabilitado ni como texto de relleno, y el resto del lobby SHALL comportarse como lo definen `online-lobby` y `game-mode-selection`.

#### Scenario: Compilación sin enlace

- **GIVEN** que la compilación no incluye un enlace de instalación
- **WHEN** se muestra el lobby, conectado o sin conexión
- **THEN** no aparece «Compartir app» ni ningún elemento que lo sustituya, y «Cambiar apodo», «Crear juego» y «Volver al inicio» se muestran y funcionan igual que antes

#### Scenario: Valor vacío

- **GIVEN** que la configuración define la clave con un valor vacío o solo espacios
- **WHEN** se compila y se muestra el lobby
- **THEN** no aparece «Compartir app»

### Requirement: Diálogo con el código QR

Al pulsar «Compartir app», la aplicación SHALL abrir un diálogo con el título «Compartir app», el código QR del enlace de instalación, el texto «Escanea para instalar el Triqui» y la acción «Cerrar». El diálogo SHALL cerrarse con «Cerrar», con Back o tocando fuera de él, y al cerrarse SHALL mostrarse el lobby en el mismo estado en que estaba. El diálogo SHALL seguir abierto, con el mismo contenido, al rotar el dispositivo. Mientras el diálogo está abierto, Back MUST cerrarlo sin volver a la pantalla inicial.

#### Scenario: Abrir el diálogo

- **GIVEN** que el lobby muestra «Compartir app»
- **WHEN** la persona la pulsa
- **THEN** aparece un diálogo con el título «Compartir app», un código QR, el texto «Escanea para instalar el Triqui» y la acción «Cerrar»

#### Scenario: Cerrar con la acción

- **GIVEN** que el diálogo está abierto
- **WHEN** la persona pulsa «Cerrar»
- **THEN** el diálogo se cierra y se muestra el lobby

#### Scenario: Cerrar con Back

- **GIVEN** que el diálogo está abierto
- **WHEN** se pulsa Back
- **THEN** el diálogo se cierra, se sigue mostrando el lobby y no se vuelve a la pantalla inicial

#### Scenario: Cerrar tocando fuera

- **GIVEN** que el diálogo está abierto
- **WHEN** la persona toca fuera del diálogo
- **THEN** el diálogo se cierra y se muestra el lobby

#### Scenario: Rotación con el diálogo abierto

- **GIVEN** que el diálogo está abierto
- **WHEN** el dispositivo rota
- **THEN** el diálogo sigue abierto con el mismo contenido, sin duplicarse

#### Scenario: El diálogo no altera el lobby

- **GIVEN** que el lobby muestra la lista de juegos y el diálogo está abierto
- **WHEN** se cierra el diálogo
- **THEN** el lobby conserva su estado, su lista y su apodo

### Requirement: Contenido y dibujo del código QR

El código QR SHALL codificar exactamente el enlace de instalación configurado, sin añadir ni quitar caracteres. Se SHALL dibujar siempre con módulos oscuros sobre fondo blanco y con su margen de silencio, también con el tema oscuro del sistema activo. El QR SHALL ser cuadrado y medir al menos 200 dp de lado cuando el espacio lo permita, y MUST NOT recortarse en landscape ni con fuentes escaladas (con desplazamiento o reducción del lado si hace falta). El QR SHALL exponer a las tecnologías de asistencia la descripción «Código QR para instalar la app».

#### Scenario: El QR codifica el enlace

- **GIVEN** que el enlace configurado es `https://example.com/invitacion`
- **WHEN** se genera el QR y se decodifica con un lector de QR
- **THEN** el resultado es exactamente `https://example.com/invitacion`

#### Scenario: Colores fijos con tema oscuro

- **GIVEN** que el tema oscuro del sistema está activo
- **WHEN** se muestra el diálogo
- **THEN** el QR se dibuja con módulos oscuros sobre un fondo blanco con margen de silencio alrededor

#### Scenario: Tamaño mínimo

- **GIVEN** que el dispositivo está en portrait con espacio suficiente
- **WHEN** se muestra el diálogo
- **THEN** el QR es cuadrado y mide al menos 200 dp de lado

#### Scenario: Landscape y fuente grande

- **GIVEN** que el dispositivo está en landscape o la fuente está escalada
- **WHEN** se muestra el diálogo
- **THEN** el QR sigue siendo cuadrado y completo, sin recortes, y «Cerrar» es alcanzable

#### Scenario: Descripción para accesibilidad

- **GIVEN** que el diálogo está abierto
- **WHEN** se recorre con una tecnología de asistencia
- **THEN** el QR expone la descripción «Código QR para instalar la app» y «Cerrar» mide al menos 48 dp

### Requirement: El enlace se configura fuera del repositorio

El enlace de instalación SHALL leerse de la clave `share.url` del archivo `local.properties` del proyecto, que no se versiona, y SHALL incrustarse en el APK al compilar. El valor MUST NOT escribirse en ningún archivo versionado, prueba ni documento; las pruebas SHALL usar un enlace ficticio. La compilación SHALL tener éxito con y sin la clave definida, y la caché de configuración de Gradle MUST seguir funcionando en ambos casos.

#### Scenario: Compilar con la clave

- **GIVEN** que `local.properties` define `share.url`
- **WHEN** se compila la app y se abre el lobby
- **THEN** aparece «Compartir app» y su QR codifica el valor de la clave

#### Scenario: Compilar sin la clave

- **GIVEN** que `local.properties` no define `share.url`
- **WHEN** se compila la app
- **THEN** la compilación termina con éxito y el lobby no muestra «Compartir app»

#### Scenario: Caché de configuración

- **GIVEN** que la caché de configuración está activa
- **WHEN** se ejecuta la compilación dos veces seguidas, con y sin la clave
- **THEN** la segunda ejecución de cada caso reutiliza la caché de configuración

#### Scenario: El valor no se versiona

- **GIVEN** que `local.properties` define `share.url`
- **WHEN** se revisa lo que Git rastrea o ignora
- **THEN** `local.properties` está ignorado y el valor no aparece en ningún archivo versionado
