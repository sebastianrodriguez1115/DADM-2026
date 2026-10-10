# Revisión independiente de constraints / render

## Geometría y redondeo

- `GameBoard.kt:41–45`: mínimo = 3 × (ceil(48 × density) + 2 × round(2 × density)). El padding está antes del tag/clickable (`122–130`), por lo que la medición de las celdas corresponde al objetivo interior, no a la franja decorativa.
- Mínimos calculados independientemente:

| Densidad | Objetivo mínimo px | Padding por lado px | Tablero mínimo px | Celda interior px en mínimo |
|---|---:|---:|---:|---:|
| 1.1 | 53 | 2 | 171 | 53 |
| 1.25 | 60 | 3 | 198 | 60 |
| 1.5 | 72 | 3 | 234 | 72 |

- `TicTacToeScreen.kt:100–131`: mide primero controles; resta tres separaciones de 12 dp y padding vertical de pantalla, conserva el mínimo aunque falte altura, limita por ancho y mide tablero con `Constraints.fixed(boardSize, boardSize)`. Centrado horizontal; contenido total mayor que viewport se aloja en `verticalScroll`.
- Los anchos autorizados (320/480 dp, menos 32 dp de padding) superan los mínimos. Un ancho inferior a ese mínimo más padding queda fuera de esta validación.
- `GameBoard.kt:63–80`: pesos iguales 3×3 pueden repartir un píxel de resto; las assertions permiten ≤1 px de diferencia de dimensiones/alineación. Canvas usa tercios del mismo cuadrado (`82–100`), por lo que su línea ideal puede diferir subpíxel de una frontera de fila/columna redondeada, no cambiar la ubicación de una celda.

## Calidad de las assertions

- `GameBoardLayoutTest:96–152` comprueba nueve interiores, contención, ≥ceil(48×density) con tolerancia 0.1 px únicamente de conversión float; no usa la tolerancia de 1 dp del test histórico para aprobar el mínimo. Los tamaños físicos medidos son enteros: 0.1 px no permite aprobar un píxel faltante.
- El código fuente local de `androidx.compose.ui:ui-test-android:1.10.4`, `BoundsAssertions.kt`, confirma que `getUnclippedBoundsInRoot()` convierte con `node.layoutInfo.density`, no con la densidad del host. Multiplicar por la densidad local del caso recupera píxeles reales correctamente.
- La matriz de viewports usa fuente 2 y densidad 1.25; la matriz fraccional usa fuente 2 y 320×192. No es producto cartesiano de todas las combinaciones. Se alterna tema por tamaño, no ambos temas en cada tamaño.
- En alturas 192, los tres controles se llevan a vista con `performScrollTo().assertIsDisplayed()`. `TicTacToeScreenTest` añade click de nueva partida y acceso al tablero/celdas en otros constraints y en fuente 2/claro-oscuro.
- Las assertions de geometría usan bounds **sin clipping**, apropiados para mínimos del contenido desplazable. No certifican visibilidad simultánea de tablero y controles ni un gesto humano de scroll; esto tampoco es exigible cuando la altura es menor que el contenido.

## Render: alcance real de la revisión

- `GameBoardRenderingTest:38–68` captura las celdas X/O, comprueba píxeles del color de tema, O hueca y ausencia de texto X/O en claro/oscuro.
- `74–115` comprueba cuatro muestras de líneas en tercios, dos muestras sin línea y nueve toques de centro con callbacks únicos. No demuestra por sí solo «exactamente cuatro líneas» en cada píxel ni escala vectorial sin distorsión en toda la matriz compacta.
- Esta sesión revisa esas assertions; **no reejecuta** la clase de render ni presenta sus capturas como evidencia nueva. No se crean screenshots del dispositivo físico que pudieran incluir aplicaciones ajenas.
- La geometría compartida y los trazos vectoriales se inspeccionaron; no se observó un defecto reproducible dentro del alcance. TalkBack humano, render compacto pixel-perfect y matriz cartesiana completa permanecen como límites explícitos, no como pruebas ejecutadas.
