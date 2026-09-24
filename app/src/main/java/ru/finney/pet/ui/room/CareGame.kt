package ru.finney.pet.ui.room

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finney.pet.R
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.OutlinedText

// Общее у игр ухода: кормления (FeedingGame) и мытья (WashingGame).
//
// Покупка в них случается в конце игры, а не на кнопке панели: ребёнок
// выбирает и соглашается в CarePanel (цена и эффект видны до покупки, ТЗ п. 2.5.6),
// а деньги уходят, когда еда попала в рот или питомец отмыт. Промахнулся —
// ничего не потрачено, можно ещё раз. Передумал — «Не сейчас», тоже бесплатно.

/** Где предмет ждёт, пока его возьмут: над кнопкой отмены, по центру. */
internal val ItemRestFromBottom = 200.dp

/** Размер предмета в руке. */
internal val ItemSize = 96.dp

/**
 * Рисунок предмета по id из магазина. null — дизайнеры его ещё не нарисовали,
 * тогда в руке значок. Файлы кладёт tools/pack_items.py.
 */
@DrawableRes
internal fun itemArt(itemId: String): Int? = when (itemId) {
    "food_apple" -> R.drawable.item_food_apple
    "food_bowl" -> R.drawable.item_food_bowl
    "care_soap" -> R.drawable.item_care_soap
    "care_towel" -> R.drawable.item_care_towel
    "treat_candy" -> R.drawable.item_treat_candy
    "toy_ball" -> R.drawable.item_toy_ball
    else -> null
}

/** Предмет в руке: рисунок, а если рисунка нет — значок комнаты. */
@Composable
internal fun ItemPicture(itemId: String, fallback: FinneyIcons, size: Dp, modifier: Modifier = Modifier) {
    val art = itemArt(itemId)
    if (art != null) {
        Image(painter = painterResource(art), contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            FinneyIcon(fallback, size = size * 0.7f)
        }
    }
}

/**
 * Рамка игры: подсказка сверху, «Не сейчас» снизу, сама игра между ними.
 *
 * Подсказка — крупным контурным шрифтом, как заголовки панелей: ребёнку 7 лет
 * нужно одно короткое действие, а не абзац.
 */
@Composable
internal fun CareGameFrame(
    hint: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
        ) {
            OutlinedText(
                text = hint,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // Ниже верхней полосы с деньгами и уровнем: её закрывать нельзя,
                    // деньги спишутся на глазах.
                    .padding(top = 80.dp),
            )
            FinneyButton(
                text = "Не сейчас",
                onClick = onCancel,
                fillWidth = false,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
