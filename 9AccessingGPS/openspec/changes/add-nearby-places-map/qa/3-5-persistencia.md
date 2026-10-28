# 3.5 Persistencia del radio en el emulador

- Emulador: AVD `DADM_GraphicsSound_API35` (API 35, `google_apis`).
- App: `com.edu.unal.lugarescercanos`, build debug instalada con `./gradlew :app:installDebug`.
- Selección del dispositivo: el comando de adb se dirigió al emulador del AVD indicado, sin registrar su identificador.

## Pasos

1. Abrir la app con `adb shell am start -n com.edu.unal.lugarescercanos/.MainActivity`. El mapa muestra «Lugares cercanos» y el engranaje con la descripción «Ajustes».
2. Tocar el engranaje. Ajustes muestra «Radio de búsqueda» y «1 km» (valor por defecto, sin valor guardado).
3. Mover el slider hasta 3 km con un toque en su pista, en la posición horizontal que corresponde a 3 km (`adb shell input tap` sobre el slider, leyendo sus límites con `uiautomator dump`). Un toque mueve y suelta el slider, así que el valor se guarda. Ajustes muestra «3 km».
4. Cerrar la app con `adb shell am force-stop com.edu.unal.lugarescercanos`.
5. Abrir la app de nuevo con `adb shell am start -n com.edu.unal.lugarescercanos/.MainActivity` y tocar el engranaje.
6. Resultado: Ajustes muestra «3 km» y el slider está en 3 km.

## Resultado

- Paso 6: **OK**. El valor leído en el `uiautomator dump` tras el reinicio es `text="3 km"`.
- Captura: `qa/3-5-persistencia.png` (1080 x 2400), con Ajustes mostrando «3 km».

## Prueba instrumentada asociada

- `RadiusPreferencesTest` (3 pruebas, pasan en el mismo emulador): lo que escribe una instancia del repositorio lo lee otra sobre el mismo archivo de DataStore, con el archivo de prueba aislado en `cacheDir` y borrado al terminar.
