package ru.finney.pet.ui.room

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.ShopItem
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.pet.accessoryArt
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk

/**
 * Гардероб: купленные вещи, которые можно надеть на питомца. Переодевание бесплатное —
 * монеты ушли при покупке, и выбор вещи здесь уже не финансовое решение.
 *
 * Надетая вещь отмечена и заливкой, и словом «надето» (ТЗ п. 3.6: не только цветом).
 * Пустой гардероб говорит, где взять вещи, и ведёт в магазин — тупика нет.
 */
@Composable
fun WardrobePanel(
    items: List<ShopItem>,
    worn: String?,
    onWear: (itemId: String) -> Unit,
    onTakeOff: () -> Unit,
    onOpenShop: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FinneyPanel(title = "Гардероб", onClose = onDismiss, modifier = modifier) {
        if (items.isEmpty()) {
            Text(
                text = "Пока нечего надеть. Шляпы есть в магазине.",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
            FinneyButton(text = "В магазин", onClick = onOpenShop)
            return@FinneyPanel
        }
        items.forEach { item ->
            val isWorn = item.id == worn
            FinneyCard(accent = if (isWorn) FinneyGreen else null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    accessoryArt(item.id)?.let {
                        Image(painter = painterResource(it), contentDescription = null, modifier = Modifier.size(56.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.label, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                        if (isWorn) Text("надето", style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
                    }
                }
                FinneyButton(
                    text = if (isWorn) "Снять" else "Надеть",
                    onClick = { if (isWorn) onTakeOff() else onWear(item.id) },
                )
            }
        }
    }
}
