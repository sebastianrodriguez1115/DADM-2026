# QA manual del cambio `add-nearby-places-map` (tareas 7.2 y 7.3)

- Dispositivo: AVD `DADM_GraphicsSound_API35` (API 35, Android 15). Commit probado: `dd3b664`.
- Ubicación simulada: `geo fix` en la Universidad Nacional de Bogotá (lat 4,6381, lon -74,0840).
- Alcance: solo el emulador. No se tocó el celular.
- Las capturas están en esta misma carpeta (`qa/`).
- Resultado global tras el re-test del 2026-10-27: D1 corregido y verificado. C-07 queda «No verificado en emulador; se verifica en el celular (7.5)» y no bloquea 7.2.
- Primera pasada (2026-10-27): E-07 falló por D1. Re-test con el árbol de trabajo corregido (reinstalado con `installDebug` desde `9AccessingGPS/`): ver la sección 3.1.

## 1. Defectos

### D1. Con la ubicación del dispositivo desactivada no hay botón «Actualizar ubicación» ni recuperación (severidad media). CORREGIDO Y VERIFICADO el 2026-10-27

Estado: corregido (`LocationPanel.kt` muestra el botón en `Disabled` y `MapViewModel.onAppResumed` relee una vez al volver a primer plano si la ubicación estaba `Disabled` o `Unavailable`). Verificado en el re-test E-07, E-08 y E-09 (sección 3.1). Lo que sigue es el historial del fallo.

- Requisitos afectados:
  - `places-map`, «Actualizar la ubicación desde el mapa»: «El mapa MUST ofrecer un botón «Actualizar ubicación» que vuelve a leer la posición del usuario».
  - `current-location`, «Lectura puntual de la ubicación»: la ubicación se lee al abrir y cada vez que el usuario pulsa «Actualizar ubicación».
- Pasos:
  1. Con el permiso concedido y resultados en el mapa, ejecutar `cmd location set-location-enabled false`.
  2. Pulsar «Actualizar ubicación». Se ve «La ubicación del dispositivo está desactivada.» (correcto, captura `7-3-04-ubicacion-desactivada.png`).
  3. Ejecutar `cmd location set-location-enabled true`.
  4. Mirar la pantalla, enviar la app a segundo plano y volver, y entrar a Ajustes y volver.
- Esperado: el usuario puede volver a leer la ubicación con «Actualizar ubicación», o la app se recupera sola.
- Obtenido: la tarjeta de ubicación no se muestra, así que no hay botón. El mensaje de «desactivada» sigue aunque la ubicación ya esté activa. Solo se sale cerrando la app a la fuerza y abriéndola de nuevo.
- Causa visible en el código: `LocationPanel.kt` calcula `canRefresh = false` para `LocationUiState.Disabled`, y `LocationPermissions.kt` solo relee en `ON_RESUME` cuando el permiso se acaba de conceder.
- Captura: `7-3-05-defecto-desactivada-sin-boton-tras-reactivar.png` (ubicación ya activada y el mensaje sigue, sin botón).
- Reproducible: sí, 2 de 2 veces.

## 2. Casos de 7.2 (flujos de las specs, en el emulador)

