"""Промеряет PNG от дизайнеров: где слой лежит на холсте, каким цветом, какой обводкой.

Зачем: макеты переносились на глаз, и числа расходились с оригиналом. Кремовый фон
панелей полгода стоял `#FDF0D5`, угаданный по экрану, вместо настоящего `#FFEDCD`.
Блик на кнопке был «примерно 0.24 на 0.15» вместо промеренных 0.283 × 0.200. На
комнате три оценки положения мебели разошлись с замером на 5–12% высоты холста.

Глазу верить нельзя, линейке — можно. Скрипт печатает таблицу, число из неё
уезжает в код без пересказа.

Ничего не пишет и не трогает: только читает PNG и печатает. Поэтому его можно
гонять на любой папке, в том числе чужой, ничем не рискуя. Конвертацией занимаются
соседи — split_pet_base.py и pack_room.py.

Запуск из корня репозитория (нужен Pillow: pip install Pillow):
    py tools/measure_layers.py design/exports/environment
    py tools/measure_layers.py design/exports/ui --format kotlin

Полезно при каждом новом экспорте: сравнить числа со старыми и увидеть, что
дизайнер подвинул, — в глаза это не бросается.
"""

import argparse
import statistics
import sys
from collections import Counter
from pathlib import Path

from PIL import Image

# Пиксель считается «есть», если альфа выше порога. Не ноль: у сглаженного края
# тянется хвост в одну-две альфы, невидимый глазу, и bbox по нулю выходит на
# пару пикселей шире настоящей картинки.
ALPHA_FLOOR = 8

# Сколько цветов оставить при квантовании перед подсчётом доминирующих. Без
# квантования сглаживание и лёгкий шум дают тысячи почти одинаковых оттенков,
# и топ забивается соседями одного и того же цвета (#9F7560, #A07560, #9E745F…).
QUANT_COLORS = 32

# Пиксель тёмный, если самый яркий его канал ниже этого. Обводка в экспортах
# либо чёрная, либо #382C92 — оба проходят с запасом, заливки не проходят.
DARK_MAX = 70

# Горизонтальный отрезок тёмных пикселей длиннее этого — не обводка, а заливка
# (тёмная крышка стола, тень). В подсчёт толщины такие не идут.
STROKE_RUN_LIMIT = 60


def measure(path: Path, canvas: tuple[int, int], alpha_floor: int, top: int) -> dict:
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    alpha = image.split()[3]

    mask = alpha.point(lambda v: 255 if v > alpha_floor else 0)
    box = mask.getbbox()
    opaque = sum(mask.get_flattened_data()) // 255

    return {
        "name": path.name,
        "size": (width, height),
        "canvas_mismatch": (width, height) != canvas,
        "box": box,
        "rel": rel_box(box, canvas) if box else None,
        "coverage": opaque / (width * height),
        "colors": dominant_colors(image, mask, top),
        "stroke": stroke_width(image, mask),
    }


def rel_box(box: tuple[int, int, int, int], canvas: tuple[int, int]) -> tuple[float, float, float, float]:
    """Положение в долях холста, а не своего файла.

    Доли считаются от общего холста намеренно: слой, обрезанный по своему
    содержимому, должен вставать на то же место, где стоял в полном кадре.
    Эти же четыре числа уходят в RelRect на стороне Compose.
    """
    width, height = canvas
    return (box[0] / width, box[1] / height, box[2] / width, box[3] / height)


def dominant_colors(image: Image.Image, mask: Image.Image, top: int) -> list[tuple[str, float]]:
    """Какими цветами нарисован слой — по убыванию доли, прозрачное не в счёт."""
    quantized = image.convert("RGB").quantize(colors=QUANT_COLORS, method=Image.Quantize.FASTOCTREE)
    palette = quantized.getpalette()

    counts = Counter(
        index
        for index, visible in zip(quantized.get_flattened_data(), mask.get_flattened_data())
        if visible
    )
    total = sum(counts.values())
    if not total:
        return []

    return [
        ("#%02X%02X%02X" % tuple(palette[index * 3 : index * 3 + 3]), count / total)
        for index, count in counts.most_common(top)
    ]


