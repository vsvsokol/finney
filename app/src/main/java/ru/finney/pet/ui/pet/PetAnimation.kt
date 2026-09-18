package ru.finney.pet.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Две анимации питомца.
 *
 * Покоя — бесконечная: дыхание (тело чуть тянется вверх и сжимается),
 * покачивание корпуса и рук. Идёт всегда, отдельно дёргать не нужно.
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

    // Лямбда запоминается один раз: все четыре источника кадров — стабильные объекты,
    // и значения из них берутся в момент вызова, то есть при отрисовке.
    return remember(animation, breath, sway) {
        { composePose(breath.value, sway.value, animation.lift.value, animation.crouch.value) }
    }
}

/** Общая арифметика позы: одна на [rememberPoseProvider] и [currentPose]. */
private fun composePose(breath: Float, sway: Float, lift: Float, crouch: Float): PetPose =
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

    return composePose(breath, sway, lift.value, crouch.value)
}
