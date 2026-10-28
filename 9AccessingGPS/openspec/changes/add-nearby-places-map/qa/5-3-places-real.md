# 5.3 Búsqueda real de Places a través de MapViewModel

Prueba instrumentada temporal (`PlacesRealSearchTest`, ya retirada del repositorio), ejecutada en el emulador DADM `DADM_GraphicsSound_API35` (API 35).

Configuración de la prueba:

- `MapViewModel` construido con el `PlacesSdkPlacesRepository` real del contenedor de la app (`AppContainer`), y `mapsKeyConfigured = true` (la llave está configurada; no se registra su valor).
- Ubicación fija en 4.6381, -74.0840 (Universidad Nacional de Colombia), con un `LocationProvider` de prueba.
- Categoría «Hospitales y clínicas» (`hospital`, `medical_clinic`) y radio de 1 km (1000 m).
- Espera el estado de resultados con `StateFlow.first` y un tiempo máximo de 60 s.

Resultado: **OK**.

| Dato | Valor |
|---|---|
| Número de lugares | 20 |
| Primer nombre | Adriana Hurtado Trabajadora social bienestar |
| Distancia del primero | 440,46 m (haversine con `DistanceCalculator`) |

Registro de logcat (etiqueta `PlacesRealTest`, nivel I):

```
RESULTADO count=20 primero=Adriana Hurtado Trabajadora social bienestar distancia=440.45905167595214
```

Verificación de la ejecución: `app/build/outputs/androidTest-results/connected/debug` registra `PlacesRealSearchTest` con `tests="1" failures="0" errors="0"`.

Nota: el primer lugar, por su nombre, no parece un hospital o clínica. La búsqueda se pidió con los tipos `hospital` y `medical_clinic`; la app no revisa el tipo de cada lugar devuelto, así que el resultado es el que entregó Places para esa petición.
