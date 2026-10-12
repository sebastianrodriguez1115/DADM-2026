# Checklist de talleres

Este documento lista, taller por taller, lo que pide el enunciado de cada carpeta, para poder revisar con facilidad qué hay que entregar. Cada punto sale del PDF de enunciado de su carpeta y remite al apartado o paso del que viene. Las casillas `- [ ]` están sin marcar para que las marques tú; los puntos opcionales llevan «(Extra)». Los textos entre comillas «así» son literales del enunciado. El documento no afirma que nada esté hecho: solo dice qué se pide y dónde mirar.

## Resumen

| Taller | Carpeta | Enunciado | Entrega o calificación según el enunciado | Evidencia en el repositorio |
|---|---|---|---|---|
| 0 | [0HelloWorld](0HelloWorld/) | No hay enunciado en el repositorio | Sin información | Carpeta del proyecto; sin cambio archivado |
| 1 | No existe carpeta | No hay enunciado en el repositorio | Sin información | Ninguna |
| 2 | No existe carpeta | No hay enunciado en el repositorio | Sin información | Ninguna |
| 3 | [3TicTacToe](3TicTacToe/) | [TicTacToe.pdf](3TicTacToe/TicTacToe.pdf) | No dice qué se entrega ni cómo se califica | Proyecto y [verification.md](7Multiplayer/openspec/changes/archive/2026-09-13-add-tic-tac-toe-game/verification.md) |
| 4 | [4MenusNDialogs](4MenusNDialogs/) | [reto4.pdf](4MenusNDialogs/reto4.pdf) | No dice qué se entrega ni cómo se califica | Proyecto y [verification.md](7Multiplayer/openspec/changes/archive/2026-10-03-add-compose-menus-dialogs/verification.md) |
| 5 | [5GraphicsSound](5GraphicsSound/) | [Reto5 - graphics and sound.pdf](5GraphicsSound/Reto5%20-%20graphics%20and%20sound.pdf) | No dice qué se entrega ni cómo se califica | Proyecto y cambio archivado sin `verification.md` (ver [archive-notes.md](7Multiplayer/openspec/changes/archive/2026-10-03-add-compose-graphics-sound/archive-notes.md)) |
| 6 | [6ChangeOrientation](6ChangeOrientation/) | [Reto6 - change orientation.pdf](6ChangeOrientation/Reto6%20-%20change%20orientation.pdf) | No dice qué se entrega ni cómo se califica | Proyecto y [verification.md](7Multiplayer/openspec/changes/archive/2026-10-04-add-compose-orientation-state/verification.md) |
| 7 | [7Multiplayer](7Multiplayer/) | [Reto7 - TriquiOnline.pdf](7Multiplayer/Reto7%20-%20TriquiOnline.pdf) | «Para calificar este reto el profesor jugará con cada estudiante!» | Proyecto y [verification.md](7Multiplayer/openspec/changes/archive/2026-10-11-add-firebase-online-multiplayer/verification.md) |

---

## Taller 3: Tic-Tac-Toe

Enunciado: [3TicTacToe/TicTacToe.pdf](3TicTacToe/TicTacToe.pdf), «Challenge: Tic-Tac-Toe App».

### Entrega según el enunciado

El enunciado no dice qué se entrega, dónde ni cómo se califica.

- [ ] Anoté el medio de entrega que indica el curso: ________________

### Qué debe hacer la app

Proyecto:

- [ ] El nombre del proyecto es «AndroidTicTacToe-Tutorial2». (*Creating the Android Project*)
- [ ] El nombre de la aplicación es «Android Tic-Tac-Toe». (*Creating the Android Project*)
- [ ] El nombre del paquete es «co.edu.unal.tictactoe». (*Creating the Android Project*)
- [ ] La actividad principal se llama «AndroidTicTacToeActivity». (*Creating the Android Project*)
- [ ] La lógica del juego está separada de la interfaz: la interfaz está en «AndroidTicTacToeActivity» y la lógica en «TicTacToeGame». (*App Design*, párrafo final; paso 1)
- [ ] El constructor de «TicTacToeGame» solo inicializa el generador de números aleatorios, y no existen «getUserMove()» ni «main()». (*App Design*, paso 3)
- [ ] «TicTacToeGame» define las constantes «HUMAN_PLAYER» = 'X', «COMPUTER_PLAYER» = 'O' y «OPEN_SPOT» = ' ' (espacio). (*App Design*, pasos 2 y 4)
- [ ] «TicTacToeGame» expone públicamente «clearBoard()», «setMove(player, location)», «getComputerMove()» y «checkForWinner()», y todos los demás métodos son privados. (*App Design*, paso 4)
- [ ] «setMove» recibe una posición de 0 a 8 y no cambia el tablero si esa posición no está libre. (*App Design*, paso 4)
- [ ] «getComputerMove()» devuelve la mejor jugada (0 a 8) sin hacerla; la jugada se aplica llamando a «setMove()». (*App Design*, paso 4)
- [ ] «checkForWinner()» devuelve 0 si no hay ganador ni empate aún, 1 si hay empate, 2 si ganó X y 3 si ganó O. (*App Design*, paso 4)

Juego:

- [ ] El humano juega con X y la computadora con O. (*App Design*)
- [ ] La computadora gana si puede, bloquea al humano si puede y, si no, juega al azar. (*App Design*)
- [ ] El tablero es una cuadrícula de 3 por 3 con nueve botones, en tres filas de tres. (*Creating a Game Board*, paso 2)
- [ ] Cada botón del tablero mide 100dp por 100dp con texto de 70dp. (*Creating a Game Board*, paso 2)
- [ ] Debajo del tablero hay un texto de estado centrado horizontalmente, de 20dp, con margen superior de 20dp. (*Creating a Game Board*, paso 2)
- [ ] Al empezar, los botones no tienen texto. (*Introduction*; *Adding Game Logic*, paso 2)
- [ ] Se empieza una partida nueva al abrir la app. (*Adding Game Logic*, paso 4)
- [ ] Al empezar una partida, el texto de estado dice «You go first.». (*Adding Game Logic*, paso 3; *String Messages*)
- [ ] Al pulsar un botón libre, muestra una X verde. (*Introduction*; *Adding Game Logic*, paso 6)
- [ ] La computadora responde con una O roja. (*Introduction*; *Adding Game Logic*, paso 6)
- [ ] Un botón ocupado queda deshabilitado y no se puede volver a jugar. (*Adding Game Logic*, pasos 5 y 6)
- [ ] El texto de estado indica el turno del humano, «Your turn.». (*Introduction*; *String Messages*)
- [ ] El texto de estado indica el turno de la computadora, «Android's turn.». (*Introduction*; *String Messages*)
- [ ] El texto de estado indica el empate, «It's a tie.». (*String Messages*)
- [ ] El texto de estado indica que ganó el humano, «You won!». (*String Messages*)
- [ ] El texto de estado indica que ganó la computadora, «Android won!». (*String Messages*)
- [ ] Cuando la partida termina, el humano ya no puede seguir jugando. (*Adding Game Logic*, paso 8)
- [ ] El menú de opciones tiene «New Game», que limpia el tablero y empieza una partida nueva. (*Adding a Menu*, pasos 1 a 3)
- [ ] Los mensajes de la interfaz están en el archivo de recursos de textos y no escritos en el código; el nombre de la app es «Tic-Tac-Toe». (*String Messages*, pasos 1 a 3)

### Extras (opcionales)

- [ ] (Extra) Se alterna quién empieza cada partida. (*Extra Challenge*)
- [ ] (Extra) Se lleva la cuenta de partidas ganadas por el usuario, ganadas por la computadora y empates. (*Extra Challenge*)
- [ ] (Extra) Las cuentas se muestran en controles de texto debajo del texto de estado, uno junto al otro. (*Extra Challenge*)

### Notas