| ID | Flujo (spec) | Pasos | Esperado según la spec | Obtenido | Captura | Resultado |
|---|---|---|---|---|---|---|
| C-01 | Permiso, primer arranque (`current-location`, «Permiso concedido») | `pm clear`, abrir la app | Diálogo del sistema de permiso de ubicación con mapa de fondo | Diálogo «Allow Lugares cercanos to access this device's location?» con Precisa y Aproximada | `7-2-01-dialogo-permiso.png` | PASA |
| C-02 | Permiso denegado (`current-location`, «Permiso denegado») | Pulsar «Don't allow» | Mensaje de que necesita la ubicación y botón «Conceder permiso» | Mensaje y botón «Conceder permiso» | `7-2-02-permiso-denegado.png` | PASA |
| C-03 | «Conceder permiso» vuelve a pedir | Pulsar «Conceder permiso» | El sistema vuelve a mostrar su diálogo | Diálogo del sistema de nuevo | `7-2-03-dialogo-segunda-vez.png` | PASA |
| C-04 | Denegación permanente (`current-location`) | Pulsar «Don't allow» por segunda vez | El botón cambia a «Abrir ajustes» | Botón «Abrir ajustes» | `7-2-05-denegacion-permanente.png` | PASA |
| C-05 | «Abrir ajustes» | Pulsar «Abrir ajustes» | Se abren los ajustes de la aplicación en el sistema | Pantalla «App info» de Lugares cercanos | `7-2-06-abrir-ajustes.png` | PASA |
| C-06 | Permiso preciso (`current-location`, «Permiso concedido») | `pm clear`, abrir, «While using the app» con Precisa | La app obtiene la ubicación y centra el mapa en ella | Tarjeta con dirección y coordenadas, círculo, pines y «20 lugares» | `7-2-09-permiso-preciso-resultados.png` | PASA |
| C-07 | Solo aproximada (`current-location`, «Solo ubicación aproximada») | `pm clear`, abrir, elegir Aproximada y «While using the app» | Se muestra la ubicación y el aviso de aproximada | No verificable en el emulador: se concede solo `ACCESS_COARSE_LOCATION`, el proveedor del emulador no entrega ubicación a una app con solo ubicación aproximada y tras unos 30 s sale «No se pudo obtener la ubicación» con el botón de actualizar. No se pudo simular el proveedor de red (el shell no tiene permiso de ubicación simulada) | `7-2-07-dialogo-aproximada-seleccionada.png`, `7-2-08-solo-aproximada.png`, `7-2-08b-aproximada-no-obtenida.png` | NO VERIFICADO en emulador; se verifica en el celular (7.5). No bloquea 7.2 |
| C-08 | Tarjeta «Tu ubicación» (`current-location`, «Coordenadas y dirección disponibles») | Ver la tarjeta tras C-06 | Latitud y longitud con 5 decimales y la dirección | «Avenida Ciudad de Lima #120, Bogotá, Colombia» y «Coordenadas: 4.63810, -74.08400» | `7-2-09-permiso-preciso-resultados.png` | PASA |
| C-09 | Dirección no disponible (`current-location`) | `geo fix` en el Atlántico (0,5; -30) y pulsar actualizar | «Dirección no disponible» y coordenadas visibles | «Tu ubicación · Dirección no disponible» y «Coordenadas: 0.50000, -30.00000» | `7-2-26-direccion-no-disponible.png` | PASA |
| C-10 | Actualizar ubicación (`current-location`, «Actualizar ubicación») | Cambiar `geo fix` y pulsar el botón de actualizar | Relee y actualiza coordenadas, dirección y mapa | Coordenadas, dirección y círculo cambiaron (C-09 y regreso a Bogotá) | `7-2-25-sin-resultados.png`, `7-2-26-direccion-no-disponible.png` | PASA |
| C-11 | Categorías en orden y por defecto (`nearby-places-search`) | Abrir la app | Hospitales y clínicas, Farmacias, Restaurantes, Sitios turísticos, Parques, con la primera seleccionada | Chips en ese orden, la primera seleccionada, fila desplazable | `7-2-09-permiso-preciso-resultados.png`, `7-2-14-sitios-turisticos.png` | PASA |
| C-12 | Búsqueda por categoría y contador (`places-map`, «Contador de resultados») | Elegir cada una de las 5 categorías con 1 km | Resultados de la categoría y contador «<n> lugares» | Hospitales 20, Farmacias 20, Restaurantes 20, Sitios turísticos 20, Parques 20 «lugares» | `7-2-09-...`, `7-2-12-farmacias.png`, `7-2-13-restaurantes.png`, `7-2-14-sitios-turisticos.png`, `7-2-15-parques.png` | PASA |
| C-13 | Una sola categoría seleccionada (`places-map`, «Cambiar de categoría») | Cambiar entre categorías | Solo una queda seleccionada y la búsqueda se repite | Solo una seleccionada en cada captura, con resultados distintos | `7-2-12-farmacias.png`, `7-2-15-parques.png` | PASA |
| C-14 | Nombre visible y recorte (`places-map`, «Nombre visible sin tocar», «Nombre largo cortado») | Ver los pines | Nombre junto al pin; más de 28 caracteres termina en «…» | Etiquetas con nombre; «Hemera Unidad de Infectologí…» y «PetCastle Clínica Veterinari…» cortados a 28 caracteres con «…». Las etiquetas superpuestas son riesgo aceptado (U13) | `7-2-09-...`, `7-2-14-sitios-turisticos.png` | PASA |
| C-15 | Burbuja al tocar (`places-map`, «Detalle al tocar el marcador») | Tocar un pin | Burbuja con nombre completo y «a <distancia>» | «Xiua Ecología Femenina / a 575 m» y «PetCastle Clínica Veterinaria / a 681 m» | `7-2-10-burbuja-pin.png`, `7-2-11-burbuja-nombre-largo.png` | PASA |
| C-16 | Marcador azul, círculo y encuadre (`places-map`, «Marcador y círculo», «Encuadre tras buscar») | Ver el mapa tras buscar con 1, 2 y 3 km | Marcador distinto de los pines, círculo semitransparente y círculo completo a la vista | Marcador azul, círculo semitransparente con el radio y completo en pantalla en todos los radios | `7-2-09-...`, `7-2-19-...`, `7-2-23-radio-2km-busqueda-repetida.png` | PASA |
| C-17 | Logo de Google y lugares de Google ocultos (`places-map`) | Ver el mapa | Logo visible sin tapar y sin iconos de lugares de Google | Logo abajo a la izquierda, sin tapar; no hay iconos de comercios de Google (solo se ven los íconos de transporte y el parque Monserrate en algunas capturas) | `7-2-09-...`, `7-2-19-...` | PASA |
| C-18 | Vista inicial (`places-map`, «Vista inicial sobre Bogotá») | Mapa antes de recibir la ubicación | Plaza de Bolívar con zoom 12 | Mapa de Bogotá a escala de ciudad antes de la ubicación | `7-2-02-permiso-denegado.png`, `7-2-08-solo-aproximada.png` | PASA (a ojo: el zoom exacto no se midió) |
| C-19 | Ajustes: abrir y valor por defecto (`search-radius-settings`) | Pulsar el engranaje | Título «Ajustes», control del radio y «1 km» | Título, «Radio de búsqueda», «1 km», control con pasos | `7-2-16-ajustes-1km.png` | PASA |
| C-20 | Límites y paso, formato es-CO (`search-radius-settings`, «Límites del control») | Deslizar a los extremos y con teclado | Mínimo 0,5 km, máximo 10 km, pasos de 0,5 y coma decimal | «0,5 km», «10 km», «9,5 km», «9 km» | `7-2-17-ajustes-minimo.png`, `7-2-18-ajustes-maximo.png` | PASA |
| C-21 | Persistencia (`search-radius-settings`, «Radio conservado entre aperturas») | Elegir 3 km, forzar el cierre, abrir y entrar a Ajustes | El radio es 3 km | «3 km» en Ajustes y círculo de 3 km | `7-2-19-reapertura-3km-categoria-reiniciada.png`, `7-2-20-ajustes-persistido-3km.png` | PASA |
| C-22 | Categoría no se recuerda (`places-map`) | Con «Parques» elegida, cerrar a la fuerza y abrir | Vuelve «Hospitales y clínicas» | Hospitales y clínicas seleccionada | `7-2-19-reapertura-3km-categoria-reiniciada.png` | PASA |
| C-23 | Radio cambiado repite la búsqueda (`search-radius-settings`) | De 3 km a 2 km en Ajustes y volver | La búsqueda se repite con el radio nuevo | Otros resultados de Farmacias y círculo de 2 km (también 3 km a 0,5 km: 20 a 4 lugares) | `7-2-23-radio-2km-busqueda-repetida.png`, `7-2-18b-tras-volver-05km.png` | PASA |
| C-24 | Radio sin cambios no repite la búsqueda (`search-radius-settings`) | Modo avión, abrir Ajustes, volver sin cambiar | No se repite la búsqueda | Siguen los 20 resultados, sin error de conexión | `7-2-22b-radio-sin-cambios-sin-red.png` | PASA |
| C-25 | Back del sistema en Ajustes (`search-radius-settings`, «Volver desde los ajustes») | Abrir Ajustes y pulsar Back | Se muestra el mapa | Vuelve al mapa | `7-2-21-back-sistema-ajustes.png` | PASA |
| C-26 | Contador en singular (`places-map`, «Contador de un solo resultado») | Farmacias con 0,5 km | «1 lugar» | «1 lugar» | `7-2-24-contador-singular-1-lugar.png` | PASA |
| C-27 | Sin resultados (`places-map`, «Sin resultados») | `geo fix` en zona rural (3,0; -73,5), Farmacias, 0,5 km | «No se encontraron lugares de «Farmacias» a menos de 0,5 km» y sin pines | Texto exacto, sin pines, círculo visible | `7-2-25-sin-resultados.png` | PASA |

