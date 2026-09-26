package ru.finney.pet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Шаг сумм в плане и копилке. Совпадает с шагом родительского бонуса из economy.json. */
const val AMOUNT_STEP = 5

/**
 * Сумма с «минусом» и «плюсом»: монеты набираются нажатиями, а не с клавиатуры —
 * цифровое поле для 7-летнего барьер, а шаг в 5 финок не даёт считать копейки.
 *
 * Одна и та же на экране плана и в копилке: ребёнок учится одному движению.
 * [canAdd] и [canRemove] гасят кнопки, когда шаг уже не поместится.
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
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FinneyIconButton(
            onClick = { onChange((value - step).coerceAtLeast(0)) },
            contentDescription = "$label: убавить",
            size = 56.dp,
            enabled = canRemove,
        ) {
            OutlinedText("−", style = MaterialTheme.typography.headlineMedium)
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            CoinAmount(amount = value)
        }

        FinneyIconButton(
            onClick = { onChange(value + step) },
            contentDescription = "$label: добавить",
            size = 56.dp,
            enabled = canAdd,
        ) {
            OutlinedText("+", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
