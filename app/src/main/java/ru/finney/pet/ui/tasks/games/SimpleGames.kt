package ru.finney.pet.ui.tasks.games

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPeach

// Простые экраны для двух первых движков — чтобы старые задания из tasks.json
// тоже можно было пройти. Шаг сумм 5, как на экране плана.

private const val STEP = 5

/** «Конверты»: разложить сумму по корзинам кнопками «−» и «+». */
@Composable
internal fun EnvelopesGame(
    task: DistributorTask,
    inputError: TaskInputError?,
    onInputSeen: () -> Unit,
    onSubmit: (TaskInput) -> Unit,
) {
    var amounts by remember { mutableStateOf(task.baskets.associate { it.id to 0 }) }
    val left = task.amount - amounts.values.sum()

    MiniGameFrame(
        hint = "Разложи ${task.amount} по конвертам",
        scrollable = true,
        bottom = {
            SumRow("Осталось разложить", left.toString(), strong = true)
            FinneyButton(text = "Готово", onClick = { onSubmit(TaskInput.Distribution(amounts)) }, enabled = left == 0)
        },
    ) {
        task.baskets.forEach { basket ->
            val value = amounts.getValue(basket.id)
            GameCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedText(basket.label, style = MaterialTheme.typography.titleLarge)
                basket.hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = FinneyInk) }
                Stepper(
                    value = value.toString(),
                    onMinus = { amounts = amounts + (basket.id to value - STEP); onInputSeen() },
                    onPlus = { amounts = amounts + (basket.id to value + STEP); onInputSeen() },
                    minusEnabled = value >= STEP,
                    plusEnabled = left >= STEP,
                    what = basket.label,
                )
            }
        }
        task.goal?.let { goal ->
            val after = goal.saved + amounts.getValue(goal.basket)
            Note("${goal.label}: станет накоплено $after из ${goal.price}, осталось ${maxOf(0, goal.price - after)}.")
        }
        if (inputError != null) Note(inputErrorText(inputError), color = FinneyPeach)
    }
}

/** «Взнос»: подобрать, сколько откладывать каждый период. */
@Composable
internal fun DepositGame(task: GoalSliderTask, onSubmit: (TaskInput) -> Unit) {
    var deposit by remember { mutableIntStateOf(0) }
    val collected = deposit * task.periods

    MiniGameFrame(
        hint = "Сколько откладывать?",
        bottom = { FinneyButton(text = "Готово", onClick = { onSubmit(TaskInput.Deposit(deposit)) }) },
    ) {
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Stepper(
                value = deposit.toString(),
                onMinus = { deposit -= task.step },
                onPlus = { deposit += task.step },
                minusEnabled = deposit >= task.step,
                plusEnabled = deposit + task.step <= task.incomePerPeriod,
                what = "взнос",
            )
            SumRow("Доход за период", task.incomePerPeriod.toString())
            SumRow("Периодов", task.periods.toString())
            SumRow("Накопится", "$deposit × ${task.periods} = $collected", strong = true)
            SumRow("Нужно", task.goalPrice.toString(), strong = true)
        }
    }
}
