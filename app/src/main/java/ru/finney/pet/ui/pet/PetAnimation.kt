package ru.finney.pet.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Две анимации питомца.
 *
 * Покоя — бесконечная: дыхание (тело чуть тянется вверх и сжимается),
 * покачивание корпуса и рук, моргание. Идёт всегда, отдельно дёргать не нужно.
 *
 * Радости — по событию [playJoy]: приседание, прыжок с вытягиванием,
 * руки вверх, поджатые ноги, приземление с пружинкой.
 */
@Stable
class PetAnimation internal constructor(private val scope: CoroutineScope) {

    /** 0 — стоит на земле, 1 — верхняя точка прыжка. */
    internal val lift = Animatable(0f)

    /** 0 — обычная поза, 1 — максимальное приседание перед прыжком. */
    internal val crouch = Animatable(0f)

    private var job: Job? = null

    fun playJoy() {
        job?.cancel()
        job = scope.launch {
            crouch.animateTo(1f, tween(durationMillis = 140, easing = FastOutLinearInEasing))
            launch { crouch.animateTo(0f, tween(durationMillis = 170)) }
            lift.animateTo(1f, tween(durationMillis = 250, easing = FastOutSlowInEasing))
            lift.animateTo(0f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
            crouch.animateTo(0.55f, tween(durationMillis = 80))
            crouch.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
}

@Composable
fun rememberPetAnimation(): PetAnimation {
    val scope = rememberCoroutineScope()
    return remember(scope) { PetAnimation(scope) }
}

/**
 * Поза как функция — то, что нужно передавать в [PetView].
 *
 * Здесь наружу отдаётся не значение, а способ его получить. Анимируемые величины
 * читаются только внутри возвращённой лямбды, а её [PetView] вызывает на отрисовке.
 * Поэтому кадры анимации вообще не задевают композицию: слои питомца достаются
 * из ресурсов один раз.
 *
 * Пользоваться так: `PetView(pose = rememberPoseProvider(animation))`.
 */
@Composable
fun rememberPoseProvider(animation: PetAnimation): () -> PetPose {
    val idle = rememberInfiniteTransition(label = "idle")
    val breath = idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val sway = idle.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sway",
    )

    val lid = rememberBlink()

    // Лямбда запоминается один раз: все источники кадров — стабильные объекты,
    // и значения из них берутся в момент вызова, то есть при отрисовке.
    return remember(animation, breath, sway, lid) {
        {
            composePose(
                breath.value, sway.value, animation.lift.value, animation.crouch.value,
                lid.value,
            )
        }
    }
}

// Как у живого моргания: веко падает быстро и с разгоном, чуть задерживается
// и поднимается медленнее, с торможением. Всё вместе — около четверти секунды:
// короче глаз не успевает увидеть движение, дольше — питомец выглядит сонным.
private const val LID_CLOSE_MS = 80
private const val LID_HOLD_MS = 50L
private const val LID_OPEN_MS = 130

/**
 * Веко опускается через случайные 2–5.5 с, иногда дважды подряд.
 * Ровный ритм выглядит механически, поэтому паузы разные. У каждого питомца
 * на экране свой ритм: двое на экране подбора моргают не хором.
 */
@Composable
private fun rememberBlink(): Animatable<Float, AnimationVector1D> {
    val lid = remember { Animatable(0f) }
    LaunchedEffect(lid) {
        while (true) {
            delay(Random.nextLong(2000L, 5500L))
            repeat(if (Random.nextFloat() < 0.2f) 2 else 1) {
                lid.animateTo(1f, tween(LID_CLOSE_MS, easing = FastOutLinearInEasing))
                delay(LID_HOLD_MS)
                lid.animateTo(0f, tween(LID_OPEN_MS, easing = LinearOutSlowInEasing))
            }
        }
    }
    return lid
}

/** Общая арифметика позы: одна на [rememberPoseProvider] и [currentPose]. */
private fun composePose(
    breath: Float,
    sway: Float,
    lift: Float,
    crouch: Float,
    lid: Float,
): PetPose =
    // Дыхание и прыжок — это объём: что прибавилось по высоте, то убавилось по ширине.
    PetPose(
        scaleX = 1f - breath * 0.008f + crouch * 0.07f - lift * 0.035f,
        scaleY = 1f + breath * 0.016f - crouch * 0.10f + lift * 0.055f,
        offsetY = (-38f * lift + 8f * crouch).dp,
        tilt = sway * 0.7f,
        leftHand = sway * 3f - lift * 34f,
        rightHand = sway * 3f + lift * 34f,
        leftLeg = -lift * 8f,
        rightLeg = lift * 8f,
        lid = lid,
    )

/**
 * Складывает анимацию покоя и текущий кадр прыжка в одну позу.
 *
 * Вызывающий пересобирается каждый кадр — в разметке экранов это дорого.
 * Для показа питомца берите [rememberPoseProvider]; эта остаётся для `@Preview`
 * и мест, где поза нужна разово.
 */
@Composable
fun PetAnimation.currentPose(): PetPose {
    val idle = rememberInfiniteTransition(label = "idle")
    val breath by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val sway by idle.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sway",
    )

    // Моргание сюда не входит: превью — один неподвижный кадр, глаза в нём открыты.
    return composePose(breath, sway, lift.value, crouch.value, lid = 0f)
}
