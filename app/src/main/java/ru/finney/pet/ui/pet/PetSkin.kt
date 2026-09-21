package ru.finney.pet.ui.pet

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.TransformOrigin
import ru.finney.pet.R
import ru.finney.pet.domain.model.PetCharacter

// Всё, что отличает одного нарисованного питомца от другого: набор слоёв и точки
// вращения. Сама сборка и анимация общие — см. PetView и PetAnimation.
//
// Точки заданы долями стороны холста, поэтому не зависят от размера, в котором
// питомца рисуют на экране. Холсты у дизайнеров разные (2048 у Пушистика, 2200 у
// Рогатика), и делитель у каждого скина свой — числители те же пиксели, которые
// видно в редакторе.

/** Выражение лица. Слои различаются только областью лица, тело во всех одинаковое. */
enum class PetMood { HAPPY, SAD, DIRTY, SLEEP }

@Immutable
data class PetSkin(
    @DrawableRes val happy: Int,
    @DrawableRes val sad: Int,
    @DrawableRes val dirty: Int,
    @DrawableRes val sleep: Int,
    @DrawableRes val leftHand: Int,
    @DrawableRes val rightHand: Int,
    @DrawableRes val leftLeg: Int,
    @DrawableRes val rightLeg: Int,
    /** Плечи и бёдра — там, где конечность прилегает к туловищу. */
    val leftHandPivot: TransformOrigin,
    val rightHandPivot: TransformOrigin,
    val leftLegPivot: TransformOrigin,
    val rightLegPivot: TransformOrigin,
    /** Точка опоры: персонаж стоит на ногах, поэтому масштаб и наклон идут от ступней, а не от центра. */
    val ground: TransformOrigin,
) {
    @DrawableRes
    fun base(mood: PetMood): Int = when (mood) {
        PetMood.HAPPY -> happy
        PetMood.SAD -> sad
        PetMood.DIRTY -> dirty
        PetMood.SLEEP -> sleep
    }
}

private const val PUSHISTIK_CANVAS = 2048f
private const val ROGATIK_CANVAS = 2200f

// Звёздочка, Бантик и Лучик рисовались по шаблону Рогатика, поэтому холст у них тот же.
// Точки вращения не подбирались на глаз: плечи и бёдра — это верхний внутренний
// угол альфы соответствующего слоя, точка опоры — низ силуэта. По Пушистику и Рогатику
// правило сходится с тем, что уже стояло в коде, с точностью до пары пикселей.
private const val ZVEZDOCHKA_CANVAS = 2200f
private const val BANTIK_CANVAS = 2200f
private const val LUCHIK_CANVAS = 2200f

private val ZalinaSkin = PetSkin(
    happy = R.drawable.pushistik_base_happy,
    sad = R.drawable.pushistik_base_sad,
    dirty = R.drawable.pushistik_base_dirty,
    sleep = R.drawable.pushistik_base_sleep,
    leftHand = R.drawable.pushistik_left_hand,
    rightHand = R.drawable.pushistik_right_hand,
    leftLeg = R.drawable.pushistik_left_leg,
    rightLeg = R.drawable.pushistik_right_leg,
    leftHandPivot = TransformOrigin(930f / PUSHISTIK_CANVAS, 1500f / PUSHISTIK_CANVAS),
    rightHandPivot = TransformOrigin(1130f / PUSHISTIK_CANVAS, 1500f / PUSHISTIK_CANVAS),
    leftLegPivot = TransformOrigin(960f / PUSHISTIK_CANVAS, 1640f / PUSHISTIK_CANVAS),
    rightLegPivot = TransformOrigin(1100f / PUSHISTIK_CANVAS, 1640f / PUSHISTIK_CANVAS),
    ground = TransformOrigin(0.5f, 1828f / PUSHISTIK_CANVAS),
)

private val VanyaSkin = PetSkin(
    happy = R.drawable.rogatik_base_happy,
    sad = R.drawable.rogatik_base_sad,
    dirty = R.drawable.rogatik_base_dirty,
    sleep = R.drawable.rogatik_base_sleep,
    leftHand = R.drawable.rogatik_left_hand,
    rightHand = R.drawable.rogatik_right_hand,
    leftLeg = R.drawable.rogatik_left_leg,
    rightLeg = R.drawable.rogatik_right_leg,
    leftHandPivot = TransformOrigin(1029f / ROGATIK_CANVAS, 1545f / ROGATIK_CANVAS),
    rightHandPivot = TransformOrigin(1188f / ROGATIK_CANVAS, 1545f / ROGATIK_CANVAS),
    leftLegPivot = TransformOrigin(1054f / ROGATIK_CANVAS, 1771f / ROGATIK_CANVAS),
    rightLegPivot = TransformOrigin(1168f / ROGATIK_CANVAS, 1771f / ROGATIK_CANVAS),
    ground = TransformOrigin(0.5f, 1898f / ROGATIK_CANVAS),
)