- El enunciado está escrito para Java, layouts XML y Eclipse. Los pasos de herramienta (crear clases, `findViewById`, `Ctrl-Shift-O`, `OnClickListener`) se listan por su resultado, no literalmente; los nombres de variables internas (`mGame`, `mBoardButtons`, `mGameOver`) no se piden como tales.
- Los textos del enunciado no coinciden del todo: el código de ejemplo escribe «It's your turn.» y «It's a tie!», mientras que el archivo de textos define «Your turn.» y «It's a tie.». Las casillas usan los del archivo de textos; el enunciado no dice cuál prevalece.
- La carpeta [4MenusNDialogs](4MenusNDialogs/) contiene una copia idéntica de este PDF ([TicTacToe.pdf](4MenusNDialogs/TicTacToe.pdf)).
- El taller 6 da por hecho que existen las cuentas y el turno inicial del Extra de este taller (ver su nota).

### Evidencia en el repositorio

- Proyecto: [3TicTacToe](3TicTacToe/).
- Cambio archivado: [add-tic-tac-toe-game](7Multiplayer/openspec/changes/archive/2026-09-13-add-tic-tac-toe-game/), con [verification.md](7Multiplayer/openspec/changes/archive/2026-09-13-add-tic-tac-toe-game/verification.md).

---

## Taller 4: Menús y diálogos

Enunciado: [4MenusNDialogs/reto4.pdf](4MenusNDialogs/reto4.pdf), «Challenge: Menus and Dialog Boxes».

### Entrega según el enunciado

El enunciado no dice qué se entrega, dónde ni cómo se califica.

- [ ] Anoté el medio de entrega que indica el curso: ________________

### Qué debe hacer la app

- [ ] El menú de opciones tiene tres opciones: «New Game», «Difficulty» y «Quit». (*Introduction*; *Create the Menu in XML*, paso 5)
- [ ] El menú está definido como recurso («options_menu.xml» en la carpeta «menu») con los identificadores «new_game», «ai_difficulty» y «quit», no creado en código. (*Create the Menu in XML*, pasos 1 a 5)
- [ ] Cada opción del menú tiene un icono («new_game», «difficulty_level» y «quit_game»). (*Create the Menu in XML*, paso 5; *Add Menu Items Images*, pasos 1 a 4)
- [ ] Los iconos del menú no miden más de 42 píxeles de alto. (*Add Menu Items Images*, paso 1)
- [ ] El juego tiene tres niveles de dificultad, «Easy», «Harder» y «Expert», y el nivel inicial es «Expert». (*Introduction*; *Changes to the Game Logic*, paso 1)
- [ ] El juego permite leer y cambiar el nivel de dificultad. (*Changes to the Game Logic*, paso 2)
- [ ] En «Easy», la computadora siempre juega al azar. (*Changes to the Game Logic*)
- [ ] En «Harder», la computadora hace una jugada ganadora si puede y, si no, juega al azar. (*Changes to the Game Logic*)
- [ ] En «Expert», la computadora intenta ganar, si no puede bloquea al humano y, si tampoco puede, juega en cualquier lugar. (*Changes to the Game Logic*, paso 3)
- [ ] Existen la jugada al azar, la ganadora y la de bloqueo, y cada una deja el tablero igual que lo encontró. (*Changes to the Game Logic*, paso 3)
- [ ] «New Game» inicia una partida nueva. (*Introduction*; *Display the Menu and Respond to Menu Selections*)
- [ ] «Difficulty» abre un diálogo con un título y tres botones de radio rotulados «Easy», «Harder» y «Expert». (*Display the Menu and Respond to Menu Selections*, paso 3)
- [ ] En el diálogo de dificultad aparece seleccionado el nivel actual. (*Display the Menu and Respond to Menu Selections*, paso 3)
- [ ] Al elegir un nivel, el diálogo se cierra y el juego queda con ese nivel. (*Display the Menu and Respond to Menu Selections*, paso 3)
- [ ] Al elegir un nivel, aparece brevemente un mensaje (Toast) con el nivel elegido. (*Display the Menu and Respond to Menu Selections*, paso 3)
- [ ] «Quit» abre un diálogo de confirmación con respuestas Yes y No. (*Display the Menu and Respond to Menu Selections*, paso 4)
- [ ] El diálogo de «Quit» no se puede cancelar fuera de sus botones. (*Display the Menu and Respond to Menu Selections*, paso 4)
- [ ] Al responder Yes, la app termina; al responder No, el diálogo se cierra y la app sigue. (*Display the Menu and Respond to Menu Selections*, paso 4)
- [ ] Los textos de los diálogos (título de dificultad, niveles, pregunta de salida, Yes, No) están en el archivo de recursos de textos. (*Display the Menu and Respond to Menu Selections*, pasos 3 y 4)
- [ ] Las tres opciones del menú funcionan como se espera al ejecutar la app. (*Display the Menu and Respond to Menu Selections*, final)

