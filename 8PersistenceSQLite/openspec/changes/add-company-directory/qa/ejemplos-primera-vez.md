# 8.4 Regla de «no reaparecen» los ejemplos

Spec: `company-storage`, requisito «Datos de ejemplo solo en la primera creación».
Fecha: 2026-10-18. Teléfono físico con Android 15 (API 35), versión debug instalada con `installDebug` (8.3).
Herramienta: `adb` con `uiautomator dump` para leer la pantalla y `screencap` para la evidencia.

## Pasos y resultados

| # | Paso | Resultado | Evidencia |
|---|------|-----------|-----------|
| 1 | Partir de la instalación limpia de 8.3 con los 8 ejemplos (más «Nube Azul», creada en el caso adicional 1, 9 empresas). | Listado con 9 empresas. | n/a |
| 2 | Eliminar las 9 empresas desde la interfaz: abrir el detalle, «Eliminar» y confirmar «Eliminar» en el diálogo, 9 veces. Se borraron las 8 de ejemplo y «Nube Azul». | Tras la última eliminación se ve el listado vacío. | `8-4-01-lista-vacia-tras-eliminar-todas.png` |
| 3 | `adb shell am force-stop com.edu.unal.directorio`. `pidof` no devuelve proceso. | Proceso cerrado. | n/a |
| 4 | Abrir de nuevo con `am start -n com.edu.unal.directorio/.MainActivity`. | PASA. La pantalla muestra «Aún no hay empresas registradas» y la pista «Toca + para agregar la primera.». No aparece «Ninguna empresa coincide con la búsqueda» ni ninguno de los 8 ejemplos. Los ejemplos NO reaparecen tras reiniciar el proceso. | `8-4-02-tras-force-stop-sigue-vacia.png` |
| 5 | `adb shell pm clear com.edu.unal.directorio` y abrir de nuevo. | PASA. Vuelven los 8 ejemplos en orden alfabético: Andes Consultores, Bosque Digital, Cumbre Software, Delta Soluciones, Horizonte Ágil, Lumbre Sistemas, Ñandú Tecnología, Zafiro Labs, cada uno con su clasificación (Consultoría: Andes y Horizonte; Desarrollo a la medida: Bosque, Lumbre y Ñandú; Fábrica de software: Cumbre, Delta y Zafiro). | `8-4-03-tras-pm-clear-vuelven-8-ejemplos.png` |

## Mensaje exacto verificado (texto leído del árbol de la interfaz)

- «Aún no hay empresas registradas»
- «Toca + para agregar la primera.»

## Resultado

PASA: ambas mitades de la regla se cumplen. Sin defectos.
Nota: se eliminaron 9 empresas, no 8, porque el caso adicional 1 había creado «Nube Azul».
