package ru.finney.pet.ui.pet

import androidx.compose.ui.geometry.Rect

// Стадии роста на экране (ТЗ п. 2.5.10: не менее трёх). Стадий в логике шесть —
// стадия = уровень (Progression.stage), — а обликов три: питомец растёт целиком,
// от ступней, вместе со шляпой и очками. Отдельные рисунки стадий не нужны:
// файлы _small / _middle / _big у художников — те же слои в другом масштабе.
//
// Облик меняется уже на втором уровне: сценарий проверки проходят один раз,
// и на шаге 10 (Приложение А) рост должен быть виден сразу.
// Самый крупный облик — нынешний размер: под него подогнаны комната, ванна и кухня.

/** Стадия, при которой питомец в полный рост. Экраны без стадии рисуют его таким. */
const val GROWN_STAGE = 6

/** Масштаб облика по стадии: 1 — малыш, 2–3 — подрос, 4–6 — взрослый. */
fun growthScale(stage: Int): Float = when {
    stage <= 1 -> 0.8f
    stage <= 3 -> 0.9f
    else -> 1f
}

/**
 * Где питомец на самом деле, если [this] — место `PetView` на экране.
 * Облик сжимается к ступням ([PetSkin.ground]), а рамка раскладки остаётся прежней:
 * по ней целятся еда, губка и игрушки, и без поправки они летели бы мимо.
 */
fun Rect.grownPet(skin: PetSkin, stage: Int): Rect {
    val scale = growthScale(stage)
    if (scale == 1f || this == Rect.Zero) return this
    val originX = left + width * skin.ground.pivotFractionX
    val originY = top + height * skin.ground.pivotFractionY
    return Rect(
        left = originX + (left - originX) * scale,
        top = originY + (top - originY) * scale,
        right = originX + (right - originX) * scale,
        bottom = originY + (bottom - originY) * scale,
    )
}
