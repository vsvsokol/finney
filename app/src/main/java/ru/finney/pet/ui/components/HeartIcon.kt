package ru.finney.pet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink

/**
 * Сердечко настроения: закрашенное — радость есть, пустое — нет. Одно на мини-игры,
 * шкалу настроения и игру с игрушкой, чтобы ребёнок узнавал его везде.
 */
@Composable
fun HeartIcon(filled: Boolean, size: Dp, modifier: Modifier = Modifier, fill: Color = FinneyPink) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val heart = Path().apply {
            moveTo(w / 2, h * 0.92f)
            cubicTo(w * 0.1f, h * 0.62f, -w * 0.04f, h * 0.28f, w * 0.24f, h * 0.1f)
            cubicTo(w * 0.38f, h * 0.02f, w * 0.5f, h * 0.12f, w * 0.5f, h * 0.24f)
            cubicTo(w * 0.5f, h * 0.12f, w * 0.62f, h * 0.02f, w * 0.76f, h * 0.1f)
            cubicTo(w * 1.04f, h * 0.28f, w * 0.9f, h * 0.62f, w / 2, h * 0.92f)
            close()
        }
        drawPath(heart, if (filled) fill else FinneyCream)
        drawPath(heart, FinneyInk, style = Stroke((size / 11).coerceAtLeast(2.dp).toPx()))
    }
}
