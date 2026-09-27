package ru.finney.pet.ui.pet

import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import ru.finney.pet.R
import kotlin.math.roundToInt

/** Рисунок аксессуара и где на нём линия полей — доля высоты рисунка. */
data class AccessoryArt(@DrawableRes val res: Int, val brim: Float)

/**
 * Рисунок аксессуара по id из магазина. Файлы кладёт tools/pack_items.py
 * (`design/exports/items/acc_<id>.png` → `drawable-nodpi/acc_<id>.webp`).
 * Линия полей — самая широкая строка рисунка, замерена по файлам в ресурсах.
 * null — аксессуар без рисунка: на питомце его не видно.
 */
fun accessoryArt(itemId: String): AccessoryArt? = when (itemId) {
    "hat_cowboy" -> AccessoryArt(R.drawable.acc_hat_cowboy, brim = 0.53f)
    "hat_pirate" -> AccessoryArt(R.drawable.acc_hat_pirate, brim = 0.62f)
    "hat_wizard" -> AccessoryArt(R.drawable.acc_hat_wizard, brim = 0.71f)
    else -> null
}

/** Шляпы крупнее замера по голове: по [HatFit.width] они выглядели маленькими и не налезали. */
private const val HAT_SCALE = 1.5f

/** Насколько выше замера по голове лежат поля — доля высоты холста. */
private const val HAT_LIFT = 0.10f

/**
 * Поставить шляпу по [HatFit]: ширина — доля стороны питомца, линия полей [artBrim]
 * (доля высоты рисунка) ложится на [HatFit.brim]. Слой занимает место всего холста,
 * поэтому соседние слои питомца не сдвигаются. За верх холста шляпа может выходить.
 */
internal fun Modifier.hatPlacement(fit: HatFit, artBrim: Float): Modifier = layout { measurable, constraints ->
    val side = constraints.maxWidth
    val width = (side * fit.width * HAT_SCALE).roundToInt()
    val placeable = measurable.measure(Constraints.fixedWidth(width))
    layout(side, constraints.maxHeight) {
        placeable.place(
            x = (side * fit.centerX - width / 2f).roundToInt(),
            y = (constraints.maxHeight * (fit.brim - HAT_LIFT) - placeable.height * artBrim).roundToInt(),
        )
    }
}
