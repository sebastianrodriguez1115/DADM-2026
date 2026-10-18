# QA manual: casos adicionales (Reto 8, Directorio de empresas)

Fecha: 2026-10-18. App debug instalada en teléfono físico con Android 15 (API 35). Sin credenciales ni red. Comenzó con la base recién instalada (8 ejemplos). Herramienta: `adb` (`input tap`, `input text`, `uiautomator dump`, `screencap`).
Límite de la herramienta: `input text` no escribe tildes ni «ñ»; por eso la unicidad con tildes y «ñ» no se probó aquí (la cubren las pruebas automáticas).
Nota: Gboard puso en mayúscula la inicial de cada palabra en el campo de nombre al escribir.

## Resumen

| ID | Caso | Spec | Resultado |
|----|------|------|-----------|
| QA-1 | Crear eligiendo la clasificación en el menú | company-form «Elegir una clasificación», «El error se limpia al elegir», «Guardar un alta»; company-list «El listado refleja una nueva empresa» | PASA |
| QA-2 | Nombre (y correo) duplicado en otra capitalización | company-form «Nombre duplicado al crear», «Correo duplicado» | PASA |
| QA-3 | Editar y cambiar la clasificación | company-form «Formulario de edición», «Guardar una edición»; company-storage «Modificar conserva la identidad» | PASA |
| QA-4 | Filtrar con dos chips | company-list «Varios chips son una unión» | PASA |
| QA-5 | Back en el diálogo de eliminación | company-detail «Back cierra el diálogo sin eliminar» | PASA |
| QA-6 | Cancelar (y Back) en una edición | company-form «Cancelar descarta» | PASA |
| QA-7 | Filtros conservados al ir al detalle y volver | company-list «Volver desde el detalle» | PASA |
| QA-8 | Back en el alta con datos | company-form «Back en el alta» | PASA |
| QA-9 | Cancelar en el diálogo de eliminación | company-detail «Cancelar la eliminación» | PASA |

Defectos encontrados: ninguno.

## QA-1 Crear una empresa eligiendo su clasificación

Pasos:
1. En el listado pulsar «Agregar empresa». Ver «Nueva empresa» con campos vacíos.
2. Pulsar «Guardar» sin datos: aparecen los tres errores.
3. Escribir nombre «Nube Azul»: el error del nombre desaparece.
4. Escribir correo «contacto@nubeazul.example.com», sitio «nubeazul.example.com», teléfono «601 555 0199» y productos en dos líneas («Computacion en nube» y «Migraciones»).
5. Abrir el menú «Clasificación», elegir «Consultoría»: el error «Selecciona una clasificación» desaparece y el campo muestra «Consultoría». Abrir de nuevo y cambiar a «Fábrica de software».
6. Pulsar «Guardar».

Esperado: el menú se cierra y muestra la opción, el error se limpia al elegir, se vuelve al listado y «Nube Azul» aparece con «Fábrica de software» en su posición alfabética.
Obtenido: tal como se esperaba. «Nube Azul» quedó entre «Ñandú Tecnología» y «Zafiro Labs» con «Fábrica de software».
Evidencia: `qa-manual-00-formulario-alta-vacio.png`, `qa-manual-01a-clasificacion-elegida-error-limpio.png`, `qa-manual-01b-formulario-lleno-antes-de-guardar.png`, `qa-manual-01c-listado-con-empresa-nueva.png`.

## QA-2 Nombre duplicado en otra capitalización

Pasos: alta nueva con nombre « NUBE azul » (con espacios al inicio y al final; el teclado lo dejó como « NUBE Azul »), correo «CONTACTO@NubeAzul.example.com» (el de «Nube Azul» con otras mayúsculas), clasificación «Consultoría»; «Guardar».
Esperado: bajo el nombre «Ya existe una empresa con ese nombre», bajo el correo «Ya existe una empresa con ese correo electrónico», sin guardar.
Obtenido: ambos mensajes aparecen cada uno bajo su campo y a la vez; el formulario conserva lo escrito y la lista sigue con una sola «Nube Azul» (se comprobó en QA-8).
Evidencia: `qa-manual-02-nombre-y-correo-duplicados.png`.

## QA-3 Editar y cambiar la clasificación

Pasos: abrir el detalle de «Nube Azul» (muestra «Fábrica de software»), pulsar «Editar», ver «Editar empresa» con los valores actuales y el menú en «Fábrica de software»; abrir el menú, elegir «Desarrollo a la medida», «Guardar».
Esperado: vuelve al detalle de esa empresa con «Desarrollo a la medida»; el listado la muestra con la clasificación nueva y no se crea una segunda empresa.
Obtenido: tal como se esperaba. El detalle muestra «Desarrollo a la medida» y el listado muestra una sola «Nube Azul» con esa clasificación.
Evidencia: `qa-manual-03a-detalle-nube-azul-original.png`, `qa-manual-03b-edicion-clasificacion-cambiada.png`, `qa-manual-03c-detalle-actualizado.png`, `qa-manual-03d-listado-clasificacion-nueva.png`.

