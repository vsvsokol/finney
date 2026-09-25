package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finney.pet.domain.model.ChangeMode
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink

// «Касса Финни», как мини-игры на ловкость в Tom, только на счёт. Над прилавком —
// покупатель и табло кассы, на прилавке — поднос, под ним — ящик с монетами.
// Два режима: отсчитать сдачу из ящика (монет сколько угодно) и заплатить ровно
// из кошелька (монеты считаны, потраченные бледнеют).
//
// В оценку идёт первая попытка каждого раунда. Ошибся — покупатель вежливо
// говорит, сколько лишнего или не хватает, и ребёнок поправляет сам.

/** Где на экране прилавок — доля высоты, как в фоне [Backdrop.STORE]. */
private const val DESK_AT = 0.42f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChangeGame(task: ChangeTask, character: PetCharacter, onClose: () -> Unit, onSubmit: (TaskInput) -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    var tray by remember { mutableStateOf(emptyList<Int>()) }
    var firsts by remember { mutableStateOf(emptyList<List<Int>>()) }
    var checked by remember { mutableStateOf<Int?>(null) }
    val current = task.rounds[round]
    val give = current.mode == ChangeMode.GIVE
    val solved = checked == 0
    // Покупатель — любой питомец, кроме своего; в каждом раунде новый.
    val others = PetCharacter.entries.filter { it != character }
    val customer = others[round % others.size]

    fun check() {
        if (firsts.size == round) firsts = firsts + listOf(tray)
        checked = TaskEngines.changeDiff(current, tray)
    }

    fun next() {
        if (round + 1 == task.rounds.size) {
            onSubmit(TaskInput.Coins(firsts))
            return
        }
        round++
        tray = emptyList()
        checked = null
    }

    GameScene(backdrop = Backdrop.STORE, onClose = onClose) {
        val density = LocalDensity.current
        val barsTop = with(density) { WindowInsets.systemBars.getTop(this).toDp() }
        val barsBottom = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Верх сцены кончается ровно у столешницы фона: покупатель стоит за прилавком, а не на нём.
            val screen = maxHeight + barsTop + barsBottom + 16.dp
            val aboveDesk = (screen * DESK_AT - barsTop - 8.dp).coerceAtLeast(HudHeight + 120.dp)

            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(aboveDesk).padding(top = HudHeight),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (give) {
                        Guest(customer, 120.dp, Modifier.width(120.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Bubble(customerLine(current.target, tray.sum(), checked, paid = current.paid ?: 0), Tail.LEFT)
                            Display(current.price, current.paid ?: 0, if (solved) current.target.toString() else "?")
                            ItemPicture(current, current.label, 44.dp)
                        }
                    } else {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ItemPicture(current, current.label, 60.dp)
                                PriceTag(current.price.toString())
                            }
                            Bubble(ownLine(current.price, tray.sum(), checked), Tail.RIGHT)
                        }
                        ScenePet(character, 110.dp, Modifier.width(110.dp))
                    }
                }

                // Поднос и подсказка прокручиваются, а ящик с монетами и кнопка стоят
                // у нижнего края. Раньше поднос рос с каждой монетой и сталкивал ящик
                // вниз — следующее нажатие попадало мимо монеты.
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 30.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Раунд ${round + 1} из ${task.rounds.size}", style = MaterialTheme.typography.labelLarge, color = FinneyInk)
                    Tray(
                        label = if (give) "Сдача покупателю" else "На кассу · нужно ровно ${current.price}",
                        coins = tray,
                        solved = solved,
                        onRemove = { i -> tray = tray.filterIndexed { j, _ -> j != i }; checked = null },
                    )
                    val miss = checked
                    if (miss != null && miss != 0) {
                        ScenePanel(title = null, modifier = Modifier.fillMaxWidth()) {
                            Text(explain(give, current.price, current.paid ?: 0, current.target, tray.sum()), style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (give) Drawer(task.coins, enabled = !solved) { tray = tray + it; checked = null }
                    else Wallet(current.wallet, tray, enabled = !solved) { tray = tray + it; checked = null }

                    if (solved) {
                        FinneyButton(text = if (round + 1 == task.rounds.size) "Готово" else "Следующий покупатель", onClick = ::next)
                    } else {
                        FinneyButton(text = "Проверить", onClick = ::check, enabled = tray.isNotEmpty())
                    }
                }
            }
        }
    }
}