### Extras (opcionales)

- [ ] (Extra) La app tiene un icono propio, que se ve en la lista de aplicaciones instaladas. (*Extra Challenge*, 1)
- [ ] (Extra) Hay una opción de menú que abre un diálogo «About». (*Extra Challenge*, 2)
- [ ] (Extra) El diálogo «About» identifica a quien programó la app, muestra una imagen y tiene un botón «OK». (*Extra Challenge*, 2)

### Notas

- El enunciado está escrito para Java, layouts XML y Eclipse (`showDialog`, `onCreateDialog`, `AlertDialog.Builder`, carpetas `res/drawable`). Los pasos de herramienta se listan por su resultado. El propio enunciado indica que `showDialog` está obsoleto.
- El enunciado dice que la mayoría de las apps de Android no tienen opción «Quit». El taller 6 pide quitarla (ver más abajo): lo que este taller pide de «Quit» queda reemplazado allí.
- El título del diálogo de dificultad y la pregunta de salida no tienen texto literal en el enunciado; solo se indican los nombres de sus recursos de texto.

### Evidencia en el repositorio

- Proyecto: [4MenusNDialogs](4MenusNDialogs/).
- Cambio archivado: [add-compose-menus-dialogs](7Multiplayer/openspec/changes/archive/2026-10-03-add-compose-menus-dialogs/), con [verification.md](7Multiplayer/openspec/changes/archive/2026-10-03-add-compose-menus-dialogs/verification.md).

---

## Taller 5: Gráficos y sonido

Enunciado: [5GraphicsSound/Reto5 - graphics and sound.pdf](5GraphicsSound/Reto5%20-%20graphics%20and%20sound.pdf), «Challenge: Graphics and Sound».

### Entrega según el enunciado

El enunciado no dice qué se entrega, dónde ni cómo se califica.

- [ ] Anoté el medio de entrega que indica el curso: ________________

### Qué debe hacer la app

Tablero dibujado:

- [ ] El tablero es una vista personalizada llamada «BoardView», que extiende «View» y reemplaza a los botones. (*Introduction*; *Creating a Custom View*, pasos 1 y 11)
- [ ] El grosor de las líneas de la cuadrícula es una constante, «GRID_WIDTH», con valor 6. (*Creating a Custom View*, paso 2)
- [ ] Las líneas de la cuadrícula son gruesas y de color gris claro. (*Creating a Custom View*, paso 10)
- [ ] El tablero dibuja dos líneas verticales y dos horizontales que lo dividen en 3 por 3. (*Creating a Custom View*, pasos 9, 10 y 12)
- [ ] Hay dos imágenes, una para la X del humano y otra para la O de la computadora, en formato JPEG, GIF, PNG o BMP. (*Creating a Custom View*, pasos 3 y 4)
- [ ] Las dos imágenes miden aproximadamente lo mismo y no más de unos 100 píxeles de ancho o de alto. (*Creating a Custom View*, paso 4)
- [ ] Los nombres de los archivos de imagen usan solo minúsculas, números y guion bajo, sin espacios. (*Creating a Custom View*, paso 4)
- [ ] Cada imagen se reduce para caber en su celda y se dibuja en la celda donde está la jugada, según quién la ocupa. (*Creating a Custom View*, pasos 13 y 14)
- [ ] La vista del tablero conoce el juego y puede dar el ancho y el alto de una celda (un tercio del tablero). (*Creating a Custom View*, pasos 14 y 15)
- [ ] La vista del tablero mide 300dp por 300dp con margen superior de 5dp y los demás textos quedan bien ubicados. (*Creating a Custom View*, pasos 11 y 12)

