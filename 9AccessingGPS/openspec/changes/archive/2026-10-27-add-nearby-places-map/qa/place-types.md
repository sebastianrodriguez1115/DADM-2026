# Verificación de tipos de lugar (tarea 1.6)

- **Fecha de la consulta:** 2026-10-27
- **URL consultada:** https://developers.google.com/maps/documentation/places/web-service/place-types
- **Fecha de última actualización indicada en la página:** 2026-10-23 UTC (texto «Last updated 2026-10-23 UTC.»)
- **Método:** se descargó la página con `curl` y se convirtió el HTML a texto. Se tomó el bloque que empieza en el encabezado «Table A» y termina en el encabezado «Table B». Cada tipo se asignó al grupo cuyo nombre aparece justo antes de su lista en ese bloque. La página indica que `Table A` contiene los tipos que pueden usarse para refinar Nearby Search y Text Search («Types in Table A can be used to refine Nearby Search and Text Search requests»).

## Resultado por tipo

| Tipo | ¿Está en la Table A? | Grupo de la Table A | Categoría de la app |
|---|---|---|---|
| `hospital` | Sí | Health and Wellness | Hospitales y clínicas |
| `medical_clinic` | Sí (marcado con `*`) | Health and Wellness | Hospitales y clínicas |
| `pharmacy` | Sí | Health and Wellness | Farmacias |
| `restaurant` | Sí | Food and Drink | Restaurantes |
| `tourist_attraction` | Sí | Entertainment and Recreation | Sitios turísticos |
| `museum` | Sí | Culture | Sitios turísticos |
| `art_gallery` | Sí | Culture | Sitios turísticos |
| `park` | Sí | Entertainment and Recreation | Parques |

Todos los tipos de la lista se confirmaron en la Table A. Ninguno quedó sin confirmar.

## Tipos de clínica en la Table A

Son los tipos de la Table A cuyo nombre contiene `clinic`:

- `dental_clinic` (Health and Wellness)
- `medical_clinic` * (Health and Wellness)
- `skin_care_clinic` (Health and Wellness)

La nota de la página indica que los tipos con asterisco se añadieron en la versión del 12 de febrero de 2026 («Types with an asterisk (*) were added as part of the February 12, 2026 release.»).

## Conclusión

Sí existe un tipo de clínica en la Table A, así que la categoría «Hospitales y clínicas» conserva ese nombre. El tipo de clínica que se usaría es `medical_clinic`, junto con `hospital`; `dental_clinic` y `skin_care_clinic` son tipos especializados y no se incluyen.

## Observaciones

- En la sección «About the type tables» de la página, el punto de Nearby Search (`includedTypes`, `excludedTypes`, `includedPrimaryTypes`, `excludedPrimaryTypes`) pertenece a la lista de usos de la Table A. Sin embargo, la misma frase dice «The values in Table B are then used to populate those fields». La página no aclara esta contradicción. La pertenencia de los tipos de esta verificación se basa en el encabezado de la tabla y en la frase de la introducción sobre Table A, no en esa frase.
- No se consultó la página equivalente del Places SDK for Android, porque la página de la Web Service basta para confirmar los ocho tipos.
