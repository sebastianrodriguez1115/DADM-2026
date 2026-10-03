#!/usr/bin/env python3
"""Generate deterministic, original PCM tones for the X/O move effects."""

import math
import struct
import wave
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "app/src/main/res/raw"
RATE = 44_100
DURATION = 0.18
FADE = 0.012
PEAK = 0.35
TONES = {"game_move_x.wav": 880.0, "game_move_o.wav": 440.0}


def pcm_bytes(frequency: float) -> bytes:
    count = round(RATE * DURATION)
    fade_count = round(RATE * FADE)
    frames = bytearray()
    for n in range(count):
        envelope = 1.0
        if n < fade_count:
            envelope = 0.5 - 0.5 * math.cos(math.pi * n / fade_count)
        elif n >= count - fade_count:
            envelope = 0.5 - 0.5 * math.cos(math.pi * (count - 1 - n) / fade_count)
        sample = round(32767 * PEAK * envelope * math.sin(2 * math.pi * frequency * n / RATE))
        frames.extend(struct.pack("<h", sample))
    return bytes(frames)


def wav_bytes(frequency: float) -> bytes:
    import io

    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(pcm_bytes(frequency))
    return buffer.getvalue()


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name, frequency in TONES.items():
        data = wav_bytes(frequency)
        assert data == wav_bytes(frequency), "generation is not deterministic"
        (OUTPUT / name).write_bytes(data)
        print(f"Wrote {OUTPUT / name} ({len(data)} bytes, {frequency:g} Hz)")


if __name__ == "__main__":
    main()
