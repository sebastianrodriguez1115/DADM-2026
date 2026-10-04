# QA 3.5–3.7: plan y resultados

## Alcance y oráculo

Requerimiento original recibido: modernización Android Compose con nueve zonas Canvas, X/O vectoriales, SoundPool/mute; respuesta y apertura O tras 1000 ms acumulados en foreground; pausa conserva restante; nueva partida cancela; conservación de sesión, score y política IA. Esta entrega valida **solo core ViewModel/reloj/revalidación JVM** tras code review aprobado. No valida render, integración Android ni reproducción.

Oráculos leídos: `openspec/config.yaml`, `design.md`, `tasks.md` y deltas `specs/game-session`, `specs/tic-tac-toe-game`, `specs/game-options` del cambio. No se modifican tareas ni código. App nativa `com.edu.unal.tictactoe/.MainActivity`, sin URL/credenciales. Playwright, axe web y Postman no aplican; no se simula una validación web de Compose.

Precondición común JVM: dispatcher Main de prueba y reloj virtual controlable, instancias aisladas, motor determinista cuando corresponda; foreground inicial false, `onHostResumed()` explícito salvo QA01. Pasos ejecutados mediante las pruebas existentes, sin añadir tests al producto. La trazabilidad exacta método/XML y los conteos por criterio están en `evidence/criterion-results.json`. `evidence/jvm-summary.json` contiene todos los 107 métodos, incluidos regresión.

Riesgo alto primero: movimiento temprano/background, callbacks cancelados y score duplicado. Técnicas: límites temporales, transiciones de estados y particiones válidas/obsoletas. No se duplican tests unitarios en E2E.

