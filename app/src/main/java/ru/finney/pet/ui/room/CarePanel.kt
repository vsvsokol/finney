package ru.finney.pet.ui.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.game.PurchasePreview
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.ShopItem
import ru.finney.pet.domain.model.StatEffect
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyTheme

/**
 * Что выбирают: предмет и что с ним станет, если купить.
 *
 * Всё, кроме флага выбора, приходит из домена готовым. Считать здесь нечего:
 * цена в предмете, влияние в предпросмотре, нехватка тоже.
 */
data class CareOption(
    val item: ShopItem,
    val preview: PurchasePreview,
    val isSelected: Boolean,
)

/**
 * Почему покупать сейчас нельзя и куда идти, чтобы стало можно.
 *
 * Отдельно от нехватки денег: та относится к одной строке, а это запрет
 * на всю панель. Единственный такой случай сегодня — не подтверждён план
 * расходов (`Rejection.PlanNotConfirmed`).
 */
data class CareBlock(
    val reason: String,
    val actionLabel: String,
    val onAction: () -> Unit,
)

/**
 * Выбор предмета для ухода: чем покормить, чем помыть.
 *
 * Требование ТЗ п. 2.5.6 целиком: до покупки видны цена, категория и то, как
 * предмет повлияет на питомца; покупка подтверждается отдельным нажатием;
 * если купить нельзя — сказано, почему именно и что сделать.
 *
 * Два шага, а не покупка по нажатию на строку, — это и есть подтверждение:
 * ребёнок сначала смотрит, что изменится, и только потом соглашается.
 *
 * [block] — покупка запрещена целиком. Тогда вместо «Купить» стоит кнопка,
 * которая ведёт туда, где запрет снимается: тупика с неработающей кнопкой
 * у ребёнка быть не должно.
 *
 * [confirmLabel] — что случится по нажатию. На главном это «Купить и покормить»:
 * дальше идёт игра, а деньги уходят, когда еда попала в рот.
 *
 * [allowShortage] — кнопку можно нажать и при нехватке денег. Так в магазине:
 * домен откажет, и экран покажет, чего не хватает и что делать (шаг 7
 * Приложения А — «попытка покупки при нехватке средств»).
 */
@Composable
fun CarePanel(
    title: String,
    options: List<CareOption>,
    onPick: (itemId: String) -> Unit,
    onConfirm: (itemId: String) -> Unit,
    /** null — панель без кнопки «Закрыть»: так она стоит на экране магазина. */
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    block: CareBlock? = null,
    confirmLabel: String = "Купить",
    allowShortage: Boolean = false,
) {
    val selected = options.firstOrNull { it.isSelected }

    FinneyPanel(title = title, onClose = onDismiss, modifier = modifier) {
        if (options.isEmpty()) {
            Text(
                text = "Пока нечего купить",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
            return@FinneyPanel
        }

        options.forEach { option ->
            CareRow(option = option, onPick = { onPick(option.item.id) })
        }

        Spacer(Modifier.padding(top = 4.dp))

        if (block != null) {
            Text(
                text = block.reason,
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
            FinneyButton(text = block.actionLabel, onClick = block.onAction)
            return@FinneyPanel
        }

        // Купить можно только выбранное и только когда хватает денег. У строки
        // с нехваткой уже написано, сколько не достаёт и что делать, — кнопка
        // молча неактивной не остаётся.
        FinneyButton(
            text = if (selected == null) "Выбери, что купить" else confirmLabel,
            onClick = { selected?.let { onConfirm(it.item.id) } },
            enabled = selected != null && (allowShortage || selected.preview.shortage == 0),
        )
    }
}

/**
 * Строка предмета.
 *
 * Категория и нехватка показаны значком и словом, а не одним цветом: ТЗ п. 3.6
 * запрещает цвет как единственный способ что-то сообщить.
 */
@Composable
private fun CareRow(option: CareOption, onPick: () -> Unit) {
    val preview = option.preview
    val notEnough = preview.shortage > 0

    val accent = when {
        notEnough -> FinneyPink
        option.isSelected -> FinneyGreen
        else -> null
    }

    FinneyCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Выбрать: ${option.item.label}", onClick = onPick),
        accent = accent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = option.item.label,
                style = MaterialTheme.typography.titleMedium,
                color = FinneyInk,
            )
            CoinAmount(amount = option.item.price)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val needed = option.item.category == Category.NEEDS
            FinneyIcon(
                icon = if (needed) FinneyIcons.Plan else FinneyIcons.Star,
                size = 20.dp,
            )
            Text(
                text = if (needed) "нужное" else "желаемое",
                style = MaterialTheme.typography.bodyMedium,
                color = FinneyInk,
            )
        }

        EffectLine("сытость", preview.statsBefore.satiety, preview.statsAfter.satiety)
        EffectLine("чистота", preview.statsBefore.hygiene, preview.statsAfter.hygiene)
        EffectLine("радость", preview.statsBefore.mood, preview.statsAfter.mood)

        if (notEnough) {
            Text(
                text = "Не хватает ${preview.shortage}. Выполни задание или закрой период",
                style = MaterialTheme.typography.bodyMedium,
                color = FinneyInk,
            )
        }
    }
}

/** Шкала до и после. Строка молчит, если предмет её не трогает. */
@Composable
private fun EffectLine(label: String, before: Int, after: Int) {
    if (before == after) return
    val delta = after - before
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$label: было $before, станет $after"
        },
    ) {
        Text(
            text = "$label $before → $after",
            style = MaterialTheme.typography.bodyMedium,
            color = FinneyInk,
        )
        Text(
            text = if (delta > 0) "+$delta" else "$delta",
            style = MaterialTheme.typography.bodyMedium,
            color = if (delta > 0) FinneyInk else FinneyPeach,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun CarePanelPreview() {
    val apple = ShopItem("food_apple", "Яблоко", 10, Category.NEEDS, effect = StatEffect(satiety = 20))
    val bowl = ShopItem("food_bowl", "Миска корма", 20, Category.NEEDS, effect = StatEffect(satiety = 45))
    val stats = PetStats(satiety = 30, hygiene = 60, mood = 70)

    FinneyTheme {
        CarePanel(
            title = "Покормить",
            options = listOf(
                CareOption(apple, PurchasePreview(apple, stats, stats.copy(satiety = 50), 0), isSelected = true),
                CareOption(bowl, PurchasePreview(bowl, stats, stats.copy(satiety = 75), 5), isSelected = false),
            ),
            onPick = {},
            onConfirm = {},
            onDismiss = {},
            modifier = Modifier.width(360.dp),
        )
    }
}
