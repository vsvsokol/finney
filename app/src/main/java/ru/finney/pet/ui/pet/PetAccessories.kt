package ru.finney.pet.ui.pet

import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import ru.finney.pet.R
import kotlin.math.roundToInt

/**
 * Рисунок аксессуара по id из магазина. Файлы кладёт tools/pack_items.py
 * (`design/exports/items/acc_<id>.png` → `drawable-nodpi/acc_<id>.webp`).
 * null — аксессуар без рисунка: на питомце его не видно.
 */
@DrawableRes
fun accessoryArt(itemId: String): Int? = when (itemId) {
    "hat_cowboy" -> R.drawable.acc_hat_cowboy
    "hat_pirate" -> R.drawable.acc_hat_pirate
    "hat_wizard" -> R.drawable.acc_hat_wizard
    else -> null
}

/**
 * Поставить слой по [HatFit]: ширина — доля стороны питомца, середина и низ — доли
 * его холста. Высота — по пропорциям рисунка. Слой занимает место всего холста,
 * поэтому соседние слои питомца не сдвигаются.
 */
internal fun Modifier.hatPlacement(fit: HatFit): Modifier = layout { measurable, constraints ->
    val side = constraints.maxWidth
    val width = (side * fit.width).roundToInt()
    val placeable = measurable.measure(Constraints.fixedWidth(width))
    layout(side, constraints.maxHeight) {
        placeable.place(
            x = (side * fit.centerX - width / 2f).roundToInt(),
            y = (constraints.maxHeight * fit.bottom - placeable.height).roundToInt(),
        )
    }
}
