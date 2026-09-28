package ru.finney.pet.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.StrokeBold

// Переход в мир (плейтест 28.09): после «Начать игру» питомец уходит в круг-портал,
// круг заливает экран цветом стены зала, и зал раскрывается из того же круга.
// Навигация между экранами — обычное затухание: под сплошным цветом его не видно.

/** Цвет стены зала — снят пипеткой с room_back.webp. Им залит портал с обеих сторон. */
val PortalWall = Color(0xFFD3DCBB)

/** Сколько длится раскрытие зала на главном. */
private const val RevealMs = 600

/**
 * «Пришли из портала» — одноразовый флажок между экранами: настройка ставит его перед
 * переходом, главный забирает на первом кадре. Параметр маршрута не нужен: переход
 * бывает один раз за игру, а маршрут главного экрана общий для всех входов.
 */
object PortalArrival {
    @Volatile
    private var pending = false

    fun arm() {
        pending = true
    }

    /** Забрать флажок: второй вызов вернёт false. */
    fun take(): Boolean = pending.also { pending = false }
}

/**
 * Круг портала: заливка цветом стены и обводка ink. Обводка снаружи — как у всего
 * в айдентике.
 */
fun DrawScope.drawPortal(center: Offset, radius: Float) {
    if (radius <= 0f) return
    drawCircle(PortalWall, radius, center)
    val stroke = StrokeBold.toPx()
    drawCircle(FinneyInk, radius + stroke / 2, center, style = Stroke(stroke))
}

/**
 * Главный экран после портала: на первом кадре закрыт цветом стены, потом зал
 * раскрывается из круга в центре. [ready] — экран загрузился: пока грузится,
 * смотреть не на что, и круг ждёт.
 *
 * Флажок читается один раз при первом показе — при возврате с других экранов
 * портала нет.
 */
@Composable
fun PortalReveal(ready: Boolean) {
    val arrived = remember { PortalArrival.take() }
    if (!arrived) return
    val open = remember { Animatable(0f) }
    LaunchedEffect(ready) {
        if (ready) open.animateTo(1f, tween(RevealMs, easing = FastOutSlowInEasing))
    }
    if (open.value >= 1f) return
    val hole = Path()
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        // До угла экрана и ещё обводка — чтобы в конце кольцо ушло за край целиком.
        val far = center.getDistance() + StrokeBold.toPx() * 2
        val radius = far * open.value
        hole.reset()
        hole.fillType = PathFillType.EvenOdd
        hole.addRect(Rect(Offset.Zero, size))
        if (radius > 0f) {
            hole.addOval(Rect(center, radius))
        }
        drawPath(hole, PortalWall)
        if (radius > 0f) {
            val stroke = StrokeBold.toPx()
            drawCircle(FinneyInk, radius + stroke / 2, center, style = Stroke(stroke))
        }
    }
}
