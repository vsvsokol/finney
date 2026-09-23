"""Готовит слои комнаты из экспорта дизайнеров в ресурсы приложения.

Зачем: дизайнер рисует комнату слоями на общем холсте 1440×2400, каждый предмет
уже стоит на своём месте в кадре. Класть такие слои в ресурсы как есть нельзя —
у семи из девяти больше 88% площади это прозрачный воздух, за который Android
всё равно платит четырьмя байтами на пиксель. Полными холстами комната съедала бы
66 МБ растра, обрезанная по содержимому — 14 МБ. Крайний случай: НЛО занимает
152×118 пикселей, а в полном холсте развернулось бы в те же 3.3 МБ, что и стена.

Обрезаем по непрозрачным пикселям, а положение сохраняем долями холста — так же,
как PetSkin хранит точки вращения конечностей. Скрипт печатает готовые RelRect:
числа уезжают в RoomLayers.kt копипастой, вручную их не переписывают.

Это та же беда, что была у питомцев (см. split_pet_base.py), только с другой
стороны: там лечили размером, здесь — обрезкой. drawable-nodpi сам по себе не
виноват, он тут и нужен: комната позиционируется долями и масштабируется кодом,
и автоматическое масштабирование по плотности экрана только мешало бы.

Вход:  design/exports/environment/*.PNG — как прислал дизайнер.
Выход: app/src/main/res/drawable-nodpi/room_*.webp + числа для RoomLayers.kt.

Без потерь, а не q90: края вырезаны по альфе, и лоссовая альфа даёт кайму по
контуру — на чёрной обводке комнаты её видно особенно.

Запуск из корня репозитория (нужен Pillow: pip install Pillow):
    py tools/pack_room.py

Перезапускать после каждого нового экспорта — и переносить напечатанные RelRect
в RoomLayers.kt, иначе предмет уедет с места.
"""

import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from measure_layers import ALPHA_FLOOR  # noqa: E402  — соседний модуль, путь добавлен строкой выше

SRC = Path("design/exports/environment")
RES = Path("app/src/main/res/drawable-nodpi")

# Холст, на котором дизайнер рисовал все слои. Доли считаются от него, а не от
# размера обрезанного файла: предмет должен вставать туда же, где стоял в кадре.
CANVAS = (1440, 2400)

# Ширина комнаты в ресурсах. 1080 — потому что на экране 1080p чёрная обводка
# толщиной 14–18 пикселей при уменьшении до 720 заметно мылится, а разница в
# памяти между 1080 и 720 здесь всего 6 МБ: слои обрезаны, платим только за
# содержимое. Масштаб общий для всех слоёв, иначе предметы разъедутся.
TARGET_WIDTH = 1080

# Исходное имя → имя в ресурсах. Папка drawable-nodpi общая с питомцами, там уже
# четыре десятка файлов, поэтому префикс room_ обязателен.
# bath_foam и bath_foam_2 — не кадры анимации, а задний и передний слой пены:
# питомец садится между ними, поэтому в именах back и front, а не 1 и 2.
LAYERS = {
    "room_backrooms": "room_back",
    "window_view_ufo": "room_window_ufo",
    "window_frame": "room_window_frame",
    "lamp": "room_lamp",
    "dinner_table": "room_table",
    "bath": "room_bath",
    "bath_foam": "room_bath_foam_back",
    "bath_foam_2": "room_bath_foam_front",
}