Uso del tablero:

- [ ] Ya no se inicializan botones; al empezar una partida el tablero se vuelve a dibujar. (*Using the BoardView*, pasos 1 y 2)
- [ ] Al tocar una celda, la jugada se hace en esa celda, calculada a partir de las coordenadas del toque. (*Listen for Touches*, pasos 1 y 2)
- [ ] El toque en cada una de las nueve celdas funciona. (*Listen for Touches*, paso 4)
- [ ] No se hace jugada si la celda está ocupada o si la partida ya terminó. (*Listen for Touches*, paso 1; «setMove» devuelve si la jugada fue legal)
- [ ] Una jugada legal vuelve a dibujar el tablero, y primero aparece la X y luego la O. (*Listen for Touches*, pasos 3 y 4)

Sonido:

- [ ] Hay dos efectos de sonido, uno para la jugada del humano y otro para la de la computadora, de uno o dos segundos. (*Adding Sound*, paso 1)
- [ ] Los nombres de los archivos de sonido usan solo minúsculas, números y guion bajo. (*Adding Sound*, paso 1)
- [ ] Suena el efecto del humano cuando el humano juega. (*Adding Sound*, paso 6)
- [ ] Suena el efecto de la computadora cuando la computadora juega. (*Adding Sound*, paso 6)
- [ ] Los reproductores de sonido se crean al reanudar la pantalla y se liberan al pausarla. (*Adding Sound*, paso 5)
- [ ] El usuario puede apagar los sonidos. (*Adding Sound*, introducción; ver Notas, es ambiguo)

### Extras (opcionales)

- [ ] (Extra) La computadora espera un segundo antes de jugar. (*Extra Challenge*)
- [ ] (Extra) La espera no bloquea la interfaz, que sigue respondiendo durante el segundo. (*Extra Challenge*)
- [ ] (Extra) El humano no puede jugar mientras es el turno de la computadora. (*Extra Challenge*)
- [ ] (Extra) Se alcanza a ver el mensaje «Android's turn» y el sonido de la computadora ya no se encima con el del humano. (*Extra Challenge*)

### Notas

- El enunciado está escrito para Java, layouts XML y Eclipse (`BoardView extends View`, `onDraw`, `OnTouchListener`, `MediaPlayer`, carpetas `res/drawable` y `res/raw`). Los pasos de herramienta se listan por su resultado.
- Ambigüedad: el texto de *Adding Sound* dice «Of course you should also allow users to turn off the sounds», pero no es un paso numerado ni el enunciado explica cómo hacerlo. No está claro si es obligatorio u opcional; la casilla queda en la lista principal para que decidas con el curso.
- El enunciado menciona los archivos de sonido en mp3 y dice que los `.wav` pueden dar problemas; no impone el formato.
- Los detalles de implementación en Java (valor de retorno del manejador de toques, cálculo de las coordenadas del rectángulo) no se listan como requisitos.

### Evidencia en el repositorio

- Proyecto: [5GraphicsSound](5GraphicsSound/).
- Cambio archivado: [add-compose-graphics-sound](7Multiplayer/openspec/changes/archive/2026-10-03-add-compose-graphics-sound/). No tiene `verification.md`; tiene [archive-notes.md](7Multiplayer/openspec/changes/archive/2026-10-03-add-compose-graphics-sound/archive-notes.md) e [integration-status.md](7Multiplayer/openspec/changes/archive/2026-10-03-add-compose-graphics-sound/integration-status.md).

---

## Taller 6: Cambio de orientación y estado

