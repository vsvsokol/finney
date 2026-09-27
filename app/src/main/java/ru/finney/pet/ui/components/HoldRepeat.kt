package ru.finney.pet.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Sfx
import ru.finney.pet.ui.theme.FinneyInk

// «−» и «+», которые можно держать. Касание — ровно один шаг, как раньше.
// Палец держат — через паузу шаги идут сами, всё чаще. Шаг при этом не растёт
// никогда: ребёнок должен попасть ровно в 20 или 140, а не перескочить их
// разгоном. Быстрее становится только частота.
//
// Разгон считается по числу шагов, а не по времени: так он одинаков на любом
// телефоне и не зависит от того, как долго шла пересборка кадра. Первые шаги
// медленные — чтобы успеть отпустить на нужном числе, дальше быстрее.

/** Сколько держать, прежде чем шаги пойдут сами. Короче — касание станет удержанием. */
private const val RepeatDelayMs = 400L

/** Пауза между шагами на каждой скорости. */
private const val SlowIntervalMs = 300L
private const val MidIntervalMs = 150L
private const val FastIntervalMs = 60L

/** На каком шаге удержания переключается скорость (считая с нуля). */
private const val MidFromTick = 4
private const val FastFromTick = 12

/** Тон тика растёт с каждым шагом удержания и упирается в потолок. */
private const val PitchPerTick = 0.04f
private const val MaxPitch = 1.6f

/**
 * Сколько после шага удержания кнопка может погаснуть, чтобы это считалось упором.
 * Меньше самой медленной паузы: кнопка гаснет в ближайший кадр после шага.
 */
private const val StopWindowMs = 250L

/** Размах встряхивания на упоре, dp. */
private const val ShakeDp = 6f

/** Кольцо удержания: толщина и зазор от обводки кнопки, dp. */
private const val RingDp = 3.5f
private const val RingGapDp = 3f

private fun intervalFor(tick: Int): Long = when {
    tick < MidFromTick -> SlowIntervalMs
    tick < FastFromTick -> MidIntervalMs
    else -> FastIntervalMs
}

private fun pitchFor(tick: Int): Float = (1f + tick * PitchPerTick).coerceAtMost(MaxPitch)

/** Что знает кнопка об удержании. Не состояние Compose: перерисовывать из-за него нечего. */
private class Hold {
    /** Удержание уже сделало шаги — отпускание пальца не должно дать ещё один. */
    var repeated = false

    /** Когда был последний шаг удержания, по uptimeMillis. */
    var lastRepeatAt = 0L
}

/**
 * Круглая кнопка шага, которую можно держать.
 *
 * Пока палец на кнопке, она остаётся сжатой: interactionSource общий с [FinneyIconButton].
 * Звук и вибрацию даёт сама — поэтому у [FinneyIconButton] свой щелчок выключен.
 * Вибрация идёт через системную «вибрацию при касании» и выключается вместе с ней.
 *
 * Упор — кнопка погасла посреди удержания: повтор встаёт, кнопка встряхивается,
 * звучит отказ. Встряхивание — чтобы упор было видно не только по серому цвету (ТЗ п. 3.6).
 * Одиночное касание, которое дошло до края, упором не считается: шаг ведь удался.
 *
 * Удержание видно и без вибрации (у многих она выключена): пока палец на кнопке,
 * вокруг неё заполняется кольцо — ровно за [RepeatDelayMs]. Замкнулось — шаги
 * пошли сами. Касание тоже успевает показать краешек кольца, и ребёнок
 * догадывается, что кнопку можно держать.
 *
 * TalkBack нажимает кнопку действием, без касания, — это обычный [onStep].
 */
