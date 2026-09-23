"""Готовит слои питомцев из экспорта дизайнеров в ресурсы приложения.

Зачем: дизайнер экспортирует состояния лица (happy/sad/dirty/sleep) вместе с уже
впечатанными руками и ногами. Чтобы шевелить конечностями отдельно, из состояния
нужно убрать те же руки и ноги — иначе под повёрнутой рукой видно исходную.
Ноги и руки обычного размера в состояниях совпадают пиксель в пиксель с
отдельными слоями, поэтому вырез получается точным.

Вырезаем не вычитанием альфы, а обратным альфа-смешиванием — см. [uncomposite].
Вычитание давало по контуру рук и ног светлый шов в пиксель шириной: на сглаженном
краю альфа делится между рукой и телом, и при обратной сборке ни одна из половин
не восстанавливала полную непрозрачность.

Вход:  design/exports/pet/<питомец>/*.png — как прислал дизайнер (docs/assets-spec.md).
Выход: app/src/main/res/drawable-nodpi/*.webp — только то, что рисует PetView.

Питомцев несколько, у каждого своя папка и свой размер холста; отличаются только
именами файлов конечностей, поэтому весь разбор описан в PETS.

PNG в ресурсы не кладём: те же картинки в WebP без потерь весят втрое меньше.
Без потерь, а не q90: базовые слои вырезаны по альфе, и лоссовая альфа даёт кайму
по краю выреза — её видно из-под повёрнутой руки.

Файлы стадий роста (_middle, _big) и туловище остаются только в design/exports:
код их пока не рисует, в APK им делать нечего.

Здесь же собираются слои моргания — см. [blink] — и открытого рта — см. [mouth].

Запуск из корня репозитория (нужны Pillow и scipy: pip install Pillow scipy):
    python tools/split_pet_base.py

Перезапускать после каждого нового экспорта от дизайнера.
"""

from pathlib import Path

import numpy as np
from PIL import Image, ImageMath
from scipy import ndimage

SRC = Path("design/exports/pet")
RES = Path("app/src/main/res/drawable-nodpi")

# Сторона слоя в ресурсах. Холсты у дизайнеров 2048 и 2200, и класть их в
# drawable-nodpi как есть нельзя: nodpi запрещает Android масштабировать картинку
# при загрузке, поэтому слой разворачивается в памяти целиком — 2200×2200×4 ≈ 19 МБ.
# На питомца пять слоёв, а на экране подбора их двое: выходило под 190 МБ растра
# ради кружка в 400 пикселей, и кадр падал до 200 мс (5 fps) при 60 на главном.
# 512 хватает с запасом: крупнее всего питомца рисуют примерно в 400 px.
SIDE = 512
STATES = ("happy", "sad", "dirty", "sleep")
LIMBS = ("left_leg", "right_leg", "left_hand", "right_hand")

# Имя питомца → суффикс файлов конечностей обычного размера. У Пушистика он пустой
# (pushistik_left_hand.png), у Рогатика размер указан явно (rogatik_left_hand_small.png).
PETS = {"pushistik": "", "rogatik": "_small", "zvezdochka": "", "bantik": "", "luchik": ""}


def load(pet: str, name: str) -> Image.Image:
    return Image.open(SRC / pet / f"{pet}_{name}.png").convert("RGBA")


def save(image: Image.Image, pet: str, name: str) -> None:
    out = RES / f"{pet}_{name}.webp"
    if max(image.size) > SIDE:
        image = image.resize((SIDE, SIDE), Image.LANCZOS)
    image.save(out, "WEBP", lossless=True, method=6)
    print(f"{out} — {out.stat().st_size // 1024} КБ")


def uncomposite(state_alpha: Image.Image, limb_alpha: Image.Image) -> Image.Image:
    """Альфа тела: сколько его должно остаться, чтобы рука поверх дала исходный кадр.

    Рука кладётся на тело обычным альфа-смешиванием: state = limb + base * (1 - limb).
    Отсюда base = (state - limb) / (1 - limb). Наивное `state - limb` — это тот же
    числитель без делителя, и на сглаженном крае (limb = 0.5 при state = 1) оно даёт
    тело в половину непрозрачности вместо полной: рука возвращает свои 0.5, тело —
    половину от оставшегося, вместе 0.75, и по контуру видно светлый шов.

    Там, где рука полностью непрозрачна, тело не видно никогда и делитель обращается
    в ноль — оставляем вырез (альфа 0), иначе под повёрнутой рукой покажется исходная.
    """
    return ImageMath.lambda_eval(
        lambda v: v["convert"](
            v["min"]((v["s"] - v["l"]) * 255.0 / v["max"](255.0 - v["l"], 1.0), 255.0),
            "L",
        ),
        s=state_alpha.convert("F"),
        l=limb_alpha.convert("F"),
    )


