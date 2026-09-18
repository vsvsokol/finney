package ru.finney.pet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Монета и уровень из UI-кита. Монета — зелёный кружок с «Ф» (финки, игровая
// валюта), уровень — жёлтый кружок с цифрой. Нарисованы кодом, а не картинкой:
// графики в ресурсах пока нет, а формы простые.

/**
 * Монета игровой валюты. Только значок, без суммы — сумму ставит [CoinAmount].
 */
@Composable
fun Coin(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(FinneyGreen)
            .border(size * 0.09f, FinneyInk, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Кегль привязан к размеру кружка, а не к настройкам шрифта системы:
        // это значок, и буква должна помещаться в монету при любом масштабе текста.
        OutlinedText(
            text = "Ф",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = (size.value * 0.5f).sp),
            fill = FinneyCream,
        )
    }
}

/**
 * Сумма с монетой: баланс на главном, цена товара, размер награды.
 * Для TalkBack читается одной фразой, иначе он произносит «Ф» отдельной буквой.
 */
@Composable
fun CoinAmount(
    amount: Int,
    modifier: Modifier = Modifier,
    coinSize: Dp = 28.dp,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$amount финок" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedText(amount.toString(), style = MaterialTheme.typography.titleLarge)
        Coin(size = coinSize)
    }
}

/** Уровень игрока — жёлтый кружок с цифрой, как в макете. */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(FinneyYellow)
            .border(size * 0.06f, FinneyInk, CircleShape)
            .clearAndSetSemantics { contentDescription = "Уровень $level" },
        contentAlignment = Alignment.Center,
    ) {
        OutlinedText(
            text = level.toString(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun MoneyPreview() {
    FinneyTheme {
        Column(
            modifier = Modifier.background(FinneyCream).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoinAmount(amount = 120)
            LevelBadge(level = 1)
        }
    }
}