## 3. Casos de 7.3 (errores)

| ID | Flujo (spec) | Pasos | Esperado según la spec | Obtenido | Captura | Resultado |
|---|---|---|---|---|---|---|
| E-01 | Sin conexión (`nearby-places-search`, «Sin conexión»; `places-map`, «Error de búsqueda») | Modo avión, cambiar a Farmacias | «Sin conexión a internet» con «Reintentar» y sin pines | Texto y botón, sin pines | `7-3-sin-conexion.png` | PASA |
| E-02 | Sin reintentos automáticos (`nearby-places-search`) | Esperar y desactivar el modo avión sin pulsar nada | No busca de nuevo | Tras 8 s y con red, sigue el error | `7-3-02-sin-red-sin-reintento-automatico-tras-reconectar.png` | PASA |
| E-03 | Reintentar sin red | Pulsar «Reintentar» con modo avión | Repite y vuelve a fallar | Sigue «Sin conexión a internet» | (sin captura propia; ver `7-3-sin-conexion.png`) | PASA |
| E-04 | Reintentar con red (`nearby-places-search`, «Reintentar») | Desactivar el modo avión y pulsar «Reintentar» | Repite la búsqueda con la misma categoría y radio | «20 lugares» de Farmacias | `7-3-03-reintentar-con-red.png` | PASA |
| E-05 | Llave faltante (`places-map`, «Llave faltante») | Copia sin `secrets.properties`, instalar y abrir | «Falta configurar la llave de Google Maps» y sin búsqueda | Texto en la tarjeta; al cambiar de categoría no hay carga ni resultados | `7-3-sin-llave.png` | PASA |
| E-06 | Ubicación desactivada (`current-location`) | `set-location-enabled false` y pulsar actualizar | Mensaje de ubicación desactivada, sin lugares ni coordenadas | «La ubicación del dispositivo está desactivada.», sin tarjeta, sin pines | `7-3-04-ubicacion-desactivada.png` | PASA |
| E-07 | Recuperación tras reactivar la ubicación (`places-map`, «Actualizar la ubicación») | Activar la ubicación de nuevo | Se puede actualizar | No hay botón y el mensaje no se quita (D1) | `7-3-05-defecto-desactivada-sin-boton-tras-reactivar.png` | FALLA en la primera pasada (D1); PASA en el re-test, ver 3.1 |