def pack(source: str, target: str) -> tuple[str, tuple[float, float, float, float], int]:
    path = next(SRC.glob(f"{source}.[pP][nN][gG]"))
    image = Image.open(path).convert("RGBA")
    if image.size != CANVAS:
        raise SystemExit(f"{path.name}: холст {image.size}, а слои комнаты ждут {CANVAS}")

    mask = image.split()[3].point(lambda v: 255 if v > ALPHA_FLOOR else 0)
    box = mask.getbbox()
    if box is None:
        raise SystemExit(f"{path.name}: ни одного непрозрачного пикселя")

    cropped = image.crop(box)
    scale = TARGET_WIDTH / CANVAS[0]
    size = (max(1, round(cropped.width * scale)), max(1, round(cropped.height * scale)))
    cropped = cropped.resize(size, Image.LANCZOS)

    # Слой, закрывающий весь холст, — это фон, и альфа-канал у него четверть веса
    # впустую. Полупрозрачных пикселей там быть не должно, но в экспорте их
    # приезжает под двенадцать тысяч с альфой до 150: видимо, след кисти по краю.
    # Сквозь фон смотреть некуда, он самый нижний, — гасим канал целиком.
    if box == (0, 0, CANVAS[0], CANVAS[1]):
        cropped = cropped.convert("RGB")

    out = RES / f"{target}.webp"
    cropped.save(out, "WEBP", lossless=True, method=6)

    rel = (box[0] / CANVAS[0], box[1] / CANVAS[1], box[2] / CANVAS[0], box[3] / CANVAS[1])
    return target, rel, out.stat().st_size


# Звёздное небо за окном медленно плывёт (RoomScene.kt, WindowSky), поэтому ему
# нужна полоса, которая стыкуется сама с собой. window_view — не такая полоса:
# это одна картинка с неровным рисованным краем. Но фон у неба ровный
# (#261F46), и если резать по столбцам без звёзд, левый край полосы совпадает
# с правым пиксель в пиксель — шва на стыке нет. Неровный край и прозрачные
# углы заливаются тем же фоном: их всё равно закрывает рама.
#
# Столбцы подобраны по экспорту: звёзды занимают 693..1210, края полосы
# отступают от них на 33 и 30 пикселей — примерно как звёзды друг от друга,
# и на стыке не видно ни пустой полосы, ни тесноты.
SKY_SOURCE = "window_view"
SKY_TARGET = "room_window_sky"
SKY_COLOR = (0x26, 0x1F, 0x46)
SKY_COLUMNS = (660, 1240)


def pack_sky() -> tuple[str, tuple[float, float, float, float], int]:
    path = next(SRC.glob(f"{SKY_SOURCE}.[pP][nN][gG]"))
    image = Image.open(path).convert("RGBA")
    if image.size != CANVAS:
        raise SystemExit(f"{path.name}: холст {image.size}, а слои комнаты ждут {CANVAS}")

    top, bottom = image.getbbox()[1::2]
    left, right = SKY_COLUMNS
    strip = image.crop((left, top, right, bottom))
    for x in (0, strip.width - 1):
        column = [strip.getpixel((x, y)) for y in range(strip.height)]
        if any(p[3] > ALPHA_FLOOR and p[:3] != SKY_COLOR for p in column):
            raise SystemExit(f"{path.name}: на краю полосы неба ({left + x}) звезда — на стыке будет шов")

    flat = Image.new("RGBA", strip.size, SKY_COLOR + (255,))
    flat.alpha_composite(strip)
    scale = TARGET_WIDTH / CANVAS[0]
    flat = flat.convert("RGB").resize((round(strip.width * scale), round(strip.height * scale)), Image.LANCZOS)

    out = RES / f"{SKY_TARGET}.webp"
    flat.save(out, "WEBP", lossless=True, method=6)
    rel = (left / CANVAS[0], top / CANVAS[1], right / CANVAS[0], bottom / CANVAS[1])
    return SKY_TARGET, rel, out.stat().st_size


def main() -> None:
    # Git Bash на Windows отдаёт stdout в cp1251, и кириллица в выводе бьётся.
    sys.stdout.reconfigure(encoding="utf-8")

    RES.mkdir(parents=True, exist_ok=True)
    packed = [pack(source, target) for source, target in LAYERS.items()] + [pack_sky()]

    total_disk = sum(size for _, _, size in packed)
    print(f"Упаковано {len(packed)} слоёв, {total_disk // 1024} КБ на диске\n")
    print("Для RoomLayers.kt:\n")
    for target, rel, _ in packed:
        camel = "".join(part.capitalize() for part in target.removeprefix("room_").split("_")) or "Back"
        print(f"    val {camel} = RoomLayer(R.drawable.{target}, RelRect({rel[0]:.4f}f, {rel[1]:.4f}f, {rel[2]:.4f}f, {rel[3]:.4f}f))")


if __name__ == "__main__":
    main()
