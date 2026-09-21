package ru.finney.pet.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.game.GoalProgress
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.Goal
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyPink
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Черновик под вёрстку [@zYafALL]. Шаг сумм тот же, что на экране плана.
private const val STEP = 5

@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        GoalsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }

        is GoalsUiState.Ready -> GoalsContent(
            state = s,
            onSelect = viewModel::selectGoal,
            onDeposit = { viewModel.deposit(STEP) },
            onWithdraw = { viewModel.withdraw(STEP) },
            onComplete = viewModel::completeGoal,
            onBack = onBack,
        )
    }
}

@Composable
private fun GoalsContent(
    state: GoalsUiState.Ready,
    onSelect: (String) -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    FinneyScreen(scrollable = true, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedText("Копилка", style = MaterialTheme.typography.headlineLarge)

        FinneyPanel(title = "На что копим") {
            state.goals.forEach { row ->
                GoalCard(row = row, onSelect = { onSelect(row.goal.id) })
            }
        }

        state.progress?.let { progress ->
            FinneyPanel(title = progress.goal.label) {
                InfoRow("Накоплено", "${progress.saved} из ${progress.goal.price}")
                InfoRow("Осталось собрать", progress.remaining.toString())
                InfoRow(
                    "Периодов до цели",
                    progress.periodsToGoal?.toString() ?: "пока не посчитать",
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("На балансе:", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            CoinAmount(amount = state.balance)
        }

        if (state.progress != null) {
            FinneyButton(
                text = "Отложить $STEP",
                onClick = onDeposit,
                enabled = state.canMoveMoney && state.balance >= STEP,
            )
            FinneyButton(
                text = "Снять $STEP",
                onClick = onWithdraw,
                enabled = state.canMoveMoney && state.progress.saved >= STEP,
            )
            if (state.canComplete) {
                FinneyButton(text = "Цель достигнута!", onClick = onComplete)
            }
        }

        if (!state.canMoveMoney) {
            Text(
                text = "Сначала подтверди план периода — тогда можно будет откладывать.",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
        }

        state.rejection?.let { RejectionNote(it) }

        FinneyButton(text = "Назад", onClick = onBack)
    }
}

/** Выбор показан подписью «копим сюда», а не только рамкой (ТЗ п. 3.6). */
@Composable
private fun GoalCard(row: GoalRow, onSelect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (row.isActive) FinneyYellow else FinneySand)
            .border(if (row.isActive) 3.dp else 2.dp, FinneyInk, RoundedCornerShape(20.dp))
            .clickable(enabled = !row.isCompleted, onClick = onSelect)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = row.goal.label,
            style = MaterialTheme.typography.titleMedium,
            color = FinneyInk,
        )
        Text(
            text = when {
                row.isCompleted -> "уже купили"
                row.isActive -> "копим сюда · ${row.saved} из ${row.goal.price}"
                else -> "${row.saved} из ${row.goal.price}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = FinneyInk,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = FinneyInk)
    }
}

/** Тексты временные, до `feedback.json` [@vsvsokol] — как на экране плана. */
@Composable
private fun RejectionNote(rejection: Rejection) {
    val text = when (rejection) {
        is Rejection.InsufficientFunds -> "Не хватает ${rejection.shortage} финок."
        is Rejection.InsufficientSavings -> "В копилке пока только ${rejection.saved}."
        Rejection.NoActiveGoal -> "Сначала выбери цель."
        Rejection.PlanNotConfirmed -> "Сначала подтверди план периода."
        Rejection.InvalidAmount -> "Так не получится."
        else -> "Так пока нельзя."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = FinneyInk,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinneyPink)
            .border(2.dp, FinneyInk, RoundedCornerShape(16.dp))
            .padding(12.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun GoalsPreview() {
    val crown = Goal(id = "goal_crown", label = "Корона", price = 50, moodBonus = 20)
    FinneyTheme {
        GoalsContent(
            state = GoalsUiState.Ready(
                goals = listOf(GoalRow(crown, saved = 20, isActive = true, isCompleted = false)),
                progress = GoalProgress(
                    goal = crown,
                    saved = 20,
                    remaining = 30,
                    periodsToGoal = 2,
                    averageDeposit = 10,
                ),
                balance = 30,
                canMoveMoney = true,
                canComplete = false,
                rejection = null,
            ),
            onSelect = {},
            onDeposit = {},
            onWithdraw = {},
            onComplete = {},
            onBack = {},
        )
    }
}
