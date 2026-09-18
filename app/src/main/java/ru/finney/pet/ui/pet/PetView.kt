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
import ru.finney.pet.domain.model.PetCharacter

// Питомец собирается из слоёв одного холста, поэтому детали совпадают по положению
// сами и координаты подбирать не нужно.
//
// Порядок отрисовки снизу вверх: ноги, руки, базовый слой (голова и туловище).
// В базовом слое конечности вырезаны из состояния лица — см. tools/split_pet_base.py.
// Без выреза под повёрнутой рукой видно исходную; вырез обязан быть точным по краю,
// иначе по контуру рук и ног идёт светлый шов.
//
// Чем один питомец отличается от другого — только набором слоёв и точками
// вращения: всё это в PetSkin, а сборка и анимация общие.
//
// Сейчас только обычный размер тела. Файлы стадий роста (_middle, _big) лежат в
// design/exports/pet и в ресурсы пока не конвертируются: добавить стадию — это
// параметр у Limb и у базового слоя плюс строка в tools/split_pet_base.py.

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
    character: PetCharacter,
    mood: PetMood,
    pose: PetPose,
    modifier: Modifier = Modifier,
) {
    val skin = character.skin
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pose.scaleX
                scaleY = pose.scaleY
                translationY = pose.offsetY.toPx()
                rotationZ = pose.tilt
                transformOrigin = skin.ground
            },
    ) {
        Limb(skin.leftLeg, skin.leftLegPivot, pose.leftLeg)
        Limb(skin.rightLeg, skin.rightLegPivot, pose.rightLeg)
        Limb(skin.leftHand, skin.leftHandPivot, pose.leftHand)
        Limb(skin.rightHand, skin.rightHandPivot, pose.rightHand)

        Crossfade(
            targetState = skin.base(mood),
            animationSpec = tween(durationMillis = 280),
            label = "mood",
            modifier = Modifier.matchParentSize(),
        ) { current ->
            Image(
                painter = painterResource(current),
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