| ID | Requerimiento / capability y tarea | Técnica / precondición particular | Pasos | Esperado | Resultado (tests XML) |
|---|---|---|---|---|---|
| QA01 | Conservación sesión, `specs/game-session`; 3.5 | Transición, host no resumed | Crear respuesta y apertura; avanzar 2000 ms sin resume; reanudar y avanzar 1000 | Ninguna O antes de resume; una O después | PASA (1) |
| QA02 | Secuencia O, `specs/tic-tac-toe-game`, apertura `specs/game-session`; 3.2–3.4 | Límites 999/1000, respuesta y apertura | Aceptar X o nueva O; intentar X pendiente; avanzar 999+1; seguir avanzando | Turno O bloquea X; cero O en 999, una en 1000; sin duplicado | PASA (2) |
| QA03 | Umbral monotónico, `specs/tic-tac-toe-game`; 3.3 | Límites sub-ms y despacho tardío; reloj independiente | Despertar en 999.5 ms monotónicos; completar 1000; demorar inicio de coroutine 400 ms | Reprograma sin adelantar umbral ni añadir 400 ms extra | PASA (2) |
| QA04 | Pausa restante, `specs/game-session`; 3.5 | Transiciones e idempotencia | Consumir 400; pausar 5000; resume; avanzar 599+1; repetir resume en otra cuenta a 400 | Conserva 600 ms; cero O background; resume repetido no reinicia | PASA (1) |
| QA05 | Idempotencia pause, `specs/game-session`; 3.5 | Transiciones repetidas | 400 foreground; pause; 2000; pause repetido; 8000; resume; 599+1 | Pause repetido no descuenta background; una O al restante | PASA (1) |
| QA06 | Pausa al deadline, `specs/game-session`; 3.5 | Límite restante cero | Llegar a 1000 sin despachar; pause; despachar; resume; repetir ciclos | No O pausado; una al resume; estado estable después | PASA (1) |
| QA07 | Cancelación/sesión lógica, `specs/game-session`; 3.4/3.5 | Transiciones, misma instancia VM | Pause a 400; nueva partida; resume; luego nueva apertura y pause/resume a 400+600 | Solo apertura nueva; una O; conserva restante en VM retenido | PASA (1), **no rotación Android** |
| QA08 | Cierre sesión, `specs/game-session`; core 3.6 | Transición terminal del VM | Respuesta pendiente; 400; `ViewModelStore.clear()`; avanzar 2000 | Cancela deadline y no muta tablero/estado | PASA (1), **no cierre Activity** |
| QA09 | Nueva partida invalida pendiente, `specs/game-session`; 3.4/3.7 | Límites/cancelación | Nueva partida sobre respuesta; nueva apertura a 400; pasar deadline viejo; nueva partida al deadline sin despachar | Callback viejo no coloca marcas ni altera score; apertura propia usa 1000 nuevos | PASA (3) |
| QA10 | Selección vigente, `specs/game-options` y `specs/game-session`; core 3.7 | Particiones obsoleta/vigente; reentrancia fake | Durante selección Easy cambiar a Impossible; en otra selección iniciar nuevas X/O | Descarta selección antigua; Impossible usa apertura canónica; nuevo token espera su segundo completo, score cero | PASA (2) |
| QA11 | Foreground revalidado, `specs/game-session`; 3.5/core 3.7 | Reentrancia fake, pause durante selección | Pausar desde randomizer; avanzar background; resume | Selección obsoleta no aplica; una O tras resume; sin efectos background | PASA (1) |
| QA12 | Dificultad vigente sin reinicio, `specs/game-options`; core 3.7 | Límite y decisión Easy/Impossible | Cambiar nivel a 400 de apertura; avanzar 599+1; comprobar apertura Impossible | Tablero vacío editable; nivel vigente a 1000; sin RNG Impossible | PASA (2) |
| QA13 | Rechazo dificultad activa/obsoleta, `specs/game-options`; core 3.7 | Particiones vacío/activo/solicitud obsoleta | Capturar editable; aceptar primera jugada; pedir otro nivel o mismo nivel | Retorna false; mismo snapshot, dificultad, score y llamadas motor | PASA (2), **no diálogo/Toast** |
| QA14 | Terminal y score únicos, `specs/tic-tac-toe-game`, `specs/game-session`; core 3.7 | Tabla terminal X/O/empate | Completar victoria X, empate y victoria O; entradas posteriores inválidas/válidas; avanzar; nueva partida | X terminal no programa O; score una vez; terminal bloquea entradas; nueva conserva score/snapshots | PASA (5) |
| QA15 | Sonido no altera espera/sesión, `specs/game-options`; regresión estado 2.1/2.2 y core 3.7 | Transiciones mute/unmute | Mutear; movimientos y terminal; alternar a 400 apertura; 599+1; nuevas partidas/cambios dificultad | Conserva mute y estado; alternancia no adelanta/reinicia deadline; nueva sesión enabled | PASA (3), **no audio** |
| QA16 | RESUMED real/pausa, `specs/game-session`; 3.6 | Transiciones Android; dispositivo requerido | Lanzar Activity; respuesta; consumir ~400; background >1000; resume y medir restante | Sin O background; O al restante una sola vez | BLOQUEADO (0) |
| QA17 | Rotación/cierre real, `specs/game-session`; 3.6 | Configuración y fin sesión; dispositivo requerido | Recrear Activity con respuesta y apertura pendientes; cerrar con espera; abrir otra sesión | VM retenido/restante/sesión; no duplicación; cierre cancela y sesión nueva limpia | BLOQUEADO (0) |
| QA18 | Menú no pausa/selección UI obsoleta, `specs/game-options`; 3.7 | Transiciones/negativo UI; dispositivo requerido | Abrir menú/selector en apertura; vencer O; intentar selección capturada antes | O puede jugar con menú abierto; selección obsoleta no cambia nivel ni produce Toast | BLOQUEADO (0) |

## Exploración guiada

Charter: seguir autoridad de turno alrededor de deadline, pause, generación y nivel. Se inspeccionaron los escenarios negativos existentes de despacho tardío y reentrancia (QA03/QA09–QA11) y sus aserciones antes de verificar su ejecución fresca. No es evidencia de interacción Android ni de una auditoría completa de concurrencia. Sin defectos reproducibles en los casos ejecutados.

Resultados: QA01–QA15 PASA (28 métodos trazados); QA16–QA18 BLOQUEADO. Suite completa: 107 pasa, 0 falla/error/omitido, 14 clases. Aprobación parcial del core de 3.5; 3.6 y 3.7 completas siguen bloqueadas por integración Android sin evidencia. Véase `report.md`.
