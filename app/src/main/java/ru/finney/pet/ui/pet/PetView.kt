package ru.finney.pet.ui.pet

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.R

// Питомец собирается из слоёв одного холста 2048 x 2048, поэтому детали совпадают
// по положению сами и координаты подбирать не нужно.
//
// Порядок отрисовки снизу вверх: ноги, руки, базовый слой (голова и туловище).
// Базовый слой получен из состояния лица вычитанием конечностей — см.
// tools/split_pet_base.py. Без этого вычитания под повёрнутой рукой видно исходную.
//
// Сейчас только обычный размер тела. Файлы стадий роста (_middle, _big) лежат в
// design/exports/pet и в ресурсы пока не конвертируются: добавить стадию — это
// параметр у Limb и у базового слоя плюс строка в tools/split_pet_base.py.

/** Выражение лица. Файлы различаются только областью лица, тело во всех одинаковое. */
enum class PetMood(@DrawableRes val baseRes: Int) {
    HAPPY(R.drawable.pushistik_base_happy),
    SAD(R.drawable.pushistik_base_sad),
    DIRTY(R.drawable.pushistik_base_dirty),
    SLEEP(R.drawable.pushistik_base_sleep),
}

/**
 * Точки вращения конечностей — доли от стороны холста 2048.
 * Плечи и бёдра: там, где конечность прилегает к туловищу.
 */
private val LeftHandPivot = TransformOrigin(930f / 2048f, 1500f / 2048f)
private val RightHandPivot = TransformOrigin(1130f / 2048f, 1500f / 2048f)
private val LeftLegPivot = TransformOrigin(960f / 2048f, 1640f / 2048f)
private val RightLegPivot = TransformOrigin(1100f / 2048f, 1640f / 2048f)

/** Персонаж стоит на ногах: масштабируем и наклоняем относительно точки опоры, а не центра. */
private val GroundOrigin = TransformOrigin(0.5f, 0.89f)

/** Мгновенное положение всех частей. Анимации только заполняют эту структуру. */
data class PetPose(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val offsetY: Dp = 0.dp,
    val tilt: Float = 0f,
    val leftHand: Float = 0f,
    val rightHand: Float = 0f,
    val leftLeg: Float = 0f,
    val rightLeg: Float = 0f,
)

@Composable
fun PetView(
    mood: PetMood,
    pose: PetPose,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pose.scaleX
                scaleY = pose.scaleY
                translationY = pose.offsetY.toPx()
                rotationZ = pose.tilt
                transformOrigin = GroundOrigin
            },
    ) {
        Limb(R.drawable.pushistik_left_leg, LeftLegPivot, pose.leftLeg)
        Limb(R.drawable.pushistik_right_leg, RightLegPivot, pose.rightLeg)
        Limb(R.drawable.pushistik_left_hand, LeftHandPivot, pose.leftHand)
        Limb(R.drawable.pushistik_right_hand, RightHandPivot, pose.rightHand)

        Crossfade(
            targetState = mood,
            animationSpec = tween(durationMillis = 280),
            label = "mood",
            modifier = Modifier.matchParentSize(),
        ) { current ->
            Image(
                painter = painterResource(current.baseRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BoxScope.Limb(
    @DrawableRes res: Int,
    pivot: TransformOrigin,
    rotation: Float,
) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                transformOrigin = pivot
                rotationZ = rotation
            },
    )
}