def split(pet: str, limb_suffix: str) -> None:
    limbs = None
    for limb in LIMBS:
        image = load(pet, limb + limb_suffix)
        if limbs is None:
            limbs = Image.new("RGBA", image.size, (0, 0, 0, 0))
        limbs.alpha_composite(image)
        # В ресурсах суффикс размера не нужен: рисуется только обычный.
        save(image, pet, limb)
    limb_alpha = limbs.split()[3]

    for state in STATES:
        src = load(pet, state)
        base = src.copy()
        base.putalpha(uncomposite(src.split()[3], limb_alpha))
        check_seamless(base, limbs, src, pet, state)
        save(base, pet, f"base_{state}")


def check_seamless(base: Image.Image, limbs: Image.Image, src: Image.Image, pet: str, state: str) -> None:
    """Собранный обратно кадр должен совпасть с тем, что прислал дизайнер.

    Стережёт от возврата шва: если вырез когда-нибудь снова начнёт «съедать» альфу по
    контуру, силуэт разойдётся с оригиналом и сборка упадёт здесь, а не в приложении.
    """
    rebuilt = Image.new("RGBA", src.size, (0, 0, 0, 0))
    rebuilt.alpha_composite(limbs)
    rebuilt.alpha_composite(base)
    worst = max(
        abs(a - b)
        for a, b in zip(rebuilt.split()[3].get_flattened_data(), src.split()[3].get_flattened_data())
    )
    # 1 — округление при обратном смешивании. Шов от вычитания давал 64.
    if worst > 1:
        raise SystemExit(f"{pet} {state}: силуэт разошёлся с оригиналом на {worst} из 255 — шов по контуру")


# Маска моргания в пикселях слоя (SIDE): до BLINK_CORE от глаз — сплошная, дальше
# за BLINK_FEATHER сходит на нет. Ядро Lanczos при уменьшении размазывает разницу
# глаз пиксели на три, сплошная часть это накрывает, а растушёвка лежит там, где
# открытые и закрытые глаза уже совпадают до бита.
BLINK_CORE = 4
BLINK_FEATHER = 4
# Столько же отступаем от рта: он в моргании остаётся от настроения.
BLINK_MOUTH_GAP = 3


def blink(pet: str) -> None:
    """Слой моргания: закрытые глаза из сна, положенные поверх открытых.

    Отдельных слоёв глаз дизайнеры не присылают, а у Пушистика нет и слоя лица.
    Зато открытые и закрытые глаза уже есть в состояниях: sleep отличается от happy
    только глазами и ртом. Разница этих двух кадров и есть маска глаз — рот из неё
    выкидываем, пусть во время моргания остаётся от текущего настроения.

    Слой на каждое настроение свой, потому что брови грусти и грязь лежат поверх глаз:
    там, где настроение отличается от happy, в слое остаётся пиксель настроения,
    а в остальной маске — пиксель сна.

    Маска накладывается на уже уменьшенные кадры, а не уменьшается вместе с ними.
    Жёсткий край альфы Lanczos раскачивает: у почти прозрачных пикселей цвет после
    обратного деления на альфу уходит в белое, и на каждом моргании вокруг глаз
    вспыхивал светлый контур маски.
    """
    full = {state: load(pet, state) for state in STATES}
    changed = np.abs(pixels(full["happy"]) - pixels(full["sleep"])).max(axis=2) > 0

    # Разница распадается на три пятна: два глаза и рот. Каждый глаз нарисован
    # несколькими штрихами, поэтому пятна ищем по чуть расширенной разнице. Рот
    # отделять по столбцам нельзя: у Звёздочки он заходит под глаз по горизонтали.
    parts, count = ndimage.label(ndimage.binary_dilation(changed, iterations=12))
    if count != 3:
        raise SystemExit(f"{pet}: ждали глаз, глаз и рот — нашлось {count} частей лица")
    sizes = ndimage.sum(changed, parts, range(1, count + 1))
    eyes = np.isin(parts, np.argsort(sizes)[-2:] + 1) & changed
    mouth = changed & ~eyes

    to_eyes = ndimage.distance_transform_edt(~shrink_mask(eyes))
    to_mouth = ndimage.distance_transform_edt(~shrink_mask(mouth))
    mask = np.clip((BLINK_CORE + BLINK_FEATHER - to_eyes) / BLINK_FEATHER, 0.0, 1.0)
    mask *= np.clip((to_mouth - BLINK_MOUTH_GAP) / BLINK_MOUTH_GAP, 0.0, 1.0)

    # Полоса глаз по высоте — в её пределах в PetSkin опускается веко (eyesTop, eyesBottom).
    rows = np.flatnonzero((mask > 0).any(axis=1))
    print(f"{pet}: глаза по высоте {rows[0]}..{rows[-1] + 1} из {SIDE}")

    frames = {state: pixels(shrink(image)) for state, image in full.items()}
    for state in ("happy", "sad", "dirty"):
        mood = frames[state]
        own = np.abs(mood - frames["happy"]).max(axis=2) > 0
        layer = np.where(own[..., None], mood, frames["sleep"])
        layer[..., 3] = np.rint(layer[..., 3] * mask)
        save(Image.fromarray(layer.astype(np.uint8), "RGBA"), pet, f"blink_{state}")


