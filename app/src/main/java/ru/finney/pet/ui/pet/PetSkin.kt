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
    /** Закрытые глаза поверх открытых — по слою на каждое настроение, где глаза открыты. */
    @DrawableRes val blinkHappy: Int,
    @DrawableRes val blinkSad: Int,
    @DrawableRes val blinkDirty: Int,
    /** Открытый рот поверх любого настроения — для еды. Один на питомца. */
    @DrawableRes val mouth: Int,
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
    /** Где по высоте лежат глаза — в этих пределах опускается веко. Доли стороны, как и точки. */
    val eyesTop: Float,
    val eyesBottom: Float,
    /** Центр рта: туда летит еда, от него растягивается открытый рот. */
    val mouthCenter: TransformOrigin,
) {
    fun face(mood: PetMood): PetFace = when (mood) {
        PetMood.HAPPY -> PetFace(happy, blinkHappy)
        PetMood.SAD -> PetFace(sad, blinkSad)
        PetMood.DIRTY -> PetFace(dirty, blinkDirty)
        // Во сне глаза и так закрыты — моргать нечем.
        PetMood.SLEEP -> PetFace(sleep, blink = null)
    }
}

/** Базовый слой настроения и его моргание: меняются только вместе. */
@Immutable
data class PetFace(@DrawableRes val base: Int, @DrawableRes val blink: Int?)

private const val PUSHISTIK_CANVAS = 2048f

// Полосу глаз не снимают в редакторе: её печатает tools/split_pet_base.py, и уже
// в пикселях слоя в ресурсах, а не холста. Центр рта он печатает сразу долями.
private const val BLINK_LAYER = 512f
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
    blinkHappy = R.drawable.pushistik_blink_happy,
    blinkSad = R.drawable.pushistik_blink_sad,
    blinkDirty = R.drawable.pushistik_blink_dirty,
    mouth = R.drawable.pushistik_mouth,
    leftHand = R.drawable.pushistik_left_hand,
    rightHand = R.drawable.pushistik_right_hand,
    leftLeg = R.drawable.pushistik_left_leg,
    rightLeg = R.drawable.pushistik_right_leg,
    leftHandPivot = TransformOrigin(930f / PUSHISTIK_CANVAS, 1500f / PUSHISTIK_CANVAS),
    rightHandPivot = TransformOrigin(1130f / PUSHISTIK_CANVAS, 1500f / PUSHISTIK_CANVAS),
    leftLegPivot = TransformOrigin(960f / PUSHISTIK_CANVAS, 1640f / PUSHISTIK_CANVAS),
    rightLegPivot = TransformOrigin(1100f / PUSHISTIK_CANVAS, 1640f / PUSHISTIK_CANVAS),
    ground = TransformOrigin(0.5f, 1828f / PUSHISTIK_CANVAS),
    eyesTop = 234f / BLINK_LAYER,
    eyesBottom = 342f / BLINK_LAYER,
    mouthCenter = TransformOrigin(0.5022f, 0.6619f),
)

private val VanyaSkin = PetSkin(
    happy = R.drawable.rogatik_base_happy,
    sad = R.drawable.rogatik_base_sad,
    dirty = R.drawable.rogatik_base_dirty,
    sleep = R.drawable.rogatik_base_sleep,
    blinkHappy = R.drawable.rogatik_blink_happy,
    blinkSad = R.drawable.rogatik_blink_sad,
    blinkDirty = R.drawable.rogatik_blink_dirty,
    mouth = R.drawable.rogatik_mouth,
    leftHand = R.drawable.rogatik_left_hand,
    rightHand = R.drawable.rogatik_right_hand,
    leftLeg = R.drawable.rogatik_left_leg,
    rightLeg = R.drawable.rogatik_right_leg,
    leftHandPivot = TransformOrigin(1029f / ROGATIK_CANVAS, 1545f / ROGATIK_CANVAS),
    rightHandPivot = TransformOrigin(1188f / ROGATIK_CANVAS, 1545f / ROGATIK_CANVAS),
    leftLegPivot = TransformOrigin(1054f / ROGATIK_CANVAS, 1771f / ROGATIK_CANVAS),
    rightLegPivot = TransformOrigin(1168f / ROGATIK_CANVAS, 1771f / ROGATIK_CANVAS),
    ground = TransformOrigin(0.5f, 1898f / ROGATIK_CANVAS),
    eyesTop = 223f / BLINK_LAYER,
    eyesBottom = 337f / BLINK_LAYER,
    mouthCenter = TransformOrigin(0.502f, 0.6395f),
)

