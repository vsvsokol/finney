package ru.finney.pet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Шаг сумм в плане и копилке. Совпадает с шагом родительского бонуса из economy.json. */
const val AMOUNT_STEP = 5

/**
 * Сумма с «минусом» и «плюсом»: монеты набираются нажатиями, а не с клавиатуры —
 * цифровое поле для 7-летнего барьер, а шаг в 5 финок не даёт считать копейки.
 *
 * Одна и та же на экране плана и в копилке: ребёнок учится одному движению.
 * [canAdd] и [canRemove] гасят кнопки, когда шаг уже не поместится.
 * Держать кнопку можно — см. [HoldStepper].
 */
@Composable
fun AmountStepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    canAdd: Boolean,
    modifier: Modifier = Modifier,
    canRemove: Boolean = value > 0,
    step: Int = AMOUNT_STEP,
    /** Показать рукой на «+» — обучение (см. [pointHere]). */
    pointPlus: Boolean = false,
    /** Что над суммой между кнопками — например, рисунки того, на что эти монеты. */
    above: (@Composable () -> Unit)? = null,
) {
    HoldStepper(
        onMinus = { onChange((value - step).coerceAtLeast(0)) },
        onPlus = { onChange(value + step) },
        minusEnabled = canRemove,
        plusEnabled = canAdd,
        minusDescription = "$label: убавить",
        plusDescription = "$label: добавить",
        modifier = modifier.fillMaxWidth(),
        pointPlus = pointPlus,
    ) { bump ->
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            above?.invoke()
            CoinAmount(amount = value, modifier = bump)
        }
    }
}
