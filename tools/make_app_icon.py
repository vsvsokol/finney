"""Собирает значок приложения из design/icon.svg — утверждённого значка «Финни».

В icon.svg лежит PNG 1000 × 1000 и матрица, которая вырезает из него кадр
258 × 261: лицо питомца крупно. Кадр считается из той же матрицы, а не
промеряется вручную, — поменяют значок в Figma, скрипт возьмёт новый кадр.

Адаптивный значок — слой 108 dp, из которого лаунчер показывает середину,
а остальное оставляет на параллакс. По спецификации это 72 dp (2/3 слоя), но
Pixel Launcher в круглой маске показывает больше — около 0.72 слоя, промерено
по скриншоту эмулятора. Кадр макета ложится ровно в эту видимую часть: значок
на телефоне такой же, как в icon.svg, без лишней каймы вокруг лица.

Выход (WebP без потерь, PNG в ресурсы не кладём):
    mipmap-*dpi/ic_launcher_foreground.webp  — питомец на прозрачном
    mipmap-*dpi/ic_launcher_monochrome.webp  — тёмные линии для тематических значков
Фон — кремовый, как у значка в icon.svg: values/ic_launcher.xml.

Запуск из корня репозитория (нужны Pillow и numpy):
    python tools/make_app_icon.py
"""

import base64
import io
import re
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "design/icon.svg"
RES = ROOT / "app/src/main/res"

# Какая доля слоя видна в маске лаунчера (см. выше).
VISIBLE_SHARE = 0.72

DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

# Тёмнее этого — линия обводки или тёмные пряди: из них и складывается
# одноцветный силуэт для тематических значков Android 13.
INK_LUMA = 70


def load_icon() -> tuple[Image.Image, tuple[float, float, float, float]]:
    """PNG из icon.svg и видимый в значке кадр в его пикселях: (лево, верх, право, низ)."""
    svg = SOURCE.read_text(encoding="utf-8")
    png = base64.b64decode(re.search(r'base64,([^"]+)"', svg).group(1))
    # <use transform="matrix(sx 0 0 sy tx ty)"> в единицах objectBoundingBox:
    # кадр 0..1 значка — это пиксели (-tx / sx) .. ((1 - tx) / sx) картинки.
    sx, _, _, sy, tx, ty = map(float, re.search(r'matrix\(([^)]+)\)', svg).group(1).split())
    frame = (-tx / sx, -ty / sy, (1 - tx) / sx, (1 - ty) / sy)
    return Image.open(io.BytesIO(png)).convert("RGBA"), frame


def crop_layer(icon: Image.Image, frame: tuple[float, float, float, float]) -> Image.Image:
    left, top, right, bottom = frame
    cx, cy = (left + right) / 2, (top + bottom) / 2
    half = max(right - left, bottom - top) / VISIBLE_SHARE / 2
    return icon.crop(tuple(round(v) for v in (cx - half, cy - half, cx + half, cy + half)))


def monochrome(layer: Image.Image) -> Image.Image:
    rgba = np.asarray(layer).astype(np.float32)
    luma = rgba[..., :3] @ np.array([0.299, 0.587, 0.114], dtype=np.float32)
    ink = np.clip((INK_LUMA + 30 - luma) / 30, 0, 1) * (rgba[..., 3] / 255)
    out = np.zeros_like(rgba)
    out[..., :3] = 255
    out[..., 3] = ink * 255
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def main() -> None:
    icon, frame = load_icon()
    layers = {"foreground": crop_layer(icon, frame)}
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
