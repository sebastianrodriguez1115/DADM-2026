# Sesión manual del usuario (tarea 8.8), registro parcial

- Fecha: 2026-10-11.
- Quién: el usuario, con su teléfono (dispositivo físico, API 35) contra un emulador con ventana, con la versión final de la app. Antes de empezar, el orquestador comprobó que la app instalada en el teléfono contenía las clases del modo en línea y la corrección de la pantalla encendida.
- Estado de la tarea 8.8: **parcial**. Faltan las respuestas de los puntos 2 y 3. TalkBack no se ejecutó. La tarea no está marcada.

## Respuestas del usuario

Las respuestas van entre comillas, tal como las dio el usuario; no se amplían ni se interpretan.

| # | Comprobación pedida | Respuesta del usuario |
|---|---|---|
| — | Partida completa en línea entre teléfono y emulador | «Me funcionó el happy path» |
| 1 | En la partida suena una vez cada jugada, propia y del rival, con sonidos distintos para X y O, y nada al abrirse la partida | «Sí, sonido normal.» |
| 2 | Girar el teléfono a mitad de partida no reproduce ningún sonido | Sin respuesta todavía: el usuario pidió aclarar la pregunta. **Pendiente.** |
| 3 | Con «Sonido» deshabilitado desde el menú del juego local, la partida en línea no suena | Sin respuesta todavía. **Pendiente.** |
| 4 | Modo avión en el lobby: aparece «Sin conexión», «Crear juego» queda deshabilitado y el aviso se va solo al desactivarlo | «Sirvió como describes.» |
| 5 | Repetir el modo avión sin cerrar la app: «Sin conexión» aparece también la segunda vez | «Sí, y se restaura el botón cuando vuelve la conexión también.» |
| 6 | Borrar desde la consola un juego en espera: el teléfono vuelve al lobby con «Este juego ya no está disponible» | «Sí, funcionó como describes» |
| 7 | Al final, con las dos apps en la pantalla inicial, no quedan juegos en `games` | «Sí, no hay games en esas condiciones.» |
| 8 | Partida en curso, Inicio 20 s y volver: «Se perdió la conexión. La partida terminó.» en el teléfono y «Tu rival salió de la partida» en el emulador | «Correcto.» |

## Lectura del orquestador (no es una respuesta del usuario)

Después del punto 8, con el triqui en primer plano en el lobby del teléfono y el mismo proceso de la app vivo desde su instalación (unos 30 minutos, así que todas las comprobaciones ocurrieron en ese proceso), `dumpsys power` no mostraba ningún bloqueo de pantalla activo atribuido a la app, y la ventana de la app no tenía `KEEP_SCREEN_ON`.

Es coherente con la corrección de D-4 tras un segundo plano largo en el dispositivo físico, que era el caso que faltaba en la tercera pasada. Reservas:

- La lectura se hizo minutos después de volver al lobby, no en el instante de volver.
- En este teléfono el indicador de ventana ya había resultado poco fiable en pasadas anteriores (ver `e2e/tercera-pasada/resultados.md`); el indicador que cuenta es el de energía.

## Qué dicen los puntos 4 y 5 sobre D-3

Según el reporte del usuario, en el teléfono «Sin conexión» apareció en los dos cortes seguidos de modo avión y la acción «Crear juego» se restableció al volver la conexión. Lo que falló en el emulador en la primera pasada (D-3) no se reprodujo en el dispositivo físico. No se cambió nada en la app: el fallo solo se vio en el emulador y su causa allí no se investigó.

## TalkBack

No se ejecutó. Fue una propuesta del orquestador (mantener el criterio de los talleres anteriores) que el usuario no objetó; no es una decisión expresa suya. Queda como no verificado.

## Incidencias de la sesión

- El teléfono tuvo instalada unos minutos la app del taller anterior, porque el usuario la lanzó desde otro proyecto del IDE con el mismo identificador de aplicación. Se corrigió antes de las comprobaciones.
- El emulador se colgó y se cerró una vez durante la sesión (fallo del emulador, no de la app) y se volvió a abrir.