def stroke_width(image: Image.Image, mask: Image.Image) -> tuple[int, str] | None:
    """Толщина обводки — медиана длин горизонтальных отрезков тёмных пикселей.

    Медиана, а не среднее: одна длинная тёмная полоса внизу объекта перекосила бы
    среднее вдвое, а медиану — нет. Отрезки считаются по строкам, потому что
    вертикальный участок контура пересекает строку ровно на свою толщину.
    """
    width, height = image.size
    pixels = image.convert("RGB").get_flattened_data()
    visible = mask.get_flattened_data()

    runs: list[int] = []
    darkest = Counter()
    # Каждая восьмая строка: на толщину это не влияет, а времени уходит втрое меньше.
    for y in range(0, height, 8):
        run = 0
        for x in range(y * width, y * width + width):
            red, green, blue = pixels[x]
            if visible[x] and max(red, green, blue) < DARK_MAX:
                run += 1
                darkest["#%02X%02X%02X" % (red, green, blue)] += 1
            else:
                if 0 < run <= STROKE_RUN_LIMIT:
                    runs.append(run)
                run = 0
        if 0 < run <= STROKE_RUN_LIMIT:
            runs.append(run)

    if not runs:
        return None
    return round(statistics.median(runs)), darkest.most_common(1)[0][0]


def print_table(rows: list[dict], canvas: tuple[int, int]) -> None:
    print(f"Холст {canvas[0]}x{canvas[1]}\n")
    for row in rows:
        mark = "  !размер другой" if row["canvas_mismatch"] else ""
        print(f"{row['name']}   {row['size'][0]}x{row['size'][1]}{mark}")

        if row["box"] is None:
            print("  пусто — ни одного непрозрачного пикселя\n")
            continue

        left, top, right, bottom = row["box"]
        rl, rt, rr, rb = row["rel"]
        print(f"  bbox px     {left}, {top} .. {right}, {bottom}   ({right - left} x {bottom - top})")
        print(f"  bbox доли   {rl:.4f}, {rt:.4f} .. {rr:.4f}, {rb:.4f}")
        print(f"  покрытие    {row['coverage'] * 100:.1f}% непрозрачных")

        if row["colors"]:
            shown = "   ".join(f"{hex_} {share * 100:4.1f}%" for hex_, share in row["colors"])
            print(f"  цвета       {shown}")
        if row["stroke"]:
            thickness, color = row["stroke"]
            print(f"  обводка     ~{thickness} px ({thickness / canvas[0] * 100:.2f}% ширины)  {color}")
        print()


def print_kotlin(rows: list[dict]) -> None:
    """Готовые строки для RelRect — чтобы числа попадали в код копипастой, без переписывания."""
    for row in rows:
        if row["rel"] is None:
            continue
        rl, rt, rr, rb = row["rel"]
        name = Path(row["name"]).stem
        camel = "".join(part.capitalize() for part in name.replace("-", "_").split("_"))
        print(f"    val {camel} = RoomLayer(R.drawable.{name}, RelRect({rl:.4f}f, {rt:.4f}f, {rr:.4f}f, {rb:.4f}f))")


def print_csv(rows: list[dict]) -> None:
    print("файл,ширина,высота,left,top,right,bottom,покрытие")
    for row in rows:
        if row["rel"] is None:
            continue
        rl, rt, rr, rb = row["rel"]
        print(
            f"{row['name']},{row['size'][0]},{row['size'][1]},"
            f"{rl:.4f},{rt:.4f},{rr:.4f},{rb:.4f},{row['coverage']:.4f}"
        )


def main() -> None:
    # Git Bash на Windows отдаёт stdout в cp1251, и кириллица в выводе бьётся.
    sys.stdout.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(description="Замер PNG: bbox, палитра, обводка.")
    parser.add_argument("folder", type=Path, help="папка с PNG")
    parser.add_argument("--canvas", type=int, nargs=2, metavar=("W", "H"), help="эталонный холст; по умолчанию самый большой файл")
    parser.add_argument("--top", type=int, default=6, help="сколько доминирующих цветов печатать")
    parser.add_argument("--format", choices=("table", "kotlin", "csv"), default="table")
    parser.add_argument("--alpha", type=int, default=ALPHA_FLOOR, help="порог непрозрачности")
    args = parser.parse_args()

    files = sorted(p for p in args.folder.iterdir() if p.suffix.lower() == ".png")
    if not files:
        raise SystemExit(f"{args.folder}: ни одного PNG")

    if args.canvas:
        canvas = tuple(args.canvas)
    else:
        sizes = [Image.open(p).size for p in files]
        canvas = (max(s[0] for s in sizes), max(s[1] for s in sizes))

    rows = [measure(path, canvas, args.alpha, args.top) for path in files]

    if args.format == "kotlin":
        print_kotlin(rows)
    elif args.format == "csv":
        print_csv(rows)
    else:
        print_table(rows, canvas)


if __name__ == "__main__":
    main()
