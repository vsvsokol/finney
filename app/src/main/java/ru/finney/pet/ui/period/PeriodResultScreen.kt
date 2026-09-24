package ru.finney.pet.ui.period

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.LevelBadge
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.FinneyYellow

// Черновик под вёрстку @zYafALL: разметка простая, контракт ViewModel останется.
// Что показывать, решает PeriodResultViewModel — экран только раскладывает готовое.

@Composable
fun PeriodResultScreen(
    periodNumber: Int,
    onBack: () -> Unit,
    viewModel: PeriodResultViewModel = viewModel(factory = PeriodResultViewModel.factory(periodNumber)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        PeriodResultUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }

        PeriodResultUiState.Unavailable -> FinneyScreen {
            OutlinedText("Итоги периода", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Этот период ещё не закрыт.",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
            FinneyButton(text = "На главный", onClick = onBack)
        }

        is PeriodResultUiState.Ready -> PeriodResultContent(state = s, onBack = onBack)
    }
}

@Composable
private fun PeriodResultContent(state: PeriodResultUiState.Ready, onBack: () -> Unit) {
    FinneyScreen(scrollable = true, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedText("Итоги периода ${state.periodNumber}", style = MaterialTheme.typography.headlineLarge)

        FinneyPanel(title = "Как вышло") {
            FactRow("Нужное", state.facts.needs, state.plan.needs)
            FactRow("Желаемое", state.facts.wants, state.plan.wants)
            FactRow("Копилка", state.facts.savings, state.plan.savings)
            if (state.facts.unplannedIncome > 0) {
                FactRow("Пришло сверх плана", state.facts.unplannedIncome, state.facts.unplannedIncome)
            }
        }

        // Каждое условие очков отдельной строкой: ребёнку видно, за что дали и чего не хватило.
        FinneyPanel(title = "За что очки") {
            ScoreRow("Питомец накормлен и чист", state.needsCovered)
            ScoreRow("План выполнен", state.planMatched)
            ScoreRow("В копилку отложено", state.savingsAdded)
            ScoreRow("Заданий пройдено: ${state.successfulTasks}", state.successfulTasks > 0)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LevelBadge(level = state.level)
            Text(
                text = if (state.leveledUp) "Новый уровень!" else "Очков за период: ${state.pointsEarned}",
                style = MaterialTheme.typography.titleLarge,
                color = FinneyInk,
            )
        }
        if (state.harderGames.isNotEmpty()) {
            Text(
                text = "Новые задания в мини-играх: ${state.harderGames.joinToString { "«$it»" }}",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Осталось:", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            CoinAmount(amount = state.balance)
        }

        // Итог словами, а не только цветом плашки (ТЗ п. 3.6).
        Text(
            text = if (state.planMatched) "План выполнен!" else "В следующий раз получится точнее",
            style = MaterialTheme.typography.titleLarge,
            color = FinneyInk,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (state.planMatched) FinneyGreen else FinneyYellow)
                .border(3.dp, FinneyInk, RoundedCornerShape(20.dp))
                .padding(14.dp),
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Дальше — период ${state.nextPeriodNumber}.",
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
        )

        FinneyButton(text = "На главный", onClick = onBack)
    }
}

/** Строка «потрачено из запланированного». */
@Composable
private fun FactRow(label: String, fact: Int, planned: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = FinneyInk,
            modifier = Modifier.weight(1f),
        )
        Text(text = "$fact из $planned", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
    }
}

/** Условие очков: галочка или прочерк плюс подпись — цвет здесь ничего не решает. */
@Composable
private fun ScoreRow(label: String, earned: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (earned) "✓" else "—",
            style = MaterialTheme.typography.titleLarge,
            color = FinneyInk,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun PeriodResultPreview() {
    FinneyTheme {
        PeriodResultContent(
            state = PeriodResultUiState.Ready(
                periodNumber = 1,
                plan = Plan(budget = 50, needs = 20, wants = 10, savings = 10),
                facts = PeriodFacts(needs = 20, wants = 5, savings = 10, unplannedIncome = 0),
                needsCovered = true,
                planMatched = true,
                savingsAdded = true,
                successfulTasks = 1,
                pointsEarned = 5,
                totalPoints = 5,
                level = 2,
                leveledUp = true,
                harderGames = listOf("Касса Финни"),
                balance = 15,
                nextPeriodNumber = 2,
            ),
            onBack = {},
        )
    }
}
