"""Готовит слои питомца из экспорта дизайнера в ресурсы приложения.

Зачем: дизайнер экспортирует состояния лица (pushistik_happy/sad/dirty/sleep) вместе
с уже впечатанными руками и ногами. Чтобы шевелить конечностями отдельно, из
состояния нужно вычесть те же руки и ноги — иначе под повёрнутой рукой видно
исходную. Ноги и руки обычного размера в состояниях совпадают пиксель в пиксель
с отдельными слоями, поэтому вычитание по альфе даёт чистый результат.

Вход:  design/exports/pet/*.png — как прислал дизайнер (docs/assets-spec.md).
Выход: app/src/main/res/drawable-nodpi/*.webp — только то, что рисует PetView.

PNG в ресурсы не кладём: те же картинки в WebP без потерь весят втрое меньше.
Без потерь, а не q90: базовые слои получены вычитанием по альфе, и лоссовая
альфа даёт кайму по краю выреза — её видно из-под повёрнутой руки.

Файлы стадий роста (_middle, _big) и туловище остаются только в design/exports:
код их пока не рисует, в APK им делать нечего.

Запуск из корня репозитория (нужен Pillow: pip install Pillow):
    python tools/split_pet_base.py

Перезапускать после каждого нового экспорта от дизайнера.
"""

from pathlib import Path

from PIL import Image, ImageChops

SRC = Path("design/exports/pet")
RES = Path("app/src/main/res/drawable-nodpi")
CANVAS = 2048
STATES = ("happy", "sad", "dirty", "sleep")
LIMBS = ("left_leg", "right_leg", "left_hand", "right_hand")


def load(name: str) -> Image.Image:
    return Image.open(SRC / f"pushistik_{name}.png").convert("RGBA")


def save(image: Image.Image, name: str) -> None:
    out = RES / f"pushistik_{name}.webp"
    image.save(out, "WEBP", lossless=True, method=6)
    print(f"{out} — {out.stat().st_size // 1024} КБ")


def main() -> None:
    RES.mkdir(parents=True, exist_ok=True)

    limbs = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    for limb in LIMBS:
        image = load(limb)
        limbs.alpha_composite(image)
        save(image, limb)
    limb_alpha = limbs.split()[3]

    for state in STATES:
        src = load(state)
        base = src.copy()
        base.putalpha(ImageChops.subtract(src.split()[3], limb_alpha))
        save(base, f"base_{state}")


if __name__ == "__main__":
    main()
