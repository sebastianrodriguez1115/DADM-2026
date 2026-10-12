# Firebase para el Triqui en línea

Esta carpeta contiene las reglas de seguridad de Realtime Database y los pasos para dejar el proyecto de Firebase listo. Los nombres de los menús de la consola son los que conocemos hoy y pueden variar.

## Qué hay que saber

- `app/google-services.json` está versionado en git. Solo contiene identificadores del proyecto, no secretos. Va en `app/`, junto a `build.gradle.kts`.
- La seguridad de los datos no depende de ocultar ese archivo, sino de las reglas publicadas (`firebase/database.rules.json`) y de restringir la clave de API en la consola de Google Cloud (Credenciales).

Para entender qué hace cada regla y por qué existe, véase [RULES.md](RULES.md).

## Pasos previos (una sola vez)

1. **Activar el inicio de sesión anónimo.** Consola de Firebase, Compilación (Build), Authentication, pestaña Método de acceso (Sign-in method), Anónimo (Anonymous): activar y guardar.
2. **Publicar las reglas.** Consola de Firebase, Compilación (Build), Realtime Database, pestaña Reglas (Rules): reemplazar todo el contenido por el de `firebase/database.rules.json` y pulsar Publicar. Sin las reglas publicadas la app no puede leer ni escribir: la base se crea bloqueada y las reglas de prueba caducan.
3. Opcional: probar las reglas en el simulador de la misma pestaña (Rules Playground) antes de usar la app.

## Enlace de instalación para «Compartir app»

El lobby en línea puede mostrar «Compartir app», que abre un código QR con el enlace para instalar la app. El enlace se configura en la clave `share.url` de `local.properties` (el archivo de la raíz del proyecto, junto a `sdk.dir`):

```properties
share.url=https://example.com/invitacion
```

- Use un **enlace de invitación** de Firebase App Distribution (Consola de Firebase, App Distribution, Testers y grupos, grupo, «Enlace de invitación»), no el enlace de una versión concreta. El de una versión solo lo abren testers que ya tienen acceso y apunta a una única versión, así que quien escanee el QR no podría instalar nada.
- El valor **no se versiona**: `local.properties` está ignorado por git y el enlace solo queda dentro del APK compilado. No lo copie en archivos del repositorio, issues ni capturas.
- Las versiones subidas a App Distribution **caducan a los 150 días**: pasado ese plazo hay que subir una versión nueva para que el enlace siga sirviendo una instalación.
- Sin la clave, o con ella vacía, la acción «Compartir app» no aparece. Tras cambiar el valor, vuelva a compilar: el build lo lee de nuevo solo.

## Qué se ve en la app si falta un paso

- Falta el inicio de sesión anónimo: «No se pudo conectar con el servicio» en el lobby, con «Reintentar».
- Faltan las reglas publicadas (o están caducadas): la lista de juegos siempre está vacía y al crear aparece «No se pudo crear el juego».
- Sin red: «Sin conexión» y las acciones quedan deshabilitadas hasta que vuelva.

## Borrar a mano un juego residual

Un juego se borra solo cuando su creador o su invitado se desconectan. Si alguno queda (por ejemplo, tras una prueba interrumpida): consola de Firebase, Realtime Database, pestaña Datos (Data), nodo `games`, pasar el cursor sobre el juego, pulsar la X de eliminar y confirmar.

## Verificación con dos instancias

1. Compilar e instalar el APK de depuración en dos destinos (dos emuladores, o un emulador y un dispositivo): `./gradlew :app:assembleDebug` y luego `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk` para cada uno.
2. Abrir «Jugar en línea» en ambos, escribir un apodo distinto en cada uno.
3. En el primero, crear un juego; en el segundo, comprobar que aparece en la lista y unirse. Jugar alternando turnos hasta que haya resultado.
4. Repetir con cancelación, abandono con «Salir» y cierre de la app para comprobar que el juego desaparece de la lista y de la consola.
