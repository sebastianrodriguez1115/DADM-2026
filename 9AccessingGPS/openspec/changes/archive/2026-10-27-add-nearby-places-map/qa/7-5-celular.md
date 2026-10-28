# QA en el celular del usuario (tarea 7.5)

- Dispositivo: Samsung Galaxy S21 Ultra (SM-G998B), Android 15.
- Google Play services: 26.41.31 (`dumpsys package com.google.android.gms`, versionName).
- Instalación: `./gradlew :app:installDebug` desde `9AccessingGPS/`, con el keystore de depuración de este Mac. Fecha: 2026-10-27.
- Privacidad: no se guardaron capturas del celular. Se inspeccionó la pantalla con `uiautomator dump` y capturas temporales fuera del repositorio, ya borradas. Esta tabla no incluye direcciones, coordenadas ni nombres de lugares.
- Orden de la prueba: `pm clear` de la app, primer arranque, concesión de solo ubicación aproximada (C-07 y búsquedas), luego `pm grant` de la ubicación precisa y reapertura.

| ID | Flujo | Resultado esperado | Obtenido (sin datos de ubicación) | Resultado |
|---|---|---|---|---|
| P-01 | Instalación y apertura | La app se instala y abre sin fallos | «Installed on 1 device», `MainActivity` abre; sin entradas de la app en el buffer de crashes | PASA |
| P-02 | Diálogo de permisos (`current-location`) | Diálogo del sistema con Precisa/Aproximada y «While using the app» | Diálogo «Allow Lugares cercanos to access this device's location?» con Precise, Approximate, While using the app, Only this time, Don't allow | PASA |
| C-07 | Solo aproximada (`current-location`, «Solo ubicación aproximada») | La app sigue funcionando, muestra la ubicación y avisa que es aproximada | Con solo `ACCESS_COARSE_LOCATION` concedido (FINE sin conceder): tarjeta «Tu ubicación» con dirección y coordenadas, aviso «Ubicación aproximada: los lugares y las distancias pueden ser menos precisos.», «20 lugares», círculo y pines con nombre | PASA |
| P-03 | Mapa con ubicación real, tarjeta y círculo (`places-map`) | Marcador azul, tarjeta «Tu ubicación» con dirección y 5 decimales, círculo del radio a la vista | Tarjeta con dirección y coordenadas de 5 decimales, círculo completo a la vista, marcador azul, logo de Google visible (caso aproximada y precisa) | PASA |
| P-04 | Categoría Hospitales y clínicas | Pines con nombre y contador | «20 lugares», nombres junto a cada pin (etiquetas superpuestas: riesgo aceptado) | PASA |
| P-05 | Categoría Farmacias | Resultados de la categoría y contador | «20 lugares» | PASA |
| P-06 | Categoría Restaurantes | Resultados y contador; solo el chip seleccionado | «20 lugares», un solo chip seleccionado | PASA |
| P-07 | Categoría Sitios turísticos | Resultados y contador | «6 lugares», un solo chip seleccionado | PASA |
| P-08 | Categoría Parques | Resultados y contador | «20 lugares» | PASA |
| P-09 | Ajustes: abrir y radio por defecto (`search-radius-settings`) | Título «Ajustes», «Radio de búsqueda» y «1 km» | Los tres textos presentes | PASA |
| P-10 | Ajustes: cambiar el radio y volver repite la búsqueda | Búsqueda repetida con el radio nuevo | Con Farmacias: 3 km y volver mantiene el mapa con círculo de 3 km; 0,5 km y volver cambia «20 lugares» a «12 lugares» (formato «0,5 km» con coma) | PASA |
| P-11 | Permiso preciso (`current-location`, «Permiso concedido») | Con ubicación precisa no hay aviso de aproximada | Tras `pm grant` de FINE y reabrir la app (cierre y apertura): sin aviso, tarjeta y 20 lugares. Ver observación O-1 | PASA |

## Observaciones

- O-1 (baja, no es defecto reproducido por una vía de usuario): si se concede la ubicación precisa con `pm grant` mientras la app sigue abierta, el aviso de aproximada persiste aunque se pulse «Actualizar ubicación» (las coordenadas sí se actualizan). Desaparece al cerrar y abrir la app. Por el diálogo del sistema o por los ajustes de Android el sistema reinicia el proceso al cambiar el permiso, así que el usuario normal no debería verlo. No se probó ese camino en el celular porque exige cambiar permisos desde los ajustes del sistema.
- La app no se probó con la ubicación del sistema desactivada ni sin red en el celular: están cubiertas en el emulador (7.3) y está prohibido cambiar ajustes globales del celular.

## Estado final del celular

App instalada, permiso de ubicación precisa concedido, radio 1 km, pantalla del mapa abierta con «Hospitales y clínicas» y «20 lugares».
