# Comprobación manual del QR (tarea 5.4)

Fecha: 2026-10-12.

## Qué se le pidió al usuario

- Escanear con otro teléfono (Android 15 o superior) el código de «Compartir app» mostrado en su teléfono.
- Comprobar que el APK se descarga sin registro, que se instala y que se abre.
- Repetir con el tema oscuro.

## Respuestas del usuario (literales)

- «Funciono, descargo e instalo sin pedir registro.»
- «El QR en el tema oscuro tambien sirvio normal.»

## Comprobación del orquestador (no es del usuario)

El usuario publicó en su repositorio una versión con etiqueta fija y el archivo `triqui.apk`. Desde el computador se comprobó que esa dirección descarga, sin iniciar sesión, un archivo idéntico byte a byte al APK compilado con ese mismo enlace configurado. El enlace no se copia aquí.

## Lo que no quedó comprobado

- El usuario no dijo si la app instalada se abrió.
- No mencionó la orientación horizontal: no se comprobó el escaneo en landscape.
- No se sabe qué aviso mostró Android al instalar (por ejemplo el de «instalar apps desconocidas»).
- Solo hay una prueba, con un teléfono lector; no se probaron otras cámaras ni versiones de Android.
