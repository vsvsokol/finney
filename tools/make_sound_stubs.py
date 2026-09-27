"""Заглушки звуков, которых ещё нет: синтезированные бипы с настоящими именами.

Код уже играет каждый звук из списка ниже. Пока звуковик не прислал файл, в
app/src/main/res/raw/ лежит заглушка — узнаваемый синтетический звук той же длины
и того же смысла. Настоящий файл кладётся поверх с тем же именем, код не меняется.

Скрипт пишет только те файлы, которых в res/raw ещё нет: готовые звуки он не
перезапишет. Чтобы пересоздать заглушку, удалите её файл и запустите заново.

Нужны numpy и ffmpeg в PATH. Запуск из корня репозитория:

    python tools/make_sound_stubs.py
"""

import subprocess
from pathlib import Path

import numpy as np

RAW = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"
RATE = 44100


def silence(ms):
    return np.zeros(int(RATE * ms / 1000))


def tone(freq, ms, decay=8.0, harmonics=(1.0, 0.3, 0.1), attack_ms=3):
    """Колокольчик: синус с обертонами и экспоненциальным затуханием."""
    t = np.arange(int(RATE * ms / 1000)) / RATE
    wave = sum(a * np.sin(2 * np.pi * freq * (i + 1) * t) for i, a in enumerate(harmonics))
    env = np.exp(-decay * t / (ms / 1000))
    attack = min(len(t), int(RATE * attack_ms / 1000))
    env[:attack] *= np.linspace(0, 1, attack)
    return wave * env


def noise(ms, low, high, seed=1):
    """Полосовой шум: шорох, всплеск, вжух."""
    n = int(RATE * ms / 1000)
    spectrum = np.fft.rfft(np.random.default_rng(seed).standard_normal(n))
    freqs = np.fft.rfftfreq(n, 1 / RATE)
    spectrum[(freqs < low) | (freqs > high)] = 0
    return np.fft.irfft(spectrum, n)


def seq(*parts):
    return np.concatenate(parts)


def mix(*parts):
    out = np.zeros(max(len(p) for p in parts))
    for p in parts:
        out[: len(p)] += p
    return out


def hump(n, peak=0.5):
    """Огибающая «нарастает и гаснет», пик на доле [peak] длины."""
    x = np.linspace(0, 1, n)
    return np.where(x < peak, x / peak, (1 - x) / (1 - peak))


def coin():
    return seq(tone(1319, 70, decay=5), tone(1760, 220, decay=6))


def purchase():
    rattle = noise(120, 3000, 9000, seed=2) * hump(int(RATE * 0.12), 0.1) * 0.5
    return seq(rattle, mix(tone(1047, 350, decay=5), tone(1319, 350, decay=5), tone(1568, 350, decay=5)))


def game_correct():
    return seq(tone(1047, 80, decay=4), tone(1568, 200, decay=6))


def game_wrong():
    # Мягко и без упрёка: два тихих круглых тона вниз, без жужжания.
    soft = (1.0, 0.15)
    return seq(tone(523, 110, decay=4, harmonics=soft), tone(392, 220, decay=5, harmonics=soft)) * 0.6


def game_win():
    notes = [523, 659, 784]
    return seq(*[tone(f, 110, decay=3) for f in notes], mix(tone(1047, 700, decay=5), tone(784, 700, decay=5) * 0.5))


def game_fail():
    # «Почти!»: тёплое мажорное завершение, а не проигрыш.
    soft = (1.0, 0.2)
    return seq(
        tone(659, 140, decay=3, harmonics=soft),
        tone(587, 140, decay=3, harmonics=soft),
        mix(tone(523, 600, decay=5, harmonics=soft), tone(392, 600, decay=5, harmonics=soft) * 0.6),
    ) * 0.7


def level_up():
    run = [tone(f, 70, decay=3) for f in (523, 659, 784, 1047, 1319)]
    chord = mix(*[tone(f, 1100, decay=5) for f in (1047, 1319, 1568)])
    sparkle = seq(silence(250), tone(2637, 120, decay=5) * 0.3, tone(3136, 160, decay=5) * 0.3)
    return seq(*run, mix(chord, sparkle))


def wash_clean():
    return seq(*[tone(f, 70, decay=4, harmonics=(1.0,)) for f in (2093, 2637, 3136)], tone(4186, 200, decay=6, harmonics=(1.0,))) * 0.6


def capsule_door():
    slide = noise(220, 200, 1500, seed=4) * hump(int(RATE * 0.22), 0.7) * 0.4
    thud = tone(110, 160, decay=6, harmonics=(1.0, 0.5))
    return seq(slide, thud)


def ufo():
    t = np.arange(int(RATE * 1.4)) / RATE
    freq = 700 + 250 * np.sin(2 * np.pi * 0.6 * t) + 30 * np.sin(2 * np.pi * 7 * t)
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    return np.sin(phase) * hump(len(t), 0.5) * 0.35


def pour():
    stream = noise(420, 600, 2500, seed=5) * hump(int(RATE * 0.42), 0.2) * 0.35
    blips = seq(*[seq(silence(40), tone(f, 60, decay=6, harmonics=(1.0,)) * 0.3) for f in (520, 610, 700, 820, 900)])
    return mix(stream, blips)


STUBS = {
    "sfx_coin": coin,
    "sfx_purchase": purchase,
    "sfx_game_correct": game_correct,
    "sfx_game_wrong": game_wrong,
    "sfx_game_win": game_win,
    "sfx_game_fail": game_fail,
    "sfx_level_up": level_up,
    "sfx_wash_clean": wash_clean,
    "sfx_capsule_door": capsule_door,
    "sfx_ufo": ufo,
    "sfx_pour": pour,
}


def write_ogg(samples, path, peak_db=-4.0):
    samples = samples * (10 ** (peak_db / 20) / np.abs(samples).max())
    fade = min(len(samples) // 4, int(RATE * 0.01))
    samples[-fade:] *= np.linspace(1, 0, fade)
    subprocess.run(
        ["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ac", "1", "-ar", str(RATE), "-i", "-",
         "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1", str(path)],
        input=samples.astype(np.float32).tobytes(),
        check=True,
    )


def main():
    RAW.mkdir(parents=True, exist_ok=True)
    for name, make in STUBS.items():
        path = RAW / f"{name}.ogg"
        if path.exists():
            print(f"есть     {path.name}")
            continue
        write_ogg(make(), path)
        print(f"заглушка {path.name}")


if __name__ == "__main__":
    main()
