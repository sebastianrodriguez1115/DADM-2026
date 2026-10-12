# Checklist de talleres

Lista de lo que se muestra en la app, taller por taller; se presenta con la app de la carpeta `7Multiplayer`.

## Taller 3: Tic-Tac-Toe

Se muestra en «Jugar contra la computadora».

- [ ] Abrir la partida: tablero de 3 por 3 vacío y el texto «La persona hace la primera jugada».
- [ ] Tocar una casilla libre: aparece una X verde y el texto pasa a «Turno de la computadora».
- [ ] La computadora responde con una O roja y el texto vuelve a «Tu turno».
- [ ] Tocar una casilla ocupada: no pasa nada.
- [ ] Con la dificultad «Experto», la computadora gana si puede y, si no, bloquea al humano.
- [ ] Ganar como humano: el texto dice «Victoria humana».
- [ ] Perder: el texto dice «Victoria de la computadora».
- [ ] Llenar el tablero sin ganador: el texto dice «Empate».
- [ ] Terminada la partida, tocar una casilla libre no hace nada.
- [ ] Tocar el botón «Nueva partida»: el tablero queda vacío.
- [ ] Abrir el menú y elegir «Nueva partida»: hace lo mismo.

### Extras

- [ ] En cada «Nueva partida» se alterna quién empieza (la computadora abre con una O).
- [ ] Debajo del estado se ven «Victorias de X: n», «Victorias de O: n» y «Empates: n».
- [ ] Al terminar una partida sube solo el contador que corresponde.

## Taller 4: Menús y diálogos

Se muestra en «Jugar contra la computadora», con el menú de opciones.

- [ ] Abrir el menú: aparece «Nueva partida», «Dificultad», «Sonido», «Reiniciar puntajes», «Acerca de» y «Volver al inicio», cada una con su icono.
- [ ] Con el tablero vacío, elegir «Dificultad»: diálogo «Dificultad» con «Fácil», «Difícil», «Experto» e «Imposible», y el nivel actual marcado (de inicio «Experto»).
- [ ] Elegir un nivel: el diálogo se cierra y un mensaje breve muestra el nombre del nivel.
- [ ] Con la partida en curso, «Dificultad» aparece deshabilitada.
- [ ] En «Fácil», la computadora juega al azar, sin bloquear.
- [ ] En «Difícil», la computadora gana si puede y, si no, juega al azar.

### Extras

- [ ] La app tiene un icono propio en la lista de aplicaciones.
- [ ] «Acerca de»: diálogo con una imagen, «Juan Sebastian Rodriguez» y el botón «Aceptar», que lo cierra.

## Taller 5: Gráficos y sonido

Se muestra en «Jugar contra la computadora».

- [ ] El tablero se ve dibujado: cuadrícula con las marcas X y O, sin botones ni números.
- [ ] Tocar cada una de las nueve casillas pone allí la marca.
- [ ] Con el sonido «Habilitado», suena un efecto al jugar el humano.
- [ ] Suena un efecto distinto al jugar la computadora.
- [ ] En el menú, «Sonido» alterna entre «Habilitado» y «Deshabilitado»; en «Deshabilitado» no suena nada.

### Extras

- [ ] La computadora tarda cerca de un segundo en jugar y mientras tanto se ve «Turno de la computadora».
- [ ] Durante esa espera el menú abre y responde.
- [ ] Durante esa espera, tocar una casilla libre no hace nada.
- [ ] El sonido de la computadora no se encima con el del humano.

## Taller 6: Orientación y estado

Se muestra en «Jugar contra la computadora».

- [ ] Girar el dispositivo a horizontal y a vertical: la app funciona en ambas y no muestra barra de título.
- [ ] En horizontal, el tablero queda a la izquierda y el estado, el marcador y los controles a la derecha.
- [ ] Girar a mitad de partida: se conservan las jugadas, el texto de estado y el marcador.
- [ ] Girar en el turno del humano: la computadora no hace una jugada de más.
- [ ] Con marcador distinto de cero, cerrar la app por completo y abrirla de nuevo: el marcador es el mismo.
- [ ] El menú tiene «Reiniciar puntajes», con su icono, y no tiene una opción de salir.
- [ ] Elegir «Reiniciar puntajes»: los tres contadores quedan en 0 al instante y la partida sigue igual.

### Extras

- [ ] Cambiar la dificultad, cerrar la app por completo y abrirla de nuevo: «Dificultad» muestra el mismo nivel.
- [ ] Girar justo después de jugar, antes de la respuesta de la computadora: la app no se cierra y la computadora juega una sola vez.

## Taller 7: Triqui en línea

Se muestra en «Jugar en línea», con dos dispositivos.

- [ ] Elegir «Jugar en línea» (la primera vez pide un apodo en «Tu apodo»): aparece «Juegos disponibles».
- [ ] En un dispositivo, «Crear juego»: se ve «Esperando rival…» y «Cancelar», sin tablero.
- [ ] En el otro dispositivo, la lista muestra «Juego de <apodo>» sin recargar.
- [ ] Elegir ese juego: ambos ven el tablero; quien creó ve «Juegas con X» y quien se unió «Juegas con O».
- [ ] Cada uno ve «Rival: <apodo>»; quien creó ve «Tu turno» y el otro «Turno de <apodo>».
- [ ] Solo marca quien tiene el turno; en el otro dispositivo las casillas no responden.
- [ ] Cada jugada aparece en el otro dispositivo sin tocar nada y el turno pasa.
- [ ] Al ganar: «Ganaste» para el ganador y «Ganó <apodo>» para el otro; en empate ambos ven «Empate».
- [ ] Al terminar el tablero queda bloqueado y se ofrece «Volver al lobby».

### Añadidos (no los pide el enunciado)

- [ ] «Volver al inicio» en el lobby (y en el menú de la partida contra la computadora) lleva a la pantalla inicial.
- [ ] «Compartir app» en el lobby abre un diálogo con un código QR, el texto «Escanea para instalar el Triqui» y «Cerrar».
