package ru.finney.pet.ui.period

import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.ItemArt
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.ui.components.CheckBadge
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FinneyIcon
import ru.finney.pet.ui.components.FinneyIcons
import ru.finney.pet.ui.components.SavingsIcon
import ru.finney.pet.ui.components.categoryIcon
import ru.finney.pet.ui.components.FillBar
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.SpendBar
import ru.finney.pet.ui.tasks.games.ItemPicture
import ru.finney.pet.ui.components.FinneyPanel
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.LevelBadge
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.sound.Sfx
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.RadiusCard

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
            // Траты — полосой «из плана» без зелёного: потратить больше — не успех.
            // Копилка — полосой прогресса: отложить сколько задумал — хорошо.
            FactRow(categoryIcon(Category.NEEDS), "Нужное", state.facts.needs, state.plan.needs)
            FactRow(categoryIcon(Category.WANTS), "Желаемое", state.facts.wants, state.plan.wants)
            FactRow(SavingsIcon, "Копилка", state.facts.savings, state.plan.savings, progress = true)
            if (state.facts.unplannedIncome > 0) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Пришло сверх плана", style = MaterialTheme.typography.bodyLarge, color = FinneyInk, modifier = Modifier.weight(1f))
                    CoinAmount(amount = state.facts.unplannedIncome, coinSize = 22.dp)
                }
            }
        }

        // Каждое условие отдельной строкой: ребёнку видно, что получилось и чего не хватило.
        FinneyPanel(title = "Условия уровня: нужно ${state.toPass} из 3") {
            // Коротко: знак и одно слово — про что условие. Полная фраза — для TalkBack.
            state.levelGame?.let {
                val need = if (state.gameRequired) "обязательно" else "по желанию"
                ScoreRow("Игра «$it»", state.gamePassed, "Игра уровня «$it» — $need", icon = state.levelGameIcon)
            }
            ScoreRow("Нужное", state.needsCovered, "Питомец сыт, чист и выспался")
            ScoreRow("План", state.planMatched, "Траты по плану")
            ScoreRow("Копилка", state.savingsAdded, "Отложено в копилку")
        }

        // Панели «Новое в мини-играх» здесь больше нет: ребёнок играет одну игру уровня
        // и списка остальных не видит — анонс того, что выбрать нельзя, только путал.

        CoinAmount(amount = state.balance)

        // План отдельно: это главный навык игры (ТЗ п. 2.5.5).
        PlanVerdict(state.planMatched)
    }
}

/**
 * Итог по плану — плашкой, а не кнопкой: без толстой обводки и блика, значок слева.
 * На плейтесте прежняя плашка выглядела как кнопка, и её пытались нажать.
 * Выполнен — зелёная с «✓»: это итог действия ребёнка. Не выполнен — без красного,
 * кремовая, с тетрадью плана и советом, что сделать иначе.
 */
@Composable
private fun PlanVerdict(matched: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard))
            .background(if (matched) FinneyGreen else FinneyCream)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        if (matched) CheckBadge(size = 36.dp) else FinneyIcon(FinneyIcons.Plan, size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (matched) "План выполнен!" else "План пока не вышел",
                style = MaterialTheme.typography.titleMedium,
                color = FinneyInk,
            )
            if (!matched) {
                Text(
                    "Перед покупкой загляни в план: сколько ещё можно.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinneyInk,
                )
            }
        }
    }
}

/**
 * Строка «потрачено из запланированного» и полоса под ней. [progress] — это
 * накопление, а не трата: полная полоса отмечена «✓». Направление узнаётся по
 * значку [icon], а не по цвету полосы.
 */
@Composable
private fun FactRow(icon: FinneyIcons, label: String, fact: Int, planned: Int, progress: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FinneyIcon(icon, size = 28.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
                modifier = Modifier.weight(1f),
            )
            Text(text = "$fact из $planned", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
        }
        if (progress) FillBar(fact, planned, Modifier.fillMaxWidth()) else SpendBar(fact, planned, Modifier.fillMaxWidth())
    }
}

/**
 * Условие уровня: ✓ или ↺ и одно слово — про что оно; цвет здесь ничего не решает.
 * [icon] — у игры уровня её картинка: игру узнают по ней, как в сцене.
 */
@Composable
private fun ScoreRow(label: String, earned: Boolean, description: String, icon: ItemArt? = null) {
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
        icon?.let { ItemPicture(it, label, 36.dp, Modifier.padding(end = 8.dp)) }
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
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
                balance = 15,
            ),
            onBack = {},
        )
    }
}