private val IraSkin = PetSkin(
    happy = R.drawable.zvezdochka_base_happy,
    sad = R.drawable.zvezdochka_base_sad,
    dirty = R.drawable.zvezdochka_base_dirty,
    sleep = R.drawable.zvezdochka_base_sleep,
    blinkHappy = R.drawable.zvezdochka_blink_happy,
    blinkSad = R.drawable.zvezdochka_blink_sad,
    blinkDirty = R.drawable.zvezdochka_blink_dirty,
    mouth = R.drawable.zvezdochka_mouth,
    leftHand = R.drawable.zvezdochka_left_hand,
    rightHand = R.drawable.zvezdochka_right_hand,
    leftLeg = R.drawable.zvezdochka_left_leg,
    rightLeg = R.drawable.zvezdochka_right_leg,
    leftHandPivot = TransformOrigin(1055f / ZVEZDOCHKA_CANVAS, 1530f / ZVEZDOCHKA_CANVAS),
    rightHandPivot = TransformOrigin(1163f / ZVEZDOCHKA_CANVAS, 1530f / ZVEZDOCHKA_CANVAS),
    leftLegPivot = TransformOrigin(1060f / ZVEZDOCHKA_CANVAS, 1764f / ZVEZDOCHKA_CANVAS),
    rightLegPivot = TransformOrigin(1167f / ZVEZDOCHKA_CANVAS, 1765f / ZVEZDOCHKA_CANVAS),
    ground = TransformOrigin(0.5f, 1906f / ZVEZDOCHKA_CANVAS),
    eyesTop = 201f / BLINK_LAYER,
    eyesBottom = 325f / BLINK_LAYER,
    mouthCenter = TransformOrigin(0.5043f, 0.6307f),
)

private val SevaSkin = PetSkin(
    happy = R.drawable.bantik_base_happy,
    sad = R.drawable.bantik_base_sad,
    dirty = R.drawable.bantik_base_dirty,
    sleep = R.drawable.bantik_base_sleep,
    blinkHappy = R.drawable.bantik_blink_happy,
    blinkSad = R.drawable.bantik_blink_sad,
    blinkDirty = R.drawable.bantik_blink_dirty,
    mouth = R.drawable.bantik_mouth,
    leftHand = R.drawable.bantik_left_hand,
    rightHand = R.drawable.bantik_right_hand,
    leftLeg = R.drawable.bantik_left_leg,
    rightLeg = R.drawable.bantik_right_leg,
    leftHandPivot = TransformOrigin(1044f / BANTIK_CANVAS, 1559f / BANTIK_CANVAS),
    rightHandPivot = TransformOrigin(1179f / BANTIK_CANVAS, 1558f / BANTIK_CANVAS),
    leftLegPivot = TransformOrigin(1060f / BANTIK_CANVAS, 1791f / BANTIK_CANVAS),
    rightLegPivot = TransformOrigin(1156f / BANTIK_CANVAS, 1789f / BANTIK_CANVAS),
    ground = TransformOrigin(0.5f, 1902f / BANTIK_CANVAS),
    eyesTop = 227f / BLINK_LAYER,
    eyesBottom = 335f / BLINK_LAYER,
    mouthCenter = TransformOrigin(0.5043f, 0.6516f),
)

private val YarikSkin = PetSkin(
    happy = R.drawable.luchik_base_happy,
    sad = R.drawable.luchik_base_sad,
    dirty = R.drawable.luchik_base_dirty,
    sleep = R.drawable.luchik_base_sleep,
    blinkHappy = R.drawable.luchik_blink_happy,
    blinkSad = R.drawable.luchik_blink_sad,
    blinkDirty = R.drawable.luchik_blink_dirty,
    mouth = R.drawable.luchik_mouth,
    leftHand = R.drawable.luchik_left_hand,
    rightHand = R.drawable.luchik_right_hand,
    leftLeg = R.drawable.luchik_left_leg,
    rightLeg = R.drawable.luchik_right_leg,
    leftHandPivot = TransformOrigin(1055f / LUCHIK_CANVAS, 1547f / LUCHIK_CANVAS),
    rightHandPivot = TransformOrigin(1148f / LUCHIK_CANVAS, 1547f / LUCHIK_CANVAS),
    leftLegPivot = TransformOrigin(1064f / LUCHIK_CANVAS, 1752f / LUCHIK_CANVAS),
    rightLegPivot = TransformOrigin(1158f / LUCHIK_CANVAS, 1754f / LUCHIK_CANVAS),
    ground = TransformOrigin(0.5f, 1913f / LUCHIK_CANVAS),
    eyesTop = 215f / BLINK_LAYER,
    eyesBottom = 330f / BLINK_LAYER,
    mouthCenter = TransformOrigin(0.5036f, 0.638f),
)

val PetCharacter.skin: PetSkin
    get() = when (this) {
        PetCharacter.PUSHISTIK -> ZalinaSkin
        PetCharacter.ROGATIK -> VanyaSkin
        PetCharacter.ZVEZDOCHKA -> IraSkin
        PetCharacter.BANTIK -> SevaSkin
        PetCharacter.LUCHIK -> YarikSkin
    }