# Маска рта в пикселях слоя: сплошная до MOUTH_CORE от любого из ртов, дальше
# за MOUTH_FEATHER сходит на нет. Запас нужен небольшой: слой при еде растягивают,
# а до линии подбородка у Пушистика от рта всего пикселей пять.
MOUTH_CORE = 2
MOUTH_FEATHER = 2


def mouth(pet: str) -> None:
    """Слой открытого рта: рот из сна поверх рта любого настроения.

    Рисованного «рта для еды» у дизайнеров нет, но во сне у всех пятерых рот
    приоткрыт — его и берём. Вокруг рта лицо залито ровно, поэтому пиксели сна
    в маске закрывают улыбку или грустную дугу без шва. Маска — все рты сразу:
    разница с кадром сна у happy, sad и dirty около рта.

    Слой один на питомца, а не на настроение: под маской в нём только кожа
    и рот, а они у настроений общие. Центр рта печатается — это точка, куда
    летит еда, и центр растяжения рта (mouthX, mouthY в PetSkin).
    """
    full = {state: load(pet, state) for state in STATES}
    sleep = pixels(full["sleep"])
    changed = np.abs(pixels(full["happy"]) - sleep).max(axis=2) > 0
    parts, count = ndimage.label(ndimage.binary_dilation(changed, iterations=12))
    sizes = ndimage.sum(changed, parts, range(1, count + 1))
    near_mouth = parts == np.argmin(sizes) + 1

    mouths = np.zeros_like(changed)
    for state in ("happy", "sad", "dirty"):
        mouths |= np.abs(pixels(full[state]) - sleep).max(axis=2) > 0
    mouths &= near_mouth

    ys, xs = np.nonzero(mouths & changed)
    side = full["sleep"].size[0]
    print(f"{pet}: рот в ({(xs.min() + xs.max()) / 2 / side:.4f}, {(ys.min() + ys.max()) / 2 / side:.4f})")

    to_mouth = ndimage.distance_transform_edt(~shrink_mask(mouths))
    mask = np.clip((MOUTH_CORE + MOUTH_FEATHER - to_mouth) / MOUTH_FEATHER, 0.0, 1.0)
    layer = pixels(shrink(full["sleep"]))
    layer[..., 3] = np.rint(layer[..., 3] * mask)
    save(Image.fromarray(layer.astype(np.uint8), "RGBA"), pet, "mouth")


def pixels(image: Image.Image) -> np.ndarray:
    return np.asarray(image).astype(int)


def shrink(image: Image.Image) -> Image.Image:
    """Уменьшение ровно как в [save] — чтобы слой моргания совпал с базовым до бита."""
    return image.resize((SIDE, SIDE), Image.LANCZOS) if max(image.size) > SIDE else image


def shrink_mask(mask: np.ndarray) -> np.ndarray:
    """Маска в размер слоя: пиксель попадает, если задет хоть одним пикселем холста."""
    image = Image.fromarray(mask.astype(np.uint8) * 255)
    return np.asarray(image.resize((SIDE, SIDE), Image.BOX)) > 0


def main() -> None:
    RES.mkdir(parents=True, exist_ok=True)
    for pet, limb_suffix in PETS.items():
        print(f"— {pet}")
        split(pet, limb_suffix)
        blink(pet)
        mouth(pet)


if __name__ == "__main__":
    main()