/** Реплика покупателя: просит сдачу, а после проверки — вежливо говорит, что не так. */
private fun customerLine(target: Int, sum: Int, checked: Int?, paid: Int): String = when {
    checked == null -> "Вот $paid!"
    checked == 0 -> "Спасибо! Сдача верная"
    checked > 0 -> "Тут $sum, а сдача — $target. Лишнее: $checked"
    else -> "Тут $sum, а сдача — $target. Не хватает ${-checked}"
}

private fun ownLine(price: Int, sum: Int, checked: Int?): String = when {
    checked == null -> "Заплачу ровно $price, без сдачи!"
    checked == 0 -> "Ровно! Сдача не нужна"
    checked > 0 -> "Тут $sum — это больше $price на $checked"
    else -> "Тут $sum — не хватает ${-checked}"
}

private fun explain(give: Boolean, price: Int, paid: Int, target: Int, sum: Int): String =
    if (give) "Цена $price, дали $paid. Сдача $paid − $price = $target. Сейчас на подносе $sum — поправь."
    else "Стоит $price, на кассе $sum. Убери лишнее или добавь — и проверь ещё раз."

/** Табло кассы: тёмный экран с зелёными цифрами. */
@Composable
private fun Display(price: Int, paid: Int, change: String) {
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FinneyInk)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text("Цена  $price", style = mono, color = Color(0xFF9FF0A8))
        Text("Дали  $paid", style = mono, color = Color(0xFF9FF0A8))
        Text("Сдача $change", style = mono, color = Color.White)
    }
}

/** Поднос на прилавке: монеты, которые отдаём. Нажатие на монету убирает её. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tray(label: String, coins: List<Int>, solved: Boolean, onRemove: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(FinneyCream)
            .border(4.dp, FinneyInk, RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row {
            Text(label, style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
            Text(
                if (coins.isEmpty()) "0" else coins.joinToString(" + ") + " = ${coins.sum()}",
                style = MaterialTheme.typography.titleMedium,
                color = FinneyInk,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        ) {
            coins.forEachIndexed { i, coin ->
                DenominationCoin(
                    coin,
                    Modifier.clickable(enabled = !solved, role = Role.Button, onClickLabel = "Убрать монету $coin") { onRemove(i) },
                )
            }
            if (solved) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(FinneyGreen).border(3.dp, FinneyInk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("✓", style = MaterialTheme.typography.titleLarge, color = FinneyInk) }
            }
            if (coins.isEmpty()) Text("нажми на монету внизу", style = MaterialTheme.typography.bodyMedium, color = FinneyInk)
        }
    }
}

/** Ящик кассы: по ячейке на номинал, монет сколько угодно. */
@Composable
private fun Drawer(coins: List<Int>, enabled: Boolean, onTake: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF8C6A55))
            .border(4.dp, FinneyInk, RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        coins.forEach { coin ->
            Box(
                Modifier
                    .weight(1f)
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF6E5242))
                    .border(3.dp, FinneyInk, RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Положить монету $coin") { onTake(coin) },
                contentAlignment = Alignment.Center,
            ) { DenominationCoin(coin, size = 56.dp) }
        }
    }
}

/** Кошелёк Финни: каждая монета своя, потраченные бледнеют и не нажимаются. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Wallet(wallet: List<Int>, tray: List<Int>, enabled: Boolean, onTake: (Int) -> Unit) {
    val used = tray.groupingBy { it }.eachCount().toMutableMap()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(FinneyPink)
            .border(4.dp, FinneyInk, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ru.finney.pet.ui.components.OutlinedText("Кошелёк", style = MaterialTheme.typography.titleLarge, fill = Color.White)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            wallet.sortedDescending().forEach { coin ->
                val spent = (used[coin] ?: 0) > 0
                if (spent) used[coin] = used.getValue(coin) - 1
                DenominationCoin(
                    value = coin,
                    size = 56.dp,
                    faded = spent,
                    modifier = Modifier.clickable(enabled = enabled && !spent, role = Role.Button, onClickLabel = "Положить монету $coin") {
                        onTake(coin)
                    },
                )
            }
        }
    }
}
