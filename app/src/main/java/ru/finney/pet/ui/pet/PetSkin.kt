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
// питомца рисуют на экране. Холсты у дизайнеров разные (2048 у [@Lix2w78], 2200 у
// [@lemonke68]), и делитель у каждого скина свой — числители те же пиксели, которые
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

val PetCharacter.skin: PetSkin
    get() = when (this) {
        PetCharacter.PUSHISTIK -> ZalinaSkin
        PetCharacter.ROGATIK -> VanyaSkin
    }