## 3.1 Re-test del 2026-10-27 (D1 corregido) y regresión corta

Árbol de trabajo corregido, reinstalado desde `9AccessingGPS/` en el AVD `DADM_GraphicsSound_API35`.

| ID | Flujo | Pasos | Esperado | Obtenido | Captura | Resultado |
|---|---|---|---|---|---|---|
| E-07 | D1 con botón (`places-map`, «Actualizar la ubicación desde el mapa») | Permiso y resultados; `set-location-enabled false`; pulsar actualizar | Mensaje de desactivada y botón «Actualizar ubicación» | «La ubicación del dispositivo está desactivada.» con el botón de actualizar a la derecha, sin lugares ni coordenadas | `7-3-06-desactivada-con-boton.png` | PASA |
| E-07b | Recuperación con el botón | `set-location-enabled true`, `geo fix` varias veces y pulsar el botón | Vuelven la tarjeta y los resultados | Tarjeta con dirección y coordenadas, círculo y «20 lugares» | `7-3-07-recuperada-con-boton.png` | PASA |
| E-08 | D1, recuperación automática | Con el mensaje visible, app a segundo plano, activar ubicación, `geo fix`, volver a la app | Se recupera sola | Al volver, tarjeta, círculo y «20 lugares» sin tocar nada | `7-3-08-recuperada-al-volver.png` | PASA |
| R-01 | Regresión: volver de Ajustes sin cambiar el radio no repite la búsqueda (`search-radius-settings`) | Modo avión, abrir Ajustes, volver sin cambios | No hay error de conexión, siguen los resultados | Siguen los «20 lugares» | `7-3-09-regresion-radio-sin-cambios.png` | PASA |
| R-02 | Regresión: segundo plano y volver con ubicación lista no repite la búsqueda | Modo avión, app a segundo plano y volver | Sin error de conexión, siguen los resultados | Siguen tarjeta, pines y «20 lugares» (ícono de avión en la barra) | `7-3-10-regresion-segundo-plano-sin-red.png` | PASA |
| R-03 | Regresión: radio cambiado repite la búsqueda | Modo avión, Ajustes a 3 km, volver | La búsqueda se repite (con avión sale el error) | «Sin conexión a internet» con «Reintentar». Con red, «Reintentar» devuelve resultados | `7-3-11-regresion-radio-cambiado-repite-busqueda.png` | PASA |

