# Exploración nativa — resultado parcial, no defecto

Se instaló únicamente el APK debug del proyecto (la instrumentación había retirado sus paquetes al terminar). Se lanzó MainActivity y se verificó su foco antes de capturar/tocar. La captura `board-empty.png` contiene exclusivamente el tablero: cuatro líneas completas en tercios y nueve celdas; se inspeccionó visualmente y con `verify-grid-png.py`.

Última ejecución de `explore-adb.py`:

1. `native-before.json`: nueve casillas vacías; tablero `[60,402][1380,1722]`, cuadrado de 1320 px.
2. Toque ADB al centro de fila 1, columna 1 (280,622).
3. `native-after.json`: una X en fila 1, columna 1 y una O en fila 2, columna 3 (IA vigente, ubicación no garantizada). La envoltura clickable de X está enabled=false. Estado «Tu turno», marcador sin cambios. La respuesta O síncrona es comportamiento previo permitido en 1.3; su espera pertenece a fase 3.
4. Antes de capturar el tablero post-jugada, MainActivity perdió el foco. El guard esperó hasta 10 s consultando la condición y abortó sin capturar pantallas ajenas ni tocar otras aplicaciones. Exit 1 por `RuntimeError: MainActivity is not focused; no screenshot or touches allowed`.

No se ejecutó el segundo toque exploratorio en ocupada; **no se declara PASA**. El rechazo ocupado y los nueve toques siguen cubiertos por las pruebas instrumentadas pasadas.

Durante preparación se corrigieron dos supuestos del script de QA: redacted-manufacturer requiere `dumpsys window` completo para el campo de foco; UIAutomator separa etiqueta y envoltura clickable (el enabled correcto está en la envoltura, no la etiqueta). No son defectos del producto. Hubo también cambios intermitentes de foco tras lanzamiento/durante exploración; no se guardan nombres/datos de otras aplicaciones ni se infiere una causa.

No se cambiaron ajustes globales, no se desbloqueó el teléfono ni se manipularon otras apps. No se guardaron seriales. La captura cruda temporal se eliminó; se retiene solo el recorte del tablero propio.
