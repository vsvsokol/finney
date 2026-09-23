"""Собирает значок приложения из слоя Пушистика — как в кадре айдентики 1:2.

Зачем не вырезать значок из экспорта айдентики: там он ~260 px, а передний слой
адаптивного значка на xxxhdpi — 432 px. Растянутый вырез мылится, а у питомца
есть исходник в 2048 px.

Кадр найден совмещением значка из design/exports/ui/identity-1-2.png со слоем
pushistik_happy.png: квадрат со стороной ICON_SIDE вокруг ICON_CENTER. Рисунок
значка в айдентике чуть старше нынешнего питомца (цветы на капюшоне стоят иначе),
поэтому совпадение — по лицу, а не пиксель в пиксель.

Адаптивный значок — слой 108 dp, из которого лаунчер показывает середину 72 dp,
а остальное оставляет на параллакс. Поэтому видимый квадрат айдентики — это
средние 2/3 слоя, и вырез берётся в полтора раза шире.

Выход (WebP без потерь, PNG в ресурсы не кладём):
    mipmap-*dpi/ic_launcher_foreground.webp  — питомец на прозрачном
    mipmap-*dpi/ic_launcher_monochrome.webp  — тёмные линии для тематических значков
Фон — цвет капюшона, values/ic_launcher.xml: там, где кончается капюшон,
в параллаксе не видно шва.

Запуск из корня репозитория (нужен Pillow):
    python tools/make_app_icon.py
"""

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "design/exports/pet/pushistik/pushistik_happy.png"
RES = ROOT / "app/src/main/res"

# Промер по identity-1-2.png, в пикселях холста Пушистика (2048 × 2048).
ICON_CENTER = (1028, 896)
ICON_SIDE = 1040
LAYER_SIDE = ICON_SIDE * 108 // 72

DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

# Тёмнее этого — линия обводки или тёмные пряди: из них и складывается
# одноцветный силуэт для тематических значков Android 13.
INK_LUMA = 70


def crop_layer(pet: Image.Image) -> Image.Image:
    cx, cy = ICON_CENTER
    half = LAYER_SIDE // 2
    return pet.crop((cx - half, cy - half, cx + half, cy + half))


def monochrome(layer: Image.Image) -> Image.Image:
    rgba = np.asarray(layer).astype(np.float32)
    luma = rgba[..., :3] @ np.array([0.299, 0.587, 0.114], dtype=np.float32)
    ink = np.clip((INK_LUMA + 30 - luma) / 30, 0, 1) * (rgba[..., 3] / 255)
    out = np.zeros_like(rgba)
    out[..., :3] = 255
    out[..., 3] = ink * 255
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def main() -> None:
    pet = Image.open(SOURCE).convert("RGBA")
    layers = {"foreground": crop_layer(pet)}
    layers["monochrome"] = monochrome(layers["foreground"])
    for density, side in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(exist_ok=True)
        for name, layer in layers.items():
            path = folder / f"ic_launcher_{name}.webp"
            layer.resize((side, side), Image.LANCZOS).save(path, "WEBP", lossless=True, quality=100, method=6)
            print(path.relative_to(ROOT), f"{path.stat().st_size // 1024} КБ")


if __name__ == "__main__":
    main()
