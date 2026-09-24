package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.ShelfItem
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyGreenDark
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneyYellow

// «Список покупок», как магазин в Pou и Tom, но с тележкой и списком. Товары
// стоят на полках с ценниками, нажатие кладёт в тележку. Касса не пропускает
// перебор — тот же отказ, что в магазине игры: «не хватает N». Что убрать,
// ребёнок решает сам, прямо на кассе.

private val Wood = Color(0xFFA0715A)

@Composable
internal fun ShoppingGame(
    task: BasketTask,
    character: PetCharacter,
    inputError: TaskInputError?,
    onInputSeen: () -> Unit,
    onClose: () -> Unit,
    onSubmit: (TaskInput) -> Unit,
) {
    var cart by remember { mutableStateOf(task.preloaded.toSet()) }
    var atCheckout by remember { mutableStateOf(false) }
    val total = task.shelf.filter { it.id in cart }.sumOf { it.price }

    // Касса не приняла — ребёнок остаётся на кассе и видит, сколько не хватает.
    LaunchedEffect(inputError) { if (inputError is TaskInputError.OverLimit) atCheckout = true }

    GameScene(backdrop = Backdrop.SHOP, onClose = onClose, money = task.limit) {
        if (atCheckout) {
            Checkout(
                task = task,
                cart = cart,
                total = total,
                shortage = (inputError as? TaskInputError.OverLimit)?.shortage,
                character = character,
                onRemove = { cart = cart - it; onInputSeen() },
                onBack = { atCheckout = false; onInputSeen() },
                onPay = { onSubmit(TaskInput.Basket(cart)) },
            )
        } else {
            Shelves(
                task = task,
                cart = cart,
                total = total,
                character = character,
                onToggle = { id -> cart = if (id in cart) cart - id else cart + id },
                onCheckout = { onSubmit(TaskInput.Basket(cart)) },
            )
        }
    }
}

@Composable
private fun Shelves(
    task: BasketTask,
    cart: Set<String>,
    total: Int,
    character: PetCharacter,
    onToggle: (String) -> Unit,
    onCheckout: () -> Unit,
) {
    val listRules = task.rules.filter { it.label != null }
    Column(Modifier.fillMaxSize().padding(top = HudHeight)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                if (listRules.isNotEmpty()) ShoppingList(task, listRules, cart, Modifier.weight(1f))
                ScenePet(character, 90.dp, Modifier.width(90.dp))
            }
            val hint = if (task.shelf.any { it.qty > 1 }) "Где больше за монету?" else "Сначала — по списку!"
            Bubble(hint, Tail.RIGHT, Modifier.align(Alignment.End))

            task.shelf.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    row.forEach { item -> Goods(item, inCart = item.id in cart) { onToggle(item.id) } }
                }
                Shelf()
            }
        }
        CartBar(total = total, limit = task.limit, enabled = cart.isNotEmpty(), onCheckout = onCheckout)
    }
}

