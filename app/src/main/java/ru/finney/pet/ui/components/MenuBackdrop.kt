package ru.finney.pet.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyYellow

// Фон экранов меню. На плейтесте 28.09 плоскую кремовую заливку назвали скучной,
// а понравились фоны мини-игр: точки в «Проверь чек» и крупные круги на лугу
// «Дороги к цели». Оба приёма перенесены на кремовый, и фон говорит, что за экран:
// точки — «дело» (план, магазин, настройки), круги — «событие» (знакомство,
// итог периода, цели, прогресс). Рисуется в drawBehind: один проход по холсту
// без своих слоёв и пересборок.

enum class MenuBackdrop {
    /** Точки, как на стене в «Проверь чек»: экраны-дела. */
    DOTS,

    /** Крупные мягкие круги, как на лугу «Дороги к цели»: экраны-события. */
    SHAPES,
}

fun Modifier.menuBackdrop(backdrop: MenuBackdrop): Modifier = drawBehind {
    when (backdrop) {
        MenuBackdrop.DOTS -> {
            drawRect(FinneyCream)
            // Шахматный сдвиг рядов: ровная сетка на весь экран читается как миллиметровка.
            val step = 28.dp.toPx()
            val r = 3.5.dp.toPx()
            val dot = FinneyInk.copy(alpha = 0.11f)
            var row = 0
            var y = step / 2
            while (y < size.height + step) {
                var x = if (row % 2 == 0) step / 2 else step
                while (x < size.width + step) {
                    drawCircle(dot, r, Offset(x, y))
                    x += step
                }
                y += step
                row++
            }
        }
        MenuBackdrop.SHAPES -> {
            drawRect(FinneyCream)
            // Круги уходят за края: целиком видимый круг выглядел бы как элемент
            // интерфейса, на который хочется нажать.
            val w = size.width
            val h = size.height
            val soft = FinneyYellow.copy(alpha = 0.55f)
            drawCircle(soft, w * 0.55f, Offset(w * 0.02f, h * 0.08f))
            drawCircle(FinneyPeach.copy(alpha = 0.18f), w * 0.42f, Offset(w * 1.02f, h * 0.48f))
            drawCircle(soft, w * 0.62f, Offset(w * 0.10f, h * 1.0f))
        }
    }
}
