package ru.finney.pet.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.EntryType
import ru.finney.pet.ui.components.CheckBadge
import ru.finney.pet.ui.components.Coin
import ru.finney.pet.ui.components.FillBar
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyCard
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.room.itemFallback
import ru.finney.pet.ui.tasks.games.ItemPicture
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.room.ItemPicture as RoomItemPicture

/** Стадия роста = уровень: у каждого из шести уровней своё имя. */
private val StageNames = mapOf(
    1 to "малыш",
    2 to "карапуз",
    3 to "непоседа",
    4 to "школьник",
    5 to "подросток",
    6 to "взрослый",
)

@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenPeriodResult: (periodNumber: Int) -> Unit,
    viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        ProgressUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }

        is ProgressUiState.Ready -> FinneyScreen(
            scrollable = true,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            onClose = onBack,
        ) {
            OutlinedText("Итоги и история", style = MaterialTheme.typography.headlineLarge)

            FinneyPanel(title = "${s.petName} растёт") {
                GaugeLine("Уровень", s.level, s.maxLevel)
                Line("Стадия", StageNames[s.stage] ?: s.stage.toString())
                Line("Целей достигнуто", s.goalsCompleted.toString())
            }

            FinneyPanel(title = "Цель") {
                val goal = s.goal
                if (goal == null) {
                    Body("Цель пока не выбрана — выбери её в копилке.")
                } else {
                    GaugeLine(goal.goal.label, goal.saved, goal.goal.price)
                    Line("Осталось собрать", goal.remaining.toString())
                }
            }

            s.lastClosedPeriod?.let { number ->
                FinneyButton(text = "Итоги уровня ${s.lastClosedLevel}", onClick = { onOpenPeriodResult(number) })
            }

            FinneyPanel(title = "Мини-игры") {
                if (s.tasks.isEmpty()) {
                    Body("Здесь появятся мини-игры, которые ты попробуешь.")
                }
                s.tasks.forEach { task -> GameLine(task) }
            }

            FinneyPanel(title = "История") {
                s.history.forEach { period ->
                    Text("Уровень ${period.level}", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                    period.rows.forEach { HistoryLine(it) }
                }
            }

            FinneyButton(text = "Справочник", onClick = onOpenGlossary)
        }
    }
}

/**
 * Мини-игра, которую пробовали: её картинка — та же, что в сцене игры, — название
 * и исход. Исход — словом и значком, не только цветом карточки (ТЗ п. 3.6).
 * Тема отсюда убрана: ребёнку она ничего не говорит, взрослый видит её в своём разделе.
 */
@Composable
private fun GameLine(task: TaskRow) {
    FinneyCard(accent = if (task.passed) FinneyGreen else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            task.icon?.let { ItemPicture(it, task.title, 48.dp) }
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
                Body(if (task.passed) "Пройдено, попыток: ${task.attempts}" else "Пока не вышло, попыток: ${task.attempts}")
            }
            if (task.passed) CheckBadge(size = 28.dp)
        }
    }
}

@Composable
private fun HistoryLine(row: HistoryRow) {
    // Движение по копилке показываем отдельно: деньги с баланса ушли, но не потрачены.
    val amount = when {
        row.balanceDelta != 0 -> signed(row.balanceDelta)
        row.savingsDelta != 0 -> "копилка ${signed(row.savingsDelta)}"
        else -> "0"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "${row.label}: $amount" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIcon(row)
        Text(row.label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
        Text(amount, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
    }
}

/**
 * Значок строки истории: у награды — картинка мини-игры, у покупки — сам предмет,
 * у остального — монета, копилка или кубок. По значку строку находят, не читая её.
 */
@Composable
private fun EntryIcon(row: HistoryRow) {
    val size = 32.dp
    when (row.type) {
        EntryType.TASK_REWARD -> row.game?.let { ItemPicture(it, row.label, size) } ?: FinneyIcon(FinneyIcons.Star, size = size)
        EntryType.PURCHASE -> row.itemId?.let { RoomItemPicture(it, itemFallback(it), size) } ?: FinneyIcon(FinneyIcons.Cart, size = size)
        EntryType.SAVINGS_DEPOSIT, EntryType.SAVINGS_WITHDRAW -> FinneyIcon(FinneyIcons.Piggy, size = size)
        EntryType.GOAL_COMPLETE -> FinneyIcon(FinneyIcons.Trophy, size = size)
        EntryType.INCOME, EntryType.PARENT_BONUS -> Coin(size = size)
    }
}

/** Строка «сколько из скольки» с полосой под ней: цифры не голые (плейтест, п. 5). */
@Composable
private fun GaugeLine(label: String, value: Int, max: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Line(label, "$value из $max")
        FillBar(value, max, Modifier.fillMaxWidth())
    }
}

private fun signed(value: Int) = if (value > 0) "+$value" else value.toString()

@Composable
private fun Line(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
}
