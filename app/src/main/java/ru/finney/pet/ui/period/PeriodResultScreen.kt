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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Sfx
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
            OutlinedText("Итоги уровня", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Этот уровень ещё не завершён.",
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
    // Новый уровень — праздничный джингл. Один раз на экран: поворот его не повторяет.
    val sounds = LocalSounds.current
    var celebrated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.leveledUp) {
        if (state.leveledUp && !celebrated) {
            sounds.play(Sfx.LevelUp)
            celebrated = true
        }
    }
    // «На главный» закреплена внизу: разбор длиннее экрана, и в конце прокрутки
    // кнопку не находили — выйти можно было только системным «назад».
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        bottom = { FinneyButton(text = "На главный", onClick = onBack) },
    ) {
        OutlinedText("Итоги уровня ${state.playedLevel}", style = MaterialTheme.typography.headlineLarge)

        // Главное — сразу и словами, а не только цветом плашки (ТЗ п. 3.6).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LevelBadge(level = state.level)
            Text(
                text = when {
                    state.leveledUp -> "Уровень пройден! Теперь уровень ${state.level}."
                    state.passed -> "Уровень пройден!"
                    else -> "Уровень пока не пройден. Попробуй ещё раз — деньги уже пришли."
                },
                style = MaterialTheme.typography.titleLarge,
                color = FinneyInk,
            )
        }

        FinneyPanel(title = "Как вышло") {
            FactRow("Нужное", state.facts.needs, state.plan.needs)
            FactRow("Желаемое", state.facts.wants, state.plan.wants)
            FactRow("Копилка", state.facts.savings, state.plan.savings)
            if (state.facts.unplannedIncome > 0) {
                FactRow("Пришло сверх плана", state.facts.unplannedIncome, state.facts.unplannedIncome)
            }
        }

        // Каждое условие отдельной строкой: ребёнку видно, что получилось и чего не хватило.
        FinneyPanel(title = "Условия уровня: нужно ${state.toPass} из 3") {
            // Коротко: знак и одно слово — про что условие. Полная фраза — для TalkBack.
            state.levelGame?.let { ScoreRow("Игра «$it»", state.gamePassed, "Игра уровня «$it» — обязательно") }
            ScoreRow("Нужное", state.needsCovered, "Питомец сыт, чист и выспался")
            ScoreRow("План", state.planMatched, "Траты по плану")
            ScoreRow("Копилка", state.savingsAdded, "Отложено в копилку")
        }

        if (state.harderGames.isNotEmpty()) {
            Text(
                text = "Новые задания в мини-играх: ${state.harderGames.joinToString { "«$it»" }}",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
            )
        }

        CoinAmount(amount = state.balance)

        // План отдельно: это главный навык игры (ТЗ п. 2.5.5).
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

/** Условие уровня: ✓ или ↺ и одно слово — про что оно; цвет здесь ничего не решает. */
@Composable
private fun ScoreRow(label: String, earned: Boolean, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = "$description: ${if (earned) "выполнено" else "не выполнено"}"
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (earned) "✓" else "↺",
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
                playedLevel = 1,
                plan = Plan(budget = 50, needs = 20, wants = 10, savings = 10),
                facts = PeriodFacts(needs = 20, wants = 5, savings = 10, unplannedIncome = 0),
                needsCovered = true,
                planMatched = true,
                savingsAdded = true,
                toPass = 2,
                passed = true,
                level = 2,
                leveledUp = true,
                harderGames = listOf("Касса Финни"),
                balance = 15,
            ),
            onBack = {},
        )
    }
}
