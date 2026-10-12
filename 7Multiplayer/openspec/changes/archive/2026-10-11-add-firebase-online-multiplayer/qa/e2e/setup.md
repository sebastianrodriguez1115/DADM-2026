# Preparación de la verificación de extremo a extremo (tarea 8.3)

- Fecha de la tanda: 2026-10-11.
- Aplicación: `com.edu.unal.tictactoe`, commit `86be80a`, rama `reto-7-triqui-online`, APK de depuración construido con `./gradlew :app:assembleDebug` (BUILD SUCCESSFUL; el APK ya estaba al día).
- Backend: proyecto real de Firebase del usuario (Realtime Database y autenticación anónima), configurado por `app/google-services.json` (no se reproduce ningún valor de ese archivo). Reglas de `firebase/database.rules.json` publicadas por el usuario.

## Destinos

| Destino | Descripción | API | Arranque completo | APK de depuración |
|---|---|---|---|---|
| instancia A | dispositivo físico, API 35 | 35 | sí | instalado con `install -r` |
| instancia B | emulador, API 35 | 35 | sí | instalado con `install -r` |

Guarda de identidad aplicada antes de cada operación de adb (todas con el destino explícito):

- Instancia A: único dispositivo que no es emulador, API >= 35 y `sys.boot_completed` = 1. Antes de cada bloque de casos se comprobó además `mWakefulness=Awake` y sin keyguard.
- Instancia B: `emu avd name` = `DADM_GraphicsSound_API35`, API >= 35 y arranque completo; se comprobó antes de cada bloque que el emulador seguía vivo (no se cerró durante esta tanda).

## Precondiciones

| Precondición | Estado | Fuente |
|---|---|---|
| 7.9 Inicio de sesión anónimo activado | CONFIRMADA | La tarea 7.9 está marcada por el usuario y, además, la identidad se estableció en ambas instancias (el lobby se mostró sin «No se pudo conectar con el servicio»; ver `reto/`). |
| 7.10 Reglas publicadas | CONFIRMADA | La tarea 7.10 está marcada por el usuario; el comportamiento observado es coherente con las reglas publicadas (crear, unirse, jugar y abandonar funcionan; las lecturas y escrituras de la app no son rechazadas). |
| 7.11 Reglas probadas y registradas | CONFIRMADA (2026-10-11) | `qa/rules-simulator.md`: el simulador de la consola no se usó; por decisión del usuario del 2026-10-11 se sustituyó por `FirebaseOnlineGameServiceLiveTest` (20 pruebas, tres pasadas seguidas `OK`), que cubre los siete casos. |

## Veredicto de la tarea 8.3

**Lista.** Las tres precondiciones (7.9, 7.10 y 7.11) están confirmadas; 7.11 con la evidencia automática descrita arriba, no con el simulador de la consola. Las dos instancias, el APK y la guarda de identidad están listos y con ellos se ejecutaron las tareas 8.4 a 8.7.

## Notas de operación (incidentes de la tanda)

- El teléfono se bloqueó y se apagó la pantalla una vez durante un bloque (causa no determinada: tiempo de espera de pantalla o uso del usuario); no se intentó desbloquearlo y se esperó a que volviera encendido y sin keyguard. Al volver la pantalla encendida y sin keyguard se reanudó. A partir de ahí se usó un envío periódico de `KEYCODE_WAKEUP` mientras la pantalla estaba encendida y desbloqueada, sin cambiar ningún ajuste del teléfono.
- En varios momentos otra aplicación del propio usuario quedó en primer plano en el teléfono (el usuario lo estaba usando). No se tocó esa aplicación; una captura de la partida tomada mientras estaba en primer plano mostró esa aplicación y se descartó (no está en la evidencia). Cada interrupción dejó la partida de la instancia A en segundo plano y la partida terminó como abandono (ver `abandono/` y el informe).
- Con la aplicación del teléfono en segundo plano o con la pantalla apagada, la partida en curso termina como abandono en unos segundos (ver defecto D-2 del informe final).

## Herramientas

- adb y `uiautomator dump` (volcados leídos en memoria, no guardados), `screencap` (capturas recortadas a la zona de la aplicación en el teléfono) y guiones de Python 3 estándar. No se instaló nada.
- Cortes de red solo en la instancia B: `svc wifi|data disable` y `enable`, y una vez modo avión (`cmd connectivity airplane-mode`); se comprobó con `ping` y con los ajustes del emulador que la red quedó restablecida.
- La adb del teléfono estuvo desconectada unos 12 minutos (el usuario lo desconectó o lo usó); se reanudó al reaparecer con la guarda de identidad.
