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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import ru.finney.pet.domain.model.PetCharacter

// Питомец собирается из слоёв одного холста, поэтому детали совпадают по положению
// сами и координаты подбирать не нужно.
//
// Порядок отрисовки снизу вверх: ноги, руки, базовый слой (голова и туловище),
// слой моргания. Моргание собирает из состояний сна тот же скрипт.
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
    /** Веко: 0 — глаза открыты, 1 — закрыты. */
    val lid: Float = 0f,
)

/**
 * Поза передаётся лямбдой, а не значением, и это принципиально.
 *
 * Значением её читал бы сам `PetView`, а он меняется каждый кадр анимации — значит
 * каждый кадр пересобирался бы весь состав, вместе с `painterResource` на пять слоёв.
 * Лямбда же вызывается внутри `graphicsLayer`, то есть на отрисовке: слои достаются
 * из ресурсов один раз, а кадры анимации только двигают уже готовое.
 *
 * Разница измеренная: на экране внешности два питомца разом давали 200 мс на кадр
 * (8 fps), после перехода на лямбду — обычные 60.
 */
@Composable
fun PetView(
    character: PetCharacter,
    mood: PetMood,
    pose: () -> PetPose,
    modifier: Modifier = Modifier,
) {
    val skin = character.skin
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                val current = pose()
                scaleX = current.scaleX
                scaleY = current.scaleY
                translationY = current.offsetY.toPx()
                rotationZ = current.tilt
                transformOrigin = skin.ground
            },
    ) {
        Limb(skin.leftLeg, skin.leftLegPivot) { pose().leftLeg }
        Limb(skin.rightLeg, skin.rightLegPivot) { pose().rightLeg }
        Limb(skin.leftHand, skin.leftHandPivot) { pose().leftHand }
        Limb(skin.rightHand, skin.rightHandPivot) { pose().rightHand }

        Crossfade(
            targetState = skin.face(mood),
            animationSpec = tween(durationMillis = 280),
            label = "mood",
            modifier = Modifier.matchParentSize(),
        ) { current ->
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(current.base),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
                current.blink?.let { blink ->
                    Image(
                        painter = painterResource(blink),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .eyelid(skin) { pose().lid },
                    )
                }
            }
        }
    }
}

// Мягкость края века в долях стороны. Жёсткий край на полпути выглядит разрезом
// глаза, а не веком.
private const val LID_FEATHER = 0.025f

/**
 * Веко: слой закрытых глаз проявляется сверху вниз, будто на глаз опускается кожа.
 * Мгновенная подмена открытых глаз закрытыми читалась как мигание лампочки.
 *
 * Всё, что ниже края века, из слоя стирается. Стирание (DstOut) должно задевать
 * только этот слой, поэтому он рисуется отдельно — но лишь пока веко опущено:
 * всё остальное время отдельный слой только занимал бы память.
 * Как и поза, веко читается на отрисовке, а не в композиции.
 */
private fun Modifier.eyelid(skin: PetSkin, lid: () -> Float): Modifier = this
    .graphicsLayer {
        val closing = lid() > 0f
        alpha = if (closing) 1f else 0f
        compositingStrategy = if (closing) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }
    .drawWithCache {
        val feather = size.height * LID_FEATHER
        val top = size.height * skin.eyesTop - feather
        val bottom = size.height * skin.eyesBottom
        val edge = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, endY = feather)
        onDrawWithContent {
            val closed = lid()
            if (closed <= 0f) return@onDrawWithContent
            drawContent()
            if (closed >= 1f) return@onDrawWithContent
            val y = lerp(top, bottom, closed)
            translate(top = y) {
                drawRect(edge, size = Size(size.width, feather), blendMode = BlendMode.DstOut)
            }
            drawRect(
                Color.Black,
                topLeft = Offset(0f, y + feather),
                size = Size(size.width, size.height - y - feather),
                blendMode = BlendMode.DstOut,
            )
        }
    }

@Composable
private fun BoxScope.Limb(
    @DrawableRes res: Int,
    pivot: TransformOrigin,
    rotation: () -> Float,
) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                transformOrigin = pivot
                rotationZ = rotation()
            },
    )
}
