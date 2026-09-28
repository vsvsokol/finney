package ru.finney.pet.ui.pet

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin

// Стадии роста на экране (ТЗ п. 2.5.10: не менее трёх). Стадий в логике шесть —
// стадия = уровень (Progression.stage), — а обликов три. Голова не меняется, растут
// туловище, руки и ноги: с уровнем у питомца меняются пропорции, как у ребёнка.
// Слои обликов готовит tools/split_pet_base.py: там же линия увеличенного тела
// утончается до прежней толщины, иначе обводка тела была бы жирнее, чем у головы.
//
// Облик меняется уже на втором уровне: сценарий проверки проходят один раз,
// и на шаге 10 (Приложение А) рост должен быть виден сразу.

/** Облик по стадии: 1 — малыш, 2 — подрос (уровни 2–3), 3 — взрослый (4–6). */
fun lookFor(stage: Int): Int = when {
    stage <= 1 -> 1
    stage <= 3 -> 2
    else -> 3
}

/** Во сколько раз туловище, руки и ноги облика крупнее нарисованных. Как LOOKS в скрипте. */
fun torsoScale(look: Int): Float = when (look) {
    1 -> 1f
    2 -> 1.25f
    else -> 1.5f
}

/**
 * На сколько поднять голову в облике — доля стороны. Туловище растёт от ступней, и место,
 * где на нём лежит низ головы ([PetSkin.neck]), поднимается на столько же.
 */
fun PetSkin.headLift(stage: Int): Float = (ground.pivotFractionY - neck) * (torsoScale(lookFor(stage)) - 1f)

/** Точка вращения конечности в увеличенном облике: слой растёт от ступней. */
internal fun PetSkin.grownPivot(pivot: TransformOrigin, look: Int): TransformOrigin {
    val s = torsoScale(look)
    return TransformOrigin(
        pivotFractionX = ground.pivotFractionX + (pivot.pivotFractionX - ground.pivotFractionX) * s,
        pivotFractionY = ground.pivotFractionY + (pivot.pivotFractionY - ground.pivotFractionY) * s,
    )
}

/**
 * Где голова питомца, если [this] — место `PetView` на экране: в старших обликах она
 * поднята. По этой рамке целятся еда (в рот), губка и игрушки.
 */
fun Rect.grownPet(skin: PetSkin, stage: Int): Rect {
    if (this == Rect.Zero) return this
    return translate(0f, -height * skin.headLift(stage))
}
