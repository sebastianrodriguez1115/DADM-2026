# 8.5 Teclado, accesibilidad y textos fuera de recursos

Specs: `company-form` (Uso con teclado y orientación), `company-list` (Acceso al detalle y al alta).
Fecha: 2026-10-18. Teléfono físico con Android 15 (API 35), teclado Gboard en pantalla.
Nota del entorno: el teléfono tiene la densidad de pantalla en 600 dpi (tamaño de pantalla grande), por lo que todo se ve ampliado; 48 dp equivalen a 180 px.

## 1. Formulario usable con el teclado abierto

Escenario: «Teclado abierto» (company-form).

| Paso | Obtenido | Evidencia |
|------|----------|-----------|
| Abrir «Agregar empresa», pulsar «Guardar» sin datos (se ven los 3 errores) y tocar el campo «Productos y servicios (opcional)». | El teclado se abre y el campo queda enfocado y visible sobre el teclado. | `8-5-01-teclado-abierto-productos.png` |
| Con el teclado abierto, desplazar el formulario hacia arriba (`input swipe`). | PASA. El formulario se desplaza. Quedan visibles, por encima del teclado, el menú «Clasificación» con el error «Selecciona una clasificación», «Cancelar» y «Guardar». | `8-5-02-teclado-abierto-desplazado.png` |
| Tocar el menú «Clasificación» con el teclado abierto. | PASA. El menú se abre con las tres opciones (el teclado se cierra al abrirlo). | `8-5-03-teclado-abierto-menu-abierto.png` (copia: `8-3-04-menu-clasificacion-abierto.png`) |
| Elegir una opción («Consultoría»). | PASA. El menú se cierra, el campo muestra «Consultoría» y desaparece «Selecciona una clasificación». | `qa-manual-01a-clasificacion-elegida-error-limpio.png` |

Después se completó y guardó un alta con teclado (caso adicional 1) sin que el teclado ocultara campos ni botones.
Orientación: no se probó el giro (fuera de alcance: no se conserva el estado al girar). `AndroidManifest.xml` no declara `screenOrientation` (`grep` sin resultados), de modo que la orientación no está fija.

## 2. Botones solo de ícono con descripción (`uiautomator dump`)

| Pantalla | Elemento | `content-desc` leído | Tamaño del objetivo táctil | Resultado |
|----------|----------|----------------------|----------------------------|-----------|
| Listado | botón «+» | «Agregar empresa» | 210 x 210 px (56 dp) | PASA |
| Listado, con texto en la búsqueda | ícono de borrar | «Borrar búsqueda» | 180 x 180 px (48 dp) | PASA |
| Detalle | flecha de volver | «Volver» | 180 x 180 px (48 dp) | PASA |

Observación: el formulario no tiene flecha de volver; usa «Cancelar» y Back, lo que concuerda con la spec.
No hay otros botones solo de ícono: los chips y los botones «Editar», «Eliminar», «Guardar» y «Cancelar» tienen texto.

## 3. Ningún literal de interfaz fuera de `strings.xml`

Comandos, desde `app/src/main/java/com/edu/unal/directorio`:

```
grep -rnE '"[^"]*([áéíóúñÁÉÍÓÚÑ¿¡«»]|[A-Za-z]+ [A-Za-z]+)[^"]*"' ui/     -> sin resultados
grep -rnE 'Text\(\s*"|text = "|title = "' ui/                           -> sin resultados
grep -rn "contentDescription" ui/                                       -> 3 usos, los tres con stringResource(R.string....)
grep -rnE '"[^"]*"' ui/                                                 -> solo etiquetas de prueba (`form_name`, `list_chip_...`, etc.),
                                                                           cadenas vacías como valor inicial y la expresión `onQueryChange("")`
```

PASA: no hay textos en español de la interfaz fuera de recursos. Los literales que quedan son identificadores de prueba (`testTag`) y valores vacíos, que no se muestran.

## Resultado

8.5 PASA en los tres puntos. Sin defectos.
Observación (no es defecto): con el tamaño de pantalla ampliado del teléfono, el tercer chip «Fábrica de software» queda recortado en el borde derecho hasta desplazar la fila de chips hacia la izquierda; la fila sí se desplaza y el chip es accionable.
