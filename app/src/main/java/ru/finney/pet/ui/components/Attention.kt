package ru.finney.pet.ui.components

import ru.finney.pet.ui.motion.LocalAnimations
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

// Подсказка следующего шага без слов. Плейтест: ребёнок пропускает вступление и
// жмёт наугад, поэтому экран сам показывает, куда нажать, — пульсом, а не абзацем
// текста (ТЗ п. 3.6: понятно без чтения длинной инструкции).

/** Насколько элемент «вдыхает» на пульсе. Больше — дёргается, меньше — не заметно. */
private const val PulseScale = 1.07f

/** Полупериод пульса. */
private const val PulseMs = 650

/**
 * Мягкая пульсация — «нажми сюда». [active] false — элемент стоит спокойно.
 * Масштаб читается в graphicsLayer, на отрисовке: пульс не пересобирает элемент.
 */
fun Modifier.pulse(active: Boolean): Modifier = if (!active) this else composed {
    // Без анимаций пульс застыл бы на вдохе, увеличенным: элемент стоит как есть.
    if (!LocalAnimations.current) return@composed Modifier
    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = PulseScale,
        animationSpec = infiniteRepeatable(tween(PulseMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
