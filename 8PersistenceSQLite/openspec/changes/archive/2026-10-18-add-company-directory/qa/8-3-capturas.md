# 8.3 QA manual guiado: capturas y su correspondencia con las specs

Fecha: 2026-10-18. Teléfono físico con Android 15 (API 35). Pasos previos: `adb uninstall com.edu.unal.directorio`, `./gradlew :app:installDebug`, `am start`. Las capturas solo muestran la barra de estado (hora, wifi, batería) y no contienen datos del dispositivo.

| Captura | Escenario de la spec | Qué se comprobó | Resultado |
|---------|----------------------|-----------------|-----------|
| `8-3-01-listado-inicial.png` | company-storage «Primera apertura»; company-list «Orden alfabético» y «Cada elemento muestra su clasificación» | 8 ejemplos, orden Andes, Bosque, Cumbre, Delta, Horizonte, Lumbre, Ñandú (después de «L» y antes de «Z», la «ñ» cuenta como «n»), Zafiro; cada uno con su clasificación (Consultoría: Andes y Horizonte; Desarrollo a la medida: Bosque, Lumbre, Ñandú; Fábrica de software: Cumbre, Delta, Zafiro). Título «Directorio de empresas», campo «Buscar por nombre», 3 chips y botón «+». | PASA |
| `8-3-02-filtro-combinado.png` | company-list «Búsqueda más chip» y «Varios chips son una unión» | Búsqueda «e» con los chips «Consultoría» y «Fábrica de software» marcados: salen Andes, Cumbre, Delta y Horizonte; no salen Bosque ni Lumbre (contienen «e» pero son «Desarrollo a la medida»), ni Ñandú ni Zafiro (sin «e»). | PASA |
| `8-3-03-formulario-con-errores.png` | company-form «Varios errores a la vez» y company-form «Ninguna clasificación» | Alta vacía y «Guardar»: a la vez «Escribe el nombre de la empresa», «Escribe el correo electrónico» y «Selecciona una clasificación» bajo el menú. Los campos opcionales no marcan error. | PASA |
| `8-3-04-menu-clasificacion-abierto.png` | company-form «Elegir una clasificación» | Menú abierto con exactamente «Consultoría», «Desarrollo a la medida» y «Fábrica de software»; el campo no admite texto libre. | PASA |
| `8-3-05-detalle-empresa.png` | company-detail «Empresa completa» y «Productos con varias líneas» | «Andes Consultores» con Clasificación (una sola), Correo electrónico, Sitio web, Teléfono y Productos y servicios en dos líneas, botones «Editar» y «Eliminar» y flecha «Volver». | PASA |
| `8-3-06-dialogo-eliminar.png` | company-detail «El mensaje nombra la empresa» | Título «¿Eliminar empresa?», mensaje «Se eliminará «Andes Consultores» de forma permanente.» y botones «Cancelar» y «Eliminar». | PASA |

Adicionales de apoyo: `qa-manual-00-formulario-alta-vacio.png` (alta con «Nueva empresa» y campos vacíos), `qa-manual-11-sin-coincidencias.png` («zzzz» muestra «Ninguna empresa coincide con la búsqueda» y no «Aún no hay empresas registradas»).