## QA-4 Filtrar con dos chips

Precondición: 9 empresas (8 ejemplos más «Nube Azul» con «Desarrollo a la medida»).
Pasos: marcar «Consultoría» y «Desarrollo a la medida», sin texto de búsqueda.
Esperado: exactamente las 2 de Consultoría y las 4 de Desarrollo a la medida, y ninguna de Fábrica de software.
Obtenido: salen Andes Consultores, Bosque Digital, Horizonte Ágil, Lumbre Sistemas, Ñandú Tecnología y Nube Azul (6). No salen Cumbre, Delta ni Zafiro. Al desmarcar los chips vuelven las 9.
Evidencia: `qa-manual-04-filtro-dos-chips.png`. Con «Consultoría» y «Fábrica de software» más la búsqueda «e» se obtuvo la unión esperada en `8-3-02-filtro-combinado.png`; sin búsqueda, esa pareja devolvió las 5 empresas esperadas (Andes, Cumbre, Delta, Horizonte, Zafiro).

## QA-5 Back en el diálogo de eliminación

Pasos: detalle de «Andes Consultores», «Eliminar» (aparece el diálogo), Back del sistema.
Esperado: el diálogo se cierra, sigue el detalle y la empresa no se elimina.
Obtenido: el diálogo se cerró, se siguió viendo el detalle de «Andes Consultores» y, al volver, la empresa seguía en el listado.
Evidencia: `8-3-06-dialogo-eliminar.png` (diálogo abierto) y `qa-manual-05-back-en-dialogo-detalle-sigue.png` (después de Back).

## QA-6 Cancelar una edición conserva los datos

Pasos: detalle de «Nube Azul», «Editar», añadir «XYZ» al nombre (queda «Nube AzulXYZ»), «Cancelar». Después, con otra empresa («Andes Consultores»), «Editar», añadir «QQQ» al nombre, ocultar el teclado y Back.
Esperado: se vuelve al detalle sin preguntar, con el nombre original.
Obtenido: ambos casos volvieron al detalle sin ningún aviso y con el nombre original («Nube Azul» y «Andes Consultores»).
Evidencia: `qa-manual-06a-edicion-nombre-modificado.png`, `qa-manual-06b-tras-cancelar-detalle-intacto.png`, `qa-manual-06c-back-en-edicion-detalle-intacto.png`.
Observación: con el teclado abierto, el primer Back solo oculta el teclado; el segundo vuelve (comportamiento normal de Android).

## QA-7 Filtros conservados al volver del detalle

Pasos: búsqueda «e» y chips «Consultoría» y «Fábrica de software»; tocar «Andes Consultores»; Back.
Esperado: el campo conserva «e», los chips siguen marcados y la lista es la misma.
Obtenido: tal como se esperaba (4 empresas: Andes, Cumbre, Delta, Horizonte).
Evidencia: `qa-manual-10-filtros-conservados-tras-volver.png`.

## QA-8 Back en el alta con datos escritos

Pasos: alta con nombre, correo y clasificación escritos (los de QA-2); Back.
Esperado: se muestra el listado y no se crea nada.
Obtenido: listado visible y una sola «Nube Azul».
Evidencia: lectura del árbol de la interfaz después de Back (una sola «Nube Azul»); `qa-manual-01c-listado-con-empresa-nueva.png` muestra el estado previo.

## QA-9 Cancelar en el diálogo de eliminación

Pasos: detalle de «Andes Consultores», «Eliminar», «Cancelar».
Esperado: el diálogo se cierra, se sigue viendo el detalle y la empresa conserva sus datos.
Obtenido: tal como se esperaba; al volver al listado «Andes Consultores» seguía presente.
Evidencia: lectura del árbol de la interfaz (sin captura).

## Observaciones adicionales (no son defectos)

- Con el tamaño de pantalla ampliado del teléfono, el tercer chip del filtro queda recortado en el borde derecho; la fila se desplaza horizontalmente.
- No hay flecha de volver en el formulario; usa «Cancelar» y Back, según la spec.
- Casos del enunciado sin probar aquí por límite de `adb input text` (tildes y «ñ») o ya cubiertos por pruebas automáticas: validación de formato de URL, teléfono y correo, límite de 100 caracteres del nombre, doble toque en «Guardar», fallo de almacenamiento. Se verificó a mano el error de campos obligatorios, duplicados y el flujo CRUD completo (alta, lectura, edición, eliminación).