@Composable
fun HoldRepeatButton(
    onStep: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    content: @Composable () -> Unit,
) {
    val sounds = LocalSounds.current
    val haptics = LocalHapticFeedback.current
    val step by rememberUpdatedState(onStep)
    val canStep by rememberUpdatedState(enabled)
    val interaction = remember { MutableInteractionSource() }
    val hold = remember { Hold() }
    val shake = remember { Animatable(0f) }
    val charge = remember { Animatable(0f) }

    LaunchedEffect(interaction) {
        var repeat: Job? = null
        var ring: Job? = null
        interaction.interactions.collect { event ->
            when (event) {
                is PressInteraction.Press -> {
                    hold.repeated = false
                    ring?.cancel()
                    ring = launch {
                        charge.snapTo(0f)
                        charge.animateTo(1f, tween(RepeatDelayMs.toInt(), easing = LinearEasing))
                    }
                    repeat?.cancel()
                    repeat = launch {
                        delay(RepeatDelayMs)
                        var tick = 0
                        while (canStep) {
                            hold.repeated = true
                            hold.lastRepeatAt = SystemClock.uptimeMillis()
                            step()
                            sounds.play(Sfx.Tap, pitchFor(tick))
                            val interval = intervalFor(tick)
                            haptics.performHapticFeedback(
                                when {
                                    // Скорость выросла — толчок сильнее, чтобы разгон чувствовался.
                                    tick > 0 && interval < intervalFor(tick - 1) -> HapticFeedbackType.GestureThresholdActivate
                                    interval == FastIntervalMs -> HapticFeedbackType.SegmentFrequentTick
                                    else -> HapticFeedbackType.SegmentTick
                                },
                            )
                            delay(interval)
                            tick++
                        }
                    }
                }
                is PressInteraction.Release -> {
                    repeat?.cancel()
                    ring?.cancel()
                    ring = launch { charge.animateTo(0f, tween(180)) }
                }
                is PressInteraction.Cancel -> {
                    repeat?.cancel()
                    ring?.cancel()
                    ring = launch { charge.animateTo(0f, tween(180)) }
                    // За отменой касания клика не будет — сбросить здесь, иначе
                    // следующее нажатие из TalkBack потеряется.
                    hold.repeated = false
                }
            }
        }
    }

    LaunchedEffect(enabled) {
        if (enabled || SystemClock.uptimeMillis() - hold.lastRepeatAt > StopWindowMs) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
        sounds.play(Sfx.Denied)
        shake.snapTo(0f)
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 320
                -ShakeDp at 40
                ShakeDp at 110
                -ShakeDp * 0.6f at 180
                ShakeDp * 0.6f at 250
            },
        )
    }

    FinneyIconButton(
        onClick = {
            if (hold.repeated) {
                hold.repeated = false
            } else {
                step()
                sounds.play(Sfx.Tap)
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        },
        contentDescription = contentDescription,
        size = size,
        enabled = enabled,
        sound = null,
        interactionSource = interaction,
        modifier = modifier
            .graphicsLayer { translationX = shake.value * density }
            // Кольцо рисуется за пределами кнопки и не занимает места: соседи не сдвигаются.
            .drawBehind {
                val c = charge.value
                if (c <= 0f) return@drawBehind
                val width = RingDp * density
                val inset = -(this.size.minDimension / 12f + (RingGapDp + RingDp / 2) * density)
                val topLeft = Offset(inset, inset)
                val arc = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
                drawArc(FinneyInk.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arc, style = Stroke(width))
                drawArc(FinneyInk, -90f, 360f * c, false, topLeft, arc, style = Stroke(width, cap = StrokeCap.Round))
            },
        content = content,
    )
}

/**
 * Число с кнопками «−» и «+». Общее для плана, копилки и мини-игр: движение одно,
 * меняется только вид числа — [number] получает модификатор, от которого число
 * подпрыгивает на каждом шаге. Прыжок читается в graphicsLayer, строка не пересобирается.
 *
 * Кнопки 56 dp — больше минимума ТЗ п. 3.6 и в сжатом виде (56 × 0.96 ≈ 54).
 */
@Composable
fun HoldStepper(
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
    minusDescription: String,
    plusDescription: String,
    modifier: Modifier = Modifier,
    number: @Composable RowScope.(bump: Modifier) -> Unit,
) {
    val bump = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val jump: () -> Unit = {
        scope.launch {
            bump.snapTo(1.18f)
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    Row(
        // Поля по бокам — под кольцо удержания: оно рисуется за краем кнопки,
        // и у края экрана его срезало.
        modifier = modifier.padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HoldRepeatButton(
            onStep = { onMinus(); jump() },
            enabled = minusEnabled,
            contentDescription = minusDescription,
        ) {
            OutlinedText("−", style = MaterialTheme.typography.headlineMedium)
        }
        number(
            Modifier.graphicsLayer {
                scaleX = bump.value
                scaleY = bump.value
            },
        )
        HoldRepeatButton(
            onStep = { onPlus(); jump() },
            enabled = plusEnabled,
            contentDescription = plusDescription,
        ) {
            OutlinedText("+", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
