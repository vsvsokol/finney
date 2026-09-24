package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.model.ItemArt
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.tasks.TaskDetails
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach
import ru.finney.pet.ui.theme.FinneyYellow

// Единственное место, где движок задания встречается с экраном игры: какая
// игра, в какой сцене и что показать в итоге. Новый движок — ветка в каждой из
// трёх функций ниже. Как добавить — docs/minigames.md.

/**
 * Экран игры для задания. [inputError] — ядро не приняло ввод (например, «не хватает 5»);
 * игра показывает его и прячет, когда ребёнок что-то поменял ([onInputSeen]).
 */
@Composable
fun TaskGame(
    task: TaskDefinition,
    character: PetCharacter,
    balance: Int,
    inputError: TaskInputError?,
    onInputSeen: () -> Unit,
    onClose: () -> Unit,
    onSubmit: (TaskInput) -> Unit,
) {
    when (task) {
        is SorterTask -> SorterGame(task, character, balance, onClose, onSubmit)
        is BasketTask -> ShoppingGame(task, character, inputError, onInputSeen, onClose, onSubmit)
        is GoalRaceTask -> GoalRaceGame(task, character, onClose, onSubmit)
        is ReserveTask -> ReserveGame(task, character, inputError, onInputSeen, onClose, onSubmit)
        is StandTask -> StandGame(task, character, inputError, onInputSeen, onClose, onSubmit)
        is ChangeTask -> ChangeGame(task, character, onClose, onSubmit)
        // Учебные движки без своей сцены: в контенте их сейчас нет, экран — запасной.
        is DistributorTask -> EnvelopesGame(task, inputError, onInputSeen, onSubmit)
        is GoalSliderTask -> DepositGame(task, onSubmit)
    }
}

/** Сцена задания — и на вступлении, и в итоге. Лавка в итоге уже вечерняя. */
fun backdropFor(task: TaskDefinition, finished: Boolean = false): Backdrop = when (task) {
    is SorterTask -> Backdrop.ROOM
    is BasketTask -> Backdrop.SHOP
    is GoalRaceTask -> Backdrop.FIELD
    is ReserveTask -> if (finished) Backdrop.ROOM_RAIN else Backdrop.ROOM
    is StandTask -> if (finished) Backdrop.SUNSET else Backdrop.SKY
    is ChangeTask -> Backdrop.STORE
    is DistributorTask, is GoalSliderTask -> Backdrop.ROOM
}

/** Значок задания в списке — берётся из самого контента, отдельной картинки не нужно. */
fun taskIcon(task: TaskDefinition): ItemArt? = when (task) {
    is SorterTask -> task.items.firstOrNull()
    is BasketTask -> task.shelf.firstOrNull()
    is GoalRaceTask -> task.goal
    is ReserveTask -> task.surprise
    is StandTask -> task.ingredient
    is ChangeTask -> task.rounds.firstOrNull()
    is DistributorTask, is GoalSliderTask -> null
}

/**
 * Итог игры в панели: что получилось, по пунктам, как в концептах. Пояснение
 * «что делать дальше» — в explainOk / explainFail задания, его пишет контент.
 */