/** Листок со списком: галочка ставится сама, когда правило выполнено. */
@Composable
private fun ShoppingList(task: BasketTask, rules: List<BasketRule>, cart: Set<String>, modifier: Modifier) {
    Column(
        modifier = modifier
            .padding(top = 4.dp, end = 8.dp)
            .rotate(-3f)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(Color.White)
            .border(3.dp, FinneyInk, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedText("Список", style = MaterialTheme.typography.titleLarge)
        rules.forEach { rule ->
            val done = TaskEngines.ruleMet(task, rule, cart)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(if (done) FinneyPeach else FinneyYellow)
                        .border(2.dp, FinneyInk, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (done) Text("✕", style = MaterialTheme.typography.labelMedium, color = FinneyInk) }
                Text(
                    rule.label!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinneyInk,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
            }
        }
    }
}

/** Товар на полке: рисунок и ценник «монетка · 2 шт · 10». В тележке — розовая подсветка и галочка. */
@Composable
private fun Goods(item: ShelfItem, inCart: Boolean, onToggle: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (inCart) FinneyPink.copy(alpha = 0.18f) else Color.Transparent)
            .toggleable(value = inCart, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(4.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (item.qty > 1) {
                // Упаковка — несколько штук рядом: сразу видно, что их больше.
                Row(horizontalArrangement = Arrangement.spacedBy((-18).dp)) {
                    repeat(minOf(item.qty, 3)) { ItemPicture(item, item.label, 40.dp) }
                }
            } else {
                ItemPicture(item, item.label, 56.dp)
            }
            if (inCart) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(FinneyGreen).border(2.dp, FinneyInk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("✓", style = MaterialTheme.typography.labelMedium, color = FinneyInk) }
            }
        }
        Text(item.label, style = MaterialTheme.typography.labelMedium, color = FinneyInk, textAlign = TextAlign.Center)
        PriceTag(if (item.qty > 1) "${item.qty} шт · ${item.price}" else item.price.toString())
    }
}

@Composable
internal fun PriceTag(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(2.dp, FinneyInk, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp),
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(FinneyGreen).border(2.dp, FinneyGreenDark, CircleShape))
        Text(text, style = MaterialTheme.typography.labelLarge, color = FinneyInk)
    }
}

/** Полка — деревянная доска с обводкой во всю ширину. */
@Composable
private fun Shelf() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .background(Wood)
            .border(3.dp, FinneyInk),
    )
}

/** Тележка: сколько набрано, сколько останется, и кнопка на кассу. */
@Composable
private fun CartBar(total: Int, limit: Int, enabled: Boolean, onCheckout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(FinneyCream)
            .border(4.dp, FinneyInk, RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text("🛒 $total", style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
                Text(
                    if (total <= limit) "осталось ${limit - total}" else "не хватит ${total - limit}",
                    style = MaterialTheme.typography.titleMedium,
                    color = FinneyInk,
                )
            }
            Meter(total.toFloat() / limit, Modifier.height(14.dp), color = if (total <= limit) FinneyGreen else FinneyPeach)
        }
        FinneyButton(text = "На кассу", onClick = onCheckout, enabled = enabled, fillWidth = false)
    }
}

/** Касса: чек, «не хватает N», кнопка «−» у каждой строки. */
@Composable
private fun Checkout(
    task: BasketTask,
    cart: Set<String>,
    total: Int,
    shortage: Int?,
    character: PetCharacter,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
    onPay: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(top = HudHeight).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScenePanel(title = "Касса", modifier = Modifier.fillMaxWidth()) {
            if (shortage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FinneyPeach)
                        .border(3.dp, FinneyInk, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(FinneyCream).border(2.dp, FinneyInk, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("!", style = MaterialTheme.typography.titleMedium, color = FinneyInk) }
                    Text("Не хватает $shortage монет", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                }
            }
            task.shelf.filter { it.id in cart }.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemPicture(item, item.label, 32.dp)
                    Text(item.label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
                    OutlinedText(item.price.toString(), style = MaterialTheme.typography.titleLarge)
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FinneyPeach)
                            .border(3.dp, FinneyInk, CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Убрать «${item.label}»") { onRemove(item.id) },
                        contentAlignment = Alignment.Center,
                    ) { Text("−", style = MaterialTheme.typography.titleLarge, color = FinneyInk) }
                }
            }
            Row {
                Text("Итого $total", style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
                Text("в кошельке ${task.limit}", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScenePet(character, 100.dp, Modifier.width(100.dp))
            Bubble(
                if (shortage != null) "Что-то подождёт — убери лишнее" else "Всё по списку? Платим!",
                Tail.LEFT,
                Modifier.weight(1f),
            )
        }
        FinneyButton(text = "Оплатить", onClick = onPay, enabled = cart.isNotEmpty())
        FinneyButton(text = "Вернуться к полкам", onClick = onBack)
    }
}
