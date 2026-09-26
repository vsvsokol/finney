"""Переносит рисунки предметов из экспорта дизайнеров в ресурсы приложения.

Берутся только предметы, которые есть в магазине (assets/content/shop.json)
или на которые ссылается поле "art" в мини-играх (assets/content/tasks.json):
рисунок без ссылки в игре не покажется, и в APK ему делать нечего.
Добавят предмет в магазин или мини-игру — перезапустить скрипт, рисунок подтянется сам.

Холст у предметов 2000 × 2000, а сам предмет занимает его середину. Слой
обрезается по непрозрачным пикселям и уменьшается до SIDE по большей стороне:
в руке у ребёнка предмет не крупнее 120 dp, то есть ~400 px на xxxhdpi, а
в полный холст nodpi развернулся бы на 16 МБ (см. split_pet_base.py, SIDE).

Вход:  design/exports/items/item_<id>.png, аксессуары — acc_<id>.png
Выход: app/src/main/res/drawable-nodpi/item_<id>.webp, acc_<id>.webp (WebP без потерь)

Аксессуар («kind": "accessory» в shop.json) надевается на питомца и рисуется поверх
него на всю ширину головы, поэтому ужимается не до SIDE, а до ACCESSORY_SIDE.

Запуск из корня репозитория (нужен Pillow):
    python tools/pack_items.py
"""

import json
from pathlib import Path

from PIL import Image

SRC = Path("design/exports/items")
RES = Path("app/src/main/res/drawable-nodpi")
SHOP = Path("app/src/main/assets/content/shop.json")
TASKS = Path("app/src/main/assets/content/tasks.json")
SIDE = 256
ACCESSORY_SIDE = 640


def art_names(node) -> set[str]:
    """Все значения "art" в JSON мини-игр, на любой глубине."""
    if isinstance(node, dict):
        found = {node["art"]} if isinstance(node.get("art"), str) else set()
        return found.union(*(art_names(v) for v in node.values()))
    if isinstance(node, list):
        return set().union(*(art_names(v) for v in node))
    return set()


def main() -> None:
    shop = json.loads(SHOP.read_text(encoding="utf-8"))
    names = {f"item_{item['id']}" for item in shop if item.get("kind") != "accessory"}
    names |= art_names(json.loads(TASKS.read_text(encoding="utf-8")))
    accessories = {f"acc_{item['id']}" for item in shop if item.get("kind") == "accessory"}
    for name in sorted(names | accessories):
        side = ACCESSORY_SIDE if name in accessories else SIDE
        src = SRC / f"{name}.png"
        if not src.exists():
            print(f"{name}: рисунка нет — в игре будет значок")
            continue
        image = Image.open(src).convert("RGBA")
        image = image.crop(image.getbbox())
        image.thumbnail((side, side), Image.LANCZOS)
        out = RES / f"{name}.webp"
        image.save(out, "WEBP", lossless=True, method=6)
        print(f"{out} — {image.size[0]}×{image.size[1]}, {out.stat().st_size // 1024} КБ")


if __name__ == "__main__":
    main()
