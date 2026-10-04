# Procedencia de los efectos de jugada

`game_move_x.wav` y `game_move_o.wav` son tonos originales generados localmente
por `generate_game_move_tones.py`. El script sintetiza directamente ondas
senoides; no usa ni incorpora grabaciones, muestras, bibliotecas de audio,
fuentes tipográficas ni otros materiales de terceros. Por ello no hay autoría
ni atribución de terceros que declarar para estos dos recursos. No se afirma
una licencia global nueva para el repositorio ni una concesión de derechos más
allá de esta declaración de procedencia.

Parámetros compartidos: WAV PCM sin compresión, mono, 16 bits con signo,
44 100 Hz, duración de 180 ms, pico nominal 0,35 de escala completa, y rampas
cosenoidales de entrada/salida de 12 ms. X usa senoide de 880 Hz (tono alto) y
O senoide de 440 Hz (tono bajo); no hay voz ni música. Las muestras se
construyen solo con módulos estándar de Python (`math`, `struct`, `wave` y
`pathlib`).

Desde la raíz del repositorio, regenerar byte por byte con:

```sh
python3 tools/audio/generate_game_move_tones.py
```

La salida se escribe en `app/src/main/res/raw/`. El script comprueba dentro del
proceso que una segunda generación de cada WAV produce los mismos bytes antes
de escribirlos. SHA-256 de los WAV entregados:

| Recurso | SHA-256 |
| --- | --- |
| `game_move_x.wav` | `5c0a56baea4104a4ecb14af1de61d1c8a6833fb6cb44670c3dbccce71020a2c0` |
| `game_move_o.wav` | `3330ef2de3a0cdc68f0fc24338884ee09aefece601a62f41f3b28d92f6dddcf5` |

Verificación con la biblioteca estándar de Python: cabecera PCM sin
compresión, mono, 16 bits y 44 100 Hz; 7 938 frames (180 ms); muestras inicial
y final en cero con rampas de entrada/salida cosenoidales de 12 ms; muestras
intermedias no silenciosas; pico medido de 11 468 (menor que el máximo PCM de
16 bits, 32 767), sin clipping; RMS medido de 7 763,8. Los dos recursos se
compararon por bytes y son distintos. Esto no constituye una escucha humana.
Los hashes se pueden recalcular con `shasum -a 256`.