Con D1 resuelto, E-07 queda en PASA en el re-test. Estado final del emulador: versión normal, ubicación activada, sin modo avión y radio en 1 km.

## 4. No cubierto

- C-07 (solo aproximada) por limitación del emulador.
- Errores de cuota (`OVER_QUERY_LIMIT`), llave inválida (`REQUEST_DENIED`), otro error del servicio y tiempo de espera agotado: no se pueden provocar sin cambiar la llave o el servicio. Los cubren las pruebas unitarias (`PlacesErrorMapperTest`).
- «Ubicación no obtenida» en la ruta con permiso preciso: solo se vio, indirectamente, en C-07.
- Encuadre antes de que el mapa cargue (carrera): cubierta por la prueba unitaria `CameraBoundsTest`, no reproducible a mano.
- 7.4 (otro emulador) y 7.5 (celular): son de otra tarea.

## 5. Observaciones de usabilidad (no son defectos)

- O-1. El marcador azul de «Tu ubicación» puede quedar tapado por etiquetas de lugares cercanos (p. ej. `7-2-15-parques.png`). Consecuencia del riesgo U13.
- O-2. Las etiquetas de los pines se cortan en el borde derecho de la pantalla (p. ej. «Dr. Diego Camargo Medicina E…» en `7-2-09-permiso-preciso-resultados.png`).
- O-3. Con 3 km o más y la categoría Hospitales y clínicas los 20 resultados quedan agrupados cerca del usuario, por el máximo de 20 y el orden por distancia (`7-2-19-...`). Es lo que piden las specs.
- O-4. Con el radio de 0,5 km muchas categorías devuelven pocos lugares; el círculo llena el ancho de pantalla por el encuadre.
- O-5. El mapa muestra los botones de zoom y la barra de herramientas de Google («Cómo llegar», «Abrir en Google Maps») al tocar un pin (`7-2-10-burbuja-pin.png`). Las specs no los prohíben; el segundo abre otra app.
- O-6. Al tocar un pin el mapa se centra en él y el círculo deja de estar encuadrado hasta la siguiente búsqueda.
- O-7. Con la ubicación aproximada que no se obtiene, la espera hasta el error es de unos 30 s con el indicador «Obteniendo tu ubicación…».
- O-8. La app fuerza el menú de ubicación de Google Play Services («No location access») al apagar la ubicación del dispositivo con la app abierta; es del sistema, no de la app.

## 6. Cómo repetir

1. Compilar e instalar desde `9AccessingGPS/` con `JAVA_HOME` del JBR de Android Studio y la variable de selección de dispositivo fijada al emulador (`./gradlew :app:installDebug`).
2. Enviar `emu geo fix -74.0840 4.6381 2600 8` varias veces con la app en primer plano.
3. Capturar con `adb exec-out screencap -p`.
4. Para 7.3 sin llave: copiar el proyecto sin `secrets.properties`, `build/`, `.gradle/` y `.kotlin/`, compilar e instalar, y reinstalar después la versión normal.

Herramientas instaladas: ninguna. Se usó `adb`, `uiautomator` y `rsync` del sistema. No se delegó trabajo a otros agentes.
