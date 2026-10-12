# Resultados de abandono, desconexión y segundo plano (tarea 8.6)

Destinos: "instancia A (dispositivo físico, API 35)" e "instancia B (emulador, API 35)". Bitácora con marcas de hora: `log.txt` (lee primero su nota de validez). Veredictos: pasa, falla, bloqueado, no ejecutado.

Las instancias A y B jugaron como creador (X) o invitado (O) según el caso. Tiempos medidos comparando capturas consecutivas de la instancia observadora (resolución ~0.1 s en B, ~0.4 s en A).

| ID | Escenario (especificación) | Veredicto | Evidencia / medición |
|---|---|---|---|
| M01 | Salir con el botón: diálogo y Cancelar (online-match: Salir de una partida en curso con confirmación) | pasa | `M01_A_dialogo_abandonar_con_Salir_instancia-A.png` («¿Abandonar la partida?», «Cancelar», «Abandonar»); `M01_B_sin_cambios_mientras_dialogo_instancia-B.png`: la partida del rival sigue igual; tras «Cancelar» el tablero, el turno y la partida siguen. |
| M02 | Back en curso muestra el diálogo y un segundo Back lo descarta | pasa | `M02_A_dialogo_con_Back_instancia-A.png`. |
| M03 | Confirmar «Abandonar» | pasa | `M03_abandonar_confirmado_instancia-A.png` (lobby), `M03_abandonar_confirmado_instancia-B.png` («Tu rival salió de la partida», «Volver al lobby», casillas deshabilitadas, sin ganador): 0.41 s tras el toque. |
| M04 | Back con abandono, sin diálogo | pasa | `M04_B_back_tras_abandono_lobby_instancia-B.png`. Back con la partida terminada: `../reto/R15_B_back_sin_dialogo_instancia-B.png`. |
| M05 | El invitado cierra la app (B, `force-stop`) con la partida en curso | pasa | `M05_cierre_de_B_invitado_rival_ve_abandono_instancia-A.png`: A ve «Tu rival salió de la partida» a 0.78 s del cierre, sin ganador, casillas deshabilitadas. |
| M06 | El creador cierra la app (A, `force-stop`) con la partida en curso | pasa | `M06_cierre_de_A_invitado_rival_ve_abandono_instancia-B.png`: B lo ve a 0.34 s. |
| M07 | Se apaga la red de una instancia con la partida en curso (corte en B) | pasa con observación | `M07b_A_ve_abandono_por_corte_de_red_de_B_instancia-A.png`: A ve el abandono **114 s** después del corte (detección del servicio por desconexión; durante ese tiempo A ve «Turno de Luis»). La instancia B ve «Tu rival salió de la partida» a los 0.49 s (ver M08 y D-1). |
| M08 | Sin conexión durante la partida: «Sin conexión», casillas deshabilitadas, recuperación sola | **falla** | `M08_B_sin_conexion_en_partida_instancia-B.png`: con la red de B cortada, B nunca muestra «Sin conexión» (en 20 s) sino «Tu rival salió de la partida» (casillas deshabilitadas); al restablecer la red no hay recuperación porque la partida ya quedó como abandonada en B, aunque el servicio aún no la había dado por terminada (A seguía en «Turno de Luis» a los 20 s). Ver D-1. |
| M09 | Volver de segundo plano con conexión | **falla** (a partir de ~6 s) | B (API 35) pasa a segundo plano con HOME: la partida termina como abandono para ambos jugadores a los 6.7 s (`log.txt` M09b; `M09_vuelta_de_segundo_plano_con_conexion_instancia-A.png`, `M09_vuelta_de_segundo_plano_con_conexion_instancia-B.png`). Con 2 s y 4 s en segundo plano la partida se conserva (`log.txt` M09c). Causa observada: `dumpsys netpolicy` muestra `blocked=APP_BACKGROUND` para el paquete con la app en segundo plano. Ver D-2. |
| M10 | Volver de segundo plano tras perder la conexión | pasa (efecto) | Mismo mecanismo que M09: al volver, B ve el abandono con «Volver al lobby» en lugar de la partida en curso. |
| M11 | Residuo de nodo tras estos casos | no verificado en consola | Tras cada caso el lobby de B volvió a mostrar «No hay juegos disponibles»; esa lista solo muestra juegos en espera, así que un nodo en otro estado no se vería. No se revisó la consola. |

## Medición de la detección por desconexión

| Causa | Tiempo hasta que el rival ve «Tu rival salió de la partida» |
|---|---|
| Confirmar «Abandonar» | 0.41 s |
| `force-stop` del invitado | 0.78 s |
| `force-stop` del creador | 0.34 s |
| Corte de red del jugador (agujero negro: wifi y datos deshabilitados) | 114 s (una medición válida; en otra prueba, >34 s sin detección) |
| App en segundo plano en API 35 (red bloqueada por el sistema) | 6.7 s |

## Resumen

- Pasan: M01 a M06, M07 (con observación).
- Fallan: M08 (D-1) y M09 (D-2).
- No verificado: residuo de nodos en la consola (M11).
- Tarea 8.6: no se marca.