@Composable
fun ColumnScope.ResultBody(task: TaskDefinition, details: TaskDetails, input: TaskInput) {
    when (details) {
        is TaskDetails.Sorting -> {
            val items = (task as? SorterTask)?.items.orEmpty().associateBy { it.id }
            if (details.mistakes.isEmpty()) {
                ResultRow(null, "Всё разложено верно", ok = true)
            } else {
                details.mistakes.mapNotNull(items::get).forEach {
                    ResultRow(it, "${it.label} → «${categoryWord(it.category)}»", ok = false, note = it.why)
                }
            }
            Summary("Верно с первого раза: ${details.correct} из ${details.total}")
        }

        is TaskDetails.Basket -> {
            val basket = task as? BasketTask
            val cart = (input as? TaskInput.Basket)?.items.orEmpty()
            basket?.rules?.filter { it.label != null }?.forEach { rule ->
                ResultRow(null, rule.label!!, ok = TaskEngines.ruleMet(basket, rule, cart))
            }
            basket?.let { Summary("Потрачено ${details.total} из ${it.limit}, осталось ${it.limit - details.total}") }
        }

        is TaskDetails.GoalRace -> {
            val race = task as? GoalRaceTask
            race?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ItemPicture(it.goal, it.goal.label, 64.dp)
                    Column(Modifier.weight(1f)) {
                        SumRow("Копилка", "${details.saved} из ${it.goal.price}", strong = true)
                        Meter(details.saved.toFloat() / it.goal.price, Modifier.height(14.dp))
                    }
                }
            }
            if (details.shortfall > 0) Summary("Не хватило ${details.shortfall}")
            Summary("Соблазнов позволил себе: ${details.eventsTaken}")
        }

        is TaskDetails.Reserve -> {
            val reserve = task as? ReserveTask
            SumRow("Запас был", details.reserve.toString(), strong = true)
            reserve?.let { SumRow(it.surprise.label, it.surprise.price.toString()) }
            val dropped = (input as? TaskInput.Reserve)?.dropped.orEmpty()
            reserve?.spendings?.filter { it.id in dropped }?.forEach { ResultRow(it, "${it.label} — перенесли", ok = false) }
            ResultRow(null, if (details.shortage == 0) "Запаса хватило" else "Не хватило ${details.shortage}", ok = details.shortage == 0)
        }

        is TaskDetails.Stand -> {
            val stand = task as? StandTask
            LedgerRow("+", FinneyGreen, "Продал ${details.sold} стаканов", "+${details.earned}")
            LedgerRow("−", FinneyPeach, "Купил ${details.spent / (stand?.ingredient?.price ?: 1)} ${stand?.ingredient?.label?.lowercase().orEmpty()}", "−${details.spent}")
            LedgerRow("=", Color.White, "Заработал", details.kept.toString(), highlight = true)
            val notes = listOfNotNull(
                details.leftover.takeIf { it > 0 }?.let { "$it стакана не купили." },
                details.missed.takeIf { it > 0 }?.let { "$it гостям не хватило." },
            )
            if (notes.isNotEmpty()) Summary(notes.joinToString(" ") + " На всех гостей хватило бы ${details.best}.")
        }

        is TaskDetails.Change -> {
            val change = task as? ChangeTask
            val answers = (input as? TaskInput.Coins)?.rounds.orEmpty()
            change?.rounds?.forEachIndexed { i, round ->
                val coins = answers.getOrNull(i).orEmpty()
                val diff = details.results.getOrNull(i) ?: 0
                ResultRow(
                    round,
                    "${round.label}: ${if (coins.isEmpty()) "0" else coins.joinToString(" + ")}",
                    ok = diff == 0,
                    note = if (diff == 0) null else "нужно было ${round.target}",
                )
            }
            Summary("Верно с первого раза: ${details.correct} из ${details.total}")
        }

        is TaskDetails.Distribution -> details.goalSavedAfter?.let { Summary("В копилке станет $it, осталось ${details.goalRemaining}") }
        is TaskDetails.GoalSlider -> Summary(
            if (details.shortfall == 0) "Накопится ${details.collected} — хватает!" else "Накопится ${details.collected}, не хватит ${details.shortfall}",
        )
    }
}

/** Строка итога: картинка, текст, отметка ✓ или ↺. Отметка дублирует цвет символом (ТЗ п. 3.6). */
@Composable
private fun ResultRow(art: ItemArt?, text: String, ok: Boolean, note: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        art?.let { ItemPicture(it, text, 32.dp) }
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = FinneyInk.copy(alpha = 0.75f)) }
        }
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (ok) FinneyGreen else FinneyPeach).border(2.dp, FinneyInk, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(if (ok) "✓" else "↺", style = MaterialTheme.typography.labelLarge, color = FinneyInk) }
    }
}

/** Строка «истории», как в ките: значок операции, подпись, сумма. */
@Composable
private fun LedgerRow(sign: String, color: Color, text: String, amount: String, highlight: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) FinneyYellow else Color.Transparent)
            .padding(horizontal = if (highlight) 6.dp else 0.dp, vertical = 2.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(color).border(2.dp, FinneyInk, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(sign, style = MaterialTheme.typography.titleMedium, color = FinneyInk) }
        Text(text, style = MaterialTheme.typography.titleMedium, color = FinneyInk, modifier = Modifier.weight(1f))
        OutlinedText(amount, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Summary(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
}