Enunciado: [6ChangeOrientation/Reto6 - change orientation.pdf](6ChangeOrientation/Reto6%20-%20change%20orientation.pdf), «Challenge: Changing the Orientation and Saving State».

### Entrega según el enunciado

El enunciado no dice qué se entrega, dónde ni cómo se califica.

- [ ] Anoté el medio de entrega que indica el curso: ________________

### Qué debe hacer la app

Orientación:

- [ ] La app funciona en vertical y en horizontal; no se fuerza una sola orientación. (*Introduction*; *Changing the Orientation*)
- [ ] La ventana de la app no muestra barra de título. (*Changing the Orientation*, paso 1)
- [ ] En horizontal, el tablero mide 270dp por 270dp y queda alineado a la izquierda. (*Changing the Orientation*, paso 4)
- [ ] En horizontal, los textos (estado y marcadores) quedan a la derecha del tablero. (*Changing the Orientation*, paso 4)
- [ ] Al girar el dispositivo, la app cambia de orientación y muestra todos los elementos correctamente. (*Changing the Orientation*, paso 5)

Estado al girar:

- [ ] Al girar a mitad de partida, el tablero conserva las jugadas. (*Saving Instance State*, pasos 1, 2 y 5)
- [ ] Al girar, se conservan el texto de estado, si la partida terminó y quién empieza. (*Saving Instance State*, pasos 1 y 2)
- [ ] Al girar, se conservan los marcadores. (*Changing the Orientation*, paso 5; *Saving Persistent Information*, paso 7)
- [ ] Al girar en el turno del humano, la computadora no hace una jugada adicional. (*Saving Instance State*, paso 5)
- [ ] Los marcadores se muestran en pantalla, con el recuento del humano, los empates y Android. (*Saving Instance State*, paso 3; *Saving Persistent Information*)

Estado entre ejecuciones:

- [ ] Los marcadores se guardan en preferencias llamadas «ttt_prefs», en modo privado. (*Saving Persistent Information*, pasos 1 y 2)
- [ ] Se guardan tres valores enteros con las claves «mHumanWins», «mComputerWins» y «mTies». (*Saving Persistent Information*, paso 3)
- [ ] Los marcadores se guardan cuando la app se detiene. (*Saving Persistent Information*, paso 3)
- [ ] Al abrir la app, los marcadores se restauran, con 0 si nunca se guardaron. (*Saving Persistent Information*, paso 4)
- [ ] Al salir con Atrás y volver a abrir la app, los marcadores son los mismos. (*Saving Persistent Information*, final)

Menú:

- [ ] El menú tiene una opción «Reset Scores» con su icono. (*Saving Persistent Information*, paso 5)
- [ ] La opción «Quit» ya no está en el menú. (*Saving Persistent Information*, paso 5)
- [ ] «Reset Scores» pone en 0 los tres marcadores y los vuelve a mostrar de inmediato. (*Saving Persistent Information*, paso 6 y final)

### Extras (opcionales)

- [ ] (Extra) El nivel de dificultad se guarda en las preferencias, como un entero que se convierte de nuevo al tipo enumerado al leerlo, y se conserva al reabrir la app. (*Extra Challenge*, 1)
- [ ] (Extra) Girar el dispositivo justo después de una jugada y antes de la respuesta de la computadora no cierra la app. (*Extra Challenge*, 2)
- [ ] (Extra) En ese caso, tras girar, la computadora hace su jugada si era su turno. (*Extra Challenge*, 2)

### Notas