private val IraSkin = PetSkin(
    happy = R.drawable.zvezdochka_base_happy,
    sad = R.drawable.zvezdochka_base_sad,
    dirty = R.drawable.zvezdochka_base_dirty,
    sleep = R.drawable.zvezdochka_base_sleep,
    leftHand = R.drawable.zvezdochka_left_hand,
    rightHand = R.drawable.zvezdochka_right_hand,
    leftLeg = R.drawable.zvezdochka_left_leg,
    rightLeg = R.drawable.zvezdochka_right_leg,
    leftHandPivot = TransformOrigin(1055f / ZVEZDOCHKA_CANVAS, 1530f / ZVEZDOCHKA_CANVAS),
    rightHandPivot = TransformOrigin(1163f / ZVEZDOCHKA_CANVAS, 1530f / ZVEZDOCHKA_CANVAS),
    leftLegPivot = TransformOrigin(1060f / ZVEZDOCHKA_CANVAS, 1764f / ZVEZDOCHKA_CANVAS),
    rightLegPivot = TransformOrigin(1167f / ZVEZDOCHKA_CANVAS, 1765f / ZVEZDOCHKA_CANVAS),
    ground = TransformOrigin(0.5f, 1906f / ZVEZDOCHKA_CANVAS),
)

private val SevaSkin = PetSkin(
    happy = R.drawable.bantik_base_happy,
    sad = R.drawable.bantik_base_sad,
    dirty = R.drawable.bantik_base_dirty,
    sleep = R.drawable.bantik_base_sleep,
    leftHand = R.drawable.bantik_left_hand,
    rightHand = R.drawable.bantik_right_hand,
    leftLeg = R.drawable.bantik_left_leg,
    rightLeg = R.drawable.bantik_right_leg,
    leftHandPivot = TransformOrigin(1044f / BANTIK_CANVAS, 1559f / BANTIK_CANVAS),
    rightHandPivot = TransformOrigin(1179f / BANTIK_CANVAS, 1558f / BANTIK_CANVAS),
    leftLegPivot = TransformOrigin(1060f / BANTIK_CANVAS, 1791f / BANTIK_CANVAS),
    rightLegPivot = TransformOrigin(1156f / BANTIK_CANVAS, 1789f / BANTIK_CANVAS),
    ground = TransformOrigin(0.5f, 1902f / BANTIK_CANVAS),
)

private val YarikSkin = PetSkin(
    happy = R.drawable.luchik_base_happy,
    sad = R.drawable.luchik_base_sad,
    dirty = R.drawable.luchik_base_dirty,
    sleep = R.drawable.luchik_base_sleep,
    leftHand = R.drawable.luchik_left_hand,
    rightHand = R.drawable.luchik_right_hand,
    leftLeg = R.drawable.luchik_left_leg,
    rightLeg = R.drawable.luchik_right_leg,
    leftHandPivot = TransformOrigin(1055f / LUCHIK_CANVAS, 1547f / LUCHIK_CANVAS),
    rightHandPivot = TransformOrigin(1148f / LUCHIK_CANVAS, 1547f / LUCHIK_CANVAS),
    leftLegPivot = TransformOrigin(1064f / LUCHIK_CANVAS, 1752f / LUCHIK_CANVAS),
    rightLegPivot = TransformOrigin(1158f / LUCHIK_CANVAS, 1754f / LUCHIK_CANVAS),
    ground = TransformOrigin(0.5f, 1913f / LUCHIK_CANVAS),
)

val PetCharacter.skin: PetSkin
    get() = when (this) {
        PetCharacter.PUSHISTIK -> ZalinaSkin
        PetCharacter.ROGATIK -> VanyaSkin
        PetCharacter.ZVEZDOCHKA -> IraSkin
        PetCharacter.BANTIK -> SevaSkin
        PetCharacter.LUCHIK -> YarikSkin
    }
