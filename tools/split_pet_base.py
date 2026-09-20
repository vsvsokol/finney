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

Запуск из корня репозитория (нужен Pillow: pip install Pillow):
    python tools/split_pet_base.py

Перезапускать после каждого нового экспорта от дизайнера.
"""

from pathlib import Path

from PIL import Image, ImageMath

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

# Имя питомца → суффикс файлов конечностей обычного размера. У [@Lix2w78] он пустой
# (pushistik_left_hand.png), у [@lemonke68] размер указан явно (rogatik_left_hand_small.png).
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


def main() -> None:
    RES.mkdir(parents=True, exist_ok=True)
    for pet, limb_suffix in PETS.items():
        print(f"— {pet}")
        split(pet, limb_suffix)


if __name__ == "__main__":
    main()