- El enunciado está escrito para Java, layouts XML y Eclipse (`res/layout-land`, `Ctrl-F11`, `onSaveInstanceState`, `SharedPreferences`, DDMS). Los pasos de herramienta se listan por su resultado.
- Este taller da por hecho que ya existen los marcadores («mHumanWins», «mComputerWins», «mTies») y el turno inicial («mGoFirst»), que vienen del Extra del taller 3. Si ese Extra no se hizo, las casillas de marcadores de este taller no tienen de dónde salir.
- Cambia el taller 4: aquí se pide quitar «Quit» del menú y añadir «Reset Scores».
- Opcional según el enunciado: el paso 7 dice que se «puede» quitar el código que guardaba los marcadores en el estado de instancia, y el paso 4 de *Saving Instance State* ofrece `onRestoreInstanceState` como alternativa. No se listan como requisitos.
- Según el enunciado, al salir con Atrás el tablero se borra; solo los marcadores persisten entre ejecuciones. El enunciado no pide guardar el tablero entre ejecuciones.
- El enunciado muestra los valores de «ttt_prefs» con la herramienta DDMS; ver el archivo de preferencias es una comprobación, no un requisito.

### Evidencia en el repositorio

- Proyecto: [6ChangeOrientation](6ChangeOrientation/).
- Cambio archivado: [add-compose-orientation-state](7Multiplayer/openspec/changes/archive/2026-10-04-add-compose-orientation-state/), con [verification.md](7Multiplayer/openspec/changes/archive/2026-10-04-add-compose-orientation-state/verification.md).

---

## Taller 7: Jugando Triqui Online

Enunciado: [7Multiplayer/Reto7 - TriquiOnline.pdf](7Multiplayer/Reto7%20-%20TriquiOnline.pdf), «Reto 7: Jugando Triqui Online». El reto consiste en «extender el juego del triqui desarrollado en los anteriores retos para soportar que dos jugadores puedan jugar cada uno en su dispositivo móvil».

### Entrega según el enunciado

El enunciado no dice qué se entrega ni dónde. Sobre la calificación dice: «Para calificar este reto el profesor jugará con cada estudiante!»

- [ ] Anoté el medio de entrega que indica el curso: ________________

### Qué debe hacer la app

- [ ] «En un juego habrá 2 jugadores». (*Recomendaciones*, punto 1)
- [ ] «Cualquiera de los jugadores puede crear un juego». (*Recomendaciones*, punto 2)
- [ ] «La aplicación debe mostrar al inicio una lista de juegos disponibles». (*Recomendaciones*, punto 3)
- [ ] «Un jugador se une a un juego disponible o crea uno nuevo». (*Recomendaciones*, punto 4)
- [ ] «Solo hasta que el usuario se una a un juego podrá jugar». (*Recomendaciones*, punto 5)
- [ ] «Utilice algún servicio en la nube para mediar la interacción de los juegos: por ejemplo, Firebase, Google App Engine, etc.» (*Recomendaciones*, punto 6)

### Extras (opcionales)

El enunciado no tiene extras. La función de compartir la app con un código QR es un añadido que el enunciado no pide.

### Preparación sugerida para la sesión con el profesor (no es parte del enunciado)

Estas casillas salen de las pruebas hechas en este repositorio, no del PDF.

- [ ] Los dos teléfonos tienen Android 15 o superior (la app no se instala en versiones anteriores).
- [ ] El profesor puede instalar la app (por ejemplo, con el APK subido a Firebase App Distribution).
- [ ] Los dos dispositivos tienen Internet.
- [ ] La app se mantiene en primer plano durante la partida (salir más de unos segundos la termina).
- [ ] Sé que, si la red de un jugador se corta de golpe, el otro tarda cerca de dos minutos en verlo.

### Notas

- El enunciado habla de «los anteriores retos»: parte del triqui de los talleres anteriores.
- No dice qué servicio en la nube usar: Firebase y Google App Engine son solo ejemplos.

### Evidencia en el repositorio

- Proyecto: [7Multiplayer](7Multiplayer/).
- Cambio archivado: [add-firebase-online-multiplayer](7Multiplayer/openspec/changes/archive/2026-10-11-add-firebase-online-multiplayer/), con [verification.md](7Multiplayer/openspec/changes/archive/2026-10-11-add-firebase-online-multiplayer/verification.md).
- Cambio archivado, fuera del enunciado: [add-share-app-qr](7Multiplayer/openspec/changes/archive/2026-10-12-add-share-app-qr/) (compartir la app con un código QR).
